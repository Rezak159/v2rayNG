package com.v2ray.ang.handler

import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest

/** Anonymous usage event sent after a subscription refresh request. */
object SubscriptionTelemetry {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun report(subscriptionUrl: String) {
        scope.launch {
            val installationId = MmkvManager.decodeSettingsString(AppConfig.PREF_INSTALLATION_ID)
                ?: Utils.getUuid().also { MmkvManager.encodeSettings(AppConfig.PREF_INSTALLATION_ID, it) }
            if (installationId.isBlank()) return@launch

            val context = AngApplication.application
            val payload = mapOf(
                "installationId" to installationId,
                "subscriptionFingerprint" to sha256(subscriptionUrl),
                "appVersion" to BuildConfig.VERSION_NAME,
                "appVersionCode" to PackageInfoCompat.getLongVersionCode(
                    context.packageManager.getPackageInfo(context.packageName, 0),
                ),
                "androidApi" to Build.VERSION.SDK_INT,
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                "abi" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"),
            )
            HttpUtil.postJson(AppConfig.TELEMETRY_URL, JsonUtil.toJson(payload))
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
