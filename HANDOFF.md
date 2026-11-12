# FLUX Bank — Handoff (cập nhật 2026-11-12, sau F6)

Tài liệu bàn giao để tiếp tục công việc ở session mới. Đọc file này trước, sau đó đọc `PROJECT_CONTEXT.md` (kiến trúc, design system, dữ liệu).

---

## 1. Bối cảnh

- **Đồ án**: Final Project môn Mobile Apps Development (TDTU). Đề: `docs/FinalProject_2026_2027.pdf`. Barem: `docs/Marking.xlsx`.
- **Hạn nộp: 2026-12-01.** Làm **một mình**. Cần nộp: app chạy được, đưa lên Google Play, báo cáo (class diagram, ERD, use case, kiến trúc...), video demo.
- **Barem tóm tắt** (10 điểm): Architecture 1 · Class diagram 1 · UI 1 · Database design 1 · User profiles 1 · Security 1 · Account mgmt 0.5 · Transaction 1 · Utilities 0.5 · Navigation 0.5 · Chatbot 0.5 · Hình thức báo cáo/demo 1.
- **Tech**: Android native, Kotlin, XML + ViewBinding (không Compose), MVVM, Firebase Auth + Firestore **gói Spark miễn phí**: không Storage, không Cloud Functions, không SMS thật. Chủ dự án muốn **tiết kiệm chi phí tối đa**.
- Firebase project: `banking-app-final-b5407`.
- Code ban đầu (Feature 1–2) do Google Antigravity viết. Phiên này đã làm lại toàn bộ giao diện và hoàn thiện Feature 1–2.

## 2. Trạng thái hiện tại

- Repo git ở `FLUX_Bank/`, remote `origin` = GitHub private `thedestoroyah/FLUX_Bank` (HTTPS, tạo 2026-11-12; SSH key chưa gắn với GitHub nên dùng HTTPS qua `gh`). **`main` có F1–F5; F6 nằm trên nhánh `feature/f6-navigation` (chưa merge, chờ key Maps để chạy thử phần bản đồ, mục 3)** (F4 merge 2026-10-28, commit `9f6e5c3`; F5 merge 2026-11-12, commit `9c3a311`, đã review toàn nhánh và chạy trên máy thật với rules đã publish, mục 3).
- **Unit test 340/340 pass** (trên nhánh F6), rules test 49/49, `assembleDebug` + `lintDebug` pass (0 error). F6 review toàn nhánh (2026-11-12): 0 Critical, 6 Important đã sửa có test (chạm chi nhánh lúc đang định vị lại vẽ đường từ vị trí cũ, test cache yếu, ở xa > 20 km bị báo lỗi mạng, lỗi API vị trí làm crash, định vị lại không có khung chờ, "Thử lại" bật GPS lại; thêm chặn lỗi camera khi xoay ngang). F5 review toàn nhánh: 0 Critical, 3 Important đã sửa có test (chiếm id hoá đơn của người khác, mã biên lai hoá đơn trùng, lỗi hạn mức hiện muộn). F4 đã qua review toàn nhánh (0 Critical; 6 lỗi Important đã sửa, có test).
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
| Feature 6 (nhánh `feature/f6-navigation`) | Ô Chi nhánh: Google Maps + bottom sheet, vị trí qua Fused Location, gợi ý chi nhánh có **quãng đi bộ ngắn nhất** (OSRM công cộng, so 3 chi nhánh gần nhất theo đường thẳng, có dòng giải thích khi chi nhánh gần nhất phải đi vòng), danh sách, chọn chi nhánh khác, dự phòng đường thẳng khi mất mạng, xử lý từ chối quyền / tắt định vị / hết giờ chờ. 12 chi nhánh giả lập, cụm demo quanh TDTU |
| Feature 5 | 7 ô tiện ích qua 3 kiểu màn: Hoá đơn (điện, nước; tra cứu giả lập, mỗi hoá đơn trả một lần, rules chặn trùng), Nạp ĐT (nhà mạng theo đầu số, 6 mệnh giá), Danh mục (vé máy bay, vé phim, khách sạn, mua sắm; mã đặt chỗ trên biên lai). Giao dịch loại `PAYMENT`, hạn mức ngày gộp với chuyển tiền. `DESIGN.md` rút từ code bằng skill impeccable |

