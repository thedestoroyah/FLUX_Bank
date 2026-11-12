# FLUX Bank — Feature 7: Trợ lý AI (chatbot)

- Ngày: 2026-11-12 · Hạn nộp đồ án: 2026-12-01
- Nền tảng: F1–F6 đã nằm trên `main`. F7 **không ghi Firestore, không đổi rules**.
- Mockup: [mockups/2026-11-12-f7-assistant.html](mockups/2026-11-12-f7-assistant.html) (**đã duyệt 2026-11-12**; spec cũng đã duyệt cùng ngày)
- Design system: [DESIGN.md](../../../DESIGN.md)

Ký hiệu: **[Đề]** = yêu cầu của đề bài. **[Chủ dự án]** = chủ dự án đã quyết định. **[Claude]** = Claude tự quyết (chủ dự án bỏ qua phần hỏi đáp); mọi mục [Claude] đều có thể bị chủ dự án sửa khi duyệt.

---

## 1. Mục đích

Barem dành 0,5 điểm cho Chatbot. Đề yêu cầu **[Đề]**:
1. Tích hợp chatbot thông minh, người dùng **hỏi trong màn chat**.
2. Chatbot trả lời **kèm link tới chức năng được hỏi**: nạp tiền điện thoại, trả hoá đơn hằng tháng, **xem giao dịch trong tháng** và các hoạt động tài chính khác.

Chỗ ăn điểm: link là **nút bấm mở đúng màn hình thật** của app (không phải chữ hướng dẫn), và bot **trả lời được câu hỏi về chính dữ liệu của khách** ("tháng này tôi chi bao nhiêu?").

## 2. Mục tiêu, không làm, phản mục tiêu

**Mục tiêu**
- Ô "Trợ lý AI" trên trang chủ mở màn chat (thay `ComingSoonFragment`).
- Hiểu tiếng Việt tự nhiên, có dấu hay không dấu ("nap dt cho me", "tháng này tiêu hết bao nhiêu rồi").
- Mỗi câu trả lời có 0–3 **nút hành động**; chạm là mở đúng màn của F3–F6. Quay lại (back) thì cuộc trò chuyện vẫn còn.
- Trả lời câu hỏi về số dư và chi tiêu tháng này bằng số liệu thật của khách.
- Trả lời câu hỏi thường gặp của FLUX: hạn mức, OTP, quét mặt, lãi suất, giờ mở cửa chi nhánh.
- **Chi phí 0 đồng, Firebase vẫn ở gói Spark, không billing** **[Chủ dự án: tiết kiệm tối đa]**.
- Mất mạng hay hết lượt miễn phí thì bot **vẫn trả lời được** các yêu cầu mở chức năng (chế độ cơ bản, §5).

**Không làm** **[Claude]**
- Bot **không tự thực hiện giao dịch**, không điền sẵn số tiền / số điện thoại vào form. Nó chỉ mở màn; mọi giao dịch vẫn qua form + OTP như cũ.
- Không lưu lịch sử chat lên Firestore; đóng màn chat (back về trang chủ) là xoá.
- Không gửi giọng nói, không gửi ảnh, không streaming từng chữ.
- Không chatbot cho nhân viên.
- Không bộ lọc theo tháng tuỳ chọn ở tab Lịch sử: chỉ có màn "giao dịch tháng này" mở từ bot (§6).

**Phản mục tiêu**
- Bot hỏi hoặc nhận mật khẩu, mã OTP, số thẻ.
- Bot bịa số liệu (số dư, chi tiêu) hoặc bịa chức năng FLUX không có.
- Nút hành động mở sai màn hoặc làm crash app.
- Màn chat treo khi mạng chậm: luôn có chỉ báo "đang trả lời" và tối đa ~20 s là có câu trả lời (từ AI hoặc chế độ cơ bản).
- Phát sinh chi phí, hoặc project Firebase bị chuyển lên Blaze.

## 3. Ràng buộc
Giữ như F1–F6: Kotlin + XML/ViewBinding, MVVM, chỉ giao diện sáng, chữ tiếng Việt trong `strings.xml`, chỉ dùng token của design system. Màn mới phải **duyệt mockup trước khi code giao diện** **[Chủ dự án]**. Làm theo TDD. Không commit `.idea/*`.

## 4. Công nghệ **[Claude]**

