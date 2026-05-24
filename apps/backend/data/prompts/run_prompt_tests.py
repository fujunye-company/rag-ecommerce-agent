"""
Prompt 自动化测试工具

对意图分类 / 槽位填充 / 推荐理由生成三个 Prompt 模板
调用当前 LLM 后端进行批量测试，计算准确率。
"""
import json
import sys
import asyncio
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from app.services.intent import classify_intent, extract_slots
from app.services.llm_client import chat_completion

PROMPTS_DIR = Path(__file__).parent


async def test_intent_classify(test_file: str = "test_intent.json") -> dict:
    """测试意图分类准确率"""
    with open(PROMPTS_DIR / test_file, encoding="utf-8") as f:
        cases = json.load(f)

    correct = 0
    errors = []
    total_latency = 0.0

    print(f"\n=== Intent Classification ({len(cases)} cases) ===")
    for i, case in enumerate(cases):
        query = case["input"]
        expected = case["expected"]

        t0 = time.monotonic()
        try:
            result = await classify_intent(query)
            actual = result["intent"]
            confidence = result.get("confidence", 0)
            elapsed = (time.monotonic() - t0) * 1000
            total_latency += elapsed

            match = actual == expected
            icon = "✅" if match else "❌"
            if match:
                correct += 1
            else:
                errors.append({
                    "input": query,
                    "expected": expected,
                    "actual": actual,
                    "confidence": confidence,
                })

            print(f"  [{i+1:2d}/{len(cases)}] {icon} \"{query[:30]}\" → {actual} "
                  f"({confidence:.2f}, {elapsed:.0f}ms)")

        except Exception as e:
            elapsed = (time.monotonic() - t0) * 1000
            print(f"  [{i+1:2d}/{len(cases)}] ❌ \"{query[:30]}\" ERROR: {e}")
            errors.append({
                "input": query,
                "expected": expected,
                "actual": "ERROR",
                "error": str(e),
            })

        if i > 0 and i % 5 == 0:
            await asyncio.sleep(0.3)

    accuracy = correct / len(cases) if cases else 0
    avg_latency = total_latency / correct if correct > 0 else 0

    return {
        "test": "intent_classify",
        "total": len(cases),
        "correct": correct,
        "accuracy": round(accuracy, 4),
        "avg_latency_ms": round(avg_latency, 1),
        "errors": errors,
        "target_accuracy": 0.90,
        "target_met": accuracy >= 0.90,
    }


async def test_slot_extraction(test_file: str = "test_slots.json") -> dict:
    """测试槽位填充质量"""
    with open(PROMPTS_DIR / test_file, encoding="utf-8") as f:
        cases = json.load(f)

    correct = 0
    errors = []
    total_latency = 0.0

    print(f"\n=== Slot Extraction ({len(cases)} cases) ===")
    for i, case in enumerate(cases):
        query = case["input"]
        intent = case.get("intent", "commodity_recommend")
        expected = case["expected"]

        t0 = time.monotonic()
        try:
            result = await extract_slots(query, intent)
            elapsed = (time.monotonic() - t0) * 1000
            total_latency += elapsed

            # 简单检查：category 正确
            expected_cat = expected.get("category")
            actual_cat = result.get("category")

            cat_match = (expected_cat is None or
                        (actual_cat and expected_cat in str(actual_cat)))
            icon = "✅" if cat_match else "⚠️"
            if cat_match:
                correct += 1
            else:
                errors.append({
                    "input": query,
                    "expected": expected,
                    "actual": result,
                })

            print(f"  [{i+1:2d}/{len(cases)}] {icon} \"{query[:30]}\" → "
                  f"cat={actual_cat}, brand={result.get('brand_preference')}, "
                  f"price=[{result.get('price_min')}-{result.get('price_max')}]")

        except Exception as e:
            elapsed = (time.monotonic() - t0) * 1000
            print(f"  [{i+1:2d}/{len(cases)}] ❌ \"{query[:30]}\" ERROR: {e}")
            errors.append({
                "input": query,
                "expected": expected,
                "actual": "ERROR",
                "error": str(e),
            })

        if i > 0 and i % 5 == 0:
            await asyncio.sleep(0.3)

    accuracy = correct / len(cases) if cases else 0
    avg_latency = total_latency / len(cases) if len(cases) > 0 else 0

    return {
        "test": "slot_extract",
        "total": len(cases),
        "correct": correct,
        "accuracy": round(accuracy, 4),
        "avg_latency_ms": round(avg_latency, 1),
        "errors": errors,
    }


