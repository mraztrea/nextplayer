# Đặc tả Tính năng: Nhận dạng và Dịch Audio Offline

**Nhánh tính năng**: `007-offline-asr-translation`  
**Ngày tạo**: 2026-06-10  
**Trạng thái**: Draft  
**Mô tả đầu vào**: "Cho phép transcript audio và dịch sang tiếng Việt/Anh tương tự phụ đề dịch thời gian thực, nhưng chạy offline trên điện thoại Android. Cho phép tự động tải model hoặc import thủ công."

## Clarifications

### Session 2026-06-10

- Q: Phạm vi ngôn ngữ v1 là gì? → A: Tập trung transcript tiếng Nhật, dịch sang tiếng Việt hoặc tiếng Anh.
- Q: Quy tắc mạng khi chạy phiên offline là gì? → A: Phiên offline không dùng mạng; chỉ tải model trước phiên.
- Q: Quy tắc import model thủ công là gì? → A: Chỉ import gói model được hỗ trợ, có manifest/metadata hợp lệ.
- Q: Thành phần gói model v1 là gì? → A: Một gói đầy đủ gồm transcript tiếng Nhật và dịch Nhật -> Việt/Anh.
- Q: Chính sách tải model tự động là gì? → A: Luôn hỏi xác nhận; mặc định chỉ tải qua Wi-Fi.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Bật phụ đề offline khi xem video (Priority: P1)

Người dùng đang xem video trên điện thoại Android muốn thấy lời thoại được nhận dạng và hiển thị thành phụ đề mà không cần kết nối internet sau khi đã chuẩn bị model offline.

**Lý do ưu tiên**: Đây là giá trị cốt lõi của tính năng. Nếu không tạo được transcript offline từ audio đang phát, các phần dịch và quản lý model không có ý nghĩa.

**Kiểm thử độc lập**: Có thể kiểm thử bằng cách chuẩn bị model hợp lệ, bật chế độ offline, chuyển thiết bị sang chế độ không có mạng, phát video có lời thoại và xác minh phụ đề gốc xuất hiện trên màn hình.

1. **Cho trước** model offline đã sẵn sàng và video có lời thoại đang phát, **Khi** người dùng bật phụ đề offline, **Thì** transcript gốc xuất hiện theo tiến trình audio mà không yêu cầu internet.
2. **Cho trước** phụ đề offline đang hoạt động, **Khi** người dùng tắt tính năng, **Thì** phụ đề biến mất và quá trình xử lý audio dừng.
3. **Cho trước** chưa có model sẵn sàng, **Khi** người dùng bật phụ đề offline, **Thì** ứng dụng hiển thị hướng dẫn tải model hoặc import thủ công thay vì bắt đầu phiên lỗi.

### User Story 2 - Chuẩn bị model bằng tải tự động hoặc import thủ công (Priority: P1)

Người dùng muốn tự chọn cách chuẩn bị model offline: để ứng dụng tải gói model phù hợp hoặc import model đã có sẵn từ bộ nhớ thiết bị.

**Lý do ưu tiên**: Model là điều kiện tiên quyết cho chế độ offline. Người dùng cần một luồng rõ ràng, kiểm soát được dung lượng và biết khi nào tính năng đã sẵn sàng.

**Kiểm thử độc lập**: Có thể kiểm thử bằng cách vào cài đặt offline subtitle, tải một model được đề xuất, xóa model, rồi import lại một gói model hợp lệ từ thiết bị.

1. **Cho trước** người dùng chưa có model, **Khi** họ chọn tải tự động, **Thì** ứng dụng hiển thị dung lượng, tiến trình tải, trạng thái hoàn tất và đánh dấu model là sẵn sàng.
2. **Cho trước** người dùng có gói model hợp lệ trong bộ nhớ thiết bị, **Khi** họ import thủ công, **Thì** ứng dụng kiểm tra gói model và đánh dấu model là sẵn sàng nếu hợp lệ.
3. **Cho trước** tải hoặc import bị gián đoạn, **Khi** người dùng quay lại màn hình cài đặt, **Thì** ứng dụng hiển thị trạng thái thất bại có thể thử lại và không dùng model hỏng.

