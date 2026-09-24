package info.cdljob.driverhub

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class RoadLocationService : Service() {

    companion object {
        private const val SERVICE_CHANNEL_ID = "cdl_road_tracking"
        private const val SERVICE_NOTIFICATION_ID = 4101
    }

    private lateinit var fusedLocation: FusedLocationProviderClient
    private var firebaseConfigured = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val bearing = if (location.hasBearing()) location.bearing else null

            DeviceApi.updateLocation(
                this@RoadLocationService,
                location.latitude,
                location.longitude,
                if (location.hasAccuracy()) location.accuracy else null,
                bearing
            ) { backendPushConfigured ->
                val remotePushReady =
                    firebaseConfigured &&
                    DeviceApi.isPushRegistered(this@RoadLocationService) &&
                    backendPushConfigured

                if (!remotePushReady) {
                    RoadAlertChecker.check(
                        this@RoadLocationService,
                        location.latitude,
                        location.longitude,
                        bearing
                    )
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        firebaseConfigured = FirebaseBootstrap.initialize(this)
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        startForeground(SERVICE_NOTIFICATION_ID, trackingNotification())
        requestLocationUpdates()
    }

    private fun trackingNotification(): android.app.Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Road alert tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps your road-alert location current while CDL Live Road is closed."
            }
        )

        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this,
            4102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return android.app.Notification.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("CDL Live Road alerts active")
            .setContentText("Monitoring your road area for new safety alerts")
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
    }

    private fun requestLocationUpdates() {
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 120_000L)
            .setMinUpdateIntervalMillis(60_000L)
            .setMinUpdateDistanceMeters(805f)
            .build()

        try {
            fusedLocation.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        fusedLocation.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
