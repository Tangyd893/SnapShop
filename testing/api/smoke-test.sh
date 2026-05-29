#!/usr/bin/env bash
# =============================================================================
# SnapShop 秒杀平台 - 核心链路冒烟测试脚本
# 测试流程：注册 → 登录 → 查商品 → 获取秒杀令牌 → 提交秒杀 → 查询秒杀结果
#           → 查询订单 → 创建支付 → 模拟支付 → 查询支付状态
# 返回码：0=全部通过，1=存在失败
# =============================================================================

set -euo pipefail

# -------------------- 配置 --------------------
BASE_URL="${BASE_URL:-http://localhost:8080}"
REPORT_FILE="${REPORT_FILE:-testing/reports/smoke-$(date +%Y%m%d-%H%M%S).txt}"
USERNAME="smoke_$(date +%s)"
PASSWORD="123456"
PHONE="138$(printf '%08d' $((RANDOM % 100000000)))"
ACCESS_TOKEN=""
USER_ID=""
PRODUCT_ID=""
SKU_ID=""
ACTIVITY_ID=""
REQUEST_ID=""
SECKILL_TOKEN=""
ORDER_ID=""
PAYMENT_ID=""
MAX_POLL_RETRY=15
POLL_INTERVAL=2
PASS_COUNT=0
FAIL_COUNT=0
TOTAL_COUNT=0

# -------------------- 颜色输出 --------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# -------------------- 初始化报告 --------------------
mkdir -p "$(dirname "$REPORT_FILE")"
{
    echo "========================================="
    echo "SnapShop 冒烟测试报告"
    echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "目标: $BASE_URL"
    echo "========================================="
} > "$REPORT_FILE"

print_step() {
    echo -e "\n${CYAN}============================================================${NC}"
    echo -e "${CYAN}[步骤] $1${NC}"
    echo -e "${CYAN}============================================================${NC}"
    echo "" >> "$REPORT_FILE"
    echo "=== $1 ===" >> "$REPORT_FILE"
}

print_request() {
    echo -e "${YELLOW}[请求] $1${NC}"
}

print_response() {
    echo -e "${GREEN}[响应] HTTP $1${NC}"
    echo "$2" | head -5
}

print_error() {
    echo -e "${RED}[错误] $1${NC}"
}

record_result() {
    local step="$1"
    local status="$2"
    local detail="${3:-}"
    TOTAL_COUNT=$((TOTAL_COUNT + 1))
    if [ "$status" = "PASS" ]; then
        PASS_COUNT=$((PASS_COUNT + 1))
        echo -e "${GREEN}[PASS]${NC} $step"
        echo "[PASS] $step" >> "$REPORT_FILE"
    else
        FAIL_COUNT=$((FAIL_COUNT + 1))
        echo -e "${RED}[FAIL]${NC} $step: $detail"
        echo "[FAIL] $step: $detail" >> "$REPORT_FILE"
    fi
}

# -------------------- JSON 解析辅助函数 --------------------
json_get() {
    python3 -c "import sys,json; print(json.load(sys.stdin).get('$1',''))" 2>/dev/null || echo ""
}

json_get_data() {
    python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('$1',''))" 2>/dev/null || echo ""
}

json_get_data_list_first() {
    python3 -c "
import sys,json
data=json.load(sys.stdin).get('data',{})
records=data.get('records',[])
if records:
    print(records[0].get('$1',''))
" 2>/dev/null || echo ""
}

# -------------------- 执行函数 --------------------
do_curl() {
    local method="$1"
    local url="$2"
    local data="${3:-}"
    local extra_args="${4:-}"

    local response
    local http_code
    local body

    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            $extra_args \
            -d "$data" 2>&1)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            $extra_args 2>&1)
    fi

    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    print_response "$http_code" "$(echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body")"
    echo "$body"
    echo "HTTP $http_code: $(echo "$body" | head -1)" >> "$REPORT_FILE"
}

# =============================================================================
# 测试步骤
# =============================================================================

# -------------------- 1. 用户注册 --------------------
print_step "1. 用户注册"
print_request "POST /api/auth/register"
REGISTER_BODY=$(cat <<EOF
{
    "username": "${USERNAME}",
    "password": "${PASSWORD}",
    "phone": "${PHONE}"
}
EOF
)
REGISTER_RESP=$(do_curl "POST" "${BASE_URL}/api/auth/register" "$REGISTER_BODY")
REGISTER_CODE=$(echo "$REGISTER_RESP" | json_get "code")
if [ "$REGISTER_CODE" = "0" ]; then
    USER_ID=$(echo "$REGISTER_RESP" | json_get_data "userId")
    record_result "用户注册" "PASS"
