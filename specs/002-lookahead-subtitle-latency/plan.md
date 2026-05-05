# Implementation Plan: Tối ưu Độ trễ Phụ đề Dịch với Lookahead Audio

**Branch**: `002-lookahead-subtitle-latency` | **Date**: 2026-05-05 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `specs/002-lookahead-subtitle-latency/spec.md`

## Summary

Tối ưu pipeline phụ đề Soniox hiện có theo hướng chuẩn bị audio trước thời điểm phát bằng một luồng lookahead song song. Kết quả mong muốn là: Soniox vẫn tiếp tục xử lý speech-to-text và translation trong cùng một phiên WebSocket thời gian thực, nhưng engine sẽ gửi audio sớm hơn khoảng 5 giây, dùng timestamp của token gốc làm timing truth, rồi chỉ render phụ đề khi `ExoPlayer.currentPosition` đi vào đúng cửa sổ media-time tương ứng.

## Technical Context

**Language/Version**: Kotlin 2.3.20, JVM 17  
**Primary Dependencies**: AndroidX Media3 1.10.0, OkHttp 4.12.0, Kotlin Coroutines 1.10.2, Jetpack Compose, Hilt, AndroidX Security Crypto  
**Storage**: DataStore + EncryptedSharedPreferences cho cấu hình; buffer lookahead và subtitle state ở memory  
**Testing**: JUnit4, kotlinx-coroutines-test, Android instrumentation/Espresso cho player flow  
**Target Platform**: Android API 23+  
**Project Type**: Mobile app đa module  
**Performance Goals**: Sau khi warm up, phụ đề dịch xuất hiện trong vòng 250ms kể từ lúc lời thoại bắt đầu phát; seek recovery trong vòng 2 giây; không gây giật phát video mà người dùng nhận thấy  
**Constraints**: Không spoiler subtitle tương lai; chỉ dùng 1 phiên Soniox active cho media item hiện tại; giới hạn lookahead khoảng 5 giây với cơ chế high-water/low-water; phải reset đúng khi seek/đổi track/pause dài  
**Scale/Scope**: 1 người dùng cục bộ, 1 video đang phát, 1 audio track được chọn, 1 worker lookahead decode

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

*GATE: Constitution hiện vẫn là template placeholder, chưa có quy tắc ràng buộc khả thi để chặn plan này. Không phát hiện vi phạm cần justification trước hoặc sau Phase 1.*

## Project Structure

### Documentation (this feature)

```text
specs/002-lookahead-subtitle-latency/
├── plan.md                              # This file
├── research.md                          # Phase 0: technical decisions
├── data-model.md                        # Phase 1: runtime entities and state
├── quickstart.md                        # Phase 1: verification and usage flow
├── contracts/
│   ├── soniox-timed-token-mapping.md    # Soniox timing + translation contract
│   └── subtitle-sync-player.md          # Player ↔ subtitle engine sync contract
├── checklists/
│   └── requirements.md                  # Spec checklist from /speckit-specify
└── tasks.md                             # Phase 2 output (/speckit-tasks)
```

### Source Code (repository root)

```text
core/
├── subtitle/
│   └── src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/
│       ├── audio/
│       │   ├── AudioBatcher.kt                  # Existing PCM batching
│       │   ├── SubtitleAudioProcessor.kt        # Existing current-position tap, giữ làm fallback/control
│       │   └── LookaheadAudioPipeline.kt        # New bounded extractor/decoder pipeline
│       ├── engine/
│       │   ├── SubtitleEngine.kt                # Orchestrates lookahead + Soniox + sync state
│       │   ├── SonioxWebSocketClient.kt         # Existing WebSocket transport
│       │   └── SonioxTokenParser.kt             # Extend to preserve timing metadata
│       ├── model/
│       │   ├── SubtitleSegment.kt               # Extend with media-time fields
│       │   ├── LookaheadSessionState.kt         # New lookahead runtime state
│       │   └── LookaheadAudioChunk.kt           # New PCM chunk + media-time span
│       └── session/
│           └── SubtitleSessionManager.kt        # Convert from arrival-time buffer to timed sync buffer
└── model/
    └── ...                                     # Reuse existing player preference models if needed

feature/
└── player/
    └── src/main/java/dev/anilbeesetti/nextplayer/feature/player/
        ├── PlayerViewModel.kt                   # Expose timed render state and notices
        ├── service/PlayerService.kt             # Feed playback position/seek/track changes into engine
        └── ui/SubtitleOverlay.kt                # Render only segments whose media-time is due
```

**Structure Decision**: Giữ nguyên kiến trúc multi-module hiện tại và mở rộng module `core:subtitle` đã có. Không tạo module Gradle mới. Phần phức tạp được giữ trong `core:subtitle`; `feature:player` chỉ truyền trạng thái playback vào engine và render state đã được đồng bộ hóa.

## Complexity Tracking

> Không có vi phạm constitution cần justification.
