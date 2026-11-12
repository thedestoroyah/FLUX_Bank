# FLUX Bank — Feature 6: Navigation (bản đồ chi nhánh, đường đi bộ ngắn nhất)

- Ngày: 2026-11-12 · Hạn nộp đồ án: 2026-12-01 (dự kiến F6 xong ~15/11)
- Nền tảng: F1–F5 đã xong và nằm trên `main`. F6 **không đụng Firestore, không đổi rules**.
- Mockup: [mockups/2026-11-12-f6-navigation.html](mockups/2026-11-12-f6-navigation.html) (**đã duyệt 2026-11-12**)
- Design system: [DESIGN.md](../../../DESIGN.md)

Ký hiệu: **[Đề]** = yêu cầu của đề bài. **[Chủ dự án]** = chủ dự án đã quyết định. **[Claude]** = Claude tự quyết (chủ dự án bỏ qua phần hỏi đáp); mọi mục [Claude] đều có thể bị chủ dự án sửa khi duyệt.

---

## 1. Mục đích

Barem dành 0,5 điểm cho Navigation. Đề yêu cầu **[Đề]**:
1. Người dùng **xác định được vị trí của mình**.
2. App hiện **người dùng trên bản đồ** và **vị trí các chi nhánh gần đó**.
3. App **gợi ý thông minh đường đi bộ ngắn nhất** tới chi nhánh gần nhất.

Chữ "thông minh" là chỗ ăn điểm: chi nhánh gần nhất **theo đường chim bay** chưa chắc gần nhất **khi đi bộ** (sông, rạch, khu chung cư chặn đường). F6 xếp hạng theo **quãng đường đi bộ thật** và nói rõ điều đó trên màn hình.

## 2. Mục tiêu, không làm, phản mục tiêu

**Mục tiêu**
- Ô "Chi nhánh" trên trang chủ mở màn bản đồ thật (thay `ComingSoonFragment`).
- Mở màn là tự làm hết: xin quyền vị trí → lấy vị trí → vẽ chấm người dùng và các chi nhánh → tính và vẽ đường đi bộ ngắn nhất tới chi nhánh được gợi ý. Người dùng không phải bấm thêm gì.
- Chạm một chi nhánh khác (trên bản đồ hoặc trong danh sách) thì vẽ đường đi bộ tới chi nhánh đó.
- **Chi phí 0 đồng, không key, không billing, Firebase vẫn ở gói Spark**: bản đồ OpenStreetMap (OSMDroid) và server chỉ đường miễn phí **[Chủ dự án]**.
- Demo trọn vẹn trong khoảng 1 phút (§9).

**Không làm** **[Claude]**
- Không chỉ đường từng bước (rẽ trái/phải), không đọc giọng nói, không bám theo vị trí khi đang đi.
- Không chỉ đường lái xe hay xe buýt; không nút mở ứng dụng bản đồ khác.
- Không lưu chi nhánh lên Firestore, không có màn nhân viên quản lý chi nhánh.
- Không ATM, không tìm kiếm chi nhánh theo tên, không lọc theo dịch vụ.
- Không bản đồ offline.

**Phản mục tiêu**
- Gợi ý chi nhánh "gần nhất" mà phải đi bộ vòng xa hơn chi nhánh khác.
- Màn trắng hoặc xoay mãi khi từ chối quyền, tắt định vị hay mất mạng: mỗi trường hợp phải có thông báo và cách sửa (§7).
- Bản đồ màu mè lấn át design system (§7, "nền bản đồ").
- Phát sinh chi phí, hoặc project Firebase bị chuyển lên Blaze.

## 3. Ràng buộc
Giữ như F1–F5: Kotlin + XML/ViewBinding, MVVM, chỉ giao diện sáng, chữ tiếng Việt trong `strings.xml`, chỉ dùng token của design system. Màn mới phải **duyệt mockup trước khi code giao diện** **[Chủ dự án]**. Làm theo TDD. Không commit `.idea/*`.

## 4. Công nghệ

Ban đầu chủ dự án chọn Google Maps + OSRM (2026-11-12). Google Cloud từ chối tạo tài khoản thanh toán (`OR_BACR2_59`, hệ thống chống gian lận; gặp nhiều ở Việt Nam với thẻ debit), nên chuyển sang **OpenStreetMap qua OSMDroid** cùng ngày **[Chủ dự án]**. Phần nghiệp vụ, ViewModel và test không đổi; chỉ đổi lớp hiển thị bản đồ.

