package com.shopping.agent.core.network

/**
 * 网络配置 — 集中管理 BASE_URL，避免分散硬编码。
 *
 * Android 模拟器访问本机后端：10.0.2.2 → Windows 宿主机
 * 真机 Wi-Fi 调试：电脑局域网 IP
 * 真机 USB adb reverse：127.0.0.1
 */
object NetworkConfig {
    /** 模拟器: http://10.0.2.2:8080 */
    const val BASE_URL = "http://10.0.2.2:8080"
    // 真机 Wi-Fi: const val BASE_URL = "http://192.168.x.x:8080"
    // 真机 USB reverse: const val BASE_URL = "http://127.0.0.1:8080"
}
