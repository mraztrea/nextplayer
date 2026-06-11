# Research: Gemini Live Realtime Subtitle Translation

## R1: Gemini Live là provider bổ sung, không thay thế Soniox

### Quyết định

Mở rộng `core:subtitle` thành provider layer có thể chạy Soniox hoặc Gemini Live theo lựa chọn của người dùng.

### Lý do

Spec đã clarify Gemini Live là provider bổ sung. Repo đã có `SubtitleEngine`, `SonioxWebSocketClient`, `SubtitleAudioProcessor`, `SecureApiKeyStorage`, `SubtitleDisplayMode` và overlay phụ đề. Tận dụng các lớp hiện có giảm rủi ro lệch lifecycle seek/reset và giữ UI nhất quán.

### Phương án thay thế đã xem xét

- Thay Soniox bằng Gemini Live: loại bỏ vì làm mất hành vi hiện có.
- Tạo module riêng cho Gemini: loại bỏ vì trùng audio/session/rendering logic và tăng chi phí tích hợp.
- Tự động chọn provider theo API key: loại bỏ vì spec yêu cầu provider do người dùng chọn và không auto fallback.

## R2: Kết nối Gemini Live Translation

### Quyết định

Thêm Gemini Live client/session trong `core:subtitle` dùng kết nối realtime WebSocket bằng OkHttp, bám raw protocol Live API để gửi audio chunks và nhận transcript events.

### Lý do

OkHttp đã được dùng cho Soniox WebSocket và phù hợp Android/Kotlin hiện tại. Tài liệu Gemini Live Translation mô tả gửi `realtimeInput.audio` qua WebSocket với raw PCM 16 kHz mono. Dùng cùng dependency tránh kéo thêm SDK lớn hoặc backend mới.

### Phương án thay thế đã xem xét

- Google Gen AI SDK trực tiếp: có thể hữu ích về API wrapper nhưng tăng dependency và cần xác minh mức hỗ trợ Android trước khi đưa vào code.
- Ephemeral token backend: an toàn hơn cho app client, nhưng yêu cầu backend và trái mong muốn nhập Google API Key trực tiếp giống Soniox.
- Python/Node examples: chỉ dùng làm tham khảo vì không phù hợp Android app runtime.

## R3: Audio format và chunking

### Quyết định

Reuse audio tap từ player pipeline, chuẩn hóa audio cho Gemini thành PCM 16-bit little-endian, 16 kHz, mono, gửi chunk khoảng 100 ms. Không dùng AudioPlaybackCapture.

### Lý do

Tài liệu Gemini Live Translation yêu cầu input audio raw 16-bit PCM 16 kHz mono và khuyến nghị chunk 100 ms. Repo đã có audio processor/batcher phục vụ Soniox và đã có regression liên quan seek/flush; reuse đường này giữ phụ đề khớp media item đang phát.

### Phương án thay thế đã xem xét

- AudioPlaybackCapture: loại bỏ vì bắt audio ngoài player, cần Android 10+, và trái spec privacy.
- Decode song song bằng MediaExtractor/MediaCodec: loại bỏ vì dễ lệch với vị trí phát, seek và track switch.
- Dùng chunk 200 ms giống Soniox cho mọi provider: loại bỏ vì Gemini docs khuyến nghị 100 ms để giảm latency.

## R4: Transcript và display modes

### Quyết định

Gemini session phải bật input transcript và output translated transcript để hỗ trợ 3 chế độ hiển thị: chỉ bản dịch, song ngữ, chỉ bản gốc.

### Lý do

Spec yêu cầu parity với Soniox cho 3 display modes. Gemini Live Translation hỗ trợ transcript audio đầu vào và transcript audio đầu ra; output transcript là nguồn cho bản dịch, input transcript là nguồn cho bản gốc.

### Phương án thay thế đã xem xét

- Chỉ dùng output transcript: không đáp ứng chế độ chỉ bản gốc/song ngữ.
- Chỉ dùng input transcript rồi dịch text nội bộ: trái Live Translation audio-only flow và tăng complexity.
- Hiển thị translated audio text khi final only: giảm nhầm lẫn, nhưng cần provisional/loading state để người dùng biết hệ thống đang xử lý.

