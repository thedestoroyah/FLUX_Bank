# FLUX Bank — Handoff (cập nhật 2026-11-24 tối: app F1–F7, báo cáo (45/47 ảnh thật) và kịch bản video đều xong và đã push lên `main`; còn lại: 2 ảnh chi nhánh + quay video, cả hai chờ chủ dự án về TP.HCM; hạn nộp 2026-12-01)

Tài liệu bàn giao để tiếp tục công việc ở session mới. Đọc file này trước, sau đó đọc `PROJECT_CONTEXT.md` (kiến trúc, design system, dữ liệu).

---

## 1. Bối cảnh

- **Đồ án**: Final Project môn Mobile Apps Development (TDTU). Đề: `docs/FinalProject_2026_2027.pdf`. Barem: `docs/Marking.xlsx`.
- **Hạn nộp: 2026-12-01.** Làm **một mình**. Cần nộp: app chạy được, báo cáo (class diagram, ERD, use case, kiến trúc...), video demo. Google Play **tuỳ chọn, chỉ cộng điểm** (thầy trả lời 2026-11-13).
- **Barem tóm tắt** (10 điểm): Architecture 1 · Class diagram 1 · UI 1 · Database design 1 · User profiles 1 · Security 1 · Account mgmt 0.5 · Transaction 1 · Utilities 0.5 · Navigation 0.5 · Chatbot 0.5 · Hình thức báo cáo/demo 1.
- **Tech**: Android native, Kotlin, XML + ViewBinding (không Compose), MVVM, Firebase Auth + Firestore **gói Spark miễn phí**: không Storage, không Cloud Functions, không SMS thật. Chủ dự án muốn **tiết kiệm chi phí tối đa**.
- Firebase project: `banking-app-final-b5407`.
- Code ban đầu (Feature 1–2) do Google Antigravity viết. Phiên này đã làm lại toàn bộ giao diện và hoàn thiện Feature 1–2.

## 2. Trạng thái hiện tại

- Repo git ở `FLUX_Bank/`, remote `origin` = GitHub private `thedestoroyah/FLUX_Bank` (HTTPS, tạo 2026-11-12; SSH key chưa gắn với GitHub nên dùng HTTPS qua `gh`). **`main` có F1–F7**: F4 merge 2026-10-28 (`9f6e5c3`), F5 merge 2026-11-12 (`9c3a311`), F6 merge 2026-11-12 (`ac3de12`), F7 merge 2026-11-13 (`d3ad2a5`); tất cả đã review toàn nhánh và chạy trên máy thật (mục 3). Chuẩn bị Play merge 2026-11-13 (`694a74f`). **Mọi thứ chính ở nhánh `main`** (nhánh mặc định trên GitHub; nhánh `feat/ui-foundation-f1-f2`, `chore/play-release` đã gộp hết vào `main` và bị xoá). File HANDOFF này nằm ở `docs/HANDOFF.md` (trước đây ở gốc repo).
- **Chuẩn bị phát hành Google Play đã merge vào `main`** (2026-11-13, `694a74f`): ký app bằng khoá upload, App Check (AI Logic vẫn Unenforce), dòng chú thích demo ở màn đăng nhập, tài liệu + hình ảnh Play ở `docs/play/`. Review toàn nhánh bởi reviewer độc lập (0 Critical; 2 Important đã sửa), bản release cài tay chạy đạt trên Xiaomi. Chi tiết mục 3, "Cho bản phát hành Google Play".
- **F7 (Trợ lý AI) đã merge vào `main`** (2026-11-13): review toàn nhánh bởi reviewer độc lập (0 Critical, 4 Important + 2 Minor nâng lên Important, đã sửa hết có test), chạy trên Xiaomi với Gemini thật. Xem mục 3 và 7.
- **Unit test 391/391 pass** (sau merge Play, 2026-11-13), rules test 49/49 (F7 không đổi rules), `assembleDebug` + `lintDebug` pass (0 error). F6 review toàn nhánh (2026-11-12): 0 Critical, 6 Important đã sửa có test (chạm chi nhánh lúc đang định vị lại vẽ đường từ vị trí cũ, test cache yếu, ở xa > 20 km bị báo lỗi mạng, lỗi API vị trí làm crash, định vị lại không có khung chờ, "Thử lại" bật GPS lại; thêm chặn lỗi camera khi xoay ngang). F5 review toàn nhánh: 0 Critical, 3 Important đã sửa có test (chiếm id hoá đơn của người khác, mã biên lai hoá đơn trùng, lỗi hạn mức hiện muộn). F4 đã qua review toàn nhánh (0 Critical; 6 lỗi Important đã sửa, có test).
- **Báo cáo cuối kỳ**: bản dựng 118 trang, 69/69 test công cụ đạt, 45/47 ảnh UI thật đã commit (thiếu 2 ảnh chi nhánh), 9/11 lỗi nhỏ đã sửa; đã review toàn nhánh bởi reviewer độc lập. **`main` == `origin/main`** (chủ dự án đã push, lịch sử đã viết lại để bỏ file ghi chú Gemini). Chi tiết mục 10.
- **Kịch bản video demo** ở `docs/video/kich-ban-demo.md` (đã commit); chưa quay.
- F1–F2 và F3 đều đã được reviewer độc lập review toàn nhánh, đã sửa hết lỗi Important; lỗi Minor còn lại ở mục 7. F3 đã chạy demo đạt trên máy thật (mục 3).

### Đã hoàn thành
| Phần | Nội dung |
|---|---|
| Design system | Màu, thang chữ (Inter), khoảng cách, style `Widget.Flux.*`, icon Phosphor duotone/regular, chỉ dùng light theme, icon app FLUX |
| Khung app | `CustomerMainActivity` (5 tab có nút QR nổi ở giữa) · `OfficerMainActivity` (5 tab) · `ScreenHost.openScreen()` |
| Feature 1 | Nhân viên tạo / tìm / sửa khách (FirebaseApp phụ, có rollback, cấp tài khoản Checking 0 ₫, mật khẩu tạm), gửi email đặt lại mật khẩu, chụp eKYC hộ khách. Khách xem hồ sơ, tự sửa SĐT (qua OTP), địa chỉ, avatar. Bắt đổi mật khẩu ở lần đăng nhập đầu |
| Feature 2 | eKYC (ML Kit + MobileFaceNet embedding), nhân viên duyệt / từ chối có lý do. Khoá app sinh trắc học (cold start, khi app bị khôi phục, sau ≥ 60 s ở nền). OTP 6 số / 120 s / dùng 1 lần / gắn với giao dịch / sai 5 lần thì khoá, gửi qua SMS giả lập kèm banner dự phòng. `TransactionAuthSheet` (bàn phím riêng + bước quét mặt khi giao dịch ≥ 10.000.000 ₫). Security Rules |
| Feature 3 | Chi tiết riêng cho Checking/Saving/Mortgage, lãi suất chung + lãi dự kiến, tiền trả mỗi tháng / 2 tuần, nạp/rút qua OTP (+ mặt khi ≥ 10 tr) bằng Firestore transaction, biên lai, tab Lịch sử, tab Lãi suất và mở tài khoản cho nhân viên. Rules có test trên emulator (`rules-test/`, 20/20) |
| Feature 4 | Tab Chuyển tiền: trong FLUX (tra tên theo số TK qua `accountIndex`), giữa tài khoản của tôi, liên ngân hàng giả lập (10 ngân hàng), người nhận gần đây. Kiểm tra trước khi ghi (`TransferPolicy`: 10k–100tr/lần, 200tr/ngày giờ VN). Rules: cộng tiền cho người khác phải có TRANSFER_OUT cặp đã trừ nguồn cùng lần ghi, thông tin người gửi trên TRANSFER_IN không giả được (40 ca test). Nạp tiền qua VNPay sandbox (WebView, kiểm HMAC-SHA512). Nút QR giữa: quét / "Mã của tôi" |
| Feature 5 | 7 ô tiện ích qua 3 kiểu màn: Hoá đơn (điện, nước; tra cứu giả lập, mỗi hoá đơn trả một lần, rules chặn trùng), Nạp ĐT (nhà mạng theo đầu số, 6 mệnh giá), Danh mục (vé máy bay, vé phim, khách sạn, mua sắm; mã đặt chỗ trên biên lai). Giao dịch loại `PAYMENT`, hạn mức ngày gộp với chuyển tiền. `DESIGN.md` rút từ code bằng skill impeccable |
| Feature 6 | Ô Chi nhánh: bản đồ OpenStreetMap (OSMDroid) + bottom sheet, vị trí qua Fused Location, gợi ý chi nhánh có **quãng đi bộ ngắn nhất** (OSRM công cộng, so 3 chi nhánh gần nhất theo đường thẳng, có dòng giải thích khi chi nhánh gần nhất phải đi vòng), danh sách, chọn chi nhánh khác, dự phòng đường thẳng khi mất mạng, xử lý từ chối quyền / tắt định vị / hết giờ chờ. 12 chi nhánh giả lập, cụm demo quanh TDTU |
| Feature 7 | Ô Trợ lý AI: màn chat, Gemini qua Firebase AI Logic (gói Spark; model `gemini-3.5-flash-lite`, không gửi `thinkingConfig`), câu trả lời kèm 0–3 nút mở đúng màn F3–F6, số liệu chi tiêu thật của tháng, "Chế độ cơ bản" theo từ khoá khi mất mạng / hết lượt / quá 20 s, tin chứa OTP trả lời ngay trên máy (không gửi Google). Màn "Giao dịch tháng N" (Lịch sử lọc theo tháng, giờ VN) |

