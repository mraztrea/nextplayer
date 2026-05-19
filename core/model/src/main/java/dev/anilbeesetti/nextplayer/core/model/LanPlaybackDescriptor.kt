package dev.anilbeesetti.nextplayer.core.model

data class LanPlaybackDescriptor(
    val serverId: String,
    val mediaPath: String,
    val displayName: String,
    val sizeBytes: Long? = null,
    val modifiedAt: Long? = null,
) {
    val uriString: String = "$SCHEME://$serverId/${mediaPath.trimStart('/')}"

    companion object {
        const val SCHEME = "nextplayer-smb"

        fun parse(uriString: String): LanPlaybackDescriptor? {
            val prefix = "$SCHEME://"
            if (!uriString.startsWith(prefix)) return null
            val value = uriString.removePrefix(prefix)
            val serverId = value.substringBefore("/", missingDelimiterValue = "")
            val mediaPath = value.substringAfter("/", missingDelimiterValue = "")
            if (serverId.isBlank() || mediaPath.isBlank()) return null
            return LanPlaybackDescriptor(
                serverId = serverId,
                mediaPath = mediaPath,
                displayName = mediaPath.substringAfterLast('/'),
            )
        }
    }
}
