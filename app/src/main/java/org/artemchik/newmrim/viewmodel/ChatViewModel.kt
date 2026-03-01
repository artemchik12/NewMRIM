package org.artemchik.newmrim.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.artemchik.newmrim.protocol.MrimClient
import org.artemchik.newmrim.protocol.MrimConstants
import org.artemchik.newmrim.protocol.data.MessageInfo
import org.artemchik.newmrim.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val contactEmail: String = "",
    val contactName: String = "",
    val contactStatus: UInt = MrimConstants.STATUS_OFFLINE,
    val contactAuthorized: Boolean = true,
    val contactAvatarUrl: String = "",
    val messages: List<MessageInfo> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isTyping: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val mrimClient: MrimClient,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val contactEmail: String = savedStateHandle["email"] ?: ""
    private val _uiState = MutableStateFlow(ChatUiState(contactEmail = contactEmail))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var typingJob: Job? = null
    private var sendTypingJob: Job? = null

    init {
        // Данные контакта
        val c = mrimClient.contacts.value.find { it.email == contactEmail }
        _uiState.update {
            it.copy(
                contactName = c?.displayName ?: contactEmail,
                contactStatus = c?.status ?: MrimConstants.STATUS_OFFLINE,
                contactAuthorized = c?.authorized ?: true,
                contactAvatarUrl = mrimClient.getAvatarUrl(contactEmail, true)
            )
        }

        // Обновление контакта в реальном времени
        viewModelScope.launch {
            mrimClient.contacts.collect { cs ->
                cs.find { it.email == contactEmail }?.let { c2 ->
                    _uiState.update {
                        it.copy(
                            contactName = c2.displayName,
                            contactStatus = c2.status,
                            contactAuthorized = c2.authorized
                        )
                    }
                }
            }
        }

        // Сообщения из Room (сохранённые + новые)
        viewModelScope.launch {
            chatRepository.getMessages(contactEmail).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }

        // Помечаем как прочитанные при входе
        viewModelScope.launch {
            chatRepository.markAsRead(contactEmail)
        }

        // "Печатает..."
        viewModelScope.launch {
            mrimClient.typingNotifications.collect { from ->
                if (from == contactEmail) {
                    _uiState.update { it.copy(isTyping = true) }
                    typingJob?.cancel()
                    typingJob = viewModelScope.launch {
                        delay(5_000)
                        _uiState.update { it.copy(isTyping = false) }
                    }
                }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
        if (text.isNotEmpty() && sendTypingJob?.isActive != true) {
            sendTypingJob = viewModelScope.launch {
                chatRepository.sendTyping(contactEmail)
                delay(5_000)
            }
        }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, inputText = "") }
            chatRepository.sendMessage(contactEmail, text)
            _uiState.update { it.copy(isSending = false) }
        }
    }
}