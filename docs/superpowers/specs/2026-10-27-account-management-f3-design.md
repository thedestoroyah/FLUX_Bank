# FLUX Bank — Feature 3: Quản lý tài khoản (Checking · Saving · Mortgage)

- Ngày: 2026-10-27 · Hạn nộp đồ án: 2026-12-01
- Nền tảng: đợt 1 đã xong ([2026-10-26-ui-foundation-f1-f2-design.md](2026-10-26-ui-foundation-f1-f2-design.md)), design system và `TransactionAuthSheet` dùng lại nguyên.
- Mockup: [mockups/2026-10-27-f3-accounts.html](mockups/2026-10-27-f3-accounts.html) (**chờ duyệt**)

Ký hiệu: **[Đề]** = yêu cầu của đề bài. **[Chủ dự án]** = chủ dự án đã quyết định. **[Claude]** = Claude đề xuất (chủ dự án nhờ "tự đề xuất thiết kế"), có thể đổi khi duyệt.

---

## 1. Mục đích

Barem dành 0,5 điểm cho Account management, nhưng phần này còn là nền cho Feature 4 (chuyển tiền) và Feature 5 (thanh toán tiện ích): cả hai cần **lịch sử giao dịch** và **cách cập nhật số dư an toàn**. Feature 3 dựng hai thứ đó trên luồng đơn giản nhất là nạp và rút, để Feature 4 chỉ phải thêm loại giao dịch mới.

Đề yêu cầu **[Đề]**:
1. Ba loại tài khoản, mỗi loại có cách hiển thị riêng.
2. Xem số dư Checking và Saving.
3. Saving: xem lãi suất và tiền lãi mỗi tháng. Nhân viên sửa lãi suất theo chính sách ngân hàng.
4. Mortgage: xem số tiền phải trả mỗi tháng **hoặc** mỗi 2 tuần.
5. Xem lịch sử giao dịch.
6. Nạp và rút tiền.

## 2. Mục tiêu, không làm, phản mục tiêu

**Mục tiêu**
- Đủ 6 ý của đề, demo được trong ~2 phút: nhân viên mở sổ tiết kiệm và khoản vay → khách nạp tiền → xem lãi/tháng → nhân viên đổi lãi suất → khách thấy lãi mới → xem khoản vay đổi giữa "hàng tháng" và "2 tuần" → xem lịch sử.
- Số dư chỉ thay đổi khi có một bản ghi giao dịch khớp, và **rules kiểm tra điều này**, không chỉ client.

**Không làm** **[Claude]**
- Không tính và cộng lãi thật vào số dư (không có server/cron trên Spark). App chỉ hiện "lãi dự kiến".
- Saving không có kỳ hạn (không có 1/3/6/12 tháng, không có đáo hạn). Chỉ có một mức lãi suất chung.
- Không giải ngân khoản vay vào Checking, không trả nợ vay (trả nợ có thể làm ở Feature 4 như một loại chuyển tiền).
- Không có lịch trả nợ chi tiết từng kỳ (bảng amortization).
- Không dùng VNPay/Stripe cho nạp tiền. Nguồn tiền nạp là "giả lập". Feature 4 tích hợp cổng thanh toán và có thể thay nguồn này.
- Không đóng/xoá tài khoản.

**Phản mục tiêu**
- Khách tự sửa được số dư bằng cách gọi thẳng Firestore mà không tạo bản ghi giao dịch tương ứng, hoặc rút quá số dư.
- Ba loại tài khoản dùng chung một màn hình chỉ khác nhãn.
- Nhân viên đổi lãi suất mà khách không thấy thay đổi (khiến phần demo bị hỏng).
- Lịch sử giao dịch phải tải toàn bộ collection hoặc chậm thấy rõ.

## 3. Ràng buộc
Giữ như đợt 1: Kotlin + XML/ViewBinding, MVVM, Firestore Spark, chỉ giao diện sáng, chữ tiếng Việt trong `strings.xml`, tiền VND kiểu `Long`. Màn hình mới phải được **duyệt mockup trước khi code giao diện** **[Chủ dự án]**. Làm theo TDD.

