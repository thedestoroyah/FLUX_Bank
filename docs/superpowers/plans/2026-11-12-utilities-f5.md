# Feature 5 — Tiện ích Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bảy ô tiện ích (Tiền điện, Tiền nước, Nạp ĐT, Vé máy bay, Vé phim, Khách sạn, Mua sắm) mở màn thật: tra hoá đơn / chọn mệnh giá / chọn mục trong danh mục, xác thực bằng `TransactionAuthSheet`, trừ tiền kèm giao dịch `PAYMENT` mới, ra biên lai và hiện trong Lịch sử.

**Architecture:** Giữ MVVM + service locator `FluxApp`. Dữ liệu giả lập (hoá đơn, nhà mạng, danh mục) và chữ lưu vào giao dịch nằm trong các object Kotlin thuần ở `domain/utility/`, có unit test. Một ViewModel cơ sở `ServicePaymentViewModel` gom phần chung (tài khoản nguồn, hạn mức, eKYC, mở sheet một lần, ghi tiền, lỗi); ba ViewModel con chỉ lo form riêng. Ghi tiền đi qua `AccountRepository.pay` dùng lại `debit()` của F4.

**Tech Stack:** Kotlin, XML + ViewBinding, Firestore (Spark), Material `MaterialDatePicker` (đã có trong Material Components), Node + Firestore emulator cho rules test. Không thêm dependency.

**Spec:** `docs/superpowers/specs/2026-11-12-utilities-f5-design.md` (đã duyệt 2026-11-12). Mockup: `docs/superpowers/specs/mockups/2026-11-12-f5-utilities.html` (đã duyệt). Design system: `DESIGN.md`.

## Global Constraints

- Làm trên nhánh `feature/f5-utilities` tách từ `main`.
- Kotlin + XML/ViewBinding, không Compose. MVVM, repository = interface + impl Firestore, bản giả trong `app/src/test/java/com/flux/bank/fakes/Fakes.kt`.
- Chỉ giao diện sáng; chỉ dùng token màu/khoảng cách (`dimens.xml`), `TextAppearance.Flux.*`, `Widget.Flux.*`; mọi chữ trên UI bằng tiếng Việt trong `strings.xml`; tiền VND kiểu `Long`, hiển thị qua `MoneyFormatter` (`48.250.000 ₫`). Theo các Named Rules trong `DESIGN.md` (emerald chỉ cho điểm chạm, nút chính đen, màu `cat-*` chỉ trong ô icon, số tiền dùng `Amount`/`AmountSmall`).
- Chữ **lưu vào Firestore** (`description`, `details`) được dựng trong `domain/utility/ServicePayments` bằng chuỗi tiếng Việt viết trong code (như `"… chuyen tien"` của F4), vì đó là dữ liệu chứ không phải chữ giao diện.
- KDoc/comment tiếng Việt, tên code tiếng Anh. Commit `feat:`/`fix:`/`test:`/`docs:`/`chore:`. TDD: test fail trước.
- `git add` theo đường dẫn cụ thể (`app docs rules-test firestore.rules README.md HANDOFF.md PROJECT_CONTEXT.md`), không `git add -A`, không commit `.idea/*`, `local.properties`.
- Hạn mức: tối thiểu 10.000 ₫, tối đa 100.000.000 ₫ mỗi lần, 200.000.000 ₫ mỗi ngày giờ Việt Nam **gộp `TRANSFER_OUT` và `PAYMENT`**. Kiểm bằng `TransferPolicy.checkAmount`.
- Chỉ khách eKYC `VERIFIED` được thanh toán; nguồn tiền là Checking/Saving (không Mortgage). Mọi thanh toán qua `TransactionAuthSheet` (OTP; ≥ 10.000.000 ₫ thêm quét mặt — sheet tự lo).
- Lệnh kiểm tra (từ `FLUX_Bank/`): `./gradlew testDebugUnitTest`. Rules: `cd rules-test && npx firebase emulators:exec --only firestore --project demo-flux "node --test rules.test.mjs"`.

## Review Focus

1. Sửa mã khách hàng sau khi đã tra: hoá đơn và trạng thái "đã trả" cũ phải biến mất, không trả nhầm hoá đơn cũ → test `editingCodeClearsBill` ở Task 6.
2. Hoá đơn vừa được trả ở máy khác giữa lúc tra và lúc ghi: không trừ tiền lần hai, màn chuyển sang "đã thanh toán" → test `alreadyPaidOnWriteShowsPaid` ở Task 6 và ca rules `second payment of same bill id denied` ở Task 1.
3. Bấm "Thanh toán" hai lần nhanh: chỉ mở một sheet → test `doubleConfirmAsksAuthOnce` ở Task 6.
4. Đổi bộ lọc (chặng, rạp, thành phố, nhóm) khi đã chọn một mục: mục cũ (không thuộc bộ lọc mới) bị bỏ chọn, tổng về 0 → test `changingFilterClearsItem` ở Task 8.
5. Trả tiền lúc nửa đêm ngày 1 hoặc máy để múi giờ khác: kỳ hoá đơn tính theo giờ Việt Nam → test `periodUsesVietnamTime` ở Task 3.

---

### Task 1: Security Rules cho `PAYMENT`

**Files:**
- Modify: `rules-test/rules.test.mjs` (thêm khối test sau các test chuyển tiền)
- Modify: `firestore.rules:87` (`ownerMoneyMoveAllowed`), `firestore.rules:130-131` (`txCreateAllowed`)

**Interfaces:**
- Produces: rules chấp nhận `transactions` có `type == 'PAYMENT'` do chủ tài khoản ghi, cùng batch trừ tiền, `amount` 10.000–100.000.000.

- [ ] **Step 1: Tạo nhánh**

```bash
git checkout main && git checkout -b feature/f5-utilities
```

- [ ] **Step 2: Viết các ca test fail** (dùng helper `move()` và dữ liệu mẫu có sẵn: `acc1` số dư 100.000, `rich` 500.000.000, `other` của người khác)