| Phần | Chọn | Lý do |
|---|---|---|
| Bản đồ | **OSMDroid** `org.osmdroid:osmdroid-android:6.1.20` (Apache-2.0), `org.osmdroid.views.MapView` trong layout XML, nền ô OpenStreetMap chuẩn (`TileSourceFactory.MAPNIK`) | Miễn phí, không key, không billing; là View thường nên hợp XML/ViewBinding. |
| Vị trí | `com.google.android.gms:play-services-location` (`FusedLocationProviderClient.getCurrentLocation`, ưu tiên độ chính xác cao) | Không cần billing hay key. Trong nhà vẫn lấy được vị trí qua Wi-Fi/mạng. Chấm người dùng do app tự vẽ bằng `Marker`. |
| Đường đi bộ | Server OSRM công cộng của FOSSGIS, profile đi bộ: `https://routing.openstreetmap.de/routed-foot/route/v1/foot/{lng1},{lat1};{lng2},{lat2}?overview=full&geometries=geojson` | Miễn phí, không key. Đã thử 2026-11-12: trả đường đi bộ thật (968 m, 777 s). `router.project-osrm.org` chỉ có profile ô tô nên không dùng. |
| HTTP + JSON | `HttpURLConnection` + `org.json` có sẵn trong Android | Không thêm thư viện mạng. Unit test cần `testImplementation("org.json:json:<bản mới>")` vì `org.json` của Android trong unit test chỉ là stub. |

Điều kiện dùng dịch vụ miễn phí (ghi vào báo cáo):
- OSMDroid phải đặt `Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID` trước khi tạo map (chính sách tile của OSM chặn user-agent mặc định); cache ô bản đồ đặt trong `cacheDir` của app (không cần quyền bộ nhớ).
- Ghi công "© OpenStreetMap" luôn hiện trong sheet (ngay dưới bản đồ), chạm mở trang bản quyền OSM (giấy phép ODbL; dùng cho cả nền bản đồ lẫn đường đi).
- Thêm giấy phép OSMDroid vào `docs/licenses/`.
- Server tile và OSRM công cộng không cam kết SLA, chỉ dùng mức nhẹ; luôn có phương án dự phòng (§5).

## 5. Nghiệp vụ **[Claude]**

### Danh sách chi nhánh
- Cứng trong code: `domain/branch/Branches.kt`, khoảng **12 chi nhánh FLUX giả lập ở TP.HCM**. Mỗi chi nhánh có `id`, tên ("FLUX Nam Sài Gòn"…), địa chỉ, toạ độ, số điện thoại, giờ mở cửa.
- Giờ mở cửa chung: T2–T6 8:00–17:00, T7 8:00–12:00, CN nghỉ. Trạng thái "Đang mở · đến 17:00" / "Đã đóng · mở lại 8:00 T2" tính theo giờ Việt Nam (`TransferPolicy.VN_ZONE`), chỉ để hiển thị, không ảnh hưởng gợi ý.
- **Cụm demo quanh TDTU (19 Nguyễn Hữu Thọ, Q.7)**: đặt 3–4 chi nhánh trong bán kính ~2 km, trong đó **có ít nhất một ca "gần theo đường chim bay nhưng phải đi vòng"** (ví dụ bên kia rạch, không có cầu gần). Builder chọn toạ độ và kiểm bằng `curl` tới server OSRM ở trên trước khi chốt. Nếu chủ dự án quay demo ở chỗ khác, thêm cụm tương tự quanh chỗ đó (§10).
- Phần còn lại rải ở các quận trung tâm để bản đồ thu nhỏ trông như mạng lưới thật.

### Gợi ý đường đi bộ ngắn nhất
1. Tính khoảng cách đường chim bay (haversine) từ người dùng tới mọi chi nhánh, sắp xếp tăng dần.
2. Lấy **3 chi nhánh gần nhất theo đường chim bay** làm ứng viên, gọi server đi bộ cho cả 3 (song song, timeout 8 s mỗi lần).
3. Gợi ý = ứng viên có **quãng đường đi bộ ngắn nhất**. Vẽ đường của nó.
4. Nếu chi nhánh gần nhất theo đường chim bay **khác** chi nhánh được gợi ý, card gợi ý hiện thêm một dòng giải thích, ví dụ "FLUX Tân Phong gần hơn theo đường thẳng (450 m) nhưng phải đi bộ 2,1 km".
5. Ứng viên nào gọi lỗi thì bỏ qua. **Cả 3 đều lỗi** (mất mạng, server sập): gợi ý chi nhánh gần nhất theo đường chim bay, vẽ **đường thẳng nét đứt**, ghi rõ "Không tải được đường đi bộ · khoảng cách đường thẳng" và nút "Thử lại".
6. Người dùng xa mọi chi nhánh (đường chim bay > 20 km) thì vẫn gợi ý chi nhánh gần nhất nhưng **không gọi server đi bộ** (không ai đi bộ 20 km), chỉ hiện khoảng cách.
7. Thời gian đi bộ lấy từ `duration` của server; quãng đường hiển thị "850 m" dưới 1 km, "1,2 km" từ 1 km.
8. Kết quả đường đi lưu tạm trong ViewModel theo `id` chi nhánh cho phiên màn hình; chạm lại chi nhánh đã tính thì không gọi lại server.

