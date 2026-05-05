# Contract: Soniox WebSocket Protocol

**Branch**: `001-soniox-subtitle-translation` | **Date**: 2026-05-05

## Endpoint

```
wss://stt-rt.soniox.com/transcribe-websocket
```

## Handshake: Config Message (gửi ngay sau onOpen)

```json
{
  "api_key": "<string>",
  "model": "stt-rt-v4",
  "audio_format": "pcm_s16le",
  "sample_rate": 16000,
  "num_channels": 1,
  "enable_endpoint_detection": true,
  "max_endpoint_delay_ms": 3000,
  "enable_speaker_diarization": true,
  "enable_language_identification": true,
  "translation": {
    "type": "one_way",
    "target_language": "vi"
  },
  "language_hints": ["en"]
}
```

## Audio Data: Binary Frame

- Format: raw PCM s16le bytes
- Batch size: ~200ms = 6400 bytes (16000 Hz × 2 bytes × 0.2s)
- Gửi liên tục khi có audio

## Keepalive Message (mỗi 15 giây)

```json
{ "type": "keepalive" }
```

## Response: Token Stream

```json
{
  "tokens": [
    {
      "text": "<string>",
      "is_final": true|false,
      "translation_status": "original"|"translation"|"none",
      "speaker": "<string>|null",
      "language": "<string>|null",
      "confidence": 0.0-1.0|null
    }
  ]
}
```

### Token Classification

| translation_status | is_final | Ý nghĩa |
|-------------------|----------|----------|
| `"original"` | `true` | Văn bản gốc đã chốt → commit segment |
| `"original"` | `false` | Văn bản tạm thời → hiển thị provisional |
| `"translation"` | `true` | Bản dịch đã chốt → gắn vào segment FIFO |
| `"none"` | `true` | Original không trong ngôn ngữ chính → xử lý như original |
| `"none"` | `false` | Provisional ngoài ngôn ngữ chính |

### Special Token

- `text: "<end>"` → Kết thúc endpoint, xóa provisional display

## Error Handling

- WebSocket close code `4001`: Invalid API Key
- WebSocket close code `4002`: Rate limited
- Network timeout: Reconnect with exponential backoff (1s, 2s, 4s, max 30s)
