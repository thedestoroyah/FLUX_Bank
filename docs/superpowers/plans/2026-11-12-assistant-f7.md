# Feature 7 Trợ lý FLUX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ô "Trợ lý AI" mở màn chat: Gemini (Firebase AI Logic, gói Spark) trả lời tiếng Việt kèm 0–3 nút mở đúng màn của F3–F6; mất mạng / hết lượt / quá 20 s thì trả lời bằng từ khoá trên máy; nút "Xem giao dịch tháng N" mở Lịch sử lọc theo tháng.

**Architecture:** Phần thuần Kotlin ở `domain/assistant/` (action, parser, ngữ cảnh tài chính, chế độ cơ bản, system prompt) và `domain/MonthSummary.kt`, test bằng JUnit. `data/assistant/` có interface `AssistantClient` + bản Gemini mỏng. `AssistantViewModel` ghép lại; UI ở `ui/customer/assistant/`. Mở màn tiện ích dùng chung một hàm với trang chủ.

**Tech Stack:** Kotlin, XML + ViewBinding, MVVM (LiveData + coroutines), Firebase BoM 35.1.0 (`firebase-ai`), `org.json`, JUnit4 + `kotlinx-coroutines-test`.

**Spec:** [docs/superpowers/specs/2026-11-12-assistant-f7-design.md](../specs/2026-11-12-assistant-f7-design.md) · Mockup đã duyệt: [mockups/2026-11-12-f7-assistant.html](../specs/mockups/2026-11-12-f7-assistant.html)

## Global Constraints

- Không ghi Firestore, không đổi `firestore.rules`.
- Model: hằng số `GeminiAssistantClient.MODEL = "gemini-3.8-flash"`, backend `GenerativeBackend.googleAI()` (Gemini Developer API, gói Spark).
- Firebase BoM `35.1.0` (ra 2026-10-21); artifact không có `-ktx`.
- Câu hỏi tối đa **500** ký tự; gửi **10** tin gần nhất (cả khách lẫn bot) làm lịch sử; tối đa **3** action mỗi câu trả lời; chờ tối đa **20.000 ms**.
- Ngữ cảnh gửi cho Google **không chứa** số tài khoản, SĐT, CCCD, tên người nhận/người gửi.
- Bot không tự giao dịch và không điền sẵn form.
- Chữ giao diện (nhãn, header, chip gợi ý, placeholder) nằm trong `strings.xml`. **Ngoại lệ có chủ đích:** system prompt và câu trả lời của chế độ cơ bản là nội dung "do bot nói", viết thẳng trong `domain/assistant/` để test JVM được (giống câu trả lời Gemini không nằm trong `strings.xml`).
- Chỉ dùng token của design system (`colors.xml`, `dimens.xml`, `TextAppearance.Flux.*`, `Widget.Flux.*`); vùng chạm ≥ 48dp; không cứng mã màu trong layout.
- KDoc/comment tiếng Việt, tên code tiếng Anh. TDD. Commit `feat:`/`fix:`/`chore:`/`test:`/`docs:`. `git add` theo đường dẫn, không `-A`, không commit `.idea/*`.
- Lệnh test: `./gradlew testDebugUnitTest` (từ `FLUX_Bank/`). Một lớp: `./gradlew testDebugUnitTest --tests 'com.flux.bank.<Lớp>'`.

## Review Focus

1. Gemini trả JSON bọc trong ```` ```json ```` hoặc có khoảng trắng / action viết thường → vẫn parse được (Task 3 có test).
2. Khách mới chưa có giao dịch nào hỏi "tháng này tôi chi bao nhiêu" → ngữ cảnh ghi 0 ₫ / chưa có giao dịch, chế độ cơ bản nói "chưa có giao dịch nào trong tháng" thay vì "0 giao dịch" lủng củng (Task 4, 5).
3. Mạng treo không trả lời → sau 20 s ra câu trả lời chế độ cơ bản, nút gửi mở lại (Task 7, test thời gian ảo).
4. Bấm chip / gửi liên tục khi đang chờ → chỉ một yêu cầu bay đi (Task 7).
5. Giao dịch 23:30 ngày 30/11 giờ VN thuộc tháng 11 cả ở màn tháng lẫn ngữ cảnh (Task 2, 4).

---

### Task 1: Nâng Firebase BoM, thêm `firebase-ai`

**Files:**
- Modify: `app/build.gradle.kts:61-63`

- [ ] **Step 1:** Đổi `firebase-bom:33.4.0` → `35.1.0`; `firebase-auth-ktx` → `firebase-auth`; `firebase-firestore-ktx` → `firebase-firestore`; thêm `implementation("com.google.firebase:firebase-ai") // Trợ lý AI (F7), Gemini Developer API qua Firebase AI Logic`. Giữ comment cũ.
- [ ] **Step 2:** Chạy `./gradlew testDebugUnitTest assembleDebug lintDebug`. Expected: BUILD SUCCESSFUL, 340/340 test pass, lint 0 error. Nếu lỗi do API Firestore/Auth đổi giữa BoM 33 → 35, sửa tại chỗ (không đổi hành vi), ghi chú trong commit.
- [ ] **Step 3: Commit** `chore: bump Firebase BoM to 35.1.0 and add firebase-ai`

