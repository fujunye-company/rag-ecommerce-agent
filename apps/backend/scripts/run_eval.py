"""
评测脚本 — 跑 eval_cases.json → 生成报告 [全量预留]
用法: python scripts/run_eval.py
"""
import json
from app.services.evaluator import run_evaluation


def main():
    with open("data/test_cases/eval_cases.json", "r") as f:
        data = json.load(f)
    # TODO: 全量阶段接入 evaluator
    # report = asyncio.run(run_evaluation(data["test_cases"]))
    print("评测功能开发中 (全量阶段)")


if __name__ == "__main__":
    main()
