package dev.anilbeesetti.nextplayer.core.subtitle.model

data class SonioxSessionConfig(
    val apiKey: String,
    val model: String = "stt-rt-v4",
    val audioFormat: String = "pcm_s16le",
    val sampleRate: Int = 16000,
    val numChannels: Int = 1,
    val sourceLanguage: String? = null,
    val targetLanguage: String = "vi",
    val endpointDelayMs: Int = 3000,
)
