package com.shopping.agent.data.remote

import com.shopping.agent.data.model.SseEvent
import kotlinx.coroutines.flow.Flow

/**
 * SSE 客户端 — connect, event 分发, 重连
 * 遵循开发规约 v2.0 §14.2
 */
class SseClient {
    // TODO: SSE 连接和事件流
    fun connect(message: String): Flow<SseEvent> {
        // 占位: 返回空流
        return kotlinx.coroutines.flow.emptyFlow()
    }
}
