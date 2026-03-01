package org.artemchik.newmrim.protocol

import android.util.Log
import org.artemchik.newmrim.protocol.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicInteger

class MrimClient {
    companion object {
        private const val TAG = "MrimClient"
        private const val CLIENT_ID = "android-mrim"
        private const val CLIENT_VERSION = "1.0"
        private const val CLIENT_BUILD = "1"
    }

    private val connection = MrimConnection()
    private val seqCounter = AtomicInteger(0)
    private var pingJob: Job? = null
    private var pingIntervalSec: Long = 30
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pendingCredentials: Pair<String, String>? = null
    private var currentEmail: String = ""
    val currentUserEmail: String get() = currentEmail   // публичный доступ для Repository
    private var _serverConfig = ServerConfig()
    val serverConfig: ServerConfig get() = _serverConfig

    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connecting : ConnectionState()
        data object Connected : ConnectionState()
        data object LoggingIn : ConnectionState()
        data object LoggedIn : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _state
    private val _contacts = MutableStateFlow<List<ContactInfo>>(emptyList())
    val contacts: StateFlow<List<ContactInfo>> = _contacts
    private val _groups = MutableStateFlow<List<GroupInfo>>(emptyList())
    val groups: StateFlow<List<GroupInfo>> = _groups
    private val _incomingMessages = MutableSharedFlow<MessageInfo>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<MessageInfo> = _incomingMessages
    private val _typingNotifications = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val typingNotifications: SharedFlow<String> = _typingNotifications
    private val _userInfo = MutableStateFlow<Map<String, String>>(emptyMap())
    val userInfo: StateFlow<Map<String, String>> = _userInfo
    private val _messageDeliveryStatus = MutableSharedFlow<Pair<UInt, UInt>>(extraBufferCapacity = 16)
    val messageDeliveryStatus: SharedFlow<Pair<UInt, UInt>> = _messageDeliveryStatus

    init {
        scope.launch {
            connection.incomingPackets.collect { try { handlePacket(it) } catch (e: Exception) { Log.e(TAG, "Handle error", e) } }
        }
    }

    private fun nextSeq(): UInt = seqCounter.incrementAndGet().toUInt()

    suspend fun login(email: String, password: String, config: ServerConfig = ServerConfig()): Result<Unit> {
        currentEmail = email; _serverConfig = config; _state.value = ConnectionState.Connecting
        val result = connection.connect(config)
        if (result.isFailure) { _state.value = ConnectionState.Error(result.exceptionOrNull()?.message ?: "Connection failed"); return result }
        _state.value = ConnectionState.Connected; pendingCredentials = Pair(email, password)
        connection.sendPacket(MrimPacket(seq = nextSeq(), msgType = MrimConstants.MRIM_CS_HELLO))
        return Result.success(Unit)
    }

    suspend fun sendMessage(to: String, text: String): UInt {
        val seq = nextSeq()
        connection.sendPacket(MrimPacket(seq = seq, msgType = MrimConstants.MRIM_CS_MESSAGE, data = MrimPacketWriter().writeUL(0u).writeLPSAscii(to).writeLPS(text).writeEmptyLPS().toByteArray()))
        _incomingMessages.emit(MessageInfo(msgId = seq, from = currentEmail, to = to, text = text, isOutgoing = true))
        return seq
    }

    suspend fun sendTypingNotification(to: String) {
        connection.sendPacket(MrimPacket(seq = nextSeq(), msgType = MrimConstants.MRIM_CS_MESSAGE, data = MrimPacketWriter().writeUL(MrimConstants.MESSAGE_FLAG_NOTIFY).writeLPSAscii(to).writeLPS(" ").writeLPS(" ").toByteArray()))
    }

    suspend fun changeStatus(status: UserStatus, title: String = "", desc: String = "") {
        connection.sendPacket(MrimPacket(seq = nextSeq(), msgType = MrimConstants.MRIM_CS_CHANGE_STATUS, data = MrimPacketWriter().writeUL(status.value).writeLPS(status.xstatusType).writeLPS(title).writeLPS(desc).writeUL(MrimConstants.FEATURE_ALL).toByteArray()))
    }

    suspend fun authorizeContact(email: String) {
        connection.sendPacket(MrimPacket(seq = nextSeq(), msgType = MrimConstants.MRIM_CS_AUTHORIZE, data = MrimPacketWriter().writeLPSAscii(email).toByteArray()))
    }

    suspend fun addContact(email: String, nickname: String, groupIndex: Int = 0, authMessage: String = ""): UInt {
        val seq = nextSeq()
        connection.sendPacket(MrimPacket(seq = seq, msgType = MrimConstants.MRIM_CS_ADD_CONTACT, data = MrimPacketWriter().writeUL(0u).writeUL(groupIndex.toUInt()).writeLPSAscii(email).writeLPS(nickname).writeEmptyLPS().writeLPS(authMessage).toByteArray()))
        return seq
    }

