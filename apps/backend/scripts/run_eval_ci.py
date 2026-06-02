"""CI evaluation script — run eval, compare against baseline, gate on thresholds.

Usage:
    python scripts/run_eval_ci.py              # default 15 samples
    python scripts/run_eval_ci.py --full        # all cases
    python scripts/run_eval_ci.py --baseline-only  # just show baseline

Exit codes:
    0 — all metrics above threshold (PASS)
    1 — one or more metrics below threshold (FAIL)
    2 — eval run error
"""
import json
import os
import sys
import time
import asyncio
import argparse
from pathlib import Path

# Ensure backend is on path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

BASE = os.environ.get("API_BASE_URL", "http://localhost:8080/api/v1")
EVAL_FILE = Path(__file__).resolve().parent.parent / "data" / "test_cases" / "eval_cases.json"
BASELINE_FILE = Path(__file__).resolve().parent.parent / "data" / "test_cases" / "eval_baseline.json"
OUTPUT_FILE = Path(__file__).resolve().parent.parent / "data" / "test_cases" / "eval_ci_results.json"

# ── Thresholds ──
THRESHOLDS = {
    "pass_rate": 70.0,          # % cases with valid text response
    "avg_latency_ms": 15000,    # max avg latency per case
}

# Number of samples for quick mode (3 per scenario, balanced)
QUICK_SAMPLE = 15


async def chat_query(session, query: str) -> tuple[str, list]:
    import aiohttp
    async with session.post(
        f"{BASE}/chat",
        json={"message": query},
        timeout=aiohttp.ClientTimeout(total=45),
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


def load_or_create_baseline() -> dict:
    if BASELINE_FILE.exists():
        with open(BASELINE_FILE, encoding="utf-8") as f:
            return json.load(f)
    return {"_note": "No baseline yet — run a full eval to establish one",
            "pass_rate": 0, "avg_latency_ms": 0, "avg_cards": 0}


def save_baseline(report: dict):
    baseline = {
        "_updated": time.strftime("%Y-%m-%d %H:%M:%S"),
        "pass_rate": report["pass_rate"],
        "avg_latency_ms": report["avg_latency_ms"],
        "avg_cards": report["avg_cards"],
    }
    with open(BASELINE_FILE, "w", encoding="utf-8") as f:
        json.dump(baseline, f, indent=2)
    return baseline


def check_thresholds(report: dict, baseline: dict) -> tuple[bool, list[str]]:
    failures = []
    for metric, threshold in THRESHOLDS.items():
        val = report.get(metric, 0)
        if val < threshold and "latency" not in metric:
            failures.append(f"{metric}: {val} < {threshold} (threshold)")
        elif metric == "avg_latency_ms" and val > threshold:
            failures.append(f"{metric}: {val:.0f}ms > {threshold}ms (max)")

    # Degradation check: if baseline exists, warn on >20% regression
    if baseline.get("pass_rate", 0) > 0:
        degradation = baseline["pass_rate"] - report.get("pass_rate", 0)
        if degradation > 20:
            failures.append(f"pass_rate degraded by {degradation:.1f}% vs baseline ({baseline['pass_rate']}%)")

    return len(failures) == 0, failures


async def main():
    parser = argparse.ArgumentParser(description="CI evaluation runner")
    parser.add_argument("--full", action="store_true", help="Run all cases (default: quick sample)")
    parser.add_argument("--baseline-only", action="store_true", help="Show baseline and exit")
    parser.add_argument("--save-baseline", action="store_true", help="Save result as new baseline")
    args = parser.parse_args()

    if args.baseline_only:
        baseline = load_or_create_baseline()
        print(json.dumps(baseline, indent=2))
        return 0

    # Load cases
    if not EVAL_FILE.exists():
        print(f"ERROR: eval_cases.json not found at {EVAL_FILE}", file=sys.stderr)
        return 2

    with open(EVAL_FILE, encoding="utf-8") as f:
        all_cases = json.load(f)

    if args.full:
        cases = all_cases
    else:
        # Balanced sample across scenarios
        from collections import defaultdict
        by_scenario = defaultdict(list)
        for c in all_cases:
            by_scenario[c["scenario"]].append(c)
        cases = []
        for s in sorted(by_scenario):
            cases.extend(by_scenario[s][: max(2, QUICK_SAMPLE // len(by_scenario))])
        cases = cases[:QUICK_SAMPLE]

    print(f"CI EVAL: {len(cases)} cases ({len(set(c['scenario'] for c in cases))} scenarios)")

    results = []
    passed = 0

    try:
        import aiohttp
    except ImportError:
        print("ERROR: aiohttp not installed. pip install aiohttp", file=sys.stderr)
        return 2

    async with aiohttp.ClientSession() as session:
        for i, case in enumerate(cases):
            t0 = time.monotonic()
            try:
                text, cards = await chat_query(session, case["query"])
                elapsed = time.monotonic() - t0
                ok = len(text.strip()) > 10
                if ok:
                    passed += 1
                results.append({
                    "id": case["id"], "scenario": case["scenario"],
                    "query": case["query"], "latency_ms": round(elapsed * 1000),
                    "text_len": len(text), "cards_count": len(cards), "passed": ok,
                })
                print(f"  [{i+1:3d}/{len(cases)}] {'PASS' if ok else 'FAIL'} "
                      f"{elapsed:.1f}s | {case['query'][:50]}")
            except Exception as e:
                results.append({
                    "id": case["id"], "scenario": case["scenario"],
                    "query": case["query"], "latency_ms": 0,
                    "text_len": 0, "cards_count": 0, "passed": False,
                    "error": str(e)[:120],
                })
                print(f"  [{i+1:3d}/{len(cases)}] FAIL ERROR: {str(e)[:80]}")

    total = len(cases)
    report = {
        "total": total,
        "passed": passed,
        "pass_rate": round(passed / total * 100, 1) if total else 0,
        "avg_latency_ms": round(sum(r["latency_ms"] for r in results) / total) if total else 0,
        "avg_cards": round(sum(r["cards_count"] for r in results) / total, 1) if total else 0,
        "details": results,
    }

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)

    baseline = load_or_create_baseline()
    all_pass, failures = check_thresholds(report, baseline)

    print(f"\n{'='*60}")
    print(f"CI EVAL: {passed}/{total} passed ({report['pass_rate']}%) | "
          f"avg {report['avg_latency_ms']}ms | {report['avg_cards']} cards avg")
    if baseline.get("pass_rate", 0) > 0:
        print(f"Baseline: {baseline['pass_rate']}% pass | {baseline['avg_latency_ms']}ms")
    print(f"Thresholds: pass_rate >= {THRESHOLDS['pass_rate']}% | latency <= {THRESHOLDS['avg_latency_ms']}ms")

    if failures:
        print(f"\nFAILURES:")
        for f in failures:
            print(f"  - {f}")
        print(f"{'='*60}")
        return 1

    if args.save_baseline:
        save_baseline(report)
        print("Baseline updated.")

    print("ALL CHECKS PASSED")
    print(f"{'='*60}")
    return 0


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
