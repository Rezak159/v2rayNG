package com.v2ray.ang.handler

import android.content.Context
import android.net.Uri
import androidx.core.content.pm.PackageInfoCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.AppUpdate
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object AppUpdateManager {
    private const val UPDATE_DIRECTORY = "updates"

    suspend fun checkForUpdate(context: Context): AppUpdate? = withContext(Dispatchers.IO) {
        val body = HttpUtil.getUrlContent(
            UrlContentRequest(url = AppConfig.APP_UPDATE_MANIFEST_URL, timeout = 5_000),
        ) ?: return@withContext null

        val update = JsonUtil.fromJsonSafe(body, AppUpdate::class.java) ?: return@withContext null
        if (!isValid(update)) {
            LogUtil.w(AppConfig.TAG, "Ignoring invalid app update manifest")
            return@withContext null
        }
        update.takeIf { it.versionCode > currentVersionCode(context) }
    }

    suspend fun download(
        context: Context,
        update: AppUpdate,
        onProgress: (Float) -> Unit = {},
    ): File? = withContext(Dispatchers.IO) {
        if (!isValid(update)) return@withContext null

        val directory = File(context.cacheDir, UPDATE_DIRECTORY).apply { mkdirs() }
        val target = File(directory, "a4vpn-${update.versionCode}.apk")
        val temporary = File(directory, "a4vpn-${update.versionCode}.apk.part")
        temporary.delete()

        val downloaded = HttpUtil.downloadToFile(
            UrlContentRequest(url = update.apkUrl, timeout = 60_000),
            temporary,
        ) { bytesRead, contentLength ->
            if (contentLength > 0) onProgress(bytesRead.toFloat() / contentLength)
        }
        if (!downloaded || sha256(temporary) != update.sha256.lowercase()) {
            temporary.delete()
            LogUtil.w(AppConfig.TAG, "Downloaded APK failed SHA-256 verification")
            return@withContext null
        }

        if (target.exists()) target.delete()
        if (!temporary.renameTo(target)) {
            temporary.delete()
            return@withContext null
        }
        target
    }

    private fun isValid(update: AppUpdate): Boolean {
        val uri = runCatching { Uri.parse(update.apkUrl) }.getOrNull() ?: return false
        return update.versionCode > 0 &&
            update.versionName.isNotBlank() &&
            uri.scheme == "https" &&
            uri.host == AppConfig.APP_UPDATE_HOST &&
            uri.path?.startsWith("/releases/") == true &&
            SHA_256.matches(update.sha256)
    }

    private fun currentVersionCode(context: Context): Int =
        PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(context.packageName, 0),
        ).toInt()

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private val SHA_256 = Regex("^[a-fA-F0-9]{64}$")
}
