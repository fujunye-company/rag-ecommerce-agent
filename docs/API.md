# API 接口文档 — AI 全栈挑战赛

> Base URL: `http://localhost:8000/api`

---

## 对话接口

### POST /api/chat

SSE 流式对话。

**请求**：
```json
{
  "message": "推荐降噪耳机",
  "conversation_id": "uuid-optional"
}
```

**SSE 事件**：
```
event: text_delta
data: {"type":"text_delta","content":"您好..."}

event: product_cards
data: {"type":"product_cards","products":[{"id":"...","title":"...","price":2499,...}]}

event: done
data: {"type":"done"}
```

---

## 商品接口

### GET /api/products

商品列表（支持筛选/排序/分页）。

| 参数 | 类型 | 说明 |
|------|------|------|
| category | string | 品类过滤 |
| brand | string | 品牌过滤 |
| price_min | float | 最低价 |
| price_max | float | 最高价 |
| keyword | string | 标题搜索 |
| sort_by | string | price_asc/price_desc/rating/sales |
| page | int | 页码 (default 1) |
| size | int | 每页条数 (default 20, max 100) |

### GET /api/products/{id}

商品详情。

### POST /api/products

创建商品。

### PUT /api/products/{id}

更新商品。

### DELETE /api/products/{id}

删除商品。

---

## 购物车接口（场景6 — 待实现）

### GET /api/cart

获取购物车。

### POST /api/cart/items

添加商品到购物车。

### PUT /api/cart/items/{id}

修改购物车商品数量。

### DELETE /api/cart/items/{id}

删除购物车商品。

### POST /api/cart/checkout

模拟下单。

---

## 图片上传/搜索接口（场景7 — 加分项，待实现）

### POST /api/upload/image

上传图片进行商品识别。

### POST /api/image-search

基于图片理解的相似商品检索。

---

## 反馈接口

### POST /api/feedback

点赞/点踩。

```json
{
  "message_id": "uuid",
  "rating": 1,
  "reason": "推荐准确"
}
```

---

## 健康检查

### GET /health

```json
{"status":"ok","version":"0.1.0","database":"connected"}
```
