"""
P2 — Android Mock 同步脚本
从 products_expanded_100.json 生成 Kotlin MockProducts.kt

用法: python sync_mock_from_dataset.py
输出: 更新 MockProducts.kt (备份旧文件为 .bak)
"""

import json
import os
import random
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
PRODUCTS_JSON = BASE_DIR.parent / "data" / "qdrant" / "products_expanded_100.json"
MOCK_KT = (
    BASE_DIR.parent.parent
    / "android" / "app" / "src" / "main" / "java" / "com" / "shopping" / "agent"
    / "data" / "mock" / "MockProducts.kt"
)

def _escape(s):
    """转义 Kotlin 字符串"""
    if s is None:
        return "null"
    return '"' + str(s).replace("\\", "\\\\").replace('"', '\\"').replace("\n", " ") + '"'

def _map_list(lst):
    if not lst:
        return "emptyList()"
    items = ", ".join(_escape(item) for item in lst)
    return f"listOf({items})"

def _map_attributes(attrs):
    if not attrs:
        return "emptyMap()"
    pairs = ", ".join(f'{_escape(k)} to {_escape(v)}' for k, v in attrs.items())
    return f"mapOf({pairs})"

def generate_kotlin(products):
    """生成 MockProducts.kt 文件内容"""
    # 按品类分组，每个品类选 1-3 个代表性商品
    by_cat = {}
    for p in products:
        cat = p["category"]
        by_cat.setdefault(cat, []).append(p)

    # 从每个品类选最多 3 个商品，共约 30 条
    selected = []
    rng = random.Random(42)
    for cat, items in sorted(by_cat.items()):
        n = min(3, len(items))
        picks = rng.sample(items, n)
        selected.append((cat, picks))

    lines = []
    lines.append("package com.shopping.agent.data.mock")
    lines.append("")
    lines.append("import com.shopping.agent.data.model.Product")
    lines.append("")
    lines.append("/**")
    lines.append(" * Mock 商品数据 — 对齐 DATA-CONTRACT.md v1.0")
    lines.append(f" * 从 100 条扩充数据自动生成，覆盖 {len(by_cat)} 个品类")
    lines.append(" */")
    lines.append("val mockProducts = listOf(")

    total = 0
    for cat, items in selected:
        lines.append(f"    // ── {cat} ({len(items)}) ──")
        for p in items:
            total += 1
            pid = _escape(p["product_id"])
            title = _escape(p["title"])
            price = p["price"]
            img = _escape(p.get("image_url"))
            imgs = _map_list(p.get("image_urls", []))
            category = _escape(p["category"])
            brand = _escape(p.get("brand", ""))
            source = _escape(p.get("source", ""))
            rc = p.get("rating_count", 0)
            rating = f"{p.get('rating', 3.0)}f"
            hl_str = " | ".join(p.get("highlights", [])[:3])
            rank = _escape(hl_str[:60] if hl_str else "")
            attrs = _map_attributes(p.get("attributes", {}))
            scenarios = _map_list(p.get("scenarios", []))

            lines.append("        Product(")
            lines.append(f"            productId = {pid}, title = {title},")
            lines.append(f"            price = {price},")
            lines.append(f"            imageUrl = {img}, imageUrls = {imgs},")
            lines.append(f"            category = {category}, brand = {brand}, source = {source},")
            lines.append(f"            ratingCount = {rc}, rating = {rating},")
            lines.append(f"            rankReason = {rank},")
            lines.append(f"            attributes = {attrs},")
            lines.append(f"        ),")

    lines.append(")")
    lines.append("")

    print(f"生成 {total} 条 Mock 数据 ({len(selected)} 个品类)")

    return "\n".join(lines)


def main():
    with open(PRODUCTS_JSON, "r", encoding="utf-8") as f:
        products = json.load(f)
    print(f"读取 {len(products)} 条商品数据")

    kotlin_code = generate_kotlin(products)

    # 备份旧文件
    if MOCK_KT.exists():
        bak_path = MOCK_KT.with_suffix(".kt.bak")
        bak_path.write_text(MOCK_KT.read_text(encoding="utf-8"), encoding="utf-8")
        print(f"备份旧文件: {bak_path}")

    MOCK_KT.write_text(kotlin_code, encoding="utf-8")
    print(f"写入: {MOCK_KT}")
    print("P2 完成! 请运行 ./gradlew assembleDebug 验证编译")


if __name__ == "__main__":
    main()
