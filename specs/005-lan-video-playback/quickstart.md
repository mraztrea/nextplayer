# Quickstart: Xem video trong mạng LAN

## Prerequisites

- Thiết bị Android và máy chứa SMB share nằm cùng mạng LAN.
- Có một SMB share chứa ít nhất 3 video trong cùng thư mục.
- Có username/password hoặc guest access hợp lệ.

## Build Verification

Chạy sau khi implementation hoàn tất:

```powershell
rtk .\gradlew :core:lan:compileDebugKotlin :core:data:compileDebugKotlin :feature:videopicker:compileDebugKotlin :feature:player:compileDebugKotlin
```

```powershell
rtk .\gradlew :core:lan:testDebugUnitTest :core:data:testDebugUnitTest :feature:videopicker:testDebugUnitTest :feature:player:testDebugUnitTest
```

```powershell
rtk .\gradlew assembleDebug
```

## Manual Scenario

1. Mở app và vào media picker.
2. Mở khu vực LAN.
3. Thêm server mới với tên hiển thị, host/IP, share/root ban đầu, username và password.
4. Đóng app rồi mở lại, xác nhận server vẫn còn trong danh sách.
5. Mở server, duyệt vào thư mục có video.
6. Xác nhận danh sách folder/video hiển thị trước khi toàn bộ thumbnail hoàn tất.
7. Xác nhận video đang thấy có thumbnail hoặc placeholder trong vòng 3 giây.
8. Mở một video và xác nhận player bắt đầu phát.
9. Quay lại thư mục, bookmark thư mục hiện tại.
10. Đóng/mở lại app, mở bookmark và xác nhận đi thẳng tới thư mục đã lưu.
11. Đổi password server thành sai, mở lại server hoặc bookmark và xác nhận lỗi authentication có hướng xử lý.
12. Xóa server và xác nhận bookmark liên quan không còn mở sai thư mục.

## Expected Results

- Server profile được lưu lại.
- Password không hiển thị rõ trên UI.
- Folder listing không bị chặn bởi thumbnail.
- Thumbnail cache được tái sử dụng khi mở lại cùng thư mục.
- Player phát video LAN không yêu cầu người dùng sao chép file thủ công.
- Bookmark trùng không tạo duplicate.
