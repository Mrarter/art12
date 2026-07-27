import UIKit
#if canImport(AlipaySDK)
import AlipaySDK
#endif

enum AppRuntimeNotification {
    static let openURL = Notification.Name("AppRuntimeOpenURL")
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        WeChatLoginService.shared.registerIfPossible()
        return true
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        #if canImport(AlipaySDK)
        if url.host == "safepay" {
            AlipaySDK.defaultService().processOrder(withPaymentResult: url, standbyCallback: nil)
            return true
        }
        #endif
        if WeChatLoginService.shared.handleOpenURL(url) {
            return true
        }
        NotificationCenter.default.post(name: AppRuntimeNotification.openURL, object: url)
        return true
    }

    func application(
        _ application: UIApplication,
        continue userActivity: NSUserActivity,
        restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void
    ) -> Bool {
        if WeChatLoginService.shared.handleUniversalLink(userActivity) {
            return true
        }
        if let url = userActivity.webpageURL {
            NotificationCenter.default.post(name: AppRuntimeNotification.openURL, object: url)
            return true
        }
        return false
    }
}
