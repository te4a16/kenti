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

//TensorFlow Lite の物体検出を扱うためのヘルパークラス

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

class ObjectDetectorHelper(
  var threshold: Float = 0.5f,  //検出スコアの閾値
  var numThreads: Int = 2,      //使用スレッド数
  var maxResults: Int = 3,      //返す最大検出数
  var currentDelegate: Int = 0, //使用するDelegate（CPU/GPU/NNAPI）
  var currentModel: Int = 0,    // 使用するモデルの種類
  val context: Context,
  val objectDetectorListener: DetectorListener?
) {

    // この例では、変更時にリセットできるように変数（var）である必要があります。
    //ObjectDetectorが変更されない場合は、遅延定数（lazy val）が望ましいでしょう。
    // ObjectDetector のインスタンス（設定変更時に再作成するため var）
    private var objectDetector: ObjectDetector? = null

    init {
        // 初期化時に Detector をセットアップ
        setupObjectDetector()
    }

    fun clearObjectDetector() {
        // Detector をクリアして再作成を可能にする
        objectDetector = null
    }

    // オブジェクト検出器を、それを使用しているスレッド上の現在の設定で初期化します。
    //CPU および NNAPI デリゲートは、メインスレッドで作成されバックグラウンドスレッド
    //で使用される検出器と併用できますが、GPU デリゲートは検出器を初期化したスレッド上
    //で使用する必要があります。
    // 現在の設定に基づいて ObjectDetector を初期化
    fun setupObjectDetector() {
        // 検出器の基本オプションを作成し、最大結果数とスコア閾値を指定する
        // モデルへの基本設定（閾値・結果数）
        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)

        // BaseOptions：スレッド数や delegate 設定を行う
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        // 使用 delegate の切替（CPU / GPU / NNAPI）デフォルトはCPU
        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                // GPU が使える端末かチェック
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

        // BaseOptions を ObjectDetectorOptions に反映
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        // 使用するモデルを選択
        val modelName =
            when (currentModel) {
                MODEL_MOBILENETV1 -> "mobilenetv1.tflite"
                MODEL_EFFICIENTDETV0 -> "efficientdet-lite0.tflite"
                MODEL_EFFICIENTDETV1 -> "efficientdet-lite1.tflite"
                MODEL_EFFICIENTDETV2 -> "efficientdet-lite2.tflite"
                else -> "mobilenetv1.tflite"
            }

        // モデルファイルを読み込み ObjectDetector を初期化
        try {
            objectDetector =
                ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            // 初期化に失敗した場合
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for details"
            )
            Log.e("Test", "TFLite failed to load model with error: " + e.message)
        }
    }

    // Bitmap 画像を入力して物体検出を実行
    fun detect(image: Bitmap, imageRotation: Int) {

        // Detector が未生成なら再生成
        if (objectDetector == null) {
            setupObjectDetector()
        }

        // 推論時間は、プロセスの開始時と終了時のシステム時間の差である
        // 処理時間計測開始
        var inferenceTime = SystemClock.uptimeMillis()

        // 画像用のプリプロセッサを作成する。
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        // 入力画像の前処理（回転補正）
        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90)) // 画面の回転を補正
                .build()

        // 画像を前処理し、検出用にTensorImageに変換する。
        // Bitmap → TensorImage に変換
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        // 検出実行
        val results = objectDetector?.detect(tensorImage)

        // 処理時間計測終了
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // 結果をコールバックで返す
        objectDetectorListener?.onResults(
            results,
            inferenceTime,
            tensorImage.height,
            tensorImage.width)
    }

    // 結果・エラーを受け取るリスナー
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
        // Delegate 種類
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        // 使用モデル種類
        const val MODEL_MOBILENETV1 = 0
        const val MODEL_EFFICIENTDETV0 = 1
        const val MODEL_EFFICIENTDETV1 = 2
        const val MODEL_EFFICIENTDETV2 = 3
    }
}
