#!/bin/bash

# ===========================================
# shiyiju-art12 本地部署脚本
# ===========================================

BASE_DIR="/Users/master/CodeBuddy/art12/backend"
LOGS_DIR="/Users/master/CodeBuddy/art12/logs"
DEPLOY_DIR="$BASE_DIR/deploy-local"

# 创建日志目录
mkdir -p "$LOGS_DIR"

# 服务配置: 服务名 JAR文件 端口
declare -A SERVICES=(
    ["gateway"]="shiyiju-gateway-1.0.0-SNAPSHOT.jar:8080"
    ["user"]="shiyiju-user-1.0.0-SNAPSHOT.jar:8081"
    ["product"]="shiyiju-product-1.0.0-SNAPSHOT.jar:8082"
    ["order"]="shiyiju-order-1.0.0-SNAPSHOT.jar:8083"
    ["auction"]="shiyiju-auction-1.0.0-SNAPSHOT.jar:8084"
    ["promotion"]="shiyiju-promotion-1.0.0-SNAPSHOT.jar:8085"
    ["community"]="shiyiju-community-1.0.0-SNAPSHOT.jar:8086"
    ["file"]="shiyiju-file-1.0.0-SNAPSHOT.jar:8087"
    ["message"]="shiyiju-message-1.0.0-SNAPSHOT.jar:8088"
    ["admin"]="shiyiju-admin-1.0.0-SNAPSHOT.jar:8090"
)

# JAR 路径映射
JAR_PATHS=(
    "$BASE_DIR/shiyiju-gateway/target/shiyiju-gateway-1.0.0-SNAPSHOT.jar"
    "$BASE_DIR/shiyiju-user/target/shiyiju-user-1.0.0-SNAPSHOT.jar"
    "$BASE_DIR/shiyiju-product/target/shiyiju-product-1.0.0-SNAPSHOT.jar"
    "$BASE_DIR/shiyiju-order/target/shiyiju-order-1.0.0-SNAPSHOT.jar"
    "$BASE_DIR/shiyiju-auction/target/shiyiju-auction-1.0.0-SNAPSHOT.jar"
    "$BASE_DIR/shiyiju-promotion/target/shiyiju-promotion-1.0.0-SNAPSHOT.jar"
    "$BASE_DIR/shiyiju-community/target/shiyiju-community-1.0.0-SNAPSHOT.jar"
    "$BASE_DIR/shiyiju-file/target/shiyiju-file-1.0.0-SNAPSHOT.jar"
    "$BASE_DIR/shiyiju-message/target/shiyiju-message-1.0.0-SNAPSHOT.jar"
    "$BASE_DIR/shiyiju-admin/target/shiyiju-admin-1.0.0-SNAPSHOT.jar"
)

# 停止所有服务
stop_all() {
    echo "🛑 停止所有服务..."
    for service in gateway user product order auction promotion community file message admin; do
        pid=$(lsof -ti:$(( ${service}_port )) 2>/dev/null || true)
        if [ -n "$pid" ]; then
            kill $pid 2>/dev/null && echo "  ✓ $service 已停止" || true
        fi
    done
    pkill -f "shiyiju-.*\.jar" 2>/dev/null || true
    echo "✅ 所有服务已停止"
}

# 启动单个服务
start_service() {
    local name=$1
    local jar=$2
    local port=$3
    
    # user 服务需要使用 -exec.jar
    local jar_name="shiyiju-$name-1.0.0-SNAPSHOT.jar"
    if [ "$name" == "user" ]; then
        jar_name="shiyiju-$name-1.0.0-SNAPSHOT-exec.jar"
    fi
    
    # 查找 JAR 文件
    local jar_path="$BASE_DIR/shiyiju-$name/target/$jar_name"
    
    if [ ! -f "$jar_path" ]; then
        echo "  ❌ $name: JAR 文件未找到 ($jar_path)"
        return 1
    fi
    
    echo "🚀 启动 $name (端口: $port)..."
    nohup java -jar "$jar_path" --spring.profiles.active=local > "$LOGS_DIR/$name.log" 2>&1 &
    echo "  ✓ $name 已启动 (PID: $!)，日志: $LOGS_DIR/$name.log"
}

