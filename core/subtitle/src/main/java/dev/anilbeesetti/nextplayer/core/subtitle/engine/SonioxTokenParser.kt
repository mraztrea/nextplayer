package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.common.Logger
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SonioxTokenParser @Inject constructor() {

    companion object {
        private const val TAG = "SonioxTokenParser"
        private const val END_TOKEN = "<end>"
    }

    data class TokenData(
        val text: String,
        val isFinal: Boolean,
        val translationStatus: String,
        val speaker: String?,
        val language: String?,
        val confidence: Float?,
        val startMs: Long?,
        val endMs: Long?,
    )

    interface Callback {
        fun onOriginal(
            text: String,
            speaker: String?,
            language: String?,
            confidence: Float?,
            startMs: Long?,
            endMs: Long?,
        )
        fun onTranslation(text: String)
        fun onProvisional(text: String, speaker: String?, language: String?)
        fun onEndpointReached()
    }

    private var callback: Callback? = null

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun parseTokens(tokens: JSONArray) {
        val parsedTokens = buildList {
            for (i in 0 until tokens.length()) {
                val token = tokens.getJSONObject(i)
                add(
                    TokenData(
                        text = token.optString("text", ""),
                        isFinal = token.optBoolean("is_final", false),
                        translationStatus = token.optString("translation_status", "none"),
                        speaker = token.takeIf { it.has("speaker") && !it.isNull("speaker") }?.getString("speaker"),
                        language = token.takeIf { it.has("language") && !it.isNull("language") }?.getString("language"),
                        confidence = if (token.has("confidence") && !token.isNull("confidence")) {
                            token.getDouble("confidence").toFloat()
                        } else {
                            null
                        },
                        startMs = if (token.has("start_ms") && !token.isNull("start_ms")) {
                            token.getLong("start_ms")
                        } else {
                            null
                        },
                        endMs = if (token.has("end_ms") && !token.isNull("end_ms")) {
                            token.getLong("end_ms")
                        } else {
                            null
                        },
                    ),
                )
            }
        }
        parseTokenData(parsedTokens)
    }

    internal fun parseTokenData(tokens: List<TokenData>) {
        val cb = callback ?: return

        // Accumulate text by classification within this batch
        val originalParts = StringBuilder()
        val translationParts = StringBuilder()
        val provisionalParts = StringBuilder()

        var lastSpeaker: String? = null
        var lastLanguage: String? = null
        var totalConfidence = 0f
        var confidenceCount = 0
        var earliestStartMs: Long? = null
        var latestEndMs: Long? = null

        for (token in tokens) {
            val text = token.text
            val isFinal = token.isFinal
            val translationStatus = token.translationStatus
            val speaker = token.speaker
            val language = token.language
            val confidence = token.confidence
            val startMs = token.startMs
            val endMs = token.endMs

            // Handle end token
            if (text == END_TOKEN) {
                flushAccumulated(
                    cb, originalParts, translationParts, provisionalParts,
                    lastSpeaker, lastLanguage, totalConfidence, confidenceCount, earliestStartMs, latestEndMs,
                )
                cb.onEndpointReached()
                continue
            }

            if (speaker != null) lastSpeaker = speaker
            if (language != null) lastLanguage = language
            if (confidence != null) {
                totalConfidence += confidence
                confidenceCount++
            }

            when {
                // Final original text (includes "none" with is_final=true)
                isFinal && (translationStatus == "original" || translationStatus == "none") -> {
                    originalParts.append(text)
                    earliestStartMs = when {
                        earliestStartMs == null -> startMs
                        startMs == null -> earliestStartMs
                        else -> minOf(earliestStartMs, startMs)
                    }
                    latestEndMs = when {
                        latestEndMs == null -> endMs
                        endMs == null -> latestEndMs
                        else -> maxOf(latestEndMs, endMs)
                    }
                }
                // Final translation text
                isFinal && translationStatus == "translation" -> {
                    translationParts.append(text)
                }
                // Provisional (non-final)
                !isFinal -> {
                    provisionalParts.append(text)
                }
            }
        }

        flushAccumulated(
            cb, originalParts, translationParts, provisionalParts,
            lastSpeaker, lastLanguage, totalConfidence, confidenceCount, earliestStartMs, latestEndMs,
        )
    }

    private fun flushAccumulated(
        cb: Callback,
        original: StringBuilder,
        translation: StringBuilder,
        provisional: StringBuilder,
        speaker: String?,
        language: String?,
        totalConfidence: Float,
        confidenceCount: Int,
        startMs: Long?,
        endMs: Long?,
    ) {
        if (original.isNotEmpty()) {
            val avgConfidence = if (confidenceCount > 0) totalConfidence / confidenceCount else null
            cb.onOriginal(original.toString(), speaker, language, avgConfidence, startMs, endMs)
            original.clear()
        }
        if (translation.isNotEmpty()) {
            cb.onTranslation(translation.toString())
            translation.clear()
        }
        if (provisional.isNotEmpty()) {
            cb.onProvisional(provisional.toString(), speaker, language)
            provisional.clear()
        }
    }
}
