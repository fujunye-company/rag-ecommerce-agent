# PRD 背景资料 —— 学术文献补充

> **补充日期**：2026-05-19
> **检索来源**：CrossRef、Semantic Scholar、arXiv API
> **说明**：文献按主题分类，标注引用数和可信度

---

## 一、RAG 基础理论文献

### 1.1 RAG 原始论文
| 字段 | 内容 |
|------|------|
| 标题 | **Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks** |
| 作者 | Patrick Lewis, Ethan Perez, Aleksandra Piktus, Fabio Petroni 等 (Facebook AI Research) |
| 发表 | NeurIPS 2020 / arXiv:2005.11401（2020-05-22） |
| 引用数 | 5000+（学术领域里程碑论文） |
| 摘要 | 提出将参数化语言模型与非参数化外部记忆结合，通过检索增强生成。证明 RAG 在开放域问答、事实验证等任务上优于纯参数模型。 |
| 对项目的价值 | **奠基性文献**——RAG 的技术定义和原始动机 |
| 🔗 | https://arxiv.org/abs/2005.11401 |

### 1.2 RAGAS 评估框架
| 字段 | 内容 |
|------|------|
| 标题 | **Ragas: Automated Evaluation of Retrieval Augmented Generation** |
| 作者 | Shahul Es 等 |
| 发表 | arXiv:2309.15217（2023-09-26） |
| 摘要 | 提出无参考答案的 RAG 评估框架，引入忠实度（Faithfulness）、回答相关性（Answer Relevancy）、上下文精准度（Context Precision）、上下文召回率（Context Recall）四个核心指标。 |
| 对项目的价值 | **质量评测体系的直接参考**——本项目评测框架的核心理论基础 |
| 🔗 | https://arxiv.org/abs/2309.15217 |

### 1.3 ARES 自动评估框架
| 字段 | 内容 |
|------|------|
| 标题 | **ARES: An Automated Evaluation Framework for Retrieval-Augmented Generation Systems** |
| 作者 | Jon Saad-Falcon 等 (Stanford) |
| 发表 | arXiv:2311.09476（2023） |
| 引用数 | 256（Semantic Scholar） |
| 对项目的价值 | 自动化 RAG 评估的替代方案，可对比 RAGAS |
| 🔗 | https://arxiv.org/abs/2311.09476 |

### 1.4 RAG 忠实度基准
| 字段 | 内容 |
|------|------|
| 标题 | **Benchmarking LLM Faithfulness in RAG with Evolving Leaderboards** |
| 发表 | arXiv:2505.04847（2025） |
| 引用数 | 13 |
| 对项目的价值 | 最新 RAG 忠实度评估基准 |
| 🔗 | https://arxiv.org/abs/2505.04847 |

### 1.5 RAG 评估指标研究
| 字段 | 内容 |
|------|------|
| 标题 | **Evaluation of RAG Metrics for Question Answering in the Telecom Domain** |
| 发表 | arXiv:2407.12873（2024） |
| 引用数 | 38 |
| 对项目的价值 | 跨领域 RAG 指标适用性研究 |
| 🔗 | https://arxiv.org/abs/2407.12873 |

### 1.6 EvaRAG 评估方法
| 字段 | 内容 |
|------|------|
| 标题 | **EvaRAG: Evaluating Advanced RAG Techniques With Indexing and Distance Metrics** |
| 发表 | IEEE Access（2025） |
| 引用数 | 2 |
| 对项目的价值 | 先进 RAG 技术（索引策略、距离度量）的评估 |
| 🔗 | DOI: 10.1109/access.2025.3646665 |

---

## 二、电商 AI Agent 文献

### 2.1 电商对话购物 Agent（⭐ 直接相关）
| 字段 | 内容 |
|------|------|
| 标题 | **Cite Before You Speak: Enhancing Context-Response Grounding in E-commerce Conversational LLM-Agents** |
| 发表 | arXiv:2503.04830（2025-03-05） |
| 摘要 | 探讨基于 LLM 的对话式购物 Agent（CSA），提出"先引用再发言"策略增强回答的上下文锚定。指出当前 LLM 购物 Agent 的主要问题是缺乏对商品上下文的忠实引用。 |
| 对项目的价值 | **与项目目标高度一致**——商品推荐中的引用机制、对话 Agent 的上下文锚定 |
| 🔗 | https://arxiv.org/abs/2503.04830 |

