package dev.anilbeesetti.nextplayer.core.subtitle.audio

import dev.anilbeesetti.nextplayer.core.common.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioBatcher @Inject constructor() {

    companion object {
        private const val TAG = "AudioBatcher"
        private const val TARGET_SAMPLE_RATE = 16000
        private const val TARGET_CHANNELS = 1
        private const val BYTES_PER_SAMPLE = 2 // s16le
        const val DEFAULT_BATCH_DURATION_MS = 200
        private const val MAX_BATCH_DURATION_MS = DEFAULT_BATCH_DURATION_MS
        private const val MAX_BATCH_SIZE_BYTES = TARGET_SAMPLE_RATE * BYTES_PER_SAMPLE * MAX_BATCH_DURATION_MS / 1000
    }

    interface Listener {
        fun onAudioBatchReady(pcmData: ByteArray)
    }

    private var listener: Listener? = null
    private var batchSizeBytes = batchSizeBytesFor(DEFAULT_BATCH_DURATION_MS)
    private val buffer = ByteBuffer.allocate(MAX_BATCH_SIZE_BYTES * 2).order(ByteOrder.LITTLE_ENDIAN)

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun setBatchDurationMs(durationMs: Int) {
        synchronized(buffer) {
            flush()
            val boundedDurationMs = durationMs.coerceIn(1, MAX_BATCH_DURATION_MS)
            batchSizeBytes = batchSizeBytesFor(boundedDurationMs)
        }
    }

    /**
     * Feed PCM audio data. Automatically resamples if needed and batches to ~200ms.
     */
    fun feedAudio(data: ByteBuffer, sampleRate: Int, channelCount: Int) {
        val processedData = processAudio(data, sampleRate, channelCount)
        if (processedData.isEmpty()) return

        synchronized(buffer) {
            var offset = 0
            while (offset < processedData.size) {
                val remaining = buffer.remaining()
                val toCopy = minOf(remaining, processedData.size - offset)
                buffer.put(processedData, offset, toCopy)
                offset += toCopy

                if (buffer.position() >= batchSizeBytes) {
                    flushBatch()
                }
            }
        }
    }

    fun flush() {
        synchronized(buffer) {
            if (buffer.position() > 0) {
                flushBatch()
            }
        }
    }

    fun reset() {
        synchronized(buffer) {
            buffer.clear()
        }
    }

    private fun flushBatch() {
        val size = buffer.position()
        val batch = ByteArray(size)
        buffer.flip()
        buffer.get(batch)
        buffer.clear()
        listener?.onAudioBatchReady(batch)
    }

    private fun processAudio(data: ByteBuffer, sampleRate: Int, channelCount: Int): ByteArray {
        // Read input samples
        val inputData = data.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val inputSampleCount = inputData.remaining() / (BYTES_PER_SAMPLE * channelCount)
        if (inputSampleCount == 0) return ByteArray(0)

        val inputSamples = ShortArray(inputSampleCount * channelCount)
        for (i in inputSamples.indices) {
            inputSamples[i] = inputData.short
        }

        // Step 1: Convert stereo to mono if needed
        val monoSamples = if (channelCount > 1) {
            ShortArray(inputSampleCount) { i ->
                var sum = 0L
                for (ch in 0 until channelCount) {
                    sum += inputSamples[i * channelCount + ch]
                }
                (sum / channelCount).toInt().toShort()
            }
        } else {
            inputSamples
        }

        // Step 2: Resample if needed (simple linear interpolation)
        val outputSamples = if (sampleRate != TARGET_SAMPLE_RATE) {
            resample(monoSamples, sampleRate, TARGET_SAMPLE_RATE)
        } else {
            monoSamples
        }

        // Step 3: Convert to s16le bytes
        val outputBuffer = ByteBuffer.allocate(outputSamples.size * BYTES_PER_SAMPLE)
            .order(ByteOrder.LITTLE_ENDIAN)
        for (sample in outputSamples) {
            outputBuffer.putShort(sample)
        }
        return outputBuffer.array()
    }

    private fun resample(input: ShortArray, fromRate: Int, toRate: Int): ShortArray {
        val ratio = fromRate.toDouble() / toRate.toDouble()
        val outputLength = (input.size / ratio).toInt()
        val output = ShortArray(outputLength)

        for (i in output.indices) {
            val srcPos = i * ratio
            val srcIndex = srcPos.toInt()
            val fraction = srcPos - srcIndex

            val sample1 = input[srcIndex]
            val sample2 = if (srcIndex + 1 < input.size) input[srcIndex + 1] else sample1
            output[i] = (sample1 + fraction * (sample2 - sample1)).toInt().toShort()
        }

        return output
    }

    private fun batchSizeBytesFor(durationMs: Int): Int {
        return TARGET_SAMPLE_RATE * BYTES_PER_SAMPLE * durationMs / 1000
    }
}
