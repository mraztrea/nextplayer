# Data Model: Gemini Live Realtime Subtitle Translation

## Entities

### SubtitleProviderSelection

Đại diện provider phụ đề realtime đang được người dùng chọn.

**Fields**:
- `provider`: enum `SubtitleProvider`
- `updatedAt`: thời điểm lựa chọn thay đổi

**Validation**:
- Giá trị hợp lệ: `SONIOX`, `GEMINI_LIVE`
- Không tự đổi provider khi provider hiện tại lỗi.

### ProviderCredential

Đại diện trạng thái khóa của từng provider.

**Fields**:
- `provider`: enum `SubtitleProvider`
- `hasKey`: boolean
- `validationState`: enum `CredentialValidationState`
- `lastValidationError`: lỗi gần nhất, không chứa API key

**Validation**:
- Google API Key và Soniox API Key được lưu riêng.
- Không ghi API key vào log, UI state hoặc crash metadata.

### GeminiTranslationSettings

Cấu hình người dùng dành riêng cho Gemini Live.

**Fields**:
- `targetLanguageAlias`: giá trị UI người dùng chọn, ví dụ `vn`, `en`, `Tiếng Việt`
- `targetLanguageCode`: mã BCP-47 gửi tới provider, ví dụ `vi`, `en`
- `displayMode`: enum `SubtitleDisplayMode`
- `echoTargetLanguage`: boolean, luôn bật trong v1
- `isEnabledByDefault`: boolean tùy chọn theo preference hiện có

**Validation**:
- `vn` phải map sang `vi`.
- `en` phải map sang `en`.
- `displayMode` phải hỗ trợ đủ `TRANSLATION_ONLY`, `BILINGUAL`, `ORIGINAL_ONLY`.

### GeminiSessionConfig

Config runtime cho một phiên Gemini Live subtitle.

**Fields**:
- `apiKeyRef`: tham chiếu tới Google API Key đã lưu an toàn
- `targetLanguageCode`: mã BCP-47
- `displayMode`: chế độ hiển thị hiện tại
- `audioFormat`: PCM 16-bit little-endian, 16 kHz, mono
- `chunkDurationMs`: 100
- `inputAudioTranscriptionEnabled`: true
- `outputAudioTranscriptionEnabled`: true
- `echoTargetLanguage`: true

**Validation**:
- Không tạo session nếu thiếu Google API Key.
- Không tạo session nếu ngôn ngữ đích không được hỗ trợ.
- Không dùng audio từ ứng dụng khác hoặc microphone.

### RealtimeSubtitleSession

Phiên phụ đề realtime đang gắn với media item hiện tại.

**Fields**:
- `provider`: enum `SubtitleProvider`
- `mediaId`: định danh media item đang phát
- `status`: enum `SubtitleSessionStatus`
- `targetLanguageCode`: mã BCP-47
- `displayMode`: enum `SubtitleDisplayMode`
- `startedAt`: thời điểm bắt đầu
- `lastResetAt`: thời điểm reset gần nhất
- `lastError`: lỗi gần nhất, không chứa audio/transcript

**State transitions**:
- `OFF` -> `NOT_CONFIGURED`: người dùng bật Gemini nhưng thiếu key hoặc config bắt buộc.
- `OFF` -> `CONNECTING`: user bật phụ đề Gemini khi config hợp lệ.
- `CONNECTING` -> `TRANSLATING`: session sẵn sàng nhận audio và transcript.
- `TRANSLATING` -> `RESETTING`: seek, đổi media, flush audio, hoặc đổi ngôn ngữ/provider.
- `RESETTING` -> `CONNECTING`: bắt đầu session mới cho audio hiện tại.
- `CONNECTING`/`TRANSLATING` -> `ERROR`: lỗi key, quyền, hạn mức, mạng, provider.
- `ERROR` -> `OFF`: user tắt phụ đề.
- `ERROR` -> `CONNECTING`: user retry Gemini.

### SubtitleSegment

Đơn vị phụ đề hiển thị trên overlay.

**Fields**:
- `id`: định danh segment nội bộ
- `provider`: `SONIOX` hoặc `GEMINI_LIVE`
- `mediaId`: media item tại thời điểm segment được tạo
- `originalText`: transcript gốc nếu có
- `translatedText`: transcript dịch nếu có
- `displayText`: text sau khi áp dụng `displayMode`
- `targetLanguageCode`: mã BCP-47
- `status`: enum `SegmentStatus`
- `createdAt`: thời điểm tạo segment
- `isProvisional`: true nếu đang xử lý

**Validation**:
- Segment cũ phải bị xóa khi seek hoặc đổi media.
- Không persist audio/transcript lâu dài trong v1.
- Không ghi `originalText` hoặc `translatedText` vào diagnostic log.

### GeminiLiveTranscriptEvent

Event nội bộ từ Gemini session để engine cập nhật overlay.

**Fields**:
- `eventType`: `INPUT_TRANSCRIPT`, `OUTPUT_TRANSCRIPT`, `STATUS`, `ERROR`
- `text`: transcript nếu event là transcript
- `isFinal`: boolean nếu provider cung cấp trạng thái hoàn chỉnh
- `languageCode`: ngôn ngữ event
- `errorCode`: mã lỗi nếu có
- `receivedAt`: thời điểm app nhận event

**Validation**:
- Event lỗi không chứa API key, audio hoặc transcript không liên quan.
- Output transcript dùng cho bản dịch; input transcript dùng cho bản gốc.

## Enums

### SubtitleProvider

- `SONIOX`
- `GEMINI_LIVE`

### SubtitleDisplayMode

- `TRANSLATION_ONLY`: chỉ hiển thị bản dịch.
- `BILINGUAL`: hiển thị bản gốc và bản dịch.
- `ORIGINAL_ONLY`: chỉ hiển thị bản gốc/transcript.

### CredentialValidationState

- `UNKNOWN`
- `VALIDATING`
- `VALID`
- `MISSING`
- `INVALID`
- `NO_PERMISSION`
- `RATE_LIMITED`

### SubtitleSessionStatus

- `OFF`
- `NOT_CONFIGURED`
- `CONNECTING`
- `TRANSLATING`
- `RESETTING`
- `ERROR`

### SegmentStatus

- `PROVISIONAL`
- `FINAL`
- `STALE`

## Relationships

- `SubtitleProviderSelection` chọn provider cho `RealtimeSubtitleSession`.
- `ProviderCredential` được quản lý riêng cho từng `SubtitleProvider`.
- `GeminiTranslationSettings` chỉ áp dụng khi `provider = GEMINI_LIVE`.
- `GeminiSessionConfig` được tạo từ `ProviderCredential` và `GeminiTranslationSettings`.
- `RealtimeSubtitleSession` tạo nhiều `SubtitleSegment`.
- `GeminiLiveTranscriptEvent` cập nhật hoặc finalize `SubtitleSegment`.

## Data Flow

1. Người dùng lưu Google API Key và chọn Gemini Live trong Settings hoặc Player.
2. Player bật phụ đề realtime và yêu cầu `RealtimeSubtitleSession` với provider Gemini.
3. Engine đọc credential/settings, tạo `GeminiSessionConfig`, mở session.
4. Audio từ media item đang phát đi qua `SubtitleAudioProcessor`, chuẩn hóa thành PCM 16 kHz mono.
5. Gemini client gửi audio chunks, nhận input/output transcript events.
6. Engine tạo provisional/final `SubtitleSegment` theo `SubtitleDisplayMode`.
7. Overlay hiển thị segment hiện tại.
8. Seek/đổi media/tắt phụ đề reset session, audio batcher và overlay.
