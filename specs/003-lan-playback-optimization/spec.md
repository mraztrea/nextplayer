# Feature Specification: Tối ưu phát video LAN và điều hướng cùng thư mục

**Feature Branch**: `003-lan-playback-optimization`  
**Created**: 2026-05-17  
**Status**: Draft  
**Input**: User description: "Tối ưu ứng dụng khi player trong mạng Lan. Hiện tại trình player này chưa tối ưu khi play video trong mạng Lan. Hãy review code trong code_reference\video_player_module để hiểu cách tối ưu. Sau đó tối ưu theo yêu cầu sau: tăng tốc độ load video; cho phép bấm Next/Prev thì sẽ đổi track tiếp theo/trước đó trong danh sách file cùng thư mục kể cả mạng Lan."

## Clarifications

### Session 2026-05-17

- Q: Khi không có item kế tiếp hoặc trước đó, Next/Prev nên phản hồi thế nào? → A: Giữ nguyên video hiện tại và hiện thông báo ngắn là không có video tiếp theo/trước đó.
- Q: Thứ tự danh sách cùng thư mục nên lấy theo nguồn nào? → A: Ưu tiên đúng thứ tự danh sách mà người dùng vừa thấy ở nguồn mở video; chỉ fallback sang thứ tự mặc định của app khi nguồn không cung cấp thứ tự.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Phát nhanh video LAN (Priority: P1)

Người dùng mở một video nằm trên thư mục mạng LAN và muốn video bắt đầu phát nhanh, không phải chờ lâu chỉ vì player đang chuẩn bị danh sách file liên quan hoặc metadata không cần thiết.

**Why this priority**: Đây là điểm đau chính được nêu ra. Nếu thời gian vào video vẫn chậm thì trải nghiệm LAN không được cải thiện dù các tính năng khác hoạt động đúng.

**Independent Test**: Có thể kiểm thử độc lập bằng cách mở nhiều video từ cùng một nguồn LAN ổn định và đo thời gian từ lúc bấm mở đến khi video bắt đầu phát hình/âm thanh.

**Acceptance Scenarios**:

1. **Given** người dùng mở một video từ nguồn LAN khả dụng, **When** player khởi tạo phiên phát, **Then** video hiện tại phải được ưu tiên bắt đầu phát mà không bị chặn bởi việc chuẩn bị các video anh em trong cùng thư mục.
2. **Given** người dùng mở lại một video LAN vừa duyệt từ cùng thư mục, **When** player hiển thị màn hình phát, **Then** thao tác điều khiển cơ bản phải phản hồi ngay cả khi ngữ cảnh thư mục vẫn đang được hoàn tất.

---

### User Story 2 - Next/Prev theo cùng thư mục cho local và LAN (Priority: P2)

Người dùng đang xem một video và muốn bấm Next hoặc Prev để chuyển sang video liền kề trong cùng thư mục, bất kể video đó nằm trên bộ nhớ cục bộ hay trên nguồn LAN đã được app duyệt tới.

**Why this priority**: Đây là hành vi điều hướng cốt lõi mà người dùng mong đợi từ player theo kiểu file manager; hiện trạng chỉ đáng tin khi đã có playlist sẵn hoặc suy được đường dẫn local.

**Independent Test**: Có thể kiểm thử độc lập bằng cách mở một video từ thư mục có ít nhất 3 video, sau đó bấm Next/Prev liên tiếp và xác nhận player đi đúng thứ tự trong local lẫn LAN.

**Acceptance Scenarios**:

1. **Given** người dùng mở một video từ thư mục local có nhiều video, **When** bấm Next, **Then** player chuyển sang video kế tiếp theo đúng thứ tự danh sách mà người dùng vừa thấy ở nguồn mở video, hoặc theo thứ tự mặc định của app nếu nguồn không cung cấp thứ tự đó.
2. **Given** người dùng mở một video từ thư mục LAN có nhiều video, **When** bấm Prev, **Then** player chuyển sang video trước đó theo đúng thứ tự danh sách mà người dùng vừa thấy ở nguồn mở video, hoặc theo thứ tự mặc định của app nếu nguồn không cung cấp thứ tự đó.
3. **Given** nguồn mở video đã truyền playlist rõ ràng từ bên ngoài, **When** người dùng bấm Next hoặc Prev, **Then** player tiếp tục tôn trọng playlist đó thay vì tự thay bằng danh sách mới.

