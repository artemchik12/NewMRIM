package org.artemchik.newmrim.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "messages",
    indices = [Index(value = ["chatEmail", "timestamp"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val msgId: Long,           // UInt -> Long
    val chatEmail: String,     // контакт (для быстрых запросов)
    val fromEmail: String,
    val toEmail: String,
    val text: String,
    val timestamp: Long,
    val flags: Long = 0,       // UInt -> Long
    val isOutgoing: Boolean,
    val isDelivered: Boolean = false,
    val isRead: Boolean = false
)

data class UnreadCount(
    val chatEmail: String,
    val count: Int
)

data class LastMessage(
    val chatEmail: String,
    val text: String,
    val timestamp: Long,
    val isOutgoing: Boolean
)

@Dao
interface MessageDao {

    @Query("""
        SELECT * FROM messages 
        WHERE chatEmail = :contactEmail 
        ORDER BY timestamp ASC
    """)
    fun getMessagesForContact(contactEmail: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("UPDATE messages SET isDelivered = 1 WHERE msgId = :msgId AND isOutgoing = 1")
    suspend fun markDelivered(msgId: Long)

    @Query("UPDATE messages SET isRead = 1 WHERE chatEmail = :contactEmail AND isRead = 0")
    suspend fun markAllRead(contactEmail: String)

    @Query("""
        SELECT chatEmail, COUNT(*) as count 
        FROM messages 
        WHERE isOutgoing = 0 AND isRead = 0 
        GROUP BY chatEmail
    """)
    fun getUnreadCounts(): Flow<List<UnreadCount>>

    @Query("""
        SELECT chatEmail, text, timestamp, isOutgoing 
        FROM messages 
        WHERE id IN (SELECT MAX(id) FROM messages GROUP BY chatEmail)
    """)
    fun getLastMessages(): Flow<List<LastMessage>>

    @Query("DELETE FROM messages WHERE chatEmail = :contactEmail")
    suspend fun deleteForContact(contactEmail: String)
}