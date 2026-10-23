package com.v2ray.ang.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.dto.AppUpdate
import kotlin.math.roundToInt

/**
 * Окно «Доступно обновление» по центру экрана. Пока APK качается, окно нельзя закрыть,
 * чтобы прогресс не потерялся: скачанный файл всё равно кэшируется, но человек
 * должен увидеть, что происходит.
 */
@Composable
fun A4UpdateDialog(
    update: AppUpdate,
    isDownloading: Boolean,
    downloadProgress: Float,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = A4PaperCard,
            border = BorderStroke(1.dp, A4Border),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(A4Red),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "Доступно обновление",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = A4Ink,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Версия ${update.versionName}, сейчас стоит ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = A4TextMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Улучшили работу приложения. Рекомендуем обновиться.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = A4Ink,
                    textAlign = TextAlign.Center,
                )
                if (isDownloading) {
                    Spacer(Modifier.height(20.dp))
                    DownloadProgress(downloadProgress)
                }
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(A4Red)
                        .alpha(if (isDownloading) 0.6f else 1f)
                        .then(if (isDownloading) Modifier else Modifier.springClick(scale = 0.98f, onClick = onUpdate))
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (isDownloading) "Скачиваем ${(downloadProgress * 100).roundToInt()}%" else "Обновить",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
                if (!isDownloading) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .springClick(scale = 0.98f, onClick = onDismiss)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Позже",
                            style = MaterialTheme.typography.labelLarge,
                            color = A4TextMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadProgress(progress: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(A4Border),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(3.dp))
                .background(A4Red),
        )
    }
}
