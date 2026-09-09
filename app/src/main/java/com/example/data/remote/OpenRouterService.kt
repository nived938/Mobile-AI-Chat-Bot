package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenRouterService {
  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun chatCompletions(
    apiKey: String,
    model: String,
    history: List<Pair<String, String>>, // role to message
    systemPrompt: String? = null
  ): Result<String> = withContext(Dispatchers.IO) {
    if (apiKey.isBlank()) {
      return@withContext Result.failure(Exception("OpenRouter API key is missing. Please add it in Settings > API Keys."))
    }

    try {
      val messagesArr = JSONArray()

      if (!systemPrompt.isNullOrBlank()) {
        val sysObj = JSONObject()
        sysObj.put("role", "system")
        sysObj.put("content", systemPrompt)
        messagesArr.put(sysObj)
      }

      for (turn in history) {
        val msgObj = JSONObject()
        val role = if (turn.first.equals("model", ignoreCase = true) || turn.first.equals("assistant", ignoreCase = true)) {
          "assistant"
        } else {
          "user"
        }
        msgObj.put("role", role)
        msgObj.put("content", turn.second)
        messagesArr.put(msgObj)
      }

      val rootJson = JSONObject()
      rootJson.put("model", model)
      rootJson.put("messages", messagesArr)
      rootJson.put("temperature", 0.7)

      val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
      val request = Request.Builder()
        .url("https://openrouter.ai/api/v1/chat/completions")
        .addHeader("Authorization", "Bearer $apiKey")
        .addHeader("HTTP-Referer", "https://aistudio.google.com")
        .addHeader("X-Title", "AI Chatbot")
        .post(requestBody)
        .build()

      client.newCall(request).execute().use { response ->
        val bodyStr = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          Log.w("OpenRouterService", "HTTP ${response.code}: $bodyStr")
          return@withContext Result.failure(Exception("OpenRouter error (${response.code}): $bodyStr"))
        }

        val resObj = JSONObject(bodyStr)
        val choices = resObj.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
          val firstChoice = choices.getJSONObject(0)
          val messageObj = firstChoice.optJSONObject("message")
          val content = messageObj?.optString("content")
          if (!content.isNullOrBlank()) {
            return@withContext Result.success(content)
          }
        }
        Result.failure(Exception("OpenRouter returned empty content."))
      }
    } catch (e: Exception) {
      Log.e("OpenRouterService", "Failed to query OpenRouter", e)
      Result.failure(e)
    }
  }

  suspend fun testApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
    if (apiKey.isBlank()) return@withContext false
    try {
      val request = Request.Builder()
        .url("https://openrouter.ai/api/v1/auth/key")
        .addHeader("Authorization", "Bearer $apiKey")
        .build()

      client.newCall(request).execute().use { response ->
        response.isSuccessful
      }
    } catch (e: Exception) {
      false
    }
  }
}
