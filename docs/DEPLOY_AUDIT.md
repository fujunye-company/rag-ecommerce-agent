# 一键部署多角度审查报告

> 审查时间: 2026-06-02 | 审查范围: deploy.sh / deploy.ps1 / Dockerfile / docker-compose / startup / config

---

## 审查角度 & 发现问题

### 角度 1: 首次启动正确性（全新 clone 队友视角）

| # | 问题 | 严重度 | 影响 |
|---|------|:---:|------|
| A1 | **ML 模型未预置** — `data/models/bge-*` 是 gitignored 的（根 `.gitignore` 明确排除），队友 clone 后不存在。Docker 首次启动时 `resolve_model_path()` 降级为 HF 名称，触发下载：embedding 1.3GB + reranker 2GB ≈ 3.3GB。`healthcheck.start_period=300s` 可能不够。 | 🔴高 | 首次启动 10-20 分钟，超时后 Docker 判 unhealthy 重启循环 |
| A2 | **HF 下载无代理配置** — Docker 容器内未设置 `HF_ENDPOINT` 或代理环境变量，国内队友直接拉 HuggingFace 极慢或超时。 | 🔴高 | 模型下载失败 → RAG 检索不可用 → 全链路不可用 |
| A3 | **deploy.sh LAN IP 检测在 macOS/WSL 下返回空或错误值** — 第 150 行 `ip route get 1` 在 macOS 不存在（macOS 用 `route get`），WSL 下可能返回内部虚拟网卡 IP。 | 🟡中 | Android APK 构建命令中的 IP 错误，队友需手动纠正 |
| A4 | **种子数据只有 100 条** — `products_expanded_100.jsonl` 实际仅 100 行（不是文件名暗示的 "expanded" 290 条）。auto-import 正常完成但 `/ready` 显示 `qdrant_item_count=100`，与 CLAUDE.md 宣称的 "290条" 不符。 | 🟡中 | 数据量缩水，部分品类检索召回不足 |

### 角度 2: 异常路径与容错

| # | 问题 | 严重度 | 影响 |
|---|------|:---:|------|
| B1 | **Qdrant 不可达时静默跳过** — `startup.py:138` 在 `_wait_for_qdrant()` 超时后直接设 `phase="ready"`，deploy.sh 仅检查 status==ready，不会报 qdrant_item_count==0。 | 🔴高 | 部署报告显示 "ready" 但 RAG 检索实际为空，队友误认为成功 |
| B2 | **deploy.sh 不校验 `qdrant_item_count` — Phase 2 只检查 `status=="ready"`，Phase 3 解析 item_count 但**不做阈值判断。即使 count=0 也输出 "deploy complete"。 | 🔴高 | 同 B1 — 空向量库 + 报告成功 = 误导 |
| B3 | **`time.sleep()` 在 async 上下文中** — `startup.py:106` 的 `_wait_for_qdrant()` 使用同步 `time.sleep(6)`，被 `ensure_qdrant_data()` (async) 直接调用。虽然 lifespan 阶段不接请求，但仍阻塞事件循环最长 120s。 | 🟡中 | 不影响功能但不符合 asyncio 规范；如果未来 lifespan 中有其他并发任务会卡死 |
| B4 | **JSONL 文件不存在时静默跳过** — `startup.py:131-135` 同样直接设 `phase="ready"`。Dockerfile `COPY . .` 会把文件打入镜像，但 volume mount `../apps/backend/data:/app/data` 会**覆盖**容器内 `/app/data/`。如果宿主机 `data/qdrant/` 目录为空，JSONL 就找不到了。 | 🟡中 | 取决于 volume mount 行为：Docker volume 覆盖是 merge 还是 replace？Docker mount 是替换，所以宿主机没有的目录容器内也看不到 |

> ⚠️ **B4 重要**：`docker-compose.yml` 第 17 行 `../apps/backend/data:/app/data` 会**完全替换**容器内 `/app/data` 目录。但 Dockerfile `COPY . .` 将 JSONL 复制到 `/app/data/qdrant/products_expanded_100.jsonl`。宿主机 volume 挂载后，如果宿主机 `apps/backend/data/` 下没有 `qdrant/` 子目录，容器内也看不到 JSONL。  
> **验证**：宿主机有 `apps/backend/data/qdrant/` 且含 JSONL → ✅。队友 clone 后同样有（因为 JSONL 被 git 跟踪）→ ✅ 暂时安全。但逻辑脆弱：未来任何 data/ 下新增的文件都需要 git 跟踪或在 volume 中有。

