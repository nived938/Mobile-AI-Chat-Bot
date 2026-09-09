package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MessageEntity
import com.example.data.model.PersonaRole

@Composable
fun ChatInputBar(
  isGenerating: Boolean,
  replyingToMessage: MessageEntity?,
  attachedImageBase64: String?,
  currentPersona: PersonaRole,
  onClearReply: () -> Unit,
  onClearImage: () -> Unit,
  onPickImage: () -> Unit,
  onOpenPersonaSelector: () -> Unit,
  onSendMessage: (String) -> Unit
) {
  var inputText by remember { mutableStateOf("") }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      // Reply preview banner
      AnimatedVisibility(visible = replyingToMessage != null) {
        if (replyingToMessage != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 6.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Replying to ${replyingToMessage.authorName}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                )
                Text(
                  text = replyingToMessage.content,
                  maxLines = 1,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                )
              }
              IconButton(
                onClick = onClearReply,
                modifier = Modifier
                  .size(28.dp)
                  .semantics { contentDescription = "Cancel reply" }
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      // Attached photo preview banner
      AnimatedVisibility(visible = !attachedImageBase64.isNullOrBlank()) {
        if (!attachedImageBase64.isNullOrBlank()) {
          val imageBitmap = remember(attachedImageBase64) {
            try {
              val bytes = Base64.decode(attachedImageBase64, Base64.NO_WRAP)
              BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) {
              null
            }
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 6.dp)
          ) {
            Row(
              modifier = Modifier.padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (imageBitmap != null) {
                Image(
                  bitmap = imageBitmap,
                  contentDescription = "Attached photo preview",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Photo Ready for Analysis",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                )
                Text(
                  text = "Using Gemini 3.1 Pro multimodal reasoning",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                )
              }
              IconButton(
                onClick = onClearImage,
                modifier = Modifier
                  .size(28.dp)
                  .semantics { contentDescription = "Remove photo" }
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      // Input row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
      ) {
        // Photo attachment button
        IconButton(
          onClick = onPickImage,
          modifier = Modifier
            .size(48.dp)
            .padding(bottom = 4.dp)
            .semantics {
              contentDescription = "Upload photo for Gemini 3.1 Pro analysis"
              role = Role.Button
            }
        ) {
          Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = null,
            tint = if (attachedImageBase64 != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Persona role pill button
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier
            .padding(bottom = 8.dp, end = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenPersonaSelector)
            .semantics {
              contentDescription = "Current persona: ${currentPersona.title}. Tap to change."
              role = Role.Button
            }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = currentPersona.icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = currentPersona.title.take(10),
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
              )
            )
          }
        }

        // Main text input field
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = {
            Text(
              text = if (attachedImageBase64 != null) "Ask about this photo..." else "Ask anything...",
              style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            )
          },
          modifier = Modifier
            .weight(1f)
            .padding(end = 6.dp)
            .semantics { contentDescription = "Message input" },
          shape = RoundedCornerShape(20.dp),
          maxLines = 4,
          keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
          keyboardActions = KeyboardActions(
            onSend = {
              if (inputText.isNotBlank() || attachedImageBase64 != null) {
                onSendMessage(inputText)
                inputText = ""
              }
            }
          ),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
          )
        )

        // Send Button
        val canSend = (inputText.isNotBlank() || attachedImageBase64 != null) && !isGenerating

        Box(
          modifier = Modifier
            .size(48.dp)
            .padding(bottom = 4.dp),
          contentAlignment = Alignment.Center
        ) {
          if (isGenerating) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = MaterialTheme.colorScheme.primary,
              strokeWidth = 2.dp
            )
          } else {
            IconButton(
              onClick = {
                if (canSend) {
                  onSendMessage(inputText)
                  inputText = ""
                }
              },
              enabled = canSend,
              modifier = Modifier
                .size(42.dp)
                .background(
                  color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                  shape = CircleShape
                )
                .semantics {
                  contentDescription = "Send message"
                  role = Role.Button
                }
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }
  }
}
