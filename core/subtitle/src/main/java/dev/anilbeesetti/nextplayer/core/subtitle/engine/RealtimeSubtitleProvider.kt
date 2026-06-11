package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleEngineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleProvider
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleSegment
import kotlinx.coroutines.flow.StateFlow

interface RealtimeSubtitleProvider {
    val provider: SubtitleProvider
    val status: StateFlow<SubtitleEngineStatus>
    val displaySegments: StateFlow<List<SubtitleSegment>>
    val provisionalText: StateFlow<String>
    val provisionalSpeaker: StateFlow<String?>

    suspend fun start(): SubtitleStartResult
    fun stop()
    fun resetSession()
    fun sendAudio(pcmData: ByteArray)
}
