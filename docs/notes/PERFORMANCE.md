# 拾物 App — 性能基准

> 测试环境: WSL2, CPU-only, 豆包 doubao-seed-2-0-lite, 2026-05-26

## 端到端延迟

| 场景 | 延迟 | 卡片数 | 说明 |
|------|------|:--:|------|
| AI 导购 (简单) | 6.3s | 10 | "推荐500元机械键盘" |
| AI 导购 (复杂) | 10.8s | 10 | "推荐适合跑步的运动耳机" |
| 拍照找货 | ~20s | 8 | VLM推理 3.6s + 检索 + SSE |
| 多轮追问 | 8-12s | 5 | 含上下文继承 |

## 分阶段延迟 (典型 10.8s 请求)

| 阶段 | 耗时 | 占比 |
|------|------|:--:|
| 意图分类 + 槽位提取 | ~1.5s | 14% |
| Embedding (BGE 1024-dim) | ~0.2s | 2% |
| Qdrant 向量检索 | ~0.05s | <1% |
| Reranker (CrossEncoder) | ~0.5s | 5% |
| ProductRanker 排序 | ~0.05s | <1% |
| LLM 流式生成 (豆包) | ~8s | 74% |
| SSE 网络传输 | ~0.5s | 5% |

## 模型规格

| 模型 | 类型 | 维度/大小 | 设备 | 推理时间 |
|------|------|----------|:--:|------|
| BAAI/bge-large-zh-v1.5 | Embedding | 1024-dim | CPU | ~0.2s |
| BAAI/bge-reranker-v2-m3 | CrossEncoder | 2.2GB | CPU | ~0.5s |
| Qwen3-VL-2B-Instruct | VLM | 4.0GB | CPU | ~3.6s |
| doubao-seed-2-0-lite | LLM | API | Cloud | ~8s |

## 优化空间

- **首屏响应**: progress 事件已实现 TTFT≈0ms 感知
- **查询缓存**: LRU 100条, 命中率取决于重复查询频率
- **GPU 推理**: 若有 GPU, Reranker/VLM 可降低 5-10x
- **流式并行**: LLM 生成与卡片构造可并行