### Vị trí
- Xin `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` khi mở màn. Người dùng chỉ cho "vị trí gần đúng" thì vẫn chạy (độ chính xác thấp hơn).
- Lấy một lần khi mở màn; nút "Vị trí của tôi" trên bản đồ lấy lại vị trí, đưa bản đồ về đó và tính lại gợi ý. Không theo dõi liên tục.
- Từ chối quyền: bản đồ vẫn hiện, căn giữa TP.HCM, vẫn có mọi chi nhánh; danh sách xếp theo tên, không có khoảng cách; sheet hiện thông báo kèm nút "Cho phép vị trí" (nếu Android không cho hỏi lại nữa thì nút mở màn cài đặt quyền của app).
- Định vị của máy đang tắt: thông báo "Định vị đang tắt" kèm nút mở cài đặt định vị (`Settings.ACTION_LOCATION_SOURCE_SETTINGS`).
- Lấy vị trí quá 15 s không ra: báo "Chưa lấy được vị trí" kèm nút "Thử lại".

## 6. Dữ liệu
Không thêm collection, không đổi rules, không ghi gì lên Firestore. Toạ độ người dùng chỉ gửi tới server OSRM để tính đường (ghi vào báo cáo và vào mục Data safety khi đưa lên Play Store: vị trí được dùng trên máy và gửi tới dịch vụ chỉ đường bên thứ ba, không lưu).

## 7. Màn hình (xem mockup)

Một màn duy nhất `BranchMapFragment`, mở bằng `ScreenHost.openScreen()` từ ô Chi nhánh. Bản đồ chiếm toàn màn, nút back và nút "Vị trí của tôi" nổi trên bản đồ, phía dưới là **bottom sheet cố định** (`BottomSheetBehavior`, không có lớp phủ tối) với hai nấc: thu gọn và mở rộng.

| # | Trạng thái | Nội dung sheet |
|---|---|---|
| 1 | Gợi ý (mặc định) | Card gợi ý: tên chi nhánh, địa chỉ, quãng đường + phút đi bộ (số to, `tnum`), pill giờ mở cửa, dòng giải thích ở §5 bước 4 (nếu có), nút chữ "Xem tất cả chi nhánh". |
| 2 | Danh sách (sheet mở rộng) | RecyclerView các chi nhánh xếp theo khoảng cách: ô icon ghim (màu nhóm `cat_blue` như ô Chi nhánh trên trang chủ), tên, địa chỉ, khoảng cách bên phải. Chi nhánh được gợi ý ở đầu, có pill "Đi bộ ngắn nhất". Chạm một dòng thì chọn chi nhánh đó và thu sheet lại. |
| 3 | Đã chọn chi nhánh khác | Như #1 cho chi nhánh vừa chọn, kèm nút chữ "Quay lại gợi ý". Nút phụ "Gọi chi nhánh" (mở trình quay số, chỉ điền số). |
| 4 | Chưa có quyền / định vị tắt / chưa lấy được vị trí | Thông báo một câu + nút chính đen (Cho phép vị trí / Mở cài đặt định vị / Thử lại). Dưới đó là danh sách chi nhánh không khoảng cách. |
| 5 | Không tải được đường đi | Như #1, đường nét đứt trên bản đồ, chữ "khoảng cách đường thẳng", nút phụ "Thử lại". |
| — | Đang tính | Sheet hiện khung giữ chỗ (skeleton) của card gợi ý; không xoay giữa màn. |

**Ký hiệu trên bản đồ** (đây là ngoại lệ có chủ đích với các quy tắc màu của DESIGN.md, chỉ trong màn bản đồ):
- **Nền bản đồ**: ô OSM được giảm độ bão hoà (`ColorMatrix` trên `tilesOverlay`, còn khoảng 30% màu) để nền gần xám, hợp "Titanium Ledger" và làm đường đi nổi lên.
- Chấm người dùng: tròn **xanh dương** viền trắng kèm quầng mờ, app tự vẽ, theo quy ước của mọi app bản đồ.
- Ghim chi nhánh: tròn obsidian với icon ngân hàng trắng.
- Chi nhánh đang chọn/gợi ý: ghim lớn hơn, vòng **emerald** (đang tương tác).
- Đường đi bộ: nét **emerald** dày, viền trắng. Đường thẳng dự phòng: nét obsidian đứt.
- Nút nổi trên bản đồ (back, vị trí của tôi): ô trắng bo 14dp viền hairline như nút back trong header, không đổ bóng.

