"""
Align product_id in Qdrant payloads and PG:
1. Qdrant: update payload.product_id to UUID5 (was source string)
2. PG: add source_product_id column, populate it
3. Verify cross-DB alignment — same UUID in Qdrant point ID, payload.product_id, and PG.id
"""
import json
import asyncio
import os
import asyncpg
from qdrant_client import QdrantClient
from qdrant_client.http import models
from uuid import uuid5, UUID

QDRANT_NAMESPACE = UUID("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
COLLECTION = os.environ.get("QDRANT_COLLECTION", "products")
BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATABASE_URL = os.environ.get("DATABASE_URL", "postgresql://shopping:shopping123@localhost:5433/shopping_agent")
QDRANT_URL = os.environ.get("QDRANT_URL", "http://localhost:6333")

def product_uuid(product_id: str) -> str:
    return str(uuid5(QDRANT_NAMESPACE, product_id))


async def fix_pg():
    """Add source_product_id to PG products."""
    conn = await asyncpg.connect(DATABASE_URL)

    with open(f"{BASE}/apps/backend/data/qdrant/seed_products.json", "r") as f:
        seed = json.load(f)
    with open(f"{BASE}/apps/backend/data/qdrant/products_expanded_100.json", "r") as f:
        expanded = json.load(f)

    pid_to_uuid = {}
    for p in seed + expanded:
        pid_to_uuid[p["product_id"]] = product_uuid(p["product_id"])

    cols = await conn.fetch("SELECT column_name FROM information_schema.columns WHERE table_name = 'products'")
    col_names = [c['column_name'] for c in cols]

    if 'source_product_id' not in col_names:
        await conn.execute("ALTER TABLE products ADD COLUMN source_product_id VARCHAR(255)")
        print("Added source_product_id column")

    uuid_to_pid = {v: k for k, v in pid_to_uuid.items()}
    pg_rows = await conn.fetch("SELECT id FROM products WHERE source_product_id IS NULL")
    updated = 0
    for row in pg_rows:
        uid = str(row['id'])
        src_pid = uuid_to_pid.get(uid, "")
        if src_pid:
            await conn.execute("UPDATE products SET source_product_id = $1 WHERE id = $2", src_pid, uid)
            updated += 1

    print(f"PG: updated {updated} rows with source_product_id")
    total = await conn.fetchval("SELECT count(*) FROM products")
    with_src = await conn.fetchval("SELECT count(*) FROM products WHERE source_product_id IS NOT NULL")
    print(f"PG: {total} total, {with_src} with source_product_id")
    await conn.close()


def fix_qdrant():
    """Update Qdrant: set payload.product_id to UUID5 via re-upsert (no re-embedding)."""
    client = QdrantClient(url=QDRANT_URL, timeout=60)

    # Scroll all with vectors
    print("Scrolling Qdrant points with vectors...")
    all_pts = []
    offset = None
    while True:
        pts, offset = client.scroll(
            collection_name=COLLECTION, limit=100, offset=offset,
            with_payload=True, with_vectors=True,
        )
        all_pts.extend(pts)
        if offset is None:
            break

    print(f"Scrolled {len(all_pts)} points")

    # Update product_id in payload, re-upsert in batches
    updated = 0
    for i in range(0, len(all_pts), 100):
        batch = all_pts[i:i + 100]
        new_batch = []
        for pt in batch:
            payload = dict(pt.payload or {})
            src_pid = payload.get("source_product_id", "") or payload.get("product_id", "")
            if src_pid:
                new_uuid = product_uuid(src_pid)
                if payload.get("product_id") != new_uuid:
                    payload["product_id"] = new_uuid
                    updated += 1
            new_batch.append(models.PointStruct(id=pt.id, vector=pt.vector, payload=payload))
        client.upsert(collection_name=COLLECTION, points=new_batch)

    print(f"Qdrant: updated product_id in {updated} payloads")

    # Verify
    pts, _ = client.scroll(collection_name=COLLECTION, limit=3, with_payload=True, with_vectors=False)
    for pt in pts:
        pid = pt.payload.get("product_id", "")
        match = "OK" if str(pt.id) == pid else "MISMATCH"
        print(f"  {str(pt.id)[:36]}... | {pid[:36]}... | {match}")


async def verify():
    conn = await asyncpg.connect(DATABASE_URL)
    client = QdrantClient(url=QDRANT_URL, timeout=10)

    # Get all Qdrant product UUIDs
    qd_uuids = set()
    offset = None
    while True:
        pts, offset = client.scroll(collection_name=COLLECTION, limit=100, offset=offset, with_payload=False, with_vectors=False)
        for pt in pts:
            qd_uuids.add(str(pt.id))
        if offset is None:
            break

    # Get all PG UUIDs
    pg_rows = await conn.fetch("SELECT id FROM products")
    pg_uuids = set(str(r['id']) for r in pg_rows)

    common = qd_uuids & pg_uuids
    print(f"\nFinal verification:")
    print(f"  Qdrant: {len(qd_uuids)} points")
    print(f"  PG:     {len(pg_uuids)} products")
    print(f"  Common: {len(common)} UUIDs match")
    print(f"  Only Qdrant: {len(qd_uuids - pg_uuids)}")
    print(f"  Only PG:     {len(pg_uuids - qd_uuids)}")

    await conn.close()


async def main():
    print("1. Fix PG: add source_product_id")
    await fix_pg()
    print("\n2. Fix Qdrant: update payload.product_id to UUID5")
    fix_qdrant()
    print("\n3. Verify")
    await verify()


if __name__ == "__main__":
    asyncio.run(main())