---

### User Story 3 - Hành vi an toàn khi thiếu ngữ cảnh thư mục (Priority: P3)

Người dùng mở một video nhưng app không thể xác định danh sách file anh em trong cùng thư mục, ví dụ mở từ một URL mạng đơn lẻ không có ngữ cảnh thư mục. Khi đó player vẫn phải phát được file hiện tại và xử lý Next/Prev một cách dễ hiểu.

**Why this priority**: Tính năng mới không được làm hỏng luồng phát hiện tại hoặc tạo ra điều hướng sai khi ngữ cảnh nguồn không đủ.

**Independent Test**: Có thể kiểm thử độc lập bằng cách mở một video chỉ có URI đơn lẻ, không có playlist hoặc danh sách thư mục, rồi kiểm tra phát hiện tại vẫn hoạt động và Next/Prev không nhảy sai file.

**Acceptance Scenarios**:

1. **Given** người dùng mở một video mà app không xác định được danh sách file cùng thư mục, **When** player bắt đầu phát, **Then** video hiện tại vẫn phát bình thường như trước.
2. **Given** người dùng đang ở nguồn chỉ có một video hợp lệ hoặc không có ngữ cảnh anh em, **When** bấm Next hoặc Prev, **Then** player không được chuyển nhầm sang file ngoài ngữ cảnh, phải giữ nguyên video hiện tại, và phải hiện thông báo ngắn rằng không có video tiếp theo hoặc trước đó.

### Edge Cases

- Khi thư mục chỉ có một video hợp lệ thì Next/Prev phải giữ nguyên video hiện tại và hiện thông báo ngắn rằng không có video tiếp theo hoặc trước đó.
- Khi video hiện tại không xuất hiện trong danh sách anh em trả về thì player phải giữ nguyên video hiện tại thay vì chuyển nhầm chỉ mục.
- Khi nguồn LAN tạm thời chậm hoặc mất kết nối trong lúc chuẩn bị danh sách anh em thì video hiện tại vẫn phải được ưu tiên phát nếu chính file đó còn mở được.
- Khi danh sách cùng thư mục có cả file không phát được, chỉ các video hợp lệ mới được đưa vào điều hướng Next/Prev.
- Khi người dùng mở video từ một URL mạng đơn lẻ không có metadata thư mục, player không được tự suy diễn thư mục bằng cách quét mạng ngoài phạm vi nguồn đã có.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST phân biệt giữa ngữ cảnh phát có playlist được truyền sẵn và ngữ cảnh cần tự xây dựng danh sách cùng thư mục.
- **FR-002**: Hệ thống MUST tự xây dựng danh sách video cùng thư mục khi người dùng mở một video từ nguồn mà app đã có ngữ cảnh thư mục local hoặc LAN, ngay cả khi không có playlist truyền vào từ bên ngoài.
- **FR-003**: Hệ thống MUST đưa video hiện tại vào đúng vị trí trong danh sách điều hướng cùng thư mục và không được tạo bản sao trùng lặp của chính video đó.
- **FR-004**: Hệ thống MUST cho phép lệnh Next và Prev chuyển sang video liền kề trong danh sách cùng thư mục đối với cả nguồn local và nguồn LAN có ngữ cảnh thư mục hợp lệ.
- **FR-005**: Hệ thống MUST ưu tiên bắt đầu phát video hiện tại trước, và không được bắt người dùng chờ hoàn tất toàn bộ việc chuẩn bị danh sách anh em mới được xem video.
- **FR-006**: Hệ thống MUST giữ cho các điều khiển phát cơ bản phản hồi được trong khi danh sách cùng thư mục hoặc metadata bổ sung vẫn đang được chuẩn bị.
- **FR-007**: Khi nguồn mở video đã truyền playlist rõ ràng, hệ thống MUST tiếp tục sử dụng playlist đó làm nguồn điều hướng chính.
- **FR-008**: Khi không thể xác định ngữ cảnh cùng thư mục, hệ thống MUST tiếp tục phát video hiện tại theo hành vi hiện có và MUST không điều hướng sang file ngoài ngữ cảnh.
- **FR-009**: Hệ thống MUST loại trừ các mục không phát được khỏi danh sách điều hướng Next/Prev.
- **FR-010**: Hệ thống MUST ưu tiên giữ thứ tự điều hướng cùng thư mục nhất quán với thứ tự danh sách mà người dùng vừa thấy ở nguồn mở video, và chỉ được fallback sang thứ tự mặc định hiện hành của ứng dụng khi nguồn đó không cung cấp thứ tự rõ ràng.
- **FR-011**: Khi người dùng bấm Next hoặc Prev nhưng không tồn tại item hợp lệ theo hướng đó, hệ thống MUST giữ nguyên video hiện tại và MUST hiện thông báo ngắn rằng không có video tiếp theo hoặc trước đó.

