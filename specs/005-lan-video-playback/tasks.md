# Tasks: Xem video trong mạng LAN

**Input**: Design documents from `specs/005-lan-video-playback/`  
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

## Format: `[ID] [P?] [Story] Description`

- `[P]`: Task có thể chạy song song nếu không phụ thuộc task chưa hoàn thành và không sửa cùng file.
- `[US1]`, `[US2]`, `[US3]`: Task thuộc user story tương ứng.
- Setup/Foundation/Polish không dùng story label.

## Path Conventions

Android multi-module repository paths are relative to repository root. Use PowerShell commands through `rtk` as required by `AGENTS.md`.

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Add `smbj = "0.14.0"` and `androidxSecurityCrypto = "1.1.0"` versions plus library aliases in `gradle/libs.versions.toml`
- [ ] T002 Register the new `:core:lan` module in `settings.gradle.kts`
- [ ] T003 Create the Android library build configuration for LAN access in `core/lan/build.gradle.kts`
- [ ] T004 Create the module manifest with namespace-safe defaults in `core/lan/src/main/AndroidManifest.xml`
- [ ] T005 Add `:core:lan` dependencies to data, media, player, and videopicker modules in `core/data/build.gradle.kts`, `core/media/build.gradle.kts`, `feature/player/build.gradle.kts`, and `feature/videopicker/build.gradle.kts`
- [ ] T006 Add network access permissions needed for SMB LAN access in `app/src/main/AndroidManifest.xml`
- [ ] T007 [P] Add LAN module ProGuard/R8 keep notes for SMBJ classes in `core/lan/consumer-rules.pro`
- [ ] T008 [P] Add LAN feature string resource placeholders in `feature/videopicker/src/main/res/values/strings.xml`
- [ ] T009 [P] Create a workflow note skeleton for implementation closeout in `memory_bank/wf_20260519_lan_video_playback.md`

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T010 [P] Create LAN domain result and error types in `core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/LanResult.kt`
- [ ] T011 [P] Create `LanServerProfile` model in `core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/LanServerProfile.kt`
- [ ] T012 [P] Create `LanFolder` model in `core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/LanFolder.kt`
- [ ] T013 [P] Create `LanMediaItem` model in `core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/LanMediaItem.kt`
- [ ] T014 [P] Create `LanFolderBookmark` model in `core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/LanFolderBookmark.kt`
- [ ] T015 [P] Create `LanThumbnailCacheEntry` model in `core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/LanThumbnailCacheEntry.kt`
- [ ] T016 Create the LAN repository contracts in `core/data/src/main/java/dev/anilbeesetti/nextplayer/core/data/repository/LanRepository.kt`
- [ ] T017 [P] Create reusable LAN test fixtures in `core/lan/src/test/java/dev/anilbeesetti/nextplayer/core/lan/testing/LanFixtures.kt`

## Phase 3: User Story 1 - Quản lý server LAN đã lưu (Priority: P1)

**Goal**: Người dùng có thể thêm, lưu, xem, sửa và xóa SMB server profile; password không hiển thị rõ và server còn sau khi mở lại app.

**Independent Test**: Thêm server mới, đóng/mở lại app, xác nhận server vẫn xuất hiện và có thể chỉnh sửa hoặc xóa.

### Tests for User Story 1

- [ ] T018 [P] [US1] Add DAO persistence tests for server profile CRUD in `core/database/src/androidTest/java/dev/anilbeesetti/nextplayer/core/database/dao/LanServerDaoTest.kt`
- [ ] T019 [P] [US1] Add credential store tests for save, update, clear, and masked state in `core/lan/src/test/java/dev/anilbeesetti/nextplayer/core/lan/security/EncryptedLanCredentialStoreTest.kt`
- [ ] T020 [P] [US1] Add repository tests for server validation and duplicate prevention in `core/data/src/test/java/dev/anilbeesetti/nextplayer/core/data/repository/LocalLanServerRepositoryTest.kt`
- [ ] T021 [P] [US1] Add ViewModel tests for add, edit, delete, and validation errors in `feature/videopicker/src/test/java/dev/anilbeesetti/nextplayer/feature/videopicker/lan/LanServerManagerViewModelTest.kt`

### Implementation for User Story 1

