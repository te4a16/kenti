package org.tensorflow.lite.examples.objectdetection

object DistanceConstants {
    // 校正で使う値を以下に定義します。
    // -----------------------------------------------------

    // 1. ターゲットとする物体の実際の幅 R_W (メートル)
    //    例: 0.20メートル (20cm)
    //private const val REAL_WIDTH_M = 0.45f

    val LABEL_WIDTH_MAP = mapOf(
        "person" to 0.45f,      // 人
        "car" to 1.8f,         // 普通車
        "bicycle" to 0.6f,     // 自転車
        "motorcycle" to 0.8f,  // バイク
        "bus" to 2.5f,         // バス
        "truck" to 2.2f        // トラック
    )

    // 2. 校正時のカメラと物体の距離 D_cal (メートル)
    //    例: 1.0メートル
    private const val CALIBRATION_DISTANCE_M = 2.0f

    // 3. 校正距離で測定された物体のピクセル幅 P_W_cal (ピクセル)
    //    例: 500ピクセル
    private const val CALIBRATION_PIXEL_WIDTH = 120f
    private const val REFERENCE_WIDTH_PERSON = 0.45f

    // -----------------------------------------------------

    /**
     * 仮想的な焦点距離 f の定数
     * レンズの特性値なので、基準（人）が一つあれば算出できます。
     */
    val VIRTUAL_FOCAL_LENGTH_F: Float =
        (CALIBRATION_DISTANCE_M * CALIBRATION_PIXEL_WIDTH) / REFERENCE_WIDTH_PERSON

    /**
     * デフォルトの幅（Mapにないラベルが来た時の予備）
     */
    val DEFAULT_REAL_WIDTH_M: Float = REFERENCE_WIDTH_PERSON

}