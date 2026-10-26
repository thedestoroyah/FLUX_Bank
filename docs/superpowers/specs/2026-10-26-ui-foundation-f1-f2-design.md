# FLUX Bank — Đợt 1: Nền tảng UI + làm lại Feature 1 & 2

- Ngày: 2026-10-26 · Hạn nộp đồ án: 2026-12-01 (còn ~5 tuần, làm 1 mình)
- Project: `FLUX_Bank/` — Android native, Kotlin, ViewBinding, MVVM, Firebase Auth + Firestore (gói **Spark miễn phí**)
- Mockup đã chốt: [mockups/2026-10-26-home-and-otp-sheet.html](mockups/2026-10-26-home-and-otp-sheet.html)
- Đề: `../FinalProject_2026_2027.pdf` · Barem: `../Marking.xlsx`

Ký hiệu: **[Chủ dự án]** = người dùng đã quyết định. **[Claude]** = quyết định của Claude, chủ dự án chưa phản đối nhưng có thể đổi.

---

## 1. Mục đích và lý do

Hai feature đầu (User profiles, Security) được một công cụ AI khác viết. Code chạy được nhưng:
- Giao diện không làm chủ dự án hài lòng ("về lâu dài không ổn"). Chủ dự án muốn giao diện kiểu **app ngân hàng hiện đại**: lấy cảm hứng bố cục, không sao chép.
- Còn thiếu nhiều so với đề: officer chưa tạo/sửa được khách hàng, OTP chưa gắn vào giao dịch, chưa có xác thực khuôn mặt cho giao dịch giá trị cao, đăng nhập sinh trắc học không phải đăng nhập thật.
- Kiến trúc không đồng bộ: Activity gọi repository trực tiếp, `AuthRepository` gánh luôn eKYC, chưa có khung điều hướng chung.

Đợt 1 dựng **nền tảng** để 5 feature sau (Account, Transaction, Utilities, Navigation, Chatbot) xây lên một cách đồng bộ, đồng thời làm Feature 1 & 2 đạt mức "ổn định, đầy đủ chức năng" theo barem (mỗi feature 1 điểm, UI 1 điểm).

## 2. Mục tiêu, không làm, phản mục tiêu

**Mục tiêu**
1. Design system mở rộng thành **bộ style/component dùng chung**; mọi màn hình mới chỉ dùng lại các style này.
2. Khung điều hướng khách hàng và officer (bottom tab) theo mockup.
3. Làm lại **toàn bộ** giao diện màn hình hiện có (Login, Customer, Officer, eKYC, duyệt eKYC) theo phong cách mới.
4. Feature 1 đầy đủ theo đề: officer tạo / tìm / sửa khách hàng; khách xem / sửa hồ sơ.
5. Feature 2 đầy đủ theo đề: eKYC, đăng nhập sinh trắc học thật, OTP cho mọi giao dịch, xác thực khuôn mặt cho giao dịch giá trị cao, Security Rules.
6. Một **component xác thực giao dịch dùng lại được** (bottom sheet OTP + quét mặt) để Feature 3/4/5 chỉ cần gọi.

**Không làm trong đợt này**
- Logic của Account (Saving/Mortgage), Transaction, Utilities, Map, Chatbot. Các tab/ô tiện ích tương ứng chỉ hiện màn hình "Sắp ra mắt".
- Dark mode.
- Đưa app lên Play Store, viết báo cáo, quay video.
- Bất cứ thứ gì cần gói Blaze (Cloud Functions, SMS thật, email OTP).

**Phản mục tiêu (dù chạy được nhưng vẫn coi là thất bại)**
- Giao diện vẫn toàn trắng và đen, hoặc mỗi màn hình một kiểu. Nếu có màn hình tự đặt màu, cỡ chữ, padding mà không dùng style chung thì chưa đạt.
- Thẻ Titanium bị thu nhỏ hoặc mất vai trò trung tâm.
- Phát sinh chi phí Firebase.
- Bottom sheet OTP bị bàn phím đẩy lệch bố cục.
- Bước "xác thực khuôn mặt" không thực sự dùng ảnh eKYC.

## 3. Ràng buộc

