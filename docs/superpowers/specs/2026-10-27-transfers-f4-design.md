# FLUX Bank — Feature 4: Chuyển tiền & cổng thanh toán VNPay

- Ngày: 2026-10-27 · Hạn nộp đồ án: 2026-12-01
- Nền tảng: F1–F3 đã xong. Dùng lại `TransactionAuthSheet`, `MoneyMoveFragment`, `ReceiptFragment`, `TransactionRowAdapter`, tab Lịch sử và hợp đồng "số dư chỉ đổi kèm một giao dịch mới" của F3 ([2026-10-27-account-management-f3-design.md](2026-10-27-account-management-f3-design.md)).
- Mockup: [mockups/2026-10-27-f4-transfers.html](mockups/2026-10-27-f4-transfers.html) (**đã duyệt 2026-10-27**)

Ký hiệu: **[Đề]** = yêu cầu của đề bài. **[Chủ dự án]** = chủ dự án đã quyết định. **[Claude]** = Claude tự quyết (chủ dự án bỏ qua phần hỏi đáp và bảo "bắt tay vào làm luôn"); mọi mục [Claude] đều có thể bị chủ dự án sửa khi duyệt.

---

## 1. Mục đích

Barem dành 1 điểm cho Transaction, mức cao nhất trong các feature. Đề yêu cầu **[Đề]**:
1. Giao dịch phải theo yêu cầu bảo mật của mục 2 (2FA bằng OTP).
2. Hệ thống **kiểm tra giao dịch có được phép và hợp lệ không trước khi ghi** vào cơ sở dữ liệu.
3. Chuyển tiền tới tài khoản **cùng ngân hàng** và **ngân hàng khác**.
4. Tích hợp cổng thanh toán **VNPay hoặc Stripe**.

F4 còn đóng lỗ hổng mà handoff đã ghi: khách đang tự "nạp" tiền giả lập được, và khi có chuyển tiền thì số tiền đó chuyển được cho người khác. F4 cho phép nạp tiền chỉ qua VNPay.

## 2. Mục tiêu, không làm, phản mục tiêu

**Mục tiêu**
- Đủ 4 ý của đề, demo trong khoảng 2–3 phút (§9).
- Tiền chỉ được **cộng vào tài khoản người khác** khi cùng lúc **trừ đúng số tiền đó** ở tài khoản của người gửi, và **rules kiểm tra điều này**, không chỉ client kiểm tra.
- Người gửi thấy đúng tên người nhận trước khi chuyển, và tên đó không giả mạo được.

**Không làm** **[Claude]**
- Không tra người nhận theo SĐT, chỉ theo số tài khoản (gõ tay, chọn "gần đây" hoặc quét QR). Lý do: tra theo SĐT cần thêm một bảng chỉ mục SĐT → tài khoản và phải giữ nó khớp khi khách đổi SĐT. Số tài khoản đã có nút sao chép từ F3.
- Không có ngân hàng khác thật. Chuyển liên ngân hàng là giả lập: tiền bị trừ ở FLUX, không tra được tên người nhận, không có bên nào nhận tiền.
- Không thu phí chuyển tiền.
- Không trả nợ khoản vay bằng chuyển tiền. Mortgage vẫn chỉ xem.
- Không gửi thông báo đẩy cho người nhận (Spark không có server để gửi FCM). Người nhận thấy tiền trong Lịch sử và số dư.
- Không lưu danh bạ người nhận. "Người nhận gần đây" suy ra từ lịch sử chuyển đi.
- Không chuyển tiền đặt lịch hoặc định kỳ.
- Rút tiền giữ nguyên như F3 (giả lập nhận tiền mặt). Không rút ra VNPay.
- Không dùng Stripe. Chỉ VNPay, vì app dùng VND và VNPay có sandbox miễn phí.
- Nhân viên không có màn mới.

**Phản mục tiêu**
- Khách tự gọi thẳng Firestore để cộng tiền vào một tài khoản (của mình hay của người khác) mà không bị trừ tiền ở đâu.
- Một giao dịch chuyển đi được dùng để cộng tiền hai lần.
- Người gửi đặt tên giả cho mình để hiện trong lịch sử của người nhận, hoặc khách đặt tên giả cho tài khoản của mình để người khác tra ra tên đó.
- Số dư bị cộng khi VNPay chưa báo thanh toán thành công.
- Người gửi đọc được số dư hoặc lịch sử của người nhận.
- Khoá bí mật VNPay nằm trong git.

