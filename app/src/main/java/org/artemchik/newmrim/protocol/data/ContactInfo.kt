package org.artemchik.newmrim.protocol.data

import org.artemchik.newmrim.protocol.MrimConstants

data class ContactInfo(
    val id: Int,
    val flags: UInt,
    val groupId: Int,
    val email: String,
    val nickname: String,
    val authorized: Boolean = false,
    val status: UInt = MrimConstants.STATUS_OFFLINE,
    val phone: String = "",
    val xstatusValue: String = "",
    val xstatusTitle: String = "",
    val xstatusDesc: String = "",
    val featuresMask: UInt = 0u,
    val useragent: String = ""
) {
    val isGroup: Boolean get() = (flags and MrimConstants.CONTACT_FLAG_GROUP) != 0u
    val isPhone: Boolean get() = (flags and MrimConstants.CONTACT_FLAG_PHONE) != 0u
    val isIgnored: Boolean get() = (flags and MrimConstants.CONTACT_FLAG_IGNORE) != 0u
    val displayName: String get() = nickname.ifEmpty { email }
    val userStatus: UserStatus get() = if (!authorized) UserStatus.OFFLINE else UserStatus.fromValue(status)
}
