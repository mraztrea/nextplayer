# Contract: LAN Video UI

## Entry Points

- Media picker có entry để mở phần LAN.
- LAN screen có tab hoặc section cho server đã lưu và bookmark thư mục.
- Từ server profile, người dùng mở share/root ban đầu rồi duyệt thư mục con.

## Server Manager

### Inputs

- Tên hiển thị.
- Host hoặc IP.
- Share/root ban đầu.
- Username.
- Password.

### User Actions

- Add server.
- Edit server.
- Delete server.
- Validate/open server.

### Required States

- Empty server list.
- Form validation error.
- Saving.
- Saved.
- Connection test loading.
- Authentication failed.
- Server unreachable.
- Permission denied.

## LAN Folder Browser

### Display Rules

- Folder và video hiển thị theo style nhất quán với local media picker.
- Video hiển thị thumbnail nếu cache/generation có kết quả.
- Placeholder ổn định hiển thị trong khi thumbnail loading hoặc failed.
- Non-video files không phải mục phát chính.

### User Actions

- Open folder.
- Navigate back to parent.
- Open video.
- Bookmark current folder.
- Refresh current folder.

### Required States

- Loading folder.
- Folder loaded.
- Empty folder.
- Offline/error with retry.
- Permission error with option to edit server.

## Bookmarks

### Display Rules

- Bookmark hiển thị tên, server, và path ngắn gọn.
- Bookmark trùng `serverId + folderPath` không được tạo thêm.

### User Actions

- Add current folder bookmark.
- Open bookmark.
- Delete bookmark.

### Required States

- Bookmark opened.
- Bookmark target missing.
- Bookmark server deleted or unavailable.
- Duplicate bookmark ignored or updated with user feedback.

## Playback Handoff

When user opens LAN video:

- UI sends one playable URI/descriptor for selected video.
- UI also sends sibling video list when listing is already available.
- Player starts selected video first; sibling preparation must not block first frame.

## Accessibility and Localization

- Labels and errors are Vietnamese-ready through string resources.
- Password field masks input by default.
- Loading/empty/error states must be visible to screen readers through existing Compose semantics patterns.
