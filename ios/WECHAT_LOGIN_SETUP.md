# iOS WeChat Login Setup

当前仓库已经补好了：

- H5 -> iOS `WKWebView` 原生桥接
- 原生 -> H5 的微信登录结果回传
- `AppDelegate` 中的 URL Scheme / Universal Link 回调入口
- `Info.plist` 中微信登录所需的基础键位
- `Art12DebugApp.entitlements` 中的 `Associated Domains`
- H5 静态站点中的 `apple-app-site-association`

要让 APP 内微信登录真正可用，还需要完成下面几步。

## 1. 接入 WeChat OpenSDK

当前代码对 `WechatOpenSDK` 做了 `canImport` 保护。

未接 SDK 时：

- 工程可以正常编译
- 点击微信登录会收到“当前 iOS 工程尚未集成 WechatOpenSDK”

接入后，`WeChatLoginService.swift` 会自动启用真实登录能力。

当前仓库已经直接 vendoring 了：

- `ios/Art12DebugApp/Vendor/WechatOpenSDK-XCFramework.xcframework`

如果你后续想升级 SDK，可以替换这个目录中的 XCFramework。

## 2. 替换 Info.plist 占位值

文件：

- `Art12DebugApp/Info.plist`

当前仓库里已经写入：

- `WeChatAppID = wxe50d10e4fbbfabda`
- `WeChatUniversalLink = https://a.art1.cn/app/`

如果微信开放平台移动应用审核后给你的移动应用 AppID 与这里不同，需要同步修改这两个位置：

- `CFBundleURLSchemes`
- `WeChatAppID`

其中：

- `WeChatAppID` 必须和微信开放平台移动应用的 AppID 一致
- `WeChatUniversalLink` 必须和微信开放平台填写的 Universal Link 完全一致

## 3. 配置 Associated Domains

微信 iOS 登录回调依赖 Universal Link。

你还需要在 iOS Target 里启用：

- `Signing & Capabilities`
- `Associated Domains`

当前仓库已写入：

```text
applinks:a.art1.cn
```

## 4. 配置 apple-app-site-association

服务端域名下需要可访问：

```text
https://a.art1.cn/apple-app-site-association
```

或：

```text
https://a.art1.cn/.well-known/apple-app-site-association
```

当前仓库已生成：

- `frontend/public/apple-app-site-association`
- `frontend/public/.well-known/apple-app-site-association`

内容使用的是当前工程真实值：

- Team ID: `63B63UC362`
- Bundle ID: `online.shiyiju.yibenart`
- App ID: `63B63UC362.online.shiyiju.yibenart`
- Path: `/app/*`

## 5. 微信开放平台配置

需要在微信开放平台移动应用里确认：

- Bundle ID
- AppID
- AppSecret
- Universal Link
- 已开通微信登录能力

这些值必须和 iOS 工程保持一致，否则会出现“拉起微信成功但回不来”或“回调不到 APP”。

后端还需要同步配置：

- `WECHAT_OPEN_APPID`
- `WECHAT_OPEN_SECRET`

否则 APP 端虽然能拿到微信登录 `code`，后端也无法继续换取 `unionid`、头像、昵称等用户资料。

推荐按场景检查后端环境变量：

- 小程序登录：
  - `WECHAT_APPID`
  - `WECHAT_SECRET`
- 微信内 H5 登录：
  - `WECHAT_OFFICIAL_APPID`
  - `WECHAT_OFFICIAL_SECRET`
- iOS / Android 原生微信登录：
  - `WECHAT_OPEN_APPID`
  - `WECHAT_OPEN_SECRET`

如果其中任一场景缺失对应密钥，这个场景下通常只能拿到部分登录态，无法完整同步微信资料。

注意：

- 当前仓库使用的微信开放平台移动应用 AppID 是 `wxe50d10e4fbbfabda`
- 如果后续微信开放平台变更了移动应用 AppID，需要把 `Info.plist` 里的微信 AppID 与 URL Scheme 一并改掉

## 6. 当前代码入口

前端：

- `frontend/src/utils/native.js`
- `frontend/src/pages/login/index.vue`
- `frontend/src/pages/user/pay-account/add.vue`

iOS：

- `Art12DebugApp/ContentView.swift`
- `Art12DebugApp/NativeBridge.swift`
- `Art12DebugApp/WeChatLoginService.swift`
- `Art12DebugApp/AppDelegate.swift`

## 7. 回调链路

1. H5 调用 `window.YibenNative.wechatLogin`
2. `WKWebView` 通过 `messageHandlers.yibenWechatLogin` 进入原生
3. 原生 `WeChatLoginService` 拉起微信
4. 微信通过 URL Scheme / Universal Link 回调 APP
5. 原生把 `code` 通过 `CustomEvent('yiben:wechat-login-result')` 发回 H5
6. 前端拿到 `code` 后继续调用后端 `/user/auth/wx-login`
