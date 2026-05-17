# Data Model: Tối Ưu Hiệu Suất Video Player

**Branch**: `004-player-performance-optimize` | **Date**: 2026-05-17

## Entities

### PlayerPreferences (mở rộng entity hiện có)

Entity `PlayerPreferences` trong `core/model` đã tồn tại. Cần bổ sung:

| Field | Type | Default | Mô tả |
|-------|------|---------|-------|
| `preferredVideoCodec` | `String?` | `null` | Tên codec ưu tiên (deferred — P3) |

> **Lưu ý**: Không thêm field mới trong giai đoạn P1/P2 vì logic LoadControl và WakeMode được xác định tại runtime dựa trên URI scheme, không cần persist.

### MediaSourceType (entity mới — enum)

Xác định loại nguồn media để áp dụng cấu hình phù hợp.

| Value | Mô tả | WakeMode | Buffer Strategy |
|-------|-------|----------|-----------------|
| `LOCAL` | File nội bộ (`content://`, `file://`) | LOCAL | Low buffer |
| `NETWORK` | Stream (`http://`, `smb://`, `ftp://`) | NETWORK | Standard buffer |

### LoadControlConfig (entity mới — data class)

Tập hợp các tham số buffer cho từng loại source.

| Field | Type | LOCAL value | NETWORK value |
|-------|------|-------------|---------------|
| `minBufferMs` | `Int` | 15,000 | 50,000 |
| `maxBufferMs` | `Int` | 30,000 | 50,000 |
| `bufferForPlaybackMs` | `Int` | 500 | 2,500 |
| `bufferForPlaybackAfterRebufferMs` | `Int` | 1,000 | 5,000 |

## Relationships

```
PlayerPreferences --(has)--> DecoderPriority (enum, existing)
MediaSourceType --(determines)--> LoadControlConfig
MediaSourceType --(determines)--> WakeMode (C.WAKE_MODE_LOCAL or C.WAKE_MODE_NETWORK)
PlayerService --(uses)--> MediaSourceType (runtime detection)
```

## State Transitions

Không có state machine phức tạp. `MediaSourceType` được xác định **một lần** khi MediaItem được set, dựa trên URI scheme. Không thay đổi trong suốt quá trình playback.
