# Implementation Plan: Xem video trong mạng LAN

**Branch**: `005-lan-video-playback` | **Date**: 2026-05-19 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/005-lan-video-playback/spec.md`

## Summary

Thêm luồng duyệt và phát video từ SMB/shared folder trong mạng LAN: người dùng lưu server với share/root ban đầu, duyệt thư mục con như local, xem thumbnail tải lười có cache, phát video qua player hiện có, và bookmark thư mục LAN để mở nhanh.

Giải pháp được chia theo ranh giới hiện hữu của app: SMB/network access nằm sau một abstraction riêng, metadata bền vững nằm trong Room, password nằm trong encrypted credential store, UI mở rộng từ `feature:videopicker`, playback mở rộng từ `feature:player`, thumbnail tích hợp với cơ chế Coil/cache hiện có.

## Technical Context

**Language/Version**: Kotlin 2.3.20, Android native  
**Primary Dependencies**: Jetpack Compose, Hilt, Room 2.8.4, DataStore, Coil 3.4.0, Media3 1.10.0, kotlinx.coroutines 1.10.2; thêm SMBJ 0.14.0 và AndroidX Security Crypto 1.1.0  
**Storage**: Room cho server/bookmark/thumbnail metadata; AndroidX Security Crypto cho password; app cache cho thumbnail LAN  
**Testing**: JUnit4, kotlinx-coroutines-test, Room androidTest/unit tests theo module hiện có, Compose UI tests cho luồng picker, compile/build Gradle  
**Target Platform**: Android mobile app  
**Project Type**: Mobile app, multi-module Gradle  
**Performance Goals**: Thư mục LAN tối đa 500 mục thao tác được trong 5 giây; 90% mục đang thấy có thumbnail hoặc placeholder trong 3 giây; mở bookmark trong 10 giây khi server truy cập được  
**Constraints**: Phiên bản đầu chỉ hỗ trợ SMB/shared folder; không tự quét LAN; không bắt buộc liệt kê toàn bộ share; không hiển thị password rõ; thumbnail không được chặn duyệt thư mục; playback hiện tại được ưu tiên hơn việc dựng playlist/phụ trợ  
**Scale/Scope**: Nhiều server đã lưu, nhiều bookmark, thư mục LAN tối đa 500 mục trong tiêu chí nghiệm thu; cache thumbnail phải có cơ chế hết hạn theo dấu hiệu file đổi/xóa

## Constitution Check

Constitution file hiện vẫn là template chưa ratify, nên không có nguyên tắc dự án cụ thể để gate formal. Áp dụng các rule đang có trong `AGENTS.md` và memory dự án:

- Tài liệu và phản hồi bằng tiếng Việt.
- Windows-first: lệnh dùng `rtk pwsh` hoặc `rtk .\gradlew`, không dùng lệnh Linux thuần.
- Thay đổi tối thiểu, bám đúng feature, không refactor ngoài phạm vi.
- Nếu implementation sửa nhiều file code, tạo `memory_bank/wf_YYYYMMDD_lan_video_playback.md`.
- Verification tối thiểu khi implement: compile các module liên quan, unit tests mới, UI/manual quickstart với SMB share thật hoặc giả lập.

**Gate Status**: PASS. Không có gate constitution đã ratify bị vi phạm. Các rủi ro chính được chuyển thành decision trong [research.md](./research.md).

## Project Structure

### Documentation (this feature)

```text
specs/005-lan-video-playback/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── lan-smb-client-contract.md
│   └── lan-video-ui-contract.md
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```text
gradle/libs.versions.toml                    # thêm SMBJ và AndroidX Security Crypto
settings.gradle.kts                          # thêm module :core:lan nếu module mới được tạo
core/lan/                                    # SMB client, credential facade, thumbnail/playback primitives
core/model/                                  # model LAN server, folder, media item, bookmark
core/database/                               # Room entities/DAO/migration cho server, bookmark, thumbnail metadata
core/data/                                   # repository phối hợp Room + credential store + SMB client
core/domain/                                 # use case thêm/sửa/xóa server, duyệt folder, bookmark, mở bookmark
core/media/                                  # thumbnail cache/fetcher integration nếu cần dùng chung
feature/videopicker/                         # UI quản lý server, LAN folder browser, bookmark entrypoints
feature/player/                              # Media3 DataSource/queue handling cho smb:// hoặc URI LAN nội bộ
app/                                         # navigation wiring và ImageLoader/decoder registration nếu cần
memory_bank/                                 # workflow note khi implementation sửa nhiều file code
```

**Structure Decision**: Tạo boundary LAN riêng thay vì nhét trực tiếp vào local media repository. SMB client cần được dùng bởi duyệt folder, thumbnail và playback; một boundary riêng giúp tránh nhân bản connection/auth logic và giảm rủi ro ảnh hưởng local picker.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| New LAN boundary/module | SMB access dùng chung cho browse, thumbnail, playback, credential handling | Đặt SMB code trong `feature:videopicker` sẽ khiến player/thumbnail phải gọi ngược UI layer |
| Encrypted credential store ngoài Room | Password không nên lưu rõ cùng metadata queryable | Lưu password trực tiếp trong Room đơn giản hơn nhưng tăng rủi ro bảo mật |
| Custom playback data source/URI handling | Media3 mặc định không đảm bảo phát trực tiếp SMB | Tải file về local trước khi phát trái yêu cầu không sao chép thủ công |

## Phase 0: Research

Research hoàn tất trong [research.md](./research.md). Các quyết định đã chốt:

- SMBJ 0.14.0 là thư viện SMB client chính cho phạm vi SMB2/SMB3.
- Room lưu metadata; AndroidX Security Crypto lưu password theo profile id.
- Playback cần custom SMB data source hoặc URI LAN nội bộ tương đương, không dựa vào HTTP/file/content local mặc định.
- Thumbnail tải lười, cache theo profile/path/size/modified time và không chặn folder listing.

## Phase 1: Design & Contracts

Thiết kế dữ liệu nằm trong [data-model.md](./data-model.md).

Contracts:

- [LAN SMB Client Contract](./contracts/lan-smb-client-contract.md)
- [LAN Video UI Contract](./contracts/lan-video-ui-contract.md)

Quickstart và verification thủ công nằm trong [quickstart.md](./quickstart.md).

## Post-Design Constitution Check

**Gate Status**: PASS. Design vẫn giữ phạm vi SMB-only, có credential isolation, có tiêu chí performance/test rõ ràng, và không yêu cầu refactor toàn bộ local picker/player.

## Phase 2: Task Planning Handoff

Chạy `/speckit-tasks` sau bước này để sinh `tasks.md`. Task generation cần ưu tiên thứ tự:

1. Dependency/module skeleton và test fixtures SMB.
2. Data model + Room migrations + credential store.
3. SMB client/repository/use cases.
4. LAN browser UI + server manager + bookmarks.
5. Thumbnail lazy cache.
6. Player integration cho SMB URI/DataSource.
7. Verification, workflow note trong `memory_bank`, và manual quickstart.
