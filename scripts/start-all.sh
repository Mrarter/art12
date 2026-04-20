#!/bin/bash
# ============================================
# 拾艺局 - 一键启动所有服务
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PROJECT_ROOT="/Users/master/CodeBuddy/art12"
cd "$PROJECT_ROOT"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}    拾艺局 - 一键启动脚本${NC}"
echo -e "${GREEN}========================================${NC}"

# 检查 Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}错误: 未检测到 Java 环境${NC}"
    exit 1
fi

echo -e "\n${YELLOW}[1/5] 检查基础设施服务...${NC}"

# 检查 MySQL
if ! nc -z localhost 3306 2>/dev/null; then
    echo -e "${YELLOW}  ⚠ MySQL 未运行 (localhost:3306)${NC}"
else
    echo -e "${GREEN}  ✓ MySQL 已运行${NC}"
fi

# 检查 Redis
if ! nc -z localhost 6379 2>/dev/null; then
    echo -e "${YELLOW}  ⚠ Redis 未运行 (localhost:6379)${NC}"
else
    echo -e "${GREEN}  ✓ Redis 已运行${NC}"
fi

# 检查 Nacos
if ! nc -z localhost 8848 2>/dev/null; then
    echo -e "${YELLOW}  ⚠ Nacos 未运行，启动中...${NC}"
    cd "$PROJECT_ROOT/nacos" && sh bin/startup.sh -m standalone > /tmp/nacos.log 2>&1 &
    sleep 15
else
    echo -e "${GREEN}  ✓ Nacos 已运行${NC}"
fi

echo -e "\n${YELLOW}[2/5] 启动后端微服务...${NC}"

BACKEND_DIR="$PROJECT_ROOT/backend"
SERVICES=(
    "shiyiju-gateway:8080"
    "shiyiju-user:8081"
    "shiyiju-product:8082"
    "shiyiju-order:8083"
    "shiyiju-auction:8084"
    "shiyiju-promotion:8085"
    "shiyiju-community:8086"
    "shiyiju-file:8087"
    "shiyiju-message:8088"
    "shiyiju-admin:8090"
)

for service_info in "${SERVICES[@]}"; do
    IFS=':' read -r service_name port <<< "$service_info"
    
    # 检查是否已经在运行
    if nc -z localhost "$port" 2>/dev/null; then
        echo -e "${GREEN}  ✓ $service_name 已运行 (port $port)${NC}"
    else
        echo -e "${YELLOW}  → 启动 $service_name...${NC}"
        nohup java -jar "$BACKEND_DIR/$service_name/target/$service_name-1.0.0-SNAPSHOT.jar" \
            > "/tmp/$service_name.log" 2>&1 &
        sleep 2
    fi
done

echo -e "\n${YELLOW}[3/5] 等待服务就绪...${NC}"
sleep 10

echo -e "\n${YELLOW}[4/5] 检查服务健康状态...${NC}"

check_service() {
    local port=$1
    local name=$2
    if nc -z localhost "$port" 2>/dev/null; then
        echo -e "${GREEN}  ✓ $name${NC}"
        return 0
    else
        echo -e "${RED}  ✗ $name 未响应${NC}"
        return 1
    fi
}

check_service 8080 "Gateway"
check_service 8081 "User"
check_service 8082 "Product"
check_service 8083 "Order"
check_service 8084 "Auction"
check_service 8085 "Promotion"
check_service 8086 "Community"
check_service 8087 "File"
check_service 8088 "Message"
check_service 8090 "Admin"

echo -e "\n${YELLOW}[5/5] 启动前端服务...${NC}"

# 前端 H5
if ! nc -z localhost 5173 2>/dev/null; then
    echo -e "${YELLOW}  → 启动小程序前端 (H5)...${NC}"
    cd "$PROJECT_ROOT/frontend"
    nohup npm run dev:h5 > /tmp/frontend.log 2>&1 &
else
    echo -e "${GREEN}  ✓ 小程序前端已运行${NC}"
fi

# 管理后台
if ! nc -z localhost 5174 2>/dev/null; then
    echo -e "${YELLOW}  → 启动管理后台前端...${NC}"
    cd "$PROJECT_ROOT/admin"
    nohup npm run dev > /tmp/admin-frontend.log 2>&1 &
else
    echo -e "${GREEN}  ✓ 管理后台前端已运行${NC}"
fi

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}    启动完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "访问地址:"
echo "  - 网关 API:     http://localhost:8080"
echo "  - 小程序前端:   http://localhost:5173"
echo "  - 管理后台:     http://localhost:5174"
echo "  - Nacos:        http://localhost:8848/nacos (nacos/nacos)"
echo ""
echo "日志查看:"
echo "  - tail -f /tmp/shiyiju-gateway.log"
echo "  - tail -f /tmp/shiyiju-order.log"
echo ""