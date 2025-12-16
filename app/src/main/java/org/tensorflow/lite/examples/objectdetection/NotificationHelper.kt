package org.tensorflow.lite.examples.objectdetection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.tensorflow.lite.examples.objectdetection.fragments.CameraFragment

/**
 * ヘッドアップ通知を管理するヘルパークラス
 * 永続的な通知ではなく、通常の通知として表示します。
 */
class NotificationHelper(private val context: Context) {

    private val NOTIFICATION_ID = 101 // 通知ID (常に同じIDを使うことで既存の通知を上書きします)
    private val CHANNEL_ID = "object_detection_alerts_channel" // 通知チャンネルID
    private val CHANNEL_NAME = "Object Detection Alerts" // 通知チャンネル名

    init {
        createNotificationChannel()
    }

    // Android O (API 26) 以降で必要な通知チャンネルを作成する
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // ヘッドアップ通知として一時的に表示されるよう、重要度を高く設定
            val importance = NotificationManager.IMPORTANCE_HIGH 
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Shows the latest object detection status as a head-up notification."
                enableVibration(true) // バイブレーションを有効にする
            }
            // チャンネルをシステムに登録
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 指定されたメッセージで通知を瞬時に表示します。
     * @param title 通知のタイトル
     * @param statusMessage 通知に表示するメッセージ
     */
    fun showNotification(title: String, statusMessage: String) {
        // 通知タップ時に開くActivityを設定 (ここではMainActivity)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 通知の構築
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title) // タイトル
            .setContentText(statusMessage) // メッセージ
            .setSmallIcon(R.drawable.ic_notification) // 必要な通知アイコン (R.drawable.ic_notification は準備済みと仮定します)
            .setContentIntent(pendingIntent) // タップ時の処理
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 重要度を高く設定（ヘッドアップ表示のため）
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true) // ユーザーがタップすると通知を自動で消す
            .build()

        // 通知を表示
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}