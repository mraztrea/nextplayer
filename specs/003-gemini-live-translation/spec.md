# Đặc tả Tính năng: Phụ đề Dịch Thời gian Thực bằng Gemini Live

**Feature Branch**: `008-gemini-live-translation`  
**Created**: 2026-06-11  
**Status**: Draft  
**Input**: User description: "$speckit-specify Thêm tính năng dịch audio và tạo phụ đề realtime tương tự như Soniox nhưng sử dụng Gemini Live API. Hãy đọc tài liệu chính thức tại: https://ai.google.dev/gemini-api/docs/live-api/live-translate và https://github.com/google-gemini/gemini-live-api-examples để hiểu cách làm. Mong muốn: Có input nhập Google API (giống Soniox API); Tạo ra phụ đề realtime, khớp với audio, cho phép chọn ngôn ngữ đích để dịch: vn, en..."

## Clarifications

### Session 2026-06-11

- Q: Gemini Live sẽ thay thế Soniox hay tồn tại như một provider có thể chọn? → A: Gemini Live là provider bổ sung; người dùng chọn Soniox hoặc Gemini cho phụ đề realtime.
- Q: Khi audio đã cùng ngôn ngữ đích thì Gemini Live subtitle phải xử lý thế nào? → A: Vẫn hiển thị phụ đề/transcript bằng ngôn ngữ đích.
- Q: Khi Gemini Live lỗi, ứng dụng có tự động fallback sang Soniox không? → A: Không fallback tự động; báo lỗi Gemini và giữ lựa chọn provider.
- Q: Gemini Live subtitle được phép ghi log chẩn đoán gì để bảo vệ quyền riêng tư? → A: Không log audio/transcript; chỉ log metadata và lỗi.
- Q: Gemini Live phải hỗ trợ những chế độ hiển thị phụ đề nào? → A: Gemini bắt buộc hỗ trợ Chỉ bản dịch, Song ngữ, Chỉ bản gốc.

## Kịch bản Người dùng & Kiểm thử *(bắt buộc)*

### User Story 1 - Phụ đề Dịch Realtime khi Xem Video (Ưu tiên: P1)

Người dùng đang xem video có tiếng nói và bật phụ đề dịch Gemini Live để thấy văn bản dịch xuất hiện gần thời điểm lời nói trong audio.

**Why this priority**: Đây là giá trị chính của tính năng; nếu phụ đề không xuất hiện đúng lúc với audio thì tính năng không đạt mục tiêu.

**Independent Test**: Có thể kiểm thử bằng cách cấu hình khóa Google hợp lệ, phát một video có lời thoại rõ ràng, bật phụ đề dịch Gemini Live và xác nhận phụ đề dịch xuất hiện trong lúc video tiếp tục phát.

**Acceptance Scenarios**:

1. **Given** người dùng đã nhập khóa Google hợp lệ và chọn ngôn ngữ đích, **When** người dùng bật phụ đề dịch trong lúc video có lời nói đang phát, **Then** hệ thống hiển thị phụ đề dịch realtime trên player.
2. **Given** video có đoạn im lặng ngắn giữa các câu, **When** lời nói tiếp tục, **Then** phụ đề mới tiếp tục xuất hiện mà không cần người dùng bật lại tính năng.
3. **Given** phụ đề dịch đang chạy, **When** người dùng tắt phụ đề dịch, **Then** overlay phụ đề dịch biến mất và phiên dịch dừng lại.

### User Story 2 - Cấu hình Google API Key và Ngôn ngữ Đích (Ưu tiên: P1)

Người dùng nhập khóa Google riêng của họ, chọn Gemini Live làm provider phụ đề realtime khi muốn dùng Gemini, và chọn ngôn ngữ muốn dịch sang trước khi bật phụ đề realtime.

**Why this priority**: Tính năng phụ thuộc vào quyền truy cập dịch của người dùng; cấu hình phải rõ ràng để người dùng tự kiểm soát khóa và ngôn ngữ.

**Independent Test**: Có thể kiểm thử trong màn hình cài đặt bằng cách nhập khóa Google, chọn tiếng Việt hoặc tiếng Anh làm ngôn ngữ đích, lưu lại, mở lại ứng dụng và xác nhận lựa chọn vẫn còn.

**Acceptance Scenarios**:

