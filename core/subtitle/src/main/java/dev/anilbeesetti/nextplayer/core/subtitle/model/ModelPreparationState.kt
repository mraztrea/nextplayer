package dev.anilbeesetti.nextplayer.core.subtitle.model

data class ModelPreparationState(
    val taskId: String = "",
    val type: ModelPreparationType? = null,
    val source: String = "",
    val requiresWifiByDefault: Boolean = true,
    val userConfirmedMobileData: Boolean = false,
    val progressPercent: Int = 0,
    val bytesCompleted: Long = 0L,
    val totalBytes: Long = 0L,
    val state: ModelPreparationTaskState = ModelPreparationTaskState.IDLE,
    val errorMessage: String? = null,
)

enum class ModelPreparationType {
    DOWNLOAD,
    IMPORT,
}

enum class ModelPreparationTaskState {
    IDLE,
    PENDING_CONFIRMATION,
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED,
}