```js
test('valid payment allowed', () => assertSucceeds(move(cust(), 'acc1', 'p1', 'PAYMENT', 60000, 40000, { service: 'TIEN_DIEN' })));
test('payment overdraft denied', () => assertFails(move(cust(), 'acc1', 'p1', 'PAYMENT', 100001, -1)));
test('payment over 100M denied', () => assertFails(move(cust(), 'rich', 'p1', 'PAYMENT', 100000001, 399999999)));
test('payment with wrong arithmetic denied', () => assertFails(move(cust(), 'acc1', 'p1', 'PAYMENT', 60000, 50000)));
test('payment owned by someone else denied', () => assertFails(move(cust(), 'acc1', 'p1', 'PAYMENT', 60000, 40000, { ownerUid: 'bob' })));
test('payment from another customers account denied', () => assertFails(move(cust(), 'other', 'p1', 'PAYMENT', 60000, 40000)));
test('second payment of same bill id denied', async () => {
  await assertSucceeds(move(cust(), 'acc1', 'bill_cust_EVN_HCM_PE01000123456_202610', 'PAYMENT', 60000, 40000));
  await assertFails(move(cust(), 'acc1', 'bill_cust_EVN_HCM_PE01000123456_202610', 'PAYMENT', 20000, 20000));
});
```

- [ ] **Step 3: Chạy rules test** — Expected: `valid payment allowed` và `second payment of same bill id denied` FAIL (rules chưa nhận `PAYMENT` nên lần ghi đầu bị từ chối); 5 ca "denied" mới và 40 ca cũ PASS.

- [ ] **Step 4: Sửa `firestore.rules`**: thêm `'PAYMENT'` vào danh sách `['WITHDRAW', 'TRANSFER_OUT']` ở `ownerMoneyMoveAllowed` và vào `['DEPOSIT', 'WITHDRAW', 'TRANSFER_OUT']` ở `txCreateAllowed` (giữ điều kiện `d.amount <= 100000000` cho `PAYMENT`; chỉ `WITHDRAW` được miễn). Cập nhật comment phía trên `txCreateAllowed`.

- [ ] **Step 5: Chạy rules test** — Expected: 47/47 PASS.

- [ ] **Step 6: Commit**

```bash
git add rules-test/rules.test.mjs firestore.rules
git commit -m "feat: rules accept PAYMENT transactions"
```

---

### Task 2: Model, hạn mức gộp, `AccountRepository.pay`, tìm theo `paymentRef`

**Files:**
- Modify: `app/src/main/java/com/flux/bank/data/model/Transaction.kt`
- Create: `app/src/main/java/com/flux/bank/data/model/ServicePayment.kt`
- Modify: `app/src/main/java/com/flux/bank/domain/TransferPolicy.kt` (`sentToday`)
- Modify: `app/src/main/java/com/flux/bank/data/repository/AccountRepository.kt`
- Modify: `app/src/main/java/com/flux/bank/data/repository/TransactionRepository.kt`
- Modify: `app/src/test/java/com/flux/bank/fakes/Fakes.kt`
- Test: `app/src/test/java/com/flux/bank/domain/TransferPolicyTest.kt`, `app/src/test/java/com/flux/bank/data/ModelDefaultsTest.kt`

**Interfaces:**
- Produces:
  - `TransactionType.PAYMENT(isIncoming = false)`
  - `Transaction` thêm `val service: String? = null` (tên enum `Utility`), `val details: List<DetailRow> = emptyList()`
  - `data class DetailRow(val label: String = "", val value: String = "")` (trong `ServicePayment.kt`; constructor không tham số để Firestore `toObject` đọc được)
  - `data class ServicePayment(val service: String, val amount: Long, val merchant: String, val reference: String, val description: String, val paymentRef: String?, val details: List<DetailRow>)` — `merchant` → `counterpartyName`, `reference` → `counterpartyAccount`
  - `class AlreadyPaidException : Exception()` (cạnh `InsufficientFundsException`)
  - `AccountRepository.pay(fromId: String, payment: ServicePayment, txId: String? = null): Result<Transaction>`
  - `TransactionRepository.findByPaymentRef(ownerUid: String, paymentRef: String): Result<Transaction?>`
  - Fakes: `FakeAccountRepository.payments: MutableList<Transaction>`, `pay` dùng `move(..., PAYMENT)` và tôn trọng `failNextMove`; `FakeTransactionRepository.findByPaymentRef` lọc `items`.

- [ ] **Step 1: Viết test fail** trong `TransferPolicyTest`:

```kotlin
@Test
fun paymentsCountTowardDailyLimit() {
    val now = at("2026-11-12T10:00+07:00")
    val history = listOf(
        Transaction(type = TransactionType.TRANSFER_OUT, amount = 50_000_000, createdAt = now),
        Transaction(type = TransactionType.PAYMENT, amount = 30_000_000, createdAt = now),
        Transaction(type = TransactionType.WITHDRAW, amount = 9_000_000, createdAt = now)
    )
    assertEquals(80_000_000, TransferPolicy.sentToday(history, now))
}
```

trong `ModelDefaultsTest`: `Transaction()` có `service == null`, `details` rỗng; `DetailRow()` có `label == ""`; và trong `TransactionLabelTest`:

```kotlin
@Test
fun paymentShowsDescription() = assertEquals(
    TxLabel(R.string.tx_payment_title, "Tiền điện kỳ 10/2026"),
    label(Transaction(type = TransactionType.PAYMENT, description = "Tiền điện kỳ 10/2026"))
)
```

- [ ] **Step 2: Chạy** `./gradlew testDebugUnitTest --tests '*TransferPolicyTest*' --tests '*ModelDefaultsTest*' --tests '*TransactionLabelTest*'` — Expected: FAIL (không biên dịch: thiếu `PAYMENT`).

- [ ] **Step 3: Thêm model** như Interfaces; `sentToday` lọc `type == TRANSFER_OUT || type == PAYMENT`, sửa KDoc. `TransactionRowAdapter.labelFor(PAYMENT)` = `TxLabel(R.string.tx_payment_title, tx.description)` với chuỗi mới `tx_payment_title` = `"%1$s"`. `ReceiptFragment.rowsFor(PAYMENT)` tạm là `listOf(getString(R.string.receipt_account) to accountLabel)` để biên dịch được; Task 9 làm đúng biên lai.

