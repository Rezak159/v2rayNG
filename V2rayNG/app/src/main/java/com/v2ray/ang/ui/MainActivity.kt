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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import com.v2ray.ang.compose.SettingsMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.v2ray.ang.compose.colorBrandCream
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
            if (it.resultCode == RESULT_OK) startV2Ray()
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
        MainScreen(
            mainViewModel = mainViewModel,
            onFabClick = ::handleFabAction,
            onTestClick = ::handleLayoutTestClick,
            onNavigate = ::navigateTo,
            onImportManually = ::importManually,
            onImportQRcode = ::importQRcode,
            onImportClipboard = ::importClipboard,
            onImportLocal = ::importConfigLocal,
            onSubUpdate = ::importConfigViaSub,
            onExportAll = ::exportAll,
            onRealPingAll = mainViewModel::testAllRealPing,
            onRestartService = ::restartV2Ray,
            onDelAllConfig = ::delAllConfig,
            onDelDuplicateConfig = ::delDuplicateConfig,
            onDelInvalidConfig = ::delInvalidConfig,
            onSortByTestResults = ::sortByTestResults,
            onEditServer = ::editServer,
            onRemoveServer = ::removeServer,
            onSelectServer = ::setSelectServer,
            onShareQRCode = ::getShareQRCodeBitmap,
            onShareClipboard = ::shareToClipboard,
            onShareFullContent = ::shareFullContentAsync,
            onSubscriptionIdChanged = mainViewModel::subscriptionIdChanged,
            onLocateSelectedServer = mainViewModel::triggerLocateSelectedServer,
            shareMethodEntries = resources.getStringArray(R.array.share_method).toList(),
            shareMethodMoreEntries = resources.getStringArray(R.array.share_method_more).toList()
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
        if (MmkvManager.getSelectServer().isNullOrEmpty()) {
            toast(R.string.title_file_chooser); return
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

    private fun importBatchConfig(server: String?) {
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
                when {
                    count > 0 -> {
                        toast(getString(R.string.title_import_config_count, count))
                        mainViewModel.setupGroupTab(forceRefresh = true)
                    }
                    countSub > 0 -> mainViewModel.setupGroupTab(forceRefresh = true)
                    else -> toastError(R.string.toast_failure)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to import batch config", e)
                toastError(R.string.toast_failure)
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

@Composable
private fun MainDialogs(
    showDelAllConfirm: Boolean,
    onDismissDelAll: () -> Unit,
    onConfirmDelAll: () -> Unit,
    showDelDuplicateConfirm: Boolean,
    onDismissDelDuplicate: () -> Unit,
    onConfirmDelDuplicate: () -> Unit,
    showDelInvalidConfirm: Boolean,
    onDismissDelInvalid: () -> Unit,
    onConfirmDelInvalid: () -> Unit,
    showRemoveConfirm: String?,
    onDismissRemove: () -> Unit,
    onConfirmRemove: (String) -> Unit,
) {
    if (showDelAllConfirm) {
        ConfirmDialog(
            message = stringResource(R.string.del_config_comfirm),
            confirmText = stringResource(android.R.string.ok),
            dismissText = stringResource(android.R.string.cancel),
            onConfirm = onConfirmDelAll,
            onDismiss = onDismissDelAll
        )
    }
    if (showDelDuplicateConfirm) {
        ConfirmDialog(
            message = stringResource(R.string.del_config_comfirm),
            confirmText = stringResource(android.R.string.ok),
            dismissText = stringResource(android.R.string.cancel),
            onConfirm = onConfirmDelDuplicate,
            onDismiss = onDismissDelDuplicate
        )
    }
    if (showDelInvalidConfirm) {
        ConfirmDialog(
            message = stringResource(R.string.del_invalid_config_comfirm),
            confirmText = stringResource(android.R.string.ok),
            dismissText = stringResource(android.R.string.cancel),
            onConfirm = onConfirmDelInvalid,
            onDismiss = onDismissDelInvalid
        )
    }
    if (showRemoveConfirm != null) {
        val guid = showRemoveConfirm
        ConfirmDialog(
            message = stringResource(R.string.del_config_comfirm),
            confirmText = stringResource(android.R.string.ok),
            dismissText = stringResource(android.R.string.cancel),
            onConfirm = { onConfirmRemove(guid) },
            onDismiss = onDismissRemove
        )
    }
}

@Composable
private fun HomeTab(
    isRunning: Boolean,
    statusText: String,
    selectedProfile: ProfileItem?,
    isDarkTheme: Boolean,
    onConnectClick: () -> Unit,
    onStatusClick: () -> Unit,
    onOpenServers: () -> Unit,
    onImportClipboard: () -> Unit,
    onImportQRcode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_a4_logo),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ConnectButton(
                isRunning = isRunning,
                isDarkTheme = isDarkTheme,
                onClick = onConnectClick
            )
            Spacer(modifier = Modifier.height(28.dp))
            AnimatedContent(
                targetState = isRunning,
                transitionSpec = {
                    (slideInVertically(animationSpec = tween(250)) { it / 2 } + fadeIn(tween(250))) togetherWith
                        (slideOutVertically(animationSpec = tween(150)) { -it / 2 } + fadeOut(tween(150)))
                },
                label = "stateLabel"
            ) { running ->
                Text(
                    text = stringResource(
                        if (running) R.string.state_connected else R.string.state_disconnected
                    ),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Crossfade(
                targetState = statusText,
                label = "statusCaption",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onStatusClick)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedContent(
            targetState = selectedProfile,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(300)) { it / 3 } + fadeIn(tween(300))) togetherWith
                    (slideOutVertically(animationSpec = tween(150)) { it / 3 } + fadeOut(tween(150)))
            },
            label = "serverCard",
            modifier = Modifier.fillMaxWidth()
        ) { profile ->
            if (profile != null) {
                SelectedServerCard(profile = profile, onClick = onOpenServers)
            } else {
                EmptyKeyCard(
                    onImportClipboard = onImportClipboard,
                    onImportQRcode = onImportQRcode
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ConnectButton(
    isRunning: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "connectPressScale"
    )
    val buttonColor by animateColorAsState(
        targetValue = if (isRunning) colorFabActive
        else if (isDarkTheme) colorFabInactiveDark
        else colorFabInactiveLight,
        animationSpec = tween(500),
        label = "connectButtonColor"
    )

    Box(contentAlignment = Alignment.Center) {
        if (isRunning) {
            val transition = rememberInfiniteTransition(label = "connectPulse")
            val ringSpec = tween<Float>(2200, easing = LinearEasing)
            val ring1 by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(ringSpec),
                label = "connectRing1"
            )
            val ring2 by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(ringSpec, initialStartOffset = StartOffset(1100)),
                label = "connectRing2"
            )
            listOf(ring1, ring2).forEach { progress ->
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(1f + 0.35f * progress)
                        .border(
                            width = 2.dp,
                            color = colorFabActive.copy(alpha = (1f - progress) * 0.45f),
                            shape = CircleShape
                        )
                )
            }
            val radarAngle by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
                label = "connectRadar"
            )
            Canvas(modifier = Modifier.size(198.dp)) {
                rotate(radarAngle) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, colorFabActive)
                        ),
                        startAngle = 30f,
                        sweepAngle = 300f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = buttonColor,
            shadowElevation = 6.dp,
            interactionSource = interactionSource,
            modifier = Modifier
                .size(170.dp)
                .scale(pressScale)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = isRunning,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.5f, animationSpec = tween(200)) + fadeIn(tween(200))) togetherWith
                            (scaleOut(targetScale = 0.5f, animationSpec = tween(150)) + fadeOut(tween(150)))
                    },
                    label = "connectIcon"
                ) { running ->
                    Icon(
                        painter = if (running) painterResource(R.drawable.ic_stop_24dp)
                        else painterResource(R.drawable.ic_play_24dp),
                        contentDescription = if (running) "Stop" else "Start",
                        tint = colorBrandCream,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedServerCard(profile: ProfileItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_current_server),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.remarks,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = getProtocolDescription(profile),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(180f)
            )
        }
    }
}

