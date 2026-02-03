package org.tensorflow.lite.examples.objectdetection

import android.graphics.RectF

//メッセージのみ
class AvoidanceNavigationManager {

    fun getAvoidanceMessage(boundingBox: RectF, imageWidth: Int): String {
        val objectCenterX = boundingBox.centerX()
        val relativeX = objectCenterX / imageWidth

        return "顔を上げてください"
    }
}