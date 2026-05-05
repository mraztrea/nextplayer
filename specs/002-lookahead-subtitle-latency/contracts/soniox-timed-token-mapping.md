# Contract: Soniox Timed Token Mapping

**Branch**: `002-lookahead-subtitle-latency` | **Date**: 2026-05-05

## Endpoint và cấu hình phiên

```text
wss://stt-rt.soniox.com/transcribe-websocket
```

Config message tiếp tục dùng cùng hợp đồng hiện tại:

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
  }
}
```

## Response fields dùng cho lookahead sync

```json
{
  "tokens": [
    {
      "text": "Hello",
      "start_ms": 600,
      "end_ms": 760,
      "confidence": 0.97,
      "is_final": true,
      "speaker": "1",
      "translation_status": "original",
      "language": "en"
    }
  ],
  "final_audio_proc_ms": 760,
  "total_audio_proc_ms": 880
}
```

## Mapping rules

### Original hoặc spoken token

- `translation_status = "original"` hoặc `"none"`
- Nếu `is_final = true`:
  - gom text vào original segment
  - lấy `start_ms` nhỏ nhất và `end_ms` lớn nhất của nhóm token làm `sourceStartMs/sourceEndMs`
  - tạo hoặc cập nhật `SubtitleSegment`
- Nếu `is_final = false`:
  - chỉ dùng cho provisional
  - không được render sớm hơn `playbackPosition`

### Translation token

- `translation_status = "translation"`
- Không có `start_ms/end_ms`
- Được ghép vào original segment chờ translation sớm nhất cùng `generationId`
- `targetStartMs/targetEndMs` kế thừa từ original segment tương ứng

### Endpoint token

- `text = "<end>"`
- Chốt provisional hiện tại
- Không tự sinh timing mới

## Timing truth

- Timing chuẩn để render lấy từ original/spoken tokens.
- Translation không tạo cửa sổ thời gian riêng.
- `final_audio_proc_ms` và `total_audio_proc_ms` dùng cho metric/debug, không thay thế `start_ms/end_ms` của segment.

## Error and fallback notes

- Nếu response thiếu `start_ms/end_ms` ở token gốc final, segment đó không được vào timed buffer chính; engine ghi nhận telemetry và có thể suy giảm an toàn.
- Token đến muộn từ generation cũ phải bị bỏ qua hoàn toàn.
