# FLUX Bank

Ứng dụng ngân hàng Android native (Kotlin, XML + ViewBinding, MVVM) dùng Firebase Authentication + Cloud Firestore (gói Spark miễn phí).

## Build & test

Gradle daemon được ghim JDK 21 trong `gradle/gradle-daemon-jvm.properties` (JRE 25 đi kèm extension Java của VSCode thiếu `jlink`).
Trong Android Studio chọn Gradle JDK = `/usr/lib/jvm/java-21-openjdk`.

```bash
./gradlew testDebugUnitTest   # unit test
./gradlew assembleDebug       # APK debug
```

## Thiết lập Firebase (làm một lần)

### 1. Deploy Security Rules
Firebase Console → **Firestore Database → Rules** → dán toàn bộ nội dung `firestore.rules` → **Publish**.
Publish lại mỗi khi file này đổi (Feature 4 thêm luật chuyển tiền và collection `accountIndex`; Feature 5 thêm loại giao dịch `PAYMENT`).

### 2. Tạo nhân viên (officer) đầu tiên
App không có chức năng tự đăng ký nhân viên; mọi khách hàng đều do nhân viên tạo trong app.

1. Console → **Authentication → Users → Add user**: nhập email và mật khẩu của nhân viên, rồi copy **User UID**.
2. Console → **Firestore Database** → collection `users` → **Add document** với Document ID = UID vừa copy, và các field (**gõ tay tên field**, đừng copy từ bảng dưới: trình xem markdown có thể chèn ký tự ẩn U+200B khiến app không đọc được field và coi nhân viên là khách):

| Field | Kiểu | Giá trị |
|---|---|---|
| `uid` | string | UID vừa copy |
| `email` | string | email nhân viên |
| `fullName` | string | họ tên nhân viên |
| `phoneNumber` | string | ví dụ `0900000001` |
| `nationalId` | string | 12 chữ số |
| `address` | string | địa chỉ |
| `role` | string | `BANK_OFFICER` |
| `kycStatus` | string | `VERIFIED` |
| `biometricEnabled` | boolean | `false` |
| `mustChangePassword` | boolean | `false` |
| `createdAt` | number | ví dụ `1793000000000` |

3. (Khuyến nghị) Collection `phoneIndex` → document ID = SĐT ở trên, field `uid` = UID nhân viên.

### 3. Tạo index cho lịch sử giao dịch (Feature 3)
Tab Lịch sử truy vấn `transactions` theo `ownerUid` và sắp theo `createdAt` giảm dần, nên cần một composite index (miễn phí):
Console → **Firestore Database → Indexes → Composite → Create index**: collection `transactions`, field `ownerUid` **Ascending**, `createdAt` **Descending**, query scope Collection. Chú ý chiều của `createdAt`: index Ascending không dùng được cho truy vấn này.
Cách khác: mở tab Lịch sử một lần, Logcat sẽ in lỗi `FAILED_PRECONDITION` kèm link tạo sẵn index; mở link và bấm Create. Index mất vài phút để chuyển sang Enabled.

### 4. Kiểm tra rules bằng Rules Playground
Trong tab Rules → **Rules Playground**, với một khách hàng đã đăng nhập:
- đọc `users/{uid khác}` → **bị chặn**
- sửa `fullName` của chính mình → **bị chặn**
- sửa `address` của chính mình → **được phép**

Với nhân viên: sửa `fullName` của khách → **được phép**.

Feature 3 (khách đã đăng nhập, `accounts/{id}` là tài khoản Checking của chính mình):
- update `accounts/{id}` chỉ đổi `balance` (không có giao dịch đi kèm) → **bị chặn**
- create `transactions/{tx}` mà `accounts/{id}.lastTxId` không trỏ về `tx` → **bị chặn**
- rút làm `balance` thành −1 → **bị chặn**
- ghi `config/rates` → **bị chặn** (nhân viên ghi mức trong (0, 30] → **được phép**)
- update `accounts/{id}` của khách khác → **bị chặn**

Feature 4: `get` một `accountIndex/{số TK}` → **được phép**; `list` collection `accountIndex` → **bị chặn**;
update `accounts/{id}` của người khác chỉ tăng `balance` (không có giao dịch cặp) → **bị chặn**.

