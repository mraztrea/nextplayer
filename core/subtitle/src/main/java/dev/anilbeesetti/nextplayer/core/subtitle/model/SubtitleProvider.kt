package dev.anilbeesetti.nextplayer.core.subtitle.model

enum class SubtitleProvider {
    SONIOX,
    GEMINI_LIVE,
    ;

    companion object {
        fun fromPreference(value: String?): SubtitleProvider {
            return entries.firstOrNull { it.name == value } ?: SONIOX
        }
    }
}
