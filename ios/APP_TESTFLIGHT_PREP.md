# 艺本艺术 iOS 测试版准备清单

当前仓库已经先准备好了这些基线配置：

- iOS 显示名称：`艺本艺术`
- iOS 版本号：`1.0.0`
- iOS 构建号：`100`
- 默认 Bundle Identifier：`online.shiyiju.yibenart`
- Apple Team ID：`63B63UC362`
- 壳工程位置：`/Users/master/CodeBuddy/art12/ios/Art12DebugApp/Art12DebugApp.xcodeproj`

## Apple 审核通过后要做的事

1. 在 Xcode 打开 `Art12DebugApp.xcodeproj`
2. 在 `Signing & Capabilities` 里选择你的 `Team`
3. 确认 `Bundle Identifier` 没和 App Store Connect 里已有 App 冲突
4. 如果要发 TestFlight，先在 App Store Connect 创建对应 App
5. 用 `Any iOS Device` 做一次 `Archive`
6. 通过 Organizer 上传到 TestFlight

## 当前已知阻塞

- 这个 iOS 工程目前还是一个轻量壳工程，只适合先跑签名、打包、上传链路验证
- 真正的 uni-app 原生容器还没有接入到这个 Xcode 工程里

## uni-app 侧还要补的项

文件：`/Users/master/CodeBuddy/art12/frontend/src/manifest.json`

当前状态：

- `name` 已是 `艺本艺术`
- `versionName` 已是 `1.0.0`
- `versionCode` 已是 `100`
- `appid` 仍为空

说明：

- 这里的 `appid` 不是微信小程序 `appid`
- 它是 uni-app / DCloud App 打包使用的应用标识
- 如果后面要走 uni-app 的正式 App 打包，需要先在 HBuilderX / DCloud 侧创建 App 并拿到对应 `appid`

## 建议的下一步顺序

1. 用 `63B63UC362` 继续补齐签名和描述文件
2. 决定 iOS 走哪条路线：
   - 继续用当前 Swift 壳工程先打一个 TestFlight 验证包
   - 把 uni-app 原生容器正式接入，再发真实业务版
3. 同时补齐 uni-app 的 DCloud `appid`

## 本地验收命令

模拟器构建：

```bash
xcodebuild -project ios/Art12DebugApp/Art12DebugApp.xcodeproj \
  -scheme Art12DebugApp \
  -configuration Debug \
  -sdk iphonesimulator build
```

真机构建：

```bash
xcodebuild -project ios/Art12DebugApp/Art12DebugApp.xcodeproj \
  -scheme Art12DebugApp \
  -configuration Release \
  -sdk iphoneos build
```
