# FLUX Bank — Handoff (cập nhật 2026-10-28)

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

- Repo git ở `FLUX_Bank/`, remote `origin` = GitHub private `thedestoroyah/FLUX_Bank` (HTTPS, tạo 2026-11-12; SSH key chưa gắn với GitHub nên dùng HTTPS qua `gh`). **`main` có F1–F4** (F4 merge 2026-10-28, commit `9f6e5c3`, đã chạy end-to-end đạt trên máy thật, mục 3). Nhánh `feature/f4-transfers` đã xoá.
- **Unit test: 216/216 pass**, rules test trên emulator 40/40. F4 đã qua review toàn nhánh (0 Critical; 6 lỗi Important đã sửa, có test). `assembleDebug` và `lintDebug` pass (lint 0 error).
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

### Chưa làm (các feature tiếp theo)
- **Feature 5** Utilities: tiền điện, tiền nước, nạp điện thoại, vé máy bay, vé phim, khách sạn, mua sắm.
- **Feature 6** Navigation: bản đồ, vị trí người dùng, chi nhánh gần nhất, đường đi bộ ngắn nhất.
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

## 5. Tài liệu & file quan trọng

| File | Nội dung |
|---|---|
| `PROJECT_CONTEXT.md` | Tổng quan kiến trúc, design system, dữ liệu, giới hạn đã biết |
| `README.md` | Build, deploy rules, tạo officer đầu tiên, luồng demo |
| `firestore.rules` | Security Rules (deploy thủ công qua Console) |
| `docs/superpowers/specs/2026-10-26-ui-foundation-f1-f2-design.md` | Thiết kế đợt 1, đã duyệt |
| `docs/superpowers/plans/2026-10-26-ui-foundation-f1-f2.md` | Kế hoạch 17 task, đã thực hiện xong |
| `docs/superpowers/specs/mockups/*.html` | Mockup đã duyệt (mở bằng trình duyệt) |
| `docs/superpowers/specs/2026-10-27-transfers-f4-design.md`, `docs/superpowers/plans/2026-10-27-transfers-f4.md` | Spec + plan F4 (đã làm xong, đã merge) |
| `docs/licenses/` | Giấy phép Inter (OFL), Phosphor (MIT), MobileFaceNet (MIT) |

### Bản đồ code (`app/src/main/java/com/flux/bank/`)
- `FluxApp.kt`: service locator, giữ `authRepository`, `userRepository`, `kycRepository`, `accountRepository`, `otpManager`, `otpNotifier`, `sessionManager`, `lockPolicy`, `appLockObserver`, `onboardingService`.
- `core/format/MoneyFormatter`, `core/validation/Validators`.
- `core/security/`: `OtpManager`, `OtpNotifier` (+ `OtpAutofillActivity`), `LockPolicy`, `AppLockObserver`.
- `data/model/`: `User`, `KycRecord`, `BankAccount` (+ `AccountType`).
- `data/repository/`: mỗi repository là một interface kèm implementation Firestore (`Auth`, `User` có `phoneIndex`, `Kyc`, `Account`).
- `domain/`: `CustomerOnboardingService`, `FaceVerificationPolicy`.
- `face/`: `FaceMatcher`, `BlinkDetector`, `FaceEmbedder` (LiteRT), `FaceDetectorHelper` (ML Kit). Model nằm ở `assets/mobilefacenet.tflite`.
- `ui/auth/`: Login, Unlock, ChangePassword, `RoleRouter`. `ui/auth/sheet/`: `TransactionAuthSheet`, `TransactionAuthViewModel`, `FaceStepController`.
- `ui/common/`: `ComingSoonFragment`, `ScreenHost`, `showKycPill`, `NumericKeypadView`, `OtpCellsView`, `SimulatedSmsBanner`, `Event`.
- `ui/customer/`: `CustomerMainActivity`, `Utility` (enum 12 tiện ích), `home/`, `profile/`, `account/` (chi tiết, nạp/rút, `VnPayFragment`, biên lai), `history/`, `transfer/` (hub, form 3 chế độ, `BankPickerSheet`), `qr/`.
- F4: `domain/TransferPolicy`, `domain/QrPayload`, `payment/VnPay`, `data/repository/PayeeRepository`, `data/model/Bank`, `Payee`, `core/format/TextNormalizer`. `ui/ekyc/`: `EkycCaptureFragment`. `ui/officer/`: `overview/`, `customers/`, `ekyc/`.
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
- Hiện 9 ô tiện ích (trừ Nạp tiền, Rút tiền, Tiết kiệm) vẫn mở `ComingSoonFragment`. Khi làm Feature 5–7 thì thay vào `HomeFragment` (click tiện ích).
- Feature 5 (thanh toán hoá đơn) nên dùng lại `AccountRepository.transferExternal` (trừ tiền + TRANSFER_OUT, `counterpartyBank` = mã nhà cung cấp) hoặc thêm `type` mới cùng nhánh `ownerMoneyMoveAllowed` trong rules; `TransferPolicy.checkAmount` cho hạn mức.
- Số dư chỉ đổi qua `AccountRepository.deposit/withdraw`: một `runTransaction` ghi `accounts/{id}` (`balance`, `lastTxId`) và `transactions/{txId}` mới. Rules `ownerMoneyMoveAllowed` bắt `lastTxId` trỏ tới giao dịch **chưa tồn tại trước đó**, cùng tài khoản, `balanceAfter` = số dư mới, đúng phép cộng/trừ.
- F4 mở rộng hợp đồng: `TRANSFER_OUT` (chủ là người gửi) + `TRANSFER_IN` (chủ là người nhận) trỏ nhau qua `pairTxId`; rules `transferCreditAllowed`/`transferInBacked`. Loại giao dịch mới: thêm nhánh ở `TransactionRowAdapter.labelFor(tx)` và `ReceiptFragment.rowsFor`, thêm ca test vào `rules-test/rules.test.mjs` trước khi sửa rules.

