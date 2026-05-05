# Đặc tả Tính năng: Phụ đề Dịch Thời gian Thực bằng Soniox

**Nhánh tính năng**: `001-soniox-subtitle-translation`  
**Ngày tạo**: 2026-05-05  
**Trạng thái**: Bản nháp  
**Mô tả đầu vào**: "Tích hợp tính năng tạo và dịch phụ đề thời gian thực bằng Soniox AI vào nextplayer, bao gồm quản lý cấu hình, pipeline xử lý audio, logic xử lý phụ đề song ngữ, và overlay hiển thị trên trình phát video."

## Làm rõ

### Phiên 2026-05-05

- Q: Phương pháp trích xuất audio từ video? → A: MediaExtractor/MediaCodec — decode audio trực tiếp từ file video, không dùng AudioPlaybackCapture.

## Kịch bản Người dùng & Kiểm thử *(bắt buộc)*

### User Story 1 - Phụ đề Dịch Thời gian Thực khi Xem Video (Ưu tiên: P1)

Người dùng mở một video trong nextplayer có âm thanh nói bằng ngoại ngữ (ví dụ: tiếng Anh, tiếng Nhật). Họ muốn thấy phụ đề tiếng Việt xuất hiện theo thời gian thực khi video phát, mà không cần tìm và tải file phụ đề riêng.

Người dùng vào cài đặt phụ đề/dịch, nhập Soniox API Key, chọn ngôn ngữ nguồn, xác nhận tiếng Việt là ngôn ngữ đích, và chọn chế độ hiển thị "Chỉ bản dịch". Khi quay lại video và bật tính năng phụ đề trực tiếp, phụ đề đã dịch xuất hiện đè lên video trong vòng vài giây kể từ lời nói.

**Lý do ưu tiên**: Đây là giá trị cốt lõi — phụ đề dịch thời gian thực khi xem video. Không có tính năng này, toàn bộ feature không có ý nghĩa.

**Kiểm thử độc lập**: Có thể kiểm thử đầy đủ bằng cách phát bất kỳ video có âm thanh nói và xác minh phụ đề đã dịch xuất hiện trên màn hình gần như thời gian thực. Mang lại giá trị ngay lập tức cho người dùng xem nội dung ngoại ngữ.

**Kịch bản Chấp nhận**:

1. **Cho trước** video đang phát có lời nói tiếng Anh và người dùng đã cấu hình Soniox API Key với ngôn ngữ đích là tiếng Việt, **Khi** người dùng bật phụ đề trực tiếp, **Thì** phụ đề tiếng Việt xuất hiện đè lên video trong vòng 3 giây kể từ lời nói.
2. **Cho trước** tính năng phụ đề trực tiếp đang hoạt động, **Khi** người nói tạm dừng hơn 3 giây, **Thì** đoạn phụ đề hiện tại được chốt và một đoạn mới bắt đầu khi lời nói tiếp tục.
3. **Cho trước** tính năng phụ đề trực tiếp đang hoạt động, **Khi** người dùng tắt phụ đề trực tiếp, **Thì** overlay phụ đề biến mất và quá trình xử lý audio dừng ngay lập tức.

---

### User Story 2 - Cấu hình Soniox API và Tùy chọn Ngôn ngữ (Ưu tiên: P1)

Người dùng muốn thiết lập thông tin Soniox API và tùy chọn ngôn ngữ trước khi sử dụng tính năng phụ đề trực tiếp. Họ vào cài đặt ứng dụng, tìm phần cài đặt dịch phụ đề, nhập API key, chọn ngôn ngữ nguồn từ danh sách, chọn ngôn ngữ đích (mặc định là tiếng Việt), và chọn chế độ hiển thị mong muốn.

**Lý do ưu tiên**: Không có cấu hình đúng, pipeline dịch không thể hoạt động. Đây là điều kiện tiên quyết cho tính năng chính.

**Kiểm thử độc lập**: Có thể kiểm thử bằng cách vào cài đặt, nhập API key, chọn ngôn ngữ, chọn chế độ hiển thị, đóng và mở lại cài đặt, và xác minh tất cả giá trị được lưu đúng.

**Kịch bản Chấp nhận**:

1. **Cho trước** người dùng đang ở cài đặt ứng dụng, **Khi** họ vào phần cài đặt dịch phụ đề, **Thì** họ thấy các trường API Key, Ngôn ngữ Nguồn, Ngôn ngữ Đích, và Chế độ Hiển thị.
2. **Cho trước** người dùng nhập Soniox API Key hợp lệ và chọn ngôn ngữ, **Khi** họ lưu và mở lại cài đặt, **Thì** tất cả giá trị đã cấu hình được giữ lại và hiển thị đúng.
3. **Cho trước** người dùng chưa nhập API Key, **Khi** họ cố bật phụ đề trực tiếp, **Thì** ứng dụng hiển thị thông báo rõ ràng hướng dẫn cấu hình API Key trong cài đặt.

