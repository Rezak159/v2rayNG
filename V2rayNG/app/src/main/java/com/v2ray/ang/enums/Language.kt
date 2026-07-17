package com.v2ray.ang.enums

enum class Language(val code: String) {
    AUTO("auto"),
    ENGLISH("en"),
    RUSSIAN("ru");

    companion object {
        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: AUTO
        }
    }
}