- [ ] T022 [P] [US1] Create `LanServerEntity` in `core/database/src/main/java/dev/anilbeesetti/nextplayer/core/database/entities/LanServerEntity.kt`
- [ ] T023 [P] [US1] Create `LanServerDao` in `core/database/src/main/java/dev/anilbeesetti/nextplayer/core/database/dao/LanServerDao.kt`
- [ ] T024 [US1] Register LAN server table and migration in `core/database/src/main/java/dev/anilbeesetti/nextplayer/core/database/MediaDatabase.kt`
- [ ] T025 [P] [US1] Create credential store contract and AndroidX Security Crypto implementation in `core/lan/src/main/java/dev/anilbeesetti/nextplayer/core/lan/security/LanCredentialStore.kt`
- [ ] T026 [US1] Implement server profile repository in `core/data/src/main/java/dev/anilbeesetti/nextplayer/core/data/repository/LocalLanServerRepository.kt`
- [ ] T027 [US1] Bind LAN repositories and credential store with Hilt in `core/data/src/main/java/dev/anilbeesetti/nextplayer/core/data/DataModule.kt`
- [ ] T028 [P] [US1] Create server management use cases in `core/domain/src/main/java/dev/anilbeesetti/nextplayer/core/domain/lan/ManageLanServersUseCases.kt`
- [ ] T029 [US1] Implement server manager state and actions in `feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/lan/LanServerManagerViewModel.kt`
- [ ] T030 [US1] Build server list and form UI with masked password behavior in `feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/lan/LanServerManagerScreen.kt`
- [ ] T031 [US1] Add Vietnamese labels and validation messages in `feature/videopicker/src/main/res/values/strings.xml`
- [ ] T032 [US1] Add LAN server manager navigation route in `feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/navigation/LanNavigation.kt`
- [ ] T033 [US1] Add LAN entry point from the media picker surface in `feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/screens/mediapicker/MediaPickerScreen.kt`

## Phase 4: User Story 2 - Duyệt thư mục và phát video LAN như local (Priority: P1)

**Goal**: Người dùng mở server hợp lệ, duyệt thư mục SMB từ share/root ban đầu, thấy folder/video với thumbnail hoặc placeholder, và phát video qua player hiện có.

**Independent Test**: Mở server hợp lệ, đi vào thư mục có video, xác nhận danh sách hiển thị, thumbnail/placeholder xuất hiện, và chọn video để phát.

### Tests for User Story 2

- [ ] T034 [P] [US2] Add SMB client contract tests for list folder, auth error, offline error, and permission error in `core/lan/src/test/java/dev/anilbeesetti/nextplayer/core/lan/smb/SmbLanClientTest.kt`
- [ ] T035 [P] [US2] Add folder browsing use case tests for root, child path, empty folder, and non-video filtering in `core/domain/src/test/java/dev/anilbeesetti/nextplayer/core/domain/lan/BrowseLanFolderUseCaseTest.kt`
- [ ] T036 [P] [US2] Add thumbnail cache tests for lazy load, cache hit, cache invalidation, and failure placeholder in `core/media/src/test/java/dev/anilbeesetti/nextplayer/core/media/LanThumbnailCacheTest.kt`
- [ ] T037 [P] [US2] Add player SMB data source tests for open, read, seek, and read interruption in `feature/player/src/test/java/dev/anilbeesetti/nextplayer/feature/player/lan/SmbMediaDataSourceTest.kt`
- [ ] T038 [P] [US2] Add LAN folder browser ViewModel tests for loading, loaded, empty, and error states in `feature/videopicker/src/test/java/dev/anilbeesetti/nextplayer/feature/videopicker/lan/LanFolderBrowserViewModelTest.kt`

### Implementation for User Story 2