---

### User Story 3 - Hiển thị Phụ đề Song ngữ (Ưu tiên: P2)

Người học ngôn ngữ đang xem video muốn thấy đồng thời cả văn bản gốc và bản dịch tiếng Việt, để có thể so sánh và học từ vựng trong ngữ cảnh.

Người dùng chọn chế độ hiển thị "Song ngữ" trong cài đặt. Khi xem video, họ thấy cả văn bản gốc và bản dịch được hiển thị cùng nhau trên overlay. Văn bản gốc đã được nhận dạng nhưng chưa dịch sẽ hiển thị chỉ báo đang tải.

**Lý do ưu tiên**: Chế độ song ngữ mang lại giá trị đáng kể cho người học ngôn ngữ và người dùng chuyên nghiệp cần xác minh độ chính xác của bản dịch, nhưng nó được xây dựng trên nền tính năng dịch đơn ngữ cốt lõi.

**Kiểm thử độc lập**: Có thể kiểm thử bằng cách chọn chế độ song ngữ, phát video có lời nói ngoại ngữ, và xác minh cả văn bản gốc và bản dịch đều xuất hiện cùng nhau trên overlay.

**Kịch bản Chấp nhận**:

1. **Cho trước** chế độ song ngữ được chọn và tính năng phụ đề trực tiếp đang hoạt động, **Khi** lời nói được nhận dạng, **Thì** cả văn bản gốc và bản dịch tiếng Việt được hiển thị trên overlay.
2. **Cho trước** chế độ song ngữ đang hoạt động và văn bản gốc đã được chốt, **Khi** bản dịch chưa đến, **Thì** chỉ báo đang tải (ví dụ: "...") được hiển thị ở vị trí bản dịch.
3. **Cho trước** người dùng chuyển từ chế độ song ngữ sang chế độ chỉ bản dịch trong lúc phát, **Khi** thay đổi chế độ có hiệu lực, **Thì** chỉ phụ đề đã dịch được hiển thị từ thời điểm đó trở đi.

---

### User Story 4 - Hiển thị Văn bản Tạm thời (Đang xử lý) (Ưu tiên: P2)

Người dùng xem video với phụ đề trực tiếp muốn cảm nhận hệ thống đang phản hồi theo thời gian thực, ngay cả trước khi một câu hoàn chỉnh được nhận dạng và dịch.

Khi người nói đang nói giữa câu, văn bản "tạm thời" (provisional) mờ hoặc có kiểu hiển thị khác biệt xuất hiện, cho thấy những gì hệ thống đang nghe. Khi câu được chốt, văn bản tạm thời được thay thế bằng phụ đề gốc và/hoặc bản dịch đã xác nhận.

**Lý do ưu tiên**: Văn bản tạm thời tạo cảm giác độ trễ thấp và phản hồi nhanh, cải thiện đáng kể trải nghiệm người dùng. Tuy nhiên, tính năng vẫn hoạt động khi không có nó (người dùng chỉ chờ văn bản đã chốt).

**Kiểm thử độc lập**: Có thể kiểm thử bằng cách phát video và quan sát văn bản mờ xuất hiện trong lúc nói, sau đó chuyển sang văn bản chốt rõ ràng khi câu hoàn thành.

**Kịch bản Chấp nhận**:

1. **Cho trước** tính năng phụ đề trực tiếp đang hoạt động và người nói đang nói giữa câu, **Khi** lời nói một phần được nhận dạng, **Thì** văn bản tạm thời (mờ/in nghiêng) xuất hiện trên overlay.
2. **Cho trước** văn bản tạm thời đang hiển thị, **Khi** đoạn lời nói được chốt, **Thì** văn bản tạm thời được thay thế bằng văn bản phụ đề đã xác nhận.
3. **Cho trước** người nói ngừng nói, **Khi** văn bản cuối cùng được phát ra và không còn token tạm thời nào đến, **Thì** vùng văn bản tạm thời được xóa sạch.

---

### User Story 5 - Phiên Hoạt động Ổn định trong Thời gian Dài (Ưu tiên: P3)

Người dùng xem phim 2 tiếng với phụ đề trực tiếp được bật. Họ kỳ vọng tính năng phụ đề duy trì hoạt động suốt quá trình xem mà không bị gián đoạn, suy giảm, hoặc tích lũy bộ nhớ.

**Lý do ưu tiên**: Phiên dài là trường hợp sử dụng phổ biến trong thực tế, nhưng tính năng cốt lõi phải hoạt động cho phiên ngắn trước. Ổn định phiên là tối ưu hóa trên nền pipeline đã hoạt động.

