package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import java.util.Locale

class DistanceAlertManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var lastAlertTime = 0L

    companion object {
        private const val ALERT_INTERVAL_MS = 3000L
        private const val ALERT_DISTANCE_2M = 2.0f
        private const val ALERT_DISTANCE_1M = 1.0f
        private const val TARGET_CLASS = "person"
    }

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.JAPANESE
            }
        }
    }

    /**
     * @param topRatio 枠の上端の座標割合 (0.0〜1.0)
     */
     /**
     * @param isWalking 追加：現在の歩行状態
     */
    fun checkAndAlert(distanceMeters: Float, className: String, topRatio: Float, isWalking: Boolean) {
        if (className != TARGET_CLASS) return

        // 歩いていない場合は、警告処理（音声・バイブ）自体を行わない
        if (!isWalking) return

        // 【足元除外】枠のてっぺんが画面の下部3割(0.7以上)にあるなら無視
        if (topRatio > 0.70f) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAlertTime < ALERT_INTERVAL_MS) return

        val distanceMessage = String.format("危険です。%.1fメートル先に人がいます。", distanceMeters)

        when {
            distanceMeters <= ALERT_DISTANCE_1M -> {
                speak(distanceMessage)
                vibrate()
                lastAlertTime = currentTime
            }
            distanceMeters <= ALERT_DISTANCE_2M -> {
                speak(distanceMessage)
                lastAlertTime = currentTime
            }
        }
    }

    private fun speak(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "DISTANCE_ALERT")
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    // CameraFragmentのonDestroyViewから呼ばれる関数を追加
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}