## 3. Ràng buộc
Giữ như F1–F3: Kotlin + XML/ViewBinding, MVVM, Firestore gói Spark (không Cloud Functions), chỉ giao diện sáng, chữ tiếng Việt trong `strings.xml`, tiền VND kiểu `Long`. Màn mới phải **duyệt mockup trước khi code giao diện** **[Chủ dự án]**. Làm theo TDD. Thêm ca test vào `rules-test/rules.test.mjs` **trước** khi sửa rules (handoff §5).

## 4. Nghiệp vụ **[Claude]**

### Ba cách chuyển
| Cách | Nguồn | Đích | Người nhận được cộng tiền |
|---|---|---|---|
| Trong FLUX | Checking hoặc Saving của mình | Số tài khoản FLUX bất kỳ (Checking/Saving), không phải chính tài khoản nguồn | Có, cùng một Firestore transaction |
| Giữa tài khoản của tôi | Checking/Saving của mình | Tài khoản Checking/Saving khác của mình | Có (cùng luồng với "Trong FLUX") |
| Liên ngân hàng | Checking/Saving của mình | Ngân hàng + số tài khoản + tên do người dùng nhập | Không (giả lập) |

"Giữa tài khoản của tôi" chỉ là màn "Trong FLUX" với đích chọn bằng chip. Dữ liệu và rules giống hệt.

