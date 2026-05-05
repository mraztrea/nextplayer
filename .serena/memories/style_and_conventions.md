# Style And Conventions
- Phản hồi và tài liệu cho người dùng dùng tiếng Việt.
- Môi trường Windows: ưu tiên lệnh `pwsh -NoProfile -Command` trong terminal; không dùng lệnh Linux thuần khi có lựa chọn PowerShell.
- Theo AGENTS: mọi shell command phải prefix `rtk`.
- Khi sửa code, thay đổi tối thiểu, bám đúng yêu cầu, không refactor lan sang vùng khác.
- Nếu sửa nhiều file code thì tạo file workflow `memory_bank/wf_{YYYYMMDD}_{ten_workflow}.md` và ghi hướng dẫn migrate/chạy lệnh nếu cần.
- Git commit message dùng tiếng Việt, giữ tiền tố chuẩn như `fix`, `feat`, `refactor`, `test`, `chore`.
- Repo dùng ktlint trên toàn bộ subprojects; code Kotlin theo style Android/Kotlin hiện hữu của repo.