# Hermes ↔ Claude Code Autonomous 协同工作流 v3.0

> 替代: `Hermes_ClaudeCode_只读分析工作流.md`（旧 v2.0 只读模式已废弃）
> 基于: [Hermes Agent Claude Code SKILL.md v2.2.0](https://github.com/NousResearch/hermes-agent/blob/main/skills/autonomous-ai-agents/claude-code/SKILL.md)

## 0. 模式变更

旧模式（v2.0 已废弃）：

```
Claude Code 只读分析 → Hermes 执行修改 → Hermes 验证
```

新模式（v3.0）：

```
Hermes 分发任务 → Claude Code 自主处理 → Claude Code 返回报告
```

Claude Code 现在可以 Read / Write / Edit / Bash / WebSearch，全自主完成任务。

---

## 1. 角色分工

### 1.1 Hermes 的角色

```
1. 需求分析与任务拆解
2. 调用 Claude Code（-p 模式或交互模式）
3. 接收 Claude Code 完成报告
4. 成果整合、校验、交付
5. 经验沉淀与技能生成
```

### 1.2 Claude Code 的角色

```
1. 读取项目文件理解上下文
2. 搜索和分析代码
3. 执行代码修改（Write / Edit）
4. 运行测试和验证命令（Bash）
5. 搜索外部技术资料（WebSearch / WebFetch）
6. 返回结构化完成报告
```

---

## 2. Hermes 调用 Claude Code 的方式

### 2.1 Print 模式（推荐，一次性任务）

```bash
claude -p "修复 backend/app/api/chat.py 中 SSE 流式返回的首字延迟问题" \
  --allowedTools "Read,Edit,Write,Bash" \
  --max-turns 10 \
  --output-format json
```

### 2.2 交互模式（多轮迭代任务）

```bash
# 由 Hermes 通过 tmux 管理
tmux new-session -d -s task-xxx -x 140 -y 40
tmux send-keys -t task-xxx 'cd /path/to/project && claude' Enter
sleep 5 && tmux send-keys -t task-xxx Enter  # 信任对话框
tmux send-keys -t task-xxx '具体的编码任务描述' Enter
```

### 2.3 管道输入模式

```bash
git diff HEAD~3 | claude -p "审查这些变更" --max-turns 5
```

---

## 3. 任务分发格式

Hermes 分发给 Claude Code 的任务应包含：

```text
1. 任务 ID 与名称
2. 任务目标（明确要修改什么、为什么）
3. 涉及范围（文件或模块路径）
4. 约束条件（不修改特定文件、保持 API 兼容等）
5. 预期产出（修改后的文件、测试通过、报告）
```

---

## 4. Claude Code 返回报告格式

任务完成后，Claude Code 输出以下格式的报告：

```markdown
# 任务完成报告

## 任务信息
- 任务 ID：
- 任务名称：
- 执行模式：print / interactive
- 消耗 Token / 耗时：

## 实际修改文件
| 文件 | 修改类型 | 说明 |
|------|---------|------|

## 修改摘要
1.
2.

## 验证结果
| 验证项 | 方法 | 结果 |
|--------|------|------|

## 仍存在的问题
1.

## 下一步建议
1.
```

---

## 5. 项目配置

CLAUDE.md 位于项目根目录，包含：
- 技术栈与项目结构
- 关键命令（启动/测试/数据库）
- API 接口列表
- 代码规范
- SSE 事件类型

设置文件 `.claude/settings.local.json` 已配置：
- Read / Write / Edit / Bash 权限
- WebSearch / WebFetch 权限
- 危险命令拦截（rm -rf / 等）

---

## 6. 执行闭环

```
用户需求
↓
Hermes 拆解任务
↓
Hermes 调用 Claude Code（-p / 交互模式）
↓
Claude Code 自主分析、编码、测试
↓
Claude Code 返回结构化完成报告
↓
Hermes 审查报告和实际变更
↓
Hermes 交付给用户 / 进入下一任务
```

---

> 版本: v3.0
> 替代文件: `Hermes_ClaudeCode_只读分析工作流.md`
> 参考: [NousResearch/hermes-agent SKILL.md](https://github.com/NousResearch/hermes-agent/blob/main/skills/autonomous-ai-agents/claude-code/SKILL.md)
