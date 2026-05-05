package dev.anilbeesetti.nextplayer.settings.utils

import java.util.Locale

object LocalesHelper {

    fun getAvailableLocales(): List<Pair<String, String>> {
        return try {
            Locale.getAvailableLocales().map {
                val key = it.isO3Language
                val language = it.displayLanguage
                Pair(language, key)
            }.distinctBy { it.second }.sortedBy { it.first }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf()
        }
    }

    fun getLocaleDisplayLanguage(key: String): String {
        return try {
            Locale.getAvailableLocales().first { it.isO3Language == key }.displayLanguage
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun getAvailableTranslationLanguages(): List<Pair<String, String>> {
        val displayLocale = Locale.getDefault()

        return try {
            Locale.getISOLanguages().mapNotNull { languageCode ->
                runCatching {
                    val locale = Locale(languageCode)
                    val displayLanguage = locale.getDisplayLanguage(displayLocale)
                    if (displayLanguage.isBlank()) {
                        null
                    } else {
                        displayLanguage.replaceFirstChar {
                            if (it.isLowerCase()) {
                                it.titlecase(displayLocale)
                            } else {
                                it.toString()
                            }
                        } to languageCode
                    }
                }.getOrNull()
            }.distinctBy { it.second }.sortedBy { it.first }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf()
        }
    }

    fun getTranslationLanguageDisplayName(languageCode: String): String {
        return try {
            Locale(languageCode).getDisplayLanguage(Locale.getDefault())
                .replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(Locale.getDefault())
                    } else {
                        it.toString()
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
