#!/bin/bash
# 拾艺局 - 独立启动脚本（不需要Nacos）
# 用于本地开发测试

echo "🎨 ========================================"
echo "🎨 拾艺局 - 艺术品交易平台"
echo "🎨 独立启动脚本（本地开发）"
echo "🎨 ========================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 检查服务是否运行
check_service() {
    if $1 > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} $2"
        return 0
    else
        echo -e "${RED}✗${NC} $2"
        return 1
    fi
}

# 1. 环境检查
echo "📋 检查环境..."
echo ""

check_service "java -version" "Java"
check_service "mysqladmin ping -u root -proot123456" "MySQL"
check_service "redis-cli ping" "Redis"

echo ""
echo "🎨 ========================================"
echo ""

# 2. 检查MySQL数据库
echo "📊 检查数据库..."
mysql -u root -proot123456 -e "USE shiyiju; SELECT COUNT(*) as 表数量 FROM information_schema.tables WHERE table_schema = 'shiyiju';" 2>/dev/null | grep -v "mysql:"

echo ""
echo "🎨 ========================================"
echo ""

# 3. 启动后端服务
echo "🚀 启动后端服务（独立模式）..."
echo ""

BACKEND_DIR="/Users/master/CodeBuddy/art12/backend"
PROFILE="local"

# 启动网关
echo -e "${YELLOW}→${NC} 启动 API网关 (端口 8080)..."
cd "$BACKEND_DIR"
nohup java -jar shiyiju-gateway/target/shiyiju-gateway-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=$PROFILE > ../logs/gateway.log 2>&1 &
GATEWAY_PID=$!
echo "  PID: $GATEWAY_PID"
sleep 5

# 检查网关是否启动成功
if curl -s http://localhost:8080 > /dev/null 2>&1; then
    echo -e "  ${GREEN}✓ 网关启动成功${NC}"
else
    echo -e "  ${YELLOW}⚠ 网关可能还在启动中${NC}"
fi

# 启动用户服务
echo -e "${YELLOW}→${NC} 启动 用户服务 (端口 8081)..."
nohup java -jar shiyiju-user/target/shiyiju-user-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=$PROFILE > ../logs/user.log 2>&1 &
USER_PID=$!
echo "  PID: $USER_PID"
sleep 4

# 启动商品服务
echo -e "${YELLOW}→${NC} 启动 商品服务 (端口 8082)..."
nohup java -jar shiyiju-product/target/shiyiju-product-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=$PROFILE > ../logs/product.log 2>&1 &
PRODUCT_PID=$!
echo "  PID: $PRODUCT_PID"
sleep 4

# 启动订单服务
echo -e "${YELLOW}→${NC} 启动 订单服务 (端口 8083)..."
nohup java -jar shiyiju-order/target/shiyiju-order-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=$PROFILE > ../logs/order.log 2>&1 &
ORDER_PID=$!
echo "  PID: $ORDER_PID"
sleep 3

# 启动文件服务
echo -e "${YELLOW}→${NC} 启动 文件服务 (端口 8087)..."
nohup java -jar shiyiju-file/target/shiyiju-file-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=$PROFILE > ../logs/file.log 2>&1 &
FILE_PID=$!
echo "  PID: $FILE_PID"
sleep 3

echo ""
echo "🎨 ========================================"
echo ""

# 4. 启动前端
echo "🚀 启动前端服务..."
echo ""

FRONTEND_DIR="/Users/master/CodeBuddy/art12/frontend"

if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    echo -e "${YELLOW}→${NC} 安装前端依赖..."
    cd "$FRONTEND_DIR"
    npm install
    echo ""
fi

echo -e "${YELLOW}→${NC} 启动 H5前端 (端口 5173)..."
cd "$FRONTEND_DIR"
nohup npm run dev:h5 > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "  PID: $FRONTEND_PID"
sleep 5

echo ""
echo "🎨 ========================================"
echo ""
echo -e "${GREEN}✅ 启动完成！${NC}"
echo ""
echo "🌐 访问地址:"
echo "  • H5前端:    http://localhost:5173"
echo "  • API网关:   http://localhost:8080"
echo "  • Swagger:   http://localhost:8080/swagger-ui.html"
echo ""
echo "📝 日志文件:"
echo "  • 网关日志:  logs/gateway.log"
echo "  • 用户日志:  logs/user.log"
echo "  • 商品日志:  logs/product.log"
echo "  • 订单日志:  logs/order.log"
echo "  • 前端日志:  logs/frontend.log"
echo ""
echo "🛑 停止所有服务:"
echo "  • killall java"
echo "  • killall node"
echo ""
echo "💡 查看日志:"
echo "  • tail -f logs/gateway.log"
echo "  • tail -f logs/frontend.log"
echo ""
echo "🎨 ========================================"
