package com.v2ray.ang.util

import android.content.Context
import io.sentry.Hint
import io.sentry.ITransportFactory
import io.sentry.RequestDetails
import io.sentry.SentryEnvelope
import io.sentry.SentryOptions
import io.sentry.transport.ITransport
import io.sentry.transport.RateLimiter
import java.io.ByteArrayOutputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Транспорт диагностики, ходящий мимо собственного VPN.
 *
 * Штатный HTTP-транспорт Sentry ушёл бы в общий сетевой стек, то есть при поднятом
 * туннеле — в наш же туннель. Отчёт об ошибке ядра тогда не доедет ровно в тот момент,
 * когда он нужнее всего, поэтому конверты уходят через [DirectNetworkHttp].
 */
class DirectSentryTransportFactory(private val context: Context) : ITransportFactory {
    override fun create(options: SentryOptions, requestDetails: RequestDetails): ITransport =
        DirectSentryTransport(context.applicationContext, options, requestDetails)
}

private class DirectSentryTransport(
    private val context: Context,
    private val options: SentryOptions,
    private val requestDetails: RequestDetails,
) : ITransport {

    private val rateLimiter = RateLimiter(options)

    // Отправка не должна блокировать поток, который поймал ошибку: очередь короткая,
    // переполнение молча роняет старые события — диагностика не важнее работы приложения.
    private val executor = ThreadPoolExecutor(
        1, 1, 30, TimeUnit.SECONDS, ArrayBlockingQueue(20),
        { runnable -> Thread(runnable, "a4-diagnostics").apply { isDaemon = true } },
        ThreadPoolExecutor.DiscardOldestPolicy(),
    )

    override fun send(envelope: SentryEnvelope, hint: Hint) {
        val filtered = rateLimiter.filter(envelope, hint) ?: return
        val body = runCatching {
            ByteArrayOutputStream().also { options.serializer.serialize(filtered, it) }.toByteArray()
        }.getOrNull() ?: return

        executor.execute {
            DirectNetworkHttp.postBytes(
                context = context,
                url = requestDetails.url.toString(),
                body = body,
                contentType = "application/x-sentry-envelope",
                headers = requestDetails.headers,
            )
        }
    }

    /** Вызывается при падении: даём очереди дошуметь, но не дольше таймаута. */
    override fun flush(timeoutMillis: Long) {
        val done = CountDownLatch(1)
        runCatching { executor.execute { done.countDown() } }
            .onFailure { return }
        runCatching { done.await(timeoutMillis, TimeUnit.MILLISECONDS) }
    }

    override fun getRateLimiter(): RateLimiter = rateLimiter

    override fun close(isRestarting: Boolean) {
        executor.shutdown()
    }

    override fun close() = close(false)
}
