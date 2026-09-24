import CoreLocation
import SwiftUI
import UIKit
import WebKit

struct DriverHubWebView: UIViewRepresentable {
    private let appURL = URL(string: "https://cdljob.info/mobile-app/")!
    @ObservedObject var locationPermission: LocationPermissionManager

    func makeCoordinator() -> Coordinator {
        Coordinator(locationPermission: locationPermission)
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        configuration.applicationNameForUserAgent = "CDLDriverHubIOS/0.3 SwiftUI"

        let controller = configuration.userContentController
        controller.add(context.coordinator, name: "cdlNative")

        let bridgeScript = WKUserScript(
            source: """
            (() => {
              document.addEventListener('click', (event) => {
                const target = event.target instanceof Element ? event.target.closest('#gpsBtn') : null;
                if (target && window.webkit?.messageHandlers?.cdlNative) {
                  window.webkit.messageHandlers.cdlNative.postMessage({ type: 'gpsRequested' });
                }
              }, true);
            })();
            """,
            injectionTime: .atDocumentEnd,
            forMainFrameOnly: true
        )
        controller.addUserScript(bridgeScript)

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.uiDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = true
        webView.isOpaque = false
        webView.backgroundColor = UIColor(red: 7 / 255, green: 17 / 255, blue: 31 / 255, alpha: 1)
        webView.scrollView.backgroundColor = webView.backgroundColor
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.scrollView.bounces = false

        context.coordinator.webView = webView

        let openAlertsKey = "cdl_open_alerts"
        let shouldOpenAlerts = UserDefaults.standard.bool(forKey: openAlertsKey)
        if shouldOpenAlerts {
            UserDefaults.standard.set(false, forKey: openAlertsKey)
        }

        let initialURL = shouldOpenAlerts
            ? URL(string: "https://cdljob.info/mobile-app/#alerts")!
            : appURL

        webView.load(URLRequest(url: initialURL, cachePolicy: .useProtocolCachePolicy, timeoutInterval: 30))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {}

    static func dismantleUIView(_ webView: WKWebView, coordinator: Coordinator) {
        webView.configuration.userContentController.removeScriptMessageHandler(forName: "cdlNative")
        webView.stopLoading()
        webView.navigationDelegate = nil
        webView.uiDelegate = nil
    }

    final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate, WKScriptMessageHandler {
        weak var webView: WKWebView?
        private let locationPermission: LocationPermissionManager

        init(locationPermission: LocationPermissionManager) {
            self.locationPermission = locationPermission
            super.init()
            NotificationCenter.default.addObserver(
                self,
                selector: #selector(openRoadAlerts),
                name: .cdlOpenRoadAlerts,
                object: nil
            )
        }

        deinit {
            NotificationCenter.default.removeObserver(self)
        }

        @objc private func openRoadAlerts() {
            UserDefaults.standard.set(false, forKey: "cdl_open_alerts")
            webView?.load(URLRequest(url: URL(string: "https://cdljob.info/mobile-app/#alerts")!))
        }

        func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
            guard message.name == "cdlNative",
                  let payload = message.body as? [String: Any],
                  let type = payload["type"] as? String else { return }

            if type == "gpsRequested" {
                locationPermission.requestRoadAlertTracking()
            }
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            guard let url = navigationAction.request.url else {
                decisionHandler(.cancel)
                return
            }

            if isInternalURL(url) {
                decisionHandler(.allow)
                return
            }

            if navigationAction.navigationType == .linkActivated {
                UIApplication.shared.open(url)
                decisionHandler(.cancel)
                return
            }

            decisionHandler(.allow)
        }

        func webView(
            _ webView: WKWebView,
            createWebViewWith configuration: WKWebViewConfiguration,
            for navigationAction: WKNavigationAction,
            windowFeatures: WKWindowFeatures
        ) -> WKWebView? {
            guard navigationAction.targetFrame == nil,
                  let url = navigationAction.request.url else { return nil }

            if isInternalURL(url) {
                webView.load(URLRequest(url: url))
            } else {
                UIApplication.shared.open(url)
            }
            return nil
        }

        private func isInternalURL(_ url: URL) -> Bool {
            guard let host = url.host?.lowercased() else { return true }
            return host == "cdljob.info" || host.hasSuffix(".cdljob.info")
        }
    }
}
