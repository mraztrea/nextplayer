# Contract: LAN SMB Client

## Purpose

Cung cấp contract nội bộ để các layer browse, thumbnail và player truy cập SMB/shared folder mà không phụ thuộc trực tiếp vào thư viện SMB cụ thể.

## Server Address

Input tối thiểu:

- `serverId`
- `host`
- `shareName`
- `initialPath`
- `username`
- `credentialKey`

Output khi resolve:

- `LanConnectionHandle` hoặc lỗi typed.

## Operations

### Validate Server

**Input**: server profile draft và credential nếu có.  
**Output**: success, authentication error, unreachable error, permission error, invalid share error.

Acceptance:

- Không lưu profile mới nếu `host`, `shareName`, hoặc `displayName` rỗng.
- Không log password.
- Timeout phải trả lỗi có thể hiển thị, không treo UI.

### List Folder

**Input**: `serverId`, folder path tương đối trong share.  
**Output**: danh sách `LanMediaItem` gồm folder và video item.

Acceptance:

- Listing không được trả path vượt ra ngoài share/root ban đầu.
- Folder rỗng trả trạng thái loaded với danh sách rỗng.
- Lỗi offline/auth/permission/path missing được phân biệt để UI hiển thị hành động phù hợp.

### Open Video Stream

**Input**: `serverId`, media path, expected size/modified metadata nếu có.  
**Output**: seekable stream hoặc data source handle cho player.

Acceptance:

- Player có thể bắt đầu phát video hiện tại trước khi hoàn tất thumbnail hoặc playlist phụ trợ.
- Seek failure phải được chuyển thành playback error, không crash app.

### Load Thumbnail Source

**Input**: `serverId`, media path, size, modified time.  
**Output**: cached thumbnail, generated thumbnail, placeholder state, hoặc failure state.

Acceptance:

- Thumbnail loading là lazy theo visible item.
- Cache hit không cần mở lại SMB stream nếu cache còn hợp lệ.
- Failure không chặn folder listing.

## Error Types

- `InvalidInput`
- `AuthenticationFailed`
- `ServerUnreachable`
- `ShareNotFound`
- `PermissionDenied`
- `PathNotFound`
- `UnsupportedMedia`
- `ReadInterrupted`
- `Unknown`

Mỗi error phải có user-facing message key và technical cause optional để log/debug.