### User Story 3 - Dịch transcript sang tiếng Việt hoặc tiếng Anh (Priority: P2)

Người dùng xem nội dung tiếng Nhật muốn đọc bản dịch tiếng Việt hoặc tiếng Anh trực tiếp trên màn hình, với tùy chọn hiển thị chỉ bản gốc, chỉ bản dịch, hoặc song ngữ.

**Lý do ưu tiên**: Dịch là giá trị chính sau khi transcript offline hoạt động. Chế độ song ngữ hữu ích cho học ngoại ngữ và kiểm tra nghĩa.

**Kiểm thử độc lập**: Có thể kiểm thử bằng cách chọn ngôn ngữ đích là tiếng Việt hoặc tiếng Anh, phát video có lời thoại tiếng Nhật và xác minh bản dịch xuất hiện đúng chế độ hiển thị.

1. **Cho trước** transcript tiếng Nhật offline đã tạo được và người dùng chọn tiếng Việt làm ngôn ngữ đích, **Khi** lời thoại được chốt, **Thì** bản dịch tiếng Việt được hiển thị.
2. **Cho trước** transcript tiếng Nhật offline đã tạo được và người dùng chọn tiếng Anh làm ngôn ngữ đích, **Khi** lời thoại được chốt, **Thì** bản dịch tiếng Anh được hiển thị.
3. **Cho trước** chế độ song ngữ đang bật, **Khi** có transcript và bản dịch, **Thì** cả văn bản gốc và bản dịch cùng xuất hiện trong overlay phụ đề.

### User Story 4 - Duy trì phiên offline ổn định khi phát lâu (Priority: P3)

Người dùng xem video dài muốn phụ đề offline tiếp tục hoạt động ổn định khi tạm dừng, tua, đổi track audio hoặc phát trong thời gian dài.

**Lý do ưu tiên**: Đây là yêu cầu chất lượng giúp tính năng dùng được trong phim dài và nội dung học tập, nhưng có thể kiểm thử sau luồng chính.

**Kiểm thử độc lập**: Có thể kiểm thử bằng cách bật phụ đề offline trong phiên phát ít nhất 30 phút, thao tác tua/tạm dừng/tiếp tục và xác minh phụ đề không lẫn nội dung cũ.

1. **Cho trước** phụ đề offline đang hoạt động, **Khi** người dùng tua đến vị trí khác, **Thì** phụ đề cũ được xóa và transcript tiếp tục từ vị trí mới.
2. **Cho trước** video bị tạm dừng, **Khi** người dùng phát lại, **Thì** phiên phụ đề tiếp tục xử lý audio mới mà không hiển thị lại nội dung cũ.
3. **Cho trước** video dài đang phát, **Khi** phụ đề offline chạy hơn 30 phút, **Thì** ứng dụng vẫn phản hồi thao tác phát/tạm dừng/tắt phụ đề.

### Edge Cases

