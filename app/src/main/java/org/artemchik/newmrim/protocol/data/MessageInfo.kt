package org.artemchik.newmrim.protocol.data

import org.artemchik.newmrim.protocol.MrimConstants

data class MessageInfo(
    val msgId: UInt = 0u,
    val from: String,
    val to: String,
    val text: String,
    val rtfText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val flags: UInt = 0u,
    val isOutgoing: Boolean = false,
    val isDelivered: Boolean = false
) {
    val isOffline: Boolean get() = (flags and MrimConstants.MESSAGE_FLAG_OFFLINE) != 0u
    val isAuthRequest: Boolean get() = (flags and MrimConstants.MESSAGE_FLAG_AUTHORIZE) != 0u
    val isTypingNotification: Boolean get() = (flags and MrimConstants.MESSAGE_FLAG_NOTIFY) != 0u
    val isSystem: Boolean get() = (flags and MrimConstants.MESSAGE_FLAG_SYSTEM) != 0u
    val hasRtf: Boolean get() = (flags and MrimConstants.MESSAGE_FLAG_RTF) != 0u
    val needsRecvAck: Boolean get() = (flags and MrimConstants.MESSAGE_FLAG_NORECV) == 0u
}
