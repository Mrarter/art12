# 拾艺局后端服务 Dockerfile
# 多阶段构建，优化镜像大小

# 第一阶段：构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制 POM 文件
COPY pom.xml .
COPY shiyiju-common/pom.xml shiyiju-common/
COPY shiyiju-gateway/pom.xml shiyiju-gateway/
COPY shiyiju-user/pom.xml shiyiju-user/
COPY shiyiju-product/pom.xml shiyiju-product/
COPY shiyiju-order/pom.xml shiyiju-order/
COPY shiyiju-auction/pom.xml shiyiju-auction/
COPY shiyiju-promotion/pom.xml shiyiju-promotion/
COPY shiyiju-community/pom.xml shiyiju-community/
COPY shiyiju-file/pom.xml shiyiju-file/
COPY shiyiju-message/pom.xml shiyiju-message/
COPY shiyiju-admin/pom.xml shiyiju-admin/

# 下载依赖（利用缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY shiyiju-common/src shiyiju-common/src
COPY shiyiju-gateway/src shiyiju-gateway/src
COPY shiyiju-user/src shiyiju-user/src
COPY shiyiju-product/src shiyiju-product/src
COPY shiyiju-order/src shiyiju-order/src
COPY shiyiju-auction/src shiyiju-auction/src
COPY shiyiju-promotion/src shiyiju-promotion/src
COPY shiyiju-community/src shiyiju-community/src
COPY shiyiju-file/src shiyiju-file/src
COPY shiyiju-message/src shiyiju-message/src
COPY shiyiju-admin/src shiyiju-admin/src

# 构建所有服务
RUN mvn package -DskipTests -B

# 第二阶段：运行阶段
FROM eclipse-temurin:17-jre-alpine

# 添加非 root 用户
RUN addgroup -S shiyiju && adduser -S shiyiju -G shiyiju

WORKDIR /app

# 复制 JAR 文件
COPY --from=builder /app/shiyiju-gateway/target/shiyiju-gateway-1.0.0-SNAPSHOT.jar gateway.jar
COPY --from=builder /app/shiyiju-user/target/shiyiju-user-1.0.0-SNAPSHOT.jar user.jar
COPY --from=builder /app/shiyiju-product/target/shiyiju-product-1.0.0-SNAPSHOT.jar product.jar
COPY --from=builder /app/shiyiju-order/target/shiyiju-order-1.0.0-SNAPSHOT.jar order.jar
COPY --from=builder /app/shiyiju-auction/target/shiyiju-auction-1.0.0-SNAPSHOT.jar auction.jar
COPY --from=builder /app/shiyiju-promotion/target/shiyiju-promotion-1.0.0-SNAPSHOT.jar promotion.jar
COPY --from=builder /app/shiyiju-community/target/shiyiju-community-1.0.0-SNAPSHOT.jar community.jar
COPY --from=builder /app/shiyiju-file/target/shiyiju-file-1.0.0-SNAPSHOT.jar file.jar
COPY --from=builder /app/shiyiju-message/target/shiyiju-message-1.0.0-SNAPSHOT.jar message.jar
COPY --from=builder /app/shiyiju-admin/target/shiyiju-admin-1.0.0-SNAPSHOT.jar admin.jar

# 复制配置文件
COPY --from=builder /app/shiyiju-gateway/src/main/resources application.yml
COPY --from=builder /app/shiyiju-gateway/src/main/resources/bootstrap.yml bootstrap.yml

# 创建启动脚本
RUN echo '#!/bin/sh\n\
SERVICE=${SERVICE:-gateway}\n\
case $SERVICE in\n\
  gateway)  exec java -jar gateway.jar ;;\n\
  user)    exec java -jar user.jar ;;\n\
  product) exec java -jar product.jar ;;\n\
  order)   exec java -jar order.jar ;;\n\
  auction) exec java -jar auction.jar ;;\n\
  promotion) exec java -jar promotion.jar ;;\n\
  community) exec java -jar community.jar ;;\n\
  file)   exec java -jar file.jar ;;\n\
  message) exec java -jar message.jar ;;\n\
  admin)  exec java -jar admin.jar ;;\n\
esac' > start.sh && chmod +x start.sh

# 复制启动脚本
COPY --from=builder /app/start.sh /app/start.sh

# 切换到非 root 用户
USER shiyiju

# 暴露端口
EXPOSE 8080 8081 8082 8083 8084 8085 8086 8087 8088 8090

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 启动命令（可以通过 SERVICE 环境变量指定服务）
CMD ["sh", "-c", "case $SERVICE in gateway) exec java -jar gateway.jar ;; user) exec java -jar user.jar ;; product) exec java -jar product.jar ;; order) exec java -jar order.jar ;; auction) exec java -jar auction.jar ;; promotion) exec java -jar promotion.jar ;; community) exec java -jar community.jar ;; file) exec java -jar file.jar ;; message) exec java -jar message.jar ;; admin) exec java -jar admin.jar ;; esac"]