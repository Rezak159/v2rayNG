package com.v2ray.ang.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.v2ray.ang.extension.DIVISOR
import com.v2ray.ang.extension.THRESHOLD
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.AppUpdate
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.main.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class A4Tab(val label: String) {
    Home("Главная"),
    Servers("Серверы"),
    Settings("Настройки"),
}

/**
 * A4BottomNav рисуется поверх контента (см. A4MainScreen), а не в потоке разметки,
 * поэтому вкладки должны сами резервировать этот отступ снизу — иначе последние
 * элементы списков оказываются под плавающей капсулой и до них не долистать.
 * Значение = верхний(6dp) + нижний(18dp) паддинг капсулы + её высота(58dp) + внутренний паддинг(6dp*2).
 * Нижний паддинг включает небольшую невидимую зону захвата под капсулой:
 * мимо-тапы гасятся вместо того, чтобы попадать по соседней карточке сервера
 * (сверху и по бокам такой проблемы не было, туда лишний отступ не добавляли).
 * 64dp пробовали раньше — капсула ощутимо уезжала вверх от нижнего края,
 * выглядело как «нав-бар вырос вдвое», поэтому держим отступ небольшим.
 */
internal val A4BottomNavClearance = 94.dp

/** A4 visual shell over the original view-model and VPN service. */
@Composable
fun A4MainScreen(
    mainViewModel: MainViewModel,
    onConnectionClick: () -> Unit,
    onSelectServer: (String) -> Unit,
    onOpenLogcat: () -> Unit,
    onOpenPerAppProxy: () -> Unit,
    onImportSubscription: (String) -> Unit,
    appUpdate: AppUpdate?,
    isDownloadingUpdate: Boolean,
    downloadProgress: Float,
    onInstallUpdate: () -> Unit,
) {
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    // Читаем state.groups здесь, во внешнем scope, чтобы он пересобирался после
    // импорта подписки. Иначе state читается только внутри дочерних лямбд, внешний
    // scope не подписан на изменения — и экран не переключался бы до перезапуска.
    val hasUsableSubscription = remember(state.groups) {
        MmkvManager.decodeSubscriptions().any { subscription ->
            MmkvManager.decodeServerList(subscription.guid).isNotEmpty()
        }
    }

    A4Theme {
        Box(Modifier.fillMaxSize()) {
            if (!hasUsableSubscription) {
                SubscriptionEntry(
                    isLoading = isLoading,
                    onImportSubscription = onImportSubscription,
                )
            } else {
                A4AppHome(
                    mainViewModel = mainViewModel,
                    isRunning = state.isRunning,
                    isTesting = state.isTesting,
                    selectedGroupId = state.selectedGroupId,
                    selectedGuid = state.selectedGuid,
                    onConnectionClick = onConnectionClick,
                    onSelectServer = onSelectServer,
                    onOpenLogcat = onOpenLogcat,
                    onOpenPerAppProxy = onOpenPerAppProxy,
                )
            }
            appUpdate?.let { update ->
                A4UpdateBanner(
                    update = update,
                    isDownloading = isDownloadingUpdate,
                    downloadProgress = downloadProgress,
                    onClick = onInstallUpdate,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun A4UpdateBanner(
    update: AppUpdate,
    isDownloading: Boolean,
    downloadProgress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(A4Ink)
            .clickable(enabled = !isDownloading, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.ArrowDownward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isDownloading) "Скачиваем обновление…" else "Доступно обновление ${update.versionName}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                if (!isDownloading && update.notes.isNotBlank()) {
                    Text(
                        update.notes,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                if (isDownloading) "${(downloadProgress * 100).roundToInt()}%" else "ОБНОВИТЬ",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
                color = A4Red,
            )
        }
        if (isDownloading) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(downloadProgress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(A4Red),
                )
            }
        }
    }
}

@Composable
private fun A4AppHome(
    mainViewModel: MainViewModel,
    isRunning: Boolean,
    isTesting: Boolean,
    selectedGroupId: String,
    selectedGuid: String?,
    onConnectionClick: () -> Unit,
    onSelectServer: (String) -> Unit,
    onOpenLogcat: () -> Unit,
    onOpenPerAppProxy: () -> Unit,
) {
    val servers by mainViewModel.serversForGroup(selectedGroupId).collectAsStateWithLifecycle()
    // speed/session держим как State и отдаём вниз лямбдами: тогда тик раз в секунду
    // рекомпозит только плитку статистики, а не весь дом с кнопкой и вкладками.
    val speedState = mainViewModel.proxySpeed.collectAsStateWithLifecycle()
    val selectedServer = servers.firstOrNull { it.guid == selectedGuid } ?: servers.firstOrNull()
    // Активная подписка для карточки трафика; пересчитывается после апдейта подписки.
    val subscription = remember(selectedGuid, servers) { activeSubscription() }
    val isSelectedServerReady = selectedServer != null && selectedServer.guid == selectedGuid
    val canControlConnection = isRunning || isSelectedServerReady
    var tab by remember { mutableStateOf(A4Tab.Home) }
    // Вход в список серверов анимируем только один раз за сессию экрана: сам таб
    // пересоздаётся при каждом переключении вкладок (AnimatedContent), а флаг
    // живёт здесь и переживает такие переключения.
    var serversEntranceShown by remember { mutableStateOf(false) }
    // Полноценный пинг всех серверов один раз за сессию: срабатывает, как только
    // список серверов впервые загрузился — то есть уже к первому открытию
    // Серверов результаты обычно готовы или вот-вот будут.
    var autoPingTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(servers) {
        if (!autoPingTriggered && servers.isNotEmpty()) {
            autoPingTriggered = true
            mainViewModel.testAllRealPing()
        }
    }

    // локальная фаза «подключаемся»: сервис знает только вкл/выкл
    var connecting by remember { mutableStateOf(false) }
    val conn = when {
        isRunning -> A4ConnState.Connected
        connecting -> A4ConnState.Connecting
        else -> A4ConnState.Disconnected
    }
    LaunchedEffect(isRunning) {
        if (isRunning) connecting = false
    }
    LaunchedEffect(connecting) {
        if (connecting) {
            delay(15_000)
            connecting = false
        }
    }

    // таймер сессии — от момента реального старта туннеля
    val sessionSeconds = remember { mutableLongStateOf(0L) }
    LaunchedEffect(conn) {
        if (conn == A4ConnState.Connected) {
            val stored = MmkvManager.decodeSettingsLong(AppConfig.A4_CONNECT_TS, 0L)
            val start = if (stored > 0L) stored else System.currentTimeMillis()
            while (true) {
                sessionSeconds.longValue = ((System.currentTimeMillis() - start) / 1000L).coerceAtLeast(0L)
                delay(1000)
            }
        } else {
            sessionSeconds.longValue = 0L
        }
    }

    LaunchedEffect(selectedServer?.guid, selectedGuid) {
        if (selectedGuid == null && selectedServer != null) onSelectServer(selectedServer.guid)
    }

    val selectServer: (String) -> Unit = { guid ->
        if (isRunning && guid != selectedGuid) connecting = true
        onSelectServer(guid)
    }

    val glassBackdrop = rememberA4GlassBackdrop()
    Box(Modifier.fillMaxSize().background(A4Paper)) {
        // В источник попадает и фон, и содержимое вкладок. Нижняя навигация
        // рисуется снаружи: иначе стекло размывало бы само себя.
        Box(Modifier.fillMaxSize().a4GlassBackdropSource(glassBackdrop)) {
            // Точки декора нарисованы в фиксированной точке экрана (верх-право) не
            // завися от вкладки — на Серверах туда садится кнопка «Пинг» и они
            // накладываются друг на друга. Точки — часть «героя» Главной, на
            // остальных вкладках их не рисуем.
            A4Backdrop(active = conn == A4ConnState.Connected, showDots = tab == A4Tab.Home)
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                A4TopBar()
                Box(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            (slideInHorizontally(
                                initialOffsetX = { dir * it / 5 },
                                animationSpec = tween(300),
                            ) + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { -dir * it / 5 },
                                    animationSpec = tween(200),
                                ) + fadeOut(tween(150)))
                        },
                        label = "tabs",
                    ) { t ->
                        when (t) {
                            A4Tab.Home -> HomeTab(
                                conn = conn,
                                connectionEnabled = canControlConnection,
                                downBps = { speedState.value.first },
                                upBps = { speedState.value.second },
                                sessionSeconds = { sessionSeconds.longValue },
                                server = selectedServer,
                                subscription = subscription,
                                onConnectionClick = {
                                    if (!isRunning) connecting = true
                                    onConnectionClick()
                                },
                                onOpenServers = { tab = A4Tab.Servers },
                            )
                            A4Tab.Servers -> ServersTab(
                                mainViewModel = mainViewModel,
                                servers = servers,
                                selectedGuid = selectedGuid,
                                isTesting = isTesting,
                                connected = conn == A4ConnState.Connected,
                                onSelectServer = selectServer,
                                onTestPing = { mainViewModel.testAllRealPing() },
                                animateEntrance = !serversEntranceShown,
                                onEntranceShown = { serversEntranceShown = true },
                            )
                            A4Tab.Settings -> A4SettingsTab(
                                onOpenLogcat = onOpenLogcat,
                                onOpenPerAppProxy = onOpenPerAppProxy,
                            )
                        }
                    }
                }
            }
        }
        A4BottomNav(
            current = tab,
            backdrop = glassBackdrop,
            modifier = Modifier.align(Alignment.BottomCenter),
            onSelect = { tab = it },
        )
    }
}