## 4. Mô hình nghiệp vụ **[Claude]**

| Loại | Ai mở | Hiển thị chính | Nạp/rút |
|---|---|---|---|
| Checking | Tự động khi nhân viên tạo khách (đã có) | Số dư | Có |
| Saving | Nhân viên mở trong chi tiết khách, số dư 0 ₫ | Số dư, lãi suất %/năm, **lãi dự kiến mỗi tháng**, lãi dự kiến 12 tháng | Có |
| Mortgage | Nhân viên mở với số tiền vay và thời hạn | **Dư nợ**, số tiền mỗi kỳ (bật giữa Hàng tháng / 2 tuần), kỳ tiếp theo, lãi suất, thời hạn, tổng lãi | Không |

- Một khách có thể có nhiều Saving và nhiều Mortgage (không giới hạn, để code đơn giản).
- **Lãi suất tiết kiệm áp dụng tức thì**: Saving không lưu lãi suất mà đọc mức chung trong `config/rates`. Nhân viên đổi lãi suất thì mọi khách thấy ngay mức mới.
- **Lãi suất vay cố định từ lúc mở**: Mortgage chép `mortgageRate` hiện tại vào tài khoản khi tạo. Nhân viên đổi lãi suất vay thì chỉ ảnh hưởng khoản vay mở sau đó. Khoản vay là hợp đồng, nên giữ cố định là hợp lý.
- Mortgage hiện thời chưa có thanh toán nên dư nợ = số tiền vay. Trường `balance` của Mortgage được hiểu là dư nợ.

### Công thức (thuần Kotlin, có unit test)
- Lãi tiết kiệm/tháng = `floor(balance × savingRate / 100 / 12)`.
- Tiền trả mỗi kỳ (niên kim): `P × r / (1 − (1 + r)^−n)`, làm tròn **lên** đến đồng.
  - Hàng tháng: `r = rate/100/12`, `n = termMonths`.
  - 2 tuần: `r = rate/100/26`, `n = round(termMonths × 26 / 12)`.
  - `rate = 0` thì mỗi kỳ trả `ceil(P / n)`.
- Kỳ tiếp theo: kỳ đầu tiên sau hôm nay, tính từ `startDate` cộng k tháng (hàng tháng) hoặc k × 14 ngày (2 tuần).
- Tổng lãi = tiền mỗi kỳ × n − P.

### Giới hạn nạp/rút **[Claude]**
- Tối thiểu 10.000 ₫ mỗi lần, cả nạp và rút.
- Nạp tối đa 100.000.000 ₫ mỗi lần, vì nguồn tiền là giả lập.
- Rút không vượt số dư.
- Chỉ khách đã eKYC `VERIFIED` mới được nạp/rút. Chưa xác minh thì hiện thông báo kèm nút dẫn tới eKYC.
- Mọi lần nạp/rút đều qua `TransactionAuthSheet` (OTP; từ 10.000.000 ₫ thêm bước quét mặt), đúng yêu cầu bảo mật của Feature 2.

## 5. Dữ liệu Firestore **[Claude]**

**`accounts/{id}`**: giữ các trường cũ, thêm:
- `lastTxId: String?`: id của giao dịch vừa làm đổi số dư (rules dùng để đối chiếu, xem §6).
- Chỉ có ở Mortgage: `loanAmount: Long`, `annualRate: Double` (%/năm), `termMonths: Int`, `startDate: Long`.

**`transactions/{id}`** (mới, chỉ tạo, không sửa/xoá):
`accountId`, `ownerUid`, `accountNumber`, `type` (`DEPOSIT` | `WITHDRAW`; Feature 4/5 thêm loại khác), `amount: Long` (> 0), `balanceAfter: Long`, `description: String`, `createdAt: Long`.

