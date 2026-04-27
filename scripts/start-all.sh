#!/bin/bash
# 拾艺局 - 一键启动脚本
# 功能：启动MySQL、Redis和所有后端服务

echo "🎨 ========================================"
echo "🎨 拾艺局 - 艺术品交易平台"
echo "🎨 一键启动脚本"
echo "🎨 ========================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 函数：检查服务是否运行
check_service() {
    if $1 > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} $2"
        return 0
    else
        echo -e "${RED}✗${NC} $2"
        return 1
    fi
}

# 函数：启动服务
start_service() {
    echo -e "${YELLOW}→${NC} $1"
    $2
    sleep 2
}

# 1. 检查环境
echo "📋 检查环境..."
echo ""

# 检查Java
if check_service "java -version" "Java"; then
    java -version 2>&1 | head -1
fi

# 检查Maven
if check_service "mvn -version" "Maven"; then
    mvn -version | head -1
fi

# 检查MySQL
if check_service "mysqladmin ping -u root -proot123456" "MySQL"; then
    echo ""
    echo "📊 数据库状态:"
    mysql -u root -proot123456 -e "SELECT COUNT(*) as 表数量 FROM information_schema.tables WHERE table_schema = 'shiyiju';" 2>/dev/null | grep -v "mysql:"
fi

# 检查Redis
if check_service "redis-cli ping" "Redis"; then
    echo ""
fi

echo ""
echo "🎨 ========================================"
echo ""

# 2. 启动MySQL（如果未运行）
echo "🚀 检查MySQL服务..."
if ! mysqladmin ping -u root -proot123456 > /dev/null 2>&1; then
    echo "  MySQL未运行，尝试启动..."
    brew services start mysql@8.0 > /dev/null 2>&1 || echo "  请手动启动MySQL: brew services start mysql@8.0"
    sleep 3
fi

# 3. 启动Redis（如果未运行）
echo "🚀 检查Redis服务..."
if ! redis-cli ping > /dev/null 2>&1; then
    echo "  Redis未运行，尝试启动..."
    brew services start redis > /dev/null 2>&1 || echo "  请手动启动Redis: brew services start redis"
    sleep 2
fi

echo ""
echo "🎨 ========================================"
echo ""

# 4. 启动后端服务
echo "🚀 启动后端服务..."
echo ""

BACKEND_DIR="/Users/master/CodeBuddy/art12/backend"

# 检查jar文件是否存在
if [ ! -f "$BACKEND_DIR/shiyiju-gateway/target/shiyiju-gateway-1.0.0-SNAPSHOT.jar" ]; then
    echo -e "${RED}错误: 未找到jar文件，请先运行: cd $BACKEND_DIR && mvn clean package -DskipTests${NC}"
    exit 1
fi

# 启动网关服务
echo -e "${YELLOW}→${NC} 启动 API网关 (端口 8080)..."
cd "$BACKEND_DIR"
nohup java -jar shiyiju-gateway/target/shiyiju-gateway-1.0.0-SNAPSHOT.jar > logs/gateway.log 2>&1 &
echo "  PID: $!"
sleep 3

# 启动用户服务
echo -e "${YELLOW}→${NC} 启动 用户服务 (端口 8081)..."
nohup java -jar shiyiju-user/target/shiyiju-user-1.0.0-SNAPSHOT.jar > logs/user.log 2>&1 &
echo "  PID: $!"
sleep 3

# 启动商品服务
echo -e "${YELLOW}→${NC} 启动 商品服务 (端口 8082)..."
nohup java -jar shiyiju-product/target/shiyiju-product-1.0.0-SNAPSHOT.jar > logs/product.log 2>&1 &
echo "  PID: $!"
sleep 3

# 启动订单服务
echo -e "${YELLOW}→${NC} 启动 订单服务 (端口 8083)..."
nohup java -jar shiyiju-order/target/shiyiju-order-1.0.0-SNAPSHOT.jar > logs/order.log 2>&1 &
echo "  PID: $!"
sleep 2

# 启动文件服务
echo -e "${YELLOW}→${NC} 启动 文件服务 (端口 8087)..."
nohup java -jar shiyiju-file/target/shiyiju-file-1.0.0-SNAPSHOT.jar > logs/file.log 2>&1 &
echo "  PID: $!"
sleep 2

echo ""
echo "🎨 ========================================"
echo ""

# 5. 启动前端服务
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
echo "  PID: $!"
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
echo "  • 后端日志:  backend/logs/*.log"
echo "  • 前端日志:  logs/frontend.log"
echo ""
echo "🛑 停止服务:"
echo "  • killall java"
echo "  • killall node"
echo ""
echo "🎨 ========================================"
