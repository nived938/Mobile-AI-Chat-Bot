package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
    modifier = Modifier
      .fillMaxHeight()
      .width(320.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxHeight()
        .padding(16.dp)
    ) {
      // Header branding
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
          modifier = Modifier.size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.SmartToy,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "AI Chatbot",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground
            )
          )
          Text(
            text = "SQLite Local + Cloud Sync",
            style = MaterialTheme.typography.labelSmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }
      }

      // New Chat action button
      Button(
        onClick = onNewConversation,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
          .semantics {
            contentDescription = "Start a new chat conversation"
            role = Role.Button
          }
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "New Chat", fontWeight = FontWeight.SemiBold)
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Workspace / Projects Section
      Text(
        text = "WORKSPACES & TEAMS",
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
      )

      // Project Tabs
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val isPersonalSelected = activeProjectId == null
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isPersonalSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPersonalSelected) MaterialTheme.colorScheme.primary else Color.Transparent
          ),
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelectProject(null) }
            .semantics {
              contentDescription = "Personal Workspace tab"
              role = Role.Button
            }
        ) {
          Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Personal",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isPersonalSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isPersonalSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
          }
        }

        for (proj in projects.take(2)) {
          val isProjSelected = activeProjectId == proj.id
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isProjSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isProjSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            ),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .clickable { onSelectProject(proj.id) }
              .semantics {
                contentDescription = "${proj.name} workspace tab"
                role = Role.Button
              }
          ) {
            Box(
              modifier = Modifier.padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = proj.name.take(9),
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = if (isProjSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isProjSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      Spacer(modifier = Modifier.height(10.dp))

      // Conversations List
      Text(
        text = "CONVERSATIONS (${conversations.size})",
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
      )

      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        items(conversations, key = { it.id }) { conv ->
          val isSelected = conv.id == currentConversationId
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isSelected) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color.Transparent
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable { onSelectConversation(conv.id) }
              .semantics {
                contentDescription = "Conversation: ${conv.title}"
                role = Role.Button
              }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = if (conv.isPinned) Icons.Default.PushPin else Icons.Default.ChatBubbleOutline,
                  contentDescription = null,
                  tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = conv.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  )
                  Text(
                    text = conv.model.replace("gemini-", "").replace("anthropic/", ""),
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  )
                }
              }

              IconButton(
                onClick = { onDeleteConversation(conv.id) },
                modifier = Modifier
                  .size(28.dp)
                  .semantics { contentDescription = "Delete conversation" }
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      Spacer(modifier = Modifier.height(10.dp))

      // Footer: Offline Mode switch & Cloud Sync button
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          // Offline mode switch
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (isOffline) Icons.Default.WifiOff else Icons.Default.Wifi,
                contentDescription = null,
                tint = if (isOffline) Color(0xFFF59E0B) else Color(0xFF10B981),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isOffline) "Offline Queue" else "Online Sync",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              )
            }
            Switch(
              checked = isOffline,
              onCheckedChange = { onToggleOffline() },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFF59E0B),
                checkedTrackColor = Color(0xFFF59E0B).copy(alpha = 0.4f)
              )
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Sync with Cloud button
          Button(
            onClick = onTriggerSync,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.primary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier
              .fillMaxWidth()
              .height(36.dp)
              .semantics {
                contentDescription = "Sync local SQLite with Cloud Supabase/Neon"
                role = Role.Button
              }
          ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Sync to Supabase/Neon",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }
    }
  }
}
