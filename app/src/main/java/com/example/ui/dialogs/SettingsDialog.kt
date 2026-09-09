package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.security.EncryptionManager
import com.example.data.sync.SyncLog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
  currentTheme: AppThemeMode,
  currentLanguage: AppLanguage,
  isHighContrast: Boolean,
  fontScale: Float,
  openRouterApiKey: String,
  isTestingKey: Boolean,
  isKeyValid: Boolean?,
  passphrase: String,
  syncLogs: List<SyncLog>,
  onDismiss: () -> Unit,
  onThemeChange: (AppThemeMode) -> Unit,
  onLanguageChange: (AppLanguage) -> Unit,
  onHighContrastToggle: (Boolean) -> Unit,
  onFontScaleChange: (Float) -> Unit,
  onSaveOpenRouterKey: (String) -> Unit,
  onTestOpenRouterKey: () -> Unit,
  onSavePassphrase: (String) -> Unit,
  onTriggerSync: () -> Unit
) {
  var keyInput by remember { mutableStateOf(openRouterApiKey) }
  var showKey by remember { mutableStateOf(false) }

  var passphraseInput by remember { mutableStateOf(passphrase) }
  var showPassphrase by remember { mutableStateOf(false) }

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
        .fillMaxWidth()
        .padding(vertical = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Dialog Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Palette,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Settings & Preferences",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
              )
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.semantics { contentDescription = "Close settings dialog" }
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: Dark Mode Theme Customization
        Text(
          text = "DARK MODE THEME",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
          )
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          AppThemeMode.entries.forEach { theme ->
            val isSelected = currentTheme == theme
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
              border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
              ),
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onThemeChange(theme) }
                .semantics {
                  contentDescription = "Theme: ${theme.displayName}"
                  role = Role.Button
                }
            ) {
              Text(
                text = theme.displayName,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: Language Preferences
        Text(
          text = "LANGUAGE PREFERENCES",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
          )
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          AppLanguage.entries.forEach { lang ->
            val isSelected = currentLanguage == lang
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
              ),
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onLanguageChange(lang) }
                .semantics {
                  contentDescription = "Language: ${lang.displayName}"
                  role = Role.Button
                }
            ) {
              Text(
                text = "${lang.displayName} (${lang.code})",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: OpenRouter API Key Vault
        Text(
          text = "OPENROUTER API KEY VAULT",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
          )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Add your OpenRouter key to chat with Claude 3.5 Sonnet, GPT-4o, DeepSeek R1, Llama 3.3, and more. Keys are encrypted with AES-256.",
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = keyInput,
          onValueChange = { keyInput = it },
          placeholder = { Text("sk-or-v1-••••••••") },
          modifier = Modifier.fillMaxWidth(),
          visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { showKey = !showKey }) {
              Icon(
                imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showKey) "Hide key" else "Show key"
              )
            }
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
          )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { onSaveOpenRouterKey(keyInput) },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Save Encrypted Key", fontSize = 12.sp)
          }

          Button(
            onClick = onTestOpenRouterKey,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f)
          ) {
            if (isTestingKey) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
              Text(
                text = when (isKeyValid) {
                  true -> "✓ Verified"
                  false -> "✗ Failed"
                  null -> "Test Key"
                },
                fontSize = 12.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 4: End-to-End Encryption Manager
        Text(
          text = "END-TO-END ENCRYPTION (AES-256-GCM)",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
          )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "All chat history and project data stored in local SQLite are sealed with AES-256-GCM and PBKDF2WithHmacSHA256 (65,536 rounds).",
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = passphraseInput,
          onValueChange = { passphraseInput = it },
          label = { Text("Encryption Passphrase") },
          modifier = Modifier.fillMaxWidth(),
          visualTransformation = if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { showPassphrase = !showPassphrase }) {
              Icon(
                imageVector = if (showPassphrase) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null
              )
            }
          }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = { onSavePassphrase(passphraseInput) },
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Update Encryption Passphrase", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 5: Accessibility & Screen Reader Controls
        Text(
          text = "ACCESSIBILITY & SCREEN READERS",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
          )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = "High Contrast Mode (WCAG AAA)",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
            Text(
              text = "Stark black background, pure white text & borders",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
          }
          Switch(
            checked = isHighContrast,
            onCheckedChange = onHighContrastToggle,
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.Yellow,
              checkedTrackColor = Color.Yellow.copy(alpha = 0.4f)
            )
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Font Scaling", style = MaterialTheme.typography.bodyMedium)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val scales = listOf(Pair("Default", 1.0f), Pair("Large", 1.15f), Pair("X-Large", 1.3f))
          scales.forEach { (label, factor) ->
            val isCurrent = fontScale == factor
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent
              ),
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onFontScaleChange(factor) }
            ) {
              Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                  )
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
        ) {
          Text("Done", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
