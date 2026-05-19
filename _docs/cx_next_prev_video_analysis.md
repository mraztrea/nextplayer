# Phân Tích: Cơ Chế Next/Prev Video trong CX File Explorer Video Player

**Ngày phân tích**: 19/05/2026  
**Source**: `code_reference/video_player_module/java_source/com/alphainventor/filemanager/viewer/`  
**Mục tiêu**: Hiểu cơ chế Next/Prev để tham chiếu sửa lỗi tương tự trong NextPlayer

---

## 1. Tổng Quan Kiến Trúc

CX File Explorer sử dụng **mô hình "Push Playlist trước, Play sau"** — hoàn toàn ngược với cách NextPlayer đang làm (NextPlayer cố gắng khám phá danh sách video **sau khi** đã launch Activity).

```
[CX File Manager]              [VideoPlayerActivity]
      │                                 │
      │  1. Gọi e.b().d(playlist)       │
      │  ──────────────────────────>    │
      │                                 │
      │  2. startActivity(intent)        │
      │  ──────────────────────────>    │
      │                                 │  3. Đọc e.b().c() trong L2()
      │                                 │  4. Lưu vào Z[], a0[], c0
      │                                 │  5. G3(currentIndex) → play
```

---

## 2. Singleton Playlist Service (`e.java`)

```java
// Class đơn giản nhất trong codebase — nhưng là chốt chặn then chốt
public class e {
    private static e b;       // singleton instance
    ArrayList a;              // danh sách playlist (ArrayList<e$a>)

    public static e b() {     // lấy singleton
        if (e.b == null) e.b = new e();
        return e.b;
    }

    public void d(ArrayList p1) { this.a = p1; }  // SET playlist (do file manager gọi)
    public ArrayList c() {                          // GET & CLEAR playlist (do VideoPlayerActivity gọi)
        ArrayList v0 = this.a;
        this.a = null;
        return v0;
    }
    public void a() { this.a = null; }             // clear
}
```

Mỗi phần tử playlist là `e$a`:
```java
public class e$a {
    public Uri a;   // URI media video
    public Uri b;   // URI subtitle (có thể null)

    public e$a(Uri mediaUri, Uri subtitleUri) { ... }
}
```

**Flow quan trọng**: File Manager tạo `ArrayList<e$a>` từ tất cả video trong thư mục (hoặc LAN share), gọi `e.b().d(list)` để lưu vào singleton, SAU ĐÓ mới `startActivity()`. VideoPlayerActivity đọc list này ngay trong `onCreate`.

---

## 3. Khởi Tạo Playlist trong VideoPlayerActivity

**Phương thức**: `L2()` — được gọi khi activity nhận intent

```java
private void L2() {
    Intent intent = getIntent();
    this.g0 = -1;   // index trong mảng gốc (non-shuffle)
    this.h0 = -1;   // index trong mảng shuffle

    // Lấy playlist từ singleton
    List playlist = e.b().c();   // lấy & xóa khỏi singleton

    if (playlist == null) {
        // Không có playlist → chỉ chơi 1 file
        Uri single = intent.getData();
        this.Z = new Uri[]{single};
        this.a0 = new Uri[]{i2(single)};   // normalize URI
        this.b0 = new Uri[1];
        this.c0 = new ArrayList();
        this.c0.add(Integer.valueOf(0));
        this.g0 = 0;
    } else {
        // Có playlist từ file manager
        int size = playlist.size();
        this.Z = new Uri[size];    // mảng URI gốc
        this.a0 = new Uri[size];   // mảng URI đã xử lý (để phát)
        this.b0 = new Uri[size];   // mảng URI subtitle

        // Tạo c0 = [0, 1, 2, ... size-1], sau đó shuffle nếu cần
        this.c0 = new ArrayList();
        for (int i = 0; i < size; i++) this.c0.add(i);
        Collections.shuffle(this.c0);   // shuffle order

        // Điền dữ liệu
        for (int i = 0; i < size; i++) {
            e$a item = (e$a) playlist.get(i);
            this.Z[i] = item.a;
            this.a0[i] = i2(item.a);     // i2() = normalize/resolve URI
            if (item.b != null) this.b0[i] = i2(item.b);
        }

        // Tìm index của video đang mở
        Uri currentUri = intent.getData();
        for (int i = 0; i < this.Z.length; i++) {
            if (currentUri.equals(this.Z[i]))     this.g0 = i;  // non-shuffle index
            if (currentUri.equals(this.Z[c0[i]])) this.h0 = i;  // shuffle index
        }
    }

    // Bắt đầu phát từ vị trí tìm được
    int startIndex = this.d0 ? this.h0 : this.g0;  // d0 = shuffle mode
    this.G3(startIndex >= 0 ? startIndex : 0);
}
```

