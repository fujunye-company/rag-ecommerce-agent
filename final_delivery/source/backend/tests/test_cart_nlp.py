"""Natural-language cart helper tests."""

from app.services.agent import _extract_cart_action, _parse_quantity


def test_cart_quantity_action_detection():
    assert _extract_cart_action("把第一个数量改成2") == "quantity"
    assert _parse_quantity("把第一个数量改成2") == 2


def test_cart_add_is_not_misclassified_as_quantity():
    assert _extract_cart_action("把第一个加到购物车") == "add"


def test_cart_quantity_chinese_number():
    assert _parse_quantity("第二个改为两件") == 2
