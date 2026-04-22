#!/bin/bash
# 拾艺局·本地开发环境启动脚本
# 使用前提：已安装 Docker Desktop

echo "🎨 拾艺局 - 本地开发环境启动脚本"
echo "========================================"

# 切换到部署目录
cd "$(dirname "$0")/../deploy/docker"

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker未运行，请先启动Docker Desktop"
    exit 1
fi

echo "✅ Docker已运行"

# 创建.env文件（如果不存在）
if [ ! -f ".env" ]; then
    echo "📝 创建环境配置文件..."
    cat > .env << 'EOF'
# MySQL配置
MYSQL_ROOT_PASSWORD=root123456
MYSQL_DATABASE=shiyiju
MYSQL_USER=shiyiju
MYSQL_PASSWORD=shiyiju123

# 时区
TZ=Asia/Shanghai
EOF
    echo "✅ 环境配置文件已创建"
fi

# 启动基础设施服务（MySQL、Redis、Nacos）
echo "🚀 启动基础设施服务（MySQL、Redis、Nacos）..."
docker-compose up -d mysql redis nacos

# 等待MySQL启动
echo "⏰ 等待MySQL启动完成..."
for i in {1..30}; do
    if docker exec shiyiju-mysql mysqladmin ping -h localhost -u root -proot123456 > /dev/null 2>&1; then
        echo "✅ MySQL已就绪"
        break
    fi
    echo "   等待中... ($i/30)"
    sleep 2
done

# 等待Redis启动
echo "⏰ 等待Redis启动..."
for i in {1..10}; do
    if docker exec shiyiju-redis redis-cli ping > /dev/null 2>&1; then
        echo "✅ Redis已就绪"
        break
    fi
    echo "   等待中... ($i/10)"
    sleep 1
done

echo ""
echo "========================================"
echo "✅ 基础设施服务启动完成！"
echo ""
echo "📋 服务访问地址："
echo "   - MySQL: localhost:3306 (root/root123456)"
echo "   - Redis: localhost:6379"
echo "   - Nacos: http://localhost:8848/nacos (nacos/nacos)"
echo ""
echo "🔧 下一步："
echo "   1. 初始化数据库（参考 docs/数据库初始化指南.md）"
echo "   2. 启动后端：cd backend && mvn spring-boot:run"
echo "   3. 启动前端：cd frontend && npm run dev:h5"
echo ""
echo "========================================"
