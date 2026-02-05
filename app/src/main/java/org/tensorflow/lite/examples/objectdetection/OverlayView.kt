/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import java.util.LinkedList
import kotlin.math.max
import org.tensorflow.lite.task.vision.detector.Detection

/**
 * OverlayView: カメラ映像上に物体検出結果（バウンディングボックス、ラベル、距離情報）を描画するためのカスタムView
 */
class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    // 検出結果のリスト
    private var results: List<Detection> = LinkedList<Detection>()

    /**
     * 算出された距離情報をFragment等の呼び出し元に通知するためのリスナー
     */
    interface DistanceAlertListener {
        fun onDistanceUpdated(distanceMeters: Float, className: String)
    }

    // 距離通知用リスナーのインスタンス
    var distanceAlertListener: DistanceAlertListener? = null

    // 描画用のPaintオブジェクト
    private var boxPaint = Paint()            // 枠線用
    private var textBackgroundPaint = Paint() // テキスト背景用
    private var textPaint = Paint()           // 文字用

    // カメラ解像度と画面表示サイズの差を補正するスケール
    private var scaleFactor: Float = 1f

    // テキストの描画範囲（サイズ）計測用
    private var bounds = Rect()

    init {
        initPaints()
    }

    /**
     * Viewの状態をクリアする
     */
    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    /**
     * 枠線やテキストの描画スタイル（色、太さ、サイズ）を初期化
     */
    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    /**
     * Viewの描画処理。検出された物体の枠、ラベル、推定距離、ピクセル幅を順に描画する
     */
    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        // 1. デバッグ情報の描画（現在の仮想焦点距離を表示）
        val focalLengthConst = DistanceConstants.VIRTUAL_FOCAL_LENGTH_F
        val calibText = "FOCAL_LENGTH: ${focalLengthConst.toInt()}"

        textBackgroundPaint.getTextBounds(calibText, 0, calibText.length, bounds)
        val calibTextX = 20f
        val calibTextY = bounds.height() + 20f

        canvas.drawRect(
            calibTextX,
            calibTextY - bounds.height() - BOUNDING_RECT_TEXT_PADDING,
            calibTextX + bounds.width() + BOUNDING_RECT_TEXT_PADDING,
            calibTextY + BOUNDING_RECT_TEXT_PADDING,
            textBackgroundPaint
        )
        canvas.drawText(calibText, calibTextX, calibTextY, textPaint)

        // 2. 各検出結果の描画
        for (result in results) {
            val boundingBox = result.boundingBox

            // 座標を現在の画面表示スケールに変換
            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            // --- 距離計算ロジック ---
            val labelName = result.categories[0].label
            // ラベル名に対応する実世界の幅(m)を取得、不明な場合はデフォルト値
            val realWidth = DistanceConstants.LABEL_WIDTH_MAP[labelName] 
                            ?: DistanceConstants.DEFAULT_REAL_WIDTH_M

            // 画像内での物体の幅（ピクセル）
            val pixelWidth = boundingBox.right - boundingBox.left

            // 単眼カメラの原理に基づき距離(m)を算出: D = (実幅 * 焦点距離) / ピクセル幅
            val distanceMeters = if (pixelWidth > 0) {
                (realWidth * focalLengthConst) / pixelWidth
            } else {
                0.0f
            }

            // 算出した距離をリスナー経由で通知
            distanceAlertListener?.onDistanceUpdated(distanceMeters, labelName)

            // --- 描画の実行 ---
            // 枠線の描画
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // 表示テキストの構築（ラベル、スコア、距離、ピクセル幅）
            val label = result.categories[0].label
            val score = String.format("%.2f", result.categories[0].score)
            val distanceText = String.format(" (%.2f m)", distanceMeters)
            val pixelWidthText = String.format(" | W: %d px", pixelWidth.toInt())
            val drawableText = "${label} ${score}${distanceText} ${pixelWidthText}"

            // テキスト背景（黒矩形）の描画
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            canvas.drawRect(
                left,
                top,
                left + bounds.width() + BOUNDING_RECT_TEXT_PADDING,
                top + bounds.height() + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // テキスト（白文字）の描画
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }
    }

    /**
     * ObjectDetectorから検出結果を受け取り、Viewを更新する
     */
    fun setResults(
      detectionResults: MutableList<Detection>,
      imageHeight: Int,
      imageWidth: Int,
    ) {
        results = detectionResults
        // カメラのプレビューサイズと実際のViewのサイズ比からスケールを計算
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
        // 再描画を要求
        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}