package com.v2ray.ang.dto.entities

data class SubscriptionItem(
    var remarks: String = "",
    var url: String = "",
    var enabled: Boolean = true,
    val addedTime: Long = System.currentTimeMillis(),
    var lastUpdated: Long = -1,
    var autoUpdate: Boolean = false,
    var updateInterval: Long = 1440, // in minutes, default to 24 hours
    var prevProfile: String? = null,
    var nextProfile: String? = null,
    var filter: String? = null,
    var allowInsecureUrl: Boolean = false,
    var userAgent: String? = null,
    // Данные из заголовков подписки (Subscription-Userinfo / profile-title).
    // Обновляются при каждом апдейте подписки. Нули — «нет данных».
    var uploadBytes: Long = 0,
    var downloadBytes: Long = 0,
    var totalBytes: Long = 0,   // 0 = безлимит / нет данных
    var expire: Long = 0,       // unix-секунды, 0 = без срока / нет данных
    var refill: Long = 0,       // unix-секунды, когда счётчик трафика обнулится; 0 = нет данных
    var title: String = "",     // декодированный profile-title
)

