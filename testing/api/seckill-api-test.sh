#!/usr/bin/env bash
# =============================================================================
# SnapShop 秒杀平台 - 核心接口测试脚本
# 测试流程：注册 → 登录 → 查商品列表 → 查商品详情 → 查秒杀活动 → 
#           获取秒杀令牌 → 提交秒杀 → 查询秒杀结果 → 查订单
# =============================================================================

set -euo pipefail

# -------------------- 配置 --------------------
BASE_URL="http://localhost:8080"
USERNAME="testuser_$(date +%s)"
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
MAX_POLL_RETRY=15
POLL_INTERVAL=2

# -------------------- 颜色输出 --------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_step() {
    echo -e "\n${CYAN}============================================================${NC}"
    echo -e "${CYAN}[步骤] $1${NC}"
    echo -e "${CYAN}============================================================${NC}"
}

print_request() {
    echo -e "${YELLOW}[请求] $1${NC}"
    echo -e "${YELLOW}curl $2${NC}"
}

print_response() {
    echo -e "${GREEN}[响应] HTTP $1${NC}"
    echo -e "${GREEN}$2${NC}"
}

print_error() {
    echo -e "${RED}[错误] $1${NC}"
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
}

# =============================================================================
# 测试步骤
# =============================================================================

# -------------------- 1. 用户注册 --------------------
print_step "1. 用户注册"
print_request "POST" "${BASE_URL}/api/auth/register"
REGISTER_BODY=$(cat <<EOF
{
    "username": "${USERNAME}",
    "password": "${PASSWORD}",
    "phone": "${PHONE}"
}
EOF
)
REGISTER_RESP=$(do_curl "POST" "${BASE_URL}/api/auth/register" "$REGISTER_BODY")
USER_ID=$(echo "$REGISTER_RESP" | json_get_data "userId")
echo "获取到 userId: ${USER_ID:-未知}"

# -------------------- 2. 用户登录 --------------------
print_step "2. 用户登录"
print_request "POST" "${BASE_URL}/api/auth/login"
LOGIN_BODY=$(cat <<EOF
{
    "account": "${USERNAME}",
    "password": "${PASSWORD}"
}
EOF
)
LOGIN_RESP=$(do_curl "POST" "${BASE_URL}/api/auth/login" "$LOGIN_BODY")
ACCESS_TOKEN=$(echo "$LOGIN_RESP" | json_get_data "accessToken")
if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
    print_error "登录失败，未能获取 accessToken，后续接口可能无法正常测试"
else
    echo "获取到 accessToken: ${ACCESS_TOKEN:0:20}..."
fi

# -------------------- 3. 查询商品列表 --------------------
print_step "3. 查询商品列表"
print_request "GET" "${BASE_URL}/api/products?pageNo=1&pageSize=5"

LIST_RESP=$(do_curl "GET" "${BASE_URL}/api/products?pageNo=1&pageSize=5")
PRODUCT_ID=$(echo "$LIST_RESP" | json_get_data_list_first "productId")
SKU_ID=$(echo "$LIST_RESP" | json_get_data_list_first "skuId")
echo "获取到 productId: ${PRODUCT_ID:-未知}, skuId: ${SKU_ID:-未知}"

# -------------------- 4. 查询商品详情 --------------------
if [ -n "$PRODUCT_ID" ] && [ "$PRODUCT_ID" != "null" ]; then
    print_step "4. 查询商品详情"
    print_request "GET" "${BASE_URL}/api/products/${PRODUCT_ID}"
    do_curl "GET" "${BASE_URL}/api/products/${PRODUCT_ID}"
else
    print_step "4. 查询商品详情（跳过，未获取到 productId）"
fi

# -------------------- 5. 查询秒杀活动列表 --------------------
print_step "5. 查询秒杀活动列表"
print_request "GET" "${BASE_URL}/api/seckill/activities?pageNo=1&pageSize=5"

ACTIVITY_RESP=$(do_curl "GET" "${BASE_URL}/api/seckill/activities?pageNo=1&pageSize=5")
ACTIVITY_ID=$(echo "$ACTIVITY_RESP" | json_get_data_list_first "activityId")
echo "获取到 activityId: ${ACTIVITY_ID:-未知}"

# 查询秒杀活动详情
if [ -n "$ACTIVITY_ID" ] && [ "$ACTIVITY_ID" != "null" ]; then
    print_step "5.1 查询秒杀活动详情"
    print_request "GET" "${BASE_URL}/api/seckill/activities/${ACTIVITY_ID}"
    ACTIVITY_DETAIL_RESP=$(do_curl "GET" "${BASE_URL}/api/seckill/activities/${ACTIVITY_ID}")

    # 从活动详情中获取秒杀商品 SKU
    ITEM_SKU=$(echo "$ACTIVITY_DETAIL_RESP" | json_get_data_list_first "skuId")
    if [ -n "$ITEM_SKU" ] && [ "$ITEM_SKU" != "null" ]; then
        SKU_ID="$ITEM_SKU"
        echo "从秒杀活动中获取到 skuId: ${SKU_ID}"
    fi
fi

