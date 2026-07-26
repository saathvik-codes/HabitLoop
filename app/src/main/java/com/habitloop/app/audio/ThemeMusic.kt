package com.habitloop.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import com.habitloop.app.R

object ThemeMusicPrefs {
    private const val PREFS = "soundscape_prefs"
    private const val ENABLED = "theme_enabled"
    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false)
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, enabled).apply()
    }
}

object ThemeMusicController {
    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player?.pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player?.setVolume(.08f, .08f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                player?.setVolume(.28f, .28f)
                player?.start()
            }
        }
    }

    fun play(context: Context) {
        if (!ThemeMusicPrefs.enabled(context)) return
        val app = context.applicationContext
        val manager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager = manager
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        focusRequest = request
        if (manager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        val active = player ?: MediaPlayer.create(app, R.raw.habitloop_theme).also {
            it.isLooping = true
            it.setVolume(.28f, .28f)
            player = it
        }
        if (!active.isPlaying) active.start()
    }

    fun pause() {
        if (player?.isPlaying == true) player?.pause()
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
    }

    fun disable(context: Context) {
        ThemeMusicPrefs.setEnabled(context, false)
        pause()
        player?.release()
        player = null
    }
}