// ---------------------------------------------------------------------------
// Каркас: шапка, нижняя навигация, фон
// ---------------------------------------------------------------------------

@Composable
private fun A4TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_a4),
            contentDescription = null,
            modifier = Modifier.size(34.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            buildAnnotatedString {
                append("a")
                withStyle(SpanStyle(color = A4Red)) { append("4") }
                append("vpn")
            },
            style = TextStyle(
                fontFamily = A4Unbounded,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = (-0.22).sp,
            ),
            color = A4Ink,
        )
    }
}

@Composable
private fun A4BottomNav(
    current: A4Tab,
    backdrop: A4GlassBackdrop,
    modifier: Modifier = Modifier,
    onSelect: (A4Tab) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lensX = remember { Animatable(0f) }
    var lensInMotion by remember { mutableStateOf(false) }
    var lensStretch by remember { mutableStateOf(1f) }
    var lastFingerX by remember { mutableStateOf<Float?>(null) }
    var dragTarget by remember { mutableStateOf<A4Tab?>(null) }
    val glassEnabled by MmkvManager.rememberMmkvBool(AppConfig.PREF_LIQUID_GLASS_ENABLED, true)
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            // Капсула плавает над контентом, а не в потоке разметки, поэтому вокруг
            // неё всегда остаётся немного пустого места (внешние отступы снизу).
            // Без поглощения тапов здесь промах мимо капсулы проваливается в список
            // под ней — выглядит как «нажал на Главную, а выбрался сервер снизу
            // списка». pointerInput должен стоять ДО .padding(...), иначе он меряет
            // размер уже урезанной padding'ом области — то есть только саму видимую
            // капсулу, где клики по вкладкам и так уже работали, а не отступы вокруг
            // неё. До самих вкладок тапы всё равно доходят раньше — их клики уже
            // отработали за счёт порядка диспетчеризации (дети раньше родителя).
            .pointerInput(Unit) { detectTapGestures {} }
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 18.dp),
    ) {
        // Плавающая стеклянная капсула из ветки liquid-glass.
        Box(
            Modifier
                .fillMaxWidth()
                .a4LiquidGlass(CircleShape, backdrop, milk = 0.82f, elevation = 10.dp, opaque = !glassEnabled)
                .padding(6.dp),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(58.dp)) {
                val itemW = maxWidth / A4Tab.entries.size
                val density = LocalDensity.current
                val itemWPx = with(density) { itemW.toPx() }
                val settledX by animateDpAsState(
                    targetValue = itemW * (dragTarget ?: current).ordinal,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 500f),
                    label = "pill",
                )
                fun tabAt(x: Float): A4Tab = A4Tab.entries[
                    (x / itemWPx).toInt().coerceIn(0, A4Tab.entries.lastIndex)
                ]
                val lensOffset = if (lensInMotion) lensX.value else with(density) { settledX.toPx() }
                val lensScaleX by animateFloatAsState(
                    targetValue = lensStretch,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
                    label = "lensStretchX",
                )
                val lensScaleY by animateFloatAsState(
                    targetValue = 2f - lensStretch,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
                    label = "lensStretchY",
                )
                // Линза подсвечивает активную вкладку поверх общего стекла.
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                lensOffset.roundToInt(),
                                0,
                            )
                        }
                        .width(itemW)
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = lensScaleX
                            scaleY = lensScaleY
                        }
                        .padding(horizontal = 3.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.92f),
                                    Color.White.copy(alpha = 0.55f),
                                ),
                            ),
                        )
                        .border(1.dp, Color.White, CircleShape),
                )
                Row(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(itemWPx, current) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { position ->
                                    dragTarget = tabAt(position.x)
                                    lensInMotion = true
                                    lastFingerX = position.x
                                    val desiredX = (position.x - itemWPx / 2f)
                                        .coerceIn(0f, size.width - itemWPx)
                                    scope.launch {
                                        lensX.snapTo(current.ordinal * itemWPx)
                                        // Мягкий «вылет» линзы к пальцу — единственное место,
                                        // где во время долгого тапа ещё нужна пружина.
                                        lensX.animateTo(desiredX, spring(dampingRatio = 0.7f, stiffness = 600f))
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, _ ->
                                    val target = tabAt(change.position.x)
                                    if (target != dragTarget) {
                                        dragTarget = target
                                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    }
                                    val speed = abs(change.position.x - (lastFingerX ?: change.position.x))
                                    lensStretch = (1f + speed / itemWPx * 0.22f).coerceAtMost(1.10f)
                                    lastFingerX = change.position.x
                                    val desiredX = (change.position.x - itemWPx / 2f)
                                        .coerceIn(0f, size.width - itemWPx)
                                    // Пока палец реально двигает линзу, она должна идти с ним
                                    // один-в-один: пружина на каждое touch-move событие только
                                    // гонится следом и вечно отстаёт — это и читалось как «лаг»/
                                    // «топорность». Пружины остаются только на старте и финале.
                                    scope.launch { lensX.snapTo(desiredX) }
                                    change.consume()
                                },
                                onDragEnd = {
                                    // Переключаем вкладку сразу, а не после того как долетит
                                    // анимация линзы: раньше onSelect ждал animateTo на общем
                                    // Animatable, а следующий жест перезапускал ту же анимацию
                                    // и отменял корутину до onSelect — свайп то срабатывал,
                                    // то нет, то с задержкой.
                                    val target = dragTarget ?: current
                                    lensStretch = 1f
                                    lastFingerX = null
                                    dragTarget = null
                                    lensInMotion = false
                                    if (target != current) onSelect(target)
                                },
                                onDragCancel = {
                                    lensStretch = 1f
                                    lastFingerX = null
                                    dragTarget = null
                                    lensInMotion = false
                                },
                            )
                        },
                ) {
                    A4Tab.entries.forEach { t ->
                        val active = t == (dragTarget ?: current)
                        val color by animateColorAsState(
                            targetValue = if (active) A4Red else A4TextMuted,
                            animationSpec = tween(220),
                            label = "tabColor",
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    if (t != current) {
                                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        onSelect(t)
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            TabIcon(tab = t, color = color)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                t.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                ),
                                color = color,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Иконки вкладок — гладкие округлые заливные Material-иконки; цвет задаётся снаружи. */
@Composable
private fun TabIcon(tab: A4Tab, color: Color) {
    val icon = when (tab) {
        A4Tab.Home -> Icons.Rounded.Home
        A4Tab.Servers -> Icons.Rounded.Public
        A4Tab.Settings -> Icons.Rounded.Settings
    }
    Icon(
        imageVector = icon,
        contentDescription = tab.label,
        tint = color,
        modifier = Modifier.size(24.dp),
    )
}

/**
 * Спокойный «журнальный» фон: контур круга, сетка точек, вращающийся
 * треугольник и тонкая линия — всё еле заметное и медленно дрейфует.
 * Сетка точек в правом верхнем углу — индикатор VPN: серая, когда выключен,
 * красная, когда включён.
 */
@Composable
internal fun A4Backdrop(active: Boolean = false, showDots: Boolean = true) {
    val infinite = rememberInfiniteTransition(label = "backdrop")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing)),
        label = "drift",
    )
    val rot by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(90000, easing = LinearEasing)),
        label = "rotation",
    )
    // плавный переход цвета точек серый↔красный при смене статуса VPN
    val warm by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(400),
        label = "dotsWarm",
    )
    val dotPulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "dotsPulse",
    )
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val drift = sin(t * 2f * PI).toFloat()

        drawCircle(
            color = A4Ink,
            radius = w * 0.38f,
            center = Offset(-w * 0.05f, h * 0.30f + drift * 14f),
            alpha = 0.045f,
            style = Stroke(1.5f.dp.toPx()),
        )

        if (showDots) {
            val step = 18.dp.toPx()
            val dotColor = lerp(A4TextMuted, A4Red, warm)
            val dotDrift = if (active) drift * 6f else 0f
            for (i in 0..2) {
                for (j in 0..2) {
                    val wave = (sin(dotPulse * 2f * PI - (i + j) * 0.8f).toFloat() + 1f) / 2f
                    val pulse = if (active) wave else 0f
                    drawCircle(
                        color = dotColor,
                        radius = (2.5f + pulse * 1.2f).dp.toPx(),
                        center = Offset(w * 0.78f + i * step, h * 0.10f + j * step + dotDrift),
                        alpha = 0.28f + pulse * 0.52f,
                    )
                }
            }
        }

        val pivot = Offset(w * 0.12f, h * 0.80f)
        rotate(rot, pivot = pivot) {
            val tri = Path().apply {
                moveTo(pivot.x, pivot.y - 34f)
                lineTo(pivot.x + 30f, pivot.y + 20f)
                lineTo(pivot.x - 30f, pivot.y + 20f)
                close()
            }
            drawPath(tri, A4Red, alpha = 0.08f, style = Stroke(2.dp.toPx()))
        }

        drawLine(
            color = A4Ink,
            start = Offset(w * 0.62f, h * 0.86f),
            end = Offset(w * 0.95f, h * 0.72f),
            strokeWidth = 1.dp.toPx(),
            alpha = 0.06f,
        )
    }
}

