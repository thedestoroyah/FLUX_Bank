# PHÂN TÍCH VÀ THIẾT KẾ

Chương này mô tả hệ thống ở mức phân tích: ai dùng ứng dụng và làm được gì (use case), các lớp trong code và quan hệ giữa chúng (class diagram), các luồng xử lý quan trọng (sơ đồ tuần tự) và cách tổ chức dữ liệu trong Firestore. Các sơ đồ được vẽ bằng PlantUML [19] từ file văn bản trong `docs/report/diagrams/`, nên có thể dựng lại khi code đổi.

## Tác nhân và use case

### Tác nhân

Hệ thống có hai tác nhân là người dùng và ba hệ thống bên ngoài.

- Khách hàng dùng ứng dụng khách hàng: xem tài khoản, nạp, rút, chuyển tiền, thanh toán tiện ích, tìm chi nhánh, hỏi trợ lý. Khách hàng không tự đăng ký; tài khoản do nhân viên tạo, đúng như đề yêu cầu.
- Nhân viên ngân hàng dùng ứng dụng nhân viên: tạo và sửa hồ sơ khách, chụp và duyệt eKYC, mở tài khoản tiết kiệm hoặc khoản vay, chỉnh lãi suất.
- VNPay là cổng thanh toán nhận tiền nạp, Gemini trả lời câu hỏi của trợ lý, OSRM tính đường đi bộ. Ba hệ thống này không do tôi điều khiển và có thể không sẵn sàng, nên mỗi chỗ đều có cách xử lý khi lỗi (ví dụ chế độ cơ bản của trợ lý, đường thẳng thay cho đường đi bộ).

Hai loại người dùng cùng đăng nhập bằng một màn hình. Sau đó `RoleRouter` đưa họ vào hai giao diện khác nhau. @fig:uc-overview là bản tổng quan, @fig:uc-customer và @fig:uc-officer chi tiết từng vai trò.