### Chưa làm
- **Báo cáo**: đã xong về nội dung và ảnh (45/47); còn 2 ảnh chi nhánh (chủ dự án tự chụp ở TP.HCM), rồi chủ dự án sửa tay lần cuối và xuất PDF (mục 10).
- **Video demo**: đề (Technical Requirements, mục 5) ghi "Provide a demo video to show all the nice features of your app", và mục 1 của HANDOFF này liệt kê video trong phần cần nộp. Kịch bản đã viết (`docs/video/kich-ban-demo.md`), chưa quay vì chủ dự án đang ở xa TP.HCM (đoạn chi nhánh cần ở đó); đừng coi video là tuỳ chọn.
- (Tuỳ chọn, cộng điểm) Tải AAB lên Closed testing trên Play Console — chủ dự án tự làm, đang chờ Google xác minh danh tính (mục 3).

## 3. Việc chủ dự án cần làm

Đã xong ngày 2026-10-27: publish rules F1–F2, chạy end-to-end F1–F2 trên máy thật, hiệu chỉnh `MATCH_THRESHOLD` (giữ 0.6), merge F1–F2 vào `main`.

Cho Feature 3 (đã xong 2026-10-27, nhánh đã merge vào `main`):
1. **Publish lại `firestore.rules`** (thêm `transactions`, `config/rates`, quyền đổi số dư có đối chiếu).
2. **Tạo composite index** `transactions` (`ownerUid` ASC, `createdAt` DESC) — README mục 3.
3. **Chạy demo F3 trên máy thật** — kết quả 2026-10-27 (Xiaomi, rules mới đã publish):
   - Đạt: nhân viên mở Tiết kiệm + khoản vay 1,5 tỷ/240 tháng (xem trước 13.017.349 / 6.003.662 ₫); tab Lãi suất đổi tiết kiệm 4,5 → 5% (ghi "Sửa bởi …"); khách nạp 2.000.000 ₫ vào Tiết kiệm qua OTP (mã hết hạn báo đúng, gửi lại mã được); rút quá số dư bị chặn trên form; rút 500.000 ₫ thành công; biên lai đúng; chi tiết Tiết kiệm hiện 5%, lãi +6.250 ₫/tháng cho 1,5 tr; chi tiết khoản vay đổi Hàng tháng / 2 tuần đúng số.
   - Lỗi tìm thấy khi chạy và đã sửa: thanh tab mất sau khi vuốt back (predictive back, có từ F1); bàn phím che sheet sửa lãi suất / lý do từ chối; xem trước số dư âm khi rút quá; lịch sử tải lỗi lại hiện "Chưa có giao dịch nào".
   - Sau khi tạo đúng index (createdAt **Descending**): tab Lịch sử (nhóm "Hôm nay", lọc theo tài khoản, mở biên lai) và "Giao dịch gần đây" đạt. Chủ dự án tự nạp 10.000.000 ₫ (OTP + quét mặt) thành công.

Cho Feature 4 (đã merge vào `main` 2026-10-28):
1. Đã xong 2026-10-28: publish rules F4, thêm `vnpay.tmn.code` / `vnpay.hash.secret` vào `local.properties`.
2. **Chạy end-to-end trên Xiaomi 2026-10-28 (rules thật trên Firebase) — đạt:**
   - Nhân viên tạo khách mới Tran Thi Binh (`binh@flux.com`, SĐT 0911222333, mật khẩu đã đổi thành `Binh2026a`); Bình đăng nhập lần đầu → `accountIndex` tự tạo (số TK 886810968242).
   - Customer nạp 1.000.000 ₫ qua VNPay sandbox (NCB test card, OTP 123456) → biên lai có mã VNPay, số dư 11.000.000 ₫. Đóng WebView giữa chừng → báo "đã huỷ", số dư không đổi, chip tài khoản còn đủ.
   - Chuyển 500.000 ₫ cho Bình theo số TK (tra ra "TRAN THI BINH") → OTP → biên lai; Bình thấy +500.000 ₫, lịch sử "Nhận từ NGUYEN CUSTOMER", biên lai nhận không có số dư.
   - Liên ngân hàng 2.000.000 ₫ sang Vietcombank; chuyển 1.000.000 ₫ Thanh toán → Tiết kiệm; "Người nhận gần đây", hạn mức còn lại, tab Lịch sử đều đúng.
   - QR: "Mã của tôi" hiện đúng cho cả hai khách; bấm qua lại nhanh không crash.
   - **Chưa thử trên máy**: quét QR bằng camera (cần hướng camera vào mã trên máy khác), bước quét mặt khi chuyển ≥ 10.000.000 ₫ (đã thử ở F3 cho nạp tiền), chặn hạn mức 200 tr/ngày (unit test có).
3. Đã merge vào `main` 2026-10-28 (sau merge: unit 216/216, rules 40/40, lint 0 error).
- Mẹo adb cho F4: bàn phím Telex đổi "ff"→"f" và "us"→"ú" (gõ thiếu rồi đưa con trỏ về chèn chữ còn thiếu); chỉ bấm BACK để ẩn bàn phím khi `dumpsys input_method` báo `mInputShown=true`, nếu không BACK sẽ đóng màn (đóng WebView VNPay = huỷ). Phím số của sheet OTP là view tự vẽ, phải chạm theo toạ độ.

Cho Feature 5 (đã merge vào `main` 2026-11-12):
1. Đã xong 2026-11-12: publish rules có `PAYMENT` và bản sau review (id `bill_<uid>_…` chỉ dùng cho `PAYMENT` của chính uid đó). Sau đó trả hoá đơn nước 357.500 ₫ trên máy vẫn đạt.
2. Chạy trên Xiaomi 2026-11-12 với rules thật — đạt: Mua sắm 1.490.000 ₫ (biên lai mã đặt chỗ), Tiền điện `PE01000123456` 2.434.700 ₫ qua OTP, tra lại thấy "đã thanh toán" + Xem biên lai, Lịch sử có icon dịch vụ, số dư khớp; lỗi số dư hiện ngay ở thanh đáy và khoá nút. **Chưa thử**: bước quét mặt khi ≥ 10 tr (thiếu số dư), hai máy trả cùng một hoá đơn cùng lúc.
- Mẹo adb cho F5: chạm vào ô mã khách hàng theo toạ độ của `etCode` (chạm theo chữ "Mã khách hàng" trúng nhãn, không mở bàn phím).

Cho Feature 6 (đã merge vào `main` 2026-11-12):
1. Không cần key hay billing: bản đồ OpenStreetMap (OSMDroid) + đường đi bộ OSRM công cộng. Đã thử Google Maps nhưng Google Cloud từ chối tạo tài khoản thanh toán (`OR_BACR2_59`), nên chuyển sang OSM cùng ngày.
2. Chạy trên Xiaomi 2026-11-12 — đạt:
   - Hộp thoại quyền; từ chối → khối "Chưa biết bạn đang ở đâu"; cấp quyền sau → tự gợi ý.
   - Gợi ý từ vị trí nhà chủ dự án: FLUX Lê Văn Lương 3,8 km / 52 phút, kèm dòng "FLUX Phú Mỹ Hưng gần hơn theo đường thẳng (2,8 km) nhưng phải đi vòng, đi bộ 4,1 km".
   - Nền OSM giảm màu + ghi công, ghim, chấm vị trí, đường emerald theo phố thật; camera khung đủ người dùng và chi nhánh, không bị sheet / nút vị trí che.
   - Danh sách xếp theo quãng đi bộ; chạm ghim hoặc dòng → chọn chi nhánh, vẽ lại đường; "Quay lại gợi ý"; "Gọi chi nhánh" mở trình quay số với số đúng.
   - Tắt mạng → đường thẳng nét đứt + "Thử lại"; bật mạng → đường đi bộ. Ép xoay ngang → màn giữ chiều dọc, không crash.
   - **Chưa thử**: đứng ở TDTU để thấy ca demo "FLUX Tân Phong 393 m nhưng đi bộ 1,3 km" (chỉ đo bằng OSRM, bảng ở KDoc `Branches`); máy không có Play services.
