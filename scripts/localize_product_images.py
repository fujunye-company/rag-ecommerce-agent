"""
Localize all product image URLs to ASCII-only local JPG paths.

This script is intentionally operational, not a one-off migration:
- copies existing local images from /images/<Chinese dir>/... to /images/products/<slug>/...
- generates simple JPG fallbacks for remote/SVG-backed products
- updates PostgreSQL products.image_urls
- updates Qdrant product payload image_url/image_urls
- rewrites seed JSON/JSONL/SQL data files so future imports keep ASCII paths

Run from repo root:
    py scripts/localize_product_images.py
"""
from __future__ import annotations

import asyncio
import json
import re
import shutil
import sys
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
BACKEND_DIR = ROOT / "apps" / "backend"
IMAGES_DIR = ROOT / "data" / "images"
PRODUCTS_DIR = IMAGES_DIR / "products"

sys.path.insert(0, str(BACKEND_DIR))

from app.core.database import engine  # noqa: E402
from app.core.config import settings  # noqa: E402
from qdrant_client import AsyncQdrantClient  # noqa: E402
from sqlalchemy import text  # noqa: E402


CATEGORY_SLUGS: dict[str, str] = {
    "耳机": "audio",
    "音箱": "audio",
    "手机": "phones",
    "平板": "tablets",
    "手表": "wearables",
    "相机": "cameras",
    "配件": "digital_accessories",
    "电脑": "computers",
    "笔记本": "computers",
    "办公电子": "office_electronics",
    "休闲鞋": "casual_shoes",
    "跑鞋": "running_shoes",
    "运动鞋": "sports_shoes",
    "瑜伽用品": "yoga",
    "瑜伽": "yoga",
    "跳绳": "jump_rope",
    "力量训练": "strength_training",
    "运动恢复": "recovery",
    "运动监测": "sports_wearables",
    "球拍": "rackets",
    "球类": "balls",
    "户外": "outdoor",
    "登山包": "hiking_bags",
    "T恤": "tshirts",
    "裙装": "dresses",
    "衬衫": "shirts",
    "裤装": "pants",
    "外套": "jackets",
    "运动服": "sportswear",
    "家居服": "loungewear",
    "行李箱": "luggage",
    "双肩包": "backpacks",
    "单肩包": "shoulder_bags",
    "斜挎包": "crossbody_bags",
    "精华液": "essence",
    "面霜": "cream",
    "眼霜": "eye_cream",
    "口红": "lipstick",
    "粉底": "foundation",
    "防晒": "sunscreen",
    "洗发水": "shampoo",
    "沐浴露": "body_wash",
    "洗手液": "hand_wash",
    "坚果": "nuts",
    "肉干": "jerky",
    "零食礼盒": "snack_gifts",
    "膨化食品": "puffed_snacks",
    "巧克力": "chocolate",
    "方便面": "instant_noodles",
    "乳制品": "dairy",
    "饮用水": "water",
    "饮料": "drinks",
    "咖啡": "coffee",
    "零食": "snacks",
    "饼干": "biscuits",
    "电饭煲": "rice_cookers",
    "空气炸锅": "air_fryers",
    "料理机": "blenders",
    "加湿器": "humidifiers",
    "枕头": "pillows",
    "纸巾": "tissues",
    "床品": "bedding",
    "吸尘器": "vacuums",
    "扫地机器人": "robot_vacuums",
    "净水器": "water_purifiers",
    "小说": "books",
    "教材": "textbooks",
    "宠物食品": "pet_food",
    "宠物用品": "pet_supplies",
    "宠物零食": "pet_snacks",
    "宠物美容": "pet_grooming",
    "台灯": "desk_lamps",
    "键盘": "keyboards",
    "办公椅": "office_chairs",
    "打印机": "printers",
    "办公配件": "office_accessories",
    "文具": "stationery",
    "婴儿家具": "baby_furniture",
    "积木": "building_blocks",
    "游戏": "games",
    "收藏品": "collectibles",
    "科教玩具": "educational_toys",
    "模型": "models",
    "过家家": "pretend_play",
    "桌游": "board_games",
    "车载电子": "car_electronics",
    "车载工具": "car_tools",
    "车内装饰": "car_interior",
    "汽车养护": "car_care",
    "汽车配件": "car_parts",
    "车品": "car_goods",
    "纸尿裤": "diapers",
    "喂养用品": "feeding",
    "婴儿湿巾": "baby_wipes",
    "婴儿健康": "baby_health",
    "婴儿玩具": "baby_toys",
    "保健品": "supplements",
    "健康设备": "health_devices",
    "个人护理": "personal_care",
    "口腔护理": "oral_care",
}


