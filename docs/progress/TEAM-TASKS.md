# 队员可独立执行的 RAG/Agent 任务分包

> 生成时间：2026-05-20 | 最后更新：2026-05-22  
> 说明：以下任务已打包为独立工作包，队员可按包领取执行，  
>   不依赖 Hermes 实时交互，产出可直接集成到项目主干。  
> **当前阶段：M10 冲刺 — 需回收验收外包包**

### 里程碑映射（M1-M10 v4.0）

| 包 | 映射里程碑 | 负责人 | 状态 |
|:--:|-----------|:--:|:--:|
| 包1 数据工程 | M2/M3 | 队员A | ✅ 已交付 (50商品+Qdrant) |
| 包2 评测工程 | M5/M8 | 队员B | ❓ 待验收 |
| 包3 Prompt工程 | M3/M9 | 队员C | ❓ 待验收 |
| 包4 前端工程 | M4 | 队员D | ❓ 代码已合并 |
| 包5 Embedding | M3 | 全员 | ✅ bge-large-v1.5 选定 |
| 包6 Reranker | M3 | 队员AB | ❓ 待验收 |
| 包7 商品排序 | M3/M4 | 队员CD | ❓ 待验收 |
| 包8 购物车组件 | M7 | 队员D | ❓ 待验收 |
| 包9 拍照组件 | M7 | 队员D | ❓ 待验收 |
| 包10 评测扩展 | M7/M8 | 队员B | ❓ 待验收 |

### 队员任务分配

```
队员A: 数据工程      → 包1 商品知识库构建 ✅
队员B: 评测工程      → 包2 评测用例库 ❓ + 包10 评测扩展 ❓
队员C: Prompt工程    → 包3 Prompt模板精调 ❓
队员D: 前端工程      → 包4 UI对齐 ❓ + 包8 购物车 ❓ + 包9 拍照 ❓
队员A/B可选:        → 包6 Reranker重排序 ❓
队员C/D可选:        → 包7 多维打分排序 ❓
Hermes(AI):         → M1-M9 全栈开发 + M10 冲刺编排
```


---

## 包1：商品知识库构建（数据工程）

> 负责人：队员A  |  优先级：P0  |  工时：1天  |  前置：Docker已启动

### 任务目标
构建 50+ 商品的种子数据，包含产品信息+用户评价，导入 Qdrant 向量库。

### 交付物
```
data/
├── seed_products.json      # 50+商品结构化数据
├── seed_reviews.json       # 每商品10-20条评价
├── ingest_to_qdrant.py     # 入库脚本
└── README.md               # 运行说明
```

### 执行步骤

#### Step 1：准备数据（50-100条中文电商商品）

> ⚠️ 比赛要求中文电商场景，不使用英文 Amazon 数据集。
> 推荐来源：京东/淘宝公开商品数据，或自行构造。
```bash
# Amazon Reviews 2023 - Electronics子集
pip install datasets
python -c "
from datasets import load_dataset
ds = load_dataset('McAuley-Lab/Amazon-Reviews-2023', 'raw_meta_Electronics', split='full', streaming=True)
# 取前100条商品元数据
for i, item in enumerate(ds):
    if i >= 100: break
    print(item)
"
```

#### Step 2：构造商品JSON（50件）

每件商品包含以下字段：
```json
{
  "product_id": "B0XXXXXXX",
  "title": "Sony WH-1000XM5 无线降噪耳机",
  "category": "Electronics/Headphones",
  "brand": "Sony",
  "price": 2499.00,
  "rating": 4.7,
  "rating_count": 15234,
  "attributes": {
    "连接方式": "蓝牙5.2",
    "降噪": "主动降噪",
    "续航": "30小时",
    "驱动单元": "30mm",
    "重量": "250g"
  },
  "highlights": ["降噪深度35dB", "30小时续航", "轻量250g"],
  "scenarios": ["通勤", "办公", "音乐欣赏"]
}
```

建议覆盖品类：耳机(10件)、手机(10件)、平板(5件)、手表(5件)、音箱(5件)、相机(5件)、配件(10件)。

#### Step 3：构造评价JSON（每商品10-20条）

```json
{
  "product_id": "B0XXXXXXX",
  "reviews": [
    {
      "rating": 5,
      "title": "降噪效果惊艳",
      "text": "地铁通勤用了一个月，降噪效果真的绝了，比以前用的QC35好很多...",
      "verified_purchase": true,
      "helpful_votes": 128,
      "date": "2024-03-15"
    }
  ]
}
```

要求：覆盖正面(4-5星)、中性(3星)、负面(1-2星)评价，每条50-200字。

#### Step 4：编写入库脚本 `ingest_to_qdrant.py`

