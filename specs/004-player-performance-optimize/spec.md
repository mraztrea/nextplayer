# Feature Specification: Tối Ưu Hiệu Suất Video Player

**Feature Branch**: `004-player-performance-optimize`  
**Created**: 2026-05-17  
**Status**: Draft  
**Input**: Áp dụng các kỹ thuật tối ưu hiệu suất từ module video player cũ (code reference) vào dự án NextPlayer hiện tại, nhằm cải thiện tốc độ khởi động video, giảm tiêu thụ bộ nhớ, mở rộng khả năng phát audio formats, và tăng tính ổn định chung.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Khởi động phát video nhanh hơn (Priority: P1)

Khi người dùng mở một video từ thư viện hoặc từ ứng dụng bên ngoài, video phải bắt đầu phát gần như ngay lập tức thay vì phải đợi buffer tải đầy. Đặc biệt với các video local (lưu trên thiết bị), thời gian từ khi chọn đến khi bắt đầu hiển thị khung hình đầu tiên phải được tối thiểu hóa.

**Why this priority**: Đây là trải nghiệm đầu tiên người dùng cảm nhận — sự chậm trễ khi mở video tạo ấn tượng tiêu cực và khiến người dùng chuyển sang ứng dụng khác.

**Independent Test**: Có thể kiểm tra độc lập bằng cách đo thời gian từ khi nhấn chọn video đến khi khung hình đầu tiên hiển thị, so sánh trước và sau tối ưu.

**Acceptance Scenarios**:

1. **Given** người dùng mở một video local (1GB, 1080p), **When** nhấn chọn video, **Then** khung hình đầu tiên hiển thị trong vòng 1 giây
2. **Given** người dùng mở một video local (4K, HEVC), **When** nhấn chọn video, **Then** khung hình đầu tiên hiển thị trong vòng 2 giây
3. **Given** người dùng chuyển sang video tiếp theo trong playlist, **When** nhấn nút Next, **Then** video mới bắt đầu phát trong vòng 1.5 giây

---

### User Story 2 - Phát được nhiều định dạng audio hơn (Priority: P2)

Khi người dùng mở video chứa định dạng audio đặc biệt (DTS, TrueHD, ALAC, AMR...) mà phần cứng thiết bị không hỗ trợ, ứng dụng phải tự động giải mã bằng phần mềm thay vì hiện lỗi "Không hỗ trợ codec".

**Why this priority**: Nhiều video từ Blu-ray rip hoặc nguồn chuyên nghiệp sử dụng DTS/TrueHD/EAC3. Không phát được audio là lỗi nghiêm trọng ảnh hưởng trực tiếp đến trải nghiệm.

**Independent Test**: Có thể kiểm tra bằng cách phát các file video mẫu chứa từng loại audio codec và xác nhận âm thanh phát bình thường.

**Acceptance Scenarios**:

1. **Given** một video chứa audio DTS, **When** thiết bị không hỗ trợ hardware decode DTS, **Then** audio vẫn phát bình thường qua software decoder
2. **Given** một video chứa audio TrueHD/EAC3, **When** người dùng phát video, **Then** audio được giải mã và phát đúng kênh, đúng tốc độ mẫu
3. **Given** một video chứa audio FLAC/Opus/Vorbis embedded, **When** phát video, **Then** audio phát bình thường với chất lượng không suy giảm

---

### User Story 3 - Giảm tiêu thụ tài nguyên hệ thống (Priority: P2)

Khi người dùng phát video local, ứng dụng sử dụng bộ nhớ RAM và CPU ở mức tối thiểu cần thiết, không giữ wake lock không cần thiết cho file local, giúp thiết bị mát hơn và pin lâu hơn.

**Why this priority**: Tiêu thụ tài nguyên quá mức dẫn đến thiết bị nóng, hao pin nhanh — đặc biệt khi xem video dài.

**Independent Test**: Có thể kiểm tra bằng cách đo mức sử dụng RAM và CPU khi phát video cùng loại, so sánh trước và sau tối ưu.

**Acceptance Scenarios**:

1. **Given** người dùng phát video local 1080p, **When** video đang phát, **Then** mức sử dụng RAM giảm ít nhất 15% so với phiên bản hiện tại
2. **Given** người dùng phát video local, **When** video đang phát, **Then** hệ thống không giữ network wake lock
3. **Given** người dùng phát video stream qua mạng, **When** video đang phát, **Then** hệ thống giữ network wake lock để đảm bảo kết nối ổn định

---

### User Story 4 - Cho phép chọn codec ưu tiên (Priority: P3)

Người dùng nâng cao có thể chọn hardware decoder cụ thể để sử dụng cho video playback, giúp tối ưu cho từng thiết bị cụ thể hoặc khắc phục lỗi decoder trên một số thiết bị.

**Why this priority**: Tính năng dành cho power user, giúp khắc phục vấn đề tương thích trên thiết bị cụ thể nhưng không ảnh hưởng trải nghiệm đại đa số người dùng.

