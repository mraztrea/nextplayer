# Tasks: Tối Ưu Hiệu Suất Video Player

**Input**: Design documents từ `/specs/004-player-performance-optimize/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, quickstart.md ✅

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Có thể chạy song song (khác file, không phụ thuộc nhau)
- **[Story]**: User story liên quan (US1, US2, US3, US4)
- Bao gồm đường dẫn file chính xác

## Scope đã điều chỉnh sau Research

| User Story | Status | Lý do |
|------------|--------|-------|
| US1 - Startup nhanh | ✅ Implement | LoadControl tối ưu |
| US2 - Audio formats | ✅ Đã có (nextlib-media3ext) | Chỉ cần verify + logging |
| US3 - Giảm tài nguyên | ✅ Implement | WakeMode + buffer tối ưu |
| US4 - Preferred codec | ⏳ Deferred (P3) | Phức tạp, ít người cần |

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Tạo enum và utility cơ bản cần thiết cho tất cả user stories

- [ ] T001 [P] Tạo enum `MediaSourceType` với companion `fromUri()` trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/model/MediaSourceType.kt`
- [ ] T002 [P] Tạo data class `LoadControlConfig` chứa buffer parameters cho mỗi `MediaSourceType` trong `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/model/LoadControlConfig.kt`

**Checkpoint**: Shared models sẵn sàng — có thể bắt đầu US1 và US3 song song

---

## Phase 2: User Story 1 — Khởi động phát video nhanh hơn (Priority: P1) 🎯 MVP

**Goal**: Giảm thời gian từ khi chọn video đến khung hình đầu tiên hiển thị, đặc biệt cho video local

**Independent Test**: Đo thời gian startup bằng logcat timestamp (`onRenderedFirstFrame` - `onCreate`), so sánh trước/sau với video 1080p local

**Acceptance Criteria**:
- Video local 1080p: first frame < 1s
- Video 4K HEVC: first frame < 2s
- Chuyển video trong playlist: < 1.5s

### Tasks

- [ ] T003 [US1] Thêm `DefaultLoadControl` vào `ExoPlayer.Builder` trong method `onCreate()` tại `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/service/PlayerService.kt` (line 579). Sử dụng `LoadControlConfig.forLocal()` với: `minBufferMs=15000`, `maxBufferMs=50000`, `bufferForPlaybackMs=500`, `bufferForPlaybackAfterRebufferMs=1000`
- [ ] T004 [US1] Thêm import `DefaultLoadControl` và `MediaSourceType` vào `PlayerService.kt`
- [ ] T005 [US1] Thêm logging vào `playbackStateListener.onRenderedFirstFrame()` (line 310) để log thời gian startup: `Log.d("PlayerPerf", "First frame rendered in ${System.currentTimeMillis() - startTimeMs}ms")`

**Checkpoint US1**: Startup time có thể đo được qua logcat. Build & test video local.

---

## Phase 3: User Story 2 — Phát được nhiều định dạng audio hơn (Priority: P2)

**Goal**: Xác nhận FFmpeg audio decoder đã hoạt động, thêm logging cho debug

**Independent Test**: Phát video mẫu chứa DTS/TrueHD/ALAC/Opus, kiểm tra audio phát bình thường

**⚠️ Note**: `nextlib-media3ext` (`NextRenderersFactory`) đã tích hợp FFmpeg audio decoder. US2 chủ yếu là **verify** và **cải thiện error handling**.

### Tasks

- [ ] T006 [P] [US2] Thêm xử lý `onPlayerError()` vào `playbackStateListener` trong `PlayerService.kt` để log chi tiết khi codec error xảy ra: loại error, codec name, format không hỗ trợ
- [ ] T007 [US2] Xác nhận `setEnableDecoderFallback(true)` (line 562) đã được set — verify rằng khi hardware decoder fail, ExoPlayer tự động fallback sang FFmpeg decoder

**Checkpoint US2**: Error logging hoạt động, decoder fallback được xác nhận.

---

