# Tasks: Soniox Real-time Subtitle Translation

**Input**: Design documents from `/specs/001-soniox-subtitle-translation/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Không có yêu cầu TDD trong spec — bỏ qua test tasks.

**Organization**: Tasks theo user story, cho phép triển khai và kiểm thử độc lập từng story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Có thể chạy song song (file khác nhau, không phụ thuộc)
- **[Story]**: User story liên quan (US1–US5)
- Bao gồm file paths chính xác

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Khởi tạo module mới và thêm dependencies

- [ ] T001 Thêm dependency `okhttp` và `androidx-security-crypto` vào `gradle/libs.versions.toml`
- [ ] T002 Tạo module `core:subtitle` — thêm `include(":core:subtitle")` vào `settings.gradle.kts` và tạo `core/subtitle/build.gradle.kts` với dependencies (okhttp, coroutines, hilt)
- [ ] T003 [P] Tạo cấu trúc thư mục `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/` với packages: `engine/`, `model/`, `session/`, `audio/`, `di/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Data models và enums dùng chung cho tất cả user stories

**⚠️ CRITICAL**: Phải hoàn thành trước khi bắt đầu bất kỳ user story nào

- [ ] T004 [P] Tạo enum `SegmentStatus` (ORIGINAL, TRANSLATED) trong `core/subtitle/src/main/java/.../subtitle/model/SegmentStatus.kt`
- [ ] T005 [P] Tạo enum `SubtitleDisplayMode` (ORIGINAL_ONLY, TRANSLATION_ONLY, BILINGUAL) trong `core/subtitle/src/main/java/.../subtitle/model/SubtitleDisplayMode.kt`
- [ ] T006 [P] Tạo data class `SubtitleSegment` (id, originalText, translationText, status, speaker, language, confidence, createdAt) trong `core/subtitle/src/main/java/.../subtitle/model/SubtitleSegment.kt`
- [ ] T007 [P] Tạo data class `SonioxSessionConfig` (apiKey, model, audioFormat, sampleRate, numChannels, sourceLanguage, targetLanguage, endpointDelayMs) trong `core/subtitle/src/main/java/.../subtitle/model/SonioxSessionConfig.kt`
- [ ] T008 [P] Tạo enum `SubtitleEngineStatus` (IDLE, CONNECTING, ACTIVE, RECONNECTING, ERROR, STOPPED) trong `core/subtitle/src/main/java/.../subtitle/model/SubtitleEngineStatus.kt`
- [ ] T009 Tạo Hilt DI module `SubtitleModule.kt` trong `core/subtitle/src/main/java/.../subtitle/di/SubtitleModule.kt` — cung cấp OkHttpClient, CoroutineScope

**Checkpoint**: Models và DI sẵn sàng — có thể bắt đầu implement user stories

---

## Phase 3: User Story 1 — Phụ đề Dịch Thời gian Thực khi Xem Video (Priority: P1) 🎯 MVP

**Goal**: Người dùng xem video có lời thoại tiếng nước ngoài, phụ đề dịch xuất hiện tự động trên overlay

**Independent Test**: Mở video có lời thoại → bật live subtitle → phụ đề dịch xuất hiện trên overlay trong vòng 3 giây

### Implementation for User Story 1

