import Foundation
import UIKit

#if canImport(WechatOpenSDK)
import WechatOpenSDK
#endif

struct WeChatLoginResult {
    let requestId: String
    let ok: Bool
    let code: String?
    let message: String?
}

final class WeChatLoginService: NSObject {
    static let shared = WeChatLoginService()

    private var pendingRequestId: String?
    private var completion: ((WeChatLoginResult) -> Void)?
    private var timeoutWorkItem: DispatchWorkItem?
    private var isRegistered = false
    private var pendingState: String?

    private enum LoginStage: String {
        case idle
        case registering
        case registered
        case sending
        case waitingForCallback
        case callbackReceived
        case failed
    }

    private override init() {
        super.init()
    }

    func registerIfPossible() {
        #if canImport(WechatOpenSDK)
        DispatchQueue.main.async {
            self.registerOnMainThreadIfNeeded()
        }
        #endif
    }

    func startLogin(requestId: String, completion: @escaping (WeChatLoginResult) -> Void) {
        #if canImport(WechatOpenSDK)
        guard Thread.isMainThread else {
            DispatchQueue.main.async {
                self.startLogin(requestId: requestId, completion: completion)
            }
            return
        }

        if let pendingRequestId {
            completion(.init(requestId: requestId, ok: false, code: nil, message: "微信登录正在进行，请稍候"))
            if pendingRequestId == requestId {
                cancelPendingRequest(message: "微信登录请求重复")
            }
            return
        }

        guard let appId = appId, !appId.isEmpty else {
            completion(.init(requestId: requestId, ok: false, code: nil, message: "未配置 WeChatAppID"))
            return
        }
        guard let universalLink = universalLink, !universalLink.isEmpty else {
            completion(.init(requestId: requestId, ok: false, code: nil, message: "未配置 WeChatUniversalLink"))
            return
        }

        _ = appId
        _ = universalLink

        guard registerOnMainThreadIfNeeded() else {
            completion(.init(requestId: requestId, ok: false, code: nil, message: "微信 SDK 注册失败，请检查 Universal Link 配置"))
            return
        }

        guard UIApplication.shared.canOpenURL(URL(string: "weixin://")!) else {
            completion(.init(requestId: requestId, ok: false, code: nil, message: "未检测到微信客户端"))
            return
        }

        guard let viewController = Self.topViewController() else {
            completion(.init(requestId: requestId, ok: false, code: nil, message: "当前页面尚未准备好，请稍后重试"))
            return
        }

        let state = "art12_\(UUID().uuidString.replacingOccurrences(of: "-", with: ""))"
        let req = SendAuthReq()
        req.scope = "snsapi_userinfo"
        req.state = state

        self.pendingRequestId = requestId
        self.pendingState = state
        self.completion = completion
        scheduleTimeout(for: requestId)
        record(stage: .sending)

        WXApi.sendAuthReq(req, viewController: viewController, delegate: self) { [weak self] success in
            guard let self else { return }
            DispatchQueue.main.async {
                guard self.pendingRequestId == requestId else { return }
                if !success {
                    self.record(stage: .failed)
                    self.finishPendingRequest(.init(requestId: requestId, ok: false, code: nil, message: "微信客户端拉起失败，请稍后重试"))
                } else {
                    self.record(stage: .waitingForCallback)
                }
            }
        }
        #else
        completion(.init(requestId: requestId, ok: false, code: nil, message: "当前安装到手机的可能还是旧包，未加载 WechatOpenSDK，请先卸载 App 后重新从 Xcode 安装"))
        #endif
    }

    func handleOpenURL(_ url: URL) -> Bool {
        #if canImport(WechatOpenSDK)
        return WXApi.handleOpen(url, delegate: self)
        #else
        return false
        #endif
    }

    func handleUniversalLink(_ userActivity: NSUserActivity) -> Bool {
        #if canImport(WechatOpenSDK)
        return WXApi.handleOpenUniversalLink(userActivity, delegate: self)
        #else
        return false
        #endif
    }

