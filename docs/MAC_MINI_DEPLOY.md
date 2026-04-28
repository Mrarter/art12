# 拾艺局 - Mac Mini 本地部署指南

## 环境要求

### 硬件
- Mac Mini (Apple M1/M2/M3 或 Intel 均可)
- 推荐 16GB+ 内存
- 500GB+ 硬盘空间

### 软件
- macOS 12+ (Monterey 或更新版本)
- Docker Desktop for Mac 或 OrbStack
- 或直接安装 Java 17+ 和 MySQL 8.0

---

## 方案一：使用 Docker Desktop (推荐)

### 1. 安装 Docker Desktop
从 [docker.com](https://www.docker.com/products/docker-desktop/) 下载安装

### 2. 配置环境变量
```bash
cat > .env << EOF
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=shiyiju
MYSQL_USER=shiyiju
MYSQL_PASSWORD=shiyiju123
TZ=Asia/Shanghai
EOF
```

### 3. 启动所有服务
```bash
cd deploy/docker
docker-compose up -d
```

### 4. 查看服务状态
```bash
docker-compose ps
```

### 5. 查看日志
```bash
docker-compose logs -f gateway
```

---

## 方案二：直接运行（不依赖 Docker）

### 1. 安装依赖

```bash
# 安装 Homebrew (如果没有)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 安装 Java 17
brew install openjdk@17
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home' >> ~/.zshrc
source ~/.zshrc

# 安装 MySQL 8.0
brew install mysql
brew services start mysql

# 设置 MySQL root 密码
mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'root';"
mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS shiyiju CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 2. 初始化数据库
```bash
mysql -u root -proot shiyiju < backend/sql/init_database.sql
```

### 3. 安装 Redis
```bash
brew install redis
brew services start redis
```

### 4. 编译后端服务
```bash
cd backend

# 编译所有模块
./mvnw clean package -DskipTests

# 或使用 Maven
mvn clean package -DskipTests
```

### 5. 启动服务（按顺序）
```bash
# 在不同终端窗口启动，或使用脚本

# 终端 1: Gateway (8080)
java -jar shiyiju-gateway/target/shiyiju-gateway-*.jar

# 终端 2: User (8081)
java -jar shiyiju-user/target/shiyiju-user-*.jar

# 终端 3: Product (8082)
java -jar shiyiju-product/target/shiyiju-product-*.jar

# 终端 4: Order (8083)
java -jar shiyiju-order/target/shiyiju-order-*.jar

# 终端 5: Auction (8084)
java -jar shiyiju-auction/target/shiyiju-auction-*.jar

# 终端 6: Promotion (8085)
java -jar shiyiju-promotion/target/shiyiju-promotion-*.jar

# 终端 7: Community (8086)
java -jar shiyiju-community/target/shiyiju-community-*.jar

# 终端 8: File (8087)
java -jar shiyiju-file/target/shiyiju-file-*.jar

# 终端 9: Message (8088)
java -jar shiyiju-message/target/shiyiju-message-*.jar

# 终端 10: Admin (8090)
java -jar shiyiju-admin/target/shiyiju-admin-*.jar
```

### 6. 构建并启动前端
```bash
# 小程序前端
cd frontend
npm install
npm run build
# 使用 nginx 或 http-server 托管
brew install nginx
nginx -c $(pwd)/deploy/docker/nginx.conf

# 管理后台
cd admin
npm install
npm run build
# 同上托管
```

---

## 快速启动脚本

### start-all.sh - 启动所有服务
```bash
#!/bin/bash

echo "Starting MySQL..."
brew services start mysql

echo "Starting Redis..."
brew services start redis

echo "Starting backend services..."
cd backend

# 后台启动各个服务
nohup java -jar shiyiju-gateway/target/shiyiju-gateway-*.jar > ../logs/gateway.log 2>&1 &
nohup java -jar shiyiju-user/target/shiyiju-user-*.jar > ../logs/user.log 2>&1 &
nohup java -jar shiyiju-product/target/shiyiju-product-*.jar > ../logs/product.log 2>&1 &
nohup java -jar shiyiju-order/target/shiyiju-order-*.jar > ../logs/order.log 2>&1 &
nohup java -jar shiyiju-auction/target/shiyiju-auction-*.jar > ../logs/auction.log 2>&1 &
nohup java -jar shiyiju-promotion/target/shiyiju-promotion-*.jar > ../logs/promotion.log 2>&1 &
nohup java -jar shiyiju-community/target/shiyiju-community-*.jar > ../logs/community.log 2>&1 &
nohup java -jar shiyiju-file/target/shiyiju-file-*.jar > ../logs/file.log 2>&1 &
nohup java -jar shiyiju-message/target/shiyiju-message-*.jar > ../logs/message.log 2>&1 &
nohup java -jar shiyiju-admin/target/shiyiju-admin-*.jar > ../logs/admin.log 2>&1 &

echo "All services started!"
echo "Gateway: http://localhost:8080"
echo "Admin Panel: http://localhost:8090"
```

---

## 端口说明

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8080 | API 网关入口 |
| User | 8081 | 用户服务 |
| Product | 8082 | 商品/作品服务 |
| Order | 8083 | 订单服务 |
| Auction | 8084 | 拍卖服务 |
| Promotion | 8085 | 分销服务 |
| Community | 8086 | 社区服务 |
| File | 8087 | 文件上传服务 |
| Message | 8088 | 消息服务 |
| Admin | 8090 | 管理后台 API |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Frontend | 5173 | 小程序前端 |
| Admin UI | 5174 | 管理后台前端 |

---

## 验证部署

```bash
# 测试 API 网关
curl http://localhost:8080/api/product/categories

# 测试管理后台
curl http://localhost:8090/admin/config/all

# 查看服务健康状态
curl http://localhost:8080/actuator/health
```

---

## 外部访问配置

如果需要从外部网络访问：

### 1. 配置固定 IP
在系统设置中为 Mac Mini 设置固定 IP

### 2. 端口映射（如有路由器）
```
8080 -> Mac Mini IP:8080  (API)
5173 -> Mac Mini IP:5173  (H5前端)
```

### 3. 修改前端 API 地址
将前端环境变量 `VITE_API_BASE_URL` 改为 Mac Mini 的 IP 地址

### 4. 小程序配置
在微信小程序后台添加 request 合法域名：
- `http://your-mac-mini-ip:8080`