- [ ] T010 [US1] Implement `SonioxWebSocketClient` trong `core/subtitle/src/main/java/.../subtitle/engine/SonioxWebSocketClient.kt` — OkHttp WebSocket, gửi config JSON khi onOpen, gửi binary audio frame, keepalive 15s, reconnect với exponential backoff
- [ ] T011 [US1] Implement `SonioxTokenParser` trong `core/subtitle/src/main/java/.../subtitle/engine/SonioxTokenParser.kt` — parse JSON token stream, phân loại theo `is_final` + `translation_status`, emit callbacks (onOriginal, onTranslation, onProvisional)
- [ ] T012 [US1] Implement `SubtitleSessionManager` trong `core/subtitle/src/main/java/.../subtitle/session/SubtitleSessionManager.kt` — FIFO queue ghép original↔translation, display buffer (trimmable), session log (non-trimmable), cleanup stale segments (>10s hoặc >3 pending)
- [ ] T013 [US1] Implement `AudioBatcher` trong `core/subtitle/src/main/java/.../subtitle/audio/AudioBatcher.kt` — nhận PCM data, batch thành ~200ms (6400 bytes), resample nếu cần (48kHz→16kHz, stereo→mono), output s16le
- [ ] T014 [US1] Implement `SubtitleAudioProcessor` trong `core/subtitle/src/main/java/.../subtitle/audio/SubtitleAudioProcessor.kt` — ExoPlayer `AudioProcessor` implementation, tap vào audio pipeline, chuyển PCM cho AudioBatcher
- [ ] T015 [US1] Implement interface `SubtitleEngine` và class `SubtitleEngineImpl` trong `core/subtitle/src/main/java/.../subtitle/engine/SubtitleEngine.kt` — orchestrate WebSocket + TokenParser + SessionManager + AudioBatcher, expose StateFlow<SubtitleEngineStatus>
- [ ] T016 [US1] Tạo `SubtitleOverlay` Composable trong `feature/player/src/main/java/.../player/ui/SubtitleOverlay.kt` — render phụ đề trên video, hỗ trợ TRANSLATION_ONLY mode, provisional text kiểu italic/mờ, nền bán trong suốt
- [ ] T017 [US1] Sửa `PlayerViewModel.kt` trong `feature/player/src/main/java/.../player/PlayerViewModel.kt` — inject SubtitleEngine, quản lý start/stop theo playback state, expose subtitle segments cho UI
- [ ] T018 [US1] Sửa `PlayerService.kt` trong `feature/player/src/main/java/.../player/service/PlayerService.kt` — inject `SubtitleAudioProcessor` vào ExoPlayer audio pipeline, đồng bộ playback state/position cho subtitle engine
- [ ] T019 [US1] Tích hợp `SubtitleOverlay` và toggle `Live Subtitle` vào `MediaPlayerScreen.kt` trong `feature/player/src/main/java/.../player/MediaPlayerScreen.kt` — hiển thị overlay khi live subtitle đang active

**Checkpoint**: US1 hoàn thành — có thể xem video với phụ đề dịch thời gian thực (mode TRANSLATION_ONLY)

---

## Phase 4: User Story 2 — Cấu hình Soniox API, Ngôn ngữ và Chế độ Hiển thị (Priority: P1)

**Goal**: Người dùng cấu hình Soniox API Key, ngôn ngữ nguồn/đích, và chế độ hiển thị trước khi bật live subtitle

**Independent Test**: Vào Settings → Subtitle → nhập API key, đổi ngôn ngữ/chế độ hiển thị → đóng/mở lại app → khởi động phụ đề → cấu hình được giữ và áp dụng đúng

### Implementation for User Story 2

- [x] T020 [P] [US2] Mở rộng `PlayerPreferences` trong `core/model/src/main/java/.../model/PlayerPreferences.kt` — thêm fields: sourceLanguage, targetLanguage, displayMode, endpointDelayMs, liveSubtitleEnabled
- [x] T021 [P] [US2] Mở rộng `PlayerPreferencesDataSource` trong `core/datastore/src/main/java/.../datastore/datasource/PlayerPreferencesDataSource.kt` — thêm read/write cho translation preferences mới
- [x] T022 [US2] Sửa `SubtitlePreferencesScreen.kt` trong `feature/settings/src/main/java/.../settings/screens/subtitle/SubtitlePreferencesScreen.kt` — thêm section "Translation": API key field, dropdown chọn source/target language, radio buttons chọn display mode
- [x] T023 [US2] Sửa `SubtitlePreferencesViewModel.kt` trong `feature/settings/src/main/java/.../settings/screens/subtitle/SubtitlePreferencesViewModel.kt` — expose và update translation preferences qua DataStore
- [x] T025 [US2] Cập nhật `SubtitleEngineImpl` — đọc TranslationPreferences khi khởi tạo session, build SonioxSessionConfig từ preferences
- [x] T026 [P] [US2] Tạo `SecureApiKeyStorage` trong `core/subtitle/src/main/java/.../subtitle/storage/SecureApiKeyStorage.kt` — sử dụng EncryptedSharedPreferences để lưu/đọc/xóa API key
- [x] T027 [US2] Bổ sung logic UI/API cho API Key trong `SubtitlePreferencesScreen.kt` và `SubtitlePreferencesViewModel.kt` — toggle hiện/ẩn, validate action, trạng thái valid/invalid
- [x] T028 [US2] Implement API Key validation trong `SonioxWebSocketClient.kt` — thử kết nối nhanh, phân biệt lỗi invalid key / subscription / rate limit
- [x] T029 [US2] Cập nhật `SubtitleEngineImpl` — kiểm tra API key validity trước khi start session (FR-016), hiển thị lỗi rõ ràng nếu key thiếu/invalid

