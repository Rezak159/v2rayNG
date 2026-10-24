package com.v2ray.ang.handler

/**
 * Вычищает из диагностических отчётов всё, что может выдать сервер или пользователя.
 *
 * Логи ядра и сообщения об ошибках содержат адреса серверов, SNI, UUID из конфигов и
 * ссылки на подписки. Такие данные не должны покидать устройство ни при каком согласии:
 * утечка деанонимизирует и пользователя, и наши серверы. Поэтому чистим текст, а не
 * полагаемся на то, что «сюда ничего чувствительного не попадёт».
 */
object DiagnosticsScrubber {

    private const val MAX_LENGTH = 4_000

    /** Ссылки на профили и подписки — вырезаем целиком, оставляя только схему. */
    private val SHARE_LINK = Regex(
        "(?i)\\b(vmess|vless|trojan|ss|ssr|hysteria2?|hy2|tuic|wireguard|wg|socks5?|https?)://\\S+"
    )

    /** `sni=example.com`, `"address": "1.2.3.4"`, `password=…` в дампах конфигов. */
    private val SENSITIVE_FIELD = Regex(
        "(?i)\\b(sni|host|hostname|address|add|server|domain|peer|endpoint|remarks|ps|email|" +
            "user|username|password|pass|pwd|uuid|id|key|token|psk|publickey|privatekey|" +
            "presharedkey|shortid|servicename|path|flow|alpn|fingerprint)" +
            "(\"?\\s*[=:]\\s*\"?)([^\\s\",}\\]]+)"
    )

    private val UUID = Regex(
        "(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b"
    )

    private val IPV4 = Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d{1,5})?\\b")

    /** Три и больше групп с двоеточиями: в отличие от `12:34:56` это уже не время. */
    private val IPV6 = Regex("\\b(?:[0-9a-fA-F]{1,4}:){3,}[0-9a-fA-F]{1,4}\\b")

    private val HOST_PORT = Regex(
        "\\b(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}:\\d{1,5}\\b"
    )

    /** `CoreServiceManager.kt:120` неотличим от `host:port` — держим список расширений. */
    private val SOURCE_FILE_SUFFIX = Regex(
        "(?i)\\.(kt|kts|java|xml|json|so|aar|jar|txt|md|log|properties|gradle|png|webp):\\d+$"
    )

    /** Длинные base64-блоки: ключи, сериализованные конфиги, содержимое подписки. */
    private val BLOB = Regex("\\b[A-Za-z0-9+/]{40,}={0,2}\\b")

    fun redact(text: String?): String? {
        if (text.isNullOrEmpty()) return text

        var result = text
        result = SHARE_LINK.replace(result) { "${it.groupValues[1]}://<redacted>" }
        result = SENSITIVE_FIELD.replace(result) { "${it.groupValues[1]}${it.groupValues[2]}<redacted>" }
        result = BLOB.replace(result, "<blob>")
        result = UUID.replace(result, "<uuid>")
        result = HOST_PORT.replace(result) { match ->
            if (SOURCE_FILE_SUFFIX.containsMatchIn(match.value)) match.value else "<host>:<port>"
        }
        result = IPV4.replace(result, "<ip>")
        result = IPV6.replace(result, "<ip>")

        return if (result.length > MAX_LENGTH) result.take(MAX_LENGTH) + "…<truncated>" else result
    }
}