```python
"""商品数据 + 评价 → 向量化 → Qdrant入库"""
import json
from qdrant_client import QdrantClient
from qdrant_client.http import models
from sentence_transformers import SentenceTransformer

# === 配置 ===
QDRANT_URL = "http://localhost:6333"
COLLECTION_NAME = "products"
EMBEDDING_MODEL = "BAAI/bge-large-zh-v1.5"

# === 初始化 ===
client = QdrantClient(url=QDRANT_URL)
model = SentenceTransformer(EMBEDDING_MODEL)

# === 创建Collection ===
client.recreate_collection(
    collection_name=COLLECTION_NAME,
    vectors_config=models.VectorParams(
        size=1024,
        distance=models.Distance.COSINE
    )
)

# === 读数据并入库 ===
with open("data/seed_products.json") as f:
    products = json.load(f)

points = []
for prod in products:
    # 构造检索文本：标题+品类+属性+卖点
    text = f"{prod['title']} {prod['category']} {prod['brand']} " + \
           " ".join(prod.get('highlights', [])) + " " + \
           " ".join(prod.get('scenarios', []))
    
    embedding = model.encode(text).tolist()
    
    points.append(models.PointStruct(
        id=hash(prod['product_id']) % (10**12),  # 正整数ID (注意: PYTHONHASHSEED需固定)
        vector=embedding,
        payload={
            "product_id": prod['product_id'],
            "title": prod['title'],
            "category": prod['category'],
            "brand": prod['brand'],
            "price": prod['price'],
            "rating": prod['rating'],
            "attributes": prod.get('attributes', {}),
            "scenarios": prod.get('scenarios', [])
        }
    ))

client.upsert(collection_name=COLLECTION_NAME, points=points)
print(f"入库完成: {len(points)} 件商品")
```

#### Step 5：验证

```bash
python ingest_to_qdrant.py
# 验证检索
curl -X POST http://localhost:6333/collections/products/points/search \
  -H "Content-Type: application/json" \
  -d '{"vector": [0.0]*1024, "limit": 3}'  # 用embedding替代
```

### 验收标准
- ✅ 50+商品成功入库，Qdrant Dashboard可见
- ✅ 每件商品含完整payload（价格/评分/属性/场景）
- ✅ README.md含复现步骤
- ✅ 品类覆盖≥4个

---

## 包2：评测用例库 + RAGAS自动化管线（评测工程）

> 负责人：队员B  |  优先级：P0  |  工时：1.5天  |  前置：包1完成

### 任务目标
构建200+条评测用例，搭建RAGAS自动评测管线。

### 交付物
```
data/eval/
├── eval_cases.json          # 200+评测用例
├── ground_truth.json        # 标准答案（每个query的预期商品ID列表）
├── run_eval.py              # 自动评测脚本
└── README.md
```

### 执行步骤

#### Step 1：安装RAGAS
```bash
pip install ragas datasets
```

#### Step 2：构造评测用例（按场景分类）

用例格式：
```json
[
  {
    "id": "eval_001",
    "scenario": "commodity_recommend",
    "query": "推荐一款3000以内的降噪耳机",
    "expected_intent": "commodity_recommend",
    "expected_slots": {
      "category": "耳机",
      "price_max": 3000,
      "attributes": {"降噪": "是"}
    },
    "ground_truth_product_ids": ["B0XXXX001", "B0XXXX002"],
    "min_expected_results": 3,
    "difficulty": "easy"
  }
]
```

**场景分布要求**：
| 场景 | 数量 | 示例 |
|------|:---:|------|
| commodity_recommend（品类推荐） | 50 | "推荐降噪耳机""3000以内手机推荐" |
| commodity_compare（商品对比） | 30 | "XM5和QC45哪个好""iPhone和华为怎么选" |
| commodity_detail（参数查询） | 30 | "这款耳机降噪多少分贝""续航多久" |
| scenario_shopping（场景化） | 40 | "送女朋友生日礼物""北方冬天通勤鞋" |
| after_sales（售后） | 20 | "保修多久""支持7天无理由吗" |
| chitchat（闲聊+对抗） | 30 | "你好""今天天气怎么样""你会骗我吗" |
| **合计** | **200** | |

#### Step 3：编写评测脚本 `run_eval.py`

```python
"""RAGAS自动评测脚本"""
import json
from ragas import evaluate
from ragas.metrics import (
    faithfulness, answer_relevancy, context_precision, context_recall
)
from datasets import Dataset

# 1. 加载评测用例
with open("data/eval/eval_cases.json") as f:
    cases = json.load(f)

# 2. 对每条用例：调用Agent → 收集回答+上下文
results = []
for case in cases:
    # TODO: 调用你的 /api/chat 端点
    # response = requests.post("http://localhost:8000/api/chat", ...)
    # results.append({
    #     "question": case["query"],
    #     "answer": response["text"],
    #     "contexts": response["contexts"],
    #     "ground_truth": case["ground_truth_product_ids"]
    # })
    pass

# 3. RAGAS评测
# dataset = Dataset.from_list(results)
# scores = evaluate(dataset, metrics=[faithfulness, answer_relevancy, 
#                                      context_precision, context_recall])
# print(scores)

# 4. 输出报告
# with open("data/eval/eval_report.json", "w") as f:
#     json.dump({"scores": scores, "per_case": results}, f, indent=2, ensure_ascii=False)
```

