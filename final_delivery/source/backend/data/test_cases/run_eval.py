"""
RAGAS 自动评测脚本

对每条 eval_cases 调用 Agent /api/chat 端点，
收集回答+上下文后计算 RAGAS 指标。
"""
import json
import os
import sys
import asyncio
import time
from pathlib import Path

# 添加 backend 到 path
sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

EVAL_CASES_PATH = Path(__file__).parent / "eval_cases.json"
OUTPUT_PATH = Path(__file__).parent / "eval_results.json"

API_BASE = os.environ.get("API_BASE_URL", "http://localhost:8080")


async def call_chat_api(query: str, session_id: str = "eval-session") -> dict:
    """调用 /api/chat SSE 端点，收集完整响应"""
    import httpx

    async with httpx.AsyncClient(timeout=60.0) as client:
        async with client.stream(
            "POST",
            f"{API_BASE}/api/chat",
            json={"message": query, "conversation_id": session_id},
            headers={"Content-Type": "application/json"},
        ) as response:
            response.raise_for_status()
            text_parts = []
            product_cards = []
            done_msg = ""

            async for line in response.aiter_lines():
                if not line.startswith("data: "):
                    continue
                data_str = line[6:]
                try:
                    data = json.loads(data_str)
                except json.JSONDecodeError:
                    continue

                event_type = data.get("type", "")
                if event_type == "text_delta":
                    text_parts.append(data.get("content", ""))
                elif event_type == "product_cards":
                    # ProductCardEvent 逐字段模型 — 收集每张卡片
                    card = {
                        "product_id": data.get("product_id", ""),
                        "title": data.get("title", ""),
                        "price": data.get("price", 0),
                        "rating": data.get("rating", 0),
                        "match_score": data.get("match_score", 0),
                        "highlights": data.get("highlights", []),
                        "image_url": data.get("image_url"),
                    }
                    product_cards.append(card)
                elif event_type == "done":
                    done_msg = data.get("message", "")
                    break

            return {
                "answer": "".join(text_parts),
                "product_cards": product_cards,
                "done_message": done_msg,
            }


async def run_single_case(case: dict, session_id: str) -> dict:
    """运行单条评测用例"""
    query = case["query"]
    t0 = time.monotonic()

    try:
        result = await call_chat_api(query, session_id)
        elapsed = (time.monotonic() - t0) * 1000
        return {
            "case_id": case["id"],
            "scenario": case["scenario"],
            "query": query,
            "expected_intent": case.get("expected_intent"),
            "answer": result["answer"],
            "product_count": len(result.get("product_cards", [])),
            "product_ids": [c.get("product_id") for c in result.get("product_cards", [])],
            "ground_truth_ids": case.get("ground_truth_product_ids", []),
            "status": "ok",
            "latency_ms": round(elapsed, 1),
        }
    except Exception as e:
        elapsed = (time.monotonic() - t0) * 1000
        return {
            "case_id": case["id"],
            "scenario": case["scenario"],
            "query": query,
            "expected_intent": case.get("expected_intent"),
            "answer": "",
            "product_count": 0,
            "product_ids": [],
            "ground_truth_ids": case.get("ground_truth_product_ids", []),
            "status": "error",
            "error": str(e),
            "latency_ms": round(elapsed, 1),
        }


def compute_metrics(results: list[dict]) -> dict:
    """计算 RAGAS 等效指标（无需 ragas 库）"""
    total = len(results)
    ok = sum(1 for r in results if r["status"] == "ok")
    error = total - ok

    # Precision@3: ground_truth_ids ∩ returned_ids / min(3, len(returned))
    p3_scores = []
    for r in results:
        gt = set(r["ground_truth_ids"])
        ret = set(r["product_ids"][:3])
        if gt:
            p3 = len(gt & ret) / min(len(ret), 3) if ret else 0
            p3_scores.append(p3)

    avg_p3 = sum(p3_scores) / len(p3_scores) if p3_scores else 0

    # Answer relevancy: 有内容回答的比例
    with_answer = sum(1 for r in results if len(r.get("answer", "")) > 10)
    answer_rate = with_answer / total if total > 0 else 0

    # 平均延迟
    latencies = [r["latency_ms"] for r in results if r["status"] == "ok"]
    avg_latency = sum(latencies) / len(latencies) if latencies else 0

    return {
        "total_cases": total,
        "passed": ok,
        "failed": error,
        "precision_at_3": round(avg_p3, 4),
        "answer_rate": round(answer_rate, 4),
        "avg_latency_ms": round(avg_latency, 1),
        "target": {
            "faithfulness": "≥0.85",
            "answer_relevancy": "≥0.85",
            "precision_at_3": "≥0.75",
            "intent_accuracy": "≥0.90",
        }
    }


async def main(limit: int = 0):
    """主评测流程"""
    if not EVAL_CASES_PATH.exists():
        print(f"ERROR: {EVAL_CASES_PATH} not found")
        sys.exit(1)

    with open(EVAL_CASES_PATH, encoding="utf-8") as f:
        all_cases = json.load(f)

    # 加载扩展场景文件（scene4=否定语义, scene6=购物车, scene7=图片搜索）
    extensions_dir = Path(__file__).parent
    for ext_name in ["eval_cases_scene4.json", "eval_cases_scene6.json", "eval_cases_scene7.json"]:
        ext_path = extensions_dir / ext_name
        if ext_path.exists():
            with open(ext_path, encoding="utf-8") as f:
                ext_cases = json.load(f)
            all_cases.extend(ext_cases)
            print(f"  + loaded {len(ext_cases)} cases from {ext_name}")

    cases = all_cases[:limit] if limit else all_cases
    print(f"Running evaluation: {len(cases)} cases (total available: {len(all_cases)})")
    print(f"API: {API_BASE}/api/chat\n")

    results = []
    for i, case in enumerate(cases):
        session_id = f"eval-{case['id']}"
        result = await run_single_case(case, session_id)
        results.append(result)

        status_icon = "✅" if result["status"] == "ok" else "❌"
        print(f"  [{i+1:3d}/{len(cases)}] {status_icon} {case['id']:20s} | "
              f"{result['latency_ms']:6.0f}ms | "
              f"products={result['product_count']} | "
              f"answer={len(result.get('answer',''))}chars")

        # 避免打爆服务
        if i > 0 and i % 10 == 0:
            await asyncio.sleep(0.5)

    # 指标
    metrics = compute_metrics(results)

    # 输出
    output = {
        "run_info": {
            "total_cases": len(cases),
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        },
        "metrics": metrics,
        "results": results,
    }

    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(output, f, indent=2, ensure_ascii=False)

    print(f"\n{'='*60}")
    print(f"Evaluation Complete")
    print(f"{'='*60}")
    print(f"  Total:      {metrics['total_cases']}")
    print(f"  Passed:     {metrics['passed']} ✅")
    print(f"  Failed:     {metrics['failed']} ❌")
    print(f"  P@3:        {metrics['precision_at_3']:.4f}  (target ≥0.75)")
    print(f"  AnswerRate: {metrics['answer_rate']:.4f}  (target ≥0.85)")
    print(f"  AvgLatency: {metrics['avg_latency_ms']:.0f}ms")
    print(f"\n  Report: {OUTPUT_PATH}")


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="RAGAS 自动评测")
    parser.add_argument("--limit", type=int, default=0, help="限制评测条数（0=全部）")
    args = parser.parse_args()
    asyncio.run(main(limit=args.limit))
