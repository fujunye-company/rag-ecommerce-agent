"""Shared exclusion helpers for anti-selection queries."""

from __future__ import annotations

import re
from typing import Any


JAPANESE_BRAND_TERMS = {
    "日系", "日本", "日牌",
    "资生堂", "Shiseido", "安热沙", "Anessa", "安耐晒",
    "SK-II", "SKII", "SK2", "肌肤之钥", "CPB", "黛珂", "Decorte",
    "植村秀", "Shu Uemura", "高丝", "Kose", "花王", "Kao",
    "珂润", "Curel", "无印良品", "MUJI", "索尼", "Sony",
    "任天堂", "Nintendo", "佳能", "Canon", "尼康", "Nikon",
    "松下", "Panasonic",
}

BRAND_FAMILY_ALIASES: dict[str, set[str]] = {
    "日系": JAPANESE_BRAND_TERMS,
    "日本": JAPANESE_BRAND_TERMS,
    "日牌": JAPANESE_BRAND_TERMS,
}

TEXT_EXCLUSION_ALIASES: dict[str, set[str]] = {
    "酒精": {"酒精", "乙醇", "alcohol", "ethanol"},
    "含酒精": {"酒精", "乙醇", "alcohol", "ethanol"},
    "日系": {term.lower() for term in JAPANESE_BRAND_TERMS},
    "日本": {term.lower() for term in JAPANESE_BRAND_TERMS},
    "日牌": {term.lower() for term in JAPANESE_BRAND_TERMS},
}


def expand_exclude_brands(brands: list[str] | None) -> list[str]:
    expanded: set[str] = set()
    for raw in brands or []:
        term = str(raw).strip()
        if not term:
            continue
        expanded.add(term)
        expanded.update(BRAND_FAMILY_ALIASES.get(term, set()))
    return sorted(expanded)


def expand_exclude_text_terms(terms: list[str] | None) -> list[str]:
    expanded: set[str] = set()
    for raw in terms or []:
        term = str(raw).strip()
        if not term:
            continue
        expanded.add(term)
        expanded.update(TEXT_EXCLUSION_ALIASES.get(term, set()))
    return sorted(expanded)


def normalize_exclusion_slots(slots: dict[str, Any]) -> dict[str, Any]:
    """Return a copy with brand families and text aliases expanded."""
    normalized = dict(slots or {})
    normalized["exclude_brands"] = expand_exclude_brands(normalized.get("exclude_brands") or [])
    normalized["exclude_text_terms"] = expand_exclude_text_terms(normalized.get("exclude_text_terms") or [])
    by_cat = normalized.get("exclude_by_category") or {}
    if by_cat:
        normalized["exclude_by_category"] = {
            cat: expand_exclude_brands(brands)
            for cat, brands in by_cat.items()
        }
    return normalized


def _lower_blob(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, dict):
        return " ".join(f"{k} {v}" for k, v in value.items()).lower()
    if isinstance(value, list):
        return " ".join(str(v) for v in value).lower()
    return str(value).lower()


def product_violates_exclusions(product: dict[str, Any], slots_or_prefs: dict[str, Any]) -> bool:
    """Hard-filter a product against explicit user exclusions."""
    slots = normalize_exclusion_slots(slots_or_prefs or {})
    brand = str(product.get("brand") or "").strip().lower().replace(" ", "")
    title = _lower_blob(product.get("title"))
    category = _lower_blob(product.get("category"))
    highlights = _lower_blob(product.get("highlights"))
    attrs = _lower_blob(product.get("attributes"))
    description = _lower_blob(product.get("description"))
    haystack = " ".join([title, category, brand, highlights, attrs, description])

    for raw in slots.get("exclude_brands") or []:
        term = str(raw).strip().lower().replace(" ", "")
        if term and (term == brand or term in brand or term in haystack):
            return True

    for raw in slots.get("exclude_categories") or []:
        term = str(raw).strip().lower()
        if term and term in category:
            return True

    for key, val in (slots.get("exclude_attributes") or {}).items():
        attrs_dict = product.get("attributes") or {}
        actual = attrs_dict.get(key)
        if actual is not None and str(val).lower() in str(actual).lower():
            return True
        if str(val).strip() and str(val).lower() in attrs:
            return True

    for raw in slots.get("exclude_text_terms") or []:
        term = str(raw).strip().lower()
        if term and re.search(re.escape(term), haystack, flags=re.IGNORECASE):
            return True

    return False