### Chưa làm (các feature tiếp theo)
- **Feature 7** AI chatbot: trả lời kèm link tới các chức năng.
- Đưa lên Play Store, viết báo cáo, quay video demo.

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

Cho Feature 6 (nhánh `feature/f6-navigation`, chưa merge):
1. **Tạo key Maps** theo README mục 7: project Google Cloud **mới** (không phải project Firebase, nếu không Firebase tự lên Blaze), gắn thanh toán, bật chỉ Maps SDK for Android, khoá key theo `com.flux.bank` + SHA-1, thêm `maps.api.key` vào `local.properties`, kiểm Firebase vẫn Spark.
2. Đã chạy trên Xiaomi 2026-11-12 (chưa có key, nền bản đồ trống) — đạt: hộp thoại quyền, từ chối → khối "Chưa biết bạn đang ở đâu", cấp quyền sau → tự gợi ý; gợi ý thật từ vị trí nhà chủ dự án (FLUX Lê Văn Lương 3,8 km, có dòng "FLUX Phú Mỹ Hưng gần hơn theo đường thẳng (2,8 km) nhưng phải đi vòng, đi bộ 4,1 km"); danh sách xếp theo quãng đi bộ; chọn chi nhánh khác + "Quay lại gợi ý"; tắt mạng → đường thẳng + "Thử lại", bật mạng → đường đi bộ. **Chưa thử** (cần key): nền bản đồ, ghim, đường emerald, camera, chạm ghim; "Gọi chi nhánh"; xoay ngang (đã chặn lỗi `newLatLngBounds` bằng try/catch, cần xem trên máy).
3. Sau khi có key: chạy lại kịch bản trong README "Luồng demo" Feature 6 rồi merge nhánh.
- Mẹo adb cho F6: đừng `input swipe` từ mép dưới (trúng cử chỉ về màn hình chính của MIUI); kéo sheet từ giữa sheet. Đặt lại quyền vị trí: `pm revoke` + `pm clear-permission-flags … user-set user-fixed`.

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
- F6 (chủ dự án bỏ qua hỏi đáp, duyệt spec + mockup + plan 2026-11-12): **Google Maps SDK hiện bản đồ + OSRM công cộng tính đường đi bộ** (không dùng Routes API tính tiền); key Maps lấy từ project Google Cloud **riêng** để Firebase giữ Spark; chi nhánh giả lập trong app; không chỉ đường từng bước, không ATM, không tìm kiếm.
- F5 (chủ dự án bỏ qua hỏi đáp, duyệt toàn bộ spec + mockup + plan 2026-11-12): 3 kiểu màn cho 7 tiện ích, dữ liệu giả lập nằm trong app (không lưu Firestore), giao dịch loại `PAYMENT`, hạn mức ngày **gộp chung** với chuyển tiền, không chọn ghế / khứ hồi / giỏ hàng / hoàn tiền.
- **Không làm lại giao diện** (2026-11-12): giữ design system hiện có, chỉ dùng skill impeccable để làm mockup màn mới và soát (`critique`/`audit`) trước khi quay video. `DESIGN.md` là bản mô tả design system cho AI đọc.
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
| `docs/superpowers/specs/2026-11-12-navigation-f6-design.md`, `docs/superpowers/plans/2026-11-12-navigation-f6.md` | Spec + plan F6 (nhánh `feature/f6-navigation`) |
| `DESIGN.md`, `.impeccable/design.json` | Design system "The Titanium Ledger" rút từ code (token, quy tắc, component) — đọc trước khi làm mockup/màn mới |
| `docs/licenses/` | Giấy phép Inter (OFL), Phosphor (MIT), MobileFaceNet (MIT) |

