# Data Model: Xem video trong mạng LAN

## LAN Server Profile

Đại diện cho một SMB/shared folder do người dùng lưu.

**Fields**:

- `id`: định danh nội bộ ổn định.
- `displayName`: tên hiển thị, bắt buộc, không rỗng.
- `host`: IP hoặc hostname, bắt buộc.
- `shareName`: share/root ban đầu, bắt buộc.
- `initialPath`: path con tùy chọn dưới share, mặc định rỗng.
- `username`: username tùy chọn nếu server cho phép guest.
- `credentialKey`: khóa tham chiếu password trong encrypted credential store.
- `createdAt`: thời điểm tạo.
- `updatedAt`: thời điểm cập nhật.
- `lastConnectedAt`: thời điểm kết nối thành công gần nhất, tùy chọn.

**Validation Rules**:

- `host`, `shareName`, `displayName` phải có giá trị sau khi trim.
- Cặp `host + shareName + initialPath + username` không được tạo profile trùng nếu người dùng không cố ý sửa profile cũ.
- UI không bao giờ hiển thị password rõ; chỉ hiển thị trạng thái đã có hoặc chưa có password.

**Relationships**:

- Một server profile có nhiều folder bookmark.
- Một server profile có nhiều thumbnail cache entry.

## LAN Credential

Password được lưu tách khỏi Room bằng encrypted credential store.

**Fields**:

- `credentialKey`: khóa nội bộ, liên kết với `LAN Server Profile`.
- `password`: giá trị nhạy cảm được mã hóa khi lưu.
- `updatedAt`: thời điểm cập nhật credential.

**Validation Rules**:

- Khi profile bị xóa, credential tương ứng phải bị xóa.
- Khi người dùng xóa password, credentialKey vẫn có thể tồn tại nhưng trạng thái phải phản ánh không có password.

## LAN Folder

Thư mục SMB đang được duyệt.

**Fields**:

- `serverId`: profile chứa thư mục.
- `path`: path tương đối trong share.
- `displayPath`: path hiển thị cho người dùng.
- `parentPath`: path cha nếu có.
- `loadedAt`: thời điểm listing gần nhất.

**Validation Rules**:

- Path không được thoát ra ngoài share/root ban đầu.
- Lỗi quyền truy cập, offline hoặc path không tồn tại phải được trả thành trạng thái lỗi có thể hiển thị.

## LAN Media Item

Một item trong folder listing.

**Fields**:

- `serverId`: profile chứa item.
- `path`: path tương đối trong share.
- `name`: tên item.
- `type`: `folder` hoặc `video`.
- `sizeBytes`: kích thước nếu có.
- `modifiedAt`: thời điểm sửa nếu server trả về.
- `thumbnailState`: `notRequested`, `loading`, `ready`, `failed`.
- `playbackUri`: URI nội bộ hoặc SMB URI mà player hiểu.

**Validation Rules**:

- Chỉ item loại `video` được mở bằng player.
- Folder luôn điều hướng vào browser, không đi vào player.
- Item không phải video có thể bị ẩn khỏi danh sách hoặc hiển thị chỉ khi cần điều hướng; không được mở như video.

## Folder Bookmark

Lối tắt đến một thư mục LAN.

**Fields**:

- `id`: định danh nội bộ.
- `serverId`: server profile liên quan.
- `folderPath`: path thư mục trong share.
- `displayName`: tên bookmark, mặc định lấy từ thư mục hoặc server/path.
- `createdAt`: thời điểm tạo.
- `updatedAt`: thời điểm cập nhật.
- `lastOpenedAt`: thời điểm mở gần nhất, tùy chọn.

**Validation Rules**:

- Một server không có hai bookmark cho cùng `folderPath`.
- Khi server bị xóa, bookmark liên quan phải bị xóa hoặc được đánh dấu không hợp lệ trước khi hiển thị.
- Khi mở bookmark thất bại, không xóa tự động; cho người dùng thử lại, sửa server hoặc xóa bookmark.

## LAN Thumbnail Cache Entry

Metadata cho thumbnail đã lưu.

**Fields**:

- `cacheKey`: hash ổn định từ `serverId + path + sizeBytes + modifiedAt`.
- `serverId`: server profile liên quan.
- `mediaPath`: path video.
- `thumbnailFilePath`: file cache trong app storage.
- `sizeBytes`: kích thước video lúc tạo thumbnail.
- `modifiedAt`: modified time lúc tạo thumbnail.
- `createdAt`: thời điểm tạo cache.
- `lastUsedAt`: thời điểm dùng gần nhất.

**Validation Rules**:

- Nếu `sizeBytes` hoặc `modifiedAt` thay đổi, cache entry cũ không còn hợp lệ.
- Cache miss hoặc decode fail phải trả placeholder, không chặn folder listing.
- Cache có thể bị dọn theo quota app cache mà không làm mất server/bookmark.

## State Transitions

### Server Profile

`draft` → `saved` → `connected` hoặc `connectionFailed` → `updated` hoặc `deleted`

### Folder Listing

`idle` → `loading` → `loaded` hoặc `error`

### Thumbnail

`notRequested` → `loading` → `ready` hoặc `failed` → `notRequested` khi file đổi

### Bookmark

`active` → `openFailed` → `active` khi mở lại thành công hoặc `deleted`
