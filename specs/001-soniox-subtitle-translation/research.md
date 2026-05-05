# Research: Soniox Real-time Subtitle Translation

**Branch**: `001-soniox-subtitle-translation` | **Date**: 2026-05-05

## R1: Trích xuất Audio từ Video trên Android (MediaExtractor/MediaCodec)

### Quyết định
Sử dụng `MediaExtractor` + `MediaCodec` để decode audio track trực tiếp từ file video, sau đó resample về 16 kHz mono PCM s16le.

### Lý do
- Không cần quyền `RECORD_AUDIO` hoặc `FOREGROUND_SERVICE_MEDIA_PROJECTION`.
- Chỉ lấy đúng audio track của video, không lẫn âm thanh hệ thống/notification.
- Hoạt động trên mọi Android từ API 16+ (minSdk hiện tại = 23).
- nextplayer đã dùng Media3/ExoPlayer — có thể tận dụng ExoPlayer renderer hoặc `AudioProcessor` để tap vào audio pipeline.

### Phương án thay thế đã xem xét
- **AudioPlaybackCapture**: Cần Android 10+ (API 29), cần quyền đặc biệt, bắt tất cả audio app. Loại bỏ.
- **ExoPlayer AudioProcessor custom**: Có thể inject AudioProcessor vào ExoPlayer pipeline để tap raw PCM trước khi render. Đây là phương án tối ưu nhất vì ExoPlayer đã decode sẵn audio và có thể cung cấp PCM trực tiếp.

### Quyết định cuối cùng
**Ưu tiên phương án ExoPlayer AudioProcessor** — inject một `TeeAudioProcessor` hoặc custom `AudioProcessor` vào ExoPlayer pipeline để nhận PCM đã decode mà không cần chạy `MediaExtractor` riêng. Nếu ExoPlayer API không cho phép tap dễ dàng, fallback sang `MediaExtractor` riêng biệt chạy song song.

## R2: WebSocket Client trên Android (Kotlin)

### Quyết định
Sử dụng `OkHttp WebSocket` — thư viện phổ biến nhất trên Android, hỗ trợ WebSocket natively.

### Lý do
- OkHttp là dependency rất phổ biến, nhẹ, và ổn định.
- Hỗ trợ binary frame (cần để gửi PCM audio).
- Connection management, reconnection dễ implement.
- nextplayer hiện chưa có OkHttp nhưng đây là dependency nhẹ, thêm vào không gây conflict.

### Phương án thay thế
- **Ktor Client WebSocket**: Nặng hơn, kéo thêm nhiều dependency.
- **Java-WebSocket**: Ít maintained hơn OkHttp.

## R3: Audio Resampling trên Android

### Quyết định
Sử dụng simple linear interpolation resampling hoặc `AudioProcessor` chain của ExoPlayer.

### Lý do
- ExoPlayer decode audio ở sample rate gốc của video (thường 44.1kHz hoặc 48kHz).
- Cần downsample về 16kHz mono.
- ExoPlayer có sẵn `SonicAudioProcessor` và `ChannelMixingAudioProcessor` cho các thao tác này.
- Nếu dùng custom AudioProcessor, có thể nhận PCM ở format bất kỳ và tự resample.

### Approach
- Dùng ExoPlayer `AudioProcessor` chain: `ChannelMixingAudioProcessor` (stereo→mono) + custom resample processor (48kHz→16kHz).
- Convert sang s16le (little-endian 16-bit signed integer).

## R4: Lưu trữ API Key an toàn trên Android

### Quyết định
Sử dụng `EncryptedSharedPreferences` từ AndroidX Security library.

### Lý do
- API Key là thông tin nhạy cảm, không nên lưu plaintext.
- `EncryptedSharedPreferences` mã hóa cả key và value bằng AES-256-GCM.
- Tương thích với Android 6.0+ (API 23) — đúng với minSdk = 23 của nextplayer.
- Đơn giản hơn Android Keystore trực tiếp.

### Phương án thay thế
- **Plain SharedPreferences/DataStore**: Không an toàn. Loại bỏ.
- **Android Keystore trực tiếp**: Quá phức tạp cho use case này. Loại bỏ.

## R5: Tích hợp vào kiến trúc nextplayer hiện có

### Phân tích cấu trúc hiện tại
```
Modules:
├── app                          # Application entry, navigation
├── core:common                  # Shared utilities
├── core:data                    # Repository implementations
├── core:database                # Room database
├── core:datastore               # DataStore preferences (AppPreferences, PlayerPreferences)
├── core:domain                  # Use cases
├── core:media                   # Media utilities
├── core:model                   # Data models/enums
├── core:ui                      # Shared UI components
├── feature:player               # Video player (PlayerActivity, ExoPlayer)
├── feature:settings             # Settings screens (có sẵn SubtitlePreferencesScreen)
└── feature:videopicker          # Video browser/picker
```

### Quyết định kiến trúc
Tạo module mới `core:subtitle` chứa toàn bộ logic Soniox (WebSocket, token parsing, FIFO queue, session management). Module này:
- Được sử dụng bởi `feature:player` để hiển thị overlay.
- Cấu hình được quản lý qua `core:datastore` (mở rộng `PlayerPreferences`).
- UI cài đặt mở rộng `SubtitlePreferencesScreen` hiện có trong `feature:settings`.

### Lý do
- Tách logic Soniox khỏi player giúp dễ test và maintain.
- Tuân theo kiến trúc multi-module hiện tại của nextplayer.
- `core:subtitle` không phụ thuộc vào UI — chỉ chứa business logic và data layer.
