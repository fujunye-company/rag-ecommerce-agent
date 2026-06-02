"""
Sync PostgreSQL products to match Qdrant (290 products).

Steps:
1. DELETE all existing PG products (no FKs, safe to clear)
2. INSERT 190 seed + 100 expanded = 290 products with UUID5 deterministic IDs
"""
import json
import asyncio
import asyncpg
from uuid import uuid5, UUID
import os
import random

# Same namespace as Qdrant ingest_to_qdrant.py
QDRANT_NAMESPACE = UUID("6ba7b810-9dad-11d1-80b4-00c04fd430c8")

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SEED_PATH = os.path.join(BASE, 'apps', 'backend', 'data', 'qdrant', 'seed_products.json')
EXPANDED_PATH = os.path.join(BASE, 'apps', 'backend', 'data', 'qdrant', 'products_expanded_100.json')

DB_URL = 'postgresql://shopping:shopping123@localhost:5433/shopping_agent'


def product_uuid(product_id: str) -> str:
    return str(uuid5(QDRANT_NAMESPACE, product_id))


def map_to_pg(p: dict) -> dict:
    """Map source product dict to PG columns."""
    return {
        "id": product_uuid(p["product_id"]),
        "title": p.get("title", ""),
        "description": p.get("description", ""),
        "price": float(p.get("price", 0)),
        "category": p.get("category", ""),
        "brand": p.get("brand", ""),
        "rating": float(p.get("rating", 3.0)),
        "image_urls": p.get("image_urls") or ([p["image_url"]] if p.get("image_url") else []),
        "stock": random.randint(50, 500),
        "sales": random.randint(0, 2000),
        "tags": [],
        "attributes": json.dumps(p.get("attributes") or {}, ensure_ascii=False),
        "highlights": p.get("highlights") or [],
        "scenarios": p.get("scenarios") or [],
    }


async def main():
    # Load source data
    with open(SEED_PATH, 'r', encoding='utf-8') as f:
        seed = json.load(f)
    with open(EXPANDED_PATH, 'r', encoding='utf-8') as f:
        expanded = json.load(f)

    print(f"Source: seed={len(seed)}, expanded={len(expanded)}, total={len(seed) + len(expanded)}")

    # Map all to PG rows
    rows = []
    seen_ids = set()
    for p in seed:
        row = map_to_pg(p)
        if row["id"] not in seen_ids:
            seen_ids.add(row["id"])
            rows.append(row)
    for p in expanded:
        row = map_to_pg(p)
        if row["id"] not in seen_ids:
            seen_ids.add(row["id"])
            rows.append(row)

    print(f"Unique products to import: {len(rows)}")

    conn = await asyncpg.connect(DB_URL)

    # Count before
    before = await conn.fetchval('SELECT count(*) FROM products')
    print(f"PG before: {before} products")

    # Delete all
    await conn.execute('DELETE FROM products')
    print("Deleted all existing products.")

    # Insert in batches
    batch_size = 50
    columns = ["id", "title", "description", "price", "category", "brand", "rating",
               "image_urls", "stock", "sales", "tags", "attributes", "highlights", "scenarios"]

    for i in range(0, len(rows), batch_size):
        batch = rows[i:i + batch_size]
        placeholders = []
        params = []
        for j, row in enumerate(batch):
            ph = []
            for col in columns:
                params.append(row[col])
                ph.append(f"${len(params)}")
            placeholders.append(f"({', '.join(ph)})")

        sql = f"INSERT INTO products ({', '.join(columns)}) VALUES {', '.join(placeholders)}"
        await conn.execute(sql, *params)
        print(f"  Inserted {i + len(batch)}/{len(rows)}")

    after = await conn.fetchval('SELECT count(*) FROM products')
    cats = await conn.fetchval('SELECT count(DISTINCT category) FROM products')
    print(f"PG after: {after} products, {cats} categories")

    await conn.close()
    print("Done.")


if __name__ == '__main__':
    asyncio.run(main())
