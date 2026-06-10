# Research: Offline ASR Translation

## Decision: Dùng `core:subtitle` làm boundary offline engine

**Rationale**: Repo đã có `core:subtitle` cho Soniox WebSocket, audio batching, token/segment model và session manager. Mở rộng module này giữ player/settings chỉ tương tác qua state và use-case hiện có.

**Alternatives considered**:
- Tạo module mới `core:offline-asr`: tách rõ hơn nhưng tăng coupling khi phải chia sẻ audio processor, segment model và overlay state.
- Đưa runtime vào `feature:player`: nhanh cho prototype nhưng làm UI phụ thuộc trực tiếp vào native inference.

## Decision: Ưu tiên adapter sherpa-onnx-compatible cho prototype streaming

**Rationale**: sherpa-onnx hỗ trợ Android, Kotlin, streaming/non-streaming ASR và chạy không cần Internet. Hướng này phù hợp hơn với phụ đề đang phát vì có streaming demo và API Kotlin/Java. Nguồn chính: `https://github.com/k2-fsa/sherpa-onnx`, Android examples `https://github.com/k2-fsa/sherpa-onnx/tree/master/android`, Kotlin examples `https://github.com/k2-fsa/sherpa-onnx/tree/master/kotlin-api-examples`.

**Alternatives considered**:
- whisper.cpp: có Android example và ggml model format, phù hợp fallback/benchmark; streaming sample upstream là kiểu chunked inference nên cần nhiều glue hơn cho subtitle realtime. Nguồn chính: `https://github.com/ggml-org/whisper.cpp`, Android example `https://github.com/ggml-org/whisper.cpp/tree/master/examples/whisper.android`.
- Soniox online fallback: bị loại khỏi runtime offline vì spec yêu cầu phiên offline không dùng mạng.

## Decision: Một gói model v1 đầy đủ với manifest JSON

**Rationale**: Spec đã chốt v1 là một gói đầy đủ gồm transcript tiếng Nhật và dịch Nhật -> Việt/Anh. Manifest là contract để import/tải model không dựa vào đoán file, giúp tránh model thiếu phần dịch nhưng vẫn bị đánh dấu sẵn sàng.

**Alternatives considered**:
- Tách ASR và translation thành nhiều gói: linh hoạt hơn nhưng làm UX và trạng thái readiness phức tạp trong v1.
- Cho chọn thư mục bất kỳ: dễ thử nghiệm nhưng khó đảm bảo integrity và compatibility.

## Decision: Tải model bằng bước chuẩn bị riêng, không trong phiên subtitle

**Rationale**: Phiên offline không được tạo kết nối mạng. Tải tự động cần xác nhận dung lượng và mặc định chỉ Wi-Fi. Điều này tách rõ trạng thái network/download khỏi trạng thái playback/inference.

**Alternatives considered**:
- Tự tải khi bật subtitle: vi phạm kỳ vọng offline và dễ làm gián đoạn playback.
- Cho fallback online khi model lỗi: mâu thuẫn clarification đã chốt.

## Decision: Lưu model trong app-specific storage

**Rationale**: App-specific storage tránh yêu cầu quyền file rộng, dễ xóa model, và phù hợp import qua Storage Access Framework. DataStore chỉ lưu metadata/state nhẹ, không lưu file model.

**Alternatives considered**:
- MediaStore/shared external storage: cần xử lý quyền và cleanup phức tạp.
- Bundled model trong APK: làm APK lớn và không đáp ứng yêu cầu tải/import thủ công.

## Decision: Prototype dịch offline đi qua cùng `OfflineSubtitleEngine`

**Rationale**: Translation runtime có thể là một phần của gói model hoặc adapter native. Với app layer, kết quả chỉ cần `TranscriptSegment` có `translationText`. Boundary này cho phép fake/prototype engine trong test và runtime native trong bản hoàn chỉnh.

**Alternatives considered**:
- Dịch bằng online API: không hợp lệ cho phiên offline.
- Tách `OfflineTranslator` ngay từ đầu: rõ hơn về domain nhưng tăng số boundary khi v1 model là một gói đầy đủ.