- Thiết bị không đủ dung lượng để tải hoặc import model.
- Người dùng hủy tải model giữa chừng hoặc mất mạng trong quá trình tải ban đầu.
- Người dùng đang dùng dữ liệu di động khi tải model; ứng dụng phải chờ xác nhận rõ ràng trước khi tải nếu người dùng cho phép vượt mặc định Wi-Fi.
- Thiết bị mất mạng sau khi model đã sẵn sàng; phiên phụ đề offline vẫn phải tiếp tục mà không cần chuyển sang xử lý trực tuyến.
- Gói model import sai định dạng, thiếu manifest/metadata, thiếu file, không tương thích hoặc bị hỏng.
- Audio không có lời nói rõ ràng, chỉ có nhạc/nhiễu hoặc nhiều người nói chồng lên nhau.
- Người dùng chọn ngôn ngữ đích không thuộc Việt/Anh hoặc dùng audio không phải tiếng Nhật trong phạm vi v1.
- Người dùng chuyển sang chế độ offline khi đang không có model sẵn sàng.
- Người dùng xóa model trong khi một phiên phụ đề offline đang hoạt động.
- Thiết bị vào chế độ tiết kiệm pin hoặc bị thiếu tài nguyên trong phiên phát dài.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống PHẢI cung cấp tùy chọn bật/tắt phụ đề offline trong trải nghiệm xem video.
- **FR-002**: Hệ thống PHẢI tạo transcript từ audio của nội dung đang phát sau khi model offline đã sẵn sàng, không yêu cầu internet trong phiên sử dụng.
- **FR-003**: Hệ thống PHẢI đặt tiếng Nhật là ngôn ngữ nguồn chính trong v1 và hiển thị rõ khi audio/ngôn ngữ không nằm trong phạm vi được hỗ trợ.
- **FR-004**: Hệ thống PHẢI hỗ trợ dịch transcript sang tiếng Việt và tiếng Anh.
- **FR-005**: Hệ thống PHẢI hỗ trợ các chế độ hiển thị: chỉ bản gốc, chỉ bản dịch, và song ngữ.
- **FR-006**: Hệ thống PHẢI cung cấp màn hình quản lý model offline với trạng thái: chưa có model, đang tải, đang import, sẵn sàng, lỗi, và cần cập nhật.
- **FR-007**: Hệ thống PHẢI cho phép tải tự động model được đề xuất, luôn hiển thị dung lượng và yêu cầu người dùng xác nhận trước khi tải; mặc định chỉ tải qua Wi-Fi.
- **FR-008**: Hệ thống PHẢI cho phép import thủ công gói model được hỗ trợ từ bộ nhớ thiết bị và chỉ đánh dấu sẵn sàng khi manifest/metadata hợp lệ.
- **FR-009**: Hệ thống PHẢI ngăn sử dụng model hỏng, thiếu manifest/metadata, import dở dang hoặc không tương thích.
- **FR-010**: Hệ thống PHẢI xem model v1 là một gói đầy đủ gồm khả năng transcript tiếng Nhật và dịch Nhật sang tiếng Việt/tiếng Anh; gói thiếu một phần không được đánh dấu sẵn sàng.
- **FR-011**: Hệ thống PHẢI cho phép người dùng xem dung lượng model đang dùng và xóa model khi không còn cần.
- **FR-012**: Hệ thống PHẢI hiển thị lỗi thân thiện khi không thể tải, import, nhận dạng hoặc dịch, kèm hành động khắc phục phù hợp.
- **FR-013**: Hệ thống PHẢI xóa hoặc reset trạng thái phụ đề khi người dùng tua, đổi media item, đổi track audio hoặc tắt tính năng.
- **FR-014**: Hệ thống PHẢI đảm bảo phiên phụ đề offline không tạo kết nối mạng để nhận dạng, dịch, gửi audio, gửi transcript hoặc gửi bản dịch ra ngoài thiết bị; mạng chỉ được dùng cho bước tải model trước phiên.
- **FR-015**: Hệ thống PHẢI lưu lại lựa chọn của người dùng về model, ngôn ngữ và chế độ hiển thị qua các lần mở lại ứng dụng.
- **FR-016**: Hệ thống PHẢI ngăn tính năng làm hỏng trải nghiệm phát video chính; người dùng luôn có thể tắt phụ đề offline nếu thiết bị không đáp ứng tốt.

### Key Entities *(include if feature involves data)*

