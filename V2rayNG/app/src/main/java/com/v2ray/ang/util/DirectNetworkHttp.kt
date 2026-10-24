package com.v2ray.ang.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Requests A4VPN service endpoints through Wi-Fi/mobile, never through this app's VPN network. */
object DirectNetworkHttp {
    fun getText(context: Context, url: String, timeout: Int): String? =
        request(context, url, timeout) { connection ->
            if (connection.responseCode !in 200..299) return@request null
            connection.inputStream.bufferedReader().use { it.readText() }
        }

    fun downloadToFile(
        context: Context,
        url: String,
        target: File,
        timeout: Int,
        onProgress: (Long, Long) -> Unit,
    ): Boolean = request(context, url, timeout) { connection ->
        if (connection.responseCode !in 200..299) return@request null
        val contentLength = connection.contentLengthLong
        connection.inputStream.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    bytesRead += read
                    onProgress(bytesRead, contentLength)
                }
            }
        }
        true
    } ?: false

    fun postJson(context: Context, url: String, json: String, timeout: Int = 3_000) {
        runCatching {
            openConnection(context, url, timeout, "POST")?.let { connection ->
                try {
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.doOutput = true
                    connection.outputStream.bufferedWriter().use { it.write(json) }
                    connection.responseCode
                } finally {
                    connection.disconnect()
                }
            }
        }.onFailure { LogUtil.w("DirectNetworkHttp", "A4VPN service request failed: ${it.message}") }
    }

    /**
     * Отправляет произвольное тело с дополнительными заголовками и возвращает код ответа
     * (null — до сервера не дошли). Нужен диагностике: конверты Sentry — не JSON, а
     * заголовки задаёт сам SDK.
     */
    fun postBytes(
        context: Context,
        url: String,
        body: ByteArray,
        contentType: String,
        headers: Map<String, String> = emptyMap(),
        timeout: Int = 10_000,
    ): Int? = runCatching {
        openConnection(context, url, timeout, "POST")?.let { connection ->
            try {
                headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
                connection.setRequestProperty("Content-Type", contentType)
                connection.doOutput = true
                connection.outputStream.use { it.write(body) }
                connection.responseCode
            } finally {
                connection.disconnect()
            }
        }
    }.onFailure { LogUtil.w("DirectNetworkHttp", "A4VPN service request failed: ${it.message}") }.getOrNull()

    private fun openConnection(
        context: Context,
        url: String,
        timeout: Int,
        method: String = "GET",
    ): HttpURLConnection? = runCatching {
        val network = directNetwork(context) ?: return@runCatching null
        (network.openConnection(URL(url)) as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = timeout
            readTimeout = timeout
            useCaches = false
        }
    }.getOrNull()

    /**
     * GET-запрос с запасными путями: сначала по каждой обычной сети (Wi-Fi/мобильная, сперва
     * та, где есть интернет), и только если не вышло — по маршруту системы по умолчанию.
     * Последний путь нужен, когда напрямую до сервера не достучаться (провайдер режет
     * соединение, чужой VPN не пускает мимо себя): через включённый VPN запрос обычно проходит.
     */
    private fun <T : Any> request(
        context: Context,
        url: String,
        timeout: Int,
        handle: (HttpURLConnection) -> T?,
    ): T? {
        val openers = directNetworks(context).map { network ->
            { network.openConnection(URL(url)) as HttpURLConnection }
        } + { URL(url).openConnection() as HttpURLConnection }

        for (open in openers) {
            val result = runCatching {
                val connection = open().apply {
                    requestMethod = "GET"
                    connectTimeout = timeout
                    readTimeout = timeout
                    useCaches = false
                }
                try {
                    handle(connection)
                } finally {
                    connection.disconnect()
                }
            }.onFailure { LogUtil.w("DirectNetworkHttp", "A4VPN service request failed: ${it.message}") }.getOrNull()
            if (result != null) return result
        }
        return null
    }

    private fun directNetwork(context: Context): Network? = directNetworks(context).firstOrNull()

    /** Сети с интернетом без VPN; проверенные системой идут первыми. */
    private fun directNetworks(context: Context): List<Network> {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return emptyList()
        return connectivity.allNetworks
            .mapNotNull { network ->
                val capabilities = connectivity.getNetworkCapabilities(network) ?: return@mapNotNull null
                val usable = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                if (usable) network to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) else null
            }
            .sortedByDescending { (_, validated) -> validated }
            .map { (network, _) -> network }
    }
}
