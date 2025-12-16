package org.tensorflow.lite.examples.objectdetection;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\n\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0006\u001a\u00020\u0004X\u0086D\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\u00020\u0004X\u0086D\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\bR\u0014\u0010\f\u001a\u00020\u0004X\u0086D\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\b\u00a8\u0006\u000e"}, d2 = {"Lorg/tensorflow/lite/examples/objectdetection/DistanceConstants;", "", "()V", "CALIBRATION_DISTANCE_M", "", "CALIBRATION_PIXEL_WIDTH", "PIXEL_WIDTH", "getPIXEL_WIDTH", "()F", "REAL_WIDTH_M", "TARGET_REAL_WIDTH_M", "getTARGET_REAL_WIDTH_M", "VIRTUAL_FOCAL_LENGTH_F", "getVIRTUAL_FOCAL_LENGTH_F", "app_debug"})
public final class DistanceConstants {
    private static final float REAL_WIDTH_M = 0.45F;
    private static final float CALIBRATION_DISTANCE_M = 2.0F;
    private static final float CALIBRATION_PIXEL_WIDTH = 120.0F;
    
    /**
     * 仮想的な焦点距離 f の定数 (D_cal * P_W_cal) / R_W
     * これを距離計算の定数として使用します。
     */
    private static final float VIRTUAL_FOCAL_LENGTH_F = 533.3334F;
    
    /**
     * 距離計算で使用するターゲット物体の実際の幅 R_W (メートル)
     */
    private static final float TARGET_REAL_WIDTH_M = 0.45F;
    private static final float PIXEL_WIDTH = 120.0F;
    @org.jetbrains.annotations.NotNull()
    public static final org.tensorflow.lite.examples.objectdetection.DistanceConstants INSTANCE = null;
    
    private DistanceConstants() {
        super();
    }
    
    /**
     * 仮想的な焦点距離 f の定数 (D_cal * P_W_cal) / R_W
     * これを距離計算の定数として使用します。
     */
    public final float getVIRTUAL_FOCAL_LENGTH_F() {
        return 0.0F;
    }
    
    /**
     * 距離計算で使用するターゲット物体の実際の幅 R_W (メートル)
     */
    public final float getTARGET_REAL_WIDTH_M() {
        return 0.0F;
    }
    
    public final float getPIXEL_WIDTH() {
        return 0.0F;
    }
}