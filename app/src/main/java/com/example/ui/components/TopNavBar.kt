package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.model.AiProvider
import com.example.data.model.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavBar(
  title: String,
  provider: AiProvider,
  modelName: String,
  syncStatus: SyncStatus,
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
          // Model selector pill
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onOpenModelSelector)
              .semantics {
                contentDescription = "Active model $modelName. Tap to change."
                role = Role.Button
              }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .background(MaterialTheme.colorScheme.primary, CircleShape)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = modelName.replace("gemini-", "").replace("anthropic/", ""),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
            }
          }

          Spacer(modifier = Modifier.width(6.dp))

          // E2E Encryption badge
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.semantics {
              contentDescription = "AES-256-GCM End-to-End Encrypted"
            }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(10.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "E2EE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              )
            }
          }

          Spacer(modifier = Modifier.width(6.dp))

          // Cloud Sync badge
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onTriggerSync)
              .semantics {
                contentDescription = "Sync status: ${syncStatus.name}. Tap to sync."
                role = Role.Button
              }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
              val (syncIcon, syncColor) = when (syncStatus) {
                SyncStatus.SYNCED -> Pair(Icons.Default.CloudDone, Color(0xFF10B981))
                SyncStatus.OFFLINE_CACHED -> Pair(Icons.Default.CloudOff, Color(0xFFF59E0B))
                SyncStatus.SYNCING -> Pair(Icons.Default.CloudSync, Color(0xFF3B82F6))
                else -> Pair(Icons.Default.CloudSync, Color(0xFFEF4444))
              }
              Icon(
                imageVector = syncIcon,
                contentDescription = null,
                tint = syncColor,
                modifier = Modifier.size(10.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = if (isOffline) "Offline" else "Synced",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Medium,
                  color = syncColor
                )
              )
            }
          }
        }
      }
    },
    navigationIcon = {
      IconButton(
        onClick = onOpenDrawer,
        modifier = Modifier.semantics {
          contentDescription = "Open sidebar navigation and chat history"
          role = Role.Button
        }
      ) {
        Icon(
          imageVector = Icons.Default.Menu,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onBackground
        )
      }
    },
    actions = {
      // Team & Role Management
      IconButton(
        onClick = onOpenRoleManagement,
        modifier = Modifier.semantics {
          contentDescription = "Team collaboration and role permissions"
          role = Role.Button
        }
      ) {
        Icon(
          imageVector = Icons.Default.Group,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // API Documentation
      IconButton(
        onClick = onOpenApiDocs,
        modifier = Modifier.semantics {
          contentDescription = "API documentation for third-party integration"
          role = Role.Button
        }
      ) {
        Icon(
          imageVector = Icons.Default.Description,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Settings
      IconButton(
        onClick = onOpenSettings,
        modifier = Modifier.semantics {
          contentDescription = "Application settings, language, and theme"
          role = Role.Button
        }
      ) {
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  )
}
