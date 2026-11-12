# FLUX BANK - ANDROID NATIVE PROJECT
- Package: com.flux.bank
- Language: Kotlin (XML layouts + ViewBinding, không dùng Compose)
- Architecture: MVVM — Activity/Fragment → ViewModel → Repository (interface + Firestore impl). Service locator đơn giản trong `FluxApp`, không dùng DI library.
- Backend: Firebase Authentication + Cloud Firestore, gói **Spark (miễn phí)** — không Storage, không Cloud Functions.
- Bản đồ (F6): Google Maps SDK for Android với key từ một project Google Cloud **riêng** (không phải project Firebase, để Firebase giữ gói Spark); đường đi bộ từ server OSRM công cộng (miễn phí, không key).
- Thiết kế & kế hoạch: spec từng feature trong `docs/superpowers/specs/` (F1–F2, F3, F4, F5, F6), plan tương ứng trong `docs/superpowers/plans/`, mockup đã duyệt trong `docs/superpowers/specs/mockups/`.
- Design system dạng tài liệu cho AI: `DESIGN.md` (+ `.impeccable/design.json`), rút từ `res/values/` ngày 2026-11-12. Code trong `res/values/` vẫn là nguồn gốc.

## Design system (bắt buộc cho mọi màn hình mới)
- Chỉ giao diện sáng. Nền `#F5F5F7`, bề mặt trắng, chữ Obsidian `#0A0D12`.
- Emerald `#10B981` chỉ cho điểm tương tác (nút QR giữa thanh tab, tab đang chọn, tiền vào, ô OTP đang nhập, viền focus). Nút chính màu đen Obsidian.
- Trang chủ: dải mint `#E7F8EF` phía sau thẻ Titanium (tỉ lệ thẻ thật 1.586:1, bóng xanh mềm).
- Icon: Phosphor (MIT) — duotone cho tiện ích/tính năng (`cat_*` màu + nền `cat_*_bg`), regular cho chrome. Font Inter (OFL). License trong `docs/licenses/`.
- Chỉ dùng token: màu `colors.xml`, khoảng cách `dimens.xml` (`space_4…space_32`), chữ `TextAppearance.Flux.*` (≥ 12sp, số tiền dùng `Amount` có `tnum`), style `Widget.Flux.*` (Card, Button.Primary/Secondary/Destructive/Text, TextField, IconTile, StatusPill, SectionLabel, FilterChip, BottomNav, BottomSheet). Không viết cứng chữ hay mã màu trong layout.
- Tile: elevation 0 + viền 1dp `#E5E7EB`. Danh sách dùng RecyclerView. Vùng chạm ≥ 48dp. Toàn bộ chữ tiếng Việt trong `strings.xml`, tiền VND `48.250.000 ₫`.

## Khung điều hướng
- Khách hàng: `CustomerMainActivity` — Trang chủ · Chuyển tiền · [Quét QR nổi] · Lịch sử · Cá nhân.
- Nhân viên: `OfficerMainActivity` — Tổng quan · Khách hàng · eKYC · Lãi suất · Cá nhân.
- Màn phụ mở bằng `ScreenHost.openScreen(fragment)` (back stack, ẩn thanh tab). Tính năng chưa làm dùng `ComingSoonFragment`.