### Key Entities *(include if feature involves data)*

- **Ngữ cảnh phát**: Tập thông tin mô tả video hiện tại được mở từ đâu, có playlist bên ngoài hay không, và có ngữ cảnh thư mục local/LAN để suy ra video anh em hay không.
- **Danh sách cùng thư mục**: Tập các video hợp lệ thuộc cùng thư mục với video hiện tại, được dùng làm cơ sở cho Next/Prev.
- **Nguồn phát mạng LAN**: Nguồn nội dung mạng mà ứng dụng đã có khả năng truy cập hoặc duyệt tới, đủ để xác định video hiện tại và các file anh em trong cùng thư mục.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Với nguồn LAN ổn định mà ứng dụng đã truy cập được, ít nhất 90% lượt mở video thử nghiệm bắt đầu phát trong vòng 2 giây kể từ khi người dùng chọn video.
- **SC-002**: Với thư mục có từ 3 video hợp lệ trở lên, ít nhất 95% thao tác Next/Prev chuyển đúng sang video liền kề theo thứ tự mà người dùng vừa thấy ở nguồn mở video, hoặc theo thứ tự mặc định của app khi nguồn không cung cấp thứ tự.
- **SC-003**: Khi không có ngữ cảnh thư mục hợp lệ, 100% trường hợp kiểm thử vẫn phát được video hiện tại mà không điều hướng nhầm sang file khác.
- **SC-004**: Trong kiểm thử thư mục local và LAN có nhiều video, người dùng có thể đi hết danh sách bằng Next/Prev theo cả hai chiều mà không bị thoát player ngoài ý muốn.

## Assumptions

- Ứng dụng đã có ít nhất một luồng mở video từ local hoặc LAN mà tại đó có thể truy cập được danh sách file trong cùng thư mục hoặc có thể suy ra ngữ cảnh thư mục từ nguồn mở.
- "Cùng thư mục" chỉ bao gồm các video hợp lệ trong đúng thư mục chứa video hiện tại, không mở rộng sang thư mục con hoặc thư mục cha.
- Khi người dùng mở một URL mạng đơn lẻ bằng tay mà không có metadata thư mục hoặc danh sách anh em, phạm vi v1 vẫn giữ hành vi phát một file hiện tại và không tự quét mạng để đoán danh sách kế tiếp.
- Nếu có cả playlist do nguồn gọi truyền vào và ngữ cảnh thư mục nội bộ, playlist do nguồn gọi cung cấp có ưu tiên cao hơn để tránh phá vỡ tích hợp hiện có.
- Nếu nguồn mở video đã hiển thị thứ tự cụ thể cho người dùng, thứ tự đó được xem là nguồn sự thật cho Next/Prev; chỉ khi không có tín hiệu này mới dùng thứ tự mặc định hiện hành của ứng dụng.