// ---------------------------------------------------------------------------
// Главная
// ---------------------------------------------------------------------------

@Composable
private fun HomeTab(
    conn: A4ConnState,
    connectionEnabled: Boolean,
    downBps: () -> Long,
    upBps: () -> Long,
    sessionSeconds: () -> Long,
    server: ServersCache?,
    subscription: SubscriptionItem?,
    onConnectionClick: () -> Unit,
    onOpenServers: () -> Unit,
) {
    val serverName = server?.profile?.remarks?.ifBlank { null } ?: "Загружаем сервер…"
    val context = LocalContext.current
    // Android держит один активный VPN-туннель на устройство: если уже поднят
    // чужой, наш either не установится, либо вытеснит его без предупреждения —
    // человек видит зависшее «шифруем трафик» и не понимает почему. Пока мы сами
    // не подключены, раз в пару секунд проверяем, не занят ли VPN-слот кем-то ещё.
    var foreignVpnActive by remember { mutableStateOf(false) }
    LaunchedEffect(conn) {
        if (conn == A4ConnState.Connected) {
            foreignVpnActive = false
            return@LaunchedEffect
        }
        while (true) {
            foreignVpnActive = isForeignVpnActive(context)
            delay(2000)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        AnimatedVisibility(visible = foreignVpnActive) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(A4Red.copy(alpha = 0.12f))
                    .border(1.dp, A4Red.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = A4Red,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "На устройстве уже активен другой VPN — отключи его, иначе a4vpn может не подключиться",
                    style = MaterialTheme.typography.bodySmall,
                    color = A4Ink,
                )
            }
        }
        StatusHeadline(conn)

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            A4PowerButton(conn = conn, enabled = connectionEnabled, onClick = onConnectionClick)
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = conn to connectionEnabled,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
                label = "hint",
            ) { (c, enabled) ->
                Text(
                    text = when {
                        !enabled && c == A4ConnState.Disconnected -> "серверы ещё загружаются"
                        c == A4ConnState.Disconnected -> "нажми, чтобы подключиться"
                        c == A4ConnState.Connecting -> "устанавливаем защищённый туннель"
                        else -> "нажми, чтобы отключиться"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = A4TextMuted,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        AnimatedVisibility(
            visible = conn == A4ConnState.Connected,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            ),
            exit = fadeOut(tween(150)),
        ) {
            Column {
                StatsRow(downBps, upBps, sessionSeconds)
                Spacer(Modifier.height(12.dp))
            }
        }

        ServerCard(
            serverName = serverName,
            pingMs = server?.testDelayMillis ?: 0L,
            shimmer = conn == A4ConnState.Connected,
            onClick = onOpenServers,
        )

        if (subscription != null && subscription.hasTrafficInfo) {
            Spacer(Modifier.height(12.dp))
            TrafficCard(subscription)
        }

        val navInsetBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Spacer(Modifier.height(24.dp + A4BottomNavClearance + navInsetBottom))
    }
}

