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
import android.graphics.RectF

//音声通知
import org.tensorflow.lite.examples.objectdetection.DistanceAlertManager
import org.tensorflow.lite.examples.objectdetection.OverlayView
import org.tensorflow.lite.examples.objectdetection.AvoidanceNavigationManager

import org.tensorflow.lite.examples.objectdetection.DistanceConstants
import org.tensorflow.lite.examples.objectdetection.StepDetector

//PiP
import org.tensorflow.lite.examples.objectdetection.PipHelper

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

    // 音声通知ヘルパー
    private lateinit var distanceAlertManager: DistanceAlertManager

    // 距離ベースの通知制御用変数
    private var isNotificationSent = false // 通知が送信済みかどうか (4m圏内に入った時)
    private val ALERT_DISTANCE_M = 4.0f // 通知を出す距離の閾値 (4メートル)

    // 画面の縦横比 (OverlayView のスケール計算に利用)
    private var previewWidth = 0
    private var previewHeight = 0
    private var scaleFactor = 1f

    //回避通知
    private val avoidanceManager = AvoidanceNavigationManager()

    //歩行検知
    private lateinit var stepDetector: StepDetector

    override fun onResume() {
        super.onResume()

        // 画面が表示されている間だけ歩行検知を開始
        stepDetector.startListening()

        //パーミッションが剥奪されていないか確認
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onPause() {
        super.onPause()
        
        // PiPモードがサポートされており、かつアクティビティが構成変更中ではない（例：画面回転ではない）場合
        // PiPモードへの移行を試みます。
        if (PipHelper.isPiPSupported() && !isChangingConfigurations()) {
            activity?.let {
                // PiPHelper を使って PiP モードに移行
                // カメラプレビューが表示されている ViewFinder を渡す
                PipHelper.enterPiPMode(it, fragmentCameraBinding.viewFinder)
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode)

    _fragmentCameraBinding?.let { binding ->
        val params = view?.layoutParams as? ViewGroup.MarginLayoutParams
        
        if (isInPictureInPictureMode) {
            // PiP時は余白を完全に消す
            params?.topMargin = 0
            view?.layoutParams = params
            view?.requestLayout()

            binding.bottomSheetLayout.root.visibility = View.GONE
            binding.overlay.visibility = View.GONE
            activity?.findViewById<View>(R.id.toolbar)?.visibility = View.GONE
        } else {
            // 通常時はアクションバー（ヘッダー）の高さ分のマージンを戻す
            val typedArray = activity?.theme?.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
            val actionBarHeight = typedArray?.getDimensionPixelSize(0, 0) ?: 0
            typedArray?.recycle() // ここで確実にメモリ解放

            params?.topMargin = actionBarHeight
            view?.layoutParams = params
            view?.requestLayout()

            binding.bottomSheetLayout.root.visibility = View.VISIBLE
            binding.overlay.visibility = View.VISIBLE
            activity?.findViewById<View>(R.id.toolbar)?.visibility = View.VISIBLE
        }
    }
}

    //アクティビティが構成変更（回転など）中かどうかをチェック
    private fun isChangingConfigurations() = activity?.isChangingConfigurations ?: false

    override fun onDestroyView() {
        super.onDestroyView()

        //音声・バイブ・Handler 解放
        distanceAlertManager.shutdown()

        _fragmentCameraBinding = null
        //super.onDestroyView()

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
        
        //歩行検知の初期化
        stepDetector = StepDetector(requireContext())

        // 音声アラート関係
        distanceAlertManager = DistanceAlertManager(requireContext())

        // OverlayView と DistanceAlertManager を接続
           // onViewCreated 内の該当箇所を修正
            fragmentCameraBinding.overlay.distanceAlertListener =
                object : OverlayView.DistanceAlertListener {
                    override fun onDistanceUpdated(
                    distanceMeters: Float,
                    className: String
                ) {
            // ここでは topRatio が取得できないため、一旦 0.0f を渡すか、
            // もしくはこのリスナー自体を無効化（onResults側で一括処理しているため）
            // distanceAlertManager.checkAndAlert(distanceMeters, className, 0.0f)
                }
            }

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
 // オブジェクト検出後にUIを更新する。元の画像の高さ/幅を抽出し、
    // OverlayViewを通じてバウンディングボックスを適切にスケーリング・配置する。
    // 物体検出結果を UI に反映
    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
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

                    if (label == "person") {
                        // 1. 座標と距離の計算
                        val topPositionRatio = boundingBox.top / imageHeight
                        val pixelWidth = boundingBox.width()
                        val currentDistance = (DistanceConstants.TARGET_REAL_WIDTH_M * DistanceConstants.VIRTUAL_FOCAL_LENGTH_F) / pixelWidth

                        // 2. 音声警告マネージャーに座標も渡す（ここで足なら内部でreturnされる）
                        //第4引数はstepDetector.isWalkingです。今回はtrueにする
                        distanceAlertManager.checkAndAlert(currentDistance, label, topPositionRatio,true)

                        // 3. 画面通知・判定用の足除外（上端が0.7より下なら足とみなして無視）
                        if (topPositionRatio > 0.70f) {
                            continue 
                        }

                        // 4. 有効な「人」の中で一番近いものを更新
                        if (score >= 0.5f && currentDistance < nearestDistance) {
                            nearestDistance = currentDistance
                            nearestPersonBox = boundingBox
                        }
                    }
                }
            }

            // 5. ヘッドアップ通知の判定（一番近い人が4m以内にいる場合）
            if (nearestPersonBox != null && nearestDistance <= ALERT_DISTANCE_M) {
                // --- 追加：歩いている時だけ通知処理へ進む ---
                 //if (stepDetector.isWalking) {
                    if (!isNotificationSent) {
                        val directionGuide = avoidanceManager.getAvoidanceMessage(nearestPersonBox!!, imageWidth)
                        finalNotificationTitle = directionGuide
                        // ここで nearestDistance を使うように修正（エラー箇所）
                        finalNotificationMessage = "前 ${String.format("%.2f m", nearestDistance)} に人がいます"
                        finalShouldNotify = true
                        isNotificationSent = true
                    }
                 //}
                    } else {
                isNotificationSent = false
            }

            if (finalShouldNotify) {
                notificationHelper.showNotification(finalNotificationTitle, finalNotificationMessage)
            }
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    // エラー時
    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }
} // CameraFragment クラス自体の終わり（ここが抜けていた可能性があります）
