# Android 签名信息

> 生成日期：2026-06-30  
> 项目：艺本艺术 (YibenArt)

---

## 签名证书详情

| 参数 | 值 |
|------|-----|
| **Keystore 文件** | `yibenart.keystore` |
| **Keystore 密码** | `YibenArt2026` |
| **别名 (Alias)** | `yibenart` |
| **Key 密码** | `YibenArt2026` |
| **签名算法** | SHA256withRSA (RSA 2048位) |
| **有效期** | 100 年 |
| **过期时间** | 2126-06-30 |
| **证书主体** | CN=艺本艺术, OU=YibenArt, O=Shiyiju, L=Hangzhou, ST=Zhejiang, C=CN |

---

## 应用信息

| 参数 | 值 |
|------|-----|
| **Android 包名** | `com.shiyiju.yibenart` |
| **应用名称** | 艺本艺术 |
| **版本号** | 1.0.0 |
| **最低 SDK** | Android 5.0 (API 21) |
| **目标 SDK** | Android 14 (API 34) |
| **支持的 ABI** | armeabi-v7a, arm64-v8a |

---

## 文件位置

```
/Users/master/CodeBuddy/art12/frontend/yibenart.keystore
```

---

## ⚠️ 重要提醒

1. **Keystore 是应用签名的唯一凭证，一旦丢失将无法更新已上架的应用！**
2. **请将 `yibenart.keystore` 备份到安全位置（U盘、加密云盘等）**
3. **密码请勿泄露给无关人员**
4. **每次版本更新必须使用同一份 Keystore 签名**

---

## 使用 HBuilderX 云端打包

1. 用 HBuilderX 打开 `/Users/master/CodeBuddy/art12/frontend/`
2. `manifest.json` 中已配置好签名信息
3. 菜单：发行 → 原生App-云打包 → 打包
