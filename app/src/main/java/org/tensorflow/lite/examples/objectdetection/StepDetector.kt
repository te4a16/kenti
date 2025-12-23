package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class StepDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    // 外部から歩行検知時の処理を注入できるようにします
    var onStepDetected: (() -> Unit)? = null

    fun startListening() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.d("StepDetector", "歩行検知を開始しました")
        } ?: Log.e("StepDetector", "歩数計センサーが見つかりません")
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        Log.d("StepDetector", "歩行検知を停止しました")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            // 一歩検知した時の処理
            Log.d("StepDetector", "歩いています。")
            onStepDetected?.invoke()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 今回は使用しません
    }
}