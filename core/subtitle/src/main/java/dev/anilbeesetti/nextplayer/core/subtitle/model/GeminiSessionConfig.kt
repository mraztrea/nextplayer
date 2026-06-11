package dev.anilbeesetti.nextplayer.core.subtitle.model

data class GeminiSessionConfig(
    val apiKey: String,
    val targetLanguageCode: String = DEFAULT_TARGET_LANGUAGE_CODE,
    val displayMode: SubtitleDisplayMode = SubtitleDisplayMode.TRANSLATION_ONLY,
    val model: String = DEFAULT_MODEL,
    val sampleRate: Int = TARGET_SAMPLE_RATE,
    val numChannels: Int = TARGET_CHANNELS,
    val chunkDurationMs: Int = CHUNK_DURATION_MS,
    val inputAudioTranscriptionEnabled: Boolean = true,
    val outputAudioTranscriptionEnabled: Boolean = true,
    val echoTargetLanguage: Boolean = true,
) {
    companion object {
        const val DEFAULT_MODEL = "models/gemini-3.5-live-translate-preview"
        const val DEFAULT_TARGET_LANGUAGE_CODE = "vi"
        const val TARGET_SAMPLE_RATE = 16000
        const val TARGET_CHANNELS = 1
        const val CHUNK_DURATION_MS = 100
        const val AUDIO_MIME_TYPE = "audio/pcm;rate=16000"
    }
}

data class GeminiLiveTranscriptEvent(
    val eventType: GeminiLiveTranscriptEventType,
    val text: String? = null,
    val isFinal: Boolean = false,
    val languageCode: String? = null,
    val errorCode: String? = null,
    val receivedAt: Long = System.currentTimeMillis(),
)

enum class GeminiLiveTranscriptEventType {
    INPUT_TRANSCRIPT,
    OUTPUT_TRANSCRIPT,
    STATUS,
    ERROR,
}