**`config/rates`** (một document): `savingRate: Double`, `mortgageRate: Double` (đều là %/năm, ví dụ 4.5 và 8.5), `updatedAt: Long`, `updatedBy: String`. Chưa có document thì app dùng mặc định 4.5 / 8.5, và nhân viên lưu lần đầu sẽ tạo document.

**Index**: một composite index `transactions (ownerUid ASC, createdAt DESC)`, tạo một lần qua link trong lỗi Firestore hoặc Console (miễn phí). README hướng dẫn cách tạo.
- shortcut: màn chi tiết tài khoản và bộ lọc theo tài khoản ở tab Lịch sử đều lọc ở client trên 100 giao dịch mới nhất của khách. Như vậy chỉ cần một index. Nếu sau này cần xem xa hơn thì thêm index `(accountId, createdAt)` và phân trang.

## 6. Security Rules **[Claude]**

Nạp và rút chạy trong một **Firestore transaction**: đọc tài khoản, kiểm tra số dư, rồi ghi cùng lúc `accounts/{id}` (`balance`, `lastTxId`) và `transactions/{txId}` mới. Rules ràng buộc hai document với nhau:

- `accounts` **update bởi khách**: là chủ tài khoản, `type` thuộc CHECKING/SAVING, chỉ đổi đúng `balance` và `lastTxId`. Giao dịch `lastTxId` **chưa tồn tại trước đó** (`!exists`) nhưng tồn tại sau khi ghi (`getAfter`), có cùng `accountId` và `balanceAfter == balance mới`, và:
  - `DEPOSIT`: balance mới = cũ + amount;
  - `WITHDRAW`: balance mới = cũ − amount, đồng thời ≥ 0.
- `transactions` **create**: `ownerUid == auth.uid`, `type` thuộc DEPOSIT/WITHDRAW, `amount` là số nguyên ≥ 10.000 (DEPOSIT ≤ 100.000.000), và `getAfter(accounts/{accountId}).lastTxId == txId` (bắt buộc ghi cùng batch với tài khoản). Không cho update/delete. Đọc: chủ sở hữu hoặc nhân viên.
- `accounts` **create bởi nhân viên**: giữ như cũ, thêm kiểm tra `balance >= 0`, và nếu là MORTGAGE thì `loanAmount > 0`, `termMonths` trong 1..360.
- `config/rates`: đã đăng nhập thì đọc được, chỉ nhân viên được ghi; mỗi mức lãi suất trong (0, 30].

Giới hạn còn lại (ghi vào báo cáo): nguồn tiền nạp là giả lập nên khách có thể tự "nạp" tiền, nhưng mỗi lần đều để lại bản ghi và bị chặn ở 100 triệu. OTP vẫn chỉ được kiểm tra trên thiết bị, như ở Feature 2.

## 7. Màn hình (xem mockup)

**Khách hàng**
1. **Thẻ trên trang chủ** (đã có): chạm vào thẻ thì mở **Chi tiết tài khoản**. Thẻ Mortgage ghi "Dư nợ" thay cho "Số dư".
2. **Chi tiết Checking / Saving**: số dư, số tài khoản kèm nút sao chép, hai nút Nạp và Rút. Saving có thêm khối lãi suất (%/năm, lãi dự kiến/tháng, lãi dự kiến 12 tháng). Bên dưới là giao dịch gần đây của tài khoản.
3. **Chi tiết Mortgage**: dư nợ, nút chuyển **Hàng tháng / 2 tuần**, số tiền mỗi kỳ (chữ lớn), kỳ tiếp theo, các dòng: số tiền vay, lãi suất, thời hạn, ngày bắt đầu, tổng lãi, tổng phải trả.
4. **Nạp tiền / Rút tiền** (cùng một màn, khác chế độ): chọn tài khoản (chip Checking/Saving), ô nhập số tiền, chip nhanh (100k · 500k · 1tr · 5tr), dòng nguồn/đích giả lập, dòng "Số dư sau giao dịch". Bấm Tiếp tục thì mở `TransactionAuthSheet`.
5. **Biên lai**: dấu tích, số tiền, loại, tài khoản, mã giao dịch, thời gian, số dư mới, nút "Về trang chủ". Chạm vào một dòng trong lịch sử cũng mở màn này (chế độ xem).
6. **Tab Lịch sử** (thay `ComingSoonFragment`): chip lọc (Tất cả + từng tài khoản), nhóm theo ngày (Hôm nay / Hôm qua / dd/MM/yyyy). Tiền vào màu Emerald có dấu +, tiền ra màu Obsidian có dấu −.
7. **Trang chủ**: mục "Giao dịch gần đây" hiện 3 giao dịch mới nhất, "Xem tất cả" chuyển sang tab Lịch sử. Ô tiện ích **Nạp tiền** và **Rút tiền** mở màn 4. Ô **Tiết kiệm** mở chi tiết Saving đầu tiên; nếu khách chưa có Saving thì hiện empty state "Liên hệ nhân viên để mở tài khoản tiết kiệm".

