Dựa trên cấu trúc của dự án `nextplayer` (một video player hiện có) và tham khảo kiến trúc xử lý phụ đề từ codebase `my-translator` cũng như tài liệu `_docs\my-translator-soniox-bilingual-subtitles.md`, hãy thực hiện tích hợp tính năng tạo và dịch phụ đề thời gian thực bằng Soniox AI.

Yêu cầu chi tiết:

1. **Quản lý Cấu hình (Settings):**
   - Bổ sung giao diện cài đặt để người dùng nhập `Soniox API Key`.
   - Thêm tùy chọn chọn ngôn ngữ nguồn (Source Language) và ngôn ngữ đích (Target Language - mặc định là Tiếng Việt).
   - Thêm cấu hình chế độ hiển thị: "Chỉ phụ đề gốc", "Chỉ phụ đề dịch", hoặc "Song ngữ" (Mặc định khi khởi tạo là "Chỉ phụ đề dịch").

2. **Pipeline Xử lý Audio:**
   - Trích xuất luồng âm thanh từ video đang phát trong `nextplayer`.
   - Chuẩn hóa dữ liệu audio về định dạng `16 kHz mono PCM (s16le)` trước khi gửi đi.
   - Thực hiện gửi batch audio (khoảng 200ms mỗi batch) qua WebSocket tới Soniox (`wss://stt-rt.soniox.com/transcribe-websocket`) để tối ưu độ trễ.

3. **Logic Xử lý Phụ đề (Core Logic):**
   - Tái cấu trúc thuật toán từ `soniox.js` và `ui.js` của `my-translator`:
     - Sử dụng `provisional text` để hiển thị ngay lập tức khi đang nhận diện (tạo cảm giác realtime).
     - Sử dụng hàng đợi FIFO để ghép cặp `original text` (đã final) với `translation text` tương ứng.
     - Xử lý các cờ `is_final` và `translation_status` từ Soniox token stream để cập nhật UI chính xác.
   - Đảm bảo cơ chế "Session Reset" và "Keepalive" (mỗi 15s) để duy trì kết nối ổn định trong suốt quá trình xem video.

4. **Hiển thị (UI/UX):**
   - Thiết kế overlay phụ đề đè lên trình phát video của `nextplayer`.
   - Hỗ trợ render linh hoạt theo chế độ người dùng đã chọn (Single view hoặc Dual view).
   - Đảm bảo phụ đề được đồng bộ hóa tốt với âm thanh của video đang phát.

Tham khảo kỹ các file sau trong `my-translator` để đảm bảo tính nhất quán:
- `src/js/soniox.js`: Cách quản lý WebSocket và parse token.
- `src/js/ui.js`: Cách quản lý mảng `segments[]` và logic FIFO cho phụ đề song ngữ.
- `src-tauri/src/commands/audio.rs`: Cách xử lý và batching PCM audio.spec