- Mẹo adb cho F6: đừng `input swipe` từ mép dưới (trúng cử chỉ về màn hình chính của MIUI); kéo sheet từ giữa sheet. Đặt lại quyền vị trí: `pm revoke` + `pm clear-permission-flags … user-set user-fixed`. Ép xoay: `settings put system accelerometer_rotation 0` + `user_rotation 1` (nhớ trả lại).

Cho Feature 7 (đã merge vào `main` 2026-11-13):
1. Đã xong: Console → AI Logic → Gemini Developer API (không thẻ). Luồng bật mới tự bắt buộc App Check → chủ dự án đã **Unenforce** App Check cho AI Logic (Console → App Check → APIs). Code App Check đã có trên `main`; thứ tự bật lại (chỉ cần nếu lên production) ở mục "Cho bản phát hành Google Play".
2. Chạy trên Xiaomi — đạt: chip "Nạp tiền điện thoại" → nút → màn Nạp ĐT → back còn chat; hỏi chi tiêu tháng (AI thật, ~6 s) → "4.282.200 ₫ qua 3 giao dịch" + nút → màn "Giao dịch tháng 11/2026" có tổng vào/ra khớp; "mã OTP của tôi là 482915" → nhắc không chia sẻ, không gửi lên AI; "chuyển tiền sang Vietcombank" → AI + nút → màn chọn kiểu chuyển → vuốt back còn chat; chế độ cơ bản khi AI lỗi (nhãn vàng); header/bàn phím không che nội dung.
3. Mẹo: lý do AI lỗi ghi ở Logcat tag `AssistantViewModel`. Gọi thử API không cần điện thoại: `curl -X POST "https://firebasevertexai.googleapis.com/v1beta/projects/banking-app-final-b5407/models/${M}:generateContent" -H "x-goog-api-key: <api_key trong google-services.json>" -d '{"contents":[...]}'` — trong zsh viết `${M}:generateContent` (viết `$M:g…` bị zsh hiểu thành modifier, ra lỗi 400 HTML). Free tier (đo 2026-11-13): `gemini-2.5-*` không còn cho người dùng mới; `gemini-3.8-flash` hay báo "high demand"; **`gemini-3.5-flash` chỉ 20 lượt/ngày cho cả project** (HTTP 429, hết thì chờ ~12 h) nên đã chuyển sang `gemini-3.5-flash-lite` (không nhận `thinkingConfig` → HTTP 400; trả lời 2–20 s). Đừng gọi thử API bừa bãi: mỗi lần gọi trừ quota của người thử.
4. **Chưa thử**: chế độ máy bay thật (đã thấy chế độ cơ bản khi API lỗi), hết quota ngày.
- Mẹo adb: helper chạm theo `resource-id` cần chạy bằng bash (zsh không tách từ khi `set -- $b`). Bản ở session 2026-11-13: `bash` script nhận resource-id, `uiautomator dump /dev/tty` → lọc `resource-id="com.flux.bank:id/<id>"` → chạm giữa `bounds`. Ví dụ id: `chipMonth`, `etMessage`, `btnBack`, `btnSend`.