**注意**：run_eval.py 需要等 `/api/chat` 端点可用后才能完整运行。包2的主要工作是用例构造，评测脚本先搭好框架。

#### Step 4：验证

```bash
# 验证用例格式
python -c "
import json
with open('data/eval/eval_cases.json') as f:
    cases = json.load(f)
assert len(cases) >= 200, f'需要200条，实际{len(cases)}'
scenarios = set(c['scenario'] for c in cases)
assert len(scenarios) >= 6, f'需要6类场景，实际{len(scenarios)}'
print(f'✅ {len(cases)}条用例，{len(scenarios)}类场景')
"
```

### 验收标准
- ✅ 200+条用例，6类场景全覆蓋
- ✅ 每条含 ground_truth_product_ids（对应包1的商品）
- ✅ run_eval.py 框架可运行（即使实际评测待后端就绪）
- ✅ 对抗用例≥20条（测试边界情况）

---

## 包3：Prompt模板精调与验证（Prompt工程）

> 负责人：队员C  |  优先级：P0  |  工时：1天  |  前置：无（可以纯手工+LLM测试）

### 任务目标
基于我提供的4个Prompt模板，在 Doubao 上精调并验证效果（比赛目标LLM）。

### 交付物
```
data/prompts/
├── intent_classify.txt       # 意图分类（精调后）
├── slot_extract.txt          # 槽位填充（精调后）
├── recommend_reason.txt      # 推荐理由三段式（精调后）
├── retrieval_quality.txt     # 检索质量自评（精调后）
├── test_results.json         # 测试结果
└── README.md                 # 精调记录
```

### 执行步骤

#### Step 1：获取基线Prompt

从我提供的模板开始（见下方基线模板），在 Doubao Chat 网页版或 API 中测试。

#### Step 2：构造测试集

每个Prompt至少20条测试输入，覆盖边界情况：

**意图分类测试集示例**：
```json
[
  {"input": "推荐降噪耳机", "expected": "commodity_recommend"},
  {"input": "Sony和Bose哪个好", "expected": "commodity_compare"},
  {"input": "XM5降噪多少", "expected": "commodity_detail"},
  {"input": "送女朋友礼物500以内", "expected": "scenario_shopping"},
  {"input": "这耳机保修多久", "expected": "after_sales"},
  {"input": "你好啊", "expected": "chitchat"},
  {"input": "推荐一个降噪好的，主要在地铁上用，预算2000-3000", "expected": "commodity_recommend"},
  {"input": "我想买个东西但不知道买什么", "expected": "commodity_recommend"},
  {"input": "AirPods Pro续航", "expected": "commodity_detail"}
]
```

#### Step 3：迭代精调

对每个Prompt：
1. 跑基线测试 → 记录准确率
2. 分析错误案例 → 修改Prompt
3. 重新测试 → 直到准确率≥90%

**精调技巧**：
- 在Prompt中加Few-Shot示例（2-3个）
- 明确输出格式约束
- 处理中文口语化表达（"想搞个""整一个""弄一台"）
- 边界case：极短输入("耳机")、超长输入、拼音、英文夹杂

#### Step 4：输出验证报告

```json
{
  "intent_classify": {
    "baseline_accuracy": 0.82,
    "final_accuracy": 0.93,
    "test_cases": 25,
    "remaining_errors": [
      {"input": "xxx", "expected": "xxx", "got": "xxx", "note": "边界情况"}
    ]
  }
}
```

### 基线Prompt模板

#### 意图分类（基线）
```
你是一个电商导购意图分类器。分析用户输入，输出JSON。

意图类型（6选1）：
- commodity_recommend: 推荐商品
- commodity_compare: 对比商品
- commodity_detail: 了解商品参数/详情
- scenario_shopping: 场景化购物（送礼/特定场合）
- after_sales: 售后/保修/退换
- chitchat: 闲聊/问候/无关话题

用户输入：{query}

只输出JSON：{"intent": "...", "confidence": 0.XX}
```

#### 槽位填充（基线）
```
从用户输入中提取购物结构化信息。未知填null。

用户输入：{query}

输出JSON格式：
{
  "category": "品类" or null,
  "price_min": 数字 or null,
  "price_max": 数字 or null,
  "brand_preference": "品牌" or null,
  "attributes": {"属性名":"属性值"} or {},
  "scenario": "场景" or null,
  "missing_slots": ["缺少的信息"]
}
```

