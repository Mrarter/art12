# 拾艺局 - 高端艺术品社交电商平台

> 高端艺术品交易平台，连接艺术家、经纪人、收藏家与艺术推广者

## 项目结构

```
shiyiju/
├── backend/                      # 后端项目
│   ├── shiyiju-gateway/         # API 网关服务 (端口: 8080)
│   ├── shiyiju-common/          # 公共模块
│   ├── shiyiju-user/            # 用户服务 (端口: 8081)
│   ├── shiyiju-product/         # 商品服务 (端口: 8082)
│   ├── shiyiju-order/           # 订单服务 (端口: 8083)
│   ├── shiyiju-auction/          # 拍卖服务 (端口: 8084)
│   ├── shiyiju-promotion/        # 分销服务 (端口: 8085)
│   ├── shiyiju-community/        # 社区服务 (端口: 8086)
│   └── shiyiju-message/          # 消息服务 (端口: 8087)
│
├── frontend/                     # 前端项目
│   └── shiyiju-app/             # Uni-app 主项目
│
├── documents/                    # 文档
│   ├── api/                     # API 文档
│   ├── db/                      # 数据库设计
│   └── guide/                   # 开发指南
│
└── deploy/                       # 部署配置
```

## 技术栈

### 后端
- Java 17
- Spring Cloud Alibaba 2023
- MySQL 8.0
- Redis 7.x
- RocketMQ 5.x

### 前端
- Uni-app 4.x
- Vue 3
- uView UI
- Pinia

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8.0+
- Redis 7.x
- Nacos (用于服务注册发现)

### 2. 数据库初始化

```bash
# 创建数据库并导入表结构
mysql -u root -p < documents/db/shiyiju_user.sql
```

### 3. 后端启动

```bash
# 进入后端目录
cd backend

# 安装依赖（首次运行）
mvn clean install -DskipTests

# 启动网关服务
cd shiyiju-gateway && mvn spring-boot:run

# 启动用户服务
cd shiyiju-user && mvn spring-boot:run
# ... 其他服务同理
```

### 4. 前端启动

```bash
# 进入前端目录
cd frontend/shiyiju-app

# 安装依赖
npm install

# 运行微信小程序
npm run dev:mp-weixin

# 运行 H5
npm run dev:h5
```

### 5. 微信小程序开发

1. 下载并打开微信开发者工具
2. 导入 `frontend/shiyiju-app`
3. 填入 AppID: `wx4b7015f42a4fa49c`
4. 编译运行

## 核心功能

### 用户体系
- [x] 微信一键登录
- [x] 多身份切换（艺术家/经纪人/收藏家/艺荐官）
- [x] 艺术家认证
- [x] 经纪人认证

### 商品模块
- [x] 画廊首页
- [x] 艺术门类分类
- [x] 作品详情
- [x] 搜索与筛选
- [ ] 动态价格调整

### 交易模块
- [x] 购物车
- [x] 订单流程
- [x] 微信支付
- [ ] 售后功能

### 拍卖模块
- [ ] 拍卖专场
- [ ] 实时竞拍
- [ ] 保证金管理

### 分销模块
- [x] 艺荐官开通
- [x] 二级分销
- [x] 佣金计算
- [ ] 团队管理
- [ ] 提现功能

### 社区模块
- [ ] 发帖与评论
- [ ] 话题功能
- [ ] 内容推荐

## 配置说明

### 微信小程序配置

`manifest.json` 中已配置:
```json
{
  "mp-weixin": {
    "appid": "wx4b7015f42a4fa49c"
  }
}
```

### 后端配置

修改 `application.yml` 中的数据库和 Redis 配置:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shiyiju
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
```

## 开发指南

### API 接口规范

- 基础路径: `/api`
- 认证方式: Bearer Token
- 响应格式:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1234567890
}
```

### 分页规范

```json
{
  "total": 100,
  "page": 1,
  "pageSize": 20,
  "totalPages": 5,
  "records": []
}
```

## 注意事项

1. **分销合规**: 系统严格限制二级分销，佣金发放需代扣代缴个税
2. **支付安全**: 佣金结算采用 T+N 模式
3. **并发控制**: 使用 Redis 分布式锁防止超卖
4. **数据追溯**: 价格变更需完整记录

## License

Private - All Rights Reserved