async def test_recommend_reason(test_file: str = "test_reasons.json") -> dict:
    """测试推荐理由三段式生成"""
    with open(PROMPTS_DIR / test_file, encoding="utf-8") as f:
        cases = json.load(f)

    correct = 0
    errors = []

    print(f"\n=== Recommend Reason Generation ({len(cases)} cases) ===")
    for i, case in enumerate(cases):
        prod = case["product"]
        query = case["query"]
        scenario = case.get("scenario", "")

        prompt = f"""你是一个电商导购助手。基于商品信息生成推荐理由。

商品：{prod['name']} | ¥{prod['price']} | ★{prod['rating']} | {', '.join(prod.get('highlights', []))}
用户需求：{query} ({scenario})

用三段式输出（每段≤30字）：
① 匹配依据：为什么这个商品适合用户
② 品质亮点：数据支撑的核心优势
③ 适用场景：最适合的使用场景

禁止使用"非常""很好""不错"等模糊词。必须引用具体数字。
只输出推荐理由文本，不要JSON。"""

        try:
            result = await chat_completion(
                messages=[{"role": "user", "content": prompt}],
                temperature=0.3,
            )

            # 简单检查：包含三段式（①②③ 或 匹配/亮点/场景）
            has_structure = (
                "①" in result or "②" in result or "③" in result or
                "匹配" in result or "亮点" in result or "场景" in result
            )
            icon = "✅" if has_structure else "⚠️"
            if has_structure:
                correct += 1
            else:
                errors.append({
                    "product": prod["name"],
                    "query": query,
                    "response": result[:200],
                })

            print(f"  [{i+1:2d}/{len(cases)}] {icon} {prod['name'][:20]} → "
                  f"{result[:60].replace(chr(10), ' ')}...")

        except Exception as e:
            print(f"  [{i+1:2d}/{len(cases)}] ❌ {prod['name'][:20]} ERROR: {e}")
            errors.append({
                "product": prod["name"],
                "query": query,
                "error": str(e),
            })

        if i > 0 and i % 5 == 0:
            await asyncio.sleep(0.3)

    accuracy = correct / len(cases) if cases else 0

    return {
        "test": "recommend_reason",
        "total": len(cases),
        "correct": correct,
        "accuracy": round(accuracy, 4),
        "errors": errors,
    }


async def main(tests: list[str] = None):
    """运行 Prompt 测试"""
    if tests is None:
        tests = ["intent", "slots", "reason"]

    t0 = time.monotonic()
    results = {}

    if "intent" in tests:
        results["intent_classify"] = await test_intent_classify()

    if "slots" in tests:
        results["slot_extract"] = await test_slot_extraction()

    if "reason" in tests:
        results["recommend_reason"] = await test_recommend_reason()

    total_elapsed = time.monotonic() - t0

    # Summary
    print(f"\n{'='*60}")
    print(f"Prompt Test Summary ({total_elapsed:.0f}s)")
    print(f"{'='*60}")
    for name, result in results.items():
        status = "✅" if result["accuracy"] >= 0.85 else "⚠️"
        target = f"(target {result.get('target_accuracy', 0.85)})" if "target_accuracy" in result else ""
        print(f"  {status} {name:25s}: {result['accuracy']:.1%} "
              f"({result['correct']}/{result['total']}) {target}")

    # Write report
    report = {
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "total_elapsed_s": round(total_elapsed, 1),
        "results": results,
    }
    report_path = PROMPTS_DIR / "test_results.json"
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2, ensure_ascii=False)

    print(f"\n  Report: {report_path}")


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Prompt 自动化测试")
    parser.add_argument("--tests", nargs="+", choices=["intent", "slots", "reason"],
                       help="指定测试项（默认全部）")
    args = parser.parse_args()
    asyncio.run(main(args.tests))