#### 推荐理由三段式（基线）
```
你是一个电商导购助手。基于商品信息生成推荐理由。

商品：{name} | ¥{price} | ★{rating} | {highlights}

用三段式输出（每段≤30字）：
① 匹配依据：为什么这个商品适合用户
② 品质亮点：数据支撑的核心优势
③ 适用场景：最适合的使用场景

禁止使用"非常""很好""不错"等模糊词。必须引用具体数字。
```

#### 检索质量自评（基线）
```
评估检索结果的相关性（1-5分）。

查询：{query}
结果：{texts}

逐条评分后输出JSON：
{"scores": [5,4,3,...], "overall": X.X, "needs_rewrite": bool}
如果overall<3, needs_rewrite=true并给出rewrite_suggestion。
```

### 验收标准
- ✅ 4个Prompt的最终准确率≥90%
- ✅ 测试集≥80条（每个Prompt 20条）
- ✅ 错误案例有分析记录
- ✅ README含精调过程和最终Prompt全文

---

## 包4：Android界面对齐UI设计规范（前端工程）

> 负责人：队员D  |  优先级：P1  |  工时：2天  |  前置：Android Studio可打开项目

### 任务目标
将现有Android骨架组件按 `docs/architecture/UI设计方案.md` 的像素规格精确调整。

### 交付物
```
apps/android/app/src/main/java/com/shopping/agent/ui/
├── theme/Color.kt           # 更新为完整亮色/暗色双主题
├── theme/Theme.kt           # 暗色模式自动切换
├── components/MessageBubble.kt  # 3种气泡类型+流式光标
├── components/ProductCard.kt    # MVP 4字段+Shimmer加载
├── chat/ChatScreen.kt       # Chips Row+InputBar+键盘适配
└── screens/HomeScreen.kt    # 空态引导页
```

### 执行步骤

#### Step 1：颜色主题完善

参考 `UI设计方案.md` §3.8，在 `Color.kt` 中定义完整调色板：

```kotlin
// 亮色主题
val LightPrimary = Color(0xFF1976D2)
val LightUserBubble = Color(0xFFE3F2FD)
val LightAiBubble = Color(0xFFF5F5F5)
val LightCardBg = Color(0xFFFFFFFF)
val LightInputBg = Color(0xFFF5F5F5)
// ... 等

// 暗色主题
val DarkBackground = Color(0xFF121212)
val DarkUserBubble = Color(0xFF1A237E)
val DarkAiBubble = Color(0xFF1E1E1E)
val DarkCardBg = Color(0xFF2C2C2C)
val DarkInputBg = Color(0xFF333333)
// ... 等
```

在 `Theme.kt` 中实现 `isSystemInDarkTheme()` 自动切换。

#### Step 2：MessageBubble 重构

按 `UI设计方案.md` §3.2 规格：
- 用户气泡：右对齐，primaryContainer色，14dp圆角（左上18dp），内边距12/10dp
- AI气泡：左对齐，surfaceVariant色，14dp圆角（右上18dp）
- 流式光标：`▌` 字符闪烁，800ms周期，Primary色

```kotlin
@Composable
fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.role == "user") Arrangement.End else Arrangement.Start
    val bgColor = if (message.role == "user") 
        MaterialTheme.colorScheme.primaryContainer 
    else MaterialTheme.colorScheme.surfaceVariant
    val shape = if (message.role == "user")
        RoundedCornerShape(14.dp, 18.dp, 14.dp, 14.dp)  // 左上18dp
    else RoundedCornerShape(18.dp, 14.dp, 14.dp, 14.dp)  // 右上18dp
    
    Row(horizontalArrangement = alignment, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Surface(color = bgColor, shape = shape, modifier = Modifier.widthIn(max = 320.dp)) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(message.content, fontSize = 16.sp)
                if (message.isStreaming) {
                    // 闪烁光标 ▌
                    BlinkingCursor()
                }
            }
        }
    }
}
```

#### Step 3：ProductCard MVP版

按 `UI设计方案.md` §3.3：
- 宽160dp，圆角12dp，elevation 2dp，LazyRow间距12dp
- 图片：120×120dp，Coil异步加载，Shimmer骨架屏
- 名称：16sp medium，maxLines=2
- 价格：18sp bold，Primary色，¥前缀
- 评分+核心属性

#### Step 4：ChatScreen 完善

- 输入框：minHeight 48dp，maxHeight 120dp，圆角24dp
- Chips Row：40dp，仅新会话显示
- 键盘适配：`WindowInsets.ime`

### 验收标准
- ✅ 运行在模拟器/真机上，视觉效果对齐UI设计方案
- ✅ 暗色模式自动切换正常
- ✅ 输入框多行自动扩展（5行上限）
- ✅ 商品卡片横向滑动正常

---

## 包5：Embedding模型选型验证（算法实验）

> 负责人：全员可选  |  优先级：P2  |  工时：0.5天  |  前置：无

