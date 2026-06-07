package com.shopping.agent.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.shopping.agent.core.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * Voice recognition client. Uploads recorded audio to /api/v1/voice/recognize.
 */
class AudioClient(
    private val baseUrl: String = NetworkConfig.BASE_URL,
    private val gson: Gson = Gson(),
) {
    suspend fun transcribe(audioFile: File): String = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                audioFile.name,
                audioFile.asRequestBody("audio/mp4".toMediaType()),
            )
            .build()

        val request = Request.Builder()
            .url("$baseUrl/api/v1/voice/recognize")
            .post(body)
            .build()

        NetworkConfig.httpClient.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}: ${payload.ifBlank { response.message }}")
            }

            val root = gson.fromJson(payload, JsonObject::class.java)
            val data = root.getAsJsonObject("data")
            data?.get("text")?.asString?.trim().orEmpty()
        }
    }
}
