#!/bin/bash
# ============================================
# 拾艺局 - 服务健康检查脚本
# ============================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}    拾艺局 - 服务健康检查${NC}"
echo -e "${GREEN}========================================${NC}"

# 检查函数
check_port() {
    local port=$1
    local name=$2
    if nc -z localhost "$port" 2>/dev/null; then
        echo -e "${GREEN}  ✓ $name (port $port) - 正常${NC}"
        return 0
    else
        echo -e "${RED}  ✗ $name (port $port) - 未运行${NC}"
        return 1
    fi
}

echo ""
echo "📡 基础设施服务:"
check_port 3306 "MySQL"
check_port 6379 "Redis"
check_port 8848 "Nacos"

echo ""
echo "🔧 后端微服务:"
check_port 8080 "Gateway (API网关)"
check_port 8081 "User (用户服务)"
check_port 8082 "Product (商品服务)"
check_port 8083 "Order (订单服务)"
check_port 8084 "Auction (拍卖服务)"
check_port 8085 "Promotion (分销服务)"
check_port 8086 "Community (社区服务)"
check_port 8087 "File (文件服务)"
check_port 8088 "Message (消息服务)"
check_port 8090 "Admin (管理后台)"

echo ""
echo "🖥️ 前端服务:"
check_port 5173 "小程序前端 (H5)"
check_port 5174 "管理后台前端"

echo ""
echo "🔍 API 健康检测:"

# 测试网关API
if nc -z localhost 8080 2>/dev/null; then
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/product/categories 2>/dev/null || echo "000")
    if [ "$response" = "200" ] || [ "$response" = "404" ]; then
        echo -e "${GREEN}  ✓ 网关API响应正常 (HTTP $response)${NC}"
    else
        echo -e "${YELLOW}  ⚠ 网关API响应异常 (HTTP $response)${NC}"
    fi
else
    echo -e "${RED}  ✗ 网关未运行，无法测试API${NC}"
fi

echo ""
echo "========================================"
echo "检查完成 $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"