| Phần | Chọn | Lý do |
|---|---|---|
| Mô hình | **Gemini qua Firebase AI Logic**, backend **Gemini Developer API** (`Firebase.ai(backend = GenerativeBackend.googleAI())`), model `gemini-3.8-flash` (tên lấy từ trang get-started ngày 2026-11-12, để trong một hằng số) | Firebase AI Logic miễn phí, Gemini Developer API có free tier chạy trên **gói Spark, không billing** (đã kiểm trang pricing 2026-11-12). Gọi qua proxy của Firebase nên **không có API key Gemini trong APK**. Dùng chung project Firebase hiện có. |
| Thư viện | `com.google.firebase:firebase-ai` | Phải **nâng Firebase BoM 33.4.0 → 35.x** (bản đã ra ≥ 2 tuần). Từ BoM 34 không còn artifact `-ktx`: đổi `firebase-auth-ktx` → `firebase-auth`, `firebase-firestore-ktx` → `firebase-firestore` (code hiện không import `com.google.firebase.ktx`, nên chỉ đổi Gradle). Chạy lại toàn bộ test sau khi nâng. |
| Định dạng trả lời | **Structured output**: `responseMimeType = "application/json"` + `responseSchema` `{ reply: string, actions: string[] (enum §5) }` | Không cần function calling; app tự kiểm tra từng action, action lạ thì bỏ. Parse bằng `org.json` như F6. |
| Chế độ cơ bản | Bộ so từ khoá chạy trên máy (`AssistantFallback`), dùng `TextNormalizer.stripDiacritics` | Bot vẫn chạy khi mất mạng / hết quota / lỗi. Có unit test. |

Việc trên Console (chủ dự án làm, ghi vào README): Firebase Console → **AI Logic** → Get started → chọn **Gemini Developer API**. Không cần thẻ.

**App Check** **[Claude]**: chưa bật trong F7. Không có App Check thì ai có file cấu hình Firebase của app cũng có thể dùng quota Gemini miễn phí của project (không mất tiền, chỉ hết lượt). Bật App Check (Play Integrity cho bản Play, debug provider cho bản debug) ở bước đưa lên Google Play; ghi vào "Giới hạn đã biết".

**Dữ liệu gửi cho Google** (ghi vào báo cáo và Data safety khi lên Play): ở free tier, Google có thể dùng nội dung gửi lên để cải thiện sản phẩm. Vì vậy app chỉ gửi **số liệu tối thiểu** (§5, "Ngữ cảnh"): không số tài khoản, không SĐT, không CCCD, không tên người nhận. Màn chat có dòng nhắc "Không nhập mật khẩu hay mã OTP".

## 5. Nghiệp vụ **[Claude]**

### Hành động (link) bot được phép trả
Enum `AssistantAction`, mỗi giá trị có nhãn nút và icon (dùng lại icon/màu của `Utility`):

| Action | Mở màn | Nhãn nút |
|---|---|---|
| `TOPUP` | `TopUpFragment()` | Nạp tiền điện thoại |
| `BILL_ELECTRICITY` | `BillPayFragment.newInstance(ELECTRICITY)` | Thanh toán tiền điện |
| `BILL_WATER` | `BillPayFragment.newInstance(WATER)` | Thanh toán tiền nước |
| `MONTH_HISTORY` | Màn giao dịch tháng này (§6) | Xem giao dịch tháng 11 (tháng hiện tại) |
| `TRANSFER` | tab Chuyển tiền (`showTab(TRANSFER)`) | Chuyển tiền |
| `DEPOSIT` / `WITHDRAW` | `MoneyMoveFragment.newInstance(true/false)` | Nạp tiền / Rút tiền |
| `SAVING` | chi tiết tài khoản Tiết kiệm (hoặc màn "chưa có" như ô trang chủ) | Tài khoản tiết kiệm |
| `FLIGHT` / `MOVIE` / `HOTEL` / `SHOP` | `CatalogFragment.newInstance(kind)` | Vé máy bay / Vé xem phim / Khách sạn / Mua sắm |
| `BRANCH` | `BranchMapFragment()` | Chi nhánh gần nhất |

Cách mở các ô tiện ích phải **dùng chung một chỗ** với `HomeFragment.openUtility` (tách hàm dùng chung, không chép `when`), để bot và trang chủ luôn mở cùng một màn.

### Một lượt hỏi đáp
1. Khách gửi câu hỏi (tối đa 500 ký tự, nút gửi khoá khi rỗng hoặc đang chờ trả lời).
2. App dựng **ngữ cảnh** mới (§ dưới), gọi Gemini với system instruction + ngữ cảnh + **10 lượt gần nhất** của cuộc trò chuyện.
3. Parse JSON → câu trả lời + tối đa 3 action hợp lệ. Hiện bong bóng bot và các nút dưới nó.
4. Lỗi bất kỳ (mạng, quota, JSON hỏng, quá 20 s) → trả lời bằng **chế độ cơ bản** cho cùng câu hỏi, bong bóng có nhãn nhỏ "Chế độ cơ bản".

