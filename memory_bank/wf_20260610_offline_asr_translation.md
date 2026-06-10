# Workflow 2026-06-10: Offline ASR Translation

## Mục tiêu

Triển khai prototype tính năng phụ đề offline cho audio tiếng Nhật, dịch sang tiếng Việt hoặc tiếng Anh, theo Spec Kit feature `specs/003-offline-asr-translation`.

## Phạm vi thực hiện

- Tạo plan, research, data model, contracts, quickstart, tasks.
- Sửa helper Spec Kit PowerShell để `setup-tasks.ps1` nhận active feature từ `.specify/feature.json`.
- Triển khai foundation trong `core:subtitle`, `core:datastore`, `feature:settings`, `feature:player`.

## Ghi chú kỹ thuật

- Runtime offline v1 dùng prototype boundary, không nhúng model thật vào repo.
- Model thật phải được tải/import ngoài phiên subtitle offline.
- Phiên offline không được gửi audio/transcript/bản dịch ra network.

## Verification

- `.\gradlew.bat testDebugUnitTest`: PASS sau khi đổi manifest parser từ `org.json` sang `kotlinx.serialization.json`.
- `.\gradlew.bat assembleDebug`: PASS. Lần đầu fail tạm ở `:app:packageDebug`, chạy riêng `:app:packageDebug --stacktrace` pass và rerun `assembleDebug` pass.
