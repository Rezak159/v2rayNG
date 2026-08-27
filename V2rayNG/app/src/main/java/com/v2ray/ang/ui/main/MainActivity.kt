package com.v2ray.ang.ui.main

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.AppUpdate
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.enums.PermissionType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.AppUpdateManager
import com.v2ray.ang.handler.AppPolicyManager
import com.v2ray.ang.handler.AppPolicyState
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.A4MainScreen
import com.v2ray.ang.ui.AboutActivity
import com.v2ray.ang.ui.backup.BackupActivity
import com.v2ray.ang.ui.base.HelperBaseComponentActivity
import com.v2ray.ang.ui.compose.ConfirmDialog
import com.v2ray.ang.ui.checkupdate.CheckUpdateActivity
import com.v2ray.ang.ui.logcat.LogcatActivity
import com.v2ray.ang.ui.perappproxy.PerAppProxyActivity
import com.v2ray.ang.ui.routing.RoutingSettingActivity
import com.v2ray.ang.ui.server.ProfileEditorResult
import com.v2ray.ang.ui.server.ServerCustomConfigActivity
import com.v2ray.ang.ui.server.ServerGroupActivity
import com.v2ray.ang.ui.server.ServerHttpActivity
import com.v2ray.ang.ui.server.ServerHysteria2Activity
import com.v2ray.ang.ui.server.ServerProxyChainActivity
import com.v2ray.ang.ui.server.ServerShadowsocksActivity
import com.v2ray.ang.ui.server.ServerSocksActivity
import com.v2ray.ang.ui.server.ServerTrojanActivity
import com.v2ray.ang.ui.server.ServerVlessActivity
import com.v2ray.ang.ui.server.ServerVmessActivity
import com.v2ray.ang.ui.server.ServerWireguardActivity
import com.v2ray.ang.ui.settings.SettingsActivity
import com.v2ray.ang.ui.subscription.SubSettingActivity
import com.v2ray.ang.ui.userasset.UserAssetActivity
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.SubLinkUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : HelperBaseComponentActivity() {

    companion object {
        const val EXTRA_SUBSCRIPTION_URL = "com.v2ray.ang.SUBSCRIPTION_URL"
    }

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application, MainRepository(application as AngApplication))
    }

    private var appUpdate by mutableStateOf<AppUpdate?>(null)
    private var isDownloadingUpdate by mutableStateOf(false)
    private var updateDownloadProgress by mutableStateOf(0f)
    private var showInstallPermissionReminder by mutableStateOf(false)
    private var pendingUpdateApk: File? = null
    private var appPolicyState by mutableStateOf<AppPolicyState>(AppPolicyState.Allowed)

    /** Ключ доступа, найденный в буфере обмена; показывается баннером на главном экране. */
    private var clipboardSubLink by mutableStateOf<String?>(null)

    private val policyNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshAppPolicy()
    }

    private val requestVpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) startV2Ray()
        }

    private val profileEditorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult
            val action = data.getStringExtra(ProfileEditorResult.EXTRA_ACTION)
                ?: return@registerForActivityResult
            if (action != ProfileEditorResult.ACTION_SAVED &&
                action != ProfileEditorResult.ACTION_DELETED
            ) return@registerForActivityResult
            val restartService = data.getBooleanExtra(
                ProfileEditorResult.EXTRA_RESTART_SERVICE, false
            )
            mainViewModel.onAction(MainAction.RefreshGroups)
            if (restartService && mainViewModel.uiState.value.isRunning) {
                restartV2Ray()
            }
        }

    private val settingsActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val restartService = SettingsChangeManager.consumeRestartService()
            val refreshGroups = SettingsChangeManager.consumeSetupGroupTab()
            mainViewModel.refreshUiSettings()
            if (refreshGroups) mainViewModel.onAction(MainAction.RefreshGroups)
            if (restartService && mainViewModel.uiState.value.isRunning) restartV2Ray()
        }

    private val requestInstallPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Пользователь мог включить разрешение и просто нажать «назад» —
            // resultCode здесь не гарантирован, поэтому просто перепроверяем.
            val apk = pendingUpdateApk ?: return@registerForActivityResult
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()) {
                pendingUpdateApk = null
                installApk(apk)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel.onAction(MainAction.Initialize)

        checkAndRequestPermission(PermissionType.POST_NOTIFICATIONS) {}
        importFromIntent(intent)
        refreshAppPolicy()
        checkForAppUpdate()
        collectViewModelEvents()
    }

    /** События, которые может выполнить только Activity. */
    private fun collectViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.viewModelEvent.collect { event ->
                    if (event is MainEvent.RestartService) restartV2Ray()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        getSystemService(ConnectivityManager::class.java)?.registerDefaultNetworkCallback(policyNetworkCallback)
    }

    override fun onStop() {
        getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(policyNetworkCallback)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        refreshAppPolicy()
        // Платная могла истечь (или, наоборот, продлиться) за время, пока экрана
        // не было на виду — приводим доступ в соответствие с подписками.
        lifecycleScope.launch { mainViewModel.syncPlan() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Начиная с Android 10 буфер обмена отдаётся только сфокусированной активити,
        // поэтому проверяем именно здесь, а не в onResume.
        if (hasFocus) checkClipboardForKey()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importFromIntent(intent)
    }

    @Composable
    override fun ScreenContent() {
        BackHandler { moveTaskToBack(false) }
        val blocked = appPolicyState as? AppPolicyState.UpdateRequired
        if (blocked != null) {
            ForcedUpdateScreen(blocked.message, blocked.downloadUrl)
            return
        }
        A4MainScreen(
            mainViewModel = mainViewModel,
            onConnectionClick = ::handleFabAction,
            onSelectServer = ::setSelectServer,
            onOpenLogcat = { navigateTo(MainDestination.Logcat) },
            onOpenPerAppProxy = { navigateTo(MainDestination.PerAppProxy) },
            onImportSubscription = ::importSubscriptionFromEntry,
            onConnectFreeAccess = { mainViewModel.onAction(MainAction.ConnectFreeAccess) },
            clipboardKeyAvailable = clipboardSubLink != null,
            onClipboardKeyConnect = ::connectClipboardKey,
            onClipboardKeyDismiss = ::dismissClipboardKey,
            appUpdate = appUpdate,
            isDownloadingUpdate = isDownloadingUpdate,
            downloadProgress = updateDownloadProgress,
            onInstallUpdate = ::downloadAndInstallUpdate,
        )
        if (showInstallPermissionReminder) {
            ConfirmDialog(
                title = getString(R.string.update_unknown_sources_title),
                message = getString(R.string.update_unknown_sources_message),
                confirmText = getString(R.string.update_unknown_sources_action),
                onConfirm = {
                    showInstallPermissionReminder = false
                    requestInstallPermission.launch(
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:$packageName"),
                        ),
                    )
                },
                onDismiss = {
                    showInstallPermissionReminder = false
                    pendingUpdateApk = null
                },
            )
        }
    }

    @Composable
    private fun ForcedUpdateScreen(message: String, downloadUrl: String) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Требуется обновление", style = MaterialTheme.typography.headlineSmall)
            Text(message, modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyLarge)
            Button(
                modifier = Modifier.padding(top = 24.dp),
                onClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))) },
            ) { Text("Скачать новую версию") }
        }
    }

    private fun refreshAppPolicy() {
        lifecycleScope.launch {
            val state = AppPolicyManager.refresh(this@MainActivity)
            appPolicyState = state
            if (state is AppPolicyState.UpdateRequired) LauncherManager.stopService(this@MainActivity)
        }
    }

    private fun checkForAppUpdate() {
        lifecycleScope.launch {
            appUpdate = AppUpdateManager.checkForUpdate(this@MainActivity)
        }
    }

    private fun downloadAndInstallUpdate() {
        val update = appUpdate ?: return
        if (isDownloadingUpdate) return

        isDownloadingUpdate = true
        updateDownloadProgress = 0f
        lifecycleScope.launch {
            val apk = AppUpdateManager.download(this@MainActivity, update) { progress ->
                updateDownloadProgress = progress
            }
            isDownloadingUpdate = false
            if (apk == null) {
                toastError(R.string.toast_failure)
                return@launch
            }
            requestApkInstallation(apk)
        }
    }

    private fun requestApkInstallation(apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            pendingUpdateApk = apk
            showInstallPermissionReminder = true
            return
        }
        installApk(apk)
    }

    private fun installApk(apk: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.cache", apk)
        startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }

    private fun importFromIntent(intent: Intent?) {
        val url = intent?.getStringExtra(EXTRA_SUBSCRIPTION_URL) ?: return
        intent.removeExtra(EXTRA_SUBSCRIPTION_URL)
        importSubscriptionFromEntry(url)
    }

    /**
     * Проверить буфер обмена на наш ключ доступа и показать баннер.
     *
     * Содержимое буфера никуда не логируется — там могут быть чужие данные.
     */
    private fun checkClipboardForKey() {
        val link = SubLinkUtil.resolve(Utils.getClipboard(this))
        if (link == null) {
            clipboardSubLink = null
            return
        }
        if (SubLinkUtil.fingerprint(link) ==
            MmkvManager.decodeSettingsString(AppConfig.PREF_DISMISSED_SUB_LINK)
        ) {
            clipboardSubLink = null
            return
        }
        if (MmkvManager.decodeSubscriptions().any { it.subscription.url == link }) {
            clipboardSubLink = null
            return
        }
        clipboardSubLink = link
    }

    private fun connectClipboardKey() {
        val link = clipboardSubLink ?: return
        clipboardSubLink = null
        importSubscriptionFromEntry(link)
    }

    private fun dismissClipboardKey() {
        val link = clipboardSubLink ?: return
        MmkvManager.encodeSettings(AppConfig.PREF_DISMISSED_SUB_LINK, SubLinkUtil.fingerprint(link))
        clipboardSubLink = null
    }

    private fun importSubscriptionFromEntry(value: String) {
        val subscriptionUrl = SubLinkUtil.resolve(value)
        if (subscriptionUrl == null) {
            toastError(R.string.toast_failure)
            return
        }

        // Платная подписка на устройстве одна: старую (в т.ч. неудачно закачавшуюся)
        // подчищаем перед вводом новой ссылки, иначе importUrlAsSubscription молча
        // откажет в добавлении, пока висит любая нефри-подписка.
        MmkvManager.decodeSubscriptions()
            .filter { it.subscription.url != AppConfig.FREE_SUB_URL }
            .forEach {
                MmkvManager.removeServerViaSubid(it.guid)
                MmkvManager.removeSubscription(it.guid)
            }

        mainViewModel.onAction(MainAction.ImportSubscriptionKey(subscriptionUrl))
    }

    private fun shareToClipboard(guid: String): Boolean =
        AngConfigManager.share2Clipboard(this, guid) == 0

    private fun shareFullContentAsync(guid: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = AngConfigManager.shareFullContent2Clipboard(this@MainActivity, guid)
            withContext(Dispatchers.Main) {
                if (result == 0) toastSuccess(R.string.toast_success)
                else toastError(R.string.toast_failure)
            }
        }
    }

    private fun navigateTo(destination: MainDestination) {
        val intent = when (destination) {
            MainDestination.Subscriptions -> Intent(this, SubSettingActivity::class.java)
            MainDestination.PerAppProxy -> Intent(this, PerAppProxyActivity::class.java)
            MainDestination.Routing -> Intent(this, RoutingSettingActivity::class.java)
            MainDestination.UserAssets -> Intent(this, UserAssetActivity::class.java)
            MainDestination.Settings -> Intent(this, SettingsActivity::class.java)
            MainDestination.Logcat -> Intent(this, LogcatActivity::class.java)
            MainDestination.CheckUpdate -> Intent(this, CheckUpdateActivity::class.java)
            MainDestination.BackupRestore -> Intent(this, BackupActivity::class.java)
            MainDestination.About -> Intent(this, AboutActivity::class.java)
            MainDestination.Promotion -> {
                Utils.openUri(
                    this,
                    "${Utils.decode(AppConfig.APP_PROMOTION_URL)}?t=${System.currentTimeMillis()}"
                )
                return
            }
        }
        settingsActivityLauncher.launch(intent)
    }

    private fun handleFabAction() {
        if (appPolicyState is AppPolicyState.UpdateRequired) return
        if (mainViewModel.uiState.value.isRunning) {
            LauncherManager.stopService(this)
        } else if (SettingsManager.isVpnMode()) {
            val intent = VpnService.prepare(this)
            if (intent == null) startV2Ray() else requestVpnPermission.launch(intent)
        } else {
            startV2Ray()
        }
    }

    private fun handleLayoutTestClick() {
        if (mainViewModel.uiState.value.isRunning) {
            mainViewModel.testCurrentServerRealPing()
        }
    }

    private fun startV2Ray() {
        if (mainViewModel.uiState.value.selectedGuid.isNullOrEmpty()) {
            toast(R.string.title_file_chooser)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
            MmkvManager.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING)
        ) {
            checkAndRequestPermission(PermissionType.ACCESS_LOCAL_NETWORK) {}
        }
        LauncherManager.startService(this)
    }

    private fun restartV2Ray() {
        if (mainViewModel.uiState.value.isRunning) LauncherManager.stopService(this)
        lifecycleScope.launch {
            kotlinx.coroutines.delay(500)
            startV2Ray()
        }
    }

    private fun importManually(createConfigType: Int) {
        val intent = when (createConfigType) {
            EConfigType.POLICYGROUP.value -> Intent(this, ServerGroupActivity::class.java)
            EConfigType.PROXYCHAIN.value -> Intent(this, ServerProxyChainActivity::class.java)
            EConfigType.VMESS.value -> Intent(this, ServerVmessActivity::class.java)
            EConfigType.VLESS.value -> Intent(this, ServerVlessActivity::class.java)
            EConfigType.SHADOWSOCKS.value -> Intent(this, ServerShadowsocksActivity::class.java)
            EConfigType.SOCKS.value -> Intent(this, ServerSocksActivity::class.java)
            EConfigType.HTTP.value -> Intent(this, ServerHttpActivity::class.java)
            EConfigType.TROJAN.value -> Intent(this, ServerTrojanActivity::class.java)
            EConfigType.WIREGUARD.value -> Intent(this, ServerWireguardActivity::class.java)
            EConfigType.HYSTERIA2.value -> Intent(this, ServerHysteria2Activity::class.java)
            else -> Intent(this, ServerHttpActivity::class.java).apply {
                putExtra("createConfigType", createConfigType)
            }
        }.apply {
            putExtra("subscriptionId", mainViewModel.uiState.value.selectedGroupId)
        }
        profileEditorLauncher.launch(intent)
    }

    private fun importQRcode() {
        launchQRCodeScanner { scanResult ->
            if (scanResult != null) {
                mainViewModel.onAction(MainAction.ImportBatchConfig(scanResult))
            }
        }
    }

    private fun importClipboard() {
        try {
            val text = Utils.getClipboard(this)
            mainViewModel.onAction(MainAction.ImportBatchConfig(text))
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to import config from clipboard", e)
        }
    }

    private fun importConfigLocal() {
        launchFileChooser { uri ->
            if (uri == null) return@launchFileChooser
            try {
                contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    mainViewModel.onAction(MainAction.ImportBatchConfig(reader.readText()))
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to read content from URI", e)
            }
        }
    }

    private fun editServer(guid: String, profile: ProfileItem) {
        val activityClass = when (profile.configType) {
            EConfigType.CUSTOM -> ServerCustomConfigActivity::class.java
            EConfigType.POLICYGROUP -> ServerGroupActivity::class.java
            EConfigType.PROXYCHAIN -> ServerProxyChainActivity::class.java
            EConfigType.VMESS -> ServerVmessActivity::class.java
            EConfigType.VLESS -> ServerVlessActivity::class.java
            EConfigType.SHADOWSOCKS -> ServerShadowsocksActivity::class.java
            EConfigType.SOCKS -> ServerSocksActivity::class.java
            EConfigType.HTTP -> ServerHttpActivity::class.java
            EConfigType.TROJAN -> ServerTrojanActivity::class.java
            EConfigType.WIREGUARD -> ServerWireguardActivity::class.java
            EConfigType.HYSTERIA2 -> ServerHysteria2Activity::class.java
            else -> ServerHttpActivity::class.java
        }
        val intent = Intent(this, activityClass).apply {
            putExtra("guid", guid)
            putExtra("isRunning", mainViewModel.uiState.value.isRunning)
            putExtra("createConfigType", profile.configType.value)
            putExtra("subscriptionId", mainViewModel.uiState.value.selectedGroupId)
        }
        profileEditorLauncher.launch(intent)
    }

    private fun setSelectServer(guid: String) {
        val selected = mainViewModel.uiState.value.selectedGuid
        if (guid != selected) {
            mainViewModel.updateSelectedGuid(guid)
            if (mainViewModel.uiState.value.isRunning) restartV2Ray()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
