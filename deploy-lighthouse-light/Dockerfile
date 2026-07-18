# 艺本艺术 Dockerfile - 预构建 JAR 版本
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S shiyiju && adduser -S shiyiju -G shiyiju
WORKDIR /app

# 复制所有 JAR 文件
COPY services/*.jar /app/

# 启动脚本：根据 SERVICE 环境变量选择 JAR
RUN printf '%s\n' \
  '#!/bin/sh' \
  'SVC=${SERVICE:-gateway}' \
  'case $SVC in' \
  '  gateway) exec java -Xms128m -Xmx256m -jar /app/shiyiju-gateway*.jar ;;' \
  '  user)    exec java -Xms128m -Xmx256m -jar /app/shiyiju-user*.jar ;;' \
  '  product) exec java -Xms128m -Xmx256m -jar /app/shiyiju-product*.jar ;;' \
  '  order)   exec java -Xms128m -Xmx256m -jar /app/shiyiju-order*.jar ;;' \
  '  admin)   exec java -Xms128m -Xmx256m -jar /app/shiyiju-admin*.jar ;;' \
  '  *)       echo "Unknown SERVICE: $SVC"; exit 1 ;;' \
  'esac' > /start.sh && chmod +x /start.sh

USER shiyiju
EXPOSE 8080 8081 8082 8083 8090
CMD ["sh", "/start.sh"]