### Dữ liệu Firestore
`users/{uid}` · `phoneIndex/{sđt}` → `{uid}` · `kyc/{uid}` (ảnh Base64 + `faceEmbedding`) · `accounts/{id}` (`ownerUid`, `type`, `accountNumber` `8868xxxxxxxx`, `balance` Long VND). Chi tiết ở `PROJECT_CONTEXT.md`.

## 6. Môi trường & mẹo (quan trọng)

- **Build**: daemon Gradle đã ghim JDK 21 (`gradle/gradle-daemon-jvm.properties`), không cần `JAVA_HOME`: `./gradlew testDebugUnitTest assembleDebug`. Android Studio: Gradle JDK = `/usr/lib/jvm/java-21-openjdk`.
- **Test rules**: `rules-test/` (Node + Firestore emulator), xem README mục 5.
- **Firestore Console**: gõ tay tên field khi tạo document (copy từ markdown có thể dính ký tự ẩn U+200B, app sẽ không đọc được field).
- **Emulator**: AVD `Pixel_7` (Android 37, image production nên không có adb root). Chạy không cửa sổ ở chế độ background:
  ```bash
  ~/Android/Sdk/emulator/emulator -avd Pixel_7 -no-window -no-audio -no-boot-anim -no-snapshot-save -gpu swiftshader_indirect
  ~/Android/Sdk/platform-tools/adb install -r -g app/build/outputs/apk/debug/app-debug.apk
  adb exec-out screencap -p > shot.png
  ```
- **Chụp màn hình khi không có tài khoản đăng nhập**: tạo tạm `app/src/debug/AndroidManifest.xml` để export `CustomerMainActivity`/`OfficerMainActivity` (`tools:replace="android:exported"`), rồi `adb shell am start -n com.flux.bank/.ui.customer.CustomerMainActivity`. Nếu cần dữ liệu thì tạm nhét dữ liệu mẫu vào ViewModel. **Luôn revert và xoá trước khi commit** (`git grep '"demo"'` để kiểm tra).
- **Điều khiển điện thoại qua adb** (Xiaomi, MIUI): máy có mã khoá, cần chủ dự án mở và bật Stay awake. Bàn phím Telex có thể đổi "us" thành "ú" khi `adb shell input text` (gõ "cuss…" để ra "cus…"). Chạm theo chữ bằng `uiautomator dump` thì **lọc `package="com.flux.bank"`**, nếu không sẽ chạm nhầm bong bóng chat Zalo nổi trên màn hình. MIUI bỏ qua phím BACK giả lập khi app bật predictive back; dùng nút back trên header hoặc `input swipe` từ mép trái.
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

## 8. Giới hạn đã biết (ghi vào báo cáo)
- Gói Spark: OTP và đối chiếu khuôn mặt chỉ được kiểm tra trên thiết bị; rules chỉ giới hạn field nào được sửa. Hướng nâng cấp: Cloud Functions phát và kiểm token xác thực.
- `listCustomers` tải toàn bộ khách hàng rồi lọc ở client (shortcut, đủ cho quy mô đồ án).

## 9. Cách bắt đầu session mới

Gợi ý câu lệnh mở đầu:
> Đọc `FLUX_Bank/HANDOFF.md` và `FLUX_Bank/PROJECT_CONTEXT.md`. Tiếp theo brainstorm Feature 5 (Utilities) bằng `/superpowers:brainstorming`.

Quy trình đã dùng và nên giữ: `superpowers:brainstorming` (hỏi từng câu, mockup HTML trên visual companion) → viết spec vào `docs/superpowers/specs/` → `superpowers:writing-plans` → `superpowers:executing-plans` (chủ dự án chọn chế độ **Native**) → review toàn nhánh → `finishing-a-development-branch`.

Thứ tự đề xuất cho khoảng 34 ngày còn lại (tính từ 2026-10-28; F1–F4 đã xong):
1. Feature 5: tận dụng `TransactionAuthSheet` và lưới tiện ích có sẵn.
2. Feature 6: Google Maps (cần API key, có hạn mức miễn phí) hoặc OSMDroid nếu muốn tránh billing.
3. Feature 7: chatbot. Cần quyết định dùng LLM API hay luật cố định; lưu ý chi phí và không để API key nằm trong app.
4. Play Store (tài khoản developer tốn $25, chủ dự án cần biết trước), báo cáo, video. Nên chừa ít nhất 5–7 ngày cuối cho phần này.
