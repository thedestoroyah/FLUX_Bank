# FLUX Bank (Demo) — Chính sách bảo mật / Privacy Policy

Hiệu lực từ / Effective date: 2026-11-13

---

## Tiếng Việt

**FLUX Bank (Demo)** là ứng dụng mô phỏng ngân hàng số, làm cho đồ án môn *Mobile Application Development* tại Trường Đại học Tôn Đức Thắng. Đây **không phải ngân hàng thật**: mọi tài khoản, số dư và giao dịch đều là giả lập, không có tiền thật và không liên kết với ngân hàng nào.

### Dữ liệu chúng tôi thu thập
- **Thông tin tài khoản:** họ tên, email, số điện thoại, địa chỉ, số CCCD. Nhân viên (tài khoản quản trị của đồ án) nhập khi tạo khách hàng; bạn có thể tự sửa SĐT và địa chỉ.
- **Ảnh eKYC:** ảnh CCCD và ảnh chân dung, cùng đặc trưng khuôn mặt dạng số tính trên máy, dùng để xác minh danh tính và để xác nhận giao dịch giá trị lớn.
- **Dữ liệu tài chính giả lập:** số dư, lịch sử giao dịch, hoá đơn và vé đã thanh toán trong app.
- **Vị trí chính xác:** chỉ khi bạn mở bản đồ chi nhánh và cho phép, để gợi ý chi nhánh gần nhất. Vị trí **không được lưu**.
- **Nội dung trò chuyện với Trợ lý AI:** câu bạn hỏi và một bản tóm tắt số liệu (số dư theo loại tài khoản, tổng chi theo nhóm trong tháng). Không gửi số tài khoản, số điện thoại, số CCCD hay tên người nhận. Cuộc trò chuyện không được lưu.
- **Định danh ứng dụng/thiết bị:** do Firebase và App Check tạo, để đăng nhập và chống ứng dụng giả mạo.

Sinh trắc học (vân tay/khuôn mặt) dùng để mở khoá app được xử lý hoàn toàn bởi hệ điều hành, app không nhận được dữ liệu này.

### Dữ liệu được gửi tới ai
- **Google Firebase** (Authentication, Cloud Firestore, App Check): nơi lưu tài khoản, eKYC và giao dịch.
- **Google Gemini qua Firebase AI Logic:** nhận câu hỏi và số liệu tóm tắt khi bạn dùng Trợ lý AI. Ở gói miễn phí, Google có thể dùng nội dung này để cải thiện sản phẩm. **Đừng nhập mật khẩu hay mã OTP vào Trợ lý**; app cũng tự chặn những tin như vậy.
- **Máy chủ chỉ đường OSRM (FOSSGIS) và OpenStreetMap:** nhận toạ độ của bạn và của chi nhánh để vẽ bản đồ và tính đường đi bộ.
- **VNPay (môi trường sandbox):** khi bạn nạp tiền thử, bạn nhập thông tin thẻ thử nghiệm trực tiếp trên trang của VNPay; app không lưu thông tin thẻ.

Chúng tôi không bán dữ liệu, không dùng quảng cáo.

### Bảo mật và lưu trữ
Dữ liệu được mã hoá khi truyền (HTTPS). Quyền truy cập được giới hạn bởi Firebase Security Rules: mỗi khách chỉ đọc được dữ liệu của mình. Dữ liệu được giữ đến khi đồ án kết thúc hoặc khi bạn yêu cầu xoá.

### Quyền của bạn
Bạn có thể yêu cầu xem hoặc xoá toàn bộ dữ liệu của mình bằng cách gửi email tới **datpham1365@gmail.com**. Chúng tôi xử lý trong vòng 7 ngày.

### Liên hệ
datpham1365@gmail.com

---

## English

**FLUX Bank (Demo)** is a simulated mobile banking app built as a university project for the *Mobile Application Development* course at Ton Duc Thang University. It is **not a real bank**: all accounts, balances and transactions are simulated, no real money is involved, and the app is not affiliated with any bank.

### Data we collect
- **Account information:** full name, email, phone number, address, national ID number, entered by a project officer account when creating a customer; you can edit your phone number and address.
- **eKYC images:** ID card photo and selfie, plus a numeric face template computed on the device, used to verify identity and to confirm high-value transactions.
- **Simulated financial data:** balances, transaction history, bills and tickets paid in the app.
- **Precise location:** only when you open the branch map and grant permission, to suggest the nearest branch. Location is **not stored**.
- **AI assistant messages:** your question plus a summary of figures (balance per account type, monthly spending per category). No account numbers, phone numbers, national ID or recipient names are sent. Conversations are not stored.
- **App/device identifiers:** created by Firebase and App Check for sign-in and to block tampered apps.

Biometric unlock (fingerprint/face) is handled entirely by the operating system; the app never receives that data.

### Who receives data
- **Google Firebase** (Authentication, Cloud Firestore, App Check): stores accounts, eKYC data and transactions.
- **Google Gemini via Firebase AI Logic:** receives your question and the figure summary when you use the AI assistant. On the free tier Google may use this content to improve its products. **Do not type passwords or OTP codes into the assistant**; the app also blocks such messages.
- **OSRM routing server (FOSSGIS) and OpenStreetMap:** receive your coordinates and the branch coordinates to draw the map and compute the walking route.
- **VNPay (sandbox):** when you top up with a test card, you enter it directly on VNPay's page; the app does not store card data.

We do not sell data and show no ads.

### Security and retention
Data is encrypted in transit (HTTPS). Access is limited by Firebase Security Rules: each customer can only read their own data. Data is kept until the project ends or until you ask us to delete it.

### Your rights
You can ask to see or delete all your data by emailing **datpham1365@gmail.com**. We handle requests within 7 days.

### Contact
datpham1365@gmail.com
