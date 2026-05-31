#!/bin/bash
# ============================================================
# 拾物 E2E 场景测试 — 覆盖竞赛 9 场景 + 加分项
# 用法: bash tests/e2e_scenarios.sh [BASE_URL]
# 默认: http://localhost:8080/api/v1
# ============================================================
set -euo pipefail

BASE="${1:-http://localhost:8080/api/v1}"
PASS=0; FAIL=0; TOTAL=0
SESSION=""

green() { echo -e "\033[32m$*\033[0m"; }
red()   { echo -e "\033[31m$*\033[0m"; }
bold()  { echo -e "\033[1m$*\033[0m"; }
dim()   { echo -e "\033[2m$*\033[0m"; }

check() {
    local label="$1"; shift
    TOTAL=$((TOTAL + 1))
    if "$@"; then
        green "  PASS: $label"
        PASS=$((PASS + 1))
        return 0
    else
        red "  FAIL: $label"
        FAIL=$((FAIL + 1))
        return 1
    fi
}

extract_session() {
    SESSION=$(echo "$1" | grep -o '"session_id":"[^"]*"' | head -1 | cut -d'"' -f4)
    [ -n "$SESSION" ] && dim "  session: $SESSION"
}

# ── 0. Health ─────────────────────────────────────────────
bold "=== 0. Health Check ==="
HEALTH=$(curl -s "$BASE/health" || echo '{"status":"down"}')
check "GET /health returns 200" echo "$HEALTH" | grep -q '"database"'

# ── 1. 场景1: 单轮模糊推荐 ───────────────────────────────
bold "=== 场景1: 单轮模糊推荐 ==="
RESP=$(curl -s -N "$BASE/chat" \
    -H "Content-Type: application/json" \
    -d '{"message":"推荐适合油皮的洗面奶"}' 2>/dev/null || true)
check "SSE text_delta 事件" echo "$RESP" | grep -q '"text_delta"'
check "SSE product_cards 事件" echo "$RESP" | grep -q '"product_cards"'
check "SSE done 事件" echo "$RESP" | grep -q '"done"'
extract_session "$RESP"

# ── 2. 场景2: 条件筛选 ───────────────────────────────────
bold "=== 场景2: 条件筛选 ==="
RESP=$(curl -s -N "$BASE/chat" \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"200元以下的蓝牙耳机有哪些？\",\"session_id\":\"$SESSION\"}" 2>/dev/null || true)
check "价格条件筛选有结果" echo "$RESP" | grep -q '"product_cards"'

# ── 3. 场景3: 多轮追问 ───────────────────────────────────
bold "=== 场景3: 多轮追问与细化 ==="
# 第1轮
R1=$(curl -s -N "$BASE/chat" \
    -H "Content-Type: application/json" \
    -d '{"message":"推荐跑鞋"}' 2>/dev/null || true)
extract_session "$R1"
check "R1: 跑鞋推荐有返回" echo "$R1" | grep -q '"product_cards"'

# 第2轮: 追加条件
R2=$(curl -s -N "$BASE/chat" \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"轻量的\",\"session_id\":\"$SESSION\"}" 2>/dev/null || true)
check "R2: 轻量条件有返回" echo "$R2" | grep -q '"text_delta"'

# 第3轮: 价格限制
R3=$(curl -s -N "$BASE/chat" \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"500以内\",\"session_id\":\"$SESSION\"}" 2>/dev/null || true)
check "R3: 价格限制有返回" echo "$R3" | grep -q '"text_delta"'

# ── 4. 场景4: 对比决策 ───────────────────────────────────
bold "=== 场景4: 对比决策 ==="
RESP=$(curl -s -X POST "$BASE/products/compare" \
    -H "Content-Type: application/json" \
    -d '{"product_ids":["prod_001","prod_002"],"dimensions":[]}' 2>/dev/null || true)
check "POST /products/compare 返回200" echo "$RESP" | grep -q '"dimensions"'

# ── 5. 场景5: Agent 主动反问 ──────────────────────────────
bold "=== 场景5: Agent 主动反问 ==="
RESP=$(curl -s -N "$BASE/chat" \
    -H "Content-Type: application/json" \
    -d '{"message":"推荐一款手机"}' 2>/dev/null || true)
check "信息不足时触发 clarify 或 text_delta" echo "$RESP" | grep -qE '"clarify"|"text_delta"'

# ── 6. 场景6: 反选排除 ───────────────────────────────────
bold "=== 场景6: 反选/排除约束 ==="
RESP=$(curl -s -N "$BASE/chat" \
    -H "Content-Type: application/json" \
    -d '{"message":"推荐防晒霜，不要含酒精的，不要日系品牌"}' 2>/dev/null || true)
