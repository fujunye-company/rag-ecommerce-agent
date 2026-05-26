"""
图像理解 — VLM 商品图片结构化提取

仅使用本地 Qwen3-VL-2B 模型推理，无外部 API 依赖。
模型: Qwen/Qwen3-VL-2B-Instruct (~4GB, ModelScope 缓存)
"""
import asyncio
import base64
import json
import logging
import re
import uuid
from pathlib import Path

logger = logging.getLogger("image_parser")

# 上传图片存储目录
UPLOAD_DIR = Path(__file__).resolve().parents[3] / "uploads"
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

# 延迟加载
_local_model = None
_local_processor = None
_model_load_attempted = False


def _get_local_model():
    """延迟加载本地 Qwen3-VL-2B 模型（优先 ModelScope 缓存）"""
    global _local_model, _local_processor, _model_load_attempted
    if _local_model is not None:
        return _local_model, _local_processor
    if _model_load_attempted:
        # 上次加载失败且没经过重置，跳过重试
        return _local_model, _local_processor
    try:
        from transformers import AutoModelForImageTextToText, AutoProcessor
        model_id = "Qwen/Qwen3-VL-2B-Instruct"

        # 优先从 ModelScope 缓存加载
        modelscope_cache = Path.home() / ".cache" / "modelscope" / "qwen" / "Qwen3-VL-2B-Instruct"
        if modelscope_cache.exists():
            model_id = str(modelscope_cache)
            logger.info("Loading VLM from ModelScope cache: %s", model_id)

        logger.info("Loading local VLM: %s ...", model_id)
        _local_model = AutoModelForImageTextToText.from_pretrained(
            model_id, torch_dtype="auto", device_map="auto",
        )
        _local_processor = AutoProcessor.from_pretrained(model_id)
        _model_load_attempted = True  # 加载成功，缓存状态
        logger.info("Local VLM loaded: %s", model_id)
    except Exception as e:
        logger.error("Local VLM unavailable: %s", e)
        _local_model = None
        _local_processor = None
        _model_load_attempted = False  # 允许下次重试（而非永久缓存失败状态）
    return _local_model, _local_processor


async def parse_product_image(image_bytes: bytes) -> dict:
    """
    解析商品图片，提取结构化商品信息。

    使用本地 Qwen3-VL-2B 模型推理（在线程池中执行，不阻塞事件循环）。
    模型不可用时抛出 RuntimeError。

    Returns:
        {category, brand, color, material, style, keywords, description, confidence}
    """
    local_model, processor = _get_local_model()
    if local_model:
        return await asyncio.to_thread(_parse_with_local, image_bytes, local_model, processor)

    raise RuntimeError(
        "VLM 模型未就绪。请确保已下载 Qwen3-VL-2B-Instruct 到 "
        "~/.cache/modelscope/qwen/Qwen3-VL-2B-Instruct"
    )


def _parse_with_local(image_bytes, model, processor) -> dict:
    """使用本地 Qwen3-VL-2B 解析 — processor 原生处理 image URL（在 executor 线程中运行）"""
    img_b64 = base64.b64encode(image_bytes).decode("utf-8")
    data_url = f"data:image/jpeg;base64,{img_b64}"

    prompt = _build_prompt()

    messages = [{
        "role": "user",
        "content": [
            {"type": "image", "url": data_url},
            {"type": "text", "text": prompt},
        ],
    }]

    text = processor.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    inputs = processor(text=text, images=[data_url], return_tensors="pt").to(model.device)

    generated_ids = model.generate(**inputs, max_new_tokens=256)
    generated_ids_trimmed = [
        out_ids[len(in_ids):]
        for in_ids, out_ids in zip(inputs.input_ids, generated_ids)
    ]
    output_text = processor.batch_decode(
        generated_ids_trimmed, skip_special_tokens=True,
        clean_up_tokenization_spaces=False,
    )[0]

    return _parse_vlm_output(output_text)


def _build_prompt() -> str:
    return """你是一个电商商品图片分析助手。请分析这张商品图片，提取以下信息：

1. 品类(category)：具体品类，如"运动鞋""T恤""手机""耳机"
2. 品牌(brand)：品牌logo或文字
3. 颜色(color)：主要颜色
4. 材质(material)：材质特征
5. 风格(style)：设计风格
6. 关键词(keywords)：3-5个电商检索关键词
7. 描述(description)：一句话（15字以内）

只输出JSON：{"category": "...", "brand": "...", "color": "...", "material": "...", "style": "...", "keywords": [...], "description": "..."}
无法识别填 null。"""


def _parse_vlm_output(text: str) -> dict:
    """稳健解析 VLM JSON 输出"""
    default = {
        "category": None, "brand": None, "color": None,
        "material": None, "style": None, "keywords": [],
        "description": None, "confidence": 0.0,
    }

    text = text.strip()
    text = re.sub(r'^```(?:json)?\s*', '', text)
    text = re.sub(r'\s*```$', '', text)

    try:
        data = json.loads(text)
    except json.JSONDecodeError:
        m = re.search(r'\{[^{}]*\}', text, re.DOTALL)
        if m:
            try:
                data = json.loads(m.group())
            except json.JSONDecodeError:
                return default
        else:
            return default

    result = {**default}
    for k in default:
        if k in data and data[k] is not None:
            result[k] = data[k]

    if not isinstance(result["keywords"], list):
        result["keywords"] = [str(result["keywords"])]

    if not result["description"] and result["keywords"]:
        result["description"] = "，".join(str(k) for k in result["keywords"][:3])

    non_null = sum(1 for k in ["category", "brand", "color", "material", "style", "description"]
                   if result.get(k))
    result["confidence"] = round(non_null / 6, 2) if non_null > 0 else 0.0

    return result


async def parse_product_image_from_path(image_path: str) -> dict:
    """从文件路径解析（在线程池中读取文件，不阻塞事件循环）"""
    data = await asyncio.to_thread(lambda: Path(image_path).read_bytes())
    return await parse_product_image(data)


def save_upload_image(image_bytes: bytes, filename: str) -> str:
    """保存上传图片，返回文件路径"""
    ext = Path(filename).suffix or ".jpg"
    safe_name = f"{uuid.uuid4().hex}{ext}"
    filepath = UPLOAD_DIR / safe_name
    filepath.write_bytes(image_bytes)
    logger.info("Image saved: %s", filepath)
    return str(filepath)
