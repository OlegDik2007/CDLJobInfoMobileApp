package info.cdljob.driverhub

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CdlFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val ALERT_CHANNEL_ID = "cdl_road_alerts"
        private const val SEEN_PREFS = "cdl_road_alert_seen"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        DeviceApi.registerPushToken(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Road alert nearby"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Open CDL Live Road for details."
        val alertKey = message.data["alert_key"]

        if (!alertKey.isNullOrBlank()) {
            getSharedPreferences(SEEN_PREFS, MODE_PRIVATE)
                .edit()
                .putLong(alertKey, System.currentTimeMillis())
                .apply()
        }

        showAlert(title, body, alertKey)
    }

    private fun showAlert(title: String, body: String, alertKey: String?) {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Road alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Crashes, closures, restrictions and severe road alerts near the driver"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_tab", "alerts")
            putExtra("alert_key", alertKey)
        }
        val pending = PendingIntent.getActivity(
            this,
            301,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = android.app.Notification.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(android.app.Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setCategory(android.app.Notification.CATEGORY_NAVIGATION)
            .build()

        manager.notify((alertKey ?: System.currentTimeMillis().toString()).hashCode(), notification)
    }
}
