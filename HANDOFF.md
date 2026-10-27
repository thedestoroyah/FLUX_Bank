# FLUX Bank — Handoff (cập nhật 2026-10-26)

Tài liệu bàn giao để tiếp tục công việc ở session mới. Đọc file này trước, sau đó đọc `PROJECT_CONTEXT.md` (kiến trúc, design system, dữ liệu).

---

## 1. Bối cảnh

- **Đồ án**: Final Project môn Mobile Apps Development (TDTU). Đề: `../FinalProject_2026_2027.pdf`. Barem: `../Marking.xlsx`.
- **Hạn nộp: 2026-12-01.** Làm **một mình**. Cần nộp: app chạy được, đưa lên Google Play, báo cáo (class diagram, ERD, use case, kiến trúc...), video demo.
- **Barem tóm tắt** (10 điểm): Architecture 1 · Class diagram 1 · UI 1 · Database design 1 · User profiles 1 · Security 1 · Account mgmt 0.5 · Transaction 1 · Utilities 0.5 · Navigation 0.5 · Chatbot 0.5 · Hình thức báo cáo/demo 1.
- **Tech**: Android native, Kotlin, XML + ViewBinding (không Compose), MVVM, Firebase Auth + Firestore **gói Spark miễn phí**: không Storage, không Cloud Functions, không SMS thật. Chủ dự án muốn **tiết kiệm chi phí tối đa**.
- Firebase project: `banking-app-final-b5407`.
- Code ban đầu (Feature 1–2) do Google Antigravity viết. Phiên này đã làm lại toàn bộ giao diện và hoàn thiện Feature 1–2.

## 2. Trạng thái hiện tại

- Repo git ở `FLUX_Bank/`. Nhánh làm việc: **`feat/ui-foundation-f1-f2`** (18 commit trên `main`). **Chưa merge**, chưa có remote.
- **Đang chờ chủ dự án chọn**: (1) merge vào `main` ở local, (2) thêm remote, push và tạo PR, hoặc (3) giữ nguyên nhánh.
- **Unit test: 118/118 pass.** `assembleDebug` và `lintDebug` pass (lint 0 error).
- Đã review toàn bộ nhánh bằng một reviewer độc lập. Đã sửa hết lỗi Important; danh sách lỗi Minor còn lại ở mục 7.

### Đã hoàn thành
| Phần | Nội dung |
|---|---|
| Design system | Màu, thang chữ (Inter), khoảng cách, style `Widget.Flux.*`, icon Phosphor duotone/regular, chỉ dùng light theme, icon app FLUX |
| Khung app | `CustomerMainActivity` (5 tab có nút QR nổi ở giữa) · `OfficerMainActivity` (5 tab) · `ScreenHost.openScreen()` |
| Feature 1 | Nhân viên tạo / tìm / sửa khách (FirebaseApp phụ, có rollback, cấp tài khoản Checking 0 ₫, mật khẩu tạm), gửi email đặt lại mật khẩu, chụp eKYC hộ khách. Khách xem hồ sơ, tự sửa SĐT (qua OTP), địa chỉ, avatar. Bắt đổi mật khẩu ở lần đăng nhập đầu |
| Feature 2 | eKYC (ML Kit + MobileFaceNet embedding), nhân viên duyệt / từ chối có lý do. Khoá app sinh trắc học (cold start, khi app bị khôi phục, sau ≥ 60 s ở nền). OTP 6 số / 120 s / dùng 1 lần / gắn với giao dịch / sai 5 lần thì khoá, gửi qua SMS giả lập kèm banner dự phòng. `TransactionAuthSheet` (bàn phím riêng + bước quét mặt khi giao dịch ≥ 10.000.000 ₫). Security Rules |

### Chưa làm (các feature tiếp theo)
- **Feature 3** Account management: Saving (lãi suất, lãi/tháng; nhân viên sửa lãi suất ở tab "Lãi suất"), Mortgage (số tiền trả mỗi tháng / 2 tuần), lịch sử giao dịch, nạp / rút.
- **Feature 4** Transaction: chuyển tiền cùng ngân hàng (tra người nhận qua `phoneIndex`) và khác ngân hàng, kiểm tra hợp lệ trước khi ghi (Firestore transaction), tích hợp VNPay hoặc Stripe (sandbox).
- **Feature 5** Utilities: tiền điện, tiền nước, nạp điện thoại, vé máy bay, vé phim, khách sạn, mua sắm.
- **Feature 6** Navigation: bản đồ, vị trí người dùng, chi nhánh gần nhất, đường đi bộ ngắn nhất.
- **Feature 7** AI chatbot: trả lời kèm link tới các chức năng.
- Đưa lên Play Store, viết báo cáo, quay video demo.

## 3. Việc chủ dự án cần làm (chưa xác nhận đã xong)

