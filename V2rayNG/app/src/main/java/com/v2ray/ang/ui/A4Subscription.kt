package com.v2ray.ang.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.handler.MmkvManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToLong

private const val BYTES_PER_GIB = 1_073_741_824.0

// ---------------------------------------------------------------------------
// Данные подписки: трафик и срок из заголовков Subscription-Userinfo
// ---------------------------------------------------------------------------

internal val SubscriptionItem.usedBytes: Long get() = uploadBytes + downloadBytes

/** Есть ли что показывать (трафик или срок пришли из подписки). */
internal val SubscriptionItem.hasTrafficInfo: Boolean
    get() = totalBytes > 0 || expire > 0 || usedBytes > 0

internal val SubscriptionItem.isUnlimited: Boolean get() = totalBytes <= 0

/** Активная подписка: та, что владеет выбранным сервером; иначе первая. */
internal fun activeSubscription(): SubscriptionItem? {
    val subs = MmkvManager.decodeSubscriptions()
    if (subs.isEmpty()) return null
    val subId = MmkvManager.getSelectServer()
        ?.let { MmkvManager.decodeServerConfig(it)?.subscriptionId }
    return (subs.firstOrNull { it.guid == subId } ?: subs.first()).subscription
}

/** Байты → строка в ГБ (GiB): целое от 10 и больше, иначе один знак. */
internal fun formatGib(bytes: Long): String {
    val g = bytes / BYTES_PER_GIB
    return if (g >= 10) g.roundToLong().toString() else "%.1f".format(g)
}

/** Доля израсходованного трафика 0..1 (для безлимита — 0). */
internal fun SubscriptionItem.usedFraction(): Float {
    if (totalBytes <= 0) return 0f
    return (usedBytes.toDouble() / totalBytes).coerceIn(0.0, 1.0).toFloat()
}

/** Остаток трафика в байтах (не меньше нуля). */
internal fun SubscriptionItem.remainingBytes(): Long = (totalBytes - usedBytes).coerceAtLeast(0)

/** Осталось дней до окончания, либо null если срока нет. */
internal fun SubscriptionItem.daysLeft(): Long? {
    if (expire <= 0) return null
    val ms = expire * 1000L - System.currentTimeMillis()
    return if (ms <= 0) 0 else ceil(ms / 86_400_000.0).toLong()
}

internal fun SubscriptionItem.expireDateText(): String? {
    if (expire <= 0) return null
    return SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru")).format(Date(expire * 1000L))
}

/** Короткая метка срока для карточек трафика: дни, либо год окончания, если их больше 365. */
internal fun SubscriptionItem.expiryShortLabel(): String? {
    val days = daysLeft() ?: return null
    if (days > 365) {
        val year = SimpleDateFormat("yyyy", Locale.forLanguageTag("ru")).format(Date(expire * 1000L))
        return "до $year"
    }
    return "$days ${pluralDays(days)}"
}

/** Дата обнуления счётчика трафика, либо null если её нет или трафик безлимитный. */
internal fun SubscriptionItem.refillDateText(): String? {
    if (refill <= 0 || isUnlimited) return null
    return SimpleDateFormat("d MMMM", Locale.forLanguageTag("ru")).format(Date(refill * 1000L))
}

/** Когда подписка обновлялась в последний раз, либо null если ни разу. */
internal fun SubscriptionItem.lastUpdatedText(): String? {
    if (lastUpdated <= 0) return null
    val ru = Locale.forLanguageTag("ru")
    val moment = Date(lastUpdated)
    val today = SimpleDateFormat("yyyyMMdd", ru).let { it.format(moment) == it.format(Date()) }
    val pattern = if (today) "HH:mm" else "d MMM, HH:mm"
    return SimpleDateFormat(pattern, ru).format(moment)
}

/** Русское склонение слова «день». */
internal fun pluralDays(n: Long): String {
    val n100 = n % 100
    val n10 = n % 10
    return when {
        n100 in 11..14 -> "дней"
        n10 == 1L -> "день"
        n10 in 2..4 -> "дня"
        else -> "дней"
    }
}

// ---------------------------------------------------------------------------
// Общие визуальные элементы
// ---------------------------------------------------------------------------

/** Полоса трафика: красная доля израсходованного на фоне A4Border. */
@Composable
internal fun A4TrafficBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(A4Border),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(4.dp))
                .background(A4Red),
        )
    }
}
