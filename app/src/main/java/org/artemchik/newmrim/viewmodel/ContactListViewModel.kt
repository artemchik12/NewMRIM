package org.artemchik.newmrim.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.artemchik.newmrim.db.LastMessage
import org.artemchik.newmrim.protocol.MrimClient
import org.artemchik.newmrim.protocol.MrimConstants
import org.artemchik.newmrim.protocol.data.ContactInfo
import org.artemchik.newmrim.protocol.data.UserStatus
import org.artemchik.newmrim.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactListUiState(
    val contacts: List<ContactInfo> = emptyList(),
    val searchQuery: String = "",
    val isConnected: Boolean = false,
    val nickname: String = "",
    val unreadMail: Int = 0,
    val unreadCounts: Map<String, Int> = emptyMap(),
    val lastMessages: Map<String, LastMessage> = emptyMap()
)

@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val mrimClient: MrimClient,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactListUiState())
    val uiState: StateFlow<ContactListUiState> = _uiState.asStateFlow()

    init {
        // Контакты
        viewModelScope.launch {
            mrimClient.contacts.collect { all ->
                updateContactList(all, _uiState.value.searchQuery)
            }
        }

        // Состояние подключения
        viewModelScope.launch {
            mrimClient.connectionState.collect { cs ->
                _uiState.update { it.copy(isConnected = cs is MrimClient.ConnectionState.LoggedIn) }
            }
        }

        // Информация о пользователе
        viewModelScope.launch {
            mrimClient.userInfo.collect { info ->
                _uiState.update {
                    it.copy(
                        nickname = info["MRIM.NICKNAME"] ?: "",
                        unreadMail = info["MESSAGES.UNREAD"]?.toIntOrNull() ?: 0
                    )
                }
            }
        }

        // Непрочитанные (из Room)
        viewModelScope.launch {
            chatRepository.getUnreadCounts().collect { counts ->
                _uiState.update { it.copy(unreadCounts = counts) }
                updateContactList(mrimClient.contacts.value, _uiState.value.searchQuery)
            }
        }

        // Последние сообщения (из Room)
        viewModelScope.launch {
            chatRepository.getLastMessages().collect { msgs ->
                _uiState.update { it.copy(lastMessages = msgs) }
            }
        }
    }

    private fun updateContactList(all: List<ContactInfo>, query: String) {
        _uiState.update { s ->
            s.copy(
                contacts = all
                    .filter { !it.isGroup }
                    .filter { matchesSearch(it, query) }
                    .sortedWith(compareByDescending<ContactInfo> {
                        s.unreadCounts.getOrDefault(it.email, 0) > 0
                    }.thenByDescending {
                        it.status != MrimConstants.STATUS_OFFLINE
                    }.thenBy { it.displayName })
            )
        }
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
        updateContactList(mrimClient.contacts.value, q)
    }

    fun addContact(email: String) {
        viewModelScope.launch {
            mrimClient.addContact(email, email.substringBefore("@"))
        }
    }

    fun clearUnread(email: String) {
        viewModelScope.launch { chatRepository.markAsRead(email) }
    }

    fun changeStatus(status: UserStatus) {
        viewModelScope.launch { mrimClient.changeStatus(status) }
    }

    fun logout() = mrimClient.disconnect()

    fun getAvatarUrl(email: String, small: Boolean = true) = mrimClient.getAvatarUrl(email, small)

    private fun matchesSearch(c: ContactInfo, q: String): Boolean {
        if (q.isBlank()) return true
        val l = q.lowercase()
        return c.email.lowercase().contains(l) || c.nickname.lowercase().contains(l)
    }
}