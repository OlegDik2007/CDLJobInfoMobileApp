package info.cdljob.driverhub

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : Activity() {

    companion object {
        private const val APP_URL = "https://cdljob.info/mobile-app/"
        private const val LOCATION_REQUEST = 101
        private const val NOTIFICATION_REQUEST = 102
    }

    private lateinit var webView: WebView
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var pendingGeoOrigin: String? = null

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

    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setGeolocationEnabled(true)
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = true
            userAgentString = "$userAgentString CDLDriverHubAndroid/0.3.2 Kotlin"
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webChromeClient = object : WebChromeClient() {
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
                "Location is off. Use the road search in the app.",
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
