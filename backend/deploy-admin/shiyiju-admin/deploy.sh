#!/bin/bash
# shiyiju-admin 云端部署脚本
# 使用CloudBase CLI进行部署

echo "========================================"
echo "拾艺局 shiyiju-admin 云端部署脚本"
echo "========================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 1. 检查环境
echo "📋 检查环境..."

if ! command -v tcb &> /dev/null; then
    echo -e "${RED}✗ CloudBase CLI 未安装${NC}"
    echo "  请运行: npm install -g @cloudbase/cli"
    exit 1
fi

if [ ! -f "shiyiju-admin-1.0.0-SNAPSHOT.jar" ]; then
    echo -e "${RED}✗ JAR文件不存在${NC}"
    echo "  请先运行: mvn clean package -DskipTests"
    exit 1
fi

echo -e "${GREEN}✓ 环境检查通过${NC}"
echo ""

# 2. 确认部署信息
echo "📦 部署信息:"
echo "  服务名称: shiyiju-admin"
echo "  JAR文件: shiyiju-admin-1.0.0-SNAPSHOT.jar"
echo "  文件大小: $(ls -lh shiyiju-admin-1.0.0-SNAPSHOT.jar | awk '{print $5}')"
echo "  环境ID: shiyiju-9g4pxbgh603bf08b"
echo ""

# 3. 部署
echo "🚀 开始部署..."
echo ""

# 使用tcb deploy命令
tcb cloudbase deploy . \
    --service-name shiyiju-admin \
    --env-id shiyiju-9g4pxbgh603bf08b \
    --force

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}========================================"
    echo -e "✅ 部署成功！"
    echo -e "========================================${NC}"
    echo ""
    echo "🌐 访问地址: https://shiyiju-admin-$ENV_ID"
    echo ""
else
    echo ""
    echo -e "${RED}========================================"
    echo -e "✗ 部署失败"
    echo -e "========================================${NC}"
    echo ""
    echo "💡 尝试以下方式部署:"
    echo "  1. 通过CloudBase控制台部署"
    echo "  2. 检查网络连接"
    echo "  3. 确认账户状态"
    echo ""
    echo "🌐 控制台地址: https://console.cloudbase.net/cloudrun"
    exit 1
fi
