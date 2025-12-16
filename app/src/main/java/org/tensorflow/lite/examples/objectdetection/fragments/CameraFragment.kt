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

 /*
 *cameraXを使ってカメラ映像を取得し、TensorFlow Liteの物体検出を行うFragment
 */

package org.tensorflow.lite.examples.objectdetection.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.tensorflow.lite.examples.objectdetection.ObjectDetectorHelper
import org.tensorflow.lite.examples.objectdetection.R
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentCameraBinding
import org.tensorflow.lite.task.vision.detector.Detection
//通知
import android.os.Handler
import android.os.Looper
import org.tensorflow.lite.examples.objectdetection.NotificationHelper
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment(), ObjectDetectorHelper.DetectorListener {

    private val TAG = "ObjectDetection"

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    //物体検出の補助クラス
    private lateinit var objectDetectorHelper: ObjectDetectorHelper

    //カメラ画像保持用
    private lateinit var bitmapBuffer: Bitmap

    //CameraX用の各種ユースケース
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    //カメラ処理を行うバックグラウンドスレッド
    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    // 通知ヘルパー
    private lateinit var notificationHelper: NotificationHelper

    // 通知用のタイマー管理
    private val notificationHandler = Handler(Looper.getMainLooper())
    private var lastNotificationTime = 0L
    private val NOTIFICATION_INTERVAL = 5000L // 5秒間隔 (ミリ秒)

    override fun onResume() {
        super.onResume()
        //パーミッションが剥奪されていないか確認
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        //バックグラウンドスレッド停止
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)

        // NotificationHelper の初期化
        notificationHelper = NotificationHelper(requireContext())

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //物体検出の初期化
        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this)

        //バックグラウンド実行者を初期化する
        //バックグラウンドスレッド開始
        cameraExecutor = Executors.newSingleThreadExecutor()

        //viewFinderがレイアウト完了したらカメラをセットアップ
        fragmentCameraBinding.viewFinder.post {
            //カメラとその使用例を設定する
            setUpCamera()
        }

        //UIコントロールウィジェットにリスナーをアタッチする
        //下部UIのボタンなど設定
        initBottomSheetControls()
    }

    // 下部の設定用 UI（閾値・検出数・スレッド数・モデルなど）のリスナー設定を行う
    private fun initBottomSheetControls() {
        //クリック時、検出スコアの閾値を下限値まで引き下げ
        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (objectDetectorHelper.threshold >= 0.1) {
                objectDetectorHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }

        //クリックすると、検出スコアの閾値下限を引き上げる
        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (objectDetectorHelper.threshold <= 0.8) {
                objectDetectorHelper.threshold += 0.1f
                updateControlsUi()
            }
        }

        //クリック時、検出数減らす
        fragmentCameraBinding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
            if (objectDetectorHelper.maxResults > 1) {
                objectDetectorHelper.maxResults--
                updateControlsUi()
            }
        }

        //クリック時、検出数増やす
        fragmentCameraBinding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
            if (objectDetectorHelper.maxResults < 5) {
                objectDetectorHelper.maxResults++
                updateControlsUi()
            }
        }

        //クリック時、スレッド数減
        // When clicked, decrease the number of threads used for detection
        fragmentCameraBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
            if (objectDetectorHelper.numThreads > 1) {
                objectDetectorHelper.numThreads--
                updateControlsUi()
            }
        }

        //クリック時、スレッド数増
        fragmentCameraBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
            if (objectDetectorHelper.numThreads < 4) {
                objectDetectorHelper.numThreads++
                updateControlsUi()
            }
        }

        //推論デリゲート選択(CPU / GPU / NNAPI)
        //クリックすると、推論に使用される基盤となるハードウェアを変更します。
        //現在の選択肢はCPU、GPU、NNAPIです
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentDelegate = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }

        //モデル選択(各種TFLiteモデル)
        //クリックすると、オブジェクト検出に使用される基盤モデルを変更します
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentModel = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    // UI 表示値更新 + 検出器リセット
    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.text =
            objectDetectorHelper.maxResults.toString()
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", objectDetectorHelper.threshold)
        fragmentCameraBinding.bottomSheetLayout.threadsValue.text =
            objectDetectorHelper.numThreads.toString()

        // 再初期化ではなくクリアする必要があるのは、GPUデリゲートが
        //適用可能な場合に使用するスレッド上で初期化される必要があるためである。
        //検出器再生成
        objectDetectorHelper.clearObjectDetector()
        fragmentCameraBinding.overlay.clear()
    }

    //CameraXを初期化し、カメラユースケースのバインド準備を行う
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // カメラのユースケースを構築し、連携させる
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    //プレビュー / 画像解析ユースケースをバインド
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector - 背面カメラのみを使用しているという前提で動作します
        //背面カメラを選択
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview. 4:3比率のみを使用しています。これは当社のモデルに最も近い比率だからです。
        //プレビュー設定
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()

        // 画像解析。モデル動作に合わせるためRGBA 8888を使用
        //画像解析（RGBA 8888）で物体検出を行う
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                //その後、アナライザーをインスタンスに割り当てることができます
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                    // 初回のみ Bitmap バッファを生成
                        if (!::bitmapBuffer.isInitialized) {
                            //画像回転とRGB画像バッファは、アナライザの実行が
                            //開始された後にのみ初期化される
                            bitmapBuffer = Bitmap.createBitmap(
                              image.width,
                              image.height,
                              Bitmap.Config.ARGB_8888
                            )
                        }

                        detectObjects(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        //再バインドのため一度クリア
        cameraProvider.unbindAll()

        try {
            //ここで渡せるユースケースの数は可変です - 
            //カメラは CameraControl および CameraInfo へのアクセスを提供します
            // プレビューと解析をライフサイクルにバインド
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            // ビューファインダーの表面プロバイダーをプレビューユースケースに接続する
            // プレビュー表示先をセット
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    //画像解析　→　物体検出
    private fun detectObjects(image: ImageProxy) {
        // RGBA バッファを bitmapBuffer にコピー
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        //ビットマップと回転角度をオブジェクト検出ヘルパーに渡して処理と検出を行う
        // TFLite に画像と回転を渡して検出
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 画面回転時にターゲット回転を更新
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    //オブジェクト検出後にUIを更新する。元の画像の高さ/幅を抽出し、
    //OverlayViewを通じてバウンディングボックスを適切にスケーリング・配置する。
    // 物体検出結果を UI に反映
    override fun onResults(
      results: MutableList<Detection>?,
      inferenceTime: Long,
      imageHeight: Int,
      imageWidth: Int
    ) {
        activity?.runOnUiThread {
            //推論時間表示
            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
                            String.format("%d ms", inferenceTime)

            // 必要な情報をOverlayViewに渡してキャンバス上に描画する
            //検出結果を OverlayView に渡す
            fragmentCameraBinding.overlay.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )

            //再描画
            fragmentCameraBinding.overlay.invalidate()

            // -------------------------------------------------------------
            val currentTime = System.currentTimeMillis()
            
            // 最後の通知から 5秒 (5000ms) 以上経過しているかチェック
            if (currentTime - lastNotificationTime >= NOTIFICATION_INTERVAL) {
                
                // 検出されたオブジェクトの数をカウント
                val detectedCount = results?.size ?: 0
                
                val title: String
                val message: String
                
                if (detectedCount > 0) {
                    // 検出されたオブジェクトがある場合、最も信頼度の高いものを表示
                    val topResult = results!!.maxByOrNull { it.categories[0].score }
                    val label = topResult?.categories?.get(0)?.label ?: "Unknown"
                    val score = String.format("%.0f%%", (topResult?.categories?.get(0)?.score ?: 0f) * 100)
                    
                    title = "Object Detected! ($detectedCount item${if (detectedCount > 1) "s" else ""})"
                    message = "$label detected with $score confidence."
                    
                } else {
                    // 検出されたオブジェクトがない場合
                    title = "No Object Detected"
                    message = "Ready for detection. Move camera to target."
                }
                
                // 通知を表示 (ヘッドアップ通知として表示されます)
                notificationHelper.showNotification(title, message)
                
                // 最後の通知時間を更新
                lastNotificationTime = currentTime
            }
            // -------------------------------------------------------------
            
        }
    }

    //エラー時
    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }
}
