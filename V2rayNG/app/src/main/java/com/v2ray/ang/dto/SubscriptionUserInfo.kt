package com.v2ray.ang.dto

/**
 * Разобранный заголовок `Subscription-Userinfo`, например:
 * `upload=0; download=65573160168; total=429496729600; expire=1787722200`
 */
data class SubscriptionUserInfo(
    val upload: Long = 0,
    val download: Long = 0,
    val total: Long = 0,   // 0 = безлимит
    val expire: Long = 0,  // unix-секунды, 0 = без срока
) {
    companion object {
        private val KNOWN = setOf("upload", "download", "total", "expire")

        fun parse(header: String?): SubscriptionUserInfo? {
            if (header.isNullOrBlank()) return null
            val map = header.split(';')
                .mapNotNull { part ->
                    val kv = part.split('=', limit = 2)
                    if (kv.size == 2) kv[0].trim().lowercase() to kv[1].trim() else null
                }
                .toMap()
            if (map.keys.none { it in KNOWN }) return null
            return SubscriptionUserInfo(
                upload = map["upload"]?.toLongOrNull() ?: 0,
                download = map["download"]?.toLongOrNull() ?: 0,
                total = map["total"]?.toLongOrNull() ?: 0,
                expire = map["expire"]?.toLongOrNull() ?: 0,
            )
        }
    }
}