/** true, если VPN-слот системы занят чужим приложением (наша служба сейчас не запущена). */
private fun isForeignVpnActive(context: Context): Boolean {
    val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = connectivity.activeNetwork ?: return false
    val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
}

/**
 * Отдельная карточка остатка трафика на Главной:
 *   339 ГБ  осталось                 37 дней ⏳
 *   ███████░░░░░░░░░░░░░░░░░░░░  15%
 * При безлимите — просто «Безлимит» без полосы.
 */
@Composable
private fun TrafficCard(sub: SubscriptionItem) {
    val days = sub.daysLeft()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(A4PaperCard)
            .border(1.dp, A4Border, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (sub.isUnlimited) {
                Text("∞ ГБ", style = MaterialTheme.typography.titleMedium, color = A4Ink)
            } else {
                val low = sub.usedFraction() >= 0.9f
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = if (low) A4Red else A4Ink)) {
                            append("${formatGib(sub.remainingBytes())} ГБ")
                        }
                        withStyle(SpanStyle(color = A4TextMuted)) { append("  осталось") }
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.weight(1f))
            val label = sub.expiryShortLabel()
            if (label != null) {
                val urgent = days != null && days < 7
                val tint = if (urgent) A4Red else A4TextMuted
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = tint,
                    )
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        A4TrafficBar(if (sub.isUnlimited) 1f else sub.usedFraction(), Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatusHeadline(conn: A4ConnState) {
    Column {
        Text("ТРАФИК", style = MaterialTheme.typography.displayLarge, color = A4Ink)
        AnimatedContent(
            targetState = conn,
            transitionSpec = {
                (slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) + fadeIn(tween(250))) togetherWith
                    (slideOutVertically(targetOffsetY = { -it / 2 }, animationSpec = tween(180)) +
                        fadeOut(tween(150))) using SizeTransform(clip = false)
            },
            label = "statusWord",
        ) { c ->
            when (c) {
                A4ConnState.Disconnected -> Text(
                    "ОТКРЫТ",
                    style = MaterialTheme.typography.displayLarge,
                    color = A4Red,
                )
                A4ConnState.Connecting -> Row(verticalAlignment = Alignment.Bottom) {
                    Text("ШИФРУЕМ", style = MaterialTheme.typography.displayLarge, color = A4Ink)
                    ConnectingDots()
                }
                A4ConnState.Connected -> MarkerWord("ЗАЩИЩЁН")
            }
        }
    }
}

