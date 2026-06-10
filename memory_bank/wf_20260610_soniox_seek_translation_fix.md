# Workflow: Sửa lỗi dịch phụ đề Soniox sau khi seek/tua video

## Nguyên nhân

- `PlayerService.onPositionDiscontinuity()` chỉ xử lý seek để lưu vị trí khi chuyển sang media item khác; seek trong cùng video bị return sớm nên Soniox session không được reset.
- `SubtitleAudioProcessor.flush()` được Media3 gọi khi seek/tua, nhưng trước đây chỉ xóa output passthrough, không xóa PCM partial đang nằm trong `AudioBatcher`. Sau seek có thể phát batch lẫn âm thanh trước/sau seek, làm phiên dịch Soniox dễ kẹt hoặc không tiếp tục dịch ổn định.
- `SubtitleAudioProcessor.queueInput()` từng trả `inputBuffer.duplicate()` trực tiếp làm output passthrough. Output vì vậy dùng chung memory với input buffer của Media3; khi buffer input bị tái sử dụng/ghi tiếp, PCM output có thể đổi theo và gây audio rè/lag.

## Thay đổi

- Reset `AudioBatcher` trong `SubtitleAudioProcessor.flush()` khi live subtitle tap đang bật.
- Copy PCM passthrough sang output buffer riêng, tái sử dụng buffer direct để output không phụ thuộc vòng đời input buffer của Media3.
- Inject `SubtitleEngine` vào `PlayerService` và gọi `subtitleEngine.resetSession()` khi nhận `DISCONTINUITY_REASON_SEEK`.
- `SubtitleEngine.resetSession()` bỏ qua seek khi chưa có live subtitle session trước khi ghi log/reset.
- Thêm unit tests `SubtitleAudioProcessorTest.flushDropsPartialSubtitleBatchAfterSeek` và `outputKeepsOriginalPcmWhenInputBufferIsReused`.

## Verification

```powershell
.\gradlew :core:subtitle:testDebugUnitTest --tests "dev.anilbeesetti.nextplayer.core.subtitle.audio.SubtitleAudioProcessorTest"
.\gradlew :feature:player:compileDebugKotlin
.\gradlew :core:subtitle:ktlintCheck :feature:player:ktlintCheck
```
