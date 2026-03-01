package org.artemchik.newmrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.artemchik.newmrim.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }
    val passwordFocus = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isLoggedIn) { if (uiState.isLoggedIn) onLoginSuccess() }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Card(
                Modifier.widthIn(max = 420.dp).padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(scrollState).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Заголовок
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Chat, null,
                                Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        "NewMRIM",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        "Вход в Mail.Ru Агент",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(4.dp))

                    // Email
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChanged,
                        label = { Text("Email") },
                        placeholder = { Text("user@mail.ru") },
                        leadingIcon = { Icon(Icons.Outlined.Email, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocus.requestFocus() }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    )

                    // Пароль
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChanged,
                        label = { Text("Пароль") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    "Показать/скрыть"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { viewModel.login() }),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().focusRequester(passwordFocus),
                        enabled = !uiState.isLoading
                    )

                    // Запомнить пароль
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.rememberPassword,
                            onCheckedChange = viewModel::onRememberPasswordChanged,
                            enabled = !uiState.isLoading
                        )
                        Text(
                            "Запомнить пароль",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // Настройки сервера
                    ServerSettingsSection(
                        expanded = uiState.showServerSettings,
                        onToggle = viewModel::toggleServerSettings,
                        host = uiState.serverHost,
                        port = uiState.serverPort,
                        useRedirector = uiState.useRedirector,
                        avatarHost = uiState.avatarHost,
                        onHostChanged = viewModel::onServerHostChanged,
                        onPortChanged = viewModel::onServerPortChanged,
                        onUseRedirectorChanged = viewModel::onUseRedirectorChanged,
                        onAvatarHostChanged = viewModel::onAvatarHostChanged,
                        onResetDefaults = viewModel::resetToDefaults,
                        enabled = !uiState.isLoading
                    )

                    // Кнопка входа
                    Button(
                        onClick = viewModel::login,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isLoading
                    ) {
                        AnimatedContent(uiState.isLoading, label = "btn") { loading ->
                            if (loading) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Text("Подключение...")
                                }
                            } else {
                                Text("Войти", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }

              //      Text(
                //        "Пароль передаётся открытым текстом\n(особенность протокола MRIM)",
                  //      style = MaterialTheme.typography.labelSmall,
                    //    color = MaterialTheme.colorScheme.onSurfaceVariant,
                      //  textAlign = TextAlign.Center
                   // )
                }
            }
        }
    }
}

@Composable
private fun ServerSettingsSection(
    expanded: Boolean, onToggle: () -> Unit,
    host: String, port: String, useRedirector: Boolean, avatarHost: String,
    onHostChanged: (String) -> Unit, onPortChanged: (String) -> Unit,
    onUseRedirectorChanged: (Boolean) -> Unit, onAvatarHostChanged: (String) -> Unit,
    onResetDefaults: () -> Unit, enabled: Boolean
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Settings, null, Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Настройки сервера",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                null, tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    AnimatedVisibility(
        expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Перенаправление", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (useRedirector) "Порт 2042 → оптимальный сервер"
                            else "Прямое подключение",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = useRedirector, onCheckedChange = onUseRedirectorChanged, enabled = enabled)
                }

                OutlinedTextField(
                    value = host, onValueChange = onHostChanged,
                    label = { Text("Хост") }, placeholder = { Text("mrim.mail.ru") },
                    leadingIcon = { Icon(Icons.Outlined.Dns, null, Modifier.size(20.dp)) },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(), enabled = enabled
                )

                OutlinedTextField(
                    value = port, onValueChange = onPortChanged,
                    label = { Text(if (useRedirector) "Порт (перенаправляющий)" else "Порт (прямой)") },
                    placeholder = { Text(if (useRedirector) "2042" else "2041") },
                    leadingIcon = { Icon(Icons.Outlined.Tag, null, Modifier.size(20.dp)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), enabled = enabled
                )

                HorizontalDivider(
                    Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text("Сервер аватарок", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = avatarHost, onValueChange = onAvatarHostChanged,
                    label = { Text("Хост аватарок") }, placeholder = { Text("obraz.foto.mail.ru") },
                    leadingIcon = { Icon(Icons.Outlined.Image, null, Modifier.size(20.dp)) },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(), enabled = enabled,
                    supportingText = { Text("их нет они пропали") }
                )

                OutlinedButton(
                    onClick = onResetDefaults, Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), enabled = enabled
                ) {
                    Icon(Icons.Outlined.RestartAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Сбросить по умолчанию")
                }
            }
        }
    }
}