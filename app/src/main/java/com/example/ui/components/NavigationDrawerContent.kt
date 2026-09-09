package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.SyncStatus

@Composable
fun NavigationDrawerContent(
  currentConversationId: String?,
  conversations: List<ConversationEntity>,
  projects: List<ProjectEntity>,
  activeProjectId: String?,
  syncStatus: SyncStatus,
  isOffline: Boolean,
  onSelectConversation: (String) -> Unit,
  onNewConversation: () -> Unit,
  onDeleteConversation: (String) -> Unit,
  onSelectProject: (String?) -> Unit,
  onCreateProject: () -> Unit,
  onToggleOffline: () -> Unit,
  onTriggerSync: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxHeight().fillMaxWidth(0.9f)
  ) {
    Column(modifier = Modifier.fillMaxHeight().padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
          modifier = Modifier.size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text("NovaMind", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          Text("Private on-device AI", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary))
        }
      }

      Button(
        onClick = onNewConversation,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth().height(44.dp).semantics { contentDescription = "Start a new chat"; role = Role.Button }
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(7.dp))
        Text("New Chat", fontWeight = FontWeight.SemiBold)
      }

      Spacer(modifier = Modifier.height(16.dp))
      Text("CONVERSATIONS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp))
      Spacer(modifier = Modifier.height(6.dp))

      LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(conversations, key = { it.id }) { conv ->
          val selected = conv.id == currentConversationId
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f) else Color.Transparent),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onSelectConversation(conv.id) }
          ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(if (conv.isPinned) Icons.Default.PushPin else Icons.Default.ChatBubbleOutline, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(conv.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal))
              IconButton(onClick = { onDeleteConversation(conv.id) }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete chat", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f), modifier = Modifier.size(17.dp))
              }
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      Spacer(modifier = Modifier.height(10.dp))
      Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text("On-device AI", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
          Spacer(modifier = Modifier.height(3.dp))
          Text("Qwen2.5 0.5B · GGUF · no API key", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
          Spacer(modifier = Modifier.height(3.dp))
          Text("Model downloads once, then chat works offline.", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)))
        }
      }
    }
  }
}