- Kotlin, XML layout + ViewBinding (không chuyển sang Compose), MVVM + Coroutines + LiveData/StateFlow như code hiện có.
- Firebase Spark: chỉ dùng Auth (email/password) + Firestore. Không dùng Storage (ảnh lưu Base64 trong Firestore, như hiện tại). Không dùng Cloud Functions.
- Mọi thư viện mới phải miễn phí, chạy on-device, giấy phép cho phép dùng (Apache/MIT/OFL).
- minSdk 26, targetSdk 37 (giữ nguyên).
- Giao diện **chỉ sáng** (light only).
- Toàn bộ chữ hiển thị là **tiếng Việt**, nằm trong `strings.xml`. Tiền tệ **VND** (định dạng `48.250.000 ₫`, lưu kiểu `Long`). **[Chủ dự án]**
- Một người làm, ~5 tuần cho cả đồ án → đợt 1 nên gọn, khoảng 1,5–2 tuần.

## 4. Giao diện — những gì chủ dự án đã chốt

### 4.1 Design system (giữ gốc + bổ sung) **[Chủ dự án]**
Giữ nguyên bảng màu trong `res/values/colors.xml` (nền `#FFFFFF`/`#F5F5F7`, Obsidian `#0A0D12`, Apple Blue `#0071E3`, Emerald `#10B981`, Gold `#D97706`, thẻ Titanium), bo góc 20–24dp cho tile lớn, 12–16dp cho chip và nút, vùng chạm ≥ 48dp.

Bổ sung:
- **Bộ style dùng chung** trong `themes.xml`/`styles.xml`: Card (Bento tile), Button.Primary (đen), Button.Secondary (viền), TextField, dòng giao dịch, ô hiển thị số tiền, StatusPill, BottomSheet, ô tiện ích (icon tile + nhãn), màn "Sắp ra mắt", empty state.
- **Thang chữ** khoảng 6 cấp (ví dụ: Display, Title L, Title M, Body, Label, Caption), **tối thiểu 12sp**. Font **Inter** (đóng gói trong `res/font`). Số tiền dùng **tabular numbers** (`fontFeatureSettings="tnum"`).
- **Thang khoảng cách** 4 / 8 / 12 / 16 / 24 / 32dp, đặt trong `dimens.xml`.
- **Elevation 0 + viền mảnh `#E5E7EB`** cho các tile. Chỉ thẻ Titanium và bottom sheet có bóng mềm.
- **Icon duotone** (nét đậm, ruột tô màu nhạt), mỗi nhóm tiện ích một màu (tiền vào: emerald, điện: gold, nước/ĐT/chi nhánh: blue, vé máy bay: sky, vé phim/mua sắm: rose, khách sạn/AI: violet), đặt trên ô nền nhạt cùng tông. Gợi ý nguồn: Phosphor Icons (MIT, có kiểu duotone) chuyển sang VectorDrawable.
- **Mức dùng màu "Màu 1 + nền mint"**: Emerald chỉ xuất hiện ở các điểm tương tác (nút QR giữa thanh tab, tab đang chọn, chấm chỉ thẻ, số tiền nhận vào, ô OTP đang nhập, viền focus). Riêng trang chủ có dải nền **mint `#E7F8EF` nhạt dần xuống `#F5F5F7`** ở khoảng 40% trên, và thẻ Titanium có **bóng xanh mềm**. Nút hành động chính (Xác nhận, Tiếp tục…) vẫn **đen Obsidian**.
- Các mẫu UX chung: nút ẩn/hiện số dư, skeleton loading thay cho vòng xoay, xác nhận và nhập mã bằng bottom sheet, rung nhẹ (haptic) khi thành công.
- Quy tắc layout: ConstraintLayout cho bố cục màn hình. Danh sách dùng **RecyclerView** (bỏ quy tắc "100% ConstraintLayout" cho danh sách).

### 4.2 Khung app khách hàng **[Chủ dự án]**
- **Thanh tab 5 mục**, từ trái sang phải: **Trang chủ · Chuyển tiền · Quét QR · Lịch sử · Cá nhân**. Quét QR là **nút emerald nổi ở giữa**.
- **Trang chủ** (theo mockup):
  1. Lời chào ("Chào buổi sáng" + tên) + avatar + chuông thông báo.
  2. **Thẻ Titanium** theo **tỉ lệ thẻ thật 1.586 : 1**, chiếm phần trên màn hình, vuốt ngang để đổi tài khoản (Checking / Saving / Mortgage; đợt này chỉ có Checking), có chấm chỉ trang, nút ẩn số dư, 4 số cuối và hạn thẻ.
  3. **Lưới tiện ích 4×3**: Nạp tiền · Rút tiền · Tiền điện · Tiền nước / Nạp ĐT · Vé máy bay · Vé phim · Khách sạn / Mua sắm · Tiết kiệm · Chi nhánh · Trợ lý AI.
  4. Giao dịch gần đây, nhóm theo ngày ("Hôm nay", "Hôm qua"…). Đợt này hiện empty state.
