package com.example.data.local

import android.content.Context
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Fully on-device text AI for NovaMind.
 *
 * The model is downloaded once to the app's private storage and then runs locally
 * through llama.cpp. No Gemini/OpenRouter key is needed and chat text is not sent
 * to a remote AI service.
 */
class LocalLlmService(private val context: Context) {
  companion object {
    const val MODEL_NAME = "Qwen2.5 0.5B Instruct Q4_K_M"
    const val MODEL_FILE = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
    const val MODEL_URL = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf?download=true"

    private const val CONTEXT_SIZE = 4096
    private const val MAX_TOKENS = 512
  }

  private val client = OkHttpClient.Builder().build()
  private val modelMutex = Mutex()
  private var loadedModel: LlamaModel? = null

  fun modelFile(): File = File(context.getExternalFilesDir("models") ?: File(context.filesDir, "models"), MODEL_FILE)

  fun isModelInstalled(): Boolean = modelFile().let { it.exists() && it.length() > 100_000_000L }

  suspend fun ensureModel(): File {
    val file = modelFile()
    if (isModelInstalled()) return file

    file.parentFile?.mkdirs()
    val temp = File(file.parentFile, "$MODEL_FILE.part")

    val request = Request.Builder().url(MODEL_URL).build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw IllegalStateException("Model download failed: HTTP ${response.code}")
      }
      val body = response.body ?: throw IllegalStateException("Model download returned no data")
      body.byteStream().use { input ->
        FileOutputStream(temp).use { output ->
          input.copyTo(output, bufferSize = 1024 * 1024)
        }
      }
    }

    if (!temp.renameTo(file)) {
      temp.copyTo(file, overwrite = true)
      temp.delete()
    }
    return file
  }

  suspend fun generate(prompt: String, systemPrompt: String): String = modelMutex.withLock {
    val file = ensureModel()
    val model = loadedModel ?: Llama.loadModel(
      modelPath = file.absolutePath,
      config = LlamaConfig(
        contextSize = CONTEXT_SIZE,
        threads = maxOf(2, Runtime.getRuntime().availableProcessors() / 2),
        gpuLayers = 0,
        temperature = 0.7f,
        topP = 0.9f,
        topK = 40
      )
    ).also { loadedModel = it }

    Llama.complete(
      model = model,
      prompt = prompt,
      systemPrompt = systemPrompt,
      maxTokens = MAX_TOKENS
    ).text.trim()
  }

  fun release() {
    loadedModel?.let { Llama.releaseModel(it) }
    loadedModel = null
  }
}
