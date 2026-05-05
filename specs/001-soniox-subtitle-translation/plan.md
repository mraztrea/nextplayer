# Implementation Plan: Soniox Real-time Subtitle Translation

**Branch**: `001-soniox-subtitle-translation` | **Date**: 2026-05-05 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/001-soniox-subtitle-translation/spec.md`

## Summary

Tích hợp tính năng phụ đề dịch thời gian thực vào nextplayer bằng Soniox AI. Pipeline: trích xuất audio từ video qua ExoPlayer AudioProcessor → chuẩn hóa 16kHz mono PCM → gửi batch 200ms qua WebSocket tới Soniox → parse token stream → ghép cặp FIFO original/translation → render overlay phụ đề trên video player.

## Technical Context

**Language/Version**: Kotlin 2.3.20, JVM 17  
**Primary Dependencies**: Media3/ExoPlayer 1.10.0, OkHttp 4.12.0 (mới), Jetpack Compose, Hilt, DataStore, EncryptedSharedPreferences  
**Storage**: DataStore (preferences), EncryptedSharedPreferences (API Key)  
**Testing**: JUnit4, Espresso  
**Target Platform**: Android, minSdk 23, targetSdk 36  
**Project Type**: Mobile app (Android video player)  
**Performance Goals**: Phụ đề xuất hiện <3s sau lời nói, provisional <500ms, phiên ổn định 2+ tiếng  
**Constraints**: Không dùng AudioPlaybackCapture, decode audio trực tiếp từ file video  
**Scale/Scope**: Single user, local device, 1 WebSocket connection

## Constitution Check

*GATE: Constitution chưa được cấu hình (toàn placeholder). Bỏ qua gate check — không có ràng buộc nào cần xác nhận.*

## Project Structure

### Documentation (this feature)

```text
specs/001-soniox-subtitle-translation/
├── plan.md                              # This file
├── spec.md                              # Feature specification
├── research.md                          # Phase 0: research findings
├── data-model.md                        # Phase 1: data model
├── quickstart.md                        # Phase 1: quickstart guide
├── contracts/
│   ├── soniox-websocket.md              # Soniox WebSocket protocol
│   └── subtitle-engine-player.md        # Internal engine ↔ player contract
├── checklists/
│   └── requirements.md                  # Spec quality checklist
└── tasks.md                             # Phase 2 output (by /speckit-tasks)
```

### Source Code (repository root)

```text
core/
├── subtitle/                            # ★ NEW MODULE
│   ├── build.gradle.kts
│   └── src/main/java/.../core/subtitle/
│       ├── engine/
│       │   ├── SubtitleEngine.kt        # Interface + SubtitleEngineImpl
│       │   ├── SonioxWebSocketClient.kt # OkHttp WebSocket, keepalive, reconnect
│       │   └── SonioxTokenParser.kt     # Token classification logic
│       ├── model/
│       │   ├── SubtitleSegment.kt       # Data class
│       │   ├── SegmentStatus.kt         # Enum: ORIGINAL, TRANSLATED
│       │   └── SubtitleDisplayMode.kt   # Enum: ORIGINAL_ONLY, TRANSLATION_ONLY, BILINGUAL
│       ├── session/
│       │   ├── SubtitleSessionManager.kt # FIFO queue, display/session buffers
│       │   └── SessionResetScheduler.kt  # Make-before-break session reset
│       ├── audio/
│       │   ├── SubtitleAudioProcessor.kt # ExoPlayer AudioProcessor tap
│       │   └── AudioBatcher.kt           # 200ms PCM batching + resample
│       └── di/
│           └── SubtitleModule.kt         # Hilt DI bindings

core/datastore/
└── ...datasource/
    └── PlayerPreferencesDataSource.kt    # ★ MODIFY: thêm translation preferences

core/model/
└── ...model/
    └── PlayerPreferences.kt              # ★ MODIFY: thêm translation fields

feature/player/
└── ...player/
    ├── PlayerActivity.kt                 # ★ MODIFY: inject AudioProcessor, toggle subtitle
    ├── PlayerViewModel.kt                # ★ MODIFY: quản lý subtitle engine state
    └── ui/
        └── SubtitleOverlay.kt            # ★ NEW: Compose overlay component

feature/settings/
└── ...settings/
    ├── screens/subtitle/
    │   ├── SubtitlePreferencesScreen.kt  # ★ MODIFY: thêm translation settings UI
    │   └── SubtitlePreferencesViewModel.kt # ★ MODIFY: thêm translation preferences
    └── navigation/
        └── SubtitlePreferencesNavigation.kt # (có thể không cần sửa)
```

**Structure Decision**: Tạo module mới `core:subtitle` chứa toàn bộ business logic Soniox. Mở rộng modules hiện có (`core:datastore`, `core:model`, `feature:player`, `feature:settings`) cho integration. Giữ nguyên kiến trúc multi-module của nextplayer.

## Complexity Tracking

> Không có vi phạm constitution cần justification.