**Kiểm thử độc lập**: Có thể kiểm thử bằng cách chạy tính năng phụ đề trực tiếp trong thời gian dài (30+ phút) và xác minh nó vẫn hoạt động mà không bị gián đoạn hoặc suy giảm hiệu suất.

**Kịch bản Chấp nhận**:

1. **Cho trước** tính năng phụ đề trực tiếp đã chạy hơn 30 phút, **Khi** cơ chế reset phiên kích hoạt, **Thì** phụ đề tiếp tục hoạt động mà không có gián đoạn nào người dùng nhận thấy.
2. **Cho trước** tính năng phụ đề trực tiếp đã chạy trong thời gian dài, **Khi** bộ đệm hiển thị đạt giới hạn, **Thì** các đoạn cũ bị cắt bỏ khỏi hiển thị mà không ảnh hưởng đến việc render phụ đề mới.
3. **Cho trước** video bị tạm dừng trong thời gian dài với phụ đề trực tiếp đang bật, **Khi** không có audio được gửi, **Thì** kết nối vẫn được duy trì thông qua tin nhắn keepalive và hoạt động bình thường khi video phát lại.

---

### Trường hợp Biên

- Điều gì xảy ra khi kết nối mạng bị mất giữa phiên? Hệ thống cần phát hiện mất kết nối, thông báo cho người dùng, và cố gắng tự động kết nối lại khi có mạng trở lại.
- Điều gì xảy ra khi Soniox API Key không hợp lệ hoặc hết hạn? Hệ thống cần hiển thị thông báo lỗi rõ ràng và hướng dẫn người dùng kiểm tra API Key trong cài đặt.
- Điều gì xảy ra khi audio không có lời nói (ví dụ: chỉ có nhạc)? Hệ thống không hiển thị phụ đề và không tích lũy các đoạn rỗng.
- Điều gì xảy ra khi người dùng tua đến vị trí khác trong video? Hệ thống cần xóa các đoạn phụ đề hiện có và khởi động lại nhận dạng từ vị trí audio mới.
- Điều gì xảy ra khi video chuyển track audio? Hệ thống cần phát hiện thay đổi và reset phiên phụ đề tương ứng.
- Điều gì xảy ra khi nhiều người nói cùng lúc? Hệ thống dựa vào tính năng phân biệt người nói (speaker diarization) của Soniox; phụ đề xuất hiện khi được nhận dạng, với nhãn gán người nói khi có sẵn.

## Yêu cầu *(bắt buộc)*

### Yêu cầu Chức năng

- **FR-001**: Hệ thống PHẢI cung cấp giao diện cài đặt để người dùng nhập và lưu trữ Soniox API Key một cách an toàn.
- **FR-002**: Hệ thống PHẢI cho phép người dùng chọn ngôn ngữ nguồn và ngôn ngữ đích (mặc định là tiếng Việt) để dịch.
- **FR-003**: Hệ thống PHẢI hỗ trợ ba chế độ hiển thị: "Chỉ bản gốc", "Chỉ bản dịch" (mặc định), và "Song ngữ".
- **FR-004**: Hệ thống PHẢI trích xuất luồng audio trực tiếp từ file video bằng cách decode (MediaExtractor/MediaCodec) và chuẩn hóa về định dạng 16 kHz mono PCM (s16le). Không sử dụng AudioPlaybackCapture.
- **FR-005**: Hệ thống PHẢI gửi dữ liệu audio theo batch (khoảng 200ms mỗi batch) tới Soniox qua kết nối WebSocket.
- **FR-006**: Hệ thống PHẢI phân tích luồng token Soniox sử dụng cờ `is_final` và `translation_status` để phân loại chính xác token thành bản gốc, bản dịch, hoặc tạm thời.
- **FR-007**: Hệ thống PHẢI hiển thị văn bản tạm thời (đang xử lý) với kiểu hiển thị khác biệt để cho biết lời nói đang được nhận dạng.
- **FR-008**: Hệ thống PHẢI sử dụng hàng đợi FIFO để ghép cặp văn bản gốc đã chốt với văn bản dịch tương ứng.
- **FR-009**: Hệ thống PHẢI render overlay phụ đề đè lên trình phát video, hỗ trợ cả bố cục hiển thị đơn ngữ và song ngữ.
- **FR-010**: Hệ thống PHẢI duy trì kết nối WebSocket bằng tin nhắn keepalive (mỗi 15 giây) trong các khoảng im lặng.
- **FR-011**: Hệ thống PHẢI triển khai cơ chế reset phiên để duy trì kết nối ổn định trong thời gian dài, đảm bảo không có gián đoạn mà người dùng nhận thấy.
- **FR-012**: Hệ thống PHẢI dọn dẹp các đoạn gốc tồn đọng (cũ hơn 10 giây hoặc vượt quá 3 đoạn đang chờ) để ngăn lệch hiển thị.
- **FR-013**: Hệ thống PHẢI tách biệt bộ đệm hiển thị (có thể cắt bỏ để tối ưu UI) khỏi nhật ký phiên đầy đủ (để đảm bảo tính toàn vẹn).
- **FR-014**: Hệ thống PHẢI xóa trạng thái phụ đề và reset phiên nhận dạng khi người dùng tua đến vị trí khác trong video.
- **FR-015**: Hệ thống PHẢI xử lý lỗi kết nối một cách graceful, hiển thị thông báo thân thiện với người dùng và hỗ trợ tự động kết nối lại.
- **FR-016**: Hệ thống PHẢI xác thực API Key trước khi bắt đầu phiên phụ đề, hiển thị thông báo lỗi rõ ràng nếu key bị thiếu hoặc không hợp lệ.

