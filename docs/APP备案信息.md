# 艺本艺术 APP 备案信息

更新时间：2026-07-14

## 一、基础信息

| 字段 | 填写内容 |
| --- | --- |
| APP 服务名称 | 艺本艺术 |
| APP 服务内容 | 艺本艺术提供艺术作品展示、艺术家交流、收藏、拍卖及订单管理服务。 |
| APP 运行平台 | 安卓平台、苹果平台 |
| APP 后台域名 | a.art1.cn |
| 产品网站 | https://a.art1.cn/ |
| 备案主体 | 哒纷奇艺术（杭州）有限公司 |

说明：备案页面的“域名”填写 APP 访问后台腾讯云服务器使用的域名，目前为 `a.art1.cn`。产品网站地址也已统一为 `a.art1.cn`。

## 二、安卓平台

| 字段 | 填写内容 |
| --- | --- |
| App 包名 | `com.shiyiju.yibenart` |
| 签名 MD5 值 | `CCE1B9D7126152D91CBDE3B82280DCF5` |

### 安卓平台公钥

腾讯云页面要求填写纯公钥正文，不要填写 `BEGIN/END PUBLIC KEY` 标记，不要换行：

```text
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzUJn3N2xowO+pRA7Zd1CfoJ8bjgwv+Adpfe1GeqLLggH7jdHBhf8eiSZL+pMEZ/Zmy6tpYKTo49v6aGFkM0IBM15ER9p7WP8jmNJHiFYEiq1ujIM62b4SwcKkF/Xs/eAb48Cg/LBnvB+mwuscthINmbT52OxA508dH8XSdB+4LzQbkSU4pH6218uXri9b41+k/Xl5ioI309B++phXo5TM0fmRdYDoJaZGFwSWGvPIxjBX964T51/8+DTmf3PvQ/uP81Nm6Yv19e3c1Ggx8W5D34VTb1Yfr1KewjMz2WOZZV6YVsf8E9K+WwpgbCzZIRoaQ1/TtjaQLXTs3OM8eLAwQIDAQAB
```

## 三、苹果平台

| 字段 | 填写内容 |
| --- | --- |
| Bundle ID | `online.shiyiju.yibenart` |
| 签名 MD5 值（实际为 SHA-1） | `8DC6524ADDA4640AFBF3711A5319F597DFACBC87` |

### 苹果平台公钥

腾讯云页面填写纯公钥正文，不要填写 `BEGIN/END PUBLIC KEY` 标记，不要换行：

```text
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzPOo+cbGCEFPYkc0Ojd4yJNW/dIC5KZ1wmpdwbXq+Va6tGnYH3NVJb/nMYcjcT6AxsnCl/2htpa2p0ZBFecsKpAuQ2sIKbAHlBJja4K0DkjUg4gCMVbiy9WZ/j53j7OjafGZK5y5RdLL95t0mhFqfRgytoyzREyBZJ+iag1EaWgS4jj1EpZw9DkHM7ggs/j5n046WhkXIi67sPF1b86llK70wsfb7+noRkNZ5ANnAgPouRfXF6K4z6R36CFKGu7546CuvSP8hNfOgfDmo6CZIXo863a2E8yohx6jsXa5k/NeCpbnpVTsm+5tTMVng9h5qe/kLod66ZBL/cgg4F9nYwIDAQAB
```

## 四、备案状态

- 安卓包信息：已验证并添加。
- 苹果包信息：已验证并添加。
- APP 服务内容：已补充并保存。
- APP 图标、负责人信息及联系方式：备案页面已有信息。
- 最终“信息预览与提交备案”：尚未点击提交，提交前需复核全部主体、负责人、域名和包信息。

## 五、重要注意事项

1. 安卓签名 MD5 必须与最终上架安卓 APK 使用的签名证书一致。
2. iOS 页面字段名称虽然显示“签名 MD5 值”，实际要求填写 40 位 SHA-1 指纹。
3. 如果以后更换安卓签名证书、iOS Distribution 证书或 Bundle ID，需要同步更新备案包信息。
4. 不要把 keystore、私钥、证书密码或 Apple 密钥写入项目文档或提交到 Git。

参考：腾讯云《APP 备案特征信息填写参考规范》
