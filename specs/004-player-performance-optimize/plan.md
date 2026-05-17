# Implementation Plan: Tối Ưu Hiệu Suất Video Player

**Branch**: `004-player-performance-optimize` | **Date**: 2026-05-17 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification từ `specs/004-player-performance-optimize/spec.md`

## Summary

Tối ưu ExoPlayer trong NextPlayer bằng cách áp dụng các kỹ thuật từ module video player cũ:
- Cấu hình `DefaultLoadControl` tuỳ theo loại nguồn media (local/network) để giảm thời gian khởi động
- Thêm `setWakeMode()` phân biệt local file và network stream để tiết kiệm tài nguyên
- Tận dụng tốt hơn FFmpeg decoder đã tích hợp sẵn (`nextlib-media3ext`)

**Lưu ý**: Sau research, xác nhận FFmpeg audio decoder (FR-003, FR-004) **đã có sẵn** qua `nextlib-media3ext`. Preferred codec selection (FR-005, FR-006) được **deferred** sang P3.

## Technical Context

**Language/Version**: Kotlin 1.9+, Android SDK  
**Primary Dependencies**: Media3 ExoPlayer 1.10.0, nextlib-media3ext 1.9.3-0.12.0, Hilt DI  
**Storage**: DataStore (preferences), Room (media state)  
**Testing**: Android Instrumented Tests, Unit Tests  
**Target Platform**: Android 5.0+ (API 21+)  
**Project Type**: Mobile App (Android)  
**Performance Goals**: First frame < 1s cho video local 1080p, RAM giảm 15%  
**Constraints**: Không gây stutter trên video bitrate cao, backward compatible  
**Scale/Scope**: 3 files thay đổi chính, ~100-150 dòng code mới

## Constitution Check

*Constitution chưa được điền (vẫn là template) — skip gate.*

## Project Structure

### Documentation (this feature)

```text
specs/004-player-performance-optimize/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: Current state analysis & decisions
├── data-model.md        # Phase 1: MediaSourceType, LoadControlConfig entities
├── quickstart.md        # Phase 1: Implementation quickstart
└── tasks.md             # Phase 2 output (/speckit-tasks command)
```

### Source Code (repository root)

```text
feature/player/
├── src/main/java/dev/anilbeesetti/nextplayer/feature/player/
│   ├── service/
│   │   └── PlayerService.kt          # ← MODIFY: ExoPlayer builder + LoadControl + WakeMode
│   ├── model/
│   │   └── MediaSourceType.kt        # ← NEW: LOCAL/NETWORK enum
│   └── utils/
│       └── PlayerApi.kt              # Existing, no changes
│
core/model/
└── src/main/java/dev/anilbeesetti/nextplayer/core/model/
    └── PlayerPreferences.kt          # Existing, no changes in P1/P2

core/common/
└── src/main/java/dev/anilbeesetti/nextplayer/core/common/extensions/
    └── UriExtensions.kt              # ← MODIFY or NEW: Uri.toMediaSourceType()
```

**Structure Decision**: Sử dụng cấu trúc Android multi-module hiện có. `MediaSourceType` đặt trong `feature/player/model/` vì chỉ dùng trong player module. Extension function `Uri.toMediaSourceType()` có thể đặt trong `core/common/extensions/` nếu cần shared, hoặc trong `feature/player/extensions/` nếu chỉ player dùng.

## Implementation Details

### Task 1: Tạo MediaSourceType enum (P1 - Startup)

File: `feature/player/src/main/java/.../model/MediaSourceType.kt`

```kotlin
enum class MediaSourceType {
    LOCAL,    // content://, file://
    NETWORK;  // http://, https://, smb://, ftp://, rtsp://

    companion object {
        fun fromUri(uri: Uri): MediaSourceType {
            return when (uri.scheme?.lowercase()) {
                "content", "file", null -> LOCAL
                else -> NETWORK
            }
        }
    }
}
```

### Task 2: Tối ưu LoadControl trong ExoPlayer Builder (P1 - Core)

File: `feature/player/src/main/java/.../service/PlayerService.kt`

Thêm `DefaultLoadControl` vào `ExoPlayer.Builder` trong `onCreate()`:

```kotlin
// Xác định source type từ preferences hoặc để detect khi set media items
val loadControl = DefaultLoadControl.Builder()
    .setBufferDurationsMs(
        /* minBufferMs */ 15_000,
        /* maxBufferMs */ 50_000,
        /* bufferForPlaybackMs */ 500,
        /* bufferForPlaybackAfterRebufferMs */ 1_000,
    )
    .build()

val player = ExoPlayer.Builder(applicationContext)
    .setLoadControl(loadControl)          // ← NEW
    .setRenderersFactory(renderersFactory)
    .setTrackSelector(trackSelector)
    // ... existing config
    .build()
```

### Task 3: Thêm WakeMode (P2 - Resource)

File: `feature/player/src/main/java/.../service/PlayerService.kt`

Thêm wake mode detection trong `playbackStateListener.onMediaItemTransition()`:

```kotlin
override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
    // ... existing code ...
    // Detect source type và set wake mode
    mediaItem?.mediaId?.toUri()?.let { uri ->
        val wakeMode = when (MediaSourceType.fromUri(uri)) {
            MediaSourceType.LOCAL -> C.WAKE_MODE_LOCAL
            MediaSourceType.NETWORK -> C.WAKE_MODE_NETWORK
        }
        (mediaSession?.player as? ExoPlayer)?.setWakeMode(wakeMode)
    }
}
```

### Task 4: Logging & Error Recovery (P2 - Stability)

Thêm detailed logging khi codec error xảy ra, tận dụng `onPlayerError()` listener hiện có.

## Complexity Tracking

Không có complexity violations — tất cả thay đổi nằm trong các files hiện có, không thêm module/project mới.