/** Слово, которое «прокрашивается» красным маркером слева направо. */
@Composable
private fun MarkerWord(text: String) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        sweep.animateTo(1f, tween(500, delayMillis = 100, easing = FastOutSlowInEasing))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge,
        color = lerp(A4Ink, Color.White, sweep.value),
        modifier = Modifier
            .drawBehind {
                drawRoundRect(
                    color = A4Red,
                    size = size.copy(width = size.width * sweep.value),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
            .padding(horizontal = 8.dp),
    )
}

@Composable
private fun ConnectingDots() {
    val t by rememberInfiniteTransition(label = "dots").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "dotsT",
    )
    Row(Modifier.padding(start = 6.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { i ->
            val phase = (sin((t * 2f * Math.PI) - i * 0.9).toFloat() + 1f) / 2f
            Box(
                Modifier
                    .size(7.dp)
                    .graphicsLayer { alpha = 0.25f + phase * 0.75f }
                    .background(A4Red, CircleShape),
            )
        }
    }
}

private val speedUnits = arrayOf("Б", "КБ", "МБ", "ГБ", "ТБ", "ПБ")

// Та же лестница единиц (шаг THRESHOLD/DIVISOR), что и в toTrafficString(),
// который использует уведомление о подключении — раньше тут была отдельная
// самопальная формула с усечением до целых КБ/с, из-за чего скорость ниже
// 1 КБ/с всегда показывала "0".
private fun speedParts(bps: Long): Pair<String, String> {
    var size = bps.toDouble()
    var unitIndex = 0
    while (size >= THRESHOLD && unitIndex < speedUnits.size - 1) {
        size /= DIVISOR
        unitIndex++
    }
    val value = if (unitIndex == 0) size.toLong().toString() else "%.1f".format(size)
    return value to "${speedUnits[unitIndex]}/с"
}

