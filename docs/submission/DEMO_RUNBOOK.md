# 3-5 分钟演示 Runbook

## 一键启动

**Linux/WSL/macOS:**
```bash
./deploy.sh
```

**Windows PowerShell:**
```powershell
.\deploy.ps1
```

脚本自动完成：环境检查 → Docker 启动 → 等待后端就绪 → 数据自动入库 → 打印就绪报告。

**停止：**
```bash
./deploy.sh --stop    # Linux
.\deploy.ps1 -Stop    # Windows
```

## 手动启动（备选）

1. `docker compose -f infrastructure/docker-compose.yml up -d`
2. 等待 `http://localhost:8080/ready` 返回 `phase: ready`
3. 编译 Android App：`cd apps/android && ./gradlew assembleDebug`（默认连接 `http://10.0.2.2:8080`，真机需 `-PapiUrl=http://<IP>:8080`）。
4. 访问 `http://localhost:8080/health`，状态应为 `ok`。

## 演示路径

1. 基础推荐：输入“推荐一款 300 元以内的降噪耳机”，展示流式文本和商品卡片。
2. 主动追问：输入“推荐手机”，展示澄清问题和可点击选项。
3. 多轮细化：接着输入“拍照优先，预算 4000”，展示上下文继承。
4. 反选约束：输入“推荐防晒霜，不要含酒精的，也不要日系品牌”，展示排除结果。
5. 购物车闭环：输入“把第一个加入购物车”→“数量改成 2”→“下单”→“确认下单”。
6. 拍照找货：点击相机上传商品图，展示视觉识别状态和相似商品卡片。

## 提交前安全检查

运行 `python scripts/secret_scan.py`，不得输出真实 API Key、token 或 secret。
