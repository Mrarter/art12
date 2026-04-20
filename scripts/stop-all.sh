#!/bin/bash
# ============================================
# 拾艺局 - 一键停止所有服务
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}正在停止拾艺局所有服务...${NC}"

# 停止后端微服务
SERVICES=(
    "shiyiju-gateway"
    "shiyiju-user"
    "shiyiju-product"
    "shiyiju-order"
    "shiyiju-auction"
    "shiyiju-promotion"
    "shiyiju-community"
    "shiyiju-file"
    "shiyiju-message"
    "shiyiju-admin"
)

for service in "${SERVICES[@]}"; do
    pid=$(ps aux | grep "$service" | grep -v grep | awk '{print $2}')
    if [ -n "$pid" ]; then
        echo "停止 $service (PID: $pid)..."
        kill $pid 2>/dev/null || true
    fi
done

# 停止 Nacos
nacos_pid=$(ps aux | grep "nacos-server.jar" | grep -v grep | awk '{print $2}')
if [ -n "$nacos_pid" ]; then
    echo "停止 Nacos (PID: $nacos_pid)..."
    kill $nacos_pid 2>/dev/null || true
fi

# 停止前端进程
for port in 5173 5174; do
    pid=$(lsof -ti:$port 2>/dev/null)
    if [ -n "$pid" ]; then
        echo "停止前端服务 (PID: $pid, port $port)..."
        kill $pid 2>/dev/null || true
    fi
done

echo -e "${GREEN}所有服务已停止${NC}"