package org.tensorflow.lite.examples.objectdetection

import android.graphics.RectF

/**
 * 前方の対象物を避ける方向を判定するクラス
 */
class AvoidanceNavigationManager {

    /**
     * バウンディングボックスの位置から回避メッセージを生成する
     * @param boundingBox 検出された物体の矩形
     * @param imageWidth 入力画像の横幅
     */
    fun getAvoidanceMessage(boundingBox: RectF, imageWidth: Int): String {
        // 物体の中心のX座標
        val objectCenterX = boundingBox.centerX()
        
        // 画面の横幅に対する相対位置 (0.0: 左端, 1.0: 右端)
        val relativeX = objectCenterX / imageWidth

        return when {
            // 人が右側にいる (画面右側 60% 以降) なら「左」へ
            relativeX > 0.6f -> "左に避けてください ⬅️"
            // 人が左側にいる (画面左側 40% 以前) なら「右」へ
            relativeX < 0.4f -> "右に避けてください ➡️"
            // それ以外（真ん中付近）
            else -> "正面に人がいます！大きく避けてください ⚠️"
        }
    }
}