@Composable
private fun EmptyKeyCard(
    onImportClipboard: () -> Unit,
    onImportQRcode: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_no_server_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_no_server_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Button(
                    onClick = onImportClipboard,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text(stringResource(R.string.home_paste_key), maxLines = 1)
                }
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedButton(
                    onClick = onImportQRcode,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.home_scan_qr), maxLines = 1)
                }
            }
        }
    }
}

private suspend fun PagerState.navigateToPageOptimized(
    targetPage: Int,
    animateAdjacentPage: Boolean = true
) {
    if (pageCount <= 0) return

    val target = targetPage.coerceIn(0, pageCount - 1)
    val current = settledPage.coerceIn(0, pageCount - 1)

    if (target == current) return

    val distance = abs(target - current)

    when {
        distance == 1 && animateAdjacentPage -> animateScrollToPage(target)
        animateAdjacentPage -> {
            val adjacent = if (target > current) target - 1 else target + 1
            scrollToPage(adjacent)
            yield()
            animateScrollToPage(target)
        }
        else -> scrollToPage(target)
    }
}

@Composable
private fun GroupTabBar(
    groups: List<GroupMapItem>,
    selectedTabIndex: Int,
    mainViewModel: MainViewModel,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex.coerceIn(0, groups.lastIndex),
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = 16.dp,
        minTabWidth = 56.dp,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier
                    .tabIndicatorOffset(
                        selectedTabIndex = selectedTabIndex.coerceIn(0, groups.lastIndex),
                        matchContentSize = true
                    )
                    .clip(RoundedCornerShape(3.dp)),
                width = Dp.Unspecified,
                color = colorFabActive
            )
        },
        divider = {}
    ) {
        groups.forEachIndexed { index, group ->
            GroupTabItem(
                group = group,
                selected = index == selectedTabIndex,
                serverFlowProvider = {
                    mainViewModel.serversForGroup(group.id)
                },
                onClick = { onTabClick(index) }
            )
        }
    }
}

