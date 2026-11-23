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

Sáu use case sau là những luồng barem đòi hỏi và cũng nhiều ràng buộc nhất. Mỗi bảng ghi tên, tác nhân, điều kiện trước, luồng chính, luồng phụ và kết quả.

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
| Kết quả | Ứng dụng chỉ cho giao dịch khi hồ sơ VERIFIED (Security Rules chưa kiểm); vector đặc trưng dùng cho bước quét mặt sau này |

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

`AppLockObserver` là một `DefaultLifecycleObserver` gắn vào vòng đời của cả tiến trình. `AppLockObserver` nhờ `LockPolicy` quyết định có khoá hay không: khoá khi mở app lần đầu, hoặc khi quay lại sau từ 60 giây ở nền, và chỉ khi người dùng đã bật sinh trắc học và máy hỗ trợ. Khi cần khoá, `AppLockObserver` gọi hàm `startUnlock` mà `FluxApp` truyền vào để mở `UnlockActivity`. `UnlockActivity` dùng `BiometricHelper` rồi báo lại `onUnlocked()`. `LoginActivity` và `UnlockActivity` cũng báo cho `AppLockObserver` biết khi chính chúng đã quyết định khoá lúc mở app (`markColdStartHandled()`), để lần mở đó không bị khoá hai lần.

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


## Sơ đồ tuần tự

Bốn luồng dưới đây có nhiều bên tham gia nhất. Mỗi sơ đồ gộp các lớp cùng vai trò thành một đối tượng (ví dụ "Màn chuyển tiền" gồm Fragment và ViewModel) để vừa một trang; tên lớp cụ thể đã có ở mục 3.2.

### Chuyển tiền trong FLUX

@fig:seq-transfer là luồng đầy đủ nhất, vì nó đi qua cả ba lớp bảo vệ: kiểm tra nghiệp vụ trên máy, xác thực hai yếu tố và Security Rules.

