# Kịch bản video demo FLUX Bank

Đề bài (Technical Requirements, mục 5): "Provide a demo video to show all the nice features of your app." Video dài khoảng 9–10 phút, đi theo bảy yêu cầu nghiệp vụ của đề theo đúng thứ tự đề, để người chấm đối chiếu với barem. Lời bình viết sẵn bằng tiếng Việt, ngôi thứ nhất, đọc tự nhiên và có thể rút gọn khi thu.

## 0. Cách tiếp cận

**Nhân vật chính là khách Han** (`han@flux.com`; mật khẩu chủ dự án giữ, không ghi trong repo). Han đã nộp eKYC (nhân viên chụp hộ, ảnh mặt là của chủ dự án), đang "Chờ duyệt", số dư 0 và chưa có tài khoản tiết kiệm hay vay. Nhờ vậy cả câu chuyện nằm trong video, không cần dựng dữ liệu trước: nhân viên duyệt eKYC, mở tài khoản cho Han, Han nạp tiền, chuyển tiền, dùng tiện ích. Quét mặt khi giao dịch lớn so với ảnh của chính chủ dự án nên sẽ qua.

**Khách thứ hai là An** (`customer@flux.com`, số tài khoản 886851678506; mật khẩu chủ dự án giữ) làm người nhận tiền và để chứng minh dữ liệu cập nhật ở máy người nhận.
**Nhân viên** là `officer@flux.com` (Lê Thị Cúc).

## 1. Chuẩn bị trước ngày quay

Việc làm một lần, theo thứ tự:

1. **Cài `scrcpy`**: `sudo pacman -S scrcpy`. Quay từng đoạn bằng `scrcpy --no-audio --record=doan-NN.mp4 --max-fps=30` (một đoạn một file, lỗi chỗ nào quay lại chỗ đó).
2. **Thu lời bình riêng** bằng OBS hoặc `arecord`, sau đó ghép bằng ffmpeg (mục 4). Không thu tiếng khi đang thao tác, vì vừa nói vừa bấm dễ vấp.
3. **Máy thật**: mở khoá, chế độ dọc, cắm cáp. Bật chế độ demo để thanh trạng thái sạch:
   ```
   adb shell settings put global sysui_demo_allowed 1
   adb shell am broadcast -a com.android.systemui.demo -e command enter
   adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
   adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
   adb shell settings put system accelerometer_rotation 0
   adb shell svc power stayon true
   ```
   Tắt chế độ demo sau khi quay: `... -e command exit`.
4. **Thông báo OTP**: mã OTP giả lập hiện thành thông báo "FLUX Bank" ở đầu màn hình, chính là bước 2FA đề yêu cầu, nên phải quay được. Để banner này hiện nhưng không lộ thông báo cá nhân, chọn một trong hai cách: (a) Không làm phiền ở chế độ ưu tiên và cho phép riêng FLUX Bank; (b) tắt thông báo của Zalo, Messenger, Discord trong lúc quay.
5. **Quota Gemini miễn phí có hạn**: chỉ gọi Trợ lý khi quay đoạn 7, tối đa 3 câu hỏi cho mỗi lần quay. Không thử Trợ lý bừa trước đó.
6. **Chi nhánh cần ở gần một chi nhánh** (cụm chi nhánh demo quanh cổng TDTU, 19 Nguyễn Hữu Thọ, Q.7). Nếu đến lúc quay vẫn chưa ở TP.HCM thì xem mục 5, phương án B.
7. **Kiểm tra trước khi quay**: VNPay sandbox còn hoạt động (nút Nạp tiền mở được trang thanh toán); hai tài khoản An và Han đăng nhập được; hồ sơ eKYC của Han vẫn "Chờ duyệt". Nếu đã duyệt từ trước, nhờ nhân viên mở lại bằng cách tạo khách mới thay Han và nộp eKYC mới trong đoạn 1.
8. **Thẻ test VNPay** (sandbox): ngân hàng NCB, số thẻ `9704198526191432198`, chủ thẻ `NGUYEN VAN A`, ngày phát hành `07/15`, OTP `123456`.
9. Ghi hình bằng một tài khoản máy sạch: màn hình khoá bằng vân tay, không hiện thông báo riêng tư trên màn hình khoá.

