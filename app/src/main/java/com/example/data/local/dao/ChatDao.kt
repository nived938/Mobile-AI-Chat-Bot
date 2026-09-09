package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
  // Conversations
  @Query("SELECT * FROM conversations WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
  fun getAllConversations(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE projectId = :projectId AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
  fun getConversationsByProject(projectId: String): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
  suspend fun getConversationById(id: String): ConversationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConversation(conversation: ConversationEntity)

  @Update
  suspend fun updateConversation(conversation: ConversationEntity)

  @Query("DELETE FROM conversations WHERE id = :id")
  suspend fun deleteConversation(id: String)

  // Messages
  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
  fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
  suspend fun getMessagesListForConversation(conversationId: String): List<MessageEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: MessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessages(messages: List<MessageEntity>)

  @Update
  suspend fun updateMessage(message: MessageEntity)

  @Query("DELETE FROM messages WHERE id = :messageId")
  suspend fun deleteMessage(messageId: String)

  @Query("DELETE FROM messages WHERE conversationId = :conversationId")
  suspend fun deleteMessagesForConversation(conversationId: String)

  @Query("UPDATE messages SET isBookmarked = :isBookmarked WHERE id = :messageId")
  suspend fun setMessageBookmarked(messageId: String, isBookmarked: Boolean)

  // Projects / Workspaces
  @Query("SELECT * FROM projects ORDER BY createdAt ASC")
  fun getAllProjects(): Flow<List<ProjectEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProject(project: ProjectEntity)

  @Update
  suspend fun updateProject(project: ProjectEntity)

  @Query("DELETE FROM projects WHERE id = :id")
  suspend fun deleteProject(id: String)

  // Sync Queue
  @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY queuedAt ASC")
  suspend fun getPendingSyncItems(): List<SyncQueueEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSyncItem(item: SyncQueueEntity)

  @Query("DELETE FROM sync_queue WHERE queueId = :queueId")
  suspend fun removeSyncItem(queueId: Long)

  @Query("DELETE FROM sync_queue")
  suspend fun clearSyncQueue()
}
