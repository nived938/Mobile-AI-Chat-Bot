package com.example.data.repository

import com.example.data.local.LocalLlmService
import com.example.data.local.dao.ChatDao
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.AiProvider
import com.example.data.model.GeminiModel
import com.example.data.model.PersonaRole
import com.example.data.model.UserRole
import com.example.data.remote.GeminiService
import com.example.data.remote.OpenRouterService
import com.example.data.security.EncryptionManager
import com.example.data.sync.CloudSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
  private val chatDao: ChatDao,
  private val geminiService: GeminiService,
  private val openRouterService: OpenRouterService,
  val syncManager: CloudSyncManager,
  private val localLlmService: LocalLlmService
) {
  val conversations: Flow<List<ConversationEntity>> = chatDao.getAllConversations()
  val projects: Flow<List<ProjectEntity>> = chatDao.getAllProjects()

  fun getMessages(conversationId: String, passphrase: String): Flow<List<MessageEntity>> {
    return chatDao.getMessagesForConversation(conversationId).map { messages ->
      messages.map { msg ->
        if (msg.isEncrypted) {
          msg.copy(content = EncryptionManager.decrypt(msg.content, passphrase))
        } else {
          msg
        }
      }
    }
  }

  suspend fun createConversation(
    title: String,
    provider: AiProvider = AiProvider.GEMINI,
    model: String = "local-qwen2.5-0.5b-q4km",
    personaId: String = PersonaRole.GENERAL.id,
    projectId: String? = null
  ): String = withContext(Dispatchers.IO) {
    val id = UUID.randomUUID().toString()
    val conv = ConversationEntity(
      id = id,
      title = title,
      provider = provider.name,
      model = model,
      personaId = personaId,
      projectId = projectId
    )
    chatDao.insertConversation(conv)
    syncManager.queueSyncOperation("CONVERSATION", id, "INSERT", "{\"title\":\"$title\"}")
    id
  }

  suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {
    chatDao.deleteMessagesForConversation(id)
    chatDao.deleteConversation(id)
    syncManager.queueSyncOperation("CONVERSATION", id, "DELETE", "{}")
  }

  suspend fun sendMessage(
    conversationId: String,
    content: String,
    imageBase64: String? = null,
    passphrase: String,
    openRouterApiKey: String,
    selectedProvider: AiProvider,
    selectedModel: String,
    persona: PersonaRole,
    replyToId: String? = null,
    replyPreview: String? = null
  ): Result<String> = withContext(Dispatchers.IO) {
    val conv = chatDao.getConversationById(conversationId)
      ?: return@withContext Result.failure(Exception("Conversation not found"))

    val userMsgId = UUID.randomUUID().toString()
    val encryptedUserContent = EncryptionManager.encrypt(content, passphrase)
    val userMsg = MessageEntity(
      id = userMsgId,
      conversationId = conversationId,
      role = "user",
      content = encryptedUserContent,
      timestamp = System.currentTimeMillis(),
      imageBase64 = imageBase64,
      isEncrypted = true,
      authorName = "You",
      replyToId = replyToId,
      replyPreview = replyPreview
    )
    chatDao.insertMessage(userMsg)
    syncManager.queueSyncOperation("MESSAGE", userMsgId, "INSERT", "{\"role\":\"user\"}")

    val updatedConv = conv.copy(
      updatedAt = System.currentTimeMillis(),
      title = if (conv.title == "New Chat" || conv.title.isBlank()) content.take(32) else conv.title
    )
    chatDao.updateConversation(updatedConv)

    // Keep enough recent history for a small mobile model. The actual model runs locally.
    val rawHistory = chatDao.getMessagesListForConversation(conversationId)
    val decryptedHistory = rawHistory.map {
      val text = if (it.isEncrypted) EncryptionManager.decrypt(it.content, passphrase) else it.content
      val role = if (it.role == "assistant" || it.role == "model") "assistant" else "user"
      role to text
    }.takeLast(18)

    val prompt = buildString {
      decryptedHistory.dropLast(1).forEach { (role, text) ->
        append(if (role == "assistant") "Assistant: " else "User: ")
        append(text)
        append("\n")
      }
      append("User: ")
      append(content)
      append("\nAssistant:")
    }

    val aiResult = try {
      if (imageBase64 != null) {
        Result.failure<String>(
          UnsupportedOperationException(
            "Local text model cannot inspect images yet. The photo is stored locally; ask a text question or use the image edit command."
          )
        )
      } else {
        Result.success(localLlmService.generate(prompt, persona.systemPrompt))
      }
    } catch (error: Throwable) {
      Result.failure<String>(error)
    }

    val replyText = aiResult.getOrElse { err ->
      "⚠️ ${err.message ?: "Unable to generate a local response."}"
    }

    val assistantMsgId = UUID.randomUUID().toString()
    val encryptedReply = EncryptionManager.encrypt(replyText, passphrase)
    val assistantMsg = MessageEntity(
      id = assistantMsgId,
      conversationId = conversationId,
      role = "assistant",
      content = encryptedReply,
      timestamp = System.currentTimeMillis(),
      modelUsed = "Qwen2.5 0.5B Instruct Q4_K_M (on-device)",
      isEncrypted = true,
      authorName = "NovaMind Local AI"
    )
    chatDao.insertMessage(assistantMsg)
    syncManager.queueSyncOperation("MESSAGE", assistantMsgId, "INSERT", "{\"role\":\"assistant\"}")

    Result.success(replyText)
  }

  suspend fun toggleBookmark(messageId: String, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
    chatDao.setMessageBookmarked(messageId, isBookmarked)
  }

  suspend fun createProject(name: String, description: String, role: UserRole = UserRole.OWNER): String = withContext(Dispatchers.IO) {
    val id = UUID.randomUUID().toString()
    val project = ProjectEntity(
      id = id,
      name = name,
      description = description,
      currentUserRole = role.name,
      memberCount = 3,
      isEncrypted = true
    )
    chatDao.insertProject(project)
    syncManager.queueSyncOperation("PROJECT", id, "INSERT", "{\"name\":\"$name\"}")
    id
  }

  suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
    chatDao.deleteProject(projectId)
    syncManager.queueSyncOperation("PROJECT", projectId, "DELETE", "{}")
  }

  suspend fun seedInitialDataIfEmpty(passphrase: String) = withContext(Dispatchers.IO) {
    val existing = chatDao.getMessagesListForConversation("seed-welcome-chat")
    if (existing.isNotEmpty()) return@withContext

    val teamProjectId = "project-core-ai"
    chatDao.insertProject(
      ProjectEntity(
        id = teamProjectId,
        name = "Enterprise AI Architecture",
        description = "On-device AI workspace with local SQLite storage and optional cloud sync.",
        currentUserRole = UserRole.OWNER.name,
        memberCount = 1,
        isEncrypted = true,
        remoteSyncUrl = null
      )
    )

    chatDao.insertProject(
      ProjectEntity(
        id = "project-mobile-client",
        name = "Mobile AI App",
        description = "Private Android AI client powered by a local GGUF model.",
        currentUserRole = UserRole.ADMIN.name,
        memberCount = 1,
        isEncrypted = true,
        remoteSyncUrl = null
      )
    )

    val welcomeConvId = "seed-welcome-chat"
    chatDao.insertConversation(
      ConversationEntity(
        id = welcomeConvId,
        title = "Welcome to NovaMind",
        provider = AiProvider.GEMINI.name,
        model = "local-qwen2.5-0.5b-q4km",
        personaId = PersonaRole.GENERAL.id,
        projectId = teamProjectId,
        isPinned = true
      )
    )

    val introText = "Welcome to NovaMind!\n\n" +
      "🤖 **Private on-device AI**: Chat runs locally on your phone with a compact Qwen2.5 GGUF model.\n" +
      "🔐 **No API key**: Gemini and OpenRouter keys are not required for chat.\n" +
      "💾 **Local SQLite storage**: Conversations stay on the device.\n" +
      "🎨 **Image generation**: Type a request such as `generate an image of a cyberpunk city`.\n" +
      "🖼️ **Image editing**: Attach an image and say what you want changed or removed.\n" +
      "📱 **Mobile-first UI**: Designed for touch screens and phone-sized layouts."

    val encryptedIntro = EncryptionManager.encrypt(introText, passphrase)

    chatDao.insertMessage(
      MessageEntity(
        id = UUID.randomUUID().toString(),
        conversationId = welcomeConvId,
        role = "assistant",
        content = encryptedIntro,
        timestamp = System.currentTimeMillis() - 60000,
        isEncrypted = true,
        authorName = "NovaMind Local AI"
      )
    )
  }
}
