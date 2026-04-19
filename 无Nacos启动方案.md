# 无Nacos快速启动方案

由于Nacos下载困难，可以配置服务禁用服务注册与发现，直接启动进行测试。

## 修改步骤

### 1. 修改 Gateway 配置
文件: `backend/shiyiju-gateway/src/main/resources/application.yml`

```yaml
spring:
  cloud:
    # 禁用Nacos服务发现
    nacos:
      discovery:
        enabled: false
```

### 2. 修改各微服务配置

对以下文件进行同样修改:

- shiyiju-user/src/main/resources/application.yml
- shiyiju-product/src/main/resources/application.yml  
- shiyiju-order/src/main/resources/application.yml
- shiyiju-auction/src/main/resources/application.yml
- shiyiju-promotion/src/main/resources/application.yml
- shiyiju-community/src/main/resources/application.yml
- shiyiju-message/src/main/resources/application.yml
- shiyiju-file/src/main/resources/application.yml

每个文件添加:
```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: false
```

### 3. 启动命令

```bash
cd /Users/master/CodeBuddy/art12/backend

# 启动网关
java -jar shiyiju-gateway/target/shiyiju-gateway-1.0.0.jar

# 启动用户服务
java -jar shiyiju-user/target/shiyiju-user-1.0.0.jar
```

### 4. 验证

- 用户服务: http://localhost:8081
- 商品服务: http://localhost:8082
- 订单服务: http://localhost:8083

---

我可以帮你自动修改所有配置文件。
