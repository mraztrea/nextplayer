# Workflow 2026-05-17 - Sửa Next/Prev cho ACTION_VIEW từ file manager ngoài

## Mục tiêu
- Khi `PlayerActivity` được mở bằng `Intent.ACTION_VIEW` chỉ có một URI từ app ngoài như CxFileExplorer, player vẫn cố gắng suy ra các video anh em trong cùng thư mục để Next/Prev hoạt động.

## File đã sửa
- `core/domain/src/main/java/dev/anilbeesetti/nextplayer/core/domain/ResolvePlaybackQueueUseCase.kt`
- `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/PlayerActivity.kt`
- `core/domain/src/test/java/dev/anilbeesetti/nextplayer/core/domain/ResolvePlaybackQueueUseCaseTest.kt`

## Cách triển khai
1. Mở rộng sibling discovery trong `ResolvePlaybackQueueUseCase`:
   - Ưu tiên thử suy `currentPath` bằng `Context.getPath(uri)`.
   - Nếu lấy được path local, quét đúng thư mục cha để lấy các video anh em và giữ nguyên URI hiện tại của item đang phát.
   - Nếu không resolve được bằng path, tiếp tục fallback sang `DocumentsContract` như trước.
2. Trong `PlayerActivity`, khi queue hydration resolve được nhiều hơn 1 item mà intent ban đầu chưa có playback context, cache lại:
   - `PlayerApi.API_PLAYBACK_CONTEXT_URIS`
   - `PlayerApi.API_PLAYBACK_SOURCE_TYPE`
3. Thêm unit test hẹp cho helper path-based discovery.

## Xác thực đã chạy
- `rtk .\gradlew --no-daemon :core:domain:testDebugUnitTest --tests "dev.anilbeesetti.nextplayer.core.domain.ResolvePlaybackQueueUseCaseTest"`
- `rtk .\gradlew --no-daemon :feature:player:compileDebugKotlin`

## Lưu ý môi trường
- Cần ép Gradle dùng JDK đầy đủ với `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot` vì runtime Java của VS Code extension thiếu `jlink.exe`.
- Không cần migrate database.
