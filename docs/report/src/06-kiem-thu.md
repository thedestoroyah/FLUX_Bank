# KIỂM THỬ VÀ ĐÁNH GIÁ

Chương này trình bày cách tôi kiểm tra ứng dụng và kết quả: kiểm thử đơn vị, kiểm thử Security Rules, kiểm thử chức năng trên điện thoại thật và các lần review. Phần cuối nêu hạn chế đã biết, trạng thái Google Play và bảng tự đánh giá theo barem. Các con số kiểm thử được chạy lại ngày 23/11/2026 trên bản `main` dùng để viết báo cáo này.

## Môi trường

- Máy phát triển chạy Linux với JDK 21, Gradle 9.6.0 và Android Gradle Plugin 9.4.1.
- Thiết bị thật là một điện thoại Xiaomi (mã máy 2511FPC34G), Android 16, màn hình 1268×2756. Mọi lần chạy thử thủ công ở mục "Kiểm thử chức năng trên điện thoại thật" dùng máy này, cài bản debug qua adb.
- Dữ liệu là dự án Firebase thật (gói Spark) với tài khoản thử, gồm một khách hàng, một nhân viên và vài khách tạo thêm trong lúc thử.
- Kiểm thử rules chạy trên Firestore Emulator cục bộ, không đụng dữ liệu thật.

## Kiểm thử đơn vị

Bộ kiểm thử đơn vị có 391 ca trong 59 lớp kiểm thử, chạy bằng `./gradlew testDebugUnitTest`. Kết quả ngày 23/11/2026: 391 ca đạt, 0 thất bại, 0 bỏ qua, và cả bộ chạy hết khoảng một đến hai giây vì không cần mạng hay thiết bị. Các ViewModel được kiểm thử với repository giả (`fakes/Fakes.kt`), còn các quy tắc thuần Kotlin được kiểm thử trực tiếp. @tbl:unit-tests cho số ca theo nhóm.

