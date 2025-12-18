package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 距離に応じた警告（音声・バイブ）を管理するクラス
 *
 * 仕様：
 *  ・2m以内 → 音声通知
 *  ・1m以内 → 音声 + バイブ通知
 *  ・指定クラス（例：person）のみ警告対象
 */
class DistanceAlertManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private val vibrator =
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    // 連続通知防止用
    private var lastAlertTime = 0L

    companion object {
        private const val ALERT_INTERVAL_MS = 3000L   // 3秒に1回まで
        private const val ALERT_DISTANCE_2M = 2.0f
        private const val ALERT_DISTANCE_1M = 1.0f

        // 警告対象とするクラス名（COCOラベル）
        private const val TARGET_CLASS = "person"
    }

    init {
        // TextToSpeech 初期化
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.JAPAN
                tts?.setSpeechRate(1.1f)
            }
        }
    }

    /**
     * 距離とクラス名を受け取り、条件に応じて警告を行う
     */
    fun checkAndAlert(distanceMeters: Float, className: String) {

        // 対象クラス以外は無視
        if (className != TARGET_CLASS) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAlertTime < ALERT_INTERVAL_MS) return

        // 「〇メートル先です」と喋る用テキスト
        val distanceMessage =
            String.format("危険です。%.1fメートル先に人がいます。", distanceMeters)

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

    // 音声読み上げ
    private fun speak(message: String) {
        tts?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "DISTANCE_ALERT"
        )
    }

    // バイブレーション
    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    // 解放処理（必須）
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