- Toàn bộ trang chủ cuộn dọc được.

### 4.3 Khung app officer **[Chủ dự án: cùng phong cách]**
Thanh tab **[Claude]**: **Tổng quan · Khách hàng · eKYC · Lãi suất · Cá nhân** (5 mục, không có nút nổi). Tab Lãi suất đợt này là "Sắp ra mắt". Tổng quan hiển thị số khách hàng, số hồ sơ eKYC đang chờ, và lối tắt "Tạo khách hàng".

### 4.4 Xác thực giao dịch — bottom sheet **[Chủ dự án]**
- Popup dạng **bottom sheet** trượt lên trên **nền tối mờ** (scrim).
- Nội dung: tiêu đề "Xác nhận giao dịch", tóm tắt giao dịch (người nhận, ngân hàng/số TK, số tiền), 6 ô OTP, đồng hồ "Gửi lại mã sau mm:ss", nút "Xác nhận" (đen).
- **Bàn phím số riêng vẽ bên trong sheet**, không gọi bàn phím hệ thống, để sheet không bị đẩy lên và bàn phím bên thứ ba không đọc được OTP. **[Claude, chủ dự án đã đồng ý]**

## 5. Chức năng — những gì chủ dự án đã chốt

### 5.1 Feature 1 — Hồ sơ người dùng
- **Officer tạo khách hàng**: form họ tên, email, SĐT, số CCCD, địa chỉ, mật khẩu tạm. Validate (email hợp lệ, SĐT VN 10 số bắt đầu bằng 0, CCCD 12 số, không trùng email). User Auth được tạo qua **một `FirebaseApp` phụ**, nên officer **không bị đăng xuất**. Khi tạo, hệ thống cấp sẵn **1 tài khoản Checking** (số TK tự sinh, số dư 0 ₫).
- **Officer tìm khách** theo tên / SĐT / email, xem chi tiết (hồ sơ, trạng thái eKYC, tài khoản), **sửa hồ sơ** theo yêu cầu khách.
- **Khách xem và sửa hồ sơ** ở tab Cá nhân. Khách **chỉ tự sửa được SĐT, địa chỉ, ảnh đại diện**. Họ tên và CCCD chỉ officer sửa được. Đổi SĐT phải qua OTP.
- **Lần đầu đăng nhập** bằng mật khẩu tạm thì bắt buộc đổi mật khẩu (`mustChangePassword`).

### 5.2 Feature 2 — Bảo mật
- **eKYC**: chụp CCCD và selfie ở **độ phân giải đầy đủ** (bỏ `TakePicturePreview` thumbnail), nén xuống mức đủ cho đối chiếu khuôn mặt. Khi nộp, app phát hiện khuôn mặt (ML Kit) trong selfie. Nếu không có đúng 1 khuôn mặt thì báo lỗi ngay. App tính **embedding khuôn mặt** và lưu kèm hồ sơ.
- **Officer chụp hộ eKYC** cho khách tại quầy (cùng luồng chụp, chọn khách từ danh sách).
- **Officer duyệt / từ chối** hồ sơ; khi từ chối phải chọn/nhập **lý do**, và khách thấy lý do đó.
- **Đăng nhập sinh trắc học thật**: phiên Firebase được giữ lại. "Khoá app" khác với "Đăng xuất". Khi mở lại app mà phiên còn và người dùng đã bật sinh trắc học, app hiện màn mở khoá (BiometricPrompt). Mở khoá xong thì vào thẳng. Có lựa chọn "Đăng nhập tài khoản khác" (sign out hẳn). Đăng xuất hẳn sẽ xoá session cục bộ.
- **OTP cho mọi giao dịch**: 6 số, sinh bằng `SecureRandom`, hiệu lực 120 giây, dùng một lần, **gắn với đúng một giao dịch** (không dùng mã của giao dịch này cho giao dịch khác). Tối đa 5 lần nhập sai thì huỷ mã. Mã được gửi qua **thông báo giả lập SMS** (local notification: "FLUX: Mã OTP của bạn là 481 203. Không chia sẻ mã này cho bất kỳ ai."). Bấm vào thông báo thì mã tự điền vào sheet. **[Chủ dự án]**
- **Giao dịch từ 10.000.000 ₫ trở lên** **[Claude: ngưỡng; chủ dự án chọn mức A]**: ngay trong bottom sheet, sau OTP, thêm bước **quét mặt**:
  1. Camera trước. **ML Kit Face Detection** kiểm tra người thật (yêu cầu chớp mắt, dựa vào xác suất mở mắt).
  2. Cắt khuôn mặt, chạy **MobileFaceNet (TFLite, on-device)** để lấy embedding.
  3. So **cosine similarity** với embedding eKYC. Vượt ngưỡng thì cho qua, không vượt thì từ chối (cho thử lại tối đa 3 lần).
  4. Khách **chưa eKYC VERIFIED** thì không được làm giao dịch giá trị cao.
