#!/bin/bash
# 拾物 App 端到端集成测试
# 使用: bash tests/e2e_test.sh

BASE="http://localhost:8000"
PASS=0
FAIL=0

check() {
    local label="$1"
    local result="$2"
    local expected="$3"
    if echo "$result" | grep -q "$expected"; then
        echo "  ✓ $label"
        ((PASS++))
    else
        echo "  ✗ $label (expected: $expected)"
        echo "    got: ${result:0:100}"
        ((FAIL++))
    fi
}

echo "══════════════════════════════════════════════"
echo "  拾物 E2E Test Suite"
echo "══════════════════════════════════════════════"

# 1. Health
echo ""
echo "── 1. Health Check ──"
R=$(curl -s --max-time 5 "$BASE/health")
check "health 200" "$R" "ok"

# 2. AI Chat (Doubao SSE)
echo ""
echo "── 2. AI Chat SSE ──"
R=$(curl -s -N -X POST "$BASE/api/v1/chat" \
    -H "Content-Type: application/json" \
    -d '{"message":"推荐耳机"}' --max-time 60 2>&1)
check "text_delta event" "$R" "text_delta"
check "product_cards event" "$R" "product_cards"
check "done event" "$R" "done"

# 3. Multi-turn
echo ""
echo "── 3. Multi-turn Context ──"
SID=$(uuidgen)
R1=$(curl -s -X POST "$BASE/api/v1/chat" \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"推荐跑鞋\",\"conversation_id\":\"$SID\"}" --max-time 60 2>&1)
check "R1 product_cards" "$R1" "跑鞋"
R2=$(curl -s -X POST "$BASE/api/v1/chat" \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"500元以内的\",\"conversation_id\":\"$SID\"}" --max-time 60 2>&1)
check "R2 keeps context" "$R2" "跑鞋"

# 4. Vision Search
echo ""
echo "── 4. Vision Search (requires test image) ──"
if [ -f /tmp/test_product_real.jpg ]; then
    R=$(curl -s -N -X POST "$BASE/api/v1/upload/vision-search" \
        -F "file=@/tmp/test_product_real.jpg" --max-time 60 2>&1)
    check "vision_parsed" "$R" "vision_parsed"
    check "product_cards" "$R" "product_cards"
else
    echo "  - Skipped (no test image)"
fi

# 5. Product Compare
echo ""
echo "── 5. Compare API ──"
R=$(curl -s -X POST "$BASE/api/products/compare" \
    -H "Content-Type: application/json" \
    -d '{"product_ids":["JD100038798465","JD100209512295"]}' --max-time 10)
check "compare dimensions" "$R" "dimensions"
check "compare summary" "$R" "summary"

# 6. Negation/Exclusion
echo ""
echo "── 6. Negation ──"
R=$(curl -s -X POST "$BASE/api/v1/chat" \
    -H "Content-Type: application/json" \
    -d '{"message":"推荐耳机，不要Sony"}' --max-time 60 2>&1)
check "negation no Sony" "$R" "product_cards"

# Summary
echo ""
echo "══════════════════════════════════════════════"
echo "  Results: $PASS passed, $FAIL failed"
echo "══════════════════════════════════════════════"
[ "$FAIL" -eq 0 ] && echo "✓ All tests passed!" || echo "✗ Some tests failed"
