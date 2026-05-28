#!/usr/bin/env bash
# =============================================================================
# SnapShop 秒杀平台 - 支付服务接口测试脚本
# 测试流程：用户登录 → 查订单 → 创建支付单 → 模拟支付 → 查支付状态
# =============================================================================

set -euo pipefail

# -------------------- 配置 --------------------
BASE_URL="http://localhost:8080"
USERNAME="payment_test_$(date +%s)"
PASSWORD="123456"
PHONE="139$(printf '%08d' $((RANDOM % 100000000)))"
ACCESS_TOKEN=""
ORDER_ID=""
PAYMENT_ID=""

MAX_POLL_RETRY=15
POLL_INTERVAL=2

# -------------------- 颜色输出 --------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

print_step() {
    echo -e "\n${CYAN}============================================================${NC}"
    echo -e "${CYAN}[步骤] $1${NC}"
    echo -e "${CYAN}============================================================${NC}"
}

print_request() {
    echo -e "${YELLOW}[请求] $1${NC}"
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

    local response
    local http_code
    local body

    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            -d "$data" 2>&1)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            2>&1)
    fi

    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    print_response "$http_code" "$(echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body")"
    echo "$body"
}

# =============================================================================
# 1. 用户注册与登录
# =============================================================================
print_step "1. 用户注册"
print_request "POST ${BASE_URL}/api/auth/register"
REGISTER_BODY=$(cat <<EOF
{
    "username": "${USERNAME}",
    "password": "${PASSWORD}",
    "phone": "${PHONE}"
}
EOF
)
REGISTER_RESP=$(do_curl "POST" "${BASE_URL}/api/auth/register" "$REGISTER_BODY")
echo "用户注册完成: ${USERNAME}"

print_step "2. 用户登录"
print_request "POST ${BASE_URL}/api/auth/login"
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
    print_error "登录失败，无法继续测试"
    exit 1
fi
echo "获取到 accessToken: ${ACCESS_TOKEN:0:20}..."

# =============================================================================
# 3. 查询订单列表（获取用于支付的订单）
# =============================================================================
print_step "3. 查询订单列表"
print_request "GET ${BASE_URL}/api/orders?pageNo=1&pageSize=10"
ORDERS_RESP=$(do_curl "GET" "${BASE_URL}/api/orders?pageNo=1&pageSize=10")
ORDER_ID=$(echo "$ORDERS_RESP" | json_get_data_list_first "orderId")

if [ -z "$ORDER_ID" ] || [ "$ORDER_ID" = "null" ]; then
    echo -e "${YELLOW}未找到可支付订单，支付流程测试将在有订单后验证。${NC}"
    echo -e "${YELLOW}提示：先完成一次秒杀流程以生成订单。${NC}"
