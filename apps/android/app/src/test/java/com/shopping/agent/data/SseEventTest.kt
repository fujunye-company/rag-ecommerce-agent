package com.shopping.agent.data

import com.shopping.agent.data.model.SseEvent
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SSE 事件解析测试 — 覆盖所有 type
 */
class SseEventTest {
    @Test
    fun `parse text_delta event`() {
        val event = SseEvent.fromJson("text_delta", mapOf("content" to "hello"))
        assertTrue(event is SseEvent.TextDelta)
    }

    @Test
    fun `parse done event`() {
        val event = SseEvent.fromJson("done", emptyMap())
        assertTrue(event is SseEvent.Done)
    }

    @Test
    fun `parse error event`() {
        val event = SseEvent.fromJson("error", mapOf("message" to "fail", "code" to "E01"))
        assertTrue(event is SseEvent.Error)
    }

    @Test
    fun `unknown type falls back to Unknown`() {
        val event = SseEvent.fromJson("future_type", mapOf("data" to "x"))
        assertTrue(event is SseEvent.Unknown)
    }
}
