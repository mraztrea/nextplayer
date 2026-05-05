# Workflow: Soniox Subtitle Overlay Layout

**Ngày**: 2026-05-05
**Mục tiêu**: Tối ưu hiển thị phụ đề dịch Soniox trong player

## Thay đổi

- Giảm số segment phụ đề live hiển thị đồng thời từ 3 xuống 2 trong `SubtitleOverlay`
- Điều chỉnh vị trí overlay phụ đề live trong `MediaPlayerScreen`
- Khi `displayMode = TRANSLATION_ONLY` và màn hình ở landscape, overlay cách mép dưới màn hình 10% chiều cao màn hình thay vì dùng `80.dp`
- Các mode còn lại giữ nguyên khoảng cách đáy hiện tại

## Files sửa

- `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/SubtitleOverlay.kt`
- `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/MediaPlayerScreen.kt`

## Lệnh kiểm tra

```powershell
rtk .\gradlew.bat :feature:player:compileDebugKotlin
```

## Kết quả

- Compile `:feature:player:compileDebugKotlin` thành công