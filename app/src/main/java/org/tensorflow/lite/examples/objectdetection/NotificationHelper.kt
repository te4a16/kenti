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
 * 障害物検知時のヘッドアップ通知（画面上部へのポップアップ）を管理するクラス
 */
class NotificationHelper(private val context: Context) {

    private val NOTIFICATION_ID = 101 // 通知の識別子（同じIDを使用して通知を更新・上書きする）
    private val CHANNEL_ID = "object_detection_alerts_channel" // 通知チャンネルの一意識別子
    private val CHANNEL_NAME = "Object Detection Alerts" // ユーザーに表示されるチャンネル名

    init {
        createNotificationChannel()
    }

    /**
     * Android 8.0 (API 26) 以上で必要な通知チャンネルを構築・登録する
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // ヘッドアップ表示（ポップアップ）を有効にするため重要度を HIGH に設定
            val importance = NotificationManager.IMPORTANCE_HIGH 
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Shows the latest object detection status as a head-up notification."
                enableVibration(true)
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 通知を作成し、ユーザーに表示する
     * @param title 通知のタイトル（回避指示など）
     * @param statusMessage 通知の本文（距離情報など）
     */
    fun showNotification(title: String, statusMessage: String) {
        // 通知をタップした際にアプリ（MainActivity）を前面に開くためのIntent設定
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 通知オブジェクトの構築
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(statusMessage)
            .setSmallIcon(R.drawable.ic_notification) // ステータスバーに表示されるアイコン
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 重要度高（ヘッドアップ通知用）
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true) // タップ時に通知を自動消去
            .build()

        // システムに通知の発行を依頼
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}