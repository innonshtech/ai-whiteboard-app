package com.avitrisphare.whiteboard.presentation.canvas.utility

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SoundAlertHelper {

    /**
     * Plays a clear, multi-tone chime/beep alert when the timer finishes.
     * Uses Android ToneGenerator with RingtoneManager fallback.
     */
    fun playTimerAlert(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                // Try ToneGenerator first (built-in synthesizer, 0 latency, guaranteed on Android)
                val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 95)
                repeat(3) {
                    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 280)
                    delay(380)
                }
                delay(100)
                toneGen.release()
            } catch (t: Throwable) {
                // Fallback to default notification sound
                try {
                    val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    alertUri?.let { uri ->
                        val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
                        ringtone?.play()
                    }
                } catch (_: Throwable) {
                    // Ignore audio failure if running on a silent headless panel
                }
            }
        }
    }
}
