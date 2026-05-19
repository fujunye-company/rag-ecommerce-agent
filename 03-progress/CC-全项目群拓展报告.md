# Hermes 全项目群 CC 拓展报告

> 拓展时间：2026-05-19
> 模式：v3.0 Autonomous（从单项目 → 全项目群）

---

## 一、拓展内容

| 文件 | 位置 | 用途 |
|------|------|------|
| `CLAUDE.md` | `~/Desktop/Hermes/` | 根级全项目上下文（6 项目目录 + 环境 + 协作模式） |
| `.claude/settings.local.json` | `~/Desktop/Hermes/.claude/` | 全局权限配置（Read/Write/Edit/Bash/WebSearch 全开） |
| `hermes_cc.sh` | `~/Desktop/Hermes/` | 根级 CC 调用脚本（跨项目可用） |

---

## 二、覆盖项目

```
Desktop/Hermes/
├── CLAUDE.md                          ← ★ 新增：根级全项目上下文
├── .claude/settings.local.json        ← ★ 新增：全局权限
├── hermes_cc.sh                       ← ★ 新增：根级调用脚本
├── 01-数模校内赛/                      (归档，CLAUDE.md 可选)
├── 02-设计大赛/                        (可新增项目级 CLAUDE.md)
├── 03-SRP/                            ← ★ 已通过跨项目测试
├── 04-rag-ecommerce/                    (已有 CLAUDE.md，优先于根级)
├── 05-AIGC赛道/                        (可新增项目级 CLAUDE.md)
└── A1-音频转录管道/                    (可新增项目级 CLAUDE.md)
```

**优先级规则**：项目级 `CLAUDE.md` > 根级 `CLAUDE.md`（Claude Code 自动合并）

---

## 三、跨项目测试结果

| 测试项 | 方法 | 结果 |
|--------|------|------|
| 根级 CLAUDE.md 被读取 | CC 识别 6 个项目和协作模式 | ✅ |
| CC 写入 03-SRP/ 文件 | 创建 `_hermes_cc_test.md` | ✅ 文件真实存在 |
| CC 跨项目 Read 验证 | 读取刚创建的文件 | ✅ 内容匹配 |
| hermes_cc.sh 脚本 | `bash hermes_cc.sh "任务" --max-turns 8` | ✅ |

---

## 四、使用方式

```bash
# 跨项目任务——脚本自动 cd 到 Hermes 根目录
bash ~/Desktop/Hermes/hermes_cc.sh "在 03-SRP/ 下创建 README.md" --max-turns 10

# 带 WebSearch 的复杂任务
bash ~/Desktop/Hermes/hermes_cc.sh "搜索并总结..." --web --max-turns 15

# 指定项目目录
cd ~/Desktop/Hermes/04-rag-ecommerce && \
  claude -p "任务" --allowedTools "Read,Write,Edit,Bash" --max-turns 12
```

---

## 五、后续建议

| 项目 | 建议 |
|------|------|
| 03-SRP | 新增项目级 `CLAUDE.md`（生理信号处理/TouchDesigner 上下文） |
| 05-AIGC赛道 | 新增项目级 `CLAUDE.md`（Unity/Blender 上下文） |
| 各项目 | `git init` + `.gitignore`（如需要 CC 做版本控制） |
