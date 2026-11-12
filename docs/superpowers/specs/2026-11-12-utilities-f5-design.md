# FLUX Bank — Feature 5: Tiện ích (hoá đơn, nạp điện thoại, đặt chỗ, mua sắm)

- Ngày: 2026-11-12 · Hạn nộp đồ án: 2026-12-01 (còn 19 ngày cho F5, F6, F7, Play Store, báo cáo, video)
- Nền tảng: F1–F4 đã xong và nằm trên `main`. Dùng lại `TransactionAuthSheet`, `ReceiptFragment`, `TransactionRowAdapter`, tab Lịch sử, `TransferPolicy`, và hợp đồng "số dư chỉ đổi kèm một giao dịch mới" của F3/F4.
- Mockup: [mockups/2026-11-12-f5-utilities.html](mockups/2026-11-12-f5-utilities.html) (**đã duyệt 2026-11-12**)
- Design system: [DESIGN.md](../../../DESIGN.md) (rút từ code ngày 2026-11-12)

Ký hiệu: **[Đề]** = yêu cầu của đề bài. **[Chủ dự án]** = chủ dự án đã quyết định. **[Claude]** = Claude tự quyết (chủ dự án bỏ qua phần hỏi đáp); mọi mục [Claude] đều có thể bị chủ dự án sửa khi duyệt.

---

## 1. Mục đích

Barem dành 0,5 điểm cho Utilities. Đề yêu cầu **[Đề]**:
1. Trả tiền **điện** và **nước**.
2. **Nạp tiền** vào tài khoản điện thoại.
3. Mua **vé máy bay**, **vé xem phim**, **đặt phòng khách sạn**, hoặc **thanh toán sản phẩm** trên sàn thương mại điện tử.

Điểm thấp và thời gian ít, nên F5 phải **đủ cả 7 tiện ích nhưng gọn**: ba kiểu màn dùng chung, dữ liệu giả lập nằm sẵn trong app, ghi tiền bằng đúng một đường đã có rules.

## 2. Mục tiêu, không làm, phản mục tiêu

**Mục tiêu**
- Bảy ô Tiền điện, Tiền nước, Nạp ĐT, Vé máy bay, Vé phim, Khách sạn, Mua sắm trên trang chủ mở màn thật (thay `ComingSoonFragment`).
- Mỗi lần trả tiền: kiểm hạn mức trước khi ghi, xác thực bằng `TransactionAuthSheet` (OTP, ≥ 10.000.000 ₫ thêm quét mặt), trừ tiền và tạo giao dịch trong một Firestore transaction, ra biên lai, hiện trong Lịch sử và "Giao dịch gần đây".
- Một hoá đơn điện/nước (nhà cung cấp + mã khách hàng + kỳ) chỉ trả được **một lần** cho mỗi khách, và **rules chặn** lần thứ hai.
- Demo cả 7 tiện ích trong khoảng 2 phút (§9).

**Không làm** **[Claude]**
- Không kết nối nhà cung cấp, hãng bay, rạp, khách sạn hay sàn thật. Hoá đơn sinh cố định theo mã; danh mục là dữ liệu cố định trong code. Tên thương hiệu thật (EVN, SAWACO, Vietnam Airlines, CGV, Viettel…) chỉ để minh hoạ, giống danh sách ngân hàng của F4.
- Không lưu danh mục lên Firestore (không tốn lượt đọc, không cần màn quản trị).
- Không chọn ghế, không chọn hạng vé, không vé khứ hồi, không giỏ hàng, không mã giảm giá, không phí dịch vụ.
- Không huỷ hay hoàn tiền vé/đơn hàng.
- Không thanh toán định kỳ, không tự động trừ tiền hoá đơn, không lưu danh bạ mã khách hàng.
- Không trả bằng VNPay hay thẻ: chỉ trừ từ tài khoản Checking/Saving của khách. Không trả từ Mortgage.
- Nhân viên không có màn mới (giao dịch thanh toán của khách vẫn xem được như các giao dịch khác).

**Phản mục tiêu**
- Tiền bị trừ mà không có bản ghi giao dịch, hoặc có bản ghi mà số dư không đổi.
- Trả cùng một hoá đơn hai lần.
- Thanh toán vượt hạn mức 100 tr/lần hoặc 200 tr/ngày (gộp chung với chuyển tiền).
- Giao diện lệch design system: nút emerald, màu nhóm tràn ra ngoài ô icon, chữ < 12sp (DESIGN.md).

## 3. Ràng buộc
Giữ như F1–F4: Kotlin + XML/ViewBinding, MVVM, Firestore gói Spark, chỉ giao diện sáng, chữ tiếng Việt trong `strings.xml`, tiền VND kiểu `Long`. Màn mới phải **duyệt mockup trước khi code giao diện** **[Chủ dự án]**. Làm theo TDD. Thêm ca test vào `rules-test/rules.test.mjs` **trước** khi sửa rules. Không commit `.idea/*`.

