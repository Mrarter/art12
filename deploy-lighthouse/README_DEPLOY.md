# 艺本艺术上线说明

这套部署目录已经包含：

- 后端预构建 JAR：`services/*.jar`
- H5 静态站点：`frontend-h5/`
- 管理后台静态站点：`frontend-admin/`
- MySQL 初始化脚本：`mysql/init.sql`
- 一键编排：`docker-compose.yml`

## 1. 服务器要求

- Linux 服务器一台
- 已安装 Docker 与 Docker Compose
- 建议开放端口：`80`、`5174`、`8080`、`8090`

如果要对外提供正式域名，建议再接一个反向代理或证书服务，把：

- `art1.cn` 指向 H5
- `admin.art1.cn` 指向后台

## 2. 上传部署目录

把整个 `deploy-lighthouse/` 上传到服务器，例如：

```bash
scp -r deploy-lighthouse root@YOUR_SERVER_IP:/opt/shiyiju
```

## 3. 配置环境变量

登录服务器后：

```bash
cd /opt/shiyiju
cp .env.example .env
vim .env
```

至少修改：

- `WECHAT_APPID`
- `MYSQL_ROOT_PASSWORD`
- `WECHAT_SECRET`
- `WECHAT_OFFICIAL_APPID`
- `WECHAT_OFFICIAL_SECRET`
- `WECHAT_OPEN_APPID`
- `WECHAT_OPEN_SECRET`
- `WXPAY_APP_ID`
- `WXPAY_MINI_APP_ID`
- `WXPAY_OFFICIAL_APP_ID`
- `WXPAY_MCH_ID`
- `WXPAY_API_KEY`
- `WXPAY_MCH_KEY`
- 短信相关密钥（如果启用短信）

如果启用了支付宝支付/退款，还需要补齐：

- `ALIPAY_ENABLED=true`
- `ALIPAY_APP_ID`
- `ALIPAY_PRIVATE_KEY`
- `ALIPAY_PUBLIC_KEY`

微信退款证书文件需要放在：

- `certs/wxpay/apiclient_cert.p12`

## 3.1 微信一键登录配置

如果要启用“微信一键登录”，需要按端配置对应参数：

- 小程序登录：`WECHAT_APPID`、`WECHAT_SECRET`
- 微信内 H5 授权登录：`WECHAT_OFFICIAL_APPID`、`WECHAT_OFFICIAL_SECRET`
- iOS/Android App 原生微信登录：`WECHAT_OPEN_APPID`、`WECHAT_OPEN_SECRET`

对应关系：

- 前端 H5 跳转使用 `frontend/.env.*` 里的 `VITE_WECHAT_OFFICIAL_APP_ID`
- 后端换取 `openid` 使用 `WECHAT_OFFICIAL_APPID` / `WECHAT_OFFICIAL_SECRET`
- App 端原生 SDK 拿到的 `code` 使用 `WECHAT_OPEN_APPID` / `WECHAT_OPEN_SECRET` 换取 `openid`

还需要在微信公众平台完成以下配置：

- 公众号网页授权域名：填写 H5 正式域名，例如 `art1.cn`
- 微信开放平台移动应用：Bundle ID、Universal Link、AppID/Secret 必须与 iOS/Android App 配置一致
- H5 OAuth 回调地址当前走站点根路径 `/`，登录页会自动改写到 `/#/pages/login/index`
- 小程序后台的服务器域名、业务域名、request 合法域名要与线上域名保持一致

如果这几项有任意一项缺失，会出现这些现象：

- 小程序一键登录报“微信登录服务暂不可用”
- 微信内 H5 点击一键登录后无法拿到 `code`
- H5 回调成功但后端返回“微信公众号配置不完整”

本地可以先用仓库脚本完成证书安装和自检：

```bash
cd /Users/master/CodeBuddy/art12
scripts/configure-wxpay-cert.sh /path/to/apiclient_cert.p12
scripts/check-payment-config.sh deploy-lighthouse/.env.example
```

## 4. 检查域名配置

默认 nginx 配置文件是：

- `config/nginx-h5.conf`
- `config/nginx-admin.conf`

H5 部署有两个常见目录结构，Nginx 根目录不要配错：

- 如果挂载的是 `deploy-lighthouse/frontend-h5/`，`root` 应该是 `/usr/share/nginx/html`
- 如果挂载的是 `frontend/dist/` 整个目录，H5 实际文件在 `build/h5/` 下，`root` 必须改成 `/usr/share/nginx/html/build/h5`
- 更稳妥的做法是直接挂载 `frontend/dist/build/h5/`，这样 `root` 仍然保持 `/usr/share/nginx/html`

当前内置域名：

- `art1.cn`
- `www.art1.cn`
- `admin.art1.cn`

如果你的正式域名不同，需要先改这两个文件里的 `server_name`。

## 5. 启动服务

```bash
docker compose up -d --build
```

查看状态：

```bash
docker compose ps
docker compose logs -f gateway
docker compose logs -f admin
```

支付发布后建议立刻做一次验收：

```bash
cd /Users/master/CodeBuddy/art12
scripts/verify-payment-release.sh deploy-lighthouse/.env
```

如果服务不在本机 `127.0.0.1:8080`，可以改 `BASE_URL`：

```bash
BASE_URL=https://a.art1.cn scripts/verify-payment-release.sh deploy-lighthouse/.env
```

## 6. 访问地址

- H5：首页 `http://YOUR_DOMAIN/`
- 管理后台 `http://admin.YOUR_DOMAIN/` 或 `http://YOUR_SERVER_IP:5174/`
- 网关接口 `http://YOUR_SERVER_IP:8080/`

## 7. 常用运维命令

重启：

```bash
docker compose restart
```

停止：

```bash
docker compose down
```

更新后重建：

```bash
docker compose down
docker compose up -d --build
```

如果本次只是更新 H5 静态页或协议页，也可以先同步 `frontend-h5/` 与 `config/nginx-h5.conf`，再执行：

```bash
docker compose restart frontend-h5
```

协议页上线后建议补一轮检查：

```bash
cd /Users/master/CodeBuddy/art12
BASE_URL=https://www.art1.cn scripts/verify-legal-pages.sh deploy-lighthouse
```

如果这次只是发布协议页，也可以直接用下面这条半自动命令：

```bash
cd /Users/master/CodeBuddy/art12
REMOTE_HOST=YOUR_SERVER_IP REMOTE_USER=root REMOTE_DIR=/opt/shiyiju \
VERIFY_BASE_URL=https://www.art1.cn \
scripts/deploy-legal-pages.sh
```

如果线上出现以下现象，优先检查 H5 静态根目录是否挂错：

- 首页返回 `403 Forbidden`
- `/agreement`、`/privacy` 返回 `500 Internal Server Error`
- Nginx 日志出现 `rewrite or internal redirection cycle while internally redirecting to "/index.html"`

## 8. 当前注意事项

- 这套部署依赖仓库里现成的 JAR 和前端产物，不会在服务器上重新编译源码。
- 本地环境当前没有安装 Docker，所以我这边无法直接替你在本机完成容器启动验证。
- 如果你把服务器 IP、登录方式或域名给我，我下一步可以继续帮你整理成精确到命令的正式上线流程。
