# FLUX BANK - ANDROID NATIVE PROJECT
- Package: com.flux.bank
- Language: Kotlin (XML layouts + ViewBinding, không dùng Compose)
- Architecture: MVVM — Activity/Fragment → ViewModel → Repository (interface + Firestore impl). Service locator đơn giản trong `FluxApp`, không dùng DI library.
- Backend: Firebase Authentication + Cloud Firestore, gói **Spark (miễn phí)** — không Storage, không Cloud Functions.
- Thiết kế & kế hoạch: `docs/superpowers/specs/2026-10-26-ui-foundation-f1-f2-design.md`, `docs/superpowers/plans/2026-10-26-ui-foundation-f1-f2.md`, mockup đã duyệt trong `docs/superpowers/specs/mockups/`.

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
| `accounts/{id}` | `ownerUid`, `type` CHECKING/SAVING/MORTGAGE, `accountNumber` 8868xxxxxxxx, `balance` Long VND |

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

## Giới hạn đã biết
- Gói Spark: OTP và đối chiếu khuôn mặt được kiểm tra trên thiết bị; rules chỉ giới hạn field được sửa. Hướng nâng cấp: Cloud Functions phát/kiểm token.
- `FaceMatcher.MATCH_THRESHOLD = 0.6` lấy từ repo gốc của model; cần hiệu chỉnh bằng ảnh thật.

## Kiểm thử
`./gradlew testDebugUnitTest` — unit test cho validator, định dạng tiền, OTP, lock policy, face matcher/blink, onboarding service và các ViewModel (repository giả trong `src/test/.../fakes`).
