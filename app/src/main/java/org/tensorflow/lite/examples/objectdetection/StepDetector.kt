package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * 端末のステップセンサーを使用して、ユーザーが歩行中かどうかを判定するクラス
 */
class StepDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    // TYPE_STEP_DETECTOR: 一歩ごとにイベントを発生させるセンサー
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    // 現在歩行中かどうかを保持するフラグ（外部からは get のみ可能）
    var isWalking: Boolean = false
        private set

    private val handler = Handler(Looper.getMainLooper())
    
    /**
     * 一定時間歩行が検知されない場合に「停止」とみなす処理
     */
    private val stopWalkingRunnable = Runnable {
        if (isWalking) {
            isWalking = false
            Log.d("StepDetector", "立ち止まりました。")
        }
    }

    /**
     * センサーの監視を開始する
     */
    fun startListening() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * センサーの監視を停止する
     */
    fun stopListening() {
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(stopWalkingRunnable)
    }

    /**
     * センサーイベントを受け取った際の処理
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            // 一歩でも検知すれば歩行中フラグを立てる
            if (!isWalking) {
                isWalking = true
                Log.d("StepDetector", "歩行を検知しました。")
            }

            // 既にセットされている「停止タイマー」をキャンセル
            handler.removeCallbacks(stopWalkingRunnable)
            
            // 1秒間（1000ms）次の一歩が検知されなければ、Runnableを実行して停止状態にする
            // ※歩行速度に合わせてこの時間は調整可能
            handler.postDelayed(stopWalkingRunnable, 1000)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 精度変更時の処理（今回は未使用）
    }
}