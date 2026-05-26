"""上传接口 — 图片上传 + 视觉搜索"""
import logging
from fastapi import APIRouter, UploadFile, File, HTTPException
from fastapi.responses import StreamingResponse
from app.schemas.common import ApiResponse
from app.services.image_parser import (
    parse_product_image, save_upload_image,
)
from app.services.retriever import search_similar_products
from app.schemas.sse_events import ProductCardEvent, ErrorEvent, DoneEvent
import json

logger = logging.getLogger("upload")
router = APIRouter()


@router.post("/upload/image")
async def upload_image(file: UploadFile = File(...)):
    """图片上传 + 本地保存"""
    contents = await file.read()

    if len(contents) > 10 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="图片大小超过 10MB 限制")

    filepath = save_upload_image(contents, file.filename or "upload.jpg")

    return ApiResponse(
        data={
            "filename": file.filename,
            "content_type": file.content_type,
            "size_bytes": len(contents),
            "path": filepath,
        },
        message="上传成功"
    ).model_dump()


@router.post("/upload/vision-search")
async def vision_search(file: UploadFile = File(...)):
    """
    拍照找货：上传商品图片 → VLM 提取属性 → Qdrant 相似检索 → SSE 流式返回商品卡片

    这是「场景7：拍照找货」的后端入口。
    Android 端通过 multipart/form-data 上传图片，接收 SSE 流。
    """
    contents = await file.read()

    if len(contents) > 10 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="图片大小超过 10MB 限制")

    # 保存上传图片
    filepath = save_upload_image(contents, file.filename or "camera.jpg")
    logger.info("Vision search: image saved to %s (%d bytes)", filepath, len(contents))

    # Step 1: VLM 图像理解 → 商品结构化属性（失败时返回 ErrorEvent 流）
    try:
        product_info = await parse_product_image(contents)
        logger.info("Vision search: parsed product_info=%s", product_info)
    except RuntimeError as e:
        logger.error("VLM unavailable for vision search: %s", e)
        async def error_stream():
            yield ErrorEvent(
                code="VLM_UNAVAILABLE",
                message="视觉识别模型未就绪，请稍后重试"
            ).to_sse()
            yield DoneEvent(
                total_cards=0, latency_ms=0,
                message="模型加载中，请确保已下载 Qwen3-VL-2B-Instruct"
            ).to_sse()
        return StreamingResponse(
            error_stream(),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no"},
        )

    # Step 2: 用提取的属性构造搜索查询
    search_query = _build_search_query(product_info)

    # Step 3: Qdrant 相似商品检索
    try:
        candidates = await search_similar_products(
            query_text=search_query,
            top_k=8,
        )
    except Exception as e:
        logger.warning("Qdrant search failed, using fallback: %s", e)
        candidates = []

    # Step 4: SSE 流式返回商品卡片
    async def event_stream():
        # 先发送图片解析结果
        yield f"data: {json.dumps({'type': 'vision_parsed', 'product_info': product_info}, ensure_ascii=False)}\n\n"

        if not candidates:
            msg = ErrorEvent(
                code="NO_MATCH",
                message=f"未找到与「{product_info.get('description', '该商品')}」相似的商品，试试换个角度拍摄"
            ).to_sse()
            yield msg
            yield DoneEvent(
                total_cards=0,
                latency_ms=0,
                message=f"图片已识别：{product_info.get('description', '未知商品')}，但暂无匹配商品"
            ).to_sse()
            return

        # 流式输出商品卡片
        total = len(candidates)
        for i, product in enumerate(candidates):
            card = ProductCardEvent(
                product_id=product.get("product_id", ""),
                title=product.get("title", ""),
                price=product.get("price", 0),
                rating=product.get("rating", 0),
                match_score=product.get("match_score", product.get("score", 0.5)),
                highlights=product.get("highlights", []),
                image_url=product.get("image_url"),
                image_urls=product.get("image_urls", []),
                brand=product.get("brand"),
                category=product.get("category", ""),
                index=i + 1,
                total=total,
            )
            yield card.to_sse()

        yield DoneEvent(
            total_cards=total,
            latency_ms=0,
            message=f"根据图片「{product_info.get('description', '')}」找到 {total} 件相似商品"
        ).to_sse()

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        }
    )


def _build_search_query(product_info: dict) -> str:
    """将 VLM 结构化输出转为 Qdrant 检索查询文本（去重避免 token 重复）"""
    parts = []
    keywords = [str(k) for k in product_info.get("keywords", [])]
    seen = set()

    # 品类 + 品牌优先（最结构化）
    for key in ("category", "brand"):
        val = product_info.get(key)
        if val and str(val) not in seen:
            parts.append(str(val))
            seen.add(str(val))

    # 关键词去重
    for kw in keywords:
        if kw not in seen:
            parts.append(kw)
            seen.add(kw)

    query = " ".join(parts) if parts else "商品"
    logger.info("Vision search query: %s", query)
    return query


@router.post("/documents/upload")
async def upload_document(file: UploadFile = File(...)):
    """知识文档上传 [全量]"""
    return ApiResponse(
        data={"filename": file.filename, "status": "processing"},
        message="文档已接收，正在处理"
    ).model_dump()