## 2. Kịch bản từng đoạn

Cột "Làm gì" là thao tác trên máy; "Lời bình" là câu đọc. Thời lượng là ước tính.

### Đoạn 0 · Mở đầu (0:20)

| Làm gì | Lời bình |
|---|---|
| Màn hình tiêu đề (một slide hoặc ảnh logo FLUX + tên đề tài), rồi cắt sang màn đăng nhập | "Đây là FLUX Bank, ứng dụng ngân hàng di động cho Android do em làm một mình cho môn Mobile Apps Development. Ứng dụng có hai loại người dùng và bảy nhóm chức năng. Video đi theo đúng thứ tự bảy yêu cầu của đề." |

### Đoạn 1 · Hồ sơ người dùng và bảo mật (1:45)

| Làm gì | Lời bình |
|---|---|
| Màn đăng nhập, chỉ dòng "Ứng dụng demo… giả lập" | "Dữ liệu và tiền trong app đều là giả lập. Người dùng đăng nhập bằng email và mật khẩu qua Firebase Authentication." |
| Đăng nhập nhân viên Lê Thị Cúc. Màn Tổng quan: 2 khách, eKYC chờ duyệt | "Nhân viên và khách hàng thấy hai giao diện khác nhau. Đây là giao diện nhân viên: số khách và số hồ sơ eKYC đang chờ." |
| Tab Khách hàng, gõ "pham thu" không dấu, mở hồ sơ Han | "Nhân viên tìm khách không cần gõ dấu, xem và sửa thông tin theo yêu cầu của khách. Han vừa được nhân viên tạo, đang chờ xác minh danh tính." |
| Tab eKYC, mở hồ sơ Han: hiện CCCD và ảnh chân dung, bấm Duyệt | "eKYC có hai ảnh: giấy tờ và chân dung. App chỉ nhận ảnh có đúng một khuôn mặt, rồi tính vector đặc trưng khuôn mặt ngay trên máy. Nhân viên xem và duyệt, hoặc từ chối kèm lý do." |
| Đăng xuất, đăng nhập Han (nếu app yêu cầu đổi mật khẩu thì quay màn Tạo mật khẩu mới), vào Cá nhân, bật Đăng nhập sinh trắc học (vân tay) | "Lần đầu đăng nhập, khách phải thay mật khẩu tạm do nhân viên cấp. Han bật khoá sinh trắc học. Sau một phút ở nền, app tự khoá." |
| Đưa app xuống nền hơn 60 giây, mở lại: màn khoá, chạm vân tay | "Đây là màn khoá. Mọi giao dịch còn phải qua mã OTP, và với số tiền lớn thì thêm quét khuôn mặt. Phần đó em quay ở đoạn 3." |

### Đoạn 2 · Quản lý tài khoản (1:45)

