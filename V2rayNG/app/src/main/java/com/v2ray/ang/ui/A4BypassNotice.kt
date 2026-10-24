package com.v2ray.ang.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.v2ray.ang.handler.BypassAppListManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Сколько иконок показываем, прежде чем свернуть остальные в «+N». Две строки — потолок. */
private const val MAX_ICONS = 7

private data class BypassApp(val name: String, val icon: ImageBitmap?)

/**
 * Разовый рассказ о том, что часть приложений мы увели мимо туннеля.
 *
 * Маршрутизацию меняет серверный список, без участия человека (см. [BypassAppListManager]) — и именно
 * поэтому обязаны о ней сказать: человек, который увидит в банке свой домашний IP, должен понимать,
 * что так задумано, а не что VPN не работает. Карточка показывается один раз и уводит в настройки
 * тех, кто хочет решить иначе.
 */
@Composable
fun A4BypassNotice(onOpenPerAppProxy: () -> Unit) {
    val context = LocalContext.current
    // Список читаем один раз за композицию: markAnnounced() его тут же обнуляет, и без снимка
    // карточка схлопнулась бы в тот же кадр, в котором появилась.
    val packageNames = remember { BypassAppListManager.pendingAnnouncement() }
    var visible by remember { mutableStateOf(packageNames.isNotEmpty()) }
    if (!visible) return

    var apps by remember { mutableStateOf<List<BypassApp>>(emptyList()) }
    LaunchedEffect(packageNames) {
        apps = withContext(Dispatchers.IO) { loadApps(context, packageNames.take(MAX_ICONS)) }
    }

    val dismiss = {
        BypassAppListManager.markAnnounced()
        visible = false
    }

    A4Modal(
        icon = Icons.AutoMirrored.Rounded.AltRoute,
        title = "Российские приложения — мимо VPN",
        primaryText = "ПОНЯТНО",
        onPrimary = dismiss,
        secondaryText = "НАСТРОИТЬ",
        onSecondary = {
            dismiss()
            onOpenPerAppProxy()
        },
        onDismissRequest = dismiss,
    ) {
        Text(
            "Банки, госуслуги и маркетплейсы замечают VPN: с ним они могут работать хуже " +
                "или не работать совсем.",
            style = MaterialTheme.typography.bodyMedium,
            color = A4Ink.copy(alpha = 0.82f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Поэтому ${appCountPhrase(packageNames.size)} мы пустили мимо туннеля. " +
                "Остальной трафик защищён.",
            style = MaterialTheme.typography.bodyMedium,
            color = A4Ink.copy(alpha = 0.82f),
        )
        if (apps.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            AppIconRow(apps = apps, total = packageNames.size)
        }
        Spacer(Modifier.height(14.dp))
        A4ModalHint("Изменить список: Настройки → Исключения по приложениям")
    }
}

/**
 * Ряд иконок с подписями: одно название списком читается хуже, чем знакомый значок Сбера или WB —
 * по иконкам сразу понятно, о каких именно приложениях речь.
 *
 * Именно FlowRow, а не Row: на узком экране обычный ряд молча уезжает за край вместе со счётчиком
 * «+N», и человек видит пять иконок вместо тринадцати приложений.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppIconRow(apps: List<BypassApp>, total: Int) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        apps.forEach { app ->
            Column(
                Modifier.width(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(A4Paper),
                    contentAlignment = Alignment.Center,
                ) {
                    val icon = app.icon
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(30.dp),
                        )
                    } else {
                        Text(
                            app.name.take(1).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = A4TextMuted,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    app.name,
                    // Разрядку из A4-стиля тут гасим: в 48 dp она съедает половину названия.
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = A4TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
        val rest = total - apps.size
        if (rest > 0) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(A4Paper),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+$rest",
                    style = MaterialTheme.typography.labelLarge,
                    color = A4TextMuted,
                )
            }
        }
    }
}

/** Пакет мог уехать вместе с приложением — тогда просто пропускаем его в ряду. */
private fun loadApps(context: Context, packageNames: List<String>): List<BypassApp> {
    val pm = context.packageManager
    return packageNames.mapNotNull { packageName ->
        try {
            val info = pm.getApplicationInfo(packageName, 0)
            BypassApp(
                name = pm.getApplicationLabel(info).toString(),
                icon = runCatching { pm.getApplicationIcon(info).toBitmap().asImageBitmap() }.getOrNull(),
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}

/** «5 приложений» — счётчик со склонением; той же строкой пользуются настройки. */
internal fun appCountPhrase(count: Int): String {
    val tail = count % 100
    val last = count % 10
    val word = when {
        tail in 11..14 -> "приложений"
        last == 1 -> "приложение"
        last in 2..4 -> "приложения"
        else -> "приложений"
    }
    return "$count $word"
}
