# Workflow: Tối Ưu Hiệu Suất Video Player

**Ngày**: 2026-05-17  
**Branch**: `004-player-performance-optimize`

## Tổng quan thay đổi

### Files mới tạo
1. `feature/player/src/main/java/.../model/MediaSourceType.kt` — Enum LOCAL/NETWORK
2. `feature/player/src/main/java/.../model/LoadControlConfig.kt` — Buffer config

### Files đã sửa
1. `feature/player/src/main/java/.../service/PlayerService.kt` — Core changes:
   - Thêm `DefaultLoadControl` với buffer tối ưu cho local playback
   - Thêm `setWakeMode()` tự động theo URI scheme
   - Thêm startup time logging (`PlayerPerf` tag)
   - Thêm `onPlayerError()` logging cho codec debug

## Hướng dẫn test

### 1. Build & Install
```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 2. Đo Startup Time (Logcat)
```powershell
adb logcat -s PlayerPerf:D
# Mở 1 video local → xem log "First frame rendered in Xms"
# Mục tiêu: < 1000ms cho 1080p, < 2000ms cho 4K HEVC
```

### 3. Verify WakeMode (adb)
```powershell
# Khi phát video local:
adb shell dumpsys power | findstr "Wake"
# Phải thấy PARTIAL_WAKE_LOCK, KHÔNG có WIFI_LOCK

# Khi phát video stream:
# Phải thấy cả PARTIAL_WAKE_LOCK và WIFI_LOCK
```

### 4. Verify FFmpeg Decoder
- Phát video chứa audio DTS/TrueHD/ALAC
- Xác nhận audio phát bình thường
- Nếu lỗi → xem logcat tag `PlayerPerf` để debug

## Không cần migration database
## Không cần chạy lệnh đặc biệt nào