| Làm gì | Lời bình |
|---|---|
| Máy nhân viên (đăng nhập lại officer): hồ sơ Han → Mở tài khoản → Tiết kiệm | "Có ba loại tài khoản. Nhân viên mở thêm tài khoản tiết kiệm cho khách." |
| Mở tài khoản → Vay thế chấp: 1.500.000.000 ₫, 240 tháng, chỉ phần xem trước tiền trả hàng tháng và mỗi 2 tuần, rồi bấm Mở khoản vay | "Với khoản vay, nhân viên nhập số tiền và thời hạn, app tính trước tiền phải trả mỗi tháng hoặc mỗi hai tuần theo lãi suất của ngân hàng." |
| Tab Lãi suất: sửa lãi tiết kiệm 5% thành 6%, Lưu | "Nhân viên chỉnh lãi suất theo chính sách. Lãi tiết kiệm có hiệu lực ngay với mọi tài khoản tiết kiệm, lãi vay chỉ áp cho khoản vay mở sau đó." |
| Đăng nhập Han: Trang chủ, vuốt thẻ Thanh toán → Tiết kiệm → Vay thế chấp | "Khách có ba thẻ, mỗi loại hiện một góc nhìn riêng: số dư tài khoản thanh toán, lãi suất và lãi dự kiến mỗi tháng của tiết kiệm, dư nợ và số tiền trả của khoản vay." |
| Chi tiết vay: bấm "Hàng tháng", rồi "2 tuần" | "Khoản vay xem được theo kỳ hàng tháng hoặc hai tuần một lần." |
| Chi tiết tài khoản thanh toán: bấm Nạp tiền (không nhập, chỉ cho thấy form), rồi Rút tiền, gõ số vượt số dư để thấy lỗi | "Khách nạp và rút tiền ngay trong từng tài khoản. Rút quá số dư bị chặn ngay trên form, trước khi gửi lên hệ thống." |

Ghi chú: sửa lãi 5% thành 6% làm ảnh báo cáo khác với video. Quay xong nên đặt lại về 5%.

### Đoạn 3 · Giao dịch và xác thực hai lớp (2:15)

| Làm gì | Lời bình |
|---|---|
| Han: Nạp tiền 1.000.000 ₫ (số nhỏ để không cần quét mặt), Tiếp tục, banner OTP hiện, nhập OTP | "Mọi giao dịch phải qua OTP. OTP này sống 120 giây, dùng một lần, nhập sai năm lần thì khoá. Vì không có SMS thật nên app hiện OTP như một thông báo SMS giả lập." |
| Trang VNPay sandbox: Thẻ nội địa, chọn NCB, nhập thẻ test, OTP 123456 | "Nạp tiền đi qua cổng VNPay sandbox, đúng yêu cầu tích hợp dịch vụ thanh toán. Chữ ký VNPay được kiểm trên máy trước khi cộng tiền." |
| Biên lai thanh toán thành công, số dư mới | "Số dư chỉ cộng khi VNPay báo thành công. Nếu đóng trang giữa chừng thì số dư không đổi." |
| Han: Nạp 20.000.000 ₫: OTP, rồi bước Quét khuôn mặt, chớp mắt | "Giao dịch từ 10 triệu đồng trở lên cần thêm quét khuôn mặt. App yêu cầu chớp mắt để chống dùng ảnh tĩnh, rồi so với khuôn mặt đã đăng ký ở eKYC." |
| Chuyển tiền → Trong FLUX: nhập 886851678506, hiện tên "NGUYEN VAN AN", 500.000 ₫, OTP, biên lai | "Chuyển trong ngân hàng thì tra tên người nhận theo số tài khoản trước khi chuyển." |
| Chuyển liên ngân hàng: Vietcombank, nhập số và tên, 2.000.000 ₫ (có thể chỉ tới form, không cần hoàn tất) | "Chuyển liên ngân hàng có 10 ngân hàng giả lập." |
| Chuyển 15.000.000 ₫ cho An: OTP rồi quét mặt (chỉ nếu muốn nhấn mạnh lần nữa), hoặc bỏ qua | "Hạn mức mỗi lần 100 triệu và mỗi ngày 200 triệu, được kiểm tra cả trên form lẫn trong Security Rules." |
| Nút QR giữa thanh tab: Mã của tôi, rồi Quét mã (hướng camera sang màn hình máy tính có mã của An, nếu muốn) | "Mỗi tài khoản có mã QR. Quét mã của người nhận là form chuyển tiền được điền sẵn." |
| Đăng nhập An, mở Lịch sử | "Ở máy người nhận, số dư tăng và lịch sử có dòng 'Nhận từ…'. Hai giao dịch của một lần chuyển trỏ vào nhau nên Security Rules buộc cả hai phải được ghi cùng lúc." |

### Đoạn 4 · Tiện ích (1:30)

