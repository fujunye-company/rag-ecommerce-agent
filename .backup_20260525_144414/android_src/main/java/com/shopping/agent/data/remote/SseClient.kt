package com.shopping.agent.data.remote

import com.shopping.agent.data.model.SseEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * SSE 客户端 — OkHttp SSE 连接 + Flow 事件分发 + 自动重连
 */
class SseClient(
    private val baseUrl: String = "http://10.0.2.2:8000"  // Android emulator → host
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)  // SSE 无限读取
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    fun connect(message: String, conversationId: String? = null): Flow<SseEvent> = callbackFlow {
        val body = JSONObject().apply {
            put("message", message)
            if (conversationId != null) put("conversation_id", conversationId)
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("$baseUrl/api/chat")
            .post(body)
            .header("Accept", "text/event-stream")
            .build()

        val call = client.newCall(request)
        val response = call.execute()

        if (!response.isSuccessful) {
            trySend(SseEvent.Error("HTTP ${response.code}", "HTTP_ERROR"))
            close()
            return@callbackFlow
        }

        val reader = BufferedReader(InputStreamReader(response.body?.byteStream() ?: return@callbackFlow))
        var currentEvent = ""
        var currentData = ""

        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: break
                when {
                    l.startsWith("event:") -> currentEvent = l.removePrefix("event:").trim()
                    l.startsWith("data:") -> currentData = l.removePrefix("data:").trim()
                    l.isEmpty() && currentData.isNotEmpty() -> {
                        val event = SseEvent.fromJson(currentEvent, currentData)
                        trySend(event)
                        if (event is SseEvent.Done || event is SseEvent.Error) {
                            break
                        }
                        currentEvent = ""
                        currentData = ""
                    }
                }
            }
        } catch (e: Exception) {
            trySend(SseEvent.Error(e.message ?: "连接中断", "SSE_ERROR"))
        } finally {
            reader.close()
            response.close()
        }

        close()
        awaitClose()
    }
}
