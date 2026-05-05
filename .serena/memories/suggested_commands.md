# Suggested Commands
- Kiểm tra trạng thái git: `rtk git status --short`
- Compile module subtitle: `rtk .\gradlew :core:subtitle:compileDebugKotlin`
- Chạy test toàn repo: `rtk .\gradlew test`
- Chạy ktlint: `rtk .\gradlew ktlintCheck`
- Build app debug: `rtk .\gradlew assembleDebug`
- Cài app debug lên thiết bị/emulator: `rtk .\gradlew installDebug`
- Lấy analytics RTK: `rtk gain`
- Chạy lệnh thô nếu cần bypass proxy: `rtk proxy <cmd>`