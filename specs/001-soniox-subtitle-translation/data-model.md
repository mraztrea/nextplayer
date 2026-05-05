# Data Model: Soniox Real-time Subtitle Translation

**Branch**: `001-soniox-subtitle-translation` | **Date**: 2026-05-05

## Entities

### SubtitleSegment

Đơn vị hiển thị phụ đề trên overlay.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Auto-increment ID nội bộ |
| `originalText` | `String` | Văn bản gốc đã chốt (final) |
| `translationText` | `String?` | Văn bản dịch, null nếu chưa dịch |
| `status` | `SegmentStatus` | `ORIGINAL` hoặc `TRANSLATED` |
| `speaker` | `String?` | Nhãn người nói (từ Soniox diarization) |
| `language` | `String?` | Mã ngôn ngữ gốc (ISO 639-1) |
| `confidence` | `Float?` | Điểm tin cậy trung bình (0.0–1.0) |
| `createdAt` | `Long` | Timestamp tạo (System.currentTimeMillis) |

**State transitions**:
```
[created with originalText] → status = ORIGINAL
[translationText assigned]  → status = TRANSLATED
[stale cleanup: >10s or >3 pending] → removed from display buffer
```

### SonioxSessionConfig

Cấu hình gửi lên Soniox khi mở WebSocket.

| Field | Type | Description |
|-------|------|-------------|
| `apiKey` | `String` | Soniox API Key |
| `model` | `String` | Luôn `"stt-rt-v4"` |
| `audioFormat` | `String` | Luôn `"pcm_s16le"` |
| `sampleRate` | `Int` | Luôn `16000` |
| `numChannels` | `Int` | Luôn `1` |
| `sourceLanguage` | `String?` | Mã ngôn ngữ nguồn (null = auto-detect) |
| `targetLanguage` | `String` | Mã ngôn ngữ đích (mặc định `"vi"`) |
| `endpointDelayMs` | `Int` | Mặc định `3000` |

### TranslationPreferences

Mở rộng preferences hiện có của nextplayer. Persist qua DataStore.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `sonioxApiKey` | `String` | `""` | API Key (encrypt riêng qua EncryptedSharedPreferences) |
| `sourceLanguage` | `String` | `"auto"` | Ngôn ngữ nguồn |
| `targetLanguage` | `String` | `"vi"` | Ngôn ngữ đích |
| `displayMode` | `SubtitleDisplayMode` | `TRANSLATION_ONLY` | Chế độ hiển thị |
| `endpointDelayMs` | `Int` | `3000` | Endpoint delay cho Soniox |
| `liveSubtitleEnabled` | `Boolean` | `false` | Bật/tắt tính năng |

## Enums

### SegmentStatus
```kotlin
enum class SegmentStatus {
    ORIGINAL,    // Đã có original, chưa có translation
    TRANSLATED   // Đã có cả original và translation
}
```

### SubtitleDisplayMode
```kotlin
enum class SubtitleDisplayMode {
    ORIGINAL_ONLY,     // Chỉ hiển thị bản gốc
    TRANSLATION_ONLY,  // Chỉ hiển thị bản dịch (mặc định)
    BILINGUAL          // Hiển thị cả hai
}
```

## Relationships

```
TranslationPreferences ──1:1──> SonioxSessionConfig (derived at runtime)
SonioxSession ──1:N──> SubtitleSegment (display buffer, trimmable)
SonioxSession ──1:N──> SubtitleSegment (session log, non-trimmable)
```

## Data Flow

```
ExoPlayer AudioProcessor
    ↓ PCM s16le 16kHz mono (batch 200ms)
SonioxWebSocketClient
    ↓ token stream (JSON messages)
SonioxTokenParser
    ↓ onOriginal / onTranslation / onProvisional callbacks
SubtitleSessionManager
    ↓ segments[] (display) + sessionLog[] (full)
SubtitleOverlay (Compose)
    ↓ rendered text on video
```
