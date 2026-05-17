# Data Model: Tối ưu phát video LAN và điều hướng cùng thư mục

**Branch**: `003-lan-playback-optimization` | **Date**: 2026-05-17

## Entities

### PlaybackLaunchContext

Ngữ cảnh launch được tạo tại nguồn mở video và truyền sang player để mô tả mức độ “giàu thông tin” của phiên phát.

| Field | Type | Description |
|-------|------|-------------|
| `currentUri` | `String` | URI của video đang mở |
| `sourceType` | `PlaybackSourceType` | `EXTERNAL_PLAYLIST`, `SOURCE_FOLDER`, `SINGLE_URI` |
| `explicitPlaylistUris` | `List<String>` | Playlist caller ngoài truyền vào nếu có |
| `visibleSiblingUris` | `List<String>` | Danh sách anh em theo đúng thứ tự người dùng vừa thấy ở nguồn mở |
| `currentVisibleIndex` | `Int?` | Vị trí item hiện tại trong `visibleSiblingUris` |
| `folderKey` | `String?` | Khóa thư mục hiện tại; local có thể là path, LAN có thể là khóa opaque |
| `canResolveSiblings` | `Boolean` | Nguồn có đủ ngữ cảnh để Next/Prev theo thư mục hay không |

**Validation rules**:
- `currentUri` luôn bắt buộc.
- Nếu `explicitPlaylistUris` không rỗng, `sourceType` phải là `EXTERNAL_PLAYLIST`.
- Nếu `visibleSiblingUris` có dữ liệu, `currentVisibleIndex` phải nằm trong biên và `visibleSiblingUris[currentVisibleIndex]` phải khớp `currentUri`.
- `folderKey` chỉ cần khi `sourceType == SOURCE_FOLDER`.

### SiblingVideoEntry

Đơn vị đại diện cho một video hợp lệ trong cùng thư mục mà player có thể điều hướng tới.

| Field | Type | Description |
|-------|------|-------------|
| `uri` | `String` | URI dùng để phát |
| `displayName` | `String?` | Tên hiển thị nếu nguồn có |
| `orderIndex` | `Int` | Chỉ số thứ tự nguồn |
| `folderKey` | `String?` | Khóa thư mục mà item thuộc về |
| `isPlayable` | `Boolean` | Có đủ điều kiện đưa vào queue hay không |
| `origin` | `SiblingOrigin` | `CALLER_PLAYLIST`, `SOURCE_VISIBLE_ORDER`, `LOCAL_INDEX_FALLBACK` |

**Validation rules**:
- `orderIndex >= 0`
- Chỉ item `isPlayable = true` mới được đưa vào queue cuối.
- Không được tồn tại hai `SiblingVideoEntry` có cùng `uri` trong một queue đã resolve.

### PlaybackQueueSnapshot

Kết quả resolve queue mà player sẽ dùng để nạp hoặc hydrate Media3 playlist.

| Field | Type | Description |
|-------|------|-------------|
| `items` | `List<SiblingVideoEntry>` | Queue cuối cùng sau precedence/filter |
| `currentIndex` | `Int` | Vị trí media hiện tại trong queue |
| `resolutionMode` | `QueueResolutionMode` | `EXPLICIT_FULL`, `SOURCE_CONTEXT_FULL`, `LOCAL_FALLBACK_FULL`, `SINGLE_ITEM` |
| `canNavigateByFolder` | `Boolean` | Có thể thực hiện Next/Prev theo thư mục hay không |
| `boundaryBehavior` | `BoundaryBehavior` | V1 là `STAY_AND_NOTIFY` |

**Validation rules**:
- `items` luôn chứa `currentUri` ít nhất một lần trước bước dedupe, và đúng một lần sau bước dedupe.
- `currentIndex` phải trỏ vào item hiện tại sau khi queue đã chuẩn hóa.
- Nếu `resolutionMode == SINGLE_ITEM`, `items.size == 1` và `canNavigateByFolder == false`.

### QueueHydrationState

Trạng thái runtime của quá trình nạp queue bất đồng bộ sau khi media hiện tại đã được phát.

| Field | Type | Description |
|-------|------|-------------|
| `status` | `QueueHydrationStatus` | `IDLE`, `RESOLVING`, `READY`, `FAILED` |
| `launchContext` | `PlaybackLaunchContext` | Context gốc của phiên phát hiện tại |
| `resolvedSnapshot` | `PlaybackQueueSnapshot?` | Queue hoàn tất nếu đã resolve xong |
| `errorReason` | `String?` | Lý do fallback về single-item hoặc không hydrate được |

**State transitions**:
```text
IDLE -> RESOLVING -> READY
IDLE -> READY            (external playlist có sẵn)
RESOLVING -> FAILED      (không resolve được sibling context)
FAILED -> READY          (fallback single-item snapshot)
media transition/new intent -> IDLE
```

## Enums

### PlaybackSourceType
```kotlin
enum class PlaybackSourceType {
    EXTERNAL_PLAYLIST,
    SOURCE_FOLDER,
    SINGLE_URI,
}
```

### QueueResolutionMode
```kotlin
enum class QueueResolutionMode {
    EXPLICIT_FULL,
    SOURCE_CONTEXT_FULL,
    LOCAL_FALLBACK_FULL,
    SINGLE_ITEM,
}
```

### BoundaryBehavior
```kotlin
enum class BoundaryBehavior {
    STAY_AND_NOTIFY,
}
```

### QueueHydrationStatus
```kotlin
enum class QueueHydrationStatus {
    IDLE,
    RESOLVING,
    READY,
    FAILED,
}
```

## Relationships

```text
PlaybackLaunchContext ──1:N──> SiblingVideoEntry
PlaybackLaunchContext ──1:1──> QueueHydrationState
QueueHydrationState ──0..1──> PlaybackQueueSnapshot
PlaybackQueueSnapshot ──1:N──> SiblingVideoEntry
```

## Data Flow

```text
MediaPicker/Search/External caller
    ↓ build PlaybackLaunchContext
PlayerApi / PlayerActivity
    ↓ current item immediate start
ResolvePlaybackQueueUseCase
    ↓ PlaybackQueueSnapshot
QueueHydrationState
    ↓ apply to Media3 queue without moving current item
PlayerService / MediaController / Player UI
```
