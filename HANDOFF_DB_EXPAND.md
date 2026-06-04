# RAG 电商数据库扩充 — 任务交接报告

**日期**: 2026-05-27 15:26  
**项目**: 04-rag-ecommerce  
**交接原因**: 会话中断后恢复规划，产出 P0-P4 方案待执行

---

## 1. 任务概要

将 RAG 电商项目商品数据库从 190 条扩充至更大规模，同时补充真实商品图片。基于上午上传的扩充方案文档（`数据库条目扩充可执行方案_真实图片版.docx`）和下午确认的数据源，已产出完整 P0-P4 执行步骤。

**核心约束**: 不改 DATA-CONTRACT.md、不改 PostgreSQL/Qdrant schema、不改 Android 数据模型、不伪造商品/价格/图片。

---

## 2. 当前状态

| 组件 | 状态 |
|------|------|
| Qdrant | 190 vectors, 1024-dim, 运行中(http://localhost:6333) |
| PostgreSQL | shopping-pg 容器运行中(端口 5432) |
| 现有种子数据 | seed_products.json 190条，94品类，**无 image_url 字段** |
| 新数据源 | `Downloads/ecommerce_agent_dataset_供参考 (1)/` — 100条+100张真实图片 |
| 扩充方案文档 | `Downloads/数据库条目扩充可执行方案_真实图片版.docx` — 已全文提取 |
| Android Mock | 30条/15品类，需要同步新数据 |

**数据源详情**: 4 大类（美妆护肤/数码电子/服饰运动/食品饮料），每类 25 条，38 个细类（精华/智能手机/跑鞋/咖啡等）。每条含 product_id/title/brand/sub_category/base_price/image_path/SKUs/rag_knowledge(含营销描述+FAQ+用户评价)。图片 48-109KB JPEG，真实商品图。

**关键发现**: 数据集 schema ≠ ProductRecord。需要映射：sub_category→category, base_price→price, marketing_description→highlights, SKU properties→attributes, user_reviews→rating/rating_count。

---

## 3. 下一步执行（P0→P1→P2→P3→P4）

### P0 — 数据源对接 + 映射脚本（预估 1h）
- [ ] 安装 Pillow：`pip install Pillow`
- [ ] 编写 `map_dataset_to_productrecord.py`，映射 dataset JSON → ProductRecord
- [ ] 复制图片到 `04-rag-ecommerce/data/images/<category>/`
- [ ] 用 2-3 条样本验证映射结果

### P1 — 导入 PostgreSQL + Qdrant（预估 0.5h）
- [ ] 去重检查（dataset 100条 vs 现有 190条 product_id）
- [ ] 输出 products_expanded_100.jsonl + CSV
- [ ] 导入 PostgreSQL（INSERT，不改表结构）
- [ ] 修改 ingest_to_qdrant.py 支持 --input JSONL，向量化后 upsert
- [ ] **curl 端到端验证**: 测试精华/智能手机/低价T恤 3 个场景的 product_cards 事件含 image_url

### P2 — Android Mock 同步（预估 0.5h）
- [ ] 编写 sync_mock_from_dataset.py 生成 Kotlin Mock 文件
- [ ] `./gradlew clean assembleDebug` 编译验证

### P3 — 管道化（预估 1h）
- [ ] 重构映射脚本为通用 expand_pipeline.py
- [ ] 新增 image_validator.py（HTTP200/尺寸/hash去重）
- [ ] 生成 qa_report.json

### P4 — 现有 DB 补充 image_url（预估 0.25h）
- [ ] 查现有 190 条的 image_url 缺失情况
- [ ] 前端默认占位图策略（不做假图）

### 每阶段验证要求
- curl 验证全链路 SSE（text_delta + product_cards + done）
- Android 编译通过

---

## 4. 关键路径与避坑

| 坑 | 规避 |
|----|------|
| dataset 的 category 是大类（"美妆护肤"）不是细类 | 用 sub_category 映射到 ProductRecord.category |
| SKU 有多个 price，ProductRecord 只需一个 price | 取 base_price 或第一个 SKU 最低价 |
| Qdrant container 标记 unhealthy | 实际可用，points_count=190 正常 |
| WSL 中文目录名乱码 | 用 glob 遍历，不用硬编码中文路径 |
| 现有 seed_products.json 无 image_url | 不伪造，前端用默认占位图 |
| DATA-CONTRACT.md v1.0 是唯一权威 | 只读不改，所有映射以它为准 |

**中断位置**: 方案已产出，P0 映射脚本尚未开始编写。从 P0.2（写 map_dataset_to_productrecord.py）继续即可。

---

## 5. 建议加载的 Skills

- `rag-pipeline-debugging` — P1 导入后 curl 验证排查
- `e2e-curl-testing` — 端到端 curl 测试脚本编写
- `product-seed-builder` — 种子数据构造参考
- `data-contract-unification` — 数据契约迁移方法论
- `android-compose-phased-build` — P2 Android 编译
- `vibe-coding-guardrails` — 开发守则（不改结构）
