package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.os.Bundle

/**
 * 物体との距離に応じて音声通知とバイブレーションを制御するクラス
 */
class DistanceAlertManager(private val context: Context) {

    private var lastDistanceMeters = -1f // 前回の計測距離
    private var tts: TextToSpeech? = null
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var lastAlertTime = 0L // 最後に警告を出した時間

    companion object {
        private const val ALERT_INTERVAL_MS = 3000L // 次の警告までの最小間隔
        private const val ALERT_DISTANCE_4M = 4.0f  // 音声警告を開始する距離
        private const val ALERT_DISTANCE_2M = 2.0f  // バイブレーションを追加する距離
        private const val TARGET_CLASS = "person"   // 警告対象のラベル
    }

    init {
        // TextToSpeechの初期化
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.JAPANESE
            }
        }
    }

    /**
     * 距離や歩行状態をチェックし、必要に応じて警告を実行する
     */
    fun checkAndAlert(distanceMeters: Float, className: String, topRatio: Float, isWalking: Boolean) {
        // 対象クラス以外は無視
        if (className != TARGET_CLASS) return

        // 接近判定：前回の距離と比較して近づいているか確認
        if (lastDistanceMeters > 0) {
            val delta = lastDistanceMeters - distanceMeters 
            
            // 距離の変化が少ない、または遠ざかっている場合は警告せず終了
            if (delta < 0.1f) {
                lastDistanceMeters = distanceMeters
                return 
            }
        }
        lastDistanceMeters = distanceMeters

        // 停止中は警告を行わない
        if (!isWalking) return

        // 足元除外：検出枠の上端が画面下部(70%以上)にある場合は無視
        if (topRatio > 0.70f) return

        // 警告間隔のチェック
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAlertTime < ALERT_INTERVAL_MS) return

        val distanceMessage = "顔をあげてください。"

        // 距離に応じた警告の実行
        when {
            // 2m以内の場合は音声＋バイブ
            distanceMeters <= ALERT_DISTANCE_2M -> {
                speak(distanceMessage)
                vibrate()
                lastAlertTime = currentTime
            }
            // 4m以内の場合は音声のみ
            distanceMeters <= ALERT_DISTANCE_4M -> {
                speak(distanceMessage)
                lastAlertTime = currentTime
            }
        }
    }

    /**
     * 設定された音量でテキストを読み上げる
     */
    private fun speak(message: String) {
        // SharedPreferenceから音量設定を取得
        val sharedPrefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val volumePercent = sharedPrefs.getInt("alert_volume", 80)
        
        val params = Bundle()
        // 0.0f〜1.0fの範囲に変換して音量をセット
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumePercent / 100f)

        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, "DISTANCE_ALERT")
    }

    /**
     * バイブレーションを実行（0.5秒）
     */
    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    /**
     * リソースの解放処理
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}