def slug_for(category: str | None) -> str:
    if not category:
        return "misc"
    if category in CATEGORY_SLUGS:
        return CATEGORY_SLUGS[category]
    cleaned = re.sub(r"[^a-zA-Z0-9]+", "_", category).strip("_").lower()
    return cleaned or "misc"


def safe_name(value: str | None) -> str:
    value = value or "product"
    value = re.sub(r"[^a-zA-Z0-9._-]+", "_", value)
    value = value.strip("._-")
    return value[:80] or "product"


def target_url(product_id: str, category: str | None) -> str:
    return f"/images/products/{slug_for(category)}/{safe_name(product_id)}.jpg"


def local_path_from_url(url: str) -> Path | None:
    if not url.startswith("/images/"):
        return None
    return IMAGES_DIR / url[len("/images/") :]


def target_path(url: str) -> Path:
    assert url.startswith("/images/")
    return IMAGES_DIR / url[len("/images/") :]


def copy_or_generate_image(source_url: str | None, target: Path, title: str, category: str | None) -> str:
    target.parent.mkdir(parents=True, exist_ok=True)
    source = local_path_from_url(source_url or "")
    if source and source.exists() and source.is_file():
        shutil.copy2(source, target)
        return "copied"

    # Generate a deterministic local JPG for remote/SVG/missing sources.
    seed = sum(ord(ch) for ch in (title + (category or "")))
    hue = seed % 360
    bg = hsv_to_rgb(hue / 360, 0.22, 0.92)
    accent = hsv_to_rgb(((hue + 38) % 360) / 360, 0.38, 0.78)
    img = Image.new("RGB", (640, 640), bg)
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([72, 92, 568, 548], radius=48, fill=(255, 255, 255), outline=accent, width=8)
    draw.ellipse([228, 164, 412, 348], fill=accent)
    draw.rounded_rectangle([176, 380, 464, 430], radius=20, fill=accent)
    draw.rounded_rectangle([216, 454, 424, 488], radius=16, fill=(230, 236, 242))
    label = slug_for(category).replace("_", " ").title()
    font = ImageFont.load_default()
    draw.text((320, 520), label[:32], fill=(70, 82, 96), anchor="mm", font=font)
    img.save(target, "JPEG", quality=88, optimize=True)
    return "generated"


def hsv_to_rgb(h: float, s: float, v: float) -> tuple[int, int, int]:
    i = int(h * 6)
    f = h * 6 - i
    p = v * (1 - s)
    q = v * (1 - f * s)
    t = v * (1 - (1 - f) * s)
    r, g, b = {
        0: (v, t, p),
        1: (q, v, p),
        2: (p, v, t),
        3: (p, q, v),
        4: (t, p, v),
    }.get(i % 6, (v, p, q))
    return int(r * 255), int(g * 255), int(b * 255)


def rewrite_product_record(prod: dict[str, Any]) -> tuple[str, str]:
    pid = str(prod.get("product_id") or prod.get("id") or safe_name(prod.get("title")))
    category = prod.get("category")
    old_url = prod.get("image_url") or ((prod.get("image_urls") or [None])[0])
    new_url = target_url(pid, category)
    action = copy_or_generate_image(old_url, target_path(new_url), prod.get("title", pid), category)
    prod["image_url"] = new_url
    prod["image_urls"] = [new_url]
    return str(old_url or ""), action


def load_json_array(path: Path) -> list[dict[str, Any]]:
    return json.loads(path.read_text(encoding="utf-8"))


def save_json_array(path: Path, data: list[dict[str, Any]]) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def rewrite_json_array(path: Path) -> dict[str, int]:
    if not path.exists():
        return {}
    data = load_json_array(path)
    counts = {"records": 0, "copied": 0, "generated": 0}
    for prod in data:
        old, action = rewrite_product_record(prod)
        if old:
            counts["records"] += 1
            counts[action] += 1
    save_json_array(path, data)
    return counts