### 角度 3: 容器化配置一致性

| # | 问题 | 严重度 | 影响 |
|---|------|:---:|------|
| C1 | **阿里云 pip 镜像硬编码** — Dockerfile 第 13 行 `mirrors.aliyun.com`，境外队友 pip install 极慢。 | 🟡中 | 境外队友 Docker build 超时或失败 |
| C2 | **`docker-compose.yml` 端口映射与 `.env.example` 不一致** — compose 映射 `8080:8080`，但 `.env.example` 中 `APP_PORT=8080`（未设置，走默认）。而本地 `.env` 中 `APP_PORT=8082`。这实际是一致的（compose 覆盖了 Dockerfile CMD），但 `uvicorn` 在 Dockerfile 里硬编码 `--port 8080`。 | 🟢低 | 暂无不一致，但配置分散在多处 |
| C3 | **Env file 路径依赖执行目录** — `deploy.sh` 和 `deploy.ps1` 都 `cd` 到脚本所在目录，`COMPOSE_FILE="infrastructure/docker-compose.yml"`。但 `docker compose -f` 的 `env_file: env/.env.docker` 是相对 compose file 位置解析的 → `infrastructure/env/.env.docker` ✅ 正确。 | 🟢低 | 当前路径正确 |

### 角度 4: 队友体验 (DX)

| # | 问题 | 严重度 | 影响 |
|---|------|:---:|------|
| D1 | **deploy.sh 要求 `python` 在 PATH** — Phase 0 的 secret scan 和 Phase 2/3 的 JSON 解析依赖 `python` 命令。Windows 下 `python` 可能不存在（只有 `python3` 或 `py`）。 | 🟡中 | deploy.sh 在 Git Bash/WSL 下可用，纯 MSYS2 可能失败 |
| D2 | **deploy.ps1 缺少 curl/java/android SDK 检查** — 不像 deploy.sh 那样有明确的 docker/port 检查，deploy.ps1 只检查了 Docker 和 API Key。 | 🟢低 | 队友首次运行时错误信息不够友好 |
| D3 | **API Key 提示不醒目** — 两个脚本都有 Warn 提示，但脚本会继续执行。后端启动后 LLM 调用全部失败，队友可能不知道是 Key 没配。 | 🟡中 | 队友排查耗时 |
| D4 | **首次启动无进度反馈** — Model 下载 / Qdrant 等待期间，deploy.sh 轮询 `/ready` 但 progress 只显示 "waiting for backend to start..."。无法区分"backend 还没起来"和"backend 在下载模型"。 | 🟢低 | 队友不明所以等待 10 分钟 |

### 角度 5: 安全

| # | 问题 | 严重度 | 影响 |
|---|------|:---:|------|
| E1 | **PG 端口 5433 暴露到宿主机** — docker-compose 映射 `5433:5432`，任意本机进程可直连数据库。 | 🟡中 | 本地开发方便，生产环境应移除 |
| E2 | **Secret scan 非阻塞** — `python scripts/secret_scan.py || true` 失败不停止部署。 | 🟢低 | 按设计如此，false positive 不应阻断 |
| E3 | **`.dockerignore` 正确排除了 `.env`** ✅ | — | 无问题 |

---

## 总结

| 严重度 | 数量 | 必须修 |
|:---:|:---:|:---:|
| 🔴 高 | 3 | A1, B1, B2 |
| 🟡 中 | 8 | A2, A3, A4, B3, B4, C1, D1, D3, E1 |
| 🟢 低 | 5 | C2, C3, D2, D4, E2 |
| ✅ 正确 | — | docker-compose 服务编排、.dockerignore 排除、env 模板链路、CORS 配置、健康检查逻辑 |

### 建议修复优先级

**P0 (阻断部署)**:
1. B1/B2 — deploy 脚本增加 `qdrant_item_count == 0` 警告/阻断
2. A1 — 在 `.env.docker.example` 增加 `HF_ENDPOINT=https://hf-mirror.com` 镜像配置

**P1 (影响体验)**:
3. A3 — deploy.sh LAN IP 检测兼容 macOS (`route get` fallback)
4. D3 — Key 未配置时 deploy 脚本询问是否继续（read -p / Read-Host）
5. B4 — 确保 volume mount 不覆盖关键文件（将 JSONL 复制逻辑移到 startup 中而非依赖 Docker COPY）

**P2 (锦上添花)**:
6. C1 — 境外镜像源配置（或通过环境变量切换）
7. B3 — `time.sleep` → `asyncio.sleep`
