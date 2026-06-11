# Implementation Plan: Gemini Live Realtime Subtitle Translation

**Branch**: `008-gemini-live-translation` | **Date**: 2026-06-11 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `specs/003-gemini-live-translation/spec.md`

## Summary

Thêm Gemini Live như provider phụ đề realtime bổ sung bên cạnh Soniox. Người dùng nhập Google API Key, chọn provider Soniox/Gemini, chọn ngôn ngữ đích, rồi xem phụ đề realtime khớp audio đang phát với 3 chế độ hiển thị: chỉ bản dịch, song ngữ, chỉ bản gốc. Cách triển khai mở rộng module `core:subtitle` hiện có bằng provider/session Gemini, tái sử dụng audio tap từ player pipeline, secure key storage, settings/player UI và subtitle overlay hiện có.

## Technical Context

**Language/Version**: Kotlin 2.3.20, JVM 17  
**Primary Dependencies**: Media3/ExoPlayer 1.10.0, OkHttp 4.12.0, Jetpack Compose, Hilt, DataStore, EncryptedSharedPreferences  
**Storage**: DataStore cho preferences/provider selection; EncryptedSharedPreferences cho Google API Key và Soniox API Key tách biệt  
**Testing**: JUnit4 unit tests, Android instrumented/UI tests khi cần, ktlint  
**Target Platform**: Android, minSdk 23, targetSdk 36  
**Project Type**: Mobile app (Android video player)  
**Performance Goals**: Phụ đề Gemini đầu tiên xuất hiện trong 3 giây; reset sau seek/đổi media xóa phụ đề cũ trong 1 giây; phiên xem thử nghiệm 30 phút không làm dừng player  
**Constraints**: Không dùng AudioPlaybackCapture; chỉ dịch audio từ media item đang phát; Gemini Live Translation là audio-only; input gửi tới provider là PCM 16 kHz mono; không phát lại audio dịch trong v1; không log audio/transcript  
**Scale/Scope**: Single user, local device, 1 phiên phụ đề realtime đang hoạt động, provider được người dùng chọn thủ công

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution file hiện vẫn là template placeholder, chưa có nguyên tắc project-specific được ratify để tạo gate bắt buộc. Áp dụng các ràng buộc vận hành từ `AGENTS.md`: thay đổi tối thiểu, bám kiến trúc hiện có, dùng PowerShell trên Windows, tài liệu/phản hồi tiếng Việt, không mở rộng scope ngoài spec.

**Gate status**: PASS. Không có violation constitution cần justify.

## Project Structure

### Documentation (this feature)

```text
specs/003-gemini-live-translation/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── gemini-live-subtitle-contract.md
└── tasks.md
```

### Source Code (repository root)

```text
core/
├── subtitle/
│   └── src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/
│       ├── audio/              # existing player PCM tap and batching
│       ├── engine/             # existing SubtitleEngine + Soniox client; add provider abstraction and Gemini client/session
│       ├── model/              # provider selection, Gemini config/status/error models, shared display modes/segments
│       └── storage/            # secure provider key storage
├── datastore/                  # subtitle preferences and provider selection
└── model/                      # shared preference/domain models if required

feature/
├── settings/
│   └── src/main/java/dev/anilbeesetti/nextplayer/settings/screens/subtitle/
│       ├── SubtitlePreferencesScreen.kt
│       └── SubtitlePreferencesViewModel.kt
└── player/
    └── src/main/java/dev/anilbeesetti/nextplayer/feature/player/
        ├── PlayerViewModel.kt
        ├── state/SubtitleOptionsState.kt
        └── ui/SubtitleOverlay.kt
```

**Structure Decision**: Không tạo module mới. Mở rộng `core:subtitle` thành subtitle provider layer dùng chung cho Soniox và Gemini Live, sau đó cập nhật `feature:settings` và `feature:player` để chọn provider/cấu hình Gemini trong UI hiện có.

## Phase 0: Research

Research output: [research.md](./research.md)

Key decisions:
- Reuse player audio pipeline and `SubtitleAudioProcessor`; provider config quyết định chunk size/format.
- Add Gemini Live provider in `core:subtitle` using the same lifecycle boundary as Soniox.
- Store Google API Key separately from Soniox API Key using secure storage.
- Map UI alias `vn` to BCP-47 `vi`, and use `en` for English.
- Enable same-target-language transcript behavior for Gemini sessions.

## Phase 1: Design & Contracts

Design outputs:
- [data-model.md](./data-model.md)
- [contracts/gemini-live-subtitle-contract.md](./contracts/gemini-live-subtitle-contract.md)
- [quickstart.md](./quickstart.md)

Post-design constitution check: PASS. Design stays within existing Android modules, does not add a backend, does not introduce long-term audio/transcript storage, and keeps Gemini as user-selected provider instead of hidden fallback.

## Complexity Tracking

No constitution violations.