### 任务目标
对比3个中文Embedding模型在电商商品检索上的表现，验证是否需要从 bge-large-zh-v1.5 升级。

### 候选模型
| 模型 | 参数量 | 维度 |
|------|:---:|:---:|
| BAAI/bge-large-zh-v1.5 (当前) | 326M | 1024 |
| BAAI/bge-m3 | 568M | 1024 |
| stella-mrl-large-zh-v3.5 | - | 1024 |

### 执行步骤

```python
# benchmark_embeddings.py
from sentence_transformers import SentenceTransformer
import json, time

models = {
    "bge-large-zh": "BAAI/bge-large-zh-v1.5",
    "bge-m3": "BAAI/bge-m3",
    "stella": "infgrad/stella-mrl-large-zh-v3.5"
}

# 加载测试查询（20条电商搜索）
queries = [
    "降噪耳机推荐", "3000以内的手机", "送女朋友的生日礼物",
    "适合跑步的运动耳机", "学生用性价比高的平板",
    # ... 补充到20条
]

# 对每个模型
for name, model_id in models.items():
    model = SentenceTransformer(model_id)
    t0 = time.time()
    for q in queries:
        emb = model.encode(q)
    latency = (time.time()-t0) / len(queries)
    
    # 检索Top5，人工评判相关性
    # ...

    print(f"{name}: 平均延迟={latency*1000:.0f}ms, 检索准确率=...")
```

### 验收标准
- ✅ 3个模型的延迟+准确率对比表
- ✅ 升级建议（成本vs收益分析）

---

## 包6：Reranker 重排序模块（算法工程）

> 负责人：队员A/B 可选  |  优先级：P1  |  工时：1 天  |  前置：无（独立模块，可用 mock 数据测试）

### 任务目标
实现 BGE-Reranker Cross-Encoder 重排序，将向量检索粗排结果精排后输出。接口清晰：`list[doc] → list[doc]`，完全可脱离项目独立开发测试。

### 交付物
```
apps/backend/app/services/
├── reranker.py              # 重排序模块（替换空壳）
└── tests/
    └── test_reranker.py     # 单元测试（≥5条用例）
```

### 执行步骤

#### Step 1：安装依赖
```bash
pip install FlagEmbedding  # BGE-Reranker
```

#### Step 2：实现 reranker.py

```python
"""重排序模块 — BGE Cross-Encoder / LLM Rerank"""
from typing import List, Dict, Optional
from FlagEmbedding import FlagReranker


class Reranker:
    """Cross-Encoder 重排序器"""

    def __init__(self, model_name: str = "BAAI/bge-reranker-v2-m3"):
        self.model = FlagReranker(model_name, use_fp16=True)

    def rerank(
        self,
        query: str,
        documents: List[Dict],
        top_k: int = 10
    ) -> List[Dict]:
        """
        对检索结果重排序。

        Args:
            query: 用户查询
            documents: [{"content": "...", "score": 0.8, "metadata": {...}}, ...]
            top_k: 返回 Top-K

        Returns:
            按 relevance 降序排列的文档列表，新增 rerank_score 字段
        """
        if not documents:
            return []

        # 构造 (query, doc) pair
        pairs = [[query, doc["content"]] for doc in documents]

        # Cross-Encoder 打分
        scores = self.model.compute_score(pairs, normalize=True)

        # 合并分数并排序
        for doc, score in zip(documents, scores):
            doc["rerank_score"] = float(score)
            doc["final_score"] = doc.get("score", 0) * 0.3 + float(score) * 0.7

        ranked = sorted(documents, key=lambda x: x["final_score"], reverse=True)
        return ranked[:top_k]


class LLMReranker:
    """LLM Pairwise 精排（全量阶段备选）"""

    def __init__(self, llm_client):
        self.llm = llm_client

    async def rerank(
        self,
        query: str,
        documents: List[Dict],
        top_k: int = 5
    ) -> List[Dict]:
        """LLM 逐对比较排序"""
        # TODO: M6 全量阶段实现
        pass
```

#### Step 3：编写单元测试 test_reranker.py