1. **Publish lại `firestore.rules`**. Lần sửa cuối có siết thêm rule của `kyc/{uid}`. Rules cũ và officer đầu tiên đã được tạo theo README.
2. **Chạy thử toàn bộ luồng trên máy thật** (chưa ai chạy end-to-end): tạo khách → khách đăng nhập lần đầu và đổi mật khẩu → nộp eKYC → nhân viên duyệt → bật sinh trắc học, khoá và mở khoá → đổi SĐT qua OTP → "Kiểm tra xác thực khuôn mặt" (chính mình phải qua, người khác phải bị từ chối).
3. **Hiệu chỉnh `FaceMatcher.MATCH_THRESHOLD`** (`app/src/main/java/com/flux/bank/face/FaceMatcher.kt`, hiện là 0.6): log cosine giữa các ảnh cùng một người và giữa hai người khác nhau, đặt ngưỡng ở giữa, rồi ghi số đo vào KDoc.
4. Chọn cách xử lý nhánh (mục 2).

## 4. Những điều chủ dự án đã quyết định (đừng hỏi lại)

- Giữ design system gốc (Apple Bento, nền trắng / `#F5F5F7`, chữ Obsidian `#0A0D12`) và bổ sung token. **Emerald `#10B981` chỉ dùng cho điểm tương tác**. Trang chủ có **dải mint `#E7F8EF` phía sau thẻ**. Icon **duotone**. Nút chính màu **đen**.
- Trang chủ hướng "A1": thẻ Titanium tỉ lệ thẻ thật 1.586:1, chiếm phần trên màn hình, **lưới tiện ích 4×3**, giao dịch gần đây. Thanh tab: **Trang chủ · Chuyển tiền · Quét QR (nổi giữa) · Lịch sử · Cá nhân**.
- Giao diện nhân viên cùng phong cách. Chữ hiển thị **tiếng Việt**, tiền **VND**.
- Xác thực giao dịch bằng **bottom sheet** có nền tối, bàn phím số riêng.
- OTP gửi bằng **thông báo giả lập SMS** (không dùng Blaze).
- Xác thực khuôn mặt **mức A**: ML Kit kiểm tra chớp mắt + MobileFaceNet so với ảnh eKYC.
- Mọi màn hình mới phải được **duyệt mockup trước khi code giao diện**. Chủ dự án thích xem mockup HTML so sánh trên trình duyệt.
- Nếu thiếu thời gian, cắt theo thứ tự: skeleton/haptic → nhân viên chụp eKYC hộ → kiểm tra chớp mắt. Không cắt phần đối chiếu khuôn mặt.

## 5. Tài liệu & file quan trọng

| File | Nội dung |
|---|---|
| `PROJECT_CONTEXT.md` | Tổng quan kiến trúc, design system, dữ liệu, giới hạn đã biết |
| `README.md` | Build, deploy rules, tạo officer đầu tiên, luồng demo |
| `firestore.rules` | Security Rules (deploy thủ công qua Console) |
| `docs/superpowers/specs/2026-10-26-ui-foundation-f1-f2-design.md` | Thiết kế đợt 1, đã duyệt |
| `docs/superpowers/plans/2026-10-26-ui-foundation-f1-f2.md` | Kế hoạch 17 task, đã thực hiện xong |
| `docs/superpowers/specs/mockups/*.html` | Mockup đã duyệt (mở bằng trình duyệt) |
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
- `ui/customer/`: `CustomerMainActivity`, `Utility` (enum 12 tiện ích), `home/`, `profile/`. `ui/ekyc/`: `EkycCaptureFragment`. `ui/officer/`: `overview/`, `customers/`, `ekyc/`.
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
- Hiện các tab Chuyển tiền / Lịch sử / Quét QR, 12 ô tiện ích và tab "Lãi suất" của nhân viên đều mở `ComingSoonFragment`. Khi làm Feature 3–7 thì thay vào `CustomerMainActivity.fragmentFor()`, `HomeFragment` (click tiện ích) và `OfficerMainActivity.fragmentFor()`.
- Rules `accounts`: hiện **cấm mọi update**. Feature 4 phải mở quyền cập nhật số dư có kiểm tra (ví dụ chỉ trừ tiền tài khoản của chính mình và cộng đúng số tiền tương ứng vào tài khoản nhận). Đây là điểm cần thiết kế cẩn thận.

### Dữ liệu Firestore
`users/{uid}` · `phoneIndex/{sđt}` → `{uid}` · `kyc/{uid}` (ảnh Base64 + `faceEmbedding`) · `accounts/{id}` (`ownerUid`, `type`, `accountNumber` `8868xxxxxxxx`, `balance` Long VND). Chi tiết ở `PROJECT_CONTEXT.md`.

## 6. Môi trường & mẹo (quan trọng)

