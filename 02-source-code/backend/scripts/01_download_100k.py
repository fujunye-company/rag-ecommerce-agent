import os
from pathlib import Path

# 必须在 import huggingface_hub 之前设置
BASE_DIR = Path(__file__).resolve().parents[1]

HF_HOME = BASE_DIR / "data" / "hf_home"
RAW_ROOT = BASE_DIR / "data" / "raw" / "superlinked_external_benchmarking"
DATA_DIR = RAW_ROOT / "benchmark-100k"

os.environ.setdefault("HF_HOME", str(HF_HOME))
os.environ.setdefault("HF_HUB_CACHE", str(HF_HOME / "hub"))
os.environ.setdefault("HF_HUB_DISABLE_SYMLINKS_WARNING", "1")
os.environ.setdefault("HF_HUB_DISABLE_XET", "1")
os.environ.setdefault("HF_HUB_ETAG_TIMEOUT", "60")
os.environ.setdefault("HF_HUB_DOWNLOAD_TIMEOUT", "120")

from huggingface_hub import snapshot_download


def main():
    HF_HOME.mkdir(parents=True, exist_ok=True)
    RAW_ROOT.mkdir(parents=True, exist_ok=True)

    print("HF_HOME:", HF_HOME)
    print("下载目录:", RAW_ROOT)

    snapshot_path = snapshot_download(
        repo_id="superlinked/external-benchmarking",
        repo_type="dataset",
        local_dir=str(RAW_ROOT),
        allow_patterns=[
            "benchmark-100k/*.parquet",
            "benchmark-100k/_SUCCESS",
            "README.md",
        ],
        max_workers=2,
    )

    parquet_files = sorted(DATA_DIR.glob("*.parquet"))

    print("snapshot_path:", snapshot_path)
    print("parquet 文件数量:", len(parquet_files))
    print("数据目录:", DATA_DIR)

    if len(parquet_files) != 100:
        raise RuntimeError(
            f"下载不完整：应有 100 个 parquet 文件，当前只有 {len(parquet_files)} 个。请重新运行本脚本，它会继续补下载。"
        )

    total_size_gb = sum(f.stat().st_size for f in parquet_files) / 1024 / 1024 / 1024
    print(f"parquet 总大小: {total_size_gb:.2f} GB")
    print("100k 商品向量数据下载完成。")


if __name__ == "__main__":
    main()