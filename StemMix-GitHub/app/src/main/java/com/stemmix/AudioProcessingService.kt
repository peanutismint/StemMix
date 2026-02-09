package com.stemmix

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import kotlin.math.abs

class AudioProcessingService : Service() {
    
    private val binder = LocalBinder()
    private lateinit var mediaProjection: MediaProjection
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var processingJob: Job? = null
    
    var isCapturing = false
        private set
    
    // Audio configuration
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_STEREO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 4
    
    // Processing state
    private var stemSeparator: StemSeparator? = null
    private var pitchShifter: PitchShifter? = null
    
    // Mix levels (0.0 to 1.0)
    private var vocalsLevel = 1f
    private var drumsLevel = 1f
    private var bassLevel = 1f
    private var otherLevel = 1f
    
    // Effects
    private var keyShiftSemitones = 0
    private var tempoShift = 1f
    
    // Status callback
    var onStatusUpdate: ((buffer: Float, cpu: Int, battery: String) -> Unit)? = null
    
    inner class LocalBinder : Binder() {
        fun getService(): AudioProcessingService = this@AudioProcessingService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>("data")
        
        if (resultCode != -1 && data != null) {
            startForegroundService()
            startCapture(resultCode, data)
        }
        
        return START_STICKY
    }
    
    private fun startForegroundService() {
        val channelId = "audio_processing"
        val channel = NotificationChannel(
            channelId,
            "Audio Processing",
            NotificationManager.IMPORTANCE_LOW
        )
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.processing_notification_title))
            .setContentText(getString(R.string.processing_notification_text))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        
        startForeground(1, notification)
    }
    
    private fun startCapture(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        // Initialize stem separator and pitch shifter
        stemSeparator = StemSeparator(this)
        pitchShifter = PitchShifter(sampleRate)
        
        // Setup audio capture
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()
        
        val audioFormat = AudioFormat.Builder()
            .setEncoding(this.audioFormat)
            .setSampleRate(sampleRate)
            .setChannelMask(channelConfig)
            .build()
        
        audioRecord = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()
        
        // Setup audio output
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        
        audioRecord?.startRecording()
        audioTrack?.play()
        isCapturing = true
        
        // Start processing loop
        processingJob = serviceScope.launch {
            processAudioLoop()
        }
    }
    
    private suspend fun processAudioLoop() {
        val inputBuffer = ByteArray(bufferSize)
        val audioBuffer = mutableListOf<ShortArray>()
        val chunkSize = sampleRate * 2 // 2 seconds of audio for processing
        
        var cpuStartTime = System.currentTimeMillis()
        var processedSamples = 0L
        
        while (isActive && isCapturing) {
            // Read audio from capture
            val bytesRead = audioRecord?.read(inputBuffer, 0, inputBuffer.size) ?: 0
            
            if (bytesRead > 0) {
                // Convert bytes to shorts
                val samples = ShortArray(bytesRead / 2)
                ByteBuffer.wrap(inputBuffer).asShortBuffer().get(samples)
                
                // Add to buffer
                audioBuffer.add(samples)
                processedSamples += samples.size
                
                // Process when we have enough samples
                if (audioBuffer.sumOf { it.size } >= chunkSize) {
                    val chunk = combineBuffers(audioBuffer)
                    audioBuffer.clear()
                    
                    // Process the chunk
                    val processedChunk = processAudioChunk(chunk)
                    
                    // Output processed audio
                    val outputBytes = ByteArray(processedChunk.size * 2)
                    ByteBuffer.wrap(outputBytes).asShortBuffer().put(processedChunk)
                    audioTrack?.write(outputBytes, 0, outputBytes.size)
                    
                    // Calculate and report status
                    val elapsedTime = (System.currentTimeMillis() - cpuStartTime) / 1000f
                    val cpuUsage = ((elapsedTime / (processedSamples / sampleRate.toFloat())) * 100).toInt()
                    val bufferTime = audioBuffer.sumOf { it.size } / sampleRate.toFloat()
                    
                    onStatusUpdate?.invoke(bufferTime, cpuUsage, estimateBatteryTime())
                    
                    cpuStartTime = System.currentTimeMillis()
                    processedSamples = 0
                }
            }
            
            delay(10) // Small delay to prevent tight loop
        }
    }
    
    private fun combineBuffers(buffers: List<ShortArray>): ShortArray {
        val totalSize = buffers.sumOf { it.size }
        val combined = ShortArray(totalSize)
        var offset = 0
        for (buffer in buffers) {
            buffer.copyInto(combined, offset)
            offset += buffer.size
        }
        return combined
    }
    
    private suspend fun processAudioChunk(audio: ShortArray): ShortArray {
        // 1. Separate stems
        val stems = stemSeparator?.separate(audio) ?: mapOf(
            "vocals" to audio,
            "drums" to ShortArray(audio.size),
            "bass" to ShortArray(audio.size),
            "other" to ShortArray(audio.size)
        )
        
        // 2. Apply pitch shift if needed
        val shiftedStems = if (keyShiftSemitones != 0) {
            stems.mapValues { (_, stem) ->
                pitchShifter?.shift(stem, keyShiftSemitones) ?: stem
            }
        } else {
            stems
        }
        
        // 3. Apply tempo shift if needed (simplified - just resampling for now)
        val tempoAdjustedStems = if (tempoShift != 1f) {
            shiftedStems.mapValues { (_, stem) ->
                applyTempoShift(stem, tempoShift)
            }
        } else {
            shiftedStems
        }
        
        // 4. Mix stems with user-defined levels
        return mixStems(
            tempoAdjustedStems["vocals"] ?: ShortArray(audio.size),
            tempoAdjustedStems["drums"] ?: ShortArray(audio.size),
            tempoAdjustedStems["bass"] ?: ShortArray(audio.size),
            tempoAdjustedStems["other"] ?: ShortArray(audio.size)
        )
    }
    
    private fun mixStems(vocals: ShortArray, drums: ShortArray, bass: ShortArray, other: ShortArray): ShortArray {
        val mixed = ShortArray(vocals.size)
        
        for (i in vocals.indices) {
            val sample = (
                vocals[i] * vocalsLevel +
                drums[i] * drumsLevel +
                bass[i] * bassLevel +
                other[i] * otherLevel
            ).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            
            mixed[i] = sample.toShort()
        }
        
        return mixed
    }
    
    private fun applyTempoShift(audio: ShortArray, factor: Float): ShortArray {
        // Simple resampling (not phase-vocoder quality, but works)
        val newSize = (audio.size / factor).toInt()
        val result = ShortArray(newSize)
        
        for (i in result.indices) {
            val srcIndex = (i * factor).toInt()
            result[i] = if (srcIndex < audio.size) audio[srcIndex] else 0
        }
        
        return result
    }
    
    private fun estimateBatteryTime(): String {
        // Simplified battery estimation
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        // This would need more sophisticated calculation in production
        return "2h 30m"
    }
    
    fun setMixLevels(vocals: Float, drums: Float, bass: Float, other: Float) {
        vocalsLevel = vocals
        drumsLevel = drums
        bassLevel = bass
        otherLevel = other
    }
    
    fun setKeyShift(semitones: Int) {
        keyShiftSemitones = semitones
    }
    
    fun setTempoShift(factor: Float) {
        tempoShift = factor
    }
    
    fun togglePlayPause() {
        // This would require MediaController integration
        // Limited by what the source app exposes
    }
    
    fun skipToPrevious() {
        // MediaController integration
    }
    
    fun skipToNext() {
        // MediaController integration
    }
    
    fun stopCapture() {
        isCapturing = false
        processingJob?.cancel()
        
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        
        mediaProjection.stop()
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopCapture()
    }
}
