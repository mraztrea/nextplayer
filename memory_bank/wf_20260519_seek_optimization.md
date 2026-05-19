# Workflow: Seek Optimization (SeekParameters Directional + Throttle 150ms)

**Ngày**: 19/05/2026  
**Mục tiêu**: Áp dụng các tối ưu tìm thấy từ phân tích ngược CX File Explorer video player vào NextPlayer.

## Tóm tắt thay đổi

### Các tối ưu được implement

1. **SeekParameters định hướng** (`NEXT_SYNC` / `PREVIOUS_SYNC`): Thay vì seek đến vị trí chính xác (tốn nhiều CPU để decode), seek đến keyframe gần nhất theo hướng di chuyển — nhanh hơn đáng kể.
2. **Seek throttle 150ms**: Giới hạn gọi `seekTo()` tối đa mỗi 150ms trong khi drag, giảm số lần decode frame trong khi người dùng đang vuốt.
3. **Reset SeekParameters sau seek**: Sau mỗi seek có hướng, reset về `DEFAULT` để seek từ code khác (ví dụ: `seekTo(position)` từ seekbar release) vẫn hoạt động bình thường.

### WakeMode (đã có sẵn — không thay đổi)
`PlayerService.kt` đã có logic chọn `WAKE_MODE_LOCAL` vs `WAKE_MODE_NETWORK` dựa trên `MediaSourceType`. Không cần implement thêm.

---

## Các file bị thay đổi

### 1. `feature/player/src/main/java/.../service/CustomCommands.kt`
- Thêm enum entry `SEEK_TO_DIRECTIONAL`
- Thêm constants `SEEK_POSITION_MS_KEY`, `SEEK_IS_FORWARD_KEY`
- Thêm extension `MediaController.sendSeekToDirectional(positionMs, isForward)`

### 2. `feature/player/src/main/java/.../service/PlayerService.kt`
- Thêm `import androidx.media3.exoplayer.SeekParameters`
- Thêm handler `SEEK_TO_DIRECTIONAL` trong `onCustomCommand`:
  - Set `SeekParameters.NEXT_SYNC` hoặc `PREVIOUS_SYNC` trên ExoPlayer
  - Gọi `seekTo(positionMs)`
  - Reset về `SeekParameters.DEFAULT`

### 3. `feature/player/src/main/java/.../extensions/Player.kt`
- Thêm import `SeekParameters` và `sendSeekToDirectional`
- Thêm extension `Player.seekToWithDirection(positionMs, isForward)`:
  - `ExoPlayer`: set SeekParams → seekTo → reset
  - `MediaController`: gửi custom command
  - `else`: fallback `seekTo()`

### 4. `feature/player/src/main/java/.../state/SeekGestureState.kt`
- Thêm import `android.os.SystemClock` và `seekToWithDirection`
- Thêm fields: `lastSeekTime`, `lastSeekDirection`, `previousOnSeekValue`, `SEEK_THROTTLE_MS = 150L`
- `onDragStart()`: reset throttle state
- `onDrag()`: throttle 150ms, bypass khi đổi hướng, dùng `seekToWithDirection()`
- `onSeek()`: throttle 150ms, detect direction từ `previousOnSeekValue`, dùng `seekToWithDirection()`
- `reset()`: reset tất cả throttle fields

### 5. `feature/player/src/main/java/.../state/TapGestureState.kt`
- Thêm import `seekToWithDirection`
- `SEEK_BACKWARD`: dùng `seekToWithDirection(..., isForward = false)`
- `SEEK_FORWARD`: dùng `seekToWithDirection(..., isForward = true)`

---

## Logic throttle chi tiết

```
onDrag được gọi mỗi ~16ms (60fps)
  ├── Cập nhật seekAmount (cho UI hiển thị) — luôn luôn
  ├── Kiểm tra hướng: isForward = dragAmount > 0
  ├── Nếu hướng thay đổi → seek ngay (bypass throttle)
  └── Nếu hướng giữ nguyên:
      └── Nếu (now - lastSeekTime) >= 150ms → seek
          Ngược lại → bỏ qua seek lần này
```

---

## Cách xác minh

1. Build project:
   ```powershell
   rtk ./gradlew :feature:player:assembleDebug
   ```

2. Cài đặt và test thủ công:
   - Vuốt ngang để seek: cảm giác mượt, ít lag hơn trước
   - Vuốt phải (forward): hình ảnh nhảy đến keyframe tiếp theo (có thể hơi quá vị trí muốn)
   - Vuốt trái (backward): hình ảnh nhảy về keyframe trước (có thể hơi trước vị trí muốn)
   - Double-tap phải/trái: cũng dùng keyframe seek
   - Seekbar drag: throttle 150ms, mượt hơn khi kéo nhanh

3. Test edge cases:
   - Seek đến đầu video (position = 0)
   - Seek đến cuối video (position = duration)
   - Đổi hướng giữa chừng (phải → trái) — phải seek ngay không chờ throttle

---

## Không cần migrate database hay lệnh đặc biệt nào.