### Task 2: `MonthSummary` + Lịch sử lọc theo tháng (ViewModel)

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/MonthSummary.kt`
- Modify: `app/src/main/java/com/flux/bank/ui/customer/history/HistoryViewModel.kt`
- Test: `app/src/test/java/com/flux/bank/domain/MonthSummaryTest.kt`, `app/src/test/java/com/flux/bank/ui/customer/HistoryViewModelTest.kt`

**Interfaces:**
- Produces:
  - `data class MonthSummary(val month: YearMonth, val incoming: Long, val incomingCount: Int, val outgoing: Long, val outgoingCount: Int, val transactions: List<Transaction>)` — `transactions` là giao dịch của tháng, mới nhất trước.
  - `companion object { fun of(all: List<Transaction>, month: YearMonth, zone: ZoneId = TransferPolicy.VN_ZONE): MonthSummary }` — chiều tiền theo `TransactionType.isIncoming`.
  - `HistoryViewModel.restrictTo(month: YearMonth)` và `val monthTotals: LiveData<MonthSummary?>` (null khi không lọc tháng). Tổng tính trên danh sách **sau** lọc tài khoản.

- [ ] **Step 1: Viết test fail** `MonthSummaryTest`:
  - `countsOnlyThatMonthInVietnamTime`: giao dịch PAYMENT 2.434.700 lúc `2026-11-30 23:30` (VN), TRANSFER_IN 500.000 lúc `2026-11-10 14:31`, WITHDRAW 100.000 lúc `2026-12-01 00:05`, DEPOSIT 1.000.000 lúc `2026-10-31 23:59` → `of(all, YearMonth.of(2026, 11))` cho `incoming == 500_000`, `incomingCount == 1`, `outgoing == 2_434_700`, `outgoingCount == 1`, `transactions.map { it.id }` theo thứ tự mới trước.
  - `emptyMonth`: danh sách rỗng → các tổng bằng 0, `transactions` rỗng.
- [ ] **Step 2:** Chạy `--tests 'com.flux.bank.domain.MonthSummaryTest'` → FAIL (chưa có lớp).
- [ ] **Step 3:** Viết `MonthSummary` (KDoc tiếng Việt).
- [ ] **Step 4:** Thêm vào `HistoryViewModelTest`:
  - `restrictToMonthKeepsOnlyThatMonth`: giao dịch ngày 27/10 và 30/09 → `restrictTo(YearMonth.of(2026, 10))` → `rows` chỉ còn nhóm của 27/10; `monthTotals.value!!.month == YearMonth.of(2026, 10)`.
  - `monthTotalsFollowAccountFilter`: hai giao dịch tháng 10 ở `chk` (PAYMENT 300.000) và `sav` (DEPOSIT 200.000) → `filter("chk")` → `monthTotals.value!!.outgoing == 300_000`, `incoming == 0`.
  - `noMonthFilterMeansNoTotals`: không gọi `restrictTo` → `monthTotals.value == null`.
- [ ] **Step 5:** Chạy → FAIL. Viết `restrictTo` + `monthTotals` trong `publish()` (dùng `MonthSummary.of(filtered, month, zone)` với `zone` của ViewModel). Chạy lại cả hai lớp test → PASS.
- [ ] **Step 6: Commit** `feat: month summary and month filter for history`

### Task 3: `AssistantAction`, `AssistantReply`, parser

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/assistant/AssistantAction.kt`, `AssistantReply.kt` (cùng package `com.flux.bank.domain.assistant`)
- Test: `app/src/test/java/com/flux/bank/domain/assistant/AssistantReplyParserTest.kt`

