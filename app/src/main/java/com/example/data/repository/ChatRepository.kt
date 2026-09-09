package com.example.data.repository

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
  val syncManager: CloudSyncManager
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
    model: String = GeminiModel.FLASH.modelId,
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
    val conv = chatDao.getConversationById(conversationId) ?: return@withContext Result.failure(Exception("Conversation not found"))

    // 1. Save user message locally in SQLite (encrypted)
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

    // Update conversation timestamp
    val updatedConv = conv.copy(
      updatedAt = System.currentTimeMillis(),
      title = if (conv.title == "New Chat" || conv.title.isBlank()) content.take(32) else conv.title
    )
    chatDao.updateConversation(updatedConv)

    // 2. Fetch history for context
    val rawHistory = chatDao.getMessagesListForConversation(conversationId)
    val decryptedHistory = rawHistory.map {
      val text = if (it.isEncrypted) EncryptionManager.decrypt(it.content, passphrase) else it.content
      val role = if (it.role == "assistant" || it.role == "model") "model" else "user"
      Pair(role, text)
    }

    // Determine target model: If image is present, mandate Gemini 3.1 Pro as required by prompt!
    val effectiveModel = if (imageBase64 != null && selectedProvider == AiProvider.GEMINI) {
      GeminiModel.PRO.modelId
    } else {
      selectedModel
    }

    // 3. Query AI (Gemini or OpenRouter)
    val aiResult: Result<String> = if (selectedProvider == AiProvider.OPENROUTER && openRouterApiKey.isNotBlank()) {
      openRouterService.chatCompletions(
        apiKey = openRouterApiKey,
        model = effectiveModel,
        history = decryptedHistory,
        systemPrompt = persona.systemPrompt
      )
    } else {
      // Use Gemini API
      geminiService.generateContent(
        model = effectiveModel,
        history = decryptedHistory,
        systemInstruction = persona.systemPrompt,
        imageBase64 = imageBase64
      )
    }

    // 4. Save AI Response in SQLite (encrypted)
    val replyText = aiResult.getOrElse { err ->
      "⚠️ Unable to receive response: ${err.message}\n\nPlease check network or API keys in Settings."
    }

    val assistantMsgId = UUID.randomUUID().toString()
    val encryptedReply = EncryptionManager.encrypt(replyText, passphrase)
    val assistantMsg = MessageEntity(
      id = assistantMsgId,
      conversationId = conversationId,
      role = "assistant",
      content = encryptedReply,
      timestamp = System.currentTimeMillis(),
      modelUsed = effectiveModel,
      isEncrypted = true,
      authorName = when (selectedProvider) {
        AiProvider.GEMINI -> "Gemini AI"
        AiProvider.OPENROUTER -> "OpenRouter ($effectiveModel)"
      }
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

    // Create default project for team collaboration
    val teamProjectId = "project-core-ai"
    chatDao.insertProject(
      ProjectEntity(
        id = teamProjectId,
        name = "Enterprise AI Architecture",
        description = "Collaborative design workspace for offline-first SQLite sync & secure model orchestration.",
        currentUserRole = UserRole.OWNER.name,
        memberCount = 4,
        isEncrypted = true,
        remoteSyncUrl = "https://neon.tech/cloud/db/v1/sync"
      )
    )

    chatDao.insertProject(
      ProjectEntity(
        id = "project-mobile-client",
        name = "Cross-Platform Mobile App",
        description = "React Native & Android client integration with OpenRouter gateway.",
        currentUserRole = UserRole.ADMIN.name,
        memberCount = 2,
        isEncrypted = true,
        remoteSyncUrl = "https://supabase.co/rest/v1/sync"
      )
    )

    // Create welcome conversation
    val welcomeConvId = "seed-welcome-chat"
    chatDao.insertConversation(
      ConversationEntity(
        id = welcomeConvId,
        title = "Welcome to NovaMind",
        provider = AiProvider.GEMINI.name,
        model = GeminiModel.FLASH.modelId,
        personaId = PersonaRole.GENERAL.id,
        projectId = teamProjectId,
        isPinned = true
      )
    )

    val introText = "Welcome to NovaMind!\n\n" +
      "✨ **Key Capabilities**:\n" +
      "- 🔑 **OpenRouter & Gemini Support**: Use built-in Gemini models (`gemini-3.5-flash`, `gemini-3.1-flash-lite`, and `gemini-3.1-pro-preview`) or connect your OpenRouter API key.\n" +
      "- 💾 **Local SQLite Storage**: Built on native Room SQLite with offline access.\n" +
      "- ☁️ **Seamless Cloud Synchronization**: Syncs with Supabase or Neon with conflict-free offline queuing.\n" +
      "- 🔒 **End-to-End AES-256-GCM Encryption**: All chats and sensitive keys are cryptographically sealed.\n" +
      "- 📸 **Multimodal Image Understanding**: Tap the camera icon to upload and analyze photos using `gemini-3.1-pro-preview`.\n" +
      "- 👥 **Team Workspaces & Real-Time Collaboration**: Role management (Owner, Admin, Editor, Viewer) and live team presence.\n" +
      "- ♿ **Full Accessibility**: High-contrast mode and screen-reader support."

    val encryptedIntro = EncryptionManager.encrypt(introText, passphrase)

    chatDao.insertMessage(
      MessageEntity(
        id = UUID.randomUUID().toString(),
        conversationId = welcomeConvId,
        role = "assistant",
        content = encryptedIntro,
        timestamp = System.currentTimeMillis() - 60000,
        isEncrypted = true,
        authorName = "Gemini AI"
      )
    )
  }
}
