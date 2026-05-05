# Workflow: Soniox Subtitle Translation Implementation

**Ngày bắt đầu**: 2026-05-05
**Feature**: `specs/001-soniox-subtitle-translation`

## Tiến độ

### Phase 1: Setup (T001-T003)
- [x] T001: Thêm dependencies vào libs.versions.toml
- [x] T002: Tạo module core:subtitle
- [x] T003: Tạo cấu trúc thư mục

### Phase 2: Foundational (T004-T009)
- [x] T004-T008: Tạo models và enums
- [x] T009: Tạo Hilt DI module

### Phase 3: US1 - MVP (T010-T019)
- [x] T010-T011: WebSocket + TokenParser
- [x] T012: SubtitleSessionManager
- [x] T013-T014: AudioBatcher + AudioProcessor
- [x] T015: SubtitleEngineImpl (interface + impl)
- [x] T016: SubtitleOverlay composable
- [x] T017: PlayerViewModel integration (inject SubtitleEngine, expose state)
- [x] T018: PlayerService integration (inject SubtitleAudioProcessor)
- [x] T019: MediaPlayerScreen integration (SubtitleOverlay + collectAsState)

### Phase 4: US2 - Settings (T020-T029)
- [x] T020: Mở rộng `PlayerPreferences` với source/target language, display mode, endpoint delay, live subtitle enabled, hasApiKeyConfigured
- [x] T021: Không cần API mới trong `PlayerPreferencesDataSource` vì DataStore đang đọc/ghi toàn bộ `PlayerPreferences`; các field mới persist qua serializer hiện có
- [x] T022-T023: Mở rộng `SubtitlePreferencesScreen` + `SubtitlePreferencesViewModel` với section Translation, field API key an toàn, dialog chọn source/target language, chọn display mode
- [x] T025: `SubtitleEngineImpl` tự đọc preferences + secure API key khi start session
- [x] T026: `SecureApiKeyStorage` đã có sẵn và nay được wiring vào settings/engine
- [x] T027-T029: Thêm validate/save/clear API key trong settings, validate trước khi start subtitle, và toast lỗi rõ ràng khi thiếu/invalid key
### Phase 5: US3 - Bilingual (T024)
- [x] T024: `SubtitleOverlay` đã hỗ trợ 3 mode hiển thị; fix thêm semantics cho `TRANSLATION_ONLY` để không fallback sang bản gốc, và `BILINGUAL` tiếp tục giữ placeholder `...` khi translation chưa tới

### Phase 6: US4 - Provisional (T030-T032)
- [x] T030: `SubtitleOverlay` đã hiển thị provisional bằng `AnimatedVisibility`, italic, opacity thấp, tách khỏi final segments
- [x] T031: `SubtitleSessionManager` quản lý provisional state riêng và nay xóa provisional khi nhận final translation hoặc `<end>` token
- [x] T032: `PlayerViewModel` đã expose `provisionalText` liên tục cho UI

### Phase 7: US5 - Session Stability (T033-T035)
### Phase 8: Polish (T036-T041)

## Files đã tạo mới (core:subtitle module)

| File | Mô tả |
|------|--------|
| `model/SegmentStatus.kt` | Enum trạng thái segment |
| `model/SubtitleDisplayMode.kt` | Enum chế độ hiển thị |
| `model/SubtitleEngineStatus.kt` | Enum trạng thái engine |
| `model/SubtitleSegment.kt` | Data class segment phụ đề |
| `model/SonioxSessionConfig.kt` | Data class cấu hình kết nối |
| `di/SubtitleModule.kt` | Hilt DI module (OkHttp, CoroutineScope, Engine binding) |
| `engine/SonioxWebSocketClient.kt` | WebSocket client với reconnect logic |
| `engine/SonioxTokenParser.kt` | Parse JSON token stream |
| `engine/SubtitleEngine.kt` | Interface + SubtitleEngineImpl orchestrator |
| `session/SubtitleSessionManager.kt` | FIFO queue, display buffer, provisional |
| `audio/AudioBatcher.kt` | Batch PCM 200ms, resample 48kHz→16kHz |
| `audio/SubtitleAudioProcessor.kt` | ExoPlayer AudioProcessor read-only tap |

## Files đã modify

| File | Thay đổi |
|------|----------|
| `gradle/libs.versions.toml` | +okhttp, +security-crypto |
| `settings.gradle.kts` | +core:subtitle module |
| `feature/player/build.gradle.kts` | +dependency core:subtitle |
| `feature/player/.../PlayerViewModel.kt` | +SubtitleEngine inject, state flows, toggle methods |
| `feature/player/.../PlayerService.kt` | +SubtitleAudioProcessor inject |
| `feature/player/.../MediaPlayerScreen.kt` | +SubtitleOverlay, collectAsStateWithLifecycle |
| `core/model/.../PlayerPreferences.kt` | +translation preference fields cho Soniox |
| `feature/settings/.../SubtitlePreferencesScreen.kt` | +Translation section, API key input, source/target/display mode dialogs |
| `feature/settings/.../SubtitlePreferencesViewModel.kt` | +secure API key flow, validate/save/clear, update translation prefs |
| `feature/settings/build.gradle.kts` | +dependency `:core:subtitle` |
| `core/subtitle/build.gradle.kts` | +dependency `:core:data` để engine đọc preferences |
| `core/subtitle/.../SubtitleEngine.kt` | +start từ preferences/storage, validate key trước connect |
| `core/subtitle/.../SubtitleDisplayMode.kt` | +helper map từ preference string |
| `feature/settings/.../LocalesHelper.kt` | +helper danh sách ngôn ngữ Soniox dùng mã 2 ký tự |
| `core/subtitle/.../SubtitleSessionManager.kt` | +clear provisional khi nhận final translation |
| `feature/player/.../ui/SubtitleOverlay.kt` | +translation-only semantics đúng, bilingual placeholder, provisional visual |
| `.gitignore` | +ignore patterns Kotlin/Java/universal còn thiếu |

## Files UI mới (feature:player)

| File | Mô tả |
|------|--------|
| `ui/SubtitleOverlay.kt` | Composable overlay 3 chế độ, provisional text |

## Lệnh cần chạy

```powershell
# Build debug:
cd d:\Projects\Canhan\nextplayer
.\gradlew.bat :app:assembleDebug

# Compile hẹp cho phase 4:
.\gradlew.bat :core:subtitle:compileDebugKotlin :feature:settings:compileDebugKotlin :feature:player:compileDebugKotlin

# Compile hẹp sau Phase 5-6:
.\gradlew.bat :core:subtitle:compileDebugKotlin :feature:player:compileDebugKotlin
```

## Ghi chú
- INTERNET permission đã có trong AndroidManifest.xml
- Module core:subtitle là NEW MODULE
- SubtitleAudioProcessor được inject vào PlayerService nhưng chưa thêm vào ExoPlayer audio pipeline (cần thêm vào `ExoPlayer.Builder` qua custom RenderersFactory hoặc `setAudioProcessors`)
- Phase 4-6 đã xong ở mức settings/runtime config + bilingual + provisional; Phase 7-8 (session stability, seek/pause, error UX đầy đủ) vẫn còn
- `MediaPlayerScreen` hiện đã đọc display mode đã lưu, nhưng chưa phát hiện call site UI nào đang gọi `toggleLiveSubtitle()`; cần rà lại phần player controls nếu muốn bật/tắt từ UI trong app