- [ ] **Step 4: Implement `pay`** trong `FirestoreAccountRepository`: `txRef = txId?.let(transactions::document) ?: transactions.document()`; trong `runTransaction` gọi `debit(t, fromId, txRef, amount)` dựng `Transaction(type = PAYMENT, service, counterpartyName = merchant, counterpartyAccount = reference, description, paymentRef, details, balanceAfter = balance, …)`. Bọc bằng `unwrapInsufficientFunds`. Khi thất bại và `txId != null`: đọc `transactions.document(txId)`; nếu document tồn tại thì trả `Result.failure(AlreadyPaidException())` (ghi trùng id bị rules chặn như một lần `update`; không đọc trong transaction vì rules từ chối `get` document chưa tồn tại), ngược lại trả lỗi gốc.

- [ ] **Step 5: Implement `findByPaymentRef`**: `whereEqualTo("ownerUid", …).whereEqualTo("paymentRef", …).limit(1)` — chỉ điều kiện bằng nên không cần index ghép. Cập nhật Fakes theo Interfaces.

- [ ] **Step 6: Chạy** `./gradlew testDebugUnitTest` — Expected: PASS toàn bộ (216 cũ + mới).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/flux/bank/data app/src/main/java/com/flux/bank/domain/TransferPolicy.kt app/src/main/java/com/flux/bank/ui/customer/account app/src/main/res/values/strings.xml app/src/test
git commit -m "feat: PAYMENT transaction type and AccountRepository.pay"
```

---

### Task 3: Hoá đơn giả lập `BillLookup`

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/utility/Bills.kt`
- Test: `app/src/test/java/com/flux/bank/domain/utility/BillLookupTest.kt`

**Interfaces:**
- Produces:
  - `enum class BillKind { ELECTRICITY, WATER }`
  - `enum class BillProvider(val displayName: String, val kind: BillKind)`: `EVN_HCM("EVN TP.HCM", ELECTRICITY)`, `EVN_HN("EVN Hà Nội", ELECTRICITY)`, `EVN_SPC("EVN miền Nam", ELECTRICITY)`, `SAWACO("SAWACO", WATER)`, `HAWACO("Nước sạch Hà Nội", WATER)`; `companion fun forKind(kind): List<BillProvider>`
  - `data class Bill(val provider: BillProvider, val customerCode: String, val period: YearMonth, val holderName: String, val address: String, val usage: Int, val amount: Long, val dueDate: LocalDate)` với `val key: String` = `"${provider.name}:$customerCode:${period yyyyMM}"`
  - `object BillLookup`: `fun normalizeCode(kind: BillKind, input: String): String?` · `fun periodFor(now: Long): YearMonth` · `fun lookup(provider: BillProvider, customerCode: String, period: YearMonth): Bill?` · `fun txIdFor(uid: String, bill: Bill): String` = `"bill_${uid}_${provider.name}_${code}_${yyyyMM}"`

- [ ] **Step 1: Viết test fail**

```kotlin
@Test fun electricityCodeIsPePlus11Digits() {
    assertEquals("PE01000123456", BillLookup.normalizeCode(BillKind.ELECTRICITY, " pe01000123456 "))
    assertNull(BillLookup.normalizeCode(BillKind.ELECTRICITY, "PE0100012345"))   // 10 số
    assertNull(BillLookup.normalizeCode(BillKind.ELECTRICITY, "01000123456"))     // thiếu PE
}
@Test fun waterCodeIs8To12Digits() {
    assertEquals("2210456789", BillLookup.normalizeCode(BillKind.WATER, "2210 456 789"))
    assertNull(BillLookup.normalizeCode(BillKind.WATER, "1234567"))
    assertNull(BillLookup.normalizeCode(BillKind.WATER, "1234567890123"))
}
@Test fun lookupIsStable() = assertEquals(
    BillLookup.lookup(BillProvider.EVN_HCM, "PE01000123456", YearMonth.of(2026, 10)),
    BillLookup.lookup(BillProvider.EVN_HCM, "PE01000123456", YearMonth.of(2026, 10)))
@Test fun amountsStayInRangeAndRoundTo100() { /* 200 mã ngẫu nhiên có seed: điện 150_000..2_500_000, nước 50_000..600_000, amount % 100 == 0 */ }
@Test fun codeEndingIn0000IsNotFound() = assertNull(BillLookup.lookup(BillProvider.SAWACO, "22100000", YearMonth.of(2026, 10)))
@Test fun periodUsesVietnamTime() {
    // 2026-10-31T18:00Z là 01:00 ngày 1/11 giờ VN → kỳ = 10/2026
    assertEquals(YearMonth.of(2026, 10), BillLookup.periodFor(Instant.parse("2026-10-31T18:00:00Z").toEpochMilli()))
}
@Test fun dueDateIs25thOfNextMonth() = assertEquals(LocalDate.of(2026, 11, 25),
    BillLookup.lookup(BillProvider.EVN_HN, "PE01000123456", YearMonth.of(2026, 10))!!.dueDate)
@Test fun keyAndTxIdEncodeProviderCodeAndPeriod() { /* key == "EVN_HCM:PE01000123456:202610"; txIdFor("c1", bill) == "bill_c1_EVN_HCM_PE01000123456_202610" */ }
```

- [ ] **Step 2: Chạy** `./gradlew testDebugUnitTest --tests '*BillLookupTest*'` — Expected: FAIL (chưa có lớp).

- [ ] **Step 3: Implement** trong `Bills.kt`. `periodFor` = tháng trước của ngày hiện tại theo `TransferPolicy.VN_ZONE`. `lookup` lấy số giả bằng `java.util.Random(seed)` với seed = `"${provider.name}:$code:$period".hashCode().toLong()`; chủ hộ và địa chỉ chọn từ hai danh sách cố định khoảng 6 mục (tên in hoa không dấu, ví dụ `"NGUYEN VAN AN"`, `"12 Lê Lợi, Q.1"`); `usage` (kWh hoặc m³) suy ra từ amount theo một đơn giá cố định; `dueDate = period.plusMonths(1).atDay(25)`.

