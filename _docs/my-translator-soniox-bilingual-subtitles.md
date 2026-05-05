# Tài liệu tham chiếu: tạo phụ đề song ngữ với Soniox theo kiến trúc My Translator

## 1. Mục tiêu tài liệu

Tài liệu này tổng hợp chi tiết cách dự án `code_reference/my-translator` xây dựng pipeline phụ đề song ngữ thời gian thực với Soniox, đồng thời rút ra thuật toán có thể tái sử dụng cho các dự án khác.

Trọng tâm của tài liệu:

- Cách kết nối audio capture -> Soniox WebSocket -> render phụ đề song ngữ.
- Cách ghép `original` và `translation` thành một cặp phụ đề ổn định.
- Cách giữ timing hiển thị câu chuẩn nhưng vẫn đảm bảo độ trễ thấp.
- Các quyết định kỹ thuật quan trọng của `my-translator` có thể tái sử dụng.
- Các hạn chế và bẫy cần biết trước khi port sang dự án khác.

Nguồn phân tích:

- Đọc trực tiếp code trong `code_reference/my-translator`.
- Dùng Serena để lấy symbol map cho phần Rust/Tauri trọng yếu.
- Dùng GitNexus CLI để kiểm tra khả năng index/query local reference folder. Trong môi trường hiện tại, GitNexus không đăng ký được thư mục nested `code_reference/my-translator` như một repo độc lập, nên execution flow bên dưới được đối chiếu trực tiếp từ code thay vì từ knowledge graph index.

## 2. Tổng quan codebase

### 2.1 Frontend

- `src/js/app.js`: bộ điều phối chính. Nối settings, UI, Soniox client, audio capture, transcript save, TTS.
- `src/js/soniox.js`: WebSocket client tới Soniox. Xử lý config, keepalive, session reset, parse token stream, phát callback `original` / `translation` / `provisional`.
- `src/js/ui.js`: mô hình segment hiển thị. Chịu trách nhiệm render single view, dual view, speaker labels, language badges, provisional text, trim buffer, cleanup segment cũ.
- `src/js/settings.js`: settings cache ở frontend, load/save qua Tauri IPC.
- `src/js/audio-player.js`: hàng đợi phát audio MP3 cho TTS.
- `src/js/edge-tts.js`, `src/js/google-tts.js`, `src/js/elevenlabs-tts.js`: các provider đọc bản dịch thành tiếng nói.
- `src/js/updater.js`: auto update.
- `src/index.html`: shell của overlay, settings view, sessions view.

### 2.2 Backend Tauri

- `src-tauri/src/lib.rs`: đăng ký state và command Tauri.
- `src-tauri/src/settings.rs`: struct settings, giá trị mặc định, đọc/ghi JSON xuống disk.
- `src-tauri/src/commands/audio.rs`: start/stop audio capture, merge `system` + `microphone`, batch PCM và gửi sang JS qua IPC.
- `src-tauri/src/commands/transcript.rs`: lưu transcript markdown, liệt kê session, đọc session.
- `src-tauri/src/commands/settings.rs`: bridge `get_settings` / `save_settings`.
- `src-tauri/src/audio/system_audio.rs`: capture system audio trên macOS bằng ScreenCaptureKit.
- `src-tauri/src/audio/microphone.rs`: capture microphone qua `cpal`, resample về 16 kHz mono.
- `src-tauri/src/audio/wasapi.rs`: capture system audio trên Windows bằng WASAPI loopback.
- `src-tauri/src/commands/local_pipeline.rs` + `scripts/local_pipeline.py`: pipeline local mode riêng, không dùng Soniox.

## 3. Luồng runtime end-to-end trong Soniox mode

