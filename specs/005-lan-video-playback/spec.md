# Feature Specification: Xem video trong mạng LAN

**Feature Branch**: `005-lan-video-playback`  
**Created**: 2026-05-19  
**Status**: Draft  
**Input**: User description: "Thêm tính năng xem video trong mạng Lan. Có phần quản lý server: nhập thông tin server, ip, name, username, password. Các thông tin này có thể lưu lại để sử dụng cho lần sau. Duyệt thư mục trong mạng LAN như local: có hiển thị thumbnail. Có tính năng bookmark thư mục đang xem để lần sau có thể mở thư mục đó ra nhanh."

## Clarifications

### Session 2026-05-19

- Q: Phạm vi loại server LAN trong phiên bản đầu tiên là gì? → A: Chỉ hỗ trợ SMB/shared folder trong phạm vi đầu tiên.
- Q: Phạm vi truy cập thư mục SMB bắt đầu từ đâu? → A: Người dùng nhập server kèm share/root ban đầu, sau đó duyệt các thư mục con.
- Q: Hành vi thumbnail cho video LAN là gì? → A: Tải lười thumbnail theo danh sách đang xem và lưu lại để dùng lại lần sau.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Quản lý server LAN đã lưu (Priority: P1)

Người dùng muốn thêm và lưu thông tin server trong mạng LAN để không phải nhập lại mỗi lần mở ứng dụng.

**Why this priority**: Đây là điểm vào bắt buộc trước khi người dùng có thể duyệt thư mục hoặc phát video từ mạng LAN.

**Independent Test**: Có thể kiểm thử độc lập bằng cách thêm một server mới, đóng/mở lại ứng dụng, xác nhận server vẫn xuất hiện và có thể chỉnh sửa hoặc xóa.

**Acceptance Scenarios**:

1. **Given** người dùng chưa có server LAN đã lưu, **When** nhập địa chỉ IP hoặc tên server, share/root ban đầu, tên hiển thị, username và password rồi lưu, **Then** server mới xuất hiện trong danh sách server LAN.
2. **Given** người dùng đã lưu một server, **When** đóng và mở lại ứng dụng, **Then** server đó vẫn có trong danh sách để dùng lại.
3. **Given** người dùng chọn một server đã lưu, **When** chỉnh sửa tên hiển thị hoặc thông tin đăng nhập, **Then** thông tin mới được dùng cho các lần mở sau.
4. **Given** người dùng không còn cần một server, **When** xóa server đó, **Then** server không còn xuất hiện trong danh sách và các lối tắt phụ thuộc được xử lý rõ ràng.

### User Story 2 - Duyệt thư mục và phát video LAN như local (Priority: P1)

Người dùng muốn mở một server LAN đã lưu, duyệt thư mục tương tự khi duyệt video local, nhìn thấy thumbnail và chọn video để phát.

**Why this priority**: Giá trị chính của tính năng là phát video từ thư mục trong mạng LAN mà không cần sao chép video về thiết bị trước.

**Independent Test**: Có thể kiểm thử độc lập bằng cách mở một server hợp lệ, đi vào thư mục có video, xác nhận danh sách thư mục/video hiển thị, thumbnail hoặc placeholder xuất hiện, và chọn video để phát.

**Acceptance Scenarios**:

1. **Given** server LAN hợp lệ và đang truy cập được, **When** người dùng mở server, **Then** ứng dụng hiển thị danh sách thư mục và tệp video mà người dùng có quyền xem.
2. **Given** thư mục LAN có nhiều video, **When** danh sách được hiển thị, **Then** mỗi video có tên, dấu hiệu nhận biết là video, và thumbnail nếu có thể tạo hoặc lấy được.
3. **Given** thumbnail chưa sẵn sàng hoặc không thể hiển thị, **When** danh sách vẫn đang tải, **Then** ứng dụng dùng placeholder ổn định thay vì chặn việc duyệt thư mục và cập nhật thumbnail khi sẵn sàng.
4. **Given** người dùng chọn một video trong thư mục LAN, **When** video được mở, **Then** ứng dụng bắt đầu phát bằng trải nghiệm phát video hiện có.
5. **Given** server không truy cập được, thông tin đăng nhập sai, hoặc người dùng không có quyền vào thư mục, **When** người dùng cố mở server hoặc thư mục, **Then** ứng dụng hiển thị lỗi dễ hiểu và cho phép thử lại hoặc sửa thông tin server.

