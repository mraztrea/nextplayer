# Quickstart: Lookahead Audio cho Phụ đề Dịch Soniox

**Branch**: `002-lookahead-subtitle-latency` | **Date**: 2026-05-05

## Mục tiêu kiểm chứng

1. Xác nhận Soniox vẫn chạy theo cơ chế real-time STT + translation hiện có.
2. Xác nhận pipeline lookahead duy trì lead khoảng 5 giây mà không làm video giật.
3. Xác nhận subtitle chỉ hiển thị khi `currentPosition` tới đúng cửa sổ media-time.
4. Xác nhận seek/pause/đổi track reset đúng, không còn subtitle cũ.

## Chuẩn bị

1. Soniox API Key hợp lệ.
2. Một video có lời thoại liên tục trong 2-3 phút để đo độ trễ.
3. Một video có ít nhất một điểm seek dễ nhận biết bằng tai/ngữ cảnh.
4. Thiết bị Android API 23+ hoặc emulator đủ mạnh.

## Build

```powershell
.\gradlew.bat :core:subtitle:testDebugUnitTest --tests "dev.anilbeesetti.nextplayer.core.subtitle.engine.SonioxTokenParserTest" --tests "dev.anilbeesetti.nextplayer.core.subtitle.session.SubtitleSessionManagerTest"
.\gradlew.bat :core:subtitle:compileDebugKotlin :feature:player:compileDebugKotlin
```

## Kết quả verify gần nhất

- Đã pass `:core:subtitle:testDebugUnitTest` cho `SonioxTokenParserTest` và `SubtitleSessionManagerTest`.
- Đã pass `:core:subtitle:compileDebugKotlin`.
- Đã pass `:feature:player:compileDebugKotlin`.

## Luồng xác minh thủ công

### 1. Warm-up và độ trễ hiển thị

1. Mở app, cấu hình Soniox API Key.
2. Phát video và bật live subtitle.
3. Chờ khoảng 5 giây đầu để lookahead lấp đầy.
4. Quan sát các câu thoại kế tiếp:
   - subtitle dịch không được xuất hiện trước lời thoại
   - subtitle dịch nên xuất hiện gần như ngay khi câu bắt đầu phát

### 2. Không spoiler subtitle tương lai

1. Chọn một đoạn có câu ngắn dễ nhận biết.
2. Quan sát trước khi nghe thấy câu đó.
3. Xác nhận subtitle của câu đó chưa xuất hiện trước thời điểm âm thanh tương ứng.

### 3. Seek reset

1. Trong khi live subtitle đang hoạt động, tua tiến 20-30 giây.
2. Xác nhận subtitle cũ biến mất nhanh.
3. Chờ tối đa 2 giây để subtitle mới bắt đầu theo ngữ cảnh mới.
4. Lặp lại với tua lùi.

### 4. Pause và resume

1. Pause video khi subtitle đang active.
2. Chờ vài giây.
3. Resume playback.
4. Xác nhận subtitle không nhảy sai ngữ cảnh và pipeline tiếp tục từ vị trí hiện tại.

### 5. Track change hoặc media transition

1. Nếu media có nhiều audio track, đổi track trong lúc phát.
2. Nếu không, chuyển sang media item kế tiếp.
3. Xác nhận timed buffer cũ không còn render sau khi đổi nguồn audio.

## Metric cần quan sát

| Metric | Mục tiêu |
|--------|----------|
| Warm-up time | đạt lead ổn định trong khoảng 5 giây đầu |
| Subtitle render delay sau warm-up | <= 250ms so với lúc lời thoại bắt đầu phát |
| Seek recovery | <= 2 giây trong phần lớn lượt thử |
| Subtitle cũ còn sót sau seek | <= 300ms |
| Fallback safety | phát video vẫn ổn nếu lookahead lỗi |

## Gợi ý log/telemetry nên có khi implement

- `generationId`, `playbackPositionMs`, `bufferedUntilMs`, `lookaheadLeadMs`
- số lần reset do seek/track change
- số lần fallback
- thời gian từ `audio chunk sent -> original final -> translation final -> visible render`

## Không cần

- Không có migrate database.
- Không cần thay đổi định dạng lưu trữ người dùng ngoài phần state/runtime và metadata subtitle.
