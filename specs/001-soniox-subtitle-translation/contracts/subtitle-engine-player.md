# Contract: Subtitle Engine ↔ Player UI

**Branch**: `001-soniox-subtitle-translation` | **Date**: 2026-05-05

## Callback Interface

```kotlin
interface SubtitleEngineCallback {
    fun onOriginal(text: String, speaker: String?, language: String?)
    fun onTranslation(text: String)
    fun onProvisional(text: String, speaker: String?, language: String?)
    fun onConfidence(avgConfidence: Float)
    fun onStatusChange(status: SubtitleEngineStatus)
    fun onError(message: String, isRecoverable: Boolean)
}
```

## Engine Status

```kotlin
enum class SubtitleEngineStatus {
    IDLE,           // Chưa khởi động
    CONNECTING,     // Đang kết nối WebSocket
    ACTIVE,         // Đang nhận và xử lý audio
    RECONNECTING,   // Mất kết nối, đang kết nối lại
    ERROR,          // Lỗi không phục hồi được
    STOPPED         // Đã dừng bởi người dùng
}
```

## Engine Control Interface

```kotlin
interface SubtitleEngine {
    fun start(config: SonioxSessionConfig)
    fun stop()
    fun feedAudio(pcmData: ByteArray)
    fun resetSession()
    val status: StateFlow<SubtitleEngineStatus>
}
```

## Audio Processor → Engine Contract

```kotlin
interface SubtitleAudioProcessor {
    /**
     * Nhận PCM data từ ExoPlayer AudioProcessor pipeline.
     * Format: 16kHz mono s16le.
     * Được gọi liên tục khi video đang phát.
     * Tự batch thành ~200ms trước khi gửi engine.
     */
    fun onAudioData(buffer: ByteBuffer, sampleRate: Int, channelCount: Int)
    
    fun onPlaybackPositionChanged(positionMs: Long)
    fun onPlaybackStateChanged(isPlaying: Boolean)
}
```
