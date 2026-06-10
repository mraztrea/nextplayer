# Implementation Plan: Offline ASR Translation

**Branch**: `007-offline-asr-translation` | **Date**: 2026-06-10 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `specs/003-offline-asr-translation/spec.md`

## Summary

Triển khai prototype phụ đề offline cho audio tiếng Nhật trên Android: nhận dạng lời thoại tiếng Nhật, dịch sang tiếng Việt hoặc tiếng Anh, và hiển thị qua overlay phụ đề hiện có. Tính năng mở rộng module `core:subtitle` bằng boundary engine offline, model manager có kiểm tra manifest/metadata, và UI cài đặt để tải/import/xóa model. Phiên phụ đề offline không được tạo kết nối mạng; mạng chỉ dùng ở bước tải model trước phiên.

## Technical Context

**Language/Version**: Kotlin 2.3.20, JVM 17  
**Primary Dependencies**: Media3/ExoPlayer 1.10.0, Jetpack Compose, Hilt, DataStore, OkHttp, kotlinx-serialization-json, AndroidX Security Crypto; runtime offline prototype dùng adapter có thể gắn sherpa-onnx AAR hoặc whisper.cpp native binding  
**Storage**: App-specific files cho model offline, DataStore cho trạng thái/cài đặt model, manifest JSON trong gói model  
**Testing**: JUnit4 cho parser/model manager/session logic; Gradle `testDebugUnitTest`; manual quickstart trên thiết bị/emulator Android  
**Target Platform**: Android app, minSdk 23, targetSdk 36  
**Project Type**: Mobile app multi-module  
**Performance Goals**: Transcript đầu tiên trong 5 giây sau lời nói; bản dịch trong 3 giây sau khi segment được chốt; phiên offline chạy tối thiểu 30 phút trên thiết bị hỗ trợ  
**Constraints**: Không gửi audio/transcript/bản dịch ra ngoài thiết bị trong phiên offline; v1 tập trung tiếng Nhật -> Việt/Anh; tải model luôn cần xác nhận và mặc định chỉ qua Wi-Fi; import chỉ nhận gói có manifest/metadata hợp lệ  
**Scale/Scope**: Single-user local playback, một phiên subtitle offline tại một thời điểm, một gói model v1 đầy đủ trên thiết bị

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution hiện là template chưa có nguyên tắc ràng buộc cụ thể. Áp dụng các rule dự án từ `AGENTS.md`:

- **Spec Kit workflow**: PASS - Plan nằm trong active feature `specs/003-offline-asr-translation`.
- **Windows/PowerShell commands**: PASS - Các lệnh verification dùng `pwsh`/`gradlew.bat`.
- **Surgical changes**: PASS - Thiết kế mở rộng `core:subtitle`, `core:datastore`, `feature:player`, `feature:settings` thay vì tạo subsystem song song.
- **Privacy/offline constraint**: PASS - Runtime offline không có network path; network manager chỉ ở chuẩn bị model.

Post-design re-check: PASS - Các artifacts Phase 1 giữ cùng boundary module, có contract manifest và quickstart kiểm thử offline.

## Project Structure

### Documentation (this feature)

```text
specs/003-offline-asr-translation/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── offline-model-manifest.schema.json
│   └── offline-subtitle-flow.md
└── tasks.md
```

### Source Code (repository root)

```text
core/subtitle/
├── src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/
│   ├── audio/                  # Reuse SubtitleAudioProcessor and AudioBatcher
│   ├── engine/                 # Existing Soniox engine + new offline engine boundary
│   ├── model/                  # Existing subtitle models + offline model state
│   ├── session/                # Existing session manager integration
│   └── storage/                # Secure/API storage + offline model storage
core/datastore/
└── src/main/java/dev/anilbeesetti/nextplayer/core/datastore/
    └── datasource/             # Persist offline subtitle settings
feature/player/
└── src/main/java/dev/anilbeesetti/nextplayer/feature/player/
    ├── PlayerViewModel.kt      # Select online/offline subtitle mode and lifecycle
    └── ui/SubtitleOverlay.kt   # Reuse overlay display modes
feature/settings/
└── src/main/java/dev/anilbeesetti/nextplayer/settings/screens/subtitle/
    ├── SubtitlePreferencesScreen.kt
    └── SubtitlePreferencesViewModel.kt
core/ui/src/main/res/values/strings.xml
```

**Structure Decision**: Giữ module `core:subtitle` làm trung tâm cho cả Soniox online và offline. Offline runtime được ẩn sau interface để prototype có thể dùng sherpa-onnx streaming hoặc whisper.cpp binding mà không thay đổi UI/player.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Native/runtime adapter boundary | Offline ASR/dịch cần engine on-device thay thế Soniox WebSocket | Gắn trực tiếp runtime vào `PlayerViewModel` sẽ trộn UI, audio lifecycle và native inference, khó test và khó đổi sherpa/whisper |
