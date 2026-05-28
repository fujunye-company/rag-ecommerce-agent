"""RAGAS 快速评测 — 30条用例，对接 /api/v1/chat SSE"""
import json, time, asyncio
import aiohttp
import os, sys

# Windows GBK终端下强制UTF-8输出，避免emoji等字符报错
if sys.platform == "win32":
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE = "http://localhost:8082/api/v1"
EVAL_FILE = os.path.join(os.path.dirname(__file__), "..", "data", "test_cases", "eval_cases.json")
OUTPUT_FILE = os.path.join(os.path.dirname(__file__), "..", "data", "test_cases", "eval_quick_results.json")

# 取各场景前 N 条，保证覆盖全面
SAMPLE_SIZE = 30


async def chat_query(session, query):
    async with session.post(
        f"{BASE}/chat",
        json={"message": query},
        timeout=aiohttp.ClientTimeout(total=45)
    ) as resp:
        text_parts = []
        cards = []
        async for line in resp.content:
            line = line.decode(errors="replace").strip()
            if line.startswith("data:"):
                try:
                    data = json.loads(line[5:].strip())
                except json.JSONDecodeError:
                    continue
                t = data.get("type", "")
                if t == "text_delta":
                    text_parts.append(data.get("content", ""))
                elif t == "product_cards":
                    cards.append(data.get("product_id"))
                elif t == "done":
                    break
        return "".join(text_parts), cards


async def main():
    with open(EVAL_FILE, encoding="utf-8") as f:
        all_cases = json.load(f)

    # 从每个场景均匀采样
    from collections import defaultdict
    by_scenario = defaultdict(list)
    for c in all_cases:
        by_scenario[c["scenario"]].append(c)

    cases = []
    for s in sorted(by_scenario):
        cases.extend(by_scenario[s][:max(3, SAMPLE_SIZE // len(by_scenario))])
    cases = cases[:SAMPLE_SIZE]

    print(f"Running {len(cases)} eval cases ({len(by_scenario)} scenarios)...")
    results = []
    passed = 0

    async with aiohttp.ClientSession() as session:
        for i, case in enumerate(cases):
            t0 = time.monotonic()
            try:
                text, cards = await chat_query(session, case["query"])
                elapsed = time.monotonic() - t0

                has_text = len(text.strip()) > 10
                has_cards = len(cards) > 0
                ok = has_text  # 基础: 有实质文本响应即通过

                if ok:
                    passed += 1

                results.append({
                    "id": case["id"],
                    "scenario": case["scenario"],
                    "query": case["query"],
                    "difficulty": case.get("difficulty", ""),
                    "latency_ms": round(elapsed * 1000),
                    "text_len": len(text),
                    "cards_count": len(cards),
                    "passed": ok,
                })

                status = "PASS" if ok else "FAIL"
                print(f"  [{i+1:3d}/{len(cases)}] {case['id']:12s} {status} {elapsed:.1f}s text={len(text):4d}chars cards={len(cards)} | {case['query'][:40]}")

            except Exception as e:
                results.append({
                    "id": case["id"], "scenario": case["scenario"],
                    "query": case["query"], "difficulty": case.get("difficulty", ""),
                    "latency_ms": 0, "text_len": 0, "cards_count": 0,
                    "passed": False, "error": str(e)[:120]
                })
                print(f"  [{i+1:3d}/{len(cases)}] {case['id']:12s} FAIL ERROR: {str(e)[:80]}")

    # 汇总
    total = len(cases)
    scenarios = {}
    for r in results:
        s = r["scenario"]
        if s not in scenarios:
            scenarios[s] = {"total": 0, "passed": 0, "ttft_sum": 0, "cards_sum": 0}
        scenarios[s]["total"] += 1
        if r["passed"]:
            scenarios[s]["passed"] += 1
        scenarios[s]["ttft_sum"] += r["latency_ms"]
        scenarios[s]["cards_sum"] += r["cards_count"]

    for s, d in scenarios.items():
        n = max(d["total"], 1)
        d["pass_rate"] = round(d["passed"] / n * 100)
        d["avg_latency_ms"] = round(d["ttft_sum"] / n)
        d["avg_cards"] = round(d["cards_sum"] / n, 1)

    report = {
        "total": total,
        "passed": passed,
        "pass_rate": round(passed / total * 100) if total else 0,
        "avg_latency_ms": round(sum(r["latency_ms"] for r in results) / total) if total else 0,
        "avg_cards": round(sum(r["cards_count"] for r in results) / total, 1) if total else 0,
        "scenarios": {s: {
            "total": d["total"], "passed": d["passed"],
            "pass_rate": d["pass_rate"], "avg_latency_ms": d["avg_latency_ms"],
            "avg_cards": d["avg_cards"],
        } for s, d in sorted(scenarios.items())},
    }

    os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)

    print(f"\n{'='*60}")
    print(f"EVAL COMPLETE: {passed}/{total} passed ({report['pass_rate']}%)")
    print(f"Avg Latency: {report['avg_latency_ms']}ms  Avg Cards: {report['avg_cards']}")
    print(f"\nBy scenario:")
    for s, d in sorted(scenarios.items()):
        print(f"  {s:25s} {d['passed']:2d}/{d['total']:2d} ({d['pass_rate']:3d}%) lat={d['avg_latency_ms']:4d}ms cards={d['avg_cards']}")
    print(f"{'='*60}")


if __name__ == "__main__":
    asyncio.run(main())