### User Story 3 - Bookmark thư mục LAN đang xem (Priority: P2)

Người dùng muốn đánh dấu thư mục LAN đang xem để lần sau mở nhanh mà không phải duyệt lại từ đầu.

**Why this priority**: Sau khi chức năng duyệt và phát hoạt động, bookmark giảm số thao tác cho các thư mục thường xem.

**Independent Test**: Có thể kiểm thử độc lập bằng cách mở một thư mục LAN, tạo bookmark, đóng/mở lại ứng dụng, chọn bookmark và xác nhận thư mục đó mở trực tiếp.

**Acceptance Scenarios**:

1. **Given** người dùng đang xem một thư mục trên server LAN, **When** chọn bookmark thư mục hiện tại, **Then** thư mục được thêm vào danh sách bookmark với tên dễ nhận biết.
2. **Given** người dùng đã có bookmark thư mục LAN, **When** mở danh sách bookmark và chọn bookmark đó, **Then** ứng dụng mở trực tiếp thư mục đã đánh dấu nếu server còn truy cập được.
3. **Given** người dùng tạo bookmark cho thư mục đã được đánh dấu trước đó, **When** lưu bookmark, **Then** ứng dụng không tạo bản trùng lặp và cho người dùng biết bookmark đã tồn tại hoặc được cập nhật.
4. **Given** server hoặc thư mục của bookmark không còn truy cập được, **When** người dùng mở bookmark, **Then** ứng dụng hiển thị thông báo lỗi và cho phép sửa server, thử lại hoặc xóa bookmark.

### Edge Cases

- Server LAN tắt, đổi địa chỉ, hoặc nằm ngoài mạng hiện tại.
- Username hoặc password sai, thiếu quyền đọc thư mục, hoặc phiên truy cập hết hiệu lực.
- Thư mục rỗng, chỉ có thư mục con, hoặc chứa tệp không phải video.
- Thư mục có số lượng mục lớn khiến thumbnail chưa thể hiển thị ngay.
- Thumbnail không thể tạo hoặc không có dữ liệu phù hợp cho một số video.
- Thumbnail đã lưu không còn phù hợp vì video bị đổi, xóa hoặc thay thế trên server.
- Người dùng xóa server đang được bookmark tham chiếu.
- Người dùng đổi tên server nhưng bookmark cũ vẫn cần nhận diện đúng server.
- Đường dẫn thư mục đã bookmark bị đổi tên hoặc bị xóa trên server.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Ứng dụng MUST cho phép người dùng thêm server SMB/shared folder trong mạng LAN bằng các thông tin: địa chỉ IP hoặc tên server, share/root ban đầu, tên hiển thị, username và password.
- **FR-002**: Ứng dụng MUST xác thực dữ liệu bắt buộc trước khi lưu server, tối thiểu gồm địa chỉ server, share/root ban đầu và tên hiển thị.
- **FR-003**: Ứng dụng MUST cho phép xem danh sách server LAN đã lưu, chỉnh sửa thông tin server và xóa server.
- **FR-004**: Ứng dụng MUST lưu server LAN đã cấu hình để người dùng có thể dùng lại sau khi đóng và mở lại ứng dụng.
- **FR-005**: Ứng dụng MUST không hiển thị password đã lưu dưới dạng văn bản rõ trong các màn hình quản lý server.
- **FR-006**: Ứng dụng MUST cho phép người dùng mở một server SMB/shared folder đã lưu tại share/root ban đầu và duyệt các thư mục con mà người dùng có quyền truy cập.
- **FR-007**: Ứng dụng MUST hiển thị thư mục và tệp video trong thư mục LAN theo cách nhất quán với trải nghiệm duyệt tệp local hiện có.
- **FR-008**: Ứng dụng MUST tải lười thumbnail cho tệp video LAN theo danh sách đang xem; nếu thumbnail chưa sẵn sàng hoặc không thể hiển thị, MUST hiển thị placeholder ổn định để người dùng vẫn duyệt được danh sách.
- **FR-009**: Ứng dụng MUST lưu lại thumbnail LAN đã tạo hoặc lấy được để tái sử dụng trong các lần mở sau, trừ khi thumbnail không còn hợp lệ.
- **FR-010**: Ứng dụng MUST cho phép người dùng chọn video trong thư mục LAN để phát mà không yêu cầu thao tác sao chép thủ công về thiết bị trước.
- **FR-011**: Ứng dụng MUST hiển thị lỗi có thể hành động khi server không truy cập được, thông tin đăng nhập sai, hoặc thư mục bị từ chối quyền truy cập.
- **FR-012**: Ứng dụng MUST cho phép người dùng bookmark thư mục LAN hiện đang xem.
- **FR-013**: Ứng dụng MUST lưu bookmark thư mục LAN để người dùng có thể mở lại sau khi đóng và mở lại ứng dụng.
- **FR-014**: Mỗi bookmark thư mục LAN MUST liên kết rõ với server tương ứng và đường dẫn thư mục đã đánh dấu.
- **FR-015**: Ứng dụng MUST cho phép người dùng mở nhanh thư mục LAN từ danh sách bookmark.
- **FR-016**: Ứng dụng MUST tránh tạo bookmark trùng lặp cho cùng một server và cùng một thư mục.
- **FR-017**: Ứng dụng MUST xử lý rõ ràng các bookmark không còn hợp lệ do server bị xóa, server không truy cập được, hoặc thư mục không còn tồn tại.