- [ ] **Step 4: Chạy lại** — Expected: PASS.

- [ ] **Step 5: Commit** `git add app/src/main/java/com/flux/bank/domain/utility app/src/test/java/com/flux/bank/domain/utility && git commit -m "feat: simulated bill lookup"`

---

### Task 4: Nhà mạng và mã đặt chỗ

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/utility/Carriers.kt`, `app/src/main/java/com/flux/bank/domain/utility/BookingCode.kt`
- Test: `app/src/test/java/com/flux/bank/domain/utility/CarriersTest.kt`, `app/src/test/java/com/flux/bank/domain/utility/BookingCodeTest.kt`

**Interfaces:**
- Produces:
  - `enum class Carrier(val displayName: String) { VIETTEL("Viettel"), VINAPHONE("Vinaphone"), MOBIFONE("Mobifone"), VIETNAMOBILE("Vietnamobile") }`
  - `object Carriers { val DENOMINATIONS: List<Long> = listOf(10_000, 20_000, 50_000, 100_000, 200_000, 500_000); fun detect(phone: String): Carrier? }` — `detect` chuẩn hoá bằng `Validators.normalizePhone`, trả null nếu `!Validators.isValidVnPhone`
  - `object BookingCode { const val LENGTH = 6; const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; fun generate(random: java.util.Random = java.security.SecureRandom()): String }`

- [ ] **Step 1: Viết test fail**: `detect` đúng cho **mọi** đầu số trong bảng spec §4 (Viettel `032–039, 086, 096–098`; Vinaphone `081–085, 088, 091, 094`; Mobifone `070, 076–079, 089, 090, 093`; Vietnamobile `052, 056, 058, 092`), dạng có dấu cách (`"0912 345 678"` → VINAPHONE), trả null cho `"0111234567"`, `"091234567"` (9 số) và chuỗi rỗng. `BookingCode.generate(Random(1))` dài 6 và mọi ký tự thuộc `ALPHABET`; 1.000 mã không chứa `0 O 1 I`.

- [ ] **Step 2: Chạy** — Expected: FAIL.
- [ ] **Step 3: Implement** (bảng đầu số là `Map<String, Carrier>` theo 3 số đầu).
- [ ] **Step 4: Chạy** `./gradlew testDebugUnitTest --tests '*CarriersTest*' --tests '*BookingCodeTest*'` — Expected: PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: carrier detection and booking codes"` (add 4 file).

---

### Task 5: Danh mục và dựng `ServicePayment`

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/utility/ServiceCatalog.kt`, `app/src/main/java/com/flux/bank/domain/utility/ServicePayments.kt`
- Test: `app/src/test/java/com/flux/bank/domain/utility/ServiceCatalogTest.kt`, `app/src/test/java/com/flux/bank/domain/utility/ServicePaymentsTest.kt`

**Interfaces:**
- Consumes: `Bill`, `Carrier`, `ServicePayment`, `DetailRow` (Task 2–4).
- Produces:
  - `enum class CatalogKind(val service: String, val quantityRange: IntRange, val needsDate: Boolean)`: `FLIGHT("VE_MAY_BAY", 1..9, true)`, `MOVIE("VE_PHIM", 1..8, false)`, `HOTEL("KHACH_SAN", 1..14, true)`, `SHOP("MUA_SAM", 1..10, false)`
  - `data class CatalogFilter(val id: String, val label: String)`
  - `data class CatalogItem(val id: String, val filterId: String, val title: String, val subtitle: String, val merchant: String, val unitPrice: Long, val options: List<String> = emptyList(), val stars: Int = 0)` — `options` là giờ suất chiếu `"HH:mm"` (chỉ MOVIE)
  - `object ServiceCatalog`: `fun filters(kind): List<CatalogFilter>` · `fun items(kind, filterId: String): List<CatalogItem>` (SHOP: filter `"all"` trả mọi sản phẩm) · `fun total(item: CatalogItem, quantity: Int): Long` · `fun dateRange(kind, now: Long): ClosedRange<LocalDate>?` (FLIGHT: mai…+60 ngày; HOTEL: hôm nay…+60; khác: null) · `fun isShowtimePast(time: String, now: Long): Boolean` (giờ VN)
  - `object ServicePayments`: `fun bill(bill: Bill): ServicePayment` · `fun topUp(phone: String, carrier: Carrier, amount: Long): ServicePayment` · `fun booking(kind: CatalogKind, item: CatalogItem, quantity: Int, option: String?, date: LocalDate?, code: String): ServicePayment`

- [ ] **Step 1: Viết test fail** cho `ServiceCatalog`: mỗi `kind` có ≥ 2 filter và mỗi filter ≥ 2 item (SHOP ≥ 8 sản phẩm, 4 filter `all/electronics/home/fashion`); id item là duy nhất trong cả danh mục; mọi MOVIE item có ≥ 2 suất; `total(item, 3) == item.unitPrice * 3`; `dateRange(FLIGHT, now)` = `(ngày mai, +60)` và `dateRange(HOTEL, now)` bắt đầu hôm nay (giờ VN); `isShowtimePast("09:00", 10:00 giờ VN) == true`, `"21:50"` → false.

- [ ] **Step 2: Viết test fail** cho `ServicePayments` với giá trị cố định:

```kotlin
@Test fun billPayment() {
    val bill = Bill(BillProvider.EVN_HCM, "PE01000123456", YearMonth.of(2026, 10), "NGUYEN VAN AN", "12 Lê Lợi, Q.1", 412, 1_248_600, LocalDate.of(2026, 11, 25))
    val p = ServicePayments.bill(bill)
    assertEquals("TIEN_DIEN", p.service); assertEquals(1_248_600L, p.amount)
    assertEquals("EVN TP.HCM", p.merchant); assertEquals("PE01000123456", p.reference)
    assertEquals("Tiền điện kỳ 10/2026", p.description); assertEquals(bill.key, p.paymentRef)
    assertEquals(listOf(DetailRow("Nhà cung cấp", "EVN TP.HCM"), DetailRow("Mã khách hàng", "PE01000123456"),
        DetailRow("Chủ hộ", "NGUYEN VAN AN"), DetailRow("Kỳ", "10/2026"), DetailRow("Tiêu thụ", "412 kWh")), p.details)
}
@Test fun waterUsesCubicMetres() { /* service "TIEN_NUOC", description "Tiền nước kỳ 10/2026", Tiêu thụ "18 m³" */ }
@Test fun topUpPayment() { /* topUp("0912345678", VINAPHONE, 100_000): service "NAP_DT", merchant "Vinaphone", reference "0912345678", description "Nạp ĐT 0912 345 678", paymentRef null, details [Nhà mạng, Số điện thoại "0912 345 678"] */ }
@Test fun flightBooking() { /* service "VE_MAY_BAY", amount = unitPrice*2, paymentRef = code, description "Vé máy bay <item.title>", details gồm "Chuyến bay", "Ngày bay" → "20/11/2026 · 2 khách" */ }
@Test fun hotelBookingShowsCheckout() { /* details "Nhận phòng" 20/11/2026, "Trả phòng" 22/11/2026, "Số đêm" "2" */ }
@Test fun movieAndShopBookings() { /* MOVIE: "Suất chiếu" = option, "Số vé"; SHOP: merchant "FLUX Shop", "Số lượng" */ }
```

- [ ] **Step 3: Chạy** `./gradlew testDebugUnitTest --tests '*ServiceCatalogTest*' --tests '*ServicePaymentsTest*'` — Expected: FAIL.
- [ ] **Step 4: Implement.** Dữ liệu cụ thể (tên phim/khách sạn/sản phẩm tự đặt, giá, giờ) builder tự chọn theo bảng spec §4; mockup có sẵn số liệu mẫu (VN 213 06:00→08:10 1.890.000 ₫, "Mùa Gió Chướng" 95.000 ₫, "Sông Hàn Riverside" 1.350.000 ₫/đêm, "Tai nghe không dây" 1.490.000 ₫). SĐT hiển thị theo nhóm `0912 345 678`. Ngày `dd/MM/yyyy`.
- [ ] **Step 5: Chạy lại** — Expected: PASS.
- [ ] **Step 6: Commit** `git commit -m "feat: service catalog and payment builders"`

---

### Task 6: `ServicePaymentViewModel` (phần chung) + `BillPayViewModel`

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/utility/ServicePaymentViewModel.kt`, `app/src/main/java/com/flux/bank/ui/customer/utility/BillPayViewModel.kt`
- Test: `app/src/test/java/com/flux/bank/ui/customer/utility/BillPayViewModelTest.kt`