## R5: Cùng ngôn ngữ đích vẫn hiển thị phụ đề

### Quyết định

Gemini config phải bật hành vi echo target language để khi audio đã cùng ngôn ngữ đích, app vẫn nhận nội dung phù hợp để hiển thị phụ đề/transcript.

### Lý do

Clarification yêu cầu không để overlay trống khi audio đã cùng ngôn ngữ đích. Tài liệu Gemini mô tả `echoTargetLanguage` kiểm soát việc echo input đã cùng target language.

### Phương án thay thế đã xem xét

- Để provider mặc định im lặng: loại bỏ vì người dùng dễ hiểu là tính năng hỏng.
- Hiển thị thông báo "audio đã cùng ngôn ngữ": loại bỏ vì người dùng vẫn cần phụ đề realtime.

## R6: Ngôn ngữ đích và alias UI

### Quyết định

UI lưu/hiển thị alias thân thiện nhưng session provider dùng BCP-47: `vn` hoặc "Tiếng Việt" ánh xạ sang `vi`; `en` ánh xạ sang `en`.

### Lý do

User yêu cầu chọn `vn`, `en...`; tài liệu Gemini dùng BCP-47 và liệt kê Vietnamese là `vi`. Tách alias UI khỏi provider code giúp tránh gửi mã sai.

### Phương án thay thế đã xem xét

- Lưu trực tiếp `vn`: loại bỏ vì không phải mã BCP-47 chuẩn cho Vietnamese trong Gemini Live Translation.
- Chỉ cho chọn mã kỹ thuật: loại bỏ vì UI kém thân thiện.

## R7: API key storage và privacy logging

### Quyết định

Lưu Google API Key riêng với Soniox API Key bằng secure storage hiện có hoặc mở rộng tương đương. Không log audio/transcript; chỉ log metadata như provider, target language, status, error code và reset timestamp.

### Lý do

Spec yêu cầu nhập Google API Key giống Soniox, tách provider config, và không log audio/transcript. Existing `SecureApiKeyStorage` đã là hướng phù hợp cho khóa nhạy cảm.

### Phương án thay thế đã xem xét

- Dùng chung một key slot cho mọi provider: loại bỏ vì gây nhầm provider và lỗi cấu hình.
- Log transcript opt-in: loại bỏ trong v1 để giữ privacy mặc định đơn giản.
- Backend ephemeral token: an toàn hơn nhưng ngoài scope và yêu cầu vận hành backend.

## R8: Failure handling và fallback

### Quyết định

Khi Gemini Live lỗi, app báo lỗi Gemini, giữ provider hiện tại, không auto fallback sang Soniox. Người dùng có thể tự đổi provider nếu muốn.

### Lý do

Clarification đã chốt không fallback tự động. Điều này giảm nhầm lẫn về chi phí/key/provider và giúp test lỗi deterministic.

### Phương án thay thế đã xem xét

- Auto fallback sang Soniox nếu đã cấu hình: loại bỏ vì provider change bị ẩn.
- Hỏi người dùng ngay khi lỗi: có thể thêm sau, nhưng v1 giữ flow lỗi đơn giản.

## R9: Seek/reset lifecycle

### Quyết định

Gemini provider phải dùng chung lifecycle reset với Soniox: reset khi seek, đổi media, tắt phụ đề, lỗi không recover, hoặc audio processor flush.

### Lý do

Memory dự án đã ghi bug Soniox liên quan seek: cần reset `SubtitleEngine` và audio batcher để không lẫn PCM trước/sau seek. Gemini dùng cùng audio stream nên cần cùng invariant.

### Phương án thay thế đã xem xét

- Để Gemini session tự tiếp tục sau seek: loại bỏ vì phụ đề có thể lệch audio mới.
- Chỉ xóa overlay mà không reset session/audio batcher: loại bỏ vì vẫn có thể gửi partial audio cũ.
