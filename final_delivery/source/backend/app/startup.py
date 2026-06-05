"""
First-run auto-import: ensures Qdrant has product vectors before the app serves traffic.

Idempotent — skips if collection already contains data. Designed for one-click deploy.
"""
import json
import logging
import uuid
from dataclasses import dataclass
from pathlib import Path

import httpx
from qdrant_client import QdrantClient
from qdrant_client.http import models as qdrant_models
from sentence_transformers import SentenceTransformer

from app.core.config import settings

logger = logging.getLogger("startup")

QDRANT_NAMESPACE = uuid.UUID("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
APP_ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = Path(__file__).resolve().parents[3]
DATA_DIR_CANDIDATES = [
    APP_ROOT / "data" / "qdrant",
    REPO_ROOT / "data" / "qdrant",
    REPO_ROOT / "apps" / "data" / "qdrant",
]
JSONL_PATH = next(
    (
        path / "products_expanded_100.jsonl"
        for path in DATA_DIR_CANDIDATES
        if (path / "products_expanded_100.jsonl").exists()
    ),
    DATA_DIR_CANDIDATES[0] / "products_expanded_100.jsonl",
)
MAX_RETRIES = 20
RETRY_INTERVAL_S = 6


@dataclass
class StartupState:
    phase: str = "initializing"  # initializing | seeding | warming_reranker | ready
    db_done: bool = False
    collection_exists: bool = False
    item_count: int = 0
    reranker_warm: bool = False
    message: str = ""


_state = StartupState()


def get_startup_state() -> dict:
    return {
        "phase": _state.phase,
        "db_done": _state.db_done,
        "collection_exists": _state.collection_exists,
        "item_count": _state.item_count,
        "reranker_warm": _state.reranker_warm,
        "message": _state.message,
    }


def _product_id_to_uuid(product_id: str) -> str:
    return str(uuid.uuid5(QDRANT_NAMESPACE, product_id))


def _build_doc_text(prod: dict) -> str:
    attrs = " ".join(f"{k}:{v}" for k, v in prod.get("attributes", {}).items())
    highlights = " ".join(prod.get("highlights", []))
    scenarios = " ".join(prod.get("scenarios", []))
    description = prod.get("description", "")
    review_texts = prod.get("review_summary", "")
    return (
        f"商品名称: {prod['title']} "
        f"品牌: {prod.get('brand', '')} "
        f"分类: {prod['category']} "
        f"价格: {prod['price']}元 "
        f"评分: {prod['rating']}分 "
        f"属性: {attrs} "
        f"亮点: {highlights} "
        f"场景: {scenarios} "
        f"描述: {description} "
        f"用户评价: {review_texts}"
    )


def _build_payload(prod: dict) -> dict:
    return {
        "product_id": prod["product_id"],
        "title": prod["title"],
        "category": prod["category"],
        "brand": prod.get("brand", ""),
        "price": prod.get("price", 0),
        "rating": prod.get("rating", 3.0),
        "rating_count": prod.get("rating_count", 0),
        "attributes": prod.get("attributes", {}),
        "highlights": prod.get("highlights", []),
        "scenarios": prod.get("scenarios", []),
        "image_url": prod.get("image_url") or (prod.get("image_urls", [None]) or [None])[0],
        "image_urls": prod.get("image_urls", []),
        "source": prod.get("source", ""),
    }


async def _wait_for_qdrant() -> bool:
    """Wait until Qdrant health check passes or retries exhausted."""
    import asyncio
    for i in range(MAX_RETRIES):
        try:
            r = httpx.get(f"{settings.QDRANT_URL}/healthz", timeout=5.0)
            if r.status_code == 200:
                logger.info("Qdrant reachable after %d retry(s)", i)
                return True
        except Exception:
            pass
        logger.debug("Waiting for Qdrant... (%d/%d)", i + 1, MAX_RETRIES)
        await asyncio.sleep(RETRY_INTERVAL_S)
    return False


def _load_products() -> list[dict]:
    """Load product records from the JSONL data file."""
    products = []
    with open(JSONL_PATH, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                products.append(json.loads(line))
    return products


async def ensure_qdrant_data() -> None:
    """Ensure Qdrant collection exists and contains product vectors.

    Idempotent: if collection already has data, skips immediately.
    """
    # Apply HuggingFace endpoint override for model downloads (mirror for China)
    if settings.HF_ENDPOINT:
        import os
        os.environ.setdefault("HF_ENDPOINT", settings.HF_ENDPOINT)
        logger.info("HF_ENDPOINT=%s", settings.HF_ENDPOINT)

    if not settings.AUTO_IMPORT_DATA:
        logger.info("AUTO_IMPORT_DATA=false, skipping data seed")
        _state.phase = "ready"
        return

    if not JSONL_PATH.exists():
        logger.warning("Data file not found: %s, skipping auto-import", JSONL_PATH)
        _state.phase = "ready"
        _state.message = f"Data file missing: {JSONL_PATH}"
        return

    # Wait for Qdrant
    if not await _wait_for_qdrant():
        logger.error("Qdrant unavailable after %d retries, skipping data import", MAX_RETRIES)
        _state.phase = "ready"
        _state.message = "Qdrant unreachable — data import skipped"
        return

    client = QdrantClient(url=settings.QDRANT_URL, timeout=60)
    try:
        collection_exists = client.collection_exists(settings.QDRANT_COLLECTION)
        if collection_exists:
            count = client.count(collection_name=settings.QDRANT_COLLECTION, exact=True).count
            _state.collection_exists = True
            _state.item_count = count
            if count > 0:
                logger.info("Qdrant collection '%s' already has %d vectors, skipping import",
                            settings.QDRANT_COLLECTION, count)
                _state.phase = "ready"
                return

        # Need to create and/or populate
        _state.phase = "seeding"
        products = _load_products()
        _state.message = f"Loading {len(products)} products..."
        logger.info("Auto-import: loading %d products from %s", len(products), JSONL_PATH)

        # Load embedding model (synchronous, CPU — runs in asyncio.to_thread in caller)
        import asyncio
        loop = asyncio.get_running_loop()
        model = await loop.run_in_executor(
            None,
            lambda: SentenceTransformer(settings.EMBEDDING_MODEL)
        )
        dim = model.get_sentence_embedding_dimension()
        logger.info("Embedding model ready, dim=%d", dim)

        # Create collection if needed
        if not collection_exists:
            client.create_collection(
                collection_name=settings.QDRANT_COLLECTION,
                vectors_config=qdrant_models.VectorParams(
                    size=dim,
                    distance=qdrant_models.Distance.COSINE,
                ),
            )
            logger.info("Collection '%s' created (%d-dim)", settings.QDRANT_COLLECTION, dim)

        # Vectorize + upsert in batches
        batch_size = 32
        for i in range(0, len(products), batch_size):
            batch = products[i:i + batch_size]
            texts = [_build_doc_text(p) for p in batch]
            payloads = [_build_payload(p) for p in batch]

            embeddings = await loop.run_in_executor(
                None,
                lambda t=texts: model.encode(
                    t, batch_size=batch_size, normalize_embeddings=True
                ),
            )

            points = [
                qdrant_models.PointStruct(
                    id=_product_id_to_uuid(p["product_id"]),
                    vector=emb.tolist(),
                    payload=pl,
                )
                for emb, p, pl in zip(embeddings, batch, payloads)
            ]
            client.upsert(collection_name=settings.QDRANT_COLLECTION, points=points)
            _state.item_count = min(i + batch_size, len(products))
            _state.message = f"Vectorized {_state.item_count}/{len(products)} products"
            logger.info("Auto-import: %s", _state.message)

        _state.collection_exists = True
        _state.item_count = client.count(
            collection_name=settings.QDRANT_COLLECTION, exact=True
        ).count
        _state.message = f"Import complete: {_state.item_count} vectors in Qdrant"
        logger.info("Auto-import: %s", _state.message)
    finally:
        client.close()
