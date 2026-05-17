# Implementation Plan: Tối ưu phát video LAN và điều hướng cùng thư mục

**Branch**: `003-lan-playback-optimization` | **Date**: 2026-05-17 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `specs/003-lan-playback-optimization/spec.md`

## Summary

Tối ưu luồng mở player để video hiện tại được phát ngay cả khi nguồn là thư mục LAN, đồng thời bổ sung ngữ cảnh phát đủ giàu để Next/Prev hoạt động theo đúng danh sách người dùng vừa thấy. Hướng triển khai là: truyền `playback context` từ launcher khi app đã biết danh sách anh em, resolve queue theo thứ tự ưu tiên rõ ràng, và hydrate playlist bất đồng bộ để không chặn `prepare()` của media hiện tại.

## Technical Context

**Language/Version**: Kotlin 2.3.20, JVM 17  
**Primary Dependencies**: AndroidX Media3 1.10.0, Jetpack Compose, Hilt 2.59.2, Kotlin Coroutines 1.10.2, `nextlib-media3ext` 1.9.3-0.12.0  
**Storage**: Room media index + DataStore preferences; ngữ cảnh phát và trạng thái hydrate queue chỉ giữ trong memory, không đổi schema  
**Testing**: JUnit4 + `kotlinx-coroutines-test` cho domain/player logic; `compileDebugKotlin` cho `feature:player`, `feature:videopicker`, `app`  
**Target Platform**: Android API 23+  
**Project Type**: Ứng dụng Android đa module  
**Performance Goals**: 90% lượt mở video LAN ổn định bắt đầu phát trong <= 2 giây; điều khiển cơ bản phản hồi ngay; 95% thao tác Next/Prev đi đúng item liền kề theo thứ tự nguồn hoặc fallback sort  
**Constraints**: Không chặn phát media hiện tại chỉ để resolve queue; playlist truyền từ nguồn ngoài có ưu tiên cao nhất; URL mạng đơn lẻ không được kích hoạt quét LAN mù; không migrate database; giữ backward compatibility cho caller cũ chỉ truyền `data` hoặc `video_list`  
**Scale/Scope**: 1 phiên playback chủ động, 1 media item hiện tại, 1 danh sách anh em của thư mục hiện tại, áp dụng cho local library và LAN source mà app đã duyệt tới

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

*GATE: Constitution hiện vẫn là template placeholder, chưa có rule khả thi để chặn plan này. Không phát hiện vi phạm cần justification trước hoặc sau Phase 1.*

## Project Structure

### Documentation (this feature)

```text
specs/003-lan-playback-optimization/
├── plan.md                              # This file
├── research.md                          # Phase 0: design decisions and alternatives
├── data-model.md                        # Phase 1: playback context and queue state model
├── quickstart.md                        # Phase 1: verification flow for local/LAN playback
├── contracts/
│   ├── playback-launch-context.md       # Launcher -> PlayerActivity contract
│   └── playback-queue-resolution.md     # Queue resolver and hydration behavior
└── tasks.md                             # Phase 2 output (/speckit-tasks)
```

### Source Code (repository root)

```text
app/
└── src/main/java/dev/anilbeesetti/nextplayer/navigation/
    └── MediaNavGraph.kt                         # Launch player from media picker/search

feature/
├── player/
│   └── src/main/java/dev/anilbeesetti/nextplayer/feature/player/
│       ├── PlayerActivity.kt                   # Current playback bootstrap and transition hooks
│       ├── PlayerViewModel.kt                  # Existing playlist lookup entrypoint and state holder
│       ├── service/PlayerService.kt            # ExoPlayer session, queue end behavior, metadata hydration
│       ├── utils/PlayerApi.kt                  # Intent/extras parsing contract
│       ├── model/PlaybackLaunchContext.kt      # New launch context model
│       ├── model/QueueHydrationState.kt        # New async queue hydration state
│       └── queue/PlaybackQueueResolver.kt      # New orchestration layer for queue precedence
└── videopicker/
    └── src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/
        ├── navigation/                         # Existing screen entry contract
        ├── screens/mediapicker/MediaPickerScreen.kt
        └── screens/mediapicker/MediaPickerViewModel.kt

core/
├── common/
│   └── src/main/java/dev/anilbeesetti/nextplayer/core/common/extensions/
│       └── Context.kt                          # Current local-path helpers; LAN fallback boundary
├── domain/
│   └── src/main/java/dev/anilbeesetti/nextplayer/core/domain/
│       ├── GetSortedPlaylistUseCase.kt         # Current local-only sibling inference to refactor/replace
│       ├── GetSortedVideosUseCase.kt           # Existing app-sort fallback
│       └── ResolvePlaybackQueueUseCase.kt      # New queue resolution use case
└── model/
    └── src/main/java/dev/anilbeesetti/nextplayer/core/model/
        ├── Video.kt                            # Existing indexed media model
        ├── Folder.kt                           # Existing folder model
        └── PlaybackQueueSnapshot.kt            # New resolved queue payload
```

**Structure Decision**: Giữ boundary hiện tại của repo: launcher/UI chịu trách nhiệm cung cấp ngữ cảnh nguồn nếu đang có, domain chịu trách nhiệm resolve thứ tự/fallback, và `feature:player` orchestration việc hydrate playlist vào Media3. Không thêm module Gradle mới và không đẩy logic LAN discovery xuống `PlayerService`.

## Complexity Tracking

> Không có vi phạm constitution cần justification.