1. **Given** người dùng chưa nhập khóa Google, **When** người dùng cố bật phụ đề dịch Gemini Live, **Then** hệ thống yêu cầu nhập khóa trước khi bắt đầu.
2. **Given** người dùng nhập khóa Google không hợp lệ hoặc hết quyền sử dụng, **When** hệ thống bắt đầu phiên dịch, **Then** hệ thống hiển thị lỗi dễ hiểu và không làm gián đoạn phát video.
3. **Given** người dùng chọn `vn` hoặc tiếng Việt trong danh sách ngôn ngữ, **When** phụ đề dịch chạy, **Then** hệ thống dịch sang tiếng Việt và hiển thị nhãn ngôn ngữ thân thiện cho người dùng.
4. **Given** người dùng chọn `en` hoặc English trong danh sách ngôn ngữ, **When** phụ đề dịch chạy, **Then** hệ thống dịch sang tiếng Anh và hiển thị nhãn ngôn ngữ thân thiện cho người dùng.
5. **Given** Gemini Live là provider đang được chọn, **When** người dùng đổi chế độ hiển thị giữa chỉ bản dịch, song ngữ và chỉ bản gốc, **Then** overlay phụ đề đổi theo chế độ đã chọn.

### User Story 3 - Phụ đề Khớp Trạng thái Player (Ưu tiên: P2)

Người dùng tua, tạm dừng, đổi video hoặc tiếp tục phát mà không thấy phụ đề cũ bị lẫn vào đoạn audio mới.

**Why this priority**: Phụ đề realtime phải bám theo nội dung đang nghe; phụ đề còn sót sau seek hoặc đổi media gây sai nghĩa và mất tin cậy.

**Independent Test**: Có thể kiểm thử bằng cách bật phụ đề dịch, tua đến đoạn khác, tạm dừng/tiếp tục và đổi media item, sau đó xác nhận overlay được reset đúng với audio hiện tại.

**Acceptance Scenarios**:

1. **Given** phụ đề dịch đang hiển thị, **When** người dùng tua đến vị trí khác trong cùng video, **Then** hệ thống xóa phụ đề tạm thời/cũ và chỉ hiển thị phụ đề từ vị trí mới.
2. **Given** phụ đề dịch đang chạy, **When** người dùng tạm dừng video, **Then** hệ thống không tạo thêm phụ đề mới từ phần audio đã dừng.
3. **Given** người dùng chuyển sang video khác, **When** video mới bắt đầu phát, **Then** phiên phụ đề cũ được đóng và phụ đề mới bắt đầu từ audio của video mới nếu tính năng vẫn đang bật.

### User Story 4 - Phản hồi Tạm thời và Lỗi Thân thiện (Ưu tiên: P2)

Người dùng thấy tín hiệu rằng hệ thống đang nghe/dịch và nhận được thông báo rõ ràng khi mạng, khóa hoặc dịch vụ bên ngoài gặp lỗi.

**Why this priority**: Dịch realtime có độ trễ và phụ thuộc mạng; phản hồi trạng thái giúp người dùng hiểu ứng dụng đang làm gì thay vì nghĩ rằng tính năng bị treo.

**Independent Test**: Có thể kiểm thử bằng video có lời nói, mạng chậm hoặc khóa sai để xác nhận trạng thái tạm thời, lỗi, và retry không phá trải nghiệm phát video.

**Acceptance Scenarios**:

1. **Given** audio có lời nói mới bắt đầu, **When** hệ thống đang chờ kết quả dịch hoàn chỉnh, **Then** overlay có thể hiển thị trạng thái tạm thời hoặc chỉ báo đang xử lý.
2. **Given** mạng bị mất trong lúc phụ đề dịch đang chạy, **When** hệ thống không thể tiếp tục dịch, **Then** người dùng thấy thông báo lỗi ngắn gọn và video vẫn phát bình thường.
3. **Given** dịch vụ tạm thời không phản hồi, **When** hệ thống khôi phục được kết nối, **Then** phụ đề tiếp tục từ audio hiện tại thay vì phát lại phụ đề cũ.
4. **Given** Gemini Live là provider đang được chọn, **When** phiên Gemini Live gặp lỗi, **Then** hệ thống báo lỗi Gemini và không tự động chuyển sang Soniox.

### Trường hợp Biên

- Audio đầu vào đã cùng ngôn ngữ với ngôn ngữ đích: hệ thống phải vẫn hiển thị phụ đề/transcript bằng ngôn ngữ đích thay vì để trống hoặc khiến người dùng tưởng tính năng hỏng.
- Video có nhiều người nói, giọng nặng vùng miền, chuyển ngôn ngữ nhanh hoặc tiếng nền lớn: hệ thống phải tiếp tục hoạt động và tránh hiển thị lỗi kỹ thuật không cần thiết.
- Người dùng đổi ngôn ngữ đích khi phiên dịch đang chạy: hệ thống phải áp dụng lựa chọn mới theo cách rõ ràng và không trộn phụ đề của hai ngôn ngữ.
- Khóa Google bị thiếu, sai, hết hạn mức hoặc không có quyền dùng dịch realtime: hệ thống phải chặn bắt đầu phiên dịch và hướng dẫn người dùng kiểm tra lại cấu hình.
- Gemini Live gặp lỗi trong khi Soniox cũng đã được cấu hình: hệ thống phải giữ nguyên provider Gemini Live đã chọn và không tự động fallback sang Soniox.
- Ứng dụng vào nền, màn hình khóa, hoặc audio bị gián đoạn: hệ thống phải dừng/khôi phục phiên dịch phù hợp với trạng thái phát hiện tại.
- Dịch vụ bên ngoài trả về transcript gốc nhưng chưa có bản dịch hoàn chỉnh: hệ thống phải không ghép sai bản dịch với đoạn audio khác.

