package com.v2ray.ang.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Разовая карточка-вопрос в фирменной палитре.
 *
 * Системный AlertDialog тут не годится: он приезжает в цветах Material и посреди бумажного
 * интерфейса выглядит чужим окном системы, а не частью приложения. Форма одна на все такие
 * карточки, чтобы вопрос про диагностику и рассказ про сплит-туннель читались одинаково.
 */
@Composable
internal fun A4Modal(
    icon: ImageVector,
    title: String,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(A4PaperCard)
                .border(1.dp, A4Border, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(A4Red.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = A4Red, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = A4Ink,
            )
            Spacer(Modifier.height(10.dp))
            content()
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                A4ModalButton(secondaryText, filled = false, Modifier.weight(1f), onSecondary)
                A4ModalButton(primaryText, filled = true, Modifier.weight(1f), onPrimary)
            }
        }
    }
}

/** Пояснительная строка под основным текстом: куда идти, если решение хочется поменять. */
@Composable
internal fun A4ModalHint(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(A4Paper)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = A4TextMuted)
    }
}

@Composable
private fun A4ModalButton(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (filled) Modifier.background(A4Red)
                else Modifier.border(1.dp, A4Border, RoundedCornerShape(14.dp)),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.8.sp),
            color = if (filled) Color.White else A4Ink,
        )
    }
}
