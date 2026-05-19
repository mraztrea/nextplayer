# Workflow: LAN video playback

Ngày: 2026-05-19

## Phạm vi

- Feature: `specs/005-lan-video-playback`
- Mục tiêu: quản lý SMB server, duyệt thư mục LAN, thumbnail lazy cache, phát video LAN, bookmark thư mục.

## Ghi chú triển khai

- Password server phải lưu qua encrypted credential store, không lưu rõ trong Room.
- Phiên bản đầu chỉ hỗ trợ SMB/shared folder, không tự quét LAN và không hỗ trợ DLNA/WebDAV/FTP.
- Room `media_db` tăng lên version 7, thêm bảng `lan_servers`, `lan_thumbnail_cache`, `lan_folder_bookmarks`.
- Player nhận URI nội bộ dạng `nextplayer-smb://{serverId}/{mediaPath}` và route qua SMB Media3 `DataSource`.
- Thumbnail hiện có metadata cache và placeholder ổn định trong danh sách; chưa có Coil fetcher sinh thumbnail ảnh thật trực tiếp từ SMB URI.

## Verification

- PASS: `rtk .\gradlew :core:lan:testDebugUnitTest :core:data:testDebugUnitTest :core:domain:testDebugUnitTest :core:media:testDebugUnitTest :feature:videopicker:testDebugUnitTest :feature:player:testDebugUnitTest :core:database:compileDebugAndroidTestKotlin :app:compileDebugKotlin`
- PASS: `rtk .\gradlew ktlintCheck`
- PASS: `rtk .\gradlew assembleDebug`
- GitNexus `detect_changes(scope: all)`: risk `high`, affected scope chủ yếu ở `MediaNavGraph`, `MediaPickerScreen`, `PlayerService`, Room database modules và LAN repository/navigation symbols.
- Chưa UAT trên thiết bị thật với SMB server trong LAN.

## Hướng dẫn vận hành

- Sau khi cài bản mới, Room migration tự chạy khi app mở database; không cần lệnh migrate thủ công.
- Để test thủ công: tạo SMB share có video, thêm server trong màn hình LAN, mở server, duyệt vào thư mục con, phát video và bookmark thư mục đang xem.
