# FLUX Bank — Chuẩn bị bản phát hành Google Play

- Ngày: 2026-11-13 · Hạn nộp đồ án: 2026-12-01 · Nhánh: `chore/play-release`
- Trạng thái: **đã duyệt 2026-11-13**. Nền tảng: F1–F7 đã nằm trên `main`. Chủ dự án **đã tạo tài khoản Play Console** (2026-11-13) và đang hỏi giảng viên có chấp nhận bản thử kín (closed testing) thay cho production không.
- Ký hiệu: **[Đề]** yêu cầu đề bài · **[Chủ dự án]** chủ dự án quyết · **[Claude]** Claude tự quyết (chủ dự án bỏ qua hỏi đáp), sửa được khi duyệt.

## 1. Mục đích

Đề yêu cầu app **lên Google Play Store, ai cũng tải và dùng được** **[Đề]**. Tài khoản cá nhân mới phải chạy **thử kín ≥ 12 người thử, liên tục 14 ngày** rồi mới xin được production (chính sách Google, kiểm 2026-11-13). Hạn 01/12 nên đường găng là: **tải bản thử kín lên càng sớm càng tốt**. Phần này chuẩn bị mọi thứ để chủ dự án bấm tải lên trong một buổi.

## 2. Mục tiêu, không làm, phản mục tiêu

**Mục tiêu**
- Một file **AAB đã ký** (`bundleRelease`) sẵn sàng tải lên kênh closed testing.
- **App Check** bật lại được cho Firebase AI Logic: Play Integrity cho bản tải từ Play, debug provider cho bản debug. Trợ lý F7 vẫn chạy trên máy dev.
- **Trang chính sách bảo mật** có link công khai.
- Bản nháp **mọi câu trả lời cần điền trong Play Console**: Data safety, Financial features, phân loại nội dung, đối tượng, quyền truy cập app (tài khoản demo), quảng cáo, store listing.
- **Hình ảnh cho trang Play**: icon 512×512, ảnh nổi bật 1024×500, 4–8 ảnh chụp màn hình điện thoại đúng tỉ lệ Play cho phép.
- Hướng dẫn từng bước cho phần chỉ chủ dự án làm được (Console, người thử).

**Không làm** **[Claude]**
- Không tự động hoá Play (Publishing API, CI), không bật R8/thu gọn code, không bản iOS, không Crashlytics/Analytics.
- Không cho người thử tự đăng ký tài khoản (giữ nguyên nghiệp vụ: nhân viên tạo khách).
- Không bật App Check cho Firestore/Auth (chỉ AI Logic) — bật cho Firestore sẽ khoá bản debug và cài qua adb.
- Không dịch app sang tiếng Anh.

**Phản mục tiêu**
- Keystore, mật khẩu keystore, mật khẩu tài khoản demo hay khoá VNPay **lọt vào git**.
- Debug provider của App Check **có trong bản release**.
- Bật bắt buộc App Check **trước** khi đăng ký Play Integrity và debug token → Trợ lý của mọi người rơi về chế độ cơ bản.
- App bị Play từ chối vì **giả danh tổ chức tài chính thật** hoặc khiến người dùng tưởng là ngân hàng thật.
- Phá bản debug / 391 unit test hiện có.

## 3. Quyết định

### 3.1 Ký app **[Claude]**
- Dùng **Play App Signing** (mặc định của Play): Google giữ khoá ký app, mình chỉ giữ **khoá upload**.
- Khoá upload: keystore PKCS12 tạo bằng `keytool`, lưu **ngoài repo** tại `~/keystores/flux-upload.jks`, hiệu lực 25 năm. Chủ dự án tự đặt mật khẩu (Claude không đặt hộ, không lưu).
- `app/build.gradle.kts` thêm `signingConfigs.release` đọc từ `local.properties` (đã gitignore): `release.store.file`, `release.store.password`, `release.key.alias`, `release.key.password`. **Thiếu thì chỉ task build release báo lỗi rõ ràng**, bản debug và unit test không bị ảnh hưởng.
- `versionCode = 1`, `versionName = "1.0"` (giữ nguyên). Mỗi lần tải lại phải tăng `versionCode`.
- Nhắc chủ dự án **sao lưu keystore + mật khẩu** (mất khoá upload thì phải xin Google đặt lại, mất vài ngày).