## Dữ liệu Firestore
| Collection | Ghi chú |
|---|---|
| `users/{uid}` | hồ sơ, `role`, `kycStatus`, `kycRejectReason`, `biometricEnabled`, `mustChangePassword` |
| `phoneIndex/{sđt}` | `{uid}` — SĐT duy nhất (rules chặn trùng) + tra người nhận cho chuyển tiền |
| `kyc/{uid}` | ảnh CCCD/selfie Base64 (≤ 1024px), `faceEmbedding` (MobileFaceNet 192 chiều), `submittedBy`, `reviewedBy` |
| `accounts/{id}` | `ownerUid`, `type` CHECKING/SAVING/MORTGAGE, `accountNumber` 8868xxxxxxxx, `balance` Long VND (Mortgage: dư nợ), `lastTxId`; Mortgage thêm `loanAmount`, `annualRate` (%/năm, cố định), `termMonths`, `startDate` |
| `transactions/{id}` | `accountId`, `ownerUid`, `accountNumber`, `type` DEPOSIT/WITHDRAW/TRANSFER_OUT/TRANSFER_IN/PAYMENT, `amount` > 0, `balanceAfter` (null ở TRANSFER_IN), `description`, `counterpartyName/Account/Bank` (`FLUX` nếu cùng ngân hàng), `pairTxId` (nối OUT↔IN), `paymentRef` (mã VNPay; với PAYMENT là khoá hoá đơn hoặc mã đặt chỗ), `service` (PAYMENT: tên enum `Utility`), `details` (PAYMENT: các dòng biên lai), `createdAt` — chỉ tạo, không sửa/xoá. Index (ownerUid ASC, createdAt DESC) |
| `accountIndex/{số TK}` | `accountId`, `ownerUid`, `type`, `holderName` (= `users.fullName`) — danh bạ tra người nhận; chủ tài khoản tự ghi khi mở trang chủ, chỉ `get`, không `list` |
| `config/rates` | `savingRate`, `mortgageRate` (%/năm), `updatedAt`, `updatedBy`, `updatedByName` — nhân viên sửa |

Security Rules: `firestore.rules` (deploy thủ công qua Console — xem README).

## Đã hoàn thành
### Feature 1 — Hồ sơ người dùng
- Nhân viên tạo khách (`CustomerOnboardingService`: kiểm tra định dạng + trùng SĐT/CCCD, tạo user qua FirebaseApp phụ nên nhân viên không bị đăng xuất, rollback khi lỗi giữa chừng, cấp sẵn tài khoản Checking 0 ₫, mật khẩu tạm + bắt đổi lần đầu).
- Nhân viên tìm (không dấu), xem, sửa khách; gửi email đặt lại mật khẩu; chụp eKYC hộ.
- Khách xem hồ sơ, tự sửa SĐT (qua OTP), địa chỉ, ảnh đại diện.

### Feature 2 — Bảo mật
- eKYC: chụp độ phân giải đầy đủ, ML Kit kiểm tra đúng 1 khuôn mặt, tính embedding; nhân viên duyệt/từ chối kèm lý do.
- Khoá app bằng sinh trắc học: khi mở app và khi quay lại sau ≥ 60 giây ở nền (`LockPolicy`, `AppLockObserver`, `UnlockActivity`).
- OTP: 6 số, 120 giây, dùng một lần, gắn với một giao dịch, sai 5 lần thì khoá (`OtpManager`); gửi bằng thông báo "SMS giả lập", chạm để tự điền; không có quyền thông báo thì hiện banner trong app.
- `TransactionAuthSheet`: bottom sheet xác thực dùng chung (bàn phím số riêng). Giao dịch ≥ 10.000.000 ₫ thêm bước khuôn mặt: chớp mắt (ML Kit) + MobileFaceNet so cosine với embedding eKYC (ngưỡng `FaceMatcher.MATCH_THRESHOLD`).
- Các feature sau chỉ cần gọi `TransactionAuthSheet.show(fm, AuthSummary(title, rows, amount), requestKey)` và lắng nghe `KEY_VERIFIED`.

### Feature 3 — Quản lý tài khoản
- Spec/plan: `docs/superpowers/specs/2026-10-27-account-management-f3-design.md`, `docs/superpowers/plans/2026-10-27-account-management-f3.md`, mockup `docs/superpowers/specs/mockups/2026-10-27-f3-accounts.html`.
- Chi tiết riêng cho từng loại (`ui/customer/account/AccountDetailFragment`): Checking/Saving có số dư, nạp/rút, Saving có lãi suất chung + lãi dự kiến/tháng; Mortgage có dư nợ, tiền trả mỗi kỳ (hàng tháng / 2 tuần), kỳ tiếp theo, tổng lãi. Công thức trong `domain/InterestCalculator`.
- Nạp/rút (`MoneyMoveFragment`): qua `TransactionAuthSheet`, chỉ khách VERIFIED, 10.000 ₫ ≤ số tiền (nạp ≤ 100.000.000 ₫), nguồn tiền giả lập. `AccountRepository.deposit/withdraw` chạy một Firestore transaction ghi `balance` + `lastTxId` và `transactions/{id}`; rules đối chiếu hai document. Biên lai `ReceiptFragment`.
- Tab Lịch sử (`ui/customer/history/`), "Giao dịch gần đây" ở trang chủ, ô Nạp/Rút/Tiết kiệm.
- Nhân viên: tab Lãi suất (`ui/officer/rates/`), mở Saving/Mortgage trong chi tiết khách (`OpenAccountFragment`).
- Test rules trên emulator: `rules-test/` (README mục 5).

