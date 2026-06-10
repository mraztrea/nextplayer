# Hướng dẫn Build file APK Production (Release) cho Next Player

Tài liệu này hướng dẫn chi tiết cách build (biên dịch) và ký (sign) file APK phiên bản Production (Release) cho ứng dụng Next Player trên môi trường Windows sử dụng PowerShell (`pwsh`).

---

## 1. Chuẩn bị môi trường

Để build ứng dụng Android, máy tính của bạn cần được cài đặt các công cụ sau:

1. **Java Development Kit (JDK)**: Yêu cầu cài đặt **JDK 17** hoặc mới hơn (khuyên dùng JDK 17 hoặc JDK 21).
   - Kiểm tra phiên bản Java hiện tại bằng cách chạy lệnh sau trong PowerShell (`pwsh`):
     ```powershell
     java -version
     ```
   - Hãy đảm bảo biến môi trường `JAVA_HOME` đã được cấu hình trỏ tới thư mục cài đặt JDK của bạn.
2. **Android SDK**: Thường đi kèm khi cài đặt Android Studio.
   - Hãy cấu hình đường dẫn Android SDK trong file [local.properties](file:///d:/Projects/Canhan/nextplayer/local.properties) ở thư mục gốc của dự án. Ví dụ:
     ```properties
     sdk.dir=C\:\\Users\\Tên_User\\AppData\\Local\\Android\\Sdk
     ```

---

## 2. Tạo Keystore để ký ứng dụng (Release Key)

Hệ điều hành Android yêu cầu tất cả các file APK phải được ký bằng một chứng chỉ số (digital certificate) trước khi được cài đặt lên thiết bị thật hoặc tải lên Google Play Store.

Để tạo một file Keystore mới cho phiên bản Production, hãy làm theo các bước sau:

1. Mở PowerShell (`pwsh`).
2. Chạy lệnh `keytool` (đi kèm với JDK) để tạo một keystore mới:
   ```powershell
   keytool -genkey -v -keystore nextplayer-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias nextplayer-key-alias
   ```
3. Nhập mật khẩu cho keystore, mật khẩu cho key và thông tin cá nhân/tổ chức theo yêu cầu của hệ thống.
4. Lệnh này sẽ tạo ra một file tên là `nextplayer-release-key.jks`. Hãy lưu trữ file này cẩn thận và tuyệt đối bảo mật, vì nếu mất nó bạn sẽ không thể cập nhật ứng dụng của mình trên cửa hàng ứng dụng sau này.

---

## 3. Cấu hình Signing Key trong Dự án

Có hai cách để ký ứng dụng khi build: **Cấu hình tự động** thông qua cấu hình Gradle hoặc **Ký thủ công** sau khi build xong APK.

### Cách 1: Cấu hình tự động thông qua Gradle (Khuyên dùng)

Để tự động ký APK khi chạy lệnh build, bạn có thể cấu hình thông tin keystore trong file [app/build.gradle.kts](file:///d:/Projects/Canhan/nextplayer/app/build.gradle.kts).

**Lưu ý bảo mật:** Không nên ghi trực tiếp mật khẩu vào file `build.gradle.kts`. Hãy cấu hình thông qua file `gradle.properties` (và tránh commit thông tin nhạy cảm này lên kho mã nguồn công cộng).

1. Di chuyển file `nextplayer-release-key.jks` vừa tạo vào thư mục [app/](file:///d:/Projects/Canhan/nextplayer/app/) của dự án.
2. Thêm các thông tin cấu hình vào file [gradle.properties](file:///d:/Projects/Canhan/nextplayer/gradle.properties) ở thư mục gốc của dự án:
   ```properties
   RELEASE_STORE_PASSWORD=mật_khẩu_keystore_của_bạn
   RELEASE_KEY_ALIAS=nextplayer-key-alias
   RELEASE_KEY_PASSWORD=mật_khẩu_key_của_bạn
   ```
3. Mở file [app/build.gradle.kts](file:///d:/Projects/Canhan/nextplayer/app/build.gradle.kts) và chỉnh sửa cấu hình:

   Trong khối `signingConfigs`, thêm cấu hình cho `release`:
   ```kotlin
   signingConfigs {
       getByName("debug") {
           // ... giữ nguyên cấu hình debug
       }
       create("release") {
           storeFile = file("${project.rootDir}/app/nextplayer-release-key.jks")
           storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String? ?: ""
           keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String? ?: ""
           keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String? ?: ""
       }
   }
   ```

   Trong khối `buildTypes`, cập nhật `release` sử dụng `signingConfig` vừa tạo:
   ```kotlin
   buildTypes {
       getByName("release") {
           isMinifyEnabled = true
           isShrinkResources = true
           signingConfig = signingConfigs.getByName("release") // Thêm dòng này để tự động ký
           proguardFiles(
               getDefaultProguardFile("proguard-android-optimize.txt"),
               "proguard-rules.pro",
           )
       }
       // ...
   }
   ```

### Cách 2: Ký thủ công bằng công cụ `apksigner` (Không cần sửa file Gradle)

Nếu bạn không muốn lưu thông tin key trong file cấu hình Gradle, bạn chỉ cần build ra file APK release chưa ký (unsigned APK), sau đó sử dụng công cụ của Android SDK để ký thủ công (xem chi tiết ở mục 6).

---

## 4. Các lệnh Build APK trong PowerShell

Mở PowerShell tại thư mục gốc của dự án và chạy các lệnh tương ứng dưới đây:

### A. Build bản Release chính thức (Production)
Chạy lệnh sau để build bản Production:
```powershell
.\gradlew.bat assembleRelease
```
*Lưu ý:* 
- Nếu đã cấu hình **Cách 1 (Tự động ký)**, kết quả đầu ra sẽ là các file APK đã được ký sẵn và có thể cài đặt, phát hành ngay lập tức.
- Nếu dùng **Cách 2 (Ký thủ công)**, file APK đầu ra sẽ là file chưa ký (unsigned). Bạn phải tiến hành ký trước khi cài đặt.

### B. Build bản Release ký bằng Debug Key (Dành cho kiểm thử nội bộ)
Bản build này có cấu hình tối ưu hóa giống hệt bản Release chính thức (đã bật Proguard/Minify) nhưng được ký tự động bằng Debug Key có sẵn trong dự án. Điều này giúp lập trình viên kiểm tra nhanh hiệu năng và lỗi của bản release trên thiết bị thật trước khi đóng gói chính thức.
```powershell
.\gradlew.bat assembleRelease-with-debug-signing
```

---

## 5. Vị trí file APK đầu ra sau khi build thành công

Sau khi quá trình build hoàn tất, các file APK sẽ được tạo ra tại các đường dẫn sau:

- Đối với bản **Release chính thức**: 
  `app/build/outputs/apk/release/`
- Đối với bản **Release dùng Debug Key**: 
  `app/build/outputs/apk/release-with-debug-signing/`

### Về cơ chế phân tách APK (Splits):
Dự án được cấu hình phân tách APK theo kiến trúc CPU (thiết lập `splits` trong file gradle) để tối ưu dung lượng tải về cho người dùng. Thư mục đầu ra sẽ bao gồm:
1. **File APK Universal** (Khuyên dùng để cài đặt thủ công nhanh chóng lên mọi thiết bị):
   - `app-universal-release.apk` (chứa toàn bộ các thư viện cho mọi kiến trúc chip).
2. **Các file APK tối ưu hóa** theo từng kiến trúc CPU (dung lượng nhỏ hơn, dùng để phát hành tối ưu):
   - `app-arm64-v8a-release.apk` (cho điện thoại Android hiện đại 64-bit).
   - `app-armeabi-v7a-release.apk` (cho điện thoại Android cũ 32-bit).
   - `app-x86-release.apk` và `app-x86_64-release.apk` (cho các thiết bị dùng chip Intel hoặc giả lập).

---

## 6. Hướng dẫn Ký file APK thủ công (Nếu dùng Cách 2)

Nếu bạn build ra file APK release chưa ký (unsigned), hãy làm theo các bước sau để ký file APK trên Windows:

1. **Căn chỉnh file APK (Zipalign)**:
   Sử dụng công cụ `zipalign` (nằm trong Android SDK Build Tools) để tối ưu cấu trúc file APK trước khi ký:
   ```powershell
   & "C:\Users\Tên_User\AppData\Local\Android\Sdk\build-tools\<phiên_bản>\zipalign.exe" -v 4 app-universal-release-unsigned.apk app-universal-release-aligned.apk
   ```
   *(Thay đổi đường dẫn SDK Android và phiên bản build-tools tương ứng trên máy của bạn).*

2. **Ký file APK bằng apksigner**:
   Sử dụng công cụ `apksigner` để ký file đã được căn chỉnh bằng keystore của bạn:
   ```powershell
   & "C:\Users\Tên_User\AppData\Local\Android\Sdk\build-tools\<phiên_bản>\apksigner.bat" sign --ks nextplayer-release-key.jks --out app-universal-release-signed.apk app-universal-release-aligned.apk
   ```
   *Hệ thống sẽ yêu cầu bạn nhập mật khẩu keystore đã thiết lập.*

3. **Xác minh file APK đã ký thành công**:
   Chạy lệnh xác minh để đảm bảo APK được ký đúng định dạng và sẵn sàng cài đặt:
   ```powershell
   & "C:\Users\Tên_User\AppData\Local\Android\Sdk\build-tools\<phiên_bản>\apksigner.bat" verify app-universal-release-signed.apk
   ```
   Nếu không xuất hiện thông báo lỗi, file `app-universal-release-signed.apk` của bạn đã được ký thành công!
