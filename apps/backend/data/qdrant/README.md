# 电商商品知识库 RAG 系统

基于 Qdrant + BGE 模型的电商商品知识库，支持向量语义检索和 LLM 流式回答。

## bge模型

ingest_to_qdrant.py中
```py
MODEL_PATH = os.path.join(BASE_DIR, "RAGchat", "bge-small-zh")
```
"bge-small-zh"是nilesthump电脑上的bge模型，别忘了自行修改为本地bge模型。

## Qdrant向量数据库

```py
COLLECTION_NAME = os.environ.get("COLLECTION_NAME", "products")
```
路径修改为本地配置路径。

## 项目结构

```
python-debug/
├── seed_products.json           # 50 条商品数据（7 个品类）
├── seed_reviews.json            # 683 条商品评论数据
├── product_data_sources.md      # 数据来源说明文档
├── RAGchat/bge-small-zh/        # 本地 BGE 中文向量模型（512 维）
├── qdrant_server/
│   ├── qdrant.exe               # Qdrant 1.14.0 服务端（Windows）
│   └── static/                  # Dashboard Web UI 静态文件
├── ingest_to_qdrant.py          # 知识库管理 API 服务（FastAPI，端口 8100）
├── retrieve_from_qdrant.py      # 语义检索 + LLM 流式问答脚本
└── README.md                    # 本文档
```

## 运行环境

- **Python 3.11+**
- Windows 10/11（Qdrant 服务端为 Windows 可执行文件）

### 安装依赖

```bash
# 入库 API 依赖
pip install fastapi uvicorn qdrant-client transformers torch numpy pydantic

# 检索脚本额外依赖
pip install langchain-ollama langchain-core
```

## 复现步骤

> 以下命令均在项目根目录（即 `README.md` 所在目录）下执行。终端先 `cd` 到该目录。

### 1. 启动 Qdrant 向量数据库

```bash
# Windows: 从项目根目录执行，qdrant.exe 需在 qdrant_server/ 目录下运行
cd qdrant_server
.\qdrant.exe --uri "http://localhost:6333"
```

启动成功标志：

```
Qdrant HTTP listening on 6333
Access web UI at http://localhost:6333/dashboard
```

> **注意**：`qdrant.exe` 运行时工作目录必须为 `qdrant_server/`，这样才能找到 `static/` 下的 Dashboard 文件。Dashboard 静态文件需从 [qdrant-web-ui releases](https://github.com/qdrant/qdrant-web-ui/releases) 下载 `dist-qdrant.zip`，解压到 `qdrant_server/static/` 目录。

### 2. 启动知识库管理 API

另开一个终端，回到项目根目录：

```bash
cd ..                  # 从 qdrant_server/ 返回项目根目录
python3 ingest_to_qdrant.py
```

启动成功标志：

```
Connected to Qdrant at http://localhost:6333, collection 'products' ready
Uvicorn running on http://0.0.0.0:8100
```

### 3. 初始化知识库数据

首次启动后，调用全量重载接口导入 seed 数据：

```bash
curl -X POST http://localhost:8100/products/reload
```

返回 `{"status":"ok","message":"50 products reloaded"}` 表示入库成功。

验证数量：

```bash
curl http://localhost:8100/health
# {"status":"ok","collection":"products","product_count":50}
```

### 4. 运行检索测试

在项目根目录下执行：

```bash
python3 retrieve_from_qdrant.py
```

脚本会执行 3 条内置查询（降噪耳机、游戏手机、学生平板），输出：
- 相似度检索结果（标题、分类、品牌、价格、评分、属性、亮点、场景）
- LLM 流式回答（需本地部署 `glm-5.1:cloud` 模型，否则降级为纯检索输出）

## API 功能

知识库管理 API 运行在 `http://localhost:8100`，交互式文档：[http://localhost:8100/docs](http://localhost:8100/docs)

| 方法 | 路径 | 功能 | 说明 |
|------|------|------|------|
| GET | `/health` | 健康检查 | 返回 collection 名称和商品数量 |
| POST | `/products/add` | 添加/更新商品 | 单条新增，product_id 相同则覆盖 |
| POST | `/products/batch` | 批量添加/更新 | 传入商品数组，统一向量化入库 |
| DELETE | `/products/{product_id}` | 删除商品 | 按 product_id 精确删除 |
| POST | `/products/reload` | 全量重载 | 从 seed JSON 文件清空重建 |

### 请求体示例（单条添加）

```json
POST /products/add
{
  "product_id": "NEW001",
  "title": "示例蓝牙耳机",
  "category": "Electronics/Headphones",
  "brand": "DemoBrand",
  "price": 199.0,
  "rating": 4.5,
  "rating_count": 500,
  "attributes": {"连接方式": "蓝牙5.3", "续航": "30小时"},
  "highlights": ["主动降噪", "30小时续航"],
  "scenarios": ["通勤", "运动"],
  "reviews": [
    {"rating": 5, "nickname": "用户A", "text": "音质很好"}
  ]
}
```

## 架构说明

```
                     ┌──────────────────────┐
                     │   Qdrant Server      │
                     │   localhost:6333      │
                     │   (Dashboard:6333)    │
                     └──────────┬───────────┘
                                │ gRPC/HTTP
          ┌─────────────────────┼─────────────────────┐
          │                     │                     │
  ┌───────▼───────┐   ┌────────▼────────┐   ┌────────▼────────┐
  │  ingest API   │   │  retrieve.py    │   │  Dashboard UI   │
  │  :8100        │   │  (CLI script)   │   │  :6333/dashboard│
  │  FastAPI      │   │  LCEL + Ollama  │   │  Web UI         │
  └───────────────┘   └─────────────────┘   └─────────────────┘
```

向量化流程：

```
商品文本 + 评价摘要
    │
    ▼
BGE-small-zh (512 维)
    │
    ▼
随机投影矩阵 (seed=42) → 4096 维
    │
    ▼
归一化 → Qdrant 入库
```

检索与入库使用**完全相同的模型和投影矩阵**（`seed=42`），确保查询向量和库中向量在同一个向量空间内。

## 注意事项

1. **Qdrant 启动顺序**：必须先启动 Qdrant Server，再启动 ingest API，否则 API 无法连接数据库。

2. **Dashboard 第一次无法访问**：Qdrant 1.14.0 将 Dashboard 拆分为独立项目，需手动下载 `dist-qdrant.zip` 解压到 `qdrant_server/static/`。从 `qdrant.exe` 同级目录启动才能正确加载 Dashboard。

3. **Qdrant 数据持久化**：数据存储在 `qdrant_server/storage/` 目录，重启 Qdrant 不会丢失。除非手动调用 `POST /products/reload`（会清空重建 collection）。

4. **投影矩阵一致性**：`ingest` 和 `retrieve` 使用相同的随机种子 `seed=42` 生成投影矩阵。修改任一脚本的种子值会导致检索失效。

5. **模型路径**：BGE 模型位于 `RAGchat/bge-small-zh/`，需确保该目录包含 `pytorch_model.bin`、`tokenizer.json`、`config.json` 等文件。

6. **LLM 模型**：检索脚本默认使用 `glm-5.1:cloud` 通过 Ollama 调用。若本地未部署该模型，LLM 链路会失败，脚本自动降级为纯相似度检索输出。

7. **Python 版本**：已在 Python 3.11 和 3.13 上验证通过。类型提示使用 `list[str]` 等语法，要求 Python 3.9+。

8. **Qdrant Server 版本兼容**：当前 Qdrant Server 为 1.14.0，`qdrant-client` 为 1.18.0，启动时有版本不匹配警告但不影响功能。生产环境建议版本一致。
