package com.example.kaizenkanban.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.kaizenkanban.data.local.KairosPreferences
import java.util.Locale

enum class AppLanguage {
    EN,
    RU;

    val code: String
        get() = if (this == RU) "ru" else "en"

    val locale: Locale
        get() = if (this == RU) Locale("ru") else Locale.ENGLISH

    companion object {
        fun fromCode(code: String?): AppLanguage =
            if (code.equals("ru", ignoreCase = true)) RU else EN
    }
}

class LanguageController(initial: AppLanguage, private val persist: (AppLanguage) -> Unit) {
    var language by mutableStateOf(initial)
        private set

    fun select(value: AppLanguage) {
        if (language == value) return
        language = value
        persist(value)
    }

    fun toggle() {
        select(if (language == AppLanguage.EN) AppLanguage.RU else AppLanguage.EN)
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.EN }
val LocalAppStrings = staticCompositionLocalOf { AppStrings(AppLanguage.EN) }
val LocalLanguageController = staticCompositionLocalOf<LanguageController> {
    error("LanguageController is not provided")
}

@Composable
fun ProvideAppLanguage(
    prefs: KairosPreferences,
    content: @Composable () -> Unit
) {
    val controller = remember {
        LanguageController(AppLanguage.fromCode(prefs.appLanguageCode)) { prefs.appLanguageCode = it.code }
    }
    val language = controller.language
    CompositionLocalProvider(
        LocalLanguageController provides controller,
        LocalAppLanguage provides language,
        LocalAppStrings provides AppStrings(language),
        content = content
    )
}
