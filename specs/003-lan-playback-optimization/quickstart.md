# Quickstart: Tối ưu phát video LAN và điều hướng cùng thư mục

**Branch**: `003-lan-playback-optimization` | **Date**: 2026-05-17

## Mục tiêu kiểm chứng

1. Video LAN bắt đầu phát nhanh mà không chờ resolve full queue.
2. Next/Prev đi đúng thứ tự local và LAN theo danh sách người dùng vừa thấy.
3. Playlist do caller ngoài truyền vào vẫn được ưu tiên hơn mọi suy luận nội bộ.
4. Raw network URL đơn lẻ vẫn phát được và không tự điều hướng sai.
5. Khi chạm biên queue, player giữ nguyên video hiện tại và hiện feedback ngắn.

## Chuẩn bị

1. Một thư mục local có ít nhất 3 video hợp lệ.
2. Một thư mục LAN mà app đang duyệt được, có ít nhất 3 video hợp lệ.
3. Một raw network URL đơn lẻ để kiểm tra fallback single-item.
4. Một case mở player bằng `API_PLAYLIST` để kiểm tra precedence của caller ngoài.
5. Thiết bị Android API 23+ hoặc emulator đủ mạnh; mạng LAN ổn định.

## Build và verify sau khi implement

```powershell
.\gradlew.bat :feature:player:compileDebugKotlin :feature:videopicker:compileDebugKotlin :app:compileDebugKotlin
```

Ghi chú:
- Luồng hiện tại đã được verify bằng compile của `feature:player`, `feature:videopicker`, và `app`.
- Chưa bổ sung unit test riêng cho resolver trong feature này.

## Luồng xác minh thủ công

### 1. Local folder order

1. Mở một video từ màn duyệt thư mục local.
2. Ghi lại thứ tự 3 video người dùng vừa thấy.
3. Trong player, bấm Next rồi Prev.
4. Xác nhận player đi đúng item liền kề theo thứ tự đó.

### 2. LAN open latency

1. Mở một video từ thư mục LAN đang truy cập được.
2. Đo thời gian từ lúc bấm mở đến khi phát hình/âm thanh.
3. Trong khi player vừa mở, thử Pause/Play hoặc kéo progress.
4. Xác nhận thao tác phản hồi ngay, không bị chặn bởi queue hydration.

### 3. LAN folder navigation

1. Từ cùng thư mục LAN, mở video thứ hai hoặc thứ ba.
2. Bấm Prev và Next vài lần.
3. Xác nhận player đi theo đúng thứ tự màn duyệt LAN vừa hiển thị.

### 4. External playlist precedence

1. Mở player bằng `API_PLAYLIST`.
2. Bấm Next/Prev.
3. Xác nhận queue được giữ theo playlist caller truyền vào, không bị thay bằng folder fallback.

### 5. Raw URL single-item fallback

1. Dùng luồng “Open network stream” nhập một URL video đơn lẻ.
2. Xác nhận video vẫn phát bình thường.
3. Khi bấm Next/Prev, xác nhận player giữ nguyên video hiện tại và hiện feedback ngắn là không còn video tiếp theo hoặc trước đó.

## Metric cần quan sát

| Metric | Mục tiêu |
|--------|----------|
| Open latency trên LAN ổn định | 90% lượt mở <= 2 giây |
| Độ đúng Next/Prev | >= 95% trường hợp đi đúng item liền kề |
| Boundary behavior | 100% giữ nguyên current item khi chạm biên |
| Raw URL fallback | 100% phát được current item mà không nhảy sai |

## Không cần

- Không có migrate database.
- Không cần thay đổi schema Room hay DataStore.
- Không mở rộng sang tự quét thư mục cho raw network URL đơn lẻ trong v1.
