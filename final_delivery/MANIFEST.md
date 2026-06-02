# 拾物 — 比赛交付清单

> AI 全栈挑战赛 第3届 · 华南理工大学 2026
> 交付日期：2026-05-28

---

## 目录结构

```
final_delivery/
├── MANIFEST.md          ← 本文件
├── apk/
│   └── app-debug.apk    ← Android 安装包（编译后放入）
├── source/
│   ├── backend/         ← 复制自 apps/backend/
│   └── android/         ← 复制自 apps/android/
└── docs/
    ├── REQS.md          ← 竞赛核心需求（已对齐）
    ├── ARCHITECTURE.md  ← 系统架构说明
    ├── DEMO-SCRIPT.md   ← 3-5 分钟演示脚本
    ├── PERFORMANCE.md   ← 性能基准报告
    ├── EVALUATION.md    ← RAGAS 评测报告（待运行）
    └── CHANGELOG.md     ← 变更日志
```

---

## 打包步骤

```bash
# 1. 编译 Android APK
cd apps/android && ./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk ../../final_delivery/apk/

# 2. 复制源码
cp -r apps/backend final_delivery/source/
cp -r apps/android final_delivery/source/

# 3. 复制文档
cp docs/background/REQS-竞赛核心需求.md final_delivery/docs/REQS.md
cp docs/notes/DEMO-SCRIPT.md final_delivery/docs/
cp docs/notes/PERFORMANCE.md final_delivery/docs/
cp docs/CHANGELOG.md final_delivery/docs/

# 4. 清理敏感信息
rm -f final_delivery/source/backend/.env
```

---

## 交付物检查

- [x] APK 可安装运行（24.1MB debug APK 编译通过）
- [x] 源码不含 .env / API Key（Git 历史已清理，文档已脱敏）
- [x] README 含使用说明（SETUP.md + README.md 覆盖前后端部署）
- [x] 文档齐全（架构/性能/演示脚本/DATA-CONTRACT/CHANGELOG）
- [x] 9 场景 ≥ 5 可演示（8/9 全栈完成，S7 场景推荐后端就绪）