Playground chỉ thử từng request đơn lẻ nên luồng hợp lệ (ghi tài khoản + giao dịch trong một batch) kiểm tra bằng app.

### 5. Test rules tự động (Firestore emulator)
`rules-test/` chạy 49 ca kiểm tra rules của Feature 3–5 trên emulator, gồm cả batch nạp/rút và chuyển tiền 4 document mà Playground không thử được. Cần Node và Java 21:
```bash
cd rules-test && npm install
ln -sfn ../templates node_modules/firebase-tools/lib/templates   # firebase-tools 13.29.1 tìm sai thư mục templates
npx firebase emulators:exec --only firestore --project demo-flux "node --test rules.test.mjs"
```

### 6. VNPay sandbox (Feature 4, nạp tiền)
1. Đăng ký merchant thử nghiệm tại https://sandbox.vnpayment.vn/devreg/ (miễn phí). VNPay gửi email có `vnp_TmnCode` và `vnp_HashSecret`.
2. Thêm hai dòng vào `local.properties` (file này không commit) rồi build lại:
   ```properties
   vnpay.tmn.code=XXXXXXXX
   vnpay.hash.secret=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   ```
   Thiếu khoá thì nút Nạp tiền báo "Chưa cấu hình VNPay".
3. Khi thanh toán trên trang VNPay, chọn thẻ nội địa và dùng thẻ test VNPay công bố (hướng dẫn plugin trên sandbox.vnpayment.vn):
   ngân hàng **NCB**, số thẻ `9704198526191432198`, chủ thẻ `NGUYEN VAN A`, ngày phát hành `07/15`, OTP `123456`.
   Danh sách đầy đủ (gồm thẻ cố ý lỗi: không đủ số dư, bị khoá…) ở https://sandbox.vnpayment.vn/apis/vnpay-demo/.

### 7. Bản đồ chi nhánh (Feature 6)
Không cần cấu hình gì: bản đồ là OpenStreetMap qua OSMDroid, đường đi bộ từ server OSRM công cộng, cả hai miễn phí, không key, không billing.
(Đã thử Google Maps nhưng Google Cloud từ chối tạo tài khoản thanh toán, lỗi `OR_BACR2_59`.) Cần mạng để tải nền bản đồ và đường đi.

### 8. Trợ lý AI (Feature 7)
Firebase Console → **AI Logic** → Get started → chọn **Gemini Developer API** (miễn phí, không cần thẻ, vẫn ở gói Spark; không có API key Gemini trong APK).
Luồng bật mới tự **bắt buộc App Check** cho AI Logic: tạm tắt ở Console → App Check → tab APIs → Firebase AI Logic → Unenforce (có thể mất tới 15 phút mới có hiệu lực). Bật lại kèm Play Integrity khi đưa lên Play.
Chưa bật (hoặc mất mạng, hết lượt miễn phí) thì màn chat vẫn chạy ở "Chế độ cơ bản" (trả lời theo từ khoá trên máy); lý do lỗi ghi ở Logcat, tag `AssistantViewModel`.

### 9. Ký bản phát hành (Google Play)
Google Play giữ khoá ký app (Play App Signing); mình chỉ cần **khoá upload**. Tạo một lần, lưu **ngoài repo**:
```bash
mkdir -p ~/keystores
keytool -genkeypair -storetype PKCS12 -keystore ~/keystores/flux-upload.jks -alias upload \
  -keyalg RSA -keysize 2048 -validity 9125 -dname "CN=<Họ tên>, O=TDTU, C=VN"
```
`keytool` sẽ hỏi mật khẩu (tự đặt, đừng ghi vào repo). Thêm vào `local.properties` (file này đã gitignore):
```properties
release.store.file=/home/<user>/keystores/flux-upload.jks
release.store.password=<mật khẩu keystore>
release.key.alias=upload
release.key.password=<mật khẩu khoá, PKCS12 thì trùng mật khẩu keystore>
```
Build: `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`. Thiếu dòng nào thì lệnh báo lỗi nêu tên dòng đó; bản debug và unit test không cần các dòng này.
- **Sao lưu `flux-upload.jks` và mật khẩu ra nơi khác** (Drive riêng, USB). Mất khoá upload thì phải xin Google đặt lại, mất vài ngày.
- Mỗi lần tải bản mới lên Play phải tăng `versionCode` trong `app/build.gradle.kts`.

