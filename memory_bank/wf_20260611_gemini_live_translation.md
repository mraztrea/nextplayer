# Workflow: Gemini Live realtime subtitle translation

Ngày: 2026-06-11

## Phạm vi

Triển khai feature `specs/003-gemini-live-translation` theo `tasks.md`.

## Setup và impact

- Checklist `requirements.md`: PASS, 16/16 mục hoàn tất.
- `.gitignore`: đã bổ sung `.vscode/` theo setup verification.
- GitNexus impact analysis:
  - Lệnh MCP GitNexus với repo `nextplayer` trả lỗi `Repository "nextplayer" not found`.
  - `list_repos` chỉ có các repo: `crm.hri.com.vn`, `gonhanh_rust`, `hri.com.vn_botble`, `itnavi-web`, `recland-v2`, `recland-v4`, `travelup_crm`.
  - Do đó không có blast-radius GitNexus cho workspace này. Khi sửa code, sử dụng Serena symbol overview/search và Gradle tests để bù kiểm soát phạm vi.

## Lệnh kiểm thử dự kiến

```powershell
.\gradlew :core:subtitle:testDebugUnitTest :feature:player:compileDebugKotlin :feature:settings:compileDebugKotlin :core:subtitle:ktlintCheck :feature:player:ktlintCheck :feature:settings:ktlintCheck
```

## Điều chỉnh task theo repo thực tế

- `tasks.md` ban đầu đặt provider/Gemini preferences vào `ApplicationPreferences`.
- Code hiện hữu đang lưu toàn bộ live subtitle preferences trong `PlayerPreferences` (`sourceLanguage`, `targetLanguage`, `displayMode`, `endpointDelayMs`, `liveSubtitleEnabled`, `hasApiKeyConfigured`).
- Vì vậy T010-T012 được đổi sang `PlayerPreferences`, `PlayerPreferencesSerializer`, `PlayerPreferencesDataSource` để tránh tạo preference song song không được UI/player sử dụng.
- T015 ban đầu yêu cầu update Hilt bindings ngay ở Phase 2. Sau foundation, các model mới không cần DI binding và `SecureApiKeyStorage` vẫn constructor-injected `@Singleton`; provider binding cụ thể sẽ được thực hiện khi Gemini client/provider được thêm ở US1.

## Verification log

- `.\gradlew.bat :core:subtitle:compileDebugKotlin`: BUILD SUCCESSFUL sau T005-T014.
- US1 RED test run: `.\gradlew.bat :core:subtitle:testDebugUnitTest --tests "...GeminiLiveMessageBuilderTest" --tests "...GeminiLiveTranscriptParserTest" --tests "...GeminiSubtitleProviderTest"` failed at `:core:subtitle:compileDebugUnitTestKotlin` because Gemini builder/parser/provider classes are not implemented yet.
- US1 core GREEN test run: same targeted command passed 7 tests after adding Gemini message builder, transcript parser, WebSocket client, and provider segment mapper. Builder/parser use `kotlinx.serialization.json` so JVM unit tests avoid Android `org.json` stubs.
- US1 wiring verification: `.\gradlew.bat :core:subtitle:testDebugUnitTest :feature:player:compileDebugKotlin` BUILD SUCCESSFUL. Passed 9 subtitle unit tests and compiled player provider routing/overlay changes. Manual live verification with a real Google API key/audio was not run in this environment.
- US2 verification: `.\gradlew.bat :core:subtitle:testDebugUnitTest --tests "...GeminiLanguageMapperTest" :feature:settings:testDebugUnitTest --tests "...SubtitlePreferencesViewModelTest" :feature:settings:compileDebugKotlin :feature:player:compileDebugKotlin` BUILD SUCCESSFUL. Settings navigation did not need a route change because the existing subtitle settings screen now contains provider/key/language controls. Manual app restart persistence verification was not run in this environment.
- US3 verification: `.\gradlew.bat :core:subtitle:testDebugUnitTest --tests "...SubtitleAudioProcessorTest" --tests "...SubtitleSessionManagerTest" :feature:player:compileDebugKotlin` BUILD SUCCESSFUL. Added Gemini 100 ms flush regression, reset-state test, media transition reset hook, and provider/language/display change cleanup in `PlayerViewModel`. Manual seek/media-change playback verification was not run in this environment.
- US4 verification: `.\gradlew.bat :core:subtitle:testDebugUnitTest --tests "...GeminiLiveErrorMapperTest" --tests "...SubtitleDiagnosticLoggerTest" --tests "...SubtitleEngineProviderTest" :feature:player:compileDebugKotlin` BUILD SUCCESSFUL. Added Gemini error mapper, metadata-only diagnostic logger, no-auto-fallback provider test, status enum additions, and player notices via existing toast flow. Manual error simulation/log inspection was not run in this environment.
- Final verification: `.\gradlew.bat :core:subtitle:testDebugUnitTest :feature:player:compileDebugKotlin :feature:settings:compileDebugKotlin :core:subtitle:ktlintCheck :feature:player:ktlintCheck :feature:settings:ktlintCheck` BUILD SUCCESSFUL. Subtitle unit tests passed 22 tests.
- GitNexus detect changes: MCP call `detect_changes(repo="nextplayer", scope="all", worktree="D:\\Projects\\Canhan\\nextplayer")` failed because repository `nextplayer` is not indexed. Available repos remain `gonhanh_rust`, `recland-v2`, `crm.hri.com.vn`, `hri.com.vn_botble`, `itnavi-web`, `recland-v4`, `travelup_crm`.
