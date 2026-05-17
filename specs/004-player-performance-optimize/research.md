# Research: Tối Ưu Hiệu Suất Video Player

**Branch**: `004-player-performance-optimize` | **Date**: 2026-05-17

## R1: Phân Tích Tình Trạng Hiện Tại của ExoPlayer Builder

### Phát hiện

Player hiện tại (`PlayerService.kt:559-599`) đã có:
- ✅ `SubtitleAwareRenderersFactory` extends `NextRenderersFactory` (FFmpeg)
- ✅ `DecoderPriority` setting (DEVICE_ONLY / PREFER_DEVICE / PREFER_APP)
- ✅ `DefaultTrackSelector` với preferred audio/subtitle language
- ✅ Audio attributes, focus, noisy handling
- ✅ Autoplay, repeat mode, playback speed persistence

Chưa có (cần thêm):
- ❌ Custom `DefaultLoadControl` (đang dùng mặc định)
- ❌ `setWakeMode()` phân biệt local vs network
- ❌ Preferred codec selection (chỉ có decoder priority, không có specific codec name)
- ❌ Custom `setSeekBackIncrementMs` / `setSeekForwardIncrementMs`

### Decision

Tập trung vào 3 thay đổi chính:
1. **Tối ưu LoadControl** cho local playback
2. **Thêm WakeMode** phân biệt local/network
3. **Preferred video codec** (lưu tên codec cụ thể)

### Rationale

NextPlayer đã tích hợp `nextlib-media3ext` (FFmpeg) nên FR-003 và FR-004 (FFmpeg audio decoder) **đã hoàn thành**. Cần tập trung vào buffer optimization và wake mode — hai yếu tố ảnh hưởng trực tiếp đến startup time và resource usage.

---

## R2: DefaultLoadControl Parameters cho Local Playback

### Phát hiện

Module cũ dùng `setTargetBufferBytes(1)` — buffer cực nhỏ. Media3 `DefaultLoadControl` mặc định:
- `minBufferMs`: 50,000ms (50s)
- `maxBufferMs`: 50,000ms (50s)
- `bufferForPlaybackMs`: 2,500ms
- `bufferForPlaybackAfterRebufferMs`: 5,000ms
- `targetBufferBytes`: `C.LENGTH_UNSET` (auto — khoảng 2MB trên low-RAM, 20MB trên high-RAM)

Cho local playback:
- `minBufferMs`: 15,000ms là đủ
- `bufferForPlaybackMs`: 500ms — khởi động nhanh
- `bufferForPlaybackAfterRebufferMs`: 1,000ms
- `targetBufferBytes`: Giữ `C.LENGTH_UNSET` (để Media3 tự điều chỉnh theo RAM)

### Decision

Tạo `PlayerLoadControlFactory` tạo `DefaultLoadControl` tuỳ theo loại source (local vs network). Không dùng `setTargetBufferBytes(1)` vì quá aggressivenguồn có thể gây stutter trên video bitrate cao.

### Alternatives Rejected

- `setTargetBufferBytes(1)` như module cũ: quá aggressive, gây stutter trên 4K HEVC
- Giữ nguyên default: buffer quá lớn cho local file, khởi động chậm

---

## R3: WakeMode Strategy

### Phát hiện

Android WakeMode:
- `C.WAKE_MODE_NONE` (0): Không giữ wake lock
- `C.WAKE_MODE_LOCAL` (1): Giữ partial wake lock — phù hợp cho local file
- `C.WAKE_MODE_NETWORK` (2): Giữ cả partial wake lock + wifi lock — cho stream

Module cũ: dùng flag `x1` (isNetworkStream) để phân biệt.

NextPlayer hiện tại: **không gọi `setWakeMode()`** → mặc định `WAKE_MODE_NONE`.

### Decision

Thêm `setWakeMode()` trong ExoPlayer builder dựa trên scheme của URI:
- `content://`, `file://` → `WAKE_MODE_LOCAL`
- `http://`, `https://`, `smb://`, `ftp://` → `WAKE_MODE_NETWORK`

Thực hiện khi `onSetMediaItems` xử lý media — detect scheme từ URI đầu tiên.

---

## R4: Preferred Video Codec

### Phát hiện

Module cũ cho phép filter codec theo tên (ví dụ: `c2.android.avc.decoder`). NextPlayer đã có `DecoderPriority` (3 mức), nhưng thiếu khả năng chọn codec cụ thể.

### Decision

**Deferred** — Tính năng này phức tạp (cần list available codecs, UI chọn, custom RenderersFactory override) và là P3. Sẽ implement ở phase sau nếu cần.

### Rationale

`DecoderPriority` hiện tại (DEVICE_ONLY / PREFER_DEVICE / PREFER_APP) đã đủ cho 95% use cases. Preferred codec cụ thể chỉ cần cho debug/troubleshoot trên thiết bị đặc biệt.
