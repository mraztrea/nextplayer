package dev.anilbeesetti.nextplayer.core.subtitle.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.subtitle.di.SubtitleScope
import dev.anilbeesetti.nextplayer.core.subtitle.model.LookaheadAudioChunk
import dev.anilbeesetti.nextplayer.core.subtitle.model.LookaheadPipelineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.LookaheadSessionState
import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface LookaheadAudioPipeline {
    val state: StateFlow<LookaheadSessionState>

    fun start(mediaId: String, initialPositionMs: Long, audioTrackKey: String?, generationId: Long)

    fun pause()

    fun resume(playbackPositionMs: Long)

    fun seekTo(positionMs: Long, audioTrackKey: String?, generationId: Long)

    fun updatePlaybackPosition(positionMs: Long)

    fun setFallbackActive(isActive: Boolean)

    fun stop()
}

@Singleton
class MediaCodecLookaheadAudioPipeline @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val audioBatcher: AudioBatcher,
    @param:SubtitleScope private val scope: CoroutineScope,
) : LookaheadAudioPipeline {

    companion object {
        private const val TAG = "LookaheadAudioPipeline"
        private const val TARGET_LEAD_MS = 5_000L
        private const val LOW_WATER_MS = 3_000L
        private const val HIGH_WATER_MS = 5_000L
        private const val LOOP_DELAY_MS = 50L
        private const val DEQUEUE_TIMEOUT_US = 10_000L
        private const val BYTES_PER_SAMPLE = 2
    }

    private val _state = MutableStateFlow(
        LookaheadSessionState(
            targetLeadMs = TARGET_LEAD_MS,
            lowWaterMs = LOW_WATER_MS,
            highWaterMs = HIGH_WATER_MS,
        ),
    )
    override val state: StateFlow<LookaheadSessionState> = _state.asStateFlow()

    private var decodeJob: Job? = null
    private var currentMediaId: String? = null
    private var currentAudioTrackKey: String? = null
    private var currentGenerationId = 0L
    private var nextSequenceId = 0L

    override fun start(
        mediaId: String,
        initialPositionMs: Long,
        audioTrackKey: String?,
        generationId: Long,
    ) {
        currentMediaId = mediaId
        currentAudioTrackKey = audioTrackKey
        currentGenerationId = generationId
        restartDecoding(
            mediaId = mediaId,
            positionMs = initialPositionMs,
            audioTrackKey = audioTrackKey,
            generationId = generationId,
            status = LookaheadPipelineStatus.WARMING,
        )
    }

    override fun pause() {
        _state.value = _state.value.copy(isPaused = true)
    }

    override fun resume(playbackPositionMs: Long) {
        updatePlaybackPosition(playbackPositionMs)
        _state.value = _state.value.copy(isPaused = false)
    }

    override fun seekTo(positionMs: Long, audioTrackKey: String?, generationId: Long) {
        val mediaId = currentMediaId ?: return
        currentAudioTrackKey = audioTrackKey
        currentGenerationId = generationId
        restartDecoding(
            mediaId = mediaId,
            positionMs = positionMs,
            audioTrackKey = audioTrackKey,
            generationId = generationId,
            status = LookaheadPipelineStatus.RESETTING,
        )
    }

    override fun updatePlaybackPosition(positionMs: Long) {
        _state.value = _state.value.copy(playbackPositionMs = positionMs)
    }

    override fun setFallbackActive(isActive: Boolean) {
        _state.value = _state.value.copy(
            isFallbackActive = isActive,
            status = if (isActive) LookaheadPipelineStatus.FALLBACK else _state.value.status,
        )
    }

    override fun stop() {
        decodeJob?.cancel()
        decodeJob = null
        currentMediaId = null
        currentAudioTrackKey = null
        currentGenerationId = 0L
        audioBatcher.reset()
        _state.value = LookaheadSessionState(
            targetLeadMs = TARGET_LEAD_MS,
            lowWaterMs = LOW_WATER_MS,
            highWaterMs = HIGH_WATER_MS,
        )
    }

    private fun restartDecoding(
        mediaId: String,
        positionMs: Long,
        audioTrackKey: String?,
        generationId: Long,
        status: LookaheadPipelineStatus,
    ) {
        decodeJob?.cancel()
        audioBatcher.reset()
        nextSequenceId = 0L
        _state.value = LookaheadSessionState(
            generationId = generationId,
            playbackPositionMs = positionMs,
            lookaheadCursorMs = positionMs,
            bufferedUntilMs = positionMs,
            targetLeadMs = TARGET_LEAD_MS,
            lowWaterMs = LOW_WATER_MS,
            highWaterMs = HIGH_WATER_MS,
            activeAudioTrackKey = audioTrackKey,
            status = status,
        )
        decodeJob = scope.launch {
            try {
                decodeAhead(mediaId, positionMs, audioTrackKey, generationId)
            } catch (_: InterruptedException) {
                Logger.logDebug(TAG, "Lookahead decoding interrupted")
            } catch (t: Throwable) {
                Logger.logError(TAG, "Lookahead pipeline failed", t)
                _state.value = _state.value.copy(status = LookaheadPipelineStatus.ERROR)
            }
        }
    }

    private suspend fun decodeAhead(
        mediaId: String,
        initialPositionMs: Long,
        audioTrackKey: String?,
        generationId: Long,
    ) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setMediaDataSource(mediaId)
            val trackIndex = extractor.findAudioTrackIndex(audioTrackKey)
            require(trackIndex >= 0) { "No audio track found for lookahead" }

            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = requireNotNull(inputFormat.getString(MediaFormat.KEY_MIME)) {
                "Missing audio mime type"
            }
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            extractor.seekTo(initialPositionMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEos = false
            var sawOutputEos = false
            var trimBeforeMs: Long? = initialPositionMs
            var outputSampleRate = inputFormat.getIntegerOrDefault(MediaFormat.KEY_SAMPLE_RATE, 16_000)
            var outputChannelCount = inputFormat.getIntegerOrDefault(MediaFormat.KEY_CHANNEL_COUNT, 1)

            while (!sawOutputEos && currentGenerationId == generationId) {
                val snapshot = _state.value
                if (snapshot.isFallbackActive) {
                    break
                }

                if (snapshot.isPaused) {
                    delay(LOOP_DELAY_MS)
                    continue
                }

                val leadMs = snapshot.bufferedUntilMs - snapshot.playbackPositionMs
                if (leadMs >= snapshot.highWaterMs) {
                    if (snapshot.status != LookaheadPipelineStatus.THROTTLED) {
                        _state.value = snapshot.copy(status = LookaheadPipelineStatus.THROTTLED)
                    }
                    delay(LOOP_DELAY_MS)
                    continue
                }

                if (!sawInputEos) {
                    val inputBufferIndex = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                sawInputEos = true
                            } else {
                                codec.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime,
                                    extractor.sampleFlags,
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                var drainMore = true
                while (drainMore && currentGenerationId == generationId) {
                    when (val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_US)) {
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            drainMore = false
                        }

                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val outputFormat = codec.outputFormat
                            outputSampleRate = outputFormat.getIntegerOrDefault(MediaFormat.KEY_SAMPLE_RATE, outputSampleRate)
                            outputChannelCount = outputFormat.getIntegerOrDefault(MediaFormat.KEY_CHANNEL_COUNT, outputChannelCount)
                        }

                        else -> {
                            if (outputBufferIndex >= 0) {
                                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                                if (outputBuffer != null && bufferInfo.size > 0) {
                                    val processed = processOutputBuffer(
                                        outputBuffer = outputBuffer,
                                        bufferInfo = bufferInfo,
                                        sampleRate = outputSampleRate,
                                        channelCount = outputChannelCount,
                                        trimBeforeMs = trimBeforeMs,
                                    )
                                    trimBeforeMs = processed.trimBeforeMs
                                    if (processed.chunk != null && processed.data != null) {
                                        audioBatcher.feedAudio(
                                            processed.data,
                                            outputSampleRate,
                                            outputChannelCount,
                                        )
                                        updateChunkState(processed.chunk)
                                    }
                                }

                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    sawOutputEos = true
                                }
                                codec.releaseOutputBuffer(outputBufferIndex, false)
                            }
                        }
                    }
                }

                val updatedLead = _state.value.bufferedUntilMs - _state.value.playbackPositionMs
                _state.value = _state.value.copy(
                    status = when {
                        sawOutputEos -> LookaheadPipelineStatus.READY
                        updatedLead >= _state.value.targetLeadMs -> LookaheadPipelineStatus.READY
                        else -> LookaheadPipelineStatus.WARMING
                    },
                )
            }
        } finally {
            codec?.stopSafely()
            codec?.releaseSafely()
            extractor.release()
        }
    }

    private fun processOutputBuffer(
        outputBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
        sampleRate: Int,
        channelCount: Int,
        trimBeforeMs: Long?,
    ): ProcessedOutput {
        val startMs = bufferInfo.presentationTimeUs / 1000
        val bytesPerFrame = channelCount * BYTES_PER_SAMPLE
        if (bytesPerFrame <= 0 || bufferInfo.size <= 0) {
            return ProcessedOutput(trimBeforeMs = trimBeforeMs)
        }

        val totalFrames = bufferInfo.size / bytesPerFrame
        val durationMs = if (sampleRate > 0) {
            (totalFrames * 1000L) / sampleRate
        } else {
            0L
        }
        val endMs = startMs + durationMs

        if (trimBeforeMs != null && endMs <= trimBeforeMs) {
            return ProcessedOutput(trimBeforeMs = trimBeforeMs)
        }

        val duplicate = outputBuffer.duplicate().apply {
            position(bufferInfo.offset)
            limit(bufferInfo.offset + bufferInfo.size)
        }

        var adjustedStartMs = startMs
        var adjustedData = duplicate.slice()

        if (trimBeforeMs != null && trimBeforeMs > startMs) {
            val trimDurationMs = trimBeforeMs - startMs
            val trimFrames = ((trimDurationMs * sampleRate) / 1000L).toInt().coerceAtMost(totalFrames)
            val trimBytes = trimFrames * bytesPerFrame
            adjustedData.position(trimBytes.coerceAtMost(adjustedData.limit()))
            adjustedData = adjustedData.slice()
            adjustedStartMs = trimBeforeMs
        }

        if (!adjustedData.hasRemaining()) {
            return ProcessedOutput(trimBeforeMs = null)
        }

        val adjustedFrames = adjustedData.remaining() / bytesPerFrame
        val adjustedEndMs = adjustedStartMs + ((adjustedFrames * 1000L) / sampleRate)
        val chunk = LookaheadAudioChunk(
            sequenceId = ++nextSequenceId,
            generationId = currentGenerationId,
            mediaStartMs = adjustedStartMs,
            mediaEndMs = adjustedEndMs,
            sampleRate = sampleRate,
            channelCount = channelCount,
            pcmSizeBytes = adjustedData.remaining(),
        )
        return ProcessedOutput(
            chunk = chunk,
            data = adjustedData,
            trimBeforeMs = null,
        )
    }

    private fun updateChunkState(chunk: LookaheadAudioChunk) {
        _state.value = _state.value.copy(
            lookaheadCursorMs = chunk.mediaEndMs,
            bufferedUntilMs = maxOf(_state.value.bufferedUntilMs, chunk.mediaEndMs),
            status = if (chunk.mediaEndMs - _state.value.playbackPositionMs >= _state.value.targetLeadMs) {
                LookaheadPipelineStatus.READY
            } else {
                LookaheadPipelineStatus.WARMING
            },
        )
    }

    private fun MediaExtractor.setMediaDataSource(mediaId: String) {
        val uri = Uri.parse(mediaId)
        if (uri.scheme.isNullOrBlank()) {
            setDataSource(mediaId)
        } else {
            setDataSource(context, uri, emptyMap())
        }
    }

    private fun MediaExtractor.findAudioTrackIndex(audioTrackKey: String?): Int {
        val explicitTrackIndex = audioTrackKey?.toIntOrNull()
        if (explicitTrackIndex != null && explicitTrackIndex in 0 until trackCount) {
            val explicitFormat = getTrackFormat(explicitTrackIndex)
            if (explicitFormat.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                return explicitTrackIndex
            }
        }

        for (index in 0 until trackCount) {
            val format = getTrackFormat(index)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                return index
            }
        }
        return -1
    }

    private fun MediaFormat.getIntegerOrDefault(key: String, defaultValue: Int): Int {
        return if (containsKey(key)) getInteger(key) else defaultValue
    }

    private fun MediaCodec.stopSafely() {
        runCatching { stop() }
    }

    private fun MediaCodec.releaseSafely() {
        runCatching { release() }
    }

    private data class ProcessedOutput(
        val chunk: LookaheadAudioChunk? = null,
        val data: ByteBuffer? = null,
        val trimBeforeMs: Long? = null,
    )
}
