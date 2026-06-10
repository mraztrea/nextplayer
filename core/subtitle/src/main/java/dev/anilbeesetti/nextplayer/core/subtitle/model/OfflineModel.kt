package dev.anilbeesetti.nextplayer.core.subtitle.model

data class OfflineModel(
    val id: String,
    val displayName: String,
    val version: String,
    val manifestVersion: Int,
    val sourceLanguage: String,
    val targetLanguages: Set<String>,
    val capabilities: Set<String>,
    val sizeBytes: Long,
    val installedPath: String,
    val checksum: String? = null,
    val state: OfflineModelState = OfflineModelState.NOT_INSTALLED,
    val lastError: String? = null,
) {
    val isReady: Boolean
        get() = state == OfflineModelState.READY &&
            sourceLanguage == JAPANESE_LANGUAGE &&
            VIETNAMESE_LANGUAGE in targetLanguages &&
            ENGLISH_LANGUAGE in targetLanguages &&
            JAPANESE_TRANSCRIPT_CAPABILITY in capabilities &&
            JAPANESE_TO_VIETNAMESE_CAPABILITY in capabilities &&
            JAPANESE_TO_ENGLISH_CAPABILITY in capabilities

    companion object {
        const val JAPANESE_LANGUAGE = "ja"
        const val VIETNAMESE_LANGUAGE = "vi"
        const val ENGLISH_LANGUAGE = "en"
        const val JAPANESE_TRANSCRIPT_CAPABILITY = "japanese_transcript"
        const val JAPANESE_TO_VIETNAMESE_CAPABILITY = "ja_to_vi"
        const val JAPANESE_TO_ENGLISH_CAPABILITY = "ja_to_en"
    }
}

enum class OfflineModelState {
    NOT_INSTALLED,
    DOWNLOADING,
    IMPORTING,
    READY,
    ERROR,
    NEEDS_UPDATE,
}
