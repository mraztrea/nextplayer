package dev.anilbeesetti.nextplayer.core.subtitle.session

import dev.anilbeesetti.nextplayer.core.subtitle.model.SegmentStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleSessionManager @Inject constructor() {

    companion object {
        private const val MAX_DISPLAY_SEGMENTS = 20
        private const val STALE_TIMEOUT_MS = 10_000L
        private const val MAX_PENDING_ORIGINALS = 3
        private const val CARRYOVER_CONTEXT_LIMIT = 500
    }

    private val idCounter = AtomicLong(0)

    // Display buffer (trimmable) — shown on overlay
    private val _displaySegments = MutableStateFlow<List<SubtitleSegment>>(emptyList())
    val displaySegments: StateFlow<List<SubtitleSegment>> = _displaySegments.asStateFlow()

    // Session log (non-trimmable) — full history
    private val sessionLog = mutableListOf<SubtitleSegment>()

    // Recent translations for carryover context
    private val recentTranslations = mutableListOf<String>()

    // Provisional text state
    private val _provisionalText = MutableStateFlow("")
    val provisionalText: StateFlow<String> = _provisionalText.asStateFlow()

    private val _provisionalSpeaker = MutableStateFlow<String?>(null)
    val provisionalSpeaker: StateFlow<String?> = _provisionalSpeaker.asStateFlow()

    fun onOriginal(text: String, speaker: String?, language: String?, confidence: Float?) {
        val segment = SubtitleSegment(
            id = idCounter.incrementAndGet(),
            originalText = text,
            status = SegmentStatus.ORIGINAL,
            speaker = speaker,
            language = language,
            confidence = confidence,
        )

        sessionLog.add(segment)
        val current = _displaySegments.value.toMutableList()
        current.add(segment)
        cleanupStaleSegments(current)
        trimDisplayBuffer(current)
        _displaySegments.value = current

        // Clear provisional since we got a final
        _provisionalText.value = ""
        _provisionalSpeaker.value = null
    }

    fun onTranslation(text: String) {
        // Find the oldest ORIGINAL segment without translation (FIFO)
        val current = _displaySegments.value.toMutableList()
        val targetIndex = current.indexOfFirst { it.status == SegmentStatus.ORIGINAL }

        if (targetIndex >= 0) {
            val updated = current[targetIndex].copy(
                translationText = text,
                status = SegmentStatus.TRANSLATED,
            )
            current[targetIndex] = updated

            // Update session log too
            val logIndex = sessionLog.indexOfLast { it.id == updated.id }
            if (logIndex >= 0) {
                sessionLog[logIndex] = updated
            }

            recentTranslations.add(text)
            if (recentTranslations.size > 10) {
                recentTranslations.removeFirst()
            }
        }

        _provisionalText.value = ""
        _provisionalSpeaker.value = null
        _displaySegments.value = current
    }

    fun onProvisional(text: String, speaker: String?, language: String?) {
        _provisionalText.value = text
        _provisionalSpeaker.value = speaker
    }

    fun onEndpointReached() {
        _provisionalText.value = ""
        _provisionalSpeaker.value = null
    }

    fun clearDisplay() {
        _displaySegments.value = emptyList()
        _provisionalText.value = ""
        _provisionalSpeaker.value = null
    }

    fun reset() {
        _displaySegments.value = emptyList()
        sessionLog.clear()
        recentTranslations.clear()
        _provisionalText.value = ""
        _provisionalSpeaker.value = null
        idCounter.set(0)
    }

    fun getCarryoverContext(): String {
        val context = recentTranslations.joinToString(" ")
        return if (context.length > CARRYOVER_CONTEXT_LIMIT) {
            context.takeLast(CARRYOVER_CONTEXT_LIMIT)
        } else {
            context
        }
    }

    fun trimDisplayBuffer() {
        val current = _displaySegments.value.toMutableList()
        trimDisplayBuffer(current)
        _displaySegments.value = current
    }

    private fun trimDisplayBuffer(segments: MutableList<SubtitleSegment>) {
        while (segments.size > MAX_DISPLAY_SEGMENTS) {
            segments.removeFirst()
        }
    }

    private fun cleanupStaleSegments(segments: MutableList<SubtitleSegment>) {
        val now = System.currentTimeMillis()

        // Xóa các segment hết thời gian chờ translation
        segments.removeAll { segment ->
            segment.status == SegmentStatus.ORIGINAL &&
                now - segment.createdAt > STALE_TIMEOUT_MS
        }

        // Giới hạn số lượng pending originals: xóa những cái cũ nhất
        val pending = segments.filter { it.status == SegmentStatus.ORIGINAL }
        if (pending.size > MAX_PENDING_ORIGINALS) {
            val toRemove = pending.take(pending.size - MAX_PENDING_ORIGINALS).map { it.id }.toSet()
            segments.removeAll { it.id in toRemove }
        }
    }
}
