package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
  @PrimaryKey
  val id: String,
  val title: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val provider: String = "GEMINI",
  val model: String = "gemini-3.5-flash",
  val personaId: String = "general",
  val customSystemPrompt: String? = null,
  val projectId: String? = null,
  val isPinned: Boolean = false,
  val isArchived: Boolean = false,
  val syncStatus: String = "SYNCED",
  val lastSyncTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
  @PrimaryKey
  val id: String,
  val conversationId: String,
  val role: String, // "user", "assistant", "system"
  val content: String,
  val timestamp: Long = System.currentTimeMillis(),
  val imageBase64: String? = null,
  val modelUsed: String? = null,
  val isEncrypted: Boolean = false,
  val syncStatus: String = "SYNCED",
  val authorName: String = "You",
  val replyToId: String? = null,
  val replyPreview: String? = null,
  val isBookmarked: Boolean = false
)

@Entity(tableName = "projects")
data class ProjectEntity(
  @PrimaryKey
  val id: String,
  val name: String,
  val description: String,
  val currentUserRole: String = "OWNER",
  val memberCount: Int = 1,
  val isEncrypted: Boolean = true,
  val remoteSyncUrl: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val syncStatus: String = "SYNCED"
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
  @PrimaryKey(autoGenerate = true)
  val queueId: Long = 0,
  val entityType: String, // "MESSAGE", "CONVERSATION", "PROJECT"
  val entityId: String,
  val operation: String, // "INSERT", "UPDATE", "DELETE"
  val payloadJson: String,
  val queuedAt: Long = System.currentTimeMillis(),
  val retryCount: Int = 0,
  val status: String = "PENDING"
)
