# Contract: Player ↔ Subtitle Engine Sync

**Branch**: `002-lookahead-subtitle-latency` | **Date**: 2026-05-05

## Trách nhiệm của PlayerService

`PlayerService` là nguồn sự thật cho playback runtime và phải gửi các tín hiệu sau vào subtitle engine:

```kotlin
interface SubtitlePlaybackSync {
    fun onPlaybackPosition(positionMs: Long)
    fun onPlayWhenReadyChanged(isPlaying: Boolean)
    fun onSeek(newPositionMs: Long)
    fun onMediaItemChanged(mediaId: String)
    fun onAudioTrackChanged(trackKey: String?)
    fun onStop()
}
```

## Trách nhiệm của LookaheadAudioPipeline

```kotlin
interface LookaheadAudioPipeline {
    fun start(mediaId: String, initialPositionMs: Long, audioTrackKey: String?)
    fun pause()
    fun resume(playbackPositionMs: Long)
    fun seekTo(positionMs: Long, audioTrackKey: String?)
    fun stop()
}
```

### Behavioral rules

- `start()` khởi tạo generation mới và nạp lead tới high-water mark.
- `seekTo()` phải bỏ mọi chunk chưa xử lý của generation cũ.
- `pause()` ngừng kéo thêm audio tương lai.
- `resume()` tính lại lead từ `playbackPositionMs` hiện tại.

## Trách nhiệm của SubtitleEngine

```kotlin
interface TimedSubtitleEngine {
    val visibleSegments: StateFlow<List<SubtitleSegment>>
    val provisionalText: StateFlow<String>
    val lookaheadState: StateFlow<LookaheadSessionState>

    suspend fun start()
    fun stop()
    fun onPlaybackPosition(positionMs: Long)
    fun onSeek(positionMs: Long)
    fun onAudioTrackChanged(trackKey: String?)
}
```

## Quy tắc hiển thị

- `visibleSegments` chỉ chứa segment mà:
  - `segment.generationId == currentGenerationId`
  - `currentPositionMs >= segment.targetStartMs`
  - `currentPositionMs <= segment.targetEndMs`
- Segment chưa tới lượt không được trả ra UI.
- Segment đã hết cửa sổ hoặc generation đổi phải bị loại khỏi `visibleSegments`.

## Quy tắc reset

- Seek, media item transition, audio track switch:
  - tăng `generationId`
  - clear `visibleSegments`
  - clear pending translation queue
  - restart lookahead từ vị trí mới
  - rotate hoặc reconnect phiên Soniox nếu cần

## UI contract

`PlayerViewModel` và `SubtitleOverlay` không tự suy luận timing từ raw segment list. Chúng chỉ render state đã được engine lọc theo media-time.
