package org.tensorflow.lite.examples.objectdetection

object DistanceConstants {
    // 校正で使う値を以下に定義します。
    // -----------------------------------------------------

    // 1. ターゲットとする物体の実際の幅 R_W (メートル)
    //    例: 0.20メートル (20cm)
    private const val REAL_WIDTH_M = 0.45f

    // 2. 校正時のカメラと物体の距離 D_cal (メートル)
    //    例: 1.0メートル
    private const val CALIBRATION_DISTANCE_M = 2.0f

    // 3. 校正距離で測定された物体のピクセル幅 P_W_cal (ピクセル)
    //    例: 500ピクセル
    private const val CALIBRATION_PIXEL_WIDTH = 120f

    // -----------------------------------------------------

    /**
     * 仮想的な焦点距離 f の定数 (D_cal * P_W_cal) / R_W
     * これを距離計算の定数として使用します。
     */
    val VIRTUAL_FOCAL_LENGTH_F: Float =
        (CALIBRATION_DISTANCE_M * CALIBRATION_PIXEL_WIDTH) / REAL_WIDTH_M

    /**
     * 距離計算で使用するターゲット物体の実際の幅 R_W (メートル)
     */
    val TARGET_REAL_WIDTH_M: Float = REAL_WIDTH_M

    //バウンディングボックスのピクセル幅の取得用
    val PIXEL_WIDTH: Float = CALIBRATION_PIXEL_WIDTH

}