**Interfaces:**
- Produces:
  - `enum class AssistantAction { TOPUP, BILL_ELECTRICITY, BILL_WATER, MONTH_HISTORY, TRANSFER, DEPOSIT, WITHDRAW, SAVING, FLIGHT, MOVIE, HOTEL, SHOP, BRANCH }`
  - `data class AssistantReply(val text: String, val actions: List<AssistantAction>, val basicMode: Boolean = false)`
  - `object AssistantReplyParser { const val MAX_ACTIONS = 3; fun parse(raw: String): AssistantReply }` — ném `IllegalArgumentException` khi không parse được hoặc `reply` rỗng.

- [ ] **Step 1: Viết test fail:**
  - `parsesReplyAndActions`: `{"reply":"Chạm bên dưới để nạp nhé.","actions":["TOPUP"]}` → `AssistantReply("Chạm bên dưới để nạp nhé.", listOf(TOPUP))`.
  - `dropsUnknownAndDuplicateActions`: `["TOPUP","LOAN_APPLY","TOPUP"]` → `[TOPUP]`.
  - `acceptsLowercaseAndSpaces`: `[" bill_water "]` → `[BILL_WATER]`.
  - `capsAtThree`: 5 action hợp lệ → 3 đầu tiên.
  - `stripsCodeFence`: chuỗi bọc ```` ```json\n{...}\n``` ```` → parse như thường.
  - `missingActionsMeansNone`: `{"reply":"Chào bạn"}` → `actions` rỗng.
  - `blankReplyThrows`, `notJsonThrows`: `assertThrows(IllegalArgumentException::class.java) { parse(...) }` với `{"reply":"  ","actions":[]}` và `xin chào`.
- [ ] **Step 2:** Chạy → FAIL.
- [ ] **Step 3:** Viết parser bằng `org.json.JSONObject` (bọc `JSONException` thành `IllegalArgumentException`).
- [ ] **Step 4:** Chạy → PASS.
- [ ] **Step 5: Commit** `feat: assistant actions and reply parser`

### Task 4: `FinanceSnapshot` (ngữ cảnh gửi kèm)

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/assistant/FinanceSnapshot.kt`
- Test: `app/src/test/java/com/flux/bank/domain/assistant/FinanceSnapshotTest.kt`

**Interfaces:**
- Consumes: `MonthSummary.of` (Task 2), `TransferPolicy.remainingToday`, `InterestCalculator.installment(..., PaymentFrequency.MONTHLY)`, `MoneyFormatter.formatVnd`.
- Produces:
  - `data class FinanceSnapshot(val today: LocalDate, val firstName: String, val accounts: List<BankAccount>, val rates: Rates?, val month: MonthSummary, val remainingToday: Long)`
  - `fun toPromptText(): String` — các dòng tiếng Việt (dưới); 5 giao dịch tiền ra lớn nhất của tháng.
  - `companion object { fun build(fullName: String, accounts: List<BankAccount>, history: List<Transaction>, rates: Rates?, now: Long): FinanceSnapshot; fun firstName(fullName: String): String; fun categoryLabel(tx: Transaction): String }`
  - `firstName`: từ cuối của họ tên đã trim ("Trần Thị Bình" → "Bình"; rỗng → "bạn").
  - `categoryLabel`: TRANSFER_OUT "chuyển đi", WITHDRAW "rút tiền", PAYMENT theo `service`: TIEN_DIEN "tiền điện", TIEN_NUOC "tiền nước", NAP_DT "nạp điện thoại", VE_MAY_BAY "vé máy bay", VE_PHIM "vé xem phim", KHACH_SAN "khách sạn", MUA_SAM "mua sắm", khác "thanh toán dịch vụ"; tiền vào: DEPOSIT "nạp tiền", TRANSFER_IN "nhận chuyển khoản". *(shortcut: trùng nghĩa với nhãn `Utility` trong strings.xml vì domain không đọc được resource; thêm tiện ích mới thì thêm cả ở đây.)*

Định dạng `toPromptText()` (số qua `MoneyFormatter.formatVnd`, ngày `dd/MM`):
```
Hôm nay: 12/11/2026 (giờ Việt Nam). Tên khách: Bình.
Tài khoản: Thanh toán 2.217.800 ₫; Tiết kiệm 1.500.000 ₫; Vay dư nợ 1.480.000.000 ₫, trả 13.017.349 ₫/tháng.
Lãi suất hiện hành: tiết kiệm 5%/năm, vay 8,5%/năm.
Tháng 11/2026: tiền vào 500.000 ₫ (1 giao dịch), tiền ra 4.282.200 ₫ (3 giao dịch).
Chi theo nhóm tháng này: tiền điện 2.434.700 ₫; mua sắm 1.490.000 ₫; tiền nước 357.500 ₫.
Hạn mức chuyển tiền và thanh toán còn lại hôm nay: 195.717.800 ₫.
Giao dịch tiền ra lớn nhất tháng này: 12/11 tiền điện 2.434.700 ₫; 10/11 mua sắm 1.490.000 ₫; 12/11 tiền nước 357.500 ₫.
```
Tháng chưa có giao dịch: thay ba dòng tháng bằng `Tháng 11/2026: chưa có giao dịch nào.`. `rates == null` thì bỏ dòng lãi suất.

- [ ] **Step 1: Viết test fail:**
  - `promptHasMonthTotalsAndGroups`: dựng dữ liệu đúng ví dụ trên (Checking 2.217.800, PAYMENT TIEN_DIEN/TIEN_NUOC/MUA_SAM, TRANSFER_IN 500.000 với `counterpartyName = "NGUYEN CUSTOMER"`, `accountNumber = "886810968242"`) → text chứa `"tiền ra 4.282.200 ₫ (3 giao dịch)"` và `"tiền điện 2.434.700 ₫"`.
  - `promptNeverLeaksIdentifiers`: cùng dữ liệu → text **không** chứa `"886810968242"`, `"NGUYEN"`.
  - `emptyMonthSaysNoTransactions`: không có giao dịch → chứa `"chưa có giao dịch nào"`.
  - `lateNightBelongsToMonth`: PAYMENT `2026-11-30 23:30` VN, `now` = `2026-11-30 23:45` VN → `month.outgoingCount == 1`.
  - `firstNameTakesLastWord`: `"Trần Thị Bình"` → `"Bình"`, `"  "` → `"bạn"`.
- [ ] **Step 2:** Chạy → FAIL. **Step 3:** Viết. **Step 4:** Chạy → PASS.
- [ ] **Step 5: Commit** `feat: finance snapshot for assistant context`

### Task 5: `AssistantFallback` (chế độ cơ bản)

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/assistant/AssistantFallback.kt`
- Test: `app/src/test/java/com/flux/bank/domain/assistant/AssistantFallbackTest.kt`

