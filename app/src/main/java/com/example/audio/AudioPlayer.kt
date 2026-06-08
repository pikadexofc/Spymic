package com.example.audio

import android.media.MediaPlayer
import java.io.IOException

class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    
    fun playFile(filePath: String, onCompletion: () -> Unit) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(filePath)
                    prepare()
                    setOnCompletionListener { onCompletion() }
                    start()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } else {
            mediaPlayer?.start()
        }
    }
    
    fun pause() {
        mediaPlayer?.pause()
    }
    
    fun seekTo(positionMs: Int) {
        val clamp = if (positionMs < 0) 0 else if (positionMs > getDuration()) getDuration() else positionMs
        mediaPlayer?.seekTo(clamp)
    }

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    
    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
    
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