### Feature 4 — Chuyển tiền & VNPay
- Spec/plan: `docs/superpowers/specs/2026-10-27-transfers-f4-design.md`, `docs/superpowers/plans/2026-10-27-transfers-f4.md`, mockup `docs/superpowers/specs/mockups/2026-10-27-f4-transfers.html`.
- Tab Chuyển tiền (`ui/customer/transfer/`): trong FLUX (tra tên qua `PayeeRepository`/`accountIndex`), giữa tài khoản của tôi, liên ngân hàng (giả lập, `Banks`), người nhận gần đây suy ra từ lịch sử.
- Kiểm tra trước khi ghi: `domain/TransferPolicy` (10.000 ₫ – 100.000.000 ₫/lần, 200.000.000 ₫/ngày theo giờ Việt Nam, số dư, định dạng số TK). Xác thực qua `TransactionAuthSheet`.
- `AccountRepository.transfer` ghi 4 document trong một Firestore transaction (đích cộng bằng `increment`); rules `transferCreditAllowed` bắt cộng tiền phải có OUT cặp đã trừ nguồn trong cùng lần ghi.
- Nạp tiền qua VNPay sandbox (`payment/VnPay`, `VnPayFragment` WebView), khoá trong `local.properties`. `vnp_TxnRef` = id giao dịch nên không cộng hai lần.
- Nút QR giữa thanh tab (`ui/customer/qr/QrFragment`): quét `fluxbank:<số TK>` (ML Kit) hoặc hiện "Mã của tôi" (ZXing).

### Feature 5 — Tiện ích
- Spec/plan: `docs/superpowers/specs/2026-11-12-utilities-f5-design.md`, `docs/superpowers/plans/2026-11-12-utilities-f5.md`, mockup `docs/superpowers/specs/mockups/2026-11-12-f5-utilities.html`. Design system rút từ code: `DESIGN.md` (+ `.impeccable/design.json`).
- Ba kiểu màn cho 7 ô (`ui/customer/utility/`): `BillPayFragment` (tiền điện, nước), `TopUpFragment` (nạp ĐT), `CatalogFragment` (vé máy bay, vé phim, khách sạn, mua sắm). Phần chung: `ServicePaymentViewModel` (nguồn tiền, hạn mức, eKYC, mở sheet một lần, lỗi) và `PaymentUi` (chip nguồn, sheet xác thực, mở biên lai).
- Dữ liệu giả lập thuần Kotlin trong `domain/utility/`: `BillLookup` (hoá đơn cố định theo nhà cung cấp + mã + kỳ; mã đuôi `0000` = không tìm thấy), `Carriers` (nhà mạng theo đầu số, 6 mệnh giá), `ServiceCatalog`, `BookingCode`, `ServicePayments` (dựng `ServicePayment`).
- Ghi tiền: `AccountRepository.pay` dùng lại `debit()` (một Firestore transaction, loại `PAYMENT`). Hoá đơn dùng id cố định `bill_<uid>_<nhà cung cấp>_<mã>_<yyyyMM>` nên rules chặn trả lần hai (`AlreadyPaidException`); màn hoá đơn tra `TransactionRepository.findByPaymentRef` để hiện "đã thanh toán".
- Hạn mức ngày `TransferPolicy.sentToday` gộp `TRANSFER_OUT` và `PAYMENT`. Lịch sử/biên lai lấy icon và màu từ `Utility.fromService`.