    suspend fun addGroup(name: String): UInt {
        val seq = nextSeq()
        connection.sendPacket(MrimPacket(seq = seq, msgType = MrimConstants.MRIM_CS_ADD_CONTACT, data = MrimPacketWriter().writeUL(MrimConstants.CONTACT_FLAG_GROUP).writeUL(0u).writeLPS(name).writeEmptyLPS().writeEmptyLPS().writeEmptyLPS().toByteArray()))
        return seq
    }

    fun getAvatarUrl(email: String, small: Boolean = false): String = _serverConfig.buildAvatarUrl(email, small)
    fun disconnect() { pingJob?.cancel(); connection.disconnect(); _state.value = ConnectionState.Disconnected }
    fun destroy() { disconnect(); scope.cancel() }

    private suspend fun sendLogin2(email: String, password: String) {
        _state.value = ConnectionState.LoggingIn
        val d1 = "client=\"$CLIENT_ID\" version=\"$CLIENT_VERSION\" build=\"$CLIENT_BUILD\""
        val d2 = "$CLIENT_ID $CLIENT_VERSION (build $CLIENT_BUILD);"
        connection.sendPacket(MrimPacket(seq = nextSeq(), msgType = MrimConstants.MRIM_CS_LOGIN2, data = MrimPacketWriter().writeLPSAscii(email).writeLPSAscii(password).writeUL(MrimConstants.STATUS_ONLINE).writeLPS("STATUS_ONLINE").writeLPS("").writeLPS("").writeUL(MrimConstants.FEATURE_ALL).writeLPSAscii(d1).writeLPSAscii(d2).toByteArray()))
    }

    private suspend fun sendMessageRecv(from: String, msgId: UInt) {
        connection.sendPacket(MrimPacket(seq = nextSeq(), msgType = MrimConstants.MRIM_CS_MESSAGE_RECV, data = MrimPacketWriter().writeUL(msgId).writeLPSAscii(from).toByteArray()))
    }

    private fun startPinging() {
        pingJob?.cancel(); pingJob = scope.launch {
            while (isActive && connection.isConnected) {
                delay(pingIntervalSec * 1000)
                try { connection.sendPacket(MrimPacket(seq = nextSeq(), msgType = MrimConstants.MRIM_CS_PING)) }
                catch (e: Exception) { Log.e(TAG, "Ping failed", e); break }
            }
        }
    }

    private suspend fun handlePacket(packet: MrimPacket) {
        when (packet.msgType) {
            MrimConstants.MRIM_CS_HELLO_ACK -> handleHelloAck(packet)
            MrimConstants.MRIM_CS_LOGIN_ACK -> { Log.d(TAG, "LOGIN_ACK"); _state.value = ConnectionState.LoggedIn }
            MrimConstants.MRIM_CS_LOGIN_REJ -> { val r = if (packet.data.isNotEmpty()) MrimPacketReader(packet.data).readLPSAscii() else "Unknown"; _state.value = ConnectionState.Error(LoginRejectReason.fromServerText(r).displayText) }
            MrimConstants.MRIM_CS_LOGOUT -> { var reason = "Принудительный выход"; if (packet.data.size >= 4 && MrimPacketReader(packet.data).readUL() == MrimConstants.LOGOUT_OTHER_LOGIN) reason = "Вход с другого устройства"; _state.value = ConnectionState.Error(reason); connection.disconnect() }
            MrimConstants.MRIM_CS_USER_INFO -> handleUserInfo(packet)
            MrimConstants.MRIM_CS_CONTACT_LIST2 -> handleContactList2(packet)
            MrimConstants.MRIM_CS_USER_STATUS -> handleUserStatus(packet)
            MrimConstants.MRIM_CS_MESSAGE_ACK -> handleMessageAck(packet)
            MrimConstants.MRIM_CS_MESSAGE_STATUS -> _messageDeliveryStatus.emit(Pair(packet.seq, MrimPacketReader(packet.data).readUL()))
            MrimConstants.MRIM_CS_AUTHORIZE_ACK -> { val e = MrimPacketReader(packet.data).readLPSAscii(); _contacts.value = _contacts.value.map { if (it.email == e) it.copy(authorized = true) else it } }
            MrimConstants.MRIM_CS_ADD_CONTACT_ACK -> { val r = MrimPacketReader(packet.data); Log.d(TAG, "ADD_ACK: err=0x${r.readUL().toString(16)} idx=${r.readUL()}") }
            MrimConstants.MRIM_CS_MODIFY_CONTACT_ACK -> Log.d(TAG, "MODIFY_ACK: 0x${MrimPacketReader(packet.data).readUL().toString(16)}")
            else -> Log.w(TAG, "Unhandled: 0x${packet.msgType.toString(16)}")
        }
    }

    private suspend fun handleHelloAck(packet: MrimPacket) {
        if (packet.data.size >= 4) { pingIntervalSec = MrimPacketReader(packet.data).readUL().toLong(); Log.d(TAG, "HELLO_ACK: ping=${pingIntervalSec}s") }
        startPinging(); pendingCredentials?.let { (e, p) -> sendLogin2(e, p); pendingCredentials = null }
    }

