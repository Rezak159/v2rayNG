package com.v2ray.ang.ui

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

private enum class A4Tab(val label: String) {
    Home("Главная"),
    Servers("Серверы"),
    Settings("Настройки"),
}

/** A4 visual shell over the original view-model and VPN service. */
@Composable
fun A4MainScreen(
    mainViewModel: MainViewModel,
    onConnectionClick: () -> Unit,
    onImportSubscription: (String, (Boolean) -> Unit) -> Unit,
    onSelectServer: (String) -> Unit,
    onOpenLogcat: () -> Unit,
) {
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()
    // Читаем state.groups здесь, во внешнем scope, чтобы он пересобирался после
    // импорта подписки. Иначе state читается только внутри дочерних лямбд, внешний
    // scope не подписан на изменения — и экран не переключался бы до перезапуска.
    val hasUsableSubscription = remember(state.groups) {
        MmkvManager.decodeSubscriptions().any { subscription ->
            MmkvManager.decodeServerList(subscription.guid).isNotEmpty()
        }
    }

    A4Theme {
        if (!hasUsableSubscription) {
            SubscriptionEntry(
                isLoading = state.isLoading,
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
            )
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
) {
    val servers by mainViewModel.serversForGroup(selectedGroupId).collectAsStateWithLifecycle()
    val speed by mainViewModel.proxySpeed.collectAsStateWithLifecycle()
    val selectedServer = servers.firstOrNull { it.guid == selectedGuid } ?: servers.firstOrNull()
    val isSelectedServerReady = selectedServer != null && selectedServer.guid == selectedGuid
    val canControlConnection = isRunning || isSelectedServerReady
    var tab by remember { mutableStateOf(A4Tab.Home) }

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
    var sessionSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(conn) {
        if (conn == A4ConnState.Connected) {
            val stored = MmkvManager.decodeSettingsLong(AppConfig.A4_CONNECT_TS, 0L)
            val start = if (stored > 0L) stored else System.currentTimeMillis()
            while (true) {
                sessionSeconds = ((System.currentTimeMillis() - start) / 1000L).coerceAtLeast(0L)
                delay(1000)
            }
        } else {
            sessionSeconds = 0L
        }
    }

    LaunchedEffect(selectedServer?.guid, selectedGuid) {
        if (selectedGuid == null && selectedServer != null) onSelectServer(selectedServer.guid)
    }

    val selectServer: (String) -> Unit = { guid ->
        if (isRunning && guid != selectedGuid) connecting = true
        onSelectServer(guid)
    }

    Box(Modifier.fillMaxSize().background(A4Paper)) {
        A4Backdrop()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            A4TopBar(conn)
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
                            downBps = speed.first,
                            upBps = speed.second,
                            sessionSeconds = sessionSeconds,
                            server = selectedServer,
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
                            onSelectServer = selectServer,
                            onTestPing = { mainViewModel.testAllRealPing() },
                        )
                        A4Tab.Settings -> A4SettingsTab(onOpenLogcat = onOpenLogcat)
                    }
                }
            }
            A4BottomNav(tab) { tab = it }
        }
    }
}

// ---------------------------------------------------------------------------
// Каркас: шапка, нижняя навигация, фон
// ---------------------------------------------------------------------------

@Composable
private fun A4TopBar(conn: A4ConnState) {
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
                withStyle(SpanStyle(color = A4Red)) { append("a4") }
                append("vpn")
            },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
            ),
            color = A4Ink,
        )
        Spacer(Modifier.weight(1f))
        ConnDot(conn)
    }
}

/** Точка статуса: серая — офлайн, мигает — подключение, красная с пульсом — онлайн. */
@Composable
private fun ConnDot(conn: A4ConnState) {
    val pulse by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "pulse",
    )
    val color by animateColorAsState(
        targetValue = if (conn == A4ConnState.Disconnected) A4TextMuted else A4Red,
        animationSpec = tween(300),
        label = "dotColor",
    )
    Canvas(Modifier.size(22.dp)) {
        val r = 4.dp.toPx()
        if (conn == A4ConnState.Connected) {
            drawCircle(
                color = A4Red,
                radius = r + pulse * 6.dp.toPx(),
                alpha = (1f - pulse) * 0.45f,
                style = Stroke(1.5f.dp.toPx()),
            )
        }
        val blink = if (conn == A4ConnState.Connecting) {
            0.35f + 0.65f * ((sin(pulse * 4f * PI).toFloat() + 1f) / 2f)
        } else {
            1f
        }
        drawCircle(color = color, radius = r, alpha = blink)
    }
}

