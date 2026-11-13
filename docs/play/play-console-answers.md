# Play Console — câu trả lời soạn sẵn cho FLUX Bank (Demo)

Điền theo thứ tự xuất hiện trong Play Console. Ô `<…>` là chỗ chủ dự án điền. **Không ghi mật khẩu thật vào file này.**

## 0. Tạo app
- App name: `FLUX Bank (Demo)` · Default language: Tiếng Việt (vi-VN) · App or game: **App** · Free or paid: **Free**.
- Package (từ AAB): `com.flux.bank`.

## 1. Policy and programs → App content

### Privacy policy
`https://gist.github.com/thedestoroyah/a48a98939a1fc13b32d8913c0d8711df` (Gist công khai từ `docs/play/privacy-policy.md`).

### App access
Chọn **All or some functionality is restricted** → thêm một bộ hướng dẫn:
- Name: `Tài khoản khách demo` (dùng khách thử sẵn có, đã đổi mật khẩu riêng cho việc thử)
- Username: `<EMAIL_KHÁCH_DEMO>` · Password: `<MẬT_KHẨU_KHÁCH_DEMO>`
- Any other information:
  > Simulated banking app (university project, no real money). Log in with the demo customer above. Transactions ask for a 6-digit OTP delivered as a local notification titled "SMS giả lập" (allow notifications; tap the notification to auto-fill). Keep amounts under 10,000,000 VND: larger amounts require a face match against the account owner's eKYC photo. Customer accounts are created by bank officers, so there is no sign-up screen.

### Ads
**No, my app does not contain ads.**

### Content rating (IARC)
- Category: **All other app types**.
- Violence, sexuality, language, controlled substances, gambling: **No** cho tất cả.
- Users can interact or exchange content with other users: **No** (Trợ lý là AI, không có chat giữa người dùng).
- Shares user's current physical location with other users: **No**.
- Allows users to purchase digital goods: **No** (thanh toán giả lập).
- Kết quả dự kiến: 3+/Everyone; target audience vẫn đặt 18+ ở mục dưới.

### Target audience and content
- Target age: **18 and over** only. Appeals to children: **No**.

### News apps · Government apps · Health apps · COVID-19
**No** / không áp dụng.

### Financial features
Chọn:
- **Money transfer and wire services**
- **Mobile payments and digital wallets**

Không chọn personal loan (khoản vay chỉ hiển thị, nhân viên tạo, app không cho vay). Nếu có ô mô tả thêm:
> Educational demo only. All accounts, balances, transfers and payments are simulated in a Firebase project; no real money moves and the app is not connected to any bank or payment network (VNPay is used in sandbox mode with test cards only).

### Data safety
Tổng quan:
- Does your app collect or share any of the required user data types? **Yes**.
- Is all of the user data collected by your app encrypted in transit? **Yes**.
- Which methods of account creation does your app support? **Other** (tài khoản do nhân viên tạo trong app). Delete account URL: `https://gist.github.com/thedestoroyah/a48a98939a1fc13b32d8913c0d8711df` (mục "Quyền của bạn" trong chính sách) — người dùng gửi email để xoá.
- Can users request that data be deleted? **Yes**.

Từng loại dữ liệu (Collected = thu thập; Shared = chuyển cho bên thứ ba **không phải** nhà cung cấp dịch vụ xử lý thay mình — Firebase là nhà cung cấp dịch vụ nên không tính "Shared"; Gemini gói miễn phí được Google dùng để cải thiện sản phẩm nên **tính "Shared"** cho dữ liệu gửi lên Trợ lý: tên gọi, số liệu tài chính tóm tắt, câu hỏi):