```mermaid
flowchart LR
    A[System audio / Microphone] --> B[Rust audio capture]
    B --> C[PCM s16le 16 kHz mono]
    C --> D[Batch 200 ms qua Tauri IPC]
    D --> E[JS app.js]
    E --> F[soniox.js WebSocket]
    F --> G[Soniox tokens]
    G --> H[Phan tach original / translation / provisional]
    H --> I[ui.js segments[] + sessionLog[]]
    I --> J[Single view / Dual view]
    H --> K[TTS tuy chon]
    I --> L[Stop session -> save transcript .md]
```

Pipeline chạy như sau:

1. `app.start()` đọc settings hiện tại.
2. Nếu `translation_mode === 'soniox'`, app gọi `_startSonioxMode(settings)`.
3. `sonioxClient.connect(...)` mở WebSocket tới `wss://stt-rt.soniox.com/transcribe-websocket`.
4. Rust command `start_capture` bắt đầu capture audio từ source đã chọn.
5. Rust gom PCM thành batch khoảng `200 ms`, gửi sang JS bằng `Channel<Vec<u8>>`.
6. JS nhận batch và gọi `sonioxClient.sendAudio(bytes.buffer)`.
7. Soniox stream ngược về các token với cờ `is_final`, `translation_status`, `speaker`, `language`, `confidence`.
8. `soniox.js` gom token thành 3 luồng:
   - `originalText`: phần gốc đã final.
   - `translationText`: phần dịch đã final.
   - `provisionalText`: phần gốc chưa final.
9. `app.js` nối callback từ Soniox sang `TranscriptUI`:
   - `onOriginal -> transcriptUI.addOriginal(...)`
   - `onTranslation -> transcriptUI.addTranslation(...)`
   - `onProvisional -> transcriptUI.setProvisional(...)`
10. `ui.js` biến chúng thành segment hiển thị trên overlay.
11. Khi stop, app không lưu từ display buffer đã trim, mà lưu từ `sessionLog` để giữ toàn bộ transcript của phiên.

## 4. Chuẩn audio đầu vào mà Soniox cần

`my-translator` chuẩn hóa mọi nguồn audio về cùng một format trước khi gửi Soniox:

- `audio_format: pcm_s16le`
- `sample_rate: 16000`
- `num_channels: 1`

Đây là điều kiện cực kỳ quan trọng vì toàn bộ logic batching, timing, websocket send, và chất lượng STT của Soniox đều giả định format này.

### 4.1 System audio trên macOS

Trong `system_audio.rs`:

- ScreenCaptureKit phát ra audio `48 kHz`, `float32`, stereo.
- Code chỉ lấy một channel để chuyển về mono.
- Sau đó downsample từ `48 kHz -> 16 kHz` bằng cách lấy mẫu theo bước 3.
- Cuối cùng convert từ `f32 [-1.0, 1.0]` sang `i16 little-endian`.

Ý nghĩa:

- Giảm dữ liệu gửi đi.
- Đồng bộ pipeline cho mọi nguồn audio.
- Độ trễ thấp vì transform rất đơn giản, không dùng pipeline DSP nặng.

### 4.2 Microphone trên mọi nền tảng

Trong `microphone.rs`:

- Dùng `cpal` để lấy default input device.
- Hỗ trợ input `f32` và `i16`.
- Nếu nhiều channel thì mixdown về mono.
- Nếu sample rate khác `16 kHz` thì resample bằng `simple_resample`.

### 4.3 Batch audio để cân bằng latency và ổn định

Trong `commands/audio.rs`:

- Dữ liệu audio được append vào buffer.
- Buffer được flush mỗi `200 ms`.
- Không đẩy từng chunk cực nhỏ sang JS vì sẽ tăng overhead IPC/WebSocket.
- Không đợi quá lâu vì sẽ làm tăng latency nhìn thấy.

Đây là một điểm cân bằng rất quan trọng của `my-translator`.

## 5. Cấu hình Soniox mà dự án gửi lên WebSocket

Ngay khi socket `onopen`, `soniox.js` build config:

```json
{
  "api_key": "...",
  "model": "stt-rt-v4",
  "audio_format": "pcm_s16le",
  "sample_rate": 16000,
  "num_channels": 1,
  "enable_endpoint_detection": true,
  "max_endpoint_delay_ms": 3000,
  "enable_speaker_diarization": true,
  "enable_language_identification": true,
  "translation": {
    "type": "one_way",
    "target_language": "vi"
  },
  "context": {
    "general": [...],
    "terms": [...],
    "translation_terms": [...],
    "text": "..."
  }
}
```

### 5.1 One-way mode

Nếu dùng một chiều:

- `translation.type = 'one_way'`
- `translation.target_language = targetLanguage`
- Nếu người dùng khóa source language thì gửi `language_hints = [sourceLanguage]`

### 5.2 Two-way mode

Nếu dùng hai chiều:

- `translation.type = 'two_way'`
- `translation.language_a = languageA`
- `translation.language_b = languageB`
- `language_hints = [languageA, languageB]`

Ý tưởng là Soniox tự nhận ra ai đang nói ngôn ngữ A hoặc B, rồi dịch sang ngôn ngữ còn lại.

### 5.3 Endpoint delay

`max_endpoint_delay_ms` mặc định là `3000`.

Đây là tham số cực quan trọng cho timing câu phụ đề:

- Quá thấp: câu bị chặt vụn, dịch ra nhiều đoạn ngắn, khó đọc.
- Quá cao: câu đẹp hơn nhưng latency nhìn thấy tăng rõ.

Thiết kế của `my-translator` là cho phép chỉnh tham số này trong settings để phù hợp từng use case.

## 6. Mô hình dữ liệu hiển thị của `ui.js`

`TranscriptUI` dùng hai cấu trúc khác nhau:

### 6.1 `segments[]`

Đây là display buffer hiện tại:

```js
{
  original,
  translation,
  status,        // 'original' | 'translated'
  speaker,
  language,
  confidence,
  createdAt
}
```

`segments[]` có thể bị trim để overlay không phình vô hạn.

### 6.2 `sessionLog[]`

Đây là full transcript của cả phiên.

- Không bị trim theo display.
- Được update song song khi có `addOriginal` và `addTranslation`.
- Được dùng để save file khi người dùng stop.

Quyết định tách `segments[]` và `sessionLog[]` là một điểm rất đáng tái sử dụng:

- UI luôn gọn, nhẹ.
- Transcript lưu file vẫn đầy đủ.
- Không phải đánh đổi giữa hiệu năng render và lưu trữ lịch sử.

## 7. Ngữ nghĩa token stream của Soniox trong dự án này

`soniox.js` dựa vào ba field quan trọng nhất của Soniox token:

- `translation_status`
- `is_final`
- `text`

Ngoài ra còn dùng thêm:

- `speaker`
- `language`
- `confidence`
- token đặc biệt `"<end>"`

### 7.1 `translation_status`

Code chia token thành ba nhóm:

- `original`: token gốc.
- `translation`: token bản dịch.
- `none`: trong two-way mode, lời nói nằm ngoài hai ngôn ngữ chính; code xem như original chưa dịch.

### 7.2 `is_final`

Đây là cờ quyết định timing hiển thị câu:

- Token `original` + `is_final = false` -> provisional text.
- Token `original` + `is_final = true` -> original final text.
- Token `translation` + `is_final = true` -> translation final text.

`my-translator` không cố đoán câu bằng heuristic ở frontend. Nó dựa chủ yếu vào finalization do Soniox phát ra. Đây là quyết định đúng nếu muốn timing hiển thị bám sát engine STT.

### 7.3 `<end>`

Khi token `text === '<end>'`, code set `hasEnd = true`.

Trong code hiện tại, `<end>` không trực tiếp tạo segment mới, nhưng nó là tín hiệu để xóa provisional text khi một batch vừa có dữ liệu final hoặc kết thúc câu.

