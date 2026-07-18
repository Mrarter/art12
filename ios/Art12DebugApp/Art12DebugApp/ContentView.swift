import SwiftUI
import UIKit
import WebKit

struct ContentView: View {
    @State private var webURL = AppWebURL.home
    @State private var isLoading = true
    @State private var loadError: String?

    var body: some View {
        ZStack {
            YibenWebView(
                url: webURL,
                isLoading: $isLoading,
                loadError: $loadError,
                onAppURL: handleAppURL
            )
            .ignoresSafeArea()

            if isLoading {
                ProgressView("正在加载艺本艺术...")
                    .padding(20)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
            }

            if let loadError {
                VStack(spacing: 14) {
                    Text("页面加载失败")
                        .font(.headline)
                    Text(loadError)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                    Button("重新加载") {
                        self.loadError = nil
                        self.isLoading = true
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding(20)
                .frame(maxWidth: 320)
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18))
                .padding()
            }
        }
        .onOpenURL { url in
            handleAppURL(url)
        }
        .onReceive(NotificationCenter.default.publisher(for: AppRuntimeNotification.openURL)) { notification in
            guard let url = notification.object as? URL else { return }
            handleAppURL(url)
        }
    }

    private func handleAppURL(_ url: URL) {
        guard let targetURL = AppDeepLink.webURL(from: url) else { return }
        loadError = nil
        isLoading = true
        webURL = targetURL
    }
}

struct YibenWebView: UIViewRepresentable {
    let url: URL
    @Binding var isLoading: Bool
    @Binding var loadError: String?
    let onAppURL: (URL) -> Void

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = true
        configuration.userContentController.add(
            WeakScriptMessageHandler(delegate: context.coordinator),
            name: "yibenWechatLogin"
        )
        configuration.userContentController.add(
            WeakScriptMessageHandler(delegate: context.coordinator),
            name: "yibenAlipayPay"
        )
        configuration.userContentController.addUserScript(
            WKUserScript(
                source: NativeBridgeScript.bootstrap,
                injectionTime: .atDocumentStart,
                forMainFrameOnly: true
            )
        )

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.uiDelegate = context.coordinator
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.allowsBackForwardNavigationGestures = true
        webView.allowsLinkPreview = false
        webView.customUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 26_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Mobile/15E148 Safari/604.1 YibenArt/1.0 YibenArtIOSApp"
        if #available(iOS 16.4, *) {
            webView.isInspectable = true
        }
        webView.load(AppWebURL.request(for: url))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        if webView.url != url {
            webView.load(AppWebURL.request(for: url))
            return
        }
        if loadError == nil, webView.url == nil {
            webView.load(AppWebURL.request(for: url))
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(isLoading: $isLoading, loadError: $loadError, onAppURL: onAppURL)
    }

    final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate, WKScriptMessageHandler {
        @Binding private var isLoading: Bool
        @Binding private var loadError: String?
        private let onAppURL: (URL) -> Void
        weak var webView: WKWebView?

        init(isLoading: Binding<Bool>, loadError: Binding<String?>, onAppURL: @escaping (URL) -> Void) {
            _isLoading = isLoading
            _loadError = loadError
            self.onAppURL = onAppURL
        }

        func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
            if message.name == "yibenAlipayPay" {
                handleAlipayPay(message.body)
                return
            }

            guard message.name == "yibenWechatLogin" else { return }
            guard let payload = NativeBridgePayload(body: message.body) else {
                dispatchWechatLoginResult(.init(requestId: "", ok: false, code: nil, message: "原生参数无效"))
                return
            }

            WeChatLoginService.shared.startLogin(requestId: payload.requestId) { [weak self] result in
                DispatchQueue.main.async {
                    self?.dispatchWechatLoginResult(result)
                }
            }
        }

        private func handleAlipayPay(_ body: Any) {
            guard let payload = NativeAlipayPayPayload(body: body) else {
                dispatchAlipayPayResult(requestId: "", ok: false, result: [:], message: "支付宝支付参数无效")
                return
            }

            NativeAlipayPayService.pay(orderInfo: payload.orderInfo) { [weak self] result in
                DispatchQueue.main.async {
                    let status = String(describing: result["resultStatus"] ?? "")
                    let ok = status == "9000"
                    let memo = String(describing: result["memo"] ?? "")
                    self?.dispatchAlipayPayResult(
                        requestId: payload.requestId,
                        ok: ok,
                        result: result,
                        message: ok ? "" : (memo.isEmpty ? "支付宝支付未完成" : memo)
                    )
                }
            }
        }

        private func dispatchAlipayPayResult(requestId: String, ok: Bool, result: [String: Any], message: String) {
            let detail: [String: Any] = [
                "requestId": requestId,
                "ok": ok,
                "result": result,
                "message": message
            ]
            webView?.dispatchNativeEvent(name: NativeBridgeEvent.alipayPayResult, detail: detail)
        }

        private func dispatchWechatLoginResult(_ result: WeChatLoginResult) {
            let detail: [String: Any] = [
                "requestId": result.requestId,
                "ok": result.ok,
                "code": result.code ?? "",
                "message": result.message ?? ""
            ]
            webView?.dispatchNativeEvent(name: NativeBridgeEvent.wechatLoginResult, detail: detail)
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            if let url = navigationAction.request.url {
                if AppDeepLink.webURL(from: url) != nil {
                    onAppURL(url)
                    decisionHandler(.cancel)
                    return
                }
                if openAlipayIfNeeded(url) {
                    decisionHandler(.cancel)
                    return
                }
            }
            decisionHandler(.allow)
        }

        private func openAlipayIfNeeded(_ url: URL) -> Bool {
            let scheme = url.scheme?.lowercased() ?? ""
            if scheme == "alipays" || scheme == "alipay" {
                UIApplication.shared.open(url)
                return true
            }

            guard url.host?.contains("render.alipay.com") == true,
                  let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                  let schemeValue = components.queryItems?.first(where: { $0.name == "scheme" })?.value,
                  let decoded = schemeValue.removingPercentEncoding,
                  let alipayURL = URL(string: decoded) else {
                return false
            }

            UIApplication.shared.open(alipayURL)
            return true
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            self.webView = webView
            isLoading = true
            loadError = nil
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            isLoading = false
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            isLoading = false
            loadError = error.localizedDescription
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            isLoading = false
            loadError = error.localizedDescription
        }

        func webView(
            _ webView: WKWebView,
            createWebViewWith configuration: WKWebViewConfiguration,
            for navigationAction: WKNavigationAction,
            windowFeatures: WKWindowFeatures
        ) -> WKWebView? {
            if navigationAction.targetFrame == nil {
                webView.load(navigationAction.request)
            }
            return nil
        }
    }
}

