Để build ứng dụng Next Player thành file APK hoàn chỉnh trên môi trường Windows (sử dụng PowerShell), bạn có thể làm theo các bước sau:
1. Chuẩn bị môi trường
Đảm bảo bạn đã cài đặt JDK 17 (hoặc phiên bản tương đương được yêu cầu bởi dự án) và đã set biến môi trường JAVA_HOME.
2. Sử dụng Gradle Wrapper để build
Mở PowerShell (pwsh) tại thư mục gốc của dự án (D:/Projects/CaNhan/nextplayer) và chạy các lệnh sau tùy theo nhu cầu của bạn:
Cách A: Build bản Debug (Dùng để kiểm tra nhanh)
Bản này sẽ được ký bằng key debug mặc định, cài đặt được ngay trên điện thoại để test.
./gradlew :app:assembleDebug
•
Kết quả: File APK sẽ nằm tại app/build/outputs/apk/debug/
Cách B: Build bản Release (Đã ký bằng key Debug)
Dự án của bạn có cấu hình build type release-with-debug-signing, giúp tạo ra bản release nhưng sử dụng key debug để bạn có thể cài đặt ngay mà không cần tạo keystore riêng.
./gradlew :app:assembleRelease-with-debug-signing
•
Kết quả: File APK sẽ nằm tại app/build/outputs/apk/release-with-debug-signing/
Cách C: Build bản Release chính thức (Cần Keystore)
Nếu bạn muốn build bản release chuẩn để phát hành:
1.
Bạn cần cấu hình thông tin Keystore trong file build.gradle.kts hoặc qua biến môi trường.
2.
Chạy lệnh:
./gradlew :app:assembleRelease
•
Kết quả: File APK sẽ nằm tại app/build/outputs/apk/release/
3. Lưu ý về kiến trúc CPU (ABI Splits)
Dự án của bạn có cấu hình splits, nên Gradle sẽ tạo ra nhiều file APK cho từng loại kiến trúc (arm64-v8a, armeabi-v7a, x86, x86_64) và một file Universal APK (chứa tất cả các kiến trúc).
•
Nếu bạn muốn một file "hoàn chỉnh" nhất để cài trên mọi máy, hãy tìm file có tên chứa "universal".
Tóm tắt các bước thực hiện:
1.
Mở PowerShell.
2.
Di chuyển vào thư mục dự án: cd D:/Projects/CaNhan/nextplayer
3.
Chạy lệnh build: ./gradlew :app:assembleDebug (hoặc lệnh ở Cách B).
4.
Sau khi build xong, chạy lệnh sau để mở thư mục chứa file APK:
explorer .\app\build\outputs\apk\