| Làm gì | Lời bình |
|---|---|
| Han: Tiền điện → EVN TP.HCM, mã `PE01000123456`, Tra cứu, Thanh toán, OTP, biên lai | "Tiện ích gồm điện, nước, nạp điện thoại, vé máy bay, vé phim, khách sạn và mua sắm. Dữ liệu hoá đơn là giả lập." |
| Tra lại cùng mã: "Bạn đã thanh toán kỳ này" | "Hoá đơn đã trả thì không trả lần hai." |
| Tiền nước (SAWACO), Nạp ĐT 100.000 ₫ cho số của mình (nhà mạng nhận theo đầu số) | "Nạp điện thoại tự nhận nhà mạng theo đầu số." |
| Vé máy bay TP.HCM → Hà Nội, chọn chuyến, 2 khách, thanh toán, biên lai có mã đặt chỗ | "Đặt vé máy bay cho nhiều hành khách, biên lai có mã đặt chỗ." |
| Lướt nhanh Vé phim, Khách sạn, Mua sắm (không cần thanh toán), rồi tab Lịch sử có biểu tượng theo dịch vụ | "Các tiện ích còn lại làm tương tự. Mọi thanh toán đều hiện trong lịch sử, có biểu tượng theo từng dịch vụ." |

### Đoạn 5 · Điều hướng và chi nhánh (1:00)

Chỉ quay được khi ở gần chi nhánh; xem mục 5.

| Làm gì | Lời bình |
|---|---|
| Han: Trang chủ → Chi nhánh, cho phép vị trí | "Ứng dụng xin quyền vị trí rồi hiện bản đồ OpenStreetMap, không cần API key trả phí." |
| Bản đồ: chấm vị trí, ghim chi nhánh, đường đi bộ màu xanh tới chi nhánh gợi ý, thẻ ghi quãng đi bộ và số phút | "Chi nhánh gần nhất theo đường thẳng chưa chắc là gần nhất khi đi bộ. App so sánh ba chi nhánh gần nhất theo đường đi thật và gợi ý cái đi bộ ngắn nhất, kèm dòng giải thích nếu phải đi vòng." |
| Xem tất cả chi nhánh, chạm chi nhánh khác (đường vẽ lại), rồi Quay lại gợi ý | "Người dùng cũng chọn được chi nhánh khác để xem đường đi." |
| Tắt mạng, bấm nút vị trí: đường thẳng nét đứt và nút Thử lại | "Mất mạng thì app chuyển sang đường thẳng nét đứt thay vì báo lỗi." |

### Đoạn 6 · Trợ lý AI (1:15)

| Làm gì | Lời bình |
|---|---|
| Trang chủ → Trợ lý AI, chạm chip "Tháng này tôi chi bao nhiêu?" | "Trợ lý dùng Gemini qua Firebase AI Logic, nên app không chứa API key. Câu trả lời dựa trên số liệu thật của tài khoản trong tháng." |
| Bấm nút "Xem giao dịch tháng 11" | "Trả lời kèm nút mở thẳng màn hình liên quan, ở đây là giao dịch trong tháng, có tổng tiền vào và tiền ra." |
| Gõ "nạp tiền điện thoại": nút mở màn Nạp ĐT, quay lại vẫn còn cuộc trò chuyện | "Bot chỉ mở màn, không tự thực hiện giao dịch và không tự điền form." |
| Gõ "mã OTP của tôi là 123456": bot từ chối | "Tin chứa OTP hoặc mật khẩu không bao giờ gửi cho AI, app trả lời ngay trên máy." |
| Tắt mạng, gõ "chi nhánh gần nhất": nhãn Chế độ cơ bản + nút | "Khi AI lỗi hoặc mất mạng, trợ lý chuyển sang chế độ cơ bản trả lời theo từ khoá nên người dùng vẫn có đường tắt." |

### Đoạn 7 · Kết (0:40)