**Interfaces:**
- Consumes: `AccountRepository.pay`, `TransactionRepository.findByPaymentRef/listRecent`, `AlreadyPaidException`, `InsufficientFundsException`, `TransferPolicy`, `BillLookup`, `ServicePayments.bill`.
- Produces:
  - `data class CheckoutState(val sources: List<BankAccount> = emptyList(), val sourceId: String? = null, val remainingToday: Long = TransferPolicy.DAILY_LIMIT, val amountError: TransferError? = null, val kycBlocked: Boolean = false, val submitting: Boolean = false, @StringRes val errorRes: Int? = null)` với `val source: BankAccount?`
  - `abstract class ServicePaymentViewModel(accounts, transactions, users, auth, now: () -> Long)`: `val checkout: LiveData<CheckoutState>` · `val askAuth: LiveData<Event<ServicePayment>>` · `val done: LiveData<Event<Transaction>>` · `fun load()` · `fun selectSource(accountId: String)` · `fun confirm()` · `fun onAuthResult(verified: Boolean)` · `protected abstract fun draft(): Draft?` với `data class Draft(val payment: ServicePayment, val txId: String?)` (null = form chưa hợp lệ; lớp con tự đặt lỗi form) · `protected open fun onLoaded(user: User?)` · `protected open fun onAlreadyPaid()` · `protected val uid: String?`
  - `data class BillState(val provider: BillProvider, val code: String = "", val codeError: Boolean = false, val looking: Boolean = false, val bill: Bill? = null, val notFound: Boolean = false, val paidTx: Transaction? = null)`
  - `class BillPayViewModel(kind: BillKind, …deps)`: `val bill: LiveData<BillState>` · `fun selectProvider(p: BillProvider)` · `fun setCode(input: String)` · `fun lookup()`

Hành vi phần chung (giống `TransferViewModel` của F4, đọc nó làm mẫu): `load` đọc user (eKYC), tài khoản không phải Mortgage, `remainingToday` từ `listRecent`; `confirm` chặn khi chưa VERIFIED / đang ghi / đã có yêu cầu chờ sheet; gọi `TransferPolicy.checkAmount(payment.amount, source.balance, remainingToday)`, lỗi thì đặt `amountError`; hợp lệ thì giữ `Draft` đang chờ và phát `askAuth`. `onAuthResult(true)` gọi `accounts.pay(sourceId, draft.payment, draft.txId)`; thành công → `done`; `InsufficientFundsException` → tải lại tài khoản + `R.string.err_insufficient_funds`; `AlreadyPaidException` → `onAlreadyPaid()` + `R.string.err_already_paid` ("Hoá đơn này đã được thanh toán."); lỗi khác → `R.string.err_generic`.