- **Firestore Security Rules** (file `firestore.rules` trong repo, deploy qua Console):
  - Khách chỉ đọc/ghi document của chính mình và chỉ được sửa các field cho phép (SĐT, địa chỉ, avatar, cờ biometric). Khách không tự đổi được `role`, `kycStatus`, họ tên, CCCD.
  - Officer (xác định bằng `role == BANK_OFFICER` trong document `users/{uid}` của chính họ) được đọc/sửa hồ sơ khách và duyệt eKYC.
  - Khách không tự sửa số dư tài khoản (để Feature 4 dựng tiếp bằng Firestore transaction).

### 5.3 Component xác thực dùng lại
Một API kiểu `TransactionAuthSheet.show(summary, amount) { onVerified / onCancelled }`. Sheet tự quyết có cần bước quét mặt hay không dựa vào ngưỡng. Feature 3/4/5 chỉ gọi component này.

Ở đợt 1, component được dùng thật ở hai chỗ:
- Khách **đổi SĐT** (OTP).
- Tab Cá nhân → Bảo mật → **"Kiểm tra xác thực khuôn mặt"**: khách đã VERIFIED chạy thử bước quét mặt, để demo được trước khi có Feature 4. **[Claude]**

### 5.4 Các quy tắc chi tiết **[Claude]**
- **Mức ràng buộc của OTP và quét mặt**: gói Spark không có server, nên việc kiểm tra OTP và khuôn mặt **chỉ diễn ra trên máy**. Security Rules chỉ giới hạn *ai được sửa field nào*, không chứng minh được rằng OTP đã được nhập. Đây là giới hạn đã biết, sẽ ghi vào báo cáo kèm hướng nâng cấp (Cloud Functions xác minh token). `TransactionAuthSheet` trả về một callback `onVerified` đơn giản, không phát token.
- **Khoá app**: khoá khi mở app (cold start) và khi app quay lại sau **≥ 60 giây ở nền**, áp dụng cho cả khách và officer, chỉ khi người dùng đã bật sinh trắc học. Nếu tắt sinh trắc học mà phiên còn hạn thì vào thẳng (như hành vi chuẩn của Firebase). Không có nút "Khoá app" riêng. Trong tab Cá nhân có "Đăng xuất" (đăng xuất hẳn).
- **Email cố định sau khi tạo**: SDK client không đổi được email hay mật khẩu Auth của người khác. Officer sửa được họ tên, SĐT, CCCD, địa chỉ. Officer có nút **"Gửi email đặt lại mật khẩu"** (`sendPasswordResetEmail`, miễn phí).
- **SĐT và CCCD là duy nhất**: SĐT sẽ là định danh nhận chuyển khoản trong Feature 4. Kiểm tra trùng khi tạo khách, khi officer sửa, và khi khách đổi SĐT.
- **eKYC do officer chụp hộ** vẫn vào hàng đợi duyệt như bình thường. Bất kỳ officer nào, kể cả người chụp, đều được duyệt. Hệ thống ghi lại `submittedBy` và `reviewedBy`.

### 5.5 Duyệt mockup và thứ tự cắt giảm **[Chủ dự án]**
- **Mọi màn hình chưa có mockup phải được chủ dự án duyệt mockup trước khi code giao diện**: Đăng nhập, Mở khoá, Đổi mật khẩu lần đầu, tab Cá nhân, các tab officer (Tổng quan, Khách hàng, eKYC), Tạo/Chi tiết/Sửa khách, Chụp eKYC, Duyệt eKYC, bước quét mặt trong sheet, màn "Sắp ra mắt". Kế hoạch phải có một **chốt duyệt mockup** trước phần giao diện. Phần nền (design system, repository, rules, logic) có thể làm song song.
- **Nếu thiếu thời gian, hoãn theo thứ tự**: (1) skeleton loading + haptic → (2) officer chụp hộ eKYC → (3) bước kiểm tra chớp mắt. **Không cắt** phần đối chiếu khuôn mặt bằng MobileFaceNet.

