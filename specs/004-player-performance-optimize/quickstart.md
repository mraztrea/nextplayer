# Quickstart: Tối Ưu Hiệu Suất Video Player

**Branch**: `004-player-performance-optimize` | **Date**: 2026-05-17

## Tóm tắt tính năng

Tối ưu ExoPlayer trong NextPlayer để:
1. Giảm thời gian khởi động video local (buffer thấp)
2. Quản lý wake lock thông minh (local vs network)
3. Tận dụng tốt hơn FFmpeg decoder đã tích hợp sẵn

## Scope đã điều chỉnh sau Research

| Feature | Status | Lý do |
|---------|--------|-------|
| LoadControl tối ưu local | ✅ In scope | Tác động lớn nhất vào startup time |
| WakeMode phân biệt | ✅ In scope | Tiết kiệm pin, ít code thay đổi |
| FFmpeg audio decoder | ✅ Đã có sẵn | `nextlib-media3ext` đã tích hợp |
| Preferred codec selection | ⏳ Deferred (P3) | Phức tạp, ít người dùng cần |

## Files cần thay đổi

| File | Thay đổi |
|------|---------|
| `feature/player/service/PlayerService.kt` | Thêm `DefaultLoadControl`, `setWakeMode()` vào ExoPlayer builder |
| `core/model/.../MediaSourceType.kt` | Enum mới: LOCAL, NETWORK |
| `core/common/.../UriExtensions.kt` (hoặc tương đương) | Extension function `Uri.toMediaSourceType()` |

## Cách kiểm tra

1. **Startup time**: Mở video local, đo thời gian đến first frame (logcat timestamp)
2. **WakeMode**: Dùng `adb shell dumpsys power` kiểm tra wake lock active khi phát local vs network
3. **RAM usage**: Dùng Android Studio Profiler so sánh trước/sau

## Dependencies

- Media3 ExoPlayer `1.10.0` (đã có)
- `nextlib-media3ext` `1.9.3-0.12.0` (đã có)
- Không cần thêm dependency mới