**Independent Test**: Có thể kiểm tra bằng cách vào Settings, chọn codec ưu tiên, phát video và xác nhận codec đúng đã được sử dụng.

**Acceptance Scenarios**:

1. **Given** thiết bị có nhiều video decoder (OMX và C2), **When** người dùng chọn codec ưu tiên trong Settings, **Then** ứng dụng ưu tiên sử dụng codec đã chọn khi phát video
2. **Given** codec ưu tiên không hỗ trợ format hiện tại, **When** phát video, **Then** ứng dụng tự động fallback sang codec khả dụng mà không báo lỗi

---

### Edge Cases

- Điều gì xảy ra khi FFmpeg native library không load được trên thiết bị? → Ứng dụng phải tiếp tục hoạt động bình thường với hardware decoder mặc định, chỉ mất khả năng decode các format audio đặc biệt
- Điều gì xảy ra khi buffer size quá nhỏ gây stutter trên video bitrate cao? → Hệ thống phải có cơ chế tự điều chỉnh hoặc fallback về buffer size lớn hơn khi phát hiện stutter
- Điều gì xảy ra khi người dùng phát video qua SMB/NFS (LAN) với buffer tối thiểu? → Buffer phải được điều chỉnh phù hợp cho network playback, không áp dụng buffer tối thiểu cho trường hợp này
- Điều gì xảy ra khi preferred codec bị crash/ANR? → Ứng dụng phải detect lỗi và tự động disable preferred codec, sử dụng default codec cho lần phát tiếp theo

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống PHẢI tối ưu cấu hình buffer cho local playback, giảm thời gian chờ trước khi phát
- **FR-002**: Hệ thống PHẢI phân biệt giữa local file và network stream để áp dụng cấu hình buffer và wake mode phù hợp
- **FR-003**: Hệ thống PHẢI tích hợp FFmpeg software audio decoder hỗ trợ các format: AAC, MP3, AC3, EAC3, DTS, DTS-HD, FLAC, Opus, Vorbis, ALAC, TrueHD, AMR-NB, AMR-WB, PCM A-law, PCM µ-law
- **FR-004**: Hệ thống PHẢI tự động fallback sang FFmpeg software decoder khi hardware decoder không hỗ trợ audio format
- **FR-005**: Hệ thống PHẢI cung cấp tùy chọn cho phép người dùng chọn video decoder ưu tiên
- **FR-006**: Hệ thống PHẢI tự động fallback sang decoder mặc định khi preferred decoder không hỗ trợ format hiện tại
- **FR-007**: Hệ thống PHẢI sử dụng LOCAL wake mode cho file nội bộ và NETWORK wake mode cho stream
- **FR-008**: Hệ thống PHẢI xử lý lỗi khi FFmpeg native library không load được, tiếp tục hoạt động với hardware decoder
- **FR-009**: Hệ thống PHẢI chuẩn bị media source không chặn UI thread chính
- **FR-010**: Hệ thống PHẢI log thông tin codec chi tiết khi phát sinh lỗi decode để hỗ trợ debug

### Key Entities

- **PlayerConfig**: Tập hợp các thông số cấu hình player (buffer size, wake mode, preferred codec) — thay đổi dựa trên loại media source
- **DecoderPreference**: Lưu trữ codec ưu tiên của người dùng cho video decoder — liên kết với Settings
- **AudioDecoderRegistry**: Danh sách audio decoder khả dụng (hardware + software) — xác định cách giải mã audio

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Thời gian từ khi chọn video local đến khung hình đầu tiên hiển thị giảm ít nhất 30% so với phiên bản hiện tại
- **SC-002**: Ứng dụng phát được ít nhất 14 định dạng audio khác nhau (bao gồm DTS, TrueHD, ALAC, Opus) mà không hiển thị lỗi codec
- **SC-003**: Mức sử dụng RAM khi phát video local 1080p giảm ít nhất 15% so với phiên bản hiện tại
- **SC-004**: 100% các trường hợp FFmpeg library không load được vẫn cho phép phát video với hardware decoder mà không crash
- **SC-005**: Không có hiện tượng UI freeze quá 100ms trong quá trình khởi tạo player và chuyển video

## Assumptions

- Ứng dụng NextPlayer hiện tại đã sử dụng Media3 ExoPlayer làm engine phát video
- Thiết bị đích chạy Android 5.0+ (API 21+), nơi MediaCodec hardware decoder khả dụng cho hầu hết video formats phổ biến
- FFmpeg native library sẽ được build cho 4 ABI: armeabi-v7a, arm64-v8a, x86, x86_64
- Người dùng chủ yếu phát video local — tối ưu buffer cho local playback là ưu tiên cao hơn streaming
- Cấu hình preferred codec chỉ áp dụng cho video decoder, không áp dụng cho audio decoder (audio dùng auto-selection giữa hardware và FFmpeg)
- Tính năng preferred codec là tính năng nâng cao, ẩn trong Advanced Settings
