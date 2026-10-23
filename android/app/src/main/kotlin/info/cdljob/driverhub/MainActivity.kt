package info.cdljob.driverhub

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : Activity() {

    companion object {
        private const val APP_URL = "https://cdljob.info/mobile-app/"
        private const val LOCATION_REQUEST = 101
        private const val NOTIFICATION_REQUEST = 102
        private const val FILE_CHOOSER_REQUEST = 103
        private const val BACKGROUND_LOCATION_REQUEST = 104
        private const val MONITOR_PREF = "cdl_road_monitoring_enabled"
    }

    private lateinit var webView: WebView
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var pendingGeoOrigin: String? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(7, 17, 31)
        window.navigationBarColor = Color.rgb(6, 16, 27)

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(7, 17, 31))
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        setContentView(webView)

        configureWebView()
        initializePush()
        requestNotificationPermissionIfNeeded()
        startRoadTrackingIfAllowed()

        if (savedInstanceState == null) {
            webView.loadUrl(if (shouldOpenAlerts(intent)) "$APP_URL#alerts" else APP_URL)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    private fun shouldOpenAlerts(intent: Intent?): Boolean {
        return intent?.getStringExtra("open_tab") == "alerts" ||
            intent?.hasExtra("alert_key") == true
    }

    private fun initializePush() {
        if (!FirebaseBootstrap.initialize(this)) return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.let { DeviceApi.registerPushToken(this, it) }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_REQUEST
            )
        }
    }

    private fun startRoadTrackingIfAllowed() {
        if (!getSharedPreferences("cdl_settings", MODE_PRIVATE).getBoolean(MONITOR_PREF, false)) return
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return

        val intent = Intent(this, RoadLocationService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun enableRoadMonitoring() {
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                BACKGROUND_LOCATION_REQUEST
            )
            return
        }
        getSharedPreferences("cdl_settings", MODE_PRIVATE).edit().putBoolean(MONITOR_PREF, true).apply()
        DeviceApi.setNotificationsEnabled(this, true)
        startRoadTrackingIfAllowed()
        Toast.makeText(this, NativeLanguage.t(this, "monitor_enabled"), Toast.LENGTH_LONG).show()
    }

    private fun requestRoadMonitoringConsent() {
        AlertDialog.Builder(this)
            .setTitle(NativeLanguage.t(this, "monitor_title"))
            .setMessage(NativeLanguage.t(this, "monitor_consent"))
            .setPositiveButton(NativeLanguage.t(this, "monitor_enable")) { _, _ -> enableRoadMonitoring() }
            .setNegativeButton(NativeLanguage.t(this, "monitor_cancel"), null)
            .show()
    }

    private inner class RoadMonitoringBridge {
        @JavascriptInterface
        fun start() = runOnUiThread { requestRoadMonitoringConsent() }

        @JavascriptInterface
        fun stop() = runOnUiThread {
            getSharedPreferences("cdl_settings", MODE_PRIVATE).edit().putBoolean(MONITOR_PREF, false).apply()
            DeviceApi.setNotificationsEnabled(this@MainActivity, false)
            stopService(Intent(this@MainActivity, RoadLocationService::class.java))
            Toast.makeText(this@MainActivity, NativeLanguage.t(this@MainActivity, "monitor_disabled"), Toast.LENGTH_LONG).show()
        }

        @JavascriptInterface
        fun isEnabled(): Boolean =
            getSharedPreferences("cdl_settings", MODE_PRIVATE).getBoolean(MONITOR_PREF, false)
    }

    private inner class LanguageBridge {
        @JavascriptInterface
        fun setLanguage(language: String) {
            NativeLanguage.set(this@MainActivity, language)
        }
    }

    private fun configureWebView() {
        webView.addJavascriptInterface(LanguageBridge(), "CDLNativeLanguage")
        webView.addJavascriptInterface(RoadMonitoringBridge(), "CDLRoadMonitoring")
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setGeolocationEnabled(true)
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = true
            userAgentString = "$userAgentString CDLDriverHubAndroid/0.8.0 Kotlin"
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }

                val cameraIntents = mutableListOf<Intent>()
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (cameraIntent.resolveActivity(packageManager) != null) {
                    try {
                        val photoDir = File(cacheDir, "chat-photos").apply { mkdirs() }
                        val photoFile = File.createTempFile("chat-", ".jpg", photoDir)
                        cameraImageUri = FileProvider.getUriForFile(
                            this@MainActivity,
                            "$packageName.fileprovider",
                            photoFile
                        )
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                        cameraIntent.addFlags(
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        cameraIntents.add(cameraIntent)
                    } catch (_: Exception) {
                        cameraImageUri = null
                    }
                }

                return try {
                    val chooserIntent = Intent.createChooser(galleryIntent, "Add road photo").apply {
                        if (cameraIntents.isNotEmpty()) {
                            putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toTypedArray())
                        }
                    }
                    startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST)
                    true
                } catch (_: Exception) {
                    this@MainActivity.filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = null
                    cameraImageUri = null
                    false
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                val allowed =
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                if (allowed) {
                    callback?.invoke(origin.orEmpty(), true, false)
                    return
                }

                pendingGeoOrigin = origin
                pendingGeoCallback = callback

                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_REQUEST
                )
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val host = uri.host

                if (host == "cdljob.info" || host?.endsWith(".cdljob.info") == true) {
                    return false
                }

                return openExternal(uri)
            }
        }
    }

    private fun openExternal(uri: Uri): Boolean {
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_REQUEST) {
            return
        }

        if (requestCode == BACKGROUND_LOCATION_REQUEST) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                enableRoadMonitoring()
            }
            return
        }

        if (requestCode != LOCATION_REQUEST) return

        val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }

        pendingGeoCallback?.invoke(
            pendingGeoOrigin.orEmpty(),
            granted,
            false
        )

        if (granted) {
            startRoadTrackingIfAllowed()
        } else {
            Toast.makeText(
                this,
                NativeLanguage.t(this, "notification_location_off"),
                Toast.LENGTH_LONG
            ).show()
        }

        pendingGeoCallback = null
        pendingGeoOrigin = null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (shouldOpenAlerts(intent) && ::webView.isInitialized) {
            webView.loadUrl("$APP_URL#alerts")
        }
    }

    @Deprecated("Deprecated in Android; retained for WebView file chooser compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            val resultUris: Array<Uri>? = if (resultCode != RESULT_OK) {
                null
            } else if (data?.clipData != null) {
                Array(data.clipData!!.itemCount) { index ->
                    data.clipData!!.getItemAt(index).uri
                }
            } else if (data?.data != null) {
                arrayOf(data.data!!)
            } else {
                cameraImageUri?.let { arrayOf(it) }
            }

            filePathCallback?.onReceiveValue(resultUris)
            filePathCallback = null
            cameraImageUri = null
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            webView.destroy()
        }
        super.onDestroy()
    }
}
