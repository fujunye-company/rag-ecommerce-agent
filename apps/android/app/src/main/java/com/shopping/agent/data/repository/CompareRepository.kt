package com.shopping.agent.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CompareDimension(
    val name: String,
    val values: Map<String, String>,
    val winner: String?,
)

data class CompareResult(
    val dimensions: List<CompareDimension>,
    val summary: String,
    val productIds: List<String>,
)

class CompareRepository(
    private val baseUrl: String = "http://10.0.2.2:8080"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun compareProducts(productIds: List<String>): CompareResult? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("product_ids", JSONArray(productIds))
                put("dimensions", JSONArray())
            }

            val request = Request.Builder()
                .url("$baseUrl/api/products/compare")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: return@withContext null
                val obj = JSONObject(json)
                val dimsArray = obj.optJSONArray("dimensions") ?: JSONArray()
                val dimensions = mutableListOf<CompareDimension>()
                for (i in 0 until dimsArray.length()) {
                    val d = dimsArray.getJSONObject(i)
                    val valuesObj = d.optJSONObject("values") ?: JSONObject()
                    val values = mutableMapOf<String, String>()
                    for (key in valuesObj.keys()) {
                        values[key] = valuesObj.optString(key, "")
                    }
                    dimensions.add(CompareDimension(
                        name = d.optString("name", ""),
                        values = values,
                        winner = d.optString("winner", null),
                    ))
                }
                CompareResult(
                    dimensions = dimensions,
                    summary = obj.optString("summary", ""),
                    productIds = productIds,
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
