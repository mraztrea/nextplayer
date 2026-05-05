# Đặc tả Tính năng: Tối ưu Độ trễ Phụ đề Dịch với Lookahead Audio

**Nhánh tính năng**: `[002-lookahead-subtitle-latency]`  
**Ngày tạo**: 2026-05-05  
**Trạng thái**: Bản nháp  
**Mô tả đầu vào**: "Tối ưu độ trễ của subtitle dịch bằng cách phân tích pipeline Soniox hiện tại, xác nhận cơ chế STT + translation đang dùng, và bổ sung cơ chế lookahead audio extraction để phụ đề được dịch sớm nhưng chỉ hiển thị đúng tại media-time tương ứng."

## Làm rõ

### Phiên 2026-05-05

- Q: Hệ thống hiện tại có đợi nhận toàn bộ câu hoàn chỉnh rồi mới dịch không? → A: Không. Hệ thống hiện tại dùng cùng một phiên Soniox thời gian thực cho cả nhận dạng lời nói và dịch một chiều; văn bản gốc đã chốt và văn bản dịch đã chốt được trả về dần theo từng đoạn nói, không phải gom đủ toàn bộ câu rồi mới gọi một bước dịch riêng.
- Q: Cơ chế dịch nào đang được dùng? → A: Hệ thống hiện tại đang dùng cơ chế speech-to-text và translation tích hợp ngay trong phiên Soniox thời gian thực. Không có text-to-speech và không có API dịch tách rời nằm ngoài phiên đó.
- Q: Nút thắt chính gây độ trễ hiện tại là gì? → A: Audio hiện được lấy tại đúng vị trí đang phát và phụ đề được đưa lên màn hình ngay khi phản hồi quay về. Hệ thống chưa có lớp trích xuất audio đi trước, chưa gắn mốc media-time mục tiêu cho từng đoạn, và chưa trì hoãn hiển thị để đồng bộ theo vị trí phát thực tế.

## Kịch bản Người dùng & Kiểm thử *(bắt buộc)*

### User Story 1 - Phụ đề dịch sẵn sàng trước thời điểm lời nói phát ra (Ưu tiên: P1)

Người dùng xem video ngoại ngữ và muốn phụ đề dịch xuất hiện gần như ngay khi câu thoại bắt đầu phát, thay vì chờ thêm vài giây sau khi đã nghe xong một phần câu nói.

Hệ thống chuẩn bị trước audio của vài giây sắp phát, gửi sớm cho dịch vụ nhận dạng/dịch, nhưng chỉ hiển thị phụ đề khi video đi tới đúng thời điểm tương ứng. Người dùng nhận được trải nghiệm "phụ đề đã sẵn sàng" mà không bị lộ nội dung tương lai.

**Lý do ưu tiên**: Đây là giá trị cốt lõi của feature. Nếu không giảm được độ trễ cảm nhận mà vẫn giữ đồng bộ, yêu cầu tối ưu lần này không đạt mục tiêu.

**Kiểm thử độc lập**: Có thể kiểm thử bằng cách phát video có lời thoại liên tục, đo thời điểm lời nói xuất hiện và thời điểm phụ đề dịch được hiển thị, rồi xác nhận phụ đề không xuất hiện sớm hơn câu thoại tương ứng.

**Kịch bản Chấp nhận**:

1. **Cho trước** video đã phát đủ lâu để bộ đệm lookahead ổn định, **Khi** video đi tới một đoạn lời nói có phụ đề đã được chuẩn bị sẵn, **Thì** phụ đề dịch xuất hiện không muộn hơn 250 mili giây so với thời điểm câu thoại bắt đầu phát.
2. **Cho trước** phụ đề của một đoạn tương lai đã được dịch xong từ sớm, **Khi** vị trí phát hiện tại vẫn chưa tới mốc của đoạn đó, **Thì** phụ đề đó chưa được phép hiển thị trên màn hình.
3. **Cho trước** bộ đệm lookahead tạm thời chưa nạp kịp cho một đoạn sắp phát, **Khi** đoạn đó tới lượt hiển thị, **Thì** hệ thống ưu tiên tính đúng thời điểm, không hiển thị spoiler của đoạn sau và chỉ ảnh hưởng tới đoạn bị chậm.

