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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private enum class A4Tab(val title: String) {
    Home("ГЛАВНАЯ"),
    Servers("СЕРВЕРЫ"),
    Plans("ТАРИФ"),
}

/** A4 visual shell over the original v2rayNG view-model and VPN service. */
@Composable
fun A4MainScreen(
    mainViewModel: MainViewModel,
    onConnectionClick: () -> Unit,
    onImportSubscription: (String, (Boolean) -> Unit) -> Unit,
    onSelectServer: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()
    val subscriptions = MmkvManager.decodeSubscriptions()
    val hasUsableSubscription = subscriptions.any { subscription ->
        MmkvManager.decodeServerList(subscription.guid).isNotEmpty()
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
                subscriptionName = subscriptions.first().subscription.remarks.ifBlank { "A4VPN" },
                selectedGroupId = state.selectedGroupId,
                selectedGuid = state.selectedGuid,
                onConnectionClick = onConnectionClick,
                onSelectServer = onSelectServer,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun A4AppHome(
    mainViewModel: MainViewModel,
    isRunning: Boolean,
    isTesting: Boolean,
    subscriptionName: String,
    selectedGroupId: String,
    selectedGuid: String?,
    onConnectionClick: () -> Unit,
    onSelectServer: (String) -> Unit,
    onOpenSettings: () -> Unit,
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
            A4TopBar(conn, onOpenSettings)
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
                            subscriptionName = subscriptionName,
                            server = selectedServer,
                            onConnectionClick = {
                                if (!isRunning) connecting = true
                                onConnectionClick()
                            },
                            onOpenServers = { tab = A4Tab.Servers },
                        )
                        A4Tab.Servers -> ServersTab(
                            servers = servers,
                            selectedGuid = selectedGuid,
                            isTesting = isTesting,
                            onSelectServer = selectServer,
                            onTestPing = { mainViewModel.testAllRealPing() },
                        )
                        A4Tab.Plans -> PlansTab()
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
private fun A4TopBar(conn: A4ConnState, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
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
        IconButton(onClick = onOpenSettings) {
            Icon(
                painter = painterResource(R.drawable.ic_settings_24dp),
                contentDescription = "Настройки",
                tint = A4Ink,
            )
        }
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
    Column(Modifier.fillMaxWidth().background(A4Paper)) {
        HorizontalDivider(color = A4Border)
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(54.dp),
        ) {
            val itemW = maxWidth / A4Tab.entries.size
            val underlineW = 34.dp
            val x by animateDpAsState(
                targetValue = itemW * current.ordinal + (itemW - underlineW) / 2,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "underline",
            )
            Row(Modifier.fillMaxSize()) {
                A4Tab.entries.forEach { t ->
                    val active = t == current
                    val color by animateColorAsState(
                        targetValue = if (active) A4Ink else A4TextMuted,
                        animationSpec = tween(200),
                        label = "tabColor",
                    )
                    Box(
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
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            t.title,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 1.5.sp),
                            color = color,
                        )
                    }
                }
            }
            // красное подчёркивание, переезжающее между вкладками
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = x, y = (-5).dp)
                    .width(underlineW)
                    .height(3.dp)
                    .background(A4Red),
            )
        }
    }
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
    subscriptionName: String,
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
            subscriptionName = subscriptionName,
            pingMs = server?.testDelayMillis ?: 0L,
            onClick = onOpenServers,
        )

        Spacer(Modifier.height(12.dp))

        A4BlackPlate(Modifier.fillMaxWidth()) {
            AnimatedContent(
                targetState = when (conn) {
                    A4ConnState.Disconnected ->
                        "Трафик идёт напрямую. Подключись, чтобы скрыть IP и зашифровать данные."
                    A4ConnState.Connecting ->
                        "Рукопожатие с сервером $serverName…"
                    A4ConnState.Connected ->
                        "Трафик зашифрован. Сервер: $serverName"
                },
                transitionSpec = {
                    (fadeIn(tween(350)) + slideInVertically { it / 4 }) togetherWith
                        fadeOut(tween(120)) using SizeTransform(clip = false)
                },
                label = "plate",
            ) { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }

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
    subscriptionName: String,
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
            Text(subscriptionName, style = MaterialTheme.typography.bodySmall, color = A4TextMuted)
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

@Composable
private fun ServersTab(
    servers: List<ServersCache>,
    selectedGuid: String?,
    isTesting: Boolean,
    onSelectServer: (String) -> Unit,
    onTestPing: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
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
    val subtitle = profile.description?.takeIf { it.isNotBlank() }
        ?: profile.server?.takeIf { it.isNotBlank() }
        ?: profile.configType.name

    Row(
        Modifier
            .fillMaxWidth()
            .springClick(scale = 0.98f, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(A4PaperCard)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                profile.remarks.ifBlank { "Сервер" },
                style = MaterialTheme.typography.titleMedium,
                color = A4Ink,
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = A4TextMuted)
        }
        if (server.testDelayMillis > 0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${server.testDelayMillis} мс",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = A4Ink,
                )
                Spacer(Modifier.height(4.dp))
                A4PingBars(server.testDelayMillis)
            }
            Spacer(Modifier.width(12.dp))
        } else if (server.testDelayMillis < 0) {
            Text("нет ответа", style = MaterialTheme.typography.bodySmall, color = A4TextMuted)
            Spacer(Modifier.width(12.dp))
        }
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
// Тариф (витрина: оплата живёт в Telegram-боте)
// ---------------------------------------------------------------------------