    private fun handleUserInfo(packet: MrimPacket) {
        val r = MrimPacketReader(packet.data); val info = mutableMapOf<String, String>()
        while (r.hasRemaining() && r.remaining >= 4) { try { val k = r.readLPSAscii(); val v = r.readLPS(); if (k.isNotEmpty()) info[k] = v } catch (_: Exception) { break } }
        _userInfo.value = info; Log.d(TAG, "USER_INFO: $info")
    }

    private fun handleContactList2(packet: MrimPacket) {
        if (packet.data.isEmpty()) return
        try {
            val r = MrimPacketReader(packet.data); val err = r.readUL()
            if (err != MrimConstants.CONTACT_OPER_SUCCESS) { Log.e(TAG, "CL2 err: 0x${err.toString(16)}"); return }
            val gc = r.readUL().toInt(); val gm = r.readLPSAscii(); val cm = r.readLPSAscii()
            Log.d(TAG, "CL2: groups=$gc, groupMask=$gm, contactMask=$cm")

            val groups = mutableListOf<GroupInfo>()
            for (i in 0 until gc) { parseGroup(r, gm, i)?.let { groups.add(it) } }
            _groups.value = groups

            val contacts = mutableListOf<ContactInfo>(); var id = 0
            while (r.hasRemaining() && r.remaining >= 8) {
                try { parseContact(r, cm, id)?.let { contacts.add(it) }; id++ }
                catch (_: Exception) { break }
            }
            _contacts.value = contacts
            Log.d(TAG, "Loaded ${groups.size} groups, ${contacts.size} contacts")
        } catch (e: Exception) { Log.e(TAG, "CL2 parse error", e) }
    }

    private fun parseGroup(r: MrimPacketReader, mask: String, index: Int): GroupInfo? {
        var flags = 0u; var name = ""
        for (i in mask.indices) when {
            mask[i] == 'u' && i == 0 -> flags = r.readUL()
            mask[i] == 's' && i == 1 -> name = r.readLPS()
            mask[i] == 'u' -> r.readUL()
            mask[i] == 's' -> r.readLPS()
        }
        return GroupInfo(index, flags, name)
    }

    private fun parseContact(r: MrimPacketReader, mask: String, id: Int): ContactInfo? {
        val uls = mutableListOf<UInt>(); val strs = mutableListOf<String>()
        for (ch in mask) when (ch) {
            'u' -> uls.add(r.readUL())
            's' -> strs.add(if (strs.isEmpty()) r.readLPSAscii() else r.readLPS())
        }
        val flags = uls.getOrElse(0) { 0u }
        if ((flags and MrimConstants.CONTACT_FLAG_GROUP) != 0u) return null

        return ContactInfo(
            id = id,
            flags = flags,
            groupId = uls.getOrElse(1) { 0u }.toInt(),
            email = strs.getOrElse(0) { "" },
            nickname = strs.getOrElse(1) { "" }.ifEmpty { strs.getOrElse(0) { "" } },
            // ═══ FIX: по документации 0 = авторизован, 1 = не авторизован ═══
            authorized = uls.getOrElse(2) { 0u } == 0u,
            status = uls.getOrElse(3) { MrimConstants.STATUS_OFFLINE },
            phone = strs.getOrElse(2) { "" },
            xstatusValue = strs.getOrElse(3) { "" },
            xstatusTitle = strs.getOrElse(4) { "" },
            xstatusDesc = strs.getOrElse(5) { "" },
            featuresMask = uls.getOrElse(4) { 0u },
            useragent = strs.getOrElse(6) { "" }
        )
    }

    private fun handleUserStatus(packet: MrimPacket) {
        val r = MrimPacketReader(packet.data); val status = r.readUL()
        var email: String; var xv = ""; var xt = ""; var xd = ""; var feat = 0u; var ua = ""
        try { xv = r.readLPS(); xt = r.readLPS(); xd = r.readLPS(); email = r.readLPSAscii()
            if (r.hasRemaining() && r.remaining >= 4) feat = r.readUL()
            if (r.hasRemaining() && r.remaining >= 4) ua = r.readLPSAscii()
        } catch (_: Exception) { email = xv; xv = "" }
        _contacts.value = _contacts.value.map { if (it.email == email) it.copy(status = status, xstatusValue = xv, xstatusTitle = xt, xstatusDesc = xd, featuresMask = feat, useragent = ua) else it }
    }

    private suspend fun handleMessageAck(packet: MrimPacket) {
        val r = MrimPacketReader(packet.data); val msgId = r.readUL(); val flags = r.readUL()
        val from = r.readLPSAscii(); val text = r.readLPS()
        val rtf = if (r.hasRemaining() && r.remaining >= 4) r.readLPSAscii() else ""
        if ((flags and MrimConstants.MESSAGE_FLAG_NOTIFY) != 0u) { _typingNotifications.emit(from); return }
        val msg = MessageInfo(msgId = msgId, from = from, to = currentEmail, text = text, rtfText = rtf, flags = flags, isOutgoing = false)
        _incomingMessages.emit(msg)
        if (msg.needsRecvAck) sendMessageRecv(from, msgId)
    }
}