### 3.2 App Check **[Claude]**
- Thư viện: `firebase-appcheck-playintegrity` (`implementation`, chỉ cho release) và `firebase-appcheck-debug` (`debugImplementation`).
- Mỗi build type một file `AppCheckSetup.kt` ở `app/src/debug/...` và `app/src/release/...` cùng chữ ký `fun installAppCheck(app: FluxApp)`; `FluxApp.onCreate` gọi nó. Nhờ vậy debug provider không thể lọt vào release.
- Thứ tự bật (chủ dự án làm, có hướng dẫn trong README):
  1. Tải AAB lên closed testing → Play Console → *App integrity* lấy **SHA-256 của khoá ký app** (của Google, không phải khoá upload).
  2. Firebase Console → App Check → app Android → **Play Integrity** → dán SHA-256 (+ SHA-256 khoá upload để thử bản release cài tay nếu cần).
  3. Chạy bản debug trên Xiaomi, lấy **debug token** trong Logcat (`DebugAppCheckProvider`), đăng ký trong Firebase Console.
  4. Kiểm Trợ lý vẫn trả lời bằng AI (bản debug) → mới **Enforce** App Check cho Firebase AI Logic.
- Play Integrity API: gói tiêu chuẩn miễn phí, không cần billing. Bản release cài bằng adb (không qua Play) sẽ bị từ chối token → Trợ lý dùng chế độ cơ bản, **không crash**.

### 3.3 Nhận diện "đồ án demo" **[Claude]**
- Tên trên Play: **"FLUX Bank (Demo)"**. Tên trong máy giữ "FLUX Bank".
- Mô tả Play mở đầu bằng câu nói rõ: đồ án môn học, ngân hàng giả lập, **tiền không có thật**, không liên kết ngân hàng nào.
- Màn đăng nhập thêm **một dòng chú thích** dưới form: "Ứng dụng demo cho đồ án môn học · tiền và giao dịch đều là giả lập" (Caption, màu chữ phụ, nằm trong `strings.xml`). Không đổi bố cục khác nên không cần mockup riêng.

### 3.4 Tài khoản cho người thử và người duyệt **[Claude]**
- Chủ dự án tạo trong app **một khách demo riêng** (không dùng tài khoản thử cá nhân): đã eKYC VERIFIED, có Thanh toán + Tiết kiệm, vài giao dịch mẫu, số dư vài chục triệu ₫.
- Tài khoản khách demo dùng chung cho 12 người thử và cho mục **App access** của Play (người duyệt của Google cần đăng nhập được).
- **Tài khoản nhân viên demo chỉ gửi riêng cho giảng viên**, không đưa người thử (nhân viên sửa/xoá được khách).
- Ghi chú cho người thử: giao dịch **dưới 10.000.000 ₫** (bước quét mặt so với ảnh eKYC của chủ tài khoản demo); OTP đến bằng thông báo "SMS giả lập", cần cho phép thông báo.
- Mật khẩu chỉ nằm trong Play Console và tin nhắn gửi người thử, **không ghi vào repo**.

### 3.5 Chính sách bảo mật **[Claude]**
- Song ngữ Việt–Anh ngắn, lưu ở `docs/play/privacy-policy.md`.
- Nội dung: là đồ án; dữ liệu thu thập (họ tên, email, SĐT, địa chỉ, số CCCD, ảnh CCCD/selfie + đặc trưng khuôn mặt, số dư/giao dịch giả lập, vị trí khi mở bản đồ chi nhánh, nội dung chat với Trợ lý); mục đích; bên thứ ba (Google Firebase, Google Gemini qua Firebase AI Logic, server chỉ đường OSRM của FOSSGIS, OpenStreetMap, VNPay sandbox); lưu ở Firestore; vị trí không lưu; cách xin xoá dữ liệu (email); liên hệ.
- Đăng công khai bằng **GitHub Gist public** (`gh gist create --public`), lấy link dán vào Play. Repo FLUX_Bank vẫn private. **Claude hỏi chủ dự án trước khi đăng** (đăng lên mạng là việc công khai) và hỏi email liên hệ muốn công khai.

