package com.v2ray.ang.handler

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.util.DirectNetworkHttp
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Keeps the server-maintained direct-app list separate from a person's own choices.
 *
 * In bypass mode the selected packages do not use VPN. A person can add packages to
 * that selection or explicitly remove packages supplied by the server; later server
 * updates preserve both decisions.
 */
object BypassAppListManager {
    private val packageNamePattern = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    suspend fun refresh(context: Context): Boolean = withContext(Dispatchers.IO) {
        val content = DirectNetworkHttp.getText(context, AppConfig.BYPASS_APP_LIST_URL, 5_000)
            ?: return@withContext false
        val packages = parsePackageNames(content)
        if (packages.isEmpty()) {
            LogUtil.w(AppConfig.TAG, "Ignoring empty bypass app list")
            return@withContext false
        }

        applyServerPackages(context, packages)
        true
    }

    /** Apply a user action from the per-app screen. */
    fun replaceUserSelection(selectedPackages: Set<String>): Set<String> {
        migrateLegacySelection()
        val serverPackages = serverPackages()
        val manualPackages = selectedPackages - serverPackages
        val excludedServerPackages = serverPackages - selectedPackages

        MmkvManager.encodeSettings(AppConfig.PREF_MANUAL_BYPASS_APP_SET, manualPackages.toMutableSet())
        MmkvManager.encodeSettings(AppConfig.PREF_SERVER_BYPASS_APP_EXCLUDED_SET, excludedServerPackages.toMutableSet())
        return saveEffectiveSelection(serverPackages, manualPackages, excludedServerPackages)
    }

    fun currentSelection(): Set<String> {
        migrateLegacySelection()
        return effectiveSelection(
            serverPackages = serverPackages(),
            manualPackages = manualPackages(),
            excludedServerPackages = excludedServerPackages(),
        )
    }

    private fun applyServerPackages(context: Context, packages: Set<String>) {
        migrateLegacySelection()
        val previousSelection = MmkvManager.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET).orEmpty()
        MmkvManager.encodeSettings(AppConfig.PREF_SERVER_BYPASS_APP_SET, packages.toMutableSet())

        if (MmkvManager.decodeSettingsBool(AppConfig.PREF_BYPASS_APPS, true)) {
            val effective = saveEffectiveSelection(packages, manualPackages(), excludedServerPackages())
            LogUtil.i(AppConfig.TAG, "Updated direct app list: ${effective.size} selected packages")
            if (effective != previousSelection) {
                MessageHelper.sendMsg2Service(context, AppConfig.MSG_STATE_RESTART, "")
            }
        }
    }

    private fun saveEffectiveSelection(
        serverPackages: Set<String>,
        manualPackages: Set<String>,
        excludedServerPackages: Set<String>,
    ): Set<String> {
        val effective = effectiveSelection(serverPackages, manualPackages, excludedServerPackages)
        MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY_SET, effective.toMutableSet())
        return effective
    }

    private fun effectiveSelection(
        serverPackages: Set<String>,
        manualPackages: Set<String>,
        excludedServerPackages: Set<String>,
    ): Set<String> = (serverPackages + manualPackages) - excludedServerPackages

    private fun migrateLegacySelection() {
        if (MmkvManager.decodeSettingsBool(AppConfig.PREF_BYPASS_APP_SELECTION_MIGRATED)) return

        val legacySelection = MmkvManager.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET).orEmpty()
        MmkvManager.encodeSettings(AppConfig.PREF_MANUAL_BYPASS_APP_SET, legacySelection.toMutableSet())
        MmkvManager.encodeSettings(AppConfig.PREF_SERVER_BYPASS_APP_EXCLUDED_SET, mutableSetOf())
        MmkvManager.encodeSettings(AppConfig.PREF_BYPASS_APP_SELECTION_MIGRATED, true)
    }

    private fun serverPackages(): Set<String> =
        MmkvManager.decodeSettingsStringSet(AppConfig.PREF_SERVER_BYPASS_APP_SET).orEmpty()

    private fun manualPackages(): Set<String> =
        MmkvManager.decodeSettingsStringSet(AppConfig.PREF_MANUAL_BYPASS_APP_SET).orEmpty()

    private fun excludedServerPackages(): Set<String> =
        MmkvManager.decodeSettingsStringSet(AppConfig.PREF_SERVER_BYPASS_APP_EXCLUDED_SET).orEmpty()

    private fun parsePackageNames(content: String): Set<String> = content
        .lineSequence()
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith('#') }
        .filter(packageNamePattern::matches)
        .toSet()
}