- [ ] T039 [P] [US2] Implement SMBJ-backed client adapter in `core/lan/src/main/java/dev/anilbeesetti/nextplayer/core/lan/smb/SmbLanClient.kt`
- [ ] T040 [US2] Implement folder listing and typed error mapping in `core/data/src/main/java/dev/anilbeesetti/nextplayer/core/data/repository/LocalLanFolderRepository.kt`
- [ ] T041 [P] [US2] Create folder browse and refresh use cases in `core/domain/src/main/java/dev/anilbeesetti/nextplayer/core/domain/lan/BrowseLanFolderUseCase.kt`
- [ ] T042 [US2] Implement LAN folder browser state and actions in `feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/lan/LanFolderBrowserViewModel.kt`
- [ ] T043 [US2] Build folder/video list UI with loading, empty, and error states in `feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/lan/LanFolderBrowserScreen.kt`
- [ ] T044 [US2] Reuse local video row styling for LAN media items in `feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/composables/LanVideoItem.kt`
- [ ] T045 [P] [US2] Create thumbnail cache entity and DAO in `core/database/src/main/java/dev/anilbeesetti/nextplayer/core/database/entities/LanThumbnailCacheEntity.kt` and `core/database/src/main/java/dev/anilbeesetti/nextplayer/core/database/dao/LanThumbnailCacheDao.kt`
- [ ] T046 [US2] Implement lazy thumbnail cache service in `core/media/src/main/java/dev/anilbeesetti/nextplayer/core/media/LanThumbnailCache.kt`
- [ ] T047 [US2] Register LAN thumbnail fetcher or decoder with Coil image loading in `app/src/main/java/dev/anilbeesetti/nextplayer/VideoThumbnailDecoder.kt`
- [ ] T048 [P] [US2] Create LAN playback descriptor and URI builder in `core/model/src/main/java/dev/anilbeesetti/nextplayer/core/model/LanPlaybackDescriptor.kt`
- [ ] T049 [US2] Implement SMB Media3 data source integration in `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/lan/SmbMediaDataSource.kt`
- [ ] T050 [US2] Wire SMB playback data source into player media creation in `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/service/PlayerService.kt`
- [ ] T051 [US2] Pass selected LAN video and sibling list from picker to player in `app/src/main/java/dev/anilbeesetti/nextplayer/navigation/MediaNavGraph.kt`
- [ ] T052 [US2] Add Vietnamese folder browsing and playback error messages in `feature/videopicker/src/main/res/values/strings.xml`

## Phase 5: User Story 3 - Bookmark thư mục LAN đang xem (Priority: P2)

**Goal**: Người dùng bookmark thư mục LAN đang xem, mở lại bookmark sau khi đóng/mở app, và xử lý rõ bookmark trùng hoặc không còn hợp lệ.

**Independent Test**: Mở một thư mục LAN, tạo bookmark, đóng/mở lại app, chọn bookmark và xác nhận thư mục đó mở trực tiếp.

### Tests for User Story 3

- [ ] T053 [P] [US3] Add bookmark DAO tests for create, duplicate prevention, delete, and server deletion behavior in `core/database/src/androidTest/java/dev/anilbeesetti/nextplayer/core/database/dao/LanFolderBookmarkDaoTest.kt`
- [ ] T054 [P] [US3] Add bookmark repository tests for open success, target missing, and unavailable server in `core/data/src/test/java/dev/anilbeesetti/nextplayer/core/data/repository/LocalLanBookmarkRepositoryTest.kt`
- [ ] T055 [P] [US3] Add bookmark ViewModel tests for add current folder, open bookmark, delete, and duplicate feedback in `feature/videopicker/src/test/java/dev/anilbeesetti/nextplayer/feature/videopicker/lan/LanBookmarkViewModelTest.kt`

### Implementation for User Story 3

- [ ] T056 [P] [US3] Create `LanFolderBookmarkEntity` in `core/database/src/main/java/dev/anilbeesetti/nextplayer/core/database/entities/LanFolderBookmarkEntity.kt`
- [ ] T057 [P] [US3] Create `LanFolderBookmarkDao` in `core/database/src/main/java/dev/anilbeesetti/nextplayer/core/database/dao/LanFolderBookmarkDao.kt`
- [ ] T058 [US3] Register bookmark table, relation cleanup, and migration in `core/database/src/main/java/dev/anilbeesetti/nextplayer/core/database/MediaDatabase.kt`
- [ ] T059 [US3] Implement bookmark repository methods in `core/data/src/main/java/dev/anilbeesetti/nextplayer/core/data/repository/LocalLanBookmarkRepository.kt`
- [ ] T060 [P] [US3] Create bookmark use cases in `core/domain/src/main/java/dev/anilbeesetti/nextplayer/core/domain/lan/ManageLanBookmarksUseCases.kt`
- [ ] T061 [US3] Implement bookmark list and actions state in `feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/lan/LanBookmarkViewModel.kt`
- [ ] T062 [US3] Build bookmark list UI and open/delete actions in `feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/lan/LanBookmarkScreen.kt`
- [ ] T063 [US3] Add bookmark current folder action to folder browser UI in `feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/lan/LanFolderBrowserScreen.kt`
- [ ] T064 [US3] Add Vietnamese bookmark duplicate and invalid-target messages in `feature/videopicker/src/main/res/values/strings.xml`

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T065 [P] Add LAN quickstart manual validation results to `memory_bank/wf_20260519_lan_video_playback.md`
- [ ] T066 [P] Document SMB server setup assumptions and troubleshooting in `specs/005-lan-video-playback/quickstart.md`
- [ ] T067 Run targeted compile verification and record command output summary in `memory_bank/wf_20260519_lan_video_playback.md`
- [ ] T068 Run targeted unit and android tests and record command output summary in `memory_bank/wf_20260519_lan_video_playback.md`
- [ ] T069 Run full debug assemble verification and record command output summary in `memory_bank/wf_20260519_lan_video_playback.md`
- [ ] T070 Run `rtk .\gradlew ktlintCheck` and fix only LAN feature formatting issues under `core/lan/src/main/java`, `core/data/src/main/java`, `core/domain/src/main/java`, `core/media/src/main/java`, `feature/videopicker/src/main/java`, and `feature/player/src/main/java`
- [ ] T071 Review all user-facing Vietnamese strings for consistency in `feature/videopicker/src/main/res/values/strings.xml`
- [ ] T072 Run GitNexus change detection before commit and record affected scope in `memory_bank/wf_20260519_lan_video_playback.md`
- [ ] T073 Update implementation status for completed tasks in `specs/005-lan-video-playback/tasks.md`

