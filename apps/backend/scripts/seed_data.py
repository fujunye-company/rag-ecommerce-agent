"""
种子数据导入 — 将 products.json 写入 PostgreSQL
用法: python scripts/seed_data.py [--clear]
"""
import json, asyncio, sys
from pathlib import Path
from app.core.database import AsyncSessionLocal, engine, Base
from app.models.product import Product


async def main(clear: bool = False):
    # 确保表已创建
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    # 使用项目根相对路径
    data_path = Path(__file__).parent.parent / "data" / "products.json"
    with open(data_path, "r", encoding="utf-8") as f:
        products_data = json.load(f)

    async with AsyncSessionLocal() as db:
        if clear:
            from sqlalchemy import delete
            await db.execute(delete(Product))
            print("Cleared existing products.")

        count = 0
        for p in products_data:
            try:
                db.add(Product(**p))
                count += 1
            except Exception as e:
                print(f"  ⚠️ Skipped {p.get('title','?')}: {e}")
        await db.commit()
        print(f"Seeded {count}/{len(products_data)} products.")


if __name__ == "__main__":
    clear_flag = "--clear" in sys.argv
    asyncio.run(main(clear=clear_flag))