private fun sessionParts(sec: Long): Pair<String, String> = when {
    sec >= 3600L -> "%d:%02d".format(sec / 3600, (sec % 3600) / 60) to "часы"
    else -> "%02d:%02d".format(sec / 60, sec % 60) to "мин"
}

@Composable
private fun StatsRow(downBps: () -> Long, upBps: () -> Long, sessionSeconds: () -> Long) {
    val (downValue, downUnit) = speedParts(downBps())
    val (upValue, upUnit) = speedParts(upBps())
    val (sessionValue, sessionUnit) = sessionParts(sessionSeconds())
    // Раньше каждая цифра сидела в своей карточке с рамкой и фоном — три бордера
    // подряд визуально тяжелят строку и съедают высоту. Теперь это просто три
    // колонки с тонкими вертикальными разделителями между ними.
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatCell(Icons.Rounded.ArrowDownward, downValue, downUnit, Modifier.weight(1f))
        VerticalDivider(color = A4Border, modifier = Modifier.height(30.dp))
        StatCell(Icons.Rounded.ArrowUpward, upValue, upUnit, Modifier.weight(1f))
        VerticalDivider(color = A4Border, modifier = Modifier.height(30.dp))
        StatCell(Icons.Rounded.Timer, sessionValue, sessionUnit, Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(icon: ImageVector, value: String, unit: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
        Icon(
            icon,
            contentDescription = null,
            tint = A4TextMuted,
            modifier = Modifier.size(14.dp).padding(bottom = 2.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
            color = A4Ink,
        )
        Spacer(Modifier.width(3.dp))
        Text(
            unit,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = A4TextMuted,
            modifier = Modifier.padding(bottom = 1.dp),
        )
    }
}

@Composable
private fun ServerCard(
    serverName: String,
    pingMs: Long,
    shimmer: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .springClick(scale = 0.98f, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(A4PaperCard)
            .border(1.dp, A4Border, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            A4SectionLabel("СЕРВЕР")
            Spacer(Modifier.height(4.dp))
            ShimmerText(
                text = serverName,
                style = MaterialTheme.typography.titleMedium,
                color = A4Ink,
                shimmer = shimmer,
            )
        }
        if (pingMs > 0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$pingMs мс",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = A4Ink,
                )
                Spacer(Modifier.height(4.dp))
                A4PingBars(pingMs)
            }
            Spacer(Modifier.width(10.dp))
        }
        ChevronRight()
    }
}