---

### User Story 2 - Đồng bộ chính xác sau khi tua, đổi track, hoặc tạm dừng (Ưu tiên: P1)

Người dùng thường tua nhanh, tua lùi, tạm dừng, hoặc đổi track audio trong lúc xem. Họ muốn phụ đề dịch lập tức bỏ ngữ cảnh cũ và khôi phục theo vị trí mới mà không bị "bóng ma" phụ đề từ đoạn trước.

**Lý do ưu tiên**: Nếu lookahead không reset đúng khi seek hoặc đổi nguồn audio, phụ đề sẽ sai ngữ cảnh dù độ trễ có giảm. Đây là điều kiện bắt buộc để feature dùng được trong thực tế.

**Kiểm thử độc lập**: Có thể kiểm thử bằng cách bật phụ đề dịch, tua tới nhiều vị trí khác nhau, đổi track audio nếu có, và xác minh chỉ phụ đề thuộc vị trí mới được hiển thị.

**Kịch bản Chấp nhận**:

1. **Cho trước** phụ đề dịch đang hoạt động, **Khi** người dùng tua tới một vị trí khác trong video, **Thì** toàn bộ phụ đề đang đệm từ vị trí cũ bị loại bỏ và luồng phụ đề mới bắt đầu từ vị trí sau khi tua.
2. **Cho trước** phụ đề dịch đang hoạt động, **Khi** người dùng tạm dừng video trong một khoảng đủ dài, **Thì** hệ thống ngừng xử lý lookahead không cần thiết và không hiển thị thêm phụ đề mới cho tới khi phát tiếp.
3. **Cho trước** media item hoặc track audio thay đổi, **Khi** hệ thống nhận diện thay đổi đó, **Thì** bộ đệm phụ đề cũ không còn xuất hiện và phiên mới được dựng lại theo đúng nguồn audio mới.

---

### User Story 3 - Giảm độ trễ nhưng không làm nặng máy (Ưu tiên: P2)

Người dùng xem video dài trên thiết bị di động và mong muốn tính năng phụ đề dịch vẫn mượt, không làm video giật, không tăng tiêu hao pin quá mức, và không phình bộ nhớ theo thời gian.

**Lý do ưu tiên**: Tối ưu độ trễ chỉ có giá trị nếu không phá vỡ trải nghiệm phát video. Đây là yêu cầu chất lượng cần có trước khi feature sẵn sàng triển khai rộng.

**Kiểm thử độc lập**: Có thể kiểm thử bằng cách xem video liên tục trong thời gian dài với phụ đề lookahead bật và xác minh video vẫn phát ổn định trong khi phụ đề tiếp tục đồng bộ.

**Kịch bản Chấp nhận**:

1. **Cho trước** video đang phát liên tục với phụ đề lookahead bật, **Khi** bộ đệm tương lai đã đủ dữ liệu, **Thì** hệ thống tạm dừng việc nạp thêm cho đến khi mức đệm giảm xuống ngưỡng cho phép.
2. **Cho trước** video gần kết thúc hoặc không còn đủ dữ liệu tương lai để nạp trước, **Khi** hệ thống chạm cuối nội dung, **Thì** phụ đề còn lại vẫn được hiển thị đúng và pipeline tương lai tự kết thúc gọn gàng.
3. **Cho trước** thiết bị hoặc định dạng media không hỗ trợ trích xuất lookahead ổn định, **Khi** tính năng không thể chuẩn bị audio tương lai, **Thì** phát video vẫn tiếp tục bình thường và phụ đề chuyển sang trạng thái suy giảm an toàn thay vì làm hỏng phiên phát.

---

### Trường hợp Biên