| Làm gì | Lời bình |
|---|---|
| Cắt sang một slide kiến trúc (sơ đồ ở chương 2 của báo cáo) hoặc ảnh `docs/report/diagrams/architecture.png` | "Ứng dụng viết bằng Kotlin, kiến trúc MVVM, dữ liệu trên Firebase Firestore gói Spark miễn phí, mọi quy tắc quan trọng được kiểm lại bằng Security Rules." |
| Slide số liệu: 391 unit test, 49 kiểm thử Security Rules, bản phát hành ký sẵn cho Google Play | "Phần xử lý nghiệp vụ có 391 unit test và 49 ca kiểm thử Security Rules. Em xin cảm ơn thầy đã xem." |

Tổng: khoảng 9 phút 30 giây.

## 3. Thứ tự quay thực tế

Quay theo trạng thái dữ liệu chứ không theo thứ tự video, rồi cắt ghép:

1. Nhân viên: duyệt eKYC, mở tiết kiệm và vay cho Han, sửa lãi suất (đoạn 1 nửa đầu, đoạn 2 phần nhân viên).
2. Han: đăng nhập, bật sinh trắc học, nạp 1.000.000 ₫ và 20.000.000 ₫, chuyển tiền, tiện ích (đoạn 1 nửa sau, 2, 3, 4).
3. An: xem số dư và lịch sử (cuối đoạn 3).
4. Han: Trợ lý AI (đoạn 6), rồi tắt mạng cho phần chế độ cơ bản.
5. Chi nhánh (đoạn 5) khi đã ở TP.HCM.
6. Nhân viên: đặt lãi suất tiết kiệm về 5%.

## 4. Ghép video

Ví dụ ghép hai đoạn và lời bình:
```
printf "file 'doan-01.mp4'\nfile 'doan-02.mp4'\n" > ds.txt
ffmpeg -f concat -safe 0 -i ds.txt -c copy phan-hinh.mp4
ffmpeg -i phan-hinh.mp4 -i loi-binh.wav -map 0:v -map 1:a -c:v copy -c:a aac -shortest FLUX_Bank_Demo.mp4
```
Nếu độ dài lời bình lệch với hình thì chỉnh tốc độ từng đoạn bằng `-filter:v "setpts=0.9*PTS"` hoặc thu lại lời bình theo từng đoạn.

## 5. Chi nhánh: hai phương án

- **A (khuyến nghị)**: quay đoạn 5 khi bạn ở TP.HCM, đứng gần cổng TDTU để thấy ca "Tân Phong gần hơn theo đường thẳng (393 m) nhưng đi bộ 1,3 km, app gợi ý Nguyễn Hữu Thọ (612 m)".
- **B**: nếu không về được TP.HCM trước hạn, quay đoạn 5 bằng máy ảo Android Studio, đặt vị trí giả cạnh cổng TDTU (Extended controls → Location). Máy ảo cần có Google Play services để bản đồ và vị trí hoạt động. Nói rõ trong lời bình là "quay trên máy ảo".

## 6. Rủi ro đã biết

- Vị trí giả bằng `adb` không dùng được, vì app dùng Fused Location của Google Play services và bỏ qua nó. Vì vậy phương án B phải dùng máy ảo.
- Quét mặt cần đủ sáng, mặt ngay ngắn. Quay vào ban ngày hoặc bật đèn.
- Banner OTP chỉ hiện vài giây. Nếu lỡ, chạm vào thông báo để tự điền mã, hoặc bấm "Gửi lại mã" ở màn nhập OTP (đợi hết đếm ngược).
- Nạp số lớn đi qua ba bước: OTP, quét mặt, rồi mới tới trang VNPay. Kịch bản quay hai lần nạp (1.000.000 ₫ và 20.000.000 ₫) để thấy đủ cả hai nhánh; nếu cần rút gọn video thì bỏ lần 1.000.000 ₫.
- Hồ sơ eKYC của Han chứa ảnh mặt chủ dự án và nằm trên Firebase; video công khai thì cân nhắc làm mờ ảnh trong màn duyệt eKYC khi cắt ghép.
