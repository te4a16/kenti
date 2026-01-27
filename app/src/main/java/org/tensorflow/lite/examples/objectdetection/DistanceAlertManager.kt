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
        private const val ALERT_DISTANCE_4M = 4.0f
        private const val ALERT_DISTANCE_2M = 2.0f
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

        //同方向判定処理
        if (lastDistanceMeters > 0) {
            val delta = lastDistanceMeters - distanceMeters //プラスなら近づいている
            
            // 誤差を考慮したしきい値（例：0.3m以上一気に縮まったら「接近」とみなす）
            // この 0.3f を調整して、感度を決めます
            if (delta < 0.3f) {
                // あまり距離が変わっていない、または遠ざかっている場合は更新だけして終了
                lastDistanceMeters = distanceMeters
                return 
            }
        }
        lastDistanceMeters = distanceMeters

        // 歩いていない場合は、警告処理（音声・バイブ）自体を行わない
        //if (!isWalking) return

        // 【足元除外】枠のてっぺんが画面の下部3割(0.7以上)にあるなら無視
        if (topRatio > 0.70f) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAlertTime < ALERT_INTERVAL_MS) return

        val distanceMessage = String.format("顔をあげてください。%.1fメートル先に障害物があります。", distanceMeters)

        //4メートル以下で音声通知
        when {
            distanceMeters <= ALERT_DISTANCE_2M -> {
                speak(distanceMessage)
                vibrate()
                lastAlertTime = currentTime
            }
            distanceMeters <= ALERT_DISTANCE_4M -> {
                speak(distanceMessage)
                lastAlertTime = currentTime
            }
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