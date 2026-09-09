package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MessageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionSheet(
  message: MessageEntity,
  onDismiss: () -> Unit,
  onReply: (MessageEntity) -> Unit,
  onToggleBookmark: (MessageEntity) -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState()
  var showDetails by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
      Text(
        text = "Message Options",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Action: Copy Text
      ActionItem(
        icon = Icons.Default.ContentCopy,
        label = "Copy Message Text",
        onClick = {
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          clipboard.setPrimaryClip(ClipData.newPlainText("Chat Message", message.content))
          Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
          onDismiss()
        }
      )

      // Action: Reply
      ActionItem(
        icon = Icons.AutoMirrored.Filled.Reply,
        label = "Reply to Message",
        onClick = {
          onReply(message)
          onDismiss()
        }
      )

      // Action: Bookmark / Star
      ActionItem(
        icon = if (message.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
        label = if (message.isBookmarked) "Remove Bookmark" else "Bookmark Message",
        onClick = {
          onToggleBookmark(message)
          onDismiss()
        }
      )

      // Action: View Security & Crypto Details
      ActionItem(
        icon = Icons.Default.Info,
        label = if (showDetails) "Hide Technical Security Details" else "View Technical Security & Model Details",
        onClick = { showDetails = !showDetails }
      )

      if (showDetails) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp))
            Text("Security & Audit Record", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("ID: ${message.id}", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Text("Cipher: AES-256-GCM with 128-bit authentication tag", fontSize = 10.sp)
            Text("Key Derivation: PBKDF2WithHmacSHA256 (65,536 iterations)", fontSize = 10.sp)
            Text("Model: ${message.modelUsed ?: "Local / User Input"}", fontSize = 10.sp)
            Text("Timestamp: $dateStr", fontSize = 10.sp)
            Text("Local SQLite Table: messages", fontSize = 10.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun ActionItem(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .semantics {
        contentDescription = label
        role = Role.Button
      }
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
      )
    }
  }
}
