# Workflow: Speckit Plan cho Lookahead Subtitle Latency

**Ngày**: 2026-05-05  
**Branch**: `002-lookahead-subtitle-latency`

## Mục tiêu

Tạo bộ artifact `/speckit-plan` cho feature tối ưu độ trễ subtitle dịch bằng lookahead audio, dựa trên code Soniox hiện tại trong `nextplayer`.

## Những gì đã làm

1. Chạy `setup-plan.ps1 -Json` để tạo `specs/002-lookahead-subtitle-latency/plan.md`.
2. Phân tích code hiện tại quanh `SubtitleEngine`, `SonioxWebSocketClient`, `SonioxTokenParser`, `SubtitleSessionManager`, `SubtitleAudioProcessor`, `PlayerService`, `PlayerViewModel`, `SubtitleOverlay`.
3. Xác nhận cơ chế hiện tại là Soniox real-time STT + one-way translation trên cùng WebSocket, không có API dịch tách rời.
4. Kiểm tra tài liệu Soniox để khóa quyết định về `start_ms/end_ms`:
   - token gốc có timestamp
   - token dịch không có timestamp riêng
5. Tạo các artifact planning:
   - `specs/002-lookahead-subtitle-latency/plan.md`
   - `specs/002-lookahead-subtitle-latency/research.md`
   - `specs/002-lookahead-subtitle-latency/data-model.md`
   - `specs/002-lookahead-subtitle-latency/quickstart.md`
   - `specs/002-lookahead-subtitle-latency/contracts/soniox-timed-token-mapping.md`
   - `specs/002-lookahead-subtitle-latency/contracts/subtitle-sync-player.md`
6. Cập nhật marker `SPECKIT` trong `AGENTS.md` sang plan mới.

## Quyết định kỹ thuật chính

- Dùng `MediaExtractor` + `MediaCodec` cho pipeline lookahead song song thay vì chỉ dựa vào `SubtitleAudioProcessor`.
- Dùng `start_ms/end_ms` của token gốc Soniox làm timing truth; token dịch kế thừa timing của original segment.
- Đưa logic “đến giờ mới render” vào `SubtitleSessionManager`/`SubtitleEngine`, không đẩy sang UI.
- Dùng `generationId` để reset sạch dữ liệu cũ khi seek, đổi media, hoặc đổi audio track.
- Bounded lookahead với `targetLeadMs = 5000`, `lowWaterMs = 3000`, `highWaterMs = 5000`.

## Lệnh / migrate

- Không cần migrate database.
- Chưa chạy build/test trong bước plan này.

## Cập nhật implement

### Code đã thêm/sửa

- Thêm `LookaheadAudioPipeline` dùng `MediaExtractor` + `MediaCodec` để giải mã audio đi trước và giữ lead khoảng 5 giây.
- Mở rộng `SubtitleEngine` để nhận playback sync từ player: `currentPosition`, seek, media change, play/pause, audio track change.
- Dùng `generationId` để reset timed subtitle state khi seek, chuyển media, hoặc đổi audio track.
- Thêm hard reconnect path ở `SonioxWebSocketClient` cho reset do playback lifecycle; vẫn giữ soft rotate cho periodic session reset.
- Nối `PlayerService` vào subtitle engine bằng ticker `currentPosition` 100ms và các callback lifecycle của ExoPlayer.
- Cập nhật `PlayerViewModel`, `SubtitleOverlay`, `MediaPlayerScreen` để render timed subtitle state và hiển thị notice khi fallback.

### Kiểm chứng đã chạy

```powershell
.\gradlew.bat :core:subtitle:testDebugUnitTest --tests "dev.anilbeesetti.nextplayer.core.subtitle.engine.SonioxTokenParserTest" --tests "dev.anilbeesetti.nextplayer.core.subtitle.session.SubtitleSessionManagerTest"
.\gradlew.bat :core:subtitle:compileDebugKotlin :feature:player:compileDebugKotlin
```

- `SonioxTokenParserTest` pass
- `SubtitleSessionManagerTest` pass
- `:core:subtitle:compileDebugKotlin` pass
- `:feature:player:compileDebugKotlin` pass

### Ghi chú hiệu năng / rủi ro còn lại

- Ticker sync ở `PlayerService` đang chạy mỗi 100ms để gate subtitle theo media-time; đây là lựa chọn thực dụng để có sync ổn định trước khi tối ưu sâu hơn.
- Fallback hiện chuyển về current-position tap nếu lookahead pipeline lỗi; chưa có telemetry transport/lead đầy đủ cho toàn bộ Phase 5.
- Chưa chạy xác minh thủ công trên thiết bị thật cho các kịch bản seek/audio-track/fallback; mới xác minh bằng unit test + compile.