- **OfflineModel**: Đại diện cho một gói model đầy đủ có thể chạy trên thiết bị, gồm tên hiển thị, manifest/metadata, khả năng transcript tiếng Nhật, khả năng dịch Nhật sang Việt/Anh, dung lượng, trạng thái sẵn sàng, phiên bản và thông tin tương thích.
- **ModelPreparationTask**: Đại diện cho tác vụ tải tự động hoặc import thủ công, gồm nguồn model, tiến trình, trạng thái, lỗi gần nhất và khả năng thử lại/hủy.
- **OfflineSubtitleSession**: Đại diện cho một phiên phụ đề offline đang hoạt động, gồm media item hiện tại, trạng thái bật/tắt, ngôn ngữ nguồn/đích, chế độ hiển thị và trạng thái xử lý.
- **TranscriptSegment**: Đại diện cho một đoạn lời nói đã nhận dạng, gồm văn bản gốc, bản dịch nếu có, trạng thái tạm thời/đã chốt, thời điểm hiển thị và mức tin cậy nếu có.
- **OfflineSubtitleSettings**: Đại diện cho lựa chọn người dùng về model mặc định, ngôn ngữ nguồn, ngôn ngữ đích, chế độ hiển thị, quyền tải model qua mạng và mặc định tải qua Wi-Fi.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Sau khi model đã sẵn sàng, người dùng có thể bật phụ đề offline và thấy transcript đầu tiên trong vòng 5 giây kể từ khi audio có lời nói trên thiết bị được hỗ trợ.
- **SC-002**: Người dùng có thể sử dụng transcript và dịch trong chế độ không có internet ít nhất 30 phút liên tục mà không cần kết nối lại mạng.
- **SC-003**: Người dùng có thể chuẩn bị model bằng một trong hai cách, tải tự động hoặc import thủ công, với xác nhận dung lượng trước khi tải và trạng thái hoàn tất/lỗi rõ ràng trong 100% trường hợp kiểm thử.
- **SC-004**: Ít nhất 90% thao tác tua, tạm dừng, tiếp tục và tắt phụ đề không để lại phụ đề cũ sai ngữ cảnh trên màn hình.
- **SC-005**: Khi phát audio tiếng Nhật và chọn tiếng Việt hoặc tiếng Anh làm ngôn ngữ đích, người dùng thấy bản dịch tương ứng trong vòng 3 giây sau khi đoạn transcript được chốt trên thiết bị được hỗ trợ.
- **SC-006**: Trong kiểm thử quyền riêng tư, phiên phụ đề offline không tạo kết nối mạng để xử lý nhận dạng/dịch hoặc gửi audio, transcript, bản dịch ra ngoài thiết bị.
- **SC-007**: Người dùng có thể tìm thấy trạng thái model hiện tại và hành động tiếp theo cần làm trong tối đa 2 thao tác từ màn hình cài đặt phụ đề/dịch.

## Assumptions

- Thiết bị mục tiêu là điện thoại Android có đủ dung lượng và tài nguyên để chạy model offline nhỏ phù hợp với thiết bị.
- Tải tự động model chỉ cần internet trong bước chuẩn bị ban đầu; phiên transcript/dịch sau đó hoạt động offline hoàn toàn khi model đã sẵn sàng.
- Import thủ công yêu cầu người dùng có sẵn gói model đầy đủ, được hỗ trợ và có manifest/metadata hợp lệ trên thiết bị.
- Phiên bản đầu tập trung vào audio của nội dung đang phát trong ứng dụng, không xử lý microphone hoặc audio hệ thống bên ngoài ứng dụng.
- Phiên bản đầu tập trung vào tiếng Nhật làm ngôn ngữ nguồn; tiếng Việt và tiếng Anh là hai ngôn ngữ đích được hỗ trợ.
- Lưu/export transcript thành file và đọc bản dịch thành tiếng nói nằm ngoài phạm vi phiên bản đầu.
- Nếu thiết bị không đủ khả năng xử lý mượt, ứng dụng ưu tiên giữ trải nghiệm phát video ổn định và cho phép người dùng tắt phụ đề offline.
