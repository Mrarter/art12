#!/bin/bash
# 系统健康诊断脚本 - 排查"系统繁忙"问题
# 用法: bash system-diagnosis.sh

set -e

PROJECT_DIR="/Users/master/CodeBuddy/art12"
LOG_DIR="$PROJECT_DIR/backend/logs"
REPORT_FILE="$PROJECT_DIR/backend/scripts/diagnosis-report-$(date +%Y%m%d-%H%M%S).txt"

echo "========================================" > "$REPORT_FILE"
echo "  系统健康诊断报告" >> "$REPORT_FILE"
echo "  生成时间: $(date '+%Y-%m-%d %H:%M:%S')" >> "$REPORT_FILE"
echo "========================================" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# 1. 服务器资源负载
echo "【1. 服务器资源负载】" >> "$REPORT_FILE"
echo "----------------------------------------" >> "$REPORT_FILE"

# CPU 使用率
echo "--- CPU 使用率 ---" >> "$REPORT_FILE"
top -l 1 | head -5 >> "$REPORT_FILE" 2>/dev/null || echo "top 命令不可用" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# 内存使用
echo "--- 内存使用 ---" >> "$REPORT_FILE"
vm_stat >> "$REPORT_FILE" 2>/dev/null || echo "vm_stat 命令不可用" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# 磁盘 I/O
echo "--- 磁盘使用 ---" >> "$REPORT_FILE"
df -h >> "$REPORT_FILE" 2>/dev/null
echo "" >> "$REPORT_FILE"

# 网络连接数
echo "--- 网络连接数 ---" >> "$REPORT_FILE"
netstat -an | grep ESTABLISHED | wc -l | xargs -I {} echo "ESTABLISHED 连接数: {}" >> "$REPORT_FILE" 2>/dev/null || echo "netstat 命令不可用" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# 2. 服务端口状态
echo "【2. Java 服务端口状态】" >> "$REPORT_FILE"
echo "----------------------------------------" >> "$REPORT_FILE"
for port in 8080 8081 8082 8087 8090; do
    pid=$(lsof -ti :$port 2>/dev/null || true)
    if [ -n "$pid" ]; then
        echo "端口 $port: 运行中 (PID: $pid)" >> "$REPORT_FILE"
    else
        echo "端口 $port: 未运行 ⚠️" >> "$REPORT_FILE"
    fi
done
echo "" >> "$REPORT_FILE"

# 3. 数据库状态
echo "【3. 数据库状态】" >> "$REPORT_FILE"
echo "----------------------------------------" >> "$REPORT_FILE"
mysql -uroot -p123456 shiyiju_local -e "
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Threads_running';
SHOW STATUS LIKE 'Max_used_connections';
" >> "$REPORT_FILE" 2>/dev/null || echo "数据库连接失败 ⚠️" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# 慢查询（近10分钟）
echo "--- 慢查询日志 ---" >> "$REPORT_FILE"
mysql -uroot -p123456 shiyiju_local -e "
SELECT * FROM mysql.slow_log 
WHERE start_time > DATE_SUB(NOW(), INTERVAL 10 MINUTE) 
ORDER BY query_time DESC 
LIMIT 5;
" >> "$REPORT_FILE" 2>/dev/null || echo "无慢查询或 slow_log 未启用" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# 4. 应用日志异常分析
echo "【4. 应用日志异常分析】" >> "$REPORT_FILE"
echo "----------------------------------------" >> "$REPORT_FILE"

for logfile in product.log user.log gateway.log admin.log order.log; do
    if [ -f "$LOG_DIR/$logfile" ]; then
        echo "--- $logfile 最近10条错误 ---" >> "$REPORT_FILE"
        grep -E "ERROR|Exception" "$LOG_DIR/$logfile" 2>/dev/null | tail -5 >> "$REPORT_FILE" || echo "无 ERROR" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
    fi
done

# 统计各日志错误频率
echo "--- 近5分钟错误频率 ---" >> "$REPORT_FILE"
for logfile in product.log user.log gateway.log; do
    if [ -f "$LOG_DIR/$logfile" ]; then
        count=$(grep -c "ERROR" "$LOG_DIR/$logfile" 2>/dev/null || echo 0)
        echo "$logfile: $count 条错误" >> "$REPORT_FILE"
    fi
done
echo "" >> "$REPORT_FILE"

# 5. API 健康检查
echo "【5. API 健康检查】" >> "$REPORT_FILE"
echo "----------------------------------------" >> "$REPORT_FILE"

check_api() {
    local name=$1
    local url=$2
    local start_time=$(date +%s%N)
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null || echo "000")
    local end_time=$(date +%s%N)
    local duration=$(( (end_time - start_time) / 1000000 ))
    if [ "$http_code" = "200" ]; then
        echo "$name: HTTP $http_code, ${duration}ms ✅" >> "$REPORT_FILE"
    else
        echo "$name: HTTP $http_code, ${duration}ms ❌" >> "$REPORT_FILE"
    fi
}

check_api "Gateway" "http://localhost:8080/"
check_api "Product-List" "http://localhost:8082/product/list?page=1&pageSize=1"
check_api "Admin" "http://localhost:8090/admin/product/list?page=1&size=1"
echo "" >> "$REPORT_FILE"

# 6. 汇总
echo "【6. 诊断汇总】" >> "$REPORT_FILE"
echo "----------------------------------------" >> "$REPORT_FILE"

# 检查关键问题
issues=0

# 检查是否有服务未运行
for port in 8080 8082 8090; do
    if ! lsof -ti :$port > /dev/null 2>&1; then
        echo "❌ 端口 $port 服务未运行" >> "$REPORT_FILE"
        issues=$((issues + 1))
    fi
done

# 检查日志错误
total_errors=$(grep -c "ERROR" "$LOG_DIR"/*.log 2>/dev/null | awk -F: '{sum+=$2} END {print sum+0}')
if [ "$total_errors" -gt 10 ]; then
    echo "❌ 日志中累计 $total_errors 条错误，需排查" >> "$REPORT_FILE"
    issues=$((issues + 1))
fi

# 检查 artwork_id 相关错误
if grep -q "artwork_id.*doesn't have a default value" "$LOG_DIR/product.log" 2>/dev/null; then
    echo "❌ artwork_id 字段缺少默认值，发布作品会失败" >> "$REPORT_FILE"
    issues=$((issues + 1))
fi

if [ "$issues" -eq 0 ]; then
    echo "✅ 未发现明显问题" >> "$REPORT_FILE"
else
    echo "⚠️ 发现 $issues 个问题，请查看详细日志" >> "$REPORT_FILE"
fi

echo "" >> "$REPORT_FILE"
echo "========================================" >> "$REPORT_FILE"
echo "诊断完成，报告保存至: $REPORT_FILE" >> "$REPORT_FILE"
echo "========================================" >> "$REPORT_FILE"

cat "$REPORT_FILE"
