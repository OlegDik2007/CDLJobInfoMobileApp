import CoreLocation
import Foundation
import UIKit

final class LocationPermissionManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    static let shared = LocationPermissionManager()

    private let manager = CLLocationManager()
    private let backgroundTrackingKey = "cdl_background_road_alerts_enabled"
    private var previousLocation: CLLocation?

    @Published private(set) var authorizationStatus: CLAuthorizationStatus

    private override init() {
        authorizationStatus = manager.authorizationStatus
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
    }

    func prepareLocationServices() {
        guard CLLocationManager.locationServicesEnabled() else { return }
        authorizationStatus = manager.authorizationStatus
        resumeBackgroundTrackingIfAuthorized()
    }

    func requestRoadAlertTracking() {
        guard CLLocationManager.locationServicesEnabled() else { return }

        UserDefaults.standard.set(true, forKey: backgroundTrackingKey)

        switch manager.authorizationStatus {
        case .notDetermined:
            manager.requestWhenInUseAuthorization()

        case .authorizedWhenInUse:
            manager.requestLocation()
            manager.requestAlwaysAuthorization()

        case .authorizedAlways:
            startSignificantLocationTracking()
            manager.requestLocation()

        case .denied, .restricted:
            break

        @unknown default:
            break
        }
    }

    func resumeBackgroundTrackingIfAuthorized() {
        guard UserDefaults.standard.bool(forKey: backgroundTrackingKey) else { return }
        guard manager.authorizationStatus == .authorizedAlways else { return }
        startSignificantLocationTracking()
    }

    private func startSignificantLocationTracking() {
        guard CLLocationManager.significantLocationChangeMonitoringAvailable() else { return }
        manager.startMonitoringSignificantLocationChanges()
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus

        guard UserDefaults.standard.bool(forKey: backgroundTrackingKey) else { return }

        switch manager.authorizationStatus {
        case .authorizedWhenInUse:
            manager.requestAlwaysAuthorization()

        case .authorizedAlways:
            startSignificantLocationTracking()
            manager.requestLocation()

        default:
            break
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }

        var heading: Double?
        if location.course >= 0 {
            heading = location.course
        } else if let previousLocation {
            heading = previousLocation.bearing(to: location)
        }

        previousLocation = location

        MobileDeviceApi.updateLocation(
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            accuracyMeters: location.horizontalAccuracy >= 0 ? location.horizontalAccuracy : nil,
            heading: heading
        )
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Location update failed: \(error.localizedDescription)")
    }
}

private extension CLLocation {
    func bearing(to destination: CLLocation) -> Double {
        let lat1 = coordinate.latitude * .pi / 180
        let lon1 = coordinate.longitude * .pi / 180
        let lat2 = destination.coordinate.latitude * .pi / 180
        let lon2 = destination.coordinate.longitude * .pi / 180

        let y = sin(lon2 - lon1) * cos(lat2)
        let x = cos(lat1) * sin(lat2) -
            sin(lat1) * cos(lat2) * cos(lon2 - lon1)

        let degrees = atan2(y, x) * 180 / .pi
        return (degrees + 360).truncatingRemainder(dividingBy: 360)
    }
}

enum MobileDeviceApi {
    private static let baseURL = URL(string: "https://cdljob.info")!
    private static let fallbackDeviceIdKey = "cdl_ios_device_id"

    private static var deviceId: String {
        if let identifier = UIDevice.current.identifierForVendor?.uuidString {
            return "ios-\(identifier)"
        }

        if let stored = UserDefaults.standard.string(forKey: fallbackDeviceIdKey) {
            return stored
        }

        let value = "ios-\(UUID().uuidString)"
        UserDefaults.standard.set(value, forKey: fallbackDeviceIdKey)
        return value
    }

    private static var appVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "unknown"
    }

    private static var pushEnvironment: String {
        #if DEBUG
        return "development"
        #else
        return "production"
        #endif
    }

    static func registerPushToken(_ token: String) {
        post(
            path: "/api/mobile-device-register",
            body: [
                "device_id": deviceId,
                "platform": "ios",
                "push_token": token,
                "push_environment": pushEnvironment,
                "app_version": appVersion,
                "enabled": true
            ]
        )
    }

    static func updateLocation(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double?,
        heading: Double?
    ) {
        var body: [String: Any] = [
            "device_id": deviceId,
            "platform": "ios",
            "latitude": latitude,
            "longitude": longitude,
            "app_version": appVersion,
            "enabled": true
        ]

        body["accuracy_meters"] = accuracyMeters ?? NSNull()
        body["heading"] = heading ?? NSNull()

        post(path: "/api/mobile-device-location", body: body)
    }

    private static func post(path: String, body: [String: Any]) {
        guard let url = URL(string: path, relativeTo: baseURL) else { return }
        guard let payload = try? JSONSerialization.data(withJSONObject: body) else { return }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 12
        request.httpBody = payload
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("CDLDriverHubIOS/0.3", forHTTPHeaderField: "User-Agent")

        URLSession.shared.dataTask(with: request).resume()
    }
}
