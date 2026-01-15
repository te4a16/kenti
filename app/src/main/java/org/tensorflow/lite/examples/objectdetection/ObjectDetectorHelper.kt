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

//mediapipe Tasks SDK 関連のインポート
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

/**
 * MediaPipe Object Detector を管理するヘルパークラス。
 * ライブストリーム（リアルタイム映像）解析専用,およびモデルの初期化設定を行う。
 */
class ObjectDetectorHelper(
  var threshold: Float = THRESHOLD_DEFAULT,  //検出スコアの閾値
  var maxResults: Int = MAX_RESULTS_DEFAULT,      //返す最大検出数
  var currentDelegate: Int = DELEGATE_CPU, //使用するDelegate（CPU/GPU）
  var currentModel: Int = MODEL_EFFICIENTDETV0,    // 使用するモデルの種類
  var runningMode: RunningMode = RunningMode.LIVE_STREAM,
  val context: Context,
  val objectDetectorListener: DetectorListener? = null
) {

    // この例では、変更時にリセットできるように変数（var）である必要があります。
    //ObjectDetectorが変更されない場合は、遅延定数（lazy val）が望ましいでしょう。
    // ObjectDetector のインスタンス（設定変更時に再作成するため var）
    private var objectDetector: ObjectDetector? = null
    private var imageRotation = 0  //カメラ映像の回転角度
    private lateinit var imageProcessingOptions: ImageProcessingOptions

    init {
        // 初期化時に Detector をセットアップ
        setupObjectDetector()
    }

    fun clearObjectDetector() {
        // リソースを解放し、Detector をクリアして再作成を可能にする
        objectDetector?.close()
        objectDetector = null
    }

    // オブジェクト検出器を、それを使用しているスレッド上の現在の設定で初期化します。
    //CPU および NNAPI デリゲートは、メインスレッドで作成されバックグラウンドスレッド
    //で使用される検出器と併用できますが、GPU デリゲートは検出器を初期化したスレッド上
    //で使用する必要があります。
    // 現在の設定に基づいて ObjectDetector を初期化
    fun setupObjectDetector() {

        val baseOptionsBuilder = BaseOptions.builder()

        // // 使用 delegate の切替（CPU / GPU / NNAPI）デフォルトはCPU
        when (currentDelegate) {
            DELEGATE_CPU -> {
                baseOptionsBuilder.setDelegate(Delegate.CPU)
            }
            DELEGATE_GPU -> {
                baseOptionsBuilder.setDelegate(Delegate.GPU)
            }
        }



        // 使用するモデルを選択
        val modelName =
            when (currentModel) {
                MODEL_EFFICIENTDETV0 -> "efficientdet-lite0.tflite"
                MODEL_EFFICIENTDETV2 -> "efficientdet-lite2.tflite"
                MODEL_MOBILENETV2 -> "tree_detector.tflite"
                else -> "efficientdet-lite0.tflite"
            }
        baseOptionsBuilder.setModelAssetPath(modelName)

        // Check if runningMode is consistent with objectDetectorListener
        when (runningMode) {
            RunningMode.LIVE_STREAM -> {
                if (objectDetectorListener == null) {
                    throw IllegalStateException(
                        "objectDetectorListener must be set when runningMode is LIVE_STREAM."
                    )
                }
            }

            RunningMode.IMAGE, RunningMode.VIDEO -> {
                // no-op
            }
        }



        try {
            // 検出器の基本オプションを作成し、最大結果数とスコア閾値を指定する
            // モデルへの基本設定（閾値・結果数）
            val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
                //BaseOptions を ObjectDetectorOptions に反映
                .setBaseOptions(baseOptionsBuilder.build())
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)
                .setRunningMode(runningMode) //リアルタイム検知のため


            // ⭐ ラベルのホワイトリストを設定 ⭐
            // このリストに含まれないラベルの結果は、検出器から返されません。
            //optionsBuilder.setCategoryAllowlist(ALLOWED_LABELS)

            imageProcessingOptions = ImageProcessingOptions.builder()
                .setRotationDegrees(imageRotation).build()

            when (runningMode) {
                RunningMode.IMAGE, RunningMode.VIDEO -> optionsBuilder.setRunningMode(
                    runningMode
                )

                RunningMode.LIVE_STREAM -> optionsBuilder.setRunningMode(
                    runningMode
                ).setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)
            }

            val options = optionsBuilder.build()

            objectDetector = ObjectDetector.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            // 初期化に失敗した場合
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for details"
            )
            Log.e("Test", "MediaPipe failed to load model with error: " + e.message)
        } catch (e: RuntimeException) {
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for " + "details",
                GPU_ERROR
            )
            Log.e(
                TAG,
                "Object detector failed to load model with error: " + e.message
            )
        }

    }

    // Return running status of recognizer helper
    fun isClosed(): Boolean {
        return objectDetector == null
    }

    //動画ファイルを入力して、ファイル内の検知する（いらない）
    // Accepts the URI for a video file loaded from the user's gallery and attempts to run
    // object detection inference on the video. This process will evaluate every frame in
    // the video and attach the results to a bundle that will be returned.
    fun detectVideoFile(
        videoUri: Uri, inferenceIntervalMs: Long
    ): ResultBundle? {

        if (runningMode != RunningMode.VIDEO) {
            throw IllegalArgumentException(
                "Attempting to call detectVideoFile" + " while not using RunningMode.VIDEO"
            )
        }

        if (objectDetector == null) return null

        // Inference time is the difference between the system time at the start and finish of the
        // process
        val startTime = SystemClock.uptimeMillis()

        var didErrorOccurred = false

        // Load frames from the video and run the object detection model.
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val videoLengthMs =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLong()

        // Note: We need to read width/height from frame instead of getting the width/height
        // of the video directly because MediaRetriever returns frames that are smaller than the
        // actual dimension of the video file.
        val firstFrame = retriever.getFrameAtTime(0)
        val width = firstFrame?.width
        val height = firstFrame?.height

        // If the video is invalid, returns a null detection result
        if ((videoLengthMs == null) || (width == null) || (height == null)) return null

        // Next, we'll get one frame every frameInterval ms, then run detection on these frames.
        val resultList = mutableListOf<ObjectDetectorResult>()
        val numberOfFrameToRead = videoLengthMs.div(inferenceIntervalMs)

        for (i in 0..numberOfFrameToRead) {
            val timestampMs = i * inferenceIntervalMs // ms

            retriever.getFrameAtTime(
                timestampMs * 1000, // convert from ms to micro-s
                MediaMetadataRetriever.OPTION_CLOSEST
            )?.let { frame ->
                // Convert the video frame to ARGB_8888 which is required by the MediaPipe
                val argb8888Frame =
                    if (frame.config == Bitmap.Config.ARGB_8888) frame
                    else frame.copy(Bitmap.Config.ARGB_8888, false)

                // Convert the input Bitmap object to an MPImage object to run inference
                val mpImage = BitmapImageBuilder(argb8888Frame).build()

                // Run object detection using MediaPipe Object Detector API
                objectDetector?.detectForVideo(mpImage, timestampMs)
                    ?.let { detectionResult ->
                        resultList.add(detectionResult)
                    } ?: {
                    didErrorOccurred = true
                    objectDetectorListener?.onError(
                        "ResultBundle could not be returned" + " in detectVideoFile"
                    )
                }
            } ?: run {
                didErrorOccurred = true
                objectDetectorListener?.onError(
                    "Frame at specified time could not be" + " retrieved when detecting in video."
                )
            }
        }

        retriever.release()

        val inferenceTimePerFrameMs =
            (SystemClock.uptimeMillis() - startTime).div(numberOfFrameToRead)

        return if (didErrorOccurred) {
            null
        } else {
            ResultBundle(resultList, inferenceTimePerFrameMs, height, width)
        }
    }
    //上記のメソッドはいらない



    // Bitmap 画像を入力して物体検出を実行
    fun detectLivestreamFrame(imageProxy: ImageProxy) {

        //ランニングモードがライブストリームでないならエラーを吐き出す。
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw IllegalArgumentException(
                "Attempting to call detectLivestreamFrame" + " while not using RunningMode.LIVE_STREAM"
            )
        }

        // Detector が未生成なら再生成
        /*if (objectDetector == null) {
            setupObjectDetector()
        }
         */

        // 推論時間は、プロセスの開始時と終了時のシステム時間の差である
        // 処理時間計測開始
        val frameTime = SystemClock.uptimeMillis()

        // Copy out RGB bits from the frame to a bitmap buffer
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        // If the input image rotation is change, stop all detector
        if (imageProxy.imageInfo.rotationDegrees != imageRotation) {
            imageRotation = imageProxy.imageInfo.rotationDegrees
            clearObjectDetector()
            setupObjectDetector()
            return
        }


        // Bitmap を MediaPipe 専用の画像形式 (MPImage) に変換
        val mpImage = BitmapImageBuilder(bitmapBuffer).build()

        // 検出実行
        detectAsync(mpImage, frameTime)


        // 処理時間計測終了
        //inferenceTime = SystemClock.uptimeMillis() - inferenceTime


    }

    // Run object detection using MediaPipe Object Detector API
    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        // As we're using running mode LIVE_STREAM, the detection result will be returned in
        // returnLivestreamResult function
        //Log.d("MyDetector", "detectAsync called!")
        objectDetector?.detectAsync(mpImage, imageProcessingOptions, frameTime)
    }

    // Return the detection result to this ObjectDetectorHelper's caller
    private fun returnLivestreamResult(
        result: ObjectDetectorResult, input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        // 以下のログを追加
        //Log.d("MyDetector", "Result Callback! Detections: ${result.detections().size}")

        // 中身がある場合はラベル名も出す
        /*
        result.detections().forEach {
            Log.d("MyDetector", "Detected: ${it.categories().firstOrNull()?.categoryName()} score: ${it.categories().firstOrNull()?.score()}")
        }
         */

        objectDetectorListener?.onResults(
            ResultBundle(
                listOf(result),
                inferenceTime,
                input.height,
                input.width,
                imageRotation
            )
        )
    }

    // Return errors thrown during detection to this ObjectDetectorHelper's caller
    private fun returnLivestreamError(error: RuntimeException) {
        objectDetectorListener?.onError(
            error.message ?: "An unknown error has occurred"
        )
    }


    //画像を入力して、入力された画像内を検知する。
    // Accepted a Bitmap and runs object detection inference on it to return results back
    // to the caller
    fun detectImage(image: Bitmap): ResultBundle? {

        if (runningMode != RunningMode.IMAGE) {
            throw IllegalArgumentException(
                "Attempting to call detectImage" + " while not using RunningMode.IMAGE"
            )
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        val startTime = SystemClock.uptimeMillis()

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(image).build()

        // Run object detection using MediaPipe Object Detector API
        objectDetector?.detect(mpImage)?.also { detectionResult ->
            val inferenceTimeMs = SystemClock.uptimeMillis() - startTime
            return ResultBundle(
                listOf(detectionResult),
                inferenceTimeMs,
                image.height,
                image.width
            )
        }

        // If objectDetector?.detect() returns null, this is likely an error. Returning null
        // to indicate this.
        return null
    }



    // Wraps results from inference, the time it takes for inference to be performed, and
    // the input image and height for properly scaling UI to return back to callers
    data class ResultBundle(
        val results: List<ObjectDetectorResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
        val inputImageRotation: Int = 0
    )

    // 結果・エラーを受け取るリスナー
    interface DetectorListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
    }

    companion object {
        // Delegate 種類
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        // 使用モデル種類
        const val MODEL_EFFICIENTDETV0 = 0
        const val MODEL_EFFICIENTDETV2 = 1

        const val MODEL_MOBILENETV2 = 2

        const val MAX_RESULTS_DEFAULT = 3

        const val THRESHOLD_DEFAULT = 0.5F

        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1

        const val TAG = "ObjectDetectorHelper"

        // 検出を許可するラベル（ホワイトリスト）を定義
        private val ALLOWED_LABELS = listOf("person", "bicycle", "car", "motorcycle", "bus",
            "truck", "traffic light", "fire hydrant", "stop sign", "bench", "cat", "dog",
            "horse", "cow", "bear", "umbrella", "suitcase", "sports ball", "bottle","chair",
            "potted plant", "Tree", "tree")

    }
}
