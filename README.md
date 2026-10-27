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
Console → **Firestore Database → Indexes → Composite → Create index**: collection `transactions`, field `ownerUid` **Ascending**, `createdAt` **Descending**, query scope Collection.
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

Playground chỉ thử từng request đơn lẻ nên luồng hợp lệ (ghi tài khoản + giao dịch trong một batch) kiểm tra bằng app.

### 5. Test rules tự động (Firestore emulator)
`rules-test/` chạy 20 ca kiểm tra rules của Feature 3 trên emulator, gồm cả batch nạp/rút hợp lệ mà Playground không thử được. Cần Node và Java 21:
```bash
cd rules-test && npm install
ln -sfn ../templates node_modules/firebase-tools/lib/templates   # firebase-tools 13.29.1 tìm sai thư mục templates
npx firebase emulators:exec --only firestore --project demo-flux "node --test rules.test.mjs"
```

## Luồng demo
Nhân viên đăng nhập → tạo khách hàng (mật khẩu tạm) → khách đăng nhập, bắt buộc đổi mật khẩu → khách nộp eKYC → nhân viên duyệt → khách bật sinh trắc học, khoá/mở khoá app → đổi SĐT qua OTP → kiểm tra xác thực khuôn mặt.

Feature 3: nhân viên mở trong chi tiết khách một tài khoản Tiết kiệm và một khoản vay 1.500.000.000 ₫ / 240 tháng
→ khách nạp 2.000.000 ₫ vào Tiết kiệm (OTP), rồi 10.000.000 ₫ (OTP + khuôn mặt) → thử rút quá số dư (bị chặn)
→ nhân viên đổi lãi suất tiết kiệm sang 5% → khách thấy lãi/tháng mới → xem khoản vay, đổi giữa "Hàng tháng" và "2 tuần"
(13.017.349 ₫ / 6.003.662 ₫) → tab Lịch sử hiện các giao dịch theo ngày.

## Giới hạn đã biết
Gói Spark không có Cloud Functions nên OTP và đối chiếu khuôn mặt được kiểm tra trên thiết bị; Security Rules chỉ giới hạn ai được sửa field nào. Hướng nâng cấp: Cloud Functions phát/kiểm token xác thực trước khi ghi giao dịch.
