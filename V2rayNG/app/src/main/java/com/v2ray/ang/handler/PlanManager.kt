package com.v2ray.ang.handler

import android.content.Context
import android.content.pm.PackageManager
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.util.LogUtil

/**
 * Два состояния доступа и переключение между ними.
 *
 * Подписок ровно две, обе наши:
 *  - бесплатная ([AppConfig.FREE_SUB_URL]) — канал до Telegram, чтобы человек мог
 *    дойти до бота и купить доступ. Работает только с Telegram;
 *  - платная — весь трафик.
 *
 * Активная всегда одна. Бесплатная после покупки не удаляется, а гасится флагом
 * `enabled` и включается обратно сама, если платная кончилась — иначе человек с
 * истёкшей подпиской не сможет дойти до бота и продлить.
 *
 * Отдельного «профиля настроек на подписку» здесь нет: состояний два, и всё
 * переключение — это несколько глобальных ключей MMKV в момент смены подписки.
 */
object PlanManager {

    /** Клиенты Telegram — нужны только для подсказки в интерфейсе. */
    private val TELEGRAM_PACKAGES = listOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.telegram.plus",
        "nekox.messenger",
    )

    /**
     * Домены Telegram для правил маршрутизации ядра.
     *
     * Geosite-категории сюда намеренно не берём: geosite.dat докачивается по
     * сети, а бесплатный доступ включают как раз тогда, когда сеть уже режут —
     * правило со ссылкой на отсутствующий файл уронило бы старт ядра.
     */
    val TELEGRAM_DOMAINS = listOf(
        "domain:telegram.org",
        "domain:telegram.me",
        "domain:t.me",
        "domain:telesco.pe",
        "domain:telegra.ph",
        "domain:tdesktop.com",
        "domain:cdn-telegram.org",
        "domain:comments.app",
        "domain:contest.com",
        "domain:fragment.com",
        "domain:telegram.dog",
        "domain:tg.dev",
    )

    /**
     * Сети дата-центров Telegram — по ним ходит сам мессенджер: клиент держит
     * адреса DC зашитыми и в DNS обычно не заглядывает, так что без этих
     * диапазонов доменных правил не хватит.
     */
    val TELEGRAM_IPS = listOf(
        "91.105.192.0/23",
        "91.108.4.0/22",
        "91.108.8.0/21",
        "91.108.16.0/21",
        "91.108.56.0/22",
        "95.161.64.0/20",
        "149.154.160.0/20",
        "185.76.151.0/24",
        "2001:67c:4e8::/48",
        "2001:b28:f23c::/47",
        "2001:b28:f23f::/48",
        "2a0a:f280::/32",
    )

    /** Текущий доступ, каким его видит главный экран. */
    enum class Plan {
        /** Ни одной рабочей подписки — показываем экран первого запуска. */
        NONE,

        /** Бесплатный Telegram-канал. */
        FREE,

        /** Полный доступ. */
        PAID,
    }

    // ---------------------------------------------------------------------
    // Чтение состояния
    // ---------------------------------------------------------------------

    /** Бесплатная подписка, если она заведена. */
    fun freeSubscription(): SubscriptionCache? =
        MmkvManager.decodeSubscriptions()
            .firstOrNull { it.subscription.url == AppConfig.FREE_SUB_URL }

    /** Платная подписка, если она заведена (на устройстве она одна). */
    fun paidSubscription(): SubscriptionCache? =
        MmkvManager.decodeSubscriptions().firstOrNull {
            it.subscription.url.isNotEmpty() && it.subscription.url != AppConfig.FREE_SUB_URL
        }

    /** Действует ли подписка: срок не истёк и серверы на месте. */
    private fun isUsable(cache: SubscriptionCache?): Boolean {
        val sub = cache?.subscription ?: return false
        if (isExpired(sub)) return false
        return MmkvManager.decodeServerList(cache.guid).isNotEmpty()
    }

    /** Истёк ли срок подписки (0 — срока нет). */
    fun isExpired(sub: SubscriptionItem): Boolean =
        sub.expire > 0 && sub.expire * 1000L <= System.currentTimeMillis()

    /** Платная подписка была, но больше не действует — человека вернули на бесплатную. */
    fun paidRanOut(): Boolean {
        val paid = paidSubscription() ?: return false
        return !isUsable(paid)
    }

    /**
     * Установлен ли хоть один клиент Telegram.
     *
     * На запуск это больше не влияет: режим «только Telegram» держится
     * правилами маршрутизации ядра и не зависит от того, что стоит на
     * телефоне. Нужно лишь для подсказки в интерфейсе — без клиента
     * бесплатный доступ бесполезен, хоть и безопасен.
     */
    fun hasTelegramInstalled(context: Context): Boolean =
        TELEGRAM_PACKAGES.any { isInstalled(context, it) }

    /** Включён ли режим «только Telegram». */
    fun isTelegramOnlyMode(): Boolean =
        MmkvManager.decodeSettingsBool(AppConfig.PREF_TELEGRAM_ONLY_MODE, false)

    /** Каким доступом человек пользуется прямо сейчас. */
    fun currentPlan(): Plan {
        if (isUsable(paidSubscription())) return Plan.PAID
        val free = freeSubscription() ?: return Plan.NONE
        if (MmkvManager.decodeServerList(free.guid).isEmpty()) return Plan.NONE
        return Plan.FREE
    }

    // ---------------------------------------------------------------------
    // Переключение
    // ---------------------------------------------------------------------

    /**
     * Свести состояние приложения с тем, что осталось от подписок.
     *
     * Вызывается при старте и при каждом возвращении на главный экран: платная
     * могла истечь, а могла и продлиться (панель отдаёт новый `expire` при
     * фоновом обновлении) — оба перехода делаем сами.
     *
     * @param allowNetwork Можно ли докачать серверы бесплатной подписки. При старте
     *   приложения — нельзя: там только перестановка уже сохранённого.
     * @return Guid ставшей активной подписки, если состояние поменялось, иначе null.
     */
    fun syncPlan(allowNetwork: Boolean = true): String? {
        val paid = paidSubscription()
        if (isUsable(paid)) {
            // Полный доступ действует: гасим бесплатную и снимаем Telegram-режим,
            // если человек до этого сидел на бесплатном.
            if (freeSubscription()?.subscription?.enabled != true && !isTelegramOnlyMode()) return null
            return switchToPaid()
        }

        // Платного доступа нет. Экран первого запуска — не наш случай: там ещё
        // ничего не заведено и человек сам выбирает, как войти.
        if (paid == null && freeSubscription() == null) return null
        if (isFreePlanReady()) {
            // Всё уже поднято: правила маршрутизации собираются при каждом
            // старте ядра, подновлять здесь нечего. Единственное — снимаем
            // per-app whitelist, если бесплатный доступ включила ещё старая
            // версия: иначе в туннель по-прежнему пускало бы один Telegram.
            clearLegacyTelegramWhitelist()
            return null
        }
        return activateFreePlan(allowNetwork)
    }

    /**
     * Перевести приложение на платный доступ.
     *
     * Одной транзакцией: бесплатная гаснет (но остаётся), режим «только Telegram»
     * снимается, активным становится сервер из платной подписки — иначе запуск из
     * плитки быстрых настроек или после перезагрузки поднял бы бесплатный
     * Telegram-сервер.
     *
     * @return Guid платной подписки, либо null, если её серверов ещё нет.
     */
    fun switchToPaid(): String? {
        val paid = paidSubscription() ?: return null
        if (MmkvManager.decodeServerList(paid.guid).isEmpty()) return null

        freeSubscription()?.let { free ->
            if (free.subscription.enabled) {
                free.subscription.enabled = false
                MmkvManager.encodeSubscription(free.guid, free.subscription)
            }
        }
        // Чужие исключения по приложениям не трогаем: сбрасывать есть что, только
        // если per-app proxy настроили мы сами под бесплатный доступ.
        if (isTelegramOnlyMode()) disableTelegramOnlyMode()
        selectBestServer(paid.guid)
        MmkvManager.encodeSettings(AppConfig.CACHE_SUBSCRIPTION_ID, paid.guid)
        return paid.guid
    }

    /**
     * Включить бесплатный Telegram-канал: завести подписку, если её ещё нет,
     * скачать серверы, поднять режим «только Telegram» и выбрать сервер.
     *
     * Ходит в сеть (если [allowNetwork]) — вызывать только с IO-диспетчера.
     *
     * @param allowNetwork Можно ли скачать серверы, если их ещё нет.
     * @return Guid бесплатной подписки, либо null, если серверы не скачались.
     */
    fun activateFreePlan(allowNetwork: Boolean = true): String? {
        // Без сети заводить пустую подписку смысла нет: серверы взять неоткуда.
        val guid = (if (allowNetwork) ensureFreeSubscription() else freeSubscription()?.guid)
            ?: return null
        val sub = MmkvManager.decodeSubscription(guid) ?: return null

        // Включаем до обновления: отключённые подписки updateConfigViaSub пропускает.
        if (!sub.enabled) {
            sub.enabled = true
            MmkvManager.encodeSubscription(guid, sub)
        }
        if (allowNetwork && MmkvManager.decodeServerList(guid).isEmpty()) {
            AngConfigManager.updateConfigViaSub(SubscriptionCache(guid, sub))
        }
        if (MmkvManager.decodeServerList(guid).isEmpty()) return null

        enableTelegramOnlyMode()
        selectBestServer(guid)
        MmkvManager.encodeSettings(AppConfig.CACHE_SUBSCRIPTION_ID, guid)
        return guid
    }

    /** Бесплатный канал уже поднят и трогать его не надо. */
    private fun isFreePlanReady(): Boolean {
        val free = freeSubscription() ?: return false
        if (!free.subscription.enabled) return false
        if (MmkvManager.decodeServerList(free.guid).isEmpty()) return false
        return isTelegramOnlyMode()
    }

    /** Завести бесплатную подписку, если её ещё нет. */
    private fun ensureFreeSubscription(): String? {
        freeSubscription()?.let { return it.guid }
        val item = SubscriptionItem().apply {
            remarks = AppConfig.FREE_SUB_REMARKS
            url = AppConfig.FREE_SUB_URL
            autoUpdate = true
            updateInterval = AppConfig.SUBSCRIPTION_DEFAULT_INTERVAL_MINUTES
        }
        MmkvManager.encodeSubscription("", item)
        return freeSubscription()?.guid
    }

    // ---------------------------------------------------------------------
    // Режим «только Telegram» (правила маршрутизации ядра)
    // ---------------------------------------------------------------------

    /**
     * Включить режим «только Telegram».
     *
     * Сам отбор трафика делает ядро: [AppConfig.PREF_TELEGRAM_ONLY_MODE] читает
     * CoreConfigManager и подменяет правила маршрутизации — Telegram и DNS идут
     * в туннель, всё остальное уходит напрямую. Раньше это был белый список
     * per-app proxy; он зависел от установленных приложений и, если Telegram не
     * стоял, разрешал в туннель вообще всё. Здесь запоминаем только флаг.
     *
     * Заодно снимаем per-app whitelist, оставшийся от прошлых версий: без этого
     * человек с уже включённым бесплатным доступом остался бы с VPN, в который
     * пущен ровно один Telegram, и правила ядра ничего бы не изменили.
     */
    fun enableTelegramOnlyMode() {
        clearLegacyTelegramWhitelist()
        MmkvManager.encodeSettings(AppConfig.PREF_TELEGRAM_ONLY_MODE, true)
    }

    /** Вернуть «весь трафик»: снять флаг и остатки старого белого списка. */
    fun disableTelegramOnlyMode() {
        clearLegacyTelegramWhitelist()
        MmkvManager.encodeSettings(AppConfig.PREF_TELEGRAM_ONLY_MODE, false)
    }

    /**
     * Убрать per-app proxy, если его настроили мы под бесплатный доступ.
     * Чужие исключения человека не трогаем: признак нашего списка — в нём
     * только пакеты Telegram.
     */
    private fun clearLegacyTelegramWhitelist() {
        val current = MmkvManager.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET).orEmpty()
        if (current.isEmpty() || !TELEGRAM_PACKAGES.containsAll(current)) return
        LogUtil.i(AppConfig.TAG, "PlanManager: dropping legacy Telegram per-app whitelist")
        MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY_SET, mutableSetOf())
        MmkvManager.encodeSettings(AppConfig.PREF_BYPASS_APPS, true)
        MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY, false)
    }

    private fun isInstalled(context: Context, packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** Сделать активным лучший по пингу сервер подписки, иначе первый. */
    private fun selectBestServer(subId: String) {
        val guids = MmkvManager.decodeServerList(subId)
        if (guids.isEmpty()) return
        val best = guids.minByOrNull { guid ->
            MmkvManager.decodeServerAffiliationInfo(guid)
                ?.testDelayMillis
                ?.takeIf { it > 0 }
                ?: Long.MAX_VALUE
        } ?: guids.first()
        MmkvManager.setSelectServer(best)
    }
}
