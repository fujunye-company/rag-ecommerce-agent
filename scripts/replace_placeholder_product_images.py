"""
Replace generated product placeholder JPGs with product-name based images.

The script keeps the existing ASCII local URL scheme intact:
    /images/products/<category_slug>/<product_id>.jpg

It only overwrites files marked as likely_placeholder in
apps/backend/data/qdrant/image_name_match_audit.json and writes a manifest for
handoff review:
    apps/backend/data/qdrant/product_image_replacement_manifest.json

Run from repo root:
    py scripts/replace_placeholder_product_images.py
"""
from __future__ import annotations

import html
import io
import json
import re
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib.parse import quote_plus, urlparse

import requests
from PIL import Image, ImageDraw, ImageFont, ImageStat


ROOT = Path(__file__).resolve().parents[1]
QDRANT_DIR = ROOT / "apps" / "backend" / "data" / "qdrant"
AUDIT_PATH = QDRANT_DIR / "image_name_match_audit.json"
MANIFEST_PATH = QDRANT_DIR / "product_image_replacement_manifest.json"
IMAGES_DIR = ROOT / "data" / "images"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/125.0 Safari/537.36"
)

SESSION = requests.Session()
SESSION.headers.update(
    {
        "User-Agent": USER_AGENT,
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.6",
    }
)

BAD_SOURCE_DOMAINS = {
    "placehold.co",
    "picsum.photos",
    "dummyimage.com",
    "via.placeholder.com",
}

IMAGE_EXT_RE = re.compile(r"\.(jpg|jpeg|png|webp)(?:$|\?)", re.I)
TOKEN_RE = re.compile(r"[a-zA-Z0-9][a-zA-Z0-9+._-]{1,}|[\u4e00-\u9fffA-Za-z0-9]+")
GENERIC_TOKENS = {
    "商品图",
    "官方图",
    "男女",
    "通用",
    "可选",
    "新款",
    "经典",
    "家用",
    "适用",
    "透气",
    "舒适",
    "套装",
    "礼盒",
    "多色",
    "蓝牙",
    "无线",
    "智能",
    "运动",
    "休闲",
    "product",
    "official",
    "image",
}


@dataclass
class Candidate:
    image_url: str
    page_url: str = ""
    title: str = ""
    engine: str = ""


def image_path_from_url(image_url: str) -> Path:
    if not image_url.startswith("/images/"):
        raise ValueError(f"Only local image paths are supported: {image_url}")
    return IMAGES_DIR / image_url[len("/images/") :]


def load_placeholder_items() -> list[dict[str, Any]]:
    audit = json.loads(AUDIT_PATH.read_text(encoding="utf-8"))
    placeholders = [item for item in audit.get("items", []) if item.get("kind") == "likely_placeholder"]
    source_records: dict[str, dict[str, Any]] = {}
    for path in (QDRANT_DIR / "seed_products.json", QDRANT_DIR / "products_expanded_100.json"):
        if not path.exists():
            continue
        for record in json.loads(path.read_text(encoding="utf-8")):
            product_id = str(record.get("product_id") or "")
            if product_id:
                source_records[product_id] = record

    merged: list[dict[str, Any]] = []
    for item in placeholders:
        source = source_records.get(str(item.get("product_id")), {})
        if source:
            item = {**item, **{k: source[k] for k in ("title", "category", "image_url") if k in source}}
        merged.append(item)
    return merged


def clean_title(title: str) -> str:
    title = html.unescape(title or "")
    title = re.sub(r"\s+", " ", title).strip()
    # Keep the exact product identity but remove long marketing tails that tend
    # to pollute image search results.
    parts = title.split()
    if len(parts) > 14:
        title = " ".join(parts[:14])
    return title


def build_queries(item: dict[str, Any]) -> list[str]:
    title = clean_title(str(item.get("title") or ""))
    category = str(item.get("category") or "")
    product_id = str(item.get("product_id") or "")
    tokens = relevance_tokens(item)
    compact_tokens = [token for token in tokens if len(token) > 2 or re.search(r"[\u4e00-\u9fff]", token)]
    compact = " ".join(compact_tokens[:5])
    queries = [
        f"{compact} {category} 商品图" if compact else "",
        f"{compact} 商品图" if compact else "",
        f"{title} 商品图",
        f"{title} 官方图",
        f"{title} {category}",
    ]
    if product_id.startswith("JD") and product_id[2:].isdigit():
        sku = product_id[2:]
        queries.insert(0, f"{sku} {compact or title} 京东 商品图")
    # Deduplicate while preserving order.
    seen: set[str] = set()
    out: list[str] = []
    for q in queries:
        q = re.sub(r"\s+", " ", q).strip()
        if q and q not in seen:
            seen.add(q)
            out.append(q)
    return out


