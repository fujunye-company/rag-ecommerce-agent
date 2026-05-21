# 电商AI导购Agent — 实用资源与可复用参考

> 搜索时间：2026-05-20
> 来源：GitHub搜索 + HuggingFace + CrossRef + 训练知识
> 用途：M2-M4开发中的代码参考、数据集、评测工具、模板

---

## 一、可复用开源项目（GitHub）

### 1.1 电商RAG Agent（直接对标）

| 项目 | Stars | 技术栈 | 可借鉴点 |
|------|:---:|------|---------|
| `Aakash109-hub/eCommerce-Customer-Support-Agent` | 12 | LangChain + RAG + Ollama | 生产级架构，多文档RAG |
| `teamauresta/merchant-mind` | 1 | FastAPI + RAG + Shopify | 多租户、FastAPI项目结构 |
| `saralabiswal/agentic-ecommerce-rag` | 0 | LangGraph + ChromaDB + sentence-transformers | Agent编排、商品列表优化 |
| `Rahul1038402/E-Commerce-Q-amp-A-Agent` | 0 | LangGraph + LangChain | RAG pipeline + 网页抓取 |
| `kanugurajesh/Agentic-Ecommerce` | 0 | Agentic框架 | 电商Agent设计模式 |

**建议**：`Aakash109-hub` 的项目结构最值得参考，`saralabiswal` 的 LangGraph 编排与我们的技术栈最接近。

### 1.2 LangGraph 官方示例（必读）

| 资源 | 地址 | 价值 |
|------|------|------|
| LangGraph Quick Start | https://langchain-ai.github.io/langgraph/tutorials/introduction/ | StateGraph基础 |
| Customer Support Agent | https://langchain-ai.github.io/langgraph/tutorials/customer-support/customer-support/ | **最接近本项目的官方示例** |
| Agent Supervisor (Multi-Agent) | https://langchain-ai.github.io/langgraph/tutorials/multi_agent/agent_supervisor/ | 多Agent协作参考 |
| SQL Agent | https://langchain-ai.github.io/langgraph/tutorials/sql-agent/ | 工具调用+数据库查询 |

### 1.3 LlamaIndex 电商示例

| 资源 | 地址 | 价值 |
|------|------|------|
| Building a Shopping Assistant | docs.llamaindex.ai → Use Cases → Agents | 电商Agent完整教程 |
| Qdrant Vector Store | https://docs.llamaindex.ai/en/stable/examples/vector_stores/QdrantIndexDemo/ | Qdrant集成示例 |
| Multi-Modal RAG | docs.llamaindex.ai → Use Cases → Multimodal | 图片+文本混合检索 |

---

## 二、数据集

### 2.1 商品+评价数据（核心）

| 数据集 | 规模 | 来源 | 用途 |
|--------|------|------|------|
| **McAuley-Lab/Amazon-Reviews-2023** | 571M条评价, 48M商品 | HuggingFace (51k下载) | ⭐ 主要知识库 |
| Superlinked external-benchmarking | 100k条 | HuggingFace | RAG评测基准 |
| Amazon ESCI (KDD Cup 2022) | 多语言商品搜索 | GitHub | 搜索相关性评测 |
| WANDS (Wayfair) | 42k商品, 480查询 | Wayfair | 家居品类benchmark |

**Amazon-Reviews-2023 子集建议**（PRD已确认使用）：
- Electronics（耳机/手机等3C）：~50M条
- Clothing（服装）：~30M条
- Home & Kitchen（家居）：~20M条

### 2.2 评测数据集

| 数据集 | 用途 | 规模 |
|--------|------|------|
| **RAGAS 官方评测集** | RAG质量评测 | GitHub: explodinggradients/ragas |
| BEIR benchmark | 检索质量评测 | 19个数据集 |
| MTEB / C-MTEB | Embedding模型评测 | Chinese MTEB leaderboard |
| Amazon-M2 (Multi-lingual) | 多语言推荐 | KDD 2023 |

---

## 三、中文Embedding模型选型

### 3.1 C-MTEB Leaderboard Top 5 (2026)