- [ ] **Step 1: Viết test fail** (khung giống `TransferViewModelTest`: `InstantTaskExecutorRule`, `MainDispatcherRule`, fake repo, `now` cố định `2026-11-12T09:30+07:00`, tài khoản `chk` 8.250.000 ₫, `sav`, `mor`):
  - `lookupShowsBill` — EVN_HCM + `"PE01000123456"` → `bill != null`, `paidTx == null`
  - `invalidCodeSetsErrorWithoutLookup` — `"PE123"` → `codeError`, `bill == null`
  - `code0000IsNotFound`
  - `editingCodeClearsBill` — tra xong rồi `setCode("PE01000123457")` → `bill == null`, `paidTx == null`, `notFound == false`
  - `alreadyPaidBillShowsPaidTx` — `FakeTransactionRepository` có giao dịch `paymentRef == bill.key` của `c1` → `paidTx` là giao dịch đó; `confirm()` không phát `askAuth`
  - `payWritesPaymentWithBillTxId` — confirm → `askAuth` có `payment.amount == bill.amount`; `onAuthResult(true)` → `accounts.payments.single()` có `type == PAYMENT`, `id == BillLookup.txIdFor("c1", bill)`; `done` phát
  - `doubleConfirmAsksAuthOnce` — gọi `confirm()` hai lần → `askAuth` chỉ phát một lần
  - `alreadyPaidOnWriteShowsPaid` — tra hoá đơn (chưa trả), `confirm()`; rồi thêm vào `FakeTransactionRepository.items` giao dịch `paymentRef == bill.key` (máy khác vừa trả) và đặt `accounts.failNextMove = AlreadyPaidException()` → sau `onAuthResult(true)`: `errorRes == R.string.err_already_paid`, `bill.paidTx` là giao dịch vừa thêm (`onAlreadyPaid()` tra lại bằng `findByPaymentRef`), `done` không phát
  - `insufficientFundsShowsError` — số dư nhỏ hơn hoá đơn → `amountError == INSUFFICIENT_FUNDS`, không `askAuth`
  - `unverifiedIsBlocked` — kyc `PENDING` → `kycBlocked`, `confirm()` không phát
  - `deniedAuthDoesNotPay` — `onAuthResult(false)` → `payments` rỗng, có thể `confirm()` lại

- [ ] **Step 2: Chạy** `./gradlew testDebugUnitTest --tests '*BillPayViewModelTest*'` — Expected: FAIL.
- [ ] **Step 3: Implement** hai lớp như Interfaces; thêm chuỗi `err_already_paid` vào `strings.xml`. `lookup()` gọi `findByPaymentRef(uid, bill.key)`; lỗi mạng ở bước này → `errorRes = err_generic`, vẫn hiện hoá đơn nhưng `draft()` trả null cho tới khi tra lại thành công (không trả khi chưa biết đã trả hay chưa).
- [ ] **Step 4: Chạy lại** — Expected: PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: shared service payment flow and bill payment view model"`

---

### Task 7: `TopUpViewModel`

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/utility/TopUpViewModel.kt`
- Test: `app/src/test/java/com/flux/bank/ui/customer/utility/TopUpViewModelTest.kt`

**Interfaces:**
- Consumes: `ServicePaymentViewModel`, `Carriers`, `ServicePayments.topUp`.
- Produces: `data class TopUpState(val phone: String = "", val carrier: Carrier? = null, val phoneError: Boolean = false, val amount: Long? = null)`; `class TopUpViewModel(…deps)`: `val topUp: LiveData<TopUpState>` · `fun setPhone(input: String)` · `fun selectAmount(amount: Long)`.

- [ ] **Step 1: Viết test fail**: `prefillsOwnPhone` (sau `load()`, `phone` = SĐT của user, `carrier` nhận đúng); `unknownPrefixBlocksConfirm` (`"0111234567"` → `phoneError`, `confirm()` không phát); `noAmountBlocksConfirm`; `payTopUp` (`"0912345678"`, 100.000 → `payments.single()` có `service == "NAP_DT"`, `amount == 100_000`, `txId` tự sinh); `amountOutsideDenominationsIgnored` (`selectAmount(15_000)` → `amount` giữ nguyên).
- [ ] **Step 2: Chạy** — Expected: FAIL.
- [ ] **Step 3: Implement** (`onLoaded` điền SĐT nếu người dùng chưa gõ; `phoneError` chỉ bật khi đủ 10 số mà không nhận được nhà mạng hoặc khi bấm confirm).
- [ ] **Step 4: Chạy** `./gradlew testDebugUnitTest --tests '*TopUpViewModelTest*'` — Expected: PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: phone top-up view model"`

---

### Task 8: `CatalogViewModel`

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/utility/CatalogViewModel.kt`
- Test: `app/src/test/java/com/flux/bank/ui/customer/utility/CatalogViewModelTest.kt`

**Interfaces:**
- Consumes: `ServicePaymentViewModel`, `ServiceCatalog`, `ServicePayments.booking`, `BookingCode`.
- Produces: `data class CatalogState(val kind: CatalogKind, val filterId: String, val items: List<CatalogItem>, val date: LocalDate? = null, val itemId: String? = null, val option: String? = null, val quantity: Int = 1, val total: Long = 0)` với `val item: CatalogItem?`; `class CatalogViewModel(kind: CatalogKind, …deps, codeGen: () -> String = BookingCode::generate)`: `val catalog: LiveData<CatalogState>` · `fun selectFilter(id: String)` · `fun setDate(date: LocalDate)` · `fun selectItem(id: String)` · `fun selectOption(option: String)` · `fun increment()` · `fun decrement()`.

- [ ] **Step 1: Viết test fail**:
  - `startsOnFirstFilterWithDefaultDate` — FLIGHT: `filterId` = filter đầu, `date` = ngày mai (giờ VN); MOVIE: `date == null`
  - `totalFollowsItemAndQuantity` — chọn item, `increment()` ×2 → `quantity == 3`, `total == unitPrice * 3`
  - `quantityClampedToRange` — FLIGHT `increment()` 20 lần → 9; `decrement()` 20 lần → 1
  - `changingFilterClearsItem` — chọn item rồi `selectFilter(filter khác)` → `itemId == null`, `option == null`, `total == 0`
  - `dateOutsideRangeIgnored` — `setDate(hôm qua)` → `date` không đổi
  - `movieNeedsShowtime` — chọn phim chưa chọn suất → `confirm()` không phát; `selectOption` một suất đã qua (theo `now`) bị bỏ qua
  - `totalOverLimitBlocksConfirm` — lịch sử có `TRANSFER_OUT` 199.500.000 ₫ hôm nay (còn 500.000 ₫); FLIGHT chọn chuyến đầu, 1 khách (giá > 500.000) → `confirm()` đặt `amountError == TransferError.DAILY_LIMIT`, không `askAuth`
  - `payBookingUsesGeneratedCode` — `codeGen = { "K7Q2MX" }` → `payments.single().paymentRef == "K7Q2MX"`, `service == "VE_MAY_BAY"`
- [ ] **Step 2: Chạy** — Expected: FAIL.
- [ ] **Step 3: Implement.** Mã đặt chỗ sinh một lần lúc `confirm()` hợp lệ và giữ cho tới khi ghi xong/huỷ, để biên lai và giao dịch cùng mã.
- [ ] **Step 4: Chạy** `./gradlew testDebugUnitTest --tests '*CatalogViewModelTest*'` — Expected: PASS.
- [ ] **Step 5: Commit** `git commit -m "feat: catalog booking view model"`

---

### Task 9: Lịch sử và biên lai cho `PAYMENT`

**Files:**
- Modify: `app/src/main/java/com/flux/bank/ui/customer/Utility.kt` (thêm `companion fun fromService(name: String?): Utility?`)
- Modify: `app/src/main/java/com/flux/bank/ui/customer/account/TransactionRowAdapter.kt` (`labelFor`, `ItemTransactionBinding.bind`)
- Modify: `app/src/main/java/com/flux/bank/ui/customer/account/ReceiptFragment.kt` (`rowsFor`, tiêu đề thành công, `newInstance`/`transactionFrom`, khối mã)
- Modify: `app/src/main/res/layout/fragment_receipt.xml` (thêm khối mã: `tvCodeLabel`, `tvCode` trong `Widget.Flux.Card`, ẩn mặc định)
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/flux/bank/ui/customer/TransactionLabelTest.kt`