else
    echo "获取到订单 orderId: ${ORDER_ID}"

    # =============================================================================
    # 4. 查询订单详情
    # =============================================================================
    print_step "4. 查询订单详情"
    print_request "GET ${BASE_URL}/api/orders/${ORDER_ID}"
    ORDER_DETAIL_RESP=$(do_curl "GET" "${BASE_URL}/api/orders/${ORDER_ID}")
    ORDER_STATUS=$(echo "$ORDER_DETAIL_RESP" | json_get_data "status")
    echo "订单状态: ${ORDER_STATUS:-未知}"

    # =============================================================================
    # 5. 创建支付单
    # =============================================================================
    print_step "5. 创建支付单"
    print_request "POST ${BASE_URL}/api/payments/orders/${ORDER_ID}"

    PAYMENT_BODY=$(cat <<EOF
{
    "payMethod": "BALANCE"
}
EOF
    )
    PAYMENT_RESP=$(do_curl "POST" "${BASE_URL}/api/payments/orders/${ORDER_ID}" "$PAYMENT_BODY")
    PAYMENT_ID=$(echo "$PAYMENT_RESP" | json_get_data "paymentId")
    PAY_CODE=$(echo "$PAYMENT_RESP" | json_get "code")

    if [ -n "$PAYMENT_ID" ] && [ "$PAYMENT_ID" != "null" ]; then
        echo "创建支付单成功，paymentId: ${PAYMENT_ID}"

        # =============================================================================
        # 6. 模拟支付
        # =============================================================================
        print_step "6. 模拟支付"
        print_request "POST ${BASE_URL}/api/payments/${PAYMENT_ID}/pay"

        PAY_BODY=$(cat <<EOF
{
    "payMethod": "BALANCE"
}
EOF
        )
        PAY_RESP=$(do_curl "POST" "${BASE_URL}/api/payments/${PAYMENT_ID}/pay" "$PAY_BODY")
        PAY_RESULT_CODE=$(echo "$PAY_RESP" | json_get "code")
        echo "支付结果码: ${PAY_RESULT_CODE}"

        # =============================================================================
        # 7. 查询支付状态
        # =============================================================================
        print_step "7. 查询支付状态"
        print_request "GET ${BASE_URL}/api/payments/orders/${ORDER_ID}"
        STATUS_RESP=$(do_curl "GET" "${BASE_URL}/api/payments/orders/${ORDER_ID}")
        PAY_STATUS=$(echo "$STATUS_RESP" | json_get_data "status")
        echo "支付状态: ${PAY_STATUS:-未知}"
    else
        echo -e "${YELLOW}支付单创建接口返回: $PAY_CODE${NC}"
        echo -e "${YELLOW}（接口可能暂未实现或订单状态不符合支付条件）${NC}"
    fi
fi

# =============================================================================
# 8. 支付幂等性测试
# =============================================================================
print_step "8. 支付幂等性测试"
if [ -n "$PAYMENT_ID" ] && [ "$PAYMENT_ID" != "null" ]; then
    print_request "POST ${BASE_URL}/api/payments/${PAYMENT_ID}/pay (重复支付)"
    DUP_PAY_BODY=$(cat <<EOF
{
    "payMethod": "BALANCE"
}
EOF
    )
    DUP_PAY_RESP=$(do_curl "POST" "${BASE_URL}/api/payments/${PAYMENT_ID}/pay" "$DUP_PAY_BODY")
    DUP_CODE=$(echo "$DUP_PAY_RESP" | json_get "code")
    echo "重复支付结果码: ${DUP_CODE}"
    if [ "$DUP_CODE" = "50301" ] || [ "$DUP_CODE" = "50302" ]; then
        echo -e "${GREEN}  >> 幂等拦截生效：重复支付被正确拒绝${NC}"
    else
        echo -e "${YELLOW}  >> 重复支付行为: 请确认幂等逻辑是否生效${NC}"
    fi
else
    echo "跳过（未创建支付单）"
fi

# =============================================================================
# 9. 查询支付后的订单状态
# =============================================================================
print_step "9. 查支付后订单状态"
if [ -n "$ORDER_ID" ] && [ "$ORDER_ID" != "null" ]; then
    print_request "GET ${BASE_URL}/api/orders/${ORDER_ID}"
    FINAL_ORDER_RESP=$(do_curl "GET" "${BASE_URL}/api/orders/${ORDER_ID}")
    FINAL_STATUS=$(echo "$FINAL_ORDER_RESP" | json_get_data "status")
    echo "支付后订单状态: ${FINAL_STATUS:-未知}"
fi

# =============================================================================
# 测试总结
# =============================================================================
print_step "测试完成"

echo -e "${CYAN}============================================================${NC}"
echo -e "${CYAN}支付测试结果汇总:${NC}"
echo -e "  测试用户:       ${USERNAME}"
echo -e "  订单ID:         ${ORDER_ID:-无}"
echo -e "  支付单ID:       ${PAYMENT_ID:-无}"
echo -e "  支付后订单状态: ${FINAL_STATUS:-无}"
echo -e "  访问令牌:       ${ACCESS_TOKEN:0:20}..."
echo -e "${CYAN}============================================================${NC}"
