# FLUX Bank

Ứng dụng ngân hàng Android native (Kotlin, XML + ViewBinding, MVVM) dùng Firebase Authentication + Cloud Firestore (gói Spark miễn phí).

## Build & test

Gradle daemon được ghim JDK 21 trong `gradle/gradle-daemon-jvm.properties` (JRE 25 đi kèm extension Java của VSCode thiếu `jlink`).
Trong Android Studio chọn Gradle JDK = `/usr/lib/jvm/java-21-openjdk`.

```bash
./gradlew testDebugUnitTest   # unit test
./gradlew assembleDebug       # APK debug
```

## Thiết lập Firebase (làm một lần)

### 1. Deploy Security Rules
Firebase Console → **Firestore Database → Rules** → dán toàn bộ nội dung `firestore.rules` → **Publish**.
Publish lại mỗi khi file này đổi (Feature 4 thêm luật chuyển tiền và collection `accountIndex`; Feature 5 thêm loại giao dịch `PAYMENT`).

### 2. Tạo nhân viên (officer) đầu tiên
App không có chức năng tự đăng ký nhân viên; mọi khách hàng đều do nhân viên tạo trong app.

1. Console → **Authentication → Users → Add user**: nhập email và mật khẩu của nhân viên, rồi copy **User UID**.
2. Console → **Firestore Database** → collection `users` → **Add document** với Document ID = UID vừa copy, và các field (**gõ tay tên field**, đừng copy từ bảng dưới: trình xem markdown có thể chèn ký tự ẩn U+200B khiến app không đọc được field và coi nhân viên là khách):

| Field | Kiểu | Giá trị |
|---|---|---|
| `uid` | string | UID vừa copy |
| `email` | string | email nhân viên |
| `fullName` | string | họ tên nhân viên |
| `phoneNumber` | string | ví dụ `0900000001` |
| `nationalId` | string | 12 chữ số |
| `address` | string | địa chỉ |
| `role` | string | `BANK_OFFICER` |
| `kycStatus` | string | `VERIFIED` |
| `biometricEnabled` | boolean | `false` |
| `mustChangePassword` | boolean | `false` |
| `createdAt` | number | ví dụ `1793000000000` |

3. (Khuyến nghị) Collection `phoneIndex` → document ID = SĐT ở trên, field `uid` = UID nhân viên.

### 3. Tạo index cho lịch sử giao dịch (Feature 3)
Tab Lịch sử truy vấn `transactions` theo `ownerUid` và sắp theo `createdAt` giảm dần, nên cần một composite index (miễn phí):
Console → **Firestore Database → Indexes → Composite → Create index**: collection `transactions`, field `ownerUid` **Ascending**, `createdAt` **Descending**, query scope Collection. Chú ý chiều của `createdAt`: index Ascending không dùng được cho truy vấn này.
Cách khác: mở tab Lịch sử một lần, Logcat sẽ in lỗi `FAILED_PRECONDITION` kèm link tạo sẵn index; mở link và bấm Create. Index mất vài phút để chuyển sang Enabled.

### 4. Kiểm tra rules bằng Rules Playground
Trong tab Rules → **Rules Playground**, với một khách hàng đã đăng nhập:
- đọc `users/{uid khác}` → **bị chặn**
- sửa `fullName` của chính mình → **bị chặn**
- sửa `address` của chính mình → **được phép**

Với nhân viên: sửa `fullName` của khách → **được phép**.

Feature 3 (khách đã đăng nhập, `accounts/{id}` là tài khoản Checking của chính mình):
- update `accounts/{id}` chỉ đổi `balance` (không có giao dịch đi kèm) → **bị chặn**
- create `transactions/{tx}` mà `accounts/{id}.lastTxId` không trỏ về `tx` → **bị chặn**
- rút làm `balance` thành −1 → **bị chặn**
- ghi `config/rates` → **bị chặn** (nhân viên ghi mức trong (0, 30] → **được phép**)
- update `accounts/{id}` của khách khác → **bị chặn**

Feature 4: `get` một `accountIndex/{số TK}` → **được phép**; `list` collection `accountIndex` → **bị chặn**;
update `accounts/{id}` của người khác chỉ tăng `balance` (không có giao dịch cặp) → **bị chặn**.

Playground chỉ thử từng request đơn lẻ nên luồng hợp lệ (ghi tài khoản + giao dịch trong một batch) kiểm tra bằng app.

### 5. Test rules tự động (Firestore emulator)
`rules-test/` chạy 49 ca kiểm tra rules của Feature 3–5 trên emulator, gồm cả batch nạp/rút và chuyển tiền 4 document mà Playground không thử được. Cần Node và Java 21:
```bash
cd rules-test && npm install
ln -sfn ../templates node_modules/firebase-tools/lib/templates   # firebase-tools 13.29.1 tìm sai thư mục templates
npx firebase emulators:exec --only firestore --project demo-flux "node --test rules.test.mjs"
```

### 6. VNPay sandbox (Feature 4, nạp tiền)
1. Đăng ký merchant thử nghiệm tại https://sandbox.vnpayment.vn/devreg/ (miễn phí). VNPay gửi email có `vnp_TmnCode` và `vnp_HashSecret`.
2. Thêm hai dòng vào `local.properties` (file này không commit) rồi build lại:
   ```properties
   vnpay.tmn.code=XXXXXXXX
   vnpay.hash.secret=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   ```
   Thiếu khoá thì nút Nạp tiền báo "Chưa cấu hình VNPay".