### System instruction (tóm ý, builder viết bản đầy đủ trong code)
- Là "Trợ lý FLUX", trả lời **tiếng Việt**, ngắn (≤ 3 câu), thân thiện, xưng "mình", gọi khách là "bạn".
- Chỉ nói về FLUX Bank và tài chính cá nhân; câu ngoài lề thì từ chối nhẹ và gợi ý việc bot làm được.
- **Không bao giờ hỏi hay nhận mật khẩu, OTP, số thẻ.** Nếu khách gửi, nhắc không chia sẻ.
- Số liệu chỉ lấy từ ngữ cảnh; không có thì nói không biết. Không tự thực hiện giao dịch: muốn làm gì thì trả action để khách tự bấm.
- Kiến thức cố định của FLUX: hạn mức 10.000 ₫–100.000.000 ₫/lần, 200.000.000 ₫/ngày (chuyển + thanh toán gộp); OTP 6 số, 120 giây; giao dịch ≥ 10.000.000 ₫ phải quét mặt; phải eKYC xong mới giao dịch; chi nhánh mở T2–T6 8–17h, T7 8–12h; danh sách tiện ích ở bảng trên. Lấy hằng số từ code (`TransferPolicy`, `OpeningHours`…) thay vì gõ lại số.

### Ngữ cảnh gửi kèm mỗi lượt
Dựng lại mỗi lần gửi (khách có thể vừa trả hoá đơn qua nút rồi quay lại), từ `AccountRepository.listByOwner` + `TransactionRepository.listRecent` + `RatesRepository`:
- Hôm nay (giờ VN), tên gọi của khách (chỉ tên, ví dụ "Bình").
- Từng tài khoản: loại + số dư (Mortgage: dư nợ, tiền trả mỗi tháng). Không có số tài khoản.
- Lãi suất tiết kiệm / vay hiện hành.
- Tháng hiện tại: tổng tiền vào, tổng tiền ra, số giao dịch, chi theo nhóm (chuyển đi, từng dịch vụ PAYMENT, rút), hạn mức ngày còn lại, 5 giao dịch lớn nhất (loại/dịch vụ, số tiền, ngày, **không tên người nhận**).
- Lấy dữ liệu lỗi thì vẫn gọi Gemini, ngữ cảnh ghi "chưa tải được số liệu" (bot sẽ nói không biết thay vì bịa).

### Chế độ cơ bản (`AssistantFallback`)
- Bỏ dấu + chữ thường, so cụm từ khoá → action. Ví dụ: "nap dt / nap dien thoai / the cao / nap card" → `TOPUP`; "tien dien / hoa don dien" → `BILL_ELECTRICITY`; "tien nuoc" → `BILL_WATER`; "thang nay / chi tieu / giao dich thang / lich su" → `MONTH_HISTORY`; "chuyen tien / chuyen khoan" → `TRANSFER`; "chi nhanh / phong giao dich" → `BRANCH`… (builder hoàn thiện bảng, mỗi action ≥ 2 cụm).
- `MONTH_HISTORY` trả kèm câu tóm tắt tính trên máy: "Tháng 11 bạn đã chi 4.250.000 ₫ qua 7 giao dịch."
- Không khớp gì: "Mình chưa hiểu ý bạn. Bạn có thể thử:" + 3 action gợi ý (`TOPUP`, `BILL_ELECTRICITY`, `MONTH_HISTORY`).

## 6. Màn hình (chi tiết trong mockup)

### Màn chat `ui/customer/assistant/AssistantFragment` (màn phụ, mở qua `openScreen`)
- Header kiểu màn phụ hiện có: nút back + "Trợ lý FLUX" + dòng phụ "Trả lời bởi AI, có thể chưa chính xác".
- **Trạng thái trống**: lời chào có tên khách, dòng nhắc "Không nhập mật khẩu hay mã OTP", 4 chip gợi ý chạm là gửi luôn: "Nạp tiền điện thoại", "Thanh toán tiền điện", "Tháng này tôi chi bao nhiêu?", "Chi nhánh gần nhất".
- Bong bóng khách: bên phải, nền Obsidian, chữ trắng. Bong bóng bot: bên trái, nền trắng viền 1dp. Danh sách là RecyclerView, tự cuộn xuống tin mới.
- **Nút hành động** dưới bong bóng bot: dòng tile (icon duotone `cat_*` + nhãn + mũi tên), vùng chạm ≥ 48dp, đúng phong cách lưới tiện ích.
- Đang chờ: bong bóng bot ba chấm.
- Thanh nhập ở đáy: ô chữ `Widget.Flux.TextField` + nút gửi tròn đen (emerald khi có chữ, theo quy tắc emerald cho điểm tương tác). Đẩy lên theo bàn phím (dùng lại `KeyboardInsets`).
- Cuộc trò chuyện giữ trong ViewModel của fragment: mở màn khác qua nút rồi back thì vẫn còn.

