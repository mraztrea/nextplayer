package dev.anilbeesetti.nextplayer.feature.player.state

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.feature.player.extensions.formatted
import dev.anilbeesetti.nextplayer.feature.player.extensions.seekToWithDirection
import dev.anilbeesetti.nextplayer.feature.player.extensions.setIsScrubbingModeEnabled
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@Composable
fun rememberSeekGestureState(
    player: Player,
    sensitivity: Float = 0.5f,
    enableSeekGesture: Boolean,
): SeekGestureState {
    val seekGestureState = remember {
        SeekGestureState(
            player = player,
            sensitivity = sensitivity,
            enableSeekGesture = enableSeekGesture,
        )
    }
    return seekGestureState
}

@Stable
class SeekGestureState(
    private val player: Player,
    private val enableSeekGesture: Boolean = true,
    private val sensitivity: Float = 0.5f,
) {
    var isSeeking: Boolean by mutableStateOf(false)
        private set

    var seekStartPosition: Long? by mutableStateOf(null)
        private set

    var seekAmount: Long? by mutableStateOf(null)
        private set

    private var seekStartX = 0f
    private var lastSeekTime = 0L
    private var lastSeekDirection: Boolean? = null
    private var previousOnSeekValue: Long? = null

    companion object {
        private const val SEEK_THROTTLE_MS = 150L
    }

    fun onSeek(value: Long) {
        if (!isSeeking) {
            isSeeking = true
            seekStartPosition = player.currentPosition
            previousOnSeekValue = seekStartPosition
            lastSeekTime = 0L
            lastSeekDirection = null
            player.setIsScrubbingModeEnabled(true)
        }

        seekAmount = (value - seekStartPosition!!).coerceIn(
            minimumValue = 0 - seekStartPosition!!,
            maximumValue = player.duration - seekStartPosition!!,
        )

        val isForward = value >= (previousOnSeekValue ?: seekStartPosition!!)
        val directionChanged = isForward != lastSeekDirection
        lastSeekDirection = isForward
        previousOnSeekValue = value

        val now = SystemClock.elapsedRealtime()
        if (directionChanged || (now - lastSeekTime) >= SEEK_THROTTLE_MS) {
            player.seekToWithDirection(
                positionMs = value.coerceIn(0L, player.duration),
                isForward = isForward,
            )
            lastSeekTime = now
        }
    }

    fun onSeekEnd() {
        reset()
    }

    fun onDragStart(offset: Offset) {
        if (!enableSeekGesture) return
        if (player.currentPosition == C.TIME_UNSET) return
        if (player.duration == C.TIME_UNSET) return
        if (!player.isCurrentMediaItemSeekable) return

        isSeeking = true
        seekStartX = offset.x
        seekStartPosition = player.currentPosition
        lastSeekTime = 0L
        lastSeekDirection = null

        player.setIsScrubbingModeEnabled(true)
    }

    @OptIn(UnstableApi::class)
    fun onDrag(change: PointerInputChange, dragAmount: Float) {
        if (seekStartPosition == null) return
        if (player.duration == C.TIME_UNSET) return
        if (!player.isCurrentMediaItemSeekable) return
        if (player.currentPosition <= 0L && dragAmount < 0) return
        if (player.currentPosition >= player.duration && dragAmount > 0) return
        if (change.isConsumed) return

        val newPosition = seekStartPosition!! + ((change.position.x - seekStartX) * (sensitivity * 100)).toInt()
        seekAmount = (newPosition - seekStartPosition!!).coerceIn(
            minimumValue = 0 - seekStartPosition!!,
            maximumValue = player.duration - seekStartPosition!!,
        )

        val isForward = dragAmount > 0
        val directionChanged = isForward != lastSeekDirection
        lastSeekDirection = isForward

        val now = SystemClock.elapsedRealtime()
        if (directionChanged || (now - lastSeekTime) >= SEEK_THROTTLE_MS) {
            player.seekToWithDirection(
                positionMs = newPosition.coerceIn(0L, player.duration),
                isForward = isForward,
            )
            lastSeekTime = now
        }
    }

    fun onDragEnd() {
        reset()
    }

    private fun reset() {
        player.setIsScrubbingModeEnabled(false)
        isSeeking = false
        seekStartPosition = null
        seekAmount = null
        seekStartX = 0f
        lastSeekTime = 0L
        lastSeekDirection = null
        previousOnSeekValue = null
    }
}

val SeekGestureState.seekAmountFormatted: String
    get() {
        val seekAmount = seekAmount ?: return ""
        val sign = if (seekAmount < 0) "-" else "+"
        return sign + abs(seekAmount).milliseconds.formatted()
    }

val SeekGestureState.seekToPositionFormated: String
    get() {
        val position = seekStartPosition ?: return ""
        val seekAmount = seekAmount ?: return ""
        return (position + seekAmount).milliseconds.formatted()
    }
