# Agent 框架架构分析

> 文档日期: 2026-05-19
> 分析范围: LangGraph (开源) | OpenAI Agents SDK (闭源) | Dify (开源平台)
> 聚焦维度: 核心架构图、技术栈、独特亮点、适用性评分

---

## 目录

1. [LangGraph (开源)](#1-langgraph-开源)
2. [OpenAI Agents SDK (闭源)](#2-openai-agents-sdk-闭源)
3. [Dify (开源平台)](#3-dify-开源平台)
4. [综合对比与选型建议](#4-综合对比与选型建议)

---

## 1. LangGraph (开源)

### 1.1 概述

LangGraph 是 LangChain 团队推出的有状态多 Actor 应用框架，专为构建可定制、可控制的 Agent 工作流而设计。它将 Agent 建模为图 (Graph) 结构，节点代表计算步骤，边代表控制流。2024 年发布后迅速成为开源 Agent 框架的事实标准。

| 维度 | 详情 |
|------|------|
| 开发者 | LangChain (LangChain Inc.) |
| 许可协议 | MIT |
| GitHub Stars | 40k+ |
| 语言 | Python / TypeScript |
| 核心理念 | Graph = Agent, State = Memory |

### 1.2 核心架构图

```
┌──────────────────────────────────────────────────────────────────┐
│                      LangGraph Runtime                           │
│                                                                  │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐     │
│  │   StateGraph ├────>│  Checkpointer├────>│   Store      │     │
│  │   (状态图定义) │     │  (持久化)     │     │  (长期内存)   │     │
│  └──────┬───────┘     └──────────────┘     └──────────────┘     │
│         │                                                        │
│  ┌──────▼───────────────────────────────────────────────────┐   │
│  │                    Graph Execution                       │   │
│  │  ┌──────┐   ┌──────┐   ┌──────┐   ┌──────┐   ┌──────┐  │   │
│  │  │ Node │──>│ Node │──>│ Node │──>│ Node │──>│ END  │  │   │
│  │  │  A   │   │  B   │   │  C   │   │  D   │   │      │  │   │
│  │  └──────┘   └──────┘   └──┬───┘   └──────┘   └──────┘  │   │
│  │                          │                              │   │
│  │                     ┌────▼────┐                         │   │
│  │                     │Condition│─────> Conditional Edges │   │
│  │                     └─────────┘                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                   Subgraph & Hooks                       │   │
│  │  • 子图嵌套 (Subgraph)  • Human-in-the-Loop (中断/恢复)    │   │
│  │  • Streaming (流式输出)  • ToolNode (工具调用节点)         │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

**图执行流程示意:**

```
                    ┌─────────────┐
                    │   START     │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  Agent      │ ◄── ToolNode 封装 LLM + 工具调用
                    │  Node       │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  Should     │ ◄── 条件边: 判断是否继续
                    │  Continue?  │
                    └──┬───────┬──┘
                       │ YES   │ NO
              ┌────────▼──┐  ┌─▼─────────┐
              │ Tool      │  │   END     │
              │ Execution │  └───────────┘
              └────┬──────┘
                   │
              ┌────▼──────┐
              │  Agent    │ ◄── 循环回 Agent Node
              │  Node     │
              └───────────┘
```

### 1.3 技术栈

```
┌─────────────────────────────────────────┐
│              LangGraph 技术栈             │
├─────────────────────────────────────────┤
│  核心层                                  │
│  ├── Python 3.10+ / TypeScript          │
│  ├── Pydantic v2 (数据模型与验证)         │
│  ├── LangChain Core (消息/工具抽象)       │
│  └── Rust (部分高性能模块, via PyO3)      │
│                                          │
│  LLM 集成                                │
│  ├── LangChain LLMs (100+ 模型提供商)     │
│  ├── 原生支持: OpenAI / Anthropic / ...  │
│  └── 自定义模型适配器                      │
│                                          │
│  持久化 / 内存                           │
│  ├── InMemory / SqliteSaver (开发)        │
│  ├── PostgresSaver (生产)                 │
│  ├── RedisSaver (分布式)                  │
│  └── LangGraph Store (长期记忆 API)       │
│                                          │
│  部署 / 服务化                            │
│  ├── LangGraph Platform (商业)            │
│  ├── LangGraph CLI (dev server)          │
│  ├── FastAPI / Starlette (自建)          │
│  └── Docker / K8s                       │
│                                          │
│  监控 / 调试                              │
│  ├── LangSmith (官方可观测性平台)          │
│  ├── LangGraph Studio (可视化调试)         │
│  └── Checkpoint 回溯 (时间旅行调试)        │
└─────────────────────────────────────────┘
```

### 1.4 关键代码示例

```python
# LangGraph 核心示例: ReAct Agent with Human-in-the-Loop

from typing import TypedDict, Literal
from langgraph.graph import StateGraph, END
from langgraph.checkpoint.memory import MemorySaver
from langgraph.prebuilt import ToolNode
from langchain_core.messages import HumanMessage, AIMessage
from langchain_openai import ChatOpenAI

# ---- 1. 定义状态 ----
class AgentState(TypedDict):
    messages: list          # 对话历史
    next_step: str          # 控制: "continue" | "end" | "human_review"

# ---- 2. 定义节点 ----
def agent_node(state: AgentState) -> dict:
    """Agent 决策节点: 调用 LLM 决定下一步"""
    llm = ChatOpenAI(model="gpt-4o")
    llm_with_tools = llm.bind_tools([search_tool, calculator_tool])
    response = llm_with_tools.invoke(state["messages"])
    return {"messages": [response]}

def should_continue(state: AgentState) -> Literal["tools", "human_review", "end"]:
    """条件路由: 判断是否需要调用工具/人工审核/结束"""
    last_message = state["messages"][-1]
    if last_message.tool_calls:
        if any(tc["name"] == "submit_order" for tc in last_message.tool_calls):
            return "human_review"
        return "tools"
    return "end"

# ---- 3. 构建图 ----
graph = StateGraph(AgentState)

graph.add_node("agent", agent_node)
graph.add_node("tools", ToolNode([search_tool, calculator_tool]))
graph.add_node("human_review", human_review_node)

graph.set_entry_point("agent")
graph.add_conditional_edges("agent", should_continue, {
    "tools": "tools",
    "human_review": "human_review",
    "end": END
})
graph.add_edge("tools", "agent")       # 工具结果返回 agent 继续
graph.add_edge("human_review", "agent") # 审核后继续

# ---- 4. 编译并运行 ----
memory = MemorySaver()
app = graph.compile(checkpointer=memory)

# 运行, thread_id 用于会话隔离
config = {"configurable": {"thread_id": "user-001"}}
result = app.invoke(
    {"messages": [HumanMessage(content="查询订单 #12345")]},
    config=config
)

# ---- 5. Human-in-the-Loop: 中断与恢复 ----
# 在 human_review_node 中中断
app.interrupt("需要人工审核订单，请确认后恢复")

# 后续恢复执行
app.invoke(None, config=config)  # 传入 None 从中断点继续
```

### 1.5 独特亮点

1. **图即 Agent (Graph-as-Agent)**: 用有向图显式建模 Agent 控制流，天然支持循环、并行分支、条件路由。所有路径编译时可验证，比传统 prompt-chaining 更可靠。

2. **一流的状态管理与持久化**: 通过 Checkpointer 机制自动存储每一步的状态快照，支持任意中断点恢复、时间旅行调试、多会话隔离。

3. **Human-in-the-Loop (HITL) 原生支持**: 内置 `interrupt()` 机制，可在图执行的任意节点暂停并等待人工输入，恢复后无缝继续。这对企业合规场景至关重要。

4. **子图与可组合性**: 支持 Agent 嵌套 Agent (Subgraph)，允许将复杂 Agent 拆分为可复用的子模块，类似编程中的函数调用。

5. **双模式 Agent 构建**:
   - `create_react_agent()`: 预构建的 ReAct 模式，开箱即用
   - `StateGraph`: 完全自定义的图构建，极致灵活

6. **生态系统深度绑定**: 与 LangChain 生态 (100+ LLM 集成, 1000+ 工具)、LangSmith (可观测性)、LangGraph Platform (生产部署) 形成完整闭环，但也可以脱离 LangChain 独立使用。

7. **流式输出 (Streaming)**: 支持多种流模式: `values` (状态变化), `updates` (增量更新), `messages` (token级), `debug` (调试信息)。

### 1.6 适用性评分

| 维度 | 评分 (1-10) | 说明 |
|------|------------|------|
| 学习曲线 | 6/10 | 图编程范式有学习成本，但预构建 Agent 降低门槛 |
| 灵活性 | 10/10 | 图结构可表达任意控制流，插件式节点设计 |
| 生产就绪 | 8/10 | Checkpointer 持久化 + LangGraph Platform，但生态尚在演进 |
| 社区生态 | 9/10 | LangChain 生态用户基数大，文档/教程丰富 |
| 多模型支持 | 10/10 | 可接入任意 LLM (100+ 集成) |
| 企业合规 | 8/10 | HITL 原生支持，审计日志需依赖 LangSmith |
| **综合** | **8.5/10** | **最强灵活性与生态，适合需要精细控制流的生产 Agent** |

---

## 2. OpenAI Agents SDK (闭源)

### 2.1 概述

OpenAI Agents SDK 是 OpenAI 官方推出的 Agent 开发框架 (原名 Swarm，2025年3月正式发布)。它基于 "Agent = LLM + Instructions + Tools + Handoff" 的设计哲学，强调轻量级、快速迭代和原生 OpenAI 模型集成。完全闭源但免费使用。

| 维度 | 详情 |
|------|------|
| 开发者 | OpenAI |
| 许可协议 | Apache 2.0 (代码开源), 但深度绑定 OpenAI API |
| GitHub Stars | 20k+ |
| 语言 | Python 3.10+ |
| 核心理念 | Agent + Handoff = Multi-Agent Orchestration |

### 2.2 核心架构图

```
┌────────────────────────────────────────────────────────────────────┐
│                    OpenAI Agents SDK Runtime                       │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                    Runner (运行器)                            │ │
│  │  run(agent, input) → streaming events                       │ │
│  └────────────────────────┬─────────────────────────────────────┘ │
│                           │                                       │
│  ┌────────────────────────▼─────────────────────────────────────┐ │
│  │              Agent Loop (Agent 主循环)                        │ │
│  │                                                              │ │
│  │   ┌──────────┐    ┌──────────┐    ┌──────────┐              │ │
│  │   │  Agent   │───>│   LLM    │───>│ Response │              │ │
│  │   │ (指令+工具)│    │  (模型)   │    │  (输出)   │              │ │
│  │   └──────────┘    └──────────┘    └─────┬────┘              │ │
│  │                                        │                    │ │
│  │              ┌─────────────────────────┼───────────┐        │ │
│  │              │                         │           │        │ │
│  │        ┌─────▼─────┐          ┌───────▼──────┐  ┌─▼──────┐ │ │
│  │        │  Tool     │          │  Handoff     │  │ Final  │ │ │
│  │        │  Call     │          │  (转移控制权)  │  │ Output │ │ │
│  │        └─────┬─────┘          └───────┬──────┘  └────────┘ │ │
│  │              │                        │                    │ │
│  │              └────────── 回到 LLM ◄────┘                    │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                  周边能力                                     │ │
│  │  ┌───────────┐  ┌──────────┐  ┌──────────┐  ┌────────────┐ │ │
│  │  │ Guardrails │  │ Tracing  │  │ Handoff  │  │ Function   │ │ │
│  │  │ (安全护栏)  │  │ (追踪)   │  │ Filters  │  │ Tools      │ │ │
│  │  └───────────┘  └──────────┘  └──────────┘  └────────────┘ │ │
│  └──────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────┘
```

**Handoff (Agent 间转移) 机制:**
```
                    ┌─────────────────┐
                    │  Triage Agent   │ ◄── 入口: 用户消息
                    │  (分流/路由)     │
                    └───┬─────┬─────┬─┘
                        │     │     │
              ┌─────────▼┐ ┌──▼──────▼┐ ┌─────────▼┐
              │ Sales    │ │ Support  │ │ Refund   │
              │ Agent    │ │ Agent    │ │ Agent    │
              └─────────┘ └──────────┘ └─────────┘
                   │            │            │
                   └────────────┼────────────┘
                                │
                          Handoff Back
                     (返回分流 Agent 或升级)
```

### 2.3 技术栈

```
┌──────────────────────────────────────────┐
│        OpenAI Agents SDK 技术栈           │
├──────────────────────────────────────────┤
│  核心层                                   │
│  ├── Python 3.10+ (纯 Python)            │
│  ├── Pydantic v2 (结构化输出)             │
│  ├── OpenAI Python SDK (底层 API)         │
│  └── asyncio (全异步架构)                 │
│                                           │
│  LLM 集成                                 │
│  ├── OpenAI 模型 (原生, 深度集成)          │
│  │   ├── GPT-4o / GPT-4.1               │
│  │   ├── o3 / o4-mini (推理模型)         │
│  │   └── GPT-5 (最新)                    │
│  ├── 第三方模型 (有限)                     │
│  │   ├── 通过 OpenAI-compatible API       │
│  │   └── 非官方支持落后于 LangGraph        │
│  └── 推理模型特殊处理 (reasoning_effort)   │
│                                           │
│  工具系统                                  │
│  ├── FunctionTool (Python 函数 → 工具)     │
│  ├── HostedTool (Code Interpreter 等)     │
│  ├── Agent-as-Tool (Agent 作为工具调用)    │
│  └── MCP (Model Context Protocol) 支持    │
│                                           │
│  Handoff (Agent 间通信)                   │
│  ├── handoff() 函数 (转移控制权)           │
│  ├── HandoffInputFilter (输入过滤)        │
│  └── 上下文传递 (对话历史自动携带)          │
│                                           │
│  安全与护栏                                │
│  ├── Guardrails (Input/Output)            │
│  ├── 内容过滤 (OpenAI Moderation API)      │
│  └── 敏感信息脱敏                          │
│                                           │
│  追踪与监控                                │
│  ├── OpenAI Traces (Dashboard)            │
│  ├── 自定义 Trace Processor               │
│  └── Python logging 集成                  │
│                                           │
│  部署                                      │
│  ├── 无官方部署平台                         │
│  ├── 自建 FastAPI/Flask 包装               │
│  └── 无内置会话持久化 (需自建)              │
└──────────────────────────────────────────┘
```

### 2.4 关键代码示例

```python
# OpenAI Agents SDK 核心示例: 多 Agent 协作 + Handoff

from agents import (
    Agent, Runner, handoff, function_tool,
    GuardrailFunctionOutput, input_guardrail, output_guardrail,
    RunContextWrapper
)
from pydantic import BaseModel
import asyncio

# ---- 0. 定义结构化输出 ----
class OrderInfo(BaseModel):
    order_id: str
    status: str
    total: float

# ---- 1. 定义工具 ----
@function_tool
def lookup_order(order_id: str) -> OrderInfo:
    """查询订单信息"""
    # 实际调用数据库/API
    return OrderInfo(order_id=order_id, status="shipped", total=99.99)

@function_tool
def process_refund(order_id: str, reason: str) -> str:
    """处理退款"""
    return f"退款已处理: 订单 {order_id}, 原因: {reason}"

# ---- 2. 定义 Guardrail (安全护栏) ----
@input_guardrail
async def sensitive_data_guard(
    ctx: RunContextWrapper, agent: Agent, input_data: str
) -> GuardrailFunctionOutput:
    """检查输入中是否包含敏感信息"""
    if "password" in input_data.lower() or "ssn" in input_data.lower():
        return GuardrailFunctionOutput(
            output_info={"blocked": True},
            tripwire_triggered=True
        )
    return GuardrailFunctionOutput(
        output_info={"blocked": False},
        tripwire_triggered=False
    )

# ---- 3. 定义 Agents ----
# 分流 Agent
triage_agent = Agent(
    name="Triage Agent",
    instructions="你是一个客服分流助手。根据用户意图，将对话转给相应的专业 Agent。",
    handoffs=[
        handoff(sales_agent),      # 引用前向声明, 实际定义在下方
        handoff(support_agent),
        handoff(refund_agent),
    ],
    input_guardrails=[sensitive_data_guard],
)

# 销售 Agent
sales_agent = Agent(
    name="Sales Agent",
    instructions="你是销售专家。帮助用户了解产品、推荐商品、计算价格。",
    tools=[lookup_order],
    handoffs=[handoff(triage_agent)],  # 可返回分流
)

# 售后 Agent
support_agent = Agent(
    name="Support Agent",
    instructions="你是技术支持。帮助用户解决产品使用问题。",
    tools=[lookup_order],
    handoffs=[handoff(triage_agent)],
)

# 退款 Agent
refund_agent = Agent(
    name="Refund Agent",
    instructions="你是退款专员。严格按照退款政策处理退款请求。",
    tools=[process_refund, lookup_order],
    output_type=OrderInfo,  # 结构化输出
    handoffs=[handoff(triage_agent)],
)

# ---- 4. 执行 ----
async def main():
    result = await Runner.run(
        starting_agent=triage_agent,
        input="我的订单 #12345 还没收到，我要退款！",
    )
    print(f"最终 Agent: {result.last_agent.name}")
    print(f"最终输出: {result.final_output}")  # 可能是 OrderInfo 结构化对象
    # result.to_input_list() → 获取完整对话历史
    # result.new_items → 追踪执行过程中的每一步

asyncio.run(main())
```

### 2.5 独特亮点

1. **极简原语 (Minimal Primitives)**: 只用 Agent、Tool、Handoff 三个核心原语构建多 Agent 系统。与 LangGraph 的图编程相比，心智负担极低，10 行代码即可创建可工作的 Agent。

2. **Handoff 机制**: 业界最优雅的 Agent 间通信方式。不是简单的 "Agent A 调用 Agent B"，而是真正转移控制权 (包括对话上下文)。LLM 自主决定何时 Handoff，无需显式编排。

3. **一流推理模型支持**: 原生支持 o-series 推理模型 (o3, o4-mini)，通过 `reasoning_effort` 参数控制推理深度，对复杂多步骤任务效果显著提升。

4. **结构化输出即类型**: 每个 Agent 和 Tool 可直接绑定 Pydantic 模型作为输出类型，SDK 自动处理 JSON Schema 生成和验证，无需手动 prompt engineering。

5. **Guardrails 安全护栏**: Input/Output 双护栏机制，可拦截、修改、阻断输入输出。`tripwire_triggered` 标志提供异常检测和回退能力。

6. **Agent-as-Tool**: 将任意 Agent 封装为工具供其他 Agent 调用，实现深度 Agent 组合。例如: 把一个多步推理 Agent 作为另一个 Agent 的单一工具。

7. **全异步架构**: 从头构建为 asyncio 原生，支持高并发场景。Streaming 事件流覆盖 Agent 执行的每个阶段。

8. **追踪可视化**: 内置 OpenTelemetry 风格的追踪系统，可在 OpenAI Dashboard 中可视化查看 Agent 执行轨迹、Tool 调用、Handoff 跳转。

### 2.6 适用性评分

| 维度 | 评分 (1-10) | 说明 |
|------|------------|------|
| 学习曲线 | 9/10 | 极简 API, 10 分钟上手, 半天精通 |
| 灵活性 | 6/10 | Handoff 模式优雅但受限于设计范式 |
| 生产就绪 | 6/10 | 无内置持久化, 无官方部署平台, 需大量自建 |
| 社区生态 | 6/10 | 快速成长但远不及 LangChain 生态 |
| 多模型支持 | 4/10 | 深度绑定 OpenAI, 第三方模型支持弱 |
| 企业合规 | 5/10 | Guardrails 内置但缺少审计/持久化/自托管 |
| **综合** | **6.0/10** | **极致开发体验, 但生产与企业场景受限** |

---

## 3. Dify (开源平台)

### 3.1 概述

Dify 是面向非开发者的 AI 应用构建平台，提供可视化工作流编排、RAG 管道、Agent 策略等完整能力。它将 Agent 框架从 "开发者工具" 升级为 "产品级平台"，目标是让任何人都能构建生产级 AI 应用。

| 维度 | 详情 |
|------|------|
| 开发者 | LangGenius, Inc. |
| 许可协议 | Apache 2.0 (社区版), 商业版需授权 |
| GitHub Stars | 60k+ |
| 语言 | Python (后端) + TypeScript (前端) |
| 核心理念 | 可视化编排 + 开箱即用 = AI 应用民主化 |

### 3.2 核心架构图

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           Dify Platform                                  │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                     Web UI (Next.js)                               │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────────┐   │ │
│  │  │ Workflow │ │ Chatflow │ │ Agent    │ │ Knowledge Base     │   │ │
│  │  │ Builder  │ │ Builder  │ │ Builder  │ │ (RAG) Manager      │   │ │
│  │  │ (可视化编排)│ │ (对话流)  │ │ (策略)   │ │ (文档管理)         │   │ │
│  │  └──────────┘ └──────────┘ └──────────┘ └────────────────────┘   │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                     │
│  ┌─────────────────────────────────▼──────────────────────────────────┐ │
│  │                      API Layer (Flask)                             │ │
│  │  ┌───────────────┐  ┌──────────────┐  ┌───────────────────────┐   │ │
│  │  │ App API       │  │ Knowledge    │  │ Workspace / Auth /    │   │ │
│  │  │ (执行/管理)    │  │ Base API     │  │ Billing               │   │ │
│  │  └───────────────┘  └──────────────┘  └───────────────────────┘   │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                     │
│  ┌─────────────────────────────────▼──────────────────────────────────┐ │
│  │                    Core Services (Celery 任务队列)                  │ │
│  │                                                                     │ │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────────┐   │ │
│  │  │ Workflow Engine │  │ Agent Strategy  │  │ RAG Pipeline     │   │ │
│  │  │  (工作流运行时)   │  │  (Agent 策略)    │  │  (检索增强生成)    │   │ │
│  │  │                 │  │                 │  │                  │   │ │
│  │  │ • 节点执行       │  │ • ReAct         │  │ • 文档解析        │   │ │
│  │  │ • 条件分支       │  │ • Function      │  │ • 向量化 (Embed)  │   │ │
│  │  │ • 变量传递       │  │   Calling       │  │ • 检索 (Search)   │   │ │
│  │  │ • 错误处理       │  │ • CoT           │  │ • 重排序 (Rerank) │   │ │
│  │  │ • 并行执行       │  │                 │  │ • 生成 (Generate) │   │ │
│  │  └─────────────────┘  └─────────────────┘  └──────────────────┘   │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                     │
│  ┌─────────────────────────────────▼──────────────────────────────────┐ │
│  │                    Infrastructure (基础设施)                        │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐     │ │
│  │  │PostgreSQL│  │  Redis   │  │ Vector   │  │ Object Storage│     │ │
│  │  │(元数据)   │  │ (缓存/队列)│  │ Store    │  │ (文档/文件)    │     │ │
│  │  └──────────┘  └──────────┘  │(Qdrant/  │  │(S3/MinIO)     │     │ │
│  │                              │Weaviate) │  └───────────────┘     │ │
│  │                              └──────────┘                        │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘
```

**Agent 策略内视图:**

```
┌───────────────────────────────────────────────┐
│             Dify Agent 策略模式                │
├───────────────────────────────────────────────┤
│                                                │
│   Strategy 1: Function Calling                │
│   ┌─────────┐    ┌─────────┐    ┌─────────┐  │
│   │  User   │───>│   LLM   │───>│  Tool   │  │
│   │  Input  │    │ (决策)   │    │  Calls  │  │
│   └─────────┘    └─────────┘    └────┬────┘  │
│                                      │        │
│   Strategy 2: ReAct (Thought/Observe) │        │
│   ┌─────────┐ ┌────────┐ ┌─────────┐│        │
│   │ Thought │→│ Action │→│Observe  ││        │
│   └─────────┘ └────────┘ └─────────┘│        │
│         ↑                           │        │
│         └─────────── Loop ──────────┘        │
│                                                │
│   Strategy 3: Multi-Agent (子任务委派)         │
│   ┌─────────┐    ┌──────────┐                 │
│   │Primary  │───>│ Sub-Agent│                 │
│   │ Agent   │    │  (专项)   │                 │
│   └─────────┘    └──────────┘                 │
│                                                │
└───────────────────────────────────────────────┘
```

### 3.3 技术栈

```
┌─────────────────────────────────────────────┐
│              Dify 技术栈                     │
├─────────────────────────────────────────────┤
│  前端                                       │
│  ├── Next.js 14+ (React SSR)               │
│  ├── TailwindCSS + Radix UI                │
│  ├── React Flow (工作流可视化编辑器)          │
│  └── TypeScript                            │
│                                              │
│  后端                                       │
│  ├── Python 3.10+ / Flask                  │
│  ├── Celery (异步任务队列)                    │
│  ├── SQLAlchemy + Alembic (ORM/迁移)        │
│  └── Pydantic (数据验证)                     │
│                                              │
│  数据存储                                    │
│  ├── PostgreSQL (元数据/配置/用户)            │
│  ├── Redis (缓存/会话/消息队列)               │
│  ├── 向量数据库 (可插拔)                      │
│  │   ├── Qdrant (默认推荐)                   │
│  │   ├── Weaviate                           │
│  │   ├── Milvus                             │
│  │   ├── pgvector                           │
│  │   └── 阿里云/腾讯云向量服务                 │
│  └── 对象存储 (S3/MinIO/Azure)               │
│                                              │
│  LLM 集成                                   │
│  ├── 50+ 模型提供商 (OpenAI, Claude, ...)    │
│  ├── 本地模型 (Ollama, Xinference, vLLM)     │
│  └── 统一模型接口抽象                         │
│                                              │
│  RAG 管道                                    │
│  ├── 文档解析 (PDF/Word/网页/Notion/...)     │
│  ├── 多种切分策略 (递归/语义/自定义)           │
│  ├── 嵌入模型 (OpenAI/Cohere/本地)            │
│  ├── 检索模式 (关键词/向量/混合/多路)          │
│  └── 重排序 (Cohere Rerank / 本地模型)        │
│                                              │
│  部署                                        │
│  ├── Docker Compose (单机, 推荐)             │
│  ├── Kubernetes (生产集群)                   │
│  └── Dify Cloud (SaaS, 商业)                 │
│                                              │
│  插件生态                                     │
│  ├── Plugin Marketplace (工具/模型/扩展)      │
│  ├── 自定义工具 (API-based)                  │
│  └── 代码节点 (Python/JS 沙箱执行)            │
└─────────────────────────────────────────────┘
```

### 3.4 关键代码/配置示例

```yaml
# Dify Workflow DSL (声明式工作流定义, YAML 格式)
# 这是 Dify 工作流引擎的底层表示, 通常通过 UI 生成

app:
  name: "电商智能客服 Agent"
  mode: "advanced-chat"  # workflow | chat | agent-chat | completion
  description: "处理订单查询、退款、商品推荐的智能客服"

workflow:
  graph:
    nodes:
      - id: "start"
        type: "start"
        data:
          variables:
            - name: "user_query"
              type: "string"
              required: true

      - id: "intent_classifier"
        type: "llm"
        data:
          model: "gpt-4o"
          prompt_template: |
            分类用户意图为以下类别之一:
            - order_query: 订单查询
            - refund_request: 退款申请
            - product_recommend: 商品推荐
            - general_qa: 通用问答
            用户输入: {{#start.user_query#}}
          output_schema:
            intent: string
            confidence: number

      - id: "condition_router"
        type: "if-else"
        data:
          conditions:
            - condition: "{{#intent_classifier.intent == 'order_query'}}"
              target: "order_lookup"
            - condition: "{{#intent_classifier.intent == 'refund_request'}}"
              target: "refund_agent"
            - condition: "{{#intent_classifier.intent == 'product_recommend'}}"
              target: "recommend_agent"
            - condition: "{{#intent_classifier.intent == 'general_qa'}}"
              target: "knowledge_retrieval"

      - id: "order_lookup"
        type: "tool"
        data:
          tool_name: "order_api"
          inputs:
            user_id: "{{#sys.user_id}}"

      - id: "knowledge_retrieval"
        type: "knowledge-retrieval"
        data:
          knowledge_base_id: "kb_ecommerce_faq"
          retrieval_mode: "hybrid"  # 混合检索
          top_k: 5
          score_threshold: 0.7

      - id: "final_response"
        type: "llm"
        data:
          model: "gpt-4o"
          prompt_template: |
            基于以下信息回答用户问题:
            用户问题: {{#start.user_query#}}
            查询结果: {{#context#}}
            请用友好的语气回复。

      - id: "end"
        type: "end"
        data:
          output: "{{#final_response.text}}"

    edges:
      - source: "start"
        target: "intent_classifier"
      - source: "intent_classifier"
        target: "condition_router"
      - source: "order_lookup"
        target: "final_response"
      - source: "knowledge_retrieval"
        target: "final_response"
      - source: "final_response"
        target: "end"
```

```python
# Dify 平台 API 调用示例 (外部集成)
import requests

API_BASE = "https://your-dify-instance.com/v1"
API_KEY = "app-xxxxxxxxxxxx"

# 调用 Dify Agent 应用
response = requests.post(
    f"{API_BASE}/chat-messages",
    headers={
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json"
    },
    json={
        "inputs": {"user_query": "我的订单 #12345 到哪了?"},
        "query": "我的订单 #12345 到哪了?",
        "response_mode": "streaming",  # streaming | blocking
        "conversation_id": "",          # 留空开始新对话
        "user": "user-001",
    },
    stream=True
)

for line in response.iter_lines():
    if line:
        # 解析 SSE 事件流
        print(line.decode("utf-8"))
```

### 3.5 独特亮点

1. **零代码可视化编排**: 通过拖拽式 React Flow 编辑器构建 Agent 工作流。非技术用户可直接设计、测试、部署 Agent，将 Agent 开发从 "编程问题" 变为 "业务流程设计问题"。

2. **全栈 AI 应用平台**: Dify 不是单纯的 Agent 框架，而是一个完整的产品。它同时提供:
   - **RAG 管道**: 一键构建知识库 (文档上传→解析→切片→向量化→检索)
   - **Agent 策略**: 内置 ReAct / Function Calling / Multi-Agent 三种模式
   - **对话管理**: 自动会话持久化、历史管理、对话变量
   - **可观测性**: 内置日志、监控、成本追踪、标注反馈

3. **企业级开箱即用**: Docker Compose 一行命令部署，自带 RBAC 权限系统、工作空间隔离、API 密钥管理、用量统计、计费系统。10 分钟从零到生产。

4. **Plugin Marketplace**: 插件市场支持社区贡献的工具、模型提供商、扩展。商业模式清晰 (免费社区版 + 商业企业版)，保证可持续发展。

5. **多模型抽象层**: 统一接口封装 50+ 模型提供商 (包括国产模型: 通义千问、文心一言、GLM、DeepSeek 等)，模型切换只需下拉选择。

6. **声明式 DSL + 可视化编辑**: 工作流底层是声明式 YAML/JSON DSL，版本可控、可复用、可导出。同时提供可视化编辑器覆盖 99% 场景。

7. **对话变量与状态管理**: 在工作流中定义持久化对话变量 (类似 session state)，跨多轮对话保持状态，无需额外代码。

8. **监控与标注飞轮**: 内置:
   - 消息日志 (完整对话历史)
   - 用户反馈 (点赞/点踩)
   - 标注队列 (人工标注改进)
   - Token 用量与成本分析
   - 活跃用户与应用分析

### 3.6 适用性评分

| 维度 | 评分 (1-10) | 说明 |
|------|------------|------|
| 学习曲线 | 9/10 | 可视化操作, 非技术人员也能构建 Agent |
| 灵活性 | 6/10 | DSL 灵活但不及代码级, 插件扩展有边界 |
| 生产就绪 | 9/10 | 内置权限/监控/日志/多租户, 企业级开箱 |
| 社区生态 | 8/10 | 60k Stars, 活跃社区, 企业采用率高 |
| 多模型支持 | 9/10 | 50+ 模型, 国产模型支持领先 |
| 企业合规 | 9/10 | 自托管 + RBAC + 审计日志 + 数据隔离 |
| **综合** | **8.3/10** | **最佳产品化方案, 适合需要快速交付的团队** |

---

## 4. 综合对比与选型建议

### 4.1 三维对比矩阵

```
                     LangGraph                OpenAI Agents SDK        Dify
                    ──────────               ──────────────────       ──────
  定位              Agent 开发框架            Agent 开发 SDK              AI 应用平台
  开源性            完全开源 (MIT)            代码开源 (Apache 2.0)      开源 (Apache 2.0)
  模型绑定          无绑定, 100+ LLM          深度绑定 OpenAI             50+ 模型, 中立
  构建方式          代码 (Python/TS)           代码 (Python)              可视化 + DSL
  学习门槛          中等 (图编程)              低 (3 个原语)              极低 (拖拽)
  灵活性            ★★★★★                   ★★★                       ★★★
  生产就绪度        ★★★★                    ★★★                       ★★★★★
  多 Agent 支持     子图/条件路由               Handoff 机制              工作流并行/策略
  RAG 能力          需配合 LangChain          需自行构建                  内置完整 RAG 管道
  部署方式          自建 + LangGraph Cloud     纯自建                     Docker/自建/Cloud
  企业特性          依赖 LangSmith             基本无                     RBAC/审计/多租户
  成本              开源免费                    API 费用 (OpenAI)         开源免费 + 推理成本
```

### 4.2 选型决策树

```
                     需要构建 Agent?
                         │
            ┌────────────┼────────────┐
            │            │            │
        技术团队      混合团队      业务团队
        (全栈开发)    (有开发+产品)  (非技术为主)
            │            │            │
     需要极致灵活?   需要快速迭代?   需要开箱即用?
            │            │            │
       ┌────┴────┐  ┌───┴────┐  ┌───┴────┐
       │ 是      │  │ 是     │  │ 是     │
       │         │  │        │  │        │
    LangGraph  OpenAI    Dify     Dify
              Agents SDK         (推荐)
       │         │        │
       │   绑定 OpenAI   │
       │   无所谓?       │
       │    │           │
       │  OpenAI Agents │
       │    SDK         │
       │                │
   ┌───┴────┐           │
   │ 多模型  │           │
   │ 必须?   │           │
   │  │     │           │
   │ 是 否 (用 OpenAI)  │
   │  │     │           │
   LangGraph OpenAI     │
            Agents SDK  │
```

### 4.3 电商 RAG 场景推荐

针对本项目 (电商 RAG 智能客服)，推荐策略:

**方案 A (推荐): LangGraph + 自建 RAG**
- 用 LangGraph 构建客服 Agent (意图识别→条件路由→多步推理)
- RAG 部分可用 LangChain 或自建 Pipeline
- 优势: 完全掌控控制流，支持复杂的售后流程
- 适用: 3+ 人技术团队

**方案 B (快速验证): Dify**
- 可视化搭建客服工作流
- 一键构建商品知识库 (RAG)
- 优势: 1 天 MVP，PM 可直接参与
- 适用: 1-2 人团队或快速 POC

**方案 C (不推荐): OpenAI Agents SDK**
- 对 OpenAI 依赖过深，电商场景需多模型(国内合规)
- 无内置 RAG，需自建知识库
- 生产特性缺失 (无持久化/审计/多租户)
- 适用: 纯 OpenAI 技术栈的海外场景

---

## 附录: 参考资源

### LangGraph
- 官方文档: https://langchain-ai.github.io/langgraph/
- GitHub: https://github.com/langchain-ai/langgraph
- 教程: LangGraph Academy (免费在线课程)

### OpenAI Agents SDK
- 官方文档: https://openai.github.io/openai-agents-python/
- GitHub: https://github.com/openai/openai-agents-python
- 参考: OpenAI Cookbook - Agent 示例集

### Dify
- 官方文档: https://docs.dify.ai/
- GitHub: https://github.com/langgenius/dify
- 在线体验: https://cloud.dify.ai/

---

> 文档维护: Hermes 项目组
> 最后更新: 2026-05-19
> 版本: v1.0