**Interfaces:**
- Consumes: `TextNormalizer.stripDiacritics`, `FinanceSnapshot` (nullable), `AssistantReply`.
- Produces: `object AssistantFallback { fun reply(question: String, snapshot: FinanceSnapshot?): AssistantReply }` — luôn `basicMode = true`.

Bảng từ khoá (so trên chữ thường đã bỏ dấu, theo thứ tự; lấy **mọi** action khớp, tối đa 3). Bảng tối thiểu, builder thêm được:

| Action | Cụm |
|---|---|
| TOPUP | `nap dt`, `nap dien thoai`, `the cao`, `nap card`, `nap the` |
| BILL_ELECTRICITY | `tien dien`, `hoa don dien`, `dien thang` |
| BILL_WATER | `tien nuoc`, `hoa don nuoc` |
| MONTH_HISTORY | `thang nay`, `chi tieu`, `tieu het`, `giao dich thang`, `lich su`, `sao ke` |
| TRANSFER | `chuyen tien`, `chuyen khoan`, `ck ` |
| DEPOSIT | `nap tien vao`, `nap tien tai khoan`, `vnpay` |
| WITHDRAW | `rut tien` |
| SAVING | `tiet kiem`, `lai suat` |
| FLIGHT / MOVIE / HOTEL / SHOP | `may bay`, `ve bay` / `xem phim`, `ve phim`, `rap` / `khach san`, `phong` / `mua sam`, `mua hang` |
| BRANCH | `chi nhanh`, `phong giao dich`, `atm`, `gan nhat` |

Câu trả lời (copy cố định):
- Có `MONTH_HISTORY` và `snapshot != null`: `"Tháng {M} bạn đã chi {outgoing} qua {n} giao dịch."` (tháng chưa có giao dịch: `"Tháng {M} bạn chưa có giao dịch nào."`). `snapshot == null`: `"Bạn xem giao dịch tháng này ở đây."`
- Khớp khác: `"Bạn có thể làm việc này ở đây."`
- Câu có 6 chữ số liền (`\b\d{6}\b`) hoặc `otp` / `mat khau`: `"Bạn đừng gửi mã OTP hay mật khẩu cho ai, kể cả mình. FLUX không bao giờ hỏi những mã này."`, không action.
- Không khớp: `"Mình chưa hiểu ý bạn. Bạn có thể thử:"` + `[TOPUP, BILL_ELECTRICITY, MONTH_HISTORY]`.

