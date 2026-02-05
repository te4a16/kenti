/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

/**
 * TensorFlow Lite Task Library を使用して物体検出を実行するヘルパークラス
 */
class ObjectDetectorHelper(
    var threshold: Float = 0.5f,  // 検出スコアのしきい値
    var numThreads: Int = 2,      // 推論に使用するスレッド数
    var maxResults: Int = 3,      // 取得する最大検出数
    var currentDelegate: Int = 0, // 演算デバイス（CPU/GPU/NNAPI）の選択
    var currentModel: Int = 0,    // 使用する学習済みモデルの選択
    val context: Context,
    val objectDetectorListener: DetectorListener?
) {

    // ObjectDetector インスタンス（設定変更時に再生成するため nullable/var）
    private var objectDetector: ObjectDetector? = null

    init {
        setupObjectDetector()
    }

    /**
     * Detectorを明示的にクリアする
     */
    fun clearObjectDetector() {
        objectDetector = null
    }

    /**
     * 現在の設定（モデル、スレッド数、デバイス）に基づいてObjectDetectorを初期化する
     */
    fun setupObjectDetector() {
        // 1. 検出器の基本オプション設定
        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)
                // ホワイトリスト：特定のラベル（人や車など）のみを検出対象にする
                .setLabelAllowList(ALLOWED_LABELS)

        // 2. 実行環境（スレッド数など）の設定
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        // デバイスの選択（GPU使用時は端末の互換性をチェック）
        when (currentDelegate) {
            DELEGATE_CPU -> { /* デフォルト */ }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
                    objectDetectorListener?.onError("GPU is not supported on this device")
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        // 3. モデルファイルのパス設定
        val modelName =
            when (currentModel) {
                MODEL_MOBILENETV1 -> "mobilenetv1.tflite"
                MODEL_EFFICIENTDETV0 -> "efficientdet-lite0.tflite"
                MODEL_EFFICIENTDETV1 -> "efficientdet-lite1.tflite"
                MODEL_EFFICIENTDETV2 -> "efficientdet-lite2.tflite"
                else -> "mobilenetv1.tflite"
            }

        // 4. ObjectDetectorの生成
        try {
            objectDetector =
                ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for details"
            )
            Log.e("Test", "TFLite failed to load model with error: " + e.message)
        }
    }

    /**
     * Bitmap画像に対して物体検出を実行する
     * @param image 検出対象のBitmap
     * @param imageRotation カメラの回転角度
     */
    fun detect(image: Bitmap, imageRotation: Int) {
        if (objectDetector == null) {
            setupObjectDetector()
        }

        // 推論時間の計測開始
        var inferenceTime = SystemClock.uptimeMillis()

        // 画像の前処理：デバイスの向きに合わせて画像を回転させる
        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .build()

        // BitmapをTensorImage形式に変換
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        // 検出の実行
        val results = objectDetector?.detect(tensorImage)

        // 推論時間の計算
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // 結果をリスナーを通じて通知
        objectDetectorListener?.onResults(
            results,
            inferenceTime,
            tensorImage.height,
            tensorImage.width)
    }

    /**
     * 検出結果やエラーを受け取るためのインターフェース
     */
    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
          results: MutableList<Detection>?,
          inferenceTime: Long,
          imageHeight: Int,
          imageWidth: Int
        )
    }

    companion object {
        // Delegate定数
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        
        // モデル識別定数
        const val MODEL_MOBILENETV1 = 0
        const val MODEL_EFFICIENTDETV0 = 1
        const val MODEL_EFFICIENTDETV1 = 2
        const val MODEL_EFFICIENTDETV2 = 3

        // 検出を許可するラベル（歩行支援に役立つ障害物を中心に定義）
        private val ALLOWED_LABELS = listOf(
            "person", "bicycle", "car", "motorcycle", "bus",
            "truck", "traffic light", "fire hydrant", "stop sign", "bench", "cat", "dog",
            "horse", "cow", "bear", "umbrella", "suitcase", "sports ball", "bottle","chair",
            "potted plant"
        )
    }
}