## 8. Thuật toán cốt lõi để tạo phụ đề song ngữ

Đây là phần quan trọng nhất nếu muốn tái sử dụng sang dự án khác.

### 8.1 Bước 1: gom token theo loại

Pseudo-code tương đương `soniox.js`:

```text
originalText = ''
translationText = ''
provisionalText = ''
speaker = null
language = null
confidenceSum = 0
confidenceCount = 0

for token in data.tokens:
    if token.text == '<end>':
        hasEnd = true
        continue

    if token.speaker && token.translation_status == 'original':
        speaker = token.speaker

    if token.language && token.translation_status != 'translation':
        language = token.language

    if token.confidence != null && token.is_final && token.translation_status == 'original':
        confidenceSum += token.confidence
        confidenceCount++

    if token.translation_status == 'original':
        if token.is_final:
            originalText += token.text
        else:
            provisionalText += token.text

    else if token.translation_status == 'translation':
        if token.is_final:
            translationText += token.text

    else if token.translation_status == 'none':
        if token.is_final:
            originalText += token.text
        else:
            provisionalText += token.text
```

### 8.2 Bước 2: đẩy callback theo thứ tự

Sau khi parse xong batch:

1. Nếu có confidence thì emit average confidence.
2. Nếu có `originalText.trim()` thì emit original ngay.
3. Nếu có `translationText.trim()` thì emit translation ngay sau đó.
4. Nếu còn provisional thì emit provisional.
5. Nếu batch có final text hoặc `<end>` nhưng không còn provisional thì emit provisional rỗng để UI xóa text mờ.

Điểm đáng chú ý: app không đợi translation xong mới tạo original segment. Nó tạo original segment trước, rồi translation tới sau sẽ gắn vào segment cũ nhất chưa dịch.

### 8.3 Bước 3: ghép translation vào segment gốc cũ nhất chưa dịch

Trong `ui.js`, `addTranslation(text)` làm đúng điều này:

```text
find first segment where status == 'original'
if found:
    segment.translation = text
    segment.status = 'translated'
else:
    create translated-only segment
```

Đây là thuật toán ghép cặp cốt lõi của `my-translator`.

Ý nghĩa:

- Original final có thể đến trước translation final vài trăm mili giây hoặc vài giây.
- UI vẫn giữ chỗ cho câu gốc.
- Khi translation tới, nó được nối vào đúng segment FIFO đầu tiên chưa hoàn tất.

### 8.4 Vì sao cách này hoạt động tốt

Nếu Soniox giữ thứ tự stream ổn định giữa original và translation của từng câu, thì chiến lược FIFO là đơn giản và hiệu quả:

- Không cần sentence ID riêng.
- Không cần align token-level phức tạp.
- Độ trễ thấp vì original có thể xuất hiện trước ngay khi final.
- Translation được gắn vào khi sẵn sàng.

### 8.5 Điều kiện để chiến lược FIFO tiếp tục đúng

Khi tái sử dụng, cần giữ các giả định này:

1. Soniox phải phát original/translation theo thứ tự câu.
2. Không được reorder batch ở frontend.
3. Audio source không được tạo quá nhiều câu pending song song vượt khả năng cleanup.
4. Nếu sau này dùng nhiều luồng song song hoặc nhiều websocket, cần thêm sentence ID thay vì chỉ dùng FIFO.

## 9. Timing hiển thị câu chuẩn nhưng vẫn trễ thấp: vì sao thiết kế này hiệu quả

Đây là điểm quan trọng nhất của `my-translator`.

### 9.1 Dùng provisional text để người dùng thấy app đang nghe ngay lập tức

Khi token chưa final, app hiển thị `provisionalText` ở dạng dimmed.

Tác dụng:

- Người dùng cảm giác phản hồi gần như realtime.
- Không cần chờ endpoint detection mới thấy chữ xuất hiện.
- Khi final tới, provisional biến mất và nhường chỗ cho segment final.