**Interfaces:**
- Consumes: `Transaction.service`, `Transaction.details`, `CatalogKind.service`.
- Produces: `Utility.fromService(name: String?): Utility?`; dòng `PAYMENT` (icon trong `ItemTransactionBinding.bind`) dùng `Utility.fromService(tx.service)?.icon/tint/tileBg`, không có thì `ic_bag` + `cat_rose`. (`labelFor(PAYMENT)` đã làm ở Task 2.)

- [ ] **Step 1: Viết test fail** trong `TransactionLabelTest`: `fromServiceMapsUtilityNames` (`Utility.fromService("VE_PHIM") == Utility.VE_PHIM`, `fromService("XYZ") == null`, `fromService(null) == null`) và `catalogServicesAreUtilities` (mọi `CatalogKind.service` đều cho `fromService(...) != null`).
- [ ] **Step 2: Chạy** `./gradlew testDebugUnitTest --tests '*TransactionLabelTest*'` — Expected: FAIL.
- [ ] **Step 3: Implement.** Biên lai `PAYMENT` (mockup màn 9): tiêu đề "Thanh toán thành công"; khối mã hiện khi `service` là một `CatalogKind.service`, nhãn "Mã đặt chỗ", giá trị `paymentRef`; các dòng = "Dịch vụ" (`Utility.label`) + `details` + "Trả từ" (accountLabel) + thời gian/mã giao dịch/số dư sau như cũ. `newInstance`/`transactionFrom` mang thêm `service` và `details` (hai `ArrayList<String>` nhãn/giá trị). Nút thứ hai giữ "Xem lịch sử".
- [ ] **Step 4: Chạy** `./gradlew testDebugUnitTest` — Expected: PASS toàn bộ.
- [ ] **Step 5: Commit** `git commit -m "feat: history rows and receipts for service payments"`

---

### Task 10: Màn Hoá đơn + nối trang chủ

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/utility/BillPayFragment.kt`, `app/src/main/res/layout/fragment_bill_pay.xml`, `app/src/main/java/com/flux/bank/ui/customer/utility/PaymentUi.kt`
- Modify: `app/src/main/java/com/flux/bank/ui/customer/home/HomeFragment.kt:160-171`, `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `BillPayViewModel` (Task 6), `ReceiptFragment.newInstance` (Task 9), `TransactionAuthSheet.show`/`KEY_VERIFIED`, `ScreenHost.openScreen`.
- Produces (trong `PaymentUi.kt`, dùng lại ở Task 11–12): `fun ServicePayment.authSummary(context: Context, sourceLabel: String): AuthSummary` (title "Xác nhận thanh toán", rows = "Dịch vụ" + `details` + "Trả từ"); `fun ChipGroup.bindSources(state: CheckoutState, onSelect: (String) -> Unit)` (chip `Widget.Flux.FilterChip`, nhãn "Thanh toán · 8.250.000 ₫" như mockup); `fun Fragment.observeCheckout(vm: ServicePaymentViewModel, requestKey: String, payButton: View)` nối `askAuth` → sheet, kết quả sheet → `onAuthResult`, `done` → mở `ReceiptFragment(viewOnly = false)`, `errorRes` → Snackbar, `submitting`/`kycBlocked` → khoá nút.

- [ ] **Step 1: Layout `fragment_bill_pay.xml`** theo mockup màn 1–2: header có nút back; ChipGroup nhà cung cấp; `Widget.Flux.TextField` mã khách hàng + nút "Tra cứu" (`Widget.Flux.Button.Secondary`); card hoá đơn (ẩn tới khi tra): ô icon `Widget.Flux.IconTile` theo màu tiện ích, chủ hộ, địa chỉ, dòng `item_key_value` Kỳ/Tiêu thụ/Hạn thanh toán, số tiền `AmountSmall`; khối "đã thanh toán" nền `flux_emerald_light`; ChipGroup nguồn tiền; nút đáy `Widget.Flux.Button.Primary` "Thanh toán %1$s" hoặc `Secondary` "Xem biên lai" khi `paidTx != null`. Banner eKYC dùng `showKycPill`/mẫu của màn chuyển tiền khi `kycBlocked`.
- [ ] **Step 2: `BillPayFragment.newInstance(kind: BillKind)`** nối VM và `PaymentUi`; lỗi mã hiện dưới ô ("Mã khách hàng không đúng định dạng"); `notFound` → "Không tìm thấy hoá đơn cho mã này"; "Xem biên lai" mở `ReceiptFragment(paidTx, label, viewOnly = true)`.
- [ ] **Step 3: `HomeFragment`**: `TIEN_DIEN` → `BillPayFragment.newInstance(BillKind.ELECTRICITY)`, `TIEN_NUOC` → `WATER`.
- [ ] **Step 4: Build + lint** `./gradlew testDebugUnitTest assembleDebug lintDebug` — Expected: BUILD SUCCESSFUL, lint 0 error.
- [ ] **Step 5: Chạy trên emulator/máy** (handoff §6): mở Tiền điện, tra `PE01000123456`, thấy hoá đơn; tra mã đuôi `0000` thấy lỗi; chụp màn so với mockup màn 1.
- [ ] **Step 6: Commit** `git commit -m "feat: electricity and water bill screens"`