### Màn "Giao dịch tháng này" (`MONTH_HISTORY`)
- Dùng lại `HistoryFragment` với tham số tháng (`HistoryFragment.forMonth(YearMonth)`): có header back + "Giao dịch tháng 11/2026", một dòng tổng "Tiền vào / Tiền ra" của tháng, chip lọc tài khoản và nhóm theo ngày như tab Lịch sử. `HistoryViewModel` thêm lọc theo tháng (giờ VN). Tab Lịch sử thường không đổi.
- Giới hạn có sẵn: lịch sử lấy 100 giao dịch mới nhất (đủ cho một tháng ở quy mô đồ án).

## 7. Kiểm thử (TDD)
- `AssistantFallback`: mỗi action có ca có dấu và không dấu, ca không khớp, câu tóm tắt tháng.
- Parser trả lời: JSON đúng; action lạ bị bỏ; > 3 action cắt còn 3; JSON hỏng / thiếu `reply` → lỗi (để chuyển sang chế độ cơ bản).
- Dựng ngữ cảnh: tổng vào/ra và nhóm chi của tháng (biên tháng theo giờ VN), **không chứa số tài khoản / tên người nhận**, ca tải lỗi.
- `AssistantViewModel` với client AI giả: trả lời thành công; lỗi → chế độ cơ bản có nhãn; không gửi khi rỗng / đang chờ; giữ 10 lượt gần nhất.
- `HistoryViewModel`: lọc tháng (giao dịch 23:30 ngày cuối tháng giờ VN thuộc tháng đó).
- Client Gemini thật (`GeminiAssistantClient`) chỉ là lớp mỏng, thử trên máy.

## 8. Demo (khoảng 1 phút)
1. Trang chủ → ô Trợ lý AI → chạm chip "Nạp tiền điện thoại" → bot trả lời + nút → mở màn Nạp ĐT → back, chat vẫn còn.
2. Gõ "tháng này mình tiêu hết bao nhiêu rồi" → bot nêu số thật + nút "Xem giao dịch tháng 11" → mở màn tháng.
3. Gõ "hoá đơn điện" → nút Thanh toán tiền điện.
4. Gõ "mã OTP của tôi là 123456" → bot nhắc không chia sẻ OTP.
5. Bật chế độ máy bay → gõ "chuyển tiền" → vẫn có nút, nhãn "Chế độ cơ bản".

## 9. Giới hạn đã biết (ghi vào báo cáo)
- Free tier Gemini có giới hạn lượt/phút và lượt/ngày; hết lượt thì dùng chế độ cơ bản.
- Chưa bật App Check (bật khi lên Play).
- Nội dung chat gửi tới Google; chỉ gửi số liệu tối thiểu, không định danh tài khoản.
- Bot chỉ mở màn, không điền sẵn form; câu trả lời của AI có thể chưa chính xác (ghi trên header).

## 10. Việc để builder tự quyết
Tên lớp/file, chữ chính xác của system instruction và bảng từ khoá, cách tách hàm mở tiện ích dùng chung, cách giới hạn 20 s, layout chi tiết theo mockup đã duyệt.

## 11. Ghi chú khi làm (2026-11-13)
Các chỗ đã đổi so với bản duyệt, lý do ghi trong ledger thực thi và commit:
- Firebase BoM **34.19.0** thay vì 35.x: thư viện của BoM 35 biên dịch bằng Kotlin 2.4, AGP 9.4.1 của project kèm Kotlin 2.2.
- Model **`gemini-3.5-flash`, thinking tắt** (`thinkingBudget = 0`): `gemini-3.8-flash` liên tục báo quá tải (HTTP 500) ở free tier; để thinking mặc định thì một lượt > 60 s, tắt thì ~6 s.
- Luồng bật AI Logic mới tự **bắt buộc App Check**; chủ dự án đã chọn **Unenforce** tạm (đúng §4: App Check làm khi lên Play).
- Nút `TRANSFER` mở **màn chọn kiểu chuyển** (`TransferHubFragment`) dạng màn phụ, không chuyển tab (chuyển tab xoá back stack và mất cuộc trò chuyện).
- Câu có mã OTP / mật khẩu được trả lời **ngay trên máy**, không gửi lên Gemini và không nằm trong lịch sử gửi đi (siết thêm phản mục tiêu ở §2).
- Hạn 20 s tính cho cả tải số liệu (tối đa 5 s) lẫn gọi AI.

