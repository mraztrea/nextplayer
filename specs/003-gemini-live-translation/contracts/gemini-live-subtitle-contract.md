# Contract: Gemini Live Subtitle Provider

## Scope

Contract này mô tả ranh giới hành vi giữa player/settings UI, subtitle engine và Gemini Live provider trong app Android. Đây không phải public web API.

## Provider Selection Contract

### Inputs

- `provider`: `SONIOX` hoặc `GEMINI_LIVE`
- `targetLanguage`: alias UI hoặc BCP-47 code
- `displayMode`: `TRANSLATION_ONLY`, `BILINGUAL`, `ORIGINAL_ONLY`
- `enabled`: bật/tắt phụ đề realtime

### Rules

- Người dùng chọn provider thủ công.
- Gemini Live lỗi không được tự động chuyển sang Soniox.
- Provider selection phải hiển thị rõ provider đang active.
- Nếu chọn Gemini Live nhưng thiếu Google API Key, trạng thái phải là `NOT_CONFIGURED`.

## Gemini Session Contract

### Start Preconditions

- Google API Key đã được lưu trong secure storage.
- Target language map được sang BCP-47 code hợp lệ.
- Audio source là media item đang phát trong app.
- Không có session provider khác đang active.

### Audio Input

- Format: PCM 16-bit little-endian, 16 kHz, mono.
- Chunk duration target: 100 ms.
- Source: player audio pipeline only.
- Forbidden: microphone, AudioPlaybackCapture, audio từ app khác.

### Transcript Output

Provider phải phát ra events đủ để engine dựng overlay:

```text
GeminiLiveTranscriptEvent
├── eventType: INPUT_TRANSCRIPT | OUTPUT_TRANSCRIPT | STATUS | ERROR
├── text: transcript text when applicable
├── isFinal: true | false
├── languageCode: BCP-47 language code
├── errorCode: provider/app error code when applicable
└── receivedAt: timestamp
```

### Display Mapping

- `TRANSLATION_ONLY`: overlay dùng output transcript.
- `BILINGUAL`: overlay dùng input transcript + output transcript.
- `ORIGINAL_ONLY`: overlay dùng input transcript.
- Same-target-language input: overlay vẫn phải có transcript/phụ đề bằng ngôn ngữ đích.

## Lifecycle Contract

### Reset Triggers

- Seek trong cùng media item.
- Đổi media item.
- Player stop/release.
- User tắt phụ đề realtime.
- User đổi provider.
- User đổi target language.
- Audio processor flush.
- Lỗi không thể recover.

### Reset Behavior

- Clear provisional text.
- Clear stale display segments.
- Drop partial audio batch trước reset.
- Close hoặc rotate provider session.
- New session only processes audio after reset point.

## Error Contract

### Error Categories

- Missing Google API Key.
- Invalid API Key.
- No permission for realtime translation.
- Rate limit/quota exceeded.
- Unsupported target language.
- Network unavailable.
- Provider timeout or unavailable.

### Required UX Behavior

- Video playback must continue.
- User sees actionable message.
- Provider remains Gemini Live until user changes it.
- No transcript/audio content appears in logs.

## Privacy Contract

### Allowed Diagnostic Log Fields

- provider
- target language code
- display mode
- session status
- error category/code
- reset reason
- timestamp

### Forbidden Diagnostic Log Fields

- raw audio bytes
- input transcript text
- output transcript text
- Google API Key
- Soniox API Key