/** Тонкий шеврон вправо — вместо иконочной библиотеки. */
@Composable
private fun ChevronRight(color: Color = A4TextMuted) {
    Canvas(Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawLine(color, Offset(w * 0.35f, h * 0.2f), Offset(w * 0.7f, h * 0.5f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.7f, h * 0.5f), Offset(w * 0.35f, h * 0.8f), stroke, StrokeCap.Round)
    }
}

// ---------------------------------------------------------------------------
// Серверы
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServersTab(
    mainViewModel: MainViewModel,
    servers: List<ServersCache>,
    selectedGuid: String?,
    isTesting: Boolean,
    connected: Boolean,
    onSelectServer: (String) -> Unit,
    onTestPing: () -> Unit,
    animateEntrance: Boolean,
    onEntranceShown: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    fun refreshSubscription() {
        if (isRefreshing) return
        scope.launch {
            isRefreshing = true
            try {
                withContext(Dispatchers.IO) {
                    AngConfigManager.updateConfigViaSubAll()
                }
                mainViewModel.setupGroupTab(forceRefresh = true).join()
            } finally {
                isRefreshing = false
            }
        }
    }

    // Один плавный заход всего списка вместо каскада по каждой строке — тот
    // каскад переигрывался при каждом возврате на вкладку и с длинным списком
    // ощутимо копил задержку. Проигрываем один раз за сессию экрана.
    val density = LocalDensity.current
    val entranceOffsetPx = remember { with(density) { 20.dp.toPx() } }
    val entranceProgress = remember { Animatable(if (animateEntrance) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animateEntrance) {
            entranceProgress.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            onEntranceShown()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = ::refreshSubscription,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = entranceProgress.value
                translationY = (1f - entranceProgress.value) * entranceOffsetPx
            },
    ) {
        val navInsetBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 24.dp + A4BottomNavClearance + navInsetBottom,
            ),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Выбери локацию", style = MaterialTheme.typography.headlineMedium, color = A4Ink)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionPillButton(
                        icon = Icons.Rounded.Speed,
                        label = "ПИНГ",
                        busyLabel = "МЕРИМ…",
                        isBusy = isTesting,
                        modifier = Modifier.weight(1f),
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onTestPing()
                    }
                    ActionPillButton(
                        icon = Icons.Rounded.Refresh,
                        label = "ОБНОВИТЬ",
                        busyLabel = "ОБНОВЛЯЕМ…",
                        isBusy = isRefreshing,
                        modifier = Modifier.weight(1f),
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        refreshSubscription()
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            itemsIndexed(servers, key = { _, s -> s.guid }) { _, server ->
                A4ServerRow(
                    server = server,
                    selected = server.guid == selectedGuid,
                    connected = connected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSelectServer(server.guid)
                    },
                )
                Spacer(Modifier.height(10.dp))
            }
            if (servers.isEmpty()) {
                item {
                    Text("Серверы загружаются…", style = MaterialTheme.typography.bodyMedium, color = A4TextMuted)
                }
            }
        }
    }
}

/** Тёмная пилюля-кнопка с иконкой: «Пинг» и «Обновить» на вкладке серверов. */
@Composable
private fun ActionPillButton(
    icon: ImageVector,
    label: String,
    busyLabel: String,
    isBusy: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val spin by rememberInfiniteTransition(label = "actionPillSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "actionPillSpinAngle",
    )
    Row(
        modifier
            .springClick(scale = 0.97f) { if (!isBusy) onClick() }
            .clip(RoundedCornerShape(10.dp))
            .background(A4Ink)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isBusy) {
            Canvas(Modifier.size(14.dp)) {
                rotate(spin) {
                    drawArc(
                        color = Color.White,
                        startAngle = 0f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(1.8f.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
        } else {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(7.dp))
        Text(
            if (isBusy) busyLabel else label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.5.sp),
            color = Color.White,
        )
    }
}

@Composable
private fun A4ServerRow(server: ServersCache, selected: Boolean, connected: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) A4Red else A4Border,
        animationSpec = tween(250),
        label = "border",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = tween(250),
        label = "borderW",
    )
    val profile = server.profile

    Box(
        Modifier
            .fillMaxWidth()
            .springClick(scale = 0.98f, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(A4PaperCard)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        ShimmerText(
            text = profile.remarks.ifBlank { "Сервер" },
            style = MaterialTheme.typography.titleMedium,
            color = A4Ink,
            shimmer = selected && connected,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = 96.dp),
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ServerRowPing(server.testDelayMillis)
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                ) + fadeIn(),
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(A4Ink),
                    contentAlignment = Alignment.Center,
                ) {
                    CheckMark()
                }
            }
        }
    }
}