### Thực thể Chính

- **SubtitleSegment (Đoạn phụ đề)**: Đại diện cho một đơn vị hiển thị phụ đề. Chứa văn bản gốc, văn bản dịch, trạng thái (gốc/đã dịch), nhãn người nói, mã ngôn ngữ, điểm tin cậy, và thời gian tạo.
- **SubtitleSession (Phiên phụ đề)**: Đại diện cho một phiên dịch phụ đề đang hoạt động. Theo dõi trạng thái kết nối, bộ đệm hiển thị (có thể cắt bỏ), nhật ký phiên đầy đủ, và cấu hình (ngôn ngữ, chế độ hiển thị, API key).
- **TranslationSettings (Cài đặt Dịch)**: Tùy chọn cấu hình của người dùng bao gồm API Key, ngôn ngữ nguồn, ngôn ngữ đích, chế độ hiển thị, và endpoint delay.

## Tiêu chí Thành công *(bắt buộc)*

### Kết quả Đo lường được

- **SC-001**: Người dùng thấy phụ đề đã dịch xuất hiện trong vòng 3 giây kể từ lời nói trong điều kiện mạng bình thường.
- **SC-002**: Tính năng phụ đề hoạt động liên tục ít nhất 2 tiếng mà không có gián đoạn hoặc suy giảm mà người dùng nhận thấy.
- **SC-003**: Người dùng có thể cấu hình tất cả cài đặt dịch (API Key, ngôn ngữ, chế độ hiển thị) và chúng được lưu qua các lần khởi động lại ứng dụng.
- **SC-004**: Văn bản tạm thời xuất hiện trong vòng 500ms kể từ khi bắt đầu nói, cung cấp phản hồi trực quan ngay lập tức cho thấy hệ thống đang lắng nghe.
- **SC-005**: Overlay phụ đề dễ đọc và không che khuất nội dung video quan trọng, với văn bản rõ ràng trên nền video thay đổi.
- **SC-006**: Người dùng có thể chuyển đổi giữa các chế độ hiển thị (chỉ bản gốc, chỉ bản dịch, song ngữ) mà không cần khởi động lại phiên phụ đề.
- **SC-007**: Khi người dùng tua hoặc tạm dừng video, trạng thái phụ đề được reset sạch sẽ mà không còn văn bản dư thừa từ vị trí trước.

## Giả định

- Người dùng có kết nối internet ổn định và Soniox API Key hợp lệ để sử dụng tính năng phụ đề trực tiếp.
- Ứng dụng nextplayer đã có trình phát video hoạt động với khả năng phát audio (dựa trên codebase hiện có).
- Model `stt-rt-v4` của Soniox hỗ trợ các ngôn ngữ người dùng chọn, và WebSocket API có sẵn tại `wss://stt-rt.soniox.com/transcribe-websocket`.
- Chỉ hỗ trợ chế độ dịch một chiều (nguồn → đích) trong phiên bản đầu tiên. Chế độ hai chiều nằm ngoài phạm vi v1.
- Tính năng hoạt động trên đầu ra audio của trình phát video trên thiết bị; không cần đầu vào microphone cho trường hợp sử dụng chính.
- Tính năng phân biệt người nói (speaker diarization) và nhận dạng ngôn ngữ từ Soniox được sử dụng thụ động (hiển thị nếu có sẵn) nhưng không phải là cốt lõi cho logic ghép cặp phụ đề.
- TTS (đọc bản dịch thành tiếng nói) nằm ngoài phạm vi v1.
- Lưu trữ transcript (lưu nhật ký phiên ra file) nằm ngoài phạm vi v1 nhưng cấu trúc dữ liệu nhật ký phiên cần hỗ trợ triển khai trong tương lai.
- Audio được trích xuất bằng cách decode trực tiếp từ file video (MediaExtractor/MediaCodec), không phụ thuộc vào AudioPlaybackCapture API. Cần đồng bộ vị trí decode với vị trí phát hiện tại của trình phát video.