---

### Task 11: Màn Nạp điện thoại

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/utility/TopUpFragment.kt`, `app/src/main/res/layout/fragment_top_up.xml`, `app/src/main/res/layout/item_denomination.xml`, `app/src/main/res/color/selector_denomination_stroke.xml`
- Modify: `HomeFragment.kt` (`NAP_DT`), `strings.xml`

**Interfaces:**
- Consumes: `TopUpViewModel` (Task 7), `PaymentUi` (Task 10).

- [ ] **Step 1: Layout** theo mockup màn 3: ô SĐT (`inputType="phone"`, chip/pill nhà mạng ở cuối ô bằng `endIconMode` custom hoặc `TextView` `Widget.Flux.StatusPill` đặt cạnh), gợi ý "Mặc định là số của bạn. Nhà mạng nhận theo đầu số."; lưới mệnh giá 3 cột (`RecyclerView` + `GridLayoutManager(3)`, ô cao 52dp bo 14dp, chọn = viền emerald 1.5dp nền `flux_emerald_light`); chip nguồn tiền; nút "Nạp %1$s".
- [ ] **Step 2: `TopUpFragment`** nối VM; lỗi SĐT "Số không thuộc nhà mạng hỗ trợ".
- [ ] **Step 3: `HomeFragment`**: `NAP_DT` → `TopUpFragment()`.
- [ ] **Step 4: Build + lint** như Task 10 Step 4 — Expected: PASS.
- [ ] **Step 5: Chạy trên máy**, so với mockup màn 3.
- [ ] **Step 6: Commit** `git commit -m "feat: phone top-up screen"`

---

### Task 12: Màn Danh mục (máy bay, phim, khách sạn, mua sắm)

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/utility/CatalogFragment.kt`, `CatalogItemAdapter.kt` (cùng thư mục), `app/src/main/res/layout/fragment_catalog.xml`, `item_catalog_offer.xml`, `item_catalog_product.xml`
- Modify: `HomeFragment.kt` (`VE_MAY_BAY`, `VE_PHIM`, `KHACH_SAN`, `MUA_SAM`), `strings.xml`

**Interfaces:**
- Consumes: `CatalogViewModel` (Task 8), `PaymentUi` (Task 10), `Utility` (icon/màu của từng kind).

- [ ] **Step 1: Layout** theo mockup màn 4–7: ChipGroup bộ lọc; ô ngày (chỉ khi `needsDate`, mở `MaterialDatePicker` giới hạn `dateRange`); `RecyclerView` danh sách mục (`item_catalog_offer`: ô icon màu tiện ích, tiêu đề, phụ đề, giá; chọn = viền emerald 1.5dp) hoặc lưới 2 cột cho SHOP (`item_catalog_product`); ChipGroup suất chiếu (chỉ MOVIE, suất đã qua bị tắt); ô đếm (nút −/+ 48dp, nhãn theo kind: "Hành khách", "Số vé", "Số đêm", "Số lượng"); khách sạn hiện "Trả phòng dd/MM/yyyy"; thanh đáy cố định (nền trắng, viền trên hairline): "Tổng · 2 khách" + số tiền `AmountSmall` + nút đen "Thanh toán"; lỗi hạn mức hiện trên thanh đáy.
- [ ] **Step 2: `CatalogFragment.newInstance(kind: CatalogKind)`** nối VM; tiêu đề "Vé máy bay" / "Vé xem phim" / "Đặt khách sạn" / "Mua sắm".
- [ ] **Step 3: `HomeFragment`**: bốn ô còn lại → `CatalogFragment.newInstance(…)`. Chỉ `CHI_NHANH`, `TRO_LY_AI` còn đi `ComingSoonFragment`.
- [ ] **Step 4: Build + lint** — Expected: PASS.
- [ ] **Step 5: Chạy trên máy** cả 4 kind, so với mockup màn 4–7; thử một đơn ≥ 10.000.000 ₫ (khách sạn đắt × nhiều đêm) để thấy bước quét mặt.
- [ ] **Step 6: Commit** `git commit -m "feat: flight, movie, hotel and shop catalog screens"`

---

### Task 13: Tài liệu, chạy đầu cuối

**Files:**
- Modify: `README.md` (mục rules: 47 ca; luồng demo F5), `PROJECT_CONTEXT.md` (Feature 5, `PAYMENT`, field `service`/`details`, giới hạn đã biết), `HANDOFF.md` (trạng thái, việc chủ dự án cần làm: publish rules), `app/src/main/java/com/flux/bank/data/model/Transaction.kt` KDoc (bỏ câu "Feature 5 sẽ thêm…")

- [ ] **Step 1: Chạy toàn bộ kiểm tra**: `./gradlew testDebugUnitTest assembleDebug lintDebug` và rules test — Expected: unit PASS hết, rules 47/47, lint 0 error.
- [ ] **Step 2: Cập nhật tài liệu** như Files; ghi giới hạn spec §6 (rules không biết số tiền hoá đơn/giá vé đúng bao nhiêu).
- [ ] **Step 3: Commit** `git add README.md PROJECT_CONTEXT.md HANDOFF.md app/src/main/java/com/flux/bank/data/model/Transaction.kt && git commit -m "docs: feature 5 utilities"`
- [ ] **Step 4: Báo chủ dự án** publish lại `firestore.rules` rồi chạy kịch bản demo spec §9 trên máy thật; review toàn nhánh trước khi merge (`superpowers:finishing-a-development-branch`).
