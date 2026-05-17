# Contract: Launcher -> Player Playback Context

**Branch**: `003-lan-playback-optimization` | **Date**: 2026-05-17

## Mục tiêu

Ràng buộc những gì launcher cần truyền cho `PlayerActivity` để player biết:
- có playlist caller ngoài hay không
- có source-visible order local/LAN hay không
- có được phép resolve Next/Prev theo cùng thư mục hay không

## Intent contract

```kotlin
interface PlaybackIntentContract {
    val data: Uri
    val apiPlaylist: List<Uri>?
    val playbackSourceType: String?          // EXTERNAL_PLAYLIST | SOURCE_FOLDER | SINGLE_URI
    val visibleSiblingUris: List<Uri>?
    val currentVisibleIndex: Int?
    val folderKey: String?
}
```

## Behavioral rules

- `data` luôn là video hiện tại cần phát ngay.
- Nếu `apiPlaylist` có dữ liệu:
  - nó là nguồn queue có ưu tiên cao nhất
  - `playbackSourceType` phải được hiểu là `EXTERNAL_PLAYLIST`
- Nếu `visibleSiblingUris` có dữ liệu:
  - launcher cam kết thứ tự này đúng với những gì người dùng vừa thấy
  - `currentVisibleIndex` phải chỉ vào `data`
  - player được phép dùng danh sách này cho Next/Prev mà không sắp xếp lại
- Nếu chỉ có `data` mà không có `apiPlaylist` hoặc `visibleSiblingUris`:
  - player xem đây là `SINGLE_URI`
  - player không được tự quét LAN để đoán thư mục anh em

## Compatibility rules

- Caller cũ chỉ truyền `data` vẫn hợp lệ và giữ behavior hiện tại.
- Caller ngoài đã dùng `API_PLAYLIST` không cần thay đổi nếu không muốn truyền thêm context.
- Media picker/local folder và LAN folder browser là hai launcher chính nên được nâng cấp trước để truyền `visibleSiblingUris`.