@Composable
private fun A4BottomNav(current: A4Tab, onSelect: (A4Tab) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
    ) {
        // плавающая «таблетка»-контейнер
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp), clip = false)
                .clip(RoundedCornerShape(24.dp))
                .background(A4PaperCard)
                .border(1.dp, A4Border, RoundedCornerShape(24.dp))
                .padding(6.dp),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(60.dp)) {
                val itemW = maxWidth / A4Tab.entries.size
                val x by animateDpAsState(
                    targetValue = itemW * current.ordinal,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "pill",
                )
                // красная капсула-подсветка под активной вкладкой
                Box(
                    Modifier
                        .offset(x = x)
                        .width(itemW)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(A4Red.copy(alpha = 0.12f)),
                )
                Row(Modifier.fillMaxSize()) {
                    A4Tab.entries.forEach { t ->
                        val active = t == current
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
 */
@Composable
private fun A4Backdrop() {
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

        val step = 18.dp.toPx()
        for (i in 0..2) {
            for (j in 0..2) {
                drawCircle(
                    color = A4Red,
                    radius = 2.5f.dp.toPx(),
                    center = Offset(w * 0.78f + i * step, h * 0.10f + j * step + drift * 6f),
                    alpha = 0.10f,
                )
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
    downBps: Long,
    upBps: Long,
    sessionSeconds: Long,
    server: ServersCache?,
    onConnectionClick: () -> Unit,
    onOpenServers: () -> Unit,
) {
    val serverName = server?.profile?.remarks?.ifBlank { null } ?: "Загружаем сервер…"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        A4SectionLabel("СТАТУС ЗАЩИТЫ")
        Spacer(Modifier.height(6.dp))
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
            onClick = onOpenServers,
        )

        Spacer(Modifier.height(24.dp))
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

private fun speedParts(bps: Long): Pair<String, String> = when {
    bps >= 1_048_576L -> "%.1f".format(bps / 1_048_576f) to "МБ/с"
    else -> (bps / 1024L).toString() to "КБ/с"
}

private fun sessionParts(sec: Long): Pair<String, String> = when {
    sec >= 3600L -> "%d:%02d".format(sec / 3600, (sec % 3600) / 60) to "часы"
    else -> "%02d:%02d".format(sec / 60, sec % 60) to "мин"
}

@Composable
private fun StatsRow(downBps: Long, upBps: Long, sessionSeconds: Long) {
    val (downValue, downUnit) = speedParts(downBps)
    val (upValue, upUnit) = speedParts(upBps)
    val (sessionValue, sessionUnit) = sessionParts(sessionSeconds)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("ЗАГРУЗКА", downValue, downUnit, Modifier.weight(1f))
        StatTile("ОТДАЧА", upValue, upUnit, Modifier.weight(1f))
        StatTile("СЕССИЯ", sessionValue, sessionUnit, Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(A4PaperCard)
            .border(1.dp, A4Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontSize = 9.sp),
            color = A4TextMuted,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.ExtraBold),
            color = A4Ink,
        )
        Text(unit, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = A4TextMuted)
    }
}

@Composable
private fun ServerCard(
    serverName: String,
    pingMs: Long,
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
            Text(serverName, style = MaterialTheme.typography.titleMedium, color = A4Ink)
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
    onSelectServer: (String) -> Unit,
    onTestPing: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!isRefreshing) {
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
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        A4SectionLabel("СЕРВЕРЫ")
                        Spacer(Modifier.height(6.dp))
                        Text("Выбери локацию", style = MaterialTheme.typography.headlineMedium, color = A4Ink)
                    }
                    PingTestButton(isTesting) {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onTestPing()
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            itemsIndexed(servers, key = { _, s -> s.guid }) { index, server ->
                A4StaggerIn(index) {
                    A4ServerRow(
                        server = server,
                        selected = server.guid == selectedGuid,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSelectServer(server.guid)
                        },
                    )
                }
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

@Composable
private fun PingTestButton(isTesting: Boolean, onClick: () -> Unit) {
    val spin by rememberInfiniteTransition(label = "pingTest").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "pingSpin",
    )
    Row(
        Modifier
            .springClick(scale = 0.95f) { if (!isTesting) onClick() }
            .clip(RoundedCornerShape(8.dp))
            .background(A4Ink)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isTesting) {
            Canvas(Modifier.size(12.dp)) {
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
            Spacer(Modifier.width(8.dp))
        }
        Text(
            if (isTesting) "МЕРИМ…" else "ПИНГ",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.5.sp),
            color = Color.White,
        )
    }
}

@Composable
private fun A4ServerRow(server: ServersCache, selected: Boolean, onClick: () -> Unit) {
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
        Text(
            profile.remarks.ifBlank { "Сервер" },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = 96.dp),
            style = MaterialTheme.typography.titleMedium,
            color = A4Ink,
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
// Первый запуск: ввод ключа подписки
// ---------------------------------------------------------------------------

@Composable
private fun SubscriptionEntry(
    isLoading: Boolean,
    onImportSubscription: (String, (Boolean) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var url by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submit(value: String) {
        val subscriptionUrl = value.trim()
        if (isSubmitting || isLoading) return
        if (subscriptionUrl.isBlank()) {
            errorMessage = "Вставь ключ подписки"
            return
        }

        errorMessage = null
        isSubmitting = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onImportSubscription(subscriptionUrl) { imported ->
            if (!imported) {
                isSubmitting = false
                errorMessage = "Не получилось загрузить подписку. Проверь ключ и попробуй ещё раз."
            }
        }
    }

    val controlsEnabled = !isSubmitting && !isLoading

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
                .imePadding()
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
                        withStyle(SpanStyle(color = A4Red)) { append("a4") }
                        append("vpn")
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                    ),
                    color = A4Ink,
                )
            }
            Spacer(Modifier.height(36.dp))
            A4StaggerIn(2) {
                Column(Modifier.fillMaxWidth()) {
                    A4SectionLabel("ПОДКЛЮЧЕНИЕ")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Вставь ключ подписки",
                        style = MaterialTheme.typography.headlineMedium,
                        color = A4Ink,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Ключ приходит в Telegram-боте после оформления. Скопируй его и вставь сюда.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = A4TextMuted,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            A4StaggerIn(3) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = controlsEnabled,
                        isError = errorMessage != null,
                        placeholder = {
                            Text("https://…", style = MaterialTheme.typography.bodyMedium, color = A4TextMuted)
                        },
                        supportingText = errorMessage?.let { message ->
                            { Text(message, style = MaterialTheme.typography.bodySmall, color = A4Red) }
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = A4Ink),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit(url) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = A4Ink,
                            unfocusedBorderColor = A4Border,
                            errorBorderColor = A4Red,
                            cursorColor = A4Red,
                            focusedTextColor = A4Ink,
                            unfocusedTextColor = A4Ink,
                            focusedContainerColor = A4PaperCard,
                            unfocusedContainerColor = A4PaperCard,
                            errorContainerColor = A4PaperCard,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .springClick(scale = 0.98f) {
                                if (controlsEnabled) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val text = clipboard.primaryClip
                                        ?.takeIf { it.itemCount > 0 }
                                        ?.getItemAt(0)
                                        ?.coerceToText(context)
                                        ?.toString()
                                        .orEmpty()
                                    if (text.isNotBlank()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        url = text
                                        errorMessage = null
                                    }
                                }
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(A4Ink)
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "ВСТАВИТЬ ИЗ БУФЕРА",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, letterSpacing = 1.sp),
                            color = Color.White,
                        )
                    }

                    SubmitButton(
                        busy = isSubmitting || isLoading,
                        onClick = { submit(url) },
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SubmitButton(busy: Boolean, onClick: () -> Unit) {
    val arrowNudge by rememberInfiniteTransition(label = "submit").animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "submitArrow",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .springClick(scale = 0.97f) { if (!busy) onClick() }
            .clip(RoundedCornerShape(10.dp))
            .background(if (busy) A4TextMuted else A4Red)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = busy,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(120)) using SizeTransform(clip = false)
            },
            label = "submitLabel",
        ) { isBusy ->
            if (isBusy) {
                Text(
                    "ПРОВЕРЯЕМ КЛЮЧ…",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, letterSpacing = 1.sp),
                    color = Color.White,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ПОДКЛЮЧИТЬ",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, letterSpacing = 1.sp),
                        color = Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "→",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp),
                        color = Color.White,
                        modifier = Modifier.offset(x = arrowNudge.dp),
                    )
                }
            }
        }
    }
}
