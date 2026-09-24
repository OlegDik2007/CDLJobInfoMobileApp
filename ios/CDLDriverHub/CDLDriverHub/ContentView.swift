import SwiftUI

struct ContentView: View {
    @StateObject private var locationPermission = LocationPermissionManager.shared

    var body: some View {
        ZStack {
            Color(red: 7 / 255, green: 17 / 255, blue: 31 / 255)
                .ignoresSafeArea()

            DriverHubWebView(locationPermission: locationPermission)
                .ignoresSafeArea(.container, edges: [.top, .bottom])
        }
        .onAppear {
            locationPermission.prepareLocationServices()
        }
    }
}
