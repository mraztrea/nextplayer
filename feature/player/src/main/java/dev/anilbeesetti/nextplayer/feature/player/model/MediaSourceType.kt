package dev.anilbeesetti.nextplayer.feature.player.model

import android.net.Uri

/**
 * Xác định loại nguồn media để áp dụng cấu hình buffer và wake mode phù hợp.
 */
enum class MediaSourceType {
    /** File nội bộ (content://, file://) */
    LOCAL,

    /** Stream qua mạng (http://, https://, smb://, ftp://, rtsp://) */
    NETWORK;

    companion object {
        fun fromUri(uri: Uri): MediaSourceType {
            return when (uri.scheme?.lowercase()) {
                "content", "file", null -> LOCAL
                else -> NETWORK
            }
        }
    }
}