enum AppDeepLink {
    static func webURL(from url: URL) -> URL? {
        if let universalLink = paymentReturnURL(from: url) {
            return universalLink
        }
        guard url.scheme?.lowercased() == "yibenart" else { return nil }
        guard url.host?.lowercased() == "pay-result" else { return nil }
        guard var sourceComponents = URLComponents(url: url, resolvingAgainstBaseURL: false) else { return nil }
        sourceComponents.scheme = nil
        sourceComponents.host = nil
        sourceComponents.path = ""

        var targetComponents = URLComponents()
        targetComponents.scheme = "https"
        targetComponents.host = "a.art1.cn"
        targetComponents.path = "/"
        targetComponents.query = AppWebURL.versionQuery
        targetComponents.fragment = "/pages/order/pay"

        let query = sourceComponents.percentEncodedQuery ?? ""
        if !query.isEmpty {
            targetComponents.fragment = "/pages/order/pay?\(query)"
        }

        return targetComponents.url
    }

    private static func paymentReturnURL(from url: URL) -> URL? {
        guard url.scheme?.lowercased() == "https" else { return nil }
        guard url.host?.lowercased() == "a.art1.cn" else { return nil }
        guard url.path.hasPrefix("/app/pay-result") else { return nil }

        var targetComponents = URLComponents()
        targetComponents.scheme = "https"
        targetComponents.host = "a.art1.cn"
        targetComponents.path = "/"
        targetComponents.query = AppWebURL.versionQuery
        targetComponents.fragment = "/pages/order/pay"

        if let sourceComponents = URLComponents(url: url, resolvingAgainstBaseURL: false),
           let query = sourceComponents.percentEncodedQuery,
           !query.isEmpty {
            targetComponents.fragment = "/pages/order/pay?\(query)"
        }

        return targetComponents.url
    }
}

enum AppWebURL {
    private static var buildNumber: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "unknown"
    }

    static var versionQuery: String {
        "v=ios\(buildNumber)"
    }

    static var home: URL {
        var components = URLComponents()
        components.scheme = "https"
        components.host = "a.art1.cn"
        components.path = "/"
        components.query = versionQuery
        components.fragment = "/"
        return components.url!
    }

    static func request(for url: URL) -> URLRequest {
        var request = URLRequest(
            url: url,
            cachePolicy: .reloadIgnoringLocalCacheData,
            timeoutInterval: 30
        )
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        request.setValue("no-cache", forHTTPHeaderField: "Pragma")
        return request
    }
}