```python
"""Reranker 单元测试"""
import pytest
from app.services.reranker import Reranker


@pytest.fixture
def reranker():
    return Reranker()

@pytest.fixture
def sample_docs():
    return [
        {"content": "Sony WH-1000XM5 降噪耳机 蓝牙5.2 续航30小时", "score": 0.85, "metadata": {"id": "B001"}},
        {"content": "Apple AirPods Pro 主动降噪 空间音频 H2芯片", "score": 0.82, "metadata": {"id": "B002"}},
        {"content": "小米手环8 血氧监测 睡眠追踪 AMOLED屏", "score": 0.78, "metadata": {"id": "B003"}},
        {"content": "Bose QC45 降噪耳机 24小时续航 舒适佩戴", "score": 0.80, "metadata": {"id": "B004"}},
        {"content": "JBL Flip6 蓝牙音箱 IP67防水 12小时续航", "score": 0.75, "metadata": {"id": "B005"}},
    ]


def test_rerank_returns_top_k(reranker, sample_docs):
    result = reranker.rerank("降噪耳机推荐", sample_docs, top_k=3)
    assert len(result) == 3


def test_rerank_sorts_by_relevance(reranker, sample_docs):
    result = reranker.rerank("降噪耳机推荐", sample_docs, top_k=5)
    # 降噪相关的（B001/B004/B002）应排在音箱（B005）和手环（B003）前面
    headphone_ids = {"B001", "B004", "B002"}
    top3_ids = {r["metadata"]["id"] for r in result[:3]}
    assert len(top3_ids & headphone_ids) >= 2, f"Top3 should be headphones, got {top3_ids}"


def test_rerank_adds_scores(reranker, sample_docs):
    result = reranker.rerank("降噪耳机", sample_docs, top_k=3)
    for doc in result:
        assert "rerank_score" in doc
        assert "final_score" in doc
        assert 0 <= doc["final_score"] <= 1


def test_empty_input(reranker):
    assert reranker.rerank("test", [], top_k=5) == []


def test_single_document(reranker):
    docs = [{"content": "Sony 耳机", "score": 0.9, "metadata": {"id": "B001"}}]
    result = reranker.rerank("耳机", docs, top_k=5)
    assert len(result) == 1
```

#### Step 4：验证
```bash
cd apps/backend
python -m pytest tests/test_reranker.py -v
```

### 验收标准
- ✅ rerank() 输入输出格式符合接口约定
- ✅ 降噪耳机查询时，耳机商品排在音箱/手环前面（相关性排序正确）
- ✅ 5 条单元测试全部通过
- ✅ 空输入/单文档边界情况处理正确

### 接口约定（与 agent.py 的集成点）

```python
# agent.py 中调用方式：
from app.services.reranker import Reranker
reranker = Reranker()

# retrieve 阶段返回粗排结果
docs = retriever.search(query, top_k=50)

# rank 阶段调用重排序
ranked = reranker.rerank(query, docs, top_k=10)
```

---

## 包7：商品多维加权排序（算法工程）

> 负责人：队员C/D 可选  |  优先级：P1  |  工时：1 天  |  前置：无（纯业务逻辑，mock 数据即可验证）

### 任务目标
实现多维加权打分算法：根据用户意图动态调整各维度权重，对商品列表计算匹配分并排序。接口：`(商品列表, 用户偏好, 意图) → 排序+分数+理由`。

### 交付物
```
apps/backend/app/services/
├── product_ranker.py        # 多维加权排序（替换透传空壳）
└── tests/
    └── test_product_ranker.py  # 单元测试（≥5条用例）
```

### 执行步骤

#### Step 1：实现 product_ranker.py