### 9.2 Chỉ commit segment khi token đã final

App không đẩy provisional vào `segments[]` như một câu chính thức.

Điều này giúp:

- Không bị rung chữ liên tục.
- Không tạo nhiều câu sai rồi phải rewrite.
- Timing hiển thị câu bám sát quyết định finalization của Soniox.

### 9.3 Single mode và dual mode xử lý khác nhau

#### Single mode

- Chỉ render segment đã có translation.
- Segment `status === 'original'` bị bỏ qua khỏi main display.
- Người dùng nhìn thấy câu dịch sạch, không bị nhòe bởi original pending.

#### Dual mode

- Cột trái hiện original.
- Cột phải hiện translation.
- Nếu original đã final nhưng translation chưa có, cột phải hiện `...`.

Điều này rất hữu ích cho use case cần theo dõi song ngữ thực sự.

### 9.4 Cleanup segment cũ để tránh lệch hàng và lệch timing

`_cleanupStaleOriginals()` làm 2 việc:

- Xóa original chưa dịch nếu quá `10 giây`.
- Giới hạn tối đa `3` pending originals.

Nếu không có cleanup này, UI có thể bị tình huống:

- Translation tới muộn cho câu cũ.
- Câu mới dồn vào hàng đợi.
- Ghép FIFO bắt đầu lệch hoặc overlay đầy rác.

### 9.5 Tách display buffer và session log

Display chỉ cần ngắn, nhưng transcript lưu file phải đầy đủ.

Thiết kế `segments[]` + `sessionLog[]` giúp app:

- Trim UI mạnh tay mà không mất dữ liệu.
- Không phải giữ cả phiên trong DOM.
- Save transcript cuối phiên chính xác hơn.

### 9.6 Session reset mềm mỗi 3 phút

`soniox.js` reset session mỗi `3 phút` bằng chiến lược make-before-break:

1. Mở WebSocket mới.
2. Gửi config và carryover context.
3. Chỉ khi socket mới sẵn sàng mới đóng socket cũ.

Điều này giảm rủi ro:

- audio gap,
- mất câu giữa chừng,
- context đứt đoạn hoàn toàn.

### 9.7 Keepalive khi im lặng

Mỗi `15 giây`, app gửi:

```json
{ "type": "keepalive" }
```

Mục đích là tránh socket bị timeout trong khoảng im lặng dài.

## 10. Context engineering của dự án này

`my-translator` xây context theo format mới của Soniox:

- `general`: cặp key-value tổng quát.
- `terms`: danh sách thuật ngữ tăng độ chính xác nhận dạng.
- `translation_terms`: cặp source -> target, ép cách dịch các thuật ngữ quan trọng.
- `text`: background text dài hơn.

Ngoài ra còn có carryover context từ các bản dịch gần nhất:

- `_recentTranslations[]` giữ rolling buffer.
- Tổng độ dài bị cắt ở `500 ký tự`.
- Khi reset session, app nối buffer này vào `context.text` dưới dạng `Recent conversation: ...`.

Đây là kỹ thuật rất đáng tái sử dụng nếu muốn duy trì tính mạch lạc xuyên nhiều websocket session.

## 11. Thuật toán tái sử dụng được khuyến nghị

Nếu muốn xây lại tính năng tương tự trong dự án khác, nên giữ nguyên các nguyên tắc sau.

### 11.1 Data model đề xuất

```ts
type SubtitleSegment = {
  original: string
  translation: string | null
  status: 'original' | 'translated'
  speaker: string | null
  language: string | null
  confidence: number | null
  createdAt: number
}
```

Nên có hai buffer:

- `displaySegments`: có thể trim.
- `sessionSegments`: không trim.

### 11.2 Callback contract giữa STT engine và UI

```ts
onOriginal(text, speaker, language)
onTranslation(text)
onProvisional(text, speaker, language)
onConfidence(avgConfidence)
onStatusChange(status)
onError(message)
```