| 模型 | 参数量 | 维度 | 检索得分 | 适合场景 |
|------|:---:|:---:|:---:|------|
| **BAAI/bge-large-zh-v1.5** | 326M | 1024 | 72.5 | ⭐ 当前使用，综合最优 |
| BAAI/bge-m3 | 568M | 1024 | 74.1 | 多语言+多粒度 |
| text2vec-large-chinese | 326M | 1024 | 70.8 | 轻量替代 |
| stella-mrl-large-zh-v3.5 | - | 1024 | 73.2 | 新锐模型 |
| **BAAI/bge-en-icl** | 7B | 4096 | - | LLM-based，仅供对比 |

**建议**：bge-large-zh-v1.5 够用，全量阶段可升级到 bge-m3（支持中英双语+多粒度）。

### 3.2 部署选项

| 方案 | 延迟 | 显存 | 适用 |
|------|:---:|:---:|------|
| sentence-transformers (CPU) | 50-100ms | 0 | MVP开发 |
| sentence-transformers (GPU) | 5-10ms | 2-4GB | 正式部署 |
| TEI (HuggingFace) | 3-5ms | 2-4GB | 高性能服务 |
| BGE API (硅基流动) | <50ms | 0 | 免部署方案 |

---

## 四、评测框架

### 4.1 RAGAS（必装）

```bash
pip install ragas
```

核心指标及电商适配：

| RAGAS指标 | 电商场景含义 | 目标值 |
|-----------|------------|:---:|
| Faithfulness | 推荐理由是否可追溯到商品数据 | ≥0.85 |
| Answer Relevancy | 回答是否贴合用户购物需求 | ≥0.85 |
| Context Precision | 检索到的商品是否相关 | ≥0.80 |
| Context Recall | 是否遗漏了该推荐的商品 | ≥0.75 |
| Aspect Critique | 自定义维度（价格准确/场景适配） | ≥0.80 |

### 4.2 电商自定义评估维度

```python
from ragas.metrics import AspectCritique

# 价格准确度
price_accuracy = AspectCritique(
    name="price_accuracy",
    definition="推荐商品价格是否在用户预算范围内"
)

# 场景匹配度
scenario_fit = AspectCritique(
    name="scenario_fit", 
    definition="推荐是否匹配用户的使用场景（送礼/通勤/运动等）"
)

# 推荐多样性
diversity = AspectCritique(
    name="diversity",
    definition="推荐列表是否包含不同品牌/价位的商品"
)
```

---

## 五、Prompt Engineering 模板

### 5.1 意图分类 Prompt

```
你是电商导购意图分类器。分析用户输入，输出JSON。

意图类型（6选1）：
- commodity_recommend: 直接推荐商品（"推荐降噪耳机"）
- commodity_compare: 对比商品（"Sony和Bose哪个好"）
- commodity_detail: 了解商品详情（"XM5降噪多少分贝"）
- scenario_shopping: 场景化购物（"送女朋友生日礼物"）
- after_sales: 售后咨询（"这款耳机保修多久"）
- chitchat: 闲聊（"你好"）

用户输入：{query}

输出JSON：{"intent": "...", "confidence": 0.XX}
```

### 5.2 槽位填充 Prompt

```
你是电商导购信息提取器。从用户输入中提取购物相关的结构化信息。

用户输入：{query}

提取以下字段（未知填null）：
- category: 商品品类
- price_min, price_max: 预算范围（数字）
- brand_preference: 品牌偏好
- attributes: 属性要求（如{"降噪":"是","连接":"蓝牙"}）
- scenario: 使用场景（送礼/通勤/运动/办公）
- missing_slots: 还需追问的信息

输出JSON。
```

### 5.3 推荐理由生成 Prompt（三段式）

```
基于以下商品信息，为每件商品生成推荐理由。严格使用三段式格式：

商品：{product_name} | 价格：¥{price} | 评分：{rating} |
核心卖点：{highlights}

输出格式：
① 匹配依据：说明该商品如何满足用户的具体需求
② 品质亮点：突出1-2个数据支撑的核心优势
③ 适用场景：描述最适合的使用场景

每条理由控制在30字以内，使用具体数据而非模糊描述。
禁止使用"非常""很好""不错"等模糊词。
```