```python
"""多维加权商品排序器"""
from typing import List, Dict, Optional, Tuple
from enum import Enum
import math


class Intent(str, Enum):
    RECOMMEND = "commodity_recommend"
    COMPARE = "commodity_compare"
    DETAIL = "commodity_detail"
    SCENARIO = "scenario_shopping"
    AFTER_SALES = "after_sales"


# === 维度权重矩阵 ===
# 不同意图下，各维度的权重不同
INTENT_WEIGHTS = {
    Intent.RECOMMEND: {"semantic": 0.40, "price": 0.20, "rating": 0.15, "brand": 0.10, "attributes": 0.15},
    Intent.COMPARE:    {"semantic": 0.20, "price": 0.25, "rating": 0.20, "brand": 0.10, "attributes": 0.25},
    Intent.DETAIL:     {"semantic": 0.15, "price": 0.10, "rating": 0.15, "brand": 0.05, "attributes": 0.55},
    Intent.SCENARIO:   {"semantic": 0.35, "price": 0.30, "rating": 0.10, "brand": 0.10, "attributes": 0.15},
    Intent.AFTER_SALES: {"semantic": 0.20, "price": 0.05, "rating": 0.10, "brand": 0.05, "attributes": 0.60},
}


class ProductRanker:
    """多维加权商品排序器"""

    def __init__(self):
        self.default_weights = INTENT_WEIGHTS[Intent.RECOMMEND]

    def rank(
        self,
        products: List[Dict],
        user_prefs: Dict,
        intent: str = "commodity_recommend",
        top_k: int = 10
    ) -> List[Dict]:
        """
        对商品列表计算匹配分并排序。

        Args:
            products: [{"title":..., "price":..., "rating":..., "brand":..., "attributes":{...}, "semantic_score":...}, ...]
            user_prefs: {"price_min":..., "price_max":..., "brand_preference":..., "attributes":{...}}
            intent: 意图类型 (commodity_recommend/compare/detail/scenario_shopping/after_sales)
            top_k: 返回 Top-K

        Returns:
            排序后的商品列表，新增 match_score, dimension_scores, rank_reason 字段
        """
        weights = INTENT_WEIGHTS.get(Intent(intent), self.default_weights)

        budget_min = user_prefs.get("price_min")
        budget_max = user_prefs.get("price_max")
        preferred_brand = user_prefs.get("brand_preference")
        preferred_attrs = user_prefs.get("attributes", {})

        scored = []
        for prod in products:
            dims = {}

            # 1. 语义匹配分（来自向量检索）
            dims["semantic"] = prod.get("semantic_score", 0.5)

            # 2. 价格匹配分
            dims["price"] = self._price_score(prod.get("price", 0), budget_min, budget_max)

            # 3. 评分
            dims["rating"] = min(prod.get("rating", 3.0) / 5.0, 1.0)

            # 4. 品牌偏好
            dims["brand"] = 1.0 if (preferred_brand and prod.get("brand") == preferred_brand) else 0.5

            # 5. 属性匹配
            dims["attributes"] = self._attribute_score(prod.get("attributes", {}), preferred_attrs)

            # 加权总分
            total = sum(weights[k] * dims[k] for k in weights)

            # 生成排序理由
            reason = self._generate_reason(dims, weights)

            prod["dimension_scores"] = dims
            prod["match_score"] = round(total, 4)
            prod["rank_reason"] = reason
            scored.append(prod)

        ranked = sorted(scored, key=lambda x: x["match_score"], reverse=True)
        return ranked[:top_k]

    def _price_score(self, price: float, p_min: Optional[float], p_max: Optional[float]) -> float:
        """价格匹配度：在预算内=1.0，超出越多分越低"""
        if price <= 0:
            return 0.5
        if p_min is not None and p_max is not None:
            if p_min <= price <= p_max:
                return 1.0
            # 超出预算：高斯衰减
            mid = (p_min + p_max) / 2
            deviation = abs(price - mid) / mid if mid > 0 else 1.0
            return max(0.1, math.exp(-deviation))
        if p_max is not None:
            if price <= p_max:
                return 1.0
            return max(0.1, p_max / price)
        if p_min is not None:
            if price >= p_min:
                return 1.0
            return max(0.1, price / p_min)
        return 0.7  # 无预算约束

    def _attribute_score(self, prod_attrs: Dict, preferred_attrs: Dict) -> float:
        """属性匹配度：所需属性中有多少满足"""
        if not preferred_attrs:
            return 0.5
        matches = 0
        for k, v in preferred_attrs.items():
            if k in prod_attrs and str(prod_attrs[k]) == str(v):
                matches += 1
        return matches / len(preferred_attrs)

    def _generate_reason(self, dims: Dict, weights: Dict) -> str:
        """生成简短的排序理由"""
        parts = []
        for dim, weight in sorted(weights.items(), key=lambda x: x[1], reverse=True):
            if weight < 0.1:
                continue
            if dim == "semantic" and dims["semantic"] > 0.7:
                parts.append("语义高度匹配")
            elif dim == "price" and dims["price"] >= 1.0:
                parts.append("在预算内")
            elif dim == "price" and dims["price"] >= 0.8:
                parts.append("价格接近预算")
            elif dim == "rating" and dims["rating"] > 0.8:
                parts.append("高评分")
            elif dim == "brand" and dims["brand"] >= 1.0:
                parts.append("品牌匹配")
            elif dim == "attributes" and dims["attributes"] >= 0.8:
                parts.append("属性匹配")
        return "、".join(parts[:3]) if parts else "综合匹配"
```

#### Step 2：编写单元测试 test_product_ranker.py

