package org.artemchik.newmrim.protocol

object MrimConstants {
    const val RESOLVER_HOST = "mrim.mail.ru"
    const val RESOLVER_PORT = 2042
    const val CS_MAGIC = 0xDEADBEEFu
    const val PROTO_VERSION_MAJOR = 1
    const val PROTO_VERSION_MINOR = 15
    const val HEADER_SIZE = 44

    const val MRIM_CS_HELLO              = 0x1001u
    const val MRIM_CS_HELLO_ACK          = 0x1002u
    const val MRIM_CS_LOGIN              = 0x1003u
    const val MRIM_CS_LOGIN_ACK          = 0x1004u
    const val MRIM_CS_LOGIN_REJ          = 0x1005u
    const val MRIM_CS_PING               = 0x1006u
    const val MRIM_CS_MESSAGE            = 0x1008u
    const val MRIM_CS_MESSAGE_ACK        = 0x1009u
    const val MRIM_CS_CONTACT_LIST       = 0x100Au
    const val MRIM_CS_CONTACT_LIST_ACK   = 0x100Bu
    const val MRIM_CS_USER_STATUS        = 0x100Fu
    const val MRIM_CS_MESSAGE_RECV       = 0x1011u
    const val MRIM_CS_MESSAGE_STATUS     = 0x1012u
    const val MRIM_CS_LOGOUT             = 0x1013u
    const val MRIM_CS_USER_INFO          = 0x1015u
    const val MRIM_CS_ADD_CONTACT        = 0x1019u
    const val MRIM_CS_ADD_CONTACT_ACK    = 0x101Au
    const val MRIM_CS_MODIFY_CONTACT     = 0x101Bu
    const val MRIM_CS_MODIFY_CONTACT_ACK = 0x101Cu
    const val MRIM_CS_AUTHORIZE          = 0x1020u
    const val MRIM_CS_AUTHORIZE_ACK      = 0x1021u
    const val MRIM_CS_CHANGE_STATUS      = 0x1022u
    const val MRIM_CS_CONTACT_LIST2      = 0x1037u
    const val MRIM_CS_LOGIN2             = 0x1038u
    const val MRIM_CS_SSL                = 0x1086u
    const val MRIM_CS_OK                 = 0x1087u
    const val MRIM_CS_FAILURE            = 0x1088u
    const val MRIM_CS_COMPRESS           = 0x1089u

    const val MESSAGE_FLAG_OFFLINE       = 0x00000001u
    const val MESSAGE_FLAG_NORECV        = 0x00000004u
    const val MESSAGE_FLAG_AUTHORIZE     = 0x00000008u
    const val MESSAGE_FLAG_SYSTEM        = 0x00000040u
    const val MESSAGE_FLAG_RTF           = 0x00000080u
    const val MESSAGE_FLAG_CONTACT       = 0x00000200u
    const val MESSAGE_FLAG_NOTIFY        = 0x00000400u
    const val MESSAGE_FLAG_MULTICAST     = 0x00001000u
    const val MESSAGE_FLAG_ALARM         = 0x00004000u
    const val MESSAGE_FLAG_FLASH         = 0x00008000u

    const val STATUS_OFFLINE             = 0x00000000u
    const val STATUS_ONLINE              = 0x00000001u
    const val STATUS_AWAY                = 0x00000002u
    const val STATUS_XSTATUS             = 0x00000004u
    const val STATUS_INVISIBLE           = 0x80000001u

    const val CONTACT_FLAG_GROUP         = 0x00000002u
    const val CONTACT_FLAG_INVISIBLE_FOR = 0x00000004u
    const val CONTACT_FLAG_VISIBLE_FOR   = 0x00000008u
    const val CONTACT_FLAG_IGNORE        = 0x00000010u
    const val CONTACT_FLAG_SHADOW        = 0x00000020u
    const val CONTACT_FLAG_AUTHORIZED    = 0x00000040u
    const val CONTACT_FLAG_CONFERENCE    = 0x00000080u
    const val CONTACT_FLAG_UNICODE       = 0x00000200u
    const val CONTACT_FLAG_PHONE         = 0x00100000u

    const val FEATURE_ALL                = 0x3FFu

    const val CONTACT_OPER_SUCCESS       = 0x00u
    const val CONTACT_OPER_ERROR         = 0x01u
    const val CONTACT_OPER_INTERR        = 0x02u
    const val CONTACT_OPER_NO_SUCH_USER  = 0x03u
    const val CONTACT_OPER_INVALID_INFO  = 0x04u
    const val CONTACT_OPER_USER_EXISTS   = 0x05u
    const val CONTACT_OPER_GROUP_LIMIT   = 0x06u

    const val MESSAGE_DELIVERED          = 0x0000u
    const val MESSAGE_REJECTED_NOUSER    = 0x8001u
    const val MESSAGE_REJECTED_INTERR    = 0x8003u
    const val MESSAGE_REJECTED_LIMIT     = 0x8004u
    const val MESSAGE_REJECTED_TOO_LARGE = 0x8005u
    const val MESSAGE_REJECTED_DENY_OFF  = 0x8006u

    const val LOGOUT_OTHER_LOGIN         = 0x10u

    const val AVATAR_HOST                = "obraz.foto.mail.ru"
    const val AVATAR_HOST_MIRROR         = "buddyicon.foto.mail.ru"
}