**Checkpoint**: US2 hoàn thành — người dùng cấu hình và lưu được API key/ngôn ngữ/chế độ hiển thị, phiên subtitle khởi động với cấu hình hợp lệ

## Phase 5: User Story 3 — Hiển thị Phụ đề Song ngữ (Priority: P2)

**Goal**: Người dùng học ngôn ngữ có thể xem đồng thời câu gốc và câu dịch, với placeholder rõ ràng khi bản dịch chưa đến

**Independent Test**: Chọn mode song ngữ → phát video có lời thoại → cột/vùng gốc và dịch hiển thị đồng thời; khi câu gốc đã final nhưng câu dịch chưa tới thì hiển thị `...`

### Implementation for User Story 3

- [x] T024 [US3] Cập nhật `SubtitleOverlay.kt` — hỗ trợ cả 3 chế độ hiển thị: ORIGINAL_ONLY, TRANSLATION_ONLY, BILINGUAL; trong BILINGUAL hiển thị placeholder `...` khi original đã final nhưng translation chưa đến

**Checkpoint**: US3 hoàn thành — overlay song ngữ hiển thị đúng original, translation, và trạng thái đang chờ dịch

---

## Phase 6: User Story 4 — Phản hồi Trực quan (Provisional Text) (Priority: P2)

**Goal**: Người dùng thấy phản hồi trực quan ngay lập tức (<500ms) khi hệ thống đang nhận dạng lời nói

**Independent Test**: Bắt đầu nói → văn bản tạm thời (italic/mờ) xuất hiện trong <500ms → khi final → text chuyển sang style bình thường

### Implementation for User Story 4

- [x] T030 [US4] Cập nhật `SubtitleOverlay.kt` — thêm hiển thị provisional text với animation fade-in, style italic + opacity thấp, tách biệt visual với final segments
- [x] T031 [US4] Cập nhật `SubtitleSessionManager` — quản lý provisional text state riêng biệt, xóa provisional khi nhận được final segment hoặc `<end>` token
- [x] T032 [US4] Cập nhật `PlayerViewModel` — expose provisional text state cho UI, cập nhật liên tục khi có token mới

**Checkpoint**: US4 hoàn thành — provisional text hiển thị mượt mà với visual khác biệt

---

## Phase 7: User Story 5 — Ổn định Phiên dài (Priority: P3)

**Goal**: Tính năng phụ đề hoạt động ổn định ≥2 tiếng không gián đoạn

**Independent Test**: Bật phụ đề → phát video 2+ tiếng → không có lỗi kết nối, không rò rỉ bộ nhớ, không gián đoạn

### Implementation for User Story 5

- [x] T033 [US5] Implement `SessionResetScheduler` trong `core/subtitle/src/main/java/.../subtitle/session/SessionResetScheduler.kt` — make-before-break session reset mỗi 3 phút, carry-over context ≤500 ký tự, không gián đoạn người dùng
- [x] T034 [US5] Cập nhật `SubtitleSessionManager` — implement display buffer trimming (giữ N segments gần nhất), session log không trim, quản lý bộ nhớ cho phiên dài
- [x] T035 [US5] Cập nhật `SonioxWebSocketClient` — tích hợp SessionResetScheduler, xử lý graceful close + reconnect khi reset, exponential backoff cho network errors

**Checkpoint**: US5 hoàn thành — phiên subtitle ổn định 2+ tiếng

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Xử lý edge cases, seek/pause behavior, và hoàn thiện

