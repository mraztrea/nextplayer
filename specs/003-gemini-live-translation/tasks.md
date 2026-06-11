# Tasks: Gemini Live Realtime Subtitle Translation

**Input**: Design documents from `specs/003-gemini-live-translation/`  
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/gemini-live-subtitle-contract.md`, `quickstart.md`

**Tests**: Included for provider contract, seek/reset regression, display-mode mapping, and privacy/logging criteria called out by the spec and quickstart.

## Phase 1: Setup

**Purpose**: Prepare the implementation workspace and record required impact analysis before code edits.

- [X] T001 Create implementation workflow note in `memory_bank/wf_20260611_gemini_live_translation.md`
- [X] T002 Run GitNexus impact analysis for `SubtitleEngine`, `SubtitlePreferencesViewModel`, `PlayerViewModel`, `SubtitleAudioProcessor`, and record findings in `memory_bank/wf_20260611_gemini_live_translation.md`
- [X] T003 [P] Create Gemini engine test fixture file in `core/subtitle/src/test/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/GeminiLiveTestFixtures.kt`
- [X] T004 [P] Create settings test directory and fixture file in `feature/settings/src/test/java/dev/anilbeesetti/nextplayer/settings/screens/subtitle/SubtitlePreferencesTestFixtures.kt`

---

## Phase 2: Foundational

**Purpose**: Add shared provider models, preferences, storage, and dependency wiring required by all user stories.

**CRITICAL**: No user story work should begin until this phase is complete.

- [X] T005 [P] Add `SubtitleProvider` enum in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/SubtitleProvider.kt`
- [X] T006 [P] Add `CredentialValidationState` enum in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/CredentialValidationState.kt`
- [X] T007 [P] Add Gemini session and transcript models in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/GeminiSessionConfig.kt`
- [X] T008 [P] Add `GeminiLiveError` and error category models in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/GeminiLiveError.kt`
- [X] T009 Extend `SubtitleSegment` with provider, media id, display text, language, and provisional/final metadata in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/SubtitleSegment.kt`
- [X] T010 Add provider selection and Gemini preferences fields in `core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/PlayerPreferences.kt`
- [X] T011 Verify player preferences persistence for provider, Gemini target language, and display mode in `core/datastore/src/main/java/dev/anilbeesetti/nextplayer/core/datastore/serializer/PlayerPreferencesSerializer.kt`
- [X] T012 Verify player preference data accessors for provider and Gemini settings in `core/datastore/src/main/java/dev/anilbeesetti/nextplayer/core/datastore/datasource/PlayerPreferencesDataSource.kt`
- [X] T013 Extend secure provider key slots for Google and Soniox keys in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/storage/SecureApiKeyStorage.kt`
- [X] T014 Add shared provider abstraction in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/RealtimeSubtitleProvider.kt`
- [X] T015 Verify Hilt bindings for provider factory/storage dependencies in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/di/SubtitleModule.kt`

**Checkpoint**: Shared models, persistence, storage, and DI compile without changing user-visible behavior.

---

## Phase 3: User Story 1 - Phụ đề Dịch Realtime khi Xem Video (Priority: P1)

**Goal**: User can start Gemini Live realtime subtitles while a video plays and see translated subtitle overlay without interrupting playback.

**Independent Test**: Configure a valid Google key and target language, play a video with clear speech, enable Gemini Live subtitles, confirm translated subtitle appears while playback continues, then disable subtitles and confirm overlay/session stop.

### Tests for User Story 1

- [X] T016 [P] [US1] Add setup/audio message builder tests in `core/subtitle/src/test/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/GeminiLiveMessageBuilderTest.kt`
- [X] T017 [P] [US1] Add transcript parser tests for input/output transcript events in `core/subtitle/src/test/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/GeminiLiveTranscriptParserTest.kt`
- [X] T018 [P] [US1] Add display-mode mapping tests for translation-only, bilingual, and original-only segments in `core/subtitle/src/test/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/GeminiSubtitleProviderTest.kt`

### Implementation for User Story 1

- [X] T019 [US1] Implement Gemini setup and realtime audio JSON message builder in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/GeminiLiveMessageBuilder.kt`
- [X] T020 [US1] Implement Gemini transcript event parser in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/GeminiLiveTranscriptParser.kt`
- [X] T021 [US1] Implement OkHttp Gemini Live WebSocket client in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/GeminiLiveWebSocketClient.kt`
- [X] T022 [US1] Implement Gemini provider adapter that maps transcript events to subtitle segments in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/GeminiSubtitleProvider.kt`
- [X] T023 [US1] Update `SubtitleEngine` to delegate start/stop/status/segment flows by selected provider in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SubtitleEngine.kt`
- [X] T024 [US1] Update audio batching to support Gemini 100 ms chunk target while preserving Soniox behavior in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/audio/AudioBatcher.kt`
- [X] T025 [US1] Update player start/stop flow to pass selected provider into subtitle engine in `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerViewModel.kt`
- [X] T026 [US1] Update subtitle overlay rendering for Gemini `displayText` and provisional/final state in `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/SubtitleOverlay.kt`
- [X] T027 [US1] Record US1 manual verification result in `memory_bank/wf_20260611_gemini_live_translation.md`

**Checkpoint**: US1 is independently testable with a preconfigured Google key and target language.

---

## Phase 4: User Story 2 - Cấu hình Google API Key và Ngôn ngữ Đích (Priority: P1)

**Goal**: User can choose Soniox or Gemini Live, enter a Google API Key, choose Vietnamese/English target language, persist settings, and reopen app with values retained.

**Independent Test**: Open subtitle settings, select Gemini Live, enter Google API Key, select Vietnamese or English, save, restart app, and confirm provider/key state/language/display mode are retained.

### Tests for User Story 2

- [X] T028 [P] [US2] Add provider/key/language persistence tests in `feature/settings/src/test/java/dev/anilbeesetti/nextplayer/settings/screens/subtitle/SubtitlePreferencesViewModelTest.kt`
- [X] T029 [P] [US2] Add language alias mapping tests for `vn -> vi` and `en -> en` in `core/subtitle/src/test/java/dev/anilbeesetti/nextplayer/core/subtitle/model/GeminiLanguageMapperTest.kt`

### Implementation for User Story 2

- [X] T030 [US2] Implement Gemini language alias mapper in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/GeminiLanguageMapper.kt`
- [X] T031 [US2] Extend subtitle preferences UI state and events for provider, Google API Key, Gemini language, and display mode in `feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/subtitle/SubtitlePreferencesViewModel.kt`
- [X] T032 [US2] Add provider selector, Google API Key input, Gemini target language selector, and display mode controls in `feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/subtitle/SubtitlePreferencesScreen.kt`
- [X] T033 [US2] Add user-facing strings for Gemini provider, Google API Key, language aliases, and errors in `core/ui/src/main/res/values/strings.xml`
- [X] T034 [US2] Update settings navigation entry if needed for subtitle provider configuration in `feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/navigation/SubtitlePreferencesNavigation.kt`
- [X] T035 [US2] Update player UI to expose active provider selection or provider-aware subtitle toggle in `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/MediaPlayerScreen.kt`
- [X] T036 [US2] Record US2 manual persistence verification result in `memory_bank/wf_20260611_gemini_live_translation.md`