### Kiểm tra trước khi ghi (ý 2 của đề)
| Điều kiện | Ở app | Ở rules |
|---|---|---|
| Khách đã eKYC `VERIFIED` | Có (như F3) | Không (giới hạn cũ, handoff mục 7 #14) |
| 10.000 ₫ ≤ số tiền ≤ 100.000.000 ₫ mỗi lần | Có | Có |
| Không vượt số dư | Có | Có (số dư mới ≥ 0) |
| Tổng chuyển đi trong ngày (gồm cả liên ngân hàng) ≤ 200.000.000 ₫ | Có, cộng từ lịch sử hôm nay | Không (rules không cộng dồn được) |
| Đích tồn tại, là Checking/Saving, khác tài khoản nguồn | Có | Có |
| Liên ngân hàng: số tài khoản 6–19 chữ số, tên không rỗng | Có | Không |
| OTP (và quét mặt khi ≥ 10.000.000 ₫) | `TransactionAuthSheet` | Không (giới hạn Spark, như F2) |
| Nội dung ≤ 100 ký tự, mặc định "`<TÊN KHÔNG DẤU>` chuyen tien" | Có | Không |

Hạn mức chọn thấp để demo được lỗi "vượt hạn mức ngày" mà không cần nạp quá nhiều lần. Tất cả là hằng số trong `Validators`.

### Nạp tiền qua VNPay
- Màn Nạp tiền của F3 giữ nguyên, chỉ đổi nguồn tiền thành VNPay. Thứ tự: nhập số tiền → `TransactionAuthSheet` → mở **WebView** tới trang thanh toán sandbox của VNPay → VNPay chuyển về `vnp_ReturnUrl` → app chặn URL đó, **kiểm `vnp_SecureHash`** (HMAC-SHA512), kiểm `vnp_ResponseCode == "00"`, `vnp_TransactionStatus == "00"` và `vnp_Amount == số tiền × 100` → gọi `deposit` → biên lai.
- `vnp_TxnRef` chính là id của document `transactions` sẽ tạo. Rules đã chặn tạo lại id đã có (`!exists`), nên xử lý URL trả về hai lần cũng không cộng tiền hai lần.
- Lưu `vnp_TransactionNo` vào giao dịch (`paymentRef`) để đối soát.
- Mã `vnp_TmnCode` và `vnp_HashSecret` đọc từ `local.properties` (không commit) qua `BuildConfig`. Thiếu khoá thì nút Nạp báo "Chưa cấu hình VNPay", không crash.
- Huỷ, thất bại, sai chữ ký, sai số tiền: quay lại màn Nạp kèm thông báo rõ lý do, số dư không đổi.
- Giới hạn (ghi vào báo cáo): trên Spark không có server, nên chữ ký được kiểm trên máy và khoá bí mật sandbox nằm trong APK. Rules không biết VNPay đã thu tiền hay chưa, nên khách vẫn có thể gọi thẳng Firestore để ghi một giao dịch DEPOSIT. Hướng nâng cấp: Cloud Functions nhận IPN của VNPay rồi mới cộng tiền.

## 5. Dữ liệu Firestore **[Claude]**

**`accountIndex/{accountNumber}`** (mới): danh bạ để tra người nhận, vì `accounts` và `users` của người khác không đọc được.
- `accountId`, `ownerUid`, `type` (CHECKING/SAVING), `holderName` (đúng `users/{ownerUid}.fullName`).
- App hiển thị tên in hoa, bỏ dấu (`TRAN THI BINH`), dùng lại cách bỏ dấu trong `CustomerListViewModel` (nên tách ra `core/format`).
- **Ai tạo**: chính chủ tài khoản. Sau khi trang chủ tải danh sách tài khoản, app kiểm tra một lần mỗi phiên: tài khoản Checking/Saving nào chưa có mục, hoặc mục có `holderName` khác `fullName` hiện tại, thì ghi lại. Một cơ chế này lo cả tài khoản cũ (F1–F3) lẫn tài khoản mới mở. Đổi lại, tài khoản của khách chưa từng đăng nhập thì chưa tra được. Chấp nhận được, vì khách mới nào cũng phải đăng nhập để đổi mật khẩu tạm.

**`transactions/{id}`**: thêm loại và trường mới. Các trường cũ giữ nguyên.
- `type` thêm `TRANSFER_OUT` (tiền ra) và `TRANSFER_IN` (tiền vào).
- `counterpartyName`, `counterpartyAccount`, `counterpartyBank` (mã ngân hàng; `FLUX` khi cùng ngân hàng).
- `pairTxId`: ở giao dịch chuyển trong FLUX, hai giao dịch OUT và IN trỏ vào nhau. Liên ngân hàng thì `null`.
- `paymentRef`: mã giao dịch VNPay, chỉ có ở DEPOSIT.
- Ở `TRANSFER_IN`, `balanceAfter` để trống (`null`), vì người gửi không đọc được số dư của người nhận nên không tính được. Biên lai và các chỗ đang dùng `balanceAfter` phải xử lý `null`.
- `description` (đã có) là nội dung chuyển khoản.

Một lần chuyển trong FLUX ghi 4 document trong **một** `runTransaction`:
1. `accounts/{from}`: `balance` = cũ − amount, `lastTxId` = outId (đọc trong transaction như F3).
2. `transactions/{outId}`: `TRANSFER_OUT`, chủ là người gửi, `balanceAfter` = số dư mới, `pairTxId` = inId.
3. `accounts/{to}`: `balance` = `FieldValue.increment(amount)`, `lastTxId` = inId. **Không đọc** tài khoản đích (người gửi không có quyền đọc).
4. `transactions/{inId}`: `TRANSFER_IN`, chủ là người nhận, `pairTxId` = outId, `counterpartyName` = tên người gửi.

Liên ngân hàng chỉ ghi 1 và 2, với `pairTxId = null`.

## 6. Security Rules **[Claude]**

Viết ca test trước, sau đó mới sửa `firestore.rules`.

- **`accountIndex`**: `get` khi đã đăng nhập, cấm `list` (không cho liệt kê danh bạ). `create`/`update` chỉ khi `get(accounts/{accountId}).ownerUid == auth.uid`, `accountNumber` của tài khoản đó bằng id document, `type` khớp, và `holderName == get(users/{auth.uid}).fullName`. Cấm `delete`.
- **Tài khoản người gửi**: `ownerMoneyMoveAllowed` nhận thêm `TRANSFER_OUT` (cùng phép trừ và điều kiện ≥ 0 như `WITHDRAW`).
- **Tài khoản người nhận** (nhánh mới `transferCreditAllowed`; người ghi không cần là chủ):
  - đích là CHECKING/SAVING, chỉ đổi `balance` và `lastTxId`;
  - `lastTxId` (= inId) chưa tồn tại trước đó, sau khi ghi là `TRANSFER_IN` của đúng tài khoản này, chủ là chủ tài khoản, và số dư mới = cũ + amount;
  - giao dịch cặp `pairTxId` (= outId) chưa tồn tại trước đó, sau khi ghi là `TRANSFER_OUT` của `auth.uid`, cùng `amount`, trỏ ngược `pairTxId == inId`, có `accountId` khác tài khoản đích, và tài khoản nguồn sau khi ghi có `lastTxId == outId` (tức là đã bị trừ tiền trong cùng lần ghi).
- **`transactions` create**:
  - DEPOSIT/WITHDRAW/TRANSFER_OUT: như F3 (`ownerUid == auth.uid`, tài khoản sau khi ghi trỏ `lastTxId` về đây). Thêm `TRANSFER_OUT` ≤ 100.000.000 ₫.
  - TRANSFER_IN: `ownerUid` là chủ tài khoản đích, tài khoản đích sau khi ghi trỏ `lastTxId` về đây, giao dịch cặp là `TRANSFER_OUT` của `auth.uid`, và `counterpartyName == get(users/{auth.uid}).fullName` (người gửi không giả tên được).
  - Đọc: như cũ (chủ sở hữu hoặc nhân viên). Người gửi chỉ đọc được bản OUT, người nhận chỉ đọc được bản IN.

Kiểm tra số lần gọi `get/exists/getAfter`: một lần chuyển trong FLUX chạm 5 document khác nhau, dưới giới hạn 20 của một lần ghi nhiều document.

Ca test tối thiểu (thêm vào `rules.test.mjs`): chuyển hợp lệ 4 document được phép; cộng cho tài khoản người khác mà không trừ nguồn bị chặn; cộng với số tiền khác bản OUT bị chặn; dùng lại một outId đã có bị chặn; TRANSFER_IN với `counterpartyName` giả bị chặn; chuyển làm nguồn âm bị chặn; chuyển vào MORTGAGE bị chặn; tự cộng cho mình bằng TRANSFER_IN không có cặp bị chặn; liên ngân hàng (chỉ OUT) được phép; TRANSFER_OUT > 100 tr bị chặn; `accountIndex` đúng chủ được tạo, sai chủ hoặc tên giả bị chặn, `list` bị chặn.

## 7. Màn hình (xem mockup)

1. **Tab Chuyển tiền** (thay `ComingSoonFragment` ở `CustomerMainActivity.fragmentFor()`): ba dòng Trong FLUX / Liên ngân hàng / Giữa tài khoản của tôi, và "Người nhận gần đây" (tối đa 5 người, khác nhau theo ngân hàng + số tài khoản, lấy từ các `TRANSFER_OUT` mới nhất).
2. **Chuyển trong FLUX**: chip tài khoản nguồn, ô số tài khoản (đủ 12 số thì tự tra `accountIndex`, hiện khung tên người nhận), số tiền, nội dung, dòng hạn mức còn lại hôm nay. Lỗi hiện ngay dưới ô tương ứng.
3. **Liên ngân hàng**: chip nguồn, chọn ngân hàng, số tài khoản, tên người nhận (tự viết hoa, bỏ dấu), số tiền, nội dung, ghi chú "giả lập".
4. **Chọn ngân hàng**: bottom sheet, khoảng 10 ngân hàng Việt Nam cố định trong code (mã + tên chữ, **không dùng logo**), tìm không dấu.
5. **Biên lai**: `ReceiptFragment` thêm các dòng Đến/Từ, ngân hàng, nội dung. Nút phụ "Chuyển tiếp" mở lại form chuyển. `TRANSFER_IN` không có dòng "Số dư mới".
6. **Lịch sử / Giao dịch gần đây**: nhãn mới "Chuyển đến `<TÊN>`", "Nhận từ `<TÊN>`", "Nạp tiền qua VNPay"; dòng phụ liên ngân hàng ghi tên ngân hàng.
7. **Nạp tiền**: màn F3, nguồn tiền đổi thành VNPay.
8. **WebView VNPay**: header của app (nút đóng + chữ "sandbox"), thân là trang của VNPay.
9. **Quét QR** (nút nổi giữa thanh tab, thay `ComingSoonFragment`): camera, khung quét. Mã của FLUX mở màn 2 đã điền sẵn.
10. **Mã của tôi**: QR chứa số tài khoản đang chọn (chip Checking/Saving), tên, nút sao chép.

### Mã QR **[Claude]**
- Nội dung: `fluxbank:<accountNumber>` (chuỗi riêng của app, không theo chuẩn VietQR để khỏi phải làm CRC/EMVCo).
- Sinh mã bằng ZXing `core`, quét bằng ML Kit `barcode-scanning` trên CameraX (CameraX đã có từ eKYC). Đây là hai dependency mới duy nhất của F4.
- **Thứ tự cắt khi thiếu thời gian** (tiếp sau thứ tự trong handoff): bỏ màn 9–10 (QR) trước, sau đó bỏ "Người nhận gần đây". Không cắt rules chuyển tiền và VNPay.

## 8. Kiến trúc code **[Claude]**
- `data/model`: `TransactionType` thêm `TRANSFER_OUT(false)`, `TRANSFER_IN(true)`; `Transaction` thêm các trường §5 (`balanceAfter` thành `Long?`); `Bank` (mã, tên) + danh sách cố định; `Payee` (accountId, accountNumber, holderName, type) cho kết quả tra cứu.
- `data/repository`:
  - `AccountRepository` thêm `transfer(fromId, payee, amount, description)` (FLUX) và `transferExternal(fromId, bank, accountNumber, name, amount, description)`, cùng kiểu trả về `Result<Transaction>` và cùng cách gỡ `InsufficientFundsException` như `move()`. `deposit` nhận thêm `txId` và `paymentRef` (tuỳ chọn) cho VNPay.
  - `PayeeRepository` (mới): `find(accountNumber)` đọc `accountIndex`, `ensureIndexed(accounts, fullName)`.
  - Mỗi repository mới có bản giả trong `fakes/Fakes.kt`; đăng ký trong `FluxApp`.
- `domain/TransferPolicy` (thuần Kotlin, test trước): kiểm tra §4 và trả về lỗi đầu tiên (`BELOW_MIN`, `ABOVE_MAX`, `INSUFFICIENT_FUNDS`, `DAILY_LIMIT`, `SAME_ACCOUNT`, …); tính hạn mức còn lại từ danh sách giao dịch hôm nay.
- `payment/VnPay` (thuần Kotlin, test trước): dựng URL thanh toán (sắp xếp tham số, URL-encode, HMAC-SHA512, `vnp_Amount` × 100, `vnp_CreateDate`/`vnp_ExpireDate` theo giờ Việt Nam) và đọc kết quả trả về (`Success`, `Cancelled`, `Failed(code)`, `BadSignature`, `AmountMismatch`). Test bằng một bộ khoá giả và vector tự tính.
- `ui/customer/transfer/`: `TransferHubFragment`, `TransferFragment` (+ ViewModel, chế độ FLUX / của tôi / liên ngân hàng), `BankPickerSheet`. `ui/customer/qr/`: `QrFragment` (quét + mã của tôi). `ui/customer/account/VnPayFragment` (WebView).
- `TransactionRowAdapter.labelFor()` đổi thành hàm nhận cả `Transaction` (vì nhãn có tên người nhận).
- `firestore.rules`, `rules-test/rules.test.mjs`, README (VNPay sandbox, rules mới, luồng demo F4), `HANDOFF.md`, `PROJECT_CONTEXT.md`.

## 9. Kịch bản demo
Hai khách A và B (B có thể đăng nhập trên máy ảo).
1. A nạp 1.000.000 ₫ qua VNPay sandbox bằng thẻ test NCB → biên lai.
2. A chuyển 500.000 ₫ cho B bằng số tài khoản (thấy tên B) → OTP → biên lai.
3. B mở app: số dư tăng, Lịch sử có "Nhận từ `<TÊN A>`".
4. A chuyển 2.000.000 ₫ sang Vietcombank (giả lập).
5. A thử chuyển 15.000.000 ₫ → thêm bước quét mặt.
6. A thử chuyển quá hạn mức ngày → bị chặn trên form.
7. A quét QR "Mã của tôi" trên máy B → form điền sẵn.

## 10. Việc chủ dự án cần làm
1. Đăng ký tài khoản merchant **VNPay sandbox** (miễn phí, có email trả về `vnp_TmnCode` và `vnp_HashSecret`), rồi thêm hai dòng `VNPAY_TMN_CODE=…` và `VNPAY_HASH_SECRET=…` vào `local.properties`. Builder ghi hướng dẫn chi tiết vào README, kèm thẻ test do VNPay công bố.
2. Publish lại `firestore.rules` sau khi rules test đã pass.
3. Chạy kịch bản §9 trên máy thật.

## 11. Kiểm thử
- Unit: `TransferPolicy` (mọi lỗi, hạn mức ngày qua nửa đêm), `VnPay` (dựng URL, chữ ký đúng/sai, huỷ, sai số tiền), các ViewModel (tra người nhận không thấy, chuyển cho chính mình, vượt số dư, vượt hạn mức, lỗi mạng khi ghi), nhãn giao dịch.
- Rules: các ca ở §6 trên emulator.
- Thiết bị: kịch bản §9.

## 12. Để builder tự quyết
Tên class và layout chi tiết, danh sách 10 ngân hàng, cách debounce khi tra số tài khoản, chuỗi `vnp_ReturnUrl` (miễn là một URL https mà app chặn được trong WebView), câu chữ cho lỗi và empty state (tiếng Việt, nói rõ nguyên nhân và cách xử lý).
