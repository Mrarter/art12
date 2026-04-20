#!/bin/bash
# ============================================
# 拾艺局 - 一键构建脚本
# 构建后端 + 前端 + 微信小程序
# ============================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_ROOT="/Users/master/CodeBuddy/art12"
cd "$PROJECT_ROOT"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}    拾艺局 - 一键构建${NC}"
echo -e "${BLUE}========================================${NC}"

# 检查参数
BUILD_TARGET=${1:-all}
echo -e "${YELLOW}构建目标: $BUILD_TARGET${NC}"
echo ""

# 构建后端
build_backend() {
    echo -e "${YELLOW}[1/3] 构建后端微服务...${NC}"
    cd "$PROJECT_ROOT/backend"
    
    if [ -f "mvnw" ]; then
        MVN="./mvnw"
    else
        MVN="mvn"
    fi
    
    echo "执行: $MVN clean package -DskipTests"
    $MVN clean package -DskipTests -q
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}  ✓ 后端构建成功${NC}"
    else
        echo -e "${RED}  ✗ 后端构建失败${NC}"
        exit 1
    fi
}

# 构建前端 H5
build_frontend() {
    echo -e "${YELLOW}[2/3] 构建小程序前端 (H5)...${NC}"
    cd "$PROJECT_ROOT/frontend"
    
    npm run build:h5
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}  ✓ H5 前端构建成功 (dist/)${NC}"
    else
        echo -e "${RED}  ✗ H5 前端构建失败${NC}"
        exit 1
    fi
}

# 构建微信小程序
build_mp_weixin() {
    echo -e "${YELLOW}[3/3] 构建微信小程序...${NC}"
    cd "$PROJECT_ROOT/frontend"
    
    npm run build:mp-weixin
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}  ✓ 微信小程序构建成功 (dist/build/mp-weixin/)${NC}"
    else
        echo -e "${RED}  ✗ 微信小程序构建失败${NC}"
        exit 1
    fi
}

# 构建管理后台
build_admin() {
    echo -e "${YELLOW}    构建管理后台前端...${NC}"
    cd "$PROJECT_ROOT/admin"
    
    npm run build
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}  ✓ 管理后台构建成功${NC}"
    else
        echo -e "${RED}  ✗ 管理后台构建失败${NC}"
        exit 1
    fi
}

# 根据目标执行构建
case $BUILD_TARGET in
    "backend")
        build_backend
        ;;
    "frontend")
        build_frontend
        build_mp_weixin
        build_admin
        ;;
    "all")
        build_backend
        build_frontend
        build_mp_weixin
        build_admin
        ;;
    *)
        echo "用法: ./build.sh [backend|frontend|all]"
        echo "  backend  - 仅构建后端"
        echo "  frontend - 仅构建前端 (H5 + 微信小程序)"
        echo "  all      - 构建全部 (默认)"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}    构建完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "构建产物:"
echo "  - 后端: backend/*/target/*-1.0.0-SNAPSHOT.jar"
echo "  - H5前端: frontend/dist/"
echo "  - 微信小程序: frontend/dist/build/mp-weixin/"
echo "  - 管理后台: admin/dist/"
echo ""