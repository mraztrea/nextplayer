package dev.anilbeesetti.nextplayer.core.subtitle.model

object GeminiLanguageMapper {
    fun toTargetLanguageCode(value: String): String {
        val normalized = value.trim().lowercase()
        return when {
            normalized.isBlank() -> GeminiSessionConfig.DEFAULT_TARGET_LANGUAGE_CODE
            normalized == "vn" -> "vi"
            else -> normalized
        }
    }
}
