from pathlib import Path
import pyarrow.parquet as pq
import pyarrow.dataset as ds

BASE_DIR = Path(__file__).resolve().parents[1]
DATA_DIR = BASE_DIR / "data" / "raw" / "superlinked_external_benchmarking" / "benchmark-100k"

def main():
    files = sorted(DATA_DIR.glob("*.parquet"))

    print("数据目录:", DATA_DIR)
    print("parquet 文件数量:", len(files))

    if len(files) != 100:
        raise RuntimeError("parquet 文件数量不等于 100，说明下载还没完成。")

    first_file = files[0]
    table = pq.read_table(first_file)

    print("第一个 parquet 文件:", first_file.name)
    print("第一个文件行数:", table.num_rows)
    print("字段:", table.column_names)

    row = table.slice(0, 1).to_pydict()

    print("第一条 parent_asin:", row["parent_asin"][0])
    print("第一条 title:", row["title"][0])
    print("第一条 main_category:", row["main_category"][0])
    print("第一条 price:", row["price"][0])
    print("第一条向量维度:", len(row["value"][0]))

    dataset = ds.dataset(str(DATA_DIR), format="parquet")
    print("总行数:", dataset.count_rows())

    assert dataset.count_rows() == 100000
    assert len(row["value"][0]) == 4154

    print("检查通过：100k 商品条目向量数据可用。")

if __name__ == "__main__":
    main()