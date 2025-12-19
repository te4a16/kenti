package org.tensorflow.lite.examples.objectdetection

import android.graphics.RectF

class AvoidanceNavigationManager {

    fun getAvoidanceMessage(boundingBox: RectF, imageWidth: Int): String {
        val objectCenterX = boundingBox.centerX()
        val relativeX = objectCenterX / imageWidth

        return when {
            // 対象が右側にいる(0.5より大きい)なら、左へ避ける
            relativeX > 0.5f -> "左に避けてください ⬅️"
            // 対象が左側や正面(0.5以下)にいるなら、右へ避ける
            else -> "右に避けてください ➡️"
        }
    }
}