private data class PlanTier(
    val id: String,
    val emoji: String,
    val name: String,
    val trafficGb: Int,
    val devices: Int,
    val whitelist: Boolean,
    val prices: Map<Int, Int>,
)

private val plans = listOf(
    PlanTier("basic", "🌱", "Mini", 150, 2, false, mapOf(1 to 159)),
    PlanTier("standard", "☘️", "Pro", 400, 3, true, mapOf(1 to 199, 3 to 549, 6 to 999)),
    PlanTier("family", "🌴", "Max", 1000, 4, true, mapOf(1 to 299, 3 to 799, 6 to 1499)),
)

@Composable
private fun PlansTab() {
    val haptic = LocalHapticFeedback.current
    var tierId by remember { mutableStateOf("standard") }
    var months by remember { mutableIntStateOf(1) }
    var showNote by remember { mutableStateOf(false) }

    LaunchedEffect(showNote) {
        if (showNote) {
            delay(2400)
            showNote = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            A4SectionLabel("ТАРИФ")
            Spacer(Modifier.height(6.dp))
            Text(
                "Больше трафика,\nбольше устройств",
                style = MaterialTheme.typography.headlineMedium,
                color = A4Ink,
            )
            Spacer(Modifier.height(16.dp))

            PeriodSelector(selected = months) { m ->
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                months = m
                if (plans.first { it.id == tierId }.prices[m] == null) tierId = "standard"
            }
            Spacer(Modifier.height(14.dp))

            plans.forEach { tier ->
                TierCard(
                    tier = tier,
                    months = months,
                    selected = tierId == tier.id,
                    enabled = tier.prices[months] != null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        tierId = tier.id
                    },
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(10.dp))
            FeatureStep("01", "БЕЗ ЛИМИТА СКОРОСТИ", "видео в 4К и звонки — скорость не режем")
            Spacer(Modifier.height(12.dp))
            FeatureStep("02", "ВСЕ УСТРОЙСТВА", "телефон, ноут, планшет и ТВ одновременно")
            Spacer(Modifier.height(12.dp))
            FeatureStep("03", "ПОДДЕРЖКА 24/7", "живой человек, отвечаем в среднем за 10 минут")

            Spacer(Modifier.height(20.dp))
            CtaButton(tierId = tierId, months = months, onClick = { showNote = true })
            Spacer(Modifier.height(10.dp))
            Text(
                "цены — из каталога a4vpn",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = A4TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }

        AnimatedVisibility(
            visible = showNote,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut(),
        ) {
            A4BlackPlate {
                Text(
                    "Оплата и продление — в Telegram-боте a4vpn",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun PeriodSelector(selected: Int, onSelect: (Int) -> Unit) {
    val options = listOf(1, 3, 6)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(A4PaperCard)
            .border(1.dp, A4Border, RoundedCornerShape(10.dp))
            .padding(4.dp),
    ) {
        val segW = maxWidth / 3
        val index = options.indexOf(selected)
        val offset by animateDpAsState(
            targetValue = segW * index,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "segment",
        )
        Box(
            Modifier
                .offset(x = offset)
                .width(segW)
                .fillMaxHeight()
                .clip(RoundedCornerShape(7.dp))
                .background(A4Red),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEach { m ->
                val active = m == selected
                val color by animateColorAsState(
                    targetValue = if (active) Color.White else A4Ink,
                    animationSpec = tween(250),
                    label = "segText",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(m) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (m == 1) "1 месяц" else "$m месяцев",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        color = color,
                    )
                }
            }
        }
    }
}

@Composable
private fun TierCard(
    tier: PlanTier,
    months: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val active = selected && enabled
    val borderColor by animateColorAsState(
        targetValue = if (active) A4Red else A4Border,
        animationSpec = tween(250),
        label = "tierBorder",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (active) 2.dp else 1.dp,
        animationSpec = tween(250),
        label = "tierBorderW",
    )
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        animationSpec = tween(300),
        label = "tierAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1f else 0.98f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "tierScale",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .springClick(scale = 0.98f) { if (enabled) onClick() }
                .clip(RoundedCornerShape(12.dp))
                .background(A4PaperCard)
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tier.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tier.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = A4Ink,
                )
                Text(
                    "${tier.trafficGb} ГБ · до ${tier.devices} устройств",
                    style = MaterialTheme.typography.bodySmall,
                    color = A4TextMuted,
                )
                if (tier.whitelist) {
                    Text(
                        "+ белые списки для РФ",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = A4Red,
                    )
                }
            }
            if (enabled) {
                val price = tier.prices[months]!!
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedContent(
                        targetState = price,
                        transitionSpec = {
                            (slideInVertically { -it / 2 } + fadeIn(tween(250))) togetherWith
                                (slideOutVertically { it / 2 } + fadeOut(tween(150))) using
                                SizeTransform(clip = false)
                        },
                        label = "price",
                    ) { p ->
                        Text(
                            "$p ₽",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = A4Ink,
                        )
                    }
                    if (months > 1) {
                        val saving = tier.prices[1]!! * months - price
                        Text(
                            "выгода $saving ₽",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = A4Red,
                        )
                    } else {
                        Text(
                            "в месяц",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = A4TextMuted,
                        )
                    }
                }
            } else {
                Text(
                    "только 1 месяц",
                    style = MaterialTheme.typography.bodySmall,
                    color = A4TextMuted,
                )
            }
        }

        if (tier.id == "standard") {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp)
                    .graphicsLayer { rotationZ = -3f }
                    .clip(RoundedCornerShape(3.dp))
                    .background(A4Ink)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    "ПОПУЛЯРНЫЙ",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.5.sp),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun FeatureStep(number: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        A4StepBadge(number)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                fontFamily = A4Geologica,
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
                color = A4Ink,
            )
            Text(description, style = MaterialTheme.typography.bodySmall, color = A4TextMuted)
        }
    }
}

@Composable
private fun CtaButton(tierId: String, months: Int, onClick: () -> Unit) {
    val tier = plans.first { it.id == tierId }
    val price = tier.prices[months] ?: tier.prices[1]!!

    val arrowNudge by rememberInfiniteTransition(label = "cta").animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "arrow",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .springClick(scale = 0.97f, onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(A4Red)
            .padding(vertical = 17.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = "Оформить ${tier.name} за $price ₽",
                transitionSpec = {
                    fadeIn(tween(250)) togetherWith fadeOut(tween(120)) using SizeTransform(clip = false)
                },
                label = "ctaText",
            ) { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "→",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                color = Color.White,
                modifier = Modifier.offset(x = arrowNudge.dp),
            )
        }
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
            Spacer(Modifier.height(24.dp))
            A4StaggerIn(4) {
                A4BlackPlate(Modifier.fillMaxWidth()) {
                    Text(
                        "Один ключ — все устройства из тарифа. Ничего настраивать не нужно.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
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
