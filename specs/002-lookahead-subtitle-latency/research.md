# Research: Tối ưu Độ trễ Phụ đề Dịch với Lookahead Audio

**Branch**: `002-lookahead-subtitle-latency` | **Date**: 2026-05-05

## R1: Xác nhận cơ chế Soniox hiện tại là STT + translation thời gian thực tích hợp

### Decision
Giữ nguyên một phiên Soniox WebSocket thời gian thực, trong đó speech-to-text và one-way translation cùng chạy trên cùng luồng token. Không thêm API dịch text riêng, không đợi gom đủ một câu hoàn chỉnh rồi mới gọi bước dịch tách biệt, và không mở rộng sang text-to-speech.

### Rationale
- `SonioxWebSocketClient` hiện kết nối tới `wss://stt-rt.soniox.com/transcribe-websocket`.
- `buildConfigJson()` đang gửi `model = "stt-rt-v4"` và object `translation = { type: "one_way", target_language = ... }`.
- `SubtitleEngineImpl` chỉ có một đường gửi audio: `AudioBatcher -> SonioxWebSocketClient.sendAudio()`.
- `SonioxTokenParser` hiện phân loại token theo `is_final` và `translation_status`, nghĩa là app đang tiêu thụ trực tiếp luồng token gốc/bản dịch từ cùng một response stream.
- Tài liệu chính thức của Soniox về real-time translation xác nhận token gốc và token dịch cùng nằm trong response stream của Speech-to-Text real-time API, không phải một bước dịch offline riêng.

### Alternatives considered
- **Gọi dịch text sau khi original final hoàn tất**: Dễ hiểu hơn nhưng làm tăng độ trễ thêm một round-trip và lệch khỏi cơ chế hiện có.
- **Text-to-Speech**: Người dùng đã nói nhầm TTS; phạm vi thực tế là STT + translation nên loại bỏ.

## R2: Chọn nguồn timing truth cho subtitle có đồng bộ media-time

### Decision
Dùng `start_ms` và `end_ms` của token gốc (`translation_status = "original"` hoặc `"none"`) từ Soniox làm nguồn thời gian chuẩn cho `SubtitleSegment`. Token dịch không có timestamp riêng nên sẽ kế thừa cửa sổ thời gian của original segment được ghép cặp.

### Rationale
- Tài liệu Soniox xác nhận token nói ra luôn có `start_ms` và `end_ms` theo audio timeline.
- Tài liệu Soniox cũng nói rõ token dịch không kèm timestamp; bản dịch đi theo nhịp của original tokens tương ứng.
- Đây là cách duy nhất vừa chính xác vừa tránh phải tự suy luận timing chỉ từ thời điểm phản hồi về.
- Current code đang bỏ qua toàn bộ timing metadata này; đó là gap chính khiến overlay hiện tại chỉ render theo arrival time.

### Alternatives considered
- **Dùng thời điểm nhận phản hồi để làm target timestamp**: Đơn giản nhưng tiếp tục giữ vấn đề lệch theo mạng và processing delay.
- **Tự nội suy timing từ audio batch send time**: Có thể dùng làm fallback debug, nhưng kém tin cậy hơn timestamp gốc từ Soniox.
- **Chỉ dùng `<end>` token để chốt một đoạn rồi gán thời gian gần đúng**: Không đủ chính xác cho mục tiêu subtitle hiển thị đúng nhịp.

## R3: Chọn kiến trúc lookahead audio extraction

### Decision
Thêm một pipeline giải mã song song trong `core:subtitle` dựa trên `MediaExtractor` + `MediaCodec`, duy trì lead mục tiêu khoảng 5 giây so với `ExoPlayer.currentPosition`. Pipeline này chỉ đọc audio track đang được player chọn, giải mã nhanh hơn realtime cho đến khi đạt high-water mark, sau đó tự throttle.

### Rationale
- `SubtitleAudioProcessor` chỉ nhìn thấy PCM ở đúng vị trí đang phát, nên không thể tự tạo lookahead thực sự.
- Lookahead yêu cầu đọc trước audio của cùng media item mà không đợi player phát tới.
- `MediaExtractor` + `MediaCodec` cho phép seek tới vị trí mới và đọc tiếp từ file nguồn với media-time rõ ràng.
- Có thể kiểm soát tài nguyên bằng low-water/high-water mark thay vì chạy decoder liên tục.
- Giải pháp này tách khỏi renderer playback nên nếu lỗi vẫn không làm gãy đường phát video chính.