# -------------------- 6. 获取秒杀令牌 --------------------
if [ -n "$ACTIVITY_ID" ] && [ "$ACTIVITY_ID" != "null" ] && \
   [ -n "$SKU_ID" ] && [ "$SKU_ID" != "null" ] && \
   [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then

    print_step "6. 获取秒杀令牌"
    REQUEST_ID="REQ_$(date +%s)_$((RANDOM % 10000))"
    print_request "POST" "${BASE_URL}/api/seckill/activities/${ACTIVITY_ID}/items/${SKU_ID}/token"

    TOKEN_BODY=$(cat <<EOF
{
    "requestId": "${REQUEST_ID}"
}
EOF
    )
    TOKEN_RESP=$(do_curl "POST" "${BASE_URL}/api/seckill/activities/${ACTIVITY_ID}/items/${SKU_ID}/token" "$TOKEN_BODY")
    SECKILL_TOKEN=$(echo "$TOKEN_RESP" | json_get_data "seckillToken")
    echo "获取到 seckillToken: ${SECKILL_TOKEN:0:20}..."

    # -------------------- 7. 提交秒杀 --------------------
    if [ -n "$SECKILL_TOKEN" ] && [ "$SECKILL_TOKEN" != "null" ]; then
        print_step "7. 提交秒杀请求"
        SUBMIT_REQUEST_ID="REQ_SUBMIT_$(date +%s)_$((RANDOM % 10000))"
        print_request "POST" "${BASE_URL}/api/seckill/activities/${ACTIVITY_ID}/items/${SKU_ID}/submit"

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

        RESULT_STATUS=$(echo "$SUBMIT_RESP" | json_get_data "resultStatus")
        echo "秒杀提交结果状态: ${RESULT_STATUS:-未知}"

        # -------------------- 8. 查询秒杀结果（轮询） --------------------
        if [ -n "$SUBMIT_REQUEST_ID" ]; then
            print_step "8. 查询秒杀结果（最多轮询 ${MAX_POLL_RETRY} 次，间隔 ${POLL_INTERVAL}s）"

            for i in $(seq 1 $MAX_POLL_RETRY); do
                echo -e "${YELLOW}  第 ${i}/${MAX_POLL_RETRY} 次轮询...${NC}"
                print_request "GET" "${BASE_URL}/api/seckill/results/${SUBMIT_REQUEST_ID}"
                RESULT_RESP=$(do_curl "GET" "${BASE_URL}/api/seckill/results/${SUBMIT_REQUEST_ID}")

                FINAL_STATUS=$(echo "$RESULT_RESP" | json_get_data "resultStatus")
                ORDER_ID=$(echo "$RESULT_RESP" | json_get_data "orderId")
                echo "  当前结果状态: ${FINAL_STATUS:-未知}, orderId: ${ORDER_ID:-无}"

                if [ "$FINAL_STATUS" != "排队中" ] && [ "$FINAL_STATUS" != "null" ] && [ -n "$FINAL_STATUS" ]; then
                    echo -e "${GREEN}  >> 秒杀已出最终结果: ${FINAL_STATUS}${NC}"
                    break
                fi

                if [ "$i" -lt "$MAX_POLL_RETRY" ]; then
                    sleep "$POLL_INTERVAL"
                fi
            done

            if [ "$FINAL_STATUS" = "排队中" ] || [ -z "$FINAL_STATUS" ] || [ "$FINAL_STATUS" = "null" ]; then
                echo -e "${YELLOW}  >> 轮询结束，秒杀结果仍为排队中${NC}"
            fi
        fi
    else
        print_error "未获取到秒杀令牌，跳过步骤7和8"
    fi
else
    print_step "6-8. 获取令牌/提交秒杀/查询结果（跳过，缺少必要参数）"
fi

# -------------------- 9. 查询订单 --------------------
print_step "9. 查询订单列表"
if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
    print_request "GET" "${BASE_URL}/api/orders?pageNo=1&pageSize=10"
    ORDERS_RESP=$(do_curl "GET" "${BASE_URL}/api/orders?pageNo=1&pageSize=10")
    FIRST_ORDER_ID=$(echo "$ORDERS_RESP" | json_get_data_list_first "orderId")

    if [ -n "$FIRST_ORDER_ID" ] && [ "$FIRST_ORDER_ID" != "null" ]; then
        ORDER_ID="$FIRST_ORDER_ID"
    fi

    # 查询订单详情
    if [ -n "$ORDER_ID" ] && [ "$ORDER_ID" != "null" ]; then
        print_step "9.1 查询订单详情"
        print_request "GET" "${BASE_URL}/api/orders/${ORDER_ID}"
        do_curl "GET" "${BASE_URL}/api/orders/${ORDER_ID}"
    else
        echo "未获取到订单记录"
    fi
else
    print_error "未登录，跳过订单查询"
fi

# =============================================================================
# 测试总结
# =============================================================================
print_step "测试完成"

echo -e "${CYAN}============================================================${NC}"
echo -e "${CYAN}测试结果汇总:${NC}"
echo -e "  用户名:       ${USERNAME}"
echo -e "  用户ID:       ${USER_ID:-无}"
echo -e "  商品ID:       ${PRODUCT_ID:-无}"
echo -e "  商品规格ID:   ${SKU_ID:-无}"
echo -e "  秒杀活动ID:   ${ACTIVITY_ID:-无}"
echo -e "  秒杀结果:     ${FINAL_STATUS:-无}"
echo -e "  订单ID:       ${ORDER_ID:-无}"
echo -e "  令牌:         ${ACCESS_TOKEN:0:20}..."
echo -e "${CYAN}============================================================${NC}"