## Phase 4: User Story 3 — Giảm tiêu thụ tài nguyên hệ thống (Priority: P2)

**Goal**: Quản lý wake lock thông minh dựa trên loại source media (local vs network)

**Independent Test**: Dùng `adb shell dumpsys power` kiểm tra wake lock khi phát video local (không có network lock) vs video stream (có network lock)

**Acceptance Criteria**:
- Video local: `C.WAKE_MODE_LOCAL` (partial wake lock, KHÔNG có wifi lock)
- Video stream: `C.WAKE_MODE_NETWORK` (partial wake lock + wifi lock)
- RAM usage giảm ≥15% cho video 1080p local (so sánh qua Android Profiler)

### Tasks

- [ ] T008 [US3] Thêm wake mode detection trong `onMediaItemTransition()` (line 124) tại `PlayerService.kt`. Detect URI scheme → gọi `ExoPlayer.setWakeMode()` với `C.WAKE_MODE_LOCAL` hoặc `C.WAKE_MODE_NETWORK`
- [ ] T009 [US3] Thêm import `MediaSourceType` vào `PlayerService.kt` (nếu chưa có từ T004)

**Checkpoint US3**: Wake mode được set đúng loại. Verify bằng `adb shell dumpsys power`.

---

## Phase 5: User Story 4 — Cho phép chọn codec ưu tiên (Priority: P3) ⏳ DEFERRED

**Goal**: Người dùng nâng cao có thể chọn hardware decoder cụ thể

**Status**: **DEFERRED** — Đã có `DecoderPriority` (DEVICE_ONLY / PREFER_DEVICE / PREFER_APP) đáp ứng 95% use cases. Preferred codec cụ thể (theo tên) sẽ implement khi có yêu cầu cụ thể.

**Lý do defer**: 
- Cần custom `RenderersFactory` phức tạp (filter codec by name)
- Cần UI liệt kê available codecs trên thiết bị
- Ít người dùng cần tính năng này

*(Tasks sẽ được tạo khi un-defer)*

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Đảm bảo tất cả thay đổi ổn định và đo lường được

- [ ] T010 Build project và verify không có compilation errors
- [ ] T011 Phát video local 1080p, 4K HEVC — verify không stutter, first frame nhanh
- [ ] T012 Phát video chứa audio DTS, TrueHD — verify audio phát bình thường (đã có sẵn)
- [ ] T013 Phát video từ network stream (http/smb) — verify wake mode đúng loại
- [ ] T014 Force close FFmpeg library (disable extension renderer) — verify app vẫn hoạt động với hardware decoder

---

## Dependencies

```
T001, T002 (Setup)
    │
    ├── T003 → T004 → T005 (US1: LoadControl → Import → Logging)
    │
    ├── T006, T007 (US2: Error handling, verify — ĐỘC LẬP)
    │
    └── T008 → T009 (US3: WakeMode → Import)
         
T010 → T011 → T012 → T013 → T014 (Polish: Build → Test sequentially)
```

## Parallel Execution Opportunities

| Group | Tasks | Lý do có thể song song |
|-------|-------|----------------------|
| Setup | T001, T002 | Khác file, không phụ thuộc |
| US1 + US2 | T003-T005, T006-T007 | US1 sửa builder, US2 sửa listener — khác vùng code |
| US2 + US3 | T006-T007, T008-T009 | US2 là listener, US3 là onMediaItemTransition — khác method |

## Implementation Strategy

1. **MVP (US1)**: Chỉ implement LoadControl tối ưu → đo startup time → ship nếu tốt
2. **Increment 1 (US2+US3)**: Thêm error logging + WakeMode → ship
3. **Future (US4)**: Implement khi có user request cụ thể

**Ước tính effort**: 
- Phase 1 (Setup): ~30 phút
- Phase 2 (US1): ~1 giờ
- Phase 3 (US2): ~30 phút
- Phase 4 (US3): ~30 phút
- Phase 5 (Polish): ~1 giờ
- **Tổng: ~3.5 giờ**
