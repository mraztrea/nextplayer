# Workflow: Tối ưu phát video LAN và điều hướng cùng thư mục

## Phạm vi

- Tăng tốc mở video bằng cách phát item hiện tại trước, hydrate queue sau.
- Truyền sibling context từ media picker để Next/Prev hoạt động cho local và LAN.
- Giữ fallback an toàn cho raw network URL và feedback khi chạm biên queue.

## Thay đổi chính

1. Thêm model queue/context mới trong `core:model` và `feature:player`.
2. `MediaPickerViewModel` dựng sibling list theo `parentPath` của video hiện tại.
3. `MediaNavGraph` truyền `playback_context_uris` + `playback_source_type` khi mở video từ picker.
4. `PlayerActivity` chỉ `setMediaItem()` cho current item lúc đầu, sau đó apply queue nền từ `queueHydrationState`.
5. Nút Next/Prev trong player giữ khả năng bấm ở biên và hiện toast ngắn khi không còn item hợp lệ.

## Lệnh verify đã chạy

```powershell
.\gradlew :feature:player:compileDebugKotlin :feature:videopicker:compileDebugKotlin :app:compileDebugKotlin
```

## Verify thủ công nên chạy thêm

1. Mở video từ thư mục local có ít nhất 3 file rồi bấm Next/Prev để xác nhận đúng thứ tự.
2. Mở video từ thư mục LAN có ít nhất 3 file để kiểm tra tốc độ mở và Next/Prev.
3. Dùng `Open network stream` với 1 raw URL để xác nhận fallback single-item và toast ở biên.
4. Mở player bằng `API_PLAYLIST` để xác nhận precedence của caller ngoài.

## Migrate / setup

- Không có migrate database.
- Không cần thay đổi schema Room hay DataStore.

## Bổ sung CxFileExplorer

- `code_reference/video_player_module` cho thấy player reference không tự quét LAN mù trong activity; nó nhận sẵn queue từ opener rồi mới dựng Next/Prev.
- Để tương thích explorer ngoài, `PlayerApi` nay đọc thêm queue từ `Intent.EXTRA_STREAM` và `ClipData` nếu caller có gửi.
- Nếu caller chỉ gửi 1 `content://` URI, `ResolvePlaybackQueueUseCase` sẽ thử liệt kê sibling qua `DocumentsProvider` của cùng thư mục để dựng Next/Prev cho LAN.
- Đã verify compile thêm bằng:

```powershell
.\gradlew :core:domain:compileDebugKotlin :feature:player:compileDebugKotlin :app:compileDebugKotlin
```
