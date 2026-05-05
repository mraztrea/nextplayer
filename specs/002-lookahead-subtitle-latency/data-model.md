# Data Model: Tối ưu Độ trễ Phụ đề Dịch với Lookahead Audio

**Branch**: `002-lookahead-subtitle-latency` | **Date**: 2026-05-05

## Entities

### SubtitleSegment

Đơn vị phụ đề đã chốt, mang cả nội dung lẫn media-time dùng cho render.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | ID nội bộ tăng dần |
| `originalText` | `String` | Văn bản gốc đã chốt |
| `translationText` | `String?` | Bản dịch đã ghép cặp, null nếu chưa tới |
| `status` | `SegmentStatus` | `ORIGINAL` hoặc `TRANSLATED` |
| `speaker` | `String?` | Speaker label nếu có |
| `language` | `String?` | Ngôn ngữ gốc nếu có |
| `confidence` | `Float?` | Điểm tin cậy trung bình của token gốc |
| `sourceStartMs` | `Long` | Mốc bắt đầu theo timeline audio từ Soniox |
| `sourceEndMs` | `Long` | Mốc kết thúc theo timeline audio từ Soniox |
| `targetStartMs` | `Long` | Mốc bắt đầu dùng để render trên player |
| `targetEndMs` | `Long` | Mốc kết thúc dùng để render trên player |
| `generationId` | `Long` | Phiên lookahead/playback mà segment này thuộc về |
| `createdAt` | `Long` | Timestamp cục bộ để debug/metric |

**Validation rules**:
- `sourceEndMs >= sourceStartMs`
- `targetEndMs >= targetStartMs`
- `generationId` phải khớp với generation hiện tại trước khi render
- `translationText` chỉ được gán cho segment cùng generation

**State transitions**:
```text
[original final from Soniox] -> status = ORIGINAL
[matched translation final]  -> status = TRANSLATED
[player currentPosition < targetStartMs] -> buffered, chưa render
[targetStartMs <= currentPosition <= targetEndMs] -> visible
[currentPosition > targetEndMs hoặc generation đổi] -> evicted
```

### LookaheadAudioChunk

Đơn vị PCM đã giải mã xong từ pipeline lookahead, sẵn sàng gửi sang Soniox.

| Field | Type | Description |
|-------|------|-------------|
| `sequenceId` | `Long` | Số thứ tự tăng dần |
| `generationId` | `Long` | Gắn với seek/media session hiện tại |
| `mediaStartMs` | `Long` | Mốc bắt đầu của chunk trên timeline media |
| `mediaEndMs` | `Long` | Mốc kết thúc của chunk trên timeline media |
| `sampleRate` | `Int` | Kỳ vọng 16000 |
| `channelCount` | `Int` | Kỳ vọng 1 |
| `pcmSizeBytes` | `Int` | Kích thước payload để metric/debug |

**Validation rules**:
- `mediaEndMs > mediaStartMs`
- chunk chỉ được gửi nếu `generationId` còn active
- queue chunk bị giới hạn theo watermarks

### LookaheadSessionState

Trạng thái runtime của pipeline chuẩn bị audio trước.

| Field | Type | Description |
|-------|------|-------------|
| `generationId` | `Long` | Phiên hiện tại |
| `playbackPositionMs` | `Long` | Vị trí player mới nhất |
| `lookaheadCursorMs` | `Long` | Điểm cuối cùng pipeline đã giải mã tới |
| `bufferedUntilMs` | `Long` | Mốc media-time đã chuẩn bị sẵn |
| `targetLeadMs` | `Long` | Lead mục tiêu, mặc định 5000 |
| `lowWaterMs` | `Long` | Ngưỡng nạp lại |
| `highWaterMs` | `Long` | Ngưỡng dừng nạp |
| `isPaused` | `Boolean` | Pause playback hiện tại |
| `isFallbackActive` | `Boolean` | Có đang dùng đường suy giảm an toàn không |
| `activeAudioTrackKey` | `String?` | Khóa track audio đang được bám theo |

**State transitions**:
```text
IDLE -> WARMING -> READY -> THROTTLED
SEEK/TRACK_CHANGE -> RESETTING -> WARMING
LOOKAHEAD_FAILURE -> FALLBACK
STOP -> IDLE
```

### PendingOriginalWindow

Đơn vị chờ ghép bản dịch với timing gốc đã biết.

| Field | Type | Description |
|-------|------|-------------|
| `segmentId` | `Long` | Tham chiếu `SubtitleSegment` |
| `generationId` | `Long` | Generation sở hữu |
| `sourceStartMs` | `Long` | Start time của original |
| `sourceEndMs` | `Long` | End time của original |
| `translationDeadlineMs` | `Long` | Mốc cleanup nếu translation không tới |

**Purpose**:
- Cho phép token translation quay về muộn nhưng vẫn gắn đúng timing window của original segment.

## Enums

### SegmentStatus
```kotlin
enum class SegmentStatus {
    ORIGINAL,
    TRANSLATED,
}
```

### LookaheadPipelineStatus
```kotlin
enum class LookaheadPipelineStatus {
    IDLE,
    WARMING,
    READY,
    THROTTLED,
    RESETTING,
    FALLBACK,
    ERROR,
}
```

## Relationships

```text
LookaheadSessionState ──1:N──> LookaheadAudioChunk
LookaheadSessionState ──1:N──> SubtitleSegment
SubtitleSegment ──0..1──> PendingOriginalWindow
PendingOriginalWindow ──1:1──> Translation final token mapping
```

## Data Flow

```text
PlayerService
    ↓ currentPosition / seek / track changes
LookaheadAudioPipeline
    ↓ LookaheadAudioChunk(mediaStartMs..mediaEndMs)
SonioxWebSocketClient
    ↓ tokens(original with start_ms/end_ms, translation without timing)
SonioxTokenParser
    ↓ original final + translation final + provisional
SubtitleSessionManager
    ↓ timed buffer + visible subtitle state
PlayerViewModel
    ↓ active render state
SubtitleOverlay
```
