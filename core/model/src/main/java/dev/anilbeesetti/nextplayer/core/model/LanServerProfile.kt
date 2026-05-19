package dev.anilbeesetti.nextplayer.core.model

data class LanServerProfile(
    val id: String,
    val displayName: String,
    val host: String,
    val shareName: String,
    val initialPath: String = "",
    val username: String = "",
    val credentialKey: String? = null,
    val hasPassword: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val lastConnectedAt: Long? = null,
)