# 启动所有服务
start_all() {
    echo "=========================================="
    echo "🚀 启动 shiyiju-art12 所有服务"
    echo "=========================================="
    
    # 按依赖顺序启动
    start_service "user"     "shiyiju-user-1.0.0-SNAPSHOT.jar"     8081 &
    start_service "product"  "shiyiju-product-1.0.0-SNAPSHOT.jar"  8082 &
    start_service "order"    "shiyiju-order-1.0.0-SNAPSHOT.jar"    8083 &
    start_service "auction"  "shiyiju-auction-1.0.0-SNAPSHOT.jar"  8084 &
    start_service "promotion" "shiyiju-promotion-1.0.0-SNAPSHOT.jar" 8085 &
    start_service "community" "shiyiju-community-1.0.0-SNAPSHOT.jar" 8086 &
    start_service "file"     "shiyiju-file-1.0.0-SNAPSHOT.jar"     8087 &
    start_service "message"  "shiyiju-message-1.0.0-SNAPSHOT.jar"  8088 &
    start_service "admin"    "shiyiju-admin-1.0.0-SNAPSHOT.jar"    8090 &
    start_service "gateway"  "shiyiju-gateway-1.0.0-SNAPSHOT.jar" 8080 &
    
    wait
    
    echo ""
    echo "=========================================="
    echo "✅ 所有服务已启动"
    echo "=========================================="
    echo "📍 Gateway:    http://localhost:8080"
    echo "📍 User:      http://localhost:8081"
    echo "📍 Product:   http://localhost:8082"
    echo "📍 Order:     http://localhost:8083"
    echo "📍 Auction:   http://localhost:8084"
    echo "📍 Promotion: http://localhost:8085"
    echo "📍 Community: http://localhost:8086"
    echo "📍 File:      http://localhost:8087"
    echo "📍 Message:   http://localhost:8088"
    echo "📍 Admin:     http://localhost:8090"
    echo ""
    echo "📋 日志目录: $LOGS_DIR"
}

# 查看状态
status() {
    echo "=========================================="
    echo "📊 服务状态"
    echo "=========================================="
    
    local services=("8080:gateway" "8081:user" "8082:product" "8083:order" "8084:auction" "8085:promotion" "8086:community" "8087:file" "8088:message" "8090:admin")
    
    for s in "${services[@]}"; do
        port=${s%%:*}
        name=${s#*:}
        if lsof -i:$port >/dev/null 2>&1; then
            pid=$(lsof -ti:$port)
            echo "  ✅ $name (端口 $port) - PID: $pid"
        else
            echo "  ❌ $name (端口 $port) - 未运行"
        fi
    done
}

# 查看日志
logs() {
    local service=$1
    if [ -z "$service" ]; then
        echo "📋 可用日志:"
        ls -la "$LOGS_DIR"/*.log 2>/dev/null || echo "  无日志文件"
        return
    fi
    
    if [ -f "$LOGS_DIR/$service.log" ]; then
        tail -100 "$LOGS_DIR/$service.log"
    else
        echo "❌ 日志文件不存在: $LOGS_DIR/$service.log"
    fi
}

# 主命令
case "${1:-start}" in
    start)
        stop_all
        sleep 2
        start_all
        ;;
    stop)
        stop_all
        ;;
    restart)
        stop_all
        sleep 2
        start_all
        ;;
    status)
        status
        ;;
    logs)
        logs "$2"
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status|logs [服务名]}"
        echo ""
        echo "示例:"
        echo "  $0 start       # 启动所有服务"
        echo "  $0 stop       # 停止所有服务"
        echo "  $0 restart    # 重启所有服务"
        echo "  $0 status     # 查看服务状态"
        echo "  $0 logs       # 查看所有日志"
        echo "  $0 logs user  # 查看 user 服务日志"
        exit 1
        ;;
esac
