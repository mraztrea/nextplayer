package dev.anilbeesetti.nextplayer.core.model

data class PlaybackLaunchContext(
    val currentUriString: String,
    val siblings: List<SiblingVideoEntry>,
    val sourceType: PlaybackSourceType,
)