**Nhân viên**
8. **Tab Lãi suất** (thay `ComingSoonFragment`): hai thẻ "Tiết kiệm" và "Vay thế chấp", mỗi thẻ ghi mức %/năm, ai sửa và lúc nào. Nút Sửa mở bottom sheet nhập số, có dòng ví dụ ("100.000.000 ₫ gửi tiết kiệm → lãi 375.000 ₫/tháng"), và nút Lưu.
9. **Chi tiết khách** (đã có): thêm mục "Tài khoản" liệt kê các tài khoản của khách, và nút **Mở tài khoản** để mở màn chọn Saving hoặc Mortgage. Mortgage cần nhập số tiền vay và thời hạn (chip 12 · 60 · 120 · 240 tháng hoặc tự nhập); lãi suất lấy từ `config/rates`, chỉ hiện chứ không sửa. Trước khi xác nhận có xem trước số tiền mỗi tháng.

## 8. Kiến trúc code **[Claude]**
- `data/model`: `BankAccount` thêm trường (§5); `Transaction` + `TransactionType`; `Rates`.
- `data/repository`: `AccountRepository` thêm `getAccount`, `openSaving`, `openMortgage`, `deposit`, `withdraw` (chạy `runTransaction`, trả `Result<Transaction>`, lỗi `InsufficientFunds` có thông báo riêng). Thêm `TransactionRepository.listRecent(ownerUid, limit)` và `RatesRepository.get()/update()`. Tất cả đăng ký trong `FluxApp`, mỗi repository có bản giả trong `fakes/Fakes.kt`.
- `domain/InterestCalculator`: hàm thuần cho §4, viết test trước. Kiểm tra giới hạn số tiền nạp/rút đặt trong `Validators`.
- `ui/customer/account/` (chi tiết, nạp/rút, biên lai), `ui/customer/history/`, `ui/officer/rates/`, `ui/officer/customers/OpenAccount*`.
- Thay các chỗ đang trỏ tới `ComingSoonFragment` ở `CustomerMainActivity.fragmentFor()`, `HomeFragment` và `OfficerMainActivity.fragmentFor()`.
- `firestore.rules` và README (index, rules mới, luồng demo F3).

## 9. Kiểm thử
- Unit: `InterestCalculator` (có trường hợp rate = 0, 2 tuần, làm tròn), validator số tiền, các ViewModel (nạp thiếu eKYC, rút quá số dư, đổi chế độ Mortgage, lọc lịch sử, nhân viên sửa lãi suất ngoài khoảng).
- Rules: kiểm tra tay bằng Rules Playground theo danh sách trong README (khách tự tăng `balance` mà không có giao dịch → bị chặn; rút làm số dư âm → bị chặn; khách ghi `config/rates` → bị chặn).
- Chạy thử trên máy thật theo kịch bản demo ở §2.

## 10. Để builder tự quyết
Tên class và layout chi tiết, có dùng listener realtime hay tải lại ở `onResume`, hiệu ứng chuyển màn, câu chữ cho empty state và thông báo lỗi (nhưng phải là tiếng Việt, rõ nguyên nhân và cách xử lý).
