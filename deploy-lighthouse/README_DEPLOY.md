# 拾艺局上线说明

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

- `shiyiju.online` 指向 H5
- `admin.shiyiju.online` 指向后台

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

- `MYSQL_ROOT_PASSWORD`
- `WECHAT_SECRET`
- 短信相关密钥（如果启用短信）

## 4. 检查域名配置

默认 nginx 配置文件是：

- `config/nginx-h5.conf`
- `config/nginx-admin.conf`

当前内置域名：

- `shiyiju.online`
- `www.shiyiju.online`
- `admin.shiyiju.online`

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

## 8. 当前注意事项

- 这套部署依赖仓库里现成的 JAR 和前端产物，不会在服务器上重新编译源码。
- 本地环境当前没有安装 Docker，所以我这边无法直接替你在本机完成容器启动验证。
- 如果你把服务器 IP、登录方式或域名给我，我下一步可以继续帮你整理成精确到命令的正式上线流程。
