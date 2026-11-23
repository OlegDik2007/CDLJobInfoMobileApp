package info.cdljob.driverhub

import android.app.NotificationChannel
import android.content.Context
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CdlFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val ALERT_CHANNEL_ID = "cdl_road_alerts"
        private const val SEEN_PREFS = "cdl_road_alert_seen"

        // Must exist before Firebase renders a notification while the app is backgrounded.
        fun ensureAlertChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    ALERT_CHANNEL_ID,
                    NativeLanguage.t(context, "alert_channel"),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = NativeLanguage.t(context, "alert_description")
                    enableVibration(true)
                }
            )
        }

        // On-device diagnostic only. This deliberately does not claim to test FCM delivery.
        fun showLocalTestNotification(context: Context) {
            ensureAlertChannel(context)
            val manager = context.getSystemService(NotificationManager::class.java)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_tab", "alerts")
            }
            val pending = PendingIntent.getActivity(
                context, 302, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notice = android.app.Notification.Builder(context, ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("CDL Hub — notification test")
                .setContentText("Local Android test; Firebase delivery is checked separately.")
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            manager.notify(40301, notice)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        DeviceApi.registerPushToken(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: NativeLanguage.t(this, "road_alert")
        val body = message.notification?.body
            ?: message.data["body"]
            ?: NativeLanguage.t(this, "new_road_alert")
        val alertKey = message.data["alert_key"]

        if (!alertKey.isNullOrBlank()) {
            val prefs = getSharedPreferences(SEEN_PREFS, MODE_PRIVATE)
            // Suppress repeat foreground FCM messages and duplicate local fallback alerts.
            // Background notification payloads are rendered by Android itself; server dedup handles those.
            if (prefs.contains(alertKey)) return
            prefs.edit().putLong(alertKey, System.currentTimeMillis()).apply()
        }

        showAlert(title, body, alertKey)
    }

    private fun showAlert(title: String, body: String, alertKey: String?) {
        ensureAlertChannel(this)
        val manager = getSystemService(NotificationManager::class.java)

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
            .setContentTitle(NativeLanguage.labelTitle(this, title))
            .setContentText(body)
            .setStyle(android.app.Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setCategory(android.app.Notification.CATEGORY_NAVIGATION)
            .build()

        manager.notify((alertKey ?: System.currentTimeMillis().toString()).hashCode(), notification)
    }
}
