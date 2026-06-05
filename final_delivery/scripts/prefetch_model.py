"""预下载 Embedding 模型到本地 data/models/ 目录，避免 deploy 时在线下载。

用法:
  python scripts/prefetch_model.py               # 下载 bge-large-zh-v1.5 (embedding)
  python scripts/prefetch_model.py --reranker     # 下载 bge-reranker-v2-m3
  python scripts/prefetch_model.py --all          # 下载全部
  python scripts/prefetch_model.py --check        # 仅检查已有模型状态

下载支持断点续传 (hub 镜像: hf-mirror.com)
"""
import argparse
import os
import sys
from pathlib import Path

# Ensure backend is importable
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "source" / "backend"))

from app.core.config import settings

REPO_ROOT = Path(__file__).resolve().parents[1]
MODEL_DIR = REPO_ROOT / "source" / "backend" / "data" / "models"

MODELS = {
    "embedding": {
        "repo": "BAAI/bge-large-zh-v1.5",
        "dir": MODEL_DIR / "bge-large-zh-v1.5",
    },
    "reranker": {
        "repo": "BAAI/bge-reranker-v2-m3",
        "dir": MODEL_DIR / "bge-reranker-v2-m3",
    },
}


def check_model(model_dir: Path) -> dict:
    """Inspect a local model directory and return status.

    Valid model must have config.json AND a model weight file (bin or safetensors),
    consistent with startup.py's _check_local_model().
    """
    info = {"exists": model_dir.is_dir(), "size_mb": 0, "files": 0, "valid": False}
    if not info["exists"]:
        return info
    for f in model_dir.rglob("*"):
        if f.is_file():
            info["size_mb"] += f.stat().st_size / (1024 * 1024)
            info["files"] += 1
    config = (model_dir / "config.json").is_file()
    if not config:
        return info
    weights = any(
        f.name.startswith(("pytorch_model", "model"))
        and f.name.endswith((".bin", ".safetensors"))
        for f in model_dir.iterdir() if f.is_file()
    )
    subdir_weights = any(
        d.is_dir() and (d / "config.json").is_file()
        for d in model_dir.iterdir()
    )
    info["valid"] = weights or subdir_weights
    return info


def download_model(repo_id: str, dest_dir: Path) -> bool:
    """Download model files with resume support. Returns True on success."""
    from huggingface_hub import snapshot_download

    print(f"\nDownloading {repo_id} -> {dest_dir}")
    print(f"  Mirror: {settings.HF_ENDPOINT or 'default'}")

    os.makedirs(str(dest_dir), exist_ok=True)

    try:
        snapshot_download(
            repo_id=repo_id,
            local_dir=str(dest_dir),
            resume_download=True,
            max_workers=2,
            local_files_only=False,
        )
        print(f"  OK: {repo_id} downloaded successfully")
        return True
    except KeyboardInterrupt:
        print("\n  Download interrupted. Re-run to resume. Partial files kept.")
        return False
    except Exception as e:
        print(f"  ERROR: {e}")
        print(f"  Partial files kept in {dest_dir}, re-run to resume.")
        return False


def main():
    parser = argparse.ArgumentParser(description="Prefetch embedding/reranker models")
    parser.add_argument("--reranker", action="store_true")
    parser.add_argument("--all", action="store_true")
    parser.add_argument("--check", action="store_true", help="Only check status, no download")
    args = parser.parse_args()

    if args.check:
        all_ok = True
        for name, cfg in MODELS.items():
            info = check_model(cfg["dir"])
            status = "READY" if info["valid"] else "MISSING"
            if not info["valid"]:
                all_ok = False
            print(f"[{status}] {cfg['repo']}")
            if info["exists"]:
                print(f"       {info['size_mb']:.1f} MB, {info['files']} files @ {cfg['dir']}")
        sys.exit(0 if all_ok else 1)

    targets = []
    if args.all:
        targets = ["embedding", "reranker"]
    elif args.reranker:
        targets = ["reranker"]
    else:
        targets = ["embedding"]

    ok = True
    for name in targets:
        cfg = MODELS[name]
        info = check_model(cfg["dir"])
        if info["valid"]:
            print(f"[SKIP] {cfg['repo']} — already cached ({info['size_mb']:.1f} MB)")
            continue
        if info["exists"]:
            print(f"[RESUME] {cfg['repo']} — partial download ({info['size_mb']:.1f} MB), resuming...")
        if not download_model(cfg["repo"], cfg["dir"]):
            ok = False
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
