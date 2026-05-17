package dev.anilbeesetti.nextplayer.feature.player.model

import dev.anilbeesetti.nextplayer.core.model.PlaybackQueueSnapshot

data class QueueHydrationState(
    val currentUriString: String? = null,
    val status: QueueHydrationStatus = QueueHydrationStatus.IDLE,
    val snapshot: PlaybackQueueSnapshot? = null,
)