## 6. Dữ liệu (Firestore) **[Claude]**

| Collection | Document | Field chính |
|---|---|---|
| `users` | `{uid}` | `uid, email, fullName, phoneNumber, nationalId, address, avatarBase64?, role, kycStatus, kycRejectReason?, biometricEnabled, mustChangePassword, createdAt, createdBy?` |
| `kyc` | `{uid}` | `idCardBase64, faceImageBase64, faceEmbedding: List<Double>, submittedAt, submittedBy (uid khách hoặc officer), reviewedBy?, reviewedAt?` |
| `accounts` | `{accountId}` | `accountId, ownerUid, type (CHECKING/SAVING/MORTGAGE), accountNumber, balance: Long (VND), createdAt` |

- Ảnh eKYC được **tách khỏi `users`** sang `kyc/{uid}`. Như vậy mỗi lần đọc hồ sơ không phải tải ảnh, và document không chạm giới hạn 1 MB của Firestore.
- Field boolean đặt tên **không có tiền tố `is`** (ví dụ `biometricEnabled`), để Firestore map đúng với Kotlin. Đây là cách sửa bug lệch tên `isBiometricEnabled`.
- **Không có dữ liệu cũ**: collection `users` hiện đang trống (chủ dự án đã kiểm tra ngày 2026-10-26), nên không cần chuyển đổi dữ liệu.
- **Tạo officer đầu tiên**: app không có chức năng tự đăng ký officer. Kế hoạch phải có bước hướng dẫn tạo officer thủ công: tạo user trong Firebase Auth Console, rồi tạo document `users/{uid}` với `role = BANK_OFFICER`. Ghi bước này vào README để dùng lại khi demo hoặc chấm bài. Mọi khách hàng sau đó do officer tạo trong app.

## 7. Kiến trúc code **[Claude]**

- **Mỗi vai trò một Activity chính**: `CustomerMainActivity`, `OfficerMainActivity`. Mỗi Activity chứa bottom tab, mỗi tab là một **Fragment**.
- Màn hình phụ (tạo khách, chi tiết khách, eKYC, đổi mật khẩu…) là Fragment hoặc Activity tuỳ builder chọn, miễn back hoạt động đúng.
- Repository: tách `AuthRepository` (đăng nhập, phiên, tạo user qua app phụ) / `UserRepository` (hồ sơ, tìm kiếm) / `KycRepository` / `AccountRepository` (đợt này chỉ tạo Checking và đọc danh sách).
- **Mọi màn hình đi qua ViewModel**. Không Activity/Fragment nào gọi repository trực tiếp.
- Logic thuần (validator, định dạng tiền, OTP, cosine similarity, kiểm tra người thật) đặt ở class không phụ thuộc Android, để unit test được.
- `MainActivity`/`activity_main.xml` mặc định không còn dùng nữa thì xoá.

## 8. Kiểm thử **[Claude]**
- Unit test cho: validators, định dạng VND, `OtpManager` (hết hạn, dùng một lần, gắn giao dịch, khoá sau 5 lần sai), so khớp khuôn mặt (cosine + ngưỡng), các ViewModel chính (dùng repository giả).
- Kiểm tra thủ công trên máy thật: tạo khách → đăng nhập lần đầu → đổi mật khẩu → eKYC → officer duyệt → bật sinh trắc học → khoá/mở khoá → đổi SĐT qua OTP → kiểm tra khuôn mặt (người đúng qua, ảnh người khác bị từ chối).

## 9. Để builder tự quyết
- Cách dựng thanh tab có nút nổi ở giữa (BottomNavigationView + view đè, hay BottomAppBar).
- Tên và số cấp chính xác của thang chữ, tên style, cách tổ chức `styles.xml`.
- Nguồn model MobileFaceNet cụ thể (phải có giấy phép cho phép dùng), kích thước input, và **ngưỡng cosine**. Ngưỡng chọn bằng thử nghiệm và ghi lại lý do.
- Kích thước và chất lượng nén ảnh eKYC (vừa đủ để đối chiếu, vẫn nằm trong giới hạn document).
- Cách sinh số tài khoản (miễn là không trùng).
- Nội dung và hình minh hoạ của màn "Sắp ra mắt" và các empty state.
- Lý do từ chối eKYC: danh sách chọn sẵn, nhập tự do, hay cả hai.
