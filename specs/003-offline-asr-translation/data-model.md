# Data Model: Offline ASR Translation

## OfflineModel

Đại diện cho một gói model đầy đủ dùng cho v1.

**Fields**:
- `id`: định danh ổn định từ manifest.
- `displayName`: tên hiển thị.
- `version`: phiên bản model.
- `manifestVersion`: phiên bản schema manifest.
- `sourceLanguage`: `ja`.
- `targetLanguages`: danh sách bắt buộc gồm `vi`, `en`.
- `capabilities`: gồm `japanese_transcript`, `ja_to_vi`, `ja_to_en`.
- `sizeBytes`: dung lượng dự kiến hoặc đã cài.
- `installedPath`: thư mục model trong app-specific storage.
- `checksum`: checksum của gói hoặc file quan trọng.
- `state`: `NotInstalled`, `Downloading`, `Importing`, `Ready`, `Error`, `NeedsUpdate`.
- `lastError`: lỗi gần nhất nếu có.

**Validation**:
- Chỉ `Ready` nếu manifest hợp lệ, đủ capability v1 và file bắt buộc tồn tại.
- Gói thiếu transcript tiếng Nhật hoặc thiếu một target Việt/Anh không được đánh dấu ready.

## ModelPreparationTask

Đại diện cho tải tự động hoặc import thủ công.

**Fields**:
- `taskId`
- `type`: `Download` hoặc `Import`
- `source`: URL model đã duyệt hoặc URI import từ người dùng.
- `requiresWifiByDefault`: true.
- `userConfirmedMobileData`: true/false.
- `progressPercent`
- `bytesDownloaded`
- `totalBytes`
- `state`: `PendingConfirmation`, `Running`, `Completed`, `Cancelled`, `Failed`.
- `errorMessage`

**State transitions**:
- `PendingConfirmation -> Running -> Completed`
- `PendingConfirmation -> Cancelled`
- `Running -> Failed`
- `Running -> Cancelled`

## OfflineSubtitleSession

Đại diện cho phiên phụ đề offline trong player.

**Fields**:
- `sessionId`
- `mediaId`
- `modelId`
- `sourceLanguage`: `ja`
- `targetLanguage`: `vi` hoặc `en`
- `displayMode`: `OriginalOnly`, `TranslationOnly`, `Bilingual`
- `state`: `Idle`, `Preparing`, `Running`, `Paused`, `Error`, `Stopped`
- `networkAllowed`: false trong phiên runtime.
- `currentSegments`: danh sách segment đang hiển thị.

**State transitions**:
- `Idle -> Preparing -> Running`
- `Running -> Paused -> Running`
- `Running -> Stopped`
- `Running -> Error`
- Seek/media/audio-track change resets `currentSegments`.

## TranscriptSegment

Đại diện cho một đoạn transcript/dịch.

**Fields**:
- `id`
- `startMs`
- `endMs`
- `originalText`
- `translationText`
- `sourceLanguage`: `ja`
- `targetLanguage`: `vi` hoặc `en`
- `status`: `Provisional`, `FinalOriginal`, `FinalTranslated`, `Error`
- `confidence`: optional.

## OfflineSubtitleSettings

Đại diện cho cài đặt của người dùng.

**Fields**:
- `selectedModelId`
- `targetLanguage`: mặc định `vi`.
- `displayMode`
- `downloadWifiOnly`: mặc định true.
- `allowMobileDataForCurrentDownload`: false mặc định.
- `lastKnownModelState`

**Validation**:
- Bật subtitle offline chỉ hợp lệ khi `selectedModelId` trỏ tới `OfflineModel.Ready`.