### 5.4 检索质量自评 Prompt（Self-Corrective RAG）

```
评估以下检索结果对用户查询的相关性。

用户查询：{query}
检索结果：
{retrieved_texts}

对每条结果评分1-5（5=高度相关，1=完全不相关）：
- 5: 精确匹配用户需求
- 4: 大部分相关
- 3: 部分相关但有偏差
- 2: 弱相关
- 1: 不相关

输出JSON: {"scores": [5,4,3,...], "overall": 平均分, "needs_rewrite": true/false}
如果 overall < 3，needs_rewrite = true 并提供 rewrite_suggestion。
```

---

## 六、开发工具链推荐

### 6.1 向量检索调试

| 工具 | 用途 |
|------|------|
| Qdrant Web UI (`localhost:6333/dashboard`) | 可视化查看向量和payload |
| `qdrant_client` Python API | 编程式查询/插入 |
| LlamaIndex Callback | 查看每次检索的query和返回值 |

### 6.2 SSE 流式调试

```bash
# 测试SSE流式端点
curl -N -X POST http://localhost:8000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"推荐降噪耳机","session_id":"test"}'
```

### 6.3 性能测试

```bash
# 压测工具
pip install locust

# Embedding 吞吐测试
python -c "
from sentence_transformers import SentenceTransformer
import time
model = SentenceTransformer('BAAI/bge-large-zh-v1.5')
t0 = time.time()
for _ in range(100):
    model.encode('测试文本')
print(f'100次编码: {time.time()-t0:.1f}s')
"
```

---

## 七、中文电商AI最新动态（2025-2026）

### 7.1 国内产品进展（训练知识 + 公开报道）

| 时间 | 事件 | 影响 |
|------|------|------|
| 2026.03 | 豆包日活突破1.45亿，内测电商下单 | 对话式购物入口已验证 |
| 2026.01 | 通义千问全面接入淘宝/支付宝/高德 | 阿里全生态AI化 |
| 2025.12 | 京东AI购独立App内测 | 电商AI独立App化趋势 |
| 2025.10 | 豆包接入抖音商城商品推荐 | 内容电商+AI导购融合 |
| 2025.09 | OpenAI Instant Checkout上线 | 对话内端到端下单 |

### 7.2 技术趋势

| 趋势 | 说明 | 本项目响应 |
|------|------|-----------|
| Agentic Commerce | AI Agent 自主搜索→评估→决策→支付 | 我们做搜索→评估→决策环节 |
| Multimodal RAG | 文本+图片+视频混合检索 | M2后期实现以图搜物 |
| Streaming UI | 对话式界面成为电商新入口 | 对标豆包流式体验 |
| Evaluation-as-Code | RAGAS等框架使评测自动化 | M4集成RAGAS评测 |

---

## 八、优先级实施建议

### M2初期（本周）立即可用

1. **Amazon Reviews 2023 Electronics子集** → 下载并导入Qdrant作为知识库
2. **LangGraph Customer Support Agent示例** → 直接阅读其状态图设计模式
3. **RAGAS** → pip install后在本地跑通基础评测
4. **意图分类Prompt模板**（§五.1）→ 替换当前intent.py的关键词规则

### M2中后期

5. **Aakash109-hub项目** → 参考其多文档RAG架构
6. **三段式理由Prompt**（§五.3）→ agent.py的generate_node使用
7. **检索质量自评Prompt**（§五.4）→ retriever.py的quality_check

### M3/M4

8. **C-MTEB评测** → 验证是否需升级到bge-m3
9. **LangGraph Multi-Agent示例** → 双Agent协作架构参考

---

> **文档状态**: V1.0
> **更新频率**: 随开发推进补充新发现的资源
> **相关文档**: DEV-GUIDE.md, INNOVATION-RESEARCH.md, PRIORITY_CHECKLIST.md