**Checkpoint**: US2 is independently testable from Settings without requiring a live video session.

---

## Phase 5: User Story 3 - Phụ đề Khớp Trạng thái Player (Priority: P2)

**Goal**: Seek, pause/resume, media change, and provider/language changes reset Gemini subtitle state so old text/audio batches cannot leak into the new playback position.

**Independent Test**: Enable Gemini subtitles, seek to another position, pause/resume, and switch media item; stale overlay disappears within 1 second and new subtitles only come from current audio.

### Tests for User Story 3

- [X] T037 [P] [US3] Add Gemini flush regression test in `core/subtitle/src/test/java/dev/anilbeesetti/nextplayer/core/subtitle/audio/SubtitleAudioProcessorTest.kt`
- [X] T038 [P] [US3] Add reset trigger tests for seek/media/provider/language changes in `core/subtitle/src/test/java/dev/anilbeesetti/nextplayer/core/subtitle/session/SubtitleSessionManagerTest.kt`

### Implementation for User Story 3

- [X] T039 [US3] Update session reset manager for Gemini provider reset triggers in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/session/SubtitleSessionManager.kt`
- [X] T040 [US3] Update reset scheduler to rotate or close Gemini sessions without keeping stale transcripts in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/session/SessionResetScheduler.kt`
- [X] T041 [US3] Ensure audio processor drops partial Gemini batches on flush/seek in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/audio/SubtitleAudioProcessor.kt`
- [X] T042 [US3] Ensure player service reset hook covers Gemini sessions on discontinuity/media change in `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/service/PlayerService.kt`
- [X] T043 [US3] Clear Gemini provisional and display segments on player stop/seek/provider switch in `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerViewModel.kt`
- [X] T044 [US3] Record US3 seek/media-change verification result in `memory_bank/wf_20260611_gemini_live_translation.md`

**Checkpoint**: US3 can be tested without changing settings UI once Gemini is configured.

---

## Phase 6: User Story 4 - Phản hồi Tạm thời và Lỗi Thân thiện (Priority: P2)

**Goal**: User sees processing/error states, video keeps playing on Gemini failures, provider does not auto-fallback, and diagnostic logs exclude audio/transcript.

**Independent Test**: Start Gemini subtitles, simulate missing key/network/provider errors, confirm actionable notices, no Soniox auto-switch, video playback continues, and logs contain only allowed metadata.

### Tests for User Story 4

- [X] T045 [P] [US4] Add Gemini error mapper tests for missing key, invalid key, quota, network, unsupported language, and timeout in `core/subtitle/src/test/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/GeminiLiveErrorMapperTest.kt`
- [X] T046 [P] [US4] Add diagnostic log redaction tests in `core/subtitle/src/test/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SubtitleDiagnosticLoggerTest.kt`
- [X] T047 [P] [US4] Add no-auto-fallback provider behavior tests in `core/subtitle/src/test/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SubtitleEngineProviderTest.kt`

### Implementation for User Story 4

- [X] T048 [US4] Implement Gemini error category mapper in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/GeminiLiveErrorMapper.kt`
- [X] T049 [US4] Extend subtitle engine status for not configured, connecting, translating, resetting, and Gemini error states in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/model/SubtitleEngineStatus.kt`
- [X] T050 [US4] Implement redacted diagnostic logging helper with metadata-only fields in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SubtitleDiagnosticLogger.kt`
- [X] T051 [US4] Update subtitle engine to keep selected Gemini provider on errors and prevent Soniox auto-fallback in `core/subtitle/src/main/java/dev/anilbeesetti/nextplayer/core/subtitle/engine/SubtitleEngine.kt`
- [X] T052 [US4] Update player notices for Gemini processing/error states in `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerViewModel.kt`
- [X] T053 [US4] Update player screen to show actionable Gemini notices without blocking playback in `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/MediaPlayerScreen.kt`
- [X] T054 [US4] Record US4 error/privacy verification result in `memory_bank/wf_20260611_gemini_live_translation.md`

