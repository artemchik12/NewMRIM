package org.artemchik.newmrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.artemchik.newmrim.db.LastMessage
import org.artemchik.newmrim.protocol.MrimConstants
import org.artemchik.newmrim.protocol.data.ContactInfo
import org.artemchik.newmrim.protocol.data.UserStatus
import org.artemchik.newmrim.ui.theme.*
import org.artemchik.newmrim.viewmodel.ContactListViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    onContactClick: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: ContactListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showStatusMenu by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                MediumTopAppBar(
                    title = {
                        Column {
                            Text(
                                uiState.nickname.ifEmpty { "Чаты" },
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.unreadMail > 0) {
                                Text(
                                    "✉ ${uiState.unreadMail} новых писем",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showStatusMenu = true }) {
                            val statusColor = when (uiState.isConnected) {
                                true -> OnlineGreen
                                false -> OfflineGray
                            }
                            Icon(Icons.Filled.Circle, "Status", tint = statusColor, modifier = Modifier.size(16.dp))
                        }
                        
                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            StatusMenuItem("Онлайн", OnlineGreen) { viewModel.changeStatus(UserStatus.ONLINE); showStatusMenu = false }
                            StatusMenuItem("Отошёл", AwayOrange) { viewModel.changeStatus(UserStatus.AWAY); showStatusMenu = false }
                            StatusMenuItem("Не беспокоить", Color.Red) { viewModel.changeStatus(UserStatus.DND); showStatusMenu = false }
                            StatusMenuItem("Невидимка", OfflineGray) { viewModel.changeStatus(UserStatus.INVISIBLE); showStatusMenu = false }
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Выход") },
                                leadingIcon = { Icon(Icons.Outlined.Logout, null) },
                                onClick = { viewModel.logout(); onLogout(); showStatusMenu = false }
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
                
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onSearch = { searchActive = false },
                    active = searchActive,
                    onActiveChange = { searchActive = it },
                    placeholder = { Text("Поиск контактов...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (searchActive) 0.dp else 16.dp)
                        .padding(bottom = if (searchActive) 0.dp else 8.dp)
                ) {
                    ContactListContent(uiState, viewModel, onContactClick)
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddContactDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = { Icon(Icons.Default.PersonAdd, "Add Contact") },
                text = { Text("Новый чат") }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            ContactListContent(uiState, viewModel, onContactClick)
        }
    }

    if (showAddContactDialog) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onAdd = { email -> 
                // В будущем здесь будет вызов viewModel.addContact(email)
                showAddContactDialog = false
                onContactClick(email)
            }
        )
    }
}

@Composable
private fun ContactListContent(
    uiState: org.artemchik.newmrim.viewmodel.ContactListUiState,
    viewModel: ContactListViewModel,
    onContactClick: (String) -> Unit
) {
    if (uiState.contacts.isEmpty() && uiState.searchQuery.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Загрузка списка...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else if (uiState.contacts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ничего не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            val withUnread = uiState.contacts.filter { (uiState.unreadCounts[it.email] ?: 0) > 0 }
            val online = uiState.contacts.filter { it.status != MrimConstants.STATUS_OFFLINE && (uiState.unreadCounts[it.email] ?: 0) == 0 }
            val offline = uiState.contacts.filter { it.status == MrimConstants.STATUS_OFFLINE && (uiState.unreadCounts[it.email] ?: 0) == 0 }

            if (withUnread.isNotEmpty()) {
                item { SectionHeader("Новые") }
                items(withUnread, key = { it.email }) { c ->
                    ContactItem(c, uiState.unreadCounts[c.email] ?: 0, viewModel.getAvatarUrl(c.email), uiState.lastMessages[c.email]) {
                        viewModel.clearUnread(c.email); onContactClick(c.email)
                    }
                }
            }
            if (online.isNotEmpty()) {
                item { SectionHeader("В сети") }
                items(online, key = { it.email }) { c ->
                    ContactItem(c, 0, viewModel.getAvatarUrl(c.email), uiState.lastMessages[c.email]) { onContactClick(c.email) }
                }
            }
            if (offline.isNotEmpty()) {
                item { SectionHeader("Оффлайн") }
                items(offline, key = { it.email }) { c ->
                    ContactItem(c, 0, viewModel.getAvatarUrl(c.email), uiState.lastMessages[c.email]) { onContactClick(c.email) }
                }
            }
            item { Spacer(Modifier.height(80.dp)) } // Отступ для FAB
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun StatusMenuItem(text: String, color: Color, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(Icons.Filled.Circle, null, tint = color, modifier = Modifier.size(12.dp)) },
        onClick = onClick
    )
}

@Composable
private fun ContactItem(
    contact: ContactInfo,
    unreadCount: Int,
    avatarUrl: String,
    lastMessage: LastMessage?,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                contact.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
            )
        },
        supportingContent = {
            val text = lastMessage?.text ?: contact.email
            Text(
                if (lastMessage?.isOutgoing == true) "Вы: $text" else text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = if (unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Box {
                if (avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                contact.displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                val statusColor = when (contact.status) {
                    MrimConstants.STATUS_ONLINE -> OnlineGreen
                    MrimConstants.STATUS_AWAY -> AwayOrange
                    MrimConstants.STATUS_XSTATUS -> OnlineGreen
                    else -> Color.Transparent
                }
                
                if (statusColor != Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(2.dp)
                            .background(statusColor, CircleShape)
                    )
                }
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                if (lastMessage != null) {
                    Text(
                        formatMessageTime(lastMessage.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("$unreadCount", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    )
}

@Composable
private fun AddContactDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый чат") },
        text = {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email контакта") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (email.isNotBlank()) onAdd(email) },
                enabled = email.contains("@")
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun formatMessageTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }
    return if (now.get(Calendar.DATE) == time.get(Calendar.DATE)) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    } else {
        SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(timestamp))
    }
}