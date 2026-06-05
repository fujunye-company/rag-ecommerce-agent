from app.services.agent import _build_rewrite_base, _previous_slots_from_state


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
