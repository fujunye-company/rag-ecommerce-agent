package com.shopping.agent.core.network

import com.shopping.agent.BuildConfig

/**
 * Network configuration shared by repositories and SSE clients.
 *
 * Android emulator: http://10.0.2.2:8080
 * Physical device over Wi-Fi: set API_BASE_URL to http://<LAN-IP>:8080
 * Physical device over adb reverse: set API_BASE_URL to http://127.0.0.1:8080
 */
object NetworkConfig {
    val BASE_URL: String = BuildConfig.API_BASE_URL
}
