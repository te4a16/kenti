package org.tensorflow.lite.examples.objectdetection

/**
 * 距離計算に使用する定数および校正値を管理するオブジェクト
 */
object DistanceConstants {
    // 1. 各対象物の実際の幅（メートル）の定義
    val LABEL_WIDTH_MAP = mapOf(
        "person" to 0.45f,      // 人
        "car" to 1.8f,         // 普通車
        "bicycle" to 0.6f,     // 自転車
        "motorcycle" to 0.8f,  // バイク
        "bus" to 2.5f,         // バス
        "truck" to 2.2f        // トラック
    )

    // 2. 校正（キャリブレーション）用データ
    // 基準となる物体（人）を特定の距離で撮影した際の値を定義
    private const val CALIBRATION_DISTANCE_M = 2.0f    // 校正時の距離（2メートル）
    private const val CALIBRATION_PIXEL_WIDTH = 120f   // その距離での物体のピクセル幅
    private const val REFERENCE_WIDTH_PERSON = 0.45f   // 基準とした物体の実際の幅

    /**
     * 単眼カメラの距離計算に用いる仮想的な焦点距離 (f)
     * 計算式: f = (距離 × ピクセル幅) / 実際の幅
     */
    val VIRTUAL_FOCAL_LENGTH_F: Float =
        (CALIBRATION_DISTANCE_M * CALIBRATION_PIXEL_WIDTH) / REFERENCE_WIDTH_PERSON

    /**
     * 定義されていないラベルが検出された場合のデフォルト幅
     */
    val DEFAULT_REAL_WIDTH_M: Float = REFERENCE_WIDTH_PERSON
}