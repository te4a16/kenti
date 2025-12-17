package org.tensorflow.lite.examples.objectdetection;

/**
 * ピクチャー・イン・ピクチャー (PiP) モードへの移行を処理するヘルパークラス。
 * PiPモードは Android 8.0 (API 26) 以降で利用可能です。
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nJ\u000e\u0010\u000b\u001a\u00020\f2\u0006\u0010\u0007\u001a\u00020\bJ\u0006\u0010\r\u001a\u00020\fR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000e"}, d2 = {"Lorg/tensorflow/lite/examples/objectdetection/PipHelper;", "", "()V", "TAG", "", "enterPiPMode", "", "activity", "Landroid/app/Activity;", "sourceView", "Landroid/view/View;", "isInPictureInPictureMode", "", "isPiPSupported", "app_release"})
public final class PipHelper {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "PipHelper";
    @org.jetbrains.annotations.NotNull()
    public static final org.tensorflow.lite.examples.objectdetection.PipHelper INSTANCE = null;
    
    private PipHelper() {
        super();
    }
    
    /**
     * PiPモードがこのデバイスでサポートされているかを確認します。
     * @return PiPモードが利用可能であれば true
     */
    public final boolean isPiPSupported() {
        return false;
    }
    
    /**
     * 指定されたビューのアスペクト比を使用して PiP モードに移行します。
     * このメソッドは Activity のコンテキストで呼び出す必要があります。
     *
     * @param activity PiPモードに移行する対象の Activity
     * @param sourceView PiPウィンドウのコンテンツとして使用されるビュー（通常はカメラプレビューのView）
     */
    public final void enterPiPMode(@org.jetbrains.annotations.NotNull()
    android.app.Activity activity, @org.jetbrains.annotations.NotNull()
    android.view.View sourceView) {
    }
    
    /**
     * 現在、アクティビティが PiP モードになっているかどうかを確認します。
     * @param activity 確認対象の Activity
     * @return PiPモードであれば true
     */
    public final boolean isInPictureInPictureMode(@org.jetbrains.annotations.NotNull()
    android.app.Activity activity) {
        return false;
    }
}