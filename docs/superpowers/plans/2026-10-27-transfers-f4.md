# Feature 4 — Chuyển tiền & VNPay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Khách chuyển tiền trong FLUX (theo số tài khoản, giữa tài khoản của mình), chuyển liên ngân hàng (giả lập), nạp tiền qua VNPay sandbox, và quét/hiện mã QR nhận tiền. Rules bảo đảm tiền chỉ được cộng cho người nhận khi người gửi bị trừ đúng số tiền đó trong cùng lần ghi.

**Architecture:** Giữ MVVM + service locator `FluxApp`. Mở rộng hợp đồng F3 "số dư chỉ đổi kèm một giao dịch mới": thêm `TRANSFER_OUT`/`TRANSFER_IN` có `pairTxId` trỏ vào nhau, tài khoản đích cộng bằng `FieldValue.increment` (người gửi không đọc được). Logic thuần (hạn mức, ký VNPay, nhãn, QR) nằm trong các object Kotlin có unit test, UI chỉ nối lại.

**Tech Stack:** Kotlin, XML + ViewBinding, Firestore (Spark), WebView, CameraX (đã có), ML Kit `barcode-scanning` + ZXing `core` (mới, chỉ cho QR), Node + Firestore emulator cho rules test.

**Spec:** `docs/superpowers/specs/2026-10-27-transfers-f4-design.md` (đã duyệt 2026-10-27). Mockup: `docs/superpowers/specs/mockups/2026-10-27-f4-transfers.html` (đã duyệt).

## Global Constraints

- Kotlin + XML/ViewBinding, không Compose. MVVM, repository = interface + impl Firestore, bản giả trong `app/src/test/java/com/flux/bank/fakes/Fakes.kt`.
- Chỉ giao diện sáng; chỉ dùng token màu/khoảng cách/`TextAppearance.Flux.*`/`Widget.Flux.*`; mọi chữ tiếng Việt trong `strings.xml`; tiền VND kiểu `Long`, hiển thị `48.250.000 ₫` qua `MoneyFormatter`.
- Emerald chỉ cho điểm tương tác; nút chính màu đen (`Widget.Flux.Button.Primary`). Vùng chạm ≥ 48dp.
- KDoc/comment tiếng Việt, tên code tiếng Anh. Commit `feat:`/`fix:`/`test:`/`docs:`/`chore:`. TDD: test fail trước.
- `git add` theo đường dẫn cụ thể (`app docs rules-test firestore.rules README.md …`), không `git add -A`, không commit `.idea/*`, không commit `local.properties`.
- Hạn mức: tối thiểu 10.000 ₫, tối đa 100.000.000 ₫ mỗi lần chuyển, 200.000.000 ₫ tổng chuyển đi mỗi ngày (giờ Việt Nam). Nội dung ≤ 100 ký tự, mặc định `"<TÊN KHÔNG DẤU> chuyen tien"`.
- Số tài khoản FLUX: 12 chữ số, bắt đầu `8868`. Liên ngân hàng: 6–19 chữ số. Mã ngân hàng FLUX là `"FLUX"`.
- Chỉ khách eKYC `VERIFIED` được chuyển/nạp. Mọi giao dịch qua `TransactionAuthSheet` (OTP; ≥ 10.000.000 ₫ thêm quét mặt).
- Khoá VNPay chỉ đọc từ `local.properties` (`VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`) qua `BuildConfig`; thiếu khoá thì báo "Chưa cấu hình VNPay", không crash.
- Lệnh kiểm tra chung: `./gradlew testDebugUnitTest` (từ `FLUX_Bank/`). Rules test: `cd rules-test && npx firebase emulators:exec --only firestore --project demo-flux "node --test rules.test.mjs"`.

## Review Focus