Cho bản phát hành Google Play (đã merge vào `main` 2026-11-13, `694a74f`):
- Bối cảnh: tài khoản cá nhân mới phải **thử kín ≥ 12 người, liên tục 14 ngày** rồi mới xin được production; hạn 01/12 nên rất sát. Chủ dự án **đã gửi email hỏi thầy** có chấp nhận link thử kín (thêm Gmail thầy làm người thử) hoặc ảnh Play Console + APK thay cho production không. Bản nháp email ở trong lịch sử chat, không lưu file.
- **Thầy đã trả lời (2026-11-13):** triển khai Google Play là **tuỳ chọn, không bắt buộc** — không làm không bị trừ điểm, làm được thì **được cộng điểm**.
- Đã xong:
  1. Ký app: `signingConfigs.release` đọc 4 dòng `release.*` trong `local.properties`; thiếu thì chỉ `bundleRelease`/`assembleRelease` báo lỗi (README mục 9). **Keystore thật đã tạo** ở `~/keystores/flux-upload.jks` (alias `upload`, CN=PHAM HUYNH QUOC DAT), mật khẩu chỉ chủ dự án biết. `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab` (~55 MB, `jarsigner -verify` đạt). Đã nhắc chủ dự án sao lưu keystore + mật khẩu.
  2. App Check: `app/src/{debug,release}/java/com/flux/bank/AppCheckSetup.kt` (debug provider / Play Integrity), gọi ở `FluxApp.onCreate`; `releaseRuntimeClasspath` không có `appcheck-debug`. Trên Xiaomi bản debug in **debug token `ef229bcd-a901-41f5-bad0-ad5c06d4ba9f`** (Logcat tag `DebugAppCheckProvider`); chưa đăng ký nên Firebase trả 403 nhưng AI vẫn chạy vì AI Logic đang Unenforce.
  3. Màn đăng nhập thêm dòng "Ứng dụng demo cho đồ án môn học · tiền và giao dịch đều là giả lập" (đã xem trên máy bản release).
  4. `docs/play/`: `privacy-policy.md` (đã đăng **Gist công khai** https://gist.github.com/thedestoroyah/a48a98939a1fc13b32d8913c0d8711df, email liên hệ datpham1365@gmail.com, chủ dự án đồng ý), `play-console-answers.md` (điền theo thứ tự trong Console: App access, Ads, Content rating, Target audience, Financial features, Data safety, Store listing, Closed testing), `tester-invite.md` (tin mời người thử).
  5. `docs/play/graphics/`: `icon-512.png`, `feature-1024x500.png` (chủ dự án đã duyệt), `make_graphics.py` (dựng lại icon/ảnh nổi bật; `screenshots <thư mục>` cắt ảnh 1268×2756 về 1268×2536).
  6. Trợ lý đổi sang `gemini-3.5-flash-lite` (xem mục F7 ở trên), đã thử trên máy: AI trả lời + nút đúng.
- Tài khoản thử: **dùng lại khách thử và nhân viên thử sẵn có** (hồ sơ giả; ảnh eKYC không phải CCCD thật — chủ dự án xác nhận). Khách thử cho mục App access của Play (và người thử nếu có); nhân viên thử **chỉ gửi thầy**. Chủ dự án cần **đổi mật khẩu cả hai** trước khi gửi ai.
- **Phạm vi đã thu gọn (chủ dự án chọn 2026-11-13, sau khi thầy trả lời):** không gom 12 người thử, không đăng ký debug token, không Enforce App Check (AI Logic giữ Unenforce). Review toàn nhánh đã chạy (không có Critical); Data safety + chính sách đã sửa cho khớp (Gemini gói miễn phí tính "Shared").
- Đã xong 2026-11-13: 6 ảnh Play `docs/play/graphics/screenshot-01..06.png` (trang chủ, chuyển tiền, Nạp ĐT, lịch sử, bản đồ chi nhánh, Trợ lý; ảnh bản đồ chụp khi tắt quyền vị trí để không lộ vị trí nhà). Bản release (ký khoá upload) cài tay trên Xiaomi: mở không crash, dòng chú thích demo hiện, Trợ lý trả lời bằng AI + nút. Sau đó đã cài lại bản debug. **Quyền vị trí của FLUX Bank đang bị tắt** — cấp lại trong Cài đặt nếu cần bản đồ gợi ý chi nhánh.
- **Còn lại (tuỳ chọn, chủ dự án tự làm):**
  1. Sau khi Google xác minh danh tính: Play Console → tạo app → điền theo `docs/play/play-console-answers.md` → tải `app/build/outputs/bundle/release/app-release.aab` lên **Closed testing** → gửi thầy link/ảnh Console. Không cần đủ 12 người thử.
  2. Trước khi gửi duyệt: **cập nhật Gist chính sách** từ `docs/play/privacy-policy.md` (bản trên Gist chưa có câu "tên gọi gửi cho Gemini"); đổi mật khẩu khách thử trước khi đưa vào mục App access.
  3. Sửa code thì chạy lại `./gradlew bundleRelease`; tải bản mới lên Play thì tăng `versionCode`. Muốn lên production thì phải làm lại App Check (SHA-256 khoá ký app + khoá upload → Firebase App Check → Play Integrity → Enforce AI Logic). **Đừng đăng ký debug token ghi ở trên** (nó nằm trong repo).
- Mẹo cài APK qua adb trên Xiaomi: MIUI hiện hộp thoại "Install via USB" đếm ngược ~10 s; adb **không chạm được** hộp thoại này, chủ dự án phải tự bấm Install. Bản release và debug khác chữ ký → phải `adb uninstall com.flux.bank` trước (mất phiên đăng nhập). `pm revoke` quyền vị trí làm app bị đóng.
- Lỗi nhỏ từ review (để lại): `app/build.gradle.kts:68` `startsWith("packageRelease")` chặn cả `packageReleaseResources` / trình "Generate Signed Bundle" khi máy thiếu khoá; danh sách khoá thiếu tính 2 lần (dòng 39, 64); `make_graphics.py screenshots` không xoá `screenshot-0N.png` cũ và không che dữ liệu (chỉ soát bằng mắt).

## 4. Những điều chủ dự án đã quyết định (đừng hỏi lại)

- Giữ design system gốc (Apple Bento, nền trắng / `#F5F5F7`, chữ Obsidian `#0A0D12`) và bổ sung token. **Emerald `#10B981` chỉ dùng cho điểm tương tác**. Trang chủ có **dải mint `#E7F8EF` phía sau thẻ**. Icon **duotone**. Nút chính màu **đen**.
- Trang chủ hướng "A1": thẻ Titanium tỉ lệ thẻ thật 1.586:1, chiếm phần trên màn hình, **lưới tiện ích 4×3**, giao dịch gần đây. Thanh tab: **Trang chủ · Chuyển tiền · Quét QR (nổi giữa) · Lịch sử · Cá nhân**.
- Giao diện nhân viên cùng phong cách. Chữ hiển thị **tiếng Việt**, tiền **VND**.
- Xác thực giao dịch bằng **bottom sheet** có nền tối, bàn phím số riêng.
- OTP gửi bằng **thông báo giả lập SMS** (không dùng Blaze).
- Xác thực khuôn mặt **mức A**: ML Kit kiểm tra chớp mắt + MobileFaceNet so với ảnh eKYC.
- Mọi màn hình mới phải được **duyệt mockup trước khi code giao diện**. Chủ dự án thích xem mockup HTML so sánh trên trình duyệt.
- Nếu thiếu thời gian, cắt theo thứ tự: skeleton/haptic → nhân viên chụp eKYC hộ → kiểm tra chớp mắt. Không cắt phần đối chiếu khuôn mặt.
- F4 (chủ dự án duyệt toàn bộ spec + mockup 2026-10-27): tra người nhận **chỉ theo số tài khoản** (không theo SĐT); liên ngân hàng giả lập; nạp tiền **chỉ qua VNPay** (không Stripe); hạn mức 100 tr/lần, 200 tr/ngày; QR dạng `fluxbank:<số TK>`.
- F6 (chủ dự án bỏ qua hỏi đáp, duyệt spec + mockup + plan 2026-11-12): **OpenStreetMap (OSMDroid) hiện bản đồ + OSRM công cộng tính đường đi bộ**, không key, không billing (đã thử Google Maps nhưng Google Cloud từ chối tạo tài khoản thanh toán `OR_BACR2_59`, chủ dự án chọn chuyển sang OSM); chi nhánh giả lập trong app; không chỉ đường từng bước, không ATM, không tìm kiếm.
- F5 (chủ dự án bỏ qua hỏi đáp, duyệt toàn bộ spec + mockup + plan 2026-11-12): 3 kiểu màn cho 7 tiện ích, dữ liệu giả lập nằm trong app (không lưu Firestore), giao dịch loại `PAYMENT`, hạn mức ngày **gộp chung** với chuyển tiền, không chọn ghế / khứ hồi / giỏ hàng / hoàn tiền.
- **Không làm lại giao diện** (2026-11-12): giữ design system hiện có, chỉ dùng skill impeccable để làm mockup màn mới và soát (`critique`/`audit`) trước khi quay video. `DESIGN.md` là bản mô tả design system cho AI đọc.
- F7 (chủ dự án bỏ qua hỏi đáp, duyệt spec + mockup + plan 2026-11-12, chọn thực thi native): Gemini qua Firebase AI Logic (gói Spark), bot chỉ mở màn (không tự giao dịch, không điền form), không lưu chat, chế độ cơ bản trên máy; App Check tạm Unenforce, chỉ bật lại nếu lên production.
- Phát hành Play (chủ dự án bỏ qua hỏi đáp, duyệt spec + plan 2026-11-13, chọn native): tên trên Play **"FLUX Bank (Demo)"**, dòng chú thích demo ở màn đăng nhập, Play App Signing + khoá upload ngoài repo, App Check chỉ cho AI Logic, chính sách bảo mật song ngữ trên Gist công khai, Financial features khai *Money transfer* + *Mobile payments* (giả lập), đối tượng 18+, không quảng cáo, dùng lại tài khoản thử sẵn có.
- **Google Play là tuỳ chọn** (thầy trả lời 2026-11-13: không làm không bị trừ, làm được thì cộng điểm). Chủ dự án chọn **thu gọn**: không gom 12 người thử, không Enforce App Check; chỉ tự tải Closed testing nếu kịp. Ưu tiên báo cáo + video.
- **Báo cáo đã bắt đầu ngày 2026-11-23** (chủ dự án chọn: Claude viết trọn văn bản, dựng bằng Markdown + script, công cụ AI nói thẳng ở mục 1.6, không phụ lục cài đặt/tài khoản demo). Video demo làm sau khi xong báo cáo (kịch bản đã có, còn quay).
- **Quyết định ngày 2026-11-24** (đừng hỏi lại): chủ dự án tự sửa tay file Word lần cuối, tự xuất PDF, tự sửa số trang mục lục; để trống ngày ký; giữ nguyên các mục "chưa chắc" trong báo cáo; **không cắt độ dài báo cáo** (~18.700 từ; mục tiêu 14.000–17.000 do Claude tự đặt, đề không giới hạn); bỏ ảnh email đặt lại mật khẩu (gửi email thật) và ảnh khách tự nộp eKYC; giữ ảnh có mặt chủ dự án dưới dạng avatar thay mặt (bản gốc đã xoá); để nguyên dữ liệu Han và ảnh mặt trên Firebase; tự làm file `.txt` (link GitHub + link Drive) và tự chụp 2 ảnh chi nhánh khi ở TP.HCM; kịch bản video dùng khách Han làm nhân vật chính.
- **Chạy thử trên điện thoại thật của chủ dự án** (Xiaomi cắm adb), không dùng emulator. Nếu máy đang khoá (`isKeyguardShowing=true`) thì nhờ chủ dự án mở khoá.

## 5. Tài liệu & file quan trọng

| File | Nội dung |
|---|---|
| `PROJECT_CONTEXT.md` | Tổng quan kiến trúc, design system, dữ liệu, giới hạn đã biết |
| `README.md` | Build, deploy rules, tạo officer đầu tiên, luồng demo |
| `firestore.rules` | Security Rules (deploy thủ công qua Console) |
| `docs/superpowers/specs/2026-10-26-ui-foundation-f1-f2-design.md` | Thiết kế đợt 1, đã duyệt |
| `docs/superpowers/plans/2026-10-26-ui-foundation-f1-f2.md` | Kế hoạch 17 task, đã thực hiện xong |
| `docs/superpowers/specs/mockups/*.html` | Mockup đã duyệt (mở bằng trình duyệt) |
| `docs/superpowers/specs/2026-10-27-account-management-f3-design.md`, `docs/superpowers/plans/2026-10-27-account-management-f3.md` | Spec + plan F3 (đã merge) |
| `docs/superpowers/specs/2026-10-27-transfers-f4-design.md`, `docs/superpowers/plans/2026-10-27-transfers-f4.md` | Spec + plan F4 (đã merge) |
| `docs/superpowers/specs/2026-11-12-utilities-f5-design.md`, `docs/superpowers/plans/2026-11-12-utilities-f5.md` | Spec + plan F5 (đã merge) |
| `docs/superpowers/specs/2026-11-12-navigation-f6-design.md`, `docs/superpowers/plans/2026-11-12-navigation-f6.md` | Spec + plan F6 (đã merge) |
| `docs/superpowers/specs/2026-11-12-assistant-f7-design.md`, `docs/superpowers/plans/2026-11-12-assistant-f7.md` | Spec (có §11 ghi chú khi làm) + plan F7 |
| `docs/superpowers/specs/2026-11-13-play-release-design.md`, `docs/superpowers/plans/2026-11-13-play-release.md` | Spec + plan chuẩn bị phát hành Play (đã merge) |
| `docs/report/` | Báo cáo cuối kỳ: `src/*.md`, `diagrams/*.puml`, `ui-shots.md`, `build.py`, `check.py`, `tools/`, `tests/` (mục 10) |
| `docs/superpowers/specs/2026-11-23-final-report-design.md`, `docs/superpowers/plans/2026-11-23-final-report.md` | Spec + plan báo cáo (đã thực hiện) |
| `docs/video/kich-ban-demo.md` | Kịch bản video demo: checklist chuẩn bị, 8 đoạn (làm gì / lời bình), thứ tự quay, lệnh ghép ffmpeg, rủi ro |
| `docs/play/` | Chính sách bảo mật, câu trả lời Play Console, tin mời người thử, hình ảnh trang Play + script dựng |
| `DESIGN.md`, `.impeccable/design.json` | Design system "The Titanium Ledger" rút từ code (token, quy tắc, component) — đọc trước khi làm mockup/màn mới |
| `docs/licenses/` | Giấy phép Inter (OFL), Phosphor (MIT), MobileFaceNet (MIT), ZXing (Apache-2.0), OSMDroid (Apache-2.0) + dữ liệu OpenStreetMap (ODbL) |

### Bản đồ code (`app/src/main/java/com/flux/bank/`)
- `FluxApp.kt`: service locator, gọi `installAppCheck(this)` (bản riêng ở `src/debug` và `src/release`), giữ `assistantClient` (F7), `authRepository`, `userRepository`, `kycRepository`, `accountRepository`, `transactionRepository`, `ratesRepository`, `payeeRepository`, `vnPay`, `otpManager`, `otpNotifier`, `sessionManager`, `lockPolicy`, `appLockObserver`, `onboardingService`, `walkingRouter`, `branchRecommender`, `locationSource` (F6).
- `core/format/MoneyFormatter`, `DistanceFormatter` (F6), `core/validation/Validators`.
- `core/security/`: `OtpManager`, `OtpNotifier` (+ `OtpAutofillActivity`), `LockPolicy`, `AppLockObserver`.
- `data/model/`: `User`, `KycRecord`, `BankAccount` (+ `AccountType`), `Transaction` (+ `TransactionType`), `ServicePayment` (+ `DetailRow`), `Rates`, `Bank`, `Payee`.
- `data/repository/`: mỗi repository là một interface kèm implementation Firestore (`Auth`, `User` có `phoneIndex`, `Kyc`, `Account`, `Transaction`, `Rates`, `Payee`).
- `domain/`: `CustomerOnboardingService`, `FaceVerificationPolicy`, `InterestCalculator`, `TransferPolicy`, `QrPayload`. `domain/utility/` (F5): `Bills` (`BillLookup`), `Carriers`, `ServiceCatalog`, `BookingCode`, `ServicePayments`. `domain/branch/` (F6): `Branch`/`GeoPoint`, `Branches` (12 chi nhánh giả lập), `GeoMath`, `OpeningHours`, `BranchRecommender`.
- `data/routing/` (F6): `WalkingRouter`, `OsrmWalkingRouter`. `data/location/` (F6): `LocationSource`, `FusedLocationSource`.
- `face/`: `FaceMatcher`, `BlinkDetector`, `FaceEmbedder` (LiteRT), `FaceDetectorHelper` (ML Kit). Model nằm ở `assets/mobilefacenet.tflite`.
- `ui/auth/`: Login, Unlock, ChangePassword, `RoleRouter`. `ui/auth/sheet/`: `TransactionAuthSheet`, `TransactionAuthViewModel`, `FaceStepController`.
- `ui/common/`: `ComingSoonFragment`, `ScreenHost`, `showKycPill`, `NumericKeypadView`, `OtpCellsView`, `SimulatedSmsBanner`, `Event`.
- `ui/customer/`: `CustomerMainActivity`, `Utility` (enum 12 tiện ích), `home/`, `profile/`, `account/` (chi tiết, nạp/rút, `VnPayFragment`, biên lai), `history/`, `transfer/` (hub, form 3 chế độ, `BankPickerSheet`), `qr/`, `utility/` (F5: `ServicePaymentViewModel`, `PaymentUi`, `BillPay*`, `TopUp*`, `Catalog*`), `branch/` (F6: `BranchMapFragment` OSMDroid, `BranchMapViewModel`, `BranchAdapter`, `BranchMarkers`).
- `payment/VnPay`, `core/format/TextNormalizer`. `ui/ekyc/`: `EkycCaptureFragment`. `ui/officer/`: `overview/`, `customers/`, `ekyc/`, `rates/`.
- Test: `app/src/test/...`. Repository giả nằm ở `fakes/Fakes.kt`, `testutil/MainDispatcherRule`.

### API để các feature sau dùng lại
```kotlin
// Xác thực mọi giao dịch (OTP; amount >= 10_000_000 thì thêm bước quét mặt)
TransactionAuthSheet.show(childFragmentManager, AuthSummary(title, listOf("Người nhận" to "..."), amount = 2_000_000L), "req_key")
childFragmentManager.setFragmentResultListener("req_key", viewLifecycleOwner) { _, b ->
    val ok = b.getBoolean(TransactionAuthSheet.KEY_VERIFIED)
}
// Mở màn phụ (ẩn thanh tab, có nút back)
screenHost.openScreen(SomeFragment())
// Tra người nhận theo SĐT
FluxApp.instance.userRepository.findUidByPhone("0912345678")
```
- Mở màn của ô tiện ích: `UtilityScreens.screenFor(utility, savingAccountId)` (trang chủ và Trợ lý dùng chung). Thêm nút cho Trợ lý: thêm giá trị vào `AssistantAction`, dòng mô tả trong `AssistantPrompt.ACTION_GUIDE`, nhãn trong `AssistantActionUi.LABELS`, từ khoá trong `AssistantFallback.KEYWORDS`.
- Thanh toán dịch vụ (F5): `AccountRepository.pay(fromId, ServicePayment, txId?)` → giao dịch `PAYMENT`; màn mới có thể kế thừa `ServicePaymentViewModel` + dùng `PaymentUi`. Chatbot F7 có thể mở thẳng `BillPayFragment.newInstance(kind)` / `TopUpFragment()` / `CatalogFragment.newInstance(kind)`.
- Số dư chỉ đổi qua `AccountRepository.deposit/withdraw/transfer/transferExternal/pay`: mỗi lần một `runTransaction` ghi `accounts/{id}` (`balance`, `lastTxId`) và `transactions/{txId}` mới. Rules `ownerMoneyMoveAllowed` bắt `lastTxId` trỏ tới giao dịch **chưa tồn tại trước đó**, cùng tài khoản, `balanceAfter` = số dư mới, đúng phép cộng/trừ.
- F4 mở rộng hợp đồng: `TRANSFER_OUT` (chủ là người gửi) + `TRANSFER_IN` (chủ là người nhận) trỏ nhau qua `pairTxId`; rules `transferCreditAllowed`/`transferInBacked`. Loại giao dịch mới: thêm nhánh ở `TransactionRowAdapter.labelFor(tx)` và `ReceiptFragment.rowsFor`, thêm ca test vào `rules-test/rules.test.mjs` trước khi sửa rules.

### Dữ liệu Firestore
`users/{uid}` · `phoneIndex/{sđt}` → `{uid}` · `kyc/{uid}` (ảnh Base64 + `faceEmbedding`) · `accounts/{id}` (`ownerUid`, `type`, `accountNumber` `8868xxxxxxxx`, `balance` Long VND) · `transactions/{id}` (DEPOSIT/WITHDRAW/TRANSFER_OUT/TRANSFER_IN/PAYMENT) · `accountIndex/{số TK}` · `config/rates`. Chi tiết ở `PROJECT_CONTEXT.md`.

## 6. Môi trường & mẹo (quan trọng)

- **graphify**: đồ thị tri thức của dự án đặt ở `Final_Exam/graphify-out/` (ngoài thư mục `FLUX_Bank/`, theo yêu cầu chủ dự án; đừng tạo `graphify-out/` bên trong `FLUX_Bank/`).
- **Máy 2026-11-23**: không có Node (chạy test rules bằng `bun`: `cd rules-test && JAVA_HOME=/usr/lib/jvm/java-21-openjdk bun node_modules/firebase-tools/lib/bin/firebase.js emulators:exec --only firestore --project demo-flux "bun test rules.test.mjs"`, ngoài sandbox); có pandoc, graphviz, ffmpeg, obs, ONLYOFFICE; không có LibreOffice; `scrcpy` đã cài 2026-11-24. `java-21-openjdk` và `java-25-openjdk` cùng cài (25 là mặc định); khi build báo thiếu toolchain 21 thì đó là JDK 21 bị gỡ.
- **Build**: daemon Gradle đã ghim JDK 21 (`gradle/gradle-daemon-jvm.properties`), không cần `JAVA_HOME`: `./gradlew testDebugUnitTest assembleDebug`. Android Studio: Gradle JDK = `/usr/lib/jvm/java-21-openjdk`.
- **Test rules**: `rules-test/` (Node + Firestore emulator), xem README mục 5.
- **Firestore Console**: gõ tay tên field khi tạo document (copy từ markdown có thể dính ký tự ẩn U+200B, app sẽ không đọc được field).
- **Git/GitHub**: `git push origin main` (HTTPS qua `gh`, đã cấu hình; cần chạy ngoài sandbox). Không `--force`, trừ lần dọn lịch sử 2026-11-24 (xoá file ghi chú Gemini khỏi mọi commit, chủ dự án tự push).
- **Điện thoại nối qua bộ chia USB EPZ TP35 Pro** (cắm thẳng vào laptop bằng cáp khác từng báo `unable to enumerate USB device` trong `journalctl -k`, tức cáp chỉ sạc / tiếp xúc kém). Không thấy máy thì xem `lsusb` và `journalctl -k --since "-10 min" | grep -i usb` trước khi đổ lỗi cho adb.
- **Keystore / mật khẩu**: không bao giờ đọc, in hay commit mật khẩu trong `local.properties`; kiểm chữ ký bằng `jarsigner -verify -verbose:summary -certs app/build/outputs/bundle/release/app-release.aab`.
- **adb phải chạy ngoài sandbox của Claude Code**: server adb khởi động trong sandbox không mở được USB nên `adb devices` trống dù máy đã cắm (`lsusb` vẫn thấy Xiaomi). Sửa: `adb kill-server && adb start-server` ngoài sandbox.
- **Skill thiết kế**: `impeccable` cài ở `Final_Exam/.claude/skills/` nên phải mở Claude Code từ thư mục `Final_Exam` (không phải `FLUX_Bank`) thì mới có. Bộ `taste` chỉ dành cho video, không dùng cho app.
- **Emulator** (chỉ khi không có điện thoại): AVD `Pixel_7` (Android 37, image production nên không có adb root). Chạy không cửa sổ ở chế độ background:
  ```bash
  ~/Android/Sdk/emulator/emulator -avd Pixel_7 -no-window -no-audio -no-boot-anim -no-snapshot-save -gpu swiftshader_indirect
  ~/Android/Sdk/platform-tools/adb install -r -g app/build/outputs/apk/debug/app-debug.apk
  adb exec-out screencap -p > shot.png
  ```
- **Chụp màn hình khi không có tài khoản đăng nhập**: tạo tạm `app/src/debug/AndroidManifest.xml` để export `CustomerMainActivity`/`OfficerMainActivity` (`tools:replace="android:exported"`), rồi `adb shell am start -n com.flux.bank/.ui.customer.CustomerMainActivity`. Nếu cần dữ liệu thì tạm nhét dữ liệu mẫu vào ViewModel. **Luôn revert và xoá trước khi commit** (`git grep '"demo"'` để kiểm tra).
- **Điều khiển điện thoại qua adb** (Xiaomi, MIUI): máy có mã khoá, cần chủ dự án mở và bật Stay awake. Bàn phím Telex có thể đổi "us" thành "ú" khi `adb shell input text` (gõ "cuss…" để ra "cus…"). Chạm theo chữ bằng `uiautomator dump` thì **lọc `package="com.flux.bank"`**, nếu không sẽ chạm nhầm bong bóng chat Zalo nổi trên màn hình. MIUI bỏ qua phím BACK giả lập khi app bật predictive back; dùng nút back trên header hoặc `input swipe` từ mép trái.
- **Mẹo điều khiển Xiaomi (màn 1268×2756)** đúc kết từ F5:
  - Chạm theo chữ: `uiautomator dump` → lọc node `package="com.flux.bank"` có `text`/`content-desc` khớp → `input tap` giữa `bounds`. Node có thể thiếu thuộc tính `text`, nhớ xử lý.
  - Ô nhập: chạm theo `resource-id` của `EditText` (ví dụ `etCode`), chạm theo chữ hint sẽ trúng nhãn và không mở bàn phím.
  - Lấy OTP giả lập: `adb shell dumpsys notification --noredact | grep -o "Mã OTP của bạn là [0-9 ]*"`.
  - Bàn phím số của sheet OTP (view tự vẽ): cột x = 246 / 634 / 1020, hàng y = 1808 / 1991 / 2174, số 0 ở (634, 2357), nút Xác nhận (634, 2568).
  - Ngay sau khi mở app (cold start) lần chạm đầu có thể bị nuốt: chờ khoảng 8 s hoặc chạm lại.
  - Header back: (128, 234).
- **Mẹo adb học thêm 2026-11-24 (chụp màn hình/quay video):**
  - **Thanh trạng thái sạch**: `adb shell settings put global sysui_demo_allowed 1`, rồi `am broadcast -a com.android.systemui.demo -e command enter` kèm `-e command battery -e level 100 -e plugged false` và `-e command notifications -e visible false`. Thoát: `-e command exit`. Đồng hồ không đổi được (luôn 8:16).
  - **Bàn phím Telex** (Gboard đang Tiếng Việt): `adb shell input keyevent 204` (KEYCODE_LANGUAGE_SWITCH) đổi sang English trước khi gõ email/mật khẩu; gõ có dấu cách dùng `%s`; tên "Gia" bị tự đổi thành "GIA".
  - **Màn hình hay tự khoá** khi tắt stay-on hoặc chờ lâu (kể cả chờ 60 s để thử khoá sinh trắc học): ảnh chụp ra đen thui; cần chủ dự án mở khoá lại. `adb shell svc power stayon true` chỉ có tác dụng khi đang cắm cáp.
  - **Máy bị xoay ngang** khi chủ dự án chạm vào: đặt `settings put system accelerometer_rotation 0` và `user_rotation 0` trước khi chụp; ảnh ngang có kích thước 2756×1268.
  - **Chờ animation**: màn mới cần 2,5–3 s trước khi chụp, nếu không ảnh còn lớp mờ của màn cũ. Sau khi gõ vào ô, kiểm tra IME hiện bằng `dumpsys input_method | grep mInputShown` rồi mới bấm Back (Back thừa sẽ thoát màn).
  - **Banner OTP (heads-up)** chỉ hiện khi Không làm phiền tắt (`cmd notification set_dnd off`, sau đó `set_dnd priority`); khi chụp banner, các thông báo cá nhân khác cũng có thể nhảy ra nên kiểm ảnh trước khi commit, và không bao giờ chụp thanh thông báo kéo xuống.
  - **Đọc OTP tự động**: `dumpsys notification --noredact | grep 'android.text=String (FLUX: Mã OTP'`.
  - **Quyền**: `pm revoke com.flux.bank <quyền>` làm app bị đóng; `pm grant` cũng vậy. Camera preview của màn quét QR đen nếu máy úp xuống bàn.
  - **Lưu mật khẩu Google**: sau khi đăng nhập có hộp thoại autofill "No thanks/Save", chọn `autofill_save_no`.
  - **Vị trí giả bằng adb** (`cmd location providers add-test-provider`, `appops set com.android.shell android:mock_location allow`) được hệ thống nhận nhưng FLUX không dùng (Fused Location). Khi chụp bản đồ ở xa chi nhánh, màn hiện khoảng cách thật (vd. "95,5 km") và lộ vị trí nhà.
  - **Suất phim**: các suất đã qua giờ bị vô hiệu; chỉ suất còn lại chọn được.
- Không commit các file `.idea/*` do IDE tự sửa. Dùng `git add app docs ...` thay vì `git add -A`.
- Quy ước: comment và KDoc viết tiếng Việt, tên trong code bằng tiếng Anh. Commit theo dạng `feat:` / `fix:` / `chore:` / `docs:` / `refactor:` / `test:`. Làm theo TDD (viết test fail trước).

## 7. Lỗi nhỏ để lại sau (từ review)
### Feature 1–2
1. OTP của giao dịch khác hoặc mã cũ sau khi gửi lại chỉ báo chung là "mã không đúng" (chưa có thông báo riêng).
2. Điều kiện quét mặt (đã eKYC chưa, có embedding chưa) chỉ được kiểm tra sau khi người dùng đã nhập OTP.
3. Màn mở khoá có thể chồng 2 lần nếu xuống nền ≥ 60 s ngay tại màn mở khoá.
4. Nhân viên sửa khách: chuyển SĐT và cập nhật các field khác không nguyên tử; snackbar "Đã lưu" hiện lặp lại (`saved` không reset).
5. Lỗi khi rollback `deleteCreatedAuthUser()` bị bỏ qua.
6. Ghi `mustChangePassword = false` thất bại thì lần đăng nhập sau lại bị bắt đổi mật khẩu.
7. Rules chưa chặn khách tự sửa `phoneNumber` mà không chuyển `phoneIndex` (chỉ client tuân thủ).
8. Bấm "Lưu" 2 lần nhanh có thể mở 2 sheet OTP.

### Lỗi nhỏ của Feature 3 (review 2026-10-27, để lại sau)
9. Lưu lãi suất thất bại: lỗi chỉ hiện ở `RatesFragment` (bị sheet che), trong sheet không thấy gì.
10. `MoneyMoveFragment.chipIds` sống lâu hơn view: quay lại từ màn eKYC thì chip tài khoản không được tạo lại (reset trong `onDestroyView`).
11. Process death ở màn nạp/rút: `load()` bị bỏ qua khi có `savedInstanceState`; kết quả sheet xác thực sau khôi phục bị bỏ qua.
12. Bấm back khi đang ghi giao dịch thì mất biên lai (số dư vẫn đúng).
13. Rules chưa kiểm `createdAt`/`accountNumber`/key thừa của `transactions` (khách tự đặt giờ cho giao dịch của mình được).
14. eKYC VERIFIED chỉ kiểm ở app, chưa kiểm trong rules nạp/rút.
15. Snackbar "tải lỗi" ở chi tiết tài khoản / lịch sử lặp lại mỗi lần render (dùng state thay vì `Event`).
16. `installment()` dùng `ceil` trên số thực, có thể cao hơn 1 ₫ khi chia hết.
17. Vài kích thước `dp` cứng trong layout mới (96dp, 64dp, 36dp, 56dp).

**Feature 4** đã đổi nạp tiền sang VNPay; rules vẫn không biết VNPay đã thu tiền (giới hạn Spark, ghi ở README).

### Lỗi nhỏ của Feature 4 (review 2026-10-28, để lại sau)
18. VNPay chuyển về khi app đang ở nền: `VnPayFragment` gọi `popBackStack()` sau `onSaveInstanceState` có thể crash (hiếm, chỉ khi redirect lúc app không ở trước).
19. Trang chủ chờ `ensureIndexed` xong mới tải giao dịch gần đây (lần đầu mỗi phiên chậm hơn); nên chạy trong `launch` riêng.
20. Tra người nhận lỗi mạng thì phải gõ lại số tài khoản mới tra lại được.

### Lỗi nhỏ của Feature 5 (review 2026-11-12, để lại sau)
21. Process death ở các màn tiện ích: `load()` bị bỏ qua khi có `savedInstanceState` (giống #11).
22. Chạm lại suất chiếu đang chọn thì chip bỏ check nhưng ViewModel vẫn giữ suất đó (thiếu `selectionRequired`).
23. Không kiểm lại suất chiếu / ngày bay lúc bấm thanh toán nếu để màn mở qua giờ.
24. Chạm đúp ô ngày có thể mở hai date picker (`show` → `showNow`).
25. Giờ "đã thanh toán lúc" theo múi giờ máy, không cố định giờ VN.
26. Chưa có test cho nhánh đọc lại sau lỗi của `FirestoreAccountRepository.pay` (đã thử bằng tay trên máy).

### Lỗi nhỏ của Feature 6 (review 2026-11-12, để lại sau)
27. Bấm ra ngoài hộp thoại quyền (không chọn gì) bị coi là "chặn hẳn": nút dẫn sang cài đặt app thay vì hỏi lại.
28. `ACTION_DIAL` / mở trang bản quyền OSM trên máy không có app xử lý sẽ crash (`ActivityNotFoundException`).
29. Chưa có vị trí mà chạm chi nhánh thì chỉ phóng to ghim (không có card, camera không dời tới chi nhánh đó).
30. Chọn chi nhánh khác xong, dòng của nó trong danh sách vẫn ghi khoảng cách đường thẳng.
31. `DistanceFormatter`: 999,5–999,99 m in ra "1000 m".
32. Timeout OSRM không cứng 8 s (connect và read tính riêng, chưa tính DNS).

### Lỗi nhỏ của Feature 7 (review 2026-11-13, để lại sau)
33. `savingId` chỉ có sau khi đọc lịch sử thành công: lỗi đọc lịch sử thì nút Tiết kiệm mở màn "chưa có tài khoản tiết kiệm".
34. Bóc code fence trong parser phân biệt hoa/thường và phải đứng đầu chuỗi (ít gặp vì có responseSchema).
35. Câu hỏi gửi trước khi tải xong tên khách thì ngữ cảnh ghi tên "bạn".
36. Khách chưa có tài khoản nào: ngữ cảnh ghi "Tài khoản: .".
37. Giờ mở cửa chi nhánh trong prompt viết thẳng, chưa lấy từ `OpeningHours`.
38. Hiệu ứng ba chấm chỉ dừng khi view bị recycle; tin trả lời mới chưa có `accessibilityLiveRegion` cho TalkBack.
39. `rvChat.adapter` không gỡ trong `onDestroyView` (giống `HistoryFragment`).
40. Màn chọn kiểu chuyển mở từ Trợ lý không có nút back trên header (vuốt back vẫn được).

## 8. Giới hạn đã biết (ghi vào báo cáo)
- Gói Spark: OTP và đối chiếu khuôn mặt chỉ được kiểm tra trên thiết bị; rules chỉ giới hạn field nào được sửa. Hướng nâng cấp: Cloud Functions phát và kiểm token xác thực.
- `listCustomers` tải toàn bộ khách hàng rồi lọc ở client (shortcut, đủ cho quy mô đồ án).
- Giới hạn riêng từng feature (VNPay, hạn mức ngày chỉ kiểm ở app, dữ liệu tiện ích giả lập…) ở README mục "Giới hạn đã biết" và `PROJECT_CONTEXT.md`.

## 9. Cách bắt đầu session mới

Mở Claude Code từ thư mục `Final_Exam/` (để có skill impeccable), cắm điện thoại Xiaomi (qua bộ chia EPZ) và mở khoá, rồi dùng câu lệnh mở đầu:
> Đọc `FLUX_Bank/docs/HANDOFF.md` (mục 9 và 10) và `FLUX_Bank/PROJECT_CONTEXT.md`. Tiếp tục đồ án FLUX Bank theo mục 9: quay video demo theo `docs/video/kich-ban-demo.md` và chụp 2 ảnh chi nhánh nếu tôi đã ở TP.HCM.

**Hạn nộp 2026-12-01.** Việc đã xong ngày 2026-11-24: app, 45/47 ảnh UI, báo cáo (chủ dự án đã đọc, sẽ tự sửa tay và xuất PDF lần cuối), kịch bản video, mục "Bài nộp" trong báo cáo, push `main`. **Việc còn lại, theo thứ tự:**
1. **Hai ảnh chi nhánh** `cus-branch-suggest` và `cus-branch-list` (chủ dự án tự chụp khi ở TP.HCM, cạnh cổng TDTU, 19 Nguyễn Hữu Thọ, Q.7; ảnh không được lộ vị trí nhà, nên chụp ở đó chứ không chụp ở nhà). Chụp xong đặt vào `docs/report/images/ui/<id>.png`, `git add -f`, dựng lại báo cáo. Vị trí giả bằng adb **không dùng được** (app dùng Fused Location của Google Play services nên bỏ qua); không đi được TP.HCM thì quay bằng emulator có Play services và nói rõ trong báo cáo.
2. **Quay video demo** (đề bắt buộc, Technical Requirements mục 5): theo `docs/video/kich-ban-demo.md` (~9,5 phút, bảy yêu cầu của đề, khách Han là nhân vật chính, hồ sơ eKYC của Han đang "Chờ duyệt" để duyệt ngay trong video). `scrcpy` đã cài (2026-11-24). Thu lời bình riêng rồi ghép bằng ffmpeg (lệnh trong kịch bản). Quota Gemini miễn phí có hạn: chỉ gọi Trợ lý khi quay.
3. **Nộp**: chủ dự án tự làm file `.txt` chứa link GitHub và link Google Drive (video), nộp kèm báo cáo PDF. Báo cáo đã ghi nội dung này ở mục "Bài nộp" (chương 1). Kho GitHub đang private: cấp quyền cho thầy hoặc đổi public. Sau khi có video, kiểm tra lại dòng video ở bảng 1.1 và dòng "Hình thức báo cáo, demo" ở bảng tự chấm 6.8.
4. (Tuỳ chọn) Play Closed testing do chủ dự án tự làm (mục 3); sau đó cập nhật mục "Trạng thái Google Play" ở chương 6 báo cáo.

**Trạng thái điện thoại và dữ liệu thử (2026-11-24, cuối ngày):** đã dọn các thiết lập adb (tắt chế độ demo thanh trạng thái, tắt stay-on, bật lại xoay tự động, thu hồi mock location). Chủ dự án còn tự: tắt công tắc sinh trắc học của nhân viên Lê Thị Cúc nếu không cần, thu hồi quyền camera của app (đang cấp), đặt lại bàn phím Gboard về tiếng Việt nếu muốn (đang English). Không làm phiền đang bật, app đang đăng nhập khách An. Tài khoản thử (mật khẩu chủ dự án giữ, không ghi trong repo): `customer@flux.com` (Nguyễn Văn An, số TK 886851678506), `officer@flux.com` (Lê Thị Cúc), `han@flux.com` (Pham Thu Han, eKYC đang chờ duyệt, ảnh mặt là của chủ dự án; để cho video). Hồ sơ eKYC và ảnh mặt của Han nằm trên Firebase; chủ dự án chọn để nguyên.

Quy trình đã dùng và nên giữ: `superpowers:brainstorming` → spec trong `docs/superpowers/specs/` → `superpowers:writing-plans` → `superpowers:executing-plans` (chủ dự án chọn **Native**) → review toàn nhánh → `finishing-a-development-branch`. Trả lời chủ dự án bằng tiếng Việt, gọi giảng viên là "thầy".

## 10. Báo cáo cuối kỳ (trạng thái 2026-11-24 tối, đã push lên `main`)

- **Spec/plan**: `docs/superpowers/specs/2026-11-23-final-report-design.md`, `docs/superpowers/plans/2026-11-23-final-report.md`. Mẫu Khoa: `docs/report/template/BAO_CAO.template-goc.docx` (bìa: một sinh viên 52300098, người hướng dẫn ThS. Võ Văn Thành — thầy, nam).
- **Nguồn**: `docs/report/src/` (`front.md` + `01…07-*.md` + `08-tai-lieu.md`), sơ đồ PlantUML `docs/report/diagrams/*.puml`, danh sách ảnh UI `docs/report/ui-shots.md`.
- **Dựng** (từ `FLUX_Bank/`, cần JDK, graphviz, pandoc): `uv run --with python-docx==1.1.2 --with docxcompose==1.4.0 --with "setuptools<80" --with pillow python docs/report/build.py` → `docs/report/out/FLUX_Bank_BaoCao.docx` + `.pdf` (xuất bằng ONLYOFFICE `x2t`, cùng bộ dàn trang với ONLYOFFICE Desktop). Build **tự điền sẵn** mục lục, danh mục hình, danh mục bảng kèm số trang thật (lặp: điền → xuất PDF → đọc số trang, ~30 giây); các trường vẫn giữ lệnh nên làm mới được. ONLYOFFICE **không có phím F9** cập nhật trường: muốn làm mới thì bấm vào danh mục rồi dùng nút cập nhật hoặc chuột phải → làm mới. `--quick` bỏ bước điền (nhanh hơn, danh mục còn trống). Soát nội dung/chính tả: `docs/report/check.py --spell`. Test công cụ: `cd docs/report && uv run --with python-docx==1.1.2 --with docxcompose==1.4.0 --with "setuptools<80" --with pillow --with pytest python -m pytest tests`.
- **Ảnh UI**: `docs/report/images/ui/<id>.png` (47 id trong `ui-shots.md`; 45 ảnh thật đã commit; **thiếu `cus-branch-suggest`, `cus-branch-list`**, build tự tạo ảnh giữ chỗ cho hai ảnh này). Chụp bằng `adb exec-out screencap -p`, chạy adb ngoài sandbox, máy mở khoá. Ảnh mới `git add -f` (thư mục `images/ui/` bị gitignore vì chứa ảnh sinh tự động). Ảnh có mặt chủ dự án (`off-ekyc-capture`, `off-ekyc-review`, `cus-auth-face`) đã được chủ dự án thay mặt bằng avatar; **đừng ghi đè bằng bản gốc**.
- **Việc còn lại cho báo cáo**: (1) chụp 2 ảnh chi nhánh rồi dựng lại; (2) chủ dự án tự sửa tay lần cuối trong ONLYOFFICE (danh mục đã điền sẵn; tự cập nhật số trang) rồi xuất PDF; (3) sau khi có video, rà dòng video ở bảng 1.1 và bảng tự chấm 6.8; (4) ngày ký ở trang cảm ơn và cam đoan để trống (chủ dự án chọn để nguyên); (5) cập nhật "Trạng thái Google Play" ở chương 6 nếu có thay đổi sau 13/11.
- **JDK 21** phải còn trên máy để build app (dự án ghim toolchain 21); chỉ gỡ sau khi nộp xong.
- **Lỗi nhỏ của báo cáo** (review 2026-11-24): đã sửa ngày 2026-11-24 các mục sau: soát nội dung chạy lại sau khi dựng docx; hàng tiêu đề bảng dài lặp lại và dính với hàng sau (pandoc đã đặt `tblHeader` sẵn); tài liệu tham khảo thụt dòng treo; "mỗi người dùng có ≥ 1 tài khoản" chỉ áp cho khách (văn bản, ERD, class-model); nét liền cho `CustomerOnboardingService` và `AppLockObserver → AuthRepository`; ghi các trường id lưu trùng (`uid`, `accountId`, `id`); chương 2 (nguồn phiên bản, ngôi thứ nhất, App Check chưa bắt buộc); chương 7 (hạn mức Gemini 20 lượt thuộc `gemini-3.5-flash`); chương 6 (bản release cũng đã cài, dòng F1/F2 không liệt kê quá chi tiết). **Chưa sửa, chủ dự án chọn bỏ qua:** (1) chưa có ảnh khách tự nộp eKYC và email đặt lại mật khẩu của nhân viên (đã thêm ảnh vé phim, mua sắm, hoá đơn nước, chuyển giữa tài khoản mình ngày 2026-11-24; ảnh đặt lại mật khẩu sẽ gửi email thật nên bỏ); (2) độ dài ~18.600 từ (mục tiêu 14.000–17.000, tính cả bảng); (3) `front._reset_field` chỉ đọc mảnh đầu của lệnh trường TOC; `check.py` giữ một bản debug token App Check (đã có trong HANDOFF, đừng đăng ký token này).
- **Chưa chắc / cần chủ dự án xác nhận trong báo cáo**: mục 1.6 (công cụ AI) viết rằng chủ dự án "đọc, kiểm chứng nội dung báo cáo" và rằng các lần chạy thử trên máy phần lớn do Claude Code điều khiển qua adb còn chủ dự án mở khoá và thao tác ở bước cần khuôn mặt; trạng thái Google Play (chương 6) chốt theo ngày 13/11; bảng tự chấm điểm 6.8 (tổng 9,6/10) là đề xuất. Chủ dự án đã nói giữ nguyên các mục này.