### 2.2 Agentic Commerce 安全综述（⭐ 直接相关）
| 字段 | 内容 |
|------|------|
| 标题 | **SoK: Security of Autonomous LLM Agents in Agentic Commerce** |
| 发表 | arXiv（2026-04-15） |
| 摘要 | 系统化梳理 Agentic Commerce 的安全问题，涵盖谈判、购买、支付等环节的 LLM Agent 安全风险。 |
| 对项目的价值 | **安全设计参考**——Agent 自主交易的安全风险分类和防护 |
| 🔗 | https://arxiv.org/abs/最新（2026-04） |

### 2.3 Agentic Commerce 支付基础设施
| 字段 | 内容 |
|------|------|
| 标题 | **TessPay: Verify-then-Pay Infrastructure for Trusted Agentic Commerce** |
| 发表 | arXiv（2026-01-30） |
| 摘要 | 提出"先验证后支付"的 Agentic Commerce 可信支付框架。 |
| 对项目的价值 | Agentic Commerce 的支付信任机制，本项目虽不涉及支付但可参考信任设计 |
| 🔗 | https://arxiv.org/abs/最新（2026-01） |

### 2.4 大模型电商
| 字段 | 内容 |
|------|------|
| 标题 | **Large Language Model Commerce** |
| 发表 | IEEE Intelligent Systems（2026） |
| 摘要 | 探讨大模型在电商全链路的应用框架。 |
| 对项目的价值 | 顶刊视角的大模型电商全景 |
| 🔗 | DOI: 10.1109/mis.2026.3685768 |

### 2.5 多模态推荐 Agent
| 字段 | 内容 |
|------|------|
| 标题 | **MMAgentRec: A Personalized Multi-modal Recommendation Agent with Large Language Model** |
| 发表 | Scientific Reports（2025） |
| 引用数 | 11 |
| 对项目的价值 | 多模态 + Agent + 推荐的三合一架构 |
| 🔗 | DOI: 10.1038/s41598-025-96458-w |

### 2.6 自适应 LLM 推荐
| 字段 | 内容 |
|------|------|
| 标题 | **AdaRec: Adaptive Recommendation with LLMs via Narrative Profiling and Dual-Channel Reasoning** |
| 发表 | arXiv:2511.07166（2025-11） |
| 摘要 | 提出基于 LLM 的自适应推荐框架，通过叙事画像和双通道推理实现个性化推荐。 |
| 对项目的价值 | 用户画像与推荐推理的学术方案 |
| 🔗 | https://arxiv.org/abs/2511.07166 |

### 2.7 对话式个性化购物
| 字段 | 内容 |
|------|------|
| 标题 | **Conversational AI Personalized Shopping: An Intelligent Chatbot With Multi-Layered Recommendation in E-Commerce** |
| 发表 | IJLTEMAS（2026） |
| 对项目的价值 | 多层推荐架构参考 |
| 🔗 | DOI: 10.51583/ijltemas.2026.150100064 |

---

## 三、多模态与用户体验文献

### 3.1 消费者对 AI Chatbot vs 搜索引擎的响应
| 字段 | 内容 |
|------|------|
| 标题 | **Consumer Responses to Generative AI Chatbots Versus Search Engines for Product Evaluation** |
| 发表 | Journal of Theoretical and Applied Electronic Commerce Research（2024） |
| 引用数 | 11 |
| 对项目的价值 | **用户体验实证研究**——消费者在商品评估中使用 AI 聊天机器人 vs 传统搜索引擎的行为差异 |
| 🔗 | DOI: 10.3390/jtaer20020093 |

### 3.2 多模态商品属性提取
| 字段 | 内容 |
|------|------|
| 标题 | **DRAM: Dynamic Range Modulation for Multimodal Attribute Value Extraction on E-Commerce Product Data** |
| 发表 | Electronics（2025） |
| 对项目的价值 | 多模态商品数据提取技术 |
| 🔗 | DOI: 10.3390/electronics15050969 |