else
    record_result "用户注册" "FAIL" "code=$REGISTER_CODE"
fi

# -------------------- 2. 用户登录 --------------------
print_step "2. 用户登录"
print_request "POST /api/auth/login"
LOGIN_BODY=$(cat <<EOF
{
    "account": "${USERNAME}",
    "password": "${PASSWORD}"
}
EOF
)
LOGIN_RESP=$(do_curl "POST" "${BASE_URL}/api/auth/login" "$LOGIN_BODY")
LOGIN_CODE=$(echo "$LOGIN_RESP" | json_get "code")
if [ "$LOGIN_CODE" = "0" ]; then
    ACCESS_TOKEN=$(echo "$LOGIN_RESP" | json_get_data "accessToken")
    if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
        record_result "用户登录" "PASS"
    else
        record_result "用户登录" "FAIL" "未获取到accessToken"
    fi
else
    record_result "用户登录" "FAIL" "code=$LOGIN_CODE"
fi

# -------------------- 3. 查询商品列表 --------------------
print_step "3. 查询商品列表"
print_request "GET /api/products?pageNo=1&pageSize=5"
LIST_RESP=$(do_curl "GET" "${BASE_URL}/api/products?pageNo=1&pageSize=5")
LIST_CODE=$(echo "$LIST_RESP" | json_get "code")
if [ "$LIST_CODE" = "0" ]; then
    PRODUCT_ID=$(echo "$LIST_RESP" | json_get_data_list_first "productId")
    SKU_ID=$(echo "$LIST_RESP" | json_get_data_list_first "skuId")
    if [ -n "$PRODUCT_ID" ] && [ "$PRODUCT_ID" != "null" ]; then
        record_result "查询商品列表" "PASS"
    else
        record_result "查询商品列表" "FAIL" "无商品数据"
    fi
else
    record_result "查询商品列表" "FAIL" "code=$LIST_CODE"
fi

# -------------------- 4. 查询秒杀活动 --------------------
print_step "4. 查询秒杀活动列表"
print_request "GET /api/seckill/activities?pageNo=1&pageSize=5"
ACTIVITY_RESP=$(do_curl "GET" "${BASE_URL}/api/seckill/activities?pageNo=1&pageSize=5")
ACTIVITY_CODE=$(echo "$ACTIVITY_RESP" | json_get "code")
if [ "$ACTIVITY_CODE" = "0" ]; then
    ACTIVITY_ID=$(echo "$ACTIVITY_RESP" | json_get_data_list_first "activityId")
    if [ -n "$ACTIVITY_ID" ] && [ "$ACTIVITY_ID" != "null" ]; then
        record_result "查询秒杀活动" "PASS"
        # 获取秒杀商品SKU
        print_request "GET /api/seckill/activities/${ACTIVITY_ID}"
        DETAIL_RESP=$(do_curl "GET" "${BASE_URL}/api/seckill/activities/${ACTIVITY_ID}")
        ITEM_SKU=$(echo "$DETAIL_RESP" | json_get_data_list_first "skuId")
        if [ -n "$ITEM_SKU" ] && [ "$ITEM_SKU" != "null" ]; then
            SKU_ID="$ITEM_SKU"
        fi
    else
        record_result "查询秒杀活动" "FAIL" "无活动数据"
    fi
else
    record_result "查询秒杀活动" "FAIL" "code=$ACTIVITY_CODE"
fi