@Composable
private fun GroupTabItem(
    group: GroupMapItem,
    selected: Boolean,
    serverFlowProvider: () -> StateFlow<List<ServersCache>>,
    onClick: () -> Unit
) {
    val serverFlow = remember(group.id) {
        serverFlowProvider()
    }
    val servers by serverFlow.collectAsStateWithLifecycle()

    Tab(
        selected = selected,
        onClick = onClick,
        text = {
            val text = if (group.id.isEmpty()) {
                group.remarks
            } else {
                "${group.remarks} (${servers.size})"
            }
            Text(
                text = text,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun GroupPagerPage(
    groupId: String,
    mainViewModel: MainViewModel,
    selectedGuid: String?,
    doubleColumnDisplay: Boolean,
    confirmRemove: Boolean,
    searchQuery: String,
    lazyListStates: MutableMap<String, LazyListState>,
    lazyGridStates: MutableMap<String, LazyGridState>,
    onSelectServer: (String) -> Unit,
    onEditServer: (String, ProfileItem) -> Unit,
    onShareServer: (String, ProfileItem) -> Unit,
    onMoreServer: (String, ProfileItem) -> Unit,
    onRemoveServer: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val serverFlow = remember(groupId) {
        mainViewModel.serversForGroup(groupId)
    }
    val servers by serverFlow.collectAsStateWithLifecycle()

    val canReorder = groupId.isNotEmpty() && searchQuery.isEmpty()

    ServerListPage(
        servers = servers,
        selectedGuid = selectedGuid,
        canReorder = canReorder,
        doubleColumnDisplay = doubleColumnDisplay,
        subscriptionId = groupId,
        confirmRemove = confirmRemove,
        groupId = groupId,
        lazyListStates = lazyListStates,
        lazyGridStates = lazyGridStates,
        onSelectServer = onSelectServer,
        onEditServer = onEditServer,
        onShareServer = onShareServer,
        onMoreServer = onMoreServer,
        onRemoveServer = onRemoveServer,
        onSwapServer = mainViewModel::swapServer,
        contentPadding = contentPadding
    )
}

private const val TAB_HOME = 0
private const val TAB_SERVERS = 1
private const val TAB_MORE = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onFabClick: () -> Unit,
    onTestClick: () -> Unit,
    onNavigate: (String) -> Unit,
    onImportManually: (Int) -> Unit,
    onImportQRcode: () -> Unit,
    onImportClipboard: () -> Unit,
    onImportLocal: () -> Unit,
    onSubUpdate: () -> Unit,
    onExportAll: () -> Unit,
    onRealPingAll: () -> Unit,
    onRestartService: () -> Unit,
    onDelAllConfig: () -> Unit,
    onDelDuplicateConfig: () -> Unit,
    onDelInvalidConfig: () -> Unit,
    onSortByTestResults: () -> Unit,
    onEditServer: (String, ProfileItem) -> Unit,
    onRemoveServer: (String) -> Unit,
    onSelectServer: (String) -> Unit,
    onShareQRCode: (String) -> Bitmap?,
    onShareClipboard: (String) -> Boolean,
    onShareFullContent: (String) -> Unit,
    onSubscriptionIdChanged: (String) -> Unit,
    onLocateSelectedServer: () -> Unit,
    shareMethodEntries: List<String>,
    shareMethodMoreEntries: List<String>
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val groups = uiState.groups
    val isLoading = uiState.isLoading
    val isRunning = uiState.isRunning
    val displayText = uiState.statusText
    val selectedGuid = uiState.selectedGuid
    val doubleColumnDisplay = uiState.doubleColumnDisplay
    val confirmRemove = uiState.confirmRemove

    val isDarkTheme = LocalDarkTheme.current
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }
    var showDelAllConfirm by remember { mutableStateOf(false) }
    var showDelDuplicateConfirm by remember { mutableStateOf(false) }
    var showDelInvalidConfirm by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf<String?>(null) }

    var shareTarget by remember { mutableStateOf<Triple<String, ProfileItem, Boolean>?>(null) }
    var showQRCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { groups.size.coerceAtLeast(1) }
    )

    val lazyListStates = remember { mutableStateMapOf<String, LazyListState>() }
    val lazyGridStates = remember { mutableStateMapOf<String, LazyGridState>() }

    val importMenuScrollState = rememberScrollState()
    val moreMenuScrollState = rememberScrollState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val maxMenuHeight = LocalConfiguration.current.screenHeightDp.dp - statusBarHeight - navBarHeight - 20.dp

    var locateInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(groups) {
        val validGroupIds = groups.map { it.id }.toSet()
        lazyListStates.keys.retainAll(validGroupIds)
        lazyGridStates.keys.retainAll(validGroupIds)
    }

    val latestDoubleColumnDisplay by rememberUpdatedState(doubleColumnDisplay)

    LaunchedEffect(groups, uiState.selectedGroupId) {
        if (groups.isEmpty()) return@LaunchedEffect
        val selectedIndex = groups.indexOfFirst { it.id == uiState.selectedGroupId }
            .takeIf { it >= 0 } ?: 0
        if (!pagerState.isScrollInProgress && pagerState.settledPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    val latestGroups by rememberUpdatedState(groups)
    val latestLocateInProgress by rememberUpdatedState(locateInProgress)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val currentGroups = latestGroups
                if (!latestLocateInProgress && page in currentGroups.indices) {
                    onSubscriptionIdChanged(currentGroups[page].id)
                }
            }
    }

    LaunchedEffect(mainViewModel, pagerState) {
        mainViewModel.locateEvent.collect { target ->
            if (target.groupIndex !in 0 until pagerState.pageCount) return@collect

            locateInProgress = true
            try {
                if (pagerState.settledPage != target.groupIndex) {
                    pagerState.navigateToPageOptimized(
                        targetPage = target.groupIndex,
                        animateAdjacentPage = false
                    )
                }
                onSubscriptionIdChanged(target.groupId)

                repeat(10) {
                    val ready = if (latestDoubleColumnDisplay) {
                        lazyGridStates[target.groupId] != null
                    } else {
                        lazyListStates[target.groupId] != null
                    }
                    if (ready) return@repeat
                    delay(16L)
                }

                if (latestDoubleColumnDisplay) {
                    lazyGridStates[target.groupId]?.let { gridState ->
                        gridState.scrollToItem(
                            index = target.itemPosition,
                            scrollOffset = -gridState.layoutInfo.viewportSize.height / 3
                        )
                    }
                } else {
                    lazyListStates[target.groupId]?.let { listState ->
                        listState.scrollToItem(
                            index = target.itemPosition,
                            scrollOffset = -listState.layoutInfo.viewportSize.height / 3
                        )
                    }
                }
            } finally {
                delay(32L)
                locateInProgress = false
            }
        }
    }

    MainDialogs(
        showDelAllConfirm = showDelAllConfirm,
        onDismissDelAll = { showDelAllConfirm = false },
        onConfirmDelAll = { showDelAllConfirm = false; onDelAllConfig() },
        showDelDuplicateConfirm = showDelDuplicateConfirm,
        onDismissDelDuplicate = { showDelDuplicateConfirm = false },
        onConfirmDelDuplicate = { showDelDuplicateConfirm = false; onDelDuplicateConfig() },
        showDelInvalidConfirm = showDelInvalidConfirm,
        onDismissDelInvalid = { showDelInvalidConfirm = false },
        onConfirmDelInvalid = { showDelInvalidConfirm = false; onDelInvalidConfig() },
        showRemoveConfirm = showRemoveConfirm,
        onDismissRemove = { showRemoveConfirm = null },
        onConfirmRemove = { guid -> showRemoveConfirm = null; onRemoveServer(guid) }
    )

    if (shareTarget != null) {
        val (guid, profile, more) = shareTarget!!
        val isCustom = profile.configType.isComplexType()
        val (shareOptions, skip) = if (more) {
            val options = if (isCustom) shareMethodMoreEntries.takeLast(3) else shareMethodMoreEntries
            options to if (isCustom) 2 else 0
        } else {
            val options = if (isCustom) shareMethodEntries.takeLast(1) else shareMethodEntries
            options to if (isCustom) 2 else 0
        }
        SelectListDialog(
            options = shareOptions,
            onSelected = { index, _ ->
                shareTarget = null
                when (index + skip) {
                    0 -> showQRCodeBitmap = onShareQRCode(guid)
                    1 -> onShareClipboard(guid)
                    2 -> onShareFullContent(guid)
                    3 -> onEditServer(guid, profile)
                    4 -> onRemoveServer(guid)
                }
            },
            onDismiss = { shareTarget = null }
        )
    }
    if (showQRCodeBitmap != null) {
        QRCodeDialog(bitmap = showQRCodeBitmap, onDismiss = { showQRCodeBitmap = null })
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    val selectedProfile = remember(selectedGuid, groups) {
        selectedGuid?.let { MmkvManager.decodeServerConfig(it) }
    }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (selectedTab == TAB_SERVERS) {
                AppTopBar(
                    title = stringResource(R.string.nav_servers),
                    onBackClick = {},
                    isLoading = isLoading,
                    isSearchActive = showSearch,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query ->
                        searchQuery = query
                        mainViewModel.filterConfig(query)
                    },
                    onSearchClose = {
                        searchQuery = ""
                        mainViewModel.filterConfig("")
                        showSearch = false
                    },
                    searchPlaceholder = stringResource(R.string.menu_item_search),
                    navigationIcon = {
                        if (showSearch) {
                            IconButton(onClick = {
                                searchQuery = ""
                                mainViewModel.filterConfig("")
                                showSearch = false
                            }) {
                                Icon(
                                    painterResource(R.drawable.ic_arrow_back_24dp),
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = {
                        if (!showSearch) {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(
                                    painterResource(R.drawable.ic_search_24dp),
                                    contentDescription = "filter"
                                )
                            }
                        }
                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                            IconButton(onClick = { showImportMenu = true }) {
                                Icon(
                                    painterResource(R.drawable.ic_add_24dp),
                                    contentDescription = "Add"
                                )
                            }
                            DropdownMenu(
                                expanded = showImportMenu,
                                onDismissRequest = { showImportMenu = false },
                                scrollState = importMenuScrollState,
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .heightIn(max = maxMenuHeight)
                                    .verticalScrollbar(importMenuScrollState)
                            ) {
                                listOf(
                                    R.string.menu_item_import_config_qrcode to {
                                        showImportMenu = false; onImportQRcode()
                                    },
                                    R.string.menu_item_import_config_clipboard to {
                                        showImportMenu = false; onImportClipboard()
                                    },
                                    R.string.menu_item_import_config_local to {
                                        showImportMenu = false; onImportLocal()
                                    },
                                    R.string.menu_item_import_config_policy_group to {
                                        showImportMenu = false; onImportManually(EConfigType.POLICYGROUP.value)
                                    },
                                    R.string.menu_item_import_config_proxy_chain to {
                                        showImportMenu = false; onImportManually(EConfigType.PROXYCHAIN.value)
                                    },
                                    R.string.menu_item_import_config_manually_vmess to {
                                        showImportMenu = false; onImportManually(EConfigType.VMESS.value)
                                    },
                                    R.string.menu_item_import_config_manually_vless to {
                                        showImportMenu = false; onImportManually(EConfigType.VLESS.value)
                                    },
                                    R.string.menu_item_import_config_manually_ss to {
                                        showImportMenu = false; onImportManually(EConfigType.SHADOWSOCKS.value)
                                    },
                                    R.string.menu_item_import_config_manually_socks to {
                                        showImportMenu = false; onImportManually(EConfigType.SOCKS.value)
                                    },
                                    R.string.menu_item_import_config_manually_http to {
                                        showImportMenu = false; onImportManually(EConfigType.HTTP.value)
                                    },
                                    R.string.menu_item_import_config_manually_trojan to {
                                        showImportMenu = false; onImportManually(EConfigType.TROJAN.value)
                                    },
                                    R.string.menu_item_import_config_manually_wireguard to {
                                        showImportMenu = false; onImportManually(EConfigType.WIREGUARD.value)
                                    },
                                    R.string.menu_item_import_config_manually_hysteria2 to {
                                        showImportMenu = false; onImportManually(EConfigType.HYSTERIA2.value)
                                    },
                                ).forEach { (stringRes, action) ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(stringRes)) },
                                        onClick = action
                                    )
                                }
                            }
                        }
                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    painterResource(R.drawable.ic_more_vert_24dp),
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                scrollState = moreMenuScrollState,
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .heightIn(max = maxMenuHeight)
                                    .verticalScrollbar(moreMenuScrollState)
                            ) {
                                listOf(
                                    R.string.title_service_restart to {
                                        showMenu = false; onRestartService()
                                    },
                                    R.string.title_del_all_config to {
                                        showMenu = false; showDelAllConfirm = true
                                    },
                                    R.string.title_del_duplicate_config to {
                                        showMenu = false; showDelDuplicateConfirm = true
                                    },
                                    R.string.title_del_invalid_config to {
                                        showMenu = false; showDelInvalidConfirm = true
                                    },
                                    R.string.title_export_all to {
                                        showMenu = false; onExportAll()
                                    },
                                    R.string.title_real_ping_all_server to {
                                        showMenu = false; onRealPingAll()
                                    },
                                    R.string.title_locate_selected_config to {
                                        showMenu = false; onLocateSelectedServer()
                                    },
                                    R.string.title_sort_by_test_results to {
                                        showMenu = false; onSortByTestResults()
                                    },
                                    R.string.title_sub_update to {
                                        showMenu = false; onSubUpdate()
                                    },
                                ).forEach { (stringRes, action) ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(stringRes)) },
                                        onClick = action
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            Column {
                AppDivider()
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    listOf(
                        Triple(R.drawable.ic_home_24dp, R.string.nav_home, TAB_HOME),
                        Triple(R.drawable.ic_dns_24dp, R.string.nav_servers, TAB_SERVERS),
                        Triple(R.drawable.ic_more_horiz_24dp, R.string.nav_more, TAB_MORE),
                    ).forEach { (iconRes, labelRes, tab) ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                val iconScale by animateFloatAsState(
                                    targetValue = if (selectedTab == tab) 1.15f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "navIconScale"
                                )
                                Icon(
                                    painterResource(iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.scale(iconScale)
                                )
                            },
                            label = { Text(stringResource(labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.inverseOnSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.inverseSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(animationSpec = tween(260)) { dir * it / 10 } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(200)) { -dir * it / 10 } + fadeOut(tween(120)))
                },
                label = "tabContent",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    TAB_HOME -> HomeTab(
                        isRunning = isRunning,
                        statusText = displayText,
                        selectedProfile = selectedProfile,
                        isDarkTheme = isDarkTheme,
                        onConnectClick = onFabClick,
                        onStatusClick = onTestClick,
                        onOpenServers = { selectedTab = TAB_SERVERS },
                        onImportClipboard = onImportClipboard,
                        onImportQRcode = onImportQRcode
                    )

                    TAB_SERVERS -> Column(modifier = Modifier.fillMaxSize()) {
                        if (groups.isNotEmpty()) {
                            if (groups.size > 1) {
                                GroupTabBar(
                                    groups = groups,
                                    selectedTabIndex = pagerState.currentPage.coerceIn(0, groups.lastIndex),
                                    mainViewModel = mainViewModel,
                                    onTabClick = { targetIndex ->
                                        scope.launch {
                                            pagerState.navigateToPageOptimized(
                                                targetPage = targetIndex,
                                                animateAdjacentPage = true
                                            )
                                        }
                                    }
                                )
                            }

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = true,
                                beyondViewportPageCount = 1,
                                key = { page -> groups.getOrNull(page)?.id ?: "group-page-$page" }
                            ) { page ->
                                val group = groups.getOrNull(page) ?: return@HorizontalPager

                                GroupPagerPage(
                                    groupId = group.id,
                                    mainViewModel = mainViewModel,
                                    selectedGuid = selectedGuid,
                                    doubleColumnDisplay = doubleColumnDisplay,
                                    confirmRemove = confirmRemove,
                                    searchQuery = searchQuery,
                                    lazyListStates = lazyListStates,
                                    lazyGridStates = lazyGridStates,
                                    onSelectServer = onSelectServer,
                                    onEditServer = onEditServer,
                                    onShareServer = { guid, profile ->
                                        shareTarget = Triple(guid, profile, false)
                                    },
                                    onMoreServer = { guid, profile ->
                                        shareTarget = Triple(guid, profile, true)
                                    },
                                    onRemoveServer = { guid ->
                                        if (confirmRemove) showRemoveConfirm = guid
                                        else onRemoveServer(guid)
                                    },
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        top = 12.dp,
                                        end = 16.dp,
                                        bottom = 16.dp
                                    )
                                )
                            }
                        }
                    }

                    else -> MoreTab(onNavigate = onNavigate)
                }
            }
        }
    }
}