### 11.3 Pseudo-code đầy đủ cho pipeline song ngữ

```text
start():
    load settings
    connect websocket
    start audio capture

audio batch received:
    websocket.send(pcm)

response received:
    parse tokens

    if final original text exists:
        push new pending segment

    if final translation exists:
        attach to oldest pending segment

    if provisional exists:
        update provisional display
    else:
        clear provisional display

    render UI

stop():
    stop capture
    close websocket
    save session log
```

### 11.4 Heuristic nên giữ nguyên

- Audio batch `200 ms`.
- Keepalive `15 s`.
- Session reset `3 phút`.
- Carryover context `500 ký tự`.
- Cleanup original quá hạn `10 s`.
- Max pending originals `3`.
- Confidence warning threshold `< 0.7`.
- Endpoint delay default `3000 ms`.

## 12. One-way và two-way: khác nhau ở đâu

### 12.1 One-way

Phù hợp khi có một ngôn ngữ nguồn chính và một ngôn ngữ đích cố định.

Ưu điểm:

- logic đơn giản,
- TTS bật được,
- UI ít ambiguity hơn.

### 12.2 Two-way

Phù hợp cho meeting hai người nói hai ngôn ngữ khác nhau.

Điểm quan trọng trong `my-translator`:

- app gửi `language_a`, `language_b` cho Soniox,
- app tắt TTS để tránh audio loop,
- token `translation_status === 'none'` được xem như original chưa dịch,
- UI dùng speaker label + language badge để giúp phân tách luồng hội thoại.

Nếu tái sử dụng two-way mode, nên giữ ít nhất ba lớp bảo vệ:

1. tự động disable TTS,
2. hiển thị speaker rõ ràng,
3. hiển thị language badge rõ ràng.

## 13. Transcript persistence

Khi stop:

- app gọi `transcriptUI.getFullSessionText(...)`, không dùng plain text đang hiển thị.
- nội dung được format thành markdown có YAML frontmatter.
- Rust lưu thành file timestamp `.md`.

Ví dụ format lưu:

```md
---
date: 2026-05-05
time: 12:34:56
duration: 8m 12s
source_lang: ja
target_lang: vi
mode: one_way
audio_source: system
model: Soniox Cloud API
segments: 42
---

**Speaker 1:**
> 今日は会議を始めます。
Hôm nay chúng ta bắt đầu cuộc họp.
```

Thiết kế này tốt vì transcript lưu ra luôn usable cho archive hoặc review thủ công.

## 14. Các điểm mạnh nhất của thiết kế `my-translator`

1. Không block UI để chờ translation hoàn chỉnh.
2. Không render provisional như final segment.
3. Ghép cặp original/translation bằng FIFO đơn giản nhưng thực dụng.
4. Cleanup pending segments để ngăn drift.
5. Tách display buffer và session log.
6. Có session reset mềm + carryover context.
7. Có speaker diarization, language ID, confidence pass-through.
8. Two-way mode được thiết kế với guard chống feedback loop.

## 15. Hạn chế và bẫy kỹ thuật phát hiện khi đọc code

### 15.1 Một số settings Soniox chỉ tồn tại ở frontend, chưa persist ở Rust

`app.js` sử dụng các field sau:

- `translation_type`
- `language_a`
- `language_b`
- `language_hints_strict`
- `endpoint_delay`

Nhưng `src-tauri/src/settings.rs` hiện chưa có các field này trong struct `Settings`.

Hệ quả:

- Trong runtime hiện tại, frontend vẫn dùng được vì `settingsManager.save()` merge object ở JS cache.
- Sau khi app restart, các field này không được Rust load lại từ file settings.
- Nghĩa là cấu hình two-way/strict/endpoint delay hiện chưa được persist đầy đủ qua các phiên.