---

## 4. Các Trường Trạng Thái Playlist

| Field | Type | Ý nghĩa |
|-------|------|---------|
| `Z[]` | `Uri[]` | Mảng URI gốc (như file manager truyền vào) |
| `a0[]` | `Uri[]` | Mảng URI đã normalize để ExoPlayer phát |
| `b0[]` | `Uri[]` | Mảng URI subtitle tương ứng |
| `c0` | `ArrayList<Integer>` | Order của shuffle (e.g. `[3,1,4,0,2]`) |
| `g0` | `int` | Index hiện tại trong thứ tự gốc (`-1` nếu không tìm thấy) |
| `h0` | `int` | Index hiện tại trong thứ tự shuffle |
| `i0` | `int` | **Active index** — đây là index thực sự dùng cho navigation |
| `d0` | `boolean` | `true` = shuffle mode đang bật |

---

## 5. Index Management

### `w2()` — Lấy current index (an toàn)
```java
private int w2() {
    int v0 = this.i0;
    if (v0 >= 0) {
        if (v0 < this.C2()) return this.i0;
        else return this.C2() - 1;  // clamp to last
    } else {
        return 0;  // clamp to first
    }
}
```

### `C2()` — Tổng số video
```java
private int C2() {
    return this.a0 != null ? this.a0.length : 0;
}
```

### `G3(index)` — Set current index và cập nhật UI
```java
private void G3(int index) {
    this.i0 = index;
    this.n4();      // cập nhật trạng thái enable/disable của nút
}
```

### `A2(index)` — Lấy index thực tế (xét shuffle)
```java
private int A2(int p2) {
    if (this.d0) {
        p2 = ((Integer) this.c0.get(p2)).intValue();
    }
    return p2;
}
```

### `n4()` — Cập nhật trạng thái nút Next/Prev
```java
private void n4() {
    int total = this.C2();
    int prevEnabled = (total > 0) ? 1 : 0;    // Prev luôn enable khi có item
    int nextEnabled = (this.w2() < (total - 1)) ? 1 : 0;  // Next enable khi chưa phải item cuối

    this.E3(prevEnabled, this.o);  // this.o = nút Prev (id 2131362082)
    this.E3(nextEnabled, this.n);  // this.n = nút Next (id 2131362081)
}
```

---

## 6. Next/Prev Button Flow

### Next Button (id `2131362081`)
```
onClick → VideoPlayerActivity$g.a() → case 2131362081 → VideoPlayerActivity.P1(this.c)
                                                              ↓
                                                          p0.n3()
                                                              ↓
                              if (W != null && w2() < C2()-1)
                                              ↓ (true)
                                          s3()
                                              ↓
                              if (w2() < C2())
                                  h3() → z4() nếu cần (cleanup subtitle)
                                  G3(w2() + 1)    ← SET INDEX +1
                                  r3(w2(), true)  ← LOAD & PLAY
```

