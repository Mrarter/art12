# CloudBase 云托管 Dockerfile - API 网关服务
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制应用 JAR 文件
# 构建命令：mvn clean package -DskipTests -pl shiyiju-gateway -am
COPY target/shiyiju-gateway-*.jar app.jar

# 创建非 root 用户运行应用
RUN useradd -m -u 1000 appuser
USER appuser

# 暴露服务端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]

# JVM 参数优化
CMD ["-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-XX:+UseG1GC", \
     "-XX:MaxGCPauseMillis=200", \
     "-XX:+ParallelRefProcEnabled", \
     "-XX:+HeapDumpOnOutOfMemoryError", \
     "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
     "-Dspring.profiles.active=cloudbase", \
     "-Dserver.port=8080"]