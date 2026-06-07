"""
P3 — 图片验证器
检查 apps/backend/data/images/ 下商品图片的完整性

用法: python image_validator.py [--dir apps/backend/data/images]
"""

import argparse
import hashlib
import json
import os
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    Image = None


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


def validate_images(images_dir):
    """验证图片目录，返回报告"""
    img_dir = Path(images_dir)
    if not img_dir.exists():
        return {"error": f"目录不存在: {img_dir}"}

    report = {
        "total_files": 0,
        "total_size_bytes": 0,
        "duplicates": [],
        "small_files": [],     # < 10KB 可能损坏
        "large_files": [],     # > 500KB 应压缩
        "no_dimensions": [],   # PIL 不可用或读取失败
        "by_category": {},
        "hash_map": {},        # hash → [paths]
    }

    for img_file in sorted(img_dir.rglob("*.jpg")):
        cat = img_file.parent.name
        report["by_category"].setdefault(cat, 0)
        report["by_category"][cat] += 1

        size = img_file.stat().st_size
        report["total_files"] += 1
        report["total_size_bytes"] += size

        if size < 10240:
            report["small_files"].append(str(img_file))
        if size > 512000:
            report["large_files"].append(str(img_file))

        # 尺寸检查
        if Image:
            try:
                with Image.open(img_file) as im:
                    w, h = im.size
            except Exception:
                report["no_dimensions"].append(str(img_file))
        else:
            report["no_dimensions"].append("PIL not available")

        # hash 去重
        fhash = sha256_file(img_file)
        report["hash_map"].setdefault(fhash, []).append(str(img_file))

    # 找出重复 hash
    for h, paths in report["hash_map"].items():
        if len(paths) > 1:
            report["duplicates"].append({"hash": h[:16], "files": paths})

    report["hash_map"] = {h: ps for h, ps in report["hash_map"].items() if len(ps) > 1}
    if not report["hash_map"]:
        del report["hash_map"]

    # 汇总
    report["summary"] = {
        "ok": len(report["small_files"]) == 0 and len(report["duplicates"]) == 0,
        "warnings": [],
    }
    if report["small_files"]:
        report["summary"]["warnings"].append(f"{len(report['small_files'])} 文件 <10KB")
    if report["large_files"]:
        report["summary"]["warnings"].append(f"{len(report['large_files'])} 文件 >500KB")
    if report["duplicates"]:
        report["summary"]["warnings"].append(f"{len(report['duplicates'])} 组重复图片")

    return report


def main():
    parser = argparse.ArgumentParser(description="验证商品图片完整性")
    parser.add_argument("--dir", default=None, help="图片目录路径")
    parser.add_argument("-o", "--output", default=None, help="输出 JSON 报告路径")
    args = parser.parse_args()

    project_root = Path(__file__).resolve().parent.parent
    images_dir = args.dir or (project_root / "data" / "images")

    print(f"扫描图片目录: {images_dir}")
    report = validate_images(images_dir)

    print(f"\n=== 图片验证报告 ===")
    print(f"文件总数: {report['total_files']}")
    print(f"总大小:   {report['total_size_bytes'] / 1024:.0f} KB ({report['total_size_bytes'] / 1024 / 1024:.1f} MB)")
    print(f"品类分布: {dict(report['by_category'])}")
    print(f"警告:     {report['summary']['warnings']}")
    print(f"状态:     {'PASS' if report['summary']['ok'] else 'WARNINGS'}")

    out_path = args.output or (project_root / "data" / "images" / "image_validation.json")
    out_path = Path(out_path)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print(f"\n报告已保存: {out_path}")


if __name__ == "__main__":
    main()