### Prev Button (id `2131362082`) — Có logic thông minh
```
onClick → VideoPlayerActivity$g.a() → case 2131362082 → VideoPlayerActivity.Q1(this.c)
                                                              ↓
                                                          p0.u3()
                                                              ↓
                              int state = W.i()         // playback state
                              long pos = W.k0()         // current position ms

                              if (w2() != 0 && (state==4 || pos <= 3000)) {
                                  // Đang ở đầu hoặc video đã kết thúc → Prev video
                                  if (w2() > 0) t3()
                                      ↓
                                  G3(w2() - 1)    ← SET INDEX -1
                                  r3(w2(), true)  ← LOAD & PLAY
                              } else {
                                  // Đang giữa chừng → Seek về đầu
                                  W.s(0)          ← ExoPlayer.seekTo(0)
                              }
```

**Đây là UX thông minh**: Prev khi đang ở giữa video (>3s) sẽ seek về đầu. Prev khi ở <3s đầu mới chuyển sang video trước.

---

## 7. Load & Play Video (`r3` → `VideoPlayerActivity$v`)

`r3(index, resetPosition)` khởi động `AsyncTask` (`VideoPlayerActivity$v`) để:
1. Resolve URI tại index (xét shuffle qua `A2()`)
2. Chuẩn bị subtitle từ `b0[A2(index)]`
3. Gọi ExoPlayer để load source mới
4. Tự động play

---

## 8. Auto-Advance khi Video Kết Thúc

```java
private void o3() {
    if (this.W != null) {
        if (this.w2() >= this.C2() - 1) {
            this.q3();  // Đã hết playlist → finish/loop
        } else {
            this.s3();  // Còn video tiếp → tự động next
        }
    }
}
```

---

## 9. So Sánh với NextPlayer

| Khía cạnh | CX File Explorer | NextPlayer (hiện tại) |
|-----------|-----------------|----------------------|
| **Nguồn playlist** | File Manager PUSH vào singleton trước launch | Player Activity tự DISCOVER sau launch |
| **Cơ chế truyền** | In-process singleton `e.b().d(list)` | Intent extras (`video_list`, `EXTRA_STREAM`) |
| **LAN/SMB** | ✅ Hoạt động — FM đã biết tất cả URI trong LAN share | ❌ Thất bại — `getPath()` trả null cho LAN URI |
| **Folder local** | ✅ FM biết tất cả file trong thư mục | ⚠️ Dùng `listFiles()` + MediaStore (phức tạp) |
| **Content provider** | ✅ FM biết URI từ provider của chính nó | ❌ `DocumentsContract.buildChildDocumentsUri` không phải lúc nào cũng hoạt động |
| **Shuffle** | ✅ Tích hợp sẵn (shuffle `c0` list) | ✅ Tích hợp (ExoPlayer shuffle mode) |
| **Smart Prev** | ✅ (<3s → prev video, ≥3s → seek to start) | ❌ Chỉ có seek to start |

---

## 10. Tại Sao NextPlayer Bị Lỗi "No Next Video" với LAN

**Chain lỗi:**

```
PlayerActivity.startPlayback()
    ↓ uri = smb://192.168.x.x/.../video.mp4
    ↓ launchContext = playerApi.getPlaybackLaunchContext(uri)
    
PlayerApi.getPlaybackLaunchContext()
    ↓ rawPlaylist = getPlaylist()  → EMPTY (CX không pass extras)
    ↓ extras.getString(API_PLAYBACK_CONTEXT_URIS) → null (lần đầu mở)
    ↓ return null   ← không có playlist context

PlayerViewModel.hydratePlaybackQueue()
    ↓ resolvePlaybackQueueUseCase(smbUri, null)
    
ResolvePlaybackQueueUseCase.invoke()
    ↓ getSortedPlaylistUseCase(smbUri)
        ↓ getPath(smbUri) → NULL (smb:// không phải file/content/document URI)
        ↓ return emptyList()
    ↓ fallbackEntries = [] → empty
    
    ↓ discoverSiblingEntries(context, smbUri)
        ↓ discoverSiblingEntriesFromPath()
            ↓ getPath(smbUri) → NULL
            ↓ return emptyList()
        ↓ DocumentsContract.isDocumentUri(smbUri) → false (smb:// không phải DocumentsContract)
        ↓ return emptyList()
    ↓ documentEntries = []
    
    ↓ Kết quả: PlaybackQueueSnapshot{entries=[currentUri], size=1}

applyHydratedQueue()
    ↓ snapshot.uriStrings.size <= 1 → return (không làm gì)

→ ExoPlayer queue chỉ có 1 item
→ player.hasNextMediaItem() = false
→ NextButton.state.isEnabled = false
→ onUnavailableClick() → hiển thị "No next video in queue"
```

