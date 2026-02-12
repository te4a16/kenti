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
import android.os.Handler
import android.os.Looper
import org.tensorflow.lite.examples.objectdetection.NotificationHelper
import java.util.concurrent.TimeUnit
import android.graphics.RectF
import org.tensorflow.lite.examples.objectdetection.DistanceAlertManager
import org.tensorflow.lite.examples.objectdetection.OverlayView
import org.tensorflow.lite.examples.objectdetection.AvoidanceNavigationManager
import org.tensorflow.lite.examples.objectdetection.DistanceConstants
import org.tensorflow.lite.examples.objectdetection.StepDetector
import org.tensorflow.lite.examples.objectdetection.PipHelper

/**
 * カメラ映像の取得、物体検出の実行、距離に応じた通知制御を行うメインフラグメント
 */
class CameraFragment : Fragment(), ObjectDetectorHelper.DetectorListener {

    private val TAG = "ObjectDetection"
    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService

    // 各種マネージャー・ヘルパー
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var distanceAlertManager: DistanceAlertManager
    private lateinit var stepDetector: StepDetector
    private val avoidanceManager = AvoidanceNavigationManager()

    // 通知制御用フラグと定数
    private var isNotificationSent = false
    private val ALERT_DISTANCE_M = 8.0f

    private var lastDistance: Float = Float.MAX_VALUE