### Bản đồ code (`app/src/main/java/com/flux/bank/`)
- `FluxApp.kt`: service locator, giữ `authRepository`, `userRepository`, `kycRepository`, `accountRepository`, `transactionRepository`, `ratesRepository`, `payeeRepository`, `vnPay`, `otpManager`, `otpNotifier`, `sessionManager`, `lockPolicy`, `appLockObserver`, `onboardingService`.
- `core/format/MoneyFormatter`, `core/validation/Validators`.
- `core/security/`: `OtpManager`, `OtpNotifier` (+ `OtpAutofillActivity`), `LockPolicy`, `AppLockObserver`.
- `data/model/`: `User`, `KycRecord`, `BankAccount` (+ `AccountType`), `Transaction` (+ `TransactionType`), `ServicePayment` (+ `DetailRow`), `Rates`, `Bank`, `Payee`.
- `data/repository/`: mỗi repository là một interface kèm implementation Firestore (`Auth`, `User` có `phoneIndex`, `Kyc`, `Account`, `Transaction`, `Rates`, `Payee`).
- `domain/`: `CustomerOnboardingService`, `FaceVerificationPolicy`, `InterestCalculator`, `TransferPolicy`, `QrPayload`. `domain/utility/` (F5): `Bills` (`BillLookup`), `Carriers`, `ServiceCatalog`, `BookingCode`, `ServicePayments`.
- `face/`: `FaceMatcher`, `BlinkDetector`, `FaceEmbedder` (LiteRT), `FaceDetectorHelper` (ML Kit). Model nằm ở `assets/mobilefacenet.tflite`.
- `ui/auth/`: Login, Unlock, ChangePassword, `RoleRouter`. `ui/auth/sheet/`: `TransactionAuthSheet`, `TransactionAuthViewModel`, `FaceStepController`.
- `ui/common/`: `ComingSoonFragment`, `ScreenHost`, `showKycPill`, `NumericKeypadView`, `OtpCellsView`, `SimulatedSmsBanner`, `Event`.
- `ui/customer/`: `CustomerMainActivity`, `Utility` (enum 12 tiện ích), `home/`, `profile/`, `account/` (chi tiết, nạp/rút, `VnPayFragment`, biên lai), `history/`, `transfer/` (hub, form 3 chế độ, `BankPickerSheet`), `qr/`, `utility/` (F5: `ServicePaymentViewModel`, `PaymentUi`, `BillPay*`, `TopUp*`, `Catalog*`).
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
- Còn 1 ô tiện ích mở `ComingSoonFragment`: Trợ lý AI (F7). Thay trong `HomeFragment.openUtility`. Chatbot có thể mở màn chi nhánh bằng `screenHost.openScreen(BranchMapFragment())`.
- Thanh toán dịch vụ (F5): `AccountRepository.pay(fromId, ServicePayment, txId?)` → giao dịch `PAYMENT`; màn mới có thể kế thừa `ServicePaymentViewModel` + dùng `PaymentUi`. Chatbot F7 có thể mở thẳng `BillPayFragment.newInstance(kind)` / `TopUpFragment()` / `CatalogFragment.newInstance(kind)`.
- Số dư chỉ đổi qua `AccountRepository.deposit/withdraw/transfer/transferExternal/pay`: mỗi lần một `runTransaction` ghi `accounts/{id}` (`balance`, `lastTxId`) và `transactions/{txId}` mới. Rules `ownerMoneyMoveAllowed` bắt `lastTxId` trỏ tới giao dịch **chưa tồn tại trước đó**, cùng tài khoản, `balanceAfter` = số dư mới, đúng phép cộng/trừ.
- F4 mở rộng hợp đồng: `TRANSFER_OUT` (chủ là người gửi) + `TRANSFER_IN` (chủ là người nhận) trỏ nhau qua `pairTxId`; rules `transferCreditAllowed`/`transferInBacked`. Loại giao dịch mới: thêm nhánh ở `TransactionRowAdapter.labelFor(tx)` và `ReceiptFragment.rowsFor`, thêm ca test vào `rules-test/rules.test.mjs` trước khi sửa rules.

