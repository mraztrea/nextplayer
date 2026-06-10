# Tasks: Offline ASR Translation

**Input**: Design documents from `specs/003-offline-asr-translation/`  
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

## Format: `[ID] [P?] [Story] Description`

- `[P]` means the task can run in parallel with other tasks that touch different files.
- `[US1]` maps to the user story phase.
- Every implementation task includes an explicit file path.

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Create workflow note for multi-file implementation in `memory_bank/wf_20260610_offline_asr_translation.md`
- [X] T002 Verify Kotlin/Android ignore patterns cover Gradle and local model artifacts in `.gitignore`
- [X] T003 [P] Add offline subtitle strings in `core/ui/src/main/res/values/strings.xml`

## Phase 2: Foundational (Blocking Prerequisites)

- [X] T004 [P] [US2] Add offline model domain types in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/OfflineModel.kt`
- [X] T005 [P] [US2] Add manifest parser contract in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/storage/OfflineModelManifest.kt`
- [X] T006 [P] [US2] Add model preparation state types in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/ModelPreparationState.kt`
- [X] T007 [P] [US1] Add offline subtitle engine boundary in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/OfflineSubtitleEngine.kt`
- [X] T008 Add Hilt bindings for offline subtitle services in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/di/SubtitleModule.kt`
- [X] T009 Add offline subtitle preferences to `core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/PlayerPreferences.kt`
- [X] T010 Persist offline subtitle preferences in `core/datastore/src/main/java/dev/anilbeesetti/nextplayer/core/datastore/datasource/PlayerPreferencesDataSource.kt`

## Phase 3: User Story 1 - Bật phụ đề offline khi xem video (Priority: P1) MVP

**Goal**: Người dùng bật phụ đề offline khi model đã sẵn sàng và thấy transcript từ audio đang phát.

**Independent Test Criteria**: Với fake/prototype engine và model ready, bật offline subtitle không cần API key/Soniox và không tạo network session.

- [ ] T011 [P] [US1] Add unit tests for offline engine start/stop privacy behavior in `core/subtitle/src/test/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/OfflineSubtitleEngineTest.kt`
- [ ] T012 [US1] Implement prototype offline engine in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/PrototypeOfflineSubtitleEngine.kt`
- [ ] T013 [US1] Add subtitle runtime mode selection in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/SubtitleRuntimeMode.kt`
- [ ] T014 [US1] Integrate offline mode start/stop path in `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerViewModel.kt`
- [ ] T015 [US1] Ensure seek/media reset clears offline subtitle state in `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerViewModel.kt`

## Phase 4: User Story 2 - Chuẩn bị model bằng tải tự động hoặc import thủ công (Priority: P1)

**Goal**: Người dùng có thể chuẩn bị model bằng tải tự động hoặc import thủ công, với manifest hợp lệ và Wi-Fi mặc định.

**Independent Test Criteria**: Manifest thiếu capability hoặc ngôn ngữ bắt buộc bị reject; tải model yêu cầu xác nhận và mặc định Wi-Fi only.

- [ ] T016 [P] [US2] Add manifest validation unit tests in `core/subtitle/src/test/java/dev/anilbeesetti/nextplayer/core/subtitle/storage/OfflineModelManifestTest.kt`
- [ ] T017 [US2] Implement manifest validation in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/storage/OfflineModelManifest.kt`
- [ ] T018 [US2] Implement offline model repository skeleton in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/storage/OfflineModelRepository.kt`
- [ ] T019 [US2] Add model management UI state and events in `feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/subtitle/SubtitlePreferencesViewModel.kt`
- [ ] T020 [US2] Add offline model settings section in `feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/subtitle/SubtitlePreferencesScreen.kt`

## Phase 5: User Story 3 - Dịch transcript sang tiếng Việt hoặc tiếng Anh (Priority: P2)

**Goal**: Người dùng chọn tiếng Việt hoặc tiếng Anh làm ngôn ngữ đích và thấy bản dịch trong overlay.

**Independent Test Criteria**: Prototype engine phát ra segment tiếng Nhật có `translationText` đúng target language và overlay dùng các display mode hiện có.

- [ ] T021 [P] [US3] Add offline translation target constants in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/OfflineTranslationTarget.kt`
- [ ] T022 [US3] Emit prototype Japanese transcript with Vietnamese/English translation in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/PrototypeOfflineSubtitleEngine.kt`
- [ ] T023 [US3] Restrict settings target language choices for offline mode in `feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/subtitle/SubtitlePreferencesScreen.kt`

## Phase 6: User Story 4 - Duy trì phiên offline ổn định khi phát lâu (Priority: P3)

**Goal**: Phiên offline có trạng thái rõ ràng, reset sạch khi seek/pause/stop và không chặn player.

**Independent Test Criteria**: Stop/reset idempotent; state quay về idle sau stop hoặc seek.

- [ ] T024 [US4] Make prototype offline engine stop/reset idempotent in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/PrototypeOfflineSubtitleEngine.kt`
- [ ] T025 [US4] Add player error message fallback for missing model/offline failure in `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerViewModel.kt`

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T026 Update quickstart status and implementation notes in `specs/003-offline-asr-translation/quickstart.md`
- [ ] T027 Run `.\gradlew.bat testDebugUnitTest` and record result in `memory_bank/wf_20260610_offline_asr_translation.md`
- [ ] T028 Run `.\gradlew.bat assembleDebug` and record result in `memory_bank/wf_20260610_offline_asr_translation.md`

## Dependencies & Execution Order

### Phase Dependencies

- Phase 1 must complete before code changes.
- Phase 2 blocks all user stories.
- US1 and US2 are both P1; implement US2 foundation before enabling US1 in player because model readiness gates offline start.
- US3 depends on US1 engine boundary.
- US4 depends on US1 runtime lifecycle.

### User Story Dependencies

- US1: Requires T004-T010.
- US2: Requires T004-T010.
- US3: Requires US1 engine and settings state.
- US4: Requires US1 lifecycle path.

### Parallel Opportunities

- T003, T004, T005, T006, T007 can run in parallel after setup.
- T011 and T016 test files are independent.
- T021 can run in parallel with settings UI work once foundational models exist.

## Parallel Example: User Story 2

```powershell
# Independent files after foundational domain types exist:
# T016 tests manifest validation
# T019 view-model state
# T020 settings screen UI
```

## Implementation Strategy

### MVP First

Complete Phases 1-3 plus the minimal model-ready gate from Phase 4. This proves offline subtitle mode can start without Soniox/network and render prototype translated segments.

### Incremental Delivery

1. Model/manifest/state foundation.
2. Prototype offline engine with privacy invariant.
3. Settings model preparation UI.
4. Player runtime mode integration.
5. Native sherpa-onnx or whisper.cpp adapter behind `OfflineSubtitleEngine`.

## Notes

- Runtime native integration is intentionally behind the engine boundary; if the AAR/native binary is unavailable locally, keep prototype runtime buildable and document the native binding step.
- Do not send audio/transcript/translation over network in any offline session task.