---

## 11. Hướng Giải Quyết Tham Khảo

### Hướng A: Yêu cầu app gọi (CX File Explorer) truyền playlist qua Intent

Nếu CX File Explorer có thể cấu hình để truyền playlist:
```kotlin
// Khi launch NextPlayer từ CX File Explorer
intent.putParcelableArrayListExtra("video_list", allVideosInFolder)
```
NextPlayer đã hỗ trợ đọc `video_list` extra qua `PlayerApi.getPlaylist()`.

### Hướng B: NextPlayer tự khám phá URI LAN qua DocumentsContract

Với content URI từ provider LAN (e.g. `content://com.alphainventor.filemanager.provider/...`):
```kotlin
// Đã có trong discoverSiblingEntries() nhưng cần xử lý đúng
val authority = currentUri.authority
val docId = DocumentsContract.getDocumentId(currentUri)
val parentId = docId.substringBeforeLast("/")
val childrenUri = DocumentsContract.buildChildDocumentsUri(authority, parentId)
// Query all video children
```
Cần kiểm tra xem URI từ CX File Explorer khi mở file LAN có phải `content://` hay `smb://`.

### Hướng C: Thêm LAN URI pattern vào discovery

```kotlin
private fun discoverSiblingEntries(context: Context, currentUri: Uri): List<SiblingVideoEntry> {
    val fileEntries = discoverSiblingEntriesFromPath(context, currentUri)
    if (fileEntries.size > 1) return fileEntries
    
    // Thêm: xử lý content URI từ DocumentsProvider LAN
    if (DocumentsContract.isDocumentUri(context, currentUri)) { ... }
    
    // Thêm: xử lý SMB URI trực tiếp (nếu NextPlayer có SMB client)
    if (currentUri.scheme == "smb") {
        return discoverSmb Siblings(currentUri)
    }
    
    return emptyList()
}
```

### Hướng D: Giống CX — Playlist Bridge qua in-process singleton

Tạo `PlaylistBridge` singleton trong NextPlayer, để các app trong cùng process (hoặc qua binder) truyền playlist trước khi launch:
```kotlin
object PlaylistBridge {
    var pendingPlaylist: List<Uri>? = null
}
```
**Hạn chế**: Chỉ hoạt động khi app gọi là cùng process (internal launch), không hoạt động khi external app gọi.

---

## 12. Kết Luận

Cơ chế Next/Prev của CX File Explorer hoạt động ổn định vì **file manager và video player là cùng một app** — chúng chia sẻ in-process state qua singleton `e`. File manager đã tập hợp toàn bộ danh sách video (kể cả từ LAN share) TRƯỚC khi mở player.

NextPlayer bị lỗi với LAN/external launch vì không có cơ chế nhận playlist từ app bên ngoài, và cơ chế tự khám phá (discovery) chỉ hoạt động với local file path, không hoạt động với LAN/SMB URIs.

**Fix pragmatic nhất**: Kiểm tra loại URI và khi là content URI từ DocumentsProvider, dùng `buildChildDocumentsUri` với authority + parentDocumentId để tìm sibling — cách này đã được code trong `discoverSiblingEntries()` nhưng cần debug xem tại sao không hoạt động với URI cụ thể từ CX File Explorer.
