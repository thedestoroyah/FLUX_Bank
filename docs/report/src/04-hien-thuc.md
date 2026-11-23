# HIỆN THỰC

Chương này nói về cách đưa thiết kế ở chương 3 thành code: cấu trúc thư mục, cách build và cấu hình, rồi từng tính năng với các lớp tham gia và một vài đoạn code tiêu biểu. Các đoạn code là trích nguyên văn từ mã nguồn, rút gọn phần không cần thiết.

## Cấu trúc mã nguồn

Ứng dụng có 148 file Kotlin (khoảng 11.900 dòng) trong `app/src/main`, 61 layout XML và 61 file kiểm thử (khoảng 4.900 dòng) trong `app/src/test`. Hai file riêng cho từng loại build nằm ở `app/src/debug` và `app/src/release` (mục "Build và cấu hình"). Code chia theo tầng như đã nói ở chương 2, rồi trong từng tầng chia theo tính năng. @tbl:packages liệt kê các package.

Table: Các package trong `com.flux.bank` {#tbl:packages}

| Package | Vai trò | Số file |
|------------------|----------------------------------------|---------|
| `ui/auth` | Đăng nhập, đổi mật khẩu, mở khoá, bottom sheet xác thực giao dịch | 9 |
| `ui/common` | `ScreenHost`, bàn phím số, ô nhập OTP, banner SMS giả lập | 10 |
| `ui/ekyc` | Chụp ảnh căn cước và khuôn mặt, nộp hồ sơ eKYC | 3 |
| `ui/customer` | Ứng dụng khách hàng: trang chủ, tài khoản, lịch sử, chuyển tiền, QR, tiện ích, chi nhánh, trợ lý, hồ sơ | 45 |
| `ui/officer` | Ứng dụng nhân viên: tổng quan, khách hàng, duyệt eKYC, lãi suất | 21 |
| `domain` | Quy tắc nghiệp vụ thuần Kotlin; thư mục con `utility`, `branch`, `assistant` | 21 |
| `data/model`, `data/repository` | Lớp dữ liệu và bảy repository (interface cùng cài đặt Firestore) | 15 |
| `data/assistant`, `data/location`, `data/routing` | Gemini, vị trí, đường đi bộ OSRM | 6 |
| `core/security` | `OtpManager`, `OtpNotifier`, `LockPolicy`, `AppLockObserver` | 5 |
| `core/format`, `core/validation` | Định dạng tiền và khoảng cách, bỏ dấu tiếng Việt, kiểm tra dữ liệu nhập | 4 |
| `face` | `FaceMatcher`, `FaceEmbedder`, `FaceDetectorHelper`, `BlinkDetector` | 4 |
| `payment`, `utils` | `VnPay`, phiên đăng nhập, sinh trắc học, xử lý ảnh | 4 |
| `FluxApp.kt` | Composition root | 1 |

Mô hình MobileFaceNet nằm ở `app/src/main/assets/mobilefacenet.tflite` (khoảng 5 MB). Chữ hiển thị đều nằm trong `res/values/strings.xml`; màu, khoảng cách và kiểu chữ nằm trong `res/values/` và là nguồn gốc của design system ở chương 5.

## Build và cấu hình

Dự án build bằng Gradle 9.6.0 với Android Gradle Plugin 9.4.1, `compileSdk` và `targetSdk` 37, `minSdk` 26, và JDK 21 (ghim trong `gradle/gradle-daemon-jvm.properties`). Các thư viện nằm trong `gradle/libs.versions.toml` và `app/build.gradle.kts`; toàn bộ thư viện Firebase lấy phiên bản từ một BoM duy nhất (34.19.0) để các phần của Firebase luôn tương thích nhau.

Những giá trị không được đưa vào git nằm trong `local.properties`, file này đã được `.gitignore` loại. Tôi chỉ ghi tên khoá, không ghi giá trị:

- `vnpay.tmn.code`, `vnpay.hash.secret`: thông tin cổng VNPay sandbox. `app/build.gradle.kts` đọc chúng và đưa vào `BuildConfig`. Thiếu thì `FluxApp.vnPay` là `null` và màn nạp tiền báo chưa cấu hình VNPay.
- `release.store.file`, `release.store.password`, `release.key.alias`, `release.key.password`: khoá upload để ký bản phát hành. Thiếu thì chỉ các task đóng gói bản release báo lỗi; bản debug và kiểm thử không bị ảnh hưởng.

App Check có hai bản cài đặt cùng tên hàm `installAppCheck(app)` đặt ở `app/src/debug` và `app/src/release`. Bản debug dùng debug provider, bản release dùng Play Integrity. Thư viện `firebase-appcheck-debug` chỉ có ở cấu hình `debugImplementation`, nên bản release không chứa debug provider. `FluxApp.onCreate` gọi `installAppCheck(this)` mà không cần biết đang là bản nào.

## Feature 1: Hồ sơ người dùng

Đề yêu cầu hai loại người dùng với giao diện và quyền khác nhau: nhân viên tạo tài khoản và sửa dữ liệu khách theo yêu cầu, khách xem và sửa thông tin của mình.

Nhân viên tạo khách ở `CreateCustomerFragment`, logic nằm trong `CustomerOnboardingService`. Điểm khó của việc này là tạo tài khoản đăng nhập mới mà không làm nhân viên bị đăng xuất, vì Firebase Authentication mặc định đăng nhập luôn tài khoản vừa tạo. Cách giải quyết là `FirebaseAuthRepository.createAuthUser` tạo tài khoản qua một `FirebaseApp` phụ có phiên đăng nhập riêng, nên phiên của nhân viên không bị đụng tới. Hàm `create` làm ba việc nối tiếp (tạo tài khoản đăng nhập, ghi hồ sơ, cấp tài khoản Checking) và hoàn tác phần đã làm nếu một bước sau lỗi:

```kotlin
val uid = auth.createAuthUser(input.email.trim(), input.tempPassword)
    .getOrElse { return fail(OnboardingError.Failed(it)) }
val user = User(
    uid = uid, email = input.email.trim(), fullName = input.fullName.trim(),
    phoneNumber = phone, nationalId = nationalId, address = input.address.trim(),
    role = UserRole.CUSTOMER, mustChangePassword = true, createdBy = officerUid()
)
users.createProfile(user).onFailure { return rollback(it, profileWritten = false, uid) }
accounts.createChecking(uid).onFailure { return rollback(it, profileWritten = true, uid) }
auth.releaseCreatedAuthUser()
return Result.success(user)
```

Mật khẩu của khách là mật khẩu tạm, và `mustChangePassword = true` buộc đổi ở lần đăng nhập đầu (`ChangePasswordActivity`). Trước đó, `validate` và `duplicateError` kiểm tra định dạng email, số điện thoại, số căn cước, độ mạnh mật khẩu và trùng số điện thoại hoặc căn cước. Tìm khách (`CustomerListViewModel`) tải danh sách khách rồi lọc ở phía máy, có bỏ dấu tiếng Việt (`TextNormalizer`), là đủ cho quy mô đồ án.

Khách xem hồ sơ ở `ProfileFragment` và tự sửa địa chỉ, ảnh đại diện (`EditProfileFragment`). Đổi số điện thoại là thao tác nhạy cảm vì số điện thoại là khoá tra người nhận, nên phải qua OTP; `UserRepository.changePhone` đổi số trong `users` và `phoneIndex` cùng lúc. Security Rules chỉ cho khách sửa các trường cố định (số điện thoại, địa chỉ, ảnh, cờ sinh trắc học, `mustChangePassword` và việc nộp eKYC), còn mọi trường khác chỉ nhân viên sửa được.

## Feature 2: Bảo mật

Đề yêu cầu màn hình đăng nhập, eKYC bằng sinh trắc học, xác thực khuôn mặt khi giao dịch giá trị lớn và xác thực hai yếu tố cho mọi giao dịch. Phần này gồm bốn cơ chế.

### Khoá ứng dụng bằng sinh trắc học

Khi người dùng bật, `AppLockObserver` khoá ứng dụng lúc mở và lúc quay lại sau một khoảng ở nền. Quyết định khoá nằm trong `LockPolicy`, thuần Kotlin nên kiểm thử được:

```kotlin
fun onForeground(
    backgroundedAtMs: Long?, nowMs: Long,
    hasSession: Boolean, biometricEnabled: Boolean, biometricAvailable: Boolean
): Boolean {
    if (backgroundedAtMs == null) return false
    return onColdStart(hasSession, biometricEnabled, biometricAvailable) &&
        nowMs - backgroundedAtMs >= timeoutMs
}
```

### eKYC

Khách (hoặc nhân viên chụp hộ) chụp căn cước và ảnh khuôn mặt bằng CameraX. `EkycViewModel` yêu cầu ảnh khuôn mặt có đúng một khuôn mặt (ML Kit) rồi tính vector 192 chiều bằng `FaceEmbedder`. Hồ sơ gồm hai ảnh Base64 và vector, ghi vào `kyc/{uid}` cùng lúc với việc chuyển `kycStatus` sang PENDING. Nhân viên xem hồ sơ ở `EkycReviewDetailFragment` và duyệt hoặc từ chối kèm lý do; chỉ hồ sơ VERIFIED mới được giao dịch.

### OTP

`OtpManager` sinh mã 6 số bằng `SecureRandom`. Mã có hiệu lực 120 giây, dùng một lần, gắn với một giao dịch và khoá sau 5 lần sai (@fig:state-otp). Vì gói Spark không gửi được SMS thật, `OtpNotifier` hiện mã bằng một thông báo giả lập SMS; chạm vào thông báo thì mã được điền vào ô nhập. Nếu người dùng không cấp quyền thông báo, mã hiện ở banner trong ứng dụng. Phần kiểm tra mã:

```kotlin
fun verify(transactionId: String, code: String): OtpResult {
    val expected = activeCode ?: return OtpResult.NO_ACTIVE
    if (wrongAttempts >= MAX_ATTEMPTS) return OtpResult.LOCKED
    if (now() > expiresAt) { clear(); return OtpResult.EXPIRED }
    if (transactionId != activeTransactionId || code.trim() != expected) {
        wrongAttempts++
        return OtpResult.WRONG
    }
    clear()
    return OtpResult.VALID
}
```

### Xác thực khuôn mặt khi giao dịch lớn

`TransactionAuthSheet` là bottom sheet dùng chung cho mọi giao dịch. Từ 10.000.000 ₫ (`FaceVerificationPolicy.HIGH_VALUE_THRESHOLD`), sau OTP có thêm bước quét mặt: `BlinkDetector` yêu cầu người dùng chớp mắt để biết đang có người thật, rồi `FaceEmbedder` tính vector của khuôn mặt hiện tại và `FaceMatcher` so với vector lưu lúc eKYC bằng độ tương đồng cosine:

```kotlin
fun cosine(a: FloatArray, b: FloatArray): Float {
    require(a.size == b.size) { "Embedding khác độ dài: ${a.size} vs ${b.size}" }
    var dot = 0f; var normA = 0f; var normB = 0f
    for (i in a.indices) {
        dot += a[i] * b[i]; normA += a[i] * a[i]; normB += b[i] * b[i]
    }
    require(normA > 0f && normB > 0f) { "Embedding rỗng (vector 0)" }
    return dot / (sqrt(normA) * sqrt(normB))
}
```

Ngưỡng `MATCH_THRESHOLD` là 0,6. Ngưỡng này suy từ kho mã nguồn gốc của mô hình rồi được kiểm tra trên điện thoại thật ngày 27/10/2026: so với ảnh eKYC, cùng một người đạt 0,716 đến 0,733 (hai lần đo còn lưu), người khác đạt từ −0,191 đến 0,007 (sáu lần đo). Hai nhóm cách xa nhau nên tôi giữ 0,6; mẫu đo nhỏ, đủ cho đồ án nhưng chưa đủ để nói về tỉ lệ nhận nhầm của một hệ thống thật. Số đo nằm trong KDoc của `FaceMatcher`. Các feature sau chỉ cần gọi `TransactionAuthSheet.show(...)` rồi lắng nghe kết quả, không phải biết gì về OTP hay khuôn mặt.

## Feature 3: Quản lý tài khoản

Đề yêu cầu ba loại tài khoản với giao diện khác nhau, lãi suất và lãi mỗi tháng của tiết kiệm, tiền phải trả mỗi tháng hoặc hai tuần của khoản vay, lịch sử giao dịch, nạp và rút tiền, và nhân viên chỉnh lãi suất.

`AccountDetailFragment` hiển thị một trong ba bố cục theo `AccountType`: Checking và Saving có số dư và nút nạp, rút; Saving thêm lãi suất chung và lãi dự kiến mỗi tháng; Mortgage có dư nợ, tiền trả mỗi kỳ, kỳ trả kế tiếp và tổng lãi. Công thức nằm trong `InterestCalculator`. Tiền trả mỗi kỳ của khoản vay dùng công thức niên kim, với lãi suất mỗi kỳ là lãi năm chia cho 12 (trả hàng tháng) hoặc 26 (hai tuần một lần):

```kotlin
fun installment(principal: Long, annualRatePercent: Double, termMonths: Int, frequency: PaymentFrequency): Long {
    val n = periods(termMonths, frequency)
    if (annualRatePercent == 0.0) return ceil(principal.toDouble() / n).toLong()
    val perYear = if (frequency == PaymentFrequency.MONTHLY) MONTHS_PER_YEAR else BIWEEKS_PER_YEAR
    val r = annualRatePercent / 100 / perYear
    return ceil(principal * r / (1 - (1 + r).pow(-n))).toLong()
}
```

Lãi của tài khoản tiết kiệm chỉ là lãi dự kiến, không được cộng vào số dư; đây là một giới hạn đã chọn để giữ phạm vi vừa sức, được ghi ở chương 6. Nhân viên chỉnh lãi suất ở tab Lãi suất (`RatesFragment`), ghi vào `config/rates` kèm người sửa và thời điểm, và mở tài khoản Saving hoặc Mortgage cho khách ở `OpenAccountFragment` (có xem trước tiền trả mỗi kỳ).

Nạp và rút tiền đi qua `MoneyMoveFragment` và `AccountRepository`. Nạp và rút dùng chung hàm `move`; chuyển tiền và thanh toán dùng hàm `debit`. Cả hai chạy trong một Firestore transaction: đọc tài khoản, kiểm tra số dư rồi ghi số dư mới cùng giao dịch. Lõi của `move`:

```kotlin
val account = t.get(accountRef).toObject(BankAccount::class.java) ?: error("Không tìm thấy tài khoản")
check(account.type != AccountType.MORTGAGE) { "Không nạp/rút trên khoản vay" }
val newBalance = if (type.isIncoming) account.balance + amount else account.balance - amount
if (newBalance < 0) throw InsufficientFundsException()
val tx = Transaction(id = txRef.id, accountId = accountId, ownerUid = account.ownerUid,
    accountNumber = account.accountNumber, type = type, amount = amount, balanceAfter = newBalance,
    paymentRef = paymentRef)
t.update(accountRef, mapOf("balance" to newBalance, "lastTxId" to txRef.id))
t.set(txRef, tx)
```

Vì đọc và ghi nằm trong cùng transaction, nếu số dư đã đổi từ máy khác sau khi người dùng xem trước thì Firestore tự thử lại với số dư mới, và `InsufficientFundsException` xuất hiện nếu lúc đó không còn đủ tiền. Tab Lịch sử (`HistoryFragment`) đọc 100 giao dịch mới nhất, nhóm theo ngày và lọc theo tài khoản; chạm vào một dòng thì mở biên lai (`ReceiptFragment`).

## Feature 4: Giao dịch và cổng thanh toán

Đề yêu cầu giao dịch phải qua bảo mật ở Feature 2, hệ thống kiểm tra giao dịch hợp lệ trước khi ghi, chuyển tiền cùng ngân hàng và khác ngân hàng, và tích hợp VNPay hoặc Stripe.

Tab Chuyển tiền có ba kiểu: chuyển cho tài khoản FLUX khác (tra tên người nhận qua `accountIndex`), chuyển giữa tài khoản của chính mình, và chuyển liên ngân hàng (giả lập với 10 ngân hàng trong `Banks`). Trước khi ghi, `TransferPolicy.checkAmount` kiểm tra mức tối thiểu, mức tối đa mỗi lần, số dư và hạn mức ngày:

```kotlin
fun checkAmount(amount: Long, balance: Long, remainingToday: Long): TransferError? = when {
    amount < Validators.MIN_AMOUNT -> TransferError.BELOW_MIN
    amount > MAX_PER_TRANSFER -> TransferError.ABOVE_MAX
    amount > balance -> TransferError.INSUFFICIENT_FUNDS
    amount > remainingToday -> TransferError.DAILY_LIMIT
    else -> null
}
```

Mức tối thiểu là 10.000 ₫, tối đa 100.000.000 ₫ mỗi lần và 200.000.000 ₫ mỗi ngày. Phần còn lại của hạn mức ngày (`remainingToday`) tính từ lịch sử, theo giờ Việt Nam. Ghi chuyển nội bộ là một Firestore transaction bốn thao tác như đã mô tả ở @fig:seq-transfer; Security Rules bắt buộc bên nhận chỉ được cộng tiền khi có lệnh `TRANSFER_OUT` đi kèm. Chuyển liên ngân hàng chỉ ghi một giao dịch `TRANSFER_OUT` vì không có tài khoản đích trong hệ thống.

Nút QR nằm giữa thanh tab: quét mã `fluxbank:<số tài khoản>` bằng ML Kit Barcode Scanning để điền sẵn số tài khoản nhận, hoặc hiện "Mã của tôi" (sinh bằng ZXing) để người khác quét.

Nạp tiền vào tài khoản chỉ đi qua VNPay sandbox. Ứng dụng kiểm tra kết quả do VNPay trả về trước khi cộng tiền: chữ ký phải đúng, mã giao dịch và số tiền phải khớp. Số tiền VNPay tính theo đơn vị nhỏ nhất nên nhân với 100:

```kotlin
val received = query["vnp_SecureHash"] ?: return VnPayResult.BadSignature
val signed = query.filterKeys { it.startsWith("vnp_") }
if (!sign(signed, config.hashSecret).equals(received, ignoreCase = true)) return VnPayResult.BadSignature
if (query["vnp_TxnRef"] != expectedTxnRef || query["vnp_Amount"] != (expectedAmount * 100).toString()) {
    return VnPayResult.Mismatch
}
```

Mã giao dịch `vnp_TxnRef` chính là id của giao dịch `DEPOSIT` sẽ được ghi, nên một kết quả không thể bị cộng tiền hai lần. Vì khoá bí mật VNPay nằm trong APK, chữ ký chỉ được kiểm trên máy, và Security Rules không biết VNPay đã thu tiền hay chưa. Đây là giới hạn của kiến trúc không có máy chủ, tôi ghi rõ ở chương 6; hướng khắc phục là kiểm kết quả ở một Cloud Function.

## Feature 5: Tiện ích

Đề yêu cầu trả tiền điện, nước, nạp tiền điện thoại, mua vé máy bay, vé xem phim, đặt phòng khách sạn và thanh toán mua hàng. Bảy ô tiện ích dùng ba kiểu màn:

- Hoá đơn (`BillPayFragment`) cho tiền điện và nước: tra hoá đơn theo nhà cung cấp, mã khách hàng và kỳ.
- Nạp điện thoại (`TopUpFragment`): nhà mạng nhận diện theo đầu số, sáu mệnh giá.
- Danh mục (`CatalogFragment`) cho vé máy bay, vé phim, khách sạn và mua sắm: chọn mục, số lượng, ngày; biên lai có mã đặt chỗ.

Dữ liệu hoá đơn, nhà mạng và danh mục là dữ liệu giả lập thuần Kotlin trong `domain/utility/`, không lưu Firestore. Mọi khoản thanh toán là một giao dịch `PAYMENT` ghi qua `AccountRepository.pay`, dùng lại hàm `debit` của chuyển tiền. Điểm đáng nói là cách chặn trả một hoá đơn hai lần: id giao dịch của hoá đơn được dựng cố định từ người trả, nhà cung cấp, mã khách hàng và kỳ:

```kotlin
fun txIdFor(uid: String, bill: Bill): String =
    "bill_${uid}_${bill.provider.name}_${bill.customerCode}_${bill.period.format(PERIOD_KEY)}"
```

Vì Security Rules không cho tạo giao dịch trùng id, lần trả thứ hai cho cùng một hoá đơn bị từ chối, và `AccountRepository.pay` đổi lỗi đó thành `AlreadyPaidException` để màn hình hiện "đã thanh toán". Rules cũng chỉ cho phép tiền tố `bill_<uid>_` với `uid` của chính người ghi, để không ai chiếm trước mã hoá đơn của người khác. Hạn mức ngày tính chung cho chuyển tiền và thanh toán. Ba màn dùng chung `ServicePaymentViewModel` (chọn nguồn tiền, kiểm hạn mức và eKYC, mở sheet xác thực đúng một lần) và `PaymentUi`, nên thêm một tiện ích mới chỉ cần thêm phần dựng `ServicePayment`.

## Feature 6: Chi nhánh và đường đi bộ

Đề yêu cầu xác định vị trí người dùng, hiển thị bản đồ với các chi nhánh gần đó và gợi ý đường đi bộ ngắn nhất tới chi nhánh gần nhất. Chữ "ngắn nhất" quan trọng: chi nhánh gần nhất theo đường thẳng chưa chắc là chi nhánh đi bộ ngắn nhất, vì có thể phải đi vòng qua cầu hay đường một chiều.

`BranchMapFragment` hiện bản đồ OSMDroid với nền OpenStreetMap, ghim chi nhánh, chấm vị trí người dùng và đường đi, kèm một bottom sheet cho gợi ý và danh sách. Vị trí lấy từ `FusedLocationSource` (chờ tối đa 15 giây, xử lý các trường hợp từ chối quyền và tắt định vị). Có 12 chi nhánh giả lập trong `Branches`, tập trung quanh cổng Trường Đại học Tôn Đức Thắng để dễ thử. `BranchRecommender` chọn ba chi nhánh gần nhất theo đường thẳng, gọi OSRM song song cho từng chi nhánh, rồi chọn chi nhánh có quãng đi bộ ngắn nhất:

```kotlin
val candidates = byStraight.take(CANDIDATES)
val routed = coroutineScope {
    candidates.map { c -> async { c.copy(walk = routeOrNull(user, c.branch)) } }.awaitAll()
}.filter { it.walk != null }.sortedBy { it.walk!!.distanceMeters }
if (routed.isEmpty()) return Recommendation(byStraight, nearest, nearest, routed = false)
val routedIds = routed.map { it.branch.id }.toSet()
val ranked = routed + byStraight.filter { it.branch.id !in routedIds }
return Recommendation(ranked, routed.first(), nearest, routed = true)
```

Nếu OSRM không trả lời (mất mạng, máy chủ công cộng bận) thì không có chi nhánh nào được định tuyến, và ứng dụng dự phòng bằng đường thẳng (`routed = false`) thay vì báo lỗi trắng. Nếu người dùng ở xa hơn 20 km thì không gọi OSRM. Khi chi nhánh được chọn khác chi nhánh gần nhất theo đường thẳng, giao diện hiện thêm một dòng giải thích, ví dụ chi nhánh gần hơn theo đường chim bay nhưng phải đi vòng. Toạ độ của người dùng được gửi tới máy chủ OSRM công cộng; đây là điều phải khai báo khi lên Google Play.

## Feature 7: Trợ lý AI

Đề yêu cầu một chatbot trả lời câu hỏi của người dùng, kèm liên kết tới các hoạt động được hỏi: thanh toán, xem giao dịch của tháng và các việc tài chính khác.

`AssistantFragment` là màn chat. Mỗi câu trả lời kèm tối đa ba nút; chạm nút thì mở đúng màn của các Feature 3 đến 6 (`AssistantAction` có 13 giá trị). `AssistantViewModel` điều phối cả đường AI lẫn đường dự phòng, như @fig:seq-assistant. Phần lõi là hàm `answer`, đặt một hạn chung 20 giây cho cả việc tải số liệu và việc gọi AI, và rơi về chế độ cơ bản khi có lỗi:

```kotlin
return try {
    withTimeout(timeoutMs) {
        snapshot = withTimeoutOrNull(SNAPSHOT_TIMEOUT_MS) { loadSnapshot() }
        AssistantReplyParser.parse(client.ask(AssistantPrompt.system(snapshot), turnsOf(history)))
    }
} catch (e: Exception) {
    currentCoroutineContext().ensureActive()
    onError(e)
    AssistantFallback.reply(question, snapshot)
}
```

`GeminiAssistantClient` gọi Gemini qua Firebase AI Logic với `responseMimeType` là JSON và một `responseSchema` buộc câu trả lời có dạng `{reply, actions}`, nên `AssistantReplyParser` đọc được chắc chắn và bỏ các nút lạ hoặc trùng. Model là `gemini-3.5-flash-lite`: tôi thử `gemini-3.5-flash` nhưng gói miễn phí chỉ cho 20 lượt mỗi ngày cho cả dự án, còn `gemini-3.8-flash` hay báo quá tải. `AssistantPrompt` đặt các quy tắc cho bot: chỉ nói về FLUX, không bịa số liệu, không hỏi mật khẩu hay OTP, và không tự thực hiện giao dịch. Các con số hạn mức trong lời bot lấy từ hằng số trong code nên khi hằng số đổi thì lời bot đổi theo. Giờ mở cửa chi nhánh thì hiện viết thẳng trong prompt, chưa lấy từ `OpeningHours`; đây là một lỗi nhỏ đã ghi nhận.

Dữ liệu gửi cho AI là bản tóm tắt `FinanceSnapshot`: số dư theo loại tài khoản, tổng thu chi và chi theo nhóm của tháng, hạn mức còn lại. Số tài khoản, số điện thoại, căn cước và tên người nhận không nằm trong đó. Chế độ cơ bản (`AssistantFallback`) so từ khoá sau khi bỏ dấu và vẫn mở được đúng màn khi không có mạng hay hết lượt miễn phí. Vì gói miễn phí của Google có thể dùng dữ liệu để cải thiện sản phẩm, điều này được ghi trong chính sách bảo mật (`docs/play/privacy-policy.md`).

## Bảo mật tổng hợp

Bảo mật của ứng dụng gồm nhiều lớp chồng lên nhau. @tbl:security-layers tóm lại từng lớp bảo vệ điều gì và giới hạn của nó.

Table: Các lớp bảo mật {#tbl:security-layers}

| Lớp | Bảo vệ điều gì | Giới hạn |
|-----------|-------------------------------|-------------------------------|
| Firebase Authentication | Chỉ người có tài khoản mới vào được; mật khẩu tạm buộc đổi | Phụ thuộc mật khẩu người dùng đặt |
| Security Rules | Phân quyền theo vai trò; mỗi lần đổi số dư phải có giao dịch khớp; không sửa hay xoá giao dịch | Không biết OTP, khuôn mặt, VNPay đã thu tiền hay chưa |
| OTP cho mọi giao dịch | Giao dịch đúng với người đang cầm máy | Kiểm tra trên máy, người sửa APK có thể bỏ qua |
| Quét mặt từ 10.000.000 ₫ | Ngăn người khác dùng máy đang mở khoá để chuyển khoản lớn | So khớp trên máy; chớp mắt chỉ chống ảnh tĩnh, không chống video |
| Khoá sinh trắc học | Ngăn người khác mở app khi máy bị bỏ quên | Chỉ khi người dùng bật |
| App Check | Chặn ứng dụng giả gọi Gemini bằng dự án của tôi | Chưa bật bắt buộc |
| Dữ liệu gửi ra ngoài | Chỉ bản tóm tắt cho Gemini; toạ độ cho OSRM; tile cho OpenStreetMap | Dịch vụ miễn phí có thể dùng dữ liệu cải thiện sản phẩm |

Các lớp chạy trên máy (OTP, quét mặt, khoá sinh trắc học) chống người dùng thường, còn lớp chạy ở máy chủ (Security Rules) chống thao tác làm sai lệch dữ liệu. Muốn chống cả người sửa APK thì phải đưa việc cấp và kiểm token xác thực sang Cloud Functions, điều gói Spark không cho phép.