def rewrite_jsonl(path: Path) -> dict[str, int]:
    if not path.exists():
        return {}
    counts = {"records": 0, "copied": 0, "generated": 0}
    out = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        prod = json.loads(line)
        old, action = rewrite_product_record(prod)
        if old:
            counts["records"] += 1
            counts[action] += 1
        out.append(json.dumps(prod, ensure_ascii=False, separators=(",", ":")))
    path.write_text("\n".join(out) + "\n", encoding="utf-8")
    return counts


def rewrite_sql_paths(path: Path, replacements: dict[str, str]) -> int:
    if not path.exists():
        return 0
    text_body = path.read_text(encoding="utf-8")
    changed = 0
    for old, new in replacements.items():
        if old and old in text_body:
            text_body = text_body.replace(old, new)
            changed += 1
    path.write_text(text_body, encoding="utf-8")
    return changed


async def rewrite_pg() -> dict[str, int]:
    counts = {"records": 0, "copied": 0, "generated": 0}
    async with engine.begin() as conn:
        rows = (await conn.execute(text(
            "SELECT id::text, source_product_id, title, category, image_urls FROM products"
        ))).all()
        for row in rows:
            product_id = row.source_product_id or row.id
            old_url = (row.image_urls or [None])[0]
            new_url = target_url(product_id, row.category)
            action = copy_or_generate_image(old_url, target_path(new_url), row.title, row.category)
            await conn.execute(
                text("UPDATE products SET image_urls = :urls WHERE id = :id"),
                {"urls": [new_url], "id": row.id},
            )
            counts["records"] += 1
            counts[action] += 1
    return counts


async def rewrite_qdrant() -> dict[str, int]:
    counts = {"records": 0, "copied": 0, "generated": 0}
    client = AsyncQdrantClient(url=settings.QDRANT_URL)
    try:
        offset = None
        while True:
            points, offset = await client.scroll(
                collection_name=settings.QDRANT_COLLECTION,
                offset=offset,
                limit=256,
                with_payload=True,
                with_vectors=False,
            )
            if not points:
                break
            for point in points:
                payload = point.payload or {}
                pid = str(payload.get("product_id") or point.id)
                old_url = payload.get("image_url") or ((payload.get("image_urls") or [None])[0])
                new_url = target_url(pid, payload.get("category"))
                action = copy_or_generate_image(
                    old_url,
                    target_path(new_url),
                    str(payload.get("title") or pid),
                    payload.get("category"),
                )
                await client.set_payload(
                    collection_name=settings.QDRANT_COLLECTION,
                    payload={"image_url": new_url, "image_urls": [new_url]},
                    points=[point.id],
                )
                counts["records"] += 1
                counts[action] += 1
            if offset is None:
                break
    finally:
        await client.close()
    return counts


def collect_replacements_from_jsonl(path: Path) -> dict[str, str]:
    replacements: dict[str, str] = {}
    if not path.exists():
        return replacements
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        prod = json.loads(line)
        old = prod.get("image_url") or ((prod.get("image_urls") or [None])[0])
        if old:
            replacements[old] = target_url(str(prod.get("product_id") or ""), prod.get("category"))
    return replacements


async def main() -> None:
    if not IMAGES_DIR.exists():
        raise SystemExit(f"Missing image directory: {IMAGES_DIR}")

    qdrant_dir = BACKEND_DIR / "data" / "qdrant"
    replacements = collect_replacements_from_jsonl(qdrant_dir / "products_expanded_100.jsonl")

    results: dict[str, Any] = {}
    results["seed_products.json"] = rewrite_json_array(qdrant_dir / "seed_products.json")
    results["products_expanded_100.json"] = rewrite_json_array(qdrant_dir / "products_expanded_100.json")
    results["products_expanded_100.jsonl"] = rewrite_jsonl(qdrant_dir / "products_expanded_100.jsonl")
    results["import_expanded.sql_replacements"] = rewrite_sql_paths(qdrant_dir / "import_expanded.sql", replacements)
    results["postgres"] = await rewrite_pg()
    results["qdrant"] = await rewrite_qdrant()

    print(json.dumps(results, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    asyncio.run(main())
