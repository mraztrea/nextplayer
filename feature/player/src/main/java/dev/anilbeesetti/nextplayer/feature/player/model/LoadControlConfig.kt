package dev.anilbeesetti.nextplayer.feature.player.model

import androidx.media3.exoplayer.DefaultLoadControl

/**
 * Cấu hình buffer parameters cho ExoPlayer LoadControl,
 * tối ưu theo loại nguồn media (local vs network).
 */
data class LoadControlConfig(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int,
) {
    fun toLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBufferMs,
                maxBufferMs,
                bufferForPlaybackMs,
                bufferForPlaybackAfterRebufferMs,
            )
            .build()
    }

    companion object {
        /** Buffer tối thiểu cho local playback — khởi động nhanh */
        fun forLocal() = LoadControlConfig(
            minBufferMs = 15_000,
            maxBufferMs = 50_000,
            bufferForPlaybackMs = 500,
            bufferForPlaybackAfterRebufferMs = 1_000,
        )

        /** Buffer chuẩn cho network streaming */
        fun forNetwork() = LoadControlConfig(
            minBufferMs = 50_000,
            maxBufferMs = 50_000,
            bufferForPlaybackMs = 2_500,
            bufferForPlaybackAfterRebufferMs = 5_000,
        )

        fun forSourceType(sourceType: MediaSourceType) = when (sourceType) {
            MediaSourceType.LOCAL -> forLocal()
            MediaSourceType.NETWORK -> forNetwork()
        }
    }
}
