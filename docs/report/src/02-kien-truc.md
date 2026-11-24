# KIẾN TRÚC HỆ THỐNG VÀ CÔNG NGHỆ

Chương này trình bày FLUX Bank được ghép từ những thành phần nào, code trong ứng dụng chia tầng ra sao, và vì sao tôi chọn từng công nghệ. Phần lý do quan trọng không kém phần mô tả: gần như mọi lựa chọn dưới đây đều chịu hai ràng buộc từ đầu, là chạy hoàn toàn trên gói miễn phí Firebase Spark và do một người làm.

## Kiến trúc tổng thể

FLUX Bank là một ứng dụng Android native, không có máy chủ riêng. Ứng dụng nói chuyện trực tiếp với các dịch vụ của Firebase và vài dịch vụ bên ngoài, như @fig:architecture mô tả.

![Kiến trúc tổng thể của FLUX Bank](diagrams/architecture.png){#fig:architecture width=15.5cm}

Có ba nhóm thành phần:

- Trên điện thoại là giao diện, ViewModel, repository và các lớp nghiệp vụ thuần Kotlin. Những việc liên quan tới riêng tư cũng chạy ngay trên máy: phát hiện khuôn mặt bằng ML Kit [7], tính vector đặc trưng khuôn mặt bằng mô hình MobileFaceNet [8] chạy trên LiteRT [9], xác thực sinh trắc học bằng `BiometricPrompt` [10], vẽ bản đồ bằng OSMDroid [12].
- Firebase gói Spark gồm Authentication [6] giữ tài khoản đăng nhập, Cloud Firestore [3] giữ toàn bộ dữ liệu nghiệp vụ, và Security Rules [4] là lớp kiểm tra cuối cùng phía máy chủ. Firebase AI Logic [15] đứng giữa ứng dụng và Gemini, App Check [16] được dùng để chặn các ứng dụng giả gọi vào AI Logic, hiện ở chế độ chưa bắt buộc (xem mục Trợ lý AI và App Check).
- Ngoài Firebase còn ba dịch vụ: cổng VNPay sandbox [11] cho chức năng nạp tiền, máy chủ tile OpenStreetMap [14] cho nền bản đồ, máy chủ OSRM [13] để tính đường đi bộ.

Vì không có máy chủ riêng nên mọi quy tắc nghiệp vụ phải nằm ở hai chỗ: code trên điện thoại và Security Rules. Hệ quả này quyết định nhiều thiết kế ở chương 3 và sẽ được nói rõ ở mục "Phân chia kiểm tra giữa ứng dụng và Security Rules" bên dưới.

## Kiến trúc trong ứng dụng

### Các tầng

Ứng dụng theo mô hình MVVM, gần với khuyến nghị trong hướng dẫn kiến trúc ứng dụng của Android [1]. @fig:layers cho thấy bốn tầng và chiều phụ thuộc giữa chúng.

![Các tầng của ứng dụng và chiều phụ thuộc](diagrams/layers.png){#fig:layers width=13cm}

- Tầng giao diện (`ui/`) gồm Activity và Fragment, chỉ vẽ màn hình và nhận thao tác. Hầu hết màn hình đi qua ViewModel; vài chỗ nhỏ như màn quét QR hoặc việc đọc `uid` đang đăng nhập vẫn gọi repository trực tiếp.
- Tầng trình bày là các ViewModel. Chúng giữ trạng thái màn hình, gọi repository bằng coroutine và đẩy trạng thái ra giao diện qua LiveData.
- Tầng nghiệp vụ (`domain/`, `core/`) chứa các quy tắc không cần Android, ví dụ `TransferPolicy` (hạn mức chuyển tiền), `InterestCalculator` (lãi tiết kiệm và tiền trả khoản vay), `OtpManager`, `LockPolicy`, `FaceMatcher`, `BranchRecommender`. Vì là Kotlin thuần nên kiểm thử đơn vị trực tiếp được.
- Ở tầng dữ liệu (`data/`), mỗi nguồn dữ liệu là một interface kèm một cài đặt. `AccountRepository` có `FirestoreAccountRepository`, `WalkingRouter` có `OsrmWalkingRouter`, `AssistantClient` có `GeminiAssistantClient`.

Các lớp nghiệp vụ chỉ biết interface của tầng dữ liệu, không biết Firestore. Nhờ đó khi kiểm thử tôi thay cài đặt thật bằng bản giả trong bộ `Fakes.kt`, không cần mạng hay emulator. Đây cũng là lý do bộ kiểm thử đơn vị (391 ca) chạy được trên máy phát triển mà không cần mạng.

### Điều hướng và vai trò

Ứng dụng có hai vai trò với hai giao diện riêng, đúng như yêu cầu của đề. Sau khi đăng nhập, `RoleRouter` chọn `CustomerMainActivity` hoặc `OfficerMainActivity`. Mỗi Activity chính chứa thanh tab dưới và mỗi tab là một Fragment. Các màn phụ (chi tiết tài khoản, biên lai, màn thanh toán hoá đơn...) mở bằng `ScreenHost.openScreen(fragment)`: Fragment được thêm vào back stack và thanh tab ẩn đi. Cách này giữ được cả hai thứ tôi cần: nút back hoạt động đúng, và giao diện hai vai trò dùng chung một bộ ba thành phần (Activity chính, tab, màn phụ) nên không phải viết hai lần.

### Service locator thay cho thư viện DI

Các đối tượng dùng chung (repository, `OtpManager`, `LockPolicy`, `VnPay`, `BranchRecommender`...) được tạo một lần trong lớp `FluxApp`, dạng thuộc tính `by lazy`. Màn hình và ViewModel lấy chúng từ `FluxApp.instance`. Tôi chọn cách này thay vì Hilt hay Koin vì ứng dụng chỉ có một process và khoảng mười tám dịch vụ dùng chung; một thư viện DI sẽ thêm cấu hình và thời gian build mà không giải quyết vấn đề nào thật sự có. Đổi lại, `FluxApp` phụ thuộc vào gần như mọi lớp cụ thể, và `FluxApp.instance` là trạng thái toàn cục. Điểm tựa để kiểm thử vẫn nằm ở interface: ViewModel nhận repository qua tham số hàm tạo, giá trị mặc định lấy từ `FluxApp`, nên test truyền bản giả vào mà không đụng tới `FluxApp`.

## Phân chia kiểm tra giữa ứng dụng và Security Rules

Gói Spark không có Cloud Functions, nên không có chỗ để chạy code tin cậy phía máy chủ. Việc nào kiểm được bằng Security Rules thì kiểm ở đó; việc nào rules không biểu diễn được thì chỉ kiểm trên điện thoại. @tbl:verify-where cho thấy chỗ kiểm của từng quy tắc chính.

Table: Nơi kiểm tra các quy tắc quan trọng {#tbl:verify-where}

| Quy tắc | Trên điện thoại | Security Rules |
|------------------------------------|----------------------------|------------------------------------|
| Chỉ nhân viên tạo, sửa khách và mở tài khoản | ẩn chức năng theo vai trò | kiểm `users/{uid}.role` |
| Số điện thoại duy nhất | báo lỗi sớm | tạo `phoneIndex/{sđt}` thất bại nếu đã có |
| Mỗi lần đổi số dư phải kèm một giao dịch mới khớp | Firestore transaction ghi hai document | so `lastTxId`, `balanceAfter`, phép cộng/trừ |
| Cộng tiền cho người khác phải có lệnh trừ nguồn đi kèm | transaction ghi 4 document | `TRANSFER_IN` phải có `TRANSFER_OUT` cặp |
| Mỗi khách chỉ trả một hoá đơn một lần | tra trạng thái "đã thanh toán" | id giao dịch cố định theo người trả và hoá đơn, tạo lần hai bị từ chối |
| Mã OTP đúng, còn hạn, dùng một lần | `OtpManager` | không kiểm được |
| Khuôn mặt khớp ảnh eKYC (giao dịch ≥ 10.000.000 ₫) | `FaceMatcher` | không kiểm được |
| Hạn mức 100 triệu mỗi lần, 200 triệu mỗi ngày | `TransferPolicy` | chỉ chặn mỗi lần nạp, chuyển, thanh toán quá 100 triệu; hạn mức ngày không kiểm được |

Hai dòng OTP và khuôn mặt là giới hạn thật của kiến trúc: một người sửa APK có thể bỏ qua hai bước này. Với đồ án, tôi chấp nhận và ghi lại ở chương 6. Hướng nâng cấp là chuyển việc phát và kiểm token xác thực sang Cloud Functions khi có gói Blaze.

## Công nghệ và lý do chọn

@tbl:tech-nen liệt kê nền tảng build và hạ tầng; @tbl:tech-chuc-nang liệt kê các thư viện và dịch vụ cho từng chức năng. Số phiên bản lấy từ `gradle/libs.versions.toml` và `app/build.gradle.kts`; riêng Gradle lấy từ `gradle/wrapper/gradle-wrapper.properties`, JDK 21 từ `gradle/gradle-daemon-jvm.properties`, còn Kotlin là bản đi kèm AGP 9.4.1.

Table: Nền tảng và hạ tầng {#tbl:tech-nen}

| Công nghệ | Phiên bản | Lý do chọn | Phương án thay thế (không dùng) |
|-----------------|-----------------------|-----------------------------------|-------------------------|
| Android native, Kotlin | AGP 9.4.1, Kotlin 2.2, Gradle 9.6.0, JDK 21; minSdk 26, targetSdk 37 | Đề yêu cầu app Android; Kotlin ngắn và an toàn null hơn Java | Java; đa nền tảng (Flutter, React Native) vì đề chỉ cần Android |
| Giao diện XML + ViewBinding [2] | Material Components 1.14.0, AppCompat 1.8.0 | Mockup HTML đã duyệt ánh xạ thẳng sang style XML; ViewBinding thay `findViewById`, an toàn kiểu | Jetpack Compose: phải viết lại toàn bộ phần giao diện có sẵn, không đổi được kết quả hiển thị |
| Kiến trúc MVVM [1] | Lifecycle 2.8.6, Fragment 1.8.5, Coroutines 1.8.1 | ViewModel sống qua xoay màn hình; coroutine gọn hơn callback khi chờ Firestore | Gọi repository thẳng từ Activity (code gốc của F1–F2, bỏ vì khó kiểm thử) |
| Cloud Firestore [3] | Firebase BoM 34.19.0 | Dữ liệu dùng chung giữa nhân viên và khách trên nhiều máy; có transaction; đồng bộ sẵn; gói Spark miễn phí | SQLite/Room: chỉ lưu trên một máy nên nhân viên và khách không thấy dữ liệu của nhau |
| Firebase Authentication [6] | cùng BoM | Đăng nhập email và mật khẩu, tạo tài khoản qua app phụ, không phải tự dựng máy chủ | Tự xây máy chủ đăng nhập: tốn công và phải trả phí máy chủ |
| Security Rules [4] | `firestore.rules` | Lớp kiểm tra duy nhất chạy ở máy chủ trên gói Spark | Cloud Functions: cần gói Blaze, Google Cloud từ chối lập tài khoản thanh toán (lỗi `OR_BACR2_59`) |
| Gói Spark [5] | miễn phí | Tôi đặt mục tiêu chi phí bằng 0 | Gói Blaze, vì lý do ở dòng trên |

Table: Thư viện và dịch vụ theo chức năng {#tbl:tech-chuc-nang}

| Công nghệ (phiên bản) | Dùng cho | Lý do chọn | Phương án thay thế (không dùng) |
|-------------------|-----------------|---------------------------|----------------------|
| ML Kit Face Detection [7] (16.1.7) | Đúng một khuôn mặt, phát hiện chớp mắt | Chạy trên máy, miễn phí | Dịch vụ nhận diện trên cloud: tính phí, ảnh rời khỏi máy |
| MobileFaceNet [8] trên LiteRT [9] (1.4.2) | Vector đặc trưng 192 chiều, so cosine với ảnh eKYC | Mô hình nhỏ (khoảng 5 MB), giấy phép MIT | Dịch vụ eKYC thương mại: tính phí |
| BiometricPrompt [10] (1.2.0-alpha05) | Khoá ứng dụng bằng vân tay hoặc khuôn mặt của máy | API chuẩn của Android | Màn nhập PIN tự viết |
| OTP giả lập SMS (`OtpManager`) | Xác thực hai yếu tố cho mọi giao dịch | SMS thật cần dịch vụ tính phí hoặc Cloud Functions | SMS thật, email OTP |
| VNPay sandbox [11] (HMAC-SHA512) | Nạp tiền vào tài khoản | Dùng VND, sandbox miễn phí | Stripe: tiền tệ và thủ tục không hợp ngữ cảnh Việt Nam |
| OSMDroid [12] (6.1.20), OpenStreetMap [14] | Bản đồ, ghim chi nhánh | Không cần key, không cần thanh toán | Google Maps: cần tài khoản thanh toán |
| OSRM [13] (máy chủ công cộng) | Quãng đường đi bộ ngắn nhất | Miễn phí, không key | Google Directions: như trên |
| Fused Location (play-services-location 21.3.0) | Vị trí người dùng | API vị trí chuẩn của Android | Tự đọc GPS |
| Firebase AI Logic [15] (BoM 34.19.0) | Trợ lý AI dùng Gemini | Không để API key trong APK; chạy trên Spark | Gọi thẳng Gemini bằng key trong ứng dụng: lộ key |
| Model `gemini-3.5-flash-lite` | Trả lời có cấu trúc `{reply, actions}`, không bật thinking | Chọn sau khi đo hạn mức gói miễn phí | `gemini-3.5-flash` chỉ 20 lượt mỗi ngày; `gemini-3.8-flash` hay quá tải |
| App Check [16] (Play Integrity cho release, debug provider cho debug) | Để chặn ứng dụng giả gọi AI Logic (hiện chưa bắt buộc) | Cùng hệ Firebase | Không bật: ai cũng gọi được qua key của dự án |
| ML Kit Barcode Scanning (17.3.0), ZXing (3.5.3) | Quét và sinh mã QR nhận tiền | Chạy trên máy, giấy phép Apache-2.0 | Tự giải mã QR |
| CameraX (1.6.2) | Chụp eKYC, quét mặt, quét QR | API camera thống nhất | Camera2 trực tiếp |
| Phosphor [17] (MIT), Inter [18] (OFL) | Bộ icon và font chữ | Miễn phí, dùng được trong sản phẩm | Icon Material mặc định |

Nhìn lại hai bảng, có ba quyết định ảnh hưởng nhiều nhất tới phần còn lại của báo cáo.

Quyết định đầu tiên là dùng Firestore thay cho SQLite. Đề cho phép SQLite nhưng ưu tiên cơ sở dữ liệu trên cloud, và nhu cầu thật còn rõ hơn lời ưu tiên đó: nhân viên tạo tài khoản trên máy của họ, khách đăng nhập và phải thấy tài khoản ấy trên máy khác. Chuyển tiền cho người khác càng cần một nơi dữ liệu chung. Vì vậy toàn bộ thiết kế dữ liệu ở mục 3.4 là thiết kế cho một cơ sở dữ liệu NoSQL, và ảnh eKYC phải lưu dạng Base64 (cạnh dài tối đa 1024 px) ngay trong document vì gói Spark không có Storage.

Quyết định thứ hai là mọi thứ phải miễn phí và không cần billing. Ràng buộc này loại Cloud Functions, SMS thật, Google Maps và các API tính phí. Mỗi chỗ bị loại tôi thay bằng một lựa chọn chạy trên điện thoại hoặc một dịch vụ công cộng, và giới hạn đi kèm được ghi ở chương 6.

Quyết định thứ ba là xử lý dữ liệu nhạy cảm ngay trên máy. Ảnh khuôn mặt và vector đặc trưng không rời khỏi hệ Firebase của dự án, và việc nhận diện không phụ thuộc dịch vụ bên ngoài nào. Cái giá là phần so khớp khuôn mặt nằm ở phía ứng dụng, như đã nói ở mục trước.

## Trợ lý AI và App Check

Trợ lý AI gửi dữ liệu tài chính của khách tới Google; ngoài ra chỉ có toạ độ vị trí gửi tới máy chủ OSRM. Mỗi câu hỏi đi kèm một bản tóm tắt: số dư theo loại tài khoản, tổng thu chi và chi theo nhóm của tháng, hạn mức còn lại. Tên gọi của khách (từ cuối trong họ tên) và 10 lượt trò chuyện gần nhất cũng được gửi; số tài khoản, số điện thoại, căn cước và tên người nhận thì không. Tin nhắn có chứa mã OTP hoặc mật khẩu được trả lời ngay trên máy, không gửi đi.

Ứng dụng gọi Gemini qua Firebase AI Logic, nên APK không chứa API key. Để ngăn người ngoài dùng dự án của tôi làm đường gọi AI miễn phí, App Check dùng Play Integrity cho bản phát hành. Hiện App Check ở chế độ không bắt buộc cho AI Logic vì phạm vi phát hành Google Play đã được thu gọn; ứng dụng vẫn chạy bình thường, chỉ là chưa có lớp chặn này. Nếu đưa lên production, App Check phải bật bắt buộc theo thứ tự đã ghi trong `README.md`.

Khi AI lỗi, hết lượt miễn phí hoặc quá 20 giây, ứng dụng chuyển sang "chế độ cơ bản": nhận diện từ khoá trên máy và vẫn mở đúng màn hình. Nhờ vậy chức năng trợ lý không phụ thuộc vào việc dịch vụ AI miễn phí còn sống.

## Nguyên tắc và thực hành

Có sáu thực hành tôi giữ xuyên suốt, vì chúng ảnh hưởng trực tiếp tới chất lượng sản phẩm cuối.

1. Tách tầng và phụ thuộc vào interface, như đã mô tả ở trên, để kiểm thử mà không cần dịch vụ thật.
2. Số dư chỉ đổi qua Firestore transaction. Mọi thao tác nạp, rút, chuyển, thanh toán đều đi qua một `runTransaction` ghi đồng thời số dư và bản ghi giao dịch mới; Security Rules đối chiếu hai document với nhau. Không có đường nào cập nhật số dư mà không để lại giao dịch.
3. Viết kiểm thử trước. Bộ kiểm thử gồm 391 ca đơn vị và 49 ca cho Security Rules (chạy trên Firestore Emulator). Trước khi sửa rules tôi thêm ca kiểm tra, rồi mới sửa.
4. Viết spec và kế hoạch trước khi code. Mỗi tính năng có một tài liệu thiết kế (mục tiêu, việc không làm, phản mục tiêu, ràng buộc) và một kế hoạch từng bước, lưu trong `docs/superpowers/`.
5. Duyệt mockup trước khi làm giao diện. Mọi màn mới được dựng thử bằng HTML để tôi xem và duyệt, sau đó mới viết XML. Giao diện dùng token của design system cho màu, khoảng cách và kiểu chữ; không có mã màu viết cứng, còn một số kích thước dp viết cứng trong layout (lỗi nhỏ đã ghi nhận).
6. Review toàn nhánh bởi một agent AI độc lập sau khi xong mỗi tính năng. Agent này đọc toàn bộ thay đổi với ngữ cảnh mới, không biết tôi đã nghĩ gì khi viết. Nhiều lỗi quan trọng (chiếm id hoá đơn của người khác, chạm chi nhánh lúc đang định vị lại thì vẽ đường từ vị trí cũ...) được tìm ra ở bước này và sửa kèm test. Người review ở đây là máy; chương 1 nói rõ phần việc nào do công cụ AI làm.

## Triển khai và hạ tầng

@fig:deployment cho thấy các thành phần nằm ở đâu khi build, kiểm thử và chạy.

![Sơ đồ triển khai](diagrams/deployment.png){#fig:deployment width=15.5cm}

Máy phát triển build APK bằng Gradle và cài lên điện thoại qua adb. Security Rules được dán thủ công vào Firebase Console mỗi khi sửa; Firebase CLI cũng triển khai được rules trên gói Spark, nhưng tôi chưa dùng. Để không chỉ tin vào lần dán thủ công, thư mục `rules-test/` chạy bản sao rules trên Firestore Emulator và kiểm tra 49 tình huống, gồm cả các tình huống phải bị từ chối. Bản phát hành đóng gói thành file AAB, ký bằng khoá upload lưu ngoài repository (cấu hình trong `local.properties`, không có trong git); Play App Signing giữ khoá ký chính nếu tôi tải lên Google Play. Việc tải lên là tuỳ chọn theo thông báo của giảng viên, nên tôi ghi trạng thái thật ở chương 6.
