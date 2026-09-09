package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopNavBar(
  title: String,
  provider: com.example.data.model.AiProvider,
  modelName: String,
  syncStatus: com.example.data.model.SyncStatus,
  isOffline: Boolean,
  onOpenDrawer: () -> Unit,
  onOpenModelSelector: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenApiDocs: () -> Unit,
  onOpenRoleManagement: () -> Unit,
  onTriggerSync: () -> Unit
) {
  TopAppBar(
    title = {
      Column {
        Text(
          text = title,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 2.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
            modifier = Modifier.semantics { contentDescription = "On-device AI model: Qwen2.5 0.5B" }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
              Surface(modifier = Modifier.size(6.dp), shape = CircleShape, color = Color(0xFF10B981)) {}
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "On-device AI",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.primary
                )
              )
            }
          }
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.semantics { contentDescription = "Chats stay encrypted on this device" }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "E2EE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              )
            }
          }
        }
      }
    },
    navigationIcon = {
      IconButton(
        onClick = onOpenDrawer,
        modifier = Modifier.semantics { contentDescription = "Open chat history"; role = Role.Button }
      ) { Icon(Icons.Default.Menu, contentDescription = null) }
    },
    actions = {
      IconButton(
        onClick = onOpenSettings,
        modifier = Modifier.semantics { contentDescription = "App settings"; role = Role.Button }
      ) { Icon(Icons.Default.Settings, contentDescription = null) }
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
  )
}
