#!/usr/bin/env python3
"""评测运行脚本 — 30条用例快速验证"""
import json, time, sys, os
os.environ['HF_HUB_OFFLINE'] = '1'
os.environ['TRANSFORMERS_OFFLINE'] = '1'

sys.path.insert(0, '.')
import asyncio
from app.services.evaluator import run_evaluation

async def main():
    t0 = time.time()
    with open('data/test_cases/eval_cases.json') as f:
        all_cases = json.load(f)
    cases = all_cases[:30]
    
    print(f'Running {len(cases)} cases...', flush=True)
    result = await run_evaluation(test_cases=cases)
    
    m = result['metrics']
    print(f'Passed: {result["passed"]}/{result["total"]}', flush=True)
    print(f'Pass rate: {m["pass_rate"]:.1%}', flush=True)
    print(f'Intent acc: {m["intent_accuracy"]:.1%}', flush=True)
    print(f'P@3: {m["precision_at_3"]:.4f}', flush=True)
    print(f'Avg latency: {m["avg_latency_ms"]:.0f}ms', flush=True)
    print(f'RAGAS: {m["ragas"]}', flush=True)
    print(f'Time: {time.time()-t0:.0f}s', flush=True)
    
    with open('data/test_cases/eval_results_30.json', 'w') as f:
        json.dump(result, f, ensure_ascii=False, indent=2, default=str)
    print('Saved.', flush=True)

asyncio.run(main())
