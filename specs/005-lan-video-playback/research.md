# Research: Xem video trong mạng LAN

## Decision: Hỗ trợ SMB bằng SMBJ 0.14.0 trong phiên bản đầu

**Rationale**: Spec đã chốt phạm vi SMB/shared folder. SMBJ là Java SMB2/SMB3 client, có API đăng nhập bằng username/password, connect share và list folder. Maven metadata ngày 2026-05-19 cho thấy bản mới nhất là `0.14.0`.

**Alternatives considered**:

- `jcifs-ng 2.1.10`: có SMB2 mặc định và một phần SMB3, nhưng README ghi server/workgroup browsing bị deprecate và mặc định protocol rộng hơn. Không cần cho phạm vi đã chốt.
- Android Storage Access Framework: phù hợp chọn file local/provider, không đáp ứng quản lý server SMB trong app.
- Tải file về local trước khi phát: đơn giản hóa player nhưng trái yêu cầu phát từ LAN không cần sao chép thủ công.

## Decision: Lưu server/bookmark bằng Room, lưu password bằng AndroidX Security Crypto 1.1.0

**Rationale**: Server profile, bookmark và thumbnail metadata cần query, sort, migrate và liên kết với nhau, phù hợp với Room hiện có trong `core:database`. Password là dữ liệu nhạy cảm, nên lưu tách qua encrypted SharedPreferences hoặc store tương đương dựa trên Android Keystore. Maven metadata ngày 2026-05-19 cho thấy AndroidX Security Crypto mới nhất là `1.1.0`.

**Alternatives considered**:

- Lưu toàn bộ trong DataStore: đơn giản cho preferences nhưng kém phù hợp với quan hệ server-bookmark-thumbnail và migration dạng bảng.
- Lưu password trực tiếp trong Room: dễ implement nhưng không đạt kỳ vọng bảo vệ credential.
- Không lưu password: an toàn hơn nhưng trái yêu cầu lưu thông tin server để dùng lại.

## Decision: Dùng abstraction LAN riêng cho SMB browse, thumbnail và playback

**Rationale**: SMB connection/auth/session logic được dùng bởi ít nhất ba bề mặt: duyệt thư mục, tạo thumbnail và phát video. Boundary riêng giúp `feature:videopicker` không phụ thuộc trực tiếp vào chi tiết SMB, và `feature:player` có thể đọc stream qua contract ổn định.

**Alternatives considered**:

- Đặt SMB logic trong `feature:videopicker`: nhanh cho browse nhưng player/thumbnail phải phụ thuộc UI hoặc nhân bản logic.
- Gộp vào `LocalMediaRepository`: tăng rủi ro phá hành vi local media đã ổn định.
- Tạo web server nội bộ để proxy SMB sang HTTP: thêm lifecycle/security/port complexity không cần thiết cho phạm vi đầu.

## Decision: Playback qua custom SMB data source hoặc URI LAN nội bộ được PlayerService hiểu

**Rationale**: Player hiện nhận `Uri` và Media3 xử lý local/http/content tốt, nhưng SMB không phải scheme mặc định đáng tin cậy. Cần adapter ở `feature:player` hoặc `core:lan` để mở stream SMB, hỗ trợ seek đủ tốt cho video, và giữ logic MediaSourceType NETWORK đã có cho cấu hình buffering LAN.

**Alternatives considered**:

- Dựa vào `smb://` URI mặc định của Media3: không đủ cơ sở vì Media3 default data source không đảm bảo SMB.
- ContentProvider SMB: có thể dùng được nhưng cần bảo đảm seek/openAssetFile; độ phức tạp tương đương custom data source.
- Copy file đầy đủ vào cache trước khi phát: tăng dung lượng, làm chậm mở video lớn, không đúng mục tiêu phát nhanh.

## Decision: Thumbnail tải lười và cache theo dấu hiệu file

**Rationale**: Spec yêu cầu thư mục tối đa 500 mục vẫn thao tác được nhanh, nên folder listing phải hiện trước với placeholder. Thumbnail chỉ tạo cho item đang thấy hoặc sắp thấy, sau đó cache bằng key gồm server profile, path, size và modified time để tái sử dụng khi mở lại folder/bookmark.

**Alternatives considered**:

- Tạo thumbnail cho toàn thư mục trước khi hiển thị: dễ kiểm soát nhưng chặn UI với thư mục lớn.
- Không cache thumbnail: đơn giản nhưng không đạt yêu cầu mở lại nhanh và tốn SMB I/O.
- Chỉ placeholder: không đạt yêu cầu hiển thị thumbnail khi có thể.
