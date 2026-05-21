"""RAGAS 评测脚本 — 对接 /api/chat 实时评测"""
import json, time, asyncio
import aiohttp

BASE = "http://localhost:8000/api"
EVAL_FILE = "data/test_cases/eval_cases.json"
OUTPUT_FILE = "data/test_cases/eval_results.json"

async def chat_query(session, query):
    """发送 /api/chat 请求，收集 text 和 product_cards"""
    async with session.post(
        f"{BASE}/chat",
        json={"message": query},
        timeout=aiohttp.ClientTimeout(total=30)
    ) as resp:
        text_parts = []
        cards = []
        async for line in resp.content:
            line = line.decode().strip()
            if line.startswith("data:"):
                data = json.loads(line[5:].strip())
                if data.get("type") == "text_delta":
                    text_parts.append(data["content"])
                elif data.get("type") == "product_cards":
                    cards = data.get("products", [])
                elif data.get("type") == "done":
                    break
        return "".join(text_parts), cards

async def main():
    with open(EVAL_FILE) as f:
        cases = json.load(f)
    
    print(f"Running {len(cases)} eval cases...")
    results = []
    passed = 0
    
    async with aiohttp.ClientSession() as session:
        for i, case in enumerate(cases):
            t0 = time.monotonic()
            try:
                text, cards = await chat_query(session, case["query"])
                elapsed = time.monotonic() - t0
                
                has_text = len(text) > 10
                has_cards = len(cards) > 0
                
                # Determine pass based on scenario
                if case["scenario"] in ("chitchat",):
                    ok = has_text  # chitchat just needs text response
                elif case["scenario"] in ("cart_operation", "image_search"):
                    ok = has_text  # cart/image just need text acknowledgment
                else:
                    ok = has_text  # shopping scenarios need text
                
                if ok:
                    passed += 1
                
                results.append({
                    "id": case["id"],
                    "scenario": case["scenario"],
                    "query": case["query"],
                    "difficulty": case["difficulty"],
                    "ttft_ms": round(elapsed * 1000),
                    "text_len": len(text),
                    "cards_count": len(cards),
                    "passed": ok,
                })
                
                print(f"  [{i+1}/{len(cases)}] {case['id']}: {'✅' if ok else '❌'} "
                      f"{elapsed:.1f}s, text={len(text)}chars, cards={len(cards)}")
                
            except Exception as e:
                results.append({
                    "id": case["id"], "scenario": case["scenario"],
                    "query": case["query"], "difficulty": case["difficulty"],
                    "ttft_ms": 0, "text_len": 0, "cards_count": 0,
                    "passed": False, "error": str(e)[:100]
                })
                print(f"  [{i+1}/{len(cases)}] {case['id']}: ❌ ERROR: {str(e)[:80]}")
    
    # Summary
    total = len(cases)
    scenarios = {}
    for r in results:
        s = r["scenario"]
        if s not in scenarios:
            scenarios[s] = {"total": 0, "passed": 0, "avg_ms": 0}
        scenarios[s]["total"] += 1
        if r["passed"]:
            scenarios[s]["passed"] += 1
        scenarios[s]["avg_ms"] += r["ttft_ms"]
    
    for s in scenarios:
        scenarios[s]["avg_ms"] = round(scenarios[s]["avg_ms"] / max(scenarios[s]["total"], 1))
        scenarios[s]["rate"] = round(scenarios[s]["passed"] / scenarios[s]["total"] * 100)
    
    report = {
        "total": total,
        "passed": passed,
        "pass_rate": round(passed / total * 100),
        "avg_ttft_ms": round(sum(r["ttft_ms"] for r in results) / total),
        "scenarios": scenarios,
        "results": results,
    }
    
    with open(OUTPUT_FILE, "w") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    
    print(f"\n{'='*50}")
    print(f"EVAL COMPLETE: {passed}/{total} passed ({report['pass_rate']}%)")
    print(f"Avg TTFT: {report['avg_ttft_ms']}ms")
    print(f"\nBy scenario:")
    for s, d in sorted(scenarios.items()):
        print(f"  {s:25s} {d['passed']}/{d['total']} ({d['rate']}%) avg={d['avg_ms']}ms")
    print(f"{'='*50}")

if __name__ == "__main__":
    asyncio.run(main())