/** Пинг в ряду сервера: «X мс» + столбики, либо «ошибка» для таймаута. */
@Composable
private fun ServerRowPing(pingMs: Long) {
    when {
        pingMs > 0 -> Column(horizontalAlignment = Alignment.End) {
            Text(
                "$pingMs мс",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = A4Ink,
            )
            Spacer(Modifier.height(4.dp))
            A4PingBars(pingMs)
        }
        pingMs < 0 -> Text(
            "ошибка",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = A4Red,
        )
    }
}

/** Белая галочка, нарисованная руками. */
@Composable
private fun CheckMark() {
    Canvas(Modifier.size(12.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawLine(Color.White, Offset(w * 0.12f, h * 0.55f), Offset(w * 0.4f, h * 0.82f), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.4f, h * 0.82f), Offset(w * 0.88f, h * 0.2f), stroke, StrokeCap.Round)
    }
}

// ---------------------------------------------------------------------------
// Первый запуск: вход через Telegram-бота
// ---------------------------------------------------------------------------

@Composable
private fun SubscriptionEntry(
    isLoading: Boolean,
    onImportSubscription: (String) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(A4Paper),
    ) {
        A4Backdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp))
            A4StaggerIn(0) {
                Image(
                    painter = painterResource(R.drawable.logo_a4),
                    contentDescription = null,
                    modifier = Modifier.size(76.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            A4StaggerIn(1) {
                Text(
                    buildAnnotatedString {
                        append("a")
                        withStyle(SpanStyle(color = A4Red)) { append("4") }
                        append("vpn")
                    },
                    style = TextStyle(
                        fontFamily = A4Unbounded,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = A4Ink,
                )
            }
            Spacer(Modifier.height(36.dp))
            A4StaggerIn(2) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "Ключ доступа",
                        style = MaterialTheme.typography.headlineMedium,
                        color = A4Ink,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Вставь фирменную ссылку из Telegram — приложение настроится само.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = A4TextMuted,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            A4StaggerIn(3) {
                SubscriptionLinkEntry(
                    busy = isLoading,
                    onSubmit = onImportSubscription,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SubscriptionLinkEntry(busy: Boolean, onSubmit: (String) -> Unit) {
    var subscriptionUrl by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val canSubmit = subscriptionUrl.isNotBlank() && !busy

    fun submit() {
        if (!canSubmit) return
        val trimmed = subscriptionUrl.trim()
        if (Uri.parse(trimmed).host != AppConfig.SUB_KEY_HOST) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
            error = true
            return
        }
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        onSubmit(trimmed)
    }

    val borderColor by animateColorAsState(
        targetValue = when {
            error -> A4Red
            focused -> A4Ink
            else -> A4Border
        },
        animationSpec = tween(200),
        label = "keyFieldBorder",
    )

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(A4PaperCard)
                .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            BasicTextField(
                value = subscriptionUrl,
                onValueChange = {
                    subscriptionUrl = it
                    error = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
                enabled = !busy,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = A4Ink),
                cursorBrush = SolidColor(A4Red),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                decorationBox = { innerField ->
                    if (subscriptionUrl.isEmpty()) {
                        Text(
                            "sub.a4secure.xyz/…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = A4TextMuted,
                        )
                    }
                    innerField()
                },
            )
        }
        AnimatedVisibility(visible = error) {
            Column {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Это не похоже на наш ключ — возьми ссылку у бота a4vpn в Telegram",
                    style = MaterialTheme.typography.bodySmall,
                    color = A4Red,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .springClick(scale = 0.97f) { submit() }
                .clip(RoundedCornerShape(10.dp))
                .background(if (canSubmit) A4Red else A4TextMuted)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (busy) "ПОДКЛЮЧАЕМ…" else "ПОДКЛЮЧИТЬ",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, letterSpacing = 1.sp),
                color = Color.White,
            )
        }
    }
}