## Luồng demo
Nhân viên đăng nhập → tạo khách hàng (mật khẩu tạm) → khách đăng nhập, bắt buộc đổi mật khẩu → khách nộp eKYC → nhân viên duyệt → khách bật sinh trắc học, khoá/mở khoá app → đổi SĐT qua OTP → kiểm tra xác thực khuôn mặt.

Feature 3: nhân viên mở trong chi tiết khách một tài khoản Tiết kiệm và một khoản vay 1.500.000.000 ₫ / 240 tháng
→ khách nạp 2.000.000 ₫ vào Tiết kiệm (OTP), rồi 10.000.000 ₫ (OTP + khuôn mặt) → thử rút quá số dư (bị chặn)
→ nhân viên đổi lãi suất tiết kiệm sang 5% → khách thấy lãi/tháng mới → xem khoản vay, đổi giữa "Hàng tháng" và "2 tuần"
(13.017.349 ₫ / 6.003.662 ₫) → tab Lịch sử hiện các giao dịch theo ngày.

Feature 4 (hai khách A và B, B đã đăng nhập ít nhất một lần để số tài khoản có trong `accountIndex`):
A nạp 1.000.000 ₫ qua VNPay sandbox (thẻ NCB ở mục 6) → A chuyển 500.000 ₫ cho B theo số tài khoản (thấy tên B) → OTP → biên lai
→ B mở app: số dư tăng, Lịch sử có "Nhận từ …" → A chuyển 2.000.000 ₫ sang Vietcombank (giả lập) → A chuyển 15.000.000 ₫ (thêm bước quét mặt)
→ A thử vượt hạn mức 200.000.000 ₫/ngày (bị chặn trên form) → A mở "Mã của tôi", máy B quét bằng nút QR giữa thanh tab → form chuyển điền sẵn.

Feature 5 (tiện ích, dữ liệu giả lập trong app):
Tiền điện → EVN TP.HCM, mã `PE01000123456` → Tra cứu → Thanh toán → OTP → biên lai → tra lại cùng mã: "đã thanh toán", xem biên lai
→ thử mã đuôi `0000` ("Không tìm thấy hoá đơn") → Tiền nước (SAWACO, mã 8–12 số) → Nạp ĐT 100.000 ₫ cho SĐT của mình
→ Vé máy bay TP.HCM → Hà Nội, 2 khách → biên lai có mã đặt chỗ → Vé phim, Khách sạn, Mua sắm → tab Lịch sử có icon theo dịch vụ.

Feature 6 (chi nhánh, cần mạng): Trang chủ → Chi nhánh → cho phép vị trí → bản đồ có chấm vị trí, ghim chi nhánh, đường đi bộ emerald tới chi nhánh gợi ý
→ card ghi quãng đi bộ và phút; nếu chi nhánh gần nhất theo đường thẳng phải đi vòng thì có dòng giải thích → "Xem tất cả chi nhánh"
→ chạm chi nhánh khác (đường vẽ lại) → "Quay lại gợi ý" → tắt mạng, bấm nút vị trí: đường thẳng nét đứt + "Thử lại".
Cụm chi nhánh demo đặt quanh cổng TDTU (19 Nguyễn Hữu Thọ, Q.7): đứng ở đó sẽ thấy FLUX Tân Phong gần hơn theo đường thẳng (393 m) nhưng phải đi bộ 1,3 km, app gợi ý FLUX Nguyễn Hữu Thọ (612 m).

Feature 7 (Trợ lý AI): Trang chủ → Trợ lý AI → chạm chip "Nạp tiền điện thoại" → câu trả lời kèm nút → mở màn Nạp ĐT → back, cuộc trò chuyện vẫn còn
→ gõ "tháng này mình tiêu hết bao nhiêu rồi" → số liệu thật + nút "Xem giao dịch tháng N" → màn tháng có tổng vào/ra
→ gõ "hoá đơn điện" → nút Thanh toán tiền điện → gõ "mã OTP của tôi là 123456" → bot nhắc không chia sẻ
→ bật chế độ máy bay, gõ "chuyển tiền" → nhãn "Chế độ cơ bản" + nút mở form chuyển tiền.

