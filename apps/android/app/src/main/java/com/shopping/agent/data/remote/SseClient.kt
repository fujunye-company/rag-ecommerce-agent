package com.shopping.agent.data.remote

import com.google.gson.Gson
import com.shopping.agent.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 真实 SSE 客户端。
 *
 * connect(): POST /api/v1/chat (JSON → SSE 流)
 * connectVision(): POST /api/v1/upload/vision-search (multipart → SSE 流)
 */
class SseClient(
    private val baseUrl: String = "http://10.0.2.2:8000"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)  // VLM 推理需要 15-20s
        .build()

    private val gson = Gson()

    // ── 文本聊天 ──────────────────────────────────────────

    fun connect(message: String, conversationId: String? = null): Flow<SSEEvent> = flow {
        val json = JSONObject().apply {
            put("message", message)
            conversationId?.let { put("conversation_id", it) }
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/api/v1/chat")
            .post(requestBody)
            .header("Accept", "text/event-stream")
            .build()

        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            emit(SSEEvent.Error("HTTP ${response.code}: ${response.message}"))
            return@flow
        }

        parseStream(response, conversationId)
        response.close()
    }

    // ── 拍照找货 ──────────────────────────────────────────

    /**
     * 上传图片进行视觉搜索。
     * @param imageFile 图片文件（JPEG/PNG）
     * @return SSE 事件流: vision_parsed → product_cards → done
     */
    fun connectVision(imageFile: File): Flow<SSEEvent> = flow {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", imageFile.name,
                imageFile.asRequestBody("image/*".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("$baseUrl/api/v1/upload/vision-search")
            .post(requestBody)
            .header("Accept", "text/event-stream")
            .build()

        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            emit(SSEEvent.Error("HTTP ${response.code}: ${response.message}"))
            return@flow
        }

        parseStream(response = response, convId = null, isVision = true)
        response.close()
    }

    // ── SSE 流解析 ────────────────────────────────────────

    private suspend fun FlowCollector<SSEEvent>.parseStream(
        response: okhttp3.Response,
        convId: String?,
        isVision: Boolean = false
    ) {
        val source = response.body!!.source()
        var eventType = ""
        val dataBuilder = StringBuilder()

        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break

                when {
                    line.startsWith("event: ") ->
                        eventType = line.removePrefix("event: ").trim()

                    line.startsWith("data: ") ->
                        dataBuilder.append(line.removePrefix("data: "))

                    line.isEmpty() && dataBuilder.isNotEmpty() -> {
                        val jsonStr = dataBuilder.toString()
                        dataBuilder.clear()

                        // vision-search 用 data-only 格式，从 JSON type 推断事件类型
                        val effectiveType = if (isVision && eventType.isEmpty()) {
                            inferVisionEventType(jsonStr)
                        } else {
                            eventType
                        }

                        val event = parseSseEvent(effectiveType, jsonStr, convId)
                        if (event != null) emit(event)

                        if (eventType == "error" || eventType == "done") break
                    }
                }
            }
        } finally {
            response.close()
        }
    }

    private fun inferVisionEventType(json: String): String {
        return try {
            val node = gson.fromJson(json, com.google.gson.JsonObject::class.java)
            val type = node.get("type")?.asString ?: ""
            when {
                type == "vision_parsed" -> "progress"  // 映射为 progress 在 UI 显示
                type == "product_cards" -> "product_cards"
                type == "done" -> "done"
                type == "error" -> "error"
                else -> "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun parseSseEvent(type: String, json: String, convId: String?): SSEEvent? {
        return try {
            when (type) {
                "text_delta" -> {
                    val d = gson.fromJson(json, TextDeltaPayload::class.java)
                    SSEEvent.TextDelta(delta = d.content, content = d.content)
                }
                "product_cards" -> {
                    val d = gson.fromJson(json, ProductCardPayload::class.java)
                    SSEEvent.ProductCard(
                        productId = d.product_id,
                        title = d.title,
                        price = d.price,
                        rating = d.rating,
                        matchScore = d.match_score,
                        highlights = d.highlights ?: emptyList(),
                        imageUrl = d.image_url,
                        imageUrls = d.image_urls ?: emptyList(),
                        brand = d.brand,
                        category = d.category ?: "",
                        index = d.index,
                        total = d.total
                    )
                }
                "progress" -> {
                    // vision-search 的 vision_parsed 事件 — 显示 VLM 识别结果
                    val d = gson.fromJson(json, VisionParsedPayload::class.java)
                    val desc = d.product_info?.get("description") as? String ?: ""
                    val name = d.product_info?.get("product_name") as? String ?: ""
                    val msg = if (name.isNotEmpty()) "图片识别: $name" 
                              else if (desc.isNotEmpty()) "图片识别: $desc"
                              else "图片已识别，正在检索..."
                    SSEEvent.Progress(msg)
                }
                "done" -> {
                    val d = gson.fromJson(json, DonePayload::class.java)
                    SSEEvent.Done(
                        sessionId = convId ?: "",
                        totalCards = d.total_cards,
                        latencyMs = d.latency_ms
                    )
                }
                "error" -> {
                    val d = gson.fromJson(json, ErrorPayload::class.java)
                    SSEEvent.Error(d.message)
                }
                else -> null  // 忽略未知事件
            }
        } catch (e: Exception) {
            SSEEvent.Error("SSE parse error [${type}]: ${e.message}")
        }
    }
}

// vision-search 特殊载荷
data class VisionParsedPayload(
    val type: String?,
    val product_info: Map<String, Any?>?
)
