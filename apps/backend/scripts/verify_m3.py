"""
M3 端到端验证脚本 — 不依赖 FastAPI 服务器，直接调 Agent
用法: python scripts/verify_m3.py
"""
import asyncio, json, time

async def main():
    from app.services.agent import generate_response
    
    test_queries = [
        "推荐一款3000以内的降噪耳机",
        "适合跑步的运动耳机",
        "送女朋友的生日礼物500以内",
        "你好",
        "Bose和Sony哪个好",
    ]
    
    print("=" * 60)
    print("M3 END-TO-END VERIFICATION")
    print("=" * 60)
    
    all_pass = True
    
    for query in test_queries:
        print(f"\n--- Query: '{query}' ---")
        t0 = time.monotonic()
        events = []
        cards_count = 0
        text_parts = []
        
        try:
            async for event in generate_response(query):
                events.append(event)
                data = json.loads(event.get("data", "{}"))
                if event["event"] == "text_delta":
                    text_parts.append(data.get("content", ""))
                elif event["event"] == "product_cards":
                    cards_count = len(data.get("products", []))
                elif event["event"] == "error":
                    print(f"  ❌ Error: {data.get('message', '')}")
                    all_pass = False
        except Exception as e:
            print(f"  ❌ Exception: {e}")
            all_pass = False
            continue
        
        elapsed = time.monotonic() - t0
        text = "".join(text_parts)
        
        print(f"  Text: {text[:100]}...")
        print(f"  Cards: {cards_count}")
        print(f"  Latency: {elapsed:.1f}s")
        print(f"  Events: {len(events)}")
        
        # Basic checks
        if "你好" in query or "谢谢" in query:
            if cards_count > 0:
                print(f"  ⚠️ Chitchat query returned cards (acceptable)")
        else:
            if cards_count == 0:
                print(f"  ❌ No product cards for shopping query")
                all_pass = False
            else:
                print(f"  ✅ Product cards returned")
    
    print(f"\n{'='*60}")
    print(f"RESULT: {'ALL PASSED ✅' if all_pass else 'SOME FAILED ❌'}")
    print(f"{'='*60}")

if __name__ == "__main__":
    asyncio.run(main())
