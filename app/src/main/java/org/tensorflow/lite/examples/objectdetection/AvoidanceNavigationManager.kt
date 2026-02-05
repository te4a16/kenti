package org.tensorflow.lite.examples.objectdetection

import android.graphics.RectF

/**
 * 障害物を回避するためのナビゲーションメッセージを管理するクラス
 */
class AvoidanceNavigationManager {

    /**
     * 検出された物体の位置に基づき、適切な回避指示を返す
     * @param boundingBox 物体の外接矩形
     * @param imageWidth 解析画像の幅
     * @return ユーザーへの指示メッセージ
     */
    fun getAvoidanceMessage(boundingBox: RectF, imageWidth: Int): String {
        // 物体の中心座標（X軸）を計算
        val objectCenterX = boundingBox.centerX()
        // 画像全体に対する物体の相対的な位置（0.0〜1.0）
        val relativeX = objectCenterX / imageWidth

        // 現状は一律で注意喚起メッセージを返却
        return "顔を上げてください"
    }
}