def relevance_tokens(item: dict[str, Any]) -> list[str]:
    title = clean_title(str(item.get("title") or ""))
    category = str(item.get("category") or "")
    raw_tokens = TOKEN_RE.findall(f"{title} {category}")
    tokens: list[str] = []
    for token in raw_tokens:
        token = token.strip(" -_/.,，。:：()（）[]【】")
        if len(token) < 2:
            continue
        lowered = token.lower()
        if lowered in GENERIC_TOKENS or token in GENERIC_TOKENS:
            continue
        # Very long Chinese marketing fragments overfit; short brand/model
        # fragments are more useful for search-result validation.
        if re.search(r"[\u4e00-\u9fff]", token) and len(token) > 12:
            continue
        tokens.append(lowered)
    product_id = str(item.get("product_id") or "")
    if product_id.startswith("JD") and product_id[2:].isdigit():
        tokens.insert(0, product_id[2:].lower())
    # Preserve order and favor the beginning of the title.
    seen: set[str] = set()
    out: list[str] = []
    for token in tokens:
        if token not in seen:
            seen.add(token)
            out.append(token)
    return out[:12]


def is_candidate_relevant(item: dict[str, Any], candidate: Candidate) -> tuple[bool, str]:
    tokens = relevance_tokens(item)
    haystack = " ".join([candidate.title, candidate.page_url, candidate.image_url]).lower()
    matches = [token for token in tokens if token in haystack]
    if not tokens:
        return True, "no tokens available"
    if matches:
        # One exact SKU, ASCII model, or brand token is often enough because
        # image CDNs may have sparse titles.
        first = tokens[0]
        if first in matches:
            return True, f"matched leading token: {first}"
        if any(re.search(r"[a-z0-9]", token) and len(token) >= 3 for token in matches):
            return True, f"matched ascii/model token: {matches[0]}"
        if len(matches) >= 2:
            return True, f"matched tokens: {', '.join(matches[:3])}"
    return False, f"no strong token match; expected one of {tokens[:6]}"


def duckduckgo_vqd(query: str) -> str | None:
    resp = SESSION.get(
        "https://duckduckgo.com/",
        params={"q": query, "iax": "images", "ia": "images"},
        timeout=20,
    )
    resp.raise_for_status()
    patterns = [
        r'vqd="([^"]+)"',
        r"vqd='([^']+)'",
        r"vqd=([^&]+)&",
    ]
    for pattern in patterns:
        match = re.search(pattern, resp.text)
        if match:
            return match.group(1)
    return None


def duckduckgo_candidates(query: str) -> list[Candidate]:
    vqd = duckduckgo_vqd(query)
    if not vqd:
        return []
    resp = SESSION.get(
        "https://duckduckgo.com/i.js",
        params={"l": "wt-wt", "o": "json", "q": query, "vqd": vqd, "f": ",,,", "p": "1"},
        timeout=20,
    )
    resp.raise_for_status()
    data = json.loads(resp.text, strict=False)
    candidates: list[Candidate] = []
    for row in data.get("results", []):
        url = row.get("image") or ""
        if not url:
            continue
        candidates.append(
            Candidate(
                image_url=url,
                page_url=row.get("url") or "",
                title=row.get("title") or "",
                engine="duckduckgo",
            )
        )
    return candidates


def bing_candidates(query: str) -> list[Candidate]:
    resp = SESSION.get(
        "https://www.bing.com/images/search",
        params={"q": query, "first": "1"},
        timeout=20,
    )
    resp.raise_for_status()
    candidates: list[Candidate] = []
    for match in re.finditer(r'm="([^"]+)"', resp.text):
        raw = html.unescape(match.group(1))
        try:
            data = json.loads(raw)
        except json.JSONDecodeError:
            continue
        url = data.get("murl") or ""
        if url:
            candidates.append(
                Candidate(
                    image_url=url,
                    page_url=data.get("purl") or "",
                    title=data.get("t") or "",
                    engine="bing",
                )
            )
    if candidates:
        return candidates

    # Older/newer Bing markup sometimes exposes murl directly.
    for match in re.finditer(r'"murl":"(.*?)"', resp.text):
        url = match.group(1).encode("utf-8").decode("unicode_escape")
        candidates.append(Candidate(image_url=url, engine="bing"))
    return candidates