## 4. Nghiệp vụ **[Claude]**

### Ba kiểu màn cho bảy tiện ích
| Kiểu | Tiện ích | Người dùng làm gì |
|---|---|---|
| Hoá đơn | Tiền điện, Tiền nước | Chọn nhà cung cấp (chip) → gõ mã khách hàng → Tra cứu → xem hoá đơn kỳ trước → chọn tài khoản → Thanh toán |
| Nạp điện thoại | Nạp ĐT | SĐT (mặc định số của khách, sửa được) → nhà mạng tự nhận theo đầu số → chọn 1 trong 6 mệnh giá → chọn tài khoản → Nạp |
| Danh mục | Vé máy bay, Vé phim, Khách sạn, Mua sắm | Lọc (chip, có thể thêm ngày) → chọn một mục → chọn tuỳ chọn và số lượng → thanh dưới cùng hiện tổng → Thanh toán |

### Hoá đơn điện/nước (giả lập)
- Nhà cung cấp: điện = EVN TP.HCM, EVN Hà Nội, EVN miền Nam; nước = SAWACO, Nước sạch Hà Nội.
- Mã khách hàng: điện = `PE` + 11 chữ số (nhận chữ thường, đổi sang hoa); nước = 8–12 chữ số. Sai định dạng thì báo lỗi dưới ô, không tra.
- Kỳ = tháng trước theo giờ Việt Nam (`TransferPolicy.VN_ZONE`); hạn thanh toán = ngày 25 tháng hiện tại.
- Tra cứu là hàm thuần, **cố định theo (nhà cung cấp, mã, kỳ)**: cùng đầu vào luôn ra cùng tên chủ hộ, địa chỉ (chọn từ danh sách cố định), lượng tiêu thụ và số tiền. Điện 150.000–2.500.000 ₫, nước 50.000–600.000 ₫, làm tròn tới 100 ₫. Mã có 4 số cuối là `0000` trả về "Không tìm thấy hoá đơn" để demo trường hợp lỗi.
- Đã trả hay chưa: app tìm giao dịch của khách có `paymentRef` = khoá hoá đơn (§5). Đã trả thì hiện khối "Bạn đã thanh toán kỳ này lúc …" và nút "Xem biên lai" thay cho nút trả tiền (mockup màn 2).

### Nạp điện thoại
- Nhà mạng theo 3 số đầu: Viettel `032–039, 086, 096–098`; Vinaphone `081–085, 088, 091, 094`; Mobifone `070, 076–079, 089, 090, 093`; Vietnamobile `052, 056, 058, 092`. Không khớp thì báo "Số không thuộc nhà mạng hỗ trợ" và khoá nút.
- Mệnh giá: 10.000 · 20.000 · 50.000 · 100.000 · 200.000 · 500.000 ₫. Số tiền trừ đúng bằng mệnh giá.
- SĐT kiểm bằng validator SĐT đã có (`Validators`).

### Danh mục (dữ liệu cố định trong code)
| Tiện ích | Bộ lọc | Mục | Tuỳ chọn / số lượng | Tổng |
|---|---|---|---|---|
| Vé máy bay | 3 chặng (chip) + ngày bay (từ ngày mai đến +60 ngày) | 3 chuyến mỗi chặng: giờ đi–đến, hãng, số hiệu, giá/khách | Hành khách 1–9 | giá × khách |
| Vé phim | 2 rạp (chip) | 2–3 phim mỗi rạp: tên, thể loại, thời lượng, nhãn tuổi, giá/vé | Suất chiếu hôm nay (chip; suất đã qua thì tắt) · Số vé 1–8 | giá × vé |
| Khách sạn | 3 thành phố (chip) + ngày nhận phòng (từ hôm nay đến +60 ngày) | 2–3 khách sạn mỗi thành phố: tên, hạng sao, loại phòng, giá/đêm | Số đêm 1–14 (hiện ngày trả phòng) | giá × đêm |
| Mua sắm ("FLUX Shop") | Nhóm: Tất cả / Điện tử / Gia dụng / Thời trang | Khoảng 8 sản phẩm: tên, giá | Số lượng 1–10 | giá × số lượng |

Tên phim, khách sạn, sản phẩm là tên tự đặt. Mỗi lần thanh toán ở kiểu Danh mục sinh **mã đặt chỗ** 6 ký tự (chữ hoa và số, bỏ `0 O 1 I`), in trên biên lai.

