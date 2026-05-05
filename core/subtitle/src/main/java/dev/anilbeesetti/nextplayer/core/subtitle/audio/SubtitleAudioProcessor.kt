package dev.anilbeesetti.nextplayer.core.subtitle.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import dev.anilbeesetti.nextplayer.core.common.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExoPlayer AudioProcessor that taps into the audio pipeline
 * and forwards PCM data to the AudioBatcher for subtitle processing.
 * The audio passes through unmodified — this is a read-only tap.
 */
@Singleton
class SubtitleAudioProcessor @Inject constructor(
    private val audioBatcher: AudioBatcher,
) : AudioProcessor {

    companion object {
        private const val TAG = "SubtitleAudioProcessor"
    }

    private var inputAudioFormat = AudioFormat.NOT_SET
    private var isActive = false
    private var inputBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    private var isEnabled = false

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            audioBatcher.reset()
        }
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        // Accept PCM 16-bit input
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        isActive = true
        Logger.logDebug(
            TAG,
            "Configured: sampleRate=${inputAudioFormat.sampleRate}, " +
                "channels=${inputAudioFormat.channelCount}",
        )
        return inputAudioFormat // Pass through unchanged
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive || inputBuffer.remaining() == 0) return

        // Tap: copy data to batcher if enabled
        if (isEnabled) {
            val duplicate = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            audioBatcher.feedAudio(
                duplicate,
                inputAudioFormat.sampleRate,
                inputAudioFormat.channelCount,
            )
        }

        // Pass through: set output = input (no modification)
        this.outputBuffer = inputBuffer
        this.inputBuffer = AudioProcessor.EMPTY_BUFFER
    }

    override fun queueEndOfStream() {
        inputEnded = true
        if (isEnabled) {
            audioBatcher.flush()
        }
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }

    override fun reset() {
        flush()
        isActive = false
        inputAudioFormat = AudioFormat.NOT_SET
        audioBatcher.reset()
    }
}
