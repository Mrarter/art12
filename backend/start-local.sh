#!/bin/bash

# ===========================================
# shiyiju-art12 本地部署脚本
# ===========================================

BASE_DIR="/Users/master/CodeBuddy/art12/backend"
LOGS_DIR="/Users/master/CodeBuddy/art12/logs"
DEPLOY_DIR="$BASE_DIR/deploy-local"

# 创建日志目录
mkdir -p "$LOGS_DIR"

# ===========================================
# 加载环境变量配置
# 优先级：.env (gitignored, 真实密钥) > .env.local (git-tracked, 模板)
# ===========================================
load_env_file() {
    local env_file="$1"
    if [ -f "$env_file" ]; then
        echo "  📄 加载环境变量: $env_file"
        set -a  # 自动导出后续所有变量
        source "$env_file"
        set +a
    fi
}

# 先加载 .env.local 模板，再加载 .env 覆盖（避免默认值被覆盖）
load_env_file "$BASE_DIR/../.env.local"
load_env_file "$BASE_DIR/../.env"

# 服务端口映射: "服务名:端口"
readonly SERVICES=(
    "gateway:8080"
    "user:8081"
    "product:8082"
    "order:8083"
    "auction:8084"
    "promotion:8085"
    "community:8086"
    "file:8087"
    "message:8088"
    "admin:8090"
)

# 从服务名获取端口号
get_port() {
    local target="$1"
    for entry in "${SERVICES[@]}"; do
        local name="${entry%%:*}"
        local port="${entry#*:}"
        if [ "$name" = "$target" ]; then
            echo "$port"
            return 0
        fi
    done
    return 1
}

# 停止所有服务
stop_all() {
    echo "🛑 停止所有服务..."
    for entry in "${SERVICES[@]}"; do
        local name="${entry%%:*}"
        local port="${entry#*:}"
        local pid=""
        pid=$(lsof -ti:"$port" 2>/dev/null || true)
        if [ -n "$pid" ]; then
            kill "$pid" 2>/dev/null && echo "  ✓ $name 已停止" || true
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
    for entry in "${SERVICES[@]}"; do
        local name="${entry%%:*}"
        local port="${entry#*:}"
        printf "📍 %-10s http://localhost:%s\n" "$name" "$port"
    done
    echo ""
    echo "📋 日志目录: $LOGS_DIR"
}

# 查看状态
status() {
    echo "=========================================="
    echo "📊 服务状态"
    echo "=========================================="
    
    for entry in "${SERVICES[@]}"; do
        local name="${entry%%:*}"
        local port="${entry#*:}"
        if lsof -i:"$port" >/dev/null 2>&1; then
            local pid
            pid=$(lsof -ti:"$port")
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