### Alternatives considered
- **Giữ nguyên `SubtitleAudioProcessor` làm nguồn chính**: Không thể đọc tương lai, chỉ giảm latency rất hạn chế.
- **Tạo ExoPlayer thứ hai để decode lookahead**: Quá nặng, rủi ro đồng bộ track state và lifecycle phức tạp hơn.
- **Decode cả file từ đầu hoặc nạp window lớn hơn nhiều**: Tăng CPU/RAM/pin và không cần thiết cho mục tiêu 5 giây.

## R4: Chọn nơi quyết định “đến giờ mới render”

### Decision
Đưa logic gating theo media-time vào `SubtitleSessionManager`/`SubtitleEngine`, còn `PlayerViewModel` và `SubtitleOverlay` chỉ tiêu thụ state đã được lọc. `PlayerService` sẽ tiếp tục là nguồn phát sinh playback position, seek, track change và pause/resume cho engine.

### Rationale
- Overlay chỉ nên render state đã đúng thời điểm, tránh đẩy toàn bộ luật timing lên UI.
- `PlayerService` đã là nơi gần `ExoPlayer` nhất và đã bắt được `onPositionDiscontinuity`, `currentPosition`, media item transition, audio track change.
- Session layer hiện đang quản lý buffer subtitle; mở rộng layer này sang timed sync buffer là thay đổi nhỏ nhất theo kiến trúc đang có.
- ViewModel giữ vai trò expose state, không trở thành bộ máy đồng bộ thời gian.

### Alternatives considered
- **Cho `SubtitleOverlay` tự so currentPosition và filter segments**: UI sẽ gánh logic timing, khó test và dễ lệch giữa các caller.
- **Cho `PlayerViewModel` tự scan list segment mỗi frame**: Có thể làm được nhưng làm loãng boundary giữa orchestration và presentation.

## R5: Chiến lược seek, pause, đổi track và reset phiên

### Decision
Mỗi lần seek, media item transition, hoặc audio track switch sẽ tăng `generationId`, reset lookahead cursor, xóa timed buffer chưa tới lượt, và mở lại pipeline Soniox từ vị trí mới. Khi pause kéo dài, pipeline lookahead ngừng tiến thêm nhưng giữ kết nối Soniox bằng keepalive nếu phiên còn hợp lệ.

### Rationale
- Seek là điểm sai ngữ cảnh nặng nhất; phải loại bỏ tuyệt đối mọi subtitle của generation cũ để tránh spoiler hoặc “bóng ma”.
- `PlayerService` đã có hook `onPositionDiscontinuity`, phù hợp để phát tín hiệu reset.
- `generationId` là cách rẻ để làm rơi mọi callback/audio chunk/token cũ đến muộn mà không cần block phức tạp.
- Pause ngắn không cần xóa toàn bộ session; chỉ cần dừng tăng lead và giữ kết nối hợp lý để resume nhanh hơn.

### Alternatives considered
- **Chỉ clear UI mà không reset Soniox session**: Dữ liệu cũ vẫn có thể về muộn và gắn nhầm vào vị trí mới.
- **Đóng toàn bộ session ở mọi pause ngắn**: Tốn reconnect không cần thiết và làm tăng warm-up latency.

## R6: Chiến lược hiệu năng và suy giảm an toàn

### Decision
Áp dụng cơ chế bounded processing:
- `targetLeadMs = 5000`
- `lowWaterMs = 3000`
- `highWaterMs = 5000`
- hàng đợi PCM và subtitle có giới hạn kích thước
- khi extraction/decoder không khả dụng hoặc lỗi lặp lại, fallback về đường hiện tại dựa trên `SubtitleAudioProcessor` hoặc tắt lookahead cho phiên đó nhưng không làm hỏng phát video.

### Rationale
- Lead nhỏ nhưng ổn định cho cảm nhận latency tốt hơn mà vẫn kiểm soát CPU/pin.
- Không cần giữ đệm dài hơn 5 giây vì user story chỉ đòi subtitle “sẵn sàng trước” chứ không phải precompute sâu.
- Fallback theo phiên giúp người dùng vẫn xem video bình thường trên định dạng media khó hỗ trợ.

### Alternatives considered
- **Lead cố định không có watermarks**: Decoder dễ chạy liên tục, tốn pin vô ích.
- **Không có fallback**: Một lỗi ở lookahead pipeline có thể làm mất toàn bộ live subtitle của phiên.

## Nguồn xác nhận quan trọng

- Soniox Docs — Real-time translation: https://soniox.com/docs/stt/rt/real-time-translation
- Soniox Docs — Timestamps: https://soniox.com/docs/stt/concepts/timestamps
- Soniox Docs — STT WebSocket API response fields: https://soniox.com/docs/api-reference/stt/websocket-api
