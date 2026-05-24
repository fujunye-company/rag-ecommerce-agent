# 电商商品知识库 RAG 系统

基于 Qdrant + bge-large-zh-v1.5 的电商商品知识库，支持向量语义检索和 DeepSeek LLM 推荐。

## 技术栈

| 组件 | 版本/型号 |
|------|----------|
| Embedding | BAAI/bge-large-zh-v1.5 (1024维) |
| 向量库 | Qdrant (Docker, localhost:6333) |
| LLM | DeepSeek (deepseek-chat) |
| Python | 3.11+ @ ~/.hermes-venv |

## 项目结构

```
apps/backend/data/qdrant/
├── seed_products.json           # 50 条商品数据（7 品类）
├── seed_reviews.json            # 683 条商品评价（~185字/条）
├── ingest_to_qdrant.py          # 向量化入库脚本（简单脚本形式）
├── retrieve_from_qdrant.py      # 语义检索 + LLM 推荐
├── ingest_api.py                # [加分项] CRUD REST API 服务
└── README.md                    # 本文档
```

## 环境准备

### 1. 激活虚拟环境

```bash
source ~/.hermes-venv/bin/activate
```

### 2. 安装依赖

```bash
pip install qdrant-client sentence-transformers openai
```

### 3. 启动 Qdrant（Docker）

```bash
cd /mnt/c/Users/fujunye/Desktop/Hermes/04-rag-ecommerce
docker compose -f infrastructure/docker-compose.yml up -d
```

验证 Qdrant 已启动：

```bash
curl http://localhost:6333/health
# 返回 {"title":"qdrant - vector search engine","version":"..."}
```

Dashboard: http://localhost:6333/dashboard

### 4. 配置 LLM API Key

```bash
export DEEPSEEK_API_KEY="your-key-here"
export DEEPSEEK_BASE_URL="https://api.deepseek.com/v1"
```

或确保 `apps/backend/.env` 中已配置。

## 使用说明

以下命令均在项目根目录 `04-rag-ecommerce/` 下执行。

### 入库商品数据

```bash
cd apps/backend/data/qdrant
python ingest_to_qdrant.py
```

输出示例：
```
加载 Embedding 模型: BAAI/bge-large-zh-v1.5 ...
  向量维度: 1024
连接 Qdrant: http://localhost:6333 ...
读取: 50 件商品, 50 组评价
Collection 'products' 已重建 (1024维)
向量化 50 件商品 ...
入库完成: 50 件商品
验证: Qdrant 中现有 50 个向量
✅ 全部完成！
```

### 检索 + LLM 推荐

```bash
cd apps/backend/data/qdrant

# 单次查询
python retrieve_from_qdrant.py "推荐一款降噪耳机，预算2000"

# 交互模式
python retrieve_from_qdrant.py

# 内置测试
python retrieve_from_qdrant.py --test
```

### [加分项] CRUD API 服务

```bash
cd apps/backend/data/qdrant
python ingest_api.py
# 启动在 http://localhost:8100
# API 文档: http://localhost:8100/docs
```

API 端点：
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | /health | 健康检查 + 商品数量 |
| POST | /products/add | 新增/更新单个商品 |
| POST | /products/batch | 批量新增/更新 |
| DELETE | /products/{id} | 删除商品 |
| POST | /products/reload | 从 JSON 全量重载 |

## 向量化说明

```
商品文本 (title + category + brand + price + attributes + highlights + reviews)
    │
    ▼
BAAI/bge-large-zh-v1.5 (1024维, 原生输出)
    │
    ▼
L2 归一化 → Qdrant COSINE 距离
```

- 无投影矩阵，直接使用 1024 维原生向量
- 检索与入库使用同一模型，天然一致

## 数据来源

- `seed_products.json`: 50 件中文电商商品（7 品类），自行构造
- `seed_reviews.json`: 683 条用户评价，覆盖 1-5 星，~185 字/条

## 注意事项

1. **Docker 优先**: Qdrant 通过 Docker Compose 启动，不使用 Windows 可执行文件
2. **模型自动下载**: bge-large-zh-v1.5 首次使用自动从 Hugging Face 下载（~1.3GB）
3. **API Key**: 检索脚本需要 DEEPSEEK_API_KEY 环境变量，否则降级为纯检索输出
4. **数据持久化**: Qdrant 数据存储在 Docker volume，重启不丢失
5. **Python 版本**: 需要 Python 3.9+，已在 3.11 验证