![Sơ đồ tuần tự chuyển tiền trong FLUX](diagrams/seq-transfer.png){#fig:seq-transfer width=15.5cm}

Các lỗi hạn mức và số dư bị chặn ngay ở form (bước 3) nên chưa có mã OTP nào được phát. Chỉ khi hợp lệ, `TransactionAuthSheet` mới phát mã qua thông báo SMS giả lập. Với số tiền từ 10.000.000 ₫, sheet thêm bước chớp mắt và so khuôn mặt trước khi báo "đã xác thực". Chỉ sau tín hiệu đó `AccountRepository.transfer` mới ghi. Phần ghi là một Firestore transaction duy nhất gồm bốn thao tác: tạo giao dịch `TRANSFER_OUT`, trừ số dư tài khoản nguồn, cộng số dư tài khoản đích bằng `FieldValue.increment` (nên không cần đọc tài khoản của người khác), và tạo giao dịch `TRANSFER_IN`. Security Rules chạy trên toàn bộ bốn thao tác này và chỉ chấp nhận khi chúng khớp nhau; nếu một thao tác bị từ chối thì cả transaction bị huỷ.

### Nộp và duyệt eKYC

@fig:seq-ekyc có hai nửa: khách (hoặc nhân viên chụp hộ) nộp hồ sơ, rồi nhân viên duyệt.

![Sơ đồ tuần tự nộp và duyệt eKYC](diagrams/seq-ekyc.png){#fig:seq-ekyc width=15.5cm}

Vector đặc trưng được tính ngay trước khi gửi, để lần quét mặt khi giao dịch sau này chỉ cần so, không phải xử lý lại ảnh gốc. Ảnh căn cước và ảnh khuôn mặt nằm cùng document `kyc/{uid}` dưới dạng Base64, nên ứng dụng kiểm tra tổng kích thước trước khi gửi vì Firestore giới hạn 1 MiB cho mỗi document. Việc nộp hồ sơ và đổi `kycStatus` sang PENDING nằm trong cùng một batch; Security Rules kiểm tra điều đó để khách không thể thay ảnh sau khi đã được duyệt. Khi duyệt, nhân viên cập nhật `kycStatus` và ghi dấu vết duyệt (`reviewedBy`, `reviewedAt`) cũng trong một batch.

### Nạp tiền qua VNPay

@fig:seq-vnpay cho thấy cách ứng dụng kiểm tra kết quả do VNPay trả về trước khi cộng tiền.

![Sơ đồ tuần tự nạp tiền qua VNPay](diagrams/seq-vnpay.png){#fig:seq-vnpay width=15.5cm}

Mã giao dịch `txId` được tạo trước khi mở WebView và đóng vai trò `vnp_TxnRef`. Khi VNPay chuyển hướng về, `VnPay.parseReturn` kiểm ba điều: chữ ký HMAC-SHA512 đúng, mã giao dịch khớp, số tiền khớp. Chỉ khi cả ba đúng và mã phản hồi là thành công thì `deposit` mới chạy, và nó dùng lại chính `txId` làm id của giao dịch `DEPOSIT`. Vì id cố định nên nếu kết quả bị xử lý hai lần (ví dụ mạng lỗi rồi thử ghi lại), lần ghi thứ hai bị Security Rules từ chối, tiền không bị cộng hai lần. Khoá bí mật của cổng thanh toán nằm trong `local.properties` và được đưa vào `BuildConfig` khi build, nên chữ ký được kiểm trên máy; đây là giới hạn của gói Spark, ghi ở chương 6.

### Hỏi trợ lý AI

@fig:seq-assistant mô tả đường đi của một câu hỏi, gồm cả hai đường dự phòng.

![Sơ đồ tuần tự hỏi trợ lý AI](diagrams/seq-assistant.png){#fig:seq-assistant width=15.5cm}

Hai việc xảy ra trước khi gọi AI: kiểm tra câu hỏi có chứa mã OTP hoặc mật khẩu không (nếu có thì trả lời ngay trên máy), rồi tải số liệu tài khoản tối đa 5 giây. Nếu tải số liệu chậm hơn mức đó, câu hỏi vẫn được gửi nhưng không kèm số liệu. Toàn bộ thời gian chờ, kể cả tải số liệu và gọi AI, bị chặn ở 20 giây; quá hạn hoặc AI báo lỗi thì `AssistantFallback` trả lời theo từ khoá và gắn nhãn "Chế độ cơ bản". Các nút ở cuối câu trả lời mở màn qua `UtilityScreens`, cùng cơ chế với ô tiện ích trên trang chủ, và mở bằng `openScreen` nên quay lại vẫn còn cuộc trò chuyện.

### Vòng đời mã OTP

@fig:state-otp là máy trạng thái của `OtpManager`. Tôi vẽ riêng vì các quy tắc OTP rải ở nhiều nơi trong yêu cầu của đề (hết hạn, dùng một lần, giới hạn số lần thử) và máy trạng thái là cách ngắn nhất để thấy chúng khớp nhau.

![Máy trạng thái mã OTP](diagrams/state-otp.png){#fig:state-otp width=12cm}

Mỗi lần `issue` thay mã cũ bằng mã mới và đặt lại bộ đếm sai, nên "gửi lại mã" cũng là cách thoát khỏi trạng thái bị khoá. Mã gắn với một `transactionId`: nhập đúng mã nhưng cho giao dịch khác cũng tính là một lần sai. Mã đúng bị huỷ ngay, nên không dùng được lần hai. Hết 120 giây thì mã bị xoá và các lần `verify` sau trả về `NO_ACTIVE`.

## Thiết kế cơ sở dữ liệu

### Cách đọc thiết kế

Firestore [3] là cơ sở dữ liệu document. Dữ liệu nằm trong các collection, mỗi collection chứa nhiều document có id riêng, và không có khoá ngoại, không có phép nối. Vì vậy tôi dùng sơ đồ ER theo cách đọc sau: mỗi collection là một thực thể, id của document là khoá chính, và một trường chứa id của document khác là khoá ngoại logic. Bản số của quan hệ do code và Security Rules [4] giữ, không do cơ sở dữ liệu ép.

Vì không có phép nối, một số thông tin được lưu lặp lại để đọc nhanh: tên và số tài khoản đối tác nằm ngay trong giao dịch, tên chủ tài khoản nằm trong `accountIndex`. Đổi lại, các bản sao này có thể lệch khi dữ liệu gốc đổi. Thiết kế chấp nhận điều đó ở những chỗ bản sao là lịch sử bất biến (giao dịch không bao giờ sửa), và dùng Security Rules bắt bản sao phải khớp dữ liệu gốc tại thời điểm ghi (xem bảng bất biến bên dưới).

### Sơ đồ ER

@fig:erd có bảy collection.

![Sơ đồ thực thể quan hệ của cơ sở dữ liệu Firestore](diagrams/erd.png){#fig:erd width=15.5cm}

Một người dùng (`users`) có tối đa một hồ sơ `kyc` và một mục `phoneIndex`, và có ít nhất một tài khoản trong `accounts` (nhân viên cấp sẵn một tài khoản Checking khi tạo khách). Mỗi tài khoản có nhiều giao dịch trong `transactions`; hai giao dịch của một lần chuyển nội bộ trỏ nhau qua `pairTxId`. `accountIndex` là danh bạ tra người nhận theo số tài khoản và chỉ có mục cho tài khoản Checking và Saving. `config/rates` là một document duy nhất chứa lãi suất chung. Document `accounts` còn giữ `lastTxId`, trỏ tới giao dịch gần nhất đã đổi số dư, và Security Rules dùng trường này để buộc mỗi lần đổi số dư phải có một giao dịch mới đi kèm.

### Mô tả các collection

Các bảng sau liệt kê trường của từng collection. Dấu `?` sau kiểu nghĩa là trường có thể rỗng. Số tiền luôn là số nguyên `Long` tính bằng đồng, không dùng số thực để tránh sai số làm tròn.

Table: Collection `users` (id document là `uid` của Firebase Authentication) {#tbl:db-users}

| Trường | Kiểu | Mô tả |
|-----------|---------|-----------------------------------|
| email | String | Email đăng nhập |
| fullName | String | Họ tên |
| phoneNumber | String | Số điện thoại. Tính duy nhất do `phoneIndex` giữ khi đổi số qua ứng dụng; Security Rules chưa chặn khách tự ghi trường này |
| nationalId | String | Số căn cước |
| address | String | Địa chỉ |
| avatarBase64 | String? | Ảnh đại diện |
| role | String | `CUSTOMER` hoặc `BANK_OFFICER` |
| kycStatus | String | `NOT_SUBMITTED`, `PENDING`, `VERIFIED`, `REJECTED` |
| kycRejectReason | String? | Lý do nhân viên từ chối |
| biometricEnabled | Boolean | Đã bật khoá sinh trắc học |
| mustChangePassword | Boolean | Phải đổi mật khẩu ở lần đăng nhập đầu |
| createdAt, createdBy | Long, String? | Thời điểm tạo, `uid` nhân viên tạo |

Table: Collection `phoneIndex` (id document là số điện thoại) {#tbl:db-phone}

| Trường | Kiểu | Mô tả |
|-----------|---------|-----------------------------------|
| uid | String | Người dùng sở hữu số này. Tạo document thứ hai cùng số bị Security Rules từ chối, nên số điện thoại duy nhất |

Table: Collection `kyc` (id document là `uid` của khách) {#tbl:db-kyc}

| Trường | Kiểu | Mô tả |
|-----------|---------|-----------------------------------|
| idCardBase64 | String | Ảnh căn cước, cạnh dài tối đa 1024 px |
| faceImageBase64 | String | Ảnh khuôn mặt |
| faceEmbedding | List<Double> | Vector đặc trưng 192 chiều từ MobileFaceNet |
| submittedBy, submittedAt | String, Long | Người nộp (khách hoặc nhân viên), thời điểm |
| reviewedBy, reviewedAt | String?, Long? | Nhân viên duyệt, thời điểm |

Table: Collection `accounts` {#tbl:db-accounts}

| Trường | Kiểu | Mô tả |
|-----------|---------|-----------------------------------|
| ownerUid | String | Chủ tài khoản (khoá ngoại tới `users`) |
| type | String | `CHECKING`, `SAVING`, `MORTGAGE` |
| accountNumber | String | Số tài khoản dạng `8868xxxxxxxx` |
| balance | Long | Số dư; với `MORTGAGE` là dư nợ |
| lastTxId | String? | Giao dịch gần nhất làm đổi số dư |
| loanAmount, annualRate | Long, Double | Chỉ `MORTGAGE`: số tiền vay, lãi suất %/năm cố định |
| termMonths, startDate | Int, Long | Chỉ `MORTGAGE`: kỳ hạn tháng, ngày bắt đầu |
| createdAt | Long | Thời điểm mở |

Table: Collection `transactions` (chỉ tạo, không sửa, không xoá) {#tbl:db-transactions}

| Trường | Kiểu | Mô tả |
|-----------|---------|-----------------------------------|
| accountId, ownerUid | String | Tài khoản và chủ của giao dịch |
| accountNumber | String | Bản sao số tài khoản để hiển thị |
| type | String | `DEPOSIT`, `WITHDRAW`, `TRANSFER_OUT`, `TRANSFER_IN`, `PAYMENT` |
| amount | Long | Số tiền, lớn hơn 0 (Security Rules yêu cầu tối thiểu 10.000 ₫) |
| balanceAfter | Long? | Số dư sau giao dịch; rỗng ở `TRANSFER_IN` vì người gửi không biết số dư người nhận |
| description | String | Nội dung |
| counterpartyName, counterpartyAccount, counterpartyBank | String | Đối tác; ngân hàng là `FLUX` nếu cùng ngân hàng |
| pairTxId | String? | Nối `TRANSFER_OUT` với `TRANSFER_IN` |
| paymentRef | String? | Mã giao dịch VNPay; với `PAYMENT` là khoá hoá đơn hoặc mã đặt chỗ |
| service | String? | Chỉ `PAYMENT`: loại dịch vụ |
| details | List | Chỉ `PAYMENT`: các dòng của biên lai (nhãn, giá trị) |
| createdAt | Long | Thời điểm tạo |

Table: Collection `accountIndex` (id document là số tài khoản) {#tbl:db-index}

| Trường | Kiểu | Mô tả |
|-----------|---------|-----------------------------------|
| accountId, ownerUid | String | Tài khoản và chủ tài khoản |
| type | String | `CHECKING` hoặc `SAVING` |
| holderName | String | Tên chủ tài khoản, phải trùng `users.fullName` khi ghi. Chỉ đọc được từng mục (`get`), không liệt kê (`list`) |

Table: Document `config/rates` {#tbl:db-rates}

| Trường | Kiểu | Mô tả |
|-----------|---------|-----------------------------------|
| savingRate | Double | Lãi suất tiết kiệm, %/năm, trong khoảng (0, 30] |
| mortgageRate | Double | Lãi suất vay, %/năm, trong khoảng (0, 30] |
| updatedAt, updatedBy, updatedByName | Long, String, String | Lần sửa gần nhất: thời điểm, `uid` và tên nhân viên |

### Chỉ mục

Các truy vấn theo một trường hoặc tra theo id dùng chỉ mục tự động của Firestore. Có một truy vấn cần chỉ mục ghép: danh sách giao dịch của một khách, lọc theo `ownerUid` và sắp xếp theo `createdAt` giảm dần. Chỉ mục này (`ownerUid` tăng, `createdAt` giảm) được tạo thủ công trong Firebase Console và ghi lại trong `README.md`. Lịch sử giao dịch chỉ đọc 100 giao dịch mới nhất rồi lọc theo tài khoản hoặc theo tháng ở phía máy, đủ cho quy mô đồ án và tránh phải tạo thêm chỉ mục. `phoneIndex` và `accountIndex` luôn được tra bằng id nên không cần chỉ mục nào.

### Bất biến mà Security Rules bảo vệ

Thiết kế dữ liệu chỉ có ý nghĩa khi các quan hệ giữa các collection không bị phá. Vì không có máy chủ riêng, việc giữ quan hệ do Security Rules [4] đảm nhiệm. @tbl:db-invariants liệt kê các bất biến chính, quy tắc tương ứng trong `firestore.rules` và ca kiểm tra trong `rules-test/rules.test.mjs` (chạy trên Firestore Emulator).

Table: Bất biến dữ liệu và ca kiểm tra Security Rules {#tbl:db-invariants}

| Bất biến | Quy tắc trong `firestore.rules` | Ca kiểm tra |
|------------------|--------------------------|---------------------|
| Không đổi số dư mà không có giao dịch kèm theo | `ownerMoneyMoveAllowed`: `lastTxId` trỏ tới giao dịch mới, khớp tài khoản, `balanceAfter` bằng số dư mới | `balance-only update denied`, `tx alone (lastTxId not pointing to it) denied` |
| Số học của số dư đúng, không âm | Cộng với `DEPOSIT`, trừ với `WITHDRAW`, `TRANSFER_OUT`, `PAYMENT`, kết quả ≥ 0 | `deposit with wrong arithmetic denied`, `withdraw to -1 denied` |
| Giao dịch không bị ghi đè hay dùng lại id | `!exists(txPath)`; `transactions` không cho `update`, `delete` | `reusing existing tx id denied` |
| Chỉ chủ tài khoản mới rút hoặc chuyển đi | `resource.data.ownerUid == request.auth.uid` | `moving another customers account denied` |
| Cộng tiền cho người khác phải có lệnh trừ nguồn | `transferCreditAllowed`: có `TRANSFER_OUT` cặp, cùng số tiền, nguồn đã bị trừ | `credit without debit denied`, `credit amount differs from OUT denied` |
| Thông tin người gửi trên biên lai người nhận không giả được | `transferInBacked`: tên, số tài khoản, ngân hàng phải khớp hồ sơ; `balanceAfter` rỗng | `fake sender name denied`, `TRANSFER_IN with forged source account denied` |
| Mỗi khách chỉ trả một hoá đơn một lần, không chiếm id hoá đơn của người khác (hai khách khác nhau vẫn trả được cùng một mã) | id `bill_<uid>_…` chỉ dành cho `PAYMENT` của chính `uid` đó | `second payment of same bill id denied`, `paying into another customers bill id denied` |
| Số tiền mỗi lần nạp, chuyển, thanh toán trong khoảng cho phép | `amount >= 10000` và `<= 100000000` (trừ rút) | `deposit over 100M denied`, `deposit under 10k denied` |
| Danh bạ tra người nhận không giả tên | `indexEntryValid`: tên phải bằng `users.fullName`, số tài khoản phải trùng | `index entry with fake name denied`, `index list denied` |
| Chỉ nhân viên mở tài khoản và sửa lãi suất, lãi suất hợp lý | `isOfficer()`; mỗi mức trong (0, 30]; kỳ hạn vay 1 đến 360 tháng | `customer writes rates denied`, `officer writes rate above 30 denied`, `officer mortgage term 0 denied` |