- **Build**: dùng JDK 21. Nếu gặp lỗi `jlink executable ... does not exist`, nghĩa là daemon Gradle do VSCode mở (JRE 25 không có jlink) đang chạy. Chạy `./gradlew --stop` rồi build lại:
  ```bash
  cd FLUX_Bank && ./gradlew --stop -q; JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew testDebugUnitTest assembleDebug
  ```
- **Emulator**: AVD `Pixel_7` (Android 37, image production nên không có adb root). Chạy không cửa sổ ở chế độ background:
  ```bash
  ~/Android/Sdk/emulator/emulator -avd Pixel_7 -no-window -no-audio -no-boot-anim -no-snapshot-save -gpu swiftshader_indirect
  ~/Android/Sdk/platform-tools/adb install -r -g app/build/outputs/apk/debug/app-debug.apk
  adb exec-out screencap -p > shot.png
  ```
- **Chụp màn hình khi không có tài khoản đăng nhập**: tạo tạm `app/src/debug/AndroidManifest.xml` để export `CustomerMainActivity`/`OfficerMainActivity` (`tools:replace="android:exported"`), rồi `adb shell am start -n com.flux.bank/.ui.customer.CustomerMainActivity`. Nếu cần dữ liệu thì tạm nhét dữ liệu mẫu vào ViewModel. **Luôn revert và xoá trước khi commit** (`git grep '"demo"'` để kiểm tra).
- Không commit các file `.idea/*` do IDE tự sửa. Dùng `git add app docs ...` thay vì `git add -A`.
- Quy ước: comment và KDoc viết tiếng Việt, tên trong code bằng tiếng Anh. Commit theo dạng `feat:` / `fix:` / `chore:` / `docs:` / `refactor:` / `test:`. Làm theo TDD (viết test fail trước).

## 7. Lỗi nhỏ để lại sau (từ review)
1. OTP của giao dịch khác hoặc mã cũ sau khi gửi lại chỉ báo chung là "mã không đúng" (chưa có thông báo riêng).
2. Điều kiện quét mặt (đã eKYC chưa, có embedding chưa) chỉ được kiểm tra sau khi người dùng đã nhập OTP.
3. Màn mở khoá có thể chồng 2 lần nếu xuống nền ≥ 60 s ngay tại màn mở khoá.
4. Nhân viên sửa khách: chuyển SĐT và cập nhật các field khác không nguyên tử; snackbar "Đã lưu" hiện lặp lại (`saved` không reset).
5. Lỗi khi rollback `deleteCreatedAuthUser()` bị bỏ qua.
6. Ghi `mustChangePassword = false` thất bại thì lần đăng nhập sau lại bị bắt đổi mật khẩu.
7. Rules chưa chặn khách tự sửa `phoneNumber` mà không chuyển `phoneIndex` (chỉ client tuân thủ).
8. Bấm "Lưu" 2 lần nhanh có thể mở 2 sheet OTP.

## 8. Giới hạn đã biết (ghi vào báo cáo)
- Gói Spark: OTP và đối chiếu khuôn mặt chỉ được kiểm tra trên thiết bị; rules chỉ giới hạn field nào được sửa. Hướng nâng cấp: Cloud Functions phát và kiểm token xác thực.
- `listCustomers` tải toàn bộ khách hàng rồi lọc ở client (shortcut, đủ cho quy mô đồ án).

## 9. Cách bắt đầu session mới

Gợi ý câu lệnh mở đầu:
> Đọc `FLUX_Bank/HANDOFF.md` và `FLUX_Bank/PROJECT_CONTEXT.md`. Tôi đã [làm/chưa làm] mục 3. Nhánh `feat/ui-foundation-f1-f2`: [merge / giữ]. Tiếp theo brainstorm Feature 3 (Account management) bằng `/superpowers:brainstorming`.

Quy trình đã dùng và nên giữ: `superpowers:brainstorming` (hỏi từng câu, mockup HTML trên visual companion) → viết spec vào `docs/superpowers/specs/` → `superpowers:writing-plans` → `superpowers:executing-plans` (chủ dự án chọn chế độ **Native**) → review toàn nhánh → `finishing-a-development-branch`.

Thứ tự đề xuất cho 5 tuần còn lại (khoảng 36 ngày):
1. Feature 3 + 4: phần chính và nặng điểm nhất. Cần rules cho số dư và Firestore transaction.
2. Feature 5: tận dụng `TransactionAuthSheet` và lưới tiện ích có sẵn.
3. Feature 6: Google Maps (cần API key, có hạn mức miễn phí) hoặc OSMDroid nếu muốn tránh billing.
4. Feature 7: chatbot. Cần quyết định dùng LLM API hay luật cố định; lưu ý chi phí và không để API key nằm trong app.
5. Play Store (tài khoản developer tốn $25, chủ dự án cần biết trước), báo cáo, video. Nên chừa ít nhất 5–7 ngày cuối cho phần này.
