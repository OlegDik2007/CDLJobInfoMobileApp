package info.cdljob.driverhub

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object RoadAlertChecker {
    private const val CHANNEL_ID = "cdl_road_alerts"
    private const val PREFS = "cdl_road_alert_seen"
    private val executor = Executors.newSingleThreadExecutor()

    fun check(
        context: Context,
        latitude: Double,
        longitude: Double,
        heading: Float?
    ) {
        executor.execute {
            try {
                val url = buildString {
                    append("https://cdljob.info/api/mobile-driver-feed?lat=")
                    append(latitude)
                    append("&lon=")
                    append(longitude)
                    append("&radius=50")
                    if (heading != null) {
                        append("&heading=")
                        append(heading)
                    }
                }

                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "CDLDriverHubAndroid/0.3.1")
                }

                val response = try {
                    if (connection.responseCode !in 200..299) return@execute
                    connection.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    connection.disconnect()
                }

                val json = JSONObject(response)
                if (!json.optBoolean("ok")) return@execute

                val feed = json.optJSONArray("live_feed") ?: return@execute
                val candidates = mutableListOf<JSONObject>()

                for (i in 0 until feed.length()) {
                    val item = feed.optJSONObject(i) ?: continue
                    val kind = item.optString("kind")
                    val priority = item.optDouble("priority", 0.0)
                    val relation = item.optString("relation", "nearby")
                    val distance = if (item.isNull("distance_miles")) null else item.optDouble("distance_miles")

                    val alertKind = kind == "event" ||
                        kind == "restriction" ||
                        kind == "weather" ||
                        kind == "dms" ||
                        (kind == "condition" && priority >= 76)

                    if (!alertKind || priority < 68) continue
                    if (relation == "behind" && (distance ?: 999.0) > 5.0) continue

                    candidates.add(item)
                }

                candidates
                    .sortedWith(
                        compareByDescending<JSONObject> { it.optDouble("priority", 0.0) }
                            .thenBy { if (it.isNull("distance_miles")) 9999.0 else it.optDouble("distance_miles") }
                    )
                    .take(2)
                    .forEach { notifyIfNew(context, it) }

                cleanupSeen(context)
            } catch (_: Exception) {
                // Road monitoring must never crash the foreground service.
            }
        }
    }

    private fun notifyIfNew(context: Context, item: JSONObject) {
        val alertKey = item.optString("id").ifBlank {
            listOf(
                item.optString("kind"),
                item.optString("title"),
                item.optString("timestamp")
            ).joinToString(":")
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.contains(alertKey)) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Road alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Crashes, closures, truck restrictions and severe road conditions nearby"
                enableVibration(true)
            }
        )

        val route = item.optString("route")
        val relation = item.optString("relation", "nearby")
        val distance = if (item.isNull("distance_miles")) null else item.optDouble("distance_miles")
        val kind = item.optString("kind")
        val icon = when (kind) {
            "restriction" -> "🚛"
            "weather", "condition" -> "🌨️"
            "dms" -> "🟧"
            else -> "⚠️"
        }

        val distanceText = distance?.let {
            val rounded = if (it < 10) String.format("%.1f", it) else it.toInt().toString()
            rounded + " mi " + if (relation == "ahead") "ahead" else "away"
        } ?: "nearby"

        val title = buildString {
            append(icon)
            append(" ")
            append(distanceText)
            if (route.isNotBlank()) {
                append(" — ")
                append(route)
            }
        }

        val body = item.optString("title")
            .ifBlank { item.optString("description") }
            .ifBlank { "New road alert nearby. Open CDL Live Road for details." }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_tab", "alerts")
            putExtra("alert_key", alertKey)
        }
        val pending = PendingIntent.getActivity(
            context,
            alertKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title.take(120))
            .setContentText(body.take(220))
            .setStyle(Notification.BigTextStyle().bigText(body.take(500)))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .build()

        manager.notify(alertKey.hashCode(), notification)
        prefs.edit().putLong(alertKey, System.currentTimeMillis()).apply()
    }

    private fun cleanupSeen(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cutoff = System.currentTimeMillis() - 48L * 60L * 60L * 1000L
        val editor = prefs.edit()
        var changed = false

        for ((key, value) in prefs.all) {
            val time = value as? Long ?: continue
            if (time < cutoff) {
                editor.remove(key)
                changed = true
            }
        }

        if (changed) editor.apply()
    }
}
