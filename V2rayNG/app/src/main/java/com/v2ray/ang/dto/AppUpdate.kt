package com.v2ray.ang.dto

/** Public release manifest served from app.a4vpn.net/update.json. */
data class AppUpdate(
    val versionCode: Int = 0,
    val versionName: String = "",
    val notes: String = "",
    val apkUrl: String = "",
    val sha256: String = "",
)
