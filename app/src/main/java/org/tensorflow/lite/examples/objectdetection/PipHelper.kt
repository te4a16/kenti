// PipHelper.kt
package org.tensorflow.lite.examples.objectdetection

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Log
import android.util.Rational
import android.view.View
import androidx.annotation.RequiresApi

/**
 * ピクチャー・イン・ピクチャー (PiP) モードへの移行を処理するヘルパークラス。
 * PiPモードは Android 8.0 (API 26) 以降で利用可能です。
 */
object PipHelper {

    private const val TAG = "PipHelper"

    /**
     * PiPモードがこのデバイスでサポートされているかを確認します。
     * @return PiPモードが利用可能であれば true
     */
    fun isPiPSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    /**
     * 指定されたビューのアスペクト比を使用して PiP モードに移行します。
     * このメソッドは Activity のコンテキストで呼び出す必要があります。
     *
     * @param activity PiPモードに移行する対象の Activity
     * @param sourceView PiPウィンドウのコンテンツとして使用されるビュー（通常はカメラプレビューのView）
     */
    fun enterPiPMode(activity: Activity, sourceView: View) {
        if (!isPiPSupported()) {
            Log.w(TAG, "Picture-in-Picture mode is not supported on this device version (API < 26).")
            return
        }

        // Android 8.0 (Oreo) 以降のAPIを使用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            
            // PiPウィンドウのアスペクト比を計算
            val width = sourceView.width
            val height = sourceView.height
            if (width <= 0 || height <= 0) {
                Log.e(TAG, "Source view has zero width or height, cannot calculate aspect ratio.")
                return
            }
            
            val rational = Rational(width, height)

            // PiPモードのパラメータを構築
            val pipParams = PictureInPictureParams.Builder()
                .setAspectRatio(rational)
                // Android 12 (S) 以降でのPiPウィンドウのリサイズを許可する設定
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setSeamlessResizeEnabled(true)
                    }
                }
                .build()
            
            // PiPモードへの移行を実行
            activity.enterPictureInPictureMode(pipParams)
            Log.d(TAG, "Entered Picture-in-Picture mode with ratio $width:$height.")
        }
    }
    
    /**
     * 現在、アクティビティが PiP モードになっているかどうかを確認します。
     * @param activity 確認対象の Activity
     * @return PiPモードであれば true
     */
    fun isInPictureInPictureMode(activity: Activity): Boolean {
        if (!isPiPSupported()) return false
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
             // API 24からisInPictureInPictureModeが利用可能
            activity.isInPictureInPictureMode
        } else {
            false
        }
    }
}