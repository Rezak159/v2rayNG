package com.v2ray.ang.handler

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteWorkManager
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.extension.concatUrl
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * a4vpn: keeps the built-in geo databases fresh.
 *
 * The RoscomVPN routing preset depends on categories that only exist in the
 * RoscomVPN geoip/geosite databases, and blocklists change often — so the
 * files are re-downloaded once a day via WorkManager, mirroring the
 * [SubscriptionUpdater] pattern. Manual update is available from the
 * settings screen.
 */
object GeoUpdater {

    private val builtInGeoFiles =
        listOf(AppConfig.GEOSITE_DAT, AppConfig.GEOIP_DAT, AppConfig.GEOIP_ONLY_CN_PRIVATE_DAT)
    private const val UPDATE_INTERVAL_HOURS = 24L

    /** Download URL of one built-in geo file for the given source repo. */
    fun geoFileUrl(geoFile: String, geoFilesSource: String): String {
        if (geoFile == AppConfig.GEOIP_ONLY_CN_PRIVATE_DAT) {
            return AppConfig.GEOIP_ONLY_CN_PRIVATE_URL
        }
        // The RoscomVPN source keeps geoip and geosite in two separate repos
        if (geoFilesSource == AppConfig.ROSCOM_GEO_SOURCE) {
            when (geoFile) {
                AppConfig.GEOIP_DAT -> return AppConfig.ROSCOM_GEOIP_URL
                AppConfig.GEOSITE_DAT -> return AppConfig.ROSCOM_GEOSITE_URL
            }
        }
        return String.format(AppConfig.GITHUB_DOWNLOAD_URL, geoFilesSource).concatUrl(geoFile)
    }

    /** Timestamp of the last update of one geo file, 0 when the file is missing. */
    fun geoFileLastModified(context: Context, geoFile: String): Long {
        val file = File(Utils.userAssetPath(context), geoFile)
        return if (file.exists()) file.lastModified() else 0L
    }

    /**
     * Download every built-in geo file, preferring the running proxy and
     * falling back to a direct connection.
     *
     * @return the number of successfully updated files.
     */
    fun updateGeoFiles(context: Context): Int {
        val extDir = File(Utils.userAssetPath(context))
        val source = MmkvManager.decodeSettingsString(AppConfig.PREF_GEO_FILES_SOURCES)
            ?: AppConfig.GEO_FILES_SOURCES.first()
        val httpPort = SettingsManager.getHttpPort()
        val username = SettingsManager.getSocksUsername()
        val password = SettingsManager.getSocksPassword()

        var successCount = 0
        builtInGeoFiles.forEach { geoFile ->
            val url = geoFileUrl(geoFile, source)
            val portsToTry = if (httpPort == 0) listOf(0) else listOf(httpPort, 0)
            if (portsToTry.any { tryDownload(url, extDir, geoFile, it, username, password) }) {
                successCount++
            }
        }
        LogUtil.i(AppConfig.TAG, "GeoUpdater: $successCount/${builtInGeoFiles.size} geo file(s) updated")
        return successCount
    }

    private fun tryDownload(
        url: String,
        extDir: File,
        fileName: String,
        httpPort: Int,
        proxyUsername: String?,
        proxyPassword: String?,
    ): Boolean {
        val targetTemp = File(extDir, fileName + "_temp")
        val target = File(extDir, fileName)
        try {
            if (
                HttpUtil.downloadToFile(
                    UrlContentRequest(
                        url = url,
                        timeout = 15000,
                        httpPort = httpPort,
                        proxyUsername = proxyUsername,
                        proxyPassword = proxyPassword
                    ),
                    targetTemp
                )
            ) {
                targetTemp.renameTo(target)
                return true
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "GeoUpdater: failed to download $fileName", e)
        }
        return false
    }

    /**
     * Schedule the daily background update, keeping existing work.
     * Call from: MainViewModel background initialization.
     */
    fun sync(context: Context = AngApplication.application) {
        val newest = builtInGeoFiles.maxOf { geoFileLastModified(context, it) }
        val intervalMillis = TimeUnit.HOURS.toMillis(UPDATE_INTERVAL_HOURS)
        val initialDelayMillis =
            if (newest <= 0L) 0L else maxOf(0L, newest + intervalMillis - System.currentTimeMillis())

        val request = PeriodicWorkRequestBuilder<UpdateTask>(UPDATE_INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .addTag(AppConfig.GEO_UPDATE_TASK_NAME)
            .build()

        RemoteWorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AppConfig.GEO_UPDATE_TASK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        LogUtil.i(
            AppConfig.TAG,
            "GeoUpdater: scheduled interval=${UPDATE_INTERVAL_HOURS}h initialDelay=${initialDelayMillis / 1000}s"
        )
    }

    class UpdateTask(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            LogUtil.i(AppConfig.TAG, "GeoUpdater automatic update starting")
            updateGeoFiles(applicationContext)
            return Result.success()
        }
    }
}
