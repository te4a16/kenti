package org.tensorflow.lite.examples.objectdetection;

/**
 * 距離に応じた警告（音声・バイブ）を管理するクラス
 *
 * 仕様：
 * ・2m以内 → 音声通知
 * ・1m以内 → 音声 + バイブ通知
 * ・指定クラス（例：person）のみ警告対象
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\u0018\u0000 \u00152\u00020\u0001:\u0001\u0015B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u0010J\u0006\u0010\u0011\u001a\u00020\fJ\u0010\u0010\u0012\u001a\u00020\f2\u0006\u0010\u0013\u001a\u00020\u0010H\u0002J\b\u0010\u0014\u001a\u00020\fH\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lorg/tensorflow/lite/examples/objectdetection/DistanceAlertManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "lastAlertTime", "", "tts", "Landroid/speech/tts/TextToSpeech;", "vibrator", "Landroid/os/Vibrator;", "checkAndAlert", "", "distanceMeters", "", "className", "", "shutdown", "speak", "message", "vibrate", "Companion", "app_release"})
public final class DistanceAlertManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.Nullable()
    private android.speech.tts.TextToSpeech tts;
    @org.jetbrains.annotations.NotNull()
    private final android.os.Vibrator vibrator = null;
    private long lastAlertTime = 0L;
    private static final long ALERT_INTERVAL_MS = 3000L;
    private static final float ALERT_DISTANCE_2M = 2.0F;
    private static final float ALERT_DISTANCE_1M = 1.0F;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TARGET_CLASS = "person";
    @org.jetbrains.annotations.NotNull()
    public static final org.tensorflow.lite.examples.objectdetection.DistanceAlertManager.Companion Companion = null;
    
    public DistanceAlertManager(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    /**
     * 距離とクラス名を受け取り、条件に応じて警告を行う
     */
    public final void checkAndAlert(float distanceMeters, @org.jetbrains.annotations.NotNull()
    java.lang.String className) {
    }
    
    private final void speak(java.lang.String message) {
    }
    
    private final void vibrate() {
    }
    
    public final void shutdown() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lorg/tensorflow/lite/examples/objectdetection/DistanceAlertManager$Companion;", "", "()V", "ALERT_DISTANCE_1M", "", "ALERT_DISTANCE_2M", "ALERT_INTERVAL_MS", "", "TARGET_CLASS", "", "app_release"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}