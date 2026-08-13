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
        openConnection(context, url, timeout)?.let { connection ->
            try {
                if (connection.responseCode !in 200..299) return@let null
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }

    fun downloadToFile(
        context: Context,
        url: String,
        target: File,
        timeout: Int,
        onProgress: (Long, Long) -> Unit,
    ): Boolean = openConnection(context, url, timeout)?.let { connection ->
        try {
            if (connection.responseCode !in 200..299) return@let false
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
        } finally {
            connection.disconnect()
        }
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

    private fun directNetwork(context: Context): Network? {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return null
        return connectivity.allNetworks.firstOrNull { network ->
            connectivity.getNetworkCapabilities(network)?.let { capabilities ->
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            } == true
        }
    }
}