    private var appId: String? {
        Bundle.main.object(forInfoDictionaryKey: "WeChatAppID") as? String
    }

    private var universalLink: String? {
        Bundle.main.object(forInfoDictionaryKey: "WeChatUniversalLink") as? String
    }

    #if canImport(WechatOpenSDK)
    @discardableResult
    private func registerOnMainThreadIfNeeded() -> Bool {
        guard Thread.isMainThread else { return false }
        if isRegistered { return true }
        guard let appId = appId, !appId.isEmpty,
              let universalLink = universalLink, !universalLink.isEmpty else {
            return false
        }
        record(stage: .registering)
        isRegistered = WXApi.registerApp(appId, universalLink: universalLink)
        record(stage: isRegistered ? .registered : .failed)
        return isRegistered
    }

    private func scheduleTimeout(for requestId: String) {
        timeoutWorkItem?.cancel()
        let workItem = DispatchWorkItem { [weak self] in
            guard let self, self.pendingRequestId == requestId else { return }
            self.finishPendingRequest(.init(requestId: requestId, ok: false, code: nil, message: "微信登录超时，请重试"))
        }
        timeoutWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + 60, execute: workItem)
    }

    private func finishPendingRequest(_ result: WeChatLoginResult) {
        timeoutWorkItem?.cancel()
        timeoutWorkItem = nil
        let callback = completion
        pendingRequestId = nil
        pendingState = nil
        completion = nil
        record(stage: result.ok ? .idle : .failed)
        callback?(result)
    }

    private func cancelPendingRequest(message: String) {
        guard let requestId = pendingRequestId else { return }
        finishPendingRequest(.init(requestId: requestId, ok: false, code: nil, message: message))
    }

    private func record(stage: LoginStage) {
        UserDefaults.standard.set(stage.rawValue, forKey: "WeChatLoginLastStage")
        UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: "WeChatLoginLastStageTime")
        UserDefaults.standard.set(WXApi.getVersion(), forKey: "WeChatOpenSDKVersion")
    }

    private static func topViewController(
        from root: UIViewController? = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .filter { $0.activationState == .foregroundActive }
            .flatMap(\.windows)
            .first(where: { $0.isKeyWindow })?
            .rootViewController
    ) -> UIViewController? {
        if let presented = root?.presentedViewController {
            return topViewController(from: presented)
        }
        if let navigation = root as? UINavigationController {
            return topViewController(from: navigation.visibleViewController)
        }
        if let tabs = root as? UITabBarController {
            return topViewController(from: tabs.selectedViewController)
        }
        return root
    }
    #endif
}

#if canImport(WechatOpenSDK)
extension WeChatLoginService: WXApiDelegate {
    func onReq(_ req: BaseReq) {}

    func onResp(_ resp: BaseResp) {
        guard let requestId = pendingRequestId else { return }
        record(stage: .callbackReceived)

        guard let authResp = resp as? SendAuthResp else {
            finishPendingRequest(.init(requestId: requestId, ok: false, code: nil, message: "收到未知微信回调"))
            return
        }

        if let pendingState, authResp.state != pendingState {
            finishPendingRequest(.init(requestId: requestId, ok: false, code: nil, message: "微信登录回调校验失败，请重试"))
            return
        }

        if authResp.errCode == 0, let code = authResp.code, !code.isEmpty {
            finishPendingRequest(.init(requestId: requestId, ok: true, code: code, message: nil))
            return
        }

        let message = authResp.errStr.isEmpty ? "微信登录已取消或失败" : authResp.errStr
        finishPendingRequest(.init(requestId: requestId, ok: false, code: nil, message: message))
    }

    func onNeedGrantReadPasteBoardPermission(with openURL: URL, completion: @escaping WXGrantReadPasteBoardPermissionCompletion) {
        _ = completion()
    }
}
#endif