- [ ] T036 [P] Xử lý seek: Khi người dùng tua video → xóa subtitle buffer + reset Soniox session + đồng bộ audio tap tới vị trí mới — sửa `PlayerService.kt`, `PlayerViewModel.kt`, và `SubtitleEngineImpl`
- [ ] T037 [P] Xử lý pause/resume: Khi video tạm dừng → tạm dừng gửi audio, keepalive duy trì kết nối — sửa `PlayerService.kt`, `SubtitleAudioProcessor.kt`, và `SonioxWebSocketClient.kt`
- [ ] T038 [P] Xử lý audio track switch: Phát hiện thay đổi audio track → reset subtitle session — sửa `PlayerService.kt`
- [ ] T039 [P] Xử lý lỗi kết nối: Thông báo user-friendly khi mất mạng, auto-reconnect khi có mạng — sửa `SubtitleOverlay.kt` (hiển thị trạng thái)
- [ ] T040 Thêm permission `INTERNET` (nếu chưa có) vào `app/src/main/AndroidManifest.xml`
- [ ] T041 Cập nhật ProGuard rules cho OkHttp trong `app/proguard-rules.pro`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Không phụ thuộc — bắt đầu ngay
- **Foundational (Phase 2)**: Phụ thuộc Phase 1 — BLOCKS tất cả user stories
- **US1 (Phase 3)**: Phụ thuộc Phase 2 — core pipeline, MVP
- **US2 (Phase 4)**: Phụ thuộc Phase 2 — có thể triển khai song song với US1, nhưng validation end-to-end cần engine từ US1
- **US3 (Phase 5)**: Phụ thuộc US1 + US2 (cần overlay cơ bản từ US1 và display mode từ US2)
- **US4 (Phase 6)**: Phụ thuộc US1 (mở rộng overlay + session manager)
- **US5 (Phase 7)**: Phụ thuộc US1 (mở rộng WebSocket + session manager)
- **Polish (Phase 8)**: Phụ thuộc US1 hoàn thành

### Within Each User Story

- Models/Enums trước Services
- Services trước UI components
- Engine logic trước Player integration
- Core implementation trước integration

### Parallel Opportunities

- Phase 2: T004–T008 đều chạy song song ([P])
- US1: T010 + T011 song song (WebSocket + TokenParser không phụ thuộc nhau)
- US1: T013 + T014 song song (AudioBatcher + AudioProcessor)
- Phase 4: T020 + T021 + T026 song song (model + datastore + secure storage)

---

## Parallel Example: User Story 1

```
# Launch song song — các module không phụ thuộc:
T010: SonioxWebSocketClient.kt
T011: SonioxTokenParser.kt

# Sau khi T010 + T011 xong:
T012: SubtitleSessionManager.kt (cần TokenParser callbacks)

# Song song:
T013: AudioBatcher.kt
T014: SubtitleAudioProcessor.kt

# Orchestrator (cần tất cả ở trên):
T015: SubtitleEngineImpl.kt

# UI (cần Engine):
T016: SubtitleOverlay.kt
T017: PlayerViewModel.kt (inject Engine)
T018: PlayerService.kt (inject AudioProcessor)
T019: MediaPlayerScreen.kt (integrate Overlay + toggle)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup → thêm dependencies, tạo module
2. Phase 2: Foundational → models, enums, DI
3. Phase 3: US1 → full pipeline audio→WebSocket→parser→overlay
4. **STOP và VALIDATE**: Mở video → bật subtitle → phụ đề dịch xuất hiện
5. Deploy/demo nếu sẵn sàng

### Incremental Delivery

1. Setup + Foundational → Nền tảng sẵn sàng
2. US1 → Test → **MVP sẵn sàng** (phụ đề dịch hoạt động)
3. US2 → Test → API key + ngôn ngữ + chế độ hiển thị
4. US3 → Test → Overlay song ngữ
5. US4 → Test → Provisional text
6. US5 → Test → Phiên ổn định 2+ tiếng
7. Polish → Edge cases + seek/pause/error handling

---

## Notes

- [P] tasks = file khác nhau, không phụ thuộc
- [Story] label map task vào user story cụ thể
- Mỗi user story có thể kiểm thử độc lập
- Commit sau mỗi task hoặc nhóm logic
- Dừng tại checkpoint bất kỳ để validate story
- Path `.../subtitle/` = `dev/anilbeesetti/nextplayer/core/subtitle/`
