package org.artemchik.newmrim.protocol.data

import org.artemchik.newmrim.protocol.MrimConstants

data class GroupInfo(
    val index: Int,
    val flags: UInt,
    val name: String
) {
    val isValid: Boolean get() = (flags and MrimConstants.CONTACT_FLAG_GROUP) != 0u
}