### 3.6 Câu trả lời cho Play Console **[Claude]** — `docs/play/play-console-answers.md`
- **Data safety**: liệt kê từng loại dữ liệu (Personal info, Photos, Financial info, Location — precise, Messages/other user content cho chat, App info/Device IDs cho App Check/Firebase), "thu thập" hay "chia sẻ", bắt buộc hay tuỳ chọn, mục đích; mã hoá khi truyền: có; người dùng xin xoá: có (qua email).
- **Financial features**: khai *Money transfer and wire services* và *Mobile payments and digital wallets* (đúng với tính năng hiển thị), ghi chú là giả lập. Không khai vay cá nhân (khoản vay chỉ hiển thị, nhân viên tạo, không cho vay qua app). Chủ dự án là người bấm cuối cùng.
- **Phân loại nội dung**: không bạo lực/nội dung người lớn; có tính năng tài chính giả lập, có chat AI. **Đối tượng**: 18+. **Quảng cáo**: không. **App access**: có hạn chế, kèm hướng dẫn đăng nhập khách demo. **Chính phủ / tin tức / sức khoẻ**: không.
- **Store listing**: tên (≤ 30 ký tự), mô tả ngắn (≤ 80), mô tả đầy đủ (≤ 4000) tiếng Việt, danh mục *Finance*, email liên hệ.
- **Tin nhắn mời người thử**: link opt-in, tài khoản demo, 3–4 việc nên thử, lời nhắc giữ app 14 ngày.

### 3.7 Hình ảnh **[Claude]** — `docs/play/graphics/`
- **Icon 512×512 PNG**: dựng lại từ vector icon app hiện có (`ic_launcher_foreground` + `ic_launcher_background`), không vẽ icon mới.
- **Ảnh nổi bật 1024×500**: nền và chữ theo design system (Obsidian, mint, chữ Inter "FLUX Bank" + một dòng tiếng Việt), duyệt bằng ảnh trước khi dùng.
- **Ảnh chụp màn hình**: chụp trên Xiaomi bằng tài khoản khách demo (không lộ dữ liệu thật): trang chủ, chuyển tiền, tiện ích, lịch sử, bản đồ chi nhánh, Trợ lý AI. Màn 1268×2756 vượt tỉ lệ 2:1 của Play → cắt thanh trạng thái và vùng cử chỉ còn 1268×2536.

## 4. Ràng buộc
Giữ như F1–F7: Kotlin + XML, MVVM, TDD cho logic, không commit `.idea/*`, comment tiếng Việt. Gói Spark, không billing. Không làm hỏng bản debug và bộ test.

## 5. Kiểm tra
- `./gradlew testDebugUnitTest assembleDebug lintDebug` vẫn xanh (391 test).
- `./gradlew bundleRelease` ra AAB; `jarsigner -verify` báo đã ký bằng khoá upload; `bundletool`/`unzip -l` không chứa lớp `DebugAppCheckProviderFactory`.
- Thiếu cấu hình ký: `bundleRelease` báo lỗi nói rõ thiếu khoá nào, `assembleDebug` vẫn chạy.
- Trên Xiaomi (bản debug): đăng ký debug token → Trợ lý trả lời bằng AI khi App Check **đã Enforce**.
- Ảnh chụp đúng kích thước Play (cạnh 320–3840 px, tỉ lệ ≤ 2:1).

## 6. Việc của chủ dự án (không ai làm hộ được)
Tạo keystore (đặt mật khẩu), tạo khách demo + nhân viên demo trong app, tạo app trong Play Console và điền form theo bản nháp, tải AAB lên closed testing, mời 12 người thử, lấy SHA-256 dán vào Firebase, đăng ký debug token, Enforce App Check, gửi tài khoản nhân viên demo cho giảng viên.

## 7. Việc để builder tự quyết
Câu chữ chính xác của chính sách và mô tả, cách dựng icon/ảnh nổi bật (công cụ nào), chọn màn chụp cụ thể, thông báo lỗi khi thiếu cấu hình ký.
