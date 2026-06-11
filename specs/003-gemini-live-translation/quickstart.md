# Quickstart: Gemini Live Realtime Subtitle Translation

## Prerequisites

- Android project builds on Windows with `pwsh`.
- Existing Soniox subtitle feature is present in `core:subtitle`.
- Google API Key with access to Gemini Live Translation.
- Test video with clear speech.

## Setup

### 1. Review plan artifacts

```powershell
Get-Content specs/003-gemini-live-translation/plan.md
Get-Content specs/003-gemini-live-translation/research.md
Get-Content specs/003-gemini-live-translation/data-model.md
```

### 2. Implement provider extension

Use `core:subtitle` as the owning module:

- Add provider selection model.
- Add Gemini session config/client/parser.
- Extend secure key storage for Google API Key.
- Reuse player audio processor and reset lifecycle.
- Keep Soniox behavior intact.

### 3. Update Settings UI

In `feature:settings` subtitle preferences:

- Add provider selector: Soniox / Gemini Live.
- Use one provider-aware API Key input: Soniox loads/saves the Soniox key slot, Gemini Live loads/saves the Google key slot.
- Add target language selector with `vn` -> `vi`, `en` -> `en`.
- Keep display mode selector with 3 modes.

### 4. Update Player UI

In `feature:player`:

- Let user start live subtitles with selected provider.
- Show active provider/status/error notice.
- Keep overlay behavior consistent across providers.
- Ensure seek/media-change resets Gemini session and overlay.

### 5. Verify after implementation

```powershell
.\gradlew :core:subtitle:testDebugUnitTest :feature:player:compileDebugKotlin :feature:settings:compileDebugKotlin :core:subtitle:ktlintCheck :feature:player:ktlintCheck :feature:settings:ktlintCheck
```

## Manual Test Flow

1. Open app settings.
2. Select Gemini Live as subtitle provider.
3. Enter Google API Key.
4. Select target language `vn` or Vietnamese.
5. Open a video with clear speech.
6. Enable realtime subtitle.
7. Confirm translated subtitle appears within 3 seconds.
8. Switch display mode: Chỉ bản dịch, Song ngữ, Chỉ bản gốc.
9. Seek to another position and confirm old subtitle disappears within 1 second.
10. Disable network and confirm video continues while Gemini error is shown.
11. Confirm provider does not auto-switch to Soniox.

## Privacy Check

After a Gemini subtitle session, inspect logs and confirm they contain no:

- raw audio
- input transcript
- output transcript
- Google API Key
- Soniox API Key

## Implementation Notes

- Gemini Live setup/audio JSON uses `GeminiLiveMessageBuilder` and `kotlinx.serialization.json`.
- Gemini WebSocket endpoint uses `google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent` with API key query auth.
- Gemini audio batching uses 100 ms PCM chunks; Soniox keeps the existing 200 ms batches.
- Player overlay uses `SubtitleSegment.displayText` for Gemini segments and preserves Soniox rendering behavior.
- Manual live verification still requires a real Google API key and a clear-speech video on device/emulator.