- Điều gì xảy ra khi người dùng seek liên tiếp nhiều lần trong thời gian ngắn? Hệ thống cần hủy phần lookahead đang chờ, loại bỏ các phụ đề chưa tới lượt, và chỉ giữ lại trạng thái của vị trí seek cuối cùng.
- Điều gì xảy ra khi phản hồi dịch về trễ hơn cửa sổ lookahead? Hệ thống cần bỏ qua hoặc hiển thị muộn đúng đoạn bị ảnh hưởng, nhưng tuyệt đối không được hiển thị trước nội dung tương lai.
- Điều gì xảy ra khi Soniox trả về bản gốc và bản dịch lệch nhịp nhau? Hệ thống cần giữ ghép cặp ổn định theo cùng một đơn vị thời gian mục tiêu để tránh gán nhầm bản dịch.
- Điều gì xảy ra khi video không còn đủ 5 giây ở cuối file? Hệ thống cần thu hẹp cửa sổ tương lai theo phần nội dung còn lại mà không làm lỗi pipeline.
- Điều gì xảy ra khi người dùng đổi track audio hoặc media item giữa lúc bộ đệm tương lai chưa phát hết? Hệ thống cần coi đó như một lần reset hoàn toàn của phiên lookahead.
- Điều gì xảy ra khi pipeline tương lai không khởi tạo được trên một định dạng nhất định? Hệ thống cần suy giảm an toàn mà không ảnh hưởng tới phát video cốt lõi.

## Yêu cầu *(bắt buộc)*

### Yêu cầu Chức năng

- **FR-001**: Hệ thống PHẢI giữ nguyên phạm vi tích hợp hiện tại: speech-to-text và translation cùng nằm trong một phiên Soniox thời gian thực; không thêm đường dịch tách rời và không bao gồm text-to-speech trong feature này.
- **FR-002**: Hệ thống PHẢI chuẩn bị trước audio của chính media item đang phát bằng một pipeline lookahead song song, duy trì mục tiêu đi trước khoảng 5 giây so với vị trí phát hiện tại sau khi đã nạp ổn định.
- **FR-003**: Hệ thống PHẢI chuẩn hóa audio lookahead về cùng hợp đồng âm thanh mà phiên dịch thời gian thực hiện tại chấp nhận, để không tạo ra khác biệt về chất lượng đầu vào giữa đường hiện tại và đường tối ưu độ trễ.
- **FR-004**: Hệ thống PHẢI gửi audio tương lai đó vào cùng phiên speech-to-text và translation thời gian thực hiện có trong lúc phát video đang hoạt động.
- **FR-005**: Hệ thống PHẢI gắn cho mỗi đơn vị phụ đề đã chốt một mốc thời gian mục tiêu theo media-time của nội dung lời nói, thay vì dựa vào thời điểm phản hồi về tới thiết bị.
- **FR-006**: Hệ thống PHẢI lưu các đơn vị phụ đề đã chốt trong một bộ đệm đồng bộ và chỉ cho phép hiển thị khi vị trí phát hiện tại đi vào cửa sổ thời gian của đơn vị tương ứng.
- **FR-007**: Hệ thống PHẢI ngăn mọi hình thức spoiler từ lookahead; phụ đề gốc, phụ đề dịch, và trạng thái tạm thời không được hiển thị sớm hơn mốc media-time của câu thoại tương ứng.
- **FR-008**: Hệ thống PHẢI quyết định nội dung overlay dựa trên vị trí phát hiện tại của player, để cùng một đoạn phụ đề chỉ xuất hiện khi đến lượt và tự rời màn hình khi cửa sổ của nó kết thúc.
- **FR-009**: Hệ thống PHẢI loại bỏ hoặc tái lập toàn bộ bộ đệm phụ đề khi người dùng seek, đổi media item, hoặc đổi track audio; sau đó khởi động lại lookahead từ vị trí mới mà không để sót phụ đề cũ.
- **FR-010**: Hệ thống PHẢI tạm dừng hoạt động trích xuất và gửi audio tương lai khi video dừng phát đủ lâu hoặc bị pause, và PHẢI tiếp tục từ đúng vị trí tương lai mới khi playback tiếp diễn.
- **FR-011**: Hệ thống PHẢI giới hạn mức đệm lookahead, kích thước hàng đợi phụ đề, và thời gian sống của trạng thái trung gian để kiểm soát CPU, bộ nhớ, và pin trong các phiên xem dài.
- **FR-012**: Hệ thống PHẢI suy giảm an toàn khi không thể chuẩn bị audio tương lai hoặc không thể giữ đủ mức lookahead, sao cho phát video cốt lõi không bị gián đoạn.
- **FR-013**: Hệ thống PHẢI giữ thứ tự ghép cặp ổn định giữa bản gốc và bản dịch ngay cả khi chúng quay về lệch nhịp, muộn, hoặc không đầy đủ.
- **FR-014**: Hệ thống PHẢI cung cấp trạng thái đủ rõ để kiểm chứng việc nạp lookahead, độ lệch đồng bộ, tần suất reset sau seek, và các lần suy giảm an toàn trong quá trình xác minh tính năng.

