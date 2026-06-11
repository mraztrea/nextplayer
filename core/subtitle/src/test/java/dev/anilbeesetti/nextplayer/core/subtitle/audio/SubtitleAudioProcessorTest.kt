package dev.anilbeesetti.nextplayer.core.subtitle.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleAudioProcessorTest {
    @Test
    fun outputKeepsOriginalPcmWhenInputBufferIsReused() {
        val processor = SubtitleAudioProcessor(AudioBatcher())
        processor.setPrivateField("inputAudioFormat", AudioFormat(SAMPLE_RATE, CHANNEL_COUNT, C.ENCODING_PCM_16BIT))
        processor.setPrivateField("isActive", true)

        val input = pcmBuffer(byteCount = 4, sampleValue = 1)
        processor.queueInput(input)
        input.clear()
        input.putShort(9)
        input.putShort(9)

        val output = processor.getOutput().order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(1, output.short.toInt())
        assertEquals(1, output.short.toInt())
        assertTrue(processor.getOutput() === AudioProcessor.EMPTY_BUFFER)
    }

    @Test
    fun flushDropsPartialSubtitleBatchAfterSeek() {
        val audioBatcher = AudioBatcher()
        val processor = SubtitleAudioProcessor(audioBatcher)
        val batches = mutableListOf<ByteArray>()
        audioBatcher.setListener(
            object : AudioBatcher.Listener {
                override fun onAudioBatchReady(pcmData: ByteArray) {
                    batches += pcmData
                }
            },
        )

        processor.setPrivateField("inputAudioFormat", AudioFormat(SAMPLE_RATE, CHANNEL_COUNT, C.ENCODING_PCM_16BIT))
        processor.setPrivateField("isActive", true)
        processor.setEnabled(true)

        processor.queueInput(pcmBuffer(byteCount = PRE_SEEK_BYTES, sampleValue = 1))
        processor.flush()
        processor.queueInput(pcmBuffer(byteCount = POST_SEEK_BYTES_BELOW_BATCH, sampleValue = 2))

        assertTrue(batches.isEmpty())
    }

    @Test
    fun flushDropsPartialGeminiBatchAfterSeek() {
        val audioBatcher = AudioBatcher()
        audioBatcher.setBatchDurationMs(GEMINI_BATCH_DURATION_MS)
        val processor = SubtitleAudioProcessor(audioBatcher)
        val batches = mutableListOf<ByteArray>()
        audioBatcher.setListener(
            object : AudioBatcher.Listener {
                override fun onAudioBatchReady(pcmData: ByteArray) {
                    batches += pcmData
                }
            },
        )

        processor.setPrivateField("inputAudioFormat", AudioFormat(SAMPLE_RATE, CHANNEL_COUNT, C.ENCODING_PCM_16BIT))
        processor.setPrivateField("isActive", true)
        processor.setEnabled(true)

        processor.queueInput(pcmBuffer(byteCount = PRE_SEEK_BYTES, sampleValue = 1))
        processor.flush()
        processor.queueInput(pcmBuffer(byteCount = POST_SEEK_BYTES_BELOW_GEMINI_BATCH, sampleValue = 2))

        assertTrue(batches.isEmpty())
    }

    private fun SubtitleAudioProcessor.setPrivateField(name: String, value: Any) {
        SubtitleAudioProcessor::class.java.getDeclaredField(name).apply {
            isAccessible = true
            set(this@setPrivateField, value)
        }
    }

    private fun pcmBuffer(byteCount: Int, sampleValue: Short): ByteBuffer {
        val buffer = ByteBuffer.allocate(byteCount).order(ByteOrder.LITTLE_ENDIAN)
        repeat(byteCount / BYTES_PER_SAMPLE) {
            buffer.putShort(sampleValue)
        }
        buffer.flip()
        return buffer
    }

    private companion object {
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_COUNT = 1
        private const val BYTES_PER_SAMPLE = 2
        private const val PRE_SEEK_BYTES = 200
        private const val POST_SEEK_BYTES_BELOW_BATCH = 6_200
        private const val GEMINI_BATCH_DURATION_MS = 100
        private const val POST_SEEK_BYTES_BELOW_GEMINI_BATCH = 3_000
    }
}