- [ ] **Step 1: Viết test fail:** `topupWithAndWithoutDiacritics` ("Nạp tiền điện thoại", "nap dt cho me" → `[TOPUP]`); `electricityBill` ("hoá đơn điện tháng này" → chứa `BILL_ELECTRICITY` và `MONTH_HISTORY`); `monthSpendUsesSnapshot` (snapshot tháng 11 tiền ra 4.282.200 / 3 → text `"Tháng 11 bạn đã chi 4.282.200 ₫ qua 3 giao dịch."`); `monthSpendEmptyMonth`; `otpWarningHasNoActions` ("mã OTP của tôi là 482915"); `unknownSuggestsThree` ("thời tiết hôm nay" → 3 action gợi ý); `alwaysBasicMode`.
- [ ] **Step 2:** FAIL. **Step 3:** Viết. **Step 4:** PASS.
- [ ] **Step 5: Commit** `feat: on-device keyword fallback for assistant`

### Task 6: System prompt + `AssistantClient` + Gemini

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/assistant/AssistantPrompt.kt`, `app/src/main/java/com/flux/bank/data/assistant/AssistantClient.kt`, `app/src/main/java/com/flux/bank/data/assistant/GeminiAssistantClient.kt`
- Modify: `app/src/main/java/com/flux/bank/FluxApp.kt` (thêm `val assistantClient: AssistantClient by lazy { GeminiAssistantClient() }`)
- Test: `app/src/test/java/com/flux/bank/domain/assistant/AssistantPromptTest.kt`

**Interfaces:**
- Produces:
  - `data class ChatTurn(val fromUser: Boolean, val text: String)` (trong `AssistantClient.kt`)
  - `interface AssistantClient { suspend fun ask(systemPrompt: String, turns: List<ChatTurn>): String }` — trả JSON thô.
  - `object AssistantPrompt { fun system(snapshot: FinanceSnapshot?): String }`
  - `class GeminiAssistantClient : AssistantClient` với `companion object { const val MODEL = "gemini-3.8-flash" }`: mỗi lần `ask` tạo `Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(MODEL, generationConfig { responseMimeType = "application/json"; responseSchema = Schema.obj(mapOf("reply" to Schema.string(), "actions" to Schema.array(Schema.enumeration(AssistantAction.entries.map { it.name })))) }, systemInstruction = content { text(systemPrompt) })` (system instruction cố định lúc tạo model nên tạo lại mỗi lượt), gọi `generateContent(*turns → content(role = "user"/"model"))`, trả `response.text` hoặc ném `IllegalStateException` khi null. Kiểm lại tên API theo `firebase-ai` 18.x khi biên dịch.

`system()` phải có đủ các ý của spec §5 (vai trò "Trợ lý FLUX", tiếng Việt ≤ 3 câu, xưng "mình"/"bạn", chỉ về FLUX và tài chính cá nhân, **không hỏi/nhận mật khẩu, OTP, số thẻ**, số liệu chỉ từ ngữ cảnh, không tự giao dịch, mô tả từng action bằng một dòng `TÊN: khi nào dùng`, trả JSON `{reply, actions}`), kiến thức cố định lấy từ hằng số: `Validators.MIN_AMOUNT` (10.000 ₫), `TransferPolicy.MAX_PER_TRANSFER`, `TransferPolicy.DAILY_LIMIT`, OTP 6 số (viết thẳng, `OtpManager` không có hằng) và `OtpManager.VALIDITY_MS / 1000` giây, `OtpManager.MAX_ATTEMPTS` lần sai thì khoá, ngưỡng quét mặt `FaceVerificationPolicy.HIGH_VALUE_THRESHOLD`, giờ mở cửa T2–T6 8–17h, T7 8–12h (viết thẳng; giữ khớp `OpeningHours`). Cuối prompt: `"Số liệu của khách:\n" + snapshot.toPromptText()` hoặc `"Số liệu của khách: chưa tải được số liệu, hãy nói là bạn chưa xem được."` khi null.

- [ ] **Step 1: Viết test fail** `AssistantPromptTest`: `mentionsEveryAction` (prompt chứa tên mọi `AssistantAction`); `usesPolicyNumbers` (chứa `"100.000.000 ₫"` và `"200.000.000 ₫"`); `forbidsSecrets` (chứa `"OTP"` và `"mật khẩu"`); `appendsSnapshot` (chứa `snapshot.toPromptText()`); `nullSnapshotSaysUnavailable` (chứa `"chưa tải được số liệu"`).
- [ ] **Step 2:** FAIL. **Step 3:** Viết `AssistantPrompt`, `AssistantClient`, `GeminiAssistantClient`, đăng ký trong `FluxApp`. **Step 4:** `./gradlew testDebugUnitTest assembleDebug` → PASS.
- [ ] **Step 5: Commit** `feat: assistant system prompt and Gemini client via Firebase AI Logic`

### Task 7: `AssistantViewModel`

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/assistant/AssistantViewModel.kt`
- Modify: `app/src/test/java/com/flux/bank/fakes/Fakes.kt` (thêm `FakeAssistantClient`)
- Test: `app/src/test/java/com/flux/bank/ui/customer/assistant/AssistantViewModelTest.kt`

