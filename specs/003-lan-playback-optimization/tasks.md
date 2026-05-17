# Tasks: Tối ưu phát video LAN và điều hướng cùng thư mục

**Input**: Design documents from `/specs/003-lan-playback-optimization/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Spec không yêu cầu TDD bắt buộc, nên không tách test tasks riêng. Xác minh độc lập của từng story dùng các kịch bản và lệnh trong `specs/003-lan-playback-optimization/quickstart.md`.

**Organization**: Tasks được nhóm theo user story để mỗi story có thể triển khai và kiểm thử độc lập.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Có thể chạy song song (khác file, không phụ thuộc task chưa hoàn thành)
- **[Story]**: User story liên quan (`US1`, `US2`, `US3`)
- Mỗi task đều có file path rõ ràng

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Chuẩn bị contract và feedback chung cho feature queue playback

- [X] T001 [P] Mở rộng intent extra keys cho playback context trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/utils/PlayerApi.kt`
- [X] T002 [P] Thêm user-facing strings cho feedback chạm biên queue trong `core/ui/src/main/res/values/strings.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Tạo runtime model và resolver chung cho mọi user story

**⚠️ CRITICAL**: Phải hoàn thành trước khi bắt đầu bất kỳ user story nào

- [X] T003 [P] Tạo `PlaybackSourceType.kt`, `SiblingVideoEntry.kt`, và `PlaybackQueueSnapshot.kt` trong `core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/`
- [X] T004 [P] Tạo `PlaybackLaunchContext.kt`, `QueueHydrationStatus.kt`, và `QueueHydrationState.kt` trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/model/`
- [X] T005 Cập nhật `PlayerApi.kt` để parse `PlaybackLaunchContext` và chuẩn hóa input cũ `data`/`video_list` trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/utils/PlayerApi.kt`
- [X] T006 Tạo `ResolvePlaybackQueueUseCase.kt` trong `core/domain/src/main/java/dev/anilbeesetti/nextplayer/core/domain/`
- [X] T007 Cập nhật `PlayerViewModel.kt` để inject resolver và giữ `QueueHydrationState` cho flow player trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerViewModel.kt`

**Checkpoint**: Runtime model, parser, và queue resolver nền tảng đã sẵn sàng cho mọi story.

---

## Phase 3: User Story 1 - Phát nhanh video LAN (Priority: P1) 🎯 MVP

**Goal**: Video hiện tại phải bắt đầu phát nhanh trên nguồn LAN mà không chờ full queue hoặc metadata phụ.

**Independent Test**: Mở một video từ thư mục LAN đang truy cập được, xác nhận player phát media hiện tại trong thời gian mục tiêu và các điều khiển cơ bản vẫn phản hồi ngay cả khi queue cùng thư mục chưa hydrate xong.

### Implementation for User Story 1

- [X] T008 [US1] Thêm helper đóng gói playback context từ danh sách thư mục hiện tại trong `feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/mediapicker/MediaPickerViewModel.kt`
- [X] T009 [US1] Truyền playback context khi mở video từ thư mục local/LAN trong `feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/mediapicker/MediaPickerScreen.kt` và `app/src/main/java/dev/anilbeesetti/nextplayer/navigation/MediaNavGraph.kt`
- [X] T010 [US1] Refactor bootstrap phát video để start current item trước và dời queue hydration sang bất đồng bộ trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerActivity.kt`
- [X] T011 [US1] Điều chỉnh metadata/queue refresh để background enrichment không reset current playback trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/service/PlayerService.kt`

**Checkpoint**: User Story 1 hoàn thành khi video LAN mở nhanh và player không còn bị block bởi queue preparation.

---

## Phase 4: User Story 2 - Next/Prev theo cùng thư mục cho local và LAN (Priority: P2)

**Goal**: Next/Prev phải đi đúng item liền kề theo thứ tự người dùng vừa thấy ở nguồn mở video hoặc theo app-sort fallback.

**Independent Test**: Mở video từ thư mục local và thư mục LAN có ít nhất 3 video, sau đó bấm Next/Prev liên tiếp và xác nhận player đi đúng item kế tiếp/trước đó theo đúng thứ tự nguồn hoặc fallback.

### Implementation for User Story 2

