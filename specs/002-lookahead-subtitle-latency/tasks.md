# Tasks: Tối ưu Độ trễ Phụ đề Dịch với Lookahead Audio

**Input**: Design documents from `/specs/002-lookahead-subtitle-latency/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Spec không yêu cầu TDD bắt buộc, nên không tách test tasks riêng. Xác minh độc lập của từng story dùng các kịch bản trong `quickstart.md`.

**Organization**: Tasks được nhóm theo user story để mỗi story có thể triển khai và kiểm thử độc lập.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Có thể chạy song song (khác file, không phụ thuộc task chưa hoàn thành)
- **[Story]**: User story liên quan (`US1`, `US2`, `US3`)
- Mỗi task đều có file path rõ ràng

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Chuẩn bị điểm mở rộng chung cho lookahead subtitle trong module hiện có

- [X] T001 Cập nhật wiring chung cho lookahead trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/di/SubtitleModule.kt`
- [X] T002 Cập nhật ghi chú xác minh và metric mục tiêu cho feature trong `specs/002-lookahead-subtitle-latency/quickstart.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Dữ liệu và interface runtime dùng chung cho mọi user story

**⚠️ CRITICAL**: Phải hoàn thành trước khi bắt đầu bất kỳ user story nào

- [X] T003 [P] Mở rộng `SubtitleSegment` với timing và generation fields trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/SubtitleSegment.kt`
- [X] T004 [P] Tạo `LookaheadAudioChunk` trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/LookaheadAudioChunk.kt`
- [X] T005 [P] Tạo `LookaheadPipelineStatus` trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/LookaheadPipelineStatus.kt`
- [X] T006 [P] Tạo `LookaheadSessionState` trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/LookaheadSessionState.kt`
- [X] T007 Cập nhật callback contract để giữ được timing token gốc trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SonioxTokenParser.kt`
- [X] T008 Cập nhật public interface và state flows của engine cho timed render / playback sync trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SubtitleEngine.kt`

**Checkpoint**: Runtime model và interface nền tảng sẵn sàng cho triển khai lookahead.

---

## Phase 3: User Story 1 - Phụ đề dịch sẵn sàng trước thời điểm lời nói phát ra (Priority: P1) 🎯 MVP

**Goal**: Pipeline chuẩn bị audio trước ~5 giây và chỉ render subtitle khi tới đúng media-time tương ứng.

**Independent Test**: Bật live subtitle trên video có lời thoại liên tục, chờ warm-up xong, rồi xác nhận subtitle dịch không xuất hiện sớm hơn audio và xuất hiện gần như ngay khi câu bắt đầu phát.

### Implementation for User Story 1

- [X] T009 [US1] Implement pipeline giải mã lookahead có lead khoảng 5 giây trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/audio/LookaheadAudioPipeline.kt`
- [X] T010 [US1] Parse và gom `start_ms`/`end_ms` của original final tokens trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SonioxTokenParser.kt`
- [X] T011 [US1] Chuyển `SubtitleSessionManager` sang timed sync buffer với pending original queue trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/session/SubtitleSessionManager.kt`
- [X] T012 [US1] Orchestrate lookahead audio -> Soniox -> timed segments trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SubtitleEngine.kt`
- [X] T013 [US1] Cập nhật `SonioxWebSocketClient` để giữ nguyên transport nhưng hỗ trợ payload timing cần cho timed subtitle flow trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SonioxWebSocketClient.kt`
- [X] T014 [US1] Đồng bộ `ExoPlayer.currentPosition` vào subtitle engine trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/service/PlayerService.kt`
- [X] T015 [US1] Expose visible timed subtitle state cho UI trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerViewModel.kt`
- [X] T016 [US1] Chỉ render subtitle đã tới lượt và chặn future spoiler trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/SubtitleOverlay.kt`
- [X] T017 [US1] Nối timed subtitle state vào màn hình player trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/MediaPlayerScreen.kt`