### Feature 6 — Chi nhánh và đường đi bộ
- Spec/plan: `docs/superpowers/specs/2026-11-12-navigation-f6-design.md`, `docs/superpowers/plans/2026-11-12-navigation-f6.md`, mockup `docs/superpowers/specs/mockups/2026-11-12-f6-navigation.html`.
- Ô "Chi nhánh" mở `ui/customer/branch/BranchMapFragment`: Google Maps (`SupportMapFragment`, style giảm màu `res/raw/map_style.json`) + bottom sheet cố định (gợi ý / thông báo thiếu vị trí / danh sách). Không đụng Firestore, không đổi rules.
- `domain/branch/`: `Branches` (12 chi nhánh giả lập, cụm demo quanh cổng TDTU, bảng đo trong KDoc), `GeoMath` (haversine), `OpeningHours` (T2–T6 8–17h, T7 8–12h, theo giờ VN), `BranchRecommender` (3 chi nhánh gần nhất theo đường thẳng → gọi OSRM song song → chọn quãng đi bộ ngắn nhất; lỗi hết thì dự phòng đường thẳng; > 20 km không gọi).
- `data/routing/OsrmWalkingRouter` (`routing.openstreetmap.de/routed-foot`, HttpURLConnection + org.json, timeout 8 s), `data/location/FusedLocationSource` (Fused Location, chờ tối đa 15 s). `BranchMapViewModel` lưu đường đã tính theo chi nhánh, bỏ kết quả về muộn (chạm nhanh hai chi nhánh, định vị lại).
- Key Maps: `local.properties` → `maps.api.key` → `manifestPlaceholders`; thiếu key thì nền bản đồ trống nhưng màn vẫn chạy. Hướng dẫn tạo key ở README mục 7.
- `BottomSheetBehavior` không hỗ trợ `topMargin` (cộng dồn vào vị trí thu gọn khi layout lại): giới hạn chiều cao bằng `maxHeight`.

## Giới hạn đã biết
- Gói Spark: OTP và đối chiếu khuôn mặt được kiểm tra trên thiết bị; rules chỉ giới hạn field được sửa. Hướng nâng cấp: Cloud Functions phát/kiểm token.
- `FaceMatcher.MATCH_THRESHOLD = 0.6`, đã đo trên máy thật ngày 2026-10-27 (số đo ở KDoc).
- Feature 4: hạn mức ngày chỉ kiểm ở app; chữ ký VNPay kiểm trên máy (khoá sandbox nằm trong APK) và rules không biết VNPay đã thu tiền; số TK chỉ tra được sau khi chủ đăng nhập lần đầu; chuyển liên ngân hàng là giả lập.
- Feature 5: dữ liệu tiện ích là giả lập trong app; rules không biết số tiền hoá đơn/giá vé đúng bao nhiêu (chỉ kiểm một lần trừ tiền hợp lệ ≤ 100 tr).
- Feature 3: lãi chỉ là dự kiến (không cộng vào số dư); nguồn tiền nạp giả lập nên khách tự nạp được, nhưng mỗi lần có bản ghi và tối đa 100 triệu; lịch sử lọc ở client trên 100 giao dịch mới nhất.
- Feature 6: chi nhánh giả lập trong app; OSRM công cộng không cam kết luôn chạy (lỗi thì gợi ý theo đường thẳng); chỉ so 3 chi nhánh gần nhất theo đường thẳng; toạ độ người dùng gửi tới server chỉ đường bên thứ ba (khai báo Data safety khi lên Play).

## Kiểm thử
`./gradlew testDebugUnitTest` — 335 unit test cho validator, định dạng tiền, OTP, lock policy, face matcher/blink, onboarding service, hạn mức, VNPay, dữ liệu tiện ích (hoá đơn, nhà mạng, danh mục), chi nhánh (khoảng cách, giờ mở cửa, gợi ý đường đi bộ, parse OSRM) và các ViewModel (repository giả trong `src/test/.../fakes`). Rules: 49 ca trong `rules-test/` (README mục 5).
