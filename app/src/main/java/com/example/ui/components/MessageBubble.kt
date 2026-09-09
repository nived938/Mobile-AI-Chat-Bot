package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.data.local.entity.MessageEntity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
  message: MessageEntity,
  fontScale: Float,
  onSwipeReply: (MessageEntity) -> Unit,
  onDoubleTapBookmark: (MessageEntity) -> Unit,
  onLongPress: (MessageEntity) -> Unit
) {
  val isUser = message.role == "user"
  val timeStr = remember(message.timestamp) {
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
  }

  // Swipe-to-reply gesture handling
  var dragOffsetX by remember { mutableFloatStateOf(0f) }
  val animatedOffsetX by animateFloatAsState(targetValue = dragOffsetX, label = "swipeReply")
  val swipeThreshold = 140f

  val draggableState = rememberDraggableState { delta ->
    // Drag to the right (positive delta)
    val nextOffset = (dragOffsetX + delta).coerceIn(0f, 220f)
    dragOffsetX = nextOffset
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .semantics {
        contentDescription = "${if (isUser) "Your message" else message.authorName}: ${message.content}"
        role = Role.Button
      }
  ) {
    // Reveal reply icon on drag
    if (dragOffsetX > 20f) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 12.dp)
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Reply,
          contentDescription = "Swipe to reply",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
        .draggable(
          state = draggableState,
          orientation = Orientation.Horizontal,
          onDragStopped = {
            if (dragOffsetX >= swipeThreshold) {
              onSwipeReply(message)
            }
            dragOffsetX = 0f
          }
        ),
      horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
      if (!isUser) {
        // Assistant Avatar
        Surface(
          modifier = Modifier
            .size(34.dp)
            .padding(top = 2.dp),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.SmartToy,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
      }

      Column(
        modifier = Modifier.weight(1f, fill = false),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
      ) {
        // Author & Model Tag
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 3.dp, start = 4.dp, end = 4.dp)
        ) {
          Text(
            text = message.authorName,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = (11 * fontScale).sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )

          if (message.modelUsed != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                text = message.modelUsed.replace("gemini-", "").replace("anthropic/", "").replace("openai/", ""),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = (9 * fontScale).sp,
                  color = MaterialTheme.colorScheme.primary,
                  fontFamily = FontFamily.Monospace
                )
              )
            }
          }

          if (message.isEncrypted) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "AES-256 Encrypted",
              tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
              modifier = Modifier.size(11.dp)
            )
          }

          if (message.isBookmarked) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Default.Bookmark,
              contentDescription = "Bookmarked",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(12.dp)
            )
          }
        }

        // Reply quote bubble if present
        if (!message.replyPreview.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
              .padding(bottom = 2.dp)
              .width(260.dp)
              .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
              )
          ) {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
              Box(
                modifier = Modifier
                  .width(3.dp)
                  .height(18.dp)
                  .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = message.replyPreview,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = (11 * fontScale).sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
            }
          }
        }

        // Main Bubble Box with combinedClickable for gestures
        Surface(
          shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isUser) 16.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 16.dp
          ),
          color = if (isUser) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
          } else {
            MaterialTheme.colorScheme.surface
          },
          border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
          ),
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
              onClick = {},
              onDoubleClick = { onDoubleTapBookmark(message) },
              onLongClick = { onLongPress(message) }
            )
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
          ) {
            // Attached photo display
            if (!message.imageBase64.isNullOrBlank()) {
              val imageBitmap = remember(message.imageBase64) {
                try {
                  val decodedBytes = Base64.decode(message.imageBase64, Base64.NO_WRAP)
                  BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                } catch (e: Exception) {
                  null
                }
              }

              if (imageBitmap != null) {
                Image(
                  bitmap = imageBitmap,
                  contentDescription = "Attached photo for multimodal analysis",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
              }
            }

            // Message text & code blocks
            val contentParts = remember(message.content) {
              parseMessageContent(message.content)
            }

            Column(modifier = Modifier.fillMaxWidth()) {
              contentParts.forEach { part ->
                if (part.isCode) {
                  CodeBlockView(
                    code = part.content,
                    lang = part.lang,
                    fontScale = fontScale
                  )
                } else {
                  if (part.content.isNotBlank()) {
                    Text(
                      text = part.content,
                      style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = (15 * fontScale).sp,
                        lineHeight = (22 * fontScale).sp,
                        color = MaterialTheme.colorScheme.onBackground
                      )
                    )
                  }
                }
              }
            }

            // Timestamp and status footer
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.align(Alignment.End),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = (10 * fontScale).sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
              )

              if (isUser) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "✓",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
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

private data class MessagePart(
  val isCode: Boolean,
  val content: String,
  val lang: String = "code"
)

private fun parseMessageContent(raw: String): List<MessagePart> {
  val pattern = Regex("```([a-zA-Z0-9_\\-+.]*)\\r?\\n?([\\s\\S]*?)```")
  val parts = mutableListOf<MessagePart>()
  var lastIndex = 0
  val matches = pattern.findAll(raw)

  for (match in matches) {
    if (match.range.first > lastIndex) {
      val textBefore = raw.substring(lastIndex, match.range.first)
      if (textBefore.isNotEmpty()) {
        parts.add(MessagePart(isCode = false, content = textBefore))
      }
    }
    val lang = match.groupValues.getOrNull(1)?.trim()?.ifEmpty { "code" } ?: "code"
    val code = match.groupValues.getOrNull(2)?.trim() ?: ""
    parts.add(MessagePart(isCode = true, content = code, lang = lang))
    lastIndex = match.range.last + 1
  }

  if (lastIndex < raw.length) {
    val remaining = raw.substring(lastIndex)
    if (remaining.isNotEmpty()) {
      parts.add(MessagePart(isCode = false, content = remaining))
    }
  }

  if (parts.isEmpty()) {
    parts.add(MessagePart(isCode = false, content = raw))
  }

  return parts
}

@Composable
private fun CodeBlockView(
  code: String,
  lang: String,
  fontScale: Float
) {
  var copied by remember { mutableStateOf(false) }
  val clipboard = LocalClipboardManager.current

  LaunchedEffect(copied) {
    if (copied) {
      delay(2000)
      copied = false
    }
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    shape = RoundedCornerShape(8.dp),
    color = Color.Black,
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27272A))
  ) {
    Column {
      // Header with language and copy button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF18181B))
          .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = lang.uppercase(),
          color = Color(0xFFA1A1AA),
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
          )
        )
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable {
              clipboard.setText(AnnotatedString(code))
              copied = true
            }
            .padding(horizontal = 6.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = "Copy code snippet",
            tint = if (copied) Color(0xFF34D399) else Color(0xFFA1A1AA),
            modifier = Modifier.size(13.dp)
          )
          Text(
            text = if (copied) "Copied!" else "Copy code",
            color = if (copied) Color(0xFF34D399) else Color(0xFFA1A1AA),
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium
            )
          )
        }
      }

      // Black box with white text
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(10.dp)
      ) {
        Text(
          text = code,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = (12 * fontScale).sp,
            lineHeight = (18 * fontScale).sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White
          )
        )
      }
    }
  }
}