### 3.3 多模态商品评论呈现
| 字段 | 内容 |
|------|------|
| 标题 | **Multimodal Presentation of E-commerce Product Reviews and Ratings** |
| 发表 | Journal of Trade Science（2024） |
| 引用数 | 4 |
| 对项目的价值 | 商品信息的多模态呈现方式 |
| 🔗 | DOI: 10.1108/jts-03-2024-0018 |

### 3.4 AI 增强 CRM 与用户体验
| 字段 | 内容 |
|------|------|
| 标题 | **Evaluating The Efficacy Of AI-Enhanced CRM Tools In Enhancing User Experience In Indian Online Shopping** |
| 发表 | International Journal of Environmental Sciences（2025） |
| 对项目的价值 | AI 导购对用户体验的实际影响研究 |
| 🔗 | DOI: 10.64252/2jqx1g66 |

---

## 四、知识库与商品数据文献

### 4.1 电商领域 RAG 客服增强
| 字段 | 内容 |
|------|------|
| 标题 | **Customer Support Enhancement in E-Commerce with Retrieval-Augmented Generation** |
| 发表 | Journal of Quantum Science and Technology（2025） |
| 对项目的价值 | RAG 在电商客服中的直接应用 |
| 🔗 | DOI: 10.63345/jqst.v2i1.187 |

### 4.2 RAG 领域专用评估
| 字段 | 内容 |
|------|------|
| 标题 | **Evaluation of Retrieval Methods in Domain-Specific Chatbots Based on RAG** |
| 发表 | JSAI（2025） |
| 对项目的价值 | 领域专用 Chatbot 的检索方法评估 |
| 🔗 | DOI: 10.36085/jsai.v9i1.9897 |

---

## 五、安全与治理文献

### 5.1 Agentic RAG 忠实度评估
| 字段 | 内容 |
|------|------|
| 标题 | **Evaluating Faithfulness in Agentic RAG Systems for e-Governance Applications** |
| 发表 | Big Data and Cognitive Computing（2025） |
| 引用数 | 3 |
| 对项目的价值 | Agentic RAG 忠实度评估方法论 |
| 🔗 | DOI: 10.3390/bdcc9120309 |

### 5.2 RAG 增强 AI 可信度
| 字段 | 内容 |
|------|------|
| 标题 | **Retrieval-Augmented Generation: Enhancing AI with Reliable Knowledge** |
| 发表 | International Journal of Science and Research（2025） |
| 对项目的价值 | RAG 作为 AI 可信度增强手段的论证 |
| 🔗 | DOI: 10.21275/sr251104092705 |

---

## 六、文献使用建议

### 对 PRD 写作的直接引用建议

| PRD 章节 | 推荐引用文献 | 引用点 |
|----------|-------------|--------|
| 项目背景 | RAG (Lewis 2020) | RAG 技术基础引用 |
| 技术方案 | RAGAS (Es 2023) + ARES (Saad-Falcon 2023) | 评测框架的理论基础 |
| 产品设计 | Cite Before You Speak (2025) | 对话 Agent 的引用机制 |
| 竞品分析 | Consumer Responses to GenAI Chatbots (2024) | 用户对 AI 导购接受度的学术依据 |
| 风险管理 | SoK Agentic Commerce Security (2026) | Agentic Commerce 安全风险分类 |
| 创新点 | MMAgentRec (2025) + AdaRec (2025) | 多模态 + Agent + 推荐的学术前沿 |

### 文献质量分级

| 等级 | 论文 | 依据 |
|------|------|------|
| 🥇 顶会/顶刊 | RAG (NeurIPS 2020)、Large Language Model Commerce (IEEE IS 2026)、MMAgentRec (Scientific Reports) | 顶会/顶刊发表 |
| 🥈 高引论文 | RAGAS、ARES (256引)、RAG Metrics (38引) | 被引数证明影响力 |
| 🥉 直接相关 | Cite Before You Speak、SoK Agentic Commerce、EvaRAG | 与项目主题高度吻合 |
| 🏅 参考价值 | 其余文献 | 提供特定视角或方法论参考 |

### 待补充的文献方向
- 中文电商 AI 导购的学术文献（CNKI 需要反爬验证码，建议人工检索）
- 流式输出 UX 的实证研究
- A/B 测试在 AI 产品中的方法论论文
- 大模型幻觉在电商场景的量化研究
