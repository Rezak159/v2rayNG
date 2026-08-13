package com.v2ray.ang.dto

data class AppPolicyEnvelope(
    val payload: String = "",
    val signature: String = "",
)

data class AppPolicyPayload(
    val minVersionCode: Int = 0,
    val expiresAt: Long = 0,
    val message: String = "",
    val downloadUrl: String = "",
)
