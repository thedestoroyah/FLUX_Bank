# Danh sách ảnh màn hình (báo cáo chương 5 + kịch bản video)

Mỗi dòng là một ảnh `images/ui/<id>.png` (1268×2756, chụp `adb exec-out screencap -p` trên Xiaomi).
Chưa có ảnh thật thì `build.py` tự tạo ảnh giữ chỗ cùng tên; chụp xong chỉ cần ghi đè file rồi dựng lại.
Trước khi chụp: tắt nút trợ năng nổi, bật Không làm phiền, đổi tên khách thử; ảnh bản đồ không lộ vị trí nhà.

| id | màn hình | ghi chú |
|---|---|---|
| auth-login | Đăng nhập | có dòng chú thích demo |
| auth-change-password | Đổi mật khẩu lần đầu | khách mới do nhân viên tạo |
| auth-unlock | Mở khoá sinh trắc học | sau ≥ 60 s ở nền |
| cus-home | Trang chủ khách hàng | thẻ Titanium, lưới tiện ích 4×3 |
| cus-account-checking | Chi tiết tài khoản thanh toán | số dư, nạp/rút |
| cus-account-saving | Chi tiết tài khoản tiết kiệm | lãi suất, lãi dự kiến/tháng |
| cus-account-mortgage | Chi tiết khoản vay thế chấp | tiền trả hàng tháng / 2 tuần |
| cus-deposit | Nạp tiền | chọn nguồn, số tiền |
| cus-withdraw | Rút tiền | chặn quá số dư |
| cus-receipt | Biên lai giao dịch | sau nạp/rút/chuyển |
| cus-history | Lịch sử giao dịch | lọc theo tài khoản |
| cus-history-month | Giao dịch tháng N | mở từ Trợ lý |
| cus-transfer-hub | Chọn kiểu chuyển tiền | trong FLUX / của tôi / liên ngân hàng |
| cus-transfer-internal | Chuyển trong FLUX | tra tên theo số tài khoản |
| cus-transfer-external | Chuyển liên ngân hàng | chọn ngân hàng |
| cus-transfer-own | Chuyển giữa tài khoản của tôi | Thanh toán → Tiết kiệm |
| cus-auth-otp | Sheet xác thực OTP | bàn phím số riêng |
| cus-sms-otp | Thông báo SMS giả lập | chạm để tự điền |
| cus-auth-face | Bước quét khuôn mặt | giao dịch ≥ 10.000.000 ₫ |
| cus-vnpay | Nạp tiền qua VNPay sandbox | WebView |
| cus-qr-scan | Quét mã QR | `fluxbank:<số TK>` |
| cus-qr-mine | Mã QR của tôi | |
| cus-bill-electric | Thanh toán tiền điện | tra hoá đơn |
| cus-bill-paid | Hoá đơn đã thanh toán | không trả lần hai |
| cus-bill-water | Thanh toán tiền nước | SAWACO, tra theo mã khách hàng |
| cus-topup | Nạp tiền điện thoại | nhà mạng theo đầu số |
| cus-catalog-flight | Vé máy bay | danh mục |
| cus-catalog-hotel | Đặt phòng khách sạn | danh mục |
| cus-catalog-movie | Vé xem phim | suất đã qua giờ bị khoá |
| cus-catalog-shop | Mua sắm | lưới sản phẩm |
| cus-payment-receipt | Biên lai thanh toán dịch vụ | mã đặt chỗ |
| cus-branch-suggest | Gợi ý chi nhánh đi bộ gần nhất | đường emerald trên bản đồ OSM |
| cus-branch-list | Danh sách chi nhánh | xếp theo quãng đi bộ |
| cus-branch-no-location | Chưa có quyền vị trí | |
| cus-assistant-ai | Trợ lý AI trả lời kèm nút | |
| cus-assistant-basic | Trợ lý ở chế độ cơ bản | khi AI lỗi |
| cus-assistant-otp | Tin chứa OTP được chặn | trả lời trên máy |
| cus-profile | Hồ sơ khách hàng | |
| cus-profile-phone | Đổi số điện thoại qua OTP | |
| off-overview | Tổng quan nhân viên | |
| off-customers | Danh sách / tìm khách hàng | tìm không dấu |
| off-create-customer | Tạo khách hàng | |
| off-customer-detail | Chi tiết và sửa khách hàng | |
| off-open-account | Mở tài khoản tiết kiệm / vay | xem trước tiền trả |
| off-ekyc-capture | Chụp eKYC hộ khách | |
| off-ekyc-review | Duyệt / từ chối eKYC | có lý do |
| off-rates | Sửa lãi suất | |
