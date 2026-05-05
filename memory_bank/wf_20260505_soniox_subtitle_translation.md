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
### Phase 5: US3 - Bilingual (T024)
### Phase 6: US4 - Provisional (T030-T032)
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

## Files UI mới (feature:player)

| File | Mô tả |
|------|--------|
| `ui/SubtitleOverlay.kt` | Composable overlay 3 chế độ, provisional text |

## Lệnh cần chạy

```powershell
# Build debug:
cd d:\Projects\Canhan\nextplayer
.\gradlew.bat :app:assembleDebug
```

## Ghi chú
- INTERNET permission đã có trong AndroidManifest.xml
- Module core:subtitle là NEW MODULE
- SubtitleAudioProcessor được inject vào PlayerService nhưng chưa thêm vào ExoPlayer audio pipeline (cần thêm vào `ExoPlayer.Builder` qua custom RenderersFactory hoặc `setAudioProcessors`)
- Phases 4-8 (Settings UI, Bilingual mode, Provisional display, Session Stability, Polish) chưa được implement
