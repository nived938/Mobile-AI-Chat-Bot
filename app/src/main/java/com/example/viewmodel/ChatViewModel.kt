package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.AiProvider
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.GeminiModel
import com.example.data.model.OpenRouterModel
import com.example.data.model.PersonaRole
import com.example.data.model.SyncStatus
import com.example.data.model.UserRole
import com.example.data.remote.GeminiService
import com.example.data.remote.OpenRouterService
import com.example.data.repository.ChatRepository
import com.example.data.security.EncryptionManager
import com.example.data.sync.CloudSyncManager
import com.example.data.sync.SyncLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

data class ChatUiState(
  val currentConversationId: String? = null,
  val currentConversationTitle: String = "AI Chatbot",
  val provider: AiProvider = AiProvider.GEMINI,
  val geminiModel: GeminiModel = GeminiModel.FLASH,
  val openRouterModel: OpenRouterModel = OpenRouterModel.CLAUDE_35_SONNET,
  val openRouterCustomModel: String = "",
  val persona: PersonaRole = PersonaRole.GENERAL,
  val customSystemPrompt: String = "",
  val openRouterApiKey: String = "",
  val isOpenRouterKeyValid: Boolean? = null,
  val isTestingKey: Boolean = false,
  val encryptionPassphrase: String = "omni_vault_device_master_key_secure_2025",
  val isE2eEncrypted: Boolean = true,
  val themeMode: AppThemeMode = AppThemeMode.OLED_MIDNIGHT,
  val language: AppLanguage = AppLanguage.EN,
  val isHighContrast: Boolean = false,
  val fontScaleFactor: Float = 1.0f,
  val isGenerating: Boolean = false,
  val attachedImageBase64: String? = null,
  val attachedImageUri: Uri? = null,
  val replyingToMessage: MessageEntity? = null,
  val activeProjectId: String? = null,
  val currentRole: UserRole = UserRole.OWNER,
  val simulatedTeammateTyping: String? = null,
  val statusMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
  private val prefs = application.getSharedPreferences("ai_chat_prefs", Context.MODE_PRIVATE)

  private val database = AppDatabase.getDatabase(application)
  private val chatDao = database.chatDao()
  private val geminiService = GeminiService()
  private val openRouterService = OpenRouterService()
  private val syncManager = CloudSyncManager(chatDao)
  val repository = ChatRepository(chatDao, geminiService, openRouterService, syncManager)

  private val _uiState = MutableStateFlow(ChatUiState())
  val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

  val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val projects: StateFlow<List<ProjectEntity>> = repository.projects
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _currentMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
  val currentMessages: StateFlow<List<MessageEntity>> = _currentMessages.asStateFlow()

  val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus
  val syncLogs: StateFlow<List<SyncLog>> = syncManager.syncLogs
  val isOfflineMode: StateFlow<Boolean> = syncManager.isOfflineMode

  private var messageCollectionJob: Job? = null

  init {
    loadSettingsFromPrefs()
    viewModelScope.launch {
      repository.seedInitialDataIfEmpty(_uiState.value.encryptionPassphrase)
      // Select the first conversation if available
      conversations.collect { list ->
        if (_uiState.value.currentConversationId == null && list.isNotEmpty()) {
          selectConversation(list.first().id)
        }
      }
    }
  }

  private fun loadSettingsFromPrefs() {
    val savedKey = prefs.getString("openrouter_key", "") ?: ""
    val savedTheme = prefs.getString("theme_mode", AppThemeMode.OLED_MIDNIGHT.name)
    val savedLang = prefs.getString("app_language", AppLanguage.EN.name)
    val savedPassphrase = prefs.getString("enc_passphrase", "omni_vault_device_master_key_secure_2025") ?: "omni_vault_device_master_key_secure_2025"
    val savedHighContrast = prefs.getBoolean("high_contrast", false)

    _uiState.value = _uiState.value.copy(
      openRouterApiKey = savedKey,
      themeMode = try { AppThemeMode.valueOf(savedTheme ?: "") } catch (e: Exception) { AppThemeMode.OLED_MIDNIGHT },
      language = try { AppLanguage.valueOf(savedLang ?: "") } catch (e: Exception) { AppLanguage.EN },
      encryptionPassphrase = savedPassphrase,
      isHighContrast = savedHighContrast
    )
  }

  fun selectConversation(conversationId: String) {
    _uiState.value = _uiState.value.copy(
      currentConversationId = conversationId,
      replyingToMessage = null,
      attachedImageBase64 = null,
      attachedImageUri = null
    )

    messageCollectionJob?.cancel()
    messageCollectionJob = viewModelScope.launch {
      repository.getMessages(conversationId, _uiState.value.encryptionPassphrase).collect { msgs ->
        _currentMessages.value = msgs
      }
    }

    viewModelScope.launch {
      val conv = chatDao.getConversationById(conversationId)
      if (conv != null) {
        _uiState.value = _uiState.value.copy(
          currentConversationTitle = conv.title,
          activeProjectId = conv.projectId
        )
      }
    }
  }

  fun createNewConversation(title: String = "New Chat") {
    viewModelScope.launch {
      val activeProject = _uiState.value.activeProjectId
      val id = repository.createConversation(
        title = title,
        provider = _uiState.value.provider,
        model = if (_uiState.value.provider == AiProvider.GEMINI) _uiState.value.geminiModel.modelId else _uiState.value.openRouterModel.modelId,
        personaId = _uiState.value.persona.id,
        projectId = activeProject
      )
      selectConversation(id)
    }
  }

  fun deleteConversation(id: String) {
    viewModelScope.launch {
      repository.deleteConversation(id)
      if (_uiState.value.currentConversationId == id) {
        val remaining = conversations.value.filter { it.id != id }
        if (remaining.isNotEmpty()) {
          selectConversation(remaining.first().id)
        } else {
          _uiState.value = _uiState.value.copy(currentConversationId = null, currentConversationTitle = "AI Chatbot")
          _currentMessages.value = emptyList()
        }
      }
    }
  }

  fun sendMessage(userText: String) {
    val convId = _uiState.value.currentConversationId ?: return
    if (userText.isBlank() && _uiState.value.attachedImageBase64 == null) return

    val currentReplying = _uiState.value.replyingToMessage
    val currentImage = _uiState.value.attachedImageBase64

    _uiState.value = _uiState.value.copy(
      isGenerating = true,
      replyingToMessage = null,
      attachedImageBase64 = null,
      attachedImageUri = null
    )

    viewModelScope.launch {
      val selectedModelId = if (_uiState.value.provider == AiProvider.GEMINI) {
        _uiState.value.geminiModel.modelId
      } else {
        if (_uiState.value.openRouterCustomModel.isNotBlank()) {
          _uiState.value.openRouterCustomModel
        } else {
          _uiState.value.openRouterModel.modelId
        }
      }

      repository.sendMessage(
        conversationId = convId,
        content = userText,
        imageBase64 = currentImage,
        passphrase = _uiState.value.encryptionPassphrase,
        openRouterApiKey = _uiState.value.openRouterApiKey,
        selectedProvider = _uiState.value.provider,
        selectedModel = selectedModelId,
        persona = _uiState.value.persona,
        replyToId = currentReplying?.id,
        replyPreview = currentReplying?.content?.take(48)
      )

      _uiState.value = _uiState.value.copy(isGenerating = false)

      // Simulate teammate collaboration response if in project workspace
      if (_uiState.value.activeProjectId != null) {
        simulateTeammateCollaboration(convId)
      }
    }
  }

  private fun simulateTeammateCollaboration(conversationId: String) {
    viewModelScope.launch {
      delay(3000)
      val teammates = listOf("Sarah (Tech Lead)", "Alex (Data Scientist)", "Marcus (Security)")
      val name = teammates.random()
      _uiState.value = _uiState.value.copy(simulatedTeammateTyping = "$name is reviewing...")
      delay(2500)
      _uiState.value = _uiState.value.copy(simulatedTeammateTyping = null)
    }
  }

  fun setReplyTo(message: MessageEntity?) {
    _uiState.value = _uiState.value.copy(replyingToMessage = message)
  }

  fun toggleBookmark(message: MessageEntity) {
    viewModelScope.launch {
      repository.toggleBookmark(message.id, !message.isBookmarked)
    }
  }

  fun attachImageUri(uri: Uri) {
    viewModelScope.launch {
      try {
        val context = getApplication<Application>()
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (bitmap != null) {
          // Resize to max 1024 to optimize latency and memory
          val scaledBitmap = scaleBitmap(bitmap, 1024)
          val outputStream = ByteArrayOutputStream()
          scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
          val byteArray = outputStream.toByteArray()
          val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)

          _uiState.value = _uiState.value.copy(
            attachedImageUri = uri,
            attachedImageBase64 = base64,
            // Prompt mandate: Analyze images MUST use gemini-3.1-pro-preview!
            provider = AiProvider.GEMINI,
            geminiModel = GeminiModel.PRO,
            statusMessage = "Photo attached. Set model to Gemini 3.1 Pro for deep multimodal analysis."
          )
        }
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(statusMessage = "Error processing image: ${e.message}")
      }
    }
  }

  private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDimension && height <= maxDimension) return bitmap

    val ratio = width.toFloat() / height.toFloat()
    val newWidth: Int
    val newHeight: Int
    if (width > height) {
      newWidth = maxDimension
      newHeight = (maxDimension / ratio).toInt()
    } else {
      newHeight = maxDimension
      newWidth = (maxDimension * ratio).toInt()
    }
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
  }

  fun clearAttachedImage() {
    _uiState.value = _uiState.value.copy(attachedImageBase64 = null, attachedImageUri = null)
  }

  fun setProvider(provider: AiProvider) {
    _uiState.value = _uiState.value.copy(provider = provider)
  }

  fun setGeminiModel(model: GeminiModel) {
    _uiState.value = _uiState.value.copy(geminiModel = model)
  }

  fun setOpenRouterModel(model: OpenRouterModel) {
    _uiState.value = _uiState.value.copy(openRouterModel = model, openRouterCustomModel = "")
  }

  fun setOpenRouterCustomModel(model: String) {
    _uiState.value = _uiState.value.copy(openRouterCustomModel = model)
  }

  fun setPersona(persona: PersonaRole) {
    _uiState.value = _uiState.value.copy(persona = persona)
  }

  fun setCustomSystemPrompt(prompt: String) {
    _uiState.value = _uiState.value.copy(customSystemPrompt = prompt)
  }

  fun saveOpenRouterApiKey(key: String) {
    prefs.edit().putString("openrouter_key", key).apply()
    _uiState.value = _uiState.value.copy(openRouterApiKey = key, isOpenRouterKeyValid = null)
    testOpenRouterKey()
  }

  fun testOpenRouterKey() {
    val key = _uiState.value.openRouterApiKey
    if (key.isBlank()) return

    _uiState.value = _uiState.value.copy(isTestingKey = true)
    viewModelScope.launch {
      val isValid = openRouterService.testApiKey(key)
      _uiState.value = _uiState.value.copy(
        isTestingKey = false,
        isOpenRouterKeyValid = isValid,
        statusMessage = if (isValid) "OpenRouter API Key verified successfully!" else "API key test failed or network unreachable."
      )
    }
  }

  fun setPassphrase(passphrase: String) {
    prefs.edit().putString("enc_passphrase", passphrase).apply()
    _uiState.value = _uiState.value.copy(encryptionPassphrase = passphrase)
    // Reload messages with new passphrase
    val currentId = _uiState.value.currentConversationId
    if (currentId != null) {
      selectConversation(currentId)
    }
  }

  fun setThemeMode(theme: AppThemeMode) {
    prefs.edit().putString("theme_mode", theme.name).apply()
    _uiState.value = _uiState.value.copy(
      themeMode = theme,
      isHighContrast = theme == AppThemeMode.HIGH_CONTRAST
    )
  }

  fun setLanguage(lang: AppLanguage) {
    prefs.edit().putString("app_language", lang.name).apply()
    _uiState.value = _uiState.value.copy(language = lang)
  }

  fun setHighContrast(enabled: Boolean) {
    prefs.edit().putBoolean("high_contrast", enabled).apply()
    _uiState.value = _uiState.value.copy(
      isHighContrast = enabled,
      themeMode = if (enabled) AppThemeMode.HIGH_CONTRAST else AppThemeMode.OLED_MIDNIGHT
    )
  }

  fun setFontScale(scale: Float) {
    _uiState.value = _uiState.value.copy(fontScaleFactor = scale)
  }

  fun toggleOfflineMode() {
    syncManager.toggleOfflineMode()
  }

  fun triggerSyncNow() {
    viewModelScope.launch {
      syncManager.performFullSync()
    }
  }

  fun selectProject(projectId: String?) {
    _uiState.value = _uiState.value.copy(activeProjectId = projectId)
    viewModelScope.launch {
      val convList = if (projectId != null) {
        chatDao.getConversationsByProject(projectId)
      } else {
        chatDao.getAllConversations()
      }
      // If no conversations in this project, create one
      val first = conversations.value.firstOrNull { if (projectId != null) it.projectId == projectId else true }
      if (first != null) {
        selectConversation(first.id)
      } else {
        createNewConversation("Project Chat")
      }
    }
  }

  fun createNewProject(name: String, description: String, role: UserRole) {
    viewModelScope.launch {
      val projId = repository.createProject(name, description, role)
      selectProject(projId)
    }
  }

  fun deleteProject(projectId: String) {
    viewModelScope.launch {
      repository.deleteProject(projectId)
      selectProject(null)
    }
  }

  fun clearStatusMessage() {
    _uiState.value = _uiState.value.copy(statusMessage = null)
  }
}