# -------------------- 5. 获取秒杀令牌 --------------------
if [ -n "$ACTIVITY_ID" ] && [ "$ACTIVITY_ID" != "null" ] && \
   [ -n "$SKU_ID" ] && [ "$SKU_ID" != "null" ] && \
   [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then

    print_step "5. 获取秒杀令牌"
    REQUEST_ID="REQ_$(date +%s)_$((RANDOM % 10000))"
    print_request "POST /api/seckill/activities/${ACTIVITY_ID}/items/${SKU_ID}/token"
    TOKEN_BODY=$(cat <<EOF
{
    "requestId": "${REQUEST_ID}"
}
EOF
    )
    TOKEN_RESP=$(do_curl "POST" "${BASE_URL}/api/seckill/activities/${ACTIVITY_ID}/items/${SKU_ID}/token" "$TOKEN_BODY")
    TOKEN_CODE=$(echo "$TOKEN_RESP" | json_get "code")
    if [ "$TOKEN_CODE" = "0" ]; then
        SECKILL_TOKEN=$(echo "$TOKEN_RESP" | json_get_data "seckillToken")
        if [ -n "$SECKILL_TOKEN" ] && [ "$SECKILL_TOKEN" != "null" ]; then
            record_result "获取秒杀令牌" "PASS"
        else
            record_result "获取秒杀令牌" "FAIL" "未获取到令牌"
        fi
    else
        record_result "获取秒杀令牌" "FAIL" "code=$TOKEN_CODE"
    fi
else
    print_step "5. 获取秒杀令牌（跳过）"
    record_result "获取秒杀令牌" "FAIL" "缺少必要参数"
fi

# -------------------- 6. 提交秒杀 --------------------
if [ -n "$SECKILL_TOKEN" ] && [ "$SECKILL_TOKEN" != "null" ]; then
    print_step "6. 提交秒杀请求"
    SUBMIT_REQUEST_ID="REQ_SUBMIT_$(date +%s)_$((RANDOM % 10000))"
    print_request "POST /api/seckill/activities/${ACTIVITY_ID}/items/${SKU_ID}/submit"
    SUBMIT_BODY=$(cat <<EOF
{
    "seckillToken": "${SECKILL_TOKEN}",
    "quantity": 1
}
EOF
    )
    SUBMIT_RESP=$(do_curl "POST" \
        "${BASE_URL}/api/seckill/activities/${ACTIVITY_ID}/items/${SKU_ID}/submit" \
        "$SUBMIT_BODY" \
        "-H 'X-Request-Id: ${SUBMIT_REQUEST_ID}'")
    SUBMIT_CODE=$(echo "$SUBMIT_RESP" | json_get "code")
    if [ "$SUBMIT_CODE" = "0" ]; then
        record_result "提交秒杀" "PASS"
    else
        record_result "提交秒杀" "FAIL" "code=$SUBMIT_CODE"
    fi

    # -------------------- 7. 查询秒杀结果 --------------------
    print_step "7. 查询秒杀结果（轮询）"
    FINAL_STATUS=""
    for i in $(seq 1 $MAX_POLL_RETRY); do
        print_request "GET /api/seckill/results/${SUBMIT_REQUEST_ID}"
        RESULT_RESP=$(do_curl "GET" "${BASE_URL}/api/seckill/results/${SUBMIT_REQUEST_ID}")
        FINAL_STATUS=$(echo "$RESULT_RESP" | json_get_data "resultStatus")
        ORDER_ID=$(echo "$RESULT_RESP" | json_get_data "orderId")

        if [ "$FINAL_STATUS" != "排队中" ] && [ "$FINAL_STATUS" != "null" ] && [ -n "$FINAL_STATUS" ]; then
            break
        fi
        [ "$i" -lt "$MAX_POLL_RETRY" ] && sleep "$POLL_INTERVAL"
    done

    if [ "$FINAL_STATUS" = "秒杀成功" ] && [ -n "$ORDER_ID" ] && [ "$ORDER_ID" != "null" ]; then
        record_result "查询秒杀结果" "PASS"
    else
        record_result "查询秒杀结果" "FAIL" "status=$FINAL_STATUS"
    fi
else
    print_step "6-7. 提交秒杀/查询结果（跳过）"
    record_result "提交秒杀" "FAIL" "缺少秒杀令牌"
    record_result "查询秒杀结果" "FAIL" "未提交秒杀"
fi

# -------------------- 8. 查询订单 --------------------
print_step "8. 查询订单"
if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
    print_request "GET /api/orders?pageNo=1&pageSize=10"
    ORDERS_RESP=$(do_curl "GET" "${BASE_URL}/api/orders?pageNo=1&pageSize=10")
    ORDERS_CODE=$(echo "$ORDERS_RESP" | json_get "code")
    if [ "$ORDERS_CODE" = "0" ]; then
        # 如果有秒杀订单，使用它；否则取第一个
        if [ -z "$ORDER_ID" ] || [ "$ORDER_ID" = "null" ]; then
            ORDER_ID=$(echo "$ORDERS_RESP" | json_get_data_list_first "orderId")
        fi
        if [ -n "$ORDER_ID" ] && [ "$ORDER_ID" != "null" ]; then
            record_result "查询订单列表" "PASS"
            # 查询订单详情
            print_request "GET /api/orders/${ORDER_ID}"
            do_curl "GET" "${BASE_URL}/api/orders/${ORDER_ID}"
        else
            record_result "查询订单列表" "FAIL" "无订单数据"
        fi
    else
        record_result "查询订单列表" "FAIL" "code=$ORDERS_CODE"
    fi
else
    record_result "查询订单列表" "FAIL" "未登录"
fi

# -------------------- 9. 创建支付单 --------------------
if [ -n "$ORDER_ID" ] && [ "$ORDER_ID" != "null" ]; then
    print_step "9. 创建支付单"
    print_request "POST /api/payments/orders/${ORDER_ID}"
    PAYMENT_RESP=$(do_curl "POST" "${BASE_URL}/api/payments/orders/${ORDER_ID}")
    PAYMENT_CODE=$(echo "$PAYMENT_RESP" | json_get "code")
    if [ "$PAYMENT_CODE" = "0" ]; then
        PAYMENT_ID=$(echo "$PAYMENT_RESP" | json_get_data "paymentId")
        record_result "创建支付单" "PASS"
    else
        record_result "创建支付单" "FAIL" "code=$PAYMENT_CODE"
    fi

    # -------------------- 10. 模拟支付 --------------------
    print_step "10. 模拟支付"
    print_request "POST /api/payments/${PAYMENT_ID}/pay"
    PAY_RESP=$(do_curl "POST" "${BASE_URL}/api/payments/${PAYMENT_ID}/pay")
    PAY_CODE=$(echo "$PAY_RESP" | json_get "code")
    if [ "$PAY_CODE" = "0" ]; then
        record_result "模拟支付" "PASS"
    else
        record_result "模拟支付" "FAIL" "code=$PAY_CODE"
    fi

    # -------------------- 11. 查询支付状态 --------------------
    print_step "11. 查询支付状态"
    print_request "GET /api/payments/orders/${ORDER_ID}"
    PAY_STATUS_RESP=$(do_curl "GET" "${BASE_URL}/api/payments/orders/${ORDER_ID}")
    PAY_STATUS_CODE=$(echo "$PAY_STATUS_RESP" | json_get "code")
    if [ "$PAY_STATUS_CODE" = "0" ]; then
        PAY_STATUS=$(echo "$PAY_STATUS_RESP" | json_get_data "status")
        if [ "$PAY_STATUS" = "SUCCESS" ] || [ "$PAY_STATUS" = "PAID" ]; then
            record_result "查询支付状态" "PASS"
        else
            record_result "查询支付状态" "FAIL" "status=$PAY_STATUS"
        fi
    else
        record_result "查询支付状态" "FAIL" "code=$PAY_STATUS_CODE"
    fi
else
    print_step "9-11. 支付流程（跳过）"
    record_result "创建支付单" "FAIL" "无订单ID"
    record_result "模拟支付" "FAIL" "无支付单"
    record_result "查询支付状态" "FAIL" "无支付单"
fi

# =============================================================================
# 测试总结
# =============================================================================
print_step "测试总结"

{
    echo ""
    echo "========================================="
    echo "测试结果汇总"
    echo "========================================="
    echo "通过: $PASS_COUNT / $TOTAL_COUNT"
    echo "失败: $FAIL_COUNT / $TOTAL_COUNT"
    echo ""
    echo "用户: $USERNAME"
    echo "订单: ${ORDER_ID:-无}"
    echo "========================================="
} >> "$REPORT_FILE"

echo -e "${CYAN}============================================================${NC}"
echo -e "${CYAN}测试结果汇总:${NC}"
echo -e "  通过: ${GREEN}${PASS_COUNT}${NC} / ${TOTAL_COUNT}"
echo -e "  失败: ${RED}${FAIL_COUNT}${NC} / ${TOTAL_COUNT}"
echo -e "  用户: ${USERNAME}"
echo -e "  订单: ${ORDER_ID:-无}"
echo -e "  报告: ${REPORT_FILE}"
echo -e "${CYAN}============================================================${NC}"

# 返回码
if [ "$FAIL_COUNT" -gt 0 ]; then
    exit 1
else
    exit 0
fi