### Kiểm tra trước khi ghi
- Chỉ khách eKYC **VERIFIED** được thanh toán (như nạp/rút và chuyển tiền).
- Nguồn tiền: Checking hoặc Saving của khách (chip như mockup).
- `TransferPolicy.checkAmount(amount, balance, remainingToday)`: tối thiểu 10.000 ₫, tối đa 100.000.000 ₫/lần, không quá số dư, không quá hạn mức còn lại trong ngày.
- **Hạn mức ngày gộp chung**: `TransferPolicy.sentToday` cộng cả `TRANSFER_OUT` và `PAYMENT`. Đây là đổi hành vi của F4 và phải có test.
- Tổng ở kiểu Danh mục có thể vượt 100 tr (ví dụ 9 khách hạng đắt) thì hiện lỗi hạn mức trên thanh dưới cùng và khoá nút.

## 5. Dữ liệu Firestore **[Claude]**

Không thêm collection. `transactions/{id}` thêm một loại và hai field:

| Field | Giá trị cho thanh toán dịch vụ |
|---|---|
| `type` | `PAYMENT` (mới; tiền ra, như `WITHDRAW`) |
| `service` (mới) | Tên enum `Utility` của ô tiện ích: `TIEN_DIEN`, `TIEN_NUOC`, `NAP_DT`, `VE_MAY_BAY`, `VE_PHIM`, `KHACH_SAN`, `MUA_SAM`. Lịch sử và biên lai lấy icon, màu và nhãn từ đây. |
| `counterpartyName` | Nhà cung cấp / hãng / rạp / khách sạn / "FLUX Shop" |
| `counterpartyAccount` | Mã khách hàng (hoá đơn), SĐT (nạp ĐT), số hiệu chuyến / tên phim / tên khách sạn / tên sản phẩm (danh mục) |
| `description` | Tiêu đề hiện ở Lịch sử, ví dụ "Tiền điện kỳ 10/2026", "Nạp ĐT 0912 345 678", "Vé máy bay VN 213" |
| `paymentRef` | Hoá đơn: khoá hoá đơn `<nhà cung cấp>:<mã>:<yyyyMM>`. Danh mục: mã đặt chỗ. Nạp ĐT: null. |
| `details` (mới) | Danh sách có thứ tự các cặp nhãn–giá trị cho biên lai (ví dụ "Ngày bay" → "20/11/2026 · 2 khách"). Dạng lưu do builder chọn, miễn giữ được thứ tự. |
| `balanceAfter` | Số dư mới, như WITHDRAW |

**Chống trả trùng hoá đơn:** id của giao dịch thanh toán hoá đơn **suy ra từ khoá hoá đơn và uid** (ví dụ `bill_<uid>_<nhà cung cấp>_<mã>_<yyyyMM>`). Ghi lần hai vào cùng id là `update` trên `transactions`, rules đã cấm, nên Firestore transaction thất bại; app báo "Hoá đơn này đã được thanh toán". Các loại khác dùng id tự sinh như cũ.

Truy vấn "đã trả chưa": `transactions` where `ownerUid == uid` and `paymentRef == khoá`, limit 1. Chỉ có điều kiện bằng nên không cần index ghép.

## 6. Security Rules **[Claude]**
Thay đổi tối thiểu, thêm `PAYMENT` vào đúng hai chỗ:
- `ownerMoneyMoveAllowed`: `PAYMENT` trừ tiền như `WITHDRAW` / `TRANSFER_OUT` (số dư mới = số dư cũ − amount, ≥ 0).
- `txCreateAllowed`: `PAYMENT` thuộc người ghi (`ownerUid == auth.uid`) và `amount <= 100000000`.

Ca test mới trong `rules-test/` (viết trước khi sửa rules): trả hợp lệ được phép; > 100 tr bị chặn; `ownerUid` của người khác bị chặn; `balanceAfter`/số dư không khớp bị chặn; ghi lần hai vào cùng id hoá đơn bị chặn; trả từ tài khoản người khác bị chặn.

Giới hạn (ghi vào báo cáo): rules không biết số tiền hoá đơn hay giá vé đúng bao nhiêu, vì dữ liệu giả lập nằm ở app (Spark không có server). Khách sửa app có thể trả số tiền khác, nhưng vẫn chỉ trừ tiền của chính mình và vẫn có bản ghi.

## 7. Màn hình (xem mockup)
| # | Màn | Ghi chú |
|---|---|---|
| 1 | Hoá đơn (điện) | Nút "Tra cứu" nằm cạnh/ngay dưới ô mã; khối hoá đơn chỉ hiện sau khi tra. Nút dưới cùng ghi kèm số tiền. |
| 2 | Hoá đơn đã trả (nước) | Khối mint "đã thanh toán", nút phụ "Xem biên lai" mở biên lai của giao dịch đó. |
| 3 | Nạp điện thoại | Pill nhà mạng trong ô SĐT; lưới mệnh giá 3×2, ô chọn viền emerald nền emerald nhạt. |
| 4–7 | Danh mục | Chung một layout: chip lọc, (ô ngày), danh sách mục chọn một (viền emerald khi chọn), ô đếm, thanh tổng cố định ở đáy với nút đen. Mua sắm dùng lưới 2 cột. |
| 8 | Xác nhận | `TransactionAuthSheet` có sẵn, dòng tóm tắt theo dịch vụ. |
| 9 | Biên lai | `ReceiptFragment` có sẵn + khối mã đặt chỗ / mã hoá đơn + các dòng `details`. |
| 10 | Lịch sử | Dòng `PAYMENT` dùng icon + màu của ô tiện ích (`Utility.icon/tint/tileBg`) thay mũi tên. |

