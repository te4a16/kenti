package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.os.Bundle

//距離音声通知のコントローラぽいやつ
class DistanceAlertManager(private val context: Context) {

    private var lastDistanceMeters = -1f //前回の距離を保持
    private var tts: TextToSpeech? = null
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var lastAlertTime = 0L

    companion object {
        private const val ALERT_INTERVAL_MS = 3000L
        private const val ALERT_DISTANCE_4M = 2.0f
        private const val ALERT_DISTANCE_2M = 1.0f
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
    fun checkAndAlert(distanceMeters: Float, className: String, topRatio: Float) {
        if (className != TARGET_CLASS) return

        // 足元除外（画面下部にいすぎる場合は無視）
        if (topRatio > 0.70f) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAlertTime < ALERT_INTERVAL_MS) return

        val distanceMessage = "顔をあげてください。"

        // 距離判定（同方向検知は削除済み）
        if (distanceMeters <= ALERT_DISTANCE_2M) {
            speak(distanceMessage)
            vibrate()
            lastAlertTime = currentTime
        } else if (distanceMeters <= ALERT_DISTANCE_4M) {
            speak(distanceMessage)
            lastAlertTime = currentTime
        }
    }

    // speak 関数を以下の内容に差し替えてください
    private fun speak(message: String) {
        // 保存された音量（0〜100）を読み込む
        val sharedPrefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val volumePercent = sharedPrefs.getInt("alert_volume", 80)
        
        // TextToSpeech用のパラメータを作成
        val params = Bundle()
        // 0.0f (無音) 〜 1.0f (最大) に変換してセット
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumePercent / 100f)

        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, "DISTANCE_ALERT")
    }


    //バイブレーション
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