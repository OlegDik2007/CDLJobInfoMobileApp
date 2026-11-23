#!/usr/bin/env python3
"""Native Android road notification regression contracts; requires no credentials."""
import re
import unittest
from pathlib import Path

APP = Path(__file__).resolve().parents[2] / "android/app"
JAVA = APP / "src/main/kotlin/info/cdljob/driverhub"


def read(name):
    return (JAVA / name).read_text(encoding="utf8")


class NotificationsTest(unittest.TestCase):
    def test_build_release_is_incremental(self):
        gradle = (APP / "build.gradle").read_text()
        self.assertRegex(gradle, r"applicationId\s+'com\.avidetechnologies\.cdlroadhub'")
        self.assertRegex(gradle, r"versionCode\s+13\b")
        self.assertRegex(gradle, r"versionName\s+'1\.0\.1-rc2'")

    def test_manifest_declares_notifications_without_background_location(self):
        manifest = (APP / "src/main/AndroidManifest.xml").read_text()
        self.assertIn('android.permission.POST_NOTIFICATIONS', manifest)
        self.assertIn('android.permission.FOREGROUND_SERVICE_LOCATION', manifest)
        self.assertIn('android:foregroundServiceType="location"', manifest)
        self.assertNotIn('android.permission.ACCESS_BACKGROUND_LOCATION', manifest)
        self.assertIn('com.google.firebase.messaging.default_notification_channel_id', manifest)
        self.assertIn('android:value="cdl_road_alerts"', manifest)

    def test_on_device_diagnostic_is_explicitly_local(self):
        main = read("MainActivity.kt")
        service = read("CdlFirebaseMessagingService.kt")
        self.assertIn('fun testNotification()', main)
        self.assertIn('pendingLocalNotificationTest', main)
        self.assertIn('Settings.ACTION_APP_NOTIFICATION_SETTINGS', main)
        self.assertIn('fun showLocalTestNotification(context: Context)', service)
        self.assertIn('Firebase delivery is checked separately', service)
        self.assertIn('ensureAlertChannel(this)', main)
        self.assertIn('NotificationManager.IMPORTANCE_HIGH', service)

    def test_road_monitoring_remains_opt_in(self):
        main = read("MainActivity.kt")
        self.assertIn('getBoolean(MONITOR_PREF, false)', main)
        self.assertIn('requestRoadMonitoringConsent()', main)
        self.assertIn('setPositiveButton(NativeLanguage.t(this, "monitor_enable"))', main)

    def test_fcm_requires_backend_ack_and_has_fallback(self):
        device = read("DeviceApi.kt")
        service = read("RoadLocationService.kt")
        self.assertIn('response?.optBoolean("registered", false) == true', device)
        self.assertIn('PUSH_REGISTERED_AT', device)
        self.assertIn('backendPushConfigured == false', service)
        self.assertIn('RoadAlertChecker.check(', service)
        self.assertIn('if (!manager.areNotificationsEnabled()) return', read("RoadAlertChecker.kt"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
