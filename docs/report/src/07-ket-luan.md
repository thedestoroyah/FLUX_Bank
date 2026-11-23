# KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

## Kết quả

FLUX Bank là một ứng dụng ngân hàng di động Android hoàn chỉnh về mặt chức năng. Ứng dụng có hai giao diện cho hai loại người dùng, xác thực bằng mật khẩu, sinh trắc học, OTP và khuôn mặt, ba loại tài khoản, chuyển tiền trong và ngoài ngân hàng, nạp tiền qua VNPay sandbox, bảy tiện ích, bản đồ chi nhánh với đường đi bộ ngắn nhất và một trợ lý AI có chế độ dự phòng. Dữ liệu nằm trên Cloud Firestore, được bảo vệ bằng Security Rules đã kiểm thử. Ứng dụng có 391 ca kiểm thử đơn vị và 49 ca kiểm thử Security Rules, đều đạt, và được tôi chạy thử từng tính năng trên điện thoại thật.

Về báo cáo, tôi đã trình bày kiến trúc cùng lý do chọn công nghệ, mười class diagram, sơ đồ use case, sơ đồ tuần tự và sơ đồ ER, cùng mô tả hiện thực và giao diện. Chương 6 ghi rõ những gì chưa làm được hoặc làm ở mức giả lập để người đọc không phải đoán.

## Bài học

- Ràng buộc chi phí quyết định kiến trúc. Chọn gói Spark miễn phí nghĩa là không có chỗ chạy code tin cậy ở máy chủ, nên số dư phải được bảo vệ bằng Firestore transaction cộng Security Rules, còn OTP và khuôn mặt chỉ kiểm được trên máy. Biết sớm điều này giúp tôi thiết kế quanh giới hạn thay vì phát hiện nó ở cuối.
- Code do công cụ AI viết cần được đọc và đối chiếu với đề. Bản đầu của Feature 1 và 2 chạy được nhưng thiếu nhiều thứ đề yêu cầu, và chỉ khi đặt nó cạnh đề bài tôi mới thấy chỗ thiếu.
- Review bởi một người (hoặc agent) không tham gia viết tìm ra lỗi mà tác giả bỏ sót. Ví dụ một khách có thể "chiếm" mã hoá đơn của người khác để chặn họ thanh toán; tôi không nghĩ tới tình huống này cho tới khi lần review của Feature 5 chỉ ra.
- Kiểm thử trên máy thật tìm ra lỗi mà kiểm thử đơn vị không thấy. Ở Feature 3, thanh tab biến mất sau khi vuốt back, bàn phím che sheet nhập lãi suất, và chỉ mục Firestore tạo nhầm chiều sắp xếp đều chỉ lộ ra khi chạy trên điện thoại.
- Dịch vụ miễn phí mong manh và phải có phương án dự phòng. Google Maps bị bỏ vì không lập được tài khoản thanh toán, mô hình Gemini miễn phí chỉ cho 20 lượt mỗi ngày, máy chủ OSRM công cộng có thể bận. Mỗi chỗ phụ thuộc như vậy trong ứng dụng đều có đường dự phòng (chế độ cơ bản, đường thẳng).
- Viết tài liệu thiết kế và mockup trước khi code làm giảm việc phải làm lại, nhất là ở giao diện, nơi tôi dễ đổi ý khi chỉ nhìn một trang HTML.

## Hướng phát triển

- Cloud Functions trên gói Blaze để phát và kiểm token xác thực ở máy chủ (đóng lỗ hổng sửa APK bỏ qua OTP và khuôn mặt), kiểm kết quả VNPay ở máy chủ, và cộng lãi tiết kiệm vào số dư định kỳ.
- Chuyển liên ngân hàng thật qua mạng liên ngân hàng và SMS thật cho OTP.
- Kiểm tra người thật mạnh hơn ở bước quét mặt, chống cả video và ảnh; đo ngưỡng khuôn mặt trên nhiều người hơn.
- Bật bắt buộc App Check, tính hạn mức ngày ở máy chủ, và bổ sung kiểm thử tự động cho rules của hồ sơ người dùng và eKYC.
- Hoàn thành đường lên Google Play: thử kín, rồi bản chính thức.
- Lưu lịch sử trò chuyện của trợ lý và cho phép nó điền sẵn form, với điều kiện mọi giao dịch vẫn đi qua xác thực.