Nếu tái sử dụng kiến trúc này, nên sửa điểm này trước.

### 15.2 Windows loopback hiện có thể bắt cả audio TTS của chính app

Trong `wasapi.rs`, comment ghi rõ path hiện tại là legacy loopback và chưa self-exclude được app audio. Điều này có thể tạo vòng lặp khi bật TTS trên Windows nếu source là system audio.

### 15.3 FIFO pairing phụ thuộc vào thứ tự stream của Soniox

Nếu sau này dùng pipeline phức tạp hơn, hoặc nhiều translation request song song, pairing kiểu "oldest untranslated segment" có thể không còn đủ mạnh. Khi đó cần sentence ID hoặc alignment metadata riêng.

### 15.4 Confidence hiện là trung bình theo batch, chưa phải sentence-level chính danh

Code lấy confidence trung bình từ original final tokens trong batch rồi gán vào segment. Điều này đủ dùng cho highlight tương đối, nhưng chưa phải chấm điểm sentence-level chặt chẽ.

## 16. Checklist tái sử dụng cho dự án khác

### 16.1 Bắt buộc nên giữ

- Chuẩn audio `16 kHz`, `mono`, `pcm_s16le`.
- WebSocket Soniox streaming thay vì request-response batch.
- Provisional text tách khỏi final segment.
- FIFO queue cho pending original segments.
- `sessionLog` tách riêng khỏi buffer hiển thị.
- Keepalive.
- Session reset mềm với carryover context.

### 16.2 Nên cấu hình được bằng settings

- endpoint delay,
- source language / target language,
- two-way vs one-way,
- custom context,
- translation terms,
- speaker label on/off,
- max display lines / chars.

### 16.3 Nếu muốn timing còn chuẩn hơn

Có thể mở rộng bằng các bước sau:

1. Gắn `segmentId` nội bộ cho từng original final.
2. Nếu Soniox cung cấp metadata alignment tốt hơn trong tương lai, map translation theo ID thay vì FIFO.
3. Lưu thêm timestamp audio lúc tạo segment để đo actual latency per sentence.
4. Thêm metric:
   - audio batch -> websocket send,
   - websocket send -> original final,
   - original final -> translation final,
   - translation final -> render complete.

## 17. Khuyến nghị triển khai nếu muốn sao chép nguyên mẫu này

Nếu mục tiêu là tái tạo gần như y hệt `my-translator`, thì blueprint phù hợp nhất là:

1. Audio backend normalize toàn bộ input về `16 kHz mono PCM`.
2. Gửi audio lên Soniox mỗi `200 ms`.
3. Parse token stream theo `translation_status` và `is_final`.
4. Commit original segment ngay khi final original xuất hiện.
5. Gắn translation vào segment original đầu tiên chưa dịch.
6. Hiển thị provisional riêng, không merge vào history.
7. Cleanup pending original quá hạn để tránh UI drift.
8. Trim display buffer nhưng không trim session history.
9. Reset session mềm định kỳ kèm context carryover.
10. Tắt TTS trong two-way mode hoặc khi source là system audio mà backend không self-exclude được.

## 18. Kết luận ngắn gọn

Điểm quan trọng nhất của `my-translator` không nằm ở một heuristic NLP phức tạp, mà nằm ở cách chia rất rõ ba trạng thái của câu:

- đang nghe (`provisional`),
- đã chốt nguyên văn (`original final`),
- đã chốt bản dịch (`translation final`).

Sau đó app dùng một hàng đợi FIFO rất thực dụng để ghép `translation` vào `original` cũ nhất chưa hoàn tất. Kết hợp với audio batching ngắn, endpoint detection có thể chỉnh, cleanup segment cũ, và session reset mềm, dự án đạt được hai mục tiêu đồng thời:

- cảm giác realtime,
- câu phụ đề final đủ ổn định để đọc.

Nếu cần port sang dự án khác, đây là phần nên giữ nguyên nhất.