def baidu_candidates(query: str) -> list[Candidate]:
    resp = SESSION.get(
        "https://image.baidu.com/search/acjson",
        params={
            "tn": "resultjson_com",
            "ipn": "rj",
            "ct": "201326592",
            "fp": "result",
            "queryWord": query,
            "cl": "2",
            "lm": "-1",
            "ie": "utf-8",
            "oe": "utf-8",
            "st": "-1",
            "ic": "0",
            "word": query,
            "face": "0",
            "istype": "2",
            "pn": "0",
            "rn": "30",
        },
        headers={"Referer": "https://image.baidu.com/"},
        timeout=20,
    )
    resp.raise_for_status()
    data = json.loads(resp.text, strict=False)
    candidates: list[Candidate] = []
    for row in data.get("data", []):
        url = row.get("thumbURL") or row.get("middleURL") or row.get("objURL") or ""
        if not url:
            continue
        candidates.append(
            Candidate(
                image_url=url,
                page_url=row.get("fromURL") or row.get("replaceUrl", [{}])[0].get("ObjURL", ""),
                title=html.unescape(row.get("fromPageTitleEnc") or row.get("fromPageTitle") or ""),
                engine="baidu",
            )
        )
    return candidates


def domain_of(url: str) -> str:
    host = urlparse(url).netloc.lower()
    return host[4:] if host.startswith("www.") else host


def looks_like_bad_source(url: str) -> bool:
    domain = domain_of(url)
    return any(domain == bad or domain.endswith("." + bad) for bad in BAD_SOURCE_DOMAINS)


def fetch_image(candidate: Candidate) -> tuple[bytes, tuple[int, int], str]:
    url = candidate.image_url
    if not url.startswith(("http://", "https://")):
        raise ValueError("non-http image URL")
    if looks_like_bad_source(url):
        raise ValueError("placeholder source domain")
    upper_url = url.upper()
    is_baidu_image = domain_of(url).endswith("baidu.com") and ("/IT/" in upper_url or "F=JPEG" in upper_url)
    if not is_baidu_image and not IMAGE_EXT_RE.search(url) and "image" not in url.lower() and "img" not in domain_of(url):
        # Keep this as a soft filter. Many CDN URLs are extensionless, so only
        # reject when the URL also lacks common image/CDN hints.
        raise ValueError("URL does not look like an image")

    resp = SESSION.get(url, timeout=25, stream=True, headers={"Referer": candidate.page_url or "https://duckduckgo.com/"})
    resp.raise_for_status()
    content_type = (resp.headers.get("content-type") or "").lower()
    if "svg" in content_type:
        raise ValueError("SVG is not accepted for Android Coil setup")
    data = resp.content
    if len(data) < 8_000:
        raise ValueError("image too small")

    with Image.open(io.BytesIO(data)) as img:
        img = img.convert("RGB")
        width, height = img.size
        if width < 260 or height < 260:
            raise ValueError(f"image dimensions too small: {width}x{height}")
        if max(width, height) / max(1, min(width, height)) > 3.2:
            raise ValueError(f"image is too panoramic: {width}x{height}")
        stat = ImageStat.Stat(img.resize((32, 32)))
        if max(stat.stddev) < 4.0:
            raise ValueError("image appears nearly blank")

        # Center-crop to a square product-card friendly JPG.
        side = min(width, height)
        left = (width - side) // 2
        top = (height - side) // 2
        img = img.crop((left, top, left + side, top + side)).resize((800, 800), Image.Resampling.LANCZOS)
        buf = io.BytesIO()
        img.save(buf, "JPEG", quality=90, optimize=True)
        out = buf.getvalue()
    if len(out) < 12_000:
        raise ValueError("converted JPG too small")
    return out, (800, 800), content_type


