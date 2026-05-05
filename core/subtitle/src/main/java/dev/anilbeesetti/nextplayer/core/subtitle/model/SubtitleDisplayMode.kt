package dev.anilbeesetti.nextplayer.core.subtitle.model

enum class SubtitleDisplayMode {
    ORIGINAL_ONLY,
    TRANSLATION_ONLY,
    BILINGUAL,

    ;

    companion object {
        fun fromPreference(value: String?): SubtitleDisplayMode {
            return entries.firstOrNull { it.name == value } ?: TRANSLATION_ONLY
        }
    }
}