## Yêu cầu *(bắt buộc)*

### Yêu cầu Chức năng

- **FR-001**: Hệ thống PHẢI cung cấp giao diện cài đặt để người dùng nhập, cập nhật, kiểm tra và xóa Google API Key dùng cho phụ đề dịch Gemini Live.
- **FR-002**: Hệ thống PHẢI lưu Google API Key theo cách an toàn tương đương hoặc tốt hơn cấu hình Soniox API Key hiện có.
- **FR-003**: Hệ thống PHẢI cho phép người dùng chọn ngôn ngữ đích từ danh sách ngôn ngữ được hỗ trợ, bao gồm tối thiểu tiếng Việt và tiếng Anh.
- **FR-004**: Hệ thống PHẢI hiển thị tên ngôn ngữ thân thiện và chấp nhận các alias phổ biến như `vn` cho tiếng Việt và `en` cho tiếng Anh trong trải nghiệm cấu hình.
- **FR-005**: Hệ thống PHẢI cho phép người dùng chọn Soniox hoặc Gemini Live làm provider phụ đề realtime trước hoặc trong ngữ cảnh xem video.
- **FR-006**: Hệ thống PHẢI cho phép người dùng bật/tắt phụ đề dịch Gemini Live trong ngữ cảnh xem video mà không phải rời player.
- **FR-007**: Hệ thống PHẢI chỉ dịch audio thuộc media item đang phát trong ứng dụng, không thu âm môi trường xung quanh hoặc audio từ ứng dụng khác.
- **FR-008**: Hệ thống PHẢI tạo phụ đề realtime từ audio đang phát và hiển thị trên overlay của player.
- **FR-009**: Hệ thống PHẢI căn phụ đề với audio hiện tại để phụ đề cũ không xuất hiện sau khi người dùng tua, đổi video hoặc khôi phục phát lại.
- **FR-010**: Hệ thống PHẢI hỗ trợ ba chế độ hiển thị cho Gemini Live: "Chỉ bản dịch", "Song ngữ", và "Chỉ bản gốc".
- **FR-011**: Hệ thống PHẢI hiển thị trạng thái đang xử lý khi có audio mới nhưng bản dịch cuối cùng chưa sẵn sàng.
- **FR-012**: Hệ thống PHẢI xử lý lỗi khóa, quyền sử dụng, mạng, hạn mức, ngôn ngữ không hỗ trợ và dịch vụ không phản hồi bằng thông báo thân thiện với người dùng.
- **FR-013**: Hệ thống PHẢI tiếp tục phát video bình thường khi phiên phụ đề dịch gặp lỗi.
- **FR-014**: Hệ thống PHẢI cho phép người dùng đổi ngôn ngữ đích đã lưu và đảm bảo phiên dịch mới dùng lựa chọn mới.
- **FR-015**: Hệ thống PHẢI dọn trạng thái phụ đề tạm thời và phụ đề cuối cùng khi phiên dịch bị reset do seek, đổi media, tắt tính năng hoặc lỗi không thể khôi phục.
- **FR-016**: Hệ thống PHẢI tránh lưu nội dung audio hoặc transcript lâu dài ngoài nhu cầu hiển thị realtime, trừ khi người dùng bật một tính năng lưu lịch sử riêng trong tương lai.
- **FR-017**: Hệ thống PHẢI phân biệt rõ cấu hình Soniox và cấu hình Gemini Live để người dùng biết provider nào đang được sử dụng.
- **FR-018**: Hệ thống PHẢI cung cấp trạng thái hoạt động rõ ràng cho phiên phụ đề Gemini Live: chưa cấu hình, sẵn sàng, đang kết nối, đang dịch, lỗi, và đã tắt.
- **FR-019**: Hệ thống PHẢI vẫn hiển thị phụ đề/transcript bằng ngôn ngữ đích khi audio đầu vào đã cùng ngôn ngữ với ngôn ngữ đích.
- **FR-020**: Hệ thống PHẢI không tự động chuyển từ Gemini Live sang Soniox khi Gemini Live gặp lỗi; hệ thống phải báo lỗi Gemini và giữ lựa chọn provider hiện tại cho đến khi người dùng đổi thủ công.
- **FR-021**: Hệ thống PHẢI không ghi log audio hoặc transcript; log chẩn đoán chỉ được chứa metadata và lỗi như provider, ngôn ngữ đích, trạng thái phiên, mã lỗi và thời điểm reset.

