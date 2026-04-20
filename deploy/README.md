# 拾艺局 Docker 基础配置

## 目录结构

```
deploy/
├── docker/                    # Docker Compose 本地部署
│   ├── docker-compose.yml     # 主编排文件
│   ├── .env                   # 环境变量
│   └── mysql/                 # MySQL 配置
│       └── init.sql           # 初始化脚本
│
└── k8s/                       # Kubernetes 生产部署
    ├── namespace.yaml         # 命名空间
    ├── configmap.yaml         # 配置中心
    ├── secret.yaml            # 密钥
    ├── mysql.yaml             # MySQL 部署
    ├── redis.yaml             # Redis 部署
    └── services/             # 各微服务部署
        ├── gateway.yaml
        ├── user.yaml
        ├── product.yaml
        ├── order.yaml
        ├── auction.yaml
        ├── promotion.yaml
        ├── community.yaml
        ├── file.yaml
        ├── message.yaml
        └── admin.yaml
```

## 快速启动（Docker Compose）

```bash
cd deploy/docker
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止
docker-compose down
```

## 生产部署（Kubernetes）

```bash
# 应用所有配置
kubectl apply -f ../k8s/

# 查看部署状态
kubectl get pods -n shiyiju
```

## 环境变量说明

| 变量 | 说明 | 示例 |
|------|------|------|
| MYSQL_ROOT_PASSWORD | MySQL root 密码 | root123456 |
| NACOS_SERVER_ADDR | Nacos 地址 | nacos:8848 |
| REDIS_HOST | Redis 地址 | redis |
| SPRING_PROFILES_ACTIVE | 运行环境 | prod |