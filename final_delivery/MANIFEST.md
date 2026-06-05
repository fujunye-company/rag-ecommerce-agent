# RAG 多模态电商智能导购 Agent 交付清单

交付日期：2026-06-05

## 目录结构

```text
final_delivery/
├── MANIFEST.md
├── apk/
│   └── app-debug.apk
├── source/
│   ├── backend/
│   └── android/
└── docs/
    ├── README.md
    ├── ANDROID_README.md
    ├── API.md
    ├── ARCHITECTURE.md
    ├── EVALUATION.md
    ├── CHANGELOG.md
    ├── DEMO_RUNBOOK.md
    ├── REQS.md
    ├── TTFT_BENCHMARK.md
    └── ttft_benchmark_20260605.json
```

## APK

- 文件：`final_delivery/apk/app-debug.apk`
- 大小：25,211,332 bytes
- SHA256：`1AE9A40961A20F632CD0862B81FDB920B3F9C3E4E4EAB98D4781737B6C3A5A72`
- 构建命令：`cd apps/android && .\gradlew.bat assembleDebug`
- 构建环境：JDK 17.0.18，`JAVA_HOME=C:\Program Files\Java\jdk-17`

## 源码范围

`final_delivery/source/backend` 来自 `apps/backend`，包含后端应用、脚本、测试和必要种子数据。

已排除：
- `.env`
- `.venv`
- `.pytest_cache`
- `__pycache__`
- `uploads`
- 本地模型权重与缓存：`data/models`、`data/hf_home`、`data/qdrant_storage`
- 临时日志、字节码和备份文件

`final_delivery/source/android` 来自 `apps/android`，包含 Android 工程源码和 Gradle 配置。

已排除：
- `.gradle`
- `build` / `app/build`
- `local.properties`
- `*.jks` / `*.keystore`
- `*.apk`

## 本轮验证摘要

- Android Debug 构建通过。
- 后端测试通过：`76 passed, 3 skipped`。
- 后端健康检查通过：PostgreSQL connected，Qdrant ok，`products` collection 共 290 条向量。
- 购物车到下单闭环已接通：购物车选中商品可进入确认页，下单后进入订单详情页。
- 立即购买已接入独立确认页：商品详情页点击立即购买进入 `CheckoutScreen`，提交后生成订单详情。
- SSE 首屏与严格首文本 token 复测通过：`first_event` / `first_text` 平均 47.2 ms、最大 65 ms，全部小于 1 秒。
- 推荐、排除、对比三个 RAG 场景基准均返回 3 张商品卡。

## 说明

`first_event` 和 `first_text` 均已满足小于 1 秒；完整基准数据见 `final_delivery/docs/TTFT_BENCHMARK.md` 与 `ttft_benchmark_20260605.json`。
