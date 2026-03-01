package org.artemchik.newmrim.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.artemchik.newmrim.data.SettingsDataStore
import org.artemchik.newmrim.protocol.MrimClient
import org.artemchik.newmrim.protocol.data.ServerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val rememberPassword: Boolean = false,
    val serverHost: String = ServerConfig.DEFAULT_HOST,
    val serverPort: String = ServerConfig.DEFAULT_PORT.toString(),
    val useRedirector: Boolean = true,
    val avatarHost: String = ServerConfig.DEFAULT_AVATAR_HOST,
    val showServerSettings: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val mrimClient: MrimClient,
    private val settingsStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // Загружаем настройки сервера
        viewModelScope.launch {
            settingsStore.serverConfig.first().let { c ->
                _uiState.update {
                    it.copy(
                        serverHost = c.host,
                        serverPort = c.port.toString(),
                        useRedirector = c.useRedirector,
                        avatarHost = c.avatarHost
                    )
                }
            }
        }

        // Загружаем email
        viewModelScope.launch {
            settingsStore.lastEmail.first().let { e ->
                if (e.isNotEmpty()) _uiState.update { it.copy(email = e) }
            }
        }

        // Загружаем сохранённый пароль
        viewModelScope.launch {
            val remember = settingsStore.rememberPassword.first()
            val password = settingsStore.savedPassword.first()
            _uiState.update {
                it.copy(
                    rememberPassword = remember,
                    password = if (remember) password else ""
                )
            }
        }


        viewModelScope.launch {
            mrimClient.connectionState.collect { state ->
                when (state) {
                    is MrimClient.ConnectionState.LoggedIn ->
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                    is MrimClient.ConnectionState.Error ->
                        _uiState.update { it.copy(isLoading = false, error = state.message) }
                    is MrimClient.ConnectionState.Connecting,
                    is MrimClient.ConnectionState.LoggingIn ->
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    else -> {}
                }
            }
        }
    }

    fun onEmailChanged(v: String) = _uiState.update { it.copy(email = v, error = null) }
    fun onPasswordChanged(v: String) = _uiState.update { it.copy(password = v, error = null) }
    fun onRememberPasswordChanged(v: Boolean) = _uiState.update { it.copy(rememberPassword = v) }
    fun toggleServerSettings() = _uiState.update { it.copy(showServerSettings = !it.showServerSettings) }
    fun onServerHostChanged(v: String) = _uiState.update { it.copy(serverHost = v) }
    fun onServerPortChanged(v: String) = _uiState.update { it.copy(serverPort = v.filter { c -> c.isDigit() }) }
    fun onUseRedirectorChanged(v: Boolean) = _uiState.update {
        it.copy(useRedirector = v, serverPort = if (v) ServerConfig.DEFAULT_PORT.toString() else ServerConfig.DEFAULT_DIRECT_PORT.toString())
    }
    fun onAvatarHostChanged(v: String) = _uiState.update { it.copy(avatarHost = v) }
    fun resetToDefaults() = _uiState.update {
        it.copy(serverHost = ServerConfig.DEFAULT_HOST, serverPort = ServerConfig.DEFAULT_PORT.toString(),
            useRedirector = true, avatarHost = ServerConfig.DEFAULT_AVATAR_HOST)
    }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun login() {
        val s = _uiState.value
        if (s.email.isBlank() || s.password.isBlank()) { _uiState.update { it.copy(error = "Заполните логин и пароль") }; return }
        val port = s.serverPort.toIntOrNull()
        if (port == null || port !in 1..65535) { _uiState.update { it.copy(error = "Некорректный порт (1-65535)") }; return }
        if (s.serverHost.isBlank()) { _uiState.update { it.copy(error = "Укажите адрес сервера") }; return }

        val config = ServerConfig(s.serverHost.trim(), port, s.useRedirector,
            s.avatarHost.trim().ifEmpty { ServerConfig.DEFAULT_AVATAR_HOST })

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            settingsStore.saveServerConfig(config)
            settingsStore.saveCredentials(s.email.trim(), s.password, s.rememberPassword)
            mrimClient.login(s.email.trim(), s.password, config)
        }
    }
}