![Use case tổng quát](diagrams/usecase-overview.png){#fig:uc-overview width=12cm}

![Use case của khách hàng](diagrams/usecase-customer.png){#fig:uc-customer width=15.5cm}

Trong @fig:uc-customer, mọi thao tác làm đổi tiền (nạp, rút, chuyển, thanh toán) cùng đi qua use case "Xác thực giao dịch": người dùng nhập mã OTP, và khi số tiền từ 10.000.000 ₫ trở lên thì có thêm bước quét mặt. Sửa số điện thoại cũng cần OTP vì số điện thoại là khoá tra người nhận. Quét mã QR chỉ để điền sẵn số tài khoản nhận tiền, nên use case này mở rộng use case chuyển tiền.

![Use case của nhân viên](diagrams/usecase-officer.png){#fig:uc-officer width=13cm}

Ở @fig:uc-officer, các thao tác sửa khách, gửi email đặt lại mật khẩu và mở tài khoản đều bắt đầu từ màn chi tiết khách hàng, nên chúng mở rộng use case "Tìm, xem khách hàng".

### Đặc tả các use case chính

Sáu use case sau là những luồng barem đòi hỏi, và cũng là những luồng nhiều ràng buộc nhất. Mỗi bảng ghi tên, tác nhân, điều kiện trước, luồng chính, luồng phụ và kết quả.

Table: Đặc tả use case "Đăng nhập và mở khoá sinh trắc" {#tbl:uc-login}

| Mục | Nội dung |
|---------|----------------------------------------------|
| Tác nhân | Khách hàng, nhân viên |
| Điều kiện trước | Tài khoản đã được nhân viên tạo |
| Luồng chính | 1. Người dùng nhập email và mật khẩu. |
| | 2. Firebase Authentication xác thực. |
| | 3. Ứng dụng đọc `users/{uid}` và `RoleRouter` mở `CustomerMainActivity` hoặc `OfficerMainActivity` theo `role`. |
| | 4. Nếu đã bật sinh trắc học, mỗi lần mở app và mỗi lần quay lại sau từ 60 giây ở nền, `AppLockObserver` mở `UnlockActivity` để xác thực bằng vân tay hoặc khuôn mặt của máy. |
| Luồng phụ | Lần đăng nhập đầu, `mustChangePassword` là true nên người dùng phải đổi mật khẩu trước khi vào. Sai email hoặc mật khẩu thì báo lỗi và ở lại màn đăng nhập. Máy không hỗ trợ sinh trắc học thì không bật khoá. |
| Kết quả | Có phiên đăng nhập; app tự khoá khi cần |

Table: Đặc tả use case "Tạo khách hàng" {#tbl:uc-create}

| Mục | Nội dung |
|---------|----------------------------------------------|
| Tác nhân | Nhân viên |
| Điều kiện trước | Nhân viên đã đăng nhập |
| Luồng chính | 1. Nhân viên nhập họ tên, email, số điện thoại, số căn cước, địa chỉ và mật khẩu tạm. |
| | 2. `CustomerOnboardingService.create` kiểm tra định dạng, kiểm tra trùng số điện thoại và căn cước. |
| | 3. Dịch vụ tạo tài khoản đăng nhập qua một `FirebaseApp` phụ để nhân viên không bị đăng xuất. |
| | 4. Dịch vụ ghi hồ sơ, `phoneIndex` và cấp sẵn một tài khoản Checking số dư 0 ₫. |
| Luồng phụ | Dữ liệu sai định dạng hoặc trùng thì trả `OnboardingError` tương ứng, không tạo gì. Lỗi giữa chừng thì xoá các phần đã tạo (rollback). |
| Kết quả | Khách mới có `mustChangePassword = true` và một tài khoản Checking |

Table: Đặc tả use case "Nộp và duyệt eKYC" {#tbl:uc-ekyc}

| Mục | Nội dung |
|---------|----------------------------------------------|
| Tác nhân | Khách hàng (hoặc nhân viên chụp hộ), nhân viên duyệt |
| Điều kiện trước | `kycStatus` là NOT_SUBMITTED hoặc REJECTED |
| Luồng chính | 1. Chụp ảnh căn cước và ảnh khuôn mặt. |
| | 2. ML Kit kiểm tra ảnh có đúng một khuôn mặt. |
| | 3. `FaceEmbedder` tính vector đặc trưng 192 chiều. |
| | 4. Ứng dụng ghi `kyc/{uid}` và chuyển `kycStatus` sang PENDING. |
| | 5. Nhân viên xem hồ sơ và duyệt, `kycStatus` thành VERIFIED. |
| Luồng phụ | Không có hoặc có nhiều hơn một khuôn mặt thì yêu cầu chụp lại. Nhân viên từ chối kèm lý do thì `kycStatus` thành REJECTED và khách nộp lại được. |
| Kết quả | Hồ sơ VERIFIED mới được phép giao dịch; vector đặc trưng dùng cho bước quét mặt sau này |

Table: Đặc tả use case "Chuyển tiền" {#tbl:uc-transfer}

| Mục | Nội dung |
|---------|----------------------------------------------|
| Tác nhân | Khách hàng |
| Điều kiện trước | `kycStatus` là VERIFIED; tài khoản nguồn còn số dư |
| Luồng chính | 1. Chọn kiểu chuyển (trong FLUX, giữa tài khoản của tôi, liên ngân hàng) và tài khoản nguồn. |
| | 2. Nhập số tài khoản; với tài khoản FLUX, ứng dụng tra tên chủ tài khoản qua `accountIndex`. |
| | 3. Nhập số tiền và nội dung. |
| | 4. `TransferPolicy.checkAmount` kiểm tra mức tối thiểu, mức tối đa mỗi lần, số dư và hạn mức ngày. |
| | 5. `TransactionAuthSheet` yêu cầu OTP, thêm quét mặt nếu số tiền từ 10.000.000 ₫. |
| | 6. `AccountRepository.transfer` ghi trong một Firestore transaction. |
| | 7. Hiện biên lai. |
| Luồng phụ | Vượt hạn mức, thiếu số dư, số tài khoản sai, không tìm thấy người nhận thì báo lỗi ở form và không gửi OTP. OTP sai 5 lần thì khoá cho tới khi gửi mã mới; mã hết hạn sau 120 giây. Khuôn mặt không khớp thì giao dịch bị từ chối. |
| Kết quả | Số dư nguồn giảm, số dư đích tăng, hai giao dịch `TRANSFER_OUT` và `TRANSFER_IN` nối nhau qua `pairTxId` |

Table: Đặc tả use case "Thanh toán hoá đơn" {#tbl:uc-bill}

| Mục | Nội dung |
|---------|----------------------------------------------|
| Tác nhân | Khách hàng |
| Điều kiện trước | `kycStatus` là VERIFIED |
| Luồng chính | 1. Chọn điện hoặc nước, chọn nhà cung cấp, nhập mã khách hàng. |
| | 2. `BillLookup.lookup` trả hoá đơn của kỳ hiện tại. |
| | 3. Chọn nguồn tiền, xác nhận. |
| | 4. `TransactionAuthSheet` xác thực. |
| | 5. `AccountRepository.pay` ghi giao dịch `PAYMENT` có id cố định `bill_<uid>_<nhà cung cấp>_<mã>_<yyyyMM>`. |
| | 6. Hiện biên lai. |
| Luồng phụ | Mã khách hàng kết thúc bằng 0000 thì báo không tìm thấy hoá đơn. Hoá đơn đã trả thì màn hiện "đã thanh toán" và cho xem biên lai; nếu vẫn cố ghi lần hai, Security Rules từ chối vì id đã tồn tại (`AlreadyPaidException`). |
| Kết quả | Số dư giảm đúng số tiền hoá đơn; hạn mức ngày tính gộp với chuyển tiền |

Table: Đặc tả use case "Hỏi trợ lý AI" {#tbl:uc-assistant}

| Mục | Nội dung |
|---------|----------------------------------------------|
| Tác nhân | Khách hàng; Gemini |
| Điều kiện trước | Khách đã đăng nhập |
| Luồng chính | 1. Khách nhập câu hỏi. |
| | 2. `AssistantViewModel` tải số dư và chi tiêu tháng thành `FinanceSnapshot` (tối đa 5 giây). |
| | 3. `AssistantPrompt.system` dựng chỉ dẫn hệ thống kèm số liệu. |
| | 4. `GeminiAssistantClient` gọi Gemini qua Firebase AI Logic. |
| | 5. `AssistantReplyParser` đọc câu trả lời JSON, giữ tối đa 3 nút hợp lệ. |
| | 6. Màn chat hiện câu trả lời và các nút; chạm nút thì mở đúng màn qua `UtilityScreens`. |
| Luồng phụ | Câu hỏi chứa mã OTP hoặc mật khẩu thì `AssistantFallback.secretWarning` trả lời ngay trên máy, không gửi đi. AI lỗi, hết lượt miễn phí hoặc quá 20 giây thì dùng `AssistantFallback.reply` ("Chế độ cơ bản"). |
| Kết quả | Câu trả lời trong vòng khoảng 20 giây; cuộc trò chuyện không được lưu |

## Class diagram

Toàn bộ code có khoảng 230 lớp, interface và object, nên một sơ đồ duy nhất không đọc nổi. Tôi chia theo tầng và theo tính năng: mô hình dữ liệu, repository, nghiệp vụ, khoá ứng dụng, xác thực giao dịch, một luồng giao diện tiêu biểu (chuyển tiền), ba nhóm tính năng (chi nhánh, trợ lý, tiện ích), và cuối cùng là `FluxApp` nối mọi thứ lại. Mỗi sơ đồ chỉ ghi thuộc tính và phương thức chính, đúng tên trong code. Ba kiểu quan hệ xuất hiện nhiều nhất: mũi tên nét liền là liên kết (lớp này giữ tham chiếu tới lớp kia), mũi tên nét đứt là phụ thuộc (chỉ dùng khi chạy), tam giác rỗng nét đứt là hiện thực interface. Hình thoi đặc là thành phần (composition), hình thoi rỗng là kết tập (aggregation).

### Mô hình dữ liệu

@fig:class-model là các lớp dữ liệu trong `data/model/`. Đây cũng là nền cho thiết kế cơ sở dữ liệu ở mục 3.4, vì mỗi lớp tương ứng một collection hoặc một phần của collection.

![Class diagram mô hình dữ liệu](diagrams/class-model.png){#fig:class-model width=15.5cm}

Một `User` có thể có nhiều `BankAccount` (liên kết qua `ownerUid`) và nhiều nhất một `KycRecord` (document `kyc/{uid}`). Mỗi `BankAccount` có nhiều `Transaction`, liên kết qua `accountId`. Hai giao dịch `TRANSFER_OUT` và `TRANSFER_IN` của một lần chuyển tiền trỏ nhau qua `pairTxId`, nên `Transaction` có một quan hệ tự thân bản số 0..1. `DetailRow` là thành phần của `Transaction` và `ServicePayment`: các dòng chi tiết của biên lai (kỳ hoá đơn, mã đặt chỗ...) không có ý nghĩa khi đứng riêng. `ServicePayment` là đối tượng dựng trước khi thanh toán và được ghi thành một `Transaction` loại `PAYMENT`.

Trong Firestore không có khoá ngoại, nên các liên kết trên là liên kết logic qua trường id; Security Rules và code repository giữ cho chúng nhất quán. `Payee` là bản chiếu của một document trong `accountIndex`, dùng để tra tên chủ tài khoản. `Rates` đứng một mình vì chỉ có một document `config/rates` cho cả ngân hàng. Các enum `UserRole`, `AccountType`, `TransactionType` giới hạn giá trị của các trường tương ứng.

### Repository

Mỗi nguồn dữ liệu có một interface và một cài đặt. @fig:class-repository cho thấy bảy cặp đó và các lớp mô hình mà chúng trả về.

![Class diagram tầng repository](diagrams/class-repository.png){#fig:class-repository width=14.5cm}

Tầng trên (ViewModel, dịch vụ nghiệp vụ) chỉ biết interface, nên đổi Firestore sang nguồn khác hay thay bằng bản giả khi kiểm thử không ảnh hưởng tới chúng. `AccountRepository` là interface lớn nhất vì mọi thao tác làm đổi số dư đều qua nó: nạp, rút, chuyển trong FLUX, chuyển liên ngân hàng và thanh toán dịch vụ. Cả năm phương thức này cùng dùng một cơ chế ghi (một Firestore transaction cập nhật số dư và tạo giao dịch), nên không có đường nào đổi số dư mà bỏ qua giao dịch.

### Nghiệp vụ

@fig:class-domain gồm các lớp thuần Kotlin trong `domain/`. Chúng không phụ thuộc Android nên được kiểm thử đơn vị trực tiếp.

![Class diagram tầng nghiệp vụ](diagrams/class-domain.png){#fig:class-domain width=14.8cm}

`TransferPolicy` gom luật chuyển tiền: tối đa 100.000.000 ₫ mỗi lần, 200.000.000 ₫ mỗi ngày tính theo giờ Việt Nam, kiểm tra số dư và định dạng số tài khoản. Hạn mức ngày tính cả `TRANSFER_OUT` và `PAYMENT`, nên thanh toán hoá đơn cũng làm giảm hạn mức còn lại. `InterestCalculator` tính lãi tiết kiệm mỗi tháng, tiền trả mỗi kỳ của khoản vay theo hai kiểu `PaymentFrequency` và kỳ trả kế tiếp. `FaceVerificationPolicy` quyết định bước quét mặt: từ 10.000.000 ₫ thì bắt buộc, và `precheck` báo trước nếu khách chưa eKYC hoặc hồ sơ thiếu vector đặc trưng. `CustomerOnboardingService` là lớp duy nhất ở đây phụ thuộc vào ba repository, vì việc tạo khách phải phối hợp tài khoản đăng nhập, hồ sơ và tài khoản ngân hàng. `QrPayload` mã hoá và giải mã nội dung mã QR dạng `fluxbank:<số tài khoản>`. `MonthSummary` gom các giao dịch của một tháng để màn "Giao dịch tháng N" và trợ lý dùng chung.

### Khoá ứng dụng

@fig:class-lock cho thấy cách ứng dụng tự khoá khi người dùng rời đi rồi quay lại.

![Class diagram khoá ứng dụng](diagrams/class-lock.png){#fig:class-lock width=15.5cm}

`AppLockObserver` là một `DefaultLifecycleObserver` gắn vào vòng đời của cả tiến trình. Nó nhờ `LockPolicy` quyết định có khoá hay không: khoá khi mở app lần đầu, hoặc khi quay lại sau từ 60 giây ở nền, và chỉ khi người dùng đã bật sinh trắc học và máy hỗ trợ. Khi cần khoá, `AppLockObserver` gọi hàm `startUnlock` mà `FluxApp` truyền vào để mở `UnlockActivity`. `UnlockActivity` dùng `BiometricHelper` rồi báo lại `onUnlocked()`. `LoginActivity` và `UnlockActivity` cũng báo cho `AppLockObserver` biết khi chính chúng đã quyết định khoá lúc mở app (`markColdStartHandled()`), để lần mở đó không bị khoá hai lần.

### Xác thực giao dịch

@fig:class-security gồm bottom sheet xác thực dùng chung và các lớp nó điều phối.

![Class diagram xác thực giao dịch](diagrams/class-security.png){#fig:class-security width=15.5cm}

`TransactionAuthSheet` là bottom sheet dùng lại ở mọi nơi có giao dịch. `TransactionAuthViewModel` giữ máy trạng thái `AuthSheetState` (nhập OTP, quét mặt, thất bại, đã xác thực, bị khoá). ViewModel này xin mã từ `OtpManager.issue`, đẩy mã tới `OtpNotifier` để hiện thông báo SMS giả lập, rồi kiểm bằng `OtpManager.verify`, kết quả là một giá trị `OtpResult`. Nếu `FaceVerificationPolicy` yêu cầu, ViewModel chuyển sang bước quét mặt: `TransactionAuthSheet` dựng `FaceStepController`, lớp này nối `FaceDetectorHelper` (ML Kit), `BlinkDetector` (phát hiện chớp mắt để biết đang là người thật) và `FaceEmbedder` (MobileFaceNet). Cuối cùng `TransactionAuthViewModel` so vector vừa tính với vector lúc eKYC bằng `FaceMatcher.isMatch`, ngưỡng cosine 0,6.

### Một luồng giao diện tiêu biểu: chuyển tiền

@fig:class-transfer-ui cho thấy cách các tầng ghép với nhau trong một tính năng, lấy chuyển tiền làm ví dụ vì nó dùng gần như mọi thành phần.

![Class diagram luồng giao diện chuyển tiền](diagrams/class-transfer-ui.png){#fig:class-transfer-ui width=15.5cm}

`CustomerMainActivity` hiện thực `ScreenHost`, nên `TransferHubFragment` mở màn chuyển tiền bằng `openScreen` mà không biết Activity cụ thể. `TransferFragment` chỉ vẽ form và nhận thao tác, mọi trạng thái nằm trong `TransferViewModel` dưới dạng `TransferState`. ViewModel nhận năm repository qua hàm tạo, dùng `TransferPolicy` để kiểm tra trước khi ghi, và chỉ ghi sau khi `TransferFragment` báo `TransactionAuthSheet` đã xác thực xong. Các tính năng nạp rút, thanh toán và chuyển tiền đều theo cùng khuôn: Fragment, ViewModel, repository, sheet xác thực, biên lai.

### Chi nhánh và đường đi bộ

![Class diagram tính năng chi nhánh](diagrams/class-branch.png){#fig:class-branch width=15.5cm}

@fig:class-branch cho thấy `BranchRecommender` là lớp trung tâm. `BranchRecommender` lấy 12 chi nhánh trong `Branches.ALL`, chọn ba chi nhánh gần nhất theo đường thẳng (`GeoMath`), gọi `WalkingRouter` cho từng chi nhánh, rồi chọn chi nhánh có quãng đi bộ ngắn nhất. Kết quả là một `Recommendation` gồm danh sách `RankedBranch`, chi nhánh được chọn và chi nhánh gần nhất theo đường thẳng, để giao diện giải thích khi hai chi nhánh này khác nhau. Vì `WalkingRouter` và `LocationSource` là interface nên test dùng bản giả thay vì mạng và GPS thật; `OsrmWalkingRouter` và `FusedLocationSource` là hai cài đặt thật.

### Trợ lý AI

![Class diagram trợ lý AI](diagrams/class-assistant.png){#fig:class-assistant width=15.5cm}

Trong @fig:class-assistant, `AssistantViewModel` điều phối cả đường AI lẫn đường dự phòng. Đường AI đi qua `AssistantPrompt` (dựng chỉ dẫn hệ thống từ `FinanceSnapshot`), `AssistantClient` (gọi Gemini) và `AssistantReplyParser` (đọc JSON, bỏ nút lạ hoặc trùng, giữ tối đa ba nút). Đường dự phòng là `AssistantFallback`, trả lời theo từ khoá và sinh ra cùng kiểu `AssistantReply`, nên giao diện không phân biệt hai đường. `AssistantAction` là danh sách 13 nút bot được phép đưa ra; mỗi nút ứng với một màn đã có sẵn, và `AssistantActionUi` cho biết nhãn và biểu tượng của nó. Việc mở màn dùng chung `UtilityScreens` với trang chủ, nên thêm một nút cho bot chỉ cần thêm một giá trị `AssistantAction` cùng vài dòng cấu hình.

### Tiện ích

![Class diagram nhóm tiện ích](diagrams/class-utility.png){#fig:class-utility width=15.5cm}

Bảy ô tiện ích (điện, nước, nạp điện thoại, vé máy bay, vé phim, khách sạn, mua sắm) dùng ba kiểu màn. Trong @fig:class-utility, `BillPayViewModel`, `TopUpViewModel` và `CatalogViewModel` cùng kế thừa `ServicePaymentViewModel`: lớp cha lo phần chung (chọn nguồn tiền, kiểm hạn mức và eKYC, mở sheet xác thực một lần, lỗi), lớp con chỉ dựng đúng `ServicePayment` của kiểu màn mình. Dữ liệu hoá đơn, nhà mạng và danh mục là các object thuần Kotlin giả lập trong `domain/utility/`; `ServicePayments` biến chúng thành `ServicePayment` rồi `AccountRepository.pay` ghi thành giao dịch.

### Composition root

![Class diagram FluxApp](diagrams/class-fluxapp.png){#fig:class-fluxapp width=8cm}

@fig:class-fluxapp cho thấy `FluxApp` giữ tham chiếu tới mọi dịch vụ dùng chung, dạng thuộc tính khởi tạo lười. Đây là nơi duy nhất biết lớp cụ thể nào đứng sau mỗi interface: `FluxApp` tạo `FirestoreAccountRepository` cho `AccountRepository`, `OsrmWalkingRouter` cho `WalkingRouter`, `GeminiAssistantClient` cho `AssistantClient`. `VnPay` có bản số 0..1 vì nó là `null` khi `local.properties` chưa có khoá VNPay; khi đó màn nạp tiền báo "chưa cấu hình VNPay" thay vì làm ứng dụng lỗi.