1. Người dùng sửa số tài khoản khi lần tra trước chưa trả về: tên người nhận cũ không được hiện cho số mới → test `staleLookupIsIgnored` ở Task 7.
2. WebView gọi URL trả về của VNPay hai lần (redirect lặp, bấm back): chỉ nạp một lần → test `paymentResultHandledOnce` ở Task 9.
3. URL trả về bị sửa (số tiền, mã đơn, chữ ký) hoặc mã đơn khác đơn đang chờ: không cộng tiền → test `tamperedAmountIsBadSignature`, `otherTxnRefIsMismatch` ở Task 4 và `badSignatureDoesNotDeposit` ở Task 9.
4. Điện thoại để múi giờ khác hoặc giao dịch lúc 23:59/00:01: hạn mức ngày tính theo ngày Việt Nam → test `dayBoundaryUsesVietnamTime` ở Task 3.
5. Bấm "Tiếp tục" hai lần nhanh (lỗi #8 cũ của F1–F2): chỉ mở một sheet xác thực → test `doubleConfirmAsksAuthOnce` ở Task 7.

---

### Task 1: Security Rules cho chuyển tiền và `accountIndex`

**Files:**
- Modify: `rules-test/rules.test.mjs`
- Modify: `firestore.rules`

**Interfaces:**
- Produces (dữ liệu Firestore mà mọi task sau phải ghi đúng):
  - `accountIndex/{accountNumber}` = `{ accountId, ownerUid, type: 'CHECKING'|'SAVING', holderName }`, chỉ có đúng 4 key này, `holderName == users/{ownerUid}.fullName`.
  - `transactions/{id}` thêm `type` `TRANSFER_OUT`/`TRANSFER_IN`, `pairTxId`, `counterpartyName`, `counterpartyAccount`, `counterpartyBank`, `paymentRef`. `TRANSFER_IN.counterpartyName == users/{người gửi}.fullName` (tên có dấu, đúng như hồ sơ).
  - Một lần chuyển trong FLUX = một batch/transaction 4 document (spec §5). Liên ngân hàng = 2 document, `pairTxId: null`.

- [ ] **Step 1: Thêm fixture và ca test vào `rules.test.mjs`**

Trong `beforeEach` thêm (vẫn trong `withSecurityRulesDisabled`): `users/cust` có `fullName: 'Nguyễn Văn An'`; `users/bob` = `{ uid: 'bob', role: 'CUSTOMER', fullName: 'Trần Thị Bình' }`; `accounts/bobchk` = `{ ownerUid: 'bob', type: 'CHECKING', balance: 0, accountNumber: '886800000002' }`; `accounts/sav1` = `{ ownerUid: 'cust', type: 'SAVING', balance: 0, accountNumber: '886800000003' }`; `accounts/rich` = `{ ownerUid: 'cust', type: 'CHECKING', balance: 500000000, accountNumber: '886800000004' }`. Import thêm `increment`, `getDocs`, `collection`.

Helper (ghi đúng thứ tự spec §5):

```js
function transfer(db, o = {}) {
  const { from = 'acc1', to = 'bobchk', toOwner = 'bob', outId = 'o1', inId = 'i1', amount = 50000,
          fromAfter = 100000 - amount, inAmount = amount, senderName = 'Nguyễn Văn An', debit = true, pair = inId } = o;
  const b = writeBatch(db);
  if (debit) b.update(doc(db, `accounts/${from}`), { balance: fromAfter, lastTxId: outId });
  b.set(doc(db, `transactions/${outId}`), { ...tx(outId, 'TRANSFER_OUT', amount, fromAfter, from), pairTxId: pair,
    counterpartyName: 'TRAN THI BINH', counterpartyAccount: '886800000002', counterpartyBank: 'FLUX' });
  if (to) {
    b.update(doc(db, `accounts/${to}`), { balance: increment(inAmount), lastTxId: inId });
    b.set(doc(db, `transactions/${inId}`), { id: inId, ownerUid: toOwner, accountId: to, accountNumber: '886800000002',
      type: 'TRANSFER_IN', amount: inAmount, balanceAfter: null, pairTxId: outId, counterpartyName: senderName,
      counterpartyAccount: '886800000001', counterpartyBank: 'FLUX', description: '', createdAt: 1 });
  }
  return b.commit();
}
```

Ca test (tên → kết quả):

```js
test('valid internal transfer allowed', () => assertSucceeds(transfer(cust())));
test('transfer to own saving allowed', () => assertSucceeds(transfer(cust(), { to: 'sav1', toOwner: 'cust' })));
test('external transfer (OUT only) allowed', () => assertSucceeds(transfer(cust(), { to: null, pair: null })));
test('credit without debit denied', () => assertFails(transfer(cust(), { debit: false })));
test('credit amount differs from OUT denied', () => assertFails(transfer(cust(), { inAmount: 90000 })));
test('reusing existing OUT id denied', () => assertFails(transfer(cust(), { outId: 'old' })));
test('fake sender name denied', () => assertFails(transfer(cust(), { senderName: 'Ai Đó' })));
test('transfer overdraft denied', () => assertFails(transfer(cust(), { amount: 100001, fromAfter: -1 })));
test('transfer to mortgage denied', () => assertFails(transfer(cust(), { to: 'mort', toOwner: 'cust' })));
test('transfer over 100M denied', () => assertFails(transfer(cust(), { from: 'rich', amount: 100000001, fromAfter: 399999999 })));
test('self credit with unpaired TRANSFER_IN denied', async () => {
  const db = cust(); const b = writeBatch(db);
  b.update(doc(db, 'accounts/acc1'), { balance: increment(50000), lastTxId: 'i9' });
  b.set(doc(db, 'transactions/i9'), { ...tx('i9', 'TRANSFER_IN', 50000, null), pairTxId: 'nope', counterpartyName: 'Nguyễn Văn An' });
  await assertFails(b.commit());
});
const idx = (o = {}) => ({ accountId: 'acc1', ownerUid: 'cust', type: 'CHECKING', holderName: 'Nguyễn Văn An', ...o });
test('owner creates own index entry', () => assertSucceeds(setDoc(doc(cust(), 'accountIndex/886800000001'), idx())));
test('index entry for someone else denied', () => assertFails(setDoc(doc(cust(), 'accountIndex/886800000002'), idx({ accountId: 'bobchk', ownerUid: 'bob' }))));
test('index entry with fake name denied', () => assertFails(setDoc(doc(cust(), 'accountIndex/886800000001'), idx({ holderName: 'Trần Thị Bình' }))));
test('index number mismatch denied', () => assertFails(setDoc(doc(cust(), 'accountIndex/886899999999'), idx())));
test('index list denied', () => assertFails(getDocs(collection(cust(), 'accountIndex'))));
test('other user can get index entry', async () => {
  await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), 'accountIndex/886800000002'), idx({ accountId: 'bobchk', ownerUid: 'bob', holderName: 'Trần Thị Bình' })));
  await assertSucceeds(getDoc(doc(cust(), 'accountIndex/886800000002')));
});
```

- [ ] **Step 2: Chạy rules test, xác nhận các ca mới FAIL**

Run: `cd rules-test && npx firebase emulators:exec --only firestore --project demo-flux "node --test rules.test.mjs"`
Expected: 20 ca cũ pass; các ca "allowed" mới và các ca index-allowed fail.

- [ ] **Step 3: Sửa `firestore.rules`**

- `ownerMoneyMoveAllowed`: đổi nhánh `WITHDRAW` thành `tx.type in ['WITHDRAW', 'TRANSFER_OUT']` (cùng phép trừ và `>= 0`).
- `match /accounts/{accountId}`: `allow update: if ownerMoneyMoveAllowed(accountId) || transferCreditAllowed(accountId);`
- `match /transactions/{txId}`: `allow create: if txCreateAllowed(txId);`
- Thêm `match /accountIndex/{accountNumber}`: `allow get: if signedIn(); allow list, delete: if false; allow create, update: if signedIn() && indexEntryValid(accountNumber);`

Hàm mới (logic bảo mật, chép đúng):

```
// Người nhận được cộng tiền chỉ khi cùng lần ghi có một TRANSFER_OUT mới của người ghi, cùng số tiền,
// trỏ ngược về giao dịch nhận, và tài khoản nguồn đã bị trừ (lastTxId = OUT).
function transferCreditAllowed(accountId) {
  let newData = request.resource.data;
  let inPath = /databases/$(database)/documents/transactions/$(newData.lastTxId);
  let txIn = getAfter(inPath).data;
  let outPath = /databases/$(database)/documents/transactions/$(txIn.pairTxId);
  let txOut = getAfter(outPath).data;
  return signedIn()
    && resource.data.type in ['CHECKING', 'SAVING']
    && changedKeys().hasOnly(['balance', 'lastTxId'])
    && newData.balance is int
    && !exists(inPath) && !exists(outPath)
    && txIn.type == 'TRANSFER_IN' && txIn.accountId == accountId && txIn.ownerUid == resource.data.ownerUid
    && newData.balance == resource.data.balance + txIn.amount
    && txOut.type == 'TRANSFER_OUT' && txOut.ownerUid == request.auth.uid
    && txOut.amount == txIn.amount && txOut.pairTxId == newData.lastTxId && txOut.accountId != accountId
    && getAfter(/databases/$(database)/documents/accounts/$(txOut.accountId)).data.lastTxId == txIn.pairTxId;
}

function txCreateAllowed(txId) {
  let d = request.resource.data;
  let acc = getAfter(/databases/$(database)/documents/accounts/$(d.accountId)).data;
  return signedIn() && d.amount is int && d.amount >= 10000 && acc.lastTxId == txId
    && ((d.type in ['DEPOSIT', 'WITHDRAW', 'TRANSFER_OUT'] && d.ownerUid == request.auth.uid
         && (d.type == 'WITHDRAW' || d.amount <= 100000000))
        || (d.type == 'TRANSFER_IN' && d.ownerUid == acc.ownerUid && transferInBacked(d)));
}

// TRANSFER_IN phải có cặp OUT của người ghi, cùng số tiền, và tên người gửi đúng hồ sơ.
function transferInBacked(d) {
  let out = getAfter(/databases/$(database)/documents/transactions/$(d.pairTxId)).data;
  return out.type == 'TRANSFER_OUT' && out.ownerUid == request.auth.uid && out.amount == d.amount
    && d.counterpartyName == get(/databases/$(database)/documents/users/$(request.auth.uid)).data.fullName;
}

function indexEntryValid(accountNumber) {
  let d = request.resource.data;
  let acc = get(/databases/$(database)/documents/accounts/$(d.accountId)).data;
  return d.keys().hasOnly(['accountId', 'ownerUid', 'type', 'holderName'])
    && d.ownerUid == request.auth.uid && acc.ownerUid == request.auth.uid
    && acc.accountNumber == accountNumber && d.type in ['CHECKING', 'SAVING'] && acc.type == d.type
    && d.holderName == get(/databases/$(database)/documents/users/$(request.auth.uid)).data.fullName;
}
```

Cập nhật comment đầu các `match` cho đúng (tiếng Việt).

- [ ] **Step 4: Chạy rules test, tất cả pass**

Run: lệnh ở Step 2. Expected: tất cả ca pass (20 cũ + 17 mới).

- [ ] **Step 5: Commit**

```bash
git add firestore.rules rules-test/rules.test.mjs
git commit -m "feat: rules for paired internal transfers and account index"
```

---

### Task 2: Model giao dịch mới, ngân hàng, chuẩn hoá tên

**Files:**
- Modify: `app/src/main/java/com/flux/bank/data/model/Transaction.kt`
- Create: `app/src/main/java/com/flux/bank/data/model/Bank.kt`
- Create: `app/src/main/java/com/flux/bank/data/model/Payee.kt`
- Create: `app/src/main/java/com/flux/bank/core/format/TextNormalizer.kt`
- Modify: `app/src/main/java/com/flux/bank/ui/officer/customers/CustomerListViewModel.kt` (dùng `TextNormalizer`, xoá `normalize`/`MARKS` cũ)
- Modify: `app/src/main/java/com/flux/bank/ui/customer/account/ReceiptFragment.kt` (biên dịch được với `balanceAfter: Long?`)
- Test: `app/src/test/java/com/flux/bank/core/TextNormalizerTest.kt`, `app/src/test/java/com/flux/bank/data/ModelDefaultsTest.kt`

**Interfaces:**
- Produces:
  - `enum class TransactionType(val isIncoming: Boolean) { DEPOSIT(true), WITHDRAW(false), TRANSFER_OUT(false), TRANSFER_IN(true) }`
  - `Transaction` thêm `counterpartyName: String = ""`, `counterpartyAccount: String = ""`, `counterpartyBank: String = ""`, `pairTxId: String? = null`, `paymentRef: String? = null`; `balanceAfter: Long? = 0`.
  - `data class Bank(val code: String, val shortName: String, val fullName: String)`; `object Banks { const val FLUX = "FLUX"; val ALL: List<Bank>; fun byCode(code: String): Bank? }` — 10 ngân hàng: VCB Vietcombank, TCB Techcombank, BIDV BIDV, CTG VietinBank, MB MB Bank, ACB ACB, VPB VPBank, TPB TPBank, STB Sacombank, VIB VIB (tên đầy đủ tiếng Việt như mockup).
  - `data class Payee(val accountId: String = "", val accountNumber: String = "", val ownerUid: String = "", val holderName: String = "", val type: AccountType = AccountType.CHECKING)` — `holderName` là tên gốc có dấu (đúng như rules); hiển thị qua `TextNormalizer.holderName`.
  - `object TextNormalizer { fun stripDiacritics(text: String): String; fun holderName(fullName: String): String }`

- [ ] **Step 1: Viết test fail**

```kotlin
class TextNormalizerTest {
    @Test fun stripsVietnameseMarksIncludingD() =
        assertEquals("Tran Thi Binh Dang", TextNormalizer.stripDiacritics("Trần Thị Bình Đặng"))
    @Test fun holderNameIsUpperCaseNoMarksSingleSpaced() =
        assertEquals("NGUYEN VAN AN", TextNormalizer.holderName("  nguyễn   văn  an "))
}
```

Trong `ModelDefaultsTest` thêm: `Transaction()` có `pairTxId == null`, `paymentRef == null`, `counterpartyBank == ""`; `TransactionType.TRANSFER_IN.isIncoming` true, `TRANSFER_OUT.isIncoming` false; `Banks.byCode("VCB")!!.shortName == "Vietcombank"`, `Banks.ALL.size == 10`, `Banks.byCode("FLUX") == null`.

- [ ] **Step 2: Chạy test, xác nhận FAIL** — `./gradlew testDebugUnitTest --tests '*TextNormalizerTest' --tests '*ModelDefaultsTest'`, Expected: lỗi biên dịch (chưa có class).

- [ ] **Step 3: Cài đặt** — `stripDiacritics`: NFD rồi bỏ `\p{Mn}+`, sau đó thay `đ→d`, `Đ→D` (NFD không tách chữ đ). `holderName`: `stripDiacritics(...).trim().split(Regex("\\s+")).joinToString(" ").uppercase()`. `CustomerListViewModel` gọi `TextNormalizer.stripDiacritics(text).lowercase()` ở chỗ cũ dùng `normalize`. `ReceiptFragment`: đổi tạm `MoneyFormatter.formatVnd(tx.balanceAfter ?: 0)` để biên dịch (Task 5 sửa đúng).

- [ ] **Step 4: Chạy toàn bộ unit test** — `./gradlew testDebugUnitTest`, Expected: tất cả pass (160 cũ + mới).

- [ ] **Step 5: Commit** — `git add app && git commit -m "feat: transfer transaction fields, bank list and name normalizer"`

---

### Task 3: `TransferPolicy` — kiểm tra trước khi ghi

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/TransferPolicy.kt`
- Test: `app/src/test/java/com/flux/bank/domain/TransferPolicyTest.kt`

**Interfaces:**
- Consumes: `Transaction`, `TransactionType` (Task 2), `Validators.MIN_AMOUNT`.
- Produces:
  ```kotlin
  enum class TransferError { BELOW_MIN, ABOVE_MAX, INSUFFICIENT_FUNDS, DAILY_LIMIT, SAME_ACCOUNT,
      INVALID_ACCOUNT_NUMBER, PAYEE_NOT_FOUND, NO_BANK, NO_NAME, DESCRIPTION_TOO_LONG }
  object TransferPolicy {
      const val MAX_PER_TRANSFER = 100_000_000L
      const val DAILY_LIMIT = 200_000_000L
      const val MAX_DESCRIPTION = 100
      val VN_ZONE: ZoneId  // Asia/Ho_Chi_Minh
      fun sentToday(history: List<Transaction>, now: Long): Long        // tổng TRANSFER_OUT cùng ngày VN với now
      fun remainingToday(history: List<Transaction>, now: Long): Long   // max(0, DAILY_LIMIT - sentToday)
      fun checkAmount(amount: Long, balance: Long, remainingToday: Long): TransferError?  // thứ tự: BELOW_MIN, ABOVE_MAX, INSUFFICIENT_FUNDS, DAILY_LIMIT
      fun isFluxAccountNumber(s: String): Boolean      // ^8868\d{8}$
      fun isExternalAccountNumber(s: String): Boolean  // ^\d{6,19}$
  }
  ```

- [ ] **Step 1: Viết test fail** — trong `TransferPolicyTest`, dùng `fun at(iso: String) = ZonedDateTime.parse(iso).toInstant().toEpochMilli()` và `fun out(amount: Long, at: Long) = Transaction(type = TransactionType.TRANSFER_OUT, amount = amount, createdAt = at)`:
  - `belowMinFirst`: `checkAmount(9_999, 0, 0) == BELOW_MIN`.
  - `aboveMax`: `checkAmount(100_000_001, 500_000_000, 200_000_000) == ABOVE_MAX`.
  - `overBalance`: `checkAmount(500_001, 500_000, 200_000_000) == INSUFFICIENT_FUNDS`.
  - `overDaily`: `checkAmount(60_000_000, 500_000_000, 50_000_000) == DAILY_LIMIT`; `checkAmount(50_000_000, 500_000_000, 50_000_000) == null`.
  - `sentTodayCountsOnlyTransferOut`: history có OUT 150tr hôm nay, WITHDRAW 5tr hôm nay, TRANSFER_IN 9tr hôm nay → `sentToday == 150_000_000`, `remainingToday == 50_000_000`.
  - `dayBoundaryUsesVietnamTime`: now = `2026-10-28T00:01+07:00`; OUT lúc `2026-10-27T23:59+07:00` không tính, OUT lúc `2026-10-28T00:00:30+07:00` có tính. Chạy test với `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))` trong `@Before` (khôi phục ở `@After`) để chắc không phụ thuộc múi giờ máy.
  - `accountNumberFormats`: `isFluxAccountNumber("886812345678")` true, `"886712345678"` false, `"88681234567"` false; `isExternalAccountNumber("007100041234")` true, `"12345"` false, `"12345678901234567890"` false.

- [ ] **Step 2: Chạy, FAIL** — `./gradlew testDebugUnitTest --tests '*TransferPolicyTest'`.
- [ ] **Step 3: Cài đặt** theo Interfaces (so ngày bằng `Instant.ofEpochMilli(x).atZone(VN_ZONE).toLocalDate()`).
  Ghi comment `// shortcut: chỉ cộng trong 100 giao dịch mới nhất (listRecent), nâng cấp: query theo ngày nếu khách giao dịch nhiều hơn` ở `sentToday`.
