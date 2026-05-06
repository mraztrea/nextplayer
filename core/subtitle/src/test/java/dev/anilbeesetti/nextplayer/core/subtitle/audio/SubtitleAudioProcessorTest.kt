package dev.anilbeesetti.nextplayer.core.subtitle.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleAudioProcessorTest {

    @Test
    fun `queueInput copies pass through output instead of aliasing caller buffer`() {
        val processor = SubtitleAudioProcessor(AudioBatcher())
        SubtitleAudioProcessor::class.java.getDeclaredField("isActive").apply {
            isAccessible = true
            setBoolean(processor, true)
        }

        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        input.put(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        input.flip()

        processor.queueInput(input)

        input.position(0)
        input.put(byteArrayOf(0x55, 0x66, 0x77, 0x00))
        input.flip()

        val output = processor.getOutput()
        val actual = ByteArray(output.remaining())
        output.get(actual)

        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), actual)
        assertTrue(output.isDirect)
    }
}
