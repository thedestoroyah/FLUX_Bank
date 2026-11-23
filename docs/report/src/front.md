## LỜI CẢM ƠN

Tôi xin chân thành cảm ơn thầy Võ Văn Thành, giảng viên môn Phát triển ứng dụng di động, đã giảng dạy, ra đề tài đồ án và trả lời kịp thời các câu hỏi của tôi trong quá trình thực hiện. Tôi cũng cảm ơn Khoa Công nghệ Thông tin, Trường Đại học Tôn Đức Thắng đã tạo điều kiện để tôi hoàn thành đồ án này.

## TÓM TẮT

Đồ án xây dựng FLUX Bank, một ứng dụng ngân hàng di động trên Android theo đề của môn Phát triển ứng dụng di động. Ứng dụng có hai giao diện cho khách hàng và nhân viên ngân hàng; xác thực bằng mật khẩu, sinh trắc học, mã OTP và xác thực khuôn mặt cho giao dịch từ 10.000.000 đồng; ba loại tài khoản Checking, Saving và Mortgage; chuyển tiền trong và ngoài ngân hàng, nạp tiền qua cổng VNPay; bảy tiện ích thanh toán; bản đồ chi nhánh với đường đi bộ ngắn nhất; và trợ lý AI dùng Gemini có chế độ dự phòng.

Ứng dụng viết bằng Kotlin theo mô hình MVVM, lưu dữ liệu trên Cloud Firestore và chạy hoàn toàn trên gói miễn phí Firebase Spark. Vì gói này không có máy chủ riêng, số dư được bảo vệ bằng Firestore transaction kết hợp Security Rules, còn OTP và so khớp khuôn mặt được kiểm tra trên thiết bị. Báo cáo trình bày kiến trúc và lý do chọn công nghệ, use case, class diagram, sơ đồ tuần tự, thiết kế cơ sở dữ liệu, hiện thực, giao diện, kết quả kiểm thử (391 ca kiểm thử đơn vị, 49 ca kiểm thử Security Rules, chạy thử trên điện thoại thật) và các hạn chế đã biết.

## ABSTRACT

This project builds FLUX Bank, an Android mobile banking app for the Mobile Application Development course. The app offers separate interfaces for customers and bank officers. It authenticates users with passwords, biometrics and one-time passwords, and adds face verification for transactions of 10,000,000 VND or more. It supports checking, saving and mortgage accounts, transfers within and across banks, deposits through the VNPay gateway, seven payment utilities, a branch map with the shortest walking route, and an AI assistant based on Gemini with a fallback mode.

The app is written in Kotlin with the MVVM pattern, stores its data in Cloud Firestore and runs entirely on the free Firebase Spark plan. Because that plan provides no server of its own, balances are protected by Firestore transactions combined with Security Rules, while the one-time password and face matching are checked on the device. The report presents the architecture and the reasons for each technology choice, use cases, class diagrams, sequence diagrams, the database design, the implementation, the user interface, test results (391 unit tests, 49 Security Rules tests, and runs on a real phone) and the known limitations.

## CHỮ VIẾT TẮT

| Viết tắt | Ý nghĩa |
|---|---|
| AAB | Android App Bundle |
| ADB | Android Debug Bridge |
| AI | Artificial Intelligence, trí tuệ nhân tạo |
| API | Application Programming Interface |
| APK | Android Package |
| BoM | Bill of Materials |
| eKYC | Electronic Know Your Customer, định danh khách hàng điện tử |
| ER | Entity Relationship, thực thể quan hệ |
| HMAC | Hash-based Message Authentication Code |
| JDK | Java Development Kit |
| JSON | JavaScript Object Notation |
| MVVM | Model-View-ViewModel |
| OTP | One-Time Password, mật khẩu dùng một lần |
| OSM | OpenStreetMap |
| OSRM | Open Source Routing Machine |
| QR | Quick Response |
| SMS | Short Message Service |
| UML | Unified Modeling Language |
| UI | User Interface, giao diện người dùng |
| VND | Việt Nam đồng |
| XML | Extensible Markup Language |

## GHI CHÚ AI

Các công cụ AI đã hỗ trợ trong quá trình thực hiện, cùng phần việc do tôi tự làm, được trình bày ở mục 1.6.
