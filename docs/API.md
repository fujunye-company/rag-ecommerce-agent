# API 接口文档

> 电商 AI 导购系统 — 后端接口说明

## 基础信息

- Base URL: `http://localhost:8000`
- 内容类型: `application/json`
- SSE: `text/event-stream`

## 响应格式

所有 JSON 响应统一包裹:
```json
{"code": 0, "data": {...}, "message": "ok"}
```

## MVP 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查 |
| GET | `/version` | 版本信息 |
| POST | `/api/chat` | 普通问答 |
| POST | `/api/chat/stream` | SSE 流式问答 |
| GET | `/api/products` | 商品列表 (?category=&page=&size=) |
| GET | `/api/products/{id}` | 商品详情 |
| POST | `/api/feedback` | 用户反馈 |
| POST | `/api/upload/image` | 图片上传 [预留] |

## SSE 事件

| type | 说明 |
|------|------|
| `text_delta` | 流式文本增量 |
| `product_cards` | 推荐商品卡片 |
| `done` | 流结束 |
| `error` | 错误 |

## 错误码

| code | 说明 |
|------|------|
| 4000 | 请求参数错误 |
| 4004 | 资源不存在 |
| 5001 | LLM 服务不可用 |
| 5002 | 检索服务不可用 |
| 5003 | 数据库异常 |
