# CloudBase 环境配置指南

## 环境信息
请在腾讯云 CloudBase 控制台创建环境后填写以下信息：

```bash
# CloudBase 环境配置
TENCENT_CLOUDBASE_ENVID=你的环境ID（如：shiyiju-xxx）
TENCENT_CLOUDBASE_SECRET_ID=你的SecretId
TENCENT_CLOUDBASE_SECRET_KEY=你的SecretKey
TENCENT_CLOUDBASE_REGION=ap-shanghai  # 建议选择上海区域

# 数据库配置
TENCENT_CLOUDBASE_MYSQL_HOST=你的MySQL主机地址
TENCENT_CLOUDBASE_MYSQL_PORT=3306
TENCENT_CLOUDBASE_MYSQL_USER=root
TENCENT_CLOUDBASE_MYSQL_PASSWORD=你的密码
TENCENT_CLOUDBASE_MYSQL_DATABASE=shiyiju

# 存储配置
TENCENT_CLOUDBASE_STORAGE_BUCKET=shiyiju-storage
```

## 部署架构

### 1. 前端部署（Uni-app）
```
前端项目路径：/Users/master/CodeBuddy/art12/frontend/
构建命令：npm run build:h5
输出目录：dist/build/h5
CloudBase 静态托管路径：/
```

### 2. 后端部署（Java 微服务）
推荐使用 **CloudBase 云托管（容器型）** 部署：

| 服务 | 端口 | 部署方式 |
|------|------|----------|
| shiyiju-gateway | 8080 | 容器型云托管 |
| shiyiju-user | 8081 | 容器型云托管 |
| shiyiju-product | 8082 | 容器型云托管 |
| shiyiju-order | 8083 | 容器型云托管 |
| shiyiju-auction | 8084 | 容器型云托管 |
| shiyiju-promotion | 8085 | 容器型云托管 |
| shiyiju-community | 8086 | 容器型云托管 |
| shiyiju-message | 8087 | 容器型云托管 |

### 3. 数据库迁移
将现有 MySQL 数据库迁移到 CloudBase MySQL：
1. 在 CloudBase 控制台创建 MySQL 实例
2. 导出本地数据库：`mysqldump -u root -p shiyiju > shiyiju.sql`
3. 导入到 CloudBase MySQL

## 快速开始步骤

### 步骤 1：创建 CloudBase 环境
1. 访问 [腾讯云 CloudBase 控制台](https://console.cloud.tencent.com/tcb)
2. 点击"新建环境" → 选择"标准版"
3. 填写环境名称：`shiyiju-prod`
4. 完成创建

### 步骤 2：配置环境变量
将本文件中的配置信息填入项目根目录的 `.env` 文件

### 步骤 3：前端部署
```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 构建 H5 版本
npm run build:h5

# 部署到 CloudBase 静态托管
tcb hosting:deploy ./dist/build/h5 -e 你的环境ID
```

### 步骤 4：后端部署（示例：用户服务）
```bash
# 进入后端用户服务目录
cd backend/shiyiju-user

# 构建 Docker 镜像
docker build -t shiyiju-user:latest .

# 推送到腾讯云容器镜像服务
# 使用 CloudBase 云托管部署容器
```

## CloudBase 优势

### 1. **一体化开发平台**
- 静态托管：前端部署
- 云托管：Java 微服务部署
- 数据库：MySQL/PostgreSQL
- 云存储：文件存储
- 云函数：轻量级服务

### 2. **微信生态集成**
- 微信小程序原生支持
- 微信登录一键集成
- 微信支付无缝对接
- 消息推送服务

### 3. **成本优化**
- 按量计费，无闲置成本
- 自动扩缩容
- 免费额度充足

### 4. **安全合规**
- 自动 HTTPS
- 访问权限控制
- 数据加密存储
- 防 DDoS 攻击

## 后续优化建议

### 1. **CI/CD 自动化**
配置 GitHub Actions 或 Jenkins 实现自动部署

### 2. **监控告警**
启用 CloudBase 监控和告警功能

### 3. **CDN 加速**
为静态资源配置 CDN 加速

### 4. **数据库备份**
设置自动数据库备份策略

## 联系方式
如有问题，参考以下资源：
1. [CloudBase 官方文档](https://cloud.tencent.com/document/product/876)
2. [Java 部署指南](https://cloud.tencent.com/document/product/876/46176)
3. [Uni-app 部署指南](https://cloud.tencent.com/document/product/876/46177)
```

<｜DSML｜parameter name