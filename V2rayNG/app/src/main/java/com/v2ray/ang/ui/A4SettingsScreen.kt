package com.v2ray.ang.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.handler.GeoUpdater
import com.v2ray.ang.handler.MmkvManager.rememberMmkvBool
import com.v2ray.ang.handler.MmkvManager.rememberMmkvString
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val BOT_URL = "https://t.me/a4securebot"
private val TelegramBlue = Color(0xFF229ED9)

/** Логотип Telegram (бумажный самолётик), путь из SVG в координатах 24×24. */
private val telegramPath = PathParser()
    .parsePathString(
        "M9.78 18.65l.28-4.23 7.68-6.92c.34-.31-.07-.46-.52-.19L7.74 13.3 3.64 12" +
            "c-.88-.25-.89-.86.2-1.3l15.97-6.16c.73-.33 1.43.18 1.15 1.3l-2.72 12.81" +
            "c-.19.91-.74 1.13-1.5.71L12.6 16.3l-1.99 1.93c-.23.23-.42.42-.83.42z",
    )
    .toPath()

@Composable
private fun TelegramIcon(modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val s = size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(telegramPath, tint)
        }
    }
}

/** Полноэкранный вариант (Activity): своя тема, отступ статус-бара и кнопка «назад». */
@Composable
fun A4SettingsScreen(
    onBackClick: () -> Unit,
    onOpenLogcat: () -> Unit,
) {
    A4Theme {
        A4SettingsContent(embedded = false, onBackClick = onBackClick, onOpenLogcat = onOpenLogcat)
    }
}

/** Встраиваемый вариант (вкладка нижней навигации): без темы, отступа и кнопки «назад». */
@Composable
fun A4SettingsTab(onOpenLogcat: () -> Unit) {
    A4SettingsContent(embedded = true, onBackClick = {}, onOpenLogcat = onOpenLogcat)
}