- [X] T012 [US2] Thay local-only sibling inference bằng precedence `API_PLAYLIST -> source-visible order -> app sort -> single-item` trong `core/domain/src/main/java/dev/anilbeesetti/nextplayer/core/domain/GetSortedPlaylistUseCase.kt` và `core/domain/src/main/java/dev/anilbeesetti/nextplayer/core/domain/ResolvePlaybackQueueUseCase.kt`
- [X] T013 [US2] Dùng resolved queue snapshot từ `PlayerViewModel` để hydrate playlist ổn định cho player trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerViewModel.kt` và `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerActivity.kt`
- [X] T014 [US2] Áp dụng queue đã resolve vào Media3 mà vẫn giữ current item/current position trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerActivity.kt`
- [X] T015 [US2] Căn lại hành vi transition/end-of-item cho queue nhiều item đã hydrate trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerActivity.kt` và `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/service/PlayerService.kt`

**Checkpoint**: User Story 2 hoàn thành khi Next/Prev chạy đúng thứ tự local và LAN mà không làm lệch current item.

---

## Phase 5: User Story 3 - Hành vi an toàn khi thiếu ngữ cảnh thư mục (Priority: P3)

**Goal**: Khi chỉ có raw URL hoặc khi queue không resolve được, player vẫn phát đúng file hiện tại, không quét LAN mù, và phản hồi biên theo cách nhất quán.

**Independent Test**: Mở một raw network URL đơn lẻ hoặc một phiên không có sibling context, xác nhận current item vẫn phát bình thường; khi bấm Next/Prev ở biên, player giữ nguyên item hiện tại và hiện feedback ngắn.

### Implementation for User Story 3

- [X] T016 [US3] Bổ sung single-item fallback và guardrail “không quét LAN mù” trong `core/domain/src/main/java/dev/anilbeesetti/nextplayer/core/domain/ResolvePlaybackQueueUseCase.kt`
- [X] T017 [US3] Giữ precedence của external `video_list` và raw network URL behavior trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/utils/PlayerApi.kt` và `app/src/main/java/dev/anilbeesetti/nextplayer/navigation/MediaNavGraph.kt`
- [X] T018 [US3] Thêm feedback khi không còn item theo hướng Next/Prev trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerActivity.kt`, `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/MediaPlayerScreen.kt`, và `core/ui/src/main/res/values/strings.xml`

**Checkpoint**: User Story 3 hoàn thành khi mọi phiên thiếu ngữ cảnh vẫn phát an toàn và không điều hướng sai.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Chốt tài liệu triển khai, verification, và closeout workflow

- [X] T019 [P] Cập nhật luồng verify local/LAN/raw URL/external playlist trong `specs/003-lan-playback-optimization/quickstart.md`
- [X] T020 [P] Ghi workflow triển khai, lệnh verify, và lưu ý fallback vào `memory_bank/wf_20260517_lan_playback_optimization.md`
- [X] T021 Chạy verification commands đã chốt trong `specs/003-lan-playback-optimization/quickstart.md` và cập nhật trạng thái hoàn thành vào `specs/003-lan-playback-optimization/tasks.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Bắt đầu ngay, không phụ thuộc phase khác
- **Foundational (Phase 2)**: Phụ thuộc Phase 1, chặn toàn bộ user stories
- **US1 (Phase 3)**: Phụ thuộc Phase 2, là MVP của feature
- **US2 (Phase 4)**: Phụ thuộc Phase 2 và nên triển khai sau khi US1 đã chốt cơ chế hydrate queue bất đồng bộ
- **US3 (Phase 5)**: Phụ thuộc Phase 2; dùng kết quả precedence/hydration từ US1-US2 để chốt fallback và boundary behavior
- **Polish (Phase 6)**: Phụ thuộc các user stories mong muốn đã xong

### User Story Dependencies

- **User Story 1 (P1)**: Có thể bắt đầu ngay sau Foundational
- **User Story 2 (P2)**: Phụ thuộc US1 để tái sử dụng đường hydrate queue đã tồn tại
- **User Story 3 (P3)**: Phụ thuộc US2 để chốt fallback trên resolver/player flow hoàn chỉnh

### Within Each User Story

- Launcher/context wiring trước player bootstrap
- Resolver/preference precedence trước queue apply
- Queue apply trước boundary feedback
- Verification và docs sau khi hành vi runtime đã ổn định

### Parallel Opportunities

- Setup: `T001` và `T002` có thể chạy song song
- Foundational: `T003` và `T004` có thể chạy song song
- US1: `T008` và `T009` có thể song song tới trước bước `T010`
- Polish: `T019` và `T020` có thể chạy song song

---

## Parallel Example: User Story 1

```text
T008: Thêm helper playback context trong feature/videopicker/.../MediaPickerViewModel.kt
T009: Truyền playback context trong MediaPickerScreen.kt và app/.../MediaNavGraph.kt
```

## Parallel Example: Foundational Phase

```text
T003: Tạo core playback queue models trong core/model/...
T004: Tạo feature-player hydration state models trong feature/player/.../model/...
```

## Parallel Example: Polish Phase

```text
T019: Cập nhật quickstart verify flow trong specs/003-lan-playback-optimization/quickstart.md
T020: Ghi workflow closeout trong memory_bank/wf_20260517_lan_playback_optimization.md
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Hoàn thành Setup (Phase 1)
2. Hoàn thành Foundational (Phase 2)
3. Hoàn thành User Story 1 (Phase 3)
4. Dừng lại và xác minh open latency của video LAN cùng khả năng phản hồi điều khiển cơ bản

### Incremental Delivery

1. Setup + Foundational
2. US1 -> mở video LAN nhanh, queue hydrate nền
3. US2 -> Next/Prev đúng thứ tự local/LAN
4. US3 -> fallback an toàn, raw URL không điều hướng sai, feedback biên rõ ràng
5. Polish -> verify và closeout workflow

### Suggested MVP Scope

- **MVP đề xuất**: Chỉ cần hoàn thành **User Story 1**
- **Lý do**: Đây là phần giải quyết trực tiếp điểm đau chính “mở video LAN chậm”, đồng thời tạo nền queue hydration cho các story tiếp theo

---

## Notes

- Tất cả task đều theo đúng checklist format `- [ ] Txxx ...`
- Không thêm test-first tasks riêng vì spec không yêu cầu TDD bắt buộc
- `PlayerActivity.kt`, `PlayerViewModel.kt`, `PlayerApi.kt`, `ResolvePlaybackQueueUseCase.kt`, và `PlayerService.kt` là các file nóng; tránh chạy song song nhiều task cùng sửa một file