### Thực thể Chính

- **SubtitleProviderSelection (Lựa chọn Provider Phụ đề)**: Lựa chọn của người dùng giữa Soniox và Gemini Live cho phiên phụ đề realtime hiện tại.
- **GeminiTranslationSettings (Cài đặt Dịch Gemini)**: Tùy chọn của người dùng cho provider Gemini Live, bao gồm trạng thái khóa Google, ngôn ngữ đích đã chọn, chế độ hiển thị và lựa chọn bật/tắt gần nhất.
- **ProviderCredential (Thông tin Xác thực Provider)**: Đại diện cho khóa dịch của người dùng và trạng thái hợp lệ của khóa, tách biệt giữa Soniox và Google.
- **RealtimeSubtitleSession (Phiên Phụ đề Realtime)**: Đại diện cho một phiên dịch đang hoạt động gắn với media item hiện tại, bao gồm trạng thái phiên, ngôn ngữ đích, lỗi gần nhất và thời điểm reset.
- **SubtitleSegment (Đoạn Phụ đề)**: Một đơn vị phụ đề hiển thị trên player, bao gồm văn bản gốc nếu có, văn bản dịch, trạng thái tạm thời/hoàn chỉnh, ngôn ngữ và thời điểm hiển thị tương ứng với audio.

## Tiêu chí Thành công *(bắt buộc)*

### Kết quả Đo lường được

- **SC-001**: Trong điều kiện mạng bình thường, phụ đề dịch đầu tiên xuất hiện trong vòng 3 giây kể từ khi lời nói rõ ràng bắt đầu trong audio.
- **SC-002**: Khi audio tiếp tục sau một đoạn im lặng dưới 30 giây, phụ đề dịch tiếp tục xuất hiện mà người dùng không cần bật lại tính năng.
- **SC-003**: Người dùng có thể nhập khóa Google, chọn ngôn ngữ đích và bật phụ đề dịch trong dưới 2 phút ở lần cấu hình đầu tiên.
- **SC-004**: Sau khi seek hoặc đổi video, phụ đề cũ biến mất trong vòng 1 giây và không còn xuất hiện lẫn với audio mới.
- **SC-005**: Tối thiểu 95% phiên xem thử nghiệm dài 30 phút không gặp lỗi khiến video bị dừng hoặc player bị thoát.
- **SC-006**: Tối thiểu 90% lỗi cấu hình phổ biến trong kiểm thử người dùng được hiển thị bằng thông báo có hành động rõ ràng, ví dụ nhập khóa, kiểm tra quyền, chọn lại ngôn ngữ hoặc thử lại mạng.
- **SC-007**: Phụ đề overlay vẫn đọc được trên nền video sáng/tối khác nhau và không che khuất các điều khiển player chính trong trạng thái phát bình thường.
- **SC-008**: Kiểm thử log chẩn đoán không phát hiện nội dung audio hoặc transcript trong log của phiên Gemini Live subtitle.
- **SC-009**: Người dùng có thể chuyển đổi giữa ba chế độ hiển thị Gemini Live trong lúc xem video mà không cần khởi động lại ứng dụng.

## Giả định

- Người dùng tự cung cấp Google API Key hợp lệ và chịu trách nhiệm về hạn mức/chi phí của khóa đó.
- Phạm vi v1 tập trung vào phụ đề dịch realtime; phát lại audio đã dịch không nằm trong phạm vi trừ khi được yêu cầu riêng.
- Tiếng Việt và tiếng Anh là ngôn ngữ bắt buộc trong danh sách ban đầu; các ngôn ngữ khác có thể hiển thị theo danh sách dịch realtime mà provider hỗ trợ.
- Khi người dùng nhập hoặc chọn `vn`, ứng dụng hiểu là tiếng Việt dù mã ngôn ngữ chuẩn của provider có thể khác.
- Tính năng kế thừa các nguyên tắc hiện có của phụ đề Soniox: audio lấy từ media đang phát, phụ đề overlay trên player, reset sạch khi seek và không thu âm ngoài ứng dụng.
- Dịch realtime phụ thuộc mạng và dịch vụ bên ngoài; ứng dụng cần thông báo lỗi rõ ràng nhưng không đảm bảo chất lượng dịch trong trường hợp tiếng nền lớn, accent nặng, hoặc chuyển ngôn ngữ liên tục.
