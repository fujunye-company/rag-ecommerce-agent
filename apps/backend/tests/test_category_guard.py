from app.services.agent import (
    _filter_chunks_by_requested_category,
    _filter_products_by_requested_category,
)


def test_tablet_category_guard_drops_ipad_accessories():
    chunks = [
        {"payload": {"product_id": "ipad", "title": "iPad 11英寸 平板电脑", "category": "平板"}},
        {"payload": {"product_id": "film", "title": "闪魔 iPad钢化膜", "category": "配件"}},
        {"payload": {"product_id": "matepad", "title": "华为 MatePad 平板", "category": "平板电脑"}},
    ]

    filtered = _filter_chunks_by_requested_category(chunks, "平板")

    assert [c["payload"]["product_id"] for c in filtered] == ["ipad", "matepad"]


def test_rank_candidates_keep_only_requested_tablet_category():
    products = [
        {"product_id": "xiaomi", "title": "小米平板8 Pro", "category": "平板"},
        {"product_id": "film", "title": "iPad钢化膜", "category": "配件"},
    ]

    filtered = _filter_products_by_requested_category(products, "平板")

    assert [p["product_id"] for p in filtered] == ["xiaomi"]


def test_category_guard_does_not_filter_unlisted_categories():
    chunks = [
        {"payload": {"product_id": "running", "title": "跑鞋", "category": "跑鞋"}},
        {"payload": {"product_id": "sport", "title": "运动鞋", "category": "运动鞋"}},
    ]

    assert _filter_chunks_by_requested_category(chunks, "鞋") == chunks
