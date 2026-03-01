package org.artemchik.newmrim.repository

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.artemchik.newmrim.db.*
import org.artemchik.newmrim.protocol.MrimClient
import org.artemchik.newmrim.protocol.MrimConstants
import org.artemchik.newmrim.protocol.data.MessageInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import org.artemchik.newmrim.App
import org.artemchik.newmrim.MainActivity
import org.artemchik.newmrim.R

/**
 * Репозиторий для работы с сообщениями.
 * Слушает входящие/исходящие сообщения от MrimClient и сохраняет в Room.
 * Предоставляет Flow для UI.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val mrimClient: MrimClient,
    private val messageDao: MessageDao,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        // Сохраняем все входящие сообщения в базу
        scope.launch {
            mrimClient.incomingMessages.collect { msg ->
                if (!msg.isTypingNotification && !msg.isSystem) {
                    saveMessage(msg)
                    if (!msg.isOutgoing) {
                        showNotification(msg)
                    }
                }
            }
        }

        // Обновляем статусы доставки
        scope.launch {
            mrimClient.messageDeliveryStatus.collect { (seq, status) ->
                if (status == MrimConstants.MESSAGE_DELIVERED) {
                    messageDao.markDelivered(seq.toLong())
                }
            }
        }
    }

    private suspend fun saveMessage(msg: MessageInfo) {
        val chatEmail = if (msg.isOutgoing) msg.to else msg.from
        messageDao.insert(
            MessageEntity(
                msgId = msg.msgId.toLong(),
                chatEmail = chatEmail,
                fromEmail = msg.from,
                toEmail = msg.to,
                text = msg.text,
                timestamp = msg.timestamp,
                flags = msg.flags.toLong(),
                isOutgoing = msg.isOutgoing,
                isDelivered = msg.isDelivered,
                isRead = msg.isOutgoing // исходящие сразу "прочитаны"
            )
        )
    }

    private fun showNotification(msg: MessageInfo) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, App.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(msg.from)
            .setContentText(msg.text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(msg.from.hashCode(), builder.build())
    }

    /** Сообщения для конкретного контакта (Flow из Room) */
    fun getMessages(contactEmail: String): Flow<List<MessageInfo>> {
        return messageDao.getMessagesForContact(contactEmail).map { entities ->
            entities.map { it.toMessageInfo() }
        }
    }

    /** Пометить все сообщения от контакта как прочитанные */
    suspend fun markAsRead(contactEmail: String) {
        messageDao.markAllRead(contactEmail)
    }

    /** Непрочитанные сообщения по контактам */
    fun getUnreadCounts(): Flow<Map<String, Int>> {
        return messageDao.getUnreadCounts().map { list ->
            list.associate { it.chatEmail to it.count }
        }
    }

    /** Последние сообщения для каждого контакта */
    fun getLastMessages(): Flow<Map<String, LastMessage>> {
        return messageDao.getLastMessages().map { list ->
            list.associateBy { it.chatEmail }
        }
    }

    /** Отправить сообщение через MrimClient */
    suspend fun sendMessage(to: String, text: String): UInt {
        return mrimClient.sendMessage(to, text)
    }

    /** Отправить уведомление "печатает" */
    suspend fun sendTyping(to: String) {
        mrimClient.sendTypingNotification(to)
    }
}

private fun MessageEntity.toMessageInfo(): MessageInfo {
    return MessageInfo(
        msgId = msgId.toUInt(),
        from = fromEmail,
        to = toEmail,
        text = text,
        timestamp = timestamp,
        flags = flags.toUInt(),
        isOutgoing = isOutgoing,
        isDelivered = isDelivered
    )
}