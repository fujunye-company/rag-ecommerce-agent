from app.services.agent import (
    _build_rewrite_base,
    _previous_slots_from_state,
    _prune_previous_slots_for_category_change,
)


def test_previous_slots_reads_nested_state():
    state = {"slots": {"category": "平板", "exclude_by_category": {"平板": ["Apple"]}}}

    assert _previous_slots_from_state(state)["category"] == "平板"
    assert _previous_slots_from_state(state)["exclude_by_category"] == {"平板": ["Apple"]}


def test_previous_slots_reads_legacy_top_level_state():
    state = {"category": "平板", "price_max": 3000}

    slots = _previous_slots_from_state(state)

    assert slots["category"] == "平板"
    assert slots["price_max"] == 3000


def test_nested_slots_win_over_legacy_top_level_state():
    state = {"slots": {"category": "平板"}, "category": "手机"}

    assert _previous_slots_from_state(state)["category"] == "平板"


def test_negation_rewrite_base_uses_inherited_category():
    base = _build_rewrite_base(
        query="排除 Apple",
        expanded="排除 Apple",
        slots={"category": "平板", "exclude_by_category": {"平板": ["Apple"]}},
        has_negation=True,
        negation={"positive_query": "Apple"},
    )

    assert base == "平板 热门推荐 同类商品"


def test_category_change_prunes_category_specific_preferences():
    prev = {
        "category": "手机",
        "brand_preference": "Apple",
        "attributes": {"性能": "强"},
        "scenario": "拍照",
        "price_max": 3000,
        "exclude_brands": ["Huawei"],
        "exclude_attributes": {"颜色": "黑色"},
        "exclude_text_terms": ["苹果"],
        "exclude_by_category": {"手机": ["Apple"]},
    }

    pruned = _prune_previous_slots_for_category_change(prev, {"category": "手表"})

    assert pruned["price_max"] == 3000
    assert pruned["exclude_by_category"] == {"手机": ["Apple"]}
    assert "category" not in pruned
    assert "brand_preference" not in pruned
    assert "attributes" not in pruned
    assert "exclude_brands" not in pruned
    assert "exclude_text_terms" not in pruned


def test_equivalent_category_keeps_preferences():
    prev = {"category": "手表", "brand_preference": "Huawei", "attributes": {"续航": "长"}}

    kept = _prune_previous_slots_for_category_change(prev, {"category": "智能手表"})

    assert kept == prev
