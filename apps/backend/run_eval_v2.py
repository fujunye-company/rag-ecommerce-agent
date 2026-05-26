#!/usr/bin/env python3
"""评测运行 — 写 stderr 进度 + 检查点"""
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
    
    sys.stderr.write(f'Running {len(cases)} cases...\n')
    
    # Run in batches of 10
    all_results = None
    for batch_start in range(0, len(cases), 10):
        batch = cases[batch_start:batch_start+10]
        sys.stderr.write(f'  batch {batch_start//10+1}: {len(batch)} cases...\n')
        result = await run_evaluation(test_cases=batch)
        sys.stderr.write(f'    -> passed={result["passed"]}/{result["total"]}\n')
        
        # Save checkpoint
        with open(f'data/test_cases/eval_ckpt_{batch_start}.json', 'w') as f:
            json.dump(result, f, ensure_ascii=False, indent=2, default=str)
    
    # Final: merge all batches
    with open('data/test_cases/eval_cases.json') as f:
        all_cases = json.load(f)
    cases = all_cases[:30]
    
    sys.stderr.write(f'\nRunning full {len(cases)} cases...\n')
    final = await run_evaluation(test_cases=cases)
    m = final['metrics']
    
    print(f'Total: {final["total"]}')
    print(f'Passed: {final["passed"]}/{final["total"]}')
    print(f'Failed: {final["failed"]}')
    print(f'Pass rate: {m["pass_rate"]:.1%}')
    print(f'Intent acc: {m["intent_accuracy"]:.1%}')
    print(f'P@3: {m["precision_at_3"]:.4f}')
    print(f'Avg latency: {m["avg_latency_ms"]:.0f}ms')
    
    with open('data/test_cases/eval_results_30.json', 'w') as f:
        json.dump(final, f, ensure_ascii=False, indent=2, default=str)
    sys.stderr.write(f'Done in {time.time()-t0:.0f}s\n')

asyncio.run(main())
