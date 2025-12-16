package org.tensorflow.lite.examples.objectdetection;

/**
 * ヘッドアップ通知を管理するヘルパークラス
 * 永続的な通知ではなく、通常の通知として表示します。
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\n\u001a\u00020\u000bH\u0002J\u0016\u0010\f\u001a\u00020\u000b2\u0006\u0010\r\u001a\u00020\u00062\u0006\u0010\u000e\u001a\u00020\u0006R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lorg/tensorflow/lite/examples/objectdetection/NotificationHelper;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "CHANNEL_ID", "", "CHANNEL_NAME", "NOTIFICATION_ID", "", "createNotificationChannel", "", "showNotification", "title", "statusMessage", "app_debug"})
public final class NotificationHelper {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    private final int NOTIFICATION_ID = 101;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String CHANNEL_ID = "object_detection_alerts_channel";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String CHANNEL_NAME = "Object Detection Alerts";
    
    public NotificationHelper(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    private final void createNotificationChannel() {
    }
    
    /**
     * 指定されたメッセージで通知を瞬時に表示します。
     * @param title 通知のタイトル
     * @param statusMessage 通知に表示するメッセージ
     */
    public final void showNotification(@org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String statusMessage) {
    }
}