check "否定语义处理有返回" echo "$RESP" | grep -qE '"text_delta"|"product_cards"'

RESP=$(curl -s -N "$BASE/chat" \
    -H "Content-Type: application/json" \
    -d '{"message":"推荐耳机，不要骨传导的"}' 2>/dev/null || true)
check "排除骨传导耳机有返回" echo "$RESP" | grep -qE '"text_delta"|"product_cards"'

# ── 7. 场景7: 场景化组合推荐 ──────────────────────────────
bold "=== 场景7: 场景化组合推荐 ==="
RESP=$(curl -s -N "$BASE/chat" \
    -H "Content-Type: application/json" \
    -d '{"message":"下周三亚度假，防晒到穿搭方案"}' 2>/dev/null || true)
check "场景组合推荐有返回" echo "$RESP" | grep -qE '"text_delta"|"product_cards"'

# ── 8. 场景8: 购物车 CRUD ─────────────────────────────────
bold "=== 场景8: 购物车与下单 ==="

# 创建一个新 session 用于购物车测试
CART_SESSION=$(python3 -c "import uuid; print(str(uuid.uuid4()))" 2>/dev/null || echo "cart-test-$(date +%s)")

# 8a. 加购
CART_ADD=$(curl -s -X POST "$BASE/cart/items" \
    -H "Content-Type: application/json" \
    -d "{\"session_id\":\"$CART_SESSION\",\"product_id\":\"prod_001\",\"quantity\":1}" 2>/dev/null || true)
check "POST /cart/items 加购成功" echo "$CART_ADD" | grep -q '"product_id"'

# 8b. 查看购物车
CART_GET=$(curl -s "$BASE/cart?session_id=$CART_SESSION" 2>/dev/null || true)
check "GET /cart 返回购物车" echo "$CART_GET" | grep -q '"items"'

# 8c. 修改数量
CART_UPDATE=$(curl -s -X PUT "$BASE/cart/items/1" \
    -H "Content-Type: application/json" \
    -d '{"quantity":2}' 2>/dev/null || true)
check "PUT /cart/items/1 修改数量" echo "$CART_UPDATE" | grep -q '"quantity"'

# 8d. 删除
CART_DEL=$(curl -s -X DELETE "$BASE/cart/items/1" \
    -H "Content-Type: application/json" \
    -d "{\"session_id\":\"$CART_SESSION\"}" 2>/dev/null || true)
check "DELETE /cart/items/1 删除成功" echo "$CART_DEL" | grep -q '"message"'

# 8e. 下单
ORDER=$(curl -s -X POST "$BASE/orders" \
    -H "Content-Type: application/json" \
    -d "{\"session_id\":\"$CART_SESSION\",\"address\":\"广东省广州市天河区\"}" 2>/dev/null || true)
check "POST /orders 下单成功" echo "$ORDER" | grep -q '"order_no"'

# ── 9. 场景9: 拍照找货 ───────────────────────────────────
bold "=== 场景9: 拍照找货 ==="
RESP=$(curl -s -X POST "$BASE/chat" \
    -H "Content-Type: application/json" \
    -d '{"message":"[image] 白色运动鞋","image_base64":""}' 2>/dev/null || true)
check "拍照找货有返回(文本模式)" echo "$RESP" | grep -qE '"text_delta"|"vision_query_received"'

# ── 加分项验证 ────────────────────────────────────────────
bold "=== 加分项验证 ==="

# 商品查询
PRODS=$(curl -s "$BASE/products?limit=5" 2>/dev/null || true)
check "GET /products 返回商品列表" echo "$PRODS" | grep -q '"product_id"'

# 商品详情
DETAIL=$(curl -s "$BASE/products/prod_001" 2>/dev/null || true)
check "GET /products/prod_001 返回详情" echo "$DETAIL" | grep -q '"title"'

# 查询缓存
CACHE=$(curl -s -X POST "$BASE/cache/clear" 2>/dev/null || true)
check "POST /cache/clear 缓存端点" echo "$CACHE" | grep -q '"message"'

# 反馈
FB=$(curl -s -X POST "$BASE/feedback" \
    -H "Content-Type: application/json" \
    -d '{"session_id":"test-session","message_id":"test-msg","rating":1}' 2>/dev/null || true)
check "POST /feedback 反馈端点" echo "$FB" | grep -q '"message"'

# ── 汇总 ──────────────────────────────────────────────────
echo ""
bold "============================================"
printf "  结果: %s/%s 通过" "$PASS" "$TOTAL"
if [ "$FAIL" -gt 0 ]; then
    printf " (%s 失败)\n" "$FAIL"
    red "============================================"
    exit 1
else
    green " (全部通过)"
    green "============================================"
fi