## Dependencies & Execution Order

### Phase Dependencies

| Phase | Depends On | Can Start When |
|-------|------------|----------------|
| Phase 1 Setup | None | Immediately |
| Phase 2 Foundation | Phase 1 | LAN module/dependencies exist |
| Phase 3 US1 | Phase 2 | Shared models/contracts exist |
| Phase 4 US2 | Phase 2 and server repository slice from US1 | Saved server can provide SMB profile/credential |
| Phase 5 US3 | Phase 2 and folder browser slice from US2 | Current LAN folder can be identified |
| Phase 6 Polish | US1, US2, US3 | Feature slices are implemented |

### User Story Dependencies

| Story | Priority | Dependency | Independent Value |
|-------|----------|------------|-------------------|
| US1 | P1 | Foundation | Lưu, sửa, xóa server SMB đã cấu hình |
| US2 | P1 | Foundation + usable server profile from US1 | Duyệt thư mục và phát video LAN |
| US3 | P2 | Folder browser from US2 | Mở nhanh thư mục LAN đã bookmark |

### Parallel Opportunities

- Setup tasks T007, T008, T009 can run after T001-T006 are assigned because they touch different files.
- Foundation model tasks T010-T015 can run in parallel.
- US1 tests T018-T021 can run in parallel after model contracts are defined.
- US2 tests T034-T038 can run in parallel after LAN contracts are defined.
- US3 tests T053-T055 can run in parallel after bookmark model/entity names are agreed.
- UI string additions and domain/use-case implementation can run in parallel when they do not edit the same files.

## Parallel Example: User Story 1

```text
Task group A: T018, T019, T020, T021
Task group B: T022, T023, T025, T028
Integration sequence: T024 -> T026 -> T027 -> T029 -> T030 -> T031 -> T032 -> T033
```

## Parallel Example: User Story 2

```text
Task group A: T034, T035, T036, T037, T038
Task group B: T039, T041, T045, T048
Integration sequence: T040 -> T042 -> T043 -> T044 -> T046 -> T047 -> T049 -> T050 -> T051 -> T052
```

## Parallel Example: User Story 3

```text
Task group A: T053, T054, T055
Task group B: T056, T057, T060
Integration sequence: T058 -> T059 -> T061 -> T062 -> T063 -> T064
```

## Implementation Strategy

### MVP First

MVP kỹ thuật tối thiểu là Phase 1 + Phase 2 + US1. MVP người dùng có thể xem video LAN cần thêm US2 vì US1 chỉ cung cấp quản lý server.

### Incremental Delivery

1. Hoàn thành US1 để server SMB và credential lưu bền vững.
2. Hoàn thành US2 để có đường duyệt folder, thumbnail lazy cache và playback.
3. Hoàn thành US3 để tối ưu thao tác mở lại thư mục thường xem.
4. Chạy Phase 6 để xác nhận build/test/manual quickstart và ghi workflow note.

### Parallel Team Strategy

Một worker có thể làm data/Room/credential, một worker làm SMB client/domain, một worker làm UI videopicker, và một worker làm player/thumbnail. Không để hai worker sửa cùng file `MediaDatabase.kt`, `strings.xml`, `MediaPickerScreen.kt`, hoặc `PlayerService.kt` cùng lúc.

## Notes

- Không hỗ trợ DLNA/WebDAV/FTP trong feature này.
- Không tự quét LAN hoặc liệt kê toàn bộ share trong phiên bản đầu.
- Password không được log hoặc hiển thị rõ.
- Thumbnail loading không được chặn folder listing.
- Khi implement task sửa nhiều file code, cập nhật `memory_bank/wf_20260519_lan_video_playback.md`.
