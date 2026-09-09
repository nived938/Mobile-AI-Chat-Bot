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
import com.example.data.local.LocalLlmService
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
  val currentConversationTitle: String = "NovaMind",
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
  private val localLlmService = LocalLlmService(application)
  private val syncManager = CloudSyncManager(chatDao)
  val repository = ChatRepository(chatDao, geminiService, openRouterService, syncManager, localLlmService)

  private val _uiState = MutableStateFlow(ChatUiState())
  val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
  val conversations: StateFlow<List<ConversationEntity>> = repository.conversations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  val projects: StateFlow<List<ProjectEntity>> = repository.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
      conversations.collect { list ->
        if (_uiState.value.currentConversationId == null && list.isNotEmpty()) selectConversation(list.first().id)
      }
    }
  }

  private fun loadSettingsFromPrefs() {
    val savedTheme = prefs.getString("theme_mode", AppThemeMode.OLED_MIDNIGHT.name)
    val savedLang = prefs.getString("app_language", AppLanguage.EN.name)
    val savedPassphrase = prefs.getString("enc_passphrase", "omni_vault_device_master_key_secure_2025") ?: "omni_vault_device_master_key_secure_2025"
    _uiState.value = _uiState.value.copy(
      openRouterApiKey = "",
      themeMode = try { AppThemeMode.valueOf(savedTheme ?: "") } catch (_: Exception) { AppThemeMode.OLED_MIDNIGHT },
      language = try { AppLanguage.valueOf(savedLang ?: "") } catch (_: Exception) { AppLanguage.EN },
      encryptionPassphrase = savedPassphrase,
      isHighContrast = prefs.getBoolean("high_contrast", false)
    )
  }

  fun selectConversation(conversationId: String) {
    _uiState.value = _uiState.value.copy(currentConversationId = conversationId, replyingToMessage = null, attachedImageBase64 = null, attachedImageUri = null)
    messageCollectionJob?.cancel()
    messageCollectionJob = viewModelScope.launch {
      repository.getMessages(conversationId, _uiState.value.encryptionPassphrase).collect { _currentMessages.value = it }
    }
    viewModelScope.launch {
      val conv = chatDao.getConversationById(conversationId)
      if (conv != null) _uiState.value = _uiState.value.copy(currentConversationTitle = conv.title, activeProjectId = conv.projectId)
    }
  }

  fun createNewConversation(title: String = "New Chat") {
    viewModelScope.launch {
      val id = repository.createConversation(title, AiProvider.GEMINI, "local-qwen2.5-0.5b-q4km", _uiState.value.persona.id, _uiState.value.activeProjectId)
      selectConversation(id)
    }
  }

  fun deleteConversation(id: String) {
    viewModelScope.launch {
      repository.deleteConversation(id)
      if (_uiState.value.currentConversationId == id) {
        conversations.value.firstOrNull { it.id != id }?.let { selectConversation(it.id) }
      }
    }
  }

  fun sendMessage(userText: String) {
    val convId = _uiState.value.currentConversationId ?: return
    if (userText.isBlank() && _uiState.value.attachedImageBase64 == null) return
    val currentReplying = _uiState.value.replyingToMessage
    val currentImage = _uiState.value.attachedImageBase64
    val firstRun = !localLlmService.isModelInstalled()
    _uiState.value = _uiState.value.copy(
      isGenerating = true,
      replyingToMessage = null,
      attachedImageBase64 = null,
      attachedImageUri = null,
      statusMessage = if (firstRun && currentImage == null) "Downloading the local AI model (~400 MB). This happens once." else null
    )

    viewModelScope.launch {
      repository.sendMessage(
        conversationId = convId,
        content = userText,
        imageBase64 = currentImage,
        passphrase = _uiState.value.encryptionPassphrase,
        openRouterApiKey = "",
        selectedProvider = AiProvider.GEMINI,
        selectedModel = "local-qwen2.5-0.5b-q4km",
        persona = _uiState.value.persona,
        replyToId = currentReplying?.id,
        replyPreview = currentReplying?.content?.take(48)
      )
      _uiState.value = _uiState.value.copy(isGenerating = false, statusMessage = null)
    }
  }

  fun setReplyTo(message: MessageEntity?) { _uiState.value = _uiState.value.copy(replyingToMessage = message) }
  fun toggleBookmark(message: MessageEntity) { viewModelScope.launch { repository.toggleBookmark(message.id, !message.isBookmarked) } }

  fun attachImageUri(uri: Uri) {
    viewModelScope.launch {
      try {
        val context = getApplication<Application>()
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (bitmap != null) {
          val scaledBitmap = scaleBitmap(bitmap, 1024)
          val outputStream = ByteArrayOutputStream()
          scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
          _uiState.value = _uiState.value.copy(
            attachedImageUri = uri,
            attachedImageBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP),
            statusMessage = "Photo attached. Say what you want to change or remove from it."
          )
        }
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(statusMessage = "Error processing image: ${e.message}")
      }
    }
  }

  private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) return bitmap
    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val width: Int
    val height: Int
    if (bitmap.width > bitmap.height) { width = maxDimension; height = (maxDimension / ratio).toInt() }
    else { height = maxDimension; width = (maxDimension * ratio).toInt() }
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
  }

  fun clearAttachedImage() { _uiState.value = _uiState.value.copy(attachedImageBase64 = null, attachedImageUri = null) }
  fun setProvider(provider: AiProvider) { _uiState.value = _uiState.value.copy(provider = AiProvider.GEMINI) }
  fun setGeminiModel(model: GeminiModel) { _uiState.value = _uiState.value.copy(geminiModel = model) }
  fun setOpenRouterModel(model: OpenRouterModel) { _uiState.value = _uiState.value.copy(openRouterModel = model, openRouterCustomModel = "") }
  fun setOpenRouterCustomModel(model: String) { _uiState.value = _uiState.value.copy(openRouterCustomModel = model) }
  fun setPersona(persona: PersonaRole) { _uiState.value = _uiState.value.copy(persona = persona) }
  fun setCustomSystemPrompt(prompt: String) { _uiState.value = _uiState.value.copy(customSystemPrompt = prompt) }
  fun saveOpenRouterApiKey(key: String) { prefs.edit().remove("openrouter_key").apply(); _uiState.value = _uiState.value.copy(openRouterApiKey = "", isOpenRouterKeyValid = null) }
  fun testOpenRouterKey() { _uiState.value = _uiState.value.copy(isTestingKey = false, isOpenRouterKeyValid = false, statusMessage = "NovaMind runs chat locally. No API key is required.") }
  fun setPassphrase(passphrase: String) { prefs.edit().putString("enc_passphrase", passphrase).apply(); _uiState.value = _uiState.value.copy(encryptionPassphrase = passphrase); _uiState.value.currentConversationId?.let { selectConversation(it) } }
  fun setThemeMode(theme: AppThemeMode) { prefs.edit().putString("theme_mode", theme.name).apply(); _uiState.value = _uiState.value.copy(themeMode = theme, isHighContrast = theme == AppThemeMode.HIGH_CONTRAST) }
  fun setLanguage(lang: AppLanguage) { prefs.edit().putString("app_language", lang.name).apply(); _uiState.value = _uiState.value.copy(language = lang) }
  fun setHighContrast(enabled: Boolean) { prefs.edit().putBoolean("high_contrast", enabled).apply(); _uiState.value = _uiState.value.copy(isHighContrast = enabled, themeMode = if (enabled) AppThemeMode.HIGH_CONTRAST else AppThemeMode.OLED_MIDNIGHT) }
  fun setFontScale(scale: Float) { _uiState.value = _uiState.value.copy(fontScaleFactor = scale) }
  fun toggleOfflineMode() { syncManager.toggleOfflineMode() }
  fun triggerSyncNow() { viewModelScope.launch { syncManager.performFullSync() } }
  fun selectProject(projectId: String?) { _uiState.value = _uiState.value.copy(activeProjectId = projectId); viewModelScope.launch { conversations.value.firstOrNull { if (projectId != null) it.projectId == projectId else true }?.let { selectConversation(it.id) } ?: createNewConversation("Project Chat") } }
  fun createNewProject(name: String, description: String, role: UserRole) { viewModelScope.launch { selectProject(repository.createProject(name, description, role)) } }
  fun deleteProject(projectId: String) { viewModelScope.launch { repository.deleteProject(projectId); selectProject(null) } }
  fun clearStatusMessage() { _uiState.value = _uiState.value.copy(statusMessage = null) }
}
