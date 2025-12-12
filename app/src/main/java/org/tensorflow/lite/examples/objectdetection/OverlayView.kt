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

/*
 * OverlayView: カメラ映像上に物体検出結果（バウンディングボックスとラベル）を描画するための View
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

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    // 検出結果リスト
    private var results: List<Detection> = LinkedList<Detection>()

    // ボックス・テキスト背景・文字の描画に使用する Paint
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    // カメラ画像と View のスケール差を補正する係数
    private var scaleFactor: Float = 1f

    // テキスト描画時のサイズ取得用
    private var bounds = Rect()

    init {
        // 各 Paint の初期設定
        initPaints()
    }

    //Viewをクリアして、Paintを初期状態に戻す
    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    //バウンディングボックス描画用やテキスト描画用の Paint を初期化する
    private fun initPaints() {
        // ラベル背景の黒い四角の設定
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        // ラベル文字の設定（白文字）
        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        // バウンディングボックスの設定（線色・太さ）
        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    //検出結果の描画処理
    //カメラフレーム上にViewを重ね、四角形とラベルを表示する
    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        for (result in results) {
            val boundingBox = result.boundingBox

            // 検出結果の座標を View のスケールに合わせる
            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            // バウンディングボックスを描画
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // ラベル文字（カテゴリ名 + 信頼度）
            val drawableText =
                result.categories[0].label + " " +
                        String.format("%.2f", result.categories[0].score)

            // 表示テキストの背後に矩形を描画する
            // テキスト背景のサイズ計算
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            // テキスト背景の黒い四角を描画
            canvas.drawRect(
                left,
                top,
                left + textWidth + Companion.BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + Companion.BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // ラベル文字を描画
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }
    }

    //カメラからの検出結果を受け取り、描画用データとしてセットする
    fun setResults(
      detectionResults: MutableList<Detection>, //検出結果一覧
      imageHeight: Int,                         //カメラ画像の高さ
      imageWidth: Int,                          //カメラ画像の幅
    ) {
        results = detectionResults

        // プレビュービューはFILL_STARTモードです。そのため、
        //キャプチャされた画像が表示されるサイズに合わせて、
        //バウンディングボックスを拡大する必要があります。
        //カメラ画像と View のスケール差を補正する
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }

    companion object {
        // ラベル背景の余白
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
