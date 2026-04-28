#!/bin/bash
# 拾艺局 - Mac Mini 部署脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/../.."

echo "=========================================="
echo "  拾艺局 - Mac Mini 一键部署"
echo "=========================================="

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker Desktop"
    exit 1
fi

if ! docker info &> /dev/null; then
    echo "❌ Docker 未运行，请启动 Docker Desktop"
    exit 1
fi

echo "✅ Docker 环境正常"

# 显示菜单
show_menu() {
    echo ""
    echo "请选择操作："
    echo "1. 启动所有服务"
    echo "2. 停止所有服务"
    echo "3. 重启所有服务"
    echo "4. 查看服务状态"
    echo "5. 查看日志"
    echo "6. 重新构建并启动"
    echo "0. 退出"
    echo ""
    read -p "请输入选项: " choice
}

start_services() {
    echo ""
    echo "🚀 正在启动服务..."
    cd deploy/docker
    docker-compose -f docker-compose.local.yml up -d
    
    echo ""
    echo "⏳ 等待服务启动..."
    sleep 10
    
    echo ""
    echo "📊 服务状态："
    docker-compose -f docker-compose.local.yml ps
    
    echo ""
    echo "✅ 部署完成！"
    echo ""
    echo "访问地址："
    echo "  - 小程序前端: http://localhost:5173"
    echo "  - 管理后台:   http://localhost:5174"
    echo "  - API网关:    http://localhost:8080"
    echo "  - 管理API:    http://localhost:8090"
}

stop_services() {
    echo ""
    echo "🛑 正在停止服务..."
    cd deploy/docker
    docker-compose -f docker-compose.local.yml down
    echo "✅ 服务已停止"
}

restart_services() {
    stop_services
    sleep 2
    start_services
}

show_status() {
    echo ""
    echo "📊 服务状态："
    cd deploy/docker
    docker-compose -f docker-compose.local.yml ps
}

show_logs() {
    echo ""
    echo "📝 查看日志 (Ctrl+C 退出)"
    echo ""
    echo "选择服务："
    echo "1. gateway   2. user      3. product   4. order"
    echo "5. auction  6. promotion 7. community 8. file"
    echo "9. message  10. admin    11. frontend  12. admin-frontend"
    echo "0. 返回"
    read -p "请输入选项: " log_choice
    
    cd deploy/docker
    case $log_choice in
        1) docker-compose -f docker-compose.local.yml logs -f gateway ;;
        2) docker-compose -f docker-compose.local.yml logs -f user ;;
        3) docker-compose -f docker-compose.local.yml logs -f product ;;
        4) docker-compose -f docker-compose.local.yml logs -f order ;;
        5) docker-compose -f docker-compose.local.yml logs -f auction ;;
        6) docker-compose -f docker-compose.local.yml logs -f promotion ;;
        7) docker-compose -f docker-compose.local.yml logs -f community ;;
        8) docker-compose -f docker-compose.local.yml logs -f file ;;
        9) docker-compose -f docker-compose.local.yml logs -f message ;;
        10) docker-compose -f docker-compose.local.yml logs -f admin ;;
        11) docker-compose -f docker-compose.local.yml logs -f frontend ;;
        12) docker-compose -f docker-compose.local.yml logs -f admin-frontend ;;
        0) ;;
    esac
}

rebuild_services() {
    echo ""
    echo "🔨 正在重新构建..."
    cd deploy/docker
    docker-compose -f docker-compose.local.yml down
    docker-compose -f docker-compose.local.yml build --no-cache
    docker-compose -f docker-compose.local.yml up -d
    echo "✅ 重建完成"
}

# 主循环
while true; do
    show_menu
    case $choice in
        1) start_services ;;
        2) stop_services ;;
        3) restart_services ;;
        4) show_status ;;
        5) show_logs ;;
        6) rebuild_services ;;
        0) echo "再见！"; exit 0 ;;
        *) echo "无效选项，请重新输入" ;;
    esac
done
