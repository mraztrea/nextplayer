# Research: Tối ưu phát video LAN và điều hướng cùng thư mục

**Branch**: `003-lan-playback-optimization` | **Date**: 2026-05-17

## R1: Chọn nguồn sự thật cho danh sách anh em local/LAN

### Decision
Đưa vào một `playback launch context` rõ ràng từ launcher khi app đã biết danh sách video cùng thư mục và thứ tự người dùng vừa thấy; chỉ fallback sang resolve từ media index cục bộ khi launcher không có ngữ cảnh đó.

### Rationale
- `MediaNavGraph` hiện mở `PlayerActivity` chỉ với `Intent.ACTION_VIEW` + `data`, nên player không biết thứ tự người dùng vừa thấy nếu launcher không truyền thêm gì.
- `GetSortedPlaylistUseCase` hiện suy danh sách bằng `context.getPath(uri) -> File(parent)`, phù hợp local index nhưng không đủ cho URI LAN hoặc nguồn không quy đổi ra `File`.
- Module tham chiếu `code_reference/video_player_module` mở video bằng cách mang theo list item nguồn để player điều hướng trong cùng ngữ cảnh, thay vì yêu cầu player tự tái khám phá thư mục.
- Cách này vừa giữ đúng thứ tự source-visible order, vừa tránh yêu cầu player tự quét mạng.

### Alternatives considered
- **Tiếp tục để player tự suy thư mục từ URI**: Đơn giản nhưng chỉ đáng tin cho local path và không khóa được order LAN.
- **Ép mọi nguồn ngoài phải gửi `video_list` đầy đủ**: Hoạt động được nhưng quá cứng, không hỗ trợ tốt trường hợp chỉ cần current item + sibling context rút gọn.

## R2: Chọn chiến lược mở video nhanh trên LAN

### Decision
Ưu tiên chuẩn bị và `prepare()` media hiện tại ngay lập tức; danh sách anh em cùng thư mục được hydrate bất đồng bộ sau đó và chỉ cập nhật queue khi đã resolve xong.

### Rationale
- `PlayerActivity.playVideo()` hiện đợi xong toàn bộ `playlist.mapIndexed { MediaItem.Builder()... }` rồi mới `setMediaItems(...); prepare()`, nghĩa là open time tỷ lệ thuận với chi phí chuẩn bị queue.
- Với LAN, chi phí này dễ tăng vì việc suy ngữ cảnh hoặc dựng danh sách có thể chậm hơn local.
- PlayerService đã để metadata nặng như artwork/subtitle local chạy nền trong `updatedMediaItemsWithMetadata()` và `loadArtworkForCurrentMediaItem()`, cho thấy repo đã chấp nhận pattern “phát trước, enrich sau”.
- Bất đồng bộ queue hydration thỏa FR-005 và FR-006 mà không cần thay đổi sâu Media3 session.

### Alternatives considered
- **Tiếp tục build full queue trước khi `prepare()`**: Dễ hiểu hơn nhưng không đáp ứng mục tiêu tốc độ mở video LAN.
- **Tạo queue placeholder toàn bộ rồi thay thế đồng loạt sau**: Có thể làm được nhưng rủi ro index drift cao hơn và không cần thiết cho v1.

## R3: Chọn thứ tự ưu tiên resolve queue

### Decision
Áp dụng precedence chain:
1. `API_PLAYLIST` từ caller ngoài  
2. `source-visible order` trong `playback launch context`  
3. local indexed folder fallback qua app sort hiện hành  
4. single-item fallback

### Rationale
- Spec đã khóa rằng playlist caller ngoài có ưu tiên cao nhất.
- Source-visible order là thứ tự gần nhất với kỳ vọng người dùng “đang thấy gì thì Next/Prev sẽ đi như vậy”.
- `GetSortedVideosUseCaseTest` xác nhận app đã có sort fallback rõ ràng theo preferences, nên khi source không cung cấp order vẫn có behavior nhất quán.
- Single-item fallback giữ backward compatibility cho raw URL hoặc source nghèo ngữ cảnh.

### Alternatives considered
- **Luôn dùng app sort**: Không giữ được thứ tự người dùng vừa duyệt trên LAN.
- **Luôn dùng source-visible order nếu có bất kỳ gợi ý nào**: Không đủ vì external playlist caller phải có quyền override.

## R4: Chọn phạm vi hỗ trợ LAN cho v1

### Decision
Chỉ hỗ trợ Next/Prev theo thư mục cho nguồn LAN mà app đã duyệt tới và có thể truyền `playback launch context`; nhập thủ công một network URL đơn lẻ vẫn là single-item playback.

### Rationale
- Spec đã khóa rõ không được quét mạng mù từ raw URL.
- `MediaPickerScreen` hiện có luồng “Open network stream”, nhưng luồng này không tự mang ngữ cảnh thư mục hoặc sibling list.
- Tách rõ hai nhóm nguồn làm giảm rủi ro: local/indexed và LAN đã duyệt tới có queue context sẽ được tối ưu, raw URL vẫn giữ behavior cũ.

### Alternatives considered
- **Tự suy thư mục từ raw LAN URL**: Tiềm ẩn networking ngoài phạm vi, không đáng tin, và làm tăng độ phức tạp.
- **Bỏ LAN folder support, chỉ sửa local**: Không đáp ứng yêu cầu người dùng.

## R5: Chọn hành vi biên và phản hồi người dùng

### Decision
Khi không có item hợp lệ theo hướng Next/Prev, giữ nguyên video hiện tại và phát feedback ngắn cho người dùng; không wrap vòng và không đóng session.

### Rationale
- Đây là đáp án clarify đã được khóa vào spec.
- Không wrap giúp tránh bất ngờ ở queue nguồn ngoài.
- Giữ session mở cho phép người dùng tiếp tục tua, pause, đổi track hoặc thoát chủ động.

### Alternatives considered
- **Wrap sang đầu/cuối**: Dễ gây bất ngờ và làm phức tạp semantics của queue caller ngoài.
- **Im lặng không hiện gì**: Khó hiểu với người dùng và khó test acceptance.
- **Dừng phát**: Không cần thiết và tạo trải nghiệm gắt.

## R6: Chọn chiến lược kiểm thử cho feature này

### Decision
Tập trung vào unit test cho resolver/preference precedence và compile verification cho `feature:player`, `feature:videopicker`, `app`; manual verification bao phủ local folder, LAN folder, raw URL và external playlist.

### Rationale
- Phần rủi ro nhất là luật precedence và boundary behavior, phù hợp nhất với unit test xác định đầu vào/đầu ra.
- Thay đổi chạm nhiều module UI/player nên compile-level verification là bắt buộc để giữ an toàn refactor.
- Manual flow vẫn cần cho open latency và tương tác Next/Prev thực tế trên LAN.

### Alternatives considered
- **Chỉ manual test**: Không đủ bảo vệ các nhánh precedence và fallback.
- **Đầu tư instrumentation ngay từ v1**: Giá trị có nhưng chi phí setup cao hơn mức cần cho feature scope hiện tại.