**Checkpoint**: US4 can be tested with controlled error inputs and log inspection.

---

## Final Phase: Polish & Cross-Cutting Concerns

**Purpose**: Cross-story cleanup, docs, and verification.

- [X] T055 [P] Update quickstart implementation notes if code paths differ from plan in `specs/003-gemini-live-translation/quickstart.md`
- [X] T056 [P] Update task completion evidence in `memory_bank/wf_20260611_gemini_live_translation.md`
- [X] T057 Run Gradle verification command and record output summary in `memory_bank/wf_20260611_gemini_live_translation.md`
- [X] T058 Run `gitnexus_detect_changes()` and record affected symbols/flows in `memory_bank/wf_20260611_gemini_live_translation.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- Phase 1 Setup has no dependencies.
- Phase 2 Foundational depends on Phase 1.
- US1 depends on Phase 2.
- US2 depends on Phase 2 and can run in parallel with US1 after shared models/storage exist.
- US3 depends on US1 because reset behavior needs Gemini provider/session implementation.
- US4 depends on US1 and shared status/error models; some error mapper/logging tests can start after Phase 2.
- Final Phase depends on all user stories.

### User Story Dependencies

| Story | Priority | Depends On | Notes |
|-------|----------|------------|-------|
| US1 | P1 | Foundation | MVP runtime subtitle path; can use preconfigured key. |
| US2 | P1 | Foundation | Required for full user-facing configuration. |
| US3 | P2 | US1 | Validates player lifecycle for active Gemini sessions. |
| US4 | P2 | US1 | Adds robust UX/error/privacy behavior. |

### Suggested MVP Scope

Implement Phase 1, Phase 2, US2, then US1 for a usable end-user MVP: user configures Gemini, starts realtime subtitles, sees overlay, and can stop the session.

---

## Parallel Execution Examples

### US1

```text
After Phase 2:
- T016, T017, and T018 can be implemented in parallel.
- T019 and T020 can start in parallel after T016/T017 test expectations are clear.
- T025 and T026 can run in parallel after T023 exposes provider-aware state.
```

### US2

```text
After Phase 2:
- T028 and T029 can be implemented in parallel.
- T031 and T032 can run in parallel after T030 and preference fields exist.
- T033 can run independently from UI logic.
```

### US3

```text
After US1:
- T037 and T038 can be implemented in parallel.
- T039 and T040 can run in parallel after reset expectations are defined.
- T042 and T043 can run in parallel once engine reset API is stable.
```

### US4

```text
After US1:
- T045, T046, and T047 can be implemented in parallel.
- T048 and T050 can run in parallel.
- T052 and T053 can run in parallel after T049 and T051 are complete.
```

---

## Implementation Strategy

### MVP First

1. Complete Setup and Foundational tasks.
2. Complete US2 configuration so users can select Gemini and store a Google API Key.
3. Complete US1 runtime subtitle path.
4. Verify manual quickstart steps for configure -> play -> enable -> see subtitle -> disable.

### Incremental Delivery

1. Add US3 seek/media lifecycle hardening and regressions.
2. Add US4 error/privacy UX and log redaction.
3. Run full Gradle verification and record results in `memory_bank/wf_20260611_gemini_live_translation.md`.

### Verification Command

```powershell
.\gradlew :core:subtitle:testDebugUnitTest :feature:player:compileDebugKotlin :feature:settings:compileDebugKotlin :core:subtitle:ktlintCheck :feature:player:ktlintCheck :feature:settings:ktlintCheck
```
