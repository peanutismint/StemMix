package com.stemmix

import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat

class ChannelStrip(private val rootView: View, private val label: String) {
    
    private val channelLabel: TextView = rootView.findViewById(R.id.channelLabel)
    private val volumeDisplay: TextView = rootView.findViewById(R.id.volumeDisplay)
    private val fader: SeekBar = rootView.findViewById(R.id.fader)
    private val btnMute: Button = rootView.findViewById(R.id.btnMute)
    private val btnSolo: Button = rootView.findViewById(R.id.btnSolo)
    private val vuMeter: View = rootView.findViewById(R.id.vuMeter)
    
    var isMuted = false
        private set
    var isSolo = false
        private set
    private var volume = 100
    
    var onVolumeChanged: ((Int) -> Unit)? = null
    var onMuteChanged: ((Boolean) -> Unit)? = null
    var onSoloChanged: ((Boolean) -> Unit)? = null
    
    init {
        channelLabel.text = label
        
        // Fader (note: rotation handled in XML)
        fader.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volume = progress
                volumeDisplay.text = progress.toString()
                onVolumeChanged?.invoke(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Mute button
        btnMute.setOnClickListener {
            toggleMute()
        }
        
        // Solo button
        btnSolo.setOnClickListener {
            toggleSolo()
        }
    }
    
    fun toggleMute() {
        if (!isSolo) {  // Can't mute while solo'd
            setMuted(!isMuted)
            onMuteChanged?.invoke(isMuted)
        }
    }
    
    fun setMuted(muted: Boolean, fromSolo: Boolean = false) {
        isMuted = muted
        btnMute.backgroundTintList = if (muted) {
            ContextCompat.getColorStateList(rootView.context, R.color.mute_red)
        } else {
            ContextCompat.getColorStateList(rootView.context, R.color.light_gray)
        }
        
        if (!fromSolo) {
            onMuteChanged?.invoke(isMuted)
        }
    }
    
    fun toggleSolo() {
        isSolo = !isSolo
        btnSolo.backgroundTintList = if (isSolo) {
            ContextCompat.getColorStateList(rootView.context, R.color.solo_green)
        } else {
            ContextCompat.getColorStateList(rootView.context, R.color.light_gray)
        }
        
        if (isSolo) {
            setMuted(false)  // Un-mute when solo'd
        }
        
        onSoloChanged?.invoke(isSolo)
    }
    
    fun getEffectiveVolume(): Float {
        return if (isMuted) 0f else volume / 100f
    }
    
    fun updateVuMeter(level: Float) {
        // Update VU meter width based on level (0.0 to 1.0)
        val params = vuMeter.layoutParams
        params.width = (40 * level).toInt().coerceIn(0, 40)
        vuMeter.layoutParams = params
    }
}
