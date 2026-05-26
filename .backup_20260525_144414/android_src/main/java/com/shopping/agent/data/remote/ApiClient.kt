package com.shopping.agent.data.remote

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * HTTP 客户端 — OkHttp, base URL, timeout
 */
object ApiClient {
    const val BASE_URL = "http://10.0.2.2:8000"  // Android 模拟器 → 宿主机

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
}