Table: Kiểm thử đơn vị theo nhóm {#tbl:unit-tests}

| Nhóm | Số ca | Nội dung được kiểm tra |
|--------------|------|--------------------------------------------|
| `core` | 38 | `OtpManager` (hết hạn, dùng một lần, khoá sau 5 lần sai), `LockPolicy`, `AppLockObserver`, định dạng tiền và khoảng cách, bỏ dấu tiếng Việt, kiểm tra dữ liệu nhập |
| `face`, `ui/auth`, `ui/auth/sheet`, `ui/ekyc` | 44 | `FaceMatcher`, `BlinkDetector`, đăng nhập và đổi mật khẩu, trạng thái bottom sheet xác thực (OTP, quét mặt), nộp eKYC |
| `domain` (gốc) | 32 | `TransferPolicy`, `InterestCalculator`, `CustomerOnboardingService`, `FaceVerificationPolicy`, `QrPayload`, `MonthSummary` |
| `ui/customer` | 65 | ViewModel trang chủ, chi tiết tài khoản, nạp và rút, lịch sử, chuyển tiền, nhãn giao dịch, mã biên lai |
| `payment`, `data`, `data/routing` | 22 | Chữ ký và kết quả VNPay, giá trị mặc định của mô hình, đọc kết quả OSRM |
| `domain/utility`, `ui/customer/utility` | 69 | Tra hoá đơn, nhà mạng, danh mục, mã đặt chỗ, dựng `ServicePayment`; ViewModel ba kiểu màn tiện ích |
| `domain/branch`, `ui/customer/branch` | 39 | Khoảng cách, giờ mở cửa, chọn chi nhánh theo đường đi bộ và các tình huống lỗi, ViewModel bản đồ |
| `domain/assistant`, `ui/customer/assistant` | 45 | Đọc câu trả lời của AI, ngữ cảnh gửi kèm, chế độ cơ bản, chỉ dẫn hệ thống, ViewModel trợ lý, nút hành động |
| `ui/officer`, `ui/officer/customers`, `ui/officer/ekyc`, `ui/customer/profile` | 37 | Tạo, tìm, sửa khách, mở tài khoản, lãi suất, tổng quan, duyệt eKYC, hồ sơ khách |

Kiểm thử đơn vị không đụng tới Firestore thật, camera, ML Kit hay màn hình; những phần đó được kiểm tra bằng hai cách còn lại.

## Kiểm thử Security Rules

Security Rules là lớp bảo vệ duy nhất chạy ở máy chủ, nên tôi kiểm riêng bằng 49 ca trên Firestore Emulator (thư mục `rules-test/`, Node và thư viện `@firebase/rules-unit-testing`). Ca nào cũng ghép các thao tác ghi thật như ứng dụng làm (ví dụ batch bốn document của chuyển tiền) rồi kiểm tra rules chấp nhận hay từ chối. Kết quả ngày 23/11/2026: 49 ca đạt. @tbl:rules-tests cho số ca theo nhóm; các ca tiêu biểu đã được dẫn ở @tbl:db-invariants.

Table: Kiểm thử Security Rules {#tbl:rules-tests}

| Nhóm | Số ca | Nội dung |
|--------------|------|--------------------------------------------|
| Số dư và giao dịch (Feature 3) | 20 | Nạp, rút hợp lệ; sửa số dư không kèm giao dịch; sai số học; trùng id giao dịch; sửa tài khoản người khác; lãi suất; mở tài khoản vay |
| Chuyển tiền (Feature 4) | 14 | Chuyển nội bộ, giữa tài khoản mình, liên ngân hàng; cộng tiền không có lệnh trừ; giả thông tin người gửi; chuyển quá hạn mức |
| Danh bạ `accountIndex` (Feature 4) | 6 | Chỉ chủ tài khoản ghi; tên phải đúng hồ sơ; không liệt kê được |
| Thanh toán dịch vụ (Feature 5) | 9 | Thanh toán hợp lệ; quá số dư; quá hạn mức; trả hoá đơn hai lần; chiếm id hoá đơn của người khác |

Các rules của `users`, `phoneIndex` và `kyc` (Feature 1 và 2) chưa có ca kiểm thử tự động; tôi kiểm chúng bằng tay khi chạy thử trên điện thoại (ví dụ nhân viên tạo khách, khách nộp eKYC, nhân viên duyệt). Đây là chỗ nên bổ sung đầu tiên nếu có thêm thời gian.

## Kiểm thử chức năng trên điện thoại thật

Sau mỗi tính năng tôi chạy luồng chính trên điện thoại thật với rules thật trên Firebase. @tbl:device-tests tóm tắt những gì đã chạy đạt và những gì chưa thử, theo đúng ghi chép lúc đó.

Table: Kết quả chạy thử trên điện thoại thật {#tbl:device-tests}

| Tính năng (ngày) | Đã chạy đạt | Chưa thử |
|--------------|-------------------------------|------------------------|
| F1 và F2 (27/10) | Nhân viên tạo khách; khách đăng nhập lần đầu và đổi mật khẩu; eKYC và nhân viên duyệt; bật khoá sinh trắc học; đổi số điện thoại qua OTP; quét mặt đúng người qua, người khác bị từ chối; chọn ngưỡng 0,6 | |
| F3 (27/10) | Nhân viên mở tiết kiệm và khoản vay 1,5 tỉ đồng 240 tháng; đổi lãi suất tiết kiệm; khách nạp 2.000.000 ₫ qua OTP (mã hết hạn và gửi lại mã); rút quá số dư bị chặn; chi tiết tiết kiệm và khoản vay đúng số; lịch sử và giao dịch gần đây; nạp 10.000.000 ₫ với quét mặt | |
| F4 (28/10) | Nạp 1.000.000 ₫ qua VNPay sandbox (đóng WebView giữa chừng thì số dư không đổi); chuyển 500.000 ₫ theo số tài khoản (người nhận thấy tiền); chuyển liên ngân hàng; chuyển giữa tài khoản của mình; "Mã của tôi" | Quét QR bằng camera; quét mặt khi chuyển từ 10.000.000 ₫; chặn hạn mức 200 triệu (có unit test) |
| F5 (12/11) | Mua sắm 1.490.000 ₫; trả tiền điện qua OTP, tra lại thấy "đã thanh toán"; trả hoá đơn nước; lịch sử có biểu tượng dịch vụ | Quét mặt khi từ 10.000.000 ₫; hai máy trả cùng một hoá đơn cùng lúc |
| F6 (12/11) | Hộp thoại quyền vị trí (từ chối rồi cấp lại); gợi ý chi nhánh từ vị trí thật kèm dòng giải thích khi chi nhánh gần nhất phải đi vòng; danh sách xếp theo quãng đi bộ; chọn chi nhánh khác; gọi chi nhánh; tắt mạng thì dùng đường thẳng, bật lại thì có đường đi bộ; ép xoay ngang không lỗi | Đứng ở cổng trường để thấy ca "393 m nhưng đi bộ 1,3 km" (đã đo bằng OSRM); máy không có Play services |
| F7 (13/11) | Chip "Nạp tiền điện thoại" mở đúng màn và quay lại còn chat; hỏi chi tiêu tháng bằng AI thật (khoảng 6 giây) ra đúng "4.282.200 ₫ qua 3 giao dịch"; tin chứa OTP được nhắc và không gửi lên AI; chuyển sang chế độ cơ bản khi AI lỗi | Chế độ máy bay thật; hết hạn mức ngày của AI |

Ghi chú: các lần chạy này được ghi bằng văn bản ngay sau mỗi tính năng, chưa có video. Video demo nằm ngoài phạm vi của báo cáo này.

## Review và sửa lỗi

Sau khi xong mỗi tính năng, một agent AI độc lập review toàn bộ nhánh với ngữ cảnh mới (mục 2.6). @tbl:reviews cho số lỗi tìm được và đã sửa. Lỗi "Critical" là lỗi bảo mật hoặc mất dữ liệu, "Important" là lỗi ảnh hưởng chức năng đáng kể.

Table: Kết quả review toàn nhánh {#tbl:reviews}

| Nhánh | Critical | Important | Ví dụ lỗi tìm ra |
|--------------|----------|-----------|------------------------------|
| F4: chuyển tiền và VNPay | 0 | 6 | Chuyển cho chính tài khoản nguồn; thông tin người gửi trên biên lai nhận giả được; VNPay lỗi mạng khi ghi thì phải thử lại được |
| F5: tiện ích | 0 | 3 | Chiếm id hoá đơn của người khác; trùng mã biên lai hoá đơn; lỗi hạn mức hiện muộn |
| F6: chi nhánh | 0 | 6 | Chạm chi nhánh lúc đang định vị lại thì vẽ đường từ vị trí cũ; ở xa hơn 20 km bị báo nhầm lỗi mạng; lỗi API vị trí làm ứng dụng thoát |
| F7: trợ lý | 0 | 6 | Bốn lỗi Important cùng hai lỗi Minor được nâng mức, đã sửa kèm test |
| Chuẩn bị phát hành Play | 0 | 2 | Hai lỗi đã sửa trước khi đóng gói bản phát hành |

Feature 1, 2 và 3 cũng đã qua review toàn nhánh và các lỗi quan trọng đã sửa, nhưng ghi chép lúc đó không đếm theo từng mức nên tôi không đưa vào bảng. Các lỗi nhỏ còn lại (40 mục) được ghi trong `HANDOFF.md` để xử lý sau.

## Hạn chế đã biết

Các hạn chế sau là hệ quả của việc chọn gói Spark miễn phí và làm một mình. Chúng cho thấy ứng dụng này là bản demo của một ngân hàng.

- OTP và so khớp khuôn mặt chỉ kiểm tra trên điện thoại; người sửa APK có thể bỏ qua. Hướng khắc phục: Cloud Functions phát và kiểm token.
- Hạn mức 200.000.000 ₫ mỗi ngày chỉ kiểm ở ứng dụng. Security Rules chỉ chặn từng lần nạp, chuyển, thanh toán quá 100.000.000 ₫.
- Chữ ký VNPay được kiểm trên máy vì khoá sandbox nằm trong APK, và Security Rules không biết VNPay đã thu tiền hay chưa. Một khách rành kỹ thuật có thể tự "nạp" tiền giả; mỗi lần vẫn để lại một bản ghi và tối đa 100.000.000 ₫.
- Chuyển liên ngân hàng, dữ liệu hoá đơn, giá vé, danh mục tiện ích và 12 chi nhánh đều giả lập trong ứng dụng.
- Lãi tiết kiệm chỉ là lãi dự kiến, không được cộng vào số dư.
- Số tài khoản chỉ tra được sau khi chủ tài khoản đăng nhập ít nhất một lần, vì `accountIndex` do chính chủ ghi.
- Danh sách khách được tải hết rồi lọc ở máy; lịch sử lọc trên 100 giao dịch mới nhất. Đủ cho quy mô đồ án, không đủ cho hàng triệu bản ghi.
- Trợ lý AI phụ thuộc gói miễn phí của Gemini (có giới hạn lượt, hết thì dùng chế độ cơ bản). Câu hỏi và bản tóm tắt số liệu được gửi tới Google, và gói miễn phí có thể dùng dữ liệu để cải thiện sản phẩm; số tài khoản, số điện thoại, căn cước không được gửi.
- OSRM và tile OpenStreetMap là dịch vụ công cộng không cam kết luôn chạy. Toạ độ người dùng gửi tới máy chủ OSRM.
- App Check có mã nhưng chưa bật bắt buộc cho AI Logic.

## Trạng thái Google Play

Đề yêu cầu ứng dụng lên Google Play. Giảng viên đã trả lời ngày 13/11/2026 rằng việc này là tuỳ chọn, làm được thì được cộng điểm, không làm không bị trừ điểm. Tài khoản cá nhân mới còn phải thử kín ít nhất 12 người liên tục 14 ngày trước khi xin được bản chính thức, nên với hạn nộp 01/12/2026 tôi chọn phạm vi thu gọn. Tính đến ngày 13/11/2026 tôi đã làm xong phần chuẩn bị: file AAB đã ký bằng khoá upload (kiểm chữ ký đạt), App Check, chính sách bảo mật song ngữ đăng công khai, bản nháp mọi câu trả lời trong Play Console, icon, ảnh nổi bật và sáu ảnh chụp màn hình. Tôi chưa tải bản nào lên Google Play vì Google còn đang xác minh danh tính chủ tài khoản. Các tài liệu này nằm trong `docs/play/`.

## Tự đánh giá theo barem

@tbl:self-assessment đối chiếu từng tiêu chí của `Marking.xlsx` với nơi trình bày trong báo cáo và điểm tôi tự chấm, kèm lý do ở những chỗ không chấm tối đa.

Table: Tự đánh giá theo barem {#tbl:self-assessment}

| Tiêu chí | Tối đa | Trình bày ở | Tự chấm | Lý do |
|--------------|------|--------------|------|-----------------------------|
| Architecture | 1 | Chương 2 | 1,0 | Có sơ đồ kiến trúc, triển khai, bảng công nghệ kèm lý do chọn và phương án thay thế |
| Class diagrams | 1 | Mục 3.2 | 1,0 | Mười một sơ đồ theo tầng và tính năng, có đủ các loại quan hệ |
| UI | 1 | Chương 5 | 0,9 | Design system thống nhất; chưa qua đánh giá của người dùng ngoài |
| Database design | 1 | Mục 3.4 | 1,0 | Sơ đồ ER, mô tả từng collection, chỉ mục, bất biến kèm ca kiểm thử |
| User profiles | 1 | Mục 4.3 | 1,0 | Hai vai trò với giao diện và quyền khác nhau, nhân viên tạo và sửa khách |
| Security requirements | 1 | Mục 4.4 | 0,9 | eKYC, khoá sinh trắc, OTP, quét mặt; OTP và khuôn mặt chỉ kiểm trên máy |
| Account management | 0,5 | Mục 4.5 | 0,5 | Ba loại tài khoản, lãi, tiền trả khoản vay, lịch sử, nạp, rút |
| Transaction management | 1 | Mục 4.6 | 0,9 | Chuyển nội bộ và liên ngân hàng, VNPay; liên ngân hàng giả lập, hạn mức ngày chỉ kiểm ở ứng dụng |
| Utilities | 0,5 | Mục 4.7 | 0,5 | Bảy tiện ích chạy được, dữ liệu giả lập |
| Navigation | 0,5 | Mục 4.8 | 0,5 | Bản đồ, vị trí, gợi ý đường đi bộ ngắn nhất, có dự phòng |
| Chatbot | 0,5 | Mục 4.9 | 0,5 | Trợ lý AI kèm nút mở màn, có chế độ cơ bản |
| Hình thức báo cáo, demo | 1 | Toàn báo cáo | 0,9 | Theo mẫu của Khoa; phần video chưa làm trong báo cáo này |
| Tổng | 10 | | 9,6 | |

Phần tự chấm chỉ là ý kiến của tôi; điểm chính thức do giảng viên quyết định.
