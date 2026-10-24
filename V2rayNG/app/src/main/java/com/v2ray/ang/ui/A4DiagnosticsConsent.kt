package com.v2ray.ang.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.v2ray.ang.handler.DiagnosticsReporter

/**
 * Разовый вопрос про отчёты об ошибках.
 *
 * Спрашиваем один раз и честно говорим, что уходит: без этого сбор диагностики в
 * VPN-приложении выглядит как слежка — и по сути ею и был бы. Ответ (любой)
 * запоминается, повторно диалог не показывается; передумать можно в настройках.
 *
 * [onDismissed] дёргается, когда вопроса на экране больше нет (в том числе если его и не было):
 * следующая разовая карточка ждёт этого сигнала, чтобы не встать поверх этой.
 */
@Composable
fun A4DiagnosticsConsent(onDismissed: () -> Unit = {}) {
    if (!DiagnosticsReporter.isConfigured()) {
        LaunchedEffect(Unit) { onDismissed() }
        return
    }

    val context = LocalContext.current
    var visible by remember { mutableStateOf(!DiagnosticsReporter.wasAsked()) }
    if (!visible) {
        LaunchedEffect(Unit) { onDismissed() }
        return
    }

    A4Modal(
        icon = Icons.Rounded.BugReport,
        title = "Присылать отчёты об ошибках?",
        primaryText = "ОТПРАВЛЯТЬ",
        onPrimary = {
            DiagnosticsReporter.setEnabled(context, true)
            visible = false
        },
        secondaryText = "НЕ НАДО",
        onSecondary = {
            DiagnosticsReporter.setEnabled(context, false)
            visible = false
        },
        onDismissRequest = {
            // Отказ по умолчанию: закрыли, не ответив — значит «нет».
            DiagnosticsReporter.markAsked()
            visible = false
        },
    ) {
        Text(
            "Если приложение упадёт, разработчик увидит модель телефона, версию и место сбоя в коде.",
            style = MaterialTheme.typography.bodyMedium,
            color = A4Ink.copy(alpha = 0.82f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Адреса серверов, ключ доступа и ваши сайты не отправляются никогда.",
            style = MaterialTheme.typography.bodyMedium,
            color = A4Ink.copy(alpha = 0.82f),
        )
        Spacer(Modifier.height(14.dp))
        A4ModalHint("Передумать: Настройки → Отчёты об ошибках")
    }
}
