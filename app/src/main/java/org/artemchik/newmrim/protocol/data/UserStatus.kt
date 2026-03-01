package org.artemchik.newmrim.protocol.data

import org.artemchik.newmrim.protocol.MrimConstants

enum class UserStatus(val value: UInt, val xstatusType: String) {
    ONLINE(MrimConstants.STATUS_ONLINE, "STATUS_ONLINE"),
    AWAY(MrimConstants.STATUS_AWAY, "STATUS_AWAY"),
    DND(MrimConstants.STATUS_XSTATUS, "STATUS_DND"),
    CHAT(MrimConstants.STATUS_ONLINE, "status_chat"),
    INVISIBLE(MrimConstants.STATUS_INVISIBLE, "STATUS_ONLINE"),
    OFFLINE(MrimConstants.STATUS_OFFLINE, "");

    companion object {
        fun fromValue(value: UInt): UserStatus = when {
            value == MrimConstants.STATUS_INVISIBLE -> INVISIBLE
            value == MrimConstants.STATUS_AWAY -> AWAY
            value == MrimConstants.STATUS_ONLINE -> ONLINE
            value == MrimConstants.STATUS_XSTATUS -> DND
            else -> OFFLINE
        }
    }
}