@Composable
private fun ServerListPage(
    servers: List<ServersCache>,
    selectedGuid: String?,
    canReorder: Boolean,
    doubleColumnDisplay: Boolean,
    subscriptionId: String,
    confirmRemove: Boolean,
    groupId: String,
    lazyListStates: MutableMap<String, LazyListState>,
    lazyGridStates: MutableMap<String, LazyGridState>,
    onSelectServer: (String) -> Unit,
    onEditServer: (String, ProfileItem) -> Unit,
    onShareServer: (String, ProfileItem) -> Unit,
    onMoreServer: (String, ProfileItem) -> Unit,
    onRemoveServer: (String) -> Unit,
    onSwapServer: (Int, Int) -> Unit,
    contentPadding: PaddingValues
) {
    if (doubleColumnDisplay) {
        val gridState = remember(groupId) {
            lazyGridStates.getOrPut(groupId) { LazyGridState() }
        }
        val reorderableGridState = if (canReorder) {
            rememberReorderableLazyGridState(gridState) { from, to ->
                onSwapServer(from.index, to.index)
            }
        } else null

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(gridState),
            contentPadding = contentPadding
        ) {
            itemsIndexed(items = servers, key = { _, item -> item.guid }) { index, serverCache ->
                val content: @Composable () -> Unit = {
                    ServerItemColumn(
                        index = index,
                        serverCache = serverCache,
                        selectedGuid = selectedGuid,
                        subscriptionId = subscriptionId,
                        doubleColumnDisplay = true,
                        onSelectServer = onSelectServer,
                        onEditServer = onEditServer,
                        onShareServer = onShareServer,
                        onMoreServer = onMoreServer,
                        onRemoveServer = onRemoveServer
                    )
                }
                if (canReorder && reorderableGridState != null) {
                    ReorderableItem(
                        reorderableGridState,
                        key = serverCache.guid
                    ) { isDragging ->
                        ReorderableGridItem(
                            scope = this,
                            isDragging = isDragging
                        ) { content() }
                    }
                } else {
                    Box(modifier = Modifier.animateItem()) { content() }
                }
            }
        }
    } else {
        val listState = remember(groupId) {
            lazyListStates.getOrPut(groupId) { LazyListState() }
        }
        val reorderableState = if (canReorder) {
            rememberReorderableLazyListState(listState) { from, to ->
                onSwapServer(from.index, to.index)
            }
        } else null

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(listState),
            contentPadding = contentPadding
        ) {
            itemsIndexed(items = servers, key = { _, item -> item.guid }) { index, serverCache ->
                if (canReorder && reorderableState != null) {
                    ReorderableItem(
                        reorderableState,
                        key = serverCache.guid
                    ) { isDragging ->
                        ReorderableListItem(
                            scope = this,
                            isDragging = isDragging
                        ) {
                            ServerItemRow(
                                index = index,
                                serverCache = serverCache,
                                selectedGuid = selectedGuid,
                                subscriptionId = subscriptionId,
                                onSelectServer = onSelectServer,
                                onEditServer = onEditServer,
                                onShareServer = onShareServer,
                                onMoreServer = onMoreServer,
                                onRemoveServer = onRemoveServer
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.animateItem()) {
                        ServerItemRow(
                            index = index,
                            serverCache = serverCache,
                            selectedGuid = selectedGuid,
                            subscriptionId = subscriptionId,
                            onSelectServer = onSelectServer,
                            onEditServer = onEditServer,
                            onShareServer = onShareServer,
                            onMoreServer = onMoreServer,
                            onRemoveServer = onRemoveServer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerItemRow(
    index: Int,
    serverCache: ServersCache,
    selectedGuid: String?,
    subscriptionId: String,
    onSelectServer: (String) -> Unit,
    onEditServer: (String, ProfileItem) -> Unit,
    onShareServer: (String, ProfileItem) -> Unit,
    onMoreServer: (String, ProfileItem) -> Unit,
    onRemoveServer: (String) -> Unit
) {
    val profile = serverCache.profile
    val subRemarks = if (subscriptionId.isEmpty()) {
        MmkvManager.decodeSubscription(profile.subscriptionId)?.remarks?.firstOrNull()
            ?.toString() ?: ""
    } else ""

    ServerListItem(
        index = index,
        remarks = profile.remarks,
        statistics = profile.description.nullIfBlank()
            ?: AngConfigManager.generateDescription(profile),
        typeDescription = getProtocolDescription(profile),
        testResult = serverCache.testDelayString,
        testDelayMillis = serverCache.testDelayMillis,
        isSelected = serverCache.guid == selectedGuid,
        subscriptionRemarks = subRemarks,
        doubleColumnDisplay = false,
        onClick = { onSelectServer(serverCache.guid) },
        onShare = { onShareServer(serverCache.guid, profile) },
        onEdit = { onEditServer(serverCache.guid, profile) },
        onRemove = { onRemoveServer(serverCache.guid) },
        onMore = { onMoreServer(serverCache.guid, profile) }
    )
}

@Composable
private fun ServerItemColumn(
    index: Int,
    serverCache: ServersCache,
    selectedGuid: String?,
    subscriptionId: String,
    doubleColumnDisplay: Boolean,
    onSelectServer: (String) -> Unit,
    onEditServer: (String, ProfileItem) -> Unit,
    onShareServer: (String, ProfileItem) -> Unit,
    onMoreServer: (String, ProfileItem) -> Unit,
    onRemoveServer: (String) -> Unit
) {
    val profile = serverCache.profile
    val subRemarks = if (subscriptionId.isEmpty()) {
        MmkvManager.decodeSubscription(profile.subscriptionId)?.remarks?.firstOrNull()?.toString() ?: ""
    } else ""

    Column {
        ServerListItem(
            index = index,
            remarks = profile.remarks,
            statistics = profile.description.nullIfBlank() ?: AngConfigManager.generateDescription(profile),
            typeDescription = getProtocolDescription(profile),
            testResult = serverCache.testDelayString,
            testDelayMillis = serverCache.testDelayMillis,
            isSelected = serverCache.guid == selectedGuid,
            subscriptionRemarks = subRemarks,
            doubleColumnDisplay = doubleColumnDisplay,
            onClick = { onSelectServer(serverCache.guid) },
            onEdit = { onEditServer(serverCache.guid, profile) },
            onShare = { onShareServer(serverCache.guid, profile) },
            onRemove = { onRemoveServer(serverCache.guid) },
            onMore = { onMoreServer(serverCache.guid, profile) }
        )
    }
}

@Composable
fun ServerListItem(
    index: Int,
    remarks: String,
    statistics: String,
    typeDescription: String,
    testResult: String,
    testDelayMillis: Long,
    isSelected: Boolean,
    subscriptionRemarks: String,
    doubleColumnDisplay: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colorFabActive else Color.Transparent,
        animationSpec = tween(300),
        label = "cardBorder"
    )
    val badgeColor by animateColorAsState(
        targetValue = if (isSelected) colorFabActive else MaterialTheme.colorScheme.inverseSurface,
        animationSpec = tween(300),
        label = "cardBadge"
    )
    val badgeTextColor by animateColorAsState(
        targetValue = if (isSelected) colorBrandCream else MaterialTheme.colorScheme.inverseOnSurface,
        animationSpec = tween(300),
        label = "cardBadgeText"
    )

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier.fillMaxWidth().then(dragModifier)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor),
                    Alignment.Center
                ) {
                    Text(
                        "%02d".format(index + 1),
                        Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = badgeTextColor
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(remarks, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge.copy(lineBreak = LineBreak.Paragraph), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (doubleColumnDisplay) {
                    IconButton(onClick = onMore, Modifier.size(36.dp)) {
                        Icon(painterResource(R.drawable.ic_more_vert_24dp), null, Modifier.size(24.dp))
                    }
                } else {
                    IconButton(onClick = onShare, Modifier.size(36.dp)) { Icon(painterResource(R.drawable.ic_share_24dp), null, Modifier.size(24.dp)) }
                    IconButton(onClick = onEdit, Modifier.size(36.dp)) { Icon(painterResource(R.drawable.ic_edit_24dp), null, Modifier.size(24.dp)) }
                    IconButton(onClick = onRemove, Modifier.size(36.dp)) { Icon(painterResource(R.drawable.ic_delete_24dp), null, Modifier.size(24.dp)) }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (subscriptionRemarks.isNotBlank()) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), Alignment.Center) {
                        Text(subscriptionRemarks.take(1).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(statistics, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(typeDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                AnimatedContent(
                    targetState = testResult,
                    transitionSpec = {
                        (slideInVertically(animationSpec = tween(220)) { it } + fadeIn(tween(220))) togetherWith
                            (slideOutVertically(animationSpec = tween(120)) { -it } + fadeOut(tween(120)))
                    },
                    label = "pingResult"
                ) { result ->
                    Text(result, style = MaterialTheme.typography.bodySmall, color = if (testDelayMillis < 0L) colorPingRed else colorPing, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun MoreTab(onNavigate: (String) -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .verticalScrollbar(scrollState)
    ) {
        Text(
            text = stringResource(R.string.nav_more),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
        )
        listOf(
            Triple(R.drawable.ic_subscriptions_24dp, R.string.title_sub_setting, "sub_setting"),
            Triple(R.drawable.ic_per_apps_24dp, R.string.per_app_proxy_settings, "per_app_proxy"),
            Triple(R.drawable.ic_routing_24dp, R.string.routing_settings_title, "routing_setting"),
            Triple(R.drawable.ic_file_24dp, R.string.title_user_asset_setting, "user_asset"),
            Triple(R.drawable.ic_settings_24dp, R.string.title_settings, "settings"),
        ).forEach { (iconRes, labelRes, route) ->
            SettingsMenuItem(
                icon = painterResource(iconRes),
                title = stringResource(labelRes),
                onClick = { onNavigate(route) }
            )
        }
        AppDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        listOf(
            Triple(R.drawable.ic_promotion_24dp, R.string.title_pref_promotion, "promotion"),
            Triple(R.drawable.ic_logcat_24dp, R.string.title_logcat, "logcat"),
            Triple(R.drawable.ic_check_update_24dp, R.string.update_check_for_update, "check_update"),
            Triple(R.drawable.ic_restore_24dp, R.string.title_configuration_backup_restore, "backup_restore"),
            Triple(R.drawable.ic_about_24dp, R.string.title_about, "about"),
        ).forEach { (iconRes, labelRes, route) ->
            SettingsMenuItem(
                icon = painterResource(iconRes),
                title = stringResource(labelRes),
                onClick = { onNavigate(route) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun getProtocolDescription(profile: ProfileItem): String {
    if (profile.configType.isComplexType()) return profile.configType.name
    val parts = mutableListOf(profile.configType.name)
    profile.network?.let { net ->
        if (net.isNotBlank() && !net.equals("tcp", ignoreCase = true)) parts.add(net)
    }
    profile.security?.let { sec ->
        if (sec.isNotBlank()) {
            if (profile.insecure == true && sec.equals("tls", ignoreCase = true)) {
                parts.add("$sec insecure")
            } else {
                parts.add(sec)
            }
        }
    }
    return parts.joinToString(" / ")
}
