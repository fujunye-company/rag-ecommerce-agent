"""Measure SSE first-response timing for the chat endpoint.

The script records:
- first_event_ms: first SSE event received by the client
- first_text_ms: first text_delta event received by the client
- first_card_ms: first product_cards event received by the client
- done_ms: completion event timing
"""

from __future__ import annotations

import argparse
import json
import statistics
import time
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any

import requests


DEFAULT_CASES = [
    ("chitchat", "你好"),
    ("recommend", "推荐一款200元以下的蓝牙耳机"),
    ("negation", "推荐防晒霜，不要含酒精的，不要日系品牌"),
    ("compare", "对比两款降噪耳机"),
]


@dataclass
class Timing:
    case: str
    message: str
    status_code: int
    first_event_ms: int | None = None
    first_text_ms: int | None = None
    first_card_ms: int | None = None
    done_ms: int | None = None
    event_count: int = 0
    text_chars: int = 0
    product_cards: int = 0
    error: str | None = None


def _parse_sse_block(block: list[str]) -> tuple[str | None, dict[str, Any] | None]:
    event_name = None
    data_lines: list[str] = []
    for raw in block:
        line = raw.strip()
        if not line:
            continue
        if line.startswith("event:"):
            event_name = line.split(":", 1)[1].strip()
        elif line.startswith("data:"):
            data_lines.append(line.split(":", 1)[1].strip())

    if not data_lines:
        return event_name, None

    try:
        return event_name, json.loads("\n".join(data_lines))
    except json.JSONDecodeError:
        return event_name, {"raw": "\n".join(data_lines)}


def measure_once(base_url: str, case: str, message: str, timeout: float) -> Timing:
    url = f"{base_url.rstrip('/')}/chat"
    start = time.perf_counter()
    result = Timing(case=case, message=message, status_code=0)
    block: list[str] = []

    try:
        with requests.post(
            url,
            json={"message": message, "conversation_id": f"bench-{case}-{time.time_ns()}"},
            stream=True,
            timeout=timeout,
            headers={"Accept": "text/event-stream"},
        ) as response:
            result.status_code = response.status_code
            response.raise_for_status()

            for raw in response.iter_lines(decode_unicode=True):
                if raw is None:
                    continue
                if raw == "":
                    event_name, data = _parse_sse_block(block)
                    block.clear()
                    if not event_name:
                        continue

                    elapsed_ms = int((time.perf_counter() - start) * 1000)
                    result.event_count += 1
                    if result.first_event_ms is None:
                        result.first_event_ms = elapsed_ms

                    if event_name == "text_delta" and result.first_text_ms is None:
                        result.first_text_ms = elapsed_ms
                    if event_name == "text_delta" and isinstance(data, dict):
                        result.text_chars += len(str(data.get("content", "")))

                    if event_name == "product_cards":
                        if result.first_card_ms is None:
                            result.first_card_ms = elapsed_ms
                        if isinstance(data, dict):
                            cards = data.get("cards") or data.get("products") or []
                            if isinstance(cards, list):
                                result.product_cards += len(cards)
                            if data.get("product_id"):
                                result.product_cards += 1

                    if event_name == "error" and isinstance(data, dict):
                        result.error = str(data.get("message") or data)

                    if event_name == "done":
                        result.done_ms = elapsed_ms
                        break
                else:
                    block.append(raw)
    except Exception as exc:  # noqa: BLE001 - benchmark should return the failure as data.
        result.error = repr(exc)

    if result.done_ms is None:
        result.done_ms = int((time.perf_counter() - start) * 1000)
    return result


def percentile(values: list[int], pct: float) -> float | None:
    if not values:
        return None
    if len(values) == 1:
        return float(values[0])
    sorted_values = sorted(values)
    index = (len(sorted_values) - 1) * pct
    lower = int(index)
    upper = min(lower + 1, len(sorted_values) - 1)
    weight = index - lower
    return sorted_values[lower] * (1 - weight) + sorted_values[upper] * weight


def summarize(results: list[Timing]) -> dict[str, Any]:
    values = [r.first_event_ms for r in results if r.first_event_ms is not None]
    text_values = [r.first_text_ms for r in results if r.first_text_ms is not None]
    return {
        "runs": len(results),
        "first_event_ms": {
            "avg": round(statistics.mean(values), 1) if values else None,
            "p95": round(percentile(values, 0.95), 1) if values else None,
            "max": max(values) if values else None,
            "under_1s": all(v < 1000 for v in values) if values else False,
        },
        "first_text_ms": {
            "avg": round(statistics.mean(text_values), 1) if text_values else None,
            "p95": round(percentile(text_values, 0.95), 1) if text_values else None,
            "max": max(text_values) if text_values else None,
        },
    }


def print_table(results: list[Timing]) -> None:
    print("| case | first_event_ms | first_text_ms | first_card_ms | done_ms | cards | error |")
    print("| --- | ---: | ---: | ---: | ---: | ---: | --- |")
    for item in results:
        print(
            "| {case} | {first_event} | {first_text} | {first_card} | {done} | {cards} | {error} |".format(
                case=item.case,
                first_event=item.first_event_ms if item.first_event_ms is not None else "-",
                first_text=item.first_text_ms if item.first_text_ms is not None else "-",
                first_card=item.first_card_ms if item.first_card_ms is not None else "-",
                done=item.done_ms if item.done_ms is not None else "-",
                cards=item.product_cards,
                error=item.error or "",
            )
        )


def main() -> int:
    parser = argparse.ArgumentParser(description="Benchmark chat SSE first-response timing.")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080/api/v1")
    parser.add_argument("--runs", type=int, default=1)
    parser.add_argument("--timeout", type=float, default=60.0)
    parser.add_argument("--output", type=Path, default=None)
    args = parser.parse_args()

    results: list[Timing] = []
    for run_index in range(args.runs):
        for case, message in DEFAULT_CASES:
            run_case = case if args.runs == 1 else f"{case}-{run_index + 1}"
            results.append(measure_once(args.base_url, run_case, message, args.timeout))

    print_table(results)
    summary = summarize(results)
    print("\nSummary:")
    print(json.dumps(summary, ensure_ascii=False, indent=2))

    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(
            json.dumps(
                {
                    "base_url": args.base_url,
                    "summary": summary,
                    "results": [asdict(result) for result in results],
                },
                ensure_ascii=False,
                indent=2,
            ),
            encoding="utf-8",
        )
        print(f"\nSaved: {args.output}")

    return 0 if all(result.error is None for result in results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
