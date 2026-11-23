# GIAO DIỆN NGƯỜI DÙNG

Chương này trình bày giao diện của FLUX Bank: nguyên tắc thiết kế chung, khung điều hướng của hai vai trò, rồi từng nhóm màn hình theo tính năng. Mỗi nhóm màn hình có một hình ghép ảnh chụp và phần mô tả những việc người dùng làm được trên đó.

## Nguyên tắc thiết kế

### Hướng thiết kế

Giao diện theo hướng một ứng dụng ngân hàng hiện đại: nền sáng, chữ đậm, ít trang trí. Tôi gọi hệ thống thiết kế này là "The Titanium Ledger" (cuốn sổ cái bằng titan): độ tin cậy đến từ sự chính xác, như số tiền thẳng cột, khoảng cách theo một thang cố định và viền mảnh, không đến từ màu mè. Vật thể duy nhất được làm nổi là thẻ tài khoản màu titan ở trang chủ. Ứng dụng chỉ có giao diện sáng, toàn bộ chữ hiển thị là tiếng Việt và tiền tệ viết dạng `48.250.000 ₫`.

Hệ thống này được ghi lại trong `DESIGN.md` ở thư mục gốc của dự án, rút ra từ các file `res/values/` của app để cả người lẫn công cụ AI đọc được. Code trong `res/values/` vẫn là nguồn gốc. Mọi màn hình mới phải qua một bước nữa: tôi xem và duyệt một trang HTML mô phỏng (thư mục `docs/superpowers/specs/mockups/`) trước khi cho viết XML.

### Màu sắc

@fig:ds-colors là bảng màu, được dựng trực tiếp từ `DESIGN.md`.