3. Khi thanh toán trên trang VNPay, chọn thẻ nội địa và dùng thẻ test VNPay công bố (hướng dẫn plugin trên sandbox.vnpayment.vn):
   ngân hàng **NCB**, số thẻ `9704198526191432198`, chủ thẻ `NGUYEN VAN A`, ngày phát hành `07/15`, OTP `123456`.
   Danh sách đầy đủ (gồm thẻ cố ý lỗi: không đủ số dư, bị khoá…) ở https://sandbox.vnpayment.vn/apis/vnpay-demo/.

## Luồng demo
Nhân viên đăng nhập → tạo khách hàng (mật khẩu tạm) → khách đăng nhập, bắt buộc đổi mật khẩu → khách nộp eKYC → nhân viên duyệt → khách bật sinh trắc học, khoá/mở khoá app → đổi SĐT qua OTP → kiểm tra xác thực khuôn mặt.

Feature 3: nhân viên mở trong chi tiết khách một tài khoản Tiết kiệm và một khoản vay 1.500.000.000 ₫ / 240 tháng
→ khách nạp 2.000.000 ₫ vào Tiết kiệm (OTP), rồi 10.000.000 ₫ (OTP + khuôn mặt) → thử rút quá số dư (bị chặn)
→ nhân viên đổi lãi suất tiết kiệm sang 5% → khách thấy lãi/tháng mới → xem khoản vay, đổi giữa "Hàng tháng" và "2 tuần"
(13.017.349 ₫ / 6.003.662 ₫) → tab Lịch sử hiện các giao dịch theo ngày.

Feature 4 (hai khách A và B, B đã đăng nhập ít nhất một lần để số tài khoản có trong `accountIndex`):
A nạp 1.000.000 ₫ qua VNPay sandbox (thẻ NCB ở mục 6) → A chuyển 500.000 ₫ cho B theo số tài khoản (thấy tên B) → OTP → biên lai
→ B mở app: số dư tăng, Lịch sử có "Nhận từ …" → A chuyển 2.000.000 ₫ sang Vietcombank (giả lập) → A chuyển 15.000.000 ₫ (thêm bước quét mặt)
→ A thử vượt hạn mức 200.000.000 ₫/ngày (bị chặn trên form) → A mở "Mã của tôi", máy B quét bằng nút QR giữa thanh tab → form chuyển điền sẵn.

Feature 5 (tiện ích, dữ liệu giả lập trong app):
Tiền điện → EVN TP.HCM, mã `PE01000123456` → Tra cứu → Thanh toán → OTP → biên lai → tra lại cùng mã: "đã thanh toán", xem biên lai
→ thử mã đuôi `0000` ("Không tìm thấy hoá đơn") → Tiền nước (SAWACO, mã 8–12 số) → Nạp ĐT 100.000 ₫ cho SĐT của mình
→ Vé máy bay TP.HCM → Hà Nội, 2 khách → biên lai có mã đặt chỗ → Vé phim, Khách sạn, Mua sắm → tab Lịch sử có icon theo dịch vụ.

## Giới hạn đã biết
Gói Spark không có Cloud Functions nên OTP và đối chiếu khuôn mặt được kiểm tra trên thiết bị; Security Rules chỉ giới hạn ai được sửa field nào. Hướng nâng cấp: Cloud Functions phát/kiểm token xác thực trước khi ghi giao dịch.

Feature 4:
- Hạn mức 200.000.000 ₫/ngày chỉ kiểm ở app (rules không cộng dồn được); rules chặn mỗi lần > 100.000.000 ₫ và số dư âm.
- Chữ ký VNPay được kiểm trên máy và khoá sandbox nằm trong APK; rules không biết VNPay đã thu tiền hay chưa, nên khách gọi thẳng Firestore vẫn ghi được giao dịch DEPOSIT. Hướng nâng cấp: Cloud Functions nhận IPN của VNPay rồi mới cộng tiền.
- Số tài khoản chỉ tra được sau khi chủ tài khoản đăng nhập ít nhất một lần (app tự ghi `accountIndex`).
- Giao dịch "Nhận tiền" không có số dư sau giao dịch (người gửi không được đọc số dư người nhận).
- Chuyển liên ngân hàng là giả lập: tiền bị trừ, không ngân hàng nào nhận.

Feature 5:
- Nhà cung cấp, hoá đơn, chuyến bay, phim, khách sạn, sản phẩm đều là dữ liệu giả lập nằm trong app; không kết nối dịch vụ thật.
- Rules chỉ biết đây là một lần trừ tiền `PAYMENT` hợp lệ (đúng chủ, đúng số dư, ≤ 100.000.000 ₫), không biết số tiền hoá đơn hay giá vé đúng bao nhiêu. Khách sửa app có thể trả số khác, nhưng chỉ trừ tiền của chính mình và vẫn có bản ghi.
- Mỗi hoá đơn (nhà cung cấp + mã + kỳ) chỉ trả được một lần cho mỗi khách: id giao dịch suy ra từ hoá đơn nên rules chặn lần ghi thứ hai. Hai khách khác nhau vẫn trả được cùng một mã.
- Hạn mức 200.000.000 ₫/ngày tính chung chuyển tiền và thanh toán, chỉ kiểm ở app.
