package com.learn.util.services

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*


class SoundPool(val context: Context) {

    private var ringtone: Ringtone? = null
    private var ringingState: Pair<Boolean, Boolean>? = null
    private var vibrator: Vibrator? = null
    private var mMediaPlayer: MediaPlayer? = null
    private val maxDuration: Long = 1000 * 30

    init {
        ringingState = checkRingingState()
        ringingState?.let {
            if (it.first && it.second) {
                setRinging()
                addVibration()
            } else if (!it.first && it.second) {
                addVibration()
            }
        }
        autoStopRingtoneAndVibration()
    }

    private fun addVibration() {
        val pattern: LongArray = longArrayOf(
            1000L,
            1000L
        )
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    pattern,
                    0
                )
            )
        } else {
            vibrator?.vibrate(
                pattern,
                0
            )
        }
    }

    fun stopRingtoneAndVibration() {
        mMediaPlayer?.stop()
        vibrator?.cancel()
    }

    private fun checkRingingState(): Pair<Boolean, Boolean> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> true to true
            AudioManager.RINGER_MODE_SILENT -> false to false
            AudioManager.RINGER_MODE_VIBRATE -> false to true
            else -> true to true
        }
    }

    private fun setRinging() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(
                context.applicationContext,
                ringtoneUri
            )
            mMediaPlayer = MediaPlayer()
            mMediaPlayer?.setDataSource(
                context,
                ringtoneUri
            )
            mMediaPlayer?.isLooping = true
            mMediaPlayer?.prepare()
            mMediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun autoStopRingtoneAndVibration() {
        Handler(Looper.myLooper()!!).postDelayed({
            stopRingtoneAndVibration()
        }, maxDuration)
    }
}