**Checkpoint**: User Story 1 hoàn thành khi subtitle lookahead không spoiler và render đúng media-time.

---

## Phase 4: User Story 2 - Đồng bộ chính xác sau khi tua, đổi track, hoặc tạm dừng (Priority: P1)

**Goal**: Seek, pause/resume, media transition, và audio-track switch phải reset generation đúng cách, không để sót subtitle cũ.

**Independent Test**: Trong khi live subtitle đang chạy, tua tiến/tua lùi/đổi media hoặc đổi track audio rồi xác nhận subtitle cũ biến mất nhanh và subtitle mới phục hồi theo ngữ cảnh mới.

### Implementation for User Story 2

- [X] T018 [US2] Thêm reset flow theo `generationId` cho seek/media change/audio-track change trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SubtitleEngine.kt`
- [X] T019 [US2] Hủy queue cũ và seek lại decoder lookahead theo generation mới trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/audio/LookaheadAudioPipeline.kt`
- [X] T020 [US2] Xóa visible buffer, pending queue, và provisional state khi generation đổi trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/session/SubtitleSessionManager.kt`
- [X] T021 [US2] Xử lý session rotation/reconnect an toàn khi seek-driven reset trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SonioxWebSocketClient.kt`
- [X] T022 [US2] Phát tín hiệu seek, media transition, play/pause, và audio-track switch từ player sang engine trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/service/PlayerService.kt`
- [X] T023 [US2] Đồng bộ notice/state của live subtitle với vòng đời playback trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerViewModel.kt`

**Checkpoint**: User Story 2 hoàn thành khi mọi reset theo playback lifecycle đều loại bỏ sạch dữ liệu cũ và không còn “bóng ma” subtitle.

---

## Phase 5: User Story 3 - Giảm độ trễ nhưng không làm nặng máy (Priority: P2)

**Goal**: Lookahead phải có bounded processing, fallback an toàn, và telemetry đủ để kiểm chứng độ trễ/performance.

**Independent Test**: Phát video liên tục với live subtitle bật, xác nhận lookahead giữ lead ổn định, không làm video giật, và khi lookahead lỗi thì playback vẫn bình thường.

### Implementation for User Story 3

- [X] T024 [US3] Thêm high-water/low-water throttling và queue bounds trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/audio/LookaheadAudioPipeline.kt`
- [ ] T025 [US3] Bổ sung cleanup muộn, fallback path, và health state cho timed subtitle session trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/session/SubtitleSessionManager.kt`
- [ ] T026 [US3] Kích hoạt fallback về current-position tap khi lookahead không khả dụng trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/audio/SubtitleAudioProcessor.kt` và `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SubtitleEngine.kt`
- [ ] T027 [US3] Expose telemetry cho lead, generation, fallback, và transport health trong `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SubtitleEngine.kt` và `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SonioxWebSocketClient.kt`
- [X] T028 [US3] Hiển thị trạng thái fallback/recovery thân thiện trên player trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerViewModel.kt` và `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/SubtitleOverlay.kt`

**Checkpoint**: User Story 3 hoàn thành khi lookahead có giới hạn tài nguyên rõ ràng, có fallback an toàn, và có đủ telemetry để đo latency/performance.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Hoàn thiện tài liệu triển khai, xác minh thủ công, và tinh chỉnh hằng số liên story