**Interfaces:**
- Consumes: Task 3–6; `AuthRepository.currentUid()`, `UserRepository.getUser`, `AccountRepository.listByOwner`, `TransactionRepository.listRecent`, `RatesRepository.get`.
- Produces:
  - `sealed class ChatItem { data class User(val text: String); data class Bot(val reply: AssistantReply); object Typing }`
  - `class AssistantViewModel(client: AssistantClient = FluxApp.instance.assistantClient, auth = …, users = …, accounts = …, transactions = …, rates = …, now: () -> Long = System::currentTimeMillis, timeoutMs: Long = TIMEOUT_MS) : ViewModel()` với `companion object { const val TIMEOUT_MS = 20_000L; const val MAX_INPUT = 500; const val HISTORY_TURNS = 10 }`
  - `val items: LiveData<List<ChatItem>>`, `val busy: LiveData<Boolean>`, `val firstName: LiveData<String>` (tải trong `init`), `fun send(text: String)`, `val currentMonth: YearMonth` (theo `now` giờ VN, cho nhãn nút).
  - `FakeAssistantClient(var answer: suspend (String, List<ChatTurn>) -> String)` ghi lại `calls: MutableList<Pair<String, List<ChatTurn>>>`.

Luồng `send`: trim; rỗng hoặc `busy == true` → bỏ qua; cắt còn 500 ký tự; thêm `User` + `Typing`, `busy = true`; trong `viewModelScope`: dựng snapshot (lỗi bất kỳ → null), gọi `client.ask(AssistantPrompt.system(snapshot), turns)` trong `withTimeout(timeoutMs)` rồi `AssistantReplyParser.parse`; mọi lỗi (trừ khi scope bị huỷ) → `AssistantFallback.reply(text, snapshot)`; thay `Typing` bằng `Bot`, `busy = false`. `turns` = 10 tin cuối (User → `fromUser = true`, Bot → `reply.text`), kết thúc bằng câu vừa gửi.

- [ ] **Step 1: Viết test fail** (dùng `MainDispatcherRule`, `InstantTaskExecutorRule`, fakes hiện có):
  - `aiReplyAppearsWithActions`: client trả `{"reply":"Nạp ở đây.","actions":["TOPUP"]}` → `items` = `[User("nạp dt"), Bot(AssistantReply("Nạp ở đây.", [TOPUP]))]`, `busy == false`.
  - `clientErrorFallsBackToBasicMode`: client ném `IOException` → Bot có `basicMode == true` và `actions == [TOPUP]` cho "nạp dt".
  - `badJsonFallsBack`: client trả `"xin chào"` → `basicMode == true`.
  - `timeoutFallsBack`: `timeoutMs = 1_000`, client `awaitCancellation()`; dùng `runTest(main.dispatcher)` + `advanceTimeBy(1_001)` → Bot `basicMode == true`, `busy == false`.
  - `ignoresBlankAndWhileBusy`: client treo (`CompletableDeferred` chưa complete); `send("  ")` không thêm gì; `send("a")` rồi `send("b")` → `client.calls.size == 1`, `items` có đúng 1 `User`.
  - `sendsOnlyLastTenTurns`: 6 lượt hỏi đáp liên tiếp → lần gọi cuối có `turns.size == 10`, `turns.last() == ChatTurn(true, "câu 6")`.
  - `capsInputAt500`: gửi chuỗi 600 ký tự → `User.text.length == 500`.
  - `snapshotSentWithoutAccountNumber`: tài khoản `accountNumber = "886810968242"` → `client.calls[0].first` không chứa số đó và chứa `"Thanh toán"`.
  - `firstNameLoaded`: user `fullName = "Trần Thị Bình"` → `firstName.value == "Bình"`.
- [ ] **Step 2:** FAIL. **Step 3:** Viết ViewModel + `FakeAssistantClient`. **Step 4:** PASS (cả bộ `testDebugUnitTest`).
- [ ] **Step 5: Commit** `feat: assistant view model with timeout and basic-mode fallback`