- [ ] **Step 4: Chạy, PASS.**
- [ ] **Step 5: Commit** — `git commit -m "feat: transfer limits and validation policy"` (chỉ add 2 file trên).

---

### Task 4: `VnPay` — dựng URL thanh toán và kiểm URL trả về

**Files:**
- Create: `app/src/main/java/com/flux/bank/payment/VnPay.kt`
- Modify: `app/build.gradle.kts` (`buildFeatures { buildConfig = true }`, đọc `local.properties` → `buildConfigField` `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, mặc định `""`)
- Modify: `app/src/main/java/com/flux/bank/FluxApp.kt` (`val vnPay: VnPay?` lazy, `null` khi `VNPAY_TMN_CODE` rỗng)
- Test: `app/src/test/java/com/flux/bank/payment/VnPayTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class VnPayConfig(val tmnCode: String, val hashSecret: String,
                         val payUrl: String = VnPay.SANDBOX_URL, val returnUrl: String = VnPay.RETURN_URL)
  sealed interface VnPayResult {
      data class Success(val transactionNo: String) : VnPayResult
      data object Cancelled : VnPayResult            // vnp_ResponseCode == "24"
      data class Failed(val code: String) : VnPayResult
      data object BadSignature : VnPayResult
      data object Mismatch : VnPayResult             // TxnRef hoặc Amount khác đơn đang chờ
  }
  class VnPay(private val config: VnPayConfig, private val clock: () -> Long = System::currentTimeMillis) {
      fun paymentUrl(txnRef: String, amount: Long, orderInfo: String): String
      fun isReturnUrl(url: String): Boolean
      fun parseReturn(query: Map<String, String>, expectedTxnRef: String, expectedAmount: Long): VnPayResult
      companion object {
          const val SANDBOX_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
          const val RETURN_URL = "https://fluxbank.example/vnpay-return"
          fun sign(params: Map<String, String>, secret: String): String
          fun parseQuery(url: String): Map<String, String>
      }
  }
  ```
- Tham số gửi đi (tài liệu VNPay 2.1.0): `vnp_Version=2.1.0`, `vnp_Command=pay`, `vnp_TmnCode`, `vnp_Amount=amount*100`, `vnp_CurrCode=VND`, `vnp_TxnRef`, `vnp_OrderInfo` (không dấu), `vnp_OrderType=other`, `vnp_Locale=vn`, `vnp_ReturnUrl`, `vnp_IpAddr=127.0.0.1`, `vnp_CreateDate`/`vnp_ExpireDate` (`yyyyMMddHHmmss`, Asia/Ho_Chi_Minh, hết hạn sau 15 phút).
- `sign`: bỏ `vnp_SecureHash` và `vnp_SecureHashType`, sắp xếp theo key, nối `URLEncoder.encode(k, US_ASCII)=URLEncoder.encode(v, US_ASCII)` bằng `&`, HMAC-SHA512 hex chữ thường. Query của URL gửi đi dùng đúng chuỗi đã mã hoá đó + `&vnp_SecureHash=…`.
- `parseQuery`: tách phần sau `?`, decode bằng `URLDecoder.decode(_, UTF_8)` (đổi `+` thành dấu cách); không dùng `Uri` của Android để còn unit test được và tránh lệch cách decode `+`.
- Thứ tự kiểm ở `parseReturn`: chữ ký sai → `BadSignature`; `vnp_TxnRef != expectedTxnRef` hoặc `vnp_Amount != expectedAmount*100` → `Mismatch`; `vnp_ResponseCode == "24"` → `Cancelled`; `ResponseCode == "00" && vnp_TransactionStatus == "00"` → `Success(vnp_TransactionNo)`; còn lại → `Failed(vnp_ResponseCode)`.

- [ ] **Step 1: Viết test fail** (`val vnp = VnPay(VnPayConfig("TESTCODE", "SECRET"), clock = { at("2026-10-27T09:05:00+07:00") })`):
  - `signMatchesKnownVector`: `VnPay.sign(mapOf("vnp_Amount" to "100000000", "vnp_OrderInfo" to "Nap tien FLUX abc", "vnp_TxnRef" to "abc"), "SECRET") == "668e6ae63a0afeb54120e36e6e10ae55f1770a10b9f50be5bb016536b29c21cc22058bcaf654cf2984efb9f5eb26c7821a20263222edf479db2b0affa0ab1c1d"` (tính bằng Python `hmac`/`quote_plus`).
  - `paymentUrlHasRequiredParams`: `parseQuery(vnp.paymentUrl("abc", 1_000_000, "Nap tien FLUX abc"))` có `vnp_Amount == "100000000"`, `vnp_CreateDate == "20261027090500"`, `vnp_ExpireDate == "20261027092000"`, `vnp_TmnCode == "TESTCODE"`, `vnp_ReturnUrl == VnPay.RETURN_URL`, và `vnp_SecureHash == VnPay.sign(query - "vnp_SecureHash", "SECRET")`; URL bắt đầu bằng `SANDBOX_URL + "?"`.
  - Helper `fun returned(vararg extra: Pair<String, String>): Map<String, String>`: tạo map `vnp_TxnRef=abc, vnp_Amount=100000000, vnp_ResponseCode=00, vnp_TransactionStatus=00, vnp_TransactionNo=14000001, vnp_OrderInfo=Nap tien FLUX abc` + `extra`, rồi thêm `vnp_SecureHash` ký bằng `"SECRET"`.
  - `successfulReturn`: `vnp.parseReturn(returned(), "abc", 1_000_000) == Success("14000001")`.
  - `tamperedAmountIsBadSignature`: `returned() + ("vnp_Amount" to "900000000")` → `BadSignature`.
  - `otherTxnRefIsMismatch`: `parseReturn(returned(), "xyz", 1_000_000) == Mismatch`.
  - `cancelled`: `returned("vnp_ResponseCode" to "24")` → `Cancelled`.
  - `failedStatus`: `returned("vnp_TransactionStatus" to "02")` → `Failed("00")`; `returned("vnp_ResponseCode" to "51", "vnp_TransactionStatus" to "02")` → `Failed("51")`.
  - `parseQueryDecodesPlusAndPercent`: `parseQuery("https://fluxbank.example/vnpay-return?vnp_OrderInfo=Nap+tien%3A+A&vnp_TxnRef=abc") == mapOf("vnp_OrderInfo" to "Nap tien: A", "vnp_TxnRef" to "abc")`.
  - `isReturnUrl`: true cho `RETURN_URL + "?a=1"`, false cho `SANDBOX_URL`.
- [ ] **Step 2: Chạy, FAIL** — `./gradlew testDebugUnitTest --tests '*VnPayTest'`.
- [ ] **Step 3: Cài đặt `VnPay`, `buildConfigField`, `FluxApp.vnPay`.** Đọc `local.properties` trong `app/build.gradle.kts`: `val localProps = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }`.
- [ ] **Step 4: Chạy toàn bộ unit test + `./gradlew assembleDebug`, PASS.**
- [ ] **Step 5: Commit** — `git add app/build.gradle.kts app/src && git commit -m "feat: VNPay sandbox URL signing and return verification"`

---

### Task 5: Repository chuyển tiền, danh bạ người nhận, nhãn và biên lai

**Files:**
- Create: `app/src/main/java/com/flux/bank/data/repository/PayeeRepository.kt`
- Modify: `app/src/main/java/com/flux/bank/data/repository/AccountRepository.kt`
- Modify: `app/src/main/java/com/flux/bank/FluxApp.kt` (`payeeRepository`)
- Modify: `app/src/main/java/com/flux/bank/ui/customer/home/HomeViewModel.kt` (gọi `ensureIndexed`)
- Modify: `app/src/main/java/com/flux/bank/ui/customer/account/TransactionRowAdapter.kt`, `ReceiptFragment.kt`, `res/layout/fragment_receipt.xml` (nếu cần), `ui/customer/history/HistoryFragment.kt` (dòng phụ), `res/values/strings.xml`
- Modify: `app/src/test/java/com/flux/bank/fakes/Fakes.kt`
- Test: `app/src/test/java/com/flux/bank/ui/customer/TransactionLabelTest.kt`, `HomeViewModelTest.kt`

**Interfaces:**
- Consumes: `Payee`, `Bank`, `Banks`, `TextNormalizer`, `Transaction` mới (Task 2).
- Produces:
  ```kotlin
  interface PayeeRepository {
      /** Tra `accountIndex/{accountNumber}`; không có thì Result.success(null). */
      suspend fun find(accountNumber: String): Result<Payee?>
      /** Ghi/ghi lại mục index cho các Checking/Saving của chủ khi thiếu hoặc holderName khác [fullName]. Bỏ qua tài khoản đã kiểm trong phiên. */
      suspend fun ensureIndexed(accounts: List<BankAccount>, fullName: String): Result<Unit>
  }
  // AccountRepository thêm:
  fun newTransactionId(): String
  suspend fun deposit(accountId: String, amount: Long, txId: String? = null, paymentRef: String? = null): Result<Transaction>  // thay chữ ký cũ
  suspend fun transfer(fromId: String, payee: Payee, amount: Long, description: String, senderName: String): Result<Transaction>
  suspend fun transferExternal(fromId: String, bank: Bank, accountNumber: String, holderName: String, amount: Long, description: String): Result<Transaction>
  // TransactionRowAdapter.Companion:
  data class TxLabel(@StringRes val res: Int, val name: String? = null)
  fun labelFor(tx: Transaction): TxLabel   // thay labelFor(type)
  ```
  `transfer`/`transferExternal` trả về giao dịch `TRANSFER_OUT`; lỗi số dư là `InsufficientFundsException`, gỡ khỏi ngoại lệ bọc như `move()` hiện có (tách hàm gỡ dùng chung). `senderName` là `fullName` có dấu (rules so khớp); `TRANSFER_OUT.counterpartyName = TextNormalizer.holderName(payee.holderName)`, `TRANSFER_IN.counterpartyName = senderName`. Ghi đúng 4 document theo spec §5, tài khoản đích dùng `FieldValue.increment(amount)` và **không** `t.get` tài khoản đích. Liên ngân hàng: `counterpartyBank = bank.code`, `pairTxId = null`, `counterpartyName = TextNormalizer.holderName(holderName)`.
- Fakes: `FakePayeeRepository(val index: MutableMap<String, Payee> = mutableMapOf())` có `var findGate: CompletableDeferred<Unit>?`, `val ensured = mutableListOf<Pair<List<BankAccount>, String>>()`. `FakeAccountRepository` thêm `val transfers = mutableListOf<Transaction>()`, `newTransactionId()` trả `"vnp${n}"`, `transfer`/`transferExternal` trừ nguồn (lỗi `InsufficientFundsException` khi âm), cộng đích nếu đích có trong `accounts`, dùng chung `moveGate`/`failNextMove`, và dựng `TRANSFER_OUT` với `counterpartyName`/`counterpartyBank`/`pairTxId`/`description` theo đúng quy tắc của bản Firestore ở trên (thêm vào `transfers`); `deposit` ghi `paymentRef` và dùng `txId` nếu có.

- [ ] **Step 1: Viết test fail**
  - `TransactionLabelTest`: `labelFor(Transaction(type = TRANSFER_OUT, counterpartyName = "TRAN THI BINH")) == TxLabel(R.string.tx_transfer_out, "TRAN THI BINH")`; `TRANSFER_IN` với `counterpartyName = "Nguyễn Văn An"` → `TxLabel(R.string.tx_transfer_in, "NGUYEN VAN AN")`; `DEPOSIT` có `paymentRef = "14000001"` → `TxLabel(R.string.tx_deposit_vnpay)`; `DEPOSIT` không `paymentRef` → `TxLabel(R.string.util_deposit)`; `WITHDRAW` → `TxLabel(R.string.util_withdraw)`.
  - `HomeViewModelTest.indexesOwnAccountsWithFullName`: user `customer("c1", name = "Nguyễn Văn An")`, accounts chk + sav + mortgage → sau `load()`, `payees.ensured.single() == (danh sách 3 tài khoản của c1) to "Nguyễn Văn An"` (repository tự lọc MORTGAGE). Cập nhật constructor `HomeViewModel(..., payees: PayeeRepository = FluxApp.instance.payeeRepository)` ở mọi test đang tạo `HomeViewModel`.
- [ ] **Step 2: Chạy, FAIL.**
- [ ] **Step 3: Cài đặt** repository Firestore, fakes, `FluxApp.payeeRepository`, `HomeViewModel.load()` gọi `payees.ensureIndexed(rawAccounts, user.fullName)` khi có user (lỗi bỏ qua, không chặn trang chủ), `labelFor(tx)`, strings `tx_transfer_out` "Chuyển đến %1$s", `tx_transfer_in` "Nhận từ %1$s", `tx_deposit_vnpay` "Nạp tiền qua VNPay".
  `ReceiptFragment`: nhận thêm qua arguments `counterpartyName`, `counterpartyAccount`, `counterpartyBank`, `description`, `paymentRef`; `balanceAfter` null thì ẩn dòng "Số dư mới". Hàng theo mockup màn 5: OUT: Từ / Đến (tên) / dòng phụ "`<ngân hàng>` · `<số TK>`" (FLUX hoặc `Banks.byCode().shortName`) / Nội dung / Thời gian / Mã giao dịch / Số dư mới. IN: Đến tài khoản / Từ (tên + số TK) / Nội dung / Thời gian / Mã giao dịch. DEPOSIT có `paymentRef`: Nguồn tiền = "VNPay" + dòng "Mã VNPay". Tiêu đề "Chuyển tiền thành công" / "Đã nhận tiền". Với giao dịch chuyển tiền, nút phụ đổi chữ thành "Chuyển tiếp" và mở tab `TRANSFER`.
  `HistoryFragment` dòng phụ: liên ngân hàng (`counterpartyBank` khác `FLUX` và rỗng) dùng `Banks.byCode(code)?.shortName` thay cho nhãn tài khoản.
- [ ] **Step 4: `./gradlew testDebugUnitTest assembleDebug`, PASS.**
- [ ] **Step 5: Commit** — `git commit -m "feat: transfer and payee repositories, transfer labels and receipts"`

---

### Task 6: Tab Chuyển tiền và người nhận gần đây

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/transfer/TransferHubFragment.kt`, `TransferHubViewModel.kt`, `res/layout/fragment_transfer_hub.xml`, `res/layout/item_recent_payee.xml`
- Create: `app/src/main/java/com/flux/bank/ui/customer/transfer/TransferMode.kt`
- Modify: `app/src/main/java/com/flux/bank/ui/customer/CustomerMainActivity.kt` (`CustomerTab.TRANSFER -> TransferHubFragment()`)
- Create icon: `res/drawable/ic_bank.xml` (Phosphor Bank, regular — tải SVG từ Phosphor, MIT, chuyển sang vector)
- Test: `app/src/test/java/com/flux/bank/ui/customer/TransferHubViewModelTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class TransferMode { INTERNAL, OWN, EXTERNAL }
  data class RecentPayee(val bankCode: String, val accountNumber: String, val name: String)
  data class TransferPrefill(val accountNumber: String, val bankCode: String = Banks.FLUX, val name: String = "")
  class TransferHubViewModel(transactions: TransactionRepository, auth: AuthRepository) : ViewModel() {
      val recent: LiveData<List<RecentPayee>>
      fun load()
      companion object { const val MAX_RECENT = 5; fun recentPayees(history: List<Transaction>): List<RecentPayee> }
  }
  ```
  `TransferFragment.newInstance(mode: TransferMode, prefill: TransferPrefill? = null)` được định nghĩa ở Task 8; ở task này các dòng mở tạm `ComingSoonFragment` cho tới Task 8 nối vào.
