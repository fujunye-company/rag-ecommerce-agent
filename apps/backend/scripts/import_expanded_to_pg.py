"""
P1 — 将 products_expanded_100.jsonl 导入 PostgreSQL (同步版, 无 asyncio)
用法: python import_expanded_to_pg.py
"""

import json
import hashlib
import uuid
import sys
from pathlib import Path

import psycopg2
from psycopg2.extras import execute_values

PROJECT_ROOT = Path(__file__).resolve().parent.parent
JSONL_PATH = PROJECT_ROOT / "data" / "qdrant" / "products_expanded_100.jsonl"


def product_id_to_uuid(product_id: str) -> str:
    h = hashlib.md5(product_id.encode()).hexdigest()
    return str(uuid.UUID(h))


def map_record(prod: dict) -> tuple:
    image_urls = prod.get("image_urls") or []
    if not image_urls and prod.get("image_url"):
        image_urls = [prod["image_url"]]

    return (
        product_id_to_uuid(prod["product_id"]),
        prod["title"][:256],
        "",                                  # description
        float(prod.get("price", 0)),
        prod.get("category", ""),
        prod.get("brand", ""),
        float(prod.get("rating", 3.0)),
        image_urls,
        100,                                 # stock
        0,                                   # sales
        [],                                  # tags
        json.dumps(prod.get("attributes", {}), ensure_ascii=False),
        prod.get("highlights", []),
        prod.get("scenarios", []),
    )


def main():
    with open(JSONL_PATH, "r", encoding="utf-8") as f:
        records = [json.loads(line) for line in f if line.strip()]
    print(f"读取 {len(records)} 条商品")

    conn = psycopg2.connect(
        host="localhost",
        port=5432,
        user="shopping",
        password="shopping123",
        dbname="shopping_agent",
    )
    cur = conn.cursor()

    # 检查已有 IDs
    cur.execute("SELECT id FROM products")
    existing_ids = {str(row[0]) for row in cur.fetchall()}

    new_records = []
    skipped = 0
    for prod in records:
        uid = product_id_to_uuid(prod["product_id"])
        if uid in existing_ids:
            skipped += 1
            continue
        new_records.append(map_record(prod))

    if skipped:
        print(f"跳过 {skipped} 条已存在的记录")
    print(f"待导入 {len(new_records)} 条新记录")

    if not new_records:
        print("无新数据需要导入")
        cur.close()
        conn.close()
        return

    execute_values(
        cur,
        """INSERT INTO products (id, title, description, price, category, brand, rating,
                                 image_urls, stock, sales, tags, attributes, highlights, scenarios)
        VALUES %s""",
        new_records,
        template="(%s, %s, %s, %s, %s, %s, %s, %s::varchar[], %s, %s, %s::varchar[], %s::jsonb, %s::varchar[], %s::varchar[])",
        page_size=50,
    )
    conn.commit()

    # 验证
    cur.execute("SELECT COUNT(*) FROM products")
    total = cur.fetchone()[0]
    cur.execute("SELECT category, COUNT(*) as cnt FROM products GROUP BY category ORDER BY cnt DESC")
    cats = cur.fetchall()

    print(f"\n✅ 导入完成! PostgreSQL 现有 {total} 条商品")
    print(f"品类分布 ({len(cats)} 类):")
    for cat, cnt in cats:
        print(f"  {cat}: {cnt}")

    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