### Task 8: Mở màn dùng chung + màn "Giao dịch tháng"

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/UtilityScreens.kt`
- Modify: `app/src/main/java/com/flux/bank/ui/customer/home/HomeFragment.kt:167-186`, `app/src/main/java/com/flux/bank/ui/customer/history/HistoryFragment.kt`, `app/src/main/res/layout/fragment_history.xml`, `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `HistoryViewModel.restrictTo`, `monthTotals` (Task 2).
- Produces:
  - `object UtilityScreens { fun screenFor(utility: Utility, savingAccountId: String?): Fragment }` — chuyển nguyên `when` của `HomeFragment.openUtility` sang đây (TRO_LY_AI tạm vẫn `ComingSoonFragment` tới Task 9). `HomeFragment.openUtility` chỉ còn `host().openScreen(UtilityScreens.screenFor(utility, viewModel.firstAccountId(AccountType.SAVING)))`.
  - `HistoryFragment.forMonth(month: YearMonth): HistoryFragment` (args `"month"` = `month.toString()`).

- [ ] **Step 1:** Tách `UtilityScreens`, sửa `HomeFragment`. Chạy `testDebugUnitTest assembleDebug` → PASS (hành vi trang chủ không đổi).
- [ ] **Step 2:** `fragment_history.xml`: thêm `<include android:id="@+id/header" layout="@layout/view_screen_header" android:visibility="gone"/>` trên tiêu đề và một tile tổng (mockup khung 4: hai cột "Tiền vào" `+500.000 ₫` màu tiền vào / "Tiền ra", dòng phụ "N giao dịch"; `Widget.Flux.Card`, `visibility="gone"`). Ở chế độ tháng: hiện header (tiêu đề `history_month_title` = "Giao dịch tháng %1$d/%2$d", back = `popBackStack()`), ẩn tiêu đề tab, hiện tile, gọi `viewModel.restrictTo(month)` trước `load()`. Chuỗi mới: `history_month_title`, `history_month_in`, `history_month_out`, `history_month_count` (plurals không cần — tiếng Việt: "%d giao dịch").
- [ ] **Step 3:** `assembleDebug lintDebug` → PASS, 0 error.
- [ ] **Step 4: Commit** `refactor: share utility screen routing; feat: month history screen`