- [ ] **Step 1: Test fail** — `recentPayeesDistinctNewestFirst`: history (mới nhất trước) gồm OUT tới (FLUX, 886855309182, "TRAN THI BINH"), IN từ ai đó, OUT tới (VCB, 0071000412345, "LE VAN CUONG"), OUT tới (FLUX, 886855309182) lần 2, và 5 OUT tới số khác → kết quả đúng 5 phần tử, phần tử đầu là Bình, Cường đứng thứ 2, không trùng, không có TRANSFER_IN. `recentPayeesSkipsOwnTransfersWithoutName` không cần; chỉ thêm `emptyHistoryGivesEmpty`.
- [ ] **Step 2: Chạy, FAIL.**
- [ ] **Step 3: Cài đặt** VM (`listRecent(uid)` rồi `recentPayees`), layout theo mockup màn 1 (ba dòng tùy chọn trong một `Widget.Flux.Card`, mục "Người nhận gần đây" là RecyclerView với avatar chữ tắt; ẩn mục khi rỗng), nối tab.
- [ ] **Step 4: `./gradlew testDebugUnitTest assembleDebug`, PASS.**
- [ ] **Step 5: Commit** — `git commit -m "feat: transfer tab with recent payees"`

---

### Task 7: `TransferViewModel`

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/transfer/TransferViewModel.kt`
- Test: `app/src/test/java/com/flux/bank/ui/customer/TransferViewModelTest.kt`

**Interfaces:**
- Consumes: `TransferPolicy`, `TransferError` (Task 3); `PayeeRepository`, `AccountRepository.transfer/transferExternal` (Task 5); `TransferMode`, `TransferPrefill` (Task 6); `TextNormalizer`, `Banks`.
- Produces:
  ```kotlin
  data class TransferState(
      val mode: TransferMode,
      val sources: List<BankAccount> = emptyList(), val sourceId: String? = null,
      val ownTargets: List<BankAccount> = emptyList(), val targetId: String? = null,   // OWN
      val accountNumber: String = "", val payee: Payee? = null, val lookingUp: Boolean = false, // INTERNAL
      val bank: Bank? = null, val externalName: String = "",                              // EXTERNAL
      val amount: Long = 0, val description: String = "", val remainingToday: Long = TransferPolicy.DAILY_LIMIT,
      val recipientError: TransferError? = null, val amountError: TransferError? = null,
      val kycBlocked: Boolean = false, val submitting: Boolean = false, @StringRes val errorRes: Int? = null
  )
  data class AuthRequest(val amount: Long, val recipientName: String, val recipientDetail: String)
  class TransferViewModel(
      mode: TransferMode, accounts: AccountRepository, payees: PayeeRepository, transactions: TransactionRepository,
      users: UserRepository, auth: AuthRepository, now: () -> Long = System::currentTimeMillis
  ) : ViewModel() {
      val state: LiveData<TransferState>; val askAuth: LiveData<Event<AuthRequest>>; val done: LiveData<Event<Transaction>>
      fun load(prefill: TransferPrefill?)
      fun selectSource(accountId: String); fun selectTarget(accountId: String)
      fun setAccountNumber(input: String)   // giữ chữ số; đủ 12 số hợp lệ thì tra, huỷ lần tra trước
      fun selectBank(code: String); fun setExternalName(input: String)
      fun setAmount(input: String)          // Validators.parseAmount
      fun setDescription(input: String)
      fun confirm(); fun onAuthResult(verified: Boolean)
  }
  ```
  Quy tắc: `description` mặc định `"${TextNormalizer.holderName(fullName)} chuyen tien"` khi người dùng chưa sửa; OWN: `ownTargets` = các nguồn trừ `sourceId`, mặc định chọn cái đầu; `confirm` kiểm theo thứ tự: người nhận (INVALID_ACCOUNT_NUMBER / PAYEE_NOT_FOUND / SAME_ACCOUNT / NO_BANK / NO_NAME), rồi `TransferPolicy.checkAmount`, rồi `DESCRIPTION_TOO_LONG`; chỉ khi không lỗi, KYC VERIFIED, không đang gửi và không đang chờ sheet mới phát `askAuth`. Sau `onAuthResult(true)`: INTERNAL/OWN gọi `transfer(sourceId, payee, amount, description, fullName)` (OWN: payee dựng từ tài khoản đích của chính mình, `holderName = fullName`); EXTERNAL gọi `transferExternal`. Thành công → `done`; `InsufficientFundsException` → tải lại nguồn + `R.string.err_insufficient_funds`; lỗi khác → `R.string.err_generic`. `remainingToday` tính khi `load` từ `transactions.listRecent(uid)`.

- [ ] **Step 1: Test fail** (fixture: c1 "Nguyễn Văn An" VERIFIED; chk 8.250.000 số `886812034471`; sav 120.000.000 số `886844172093`; payee index `886855309182` → Payee("bobchk", "886855309182", "bob", "Trần Thị Bình"); `now = 2026-10-27T09:30+07:00`):
  - `lookupShowsPayee`: `setAccountNumber("8868 5530 9182")` → `state.payee!!.accountId == "bobchk"`, `recipientError == null`.
  - `unknownNumberIsNotFound`: `"886899999999"` → `payee == null`, `recipientError == PAYEE_NOT_FOUND`.
  - `ownSourceNumberIsSameAccount`: nhập `"886812034471"` khi nguồn là chk (index có mục cho chk) → `SAME_ACCOUNT`.
  - `staleLookupIsIgnored`: `findGate` treo; nhập `"886855309182"`, rồi nhập `"886899999999"`, rồi mở gate → `payee == null`, `recipientError == PAYEE_NOT_FOUND` (kết quả số cũ không ghi đè).
  - `overBalanceBlocked`: payee hợp lệ, amount 9.000.000 → `amountError == INSUFFICIENT_FUNDS`, `askAuth.value == null`.
  - `dailyLimitFromHistory`: history có OUT 150.000.000 lúc 08:00 hôm nay và OUT 90.000.000 hôm qua → `remainingToday == 50_000_000`; nguồn sav, amount 60.000.000 → `DAILY_LIMIT`.
  - `notVerifiedBlocked`: kyc PENDING → `kycBlocked`, `confirm()` không phát `askAuth`.
  - `doubleConfirmAsksAuthOnce`: `confirm(); confirm()` → `askAuth` chỉ được set một lần (đếm bằng observer).
  - `internalTransferSucceeds`: amount 500.000, `confirm()`, `onAuthResult(true)` → `accounts.transfers.single()` có `type == TRANSFER_OUT`, `amount == 500_000`, `counterpartyName == "TRAN THI BINH"`, `description == "NGUYEN VAN AN chuyen tien"`; `done` có giá trị; số dư chk trong fake = 7.750.000.
  - `authDeniedWritesNothing`: `onAuthResult(false)` → `transfers` rỗng.
  - `ownModeTargetsExcludeSource`: mode OWN, nguồn chk → `ownTargets.map { it.accountId } == listOf("sav")`; chuyển 1.000.000 → sav trong fake tăng 1.000.000.
  - `externalNeedsBankAndName`: mode EXTERNAL, số `"0071000412345"`, không chọn ngân hàng → `recipientError == NO_BANK`; chọn VCB, tên rỗng → `NO_NAME`.
  - `externalTransferNormalizesName`: VCB, tên `"lê văn cường"`, 2.000.000 → `counterpartyName == "LE VAN CUONG"`, `counterpartyBank == "VCB"`, `pairTxId == null`.
  - `prefillFromRecent`: `load(TransferPrefill("886855309182"))` → tra luôn, `payee` có giá trị.
- [ ] **Step 2: Chạy, FAIL.**
- [ ] **Step 3: Cài đặt.**
- [ ] **Step 4: Chạy, PASS.**
- [ ] **Step 5: Commit** — `git commit -m "feat: transfer view model with validation before write"`

---

### Task 8: Màn chuyển tiền và chọn ngân hàng

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/transfer/TransferFragment.kt`, `BankPickerSheet.kt`, `res/layout/fragment_transfer.xml`, `res/layout/sheet_bank_picker.xml`, `res/layout/item_bank.xml`
- Modify: `TransferHubFragment.kt` (thay `ComingSoonFragment` bằng `TransferFragment`), `res/values/strings.xml`