@Composable
private fun A4SettingsContent(
    embedded: Boolean,
    onBackClick: () -> Unit,
    onOpenLogcat: () -> Unit,
) {
    if (!embedded) BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var autoStart by rememberMmkvBool(AppConfig.PREF_IS_BOOTED, false)
    var showSpeed by rememberMmkvBool(AppConfig.PREF_SPEED_ENABLED, false)
    var logLevel by rememberMmkvString(AppConfig.PREF_LOGLEVEL, "warning")

    val scope = rememberCoroutineScope()
    var geoRefresh by remember { mutableIntStateOf(0) }
    var geoUpdating by remember { mutableStateOf(false) }
    var geoStatus by remember { mutableStateOf<String?>(null) }
    val geositeDate = remember(geoRefresh) { geoFileDateText(context, AppConfig.GEOSITE_DAT) }
    val geoipDate = remember(geoRefresh) { geoFileDateText(context, AppConfig.GEOIP_DAT) }

    Box(Modifier.fillMaxSize().background(A4Paper)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (embedded) Modifier else Modifier.statusBarsPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!embedded) {
                    BackArrow(onClick = onBackClick)
                    Spacer(Modifier.width(10.dp))
                }
                Text("Настройки", style = MaterialTheme.typography.headlineMedium, color = A4Ink)
            }

            Spacer(Modifier.height(20.dp))
            A4SectionLabel("ПОДКЛЮЧЕНИЕ")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                SettingsToggleRow(
                    title = "Автозапуск",
                    description = "включать VPN после перезагрузки телефона",
                    checked = autoStart,
                    onCheckedChange = {
                        haptic.performHapticFeedback(
                            if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
                        )
                        autoStart = it
                    },
                )
                HorizontalDivider(color = A4Border)
                SettingsToggleRow(
                    title = "Скорость в уведомлении",
                    description = "показывать текущую скорость туннеля",
                    checked = showSpeed,
                    onCheckedChange = {
                        haptic.performHapticFeedback(
                            if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
                        )
                        showSpeed = it
                    },
                )
            }

            Spacer(Modifier.height(20.dp))
            A4SectionLabel("ГЕОБАЗЫ")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                SettingsInfoRow("Домены (geosite)", geositeDate)
                HorizontalDivider(color = A4Border)
                SettingsInfoRow("IP-адреса (geoip)", geoipDate)
                HorizontalDivider(color = A4Border)
                SettingsLinkRow(
                    title = if (geoUpdating) "Обновление…" else "Обновить базы",
                    description = geoStatus ?: "списки роутинга RoscomVPN, обновляются раз в день",
                    onClick = {
                        if (geoUpdating) return@SettingsLinkRow
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        geoUpdating = true
                        geoStatus = null
                        scope.launch {
                            val count = withContext(Dispatchers.IO) { GeoUpdater.updateGeoFiles(context) }
                            geoStatus = if (count > 0) "обновлено" else "не удалось — проверьте подключение"
                            geoUpdating = false
                            geoRefresh++
                        }
                    },
                )
            }

            Spacer(Modifier.height(20.dp))
            A4SectionLabel("ПРИЛОЖЕНИЕ")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                SettingsChoiceRow(
                    title = "Уровень журнала",
                    description = "что записывать в журнал",
                    options = logLevelOptions,
                    selectedValue = logLevel,
                    onSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        logLevel = it
                        LogUtil.refreshLogLevel()
                    },
                )
                HorizontalDivider(color = A4Border)
                SettingsLinkRow(
                    title = "Журнал",
                    description = "ошибки и события подключения",
                    onClick = onOpenLogcat,
                )
                HorizontalDivider(color = A4Border)
                SettingsInfoRow("Версия", BuildConfig.VERSION_NAME)
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .springClick(scale = 0.98f) { Utils.openUri(context, BOT_URL) }
                    .clip(RoundedCornerShape(14.dp))
                    .background(TelegramBlue)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TelegramIcon(Modifier.size(22.dp), tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Перейти в бота",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Дата последнего обновления геобазы или «не загружена». */
private fun geoFileDateText(context: android.content.Context, fileName: String): String {
    val timestamp = GeoUpdater.geoFileLastModified(context, fileName)
    if (timestamp <= 0L) return "не загружена"
    return SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru")).format(Date(timestamp))
}

/** Стрелка «назад», нарисованная руками. */
@Composable
private fun BackArrow(onClick: () -> Unit) {
    Box(
        Modifier
            .springClick(scale = 0.9f, onClick = onClick)
            .size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 2.2f.dp.toPx()
            drawLine(A4Ink, Offset(w * 0.05f, h * 0.5f), Offset(w * 0.95f, h * 0.5f), stroke, StrokeCap.Round)
            drawLine(A4Ink, Offset(w * 0.05f, h * 0.5f), Offset(w * 0.4f, h * 0.18f), stroke, StrokeCap.Round)
            drawLine(A4Ink, Offset(w * 0.05f, h * 0.5f), Offset(w * 0.4f, h * 0.82f), stroke, StrokeCap.Round)
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(A4PaperCard)
            .border(1.dp, A4Border, RoundedCornerShape(12.dp)),
        content = content,
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = A4Ink,
            )
            Text(description, style = MaterialTheme.typography.bodySmall, color = A4TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = A4Red,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = A4Border,
                uncheckedThumbColor = Color.White,
                uncheckedBorderColor = A4Border,
            ),
        )
    }
}

private data class LogLevelOption(val value: String, val label: String)

private val logLevelOptions = listOf(
    LogLevelOption("debug", "debug"),
    LogLevelOption("info", "info"),
    LogLevelOption("warning", "warning"),
    LogLevelOption("error", "error"),
    LogLevelOption("none", "none"),
)

/** Ряд с выбором из нескольких значений — чипы, переносятся по ширине. */
@Composable
private fun SettingsChoiceRow(
    title: String,
    description: String,
    options: List<LogLevelOption>,
    selectedValue: String,
    onSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = A4Ink,
        )
        Text(description, style = MaterialTheme.typography.bodySmall, color = A4TextMuted)
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                LogLevelChip(
                    label = option.label,
                    selected = option.value == selectedValue,
                    onClick = { onSelected(option.value) },
                )
            }
        }
    }
}

@Composable
private fun LogLevelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .springClick(scale = 0.95f, onClick = onClick)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) A4Ink else A4Paper)
            .border(1.dp, if (selected) A4Ink else A4Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) Color.White else A4Ink,
        )
    }
}

@Composable
private fun SettingsLinkRow(title: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .springClick(scale = 0.98f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = A4Ink,
            )
            Text(description, style = MaterialTheme.typography.bodySmall, color = A4TextMuted)
        }
        Text("→", style = MaterialTheme.typography.titleMedium, color = A4Ink)
    }
}

@Composable
private fun SettingsInfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = A4Ink,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodySmall, color = A4TextMuted)
    }
}
