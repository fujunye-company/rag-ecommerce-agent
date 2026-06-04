"""
Fix Qdrant point IDs: integer IDs → UUID5 deterministic IDs.
No re-embedding needed — re-upserts existing vectors/payloads with new IDs.

Also adds pg_uuid to payload so PG lookups work from retrieval results.
"""
from qdrant_client import QdrantClient
from qdrant_client.http import models
from uuid import uuid5, UUID
import os
import time

QDRANT_NAMESPACE = UUID("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
COLLECTION = os.environ.get("QDRANT_COLLECTION", "products")
QDRANT_URL = os.environ.get("QDRANT_URL", "http://localhost:6333")

def product_uuid(product_id: str) -> str:
    return str(uuid5(QDRANT_NAMESPACE, product_id))


def main():
    client = QdrantClient(url=QDRANT_URL, timeout=60)
    count = client.count(collection_name=COLLECTION, exact=True).count
    print(f"Qdrant points: {count}")

    # Scroll all points with vectors
    print("Scrolling all points...")
    all_points = []
    offset = None
    batch = 0
    while True:
        pts, offset = client.scroll(
            collection_name=COLLECTION,
            limit=100,
            offset=offset,
            with_payload=True,
            with_vectors=True,
        )
        all_points.extend(pts)
        batch += 1
        print(f"  batch {batch}: {len(pts)} points, total: {len(all_points)}")
        if offset is None:
            break

    print(f"Total scrolled: {len(all_points)}")

    # Build new points with UUID5 IDs
    new_points = []
    old_ids = []
    fixed_count = 0
    for pt in all_points:
        old_ids.append(pt.id)
        payload = dict(pt.payload or {})

        # Get product_id from payload to compute UUID5
        pid = payload.get("product_id", "")
        if not pid:
            print(f"  WARNING: point {pt.id} has no product_id in payload, skipping")
            continue

        new_uuid = product_uuid(pid)

        # Add pg_uuid to payload for PG-compatible lookups
        payload["pg_uuid"] = new_uuid
        # Also update product_id to be UUID-compatible for backwards compat
        # Keep the original string product_id as "source_product_id"
        payload["source_product_id"] = pid

        new_points.append(
            models.PointStruct(
                id=new_uuid,
                vector=pt.vector,
                payload=payload,
            )
        )
        fixed_count += 1

    print(f"Fixed {fixed_count} points with UUID5 IDs")

    # Delete old points by their integer IDs
    print("Deleting old points...")
    # Delete in batches of 100
    for i in range(0, len(old_ids), 100):
        batch_ids = old_ids[i:i + 100]
        client.delete(
            collection_name=COLLECTION,
            points_selector=models.PointIdsList(points=batch_ids),
        )
        print(f"  Deleted {i + len(batch_ids)}/{len(old_ids)}")

    # Upsert new points
    print("Upserting new points with UUID5 IDs...")
    for i in range(0, len(new_points), 100):
        batch = new_points[i:i + 100]
        client.upsert(collection_name=COLLECTION, points=batch)
        print(f"  Upserted {i + len(batch)}/{len(new_points)}")

    # Verify
    new_count = client.count(collection_name=COLLECTION, exact=True).count
    print(f"\nFinal Qdrant count: {new_count}")

    # Spot check
    pts, _ = client.scroll(collection_name=COLLECTION, limit=3, with_payload=True, with_vectors=False)
    print("\nSpot check (first 3 points):")
    for pt in pts:
        pid = pt.payload.get("product_id", "N/A")
        src = pt.payload.get("source_product_id", "N/A")
        pg = pt.payload.get("pg_uuid", "N/A")
        print(f"  ID: {str(pt.id)[:40]}...")
        print(f"  product_id: {pid}")
        print(f"  source_product_id: {src}")
        print(f"  pg_uuid: {pg[:40]}...")
        print()

    print("Done!")


if __name__ == "__main__":
    main()
