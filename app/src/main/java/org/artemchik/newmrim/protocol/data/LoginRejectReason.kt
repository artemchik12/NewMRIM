package org.artemchik.newmrim.protocol.data

enum class LoginRejectReason(val serverText: String, val displayText: String) {
    INVALID_LOGIN("Invalid login", "Неверный логин или пароль"),
    DATABASE_ERROR("Database error", "Внутренняя ошибка сервера"),
    ACCESS_DENIED("Access denied", "Доступ запрещён"),
    BLACKLISTED_IP("Black-List IP", "IP-адрес заблокирован"),
    UNKNOWN("", "Неизвестная ошибка");

    companion object {
        fun fromServerText(text: String): LoginRejectReason =
            entries.find { text.contains(it.serverText, ignoreCase = true) } ?: UNKNOWN
    }
}
