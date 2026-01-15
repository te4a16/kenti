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
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
// ⭐ MediaPipe の型に変更
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import kotlin.math.max
import kotlin.math.min
import android.util.Log

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    // 検出結果リスト
    private var results: ObjectDetectorResult? = null

    /**
     * 距離情報を外部（Fragmentなど）に通知するためのリスナー
     */
    interface DistanceAlertListener {
        fun onDistanceUpdated(distanceMeters: Float, className: String)
    }

    // 距離通知用リスナー
    var distanceAlertListener: DistanceAlertListener? = null

    // ボックス・テキスト背景・文字の描画に使用する Paint
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    // カメラ画像と View のスケール差を補正する係数
    private var scaleFactor: Float = 1f

    // テキスト描画時のサイズ取得用
    private var bounds = Rect()

    private var outputWidth = 0
    private var outputHeight = 0
    private var outputRotate = 0
    private var runningMode: RunningMode = RunningMode.IMAGE

    init {
        // 各 Paint の初期設定
        initPaints()
    }

    //Viewをクリアして、Paintを初期状態に戻す
    fun clear() {
        results = null
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    fun setRunningMode(runningMode: RunningMode) {
        this.runningMode = runningMode
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
        boxPaint.color = ContextCompat.getColor(context!!, R.color.mp_primary)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    //検出結果の描画処理
    //カメラフレーム上にViewを重ね、四角形とラベルを表示する
    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        //検出された各物体ごとに処理
        results?.detections()?.map {
            Log.d("OverlayView", "Box Left: ${it.boundingBox().left}")
            //バウンディングボックスの座標
            val boxRect = RectF(
                it.boundingBox().left,
                it.boundingBox().top,
                it.boundingBox().right,
                it.boundingBox().bottom
            )
            //画像の中心を原点に設定する。画像の中心を軸に回転させるため。
            val matrix = Matrix()
            matrix.postTranslate(-outputWidth / 2f, -outputHeight / 2f)

            // デバイスの向きに合わせて座標を回転させます。
            matrix.postRotate(outputRotate.toFloat())

            // 回転後、再び元の座標系（左上が0,0）に戻す。
            // 90度または270度回転した場合、幅（Width）と高さ（Height）が入れ替わるため、
            // 縦、横で画面の中心を変える
            if (outputRotate == 90 || outputRotate == 270) {
                matrix.postTranslate(outputHeight / 2f, outputWidth / 2f)
            } else {
                matrix.postTranslate(outputWidth / 2f, outputHeight / 2f)
            }
            //作成した変換行列を実際の長方形データ（boxRect）に適用する
            matrix.mapRect(boxRect)
            boxRect

        }?.forEachIndexed { index, floats ->

            Log.d("OverlayView", "View Width: $width, Scale: $scaleFactor")
            //座標のスケーリング（拡大・縮小）
            val top = floats.top * scaleFactor
            val bottom = floats.bottom * scaleFactor
            val left = floats.left * scaleFactor
            val right = floats.right * scaleFactor

            // バウンディングボックスの描画
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // 表示テキスト（ラベルとスコア）の準備
            val category = results?.detections()!![index].categories()[0]
            val drawableText =
                category.categoryName() + " " + String.format(
                    "%.2f",
                    category.score()
                )

            // テキスト背景の描画（読みやすくするため）
            textBackgroundPaint.getTextBounds(
                drawableText,
                0,
                drawableText.length,
                bounds
            )
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // テキスト自体の描画
            canvas.drawText(
                drawableText,
                left,
                top + bounds.height(),
                textPaint
            )
        }
        /*
        // 描画が始まる前に、校正用のピクセル幅を画面上部の情報として表示する
        // --- 【追加】校正ピクセル幅の表示 ---
        val FOCAL_LENGTH = DistanceConstants.VIRTUAL_FOCAL_LENGTH_F
        val calibText = "FOCAL_LENGTH: ${FOCAL_LENGTH.toInt()}"

        // 校正値の描画位置（例：Viewの左上隅から少し下げた位置）
        textBackgroundPaint.getTextBounds(calibText, 0, calibText.length, bounds)
        val calibTextX = 20f
        val calibTextY = bounds.height() + 20f

        // 背景矩形を描画（ここではシンプルに黒背景）
        canvas.drawRect(
            calibTextX,
            calibTextY - bounds.height() - Companion.BOUNDING_RECT_TEXT_PADDING,
            calibTextX + bounds.width() + Companion.BOUNDING_RECT_TEXT_PADDING,
            calibTextY + Companion.BOUNDING_RECT_TEXT_PADDING,
            textBackgroundPaint
        )
        // 文字を描画
        canvas.drawText(calibText, calibTextX, calibTextY, textPaint)
        // ------------------------------------

        for (result in results) {
            val boundingBox = result.boundingBox()

            // 検出結果の座標を View のスケールに合わせる
            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            // --- 【追加】距離計算ロジックをここに組み込む ---

            val focalLength = DistanceConstants.VIRTUAL_FOCAL_LENGTH_F
            val realWidth = DistanceConstants.TARGET_REAL_WIDTH_M

            // 検出されたバウンディングボックスのピクセル幅 O_pixel を取得
            val pixelWidth = boundingBox.right - boundingBox.left

            // 距離 D を計算 (メートル単位)
            // D = (O_physical * f) / O_pixel
            val distanceMeters = if (pixelWidth > 0) {
                (realWidth * focalLength) / pixelWidth
            } else {
                0.0f
            }

            // ---【追加】距離とクラス名を外部へ通知 ---
            val category = result.categories()[0]
            val className = category.categoryName()
            distanceAlertListener?.onDistanceUpdated(distanceMeters, className)

            // --- 【ここまで】距離計算ロジック ---

            // バウンディングボックスを描画
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // ラベル文字（カテゴリ名 + 信頼度）
            /*
            val drawableText =
                result.categories[0].label + " " +
                        String.format("%.2f", result.categories[0].score)
            */
            val label = category.categoryName()
            val score = String.format("%.2f", category.score())
            val distanceText = String.format(" (%.2f m)", distanceMeters)
            // リアルタイムのピクセル幅を表示
            val pixelWidthText = String.format(" | W: %d px", pixelWidth.toInt())

            val drawableText = "${label} ${score}${distanceText} ${pixelWidthText}"

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

         */
    }

    //カメラからの検出結果を受け取り、描画用データとしてセットする
    fun setResults(
      detectionResults: ObjectDetectorResult, //検出結果一覧
      outputHeight: Int,                         //カメラ画像の高さ
      outputWidth: Int,                          //カメラ画像の幅
      imageRotation: Int
    ) {
        results = detectionResults
        this.outputWidth = outputWidth
        this.outputHeight = outputHeight
        this.outputRotate = imageRotation

        // 回転による幅と高さの入れ替え
        //0度、180度: 画像はそのままの向き、または上下逆さまなので、幅と高さの関係は変わらない。
        //90度、270度: 画像が横倒しになるため、元の幅が「高さ」に、元の高さが「幅」に入れ替える。
        //これをpairを使用して、回転後の正しい「横幅」と「縦幅」を特定する。
        val rotatedWidthHeight = when (imageRotation) {
            0, 180 -> Pair(outputWidth, outputHeight)
            90, 270 -> Pair(outputHeight, outputWidth)
            else -> return
        }

        // プレビュービューはFILL_STARTモードです。そのため、
        //キャプチャされた画像が表示されるサイズに合わせて、
        //バウンディングボックスを拡大する必要があります。
        //カメラ画像と View のスケール差を補正する
        scaleFactor = when (runningMode) {
            //画像が画面からはみ出さないよう、「幅の倍率」と「高さの倍率」のうち、小さい方（min）を採用します。
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(
                    width * 1f / rotatedWidthHeight.first,
                    height * 1f / rotatedWidthHeight.second
                )
            }

            RunningMode.LIVE_STREAM -> {
                max(
                    width * 1f / rotatedWidthHeight.first,
                    height * 1f / rotatedWidthHeight.second
                )
            }
        }

        //再描画の指示
        invalidate()

    }

    companion object {
        // ラベル背景の余白
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
