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
