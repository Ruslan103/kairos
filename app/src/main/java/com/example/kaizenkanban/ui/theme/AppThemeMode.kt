package com.example.kaizenkanban.ui.theme

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    SLATE;

    companion object {
        fun fromCode(code: String?): AppThemeMode = when (code?.lowercase()) {
            "light" -> LIGHT
            "dark" -> DARK
            "slate" -> SLATE
            else -> SYSTEM
        }
    }

    val code: String
        get() = when (this) {
            SYSTEM -> "system"
            LIGHT -> "light"
            DARK -> "dark"
            SLATE -> "slate"
        }
}