| Nhóm Play | Loại | Collected | Shared | Ephemeral | Bắt buộc | Mục đích |
|---|---|---|---|---|---|---|
| Personal info | Name | Yes | **Yes** (Google Gemini, chỉ tên gọi) | No | Required | App functionality, Account management |
| Personal info | Email address | Yes | No | No | Required | App functionality, Account management |
| Personal info | Phone number | Yes | No | No | Required | App functionality, Account management, Fraud prevention/security (OTP) |
| Personal info | Address | Yes | No | No | Required | Account management |
| Personal info | Other info (số CCCD) | Yes | No | No | Required | Account management, Fraud prevention/security |
| Financial info | Purchase history (hoá đơn, vé đã trả) | Yes | No | No | Required | App functionality |
| Financial info | Other financial info (số dư, giao dịch giả lập) | Yes | **Yes** (Google Gemini, số liệu tóm tắt) | No | Required | App functionality |
| Photos and videos | Photos (ảnh CCCD, selfie eKYC) | Yes | No | No | Required | Account management, Fraud prevention/security |
| Location | Precise location | Yes | **Yes** (server chỉ đường OSRM/FOSSGIS) | **Yes** | Optional | App functionality |
| Messages | Other in-app messages (câu hỏi Trợ lý AI) | Yes | **Yes** (Google Gemini) | No | Optional | App functionality |
| Device or other IDs | Device or other IDs (Firebase, App Check) | Yes | No | No | Required | App functionality, Fraud prevention/security |

Không khai: Contacts, Calendar, Audio, Health, App activity, App info and performance (app không có Analytics/Crashlytics), Web browsing.

## 2. Grow users → Store presence → Main store listing

- **App name** (16/30): `FLUX Bank (Demo)`
- **Short description** (≤ 80):
  > Đồ án ngân hàng số giả lập: chuyển tiền, tiện ích, chi nhánh và trợ lý AI
- **Full description** (≤ 4000):

> FLUX Bank (Demo) là đồ án môn Lập trình ứng dụng di động tại Trường Đại học Tôn Đức Thắng. Đây là ngân hàng giả lập: mọi tài khoản, số dư và giao dịch đều không có thật, không có tiền thật và không liên kết với ngân hàng nào.
>
> Tính năng cho khách hàng:
> • Tài khoản Thanh toán, Tiết kiệm (lãi dự kiến) và Khoản vay (tiền trả hằng tháng hoặc 2 tuần).
> • Chuyển tiền trong FLUX theo số tài khoản, giữa các tài khoản của mình, liên ngân hàng (giả lập); quét mã QR để chuyển nhanh.
> • Xác thực mọi giao dịch bằng mã OTP; giao dịch lớn cần quét khuôn mặt so với ảnh eKYC.
> • Tiện ích: tiền điện, tiền nước, nạp điện thoại, vé máy bay, vé xem phim, khách sạn, mua sắm.
> • Bản đồ chi nhánh: gợi ý chi nhánh có đường đi bộ ngắn nhất từ vị trí của bạn.
> • Trợ lý AI: hỏi bằng tiếng Việt, trả lời kèm nút mở thẳng chức năng, xem chi tiêu trong tháng.
> • Khoá app bằng vân tay/khuôn mặt, lịch sử giao dịch theo ngày, biên lai.
>
> Tính năng cho nhân viên ngân hàng: tạo và quản lý khách hàng, duyệt eKYC, mở tài khoản tiết kiệm/khoản vay, chỉnh lãi suất.
>
> Tài khoản khách hàng do nhân viên tạo, nên app không có màn đăng ký. Dữ liệu lưu trên Google Firebase; chi tiết xem chính sách bảo mật.

- **App icon:** `docs/play/graphics/icon-512.png`
- **Feature graphic:** `docs/play/graphics/feature-1024x500.png`
- **Phone screenshots:** `docs/play/graphics/screenshot-01.png` … `screenshot-06.png`
- **App category:** Finance · **Tags:** chọn tối đa 5 gợi ý liên quan ngân hàng/tài chính cá nhân.
- **Contact details:** Email `datpham1365@gmail.com` (bắt buộc, hiện công khai) · Website: để trống · Phone: để trống.

## 3. Test and release → Testing → Closed testing
- Tạo track (mặc định *Closed testing - Alpha*) → **Countries/regions:** Việt Nam.
- **Testers:** tạo email list `FLUX testers` gồm Gmail của ≥ 12 người thử (thêm cả Gmail của giảng viên nếu thầy đồng ý) → lưu → copy **link opt-in** dán vào `docs/play/tester-invite.md`.
- **Create new release:** tải `app/build/outputs/bundle/release/app-release.aab`, Release name `1.0 (1)`, Release notes (vi-VN): `Bản thử kín đầu tiên của đồ án FLUX Bank (Demo).`
- Review release → **Start rollout to Closed testing** → gửi đi duyệt (Publishing overview → Send for review).
- Ngày người thử thứ 12 opt-in là ngày bắt đầu đếm 14 ngày; ghi vào HANDOFF.
