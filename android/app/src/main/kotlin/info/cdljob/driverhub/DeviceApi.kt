package info.cdljob.driverhub

import android.content.Context
import android.provider.Settings
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object DeviceApi {
    private const val BASE_URL = "https://cdljob.info"
    private const val PREFS = "cdl_device_api"
    private const val PUSH_REGISTERED = "push_registered"
    private val executor = Executors.newSingleThreadExecutor()

    private fun deviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "android-unknown"
    }

    private fun appVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    fun isPushRegistered(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(PUSH_REGISTERED, false)
    }

    fun registerPushToken(context: Context, token: String) {
        val body = JSONObject()
            .put("device_id", deviceId(context))
            .put("platform", "android")
            .put("push_token", token)
            .put("app_version", appVersion(context))
            .put("enabled", true)

        post("/api/mobile-device-register", body) { ok, _ ->
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PUSH_REGISTERED, ok)
                .apply()
        }
    }

    fun updateLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
        heading: Float?,
        onPushStatus: ((Boolean) -> Unit)? = null
    ) {
        val body = JSONObject()
            .put("device_id", deviceId(context))
            .put("platform", "android")
            .put("latitude", latitude)
            .put("longitude", longitude)
            .put("accuracy_meters", accuracyMeters)
            .put("heading", heading)
            .put("app_version", appVersion(context))
            .put("enabled", true)

        post("/api/mobile-device-location", body) { ok, response ->
            val configured = ok &&
                response?.optJSONObject("push")?.optBoolean("configured", false) == true
            onPushStatus?.invoke(configured)
        }
    }

    private fun post(
        path: String,
        body: JSONObject,
        callback: ((Boolean, JSONObject?) -> Unit)? = null
    ) {
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "CDLDriverHubAndroid/0.3.2")
                }

                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = try {
                    if (text.isBlank()) null else JSONObject(text)
                } catch (_: Exception) {
                    null
                }

                callback?.invoke(code in 200..299, json)
            } catch (_: Exception) {
                callback?.invoke(false, null)
            } finally {
                connection?.disconnect()
            }
        }
    }
}
