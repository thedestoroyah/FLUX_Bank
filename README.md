# FLUX Bank

Ứng dụng ngân hàng Android native (Kotlin, XML + ViewBinding, MVVM) dùng Firebase Authentication + Cloud Firestore (gói Spark miễn phí).

## Build & test

Gradle cần JDK đầy đủ (JRE đi kèm extension VSCode thiếu `jlink`):

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew testDebugUnitTest   # unit test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleDebug       # APK debug
```

## Thiết lập Firebase (làm một lần)

### 1. Deploy Security Rules
Firebase Console → **Firestore Database → Rules** → dán toàn bộ nội dung `firestore.rules` → **Publish**.

### 2. Tạo nhân viên (officer) đầu tiên
App không có chức năng tự đăng ký nhân viên; mọi khách hàng đều do nhân viên tạo trong app.

1. Console → **Authentication → Users → Add user**: nhập email và mật khẩu của nhân viên, rồi copy **User UID**.
2. Console → **Firestore Database** → collection `users` → **Add document** với Document ID = UID vừa copy, và các field:

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

### 3. Kiểm tra rules bằng Rules Playground
Trong tab Rules → **Rules Playground**, với một khách hàng đã đăng nhập:
- đọc `users/{uid khác}` → **bị chặn**
- sửa `fullName` của chính mình → **bị chặn**
- sửa `address` của chính mình → **được phép**

Với nhân viên: sửa `fullName` của khách → **được phép**.

## Luồng demo
Nhân viên đăng nhập → tạo khách hàng (mật khẩu tạm) → khách đăng nhập, bắt buộc đổi mật khẩu → khách nộp eKYC → nhân viên duyệt → khách bật sinh trắc học, khoá/mở khoá app → đổi SĐT qua OTP → kiểm tra xác thực khuôn mặt.

## Giới hạn đã biết
Gói Spark không có Cloud Functions nên OTP và đối chiếu khuôn mặt được kiểm tra trên thiết bị; Security Rules chỉ giới hạn ai được sửa field nào. Hướng nâng cấp: Cloud Functions phát/kiểm token xác thực trước khi ghi giao dịch.
