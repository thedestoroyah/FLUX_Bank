# GIỚI THIỆU

## Bối cảnh và lý do chọn đề tài

Các ngân hàng ở Việt Nam đã đưa gần như mọi giao dịch lên ứng dụng di động. Khách hàng chuyển tiền, đóng hoá đơn, mua vé hay hỏi lãi suất ngay trên điện thoại mà không cần tới quầy hay cây ATM. Đề đồ án môn Phát triển ứng dụng di động yêu cầu xây dựng một ứng dụng như vậy trên Android. Ứng dụng có hai loại người dùng (khách hàng và nhân viên ngân hàng), xác thực hai yếu tố, ba loại tài khoản, chuyển tiền kết nối cổng thanh toán, các tiện ích hằng ngày, bản đồ chi nhánh và một trợ lý AI.

Tôi chọn đề tài này vì nó buộc phải giải quyết những vấn đề thật của một ứng dụng tài chính, ngoài việc vẽ màn hình: dữ liệu dùng chung giữa nhiều người dùng, tính đúng đắn của số dư khi nhiều thao tác cùng lúc, và bảo mật khi không có máy chủ riêng. Tôi đặt tên ứng dụng là FLUX Bank. Ứng dụng là bản demo, tiền và ngân hàng đều giả lập, và màn hình đăng nhập ghi rõ điều đó.

## Mục tiêu

Mục tiêu của đồ án là xây dựng một ứng dụng Android chạy ổn định, đáp ứng đủ bảy nhóm tính năng của đề, và một báo cáo trình bày kiến trúc, thiết kế, cách hiện thực và giao diện. Có hai ràng buộc tôi tự đặt từ đầu: toàn bộ hệ thống chạy trên gói miễn phí Firebase Spark (chi phí bằng không), và mọi quyết định thiết kế phải giải thích được lý do. Ràng buộc thứ nhất ảnh hưởng nhiều nhất tới kiến trúc nên được bàn kỹ ở chương 2.

## Đối chiếu yêu cầu của đề

@tbl:requirements đối chiếu từng yêu cầu của đề với những gì đã làm và nơi trình bày trong báo cáo.

