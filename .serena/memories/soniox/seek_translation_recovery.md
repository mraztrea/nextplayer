# Soniox seek/tua translation recovery

Ngày 2026-06-10: lỗi dịch phụ đề Soniox thi thoảng không tiếp tục sau seek/tua có root cause ở 2 lớp lifecycle:

- `PlayerService.onPositionDiscontinuity()` trước đây return sớm khi seek trong cùng media item, nên không báo `SubtitleEngine.resetSession()` cho Soniox session.
- Media3 gọi `SubtitleAudioProcessor.flush()` khi seek, nhưng `flush()` chỉ xóa output passthrough và không reset `AudioBatcher`, khiến PCM partial trước seek có thể lẫn với PCM sau seek và gửi batch sai tới Soniox.

Fix đã áp dụng:
- `PlayerService` inject `SubtitleEngine` và gọi `subtitleEngine.resetSession()` khi `reason == DISCONTINUITY_REASON_SEEK`.
- `SubtitleAudioProcessor.flush()` reset `audioBatcher` khi `isEnabled`.
- `SubtitleEngine.resetSession()` check `currentConfig ?: return` trước khi log/reset để seek khi live subtitle tắt không log sai.
- Regression test: `SubtitleAudioProcessorTest.flushDropsPartialSubtitleBatchAfterSeek`.

Verification đã chạy:
- `.\gradlew :core:subtitle:testDebugUnitTest --tests "dev.anilbeesetti.nextplayer.core.subtitle.audio.SubtitleAudioProcessorTest" :feature:player:compileDebugKotlin :core:subtitle:ktlintCheck :feature:player:ktlintCheck`