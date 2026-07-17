package com.v2ray.ang.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.compose.AppDivider
import com.v2ray.ang.compose.AppTheme
import com.v2ray.ang.compose.AppTopBar
import com.v2ray.ang.compose.ConfirmDialog
import com.v2ray.ang.compose.SelectListDialog
import com.v2ray.ang.compose.LocalDarkTheme
import com.v2ray.ang.compose.QRCodeDialog
import com.v2ray.ang.compose.ReorderableGridItem
import com.v2ray.ang.compose.ReorderableListItem
import com.v2ray.ang.compose.colorConfigType
import com.v2ray.ang.compose.colorFabActive
import com.v2ray.ang.compose.colorFabInactiveDark
import com.v2ray.ang.compose.colorFabInactiveLight
import com.v2ray.ang.compose.colorPing
import com.v2ray.ang.compose.colorPingRed
import com.v2ray.ang.compose.verticalScrollbar
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.enums.PermissionType
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.handler.SubscriptionUpdater
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import com.v2ray.ang.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.yield
import kotlin.math.abs

class MainActivity : HelperBaseComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    private val requestVpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                startV2Ray()
            } else {
                mainViewModel.cancelConnectionAction()
            }
        }

    // Launcher for profile editor activities (ServerActivity, ServerCustomConfigActivity, etc.)
    private val profileEditorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult

            val data = result.data ?: return@registerForActivityResult
            val action = data.getStringExtra(ProfileEditorResult.EXTRA_ACTION)
                ?: return@registerForActivityResult

            if (action != ProfileEditorResult.ACTION_SAVED &&
                action != ProfileEditorResult.ACTION_DELETED
            ) {
                return@registerForActivityResult
            }

            val restartService = data.getBooleanExtra(
                ProfileEditorResult.EXTRA_RESTART_SERVICE,
                false
            )

            mainViewModel.setupGroupTab(forceRefresh = true)

            if (restartService && mainViewModel.uiState.value.isRunning) {
                restartV2Ray()
            }
        }

    // Launcher for settings, subscription, routing, etc. (non-editor sever pages)
    private val settingsActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val restartService = SettingsChangeManager.consumeRestartService()
            val refreshGroups = SettingsChangeManager.consumeSetupGroupTab()

            mainViewModel.refreshUiSettings()

            if (refreshGroups) {
                mainViewModel.setupGroupTab(forceRefresh = true)
            }

            if (restartService && mainViewModel.uiState.value.isRunning) {
                restartV2Ray()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel.initialize()

        checkAndRequestPermission(PermissionType.POST_NOTIFICATIONS) {}
    }

    @Composable
    override fun ScreenContent() {
        A4MainScreen(
            mainViewModel = mainViewModel,
            onConnectionClick = ::handleFabAction,
            onImportSubscription = ::importSubscription,
            onSelectServer = ::setSelectServer,
            onOpenLogcat = { navigateTo("logcat") },
        )
    }

    fun getShareQRCodeBitmap(guid: String): Bitmap? = AngConfigManager.share2QRCode(guid)
    fun shareToClipboard(guid: String): Boolean =
        AngConfigManager.share2Clipboard(this, guid) == 0

    fun shareFullContentAsync(guid: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = AngConfigManager.shareFullContent2Clipboard(this@MainActivity, guid)
            withContext(Dispatchers.Main) {
                if (result == 0) toastSuccess(R.string.toast_success)
                else toastError(R.string.toast_failure)
            }
        }
    }

    private fun navigateTo(destination: String) {
        val intent = when (destination) {
            "sub_setting" -> Intent(this, SubSettingActivity::class.java)
            "per_app_proxy" -> Intent(this, PerAppProxyActivity::class.java)
            "routing_setting" -> Intent(this, RoutingSettingActivity::class.java)
            "user_asset" -> Intent(this, UserAssetActivity::class.java)
            "settings" -> Intent(this, SettingsActivity::class.java)
            "logcat" -> Intent(this, LogcatActivity::class.java)
            "check_update" -> Intent(this, CheckUpdateActivity::class.java)
            "backup_restore" -> Intent(this, BackupActivity::class.java)
            "about" -> Intent(this, AboutActivity::class.java)
            "promotion" -> {
                Utils.openUri(
                    this,
                    "${Utils.decode(AppConfig.APP_PROMOTION_URL)}?t=${System.currentTimeMillis()}"
                )
                return
            }
            else -> return
        }
        settingsActivityLauncher.launch(intent)
    }

    private fun handleFabAction() {
        if (!mainViewModel.beginConnectionAction()) return

        if (mainViewModel.uiState.value.isRunning) {
            CoreServiceManager.stopVService(this)
        } else if (SettingsManager.isVpnMode()) {
            val intent = VpnService.prepare(this)
            if (intent == null) startV2Ray() else requestVpnPermission.launch(intent)
        } else {
            startV2Ray()
        }
    }

    private fun handleLayoutTestClick() {
        if (mainViewModel.uiState.value.isRunning) mainViewModel.testCurrentServerRealPing()
    }

    private fun startV2Ray() {
        val selectedGuid = MmkvManager.getSelectServer()
        if (selectedGuid.isNullOrEmpty() || MmkvManager.decodeServerConfig(selectedGuid) == null) {
            mainViewModel.cancelConnectionAction()
            toast(R.string.title_file_chooser)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
            MmkvManager.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING)
        ) {
            checkAndRequestPermission(PermissionType.ACCESS_LOCAL_NETWORK) {}
        }
        CoreServiceManager.startVService(this)
    }

    private fun restartV2Ray() {
        if (mainViewModel.uiState.value.isRunning) CoreServiceManager.stopVService(this)
        lifecycleScope.launch { delay(500); startV2Ray() }
    }

    private fun importManually(createConfigType: Int) {
        val intent = when (createConfigType) {
            EConfigType.POLICYGROUP.value -> Intent(this, ServerGroupActivity::class.java)
            EConfigType.PROXYCHAIN.value -> Intent(this, ServerProxyChainActivity::class.java)
            else -> Intent(this, ServerActivity::class.java).apply {
                putExtra("createConfigType", createConfigType)
            }
        }.apply {
            putExtra("subscriptionId", mainViewModel.subscriptionId)
        }
        profileEditorLauncher.launch(intent)
    }

    private fun importQRcode() {
        launchQRCodeScanner { scanResult ->
            if (scanResult != null) importBatchConfig(scanResult)
        }
    }

    private fun importClipboard() {
        try {
            importBatchConfig(Utils.getClipboard(this))
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to import config from clipboard", e)
        }
    }

    /** The simplified A4 entry point accepts one subscription URL only. */
    private fun importSubscription(
        url: String,
        onResult: (Boolean) -> Unit,
    ) {
        val subscriptions = MmkvManager.decodeSubscriptions()
        val hasUsableSubscription = subscriptions.any { subscription ->
            MmkvManager.decodeServerList(subscription.guid).isNotEmpty()
        }
        if (hasUsableSubscription) {
            onResult(true)
            return
        }

        // A previous key may have been saved but never yielded a server.
        // It must not lock the user out of entering a working replacement key.
        subscriptions.forEach { subscription ->
            MmkvManager.removeServerViaSubid(subscription.guid)
            MmkvManager.removeSubscription(subscription.guid)
        }

        val subscriptionUrl = url.trim()
        if (!Utils.isValidSubUrl(subscriptionUrl)) {
            toastError(R.string.toast_failure)
            onResult(false)
            return
        }
        importBatchConfig(subscriptionUrl) { imported ->
            if (!imported) {
                // A subscription URL may be stored even if its first update failed.
                // Do not let that empty entry lock the user out of trying again.
                MmkvManager.decodeSubscriptions()
                    .filter { MmkvManager.decodeServerList(it.guid).isEmpty() }
                    .forEach {
                        MmkvManager.removeServerViaSubid(it.guid)
                        MmkvManager.removeSubscription(it.guid)
                    }
            }
            onResult(imported)
        }
    }

    private fun importBatchConfig(
        server: String?,
        onFinished: ((Boolean) -> Unit)? = null,
    ) {
        mainViewModel.setLoading(true)
        lifecycleScope.launch {
            try {
                val (count, countSub) = withContext(Dispatchers.IO) {
                    AngConfigManager.importBatchConfig(
                        server,
                        mainViewModel.subscriptionId,
                        true
                    )
                }
                val hasUsableSubscription = MmkvManager.decodeSubscriptions().any { subscription ->
                    MmkvManager.decodeServerList(subscription.guid).isNotEmpty()
                }
                val imported = count > 0 || (countSub > 0 && hasUsableSubscription)
                when {
                    count > 0 -> {
                        toast(getString(R.string.title_import_config_count, count))
                        mainViewModel.setupGroupTab(forceRefresh = true).join()
                    }
                    countSub > 0 && hasUsableSubscription -> {
                        mainViewModel.setupGroupTab(forceRefresh = true).join()
                    }
                    else -> toastError(R.string.toast_failure)
                }
                onFinished?.invoke(imported)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to import batch config", e)
                toastError(R.string.toast_failure)
                onFinished?.invoke(false)
            } finally {
                mainViewModel.setLoading(false)
            }
        }
    }

    private fun importConfigLocal() {
        launchFileChooser { uri ->
            if (uri == null) return@launchFileChooser
            try {
                contentResolver.openInputStream(uri)
                    .use { input -> importBatchConfig(input?.bufferedReader()?.readText()) }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to read content from URI", e)
            }
        }
    }

    private fun importConfigViaSub() {
        mainViewModel.setLoading(true)
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    mainViewModel.updateConfigViaSubAll()
                }
                when {
                    result.successCount + result.failureCount + result.skipCount == 0 ->
                        toast(R.string.title_update_subscription_no_subscription)
                    result.successCount > 0 && result.failureCount + result.skipCount == 0 ->
                        toast(getString(R.string.title_update_config_count, result.configCount))
                    else ->
                        toast(
                            getString(
                                R.string.title_update_subscription_result,
                                result.configCount,
                                result.successCount,
                                result.failureCount,
                                result.skipCount
                            )
                        )
                }
                if (result.configCount > 0) {
                    mainViewModel.setupGroupTab(forceRefresh = true)
                    mainViewModel.refreshSelectedGuid()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Subscription update failed", e)
                toastError(R.string.toast_failure)
            } finally {
                mainViewModel.setLoading(false)
            }
        }
    }

    private fun exportAll() {
        mainViewModel.setLoading(true)
        lifecycleScope.launch {
            try {
                val ret = withContext(Dispatchers.IO) {
                    mainViewModel.exportAllServer()
                }
                if (ret > 0) toast(getString(R.string.title_export_config_count, ret))
                else toastError(R.string.toast_failure)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Export failed", e)
                toastError(R.string.toast_failure)
            } finally {
                mainViewModel.setLoading(false)
            }
        }
    }

    private fun delAllConfig() {
        mainViewModel.setLoading(true)
        lifecycleScope.launch {
            try {
                val ret = withContext(Dispatchers.IO) {
                    mainViewModel.removeAllServer()
                }
                mainViewModel.setupGroupTab(forceRefresh = true)
                toast(getString(R.string.title_del_config_count, ret))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Delete all failed", e)
                toastError(R.string.toast_failure)
            } finally {
                mainViewModel.setLoading(false)
            }
        }
    }

    private fun delDuplicateConfig() {
        mainViewModel.setLoading(true)
        lifecycleScope.launch {
            try {
                val ret = withContext(Dispatchers.IO) {
                    mainViewModel.removeDuplicateServer()
                }
                mainViewModel.setupGroupTab(forceRefresh = true)
                toast(getString(R.string.title_del_duplicate_config_count, ret))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Delete duplicate failed", e)
                toastError(R.string.toast_failure)
            } finally {
                mainViewModel.setLoading(false)
            }
        }
    }

    private fun delInvalidConfig() {
        mainViewModel.setLoading(true)
        lifecycleScope.launch {
            try {
                val ret = withContext(Dispatchers.IO) {
                    mainViewModel.removeInvalidServer()
                }
                mainViewModel.setupGroupTab(forceRefresh = true)
                toast(getString(R.string.title_del_config_count, ret))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Delete invalid failed", e)
                toastError(R.string.toast_failure)
            } finally {
                mainViewModel.setLoading(false)
            }
        }
    }

    private fun sortByTestResults() {
        mainViewModel.setLoading(true)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    mainViewModel.sortByTestResults()
                }
                mainViewModel.setupGroupTab(forceRefresh = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Sort by test results failed", e)
                toastError(R.string.toast_failure)
            } finally {
                mainViewModel.setLoading(false)
            }
        }
    }

    private fun editServer(guid: String, profile: ProfileItem) {
        val activityClass = when (profile.configType) {
            EConfigType.CUSTOM -> ServerCustomConfigActivity::class.java
            EConfigType.POLICYGROUP -> ServerGroupActivity::class.java
            EConfigType.PROXYCHAIN -> ServerProxyChainActivity::class.java
            else -> ServerActivity::class.java
        }
        val intent = Intent(this, activityClass).apply {
            putExtra("guid", guid)
            putExtra("isRunning", mainViewModel.uiState.value.isRunning)
            putExtra("createConfigType", profile.configType.value)
            putExtra("subscriptionId", mainViewModel.subscriptionId)
        }
        profileEditorLauncher.launch(intent)
    }

    private fun removeServer(guid: String) {
        if (guid == MmkvManager.getSelectServer()) {
            toast(R.string.toast_action_not_allowed); return
        }
        mainViewModel.removeServerAndRefresh(guid)
    }

    private fun setSelectServer(guid: String) {
        val selected = MmkvManager.getSelectServer()
        if (guid != selected) {
            mainViewModel.updateSelectedGuid(guid)
            if (mainViewModel.uiState.value.isRunning) restartV2Ray()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