Table: Đối chiếu yêu cầu của đề với ứng dụng {#tbl:requirements}

| Yêu cầu của đề | Đã làm trong FLUX Bank | Trình bày ở |
|--------------------|-----------------------------------|----------|
| 1. Hồ sơ người dùng: hai loại người dùng, nhân viên tạo tài khoản và sửa dữ liệu khách, khách xem và sửa thông tin | Hai giao diện riêng; nhân viên tạo, tìm, sửa khách và mở tài khoản; khách xem và sửa địa chỉ, ảnh, số điện thoại | 4.3, chương 5 |
| 2. Bảo mật: đăng nhập, eKYC bằng khuôn mặt, xác thực giao dịch giá trị lớn, OTP cho mọi giao dịch | Đăng nhập và khoá sinh trắc học; eKYC với ML Kit và MobileFaceNet; quét mặt từ 10.000.000 ₫; OTP cho mọi giao dịch | 4.4 |
| 3. Quản lý tài khoản: Checking, Saving, Mortgage; lãi và tiền trả định kỳ; lịch sử; nạp, rút; nhân viên sửa lãi suất | Ba loại tài khoản với giao diện riêng; lãi dự kiến; tiền trả hàng tháng hoặc hai tuần; lịch sử; nạp, rút; tab lãi suất cho nhân viên | 4.5 |
| 4. Giao dịch: kiểm tra trước khi ghi; chuyển cùng ngân hàng và khác ngân hàng; tích hợp VNPay hoặc Stripe | `TransferPolicy` và Security Rules kiểm tra; ba kiểu chuyển; nạp tiền qua VNPay sandbox | 4.6 |
| 5. Tiện ích: điện, nước; nạp điện thoại; vé máy bay, vé phim, khách sạn, mua hàng | Bảy tiện ích qua ba kiểu màn hình | 4.7 |
| 6. Điều hướng: vị trí người dùng, bản đồ chi nhánh gần, gợi ý đường đi bộ ngắn nhất | Bản đồ OpenStreetMap, 12 chi nhánh, đường đi bộ ngắn nhất từ OSRM | 4.8 |
| 7. Chatbot: trả lời kèm liên kết tới hoạt động được hỏi | Trợ lý Gemini, mỗi câu trả lời kèm tối đa ba nút mở màn | 4.9 |
| Kỹ thuật: cơ sở dữ liệu trên cloud | Cloud Firestore | 3.4 |
| Kỹ thuật: triển khai trên Google Play | Đã chuẩn bị bản phát hành; chưa tải lên (tuỳ chọn) | 6.7 |
| Kỹ thuật: báo cáo phân tích thiết kế, hiện thực, giao diện | Chương 2 đến 5 | |
| Kỹ thuật: video demo | Ngoài phạm vi báo cáo này; giao diện được trình bày bằng ảnh màn hình ở chương 5 | 5 |

## Phạm vi và ràng buộc

- Đề nêu nhóm từ 2 đến 3 người, còn đồ án này do một mình tôi thực hiện.
- Chi phí bằng không, nên mọi thứ chạy trên Firebase Spark: không Cloud Functions, không Storage, không SMS thật. Hệ quả của điều này xuất hiện ở hầu hết các chương, nhất là ở mục 2.3 và 6.6.
- Mọi giao dịch dùng tiền giả lập. Chuyển liên ngân hàng, dữ liệu hoá đơn, giá vé và 12 chi nhánh được giả lập trong ứng dụng; riêng nạp tiền đi qua cổng VNPay sandbox thật, và bản đồ, đường đi dùng OpenStreetMap và OSRM thật.
- Giảng viên đã xác nhận ngày 13/11/2026 rằng việc đưa ứng dụng lên Google Play là tuỳ chọn, nên tôi dành thời gian chính cho ứng dụng và báo cáo.
- Chữ hiển thị trong ứng dụng là tiếng Việt, tiền tệ là đồng (VND) và được lưu dạng số nguyên.

## Công cụ thực hiện

Ứng dụng được viết bằng Kotlin trong Android Studio, build bằng Gradle. Mã nguồn quản lý bằng Git, đặt trên một kho riêng ở GitHub. Phần dịch vụ phía sau cấu hình qua Firebase Console (Authentication, Firestore, AI Logic, App Check). Giao diện được dựng thử bằng các trang HTML trước khi viết XML. Báo cáo viết bằng Markdown và dựng thành file Word theo mẫu của Khoa bằng pandoc và một script Python, sơ đồ vẽ bằng PlantUML. Phiên bản các thư viện và lý do chọn nằm ở mục 2.4.

## Công cụ AI hỗ trợ

Tôi dùng công cụ AI ở nhiều khâu của đồ án, và cần nói rõ việc nào do công cụ làm, việc nào do tôi làm.

- Google Antigravity viết bản code ban đầu cho Feature 1 và 2 (hồ sơ người dùng, bảo mật). Bản này chạy được nhưng chưa đủ so với đề (nhân viên chưa tạo và sửa được khách, OTP chưa gắn với giao dịch, chưa có xác thực khuôn mặt cho giao dịch lớn) và giao diện không đồng bộ, nên tôi đã làm lại toàn bộ giao diện và hoàn thiện hai tính năng này.
- Claude Code (của Anthropic) hỗ trợ viết tài liệu thiết kế và kế hoạch cho từng tính năng, viết code Feature 3 đến 7 cùng phần làm lại của Feature 1 và 2, viết bộ kiểm thử, và làm các agent review toàn nhánh độc lập (mục 2.6). Claude Code cũng soạn phần văn bản của báo cáo này từ mã nguồn và tài liệu thiết kế của dự án.
- Gemini nằm trong sản phẩm, không dùng để làm ra nó: trợ lý AI trong ứng dụng (mục 4.9) gọi Gemini lúc chạy.

Phần việc tôi tự quyết định và tự làm gồm chọn phạm vi và công nghệ, ví dụ chuyển từ Google Maps sang OpenStreetMap khi Google Cloud từ chối lập tài khoản thanh toán. Tôi duyệt từng tài liệu thiết kế, mockup và kế hoạch trước khi cho viết code, cấu hình Firebase, VNPay và Play Console, và tạo khoá ký ứng dụng. Tôi chạy thử mọi tính năng trên điện thoại thật, quyết định cách sửa mỗi lỗi tìm ra, và đọc, kiểm chứng nội dung báo cáo. Code và văn bản đều có công cụ AI tham gia, nên các lần review nhắc trong báo cáo đều là review bằng máy, người review không phải người khác.

## Bố cục báo cáo

Báo cáo gồm bảy chương. Chương 1 là chương này. Chương 2 trình bày kiến trúc và công nghệ. Chương 3 là phần phân tích và thiết kế: use case, class diagram, sơ đồ tuần tự và cơ sở dữ liệu. Chương 4 mô tả hiện thực từng tính năng. Chương 5 trình bày giao diện. Chương 6 là kiểm thử, hạn chế và tự đánh giá. Chương 7 kết luận và nêu hướng phát triển. Sau đó là danh sách tài liệu tham khảo.