def fallback_placeholder(title: str, category: str) -> bytes:
    seed = sum(ord(ch) for ch in f"{title}{category}")
    bg = (
        225 + seed % 18,
        230 + (seed // 7) % 16,
        235 + (seed // 13) % 14,
    )
    accent = (
        80 + seed % 90,
        96 + (seed // 5) % 90,
        110 + (seed // 11) % 90,
    )
    img = Image.new("RGB", (800, 800), bg)
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([92, 110, 708, 650], radius=52, fill=(255, 255, 255), outline=accent, width=8)
    draw.ellipse([282, 190, 518, 426], fill=accent)
    draw.rounded_rectangle([224, 476, 576, 544], radius=24, fill=accent)
    label = clean_title(title)[:30] or category or "Product"
    font = ImageFont.load_default()
    draw.text((400, 700), label, fill=(60, 72, 86), anchor="mm", font=font)
    buf = io.BytesIO()
    img.save(buf, "JPEG", quality=88, optimize=True)
    return buf.getvalue()


def candidate_stream(query: str) -> list[Candidate]:
    candidates: list[Candidate] = []
    errors: list[str] = []
    for engine in (baidu_candidates, bing_candidates):
        try:
            candidates.extend(engine(query))
        except Exception as exc:  # noqa: BLE001 - manifest captures operational failures.
            errors.append(f"{engine.__name__}: {exc}")
    seen: set[str] = set()
    unique: list[Candidate] = []
    for candidate in candidates:
        key = candidate.image_url
        if key in seen:
            continue
        seen.add(key)
        unique.append(candidate)
    return unique


def replace_one(item: dict[str, Any], force: bool = False) -> dict[str, Any]:
    target = image_path_from_url(str(item["image_url"]))
    target.parent.mkdir(parents=True, exist_ok=True)
    queries = build_queries(item)
    attempted: list[dict[str, str]] = []

    for query in queries:
        candidates = candidate_stream(query)
        # Search engines often put good results early; cap attempts to avoid
        # wasting time on irrelevant deep results.
        for candidate in candidates[:18]:
            relevant, relevance_reason = is_candidate_relevant(item, candidate)
            if not relevant:
                attempted.append(
                    {
                        "query": query,
                        "engine": candidate.engine,
                        "image_url": candidate.image_url,
                        "reason": relevance_reason,
                    }
                )
                continue
            try:
                data, size, content_type = fetch_image(candidate)
            except Exception as exc:  # noqa: BLE001
                attempted.append(
                    {
                        "query": query,
                        "engine": candidate.engine,
                        "image_url": candidate.image_url,
                        "reason": str(exc),
                    }
                )
                continue

            target.write_bytes(data)
            return {
                "product_id": item.get("product_id"),
                "title": item.get("title"),
                "image_url": item.get("image_url"),
                "target_path": str(target),
                "status": "replaced",
                "query": query,
                "source_image_url": candidate.image_url,
                "source_page_url": candidate.page_url,
                "source_title": candidate.title,
                "source_engine": candidate.engine,
                "source_domain": domain_of(candidate.image_url),
                "relevance": relevance_reason,
                "size": list(size),
                "bytes": len(data),
                "content_type": content_type,
                "attempted_failures": attempted[:8],
            }

        time.sleep(0.2)

    target.write_bytes(fallback_placeholder(str(item.get("title") or ""), str(item.get("category") or "")))
    return {
        "product_id": item.get("product_id"),
        "title": item.get("title"),
        "image_url": item.get("image_url"),
        "target_path": str(target),
        "status": "failed_placeholder_restored",
        "queries": queries,
        "attempted_failures": attempted[:20],
    }


def main() -> None:
    items = load_placeholder_items()
    manifest_items: list[dict[str, Any]] = []
    replaced = 0
    failed = 0
    for idx, item in enumerate(items, 1):
        product_id = str(item.get("product_id"))
        print(f"[{idx:03d}/{len(items)}] {product_id} {clean_title(str(item.get('title') or ''))[:56]}")
        result = replace_one(item)
        manifest_items.append(result)
        if result["status"] == "replaced":
            replaced += 1
            print(f"  -> replaced from {result.get('source_engine')} {result.get('source_domain')}")
        else:
            failed += 1
            print("  -> failed")
        time.sleep(0.45)

        MANIFEST_PATH.write_text(
            json.dumps(
                {
                    "stats": {
                        "total_placeholders": len(items),
                        "replaced_this_run": replaced,
                        "failed_this_run": failed,
                    },
                    "items": manifest_items,
                },
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )

    total_replaced = sum(1 for row in manifest_items if row.get("status") == "replaced")
    total_failed = sum(1 for row in manifest_items if str(row.get("status")).startswith("failed"))
    MANIFEST_PATH.write_text(
        json.dumps(
            {
                "stats": {
                    "total_placeholders": len(items),
                    "total_replaced": total_replaced,
                    "total_failed": total_failed,
                    "replaced_this_run": replaced,
                    "failed_this_run": failed,
                },
                "items": manifest_items,
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    print(json.dumps(json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))["stats"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
