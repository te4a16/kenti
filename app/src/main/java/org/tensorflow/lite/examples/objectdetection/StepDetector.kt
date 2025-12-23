package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log

class StepDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    // 現在歩行中かどうかを保持するフラグ
    var isWalking: Boolean = false
        private set

    private val handler = Handler(Looper.getMainLooper())
    private val stopWalkingRunnable = Runnable {
        if (isWalking) {
            isWalking = false
            Log.d("StepDetector", "立ち止まりました。")
        }
    }

    fun startListening() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(stopWalkingRunnable)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            if (!isWalking) {
                isWalking = true
                Log.d("StepDetector", "歩行を検知しました。")
            }

            // 一歩検知するたびに「停止タイマー」をリセット
            handler.removeCallbacks(stopWalkingRunnable)
            // 2秒間次の一歩がなければ停止とみなす
            handler.postDelayed(stopWalkingRunnable, 2000)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}