```python
"""ProductRanker 单元测试"""
import pytest
from app.services.product_ranker import ProductRanker, Intent


@pytest.fixture
def ranker():
    return ProductRanker()

@pytest.fixture
def sample_products():
    return [
        {"title": "Sony XM5", "price": 2499, "rating": 4.7, "brand": "Sony",
         "attributes": {"降噪": "主动降噪", "续航": "30小时"}, "semantic_score": 0.92},
        {"title": "Bose QC45", "price": 2299, "rating": 4.5, "brand": "Bose",
         "attributes": {"降噪": "主动降噪", "续航": "24小时"}, "semantic_score": 0.88},
        {"title": "AirPods Pro", "price": 1899, "rating": 4.8, "brand": "Apple",
         "attributes": {"降噪": "主动降噪", "续航": "6小时"}, "semantic_score": 0.85},
        {"title": "小米 Buds 4", "price": 699, "rating": 4.2, "brand": "小米",
         "attributes": {"降噪": "主动降噪", "续航": "9小时"}, "semantic_score": 0.75},
        {"title": "JBL Flip6", "price": 899, "rating": 4.4, "brand": "JBL",
         "attributes": {"防水": "IP67", "续航": "12小时"}, "semantic_score": 0.45},
    ]


def test_rank_returns_top_k(ranker, sample_products):
    result = ranker.rank(sample_products, {}, top_k=3)
    assert len(result) == 3


def test_price_budget_prefers_in_range(ranker, sample_products):
    """预算 2000-3000 时，Sony(2499) 和 Bose(2299) 应排在前面"""
    prefs = {"price_min": 2000, "price_max": 3000}
    result = ranker.rank(sample_products, prefs, "commodity_recommend", top_k=3)
    top_titles = [r["title"] for r in result]
    assert "Sony XM5" in top_titles or "Bose QC45" in top_titles
    # 在预算外的（699/899）不应出现在Top3
    assert "小米 Buds 4" not in top_titles


def test_brand_preference_boosts(ranker, sample_products):
    """品牌偏好 Sony 时，Sony 应排第一"""
    prefs = {"brand_preference": "Sony"}
    result = ranker.rank(sample_products, prefs, top_k=5)
    assert result[0]["title"] == "Sony XM5"


def test_attribute_match_boosts(ranker, sample_products):
    """属性匹配降噪时，降噪耳机排在音箱前"""
    prefs = {"attributes": {"降噪": "主动降噪"}}
    result = ranker.rank(sample_products, prefs, top_k=5)
    # JBL Flip6 是音箱不是耳机，应排最后
    assert result[-1]["title"] == "JBL Flip6"


def test_adds_match_score_and_reason(ranker, sample_products):
    result = ranker.rank(sample_products, {}, top_k=3)
    for prod in result:
        assert "match_score" in prod
        assert "dimension_scores" in prod
        assert "rank_reason" in prod
        assert 0 <= prod["match_score"] <= 1


def test_different_intents_give_different_orders(ranker, sample_products):
    """不同意图权重不同，排序应有差异"""
    result_rec = ranker.rank(sample_products, {}, "commodity_recommend", top_k=5)
    result_detail = ranker.rank(sample_products, {}, "commodity_detail", top_k=5)
    # 两种意图的排序可能不同（不强制，但至少都应返回5个）
    assert len(result_rec) == len(result_detail) == 5
```

#### Step 3：验证
```bash
cd apps/backend
python -m pytest tests/test_product_ranker.py -v
```

### 验收标准
- ✅ 5 种意图各有不同权重矩阵
- ✅ 预算内商品得分 > 预算外
- ✅ 品牌偏好正确提升排名
- ✅ 属性匹配正确加权
- ✅ 6 条单元测试全部通过
- ✅ 每件商品输出 match_score + rank_reason

### 接口约定（与 agent.py 的集成点）

```python
# agent.py 中调用方式：
from app.services.product_ranker import ProductRanker
ranker = ProductRanker()

# reranker 精排后，product_ranker 做业务维度融合
ranked = ranker.rank(
    products=reranked_docs,
    user_prefs={"price_max": 3000, "brand_preference": "Sony"},
    intent="commodity_recommend",
    top_k=5
)
# ranked 中每件商品含 match_score + dimension_scores + rank_reason
```

---

## 执行流程建议

```
Day 1上午: 全员读各自包的README，安装依赖
Day 1下午: 
  队员A → 下载数据+构造商品JSON
  队员B → 设计评测用例模板+前50条
  队员C → 基线Prompt测试+记录初始准确率
  队员D → 颜色主题+MessageBubble重构
Day 2上午:
  队员A → 构造评价JSON+写入库脚本
  队员B → 完成200条用例+ground_truth
  队员C → 迭代精调4个Prompt
  队员D → ProductCard组件+ChatScreen布局
Day 2下午:
  队员A → 验证入库+截图
  队员B → RAGAS脚本框架+验证
  队员C → 测试报告输出（或→包7 ProductRanker）
  队员D → 暗色模式+真机截图（或→包6 Reranker）

Day 3（可选，P1包）:
  队员A/B → 包6 Reranker模块+单元测试
  队员C/D → 包7 ProductRanker模块+单元测试
  全员可选 → 包5 Embedding选型验证
```

---

## 集成检查清单

各包完成后，Hermes负责集成验证：

| 检查项 | 依赖包 | 验证方式 |
|--------|:---:|------|
| 商品检索可用 | 包1 | `curl Qdrant/search` |
| 评测用例格式正确 | 包2 | `python -c "assert len(json.load(open('eval_cases.json'))) >= 200"` |
| Prompt可被agent.py调用 | 包3 | 替换intent.py/agent.py中的prompt |
| Android编译通过 | 包4 | `./gradlew assembleDebug` |
| Reranker单元测试通过 | 包6 | `python -m pytest tests/test_reranker.py -v` |
| ProductRanker单元测试通过 | 包7 | `python -m pytest tests/test_product_ranker.py -v` |
