from app.services.intent import _keyword_extract_negation


def test_brand_negation_does_not_generate_ascii_prefix_filters():
    result = _keyword_extract_negation("排除 vivo")

    assert "vivo" in {b.lower() for b in result["exclude_brands"]}
    assert "vivo" in {t.lower() for t in result["exclude_text_terms"]}
    assert "vi" not in {t.lower() for t in result["exclude_text_terms"]}
    assert "viv" not in {t.lower() for t in result["exclude_text_terms"]}


def test_chinese_brand_alias_goes_to_exclude_brands():
    result = _keyword_extract_negation("不要苹果")

    assert "Apple" in result["exclude_brands"]
    assert "brand" not in result["exclude_attributes"]