![Bảng màu của FLUX Bank](ui/ds-colors.png){#fig:ds-colors width=15.5cm}

Màu được dùng theo quy tắc chặt. Nền cửa sổ là xám nhạt `#F5F5F7`, bề mặt (card, sheet, thanh tab) màu trắng, chữ chính màu obsidian `#0A0D12`. Nút hành động chính luôn màu đen obsidian, không bao giờ màu xanh. Màu xanh ngọc (emerald `#10B981`) chỉ xuất hiện ở chỗ người dùng chạm hoặc đang tương tác: nút quét QR nổi giữa thanh tab, tab đang chọn, ô OTP đang nhập, viền ô nhập khi focus và số tiền vào. Sáu màu nhóm (xanh ngọc, vàng, xanh dương, xanh trời, hồng, tím) chỉ nằm trong ô icon của các tiện ích. Trang chủ có một dải màu mint nhạt phía sau thẻ để làm nổi thẻ mà không cần thêm màu nào khác.

### Chữ, khoảng cách và hình khối

@fig:ds-type là thang chữ. Cả ứng dụng dùng một họ chữ duy nhất, Inter, phân cấp bằng độ đậm và cỡ chữ. Không chữ nào nhỏ hơn 12 sp. Mọi số tiền dùng chữ số đều chiều rộng (`tnum`) để các cột số thẳng hàng.

![Thang chữ Inter của FLUX Bank](ui/ds-type.png){#fig:ds-type width=15.5cm}

Khoảng cách chỉ lấy từ sáu mức 4, 8, 12, 16, 24 và 32 dp, và lề ngang của màn hình là 16 dp. Góc bo tăng dần theo kích thước vật thể: ô OTP 12 dp, nút và ô nhập 14 dp, tile 20 dp, thẻ và đỉnh bottom sheet 24 dp. Card và tile phẳng, với viền mảnh 1 dp. Bóng đổ chỉ dành cho hai vật thể nổi là thẻ tài khoản và nút QR. Vùng chạm tối thiểu là 48 dp và nút cao 52 dp.

Icon lấy từ bộ Phosphor Icons [17] giấy phép MIT, hai kiểu: duotone (hai tông) cho tính năng và tiện ích, regular (nét mảnh) cho các phần khung như nút back. Font Inter [18] có giấy phép OFL. Mọi giá trị màu, khoảng cách, kiểu chữ đều lấy từ `colors.xml`, `dimens.xml` và các style `TextAppearance.Flux.*`, `Widget.Flux.*`; không có mã màu hay kích thước viết cứng trong layout.

## Khung điều hướng

Ứng dụng khách hàng (`CustomerMainActivity`) có thanh tab dưới gồm năm mục: Trang chủ, Chuyển tiền, Quét QR (nút nổi ở giữa), Lịch sử và Cá nhân. Ứng dụng nhân viên (`OfficerMainActivity`) cũng có năm tab: Tổng quan, Khách hàng, eKYC, Lãi suất và Cá nhân. Các màn phụ (chi tiết tài khoản, biên lai, màn thanh toán...) mở chồng lên bằng `openScreen`; khi đó thanh tab ẩn đi và màn có nút quay lại ở đầu. Vuốt từ mép trái để quay lại cũng hoạt động.

## Màn hình chung

@fig:ui-auth gồm các màn trước khi vào ứng dụng, dùng chung cho hai vai trò.

::: {#fig:ui-auth .phones caption="Màn hình chung: (a) đăng nhập, có dòng chú thích đây là ứng dụng demo; (b) đổi mật khẩu bắt buộc ở lần đăng nhập đầu; (c) mở khoá bằng sinh trắc học khi quay lại sau một phút ở nền"}
![](ui/auth-login.png)
![](ui/auth-change-password.png)
![](ui/auth-unlock.png)
:::

Màn đăng nhập yêu cầu email và mật khẩu; không có nút đăng ký vì tài khoản do nhân viên tạo. Khách mới đăng nhập bằng mật khẩu tạm và bị đưa thẳng tới màn đổi mật khẩu. Nếu đã bật khoá sinh trắc học, ứng dụng hiện màn mở khoá mỗi khi mở lại hoặc quay lại sau từ 60 giây ở nền.

## Ứng dụng khách hàng

### Trang chủ và tài khoản

Trang chủ có thẻ tài khoản Titanium với số dư (có nút ẩn hoặc hiện số dư), lưới tiện ích 4 cột và danh sách giao dịch gần đây. Chạm vào thẻ mở chi tiết tài khoản, và chi tiết khác nhau theo loại tài khoản.

::: {#fig:ui-accounts .phones caption="Trang chủ và chi tiết tài khoản: (a) trang chủ với thẻ Titanium và lưới tiện ích; (b) tài khoản thanh toán Checking với số dư, nút nạp và rút; (c) tài khoản tiết kiệm Saving với lãi suất và lãi dự kiến mỗi tháng"}
![](ui/cus-home.png)
![](ui/cus-account-checking.png)
![](ui/cus-account-saving.png)
:::

Tài khoản Checking và Saving có số dư và hai nút nạp, rút. Tài khoản Saving thêm lãi suất hiện hành và số lãi dự kiến mỗi tháng. Khoản vay Mortgage có giao diện riêng ở @fig:ui-money: dư nợ, tiền phải trả mỗi kỳ (người dùng chuyển giữa trả hàng tháng và hai tuần một lần), kỳ trả kế tiếp và tổng lãi.

::: {#fig:ui-money .phones caption="Khoản vay, nạp và rút tiền: (a) khoản vay Mortgage với tiền trả mỗi kỳ; (b) nạp tiền; (c) rút tiền, bị chặn khi vượt số dư"}
![](ui/cus-account-mortgage.png)
![](ui/cus-deposit.png)
![](ui/cus-withdraw.png)
:::

Nạp và rút tiền dùng một form chung: chọn tài khoản, nhập số tiền (có các chip số tiền nhanh) và xác nhận qua bottom sheet xác thực. Form báo lỗi ngay khi số tiền vượt số dư và khoá nút xác nhận.

::: {#fig:ui-history .phones caption="Biên lai và lịch sử: (a) biên lai giao dịch; (b) tab Lịch sử nhóm theo ngày, lọc theo tài khoản; (c) màn Giao dịch tháng N với tổng thu và tổng chi, mở từ trợ lý"}
![](ui/cus-receipt.png)
![](ui/cus-history.png)
![](ui/cus-history-month.png)
:::

Mỗi giao dịch thành công kết thúc ở một biên lai gồm các dòng thông tin (số tiền, thời gian, mã giao dịch, bên nhận). Tab Lịch sử gom giao dịch theo ngày, lọc theo tài khoản, và chạm vào một dòng thì mở lại biên lai tương ứng.

### Chuyển tiền và xác thực

Tab Chuyển tiền cho chọn ba kiểu, kèm danh sách người nhận gần đây suy ra từ lịch sử.

::: {#fig:ui-transfer .phones caption="Chuyển tiền: (a) chọn kiểu chuyển; (b) chuyển cho tài khoản FLUX, ứng dụng tra ra tên chủ tài khoản; (c) chuyển liên ngân hàng, chọn ngân hàng"}
![](ui/cus-transfer-hub.png)
![](ui/cus-transfer-internal.png)
![](ui/cus-transfer-external.png)
:::

Khi nhập số tài khoản FLUX, ứng dụng hiện tên chủ tài khoản để người chuyển xác nhận đúng người nhận. Form cũng cho biết hạn mức còn lại trong ngày. Mọi giao dịch kết thúc ở bottom sheet xác thực.

::: {#fig:ui-auth-sheet .phones caption="Xác thực giao dịch: (a) bottom sheet nhập mã OTP với bàn phím số riêng; (b) thông báo SMS giả lập chứa mã; (c) bước quét mặt khi số tiền từ 10.000.000 ₫"}
![](ui/cus-auth-otp.png)
![](ui/cus-sms-otp.png)
![](ui/cus-auth-face.png)
:::

Bottom sheet hiện tóm tắt giao dịch, sáu ô nhập OTP và bàn phím số tự vẽ để bàn phím hệ thống không che nội dung. Mã OTP đến qua một thông báo giả lập SMS, chạm vào thì mã tự điền. Với số tiền từ 10.000.000 ₫, sheet chuyển sang bước quét mặt: người dùng chớp mắt, rồi ứng dụng so với ảnh eKYC.

::: {#fig:ui-payments .phones caption="Nạp tiền và mã QR: (a) trang thanh toán của VNPay sandbox trong WebView; (b) quét mã QR của tài khoản nhận; (c) mã QR của tôi"}
![](ui/cus-vnpay.png)
![](ui/cus-qr-scan.png)
![](ui/cus-qr-mine.png)
:::

Nạp tiền vào tài khoản chỉ đi qua VNPay sandbox: ứng dụng mở trang thanh toán của VNPay trong một WebView và chờ kết quả. Nút QR giữa thanh tab có hai chế độ, quét mã để điền sẵn số tài khoản nhận hoặc hiện mã của chính mình cho người khác quét.

### Tiện ích

Bảy tiện ích dùng ba kiểu màn hình, tất cả đều thanh toán bằng tiền trong tài khoản và đều qua bottom sheet xác thực.

::: {#fig:ui-utility-1 .phones caption="Hoá đơn và nạp điện thoại: (a) trả tiền điện, tra hoá đơn theo mã khách hàng; (b) hoá đơn đã thanh toán, không trả lần hai; (c) nạp tiền điện thoại, nhà mạng nhận theo đầu số"}
![](ui/cus-bill-electric.png)
![](ui/cus-bill-paid.png)
![](ui/cus-topup.png)
:::

::: {#fig:ui-utility-2 .phones caption="Vé, đặt phòng và biên lai: (a) chọn vé máy bay; (b) đặt phòng khách sạn; (c) biên lai thanh toán có mã đặt chỗ"}
![](ui/cus-catalog-flight.png)
![](ui/cus-catalog-hotel.png)
![](ui/cus-payment-receipt.png)
:::

Màn hoá đơn (điện, nước) nhận mã khách hàng và hiện hoá đơn của kỳ hiện tại; hoá đơn đã trả hiện nhãn "đã thanh toán" và cho xem lại biên lai. Màn nạp điện thoại tự nhận nhà mạng theo đầu số và có sáu mệnh giá. Màn danh mục dùng chung cho vé máy bay, vé phim, khách sạn và mua sắm: chọn mục, số lượng và ngày, rồi nhận biên lai kèm mã đặt chỗ.

### Chi nhánh và trợ lý

::: {#fig:ui-branch .phones caption="Chi nhánh: (a) gợi ý chi nhánh đi bộ gần nhất, đường đi vẽ trên bản đồ; (b) danh sách chi nhánh xếp theo quãng đi bộ; (c) khi chưa có quyền vị trí"}
![](ui/cus-branch-suggest.png)
![](ui/cus-branch-list.png)
![](ui/cus-branch-no-location.png)
:::

Ô Chi nhánh mở bản đồ OpenStreetMap với chấm vị trí của người dùng, ghim chi nhánh và đường đi bộ tới chi nhánh được gợi ý. Bottom sheet phía dưới cho biết quãng đường và thời gian đi bộ, giờ mở cửa, nút gọi chi nhánh; khi chi nhánh gợi ý khác chi nhánh gần nhất theo đường chim bay, sheet có thêm một dòng giải thích. Nếu người dùng chưa cấp quyền vị trí, màn hiện lời giải thích và nút cấp quyền thay vì để trống.

::: {#fig:ui-assistant .phones caption="Trợ lý AI: (a) câu trả lời của AI kèm các nút mở màn; (b) chế độ cơ bản khi AI không dùng được; (c) tin nhắn chứa mã OTP được nhắc nhở và không gửi đi"}
![](ui/cus-assistant-ai.png)
![](ui/cus-assistant-basic.png)
![](ui/cus-assistant-otp.png)
:::

Màn trợ lý là một cuộc trò chuyện. Khách có thể hỏi bằng tiếng Việt có dấu hay không dấu, câu trả lời kèm tối đa ba nút, và chạm nút thì mở đúng màn tương ứng (ví dụ "Nạp tiền điện thoại" mở màn nạp điện thoại). Bấm quay lại từ màn đó thì cuộc trò chuyện vẫn còn. Khi AI lỗi, nhãn "Chế độ cơ bản" cho người dùng biết câu trả lời đến từ nhận diện từ khoá.

### Hồ sơ

::: {#fig:ui-profile .phones caption="Hồ sơ khách hàng: (a) thông tin cá nhân và trạng thái eKYC; (b) đổi số điện thoại, xác thực bằng OTP"}
![](ui/cus-profile.png)
![](ui/cus-profile-phone.png)
:::

Tab Cá nhân hiện thông tin của khách, trạng thái eKYC (chưa nộp, chờ duyệt, đã duyệt hoặc bị từ chối kèm lý do), công tắc khoá sinh trắc học và các thao tác sửa địa chỉ, ảnh đại diện, số điện thoại. Đổi số điện thoại phải qua OTP.

## Ứng dụng nhân viên

Ứng dụng nhân viên dùng cùng design system với ứng dụng khách hàng, nên hai giao diện trông thuộc cùng một sản phẩm.

::: {#fig:ui-officer-1 .phones caption="Quản lý khách hàng: (a) tổng quan; (b) danh sách và tìm khách (tìm không dấu); (c) tạo khách hàng mới"}
![](ui/off-overview.png)
![](ui/off-customers.png)
![](ui/off-create-customer.png)
:::

Tab Tổng quan cho nhân viên biết số khách, số hồ sơ eKYC đang chờ và vài hồ sơ chờ mới nhất. Tab Khách hàng liệt kê khách và cho tìm theo tên không dấu. Nút tạo khách mở form nhập họ tên, email, số điện thoại, số căn cước, địa chỉ và mật khẩu tạm; form báo lỗi định dạng và trùng số điện thoại hoặc căn cước ngay tại ô nhập.

::: {#fig:ui-officer-2 .phones caption="Chi tiết khách hàng: (a) xem và sửa thông tin khách; (b) mở tài khoản tiết kiệm hoặc khoản vay, có xem trước tiền trả mỗi kỳ; (c) chụp eKYC hộ khách"}
![](ui/off-customer-detail.png)
![](ui/off-open-account.png)
![](ui/off-ekyc-capture.png)
:::

Màn chi tiết khách cho nhân viên sửa thông tin theo yêu cầu của khách, gửi email đặt lại mật khẩu, mở tài khoản Saving hoặc Mortgage và chụp eKYC hộ khách (khi khách chưa tự nộp được). Khi mở khoản vay, form hiện trước số tiền trả mỗi kỳ để nhân viên và khách cùng xem.

::: {#fig:ui-officer-3 .phones caption="eKYC và lãi suất: (a) duyệt hoặc từ chối hồ sơ eKYC kèm lý do; (b) chỉnh lãi suất tiết kiệm và vay"}
![](ui/off-ekyc-review.png)
![](ui/off-rates.png)
:::

Tab eKYC liệt kê các hồ sơ đang chờ duyệt; nhân viên mở hồ sơ để xem ảnh căn cước và ảnh khuôn mặt, rồi duyệt hoặc từ chối kèm lý do. Tab Lãi suất cho chỉnh lãi suất chung của tiết kiệm và khoản vay, có ghi người sửa và thời điểm.
