package com.v2ray.ang.util

import android.net.Uri
import android.util.Base64
import com.v2ray.ang.AppConfig
import java.security.MessageDigest

/**
 * Единая проверка ссылок на подписку A4VPN.
 *
 * Принимаем только два своих формата:
 *  - прямая ссылка на [AppConfig.SUB_KEY_HOST];
 *  - кликабельная ссылка `https://<APP_LINK_HOST><APP_LINK_SUB_PATH><base64>`,
 *    внутри которой лежит та же прямая ссылка.
 *
 * Подписки сторонних провайдеров не поддерживаются, поэтому произвольный домен
 * отвергается — этой функцией пользуются и поле ввода, и баннер из буфера обмена,
 * и разбор deep link.
 *
 * Содержимое ссылки нигде не логируется: в буфере могут оказаться чужие данные.
 */
object SubLinkUtil {

    /** Ограничение на длину, чтобы не разбирать случайно скопированную «простыню». */
    private const val MAX_LENGTH = 2048

    private val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

    /**
     * Развернуть строку в ссылку на подписку.
     *
     * @param value Строка от пользователя (поле ввода, буфер обмена, deep link).
     * @return Готовая к импорту ссылка, либо null, если это не наш ключ.
     */
    fun resolve(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty() || raw.length > MAX_LENGTH) return null

        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        val resolved = when {
            uri.host == AppConfig.APP_LINK_HOST &&
                uri.path?.startsWith(AppConfig.APP_LINK_SUB_PATH) == true -> decodeAppLink(uri)

            uri.host == AppConfig.SUB_KEY_HOST -> raw

            else -> null
        } ?: return null

        if (!Utils.isValidSubUrl(resolved)) return null
        val resolvedHost = runCatching { Uri.parse(resolved).host }.getOrNull() ?: return null
        if (resolvedHost != AppConfig.SUB_KEY_HOST && resolvedHost != AppConfig.APP_LINK_HOST) return null

        return resolved
    }

    /**
     * Похожа ли строка на наш ключ доступа.
     *
     * @param value Проверяемая строка.
     * @return True, если ссылку можно импортировать.
     */
    fun isSubLink(value: String?): Boolean = resolve(value) != null

    /**
     * Стабильный отпечаток ссылки — им помечаем отклонённый пользователем ключ,
     * чтобы не хранить саму ссылку в настройках.
     *
     * @param value Ссылка.
     * @return Hex-строка SHA-256.
     */
    fun fingerprint(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.trim().toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun decodeAppLink(uri: Uri): String? {
        val payload = uri.path?.removePrefix(AppConfig.APP_LINK_SUB_PATH)
            ?.trim('/')
            ?.takeIf(String::isNotEmpty)
            ?: return null
        return runCatching {
            String(Base64.decode(payload, BASE64_FLAGS), Charsets.UTF_8)
        }.getOrNull()?.trim()?.takeIf(String::isNotEmpty)
    }
}
