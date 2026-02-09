package com.stemmix

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    
    private lateinit var audioService: AudioProcessingService
    private var serviceBound = false
    
    // UI Components
    private lateinit var btnStartStop: Button
    private lateinit var keyDisplay: TextView
    private lateinit var tempoDisplay: TextView
    private lateinit var bufferStatus: TextView
    private lateinit var cpuUsage: TextView
    private lateinit var batteryTime: TextView
    
    // Channel strips
    private lateinit var vocalsChannel: ChannelStrip
    private lateinit var drumsChannel: ChannelStrip
    private lateinit var bassChannel: ChannelStrip
    private lateinit var otherChannel: ChannelStrip
    
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                startAudioCapture(result.resultCode, data)
            }
        }
    }
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioProcessingService.LocalBinder
            audioService = binder.getService()
            serviceBound = true
            setupServiceCallbacks()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupChannelStrips()
        setupControls()
        checkPermissions()
    }
    
    private fun initializeViews() {
        btnStartStop = findViewById(R.id.btnStartStop)
        keyDisplay = findViewById(R.id.keyDisplay)
        tempoDisplay = findViewById(R.id.tempoDisplay)
        bufferStatus = findViewById(R.id.bufferStatus)
        cpuUsage = findViewById(R.id.cpuUsage)
        batteryTime = findViewById(R.id.batteryTime)
        
        findViewById<FloatingActionButton>(R.id.fabSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
    
    private fun setupChannelStrips() {
        vocalsChannel = ChannelStrip(findViewById(R.id.vocalsChannel), "VOCALS")
        drumsChannel = ChannelStrip(findViewById(R.id.drumsChannel), "DRUMS")
        bassChannel = ChannelStrip(findViewById(R.id.bassChannel), "BASS")
        otherChannel = ChannelStrip(findViewById(R.id.otherChannel), "OTHER")
        
        val channels = listOf(vocalsChannel, drumsChannel, bassChannel, otherChannel)
        
        // Setup solo behavior
        channels.forEach { channel ->
            channel.onSoloChanged = { isSolo ->
                if (isSolo) {
                    channels.filter { it != channel }.forEach { 
                        it.setMuted(true, fromSolo = true) 
                    }
                } else {
                    // Check if any other channel is solo'd
                    val anyOtherSolo = channels.any { it != channel && it.isSolo }
                    if (!anyOtherSolo) {
                        channels.forEach { it.setMuted(false, fromSolo = true) }
                    }
                }
                updateServiceMixLevels()
            }
            
            channel.onVolumeChanged = {
                updateServiceMixLevels()
            }
            
            channel.onMuteChanged = {
                updateServiceMixLevels()
            }
        }
    }
    
    private fun setupControls() {
        // Key shift slider
        findViewById<SeekBar>(R.id.keyShiftSlider).apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val semitones = progress - 6  // Range: -6 to +6
                    keyDisplay.text = formatKeyShift(semitones)
                    if (serviceBound) {
                        audioService.setKeyShift(semitones)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        // Tempo shift slider
        findViewById<SeekBar>(R.id.tempoShiftSlider).apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val tempo = 50 + progress  // Range: 50% to 150%
                    tempoDisplay.text = "$tempo%"
                    if (serviceBound) {
                        audioService.setTempoShift(tempo / 100f)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        // Start/Stop button
        btnStartStop.setOnClickListener {
            if (serviceBound && audioService.isCapturing) {
                stopAudioCapture()
            } else {
                requestMediaProjection()
            }
        }
        
        // Transport controls (these will be limited by what media apps expose)
        findViewById<Button>(R.id.btnPlayPause).setOnClickListener {
            if (serviceBound) audioService.togglePlayPause()
        }
        
        findViewById<Button>(R.id.btnPrevious).setOnClickListener {
            if (serviceBound) audioService.skipToPrevious()
        }
        
        findViewById<Button>(R.id.btnNext).setOnClickListener {
            if (serviceBound) audioService.skipToNext()
        }
        
        findViewById<Button>(R.id.btnLoop).setOnClickListener {
            // Toggle loop mode
        }
    }
    
    private fun formatKeyShift(semitones: Int): String {
        val keys = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
        return when {
            semitones == 0 -> "Original"
            semitones > 0 -> "+$semitones"
            else -> "$semitones"
        }
    }
    
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
        
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 100)
        }
    }
    
    private fun requestMediaProjection() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
    
    private fun startAudioCapture(resultCode: Int, data: Intent) {
        val intent = Intent(this, AudioProcessingService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("data", data)
        }
        
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        btnStartStop.text = getString(R.string.stop_capture)
    }
    
    private fun stopAudioCapture() {
        if (serviceBound) {
            audioService.stopCapture()
            unbindService(serviceConnection)
            serviceBound = false
        }
        
        btnStartStop.text = getString(R.string.start_capture)
    }
    
    private fun updateServiceMixLevels() {
        if (!serviceBound) return
        
        audioService.setMixLevels(
            vocals = vocalsChannel.getEffectiveVolume(),
            drums = drumsChannel.getEffectiveVolume(),
            bass = bassChannel.getEffectiveVolume(),
            other = otherChannel.getEffectiveVolume()
        )
    }
    
    private fun setupServiceCallbacks() {
        // Setup callbacks to receive status updates from service
        audioService.onStatusUpdate = { buffer, cpu, battery ->
            runOnUiThread {
                bufferStatus.text = getString(R.string.buffer_status, String.format("%.1fs", buffer))
                cpuUsage.text = getString(R.string.cpu_usage, cpu)
                batteryTime.text = getString(R.string.battery_time, battery)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
        }
    }
}