### Thực thể Chính

- **LookaheadAudioWindow (Cửa sổ Audio Tương lai)**: Đại diện cho phần audio đã được chuẩn bị trước so với vị trí phát hiện tại. Theo dõi điểm bắt đầu, điểm kết thúc, độ phủ hiện có, và trạng thái sẵn sàng của cửa sổ.
- **TimedSubtitleUnit (Đơn vị Phụ đề Có mốc thời gian)**: Đại diện cho một đoạn phụ đề đã chốt, gồm văn bản gốc, văn bản dịch, mốc thời gian mục tiêu bắt đầu/kết thúc, và trạng thái sẵn sàng hiển thị.
- **SubtitleSyncBuffer (Bộ đệm Đồng bộ Phụ đề)**: Tập hợp các đơn vị phụ đề đã nhận nhưng chưa đến lượt hiển thị, chịu trách nhiệm loại bỏ dữ liệu cũ và trả về đúng đoạn cần render tại mỗi thời điểm.
- **LookaheadSessionState (Trạng thái Phiên Lookahead)**: Trạng thái hoạt động của phiên tối ưu độ trễ, bao gồm mức đệm hiện có, vị trí nguồn audio đang chuẩn bị, trạng thái reset, và trạng thái suy giảm an toàn nếu có.

## Tiêu chí Thành công *(bắt buộc)*

### Kết quả Đo lường được

- **SC-001**: Sau khi bộ đệm lookahead ổn định, ít nhất 90% các câu mẫu trong tập kiểm thử xuất hiện phụ đề dịch trong vòng 250 mili giây kể từ khi câu thoại tương ứng bắt đầu phát.
- **SC-002**: Độ trễ cảm nhận trung vị giữa lúc bắt đầu lời nói và lúc phụ đề dịch đầu tiên xuất hiện giảm ít nhất 40% so với bản build hiện tại khi đo trên cùng thiết bị và cùng tập video mẫu.
- **SC-003**: Sau mỗi thao tác seek thủ công, không còn phụ đề cũ bám trên màn hình quá 300 mili giây, và luồng phụ đề đúng của vị trí mới phục hồi trong vòng 2 giây với ít nhất 90% lượt thử.
- **SC-004**: Trong bài kiểm thử phát liên tục 30 phút, video không phát sinh giật hình mà người dùng nhận thấy do tính năng phụ đề lookahead, và ít nhất 95% các đoạn phụ đề đã hiển thị vẫn nằm trong cửa sổ đồng bộ mục tiêu.
- **SC-005**: Khi lookahead không nạp kịp hoặc không khả dụng, video vẫn phát bình thường và người dùng không bao giờ thấy subtitle của tương lai xuất hiện trước nội dung âm thanh tương ứng.

## Giả định

- Soniox cung cấp đủ dấu hiệu thời gian hoặc tương quan ổn định để mỗi đoạn phụ đề đã chốt có thể được gán về đúng media-time của nội dung lời nói.
- Nếu bản dịch không mang mốc thời gian riêng, hệ thống có thể kế thừa mốc của đoạn gốc tương ứng mà vẫn giữ đồng bộ hiển thị.
- Mục tiêu đi trước khoảng 5 giây là mặc định của tính năng này; trong giai đoạn khởi động, cuối video, hoặc sau seek, mức đệm thực tế có thể ngắn hơn tạm thời.
- Pipeline lấy audio tại đúng vị trí đang phát có thể tiếp tục tồn tại như đường suy giảm an toàn hoặc tương thích, nhưng không còn là chiến lược chính để đạt mục tiêu giảm độ trễ cảm nhận.
- Phạm vi lần này là speech-to-text và translation cho video đang phát cục bộ; text-to-speech nằm ngoài phạm vi.
- Người dùng đã cấu hình hợp lệ quyền truy cập dịch vụ Soniox và có kết nối mạng đủ dùng để nhận phụ đề thời gian thực.
