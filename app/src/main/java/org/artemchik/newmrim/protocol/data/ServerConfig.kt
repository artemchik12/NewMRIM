package org.artemchik.newmrim.protocol.data

data class ServerConfig(
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT,
    val useRedirector: Boolean = true,
    val avatarHost: String = DEFAULT_AVATAR_HOST
) {
    companion object {
        const val DEFAULT_HOST = "proto.mrim.su"
        const val DEFAULT_PORT = 2042
        const val DEFAULT_DIRECT_PORT = 2041
        const val DEFAULT_AVATAR_HOST = "obraz.mrim.su"
        const val FALLBACK_AVATAR_HOST = "obraz.mrim.su"
    }

    fun buildAvatarUrl(email: String, small: Boolean = false): String {
        if (email.isEmpty() || !email.contains("@")) return ""
        val parts = email.split("@")
        val login = parts[0]
        val domain = parts[1].substringBeforeLast(".")
        val type = if (small) "_mrimavatarsmall" else "_mrimavatar"
        return "http://$avatarHost/$domain/$login/$type"
    }
}