- [ ] T029 [P] Rà soát và cập nhật contract timing/render cuối cùng trong `specs/002-lookahead-subtitle-latency/contracts/soniox-timed-token-mapping.md` và `specs/002-lookahead-subtitle-latency/contracts/subtitle-sync-player.md`
- [X] T030 [P] Chạy lại checklist xác minh thủ công và cập nhật bước đo latency/seek/fallback trong `specs/002-lookahead-subtitle-latency/quickstart.md`
- [X] T031 [P] Ghi lại workflow triển khai và lưu ý hiệu năng vào `memory_bank/wf_20260505_lookahead_subtitle_latency_plan.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Bắt đầu ngay, không phụ thuộc phase khác
- **Foundational (Phase 2)**: Phụ thuộc Phase 1, chặn toàn bộ user stories
- **US1 (Phase 3)**: Phụ thuộc Phase 2, là MVP của feature
- **US2 (Phase 4)**: Phụ thuộc US1 vì reset/seek logic phải bám trên timed subtitle flow đã hoạt động
- **US3 (Phase 5)**: Phụ thuộc US1; có thể bắt đầu sau khi timed flow cơ bản ổn định, song song phần cuối với US2 nếu tránh đụng cùng file
- **Polish (Phase 6)**: Phụ thuộc các user stories mong muốn đã xong

### User Story Dependencies

- **US1**: Không phụ thuộc user story khác sau khi Foundation xong
- **US2**: Phụ thuộc US1
- **US3**: Phụ thuộc US1; tham chiếu kết quả US2 khi tinh chỉnh seek/fallback

### Within Each User Story

- Runtime models/interfaces trước orchestration
- Orchestration trước player integration
- Player integration trước UI gating
- Fallback/performance tuning sau khi timed flow cơ bản đã chạy

### Parallel Opportunities

- Phase 2: `T003`–`T006` có thể chạy song song
- US1: `T014`, `T015`, `T016` có thể tách nhịp sau khi `T012`/`T013` xong
- US2: `T020` và `T023` có thể chạy song song sau khi `T018` định nghĩa reset contract
- US3: `T024` và `T028` có thể chạy song song sau khi `T026`/`T027` định nghĩa fallback state

---

## Parallel Example: User Story 1

```text
# Sau khi timed buffer contract đã ổn:
T014: Đồng bộ currentPosition trong feature/player/.../service/PlayerService.kt
T015: Expose visible timed subtitle state trong feature/player/.../PlayerViewModel.kt
T016: Chặn future spoiler trong feature/player/.../ui/SubtitleOverlay.kt
```

## Parallel Example: User Story 2

```text
# Sau khi generation reset contract được chốt ở engine:
T020: Seek/reset decoder trong core/subtitle/.../audio/LookaheadAudioPipeline.kt
T023: Đồng bộ playback lifecycle notices trong feature/player/.../PlayerViewModel.kt
```

## Parallel Example: User Story 3

```text
# Sau khi fallback path đã tồn tại:
T024: High-water/low-water throttling trong core/subtitle/.../audio/LookaheadAudioPipeline.kt
T028: UI fallback/recovery state trong feature/player/.../PlayerViewModel.kt và ui/SubtitleOverlay.kt
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Hoàn thành Setup (Phase 1)
2. Hoàn thành Foundational (Phase 2)
3. Hoàn thành User Story 1 (Phase 3)
4. Dừng lại và xác minh subtitle timed lookahead hoạt động đúng

### Incremental Delivery

1. Setup + Foundational
2. US1 -> timed lookahead render đúng media-time
3. US2 -> reset đúng khi seek/pause/đổi track
4. US3 -> bounded processing, fallback, telemetry
5. Polish -> xác minh và chốt tài liệu

### Suggested MVP Scope

- **MVP đề xuất**: Chỉ cần hoàn thành **User Story 1**
- **Lý do**: Đây là phần chứng minh cốt lõi rằng lookahead thực sự làm giảm độ trễ cảm nhận mà không spoiler subtitle tương lai

---

## Notes

- Tất cả task đều theo đúng checklist format `- [ ] Txxx ...`
- Không thêm test tasks riêng vì spec không yêu cầu TDD bắt buộc
- `PlayerService.kt`, `SubtitleEngine.kt`, `SubtitleSessionManager.kt`, và `LookaheadAudioPipeline.kt` là bốn điểm nóng dễ va chạm; không nên chạy song song các task cùng sửa một file này