    override fun onResume() {
        super.onResume()
        // 歩行検知センサーのリスナーを開始
        stepDetector.startListening()

        // 必要な権限があるか確認し、なければ権限リクエスト画面へ遷移
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onPause() {
        super.onPause()
        // アプリがバックグラウンドに回る際、PiPモードへ移行を試行
        if (PipHelper.isPiPSupported() && !isChangingConfigurations()) {
            activity?.let {
                PipHelper.enterPiPMode(it, fragmentCameraBinding.viewFinder)
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

        _fragmentCameraBinding?.let { binding ->
            val params = view?.layoutParams as? ViewGroup.MarginLayoutParams
            
            if (isInPictureInPictureMode) {
                // PiPモード：UIを非表示にし、全画面表示に調整
                params?.topMargin = 0
                binding.bottomSheetLayout.root.visibility = View.GONE
                binding.overlay.visibility = View.GONE
                activity?.findViewById<View>(R.id.toolbar)?.visibility = View.GONE
            } else {
                // 通常モード：UIを再表示し、アクションバー分の余白を復元
                val typedArray = activity?.theme?.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
                val actionBarHeight = typedArray?.getDimensionPixelSize(0, 0) ?: 0
                typedArray?.recycle()

                params?.topMargin = actionBarHeight
                binding.bottomSheetLayout.root.visibility = View.VISIBLE
                binding.overlay.visibility = View.VISIBLE
                activity?.findViewById<View>(R.id.toolbar)?.visibility = View.VISIBLE
            }
            view?.layoutParams = params
            view?.requestLayout()
        }
    }

    private fun isChangingConfigurations() = activity?.isChangingConfigurations ?: false

    override fun onDestroyView() {
        super.onDestroyView()
        // 各種リソースの解放
        distanceAlertManager.shutdown()
        cameraExecutor.shutdown()
        _fragmentCameraBinding = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        notificationHelper = NotificationHelper(requireContext())
        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ヘルパークラスの初期化
        objectDetectorHelper = ObjectDetectorHelper(context = requireContext(), objectDetectorListener = this)
        stepDetector = StepDetector(requireContext())
        distanceAlertManager = DistanceAlertManager(requireContext())

        // カメラ用スレッドの開始
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Viewの描画完了後にカメラをセットアップ
        fragmentCameraBinding.viewFinder.post {
            setUpCamera()
        }

        // 設定パネル（BottomSheet）の初期化
        initBottomSheetControls()
    }

    /**
     * 設定パネルのボタンやスピナーにリスナーを登録
     */
    private fun initBottomSheetControls() {
        // 閾値の増減
        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (objectDetectorHelper.threshold >= 0.1) {
                objectDetectorHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }
        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (objectDetectorHelper.threshold <= 0.8) {
                objectDetectorHelper.threshold += 0.1f
                updateControlsUi()
            }
        }

        // 最大検出数の増減
        fragmentCameraBinding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
            if (objectDetectorHelper.maxResults > 1) {
                objectDetectorHelper.maxResults--
                updateControlsUi()
            }
        }
        fragmentCameraBinding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
            if (objectDetectorHelper.maxResults < 5) {
                objectDetectorHelper.maxResults++
                updateControlsUi()
            }
        }

        // スレッド数の増減
        fragmentCameraBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
            if (objectDetectorHelper.numThreads > 1) {
                objectDetectorHelper.numThreads--
                updateControlsUi()
            }
        }
        fragmentCameraBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
            if (objectDetectorHelper.numThreads < 4) {
                objectDetectorHelper.numThreads++
                updateControlsUi()
            }
        }

        // デリゲート（CPU/GPU等）の切り替え
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentDelegate = p2
                    updateControlsUi()
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

        // モデルの切り替え
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentModel = p2
                    updateControlsUi()
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
    }

    /**
     * 設定変更時にUIテキストを更新し、検出器を再生成する
     */
    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.text = objectDetectorHelper.maxResults.toString()
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text = String.format("%.2f", objectDetectorHelper.threshold)
        fragmentCameraBinding.bottomSheetLayout.threadsValue.text = objectDetectorHelper.numThreads.toString()

        objectDetectorHelper.clearObjectDetector()
        fragmentCameraBinding.overlay.clear()
    }

    /**
     * CameraXの初期化とプロバイダーの取得
     */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * カメラのPreviewとImageAnalysisをライフサイクルにバインド
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // プレビューの設定
        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // 画像解析の設定（物体検出用）
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { image ->
                    if (!::bitmapBuffer.isInitialized) {
                        bitmapBuffer = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                    }
                    detectObjects(image)
                }
            }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /**
     * カメラフレームをBitmapに変換し、検出処理を呼び出す
     */
    private fun detectObjects(image: ImageProxy) {
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        val imageRotation = image.imageInfo.rotationDegrees
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    /**
     * 物体検出完了後のコールバック。距離計算、音声警告、画面通知を行う
     */
    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
            // 描画レイヤーに結果を渡す
            results?.let {
                fragmentCameraBinding.overlay.setResults(it, imageHeight, imageWidth)
            }

            var nearestDistance = Float.MAX_VALUE
            var nearestPersonBox: android.graphics.RectF? = null
            var finalShouldNotify = false
            var finalNotificationTitle = ""
            var finalNotificationMessage = ""

            if (results != null) {
                for (detection in results) {
                    val label = detection.categories[0].label
                    val score = detection.categories[0].score
                    val boundingBox = detection.boundingBox

                    if (DistanceConstants.LABEL_WIDTH_MAP.containsKey(label)) {
                        // 1. 距離の算出
                        val topPositionRatio = boundingBox.top / imageHeight.toFloat()
                        val pixelWidth = boundingBox.width()
                        val realWidth = DistanceConstants.LABEL_WIDTH_MAP[label] ?: 0.45f
                        val currentDistance = (realWidth * DistanceConstants.VIRTUAL_FOCAL_LENGTH_F) / pixelWidth

                        // 2. 音声・バイブ警告の判定
                        distanceAlertManager.checkAndAlert(currentDistance, label, topPositionRatio, stepDetector.isWalking)

                        // 3. 画面下部の物体（足元など）は通知対象から除外
                        if (topPositionRatio > 0.70f) continue 

                        // 4. 通知対象となる最も近い物体を特定
                        if (score >= 0.5f && currentDistance < nearestDistance) {
                            nearestDistance = currentDistance
                            nearestPersonBox = boundingBox
                        }
                    }
                }
            }

            // 5. ヘッドアップ通知（画面通知）の判定
            if (nearestPersonBox != null) {
                // 進路上の判定（必要なら 0.3f..0.7f のコメントアウトを外す）
                val objectCenterX = nearestPersonBox.centerX() / imageWidth
                val currentDistance = nearestDistance // 今回検知した最短距離
                val isApproaching = (lastDistance - currentDistance) > 0.4f //(40cm)
                
                // 8m以内 かつ 歩行中 の判定
                if (nearestDistance <= ALERT_DISTANCE_M) {
                    if (stepDetector.isWalking && isApproaching) {
                        // クールタイム（isNotificationSent）でガード
                        if (!isNotificationSent) {
                            val directionGuide = avoidanceManager.getAvoidanceMessage(nearestPersonBox, imageWidth)
                            finalNotificationTitle = directionGuide
                            finalNotificationMessage = "前 ${String.format("%.2f m", nearestDistance)} に人がいます"
                            finalShouldNotify = true

                            // 通知フラグを立て、0.5秒後にリセット
                            isNotificationSent = true 
                            Handler(Looper.getMainLooper()).postDelayed({
                                isNotificationSent = false
                            }, 500) // ここを 500ms (0.5秒) に変更
                        }
                    } else if(!isApproaching){
                        // 立ち止まったら即リセットして、次に歩き出した瞬間に出るようにする
                        isNotificationSent = false
                    }
                } else {
                    // 8m圏外
                    isNotificationSent = false
                }

                lastDistance = currentDistance

            } else {
                // 誰も検知していない
                isNotificationSent = false
                lastDistance = Float.MAX_VALUE
            }

            // 6. 通知の実行
            if (finalShouldNotify) {
                notificationHelper.showNotification(finalNotificationTitle, finalNotificationMessage)
            }
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }
}