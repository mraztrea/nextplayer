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

    interface Callback {
        fun onOriginal(text: String, speaker: String?, language: String?, confidence: Float?)
        fun onTranslation(text: String)
        fun onProvisional(text: String, speaker: String?, language: String?)
        fun onEndpointReached()
    }

    private var callback: Callback? = null

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun parseTokens(tokens: JSONArray) {
        val cb = callback ?: return

        // Accumulate text by classification within this batch
        val originalParts = StringBuilder()
        val translationParts = StringBuilder()
        val provisionalParts = StringBuilder()

        var lastSpeaker: String? = null
        var lastLanguage: String? = null
        var totalConfidence = 0f
        var confidenceCount = 0

        for (i in 0 until tokens.length()) {
            val token = tokens.getJSONObject(i)
            val text = token.optString("text", "")
            val isFinal = token.optBoolean("is_final", false)
            val translationStatus = token.optString("translation_status", "none")
            val speaker = token.optString("speaker", null)
            val language = token.optString("language", null)
            val confidence = if (token.has("confidence") && !token.isNull("confidence")) {
                token.getDouble("confidence").toFloat()
            } else {
                null
            }

            // Handle end token
            if (text == END_TOKEN) {
                flushAccumulated(
                    cb, originalParts, translationParts, provisionalParts,
                    lastSpeaker, lastLanguage, totalConfidence, confidenceCount,
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
            lastSpeaker, lastLanguage, totalConfidence, confidenceCount,
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
    ) {
        if (original.isNotEmpty()) {
            val avgConfidence = if (confidenceCount > 0) totalConfidence / confidenceCount else null
            cb.onOriginal(original.toString(), speaker, language, avgConfidence)
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