### Task 9: Màn chat

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/assistant/AssistantFragment.kt`, `ChatAdapter.kt`, `AssistantActionUi.kt`; `app/src/main/res/layout/fragment_assistant.xml`, `item_chat_user.xml`, `item_chat_bot.xml`, `item_chat_action.xml`, `item_chat_typing.xml`; `app/src/main/res/drawable/ic_arrow_up.xml` (Phosphor `arrow-up` regular, MIT), drawable nền bong bóng nếu cần
- Modify: `UtilityScreens.kt` (TRO_LY_AI → `AssistantFragment()`), `strings.xml`
- Test: `app/src/test/java/com/flux/bank/ui/customer/assistant/AssistantActionUiTest.kt`

**Interfaces:**
- Consumes: `AssistantViewModel` (Task 7), `UtilityScreens.screenFor`, `HistoryFragment.forMonth` (Task 8), `TransferFragment.newInstance(TransferMode.INTERNAL)`.
- Produces: `object AssistantActionUi { fun utilityFor(action: AssistantAction): Utility? }` — TOPUP→NAP_DT, BILL_ELECTRICITY→TIEN_DIEN, BILL_WATER→TIEN_NUOC, DEPOSIT→NAP_TIEN, WITHDRAW→RUT_TIEN, SAVING→TIET_KIEM, FLIGHT→VE_MAY_BAY, MOVIE→VE_PHIM, HOTEL→KHACH_SAN, SHOP→MUA_SAM, BRANCH→CHI_NHANH; TRANSFER, MONTH_HISTORY → null. Nhãn/icon/màu nút lấy từ `Utility` khi có; TRANSFER dùng `ic_arrows_swap` + `cat_emerald`, nhãn `tab_transfer`; MONTH_HISTORY dùng `ic_history` + `cat_emerald`, nhãn `assistant_action_month` = "Xem giao dịch tháng %d".

- [ ] **Step 1: Viết test fail** `AssistantActionUiTest.everyActionHasADestination`: mọi `AssistantAction` trừ TRANSFER/MONTH_HISTORY có `utilityFor != null`, và không action nào map tới `Utility.TRO_LY_AI`. FAIL → viết `AssistantActionUi` → PASS.
- [ ] **Step 2:** Layout theo mockup đã duyệt:
  - `fragment_assistant.xml`: `view_screen_header` (tiêu đề `assistant_title` = "Trợ lý FLUX", thêm dòng phụ `assistant_subtitle` = "Trả lời bởi AI, có thể chưa chính xác" dưới tiêu đề — một TextView `Caption`/`text_secondary` ngay dưới header nếu include không có chỗ); RecyclerView `stackFromEnd`; khối trạng thái trống (tiêu đề "Chào %s" có icon `ic_sparkle` màu `cat_violet` cùng hàng, mô tả `assistant_intro`, dòng nhắc `assistant_safety` = "Không nhập mật khẩu hay mã OTP vào đây." có `ic_lock`, nhãn "Gợi ý", 4 chip: `assistant_suggest_topup` "Nạp tiền điện thoại", `assistant_suggest_electricity` "Thanh toán tiền điện", `assistant_suggest_month` "Tháng này tôi chi bao nhiêu?", `assistant_suggest_branch` "Chi nhánh gần nhất"); thanh nhập đáy (`Widget.Flux.TextField`, hint `assistant_hint` = "Hỏi Trợ lý FLUX…", `maxLength` 500, `imeOptions="actionSend"`; nút gửi tròn 48dp, nền obsidian mờ 35% khi rỗng/đang chờ, emerald khi có chữ). Gốc gọi `padAboveKeyboard()`.
  - Bong bóng khách: phải, nền `obsidian`, chữ trắng. Bong bóng bot: trái, avatar 26dp nền `cat_violet_bg` + `ic_sparkle`, nền trắng viền 1dp hairline; dưới đó nhãn "Chế độ cơ bản" (`assistant_basic_mode`, nền `warning_bg`, chữ `warning_dark`, icon `ic_wifi_slash`) khi `basicMode`; khung nút gộp (mỗi dòng `item_chat_action`: icon tile duotone + nhãn + `ic_caret_right`, cao ≥ 48dp).
  - Typing: bong bóng bot ba chấm (tôn trọng "giảm chuyển động": không animate khi `Settings.Global.ANIMATOR_DURATION_SCALE == 0`).
- [ ] **Step 3:** `AssistantFragment`: quan sát `items` (ẩn khối trống khi có tin), `busy` (khoá nút gửi + chip), `firstName`; chạm chip = `send(chữ chip)`; chạm nút: `utilityFor(action)?.let { screenHost.openScreen(UtilityScreens.screenFor(it, savingId)) }`, TRANSFER → `openScreen(TransferFragment.newInstance(TransferMode.INTERNAL))`, MONTH_HISTORY → `openScreen(HistoryFragment.forMonth(viewModel.currentMonth))`. `savingId` lấy từ ViewModel (thêm `fun savingAccountId(): String?` dùng danh sách tài khoản đã tải cho snapshot; chưa có → null, `screenFor` đã xử lý). ViewModel thuộc fragment nên back từ màn đích vẫn còn cuộc trò chuyện.
- [ ] **Step 4:** `./gradlew testDebugUnitTest assembleDebug lintDebug` → PASS, lint 0 error.
- [ ] **Step 5: Commit** `feat: assistant chat screen`

### Task 10: Bật trên Console, chạy trên máy thật, tài liệu

**Files:**
- Modify: `README.md` (mục Console: Firebase → AI Logic → Get started → Gemini Developer API, không cần thẻ), `PROJECT_CONTEXT.md` (mục Feature 7, giới hạn đã biết, số test), `HANDOFF.md`, `docs/licenses/` (ghi `ic_arrow_up` vào file Phosphor nếu file liệt kê icon)

- [ ] **Step 1:** Nhờ chủ dự án bật AI Logic trên Console (không cần thẻ). Không có thì màn chat chỉ chạy chế độ cơ bản: vẫn thử được bước 5 dưới.
- [ ] **Step 2:** Cài lên Xiaomi (adb chạy ngoài sandbox), chạy đúng kịch bản demo spec §8: chip "Nạp tiền điện thoại" → nút → màn Nạp ĐT → back còn chat; "tháng này mình tiêu hết bao nhiêu rồi" → số khớp tab Lịch sử + nút mở màn tháng (tổng vào/ra đúng); "hoá đơn điện"; "mã OTP của tôi là 123456" → nhắc không chia sẻ; chế độ máy bay → "chuyển tiền" → nhãn "Chế độ cơ bản" + nút mở form chuyển. Ghi kết quả vào HANDOFF.
- [ ] **Step 3:** Cập nhật README / PROJECT_CONTEXT / HANDOFF (giới hạn: free tier có hạn lượt, chưa App Check, nội dung chat tới Google, bot không điền form).
- [ ] **Step 4: Commit** `docs: F7 assistant setup, device run, handoff`
