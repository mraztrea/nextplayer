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
        private const val MIN_TRANSLATED_DISPLAY_DURATION_MS = 2_500L
    }

    private val idCounter = AtomicLong(0)
    private var currentGenerationId = 0L
    private var generationBasePositionMs = 0L
    private var currentPlaybackPositionMs = 0L

    // Display buffer (trimmable) — shown on overlay
    private val _displaySegments = MutableStateFlow<List<SubtitleSegment>>(emptyList())
    val displaySegments: StateFlow<List<SubtitleSegment>> = _displaySegments.asStateFlow()

    // Session log (non-trimmable) — full history
    private val sessionLog = mutableListOf<SubtitleSegment>()
    private val activeSegments = mutableListOf<SubtitleSegment>()

    // Recent translations for carryover context
    private val recentTranslations = mutableListOf<String>()

    // Provisional text state
    private val _provisionalText = MutableStateFlow("")
    val provisionalText: StateFlow<String> = _provisionalText.asStateFlow()

    private val _provisionalSpeaker = MutableStateFlow<String?>(null)
    val provisionalSpeaker: StateFlow<String?> = _provisionalSpeaker.asStateFlow()

    fun beginGeneration(generationId: Long, basePositionMs: Long, clearHistory: Boolean = true) {
        currentGenerationId = generationId
        generationBasePositionMs = basePositionMs
        currentPlaybackPositionMs = basePositionMs
        activeSegments.clear()
        if (clearHistory) {
            sessionLog.clear()
            recentTranslations.clear()
        }
        _displaySegments.value = emptyList()
        _provisionalText.value = ""
        _provisionalSpeaker.value = null
    }

    fun onPlaybackPosition(positionMs: Long) {
        currentPlaybackPositionMs = positionMs
        publishDisplaySegments()
    }

    fun onOriginal(
        text: String,
        speaker: String?,
        language: String?,
        confidence: Float?,
        startMs: Long?,
        endMs: Long?,
    ) {
        val safeStartMs = startMs ?: return
        val safeEndMs = endMs ?: safeStartMs
        val segment = SubtitleSegment(
            id = idCounter.incrementAndGet(),
            originalText = text,
            status = SegmentStatus.ORIGINAL,
            speaker = speaker,
            language = language,
            confidence = confidence,
            sourceStartMs = safeStartMs,
            sourceEndMs = safeEndMs,
            targetStartMs = generationBasePositionMs + safeStartMs,
            targetEndMs = generationBasePositionMs + safeEndMs,
            generationId = currentGenerationId,
        )

        sessionLog.add(segment)
        activeSegments.add(segment)
        publishDisplaySegments()

        // Clear provisional since we got a final
        _provisionalText.value = ""
        _provisionalSpeaker.value = null
    }

    fun onTranslation(text: String) {
        // Find the oldest ORIGINAL segment without translation (FIFO)
        val targetIndex = activeSegments.indexOfFirst {
            it.generationId == currentGenerationId && it.status == SegmentStatus.ORIGINAL
        }

        if (targetIndex >= 0) {
            val currentSegment = activeSegments[targetIndex]
            val minimumReadableEndMs =
                currentSegment.targetStartMs + MIN_TRANSLATED_DISPLAY_DURATION_MS
            val lateArrivalEndMs = if (currentPlaybackPositionMs > currentSegment.targetEndMs) {
                currentPlaybackPositionMs + MIN_TRANSLATED_DISPLAY_DURATION_MS
            } else {
                currentSegment.targetEndMs
            }
            val updated = currentSegment.copy(
                translationText = text,
                status = SegmentStatus.TRANSLATED,
                targetEndMs = maxOf(
                    currentSegment.targetEndMs,
                    minimumReadableEndMs,
                    lateArrivalEndMs,
                ),
            )
            activeSegments[targetIndex] = updated

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
        publishDisplaySegments()
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
        activeSegments.clear()
        sessionLog.clear()
        recentTranslations.clear()
        _provisionalText.value = ""
        _provisionalSpeaker.value = null
        idCounter.set(0)
        currentGenerationId = 0L
        generationBasePositionMs = 0L
        currentPlaybackPositionMs = 0L
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
        publishDisplaySegments()
    }

    private fun trimDisplayBuffer(segments: MutableList<SubtitleSegment>) {
        if (segments.size <= MAX_DISPLAY_SEGMENTS) {
            return
        }

        var overflow = segments.size - MAX_DISPLAY_SEGMENTS
        val iterator = segments.listIterator()

        while (iterator.hasNext() && overflow > 0) {
            val segment = iterator.next()
            if (segment.status == SegmentStatus.TRANSLATED) {
                iterator.remove()
                overflow--
            }
        }

        while (segments.size > MAX_DISPLAY_SEGMENTS) {
            segments.removeFirst()
        }
    }

    private fun publishDisplaySegments() {
        val visibleSegments = activeSegments
            .filter { segment ->
                segment.generationId == currentGenerationId &&
                    currentPlaybackPositionMs in segment.targetStartMs..segment.targetEndMs
            }
            .toMutableList()
        publishDisplaySegments(visibleSegments)
    }

    private fun publishDisplaySegments(segments: MutableList<SubtitleSegment>) {
        cleanupStaleSegments(segments)
        trimDisplayBuffer(segments)
        _displaySegments.value = segments
    }

    private fun cleanupStaleSegments(segments: MutableList<SubtitleSegment>) {
        activeSegments.removeAll { segment ->
            segment.generationId != currentGenerationId ||
                (segment.status == SegmentStatus.ORIGINAL &&
                    currentPlaybackPositionMs > segment.targetEndMs + STALE_TIMEOUT_MS) ||
                currentPlaybackPositionMs > segment.targetEndMs + STALE_TIMEOUT_MS
        }

        val pending = activeSegments.filter {
            it.generationId == currentGenerationId && it.status == SegmentStatus.ORIGINAL
        }
        if (pending.size > MAX_PENDING_ORIGINALS) {
            val toRemove = pending.take(pending.size - MAX_PENDING_ORIGINALS).map { it.id }.toSet()
            activeSegments.removeAll { it.id in toRemove }
            segments.removeAll { it.id in toRemove }
        }
    }
}
