# 艺术12项目 - 服务启动指南

## 前置依赖

- **JDK 17** (后端 Java 服务)
- **Node.js 16+** (前端)
- **MySQL 8.0** (端口 3306，数据库 `shiyiju_local`)
- **Redis** (端口 6379)
- **Nacos** (可选，当前配置 `enabled: false`)

---

## 1. 启动顺序（按顺序执行）

### 1.1 基础设施

```bash
# MySQL
brew services start mysql@8.0

# Redis
brew services start redis
```

### 1.2 后端服务（按端口顺序）

所有后端服务在 `backend/` 目录下，使用 Maven 构建和启动。

```bash
cd /Users/master/CodeBuddy/art12/backend

# 编译所有服务
mvn clean package -DskipTests
```

**批量启动（推荐）：**

```bash
# 统一日志目录
LOGDIR=/Users/master/CodeBuddy/art12/logs

# 用户服务（8081）
java -jar shiyiju-user/target/shiyiju-user-1.0.0-SNAPSHOT.jar > $LOGDIR/user.log 2>&1 &

# 商品/产品服务（8082）- 核心服务
java -jar shiyiju-product/target/shiyiju-product-1.0.0-SNAPSHOT.jar --spring.profiles.active=local > $LOGDIR/product.log 2>&1 &

# 订单服务（8083）
java -jar shiyiju-order/target/shiyiju-order-1.0.0-SNAPSHOT.jar > $LOGDIR/order.log 2>&1 &

# 拍卖服务（8084）
java -jar shiyiju-auction/target/shiyiju-auction-1.0.0-SNAPSHOT.jar > $LOGDIR/auction.log 2>&1 &

# 分销/推广服务（8085）
java -jar shiyiju-promotion/target/shiyiju-promotion-1.0.0-SNAPSHOT.jar > $LOGDIR/promotion.log 2>&1 &

# 社区服务（8086）
java -jar shiyiju-community/target/shiyiju-community-1.0.0-SNAPSHOT.jar > $LOGDIR/community.log 2>&1 &

# 文件服务（8087）
java -jar shiyiju-file/target/shiyiju-file-1.0.0-SNAPSHOT.jar > $LOGDIR/file.log 2>&1 &

# 消息服务（8088）
java -jar shiyiju-message/target/shiyiju-message-1.0.0-SNAPSHOT.jar > $LOGDIR/message.log 2>&1 &

# 网关（8080）
java -jar shiyiju-gateway/target/shiyiju-gateway-1.0.0-SNAPSHOT.jar > $LOGDIR/gateway.log 2>&1 &

# 管理后台服务（8090）
java -jar shiyiju-admin/target/shiyiju-admin-1.0.0-SNAPSHOT.jar > $LOGDIR/admin.log 2>&1 &
```

**单个服务重新编译启动（仅编译+启动单个）：**

```bash
cd /Users/master/CodeBuddy/art12/backend

# 产品服务
pkill -f shiyiju-product
mvn clean package -pl shiyiju-product -am -DskipTests -q
nohup java -jar shiyiju-product/target/shiyiju-product-1.0.0-SNAPSHOT.jar --spring.profiles.active=local > /Users/master/CodeBuddy/art12/logs/product.log 2>&1 &

# 网关
pkill -f shiyiju-gateway
mvn clean package -pl shiyiju-gateway -am -DskipTests -q
nohup java -jar shiyiju-gateway/target/shiyiju-gateway-1.0.0-SNAPSHOT.jar > /Users/master/CodeBuddy/art12/logs/gateway.log 2>&1 &

# 其他服务修改方式同上，替换服务名和端口
```

### 1.3 前端服务

```bash
cd /Users/master/CodeBuddy/art12

# C端前端（H5，端口 5176）
cd frontend
npm run dev:h5 &

# 管理后台（端口 5174）
cd admin
npm run dev &
```

---

## 2. 服务端口汇总

| 服务 | 端口 | 说明 |
|------|------|------|
| **Gateway（网关）** | **8080** | 统一入口 |
| 用户服务 | 8081 | 认证、用户 |
| 产品服务 | 8082 | 作品、评分、资质 |
| 订单服务 | 8083 | 订单 |
| 拍卖服务 | 8084 | 拍卖 |
| 分销服务 | 8085 | 推广、分销 |
| 社区服务 | 8086 | 社区 |
| 文件服务 | 8087 | 文件上传/CDN |
| 消息服务 | 8088 | 消息 |
| 管理后台服务 | 8090 | 运营后台API |
| **管理后台前端** | **5174** | 运营后台UI (Vite) |
| **C端前端 H5** | **5176** | 用户端UI (Vite) |

---

## 3. 进程管理

### 查看进程

```bash
# 查看所有 Java 进程
ps aux | grep java | grep -v grep

# 查看所有后端服务
ps aux | grep shiyiju- | grep -v grep

# 查看前端开发服务器
lsof -i :5174 -i :5176
```

### 停止所有服务

```bash
# 停止所有 Java 后端
pkill -f shiyiju-
# 或逐个停止
pkill -f shiyiju-product
pkill -f shiyiju-gateway
pkill -f shiyiju-admin

# 停止前端
pkill -f "vite"  # 停止所有 Vite 开发服务器
```

---

## 4. 编译构建

```bash
# 编译所有后端服务
cd /Users/master/CodeBuddy/art12/backend
mvn clean package -DskipTests

# 编译单个服务及其依赖
mvn clean package -pl shiyiju-product -am -DskipTests

# 编译前端
cd /Users/master/CodeBuddy/art12/frontend
npm run build:mp-weixin  # 微信小程序
npm run build:h5         # H5

# 编译管理后台
cd /Users/master/CodeBuddy/art12/admin
npm run build
```

---

## 5. 访问地址

| 页面 | 地址 |
|------|------|
| 运营后台管理 | http://localhost:5174 |
| C端 H5 首页 | http://localhost:5176 |
| API 网关入口 | http://localhost:8080 |
| API 测试（产品服务直连） | http://localhost:8082 |

---

## 6. 快速启动（开发常用）

```bash
# 1. 确认 MySQL/Redis 已启动
brew services start mysql@8.0
brew services start redis

# 2. 编译并启动产品服务（最常用）
cd /Users/master/CodeBuddy/art12/backend
mvn clean package -pl shiyiju-product -am -DskipTests -q
nohup java -jar shiyiju-product/target/shiyiju-product-1.0.0-SNAPSHOT.jar --spring.profiles.active=local > /Users/master/CodeBuddy/art12/logs/product.log 2>&1 &

# 3. 编译并启动网关
mvn clean package -pl shiyiju-gateway -am -DskipTests -q
nohup java -jar shiyiju-gateway/target/shiyiju-gateway-1.0.0-SNAPSHOT.jar > /Users/master/CodeBuddy/art12/logs/gateway.log 2>&1 &

# 4. 启动管理后台
cd /Users/master/CodeBuddy/art12/admin
nohup npx vite --port 5174 > /dev/null 2>&1 &

# 5. 等待 10 秒后测试
curl http://localhost:8080/api/artist/score/1
```
