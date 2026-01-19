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

    // 1. 検出結果がない場合は処理しない
    val detections = results?.detections() ?: return

    // 2. 物体ごとにループ（MediaPipeの最新形式に対応）
    detections.forEach { detection ->
        val boundingBox = detection.boundingBox()

        // --- A. 座標変換 (画面回転とスケーリングを考慮) ---
        val boxRect = RectF(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom)
        val matrix = Matrix()
        
        // 中心を軸に回転させる処理
        matrix.postTranslate(-outputWidth / 2f, -outputHeight / 2f)
        matrix.postRotate(outputRotate.toFloat())
        if (outputRotate == 90 || outputRotate == 270) {
            matrix.postTranslate(outputHeight / 2f, outputWidth / 2f)
        } else {
            matrix.postTranslate(outputWidth / 2f, outputHeight / 2f)
        }
        matrix.mapRect(boxRect)

        // Viewのサイズに合わせてスケーリング
        val left = boxRect.left * scaleFactor
        val top = boxRect.top * scaleFactor
        val right = boxRect.right * scaleFactor
        val bottom = boxRect.bottom * scaleFactor

        // --- B. 距離計算ロジック (コメントアウトされていた内容を復活) ---
        val focalLength = DistanceConstants.VIRTUAL_FOCAL_LENGTH_F
        val realWidth = DistanceConstants.TARGET_REAL_WIDTH_M
        
        // 距離計算に使うピクセル幅（回転前の元の値を使用）
        val pixelWidth = boundingBox.width()

        val distanceMeters = if (pixelWidth > 0) {
            (realWidth * focalLength) / pixelWidth
        } else {
            0.0f
        }

        // --- C. ラベル・スコア・距離情報の構築 ---
        val category = detection.categories()[0]
        val label = category.categoryName()
        val score = category.score()

        // 通知リスナーを叩く (CameraFragmentなどで音声を出すため)
        distanceAlertListener?.onDistanceUpdated(distanceMeters, label)

        // 表示用テキスト: 「ラベル スコア (距離m) | ピクセル幅px」
        val drawableText = String.format(
            "%s %.2f (%.2fm) | W: %dpx",
            label,
            score,
            distanceMeters,
            pixelWidth.toInt()
        )

        // --- D. 描画処理 ---
        val drawableRect = RectF(left, top, right, bottom)
        
        // バウンディングボックスの描画
        canvas.drawRect(drawableRect, boxPaint)

        // テキスト背景と文字の描画
        textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
        val textWidth = bounds.width()
        val textHeight = bounds.height()

        canvas.drawRect(
            left,
            top,
            left + textWidth + BOUNDING_RECT_TEXT_PADDING,
            top + textHeight + BOUNDING_RECT_TEXT_PADDING,
            textBackgroundPaint
        )
        canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
    }

    // --- E. 校正用デバッグ情報の表示 (画面左上) ---
    val calibText = "FOCAL_LENGTH: ${DistanceConstants.VIRTUAL_FOCAL_LENGTH_F.toInt()}"
    textBackgroundPaint.getTextBounds(calibText, 0, calibText.length, bounds)
    val calibX = 20f
    val calibY = bounds.height() + 20f

    canvas.drawRect(
        calibX,
        calibY - bounds.height() - BOUNDING_RECT_TEXT_PADDING,
        calibX + bounds.width() + BOUNDING_RECT_TEXT_PADDING,
        calibY + BOUNDING_RECT_TEXT_PADDING,
        textBackgroundPaint
    )
    canvas.drawText(calibText, calibX, calibY, textPaint)
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