## Giới hạn đã biết
Gói Spark không có Cloud Functions nên OTP và đối chiếu khuôn mặt được kiểm tra trên thiết bị; Security Rules chỉ giới hạn ai được sửa field nào. Hướng nâng cấp: Cloud Functions phát/kiểm token xác thực trước khi ghi giao dịch.

Feature 4:
- Hạn mức 200.000.000 ₫/ngày chỉ kiểm ở app (rules không cộng dồn được); rules chặn mỗi lần > 100.000.000 ₫ và số dư âm.
- Chữ ký VNPay được kiểm trên máy và khoá sandbox nằm trong APK; rules không biết VNPay đã thu tiền hay chưa, nên khách gọi thẳng Firestore vẫn ghi được giao dịch DEPOSIT. Hướng nâng cấp: Cloud Functions nhận IPN của VNPay rồi mới cộng tiền.
- Số tài khoản chỉ tra được sau khi chủ tài khoản đăng nhập ít nhất một lần (app tự ghi `accountIndex`).
- Giao dịch "Nhận tiền" không có số dư sau giao dịch (người gửi không được đọc số dư người nhận).
- Chuyển liên ngân hàng là giả lập: tiền bị trừ, không ngân hàng nào nhận.

Feature 5:
- Nhà cung cấp, hoá đơn, chuyến bay, phim, khách sạn, sản phẩm đều là dữ liệu giả lập nằm trong app; không kết nối dịch vụ thật.
- Rules chỉ biết đây là một lần trừ tiền `PAYMENT` hợp lệ (đúng chủ, đúng số dư, ≤ 100.000.000 ₫), không biết số tiền hoá đơn hay giá vé đúng bao nhiêu. Khách sửa app có thể trả số khác, nhưng chỉ trừ tiền của chính mình và vẫn có bản ghi.
- Mỗi hoá đơn (nhà cung cấp + mã + kỳ) chỉ trả được một lần cho mỗi khách: id giao dịch suy ra từ hoá đơn nên rules chặn lần ghi thứ hai. Hai khách khác nhau vẫn trả được cùng một mã.
- Hạn mức 200.000.000 ₫/ngày tính chung chuyển tiền và thanh toán, chỉ kiểm ở app.

Feature 6:
- Chi nhánh là dữ liệu giả lập nằm trong app (tên tự đặt, đường thật ở TP.HCM), không lưu Firestore.
- Nền bản đồ (tile.openstreetmap.org) và đường đi bộ (OSRM công cộng của FOSSGIS) là dịch vụ miễn phí, không cam kết luôn chạy, chỉ dùng mức nhẹ theo chính sách của OSM. Lỗi hoặc mất mạng thì app gợi ý theo đường thẳng và ghi rõ.
- Gợi ý chỉ so 3 chi nhánh gần nhất theo đường thẳng; xa hơn 20 km thì không tính đường đi bộ.
- Toạ độ người dùng được gửi tới server chỉ đường bên thứ ba (không lưu ở FLUX); khai báo ở mục Data safety khi lên Play Store.

Feature 7:
- Gemini Developer API ở free tier có giới hạn lượt/phút và lượt/ngày; hết lượt, mất mạng hay quá 20 giây thì app trả lời bằng từ khoá ("Chế độ cơ bản").
- Chưa bật App Check cho AI Logic: ai có file cấu hình Firebase của app cũng dùng được quota Gemini miễn phí của project (không mất tiền, chỉ hết lượt). Bật cùng Play Integrity khi lên Play.
- Câu hỏi và số liệu tóm tắt (số dư theo loại tài khoản, tổng chi theo nhóm trong tháng) được gửi tới Google; free tier có thể dùng nội dung này để cải thiện sản phẩm. App không gửi số tài khoản, SĐT, CCCD hay tên người nhận. Khai báo ở Data safety khi lên Play.
- Bot chỉ mở màn chức năng, không tự giao dịch và không điền sẵn form; câu trả lời của AI có thể chưa chính xác (ghi ngay dưới tiêu đề màn chat).
- Cuộc trò chuyện không lưu: rời màn chat về trang chủ là mất.