### Dữ liệu Firestore
`users/{uid}` · `phoneIndex/{sđt}` → `{uid}` · `kyc/{uid}` (ảnh Base64 + `faceEmbedding`) · `accounts/{id}` (`ownerUid`, `type`, `accountNumber` `8868xxxxxxxx`, `balance` Long VND) · `transactions/{id}` (DEPOSIT/WITHDRAW/TRANSFER_OUT/TRANSFER_IN/PAYMENT) · `accountIndex/{số TK}` · `config/rates`. Chi tiết ở `PROJECT_CONTEXT.md`.

## 6. Môi trường & mẹo (quan trọng)

- **Build**: daemon Gradle đã ghim JDK 21 (`gradle/gradle-daemon-jvm.properties`), không cần `JAVA_HOME`: `./gradlew testDebugUnitTest assembleDebug`. Android Studio: Gradle JDK = `/usr/lib/jvm/java-21-openjdk`.
- **Test rules**: `rules-test/` (Node + Firestore emulator), xem README mục 5.
- **Firestore Console**: gõ tay tên field khi tạo document (copy từ markdown có thể dính ký tự ẩn U+200B, app sẽ không đọc được field).
- **Git/GitHub**: `git push origin main` (HTTPS qua `gh`, đã cấu hình). Không `--force`.
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

## 8. Giới hạn đã biết (ghi vào báo cáo)
- Gói Spark: OTP và đối chiếu khuôn mặt chỉ được kiểm tra trên thiết bị; rules chỉ giới hạn field nào được sửa. Hướng nâng cấp: Cloud Functions phát và kiểm token xác thực.
- `listCustomers` tải toàn bộ khách hàng rồi lọc ở client (shortcut, đủ cho quy mô đồ án).
- Giới hạn riêng từng feature (VNPay, hạn mức ngày chỉ kiểm ở app, dữ liệu tiện ích giả lập…) ở README mục "Giới hạn đã biết" và `PROJECT_CONTEXT.md`.

## 9. Cách bắt đầu session mới

Mở Claude Code từ thư mục `Final_Exam/` (để có skill impeccable), cắm điện thoại Xiaomi và mở khoá, rồi dùng câu lệnh mở đầu:
> Đọc `FLUX_Bank/HANDOFF.md` và `FLUX_Bank/PROJECT_CONTEXT.md`. Tiếp theo brainstorm Feature 7 (AI chatbot) bằng `/superpowers:brainstorming`.

Quy trình đã dùng và nên giữ: `superpowers:brainstorming` (hỏi từng câu, mockup HTML trên visual companion) → viết spec vào `docs/superpowers/specs/` → `superpowers:writing-plans` → `superpowers:executing-plans` (chủ dự án chọn chế độ **Native**) → review toàn nhánh → `finishing-a-development-branch`.

Còn **19 ngày** (tính từ 2026-11-12; F1–F5 trên `main`, F6 xong code trên nhánh, chờ key Maps để chạy thử rồi merge). Lịch đề xuất:
1. **Feature 6**: chủ dự án tạo key Maps (README mục 7), chạy thử phần bản đồ trên máy, merge.
2. **Feature 7 Chatbot** (khoảng 3 ngày, tới ~19/11). Quyết định: LLM API (ví dụ gói miễn phí của Gemini; không để key trong APK nếu được) hay luật cố định theo ý định. Đề yêu cầu trả lời **kèm link mở chức năng**: nạp ĐT, trả hoá đơn, xem giao dịch trong tháng… — mở thẳng `TopUpFragment()`, `BillPayFragment.newInstance(kind)`, tab Lịch sử.
3. **Play Store** — làm **sớm, song song**: tài khoản developer tốn 25 USD; tài khoản cá nhân mới thường phải chạy kiểm thử kín (closed testing) với nhiều người thử trong nhiều ngày trước khi được lên production, nên kiểm tra chính sách hiện hành ngay và tạo bản internal/closed testing sớm.
4. **Báo cáo + video** (từ ~20/11): class diagram, ERD, use case, kiến trúc; trước khi quay chạy `/impeccable critique` hoặc `audit` trên các màn chính và sửa lỗi nhìn thấy (giới hạn 1 ngày). Chừa 5–7 ngày cuối.
