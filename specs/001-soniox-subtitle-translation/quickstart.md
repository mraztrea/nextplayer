# Quickstart: Soniox Subtitle Translation

**Branch**: `001-soniox-subtitle-translation` | **Date**: 2026-05-05

## Prerequisites

1. Android Studio với Kotlin 2.3.20+
2. Soniox API Key (đăng ký tại [soniox.com](https://soniox.com))
3. Thiết bị Android hoặc emulator với API 23+

## Setup

### 1. Thêm dependency OkHttp

Trong `gradle/libs.versions.toml`:
```toml
[versions]
okhttp = "4.12.0"
androidxSecurity = "1.1.0-alpha06"

[libraries]
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "androidxSecurity" }
```

### 2. Tạo module `core:subtitle`

Trong `settings.gradle.kts`, thêm:
```kotlin
include(":core:subtitle")
```

### 3. Build & Run

```powershell
.\gradlew.bat :app:assembleDebug
```

### 4. Cấu hình trong app

1. Mở Settings → Subtitle → Translation
2. Nhập Soniox API Key
3. Chọn Source Language (hoặc Auto-detect)
4. Chọn Target Language (mặc định: Tiếng Việt)
5. Chọn Display Mode (mặc định: Chỉ bản dịch)

### 5. Sử dụng

1. Mở video bất kỳ trong nextplayer
2. Bật toggle "Live Subtitle" trong player controls
3. Phụ đề xuất hiện trên overlay video

## Kiến trúc Module

```
core:subtitle/
├── src/main/java/.../subtitle/
│   ├── engine/
│   │   ├── SubtitleEngine.kt          # Interface + implementation
│   │   ├── SonioxWebSocketClient.kt   # WebSocket management
│   │   └── SonioxTokenParser.kt       # Token stream parsing
│   ├── model/
│   │   ├── SubtitleSegment.kt         # Data class
│   │   ├── SegmentStatus.kt           # Enum
│   │   └── SubtitleDisplayMode.kt     # Enum
│   ├── session/
│   │   ├── SubtitleSessionManager.kt  # FIFO queue, display buffer, session log
│   │   └── SessionResetScheduler.kt   # Periodic session reset
│   ├── audio/
│   │   ├── SubtitleAudioProcessor.kt  # ExoPlayer AudioProcessor tap
│   │   └── AudioBatcher.kt            # 200ms batching
│   ├── storage/
│   │   └── SecureApiKeyStorage.kt     # EncryptedSharedPreferences wrapper
│   └── di/
│       └── SubtitleModule.kt          # Hilt DI module
```

## Heuristic Constants

| Tham số | Giá trị | Mô tả |
|---------|---------|-------|
| Audio batch interval | 200ms | Cân bằng latency vs overhead |
| Keepalive interval | 15s | Giữ kết nối khi im lặng |
| Session reset interval | 3 phút | Ngăn session timeout |
| Stale segment timeout | 10s | Cleanup original chưa dịch |
| Max pending originals | 3 | Ngăn FIFO queue drift |
| Endpoint delay | 3000ms | Soniox sentence boundary |
| Carryover context limit | 500 ký tự | Context xuyên session reset |
| Reconnect backoff | 1s, 2s, 4s... max 30s | Exponential backoff |
