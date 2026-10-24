package com.v2ray.ang.handler

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.util.DirectSentryTransportFactory
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User

/**
 * Отчёты об ошибках в наш GlitchTip.
 *
 * Три правила, которые нельзя нарушать:
 *  1. Строго opt-in. Пока человек не согласился, SDK вообще не инициализируется.
 *  2. Инициализация — в каждом процессе. Самое интересное падает в :RunSoLibV2RayDaemon,
 *     а AngApplication.onCreate выполняется во всех трёх процессах.
 *  3. Ничего чувствительного наружу: см. [DiagnosticsScrubber]. Адреса серверов и ключи
 *     подписок не должны покидать устройство даже при полученном согласии.
 */
object DiagnosticsReporter {

    private const val TAG = "Diagnostics"

    @Volatile
    private var started = false

    /** Есть ли вообще куда слать: DSN приезжает из a4.properties и может быть пустым. */
    fun isConfigured(): Boolean = AppConfig.GLITCHTIP_DSN.isNotBlank()

    fun isEnabled(): Boolean =
        isConfigured() && MmkvManager.decodeSettingsBool(AppConfig.PREF_DIAGNOSTICS_ENABLED, false)

    /** Спрашивали ли уже согласие — чтобы не показывать диалог повторно. */
    fun wasAsked(): Boolean = MmkvManager.decodeSettingsBool(AppConfig.PREF_DIAGNOSTICS_ASKED, false)

    fun markAsked() {
        MmkvManager.encodeSettings(AppConfig.PREF_DIAGNOSTICS_ASKED, true)
    }

    /** Ответ на вопрос о согласии: включает или гасит отправку немедленно. */
    fun setEnabled(context: Context, enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DIAGNOSTICS_ENABLED, enabled)
        markAsked()
        if (enabled) start(context) else stop()
    }

    fun init(context: Context) {
        if (isEnabled()) start(context)
    }

    private fun stop() {
        if (!started) return
        started = false
        runCatching { Sentry.close() }
    }

    private fun start(context: Context) {
        if (started || !isConfigured()) return
        started = true

        runCatching {
            SentryAndroid.init(context.applicationContext) { options ->
                options.dsn = AppConfig.GLITCHTIP_DSN
                options.release = "a4vpn@${BuildConfig.VERSION_NAME}"
                options.dist = BuildConfig.VERSION_CODE.toString()
                options.environment = BuildConfig.DISTRIBUTION
                options.isDebug = false

                // Никакой аналитики поведения: только ошибки. Меньше данных — меньше рисков
                // и меньше трафика у людей, которые и так сидят через туннель.
                options.tracesSampleRate = 0.0
                options.isEnableAutoSessionTracking = false
                options.isSendDefaultPii = false
                options.isAttachScreenshot = false
                options.isAttachViewHierarchy = false
                options.isEnableUserInteractionBreadcrumbs = false
                options.isEnableUserInteractionTracing = false
                options.maxBreadcrumbs = 60

                // Конверты уходят мимо собственного VPN, иначе отчёт о падении ядра
                // не доедет ровно тогда, когда он нужен.
                options.setTransportFactory(DirectSentryTransportFactory(context.applicationContext))

                options.setBeforeSend { event, _ -> scrub(event) }
                options.setBeforeBreadcrumb { crumb, _ -> scrub(crumb) }
            }

            Sentry.configureScope { scope ->
                scope.user = User().apply { id = installationId() }
                scope.setTag("process", processName(context))
                scope.setTag("abi", Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
                scope.setTag("androidApi", Build.VERSION.SDK_INT.toString())
            }
        }.onFailure {
            started = false
            LogUtil.w(AppConfig.TAG, "Failed to start diagnostics: ${it.message}")
        }
    }

    /** Событие для хлебных крошек: контекст падения важнее самого стектрейса. */
    fun breadcrumb(category: String, message: String) {
        if (!started) return
        runCatching {
            Sentry.addBreadcrumb(
                Breadcrumb().apply {
                    this.category = category
                    this.message = message
                    this.level = SentryLevel.INFO
                }
            )
        }
    }

    private fun scrub(event: SentryEvent): SentryEvent {
        event.message?.let { message ->
            message.message = DiagnosticsScrubber.redact(message.message)
            message.formatted = DiagnosticsScrubber.redact(message.formatted)
            message.params = message.params?.map { DiagnosticsScrubber.redact(it).orEmpty() }
        }
        event.exceptions?.forEach { exception ->
            exception.value = DiagnosticsScrubber.redact(exception.value)
        }
        event.logger = DiagnosticsScrubber.redact(event.logger)
        event.serverName = null
        return event
    }

    private fun scrub(crumb: Breadcrumb): Breadcrumb {
        crumb.message = DiagnosticsScrubber.redact(crumb.message)
        return crumb
    }

    /**
     * Тот же анонимный идентификатор, что и у телеметрии подписок: связать отчёт с
     * устройством нужно, знать, кто это, — нет.
     */
    private fun installationId(): String =
        MmkvManager.decodeSettingsString(AppConfig.PREF_INSTALLATION_ID)
            ?: Utils.getUuid().also { MmkvManager.encodeSettings(AppConfig.PREF_INSTALLATION_ID, it) }

    /** Короткое имя процесса: main / RunSoLibV2RayDaemon / bg. */
    private fun processName(context: Context): String {
        val full = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            android.app.Application.getProcessName()
        } else {
            val pid = Process.myPid()
            context.getSystemService(ActivityManager::class.java)
                ?.runningAppProcesses
                ?.firstOrNull { it.pid == pid }
                ?.processName
        }
        return full?.substringAfter(':', "main") ?: "unknown"
    }
}
