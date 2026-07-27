import Foundation
import WebKit
#if canImport(AlipaySDK)
import AlipaySDK
#endif

enum NativeBridgeEvent {
    static let wechatLoginResult = "yiben:wechat-login-result"
    static let alipayPayResult = "yiben:alipay-pay-result"
}

final class WeakScriptMessageHandler: NSObject, WKScriptMessageHandler {
    weak var delegate: WKScriptMessageHandler?

    init(delegate: WKScriptMessageHandler) {
        self.delegate = delegate
    }

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        delegate?.userContentController(userContentController, didReceive: message)
    }
}

enum NativeBridgeScript {
    static let bootstrap = """
    (function() {
      window.__YIBEN_WECHAT_LOGIN__ = true;
      window.YibenNative = window.YibenNative || {};
      window.YibenNative.wechatLogin = function(payload) {
        if (!window.webkit || !window.webkit.messageHandlers || !window.webkit.messageHandlers.yibenWechatLogin) {
          throw new Error('Native bridge unavailable');
        }
        window.webkit.messageHandlers.yibenWechatLogin.postMessage(payload || {});
        return true;
      };
      window.YibenNative.alipayPay = function(payload) {
        if (!window.webkit || !window.webkit.messageHandlers || !window.webkit.messageHandlers.yibenAlipayPay) {
          throw new Error('Native bridge unavailable');
        }
        window.webkit.messageHandlers.yibenAlipayPay.postMessage(payload || {});
        return true;
      };
    })();
    """
}

struct NativeBridgePayload {
    let requestId: String
    let source: String

    init?(body: Any) {
        guard let dict = body as? [String: Any] else { return nil }
        guard let requestId = dict["requestId"] as? String, !requestId.isEmpty else { return nil }
        self.requestId = requestId
        self.source = (dict["source"] as? String) ?? "unknown"
    }
}

struct NativeAlipayPayPayload {
    let requestId: String
    let orderInfo: String

    init?(body: Any) {
        guard let dict = body as? [String: Any] else { return nil }
        guard let requestId = dict["requestId"] as? String, !requestId.isEmpty else { return nil }
        guard let orderInfo = dict["orderInfo"] as? String, !orderInfo.isEmpty else { return nil }
        self.requestId = requestId
        self.orderInfo = orderInfo
    }
}

enum NativeAlipayPayService {
    static func pay(orderInfo: String, completion: @escaping ([String: Any]) -> Void) {
        #if canImport(AlipaySDK)
        AlipaySDK.defaultService().payOrder(orderInfo, fromScheme: "yibenart") { result in
            completion(Self.stringifyKeys(result))
        }
        #else
        completion([
            "resultStatus": "SDK_MISSING",
            "memo": "支付宝 SDK 未集成"
        ])
        #endif
    }

    #if canImport(AlipaySDK)
    private static func stringifyKeys(_ result: [AnyHashable: Any]?) -> [String: Any] {
        var normalized: [String: Any] = [:]
        result?.forEach { key, value in
            normalized[String(describing: key)] = value
        }
        return normalized
    }
    #endif
}

extension WKWebView {
    func dispatchNativeEvent(name: String, detail: [String: Any]) {
        guard let data = try? JSONSerialization.data(withJSONObject: detail),
              let json = String(data: data, encoding: .utf8) else {
            return
        }

        let script = """
        window.dispatchEvent(new CustomEvent(\(name.jsonQuoted), { detail: \(json) }));
        """
        evaluateJavaScript(script, completionHandler: nil)
    }
}

private extension String {
    var jsonQuoted: String {
        guard let data = try? JSONSerialization.data(withJSONObject: [self]),
              let text = String(data: data, encoding: .utf8),
              text.count >= 2 else {
            return "\"\""
        }
        return String(text.dropFirst().dropLast())
    }
}