Mọi màn mở bằng `ScreenHost.openScreen()` từ `HomeFragment` (thay nhánh `else -> ComingSoonFragment`). Chi nhánh và Trợ lý AI vẫn là `ComingSoonFragment` (F6, F7).

## 8. Kiến trúc code **[Claude]**
- `domain/utility/`: `BillLookup` (sinh hoá đơn cố định theo mã, kiểm định dạng mã, khoá hoá đơn), `Carriers` (nhận nhà mạng), `ServiceCatalog` (dữ liệu chuyến bay, phim, khách sạn, sản phẩm), `BookingCode` (sinh mã đặt chỗ). Đều là Kotlin thuần, có unit test.
- `data/model/Transaction`: thêm `TransactionType.PAYMENT(isIncoming = false)`, field `service`, `details`.
- `AccountRepository.pay(fromId, payment, txId: String? = null)`: dùng lại `debit()` trong một `runTransaction`; trả `InsufficientFundsException` như các hàm khác; ghi trùng id báo lỗi riêng để UI hiện "đã thanh toán". Thêm hàm tìm giao dịch theo `paymentRef` cho màn hoá đơn. Fake trong `fakes/Fakes.kt` cập nhật theo.
- `TransferPolicy.sentToday`: tính cả `PAYMENT`.
- `ui/customer/utility/`: `BillPayFragment` + VM (tham số điện/nước), `TopUpFragment` + VM, `CatalogFragment` + VM (tham số loại danh mục). Dùng `Widget.Flux.*`, `TextAppearance.Flux.*`, `dimens.xml`, RecyclerView cho danh sách.
- `TransactionRowAdapter.labelFor` / icon và `ReceiptFragment.rowsFor`: thêm nhánh `PAYMENT`.

## 9. Kịch bản demo (khoảng 2 phút)
1. Trang chủ → Tiền điện → EVN TP.HCM, mã `PE01000123456` → Tra cứu → Thanh toán → OTP → biên lai.
2. Tra lại cùng mã → "đã thanh toán", mở biên lai. Thử mã đuôi `0000` → "Không tìm thấy hoá đơn".
3. Tiền nước (SAWACO) trả nhanh.
4. Nạp ĐT 100.000 ₫ cho số của mình (Vinaphone).
5. Vé máy bay SGN → HAN, 2 khách → OTP → biên lai có mã đặt chỗ.
6. Vé phim, Khách sạn, Mua sắm: mỗi cái chọn nhanh một mục và trả.
7. Tab Lịch sử: các dòng mới có icon theo dịch vụ. (Tuỳ chọn) một đơn ≥ 10.000.000 ₫ để thấy bước quét mặt.

## 10. Việc chủ dự án cần làm
1. Duyệt mockup + spec này.
2. Sau khi rules test pass: **publish lại `firestore.rules`** trên Console.
3. Chạy kịch bản §9 trên máy thật.

## 11. Kiểm thử
- Unit: `BillLookup` (cố định theo đầu vào, khoảng tiền, định dạng mã, mã `0000`, khoá hoá đơn), `Carriers` (mọi đầu số trong bảng + số lạ), `ServiceCatalog` (tổng tiền, giới hạn số lượng, ngày trả phòng), `BookingCode` (độ dài, bảng ký tự), `TransferPolicy.sentToday` gộp `PAYMENT`, và ba ViewModel (kycBlocked, lỗi hạn mức, đã trả, thành công, thiếu tiền, trùng hoá đơn).
- Rules: các ca ở §6, chạy cùng 40 ca cũ.
- `assembleDebug`, `lintDebug` (0 error), chạy trên máy thật theo §9.

## 12. Để builder tự quyết
- Dữ liệu cụ thể trong `ServiceCatalog` (tên, giờ, giá), danh sách tên chủ hộ/địa chỉ giả, cách băm mã thành số tiền.
- Cách lưu `details` trên Firestore.
- Dùng `MaterialDatePicker` hay cách khác cho ô ngày, miễn đúng khoảng ngày ở §4.
- Chia layout/adapter cho kiểu Danh mục (một adapter có nhiều kiểu dòng hay nhiều adapter nhỏ).