Mở màn thì bản đồ khung vừa người dùng + chi nhánh được gợi ý (chừa phần sheet thu gọn che).

## 8. Kiến trúc code **[Claude]**
- `domain/branch/`: `Branch` (data class), `Branches` (danh sách cố định), `GeoMath.haversineMeters`, `OpeningHours` (trạng thái theo giờ VN), `BranchRecommender` (bước 1–6 ở §5, nhận `WalkingRouter` qua constructor). Kotlin thuần, có unit test.
- `data/routing/`: interface `WalkingRouter { suspend fun route(from, to): WalkingRoute }` (`distanceMeters`, `durationSeconds`, danh sách điểm), implementation `OsrmWalkingRouter` (HTTP + parse GeoJSON, ném lỗi khi `code != "Ok"`). Hàm parse tách riêng để test bằng chuỗi JSON mẫu.
- `data/location/`: interface `LocationSource` + implementation fused, để ViewModel test được bằng fake.
- `FluxApp`: thêm `walkingRouter`, `locationSource` theo kiểu service locator đang có.
- `ui/customer/branch/`: `BranchMapFragment` + `BranchMapViewModel` (trạng thái của §7), adapter danh sách. Bản đồ là `MapView` của OSMDroid (Fragment gọi `onResume`/`onPause`/`onDetach`); ghim và chấm người dùng là `Marker` với icon vẽ bằng Canvas, đường là `Polyline`.
- `HomeFragment.openUtility`: thêm nhánh `Utility.CHI_NHANH -> BranchMapFragment()`.
- Manifest: thêm hai quyền vị trí. F7 (chatbot) có thể mở thẳng `BranchMapFragment()`.

## 9. Kịch bản demo (khoảng 1 phút)
1. Trang chủ → Chi nhánh → hộp thoại quyền vị trí → Cho phép.
2. Bản đồ hiện chấm người dùng, các ghim chi nhánh, đường đi bộ emerald tới chi nhánh gợi ý; card ghi "1,1 km · 14 phút đi bộ" và dòng giải thích "… gần hơn theo đường thẳng nhưng phải đi bộ …".
3. Kéo sheet lên → danh sách xếp theo khoảng cách → chạm chi nhánh khác → đường vẽ lại → "Quay lại gợi ý".
4. (Tuỳ chọn) Bật chế độ máy bay → "Vị trí của tôi" → đường thẳng nét đứt + "Thử lại".

## 10. Việc chủ dự án cần làm
1. Duyệt mockup + spec này.
2. Cho biết **nơi sẽ quay demo** nếu không phải TDTU, để đặt cụm chi nhánh demo quanh đó.
3. Chạy kịch bản §9 trên máy thật, ngoài trời hoặc gần cửa sổ nếu vị trí trong nhà lệch nhiều.

## 11. Kiểm thử
- Unit: `GeoMath` (khoảng cách đã biết, cùng điểm = 0), `OpeningHours` (trong giờ, sau giờ, T7 chiều, CN, theo giờ VN dù máy khác múi), `BranchRecommender` (chọn theo đường đi bộ chứ không theo đường thẳng; bỏ ứng viên lỗi; cả 3 lỗi thì dự phòng đường thẳng; > 20 km không gọi router; dòng giải thích chỉ có khi hai chi nhánh khác nhau), `OsrmWalkingRouter` parse (JSON thật đã thử, `code` lỗi, thiếu `routes`), `Branches` (id không trùng, toạ độ trong TP.HCM), `BranchMapViewModel` (từ chối quyền, định vị tắt, hết giờ chờ vị trí, gợi ý thành công, chọn chi nhánh khác, dùng lại đường đã lưu).
- `assembleDebug`, `lintDebug` (0 error), chạy trên Xiaomi theo §9.

## 12. Để builder tự quyết
- Tên, địa chỉ, toạ độ, số điện thoại cụ thể của 12 chi nhánh (giữ ràng buộc ở §5).
- Cách vẽ icon ghim (Canvas); icon ngân hàng lấy từ Phosphor (`bank`).
- Chiều cao nấc thu gọn của sheet và mức zoom.
- Bản cụ thể của `play-services-location` và `org.json` cho test.
