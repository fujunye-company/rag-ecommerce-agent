"""
种子数据导入 — 将 products.json 写入 PostgreSQL
用法: python scripts/seed_data.py
"""
import json, asyncio
from app.core.database import AsyncSessionLocal
from app.models.product import Product


async def main():
    with open("data/products.json", "r", encoding="utf-8") as f:
        products = json.load(f)

    async with AsyncSessionLocal() as db:
        for p in products:
            db.add(Product(**p))
        await db.commit()
    print(f"Seeded {len(products)} products.")


if __name__ == "__main__":
    asyncio.run(main())
