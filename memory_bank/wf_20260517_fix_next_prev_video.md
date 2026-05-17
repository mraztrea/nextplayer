# Workflow: Sửa lỗi nút Next/Prev video không hoạt động

**Ngày**: 2026-05-17
**Trạng thái**: ✅ Hoàn thành

## Mô tả lỗi
- Nút **Next Video** và **Previous Video** trong player không hoạt động
- Luôn hiển thị thông báo "No next video" / "No previous video"
- Lỗi xảy ra cả khi mở video từ app (video picker) lẫn khi có nhiều video trong cùng thư mục

## Nguyên nhân gốc rễ

**File**: `feature/player/src/main/java/.../utils/PlayerApi.kt`

Hàm `normalizedUriStrings()` luôn thêm URI hiện tại vào đầu danh sách:
```kotlin
private fun List<String>.normalizedUriStrings(currentUriString: String): List<String> {
    return buildList {
        add(currentUriString)  // <-- Luôn thêm, kể cả khi list gốc rỗng
        addAll(this@normalizedUriStrings)
    }.map(String::trim).filter(String::isNotEmpty).distinct()
}
```

Trong `getPlaybackLaunchContext()`, hàm này được gọi TRƯỚC khi kiểm tra kết quả:
```kotlin
val playlist = getPlaylist().normalizedUriStrings(currentUriString)
if (playlist.isNotEmpty()) { ... }  // Luôn true vì normalizedUriStrings thêm currentUriString
```

**Hậu quả**: Khi `getPlaylist()` trả về empty (trường hợp mở video từ app), `normalizedUriStrings` vẫn tạo ra list `[currentUriString]` → điều kiện `isNotEmpty()` luôn true → code trả về `PlaybackLaunchContext` chỉ chứa 1 video → media queue chỉ có 1 item → `hasNextMediaItem()` trả false → hiển thị "No next video".

Phần kiểm tra `API_PLAYBACK_CONTEXT_URIS` (chứa danh sách sibling videos thực sự) không bao giờ được chạy tới.

## Thay đổi

### `PlayerApi.kt` 
- Kiểm tra `getPlaylist()` trước khi gọi `normalizedUriStrings()`
- Tương tự cho `API_PLAYBACK_CONTEXT_URIS`

## Không cần migration database hay lệnh đặc biệt
