package dev.anilbeesetti.nextplayer.core.subtitle.engine

object SubtitleDiagnosticLogger {
    private val sensitiveKeys = setOf("apikey", "api_key", "audio", "pcm", "transcript", "text")

    fun metadata(vararg fields: Pair<String, String?>): String {
        return fields.joinToString(separator = " ") { (key, value) ->
            val safeValue = if (key.lowercase() in sensitiveKeys) {
                "[redacted]"
            } else {
                value.orEmpty()
            }
            "$key=$safeValue"
        }
    }
}
