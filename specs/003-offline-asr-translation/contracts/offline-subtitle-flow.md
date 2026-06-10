# Contract: Offline Subtitle Flow

## Toggle Offline Subtitle

**Preconditions**:
- A model with valid manifest is installed and marked `Ready`.
- Current media item exposes decoded PCM to the subtitle audio pipeline.
- The selected target language is `vi` or `en`.

**Input**:
- `enabled`: true/false.
- `targetLanguage`: `vi` or `en`.
- `displayMode`: `OriginalOnly`, `TranslationOnly`, or `Bilingual`.

**Expected behavior**:
- When enabled, the session starts without creating a network connection.
- When disabled, the session stops and clears visible offline subtitle state.
- If no model is ready, the UI opens or links to model preparation state.

## Prepare Model

**Download flow**:
- Display model name and size before download.
- Require explicit confirmation.
- Default to Wi-Fi only.
- If currently on mobile data, require explicit override before starting.

**Import flow**:
- Accept only supported model package.
- Read and validate `offline-model-manifest.schema.json`.
- Copy model into app-specific storage only after validation succeeds.
- Do not mark model ready when required files or capabilities are missing.

## Runtime Privacy

**Invariant**:
- During an `OfflineSubtitleSession`, the app must not send audio, transcript, or translation outside the device.
- Download/update checks are outside the session lifecycle and must not run while a session is active unless explicitly initiated by the user after stopping the session.