### Key Entities *(include if feature involves data)*

- **LAN Server Profile**: Server do người dùng cấu hình, gồm tên hiển thị, địa chỉ server, share/root ban đầu, thông tin đăng nhập, trạng thái sử dụng và thời điểm cập nhật.
- **LAN Folder**: Thư mục trên server LAN mà người dùng có thể duyệt, gồm server liên quan, đường dẫn và danh sách mục con có thể truy cập.
- **LAN Media Item**: Tệp video hoặc thư mục con hiển thị trong trình duyệt LAN, gồm tên, loại mục, vị trí trong thư mục, trạng thái thumbnail và khả năng tái sử dụng thumbnail đã lưu.
- **Folder Bookmark**: Lối tắt do người dùng tạo cho một thư mục LAN, gồm tên hiển thị, server liên quan, đường dẫn thư mục và thời điểm tạo/cập nhật.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% server LAN được lưu hợp lệ vẫn xuất hiện sau khi người dùng đóng và mở lại ứng dụng.
- **SC-002**: Người dùng có thể đi từ một server LAN đã lưu đến thao tác phát video trong tối đa 4 bước sau khi chọn server, với server và thư mục đang truy cập được.
- **SC-003**: Với thư mục LAN có tối đa 500 mục, danh sách mục có thể thao tác được trong vòng 5 giây trên kết nối LAN ổn định.
- **SC-004**: Ít nhất 90% mục video đang nhìn thấy hiển thị thumbnail hoặc placeholder ổn định trong vòng 3 giây sau khi thư mục xuất hiện, và thumbnail đã từng hiển thị được tái sử dụng khi mở lại cùng thư mục.
- **SC-005**: 100% trường hợp server offline, sai thông tin đăng nhập hoặc bị từ chối quyền truy cập hiển thị thông báo lỗi rõ ràng và không làm ứng dụng thoát ngoài ý muốn.
- **SC-006**: Người dùng có thể tạo bookmark cho thư mục hiện tại và mở lại bookmark đó trong vòng 10 giây khi server còn truy cập được.
- **SC-007**: Trong kiểm thử người dùng, ít nhất 90% người dùng có thể thêm server, mở thư mục LAN, bookmark thư mục và mở lại bookmark mà không cần hướng dẫn ngoài giao diện.

## Assumptions

- Người dùng biết sẵn địa chỉ server LAN và thông tin đăng nhập cần thiết.
- Người dùng biết sẵn share/root ban đầu cần mở trên server SMB; ứng dụng không bắt buộc phải liệt kê toàn bộ share trên server trong phiên bản đầu tiên.
- Phạm vi tính năng tập trung vào SMB/shared folder trong mạng LAN; không bao gồm tự động quét toàn bộ thiết bị trong mạng hoặc hỗ trợ các loại server LAN khác trong phiên bản đầu tiên.
- Video được phát bằng trải nghiệm phát video hiện có của ứng dụng sau khi người dùng chọn tệp.
- Thumbnail có thể dùng placeholder khi không thể hiển thị ngay hoặc không thể tạo từ video.
- Thông tin server và bookmark được lưu trên thiết bị của người dùng để phục vụ lần sử dụng sau.
