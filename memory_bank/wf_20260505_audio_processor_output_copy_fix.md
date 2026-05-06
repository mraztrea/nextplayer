# Workflow 2026-05-05: Sửa alias buffer trong SubtitleAudioProcessor

## Bối cảnh
- Triệu chứng cần kiểm tra: audio vẫn phát nhưng có thể bị rè/nhiễu/lag sau khi chèn `SubtitleAudioProcessor` vào pipeline của ExoPlayer.
- Root cause được xác nhận từ contract `AudioProcessor`: caller vẫn giữ ownership của `inputBuffer`, nhưng `SubtitleAudioProcessor` đang trả `getOutput()` bằng alias/view của chính buffer đó.

## Thay đổi
- `core/subtitle/.../SubtitleAudioProcessor.kt`
  - Copy PCM pass-through sang direct buffer riêng trước khi trả `getOutput()`.
  - Giữ nguyên logic tap sang `AudioBatcher`; chỉ sửa ownership/lifetime của output buffer.
- `core/subtitle/.../SubtitleAudioProcessorTest.kt`
  - Thêm regression test chứng minh output không còn đổi theo khi caller mutate `inputBuffer` sau `queueInput()`.

## Verification
- Chạy test mục tiêu:
  - `rtk pwsh -NoLogo -Command ".\\gradlew.bat :core:subtitle:testDebugUnitTest --tests \"dev.anilbeesetti.nextplayer.core.subtitle.audio.SubtitleAudioProcessorTest\""`

## Ghi chú
- Không có migrate database.
- Không đổi behavior của `PlayerService`, `SubtitleEngine`, `AudioBatcher`, hay volume boost trong lần sửa này.
