package dev.anilbeesetti.nextplayer.core.model

data class PlaybackQueueSnapshot(
    val currentUriString: String,
    val currentIndex: Int,
    val entries: List<SiblingVideoEntry>,
    val sourceType: PlaybackSourceType,
) {
    val uriStrings: List<String> = entries.map { it.uriString }
}
