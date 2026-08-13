package com.v2ray.ang.handler

import android.content.Context
import android.util.Base64
import androidx.core.content.pm.PackageInfoCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.AppPolicyEnvelope
import com.v2ray.ang.dto.AppPolicyPayload
import com.v2ray.ang.util.DirectNetworkHttp
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

sealed interface AppPolicyState {
    data object Allowed : AppPolicyState
    data class UpdateRequired(val message: String, val downloadUrl: String) : AppPolicyState
}

/** Server-controlled minimum version with a signed policy and a limited offline grace period. */
object AppPolicyManager {
    private const val INITIAL_GRACE_MS = 72L * 60 * 60 * 1000
    private const val PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAraVqkd3foJE+zxe844TauYTZrnzFq5TS0esqCfRaZaD/eD08GuHH82tEGt81c5yMzm0/yFA8oOJu0B/Xl3IzJMQGoe+XDjT1a65pcB6M9O9sAXDqMO86XLfGeNpBzg3K34wqAnY8mKLX8QGeaO6a4/oGGKfQ+6S4pKwK9BrT0CByYkC+3dh9iz3ATB89HmeJ3CQ/f/EhmKepyiVa51dNLq3pV/Xt4gf4OGhncjW246zuA+cGV8CboxTwy7rbJcBsEJPD4QlYRPA/OoUBNeH2xwSaFvnBG8gplehleCsjX+DQBpDUqdeP09h2KFGQfrGbg+xBd7NdjvJhiyx1vnmOPwIDAQAB"

    suspend fun refresh(context: Context): AppPolicyState = withContext(Dispatchers.IO) {
        DirectNetworkHttp.getText(context, AppConfig.APP_POLICY_URL, 5_000)
            ?.takeIf(::saveIfValid)
        currentState(context)
    }

    fun currentState(context: Context): AppPolicyState {
        val now = System.currentTimeMillis()
        val cached = MmkvManager.decodeSettingsString(AppConfig.PREF_APP_POLICY)
            ?.let(::parseVerified)
        if (cached != null) {
            if (currentVersionCode(context) < cached.minVersionCode) return blocked(cached)
            if (now <= cached.expiresAt) return AppPolicyState.Allowed
            return blocked(cached, "Не удалось подтвердить актуальность приложения. Подключитесь к интернету и обновите A4VPN.")
        }

        val firstSeen = MmkvManager.decodeSettingsLong(AppConfig.PREF_APP_POLICY_FIRST_SEEN, 0)
        if (firstSeen == 0L) {
            MmkvManager.encodeSettings(AppConfig.PREF_APP_POLICY_FIRST_SEEN, now)
            return AppPolicyState.Allowed
        }
        return if (now - firstSeen <= INITIAL_GRACE_MS) AppPolicyState.Allowed else {
            AppPolicyState.UpdateRequired(
                "Нужно подключение к интернету, чтобы подтвердить актуальность приложения.",
                AppConfig.APP_DOWNLOAD_PAGE_URL,
            )
        }
    }

    private fun saveIfValid(raw: String): Boolean {
        if (parseVerified(raw) == null) return false
        MmkvManager.encodeSettings(AppConfig.PREF_APP_POLICY, raw)
        return true
    }

    private fun parseVerified(raw: String): AppPolicyPayload? {
        val envelope = JsonUtil.fromJsonSafe(raw, AppPolicyEnvelope::class.java) ?: return null
        if (!verify(envelope)) return null
        return runCatching {
            val payload = String(Base64.decode(envelope.payload, Base64.NO_WRAP), Charsets.UTF_8)
            JsonUtil.fromJsonSafe(payload, AppPolicyPayload::class.java)
        }.getOrNull()?.takeIf { it.expiresAt > 0 && it.downloadUrl.startsWith("https://app.a4vpn.net/") }
    }

    private fun verify(envelope: AppPolicyEnvelope): Boolean = runCatching {
        val key = KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(Base64.decode(PUBLIC_KEY, Base64.NO_WRAP)),
        )
        Signature.getInstance("SHA256withRSA").run {
            initVerify(key)
            update(envelope.payload.toByteArray(Charsets.UTF_8))
            verify(Base64.decode(envelope.signature, Base64.NO_WRAP))
        }
    }.onFailure { LogUtil.w(AppConfig.TAG, "Invalid app policy signature") }.getOrDefault(false)

    private fun blocked(policy: AppPolicyPayload, fallback: String = policy.message): AppPolicyState.UpdateRequired =
        AppPolicyState.UpdateRequired(fallback.ifBlank { "Эта версия A4VPN больше не поддерживается. Обновите приложение." }, policy.downloadUrl)

    private fun currentVersionCode(context: Context): Int = PackageInfoCompat.getLongVersionCode(
        context.packageManager.getPackageInfo(context.packageName, 0),
    ).toInt()
}
