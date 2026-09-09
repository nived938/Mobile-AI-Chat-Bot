package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ApiDocsDialog(onDismiss: () -> Unit) {
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) }

  val curlExample = """
curl -X POST https://api.omnichat.dev/v1/chat/completions \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "X-Encryption-Mode: AES-256-GCM" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openrouter",
    "model": "anthropic/claude-3.5-sonnet",
    "messages": [
      {"role": "user", "content": "Analyze system architecture"}
    ],
    "sync": {
      "sqlite_table": "messages",
      "conflict_policy": "client_authoritative"
    }
  }'
  """.trimIndent()

  val jsExample = """
import { OmniChatClient } from '@omnichat/sdk';

const client = new OmniChatClient({
  apiKey: process.env.OPENROUTER_API_KEY,
  encryptionKey: 'device_passphrase',
  cloudSyncUrl: 'https://supabase.co/rest/v1'
});

// Send multi-turn query with offline SQLite auto-queue
const response = await client.chat({
  model: 'gemini-3.1-pro-preview',
  message: 'Identify security bottlenecks',
  image: 'data:image/jpeg;base64,...'
});
console.log(response.text);
  """.trimIndent()

  val syncApiDoc = """
POST /v1/sync/batch
Headers:
  Authorization: Bearer <token>
  X-Device-Id: <uuid>

Payload (JSON):
{
  "sync_version": "2.0",
  "mutations": [
    {
      "table": "messages",
      "operation": "UPSERT",
      "data": {
        "id": "uuid",
        "encrypted_blob": "enc:v1:...",
        "timestamp": 1725840000000
      }
    }
  ]
}
  """.trimIndent()

  fun copyToClipboard(text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("API Code", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .padding(vertical = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Terminal,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Third-Party API Documentation",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
              )
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Integrate your services seamlessly with the AI Chatbot's local SQLite persistence, OpenRouter model router, and Supabase/Neon synchronization layer.",
          style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Code Language Tabs
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = MaterialTheme.colorScheme.primary
            )
          }
        ) {
          Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("cURL") })
          Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Node.js") })
          Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Cloud Sync Spec") })
        }

        Spacer(modifier = Modifier.height(14.dp))

        val currentSnippet = when (selectedTab) {
          0 -> curlExample
          1 -> jsExample
          else -> syncApiDoc
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFF07090E),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = when (selectedTab) {
                  0 -> "REST Endpoint"
                  1 -> "TypeScript / JavaScript"
                  else -> "Supabase / Neon Sync Protocol"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold
                )
              )
              IconButton(
                onClick = { copyToClipboard(currentSnippet) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = "Copy code snippet",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
              Text(
                text = currentSnippet,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  color = Color(0xFF38BDF8),
                  lineHeight = 16.sp
                )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Done")
        }
      }
    }
  }
}
