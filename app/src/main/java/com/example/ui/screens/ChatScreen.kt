package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MessageEntity
import com.example.data.model.AiProvider
import com.example.data.model.GeminiModel
import com.example.ui.components.ChatInputBar
import com.example.ui.components.MessageBubble
import com.example.ui.components.NavigationDrawerContent
import com.example.ui.components.TeamPresenceBar
import com.example.ui.components.TopNavBar
import com.example.ui.dialogs.ApiDocsDialog
import com.example.ui.dialogs.MessageActionSheet
import com.example.ui.dialogs.ModelSelectorDialog
import com.example.ui.dialogs.PersonaSelectorDialog
import com.example.ui.dialogs.RoleManagementDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
  val uiState by viewModel.uiState.collectAsState()
  val conversations by viewModel.conversations.collectAsState()
  val projects by viewModel.projects.collectAsState()
  val currentMessages by viewModel.currentMessages.collectAsState()
  val syncStatus by viewModel.syncStatus.collectAsState()
  val syncLogs by viewModel.syncLogs.collectAsState()
  val isOffline by viewModel.isOfflineMode.collectAsState()

  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val listState = rememberLazyListState()

  // Photo picker launcher
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.attachImageUri(uri)
    }
  }

  // Dialog & Sheet States
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showModelSelector by remember { mutableStateOf(false) }
  var showPersonaSelector by remember { mutableStateOf(false) }
  var showRoleDialog by remember { mutableStateOf(false) }
  var showApiDocsDialog by remember { mutableStateOf(false) }
  var actionMessage by remember { mutableStateOf<MessageEntity?>(null) }

  // Auto-scroll when messages change or generation starts
  LaunchedEffect(currentMessages.size, uiState.isGenerating) {
    if (currentMessages.isNotEmpty()) {
      listState.animateScrollToItem(currentMessages.size - 1)
    }
  }

  // Show status messages in snackbar
  LaunchedEffect(uiState.statusMessage) {
    uiState.statusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
        NavigationDrawerContent(
          currentConversationId = uiState.currentConversationId,
          conversations = conversations,
          projects = projects,
          activeProjectId = uiState.activeProjectId,
          syncStatus = syncStatus,
          isOffline = isOffline,
          onSelectConversation = { id ->
            viewModel.selectConversation(id)
            coroutineScope.launch { drawerState.close() }
          },
          onNewConversation = {
            viewModel.createNewConversation("New Chat")
            coroutineScope.launch { drawerState.close() }
          },
          onDeleteConversation = { id ->
            viewModel.deleteConversation(id)
          },
          onSelectProject = { projId ->
            viewModel.selectProject(projId)
            coroutineScope.launch { drawerState.close() }
          },
          onCreateProject = {
            showRoleDialog = true
          },
          onToggleOffline = { viewModel.toggleOfflineMode() },
          onTriggerSync = { viewModel.triggerSyncNow() }
        )
      }
    }
  ) {
    Scaffold(
      topBar = {
        val activeModelName = if (uiState.provider == AiProvider.GEMINI) {
          uiState.geminiModel.displayName
        } else {
          if (uiState.openRouterCustomModel.isNotBlank()) uiState.openRouterCustomModel else uiState.openRouterModel.displayName
        }

        TopNavBar(
          title = uiState.currentConversationTitle,
          provider = uiState.provider,
          modelName = activeModelName,
          syncStatus = syncStatus,
          isOffline = isOffline,
          onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
          onOpenModelSelector = { showModelSelector = true },
          onOpenSettings = { showSettingsDialog = true },
          onOpenApiDocs = { showApiDocsDialog = true },
          onOpenRoleManagement = { showRoleDialog = true },
          onTriggerSync = { viewModel.triggerSyncNow() }
        )
      },
      bottomBar = {
        ChatInputBar(
          isGenerating = uiState.isGenerating,
          replyingToMessage = uiState.replyingToMessage,
          attachedImageBase64 = uiState.attachedImageBase64,
          currentPersona = uiState.persona,
          onClearReply = { viewModel.setReplyTo(null) },
          onClearImage = { viewModel.clearAttachedImage() },
          onPickImage = { photoPickerLauncher.launch("image/*") },
          onOpenPersonaSelector = { showPersonaSelector = true },
          onSendMessage = { text -> viewModel.sendMessage(text) }
        )
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
      containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
      ) {
        // Team Presence Bar
        val activeProj = projects.find { it.id == uiState.activeProjectId }
        TeamPresenceBar(
          projectName = activeProj?.name,
          userRole = uiState.currentRole,
          typingTeammate = uiState.simulatedTeammateTyping,
          onOpenRoleDetails = { showRoleDialog = true }
        )

        // Chat messages LazyColumn
        Box(modifier = Modifier.weight(1f)) {
          if (currentMessages.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
              ) {
                Text(
                  text = "Start a Conversation",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Type a prompt or tap 📸 to analyze photos with Gemini 3.1 Pro.",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                  )
                )
              }
            }
          } else {
            LazyColumn(
              state = listState,
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              items(currentMessages, key = { it.id }) { msg ->
                MessageBubble(
                  message = msg,
                  fontScale = uiState.fontScaleFactor,
                  onSwipeReply = { viewModel.setReplyTo(it) },
                  onDoubleTapBookmark = { viewModel.toggleBookmark(it) },
                  onLongPress = { actionMessage = it }
                )
              }

              // Typing / Generating indicator
              if (uiState.isGenerating) {
                item {
                  Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(14.dp),
                      strokeWidth = 2.dp,
                      color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Generating response...",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                      )
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // DIALOGS
  if (showSettingsDialog) {
    SettingsDialog(
      currentTheme = uiState.themeMode,
      currentLanguage = uiState.language,
      isHighContrast = uiState.isHighContrast,
      fontScale = uiState.fontScaleFactor,
      openRouterApiKey = uiState.openRouterApiKey,
      isTestingKey = uiState.isTestingKey,
      isKeyValid = uiState.isOpenRouterKeyValid,
      passphrase = uiState.encryptionPassphrase,
      syncLogs = syncLogs,
      onDismiss = { showSettingsDialog = false },
      onThemeChange = { viewModel.setThemeMode(it) },
      onLanguageChange = { viewModel.setLanguage(it) },
      onHighContrastToggle = { viewModel.setHighContrast(it) },
      onFontScaleChange = { viewModel.setFontScale(it) },
      onSaveOpenRouterKey = { viewModel.saveOpenRouterApiKey(it) },
      onTestOpenRouterKey = { viewModel.testOpenRouterKey() },
      onSavePassphrase = { viewModel.setPassphrase(it) },
      onTriggerSync = { viewModel.triggerSyncNow() }
    )
  }

  if (showModelSelector) {
    ModelSelectorDialog(
      currentProvider = uiState.provider,
      currentGeminiModel = uiState.geminiModel,
      currentOpenRouterModel = uiState.openRouterModel,
      customModelInput = uiState.openRouterCustomModel,
      onDismiss = { showModelSelector = false },
      onSelectGeminiModel = {
        viewModel.setProvider(AiProvider.GEMINI)
        viewModel.setGeminiModel(it)
      },
      onSelectOpenRouterModel = {
        viewModel.setProvider(AiProvider.OPENROUTER)
        viewModel.setOpenRouterModel(it)
      },
      onSetCustomOpenRouterModel = {
        viewModel.setProvider(AiProvider.OPENROUTER)
        viewModel.setOpenRouterCustomModel(it)
      }
    )
  }

  if (showPersonaSelector) {
    PersonaSelectorDialog(
      currentPersona = uiState.persona,
      onDismiss = { showPersonaSelector = false },
      onSelectPersona = { viewModel.setPersona(it) }
    )
  }

  if (showRoleDialog) {
    RoleManagementDialog(
      currentRole = uiState.currentRole,
      onDismiss = { showRoleDialog = false }
    )
  }

  if (showApiDocsDialog) {
    ApiDocsDialog(onDismiss = { showApiDocsDialog = false })
  }

  actionMessage?.let { msg ->
    MessageActionSheet(
      message = msg,
      onDismiss = { actionMessage = null },
      onReply = {
        viewModel.setReplyTo(it)
        actionMessage = null
      },
      onToggleBookmark = {
        viewModel.toggleBookmark(it)
        actionMessage = null
      }
    )
  }
}