**Interfaces:**
- Consumes: `TransferViewModel` (Task 7), `TransactionAuthSheet.show(fm, AuthSummary(title, rows, amount), requestKey)` + `KEY_VERIFIED`, `ReceiptFragment.newInstance(...)` (Task 5), `ScreenHost.openScreen`.
- Produces: `TransferFragment.newInstance(mode: TransferMode, prefill: TransferPrefill? = null)`; `BankPickerSheet.show(fm, requestKey)`, kết quả `bundle.getString(BankPickerSheet.KEY_CODE)`.

- [ ] **Step 1: Cài đặt `TransferFragment`** theo mockup màn 2–3: tiêu đề theo mode ("Chuyển trong FLUX" / "Giữa tài khoản của tôi" / "Liên ngân hàng"); chip nguồn (dùng lại cách tạo chip của `MoneyMoveFragment`, tạo lại chip trong `onViewCreated` — tránh lỗi #10 cũ); INTERNAL: ô số tài khoản + khung tên người nhận nền `cat_emerald_bg` (khung chỉ hiện khi có `payee`), OWN: chip đích; EXTERNAL: dòng chọn ngân hàng mở `BankPickerSheet`, ô số tài khoản, ô tên (helper "Tự viết hoa, bỏ dấu"), ghi chú giả lập. Ô số tiền dùng `AmountTextWatcher`, helper hiện "Hạn mức còn lại hôm nay: …". Ô nội dung. Lỗi `TransferError` → chuỗi tiếng Việt dưới ô tương ứng; `kycBlocked` → hiện thông báo + nút tới eKYC như `MoneyMoveFragment`. `askAuth` → `TransactionAuthSheet` với rows "Người nhận" → tên, "Tài khoản" → `"<ngân hàng> · <số>"`. `done` → `openScreen(ReceiptFragment.newInstance(tx, accountLabel, viewOnly = false))` sau khi pop màn chuyển. Bàn phím không che nút (dùng `KeyboardInsets` có sẵn).
- [ ] **Step 2: Cài đặt `BankPickerSheet`** (mockup màn 4): `Widget.Flux.BottomSheet`, ô tìm lọc theo `TextNormalizer.stripDiacritics(...).lowercase()` trên mã + tên, RecyclerView `Banks.ALL`, avatar chữ mã.
- [ ] **Step 3: Nối các dòng ở `TransferHubFragment`** và "Người nhận gần đây" (`bankCode == FLUX` → INTERNAL, khác → EXTERNAL với prefill bank/tên).
- [ ] **Step 4: `./gradlew testDebugUnitTest assembleDebug lintDebug`** — Expected: pass, lint 0 error.
- [ ] **Step 5: Kiểm tra trên emulator/máy** — mở tab Chuyển tiền, ba màn hiện đúng mockup, nhập số tài khoản ra tên, lỗi vượt số dư hiện dưới ô. Chụp màn hình theo HANDOFF §6.
- [ ] **Step 6: Commit** — `git commit -m "feat: transfer screens and bank picker"`

---

### Task 9: Nạp tiền qua VNPay

**Files:**
- Modify: `app/src/main/java/com/flux/bank/ui/customer/account/MoneyMoveViewModel.kt`, `MoneyMoveFragment.kt`
- Create: `app/src/main/java/com/flux/bank/ui/customer/account/VnPayFragment.kt`, `res/layout/fragment_vnpay.xml`
- Modify: `res/values/strings.xml` (`move_source_value` cho nạp = "VNPay · thẻ ATM / QR (sandbox)", `err_vnpay_not_configured`, `err_vnpay_cancelled`, `err_vnpay_failed`, `err_vnpay_bad_result`)
- Test: `app/src/test/java/com/flux/bank/ui/customer/MoneyMoveViewModelTest.kt`

**Interfaces:**
- Consumes: `VnPay`, `VnPayResult` (Task 4); `AccountRepository.newTransactionId()`, `deposit(accountId, amount, txId, paymentRef)` (Task 5).
- Produces:
  ```kotlin
  // MoneyMoveViewModel: thêm tham số constructor `payments: VnPay? = FluxApp.instance.vnPay`
  val openPayment: LiveData<Event<String>>          // URL thanh toán
  fun onPaymentResult(query: Map<String, String>)
  fun onPaymentClosed()                              // người dùng đóng WebView trước khi có kết quả
  // VnPayFragment
  companion object { const val REQUEST_KEY = "vnpay"; const val KEY_QUERY = "query"; fun newInstance(url: String) }
  ```
  Luồng nạp: `onAuthResult(true)` → `payments == null` thì `errorRes = err_vnpay_not_configured`; ngược lại tạo `txId = accounts.newTransactionId()`, giữ `pending (txId, accountId, amount)`, phát `openPayment(payments.paymentUrl(txId, amount, "Nap tien FLUX $txId"))`. `onPaymentResult`: không có `pending` thì bỏ qua; xoá `pending` **trước** khi xử lý; `Success(no)` → `deposit(accountId, amount, txId, no)` → `done`; `Cancelled` → `err_vnpay_cancelled`; `Failed` → `err_vnpay_failed`; `BadSignature`/`Mismatch` → `err_vnpay_bad_result`. Rút tiền không đổi.
  `VnPayFragment`: header có nút đóng + pill "sandbox" (mockup màn 8), WebView bật JavaScript và DOM storage; `shouldOverrideUrlLoading` gặp `vnPay.isReturnUrl(url)` → `setFragmentResult(REQUEST_KEY, bundleOf(KEY_QUERY to HashMap(VnPay.parseQuery(url))))` rồi pop, trả `true`. Đóng/back → `setFragmentResult(REQUEST_KEY, Bundle())` (không có `KEY_QUERY` = đã đóng). `MoneyMoveFragment` lắng nghe trên `parentFragmentManager` và gọi `onPaymentResult`/`onPaymentClosed`.

- [ ] **Step 1: Test fail** (tạo `VnPay(VnPayConfig("TESTCODE", "SECRET"))` thật; helper `signedReturn(txnRef, amount, code = "00", status = "00")` ký bằng `VnPay.sign`):
  - `depositOpensVnPayAfterAuth`: nạp 1.000.000, `confirm()`, `onAuthResult(true)` → `openPayment` có URL bắt đầu `VnPay.SANDBOX_URL`, `accounts.deposits` rỗng.
  - `successfulPaymentDepositsWithRef`: tiếp theo `onPaymentResult(signedReturn("vnp0", 1_000_000))` → `accounts.deposits.single() == ("chk" to 1_000_000L)`, giao dịch có `id == "vnp0"`, `paymentRef == "14000001"`, `done` có giá trị.
  - `paymentResultHandledOnce`: gọi `onPaymentResult` hai lần với cùng map → `deposits.size == 1`.
  - `badSignatureDoesNotDeposit`: map bị sửa `vnp_Amount` → `deposits` rỗng, `errorRes == R.string.err_vnpay_bad_result`.
  - `cancelledShowsMessage`: code `"24"` → `errorRes == R.string.err_vnpay_cancelled`, không nạp.
  - `closedWebViewClearsPending`: `onPaymentClosed()` rồi `onPaymentResult(valid)` → không nạp, `errorRes == R.string.err_vnpay_cancelled`.
  - `notConfiguredShowsError`: `payments = null` → sau `onAuthResult(true)`, `errorRes == R.string.err_vnpay_not_configured`, `openPayment.value == null`.
  - Sửa các test nạp cũ đang gọi `deposit` trực tiếp cho đúng luồng mới; test rút giữ nguyên.
- [ ] **Step 2: Chạy, FAIL.**
- [ ] **Step 3: Cài đặt** ViewModel, `VnPayFragment`, nối `MoneyMoveFragment` (`openPayment` → `screenHost.openScreen(VnPayFragment.newInstance(url))`).
- [ ] **Step 4: `./gradlew testDebugUnitTest assembleDebug lintDebug`, PASS.**
- [ ] **Step 5: Commit** — `git commit -m "feat: deposit through VNPay sandbox"`

---

### Task 10: Quét QR và Mã của tôi (cắt được)

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/QrPayload.kt`, `app/src/main/java/com/flux/bank/ui/customer/qr/QrFragment.kt`, `QrViewModel.kt` (nếu cần giữ danh sách tài khoản), `res/layout/fragment_qr.xml`
- Modify: `app/build.gradle.kts` (`com.google.mlkit:barcode-scanning:17.3.0`, `com.google.zxing:core:3.5.3`), `CustomerMainActivity.kt` (nút QR nổi mở `QrFragment`), `docs/licenses/` (thêm ghi chú ZXing Apache-2.0)
- Test: `app/src/test/java/com/flux/bank/domain/QrPayloadTest.kt`

**Interfaces:**
- Produces: `object QrPayload { const val PREFIX = "fluxbank:"; fun encode(accountNumber: String): String; fun decode(text: String): String? }` — `decode` trả số tài khoản khi chuỗi đúng `fluxbank:` + số FLUX hợp lệ (`TransferPolicy.isFluxAccountNumber`), ngược lại `null`.
- Consumes: `TransferFragment.newInstance(TransferMode.INTERNAL, TransferPrefill(accountNumber))` (Task 8).

- [ ] **Step 1: Test fail** — `encode("886812034471") == "fluxbank:886812034471"`; `decode` của nó trả `"886812034471"`; `decode("fluxbank:886712034471") == null`; `decode("https://example.com") == null`; `decode(" fluxbank:886812034471\n") == "886812034471"` (trim).
- [ ] **Step 2: Chạy, FAIL.** **Step 3: Cài đặt `QrPayload`.** **Step 4: Chạy, PASS.**
- [ ] **Step 5: Cài đặt `QrFragment`** theo mockup màn 9–10: segmented "Quét mã / Mã của tôi". Quét: xin quyền camera như `EkycCaptureFragment`, CameraX `Preview` + `ImageAnalysis` với ML Kit `BarcodeScanning` (chỉ `FORMAT_QR_CODE`); `decode` ra số → thay màn bằng `TransferFragment` (INTERNAL, prefill); chuỗi khác → Snackbar "Mã QR không phải của FLUX Bank" (một lần mỗi 2 giây). Mã của tôi: chip Checking/Saving, ZXing `QRCodeWriter` → `Bitmap` 512px, tên `TextNormalizer.holderName`, số tài khoản + nút sao chép (dùng lại cách của `AccountDetailFragment`).
- [ ] **Step 6: `./gradlew testDebugUnitTest assembleDebug lintDebug`, PASS; thử trên máy: quét mã trên màn hình máy khác ra form điền sẵn.**
- [ ] **Step 7: Commit** — `git commit -m "feat: scan and show FLUX QR codes"`

---

### Task 11: Tài liệu và kiểm tra cuối

**Files:**
- Modify: `README.md`, `HANDOFF.md`, `PROJECT_CONTEXT.md`

- [ ] **Step 1: README** — mục "Thiết lập Firebase": publish lại rules F4; mục mới "6. VNPay sandbox": đăng ký tại `https://sandbox.vnpayment.vn/devreg/`, thêm `VNPAY_TMN_CODE`/`VNPAY_HASH_SECRET` vào `local.properties`, thẻ test lấy ở trang demo của VNPay sandbox (ghi số thẻ NCB đúng như trang đó công bố tại thời điểm kiểm tra); mục 5 đổi "20 ca" thành số ca mới; "Luồng demo" thêm kịch bản F4 (spec §9); "Giới hạn đã biết" thêm: hạn mức ngày chỉ kiểm ở app, chữ ký VNPay kiểm trên máy và khoá sandbox nằm trong APK, rules không biết VNPay đã thu tiền, mục `accountIndex` chỉ có sau khi khách đăng nhập lần đầu, `TRANSFER_IN` không có số dư sau giao dịch.
- [ ] **Step 2: `PROJECT_CONTEXT.md`** — bảng dữ liệu thêm `accountIndex`, các trường mới của `transactions`; mục "Feature 4" tóm tắt như F3. **`HANDOFF.md`** — trạng thái, việc chủ dự án cần làm (VNPay sandbox, publish rules, demo §9), lỗi nhỏ còn lại từ review.
- [ ] **Step 3: Kiểm tra cuối** — `./gradlew testDebugUnitTest assembleDebug lintDebug` và rules test; Expected: tất cả pass, lint 0 error. `git grep '"demo"'` không ra code debug còn sót.
- [ ] **Step 4: Commit** — `git add README.md HANDOFF.md PROJECT_CONTEXT.md && git commit -m "docs: F4 transfers and VNPay setup"`
