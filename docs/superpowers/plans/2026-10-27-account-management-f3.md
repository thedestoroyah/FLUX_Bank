# Feature 3 — Account Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give each account type its own view (Checking/Saving balance, Saving rate and monthly interest, Mortgage installment monthly or every two weeks). Let customers deposit and withdraw under OTP/face auth, add a transaction history, and let officers edit rates and open Saving/Mortgage accounts.

**Architecture:** Same MVVM + service locator as F1–F2. All money math lives in one pure object (`InterestCalculator`). Deposits and withdrawals are a single Firestore `runTransaction` that writes the account (`balance`, `lastTxId`) and a new `transactions/{id}` doc together. Security Rules cross-check the two docs, so the balance can only change when a matching transaction record exists.

**Tech Stack:** Kotlin, XML + ViewBinding, Lifecycle/ViewModel/LiveData, Coroutines, Firebase Auth + Firestore (Spark), JUnit4 + `MainDispatcherRule` and fakes in `app/src/test/java/com/flux/bank/fakes/Fakes.kt`.

**Spec:** `docs/superpowers/specs/2026-10-27-account-management-f3-design.md` (§ numbers below refer to it). Approved mockup: `docs/superpowers/specs/mockups/2026-10-27-f3-accounts.html`. Screen numbers 1–7 match the mockup captions.

## Global Constraints

- Build/test from `FLUX_Bank/`: `./gradlew testDebugUnitTest`, `./gradlew assembleDebug lintDebug`. The Gradle daemon is pinned to JDK 21 by `gradle/gradle-daemon-jvm.properties`.
- Kotlin + XML/ViewBinding, no Compose. Light theme only. Only design tokens are allowed: `colors.xml`, `dimens.xml` `space_*`, `TextAppearance.Flux.*`, `Widget.Flux.*`. Amounts use the `Amount` appearance (`tnum`). Tiles use elevation 0 with a 1dp `#E5E7EB` stroke. Lists use RecyclerView. Touch targets are ≥ 48dp.
- Emerald is used only for interactive accents and incoming (+) amounts. Primary buttons are Obsidian.
- All visible text is Vietnamese and lives in `strings.xml`. Money is `Long` VND, formatted with `MoneyFormatter.formatVnd` / `formatSignedVnd`. Rates are `Double` %/year, shown with a Vietnamese decimal comma (`4,5%/năm`).
- Limits: amounts ≥ **10_000**. Deposits ≤ **100_000_000**. A withdrawal cannot exceed the balance. Rates must be in **(0, 30]**. Mortgage `termMonths` is **1..360** and `loanAmount > 0`. Default rates are **4.5** (saving) and **8.5** (mortgage).
- Every deposit/withdraw goes through `TransactionAuthSheet` (amount passed so ≥ 10_000_000 adds the face step). Only `kycStatus == VERIFIED` customers can deposit/withdraw.
- Comments and KDoc are in Vietnamese, identifiers in English. Follow TDD (failing test first). Commit after each task (`feat:`/`fix:`/`test:`/`docs:`). Use `git add <paths>`, never `git add -A` (do not commit `.idea/*`).
- Debug-only demo scaffolding (exported activities, sample data) must be reverted before commit: `git grep '"demo"'` must be empty.

## Review Focus

1. **Double submit.** Tapping "Tiếp tục" twice, or the auth result arriving twice, must produce exactly one deposit and one sheet. Pinned in Task 7 (`secondConfirmWhileSubmittingIsIgnored`).
2. **Balance changed since the preview.** Another device withdrew after the form showed "Số dư sau giao dịch". The repository's `InsufficientFundsException` must show a specific message and no receipt. Pinned in Task 7 (`insufficientFundsFromRepositoryShowsMessage`).
3. **Amount typing.** `"2.000.000"`, `"2000000"`, `""`, `"000"` and a 20-digit paste must all parse to a sane `Long` (digits only, capped at 12 digits) without crashing. Pinned in Task 1 (`parseAmountHandlesSeparatorsEmptyAndOverflow`).
4. **Rate typed with a decimal comma.** `"5,0"` and `"5.0"` must both mean 5.0. `"abc"`, `"0"` and `"30,01"` are rejected with a message. Pinned in Task 4 (`acceptsCommaDecimal`, `rejectsOutOfRange`).
5. **Day grouping at midnight.** A transaction at 23:59 yesterday is under "Hôm qua", one at 00:01 today is under "Hôm nay", using the device zone. Pinned in Task 8 (`groupsByLocalDay`).

---

### Task 1: Money math and amount rules

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/InterestCalculator.kt`
- Modify: `app/src/main/java/com/flux/bank/core/validation/Validators.kt`
- Test: `app/src/test/java/com/flux/bank/domain/InterestCalculatorTest.kt`, `app/src/test/java/com/flux/bank/core/ValidatorsTest.kt`

**Interfaces:**
- Produces (in `domain`):
  - `enum class PaymentFrequency { MONTHLY, BIWEEKLY }`
  - `object InterestCalculator` with:
    - `fun monthlySavingInterest(balance: Long, annualRatePercent: Double): Long`
    - `fun periods(termMonths: Int, frequency: PaymentFrequency): Int`
    - `fun installment(principal: Long, annualRatePercent: Double, termMonths: Int, frequency: PaymentFrequency): Long`
    - `fun totalInterest(principal: Long, annualRatePercent: Double, termMonths: Int, frequency: PaymentFrequency): Long`
    - `fun nextDueDate(startDate: LocalDate, frequency: PaymentFrequency, today: LocalDate): LocalDate`
- Produces (in `Validators`): `enum class AmountError { BELOW_MIN, ABOVE_DEPOSIT_MAX, INSUFFICIENT_FUNDS }`, `const val MIN_AMOUNT = 10_000L`, `const val MAX_DEPOSIT = 100_000_000L`, `fun amountError(isDeposit: Boolean, amount: Long, balance: Long): AmountError?`, `fun parseRate(input: String): Double?` (returns null when the input is not a number or is outside (0, 30]), `const val MAX_AMOUNT_DIGITS = 12`, `fun parseAmount(input: String): Long` (keeps digits only and the first 12 of them; empty input → 0).

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun savingInterestFloors() {
    assertEquals(450_000L, InterestCalculator.monthlySavingInterest(120_000_000, 4.5))
    assertEquals(416_666L, InterestCalculator.monthlySavingInterest(100_000_000, 5.0))
}
@Test fun monthlyInstallmentMatchesMockup() {
    assertEquals(13_017_349L, InterestCalculator.installment(1_500_000_000, 8.5, 240, MONTHLY))
    assertEquals(1_624_163_760L, InterestCalculator.totalInterest(1_500_000_000, 8.5, 240, MONTHLY))
}
@Test fun biweeklyInstallmentMatchesMockup() {
    assertEquals(520, InterestCalculator.periods(240, BIWEEKLY))
    assertEquals(6_003_662L, InterestCalculator.installment(1_500_000_000, 8.5, 240, BIWEEKLY))
    assertEquals(1_621_904_240L, InterestCalculator.totalInterest(1_500_000_000, 8.5, 240, BIWEEKLY))
}
@Test fun zeroRateSplitsEvenlyRoundingUp() {
    assertEquals(100_000L, InterestCalculator.installment(1_200_000, 0.0, 12, MONTHLY))
    assertEquals(100_001L, InterestCalculator.installment(1_200_001, 0.0, 12, MONTHLY))
}
@Test fun nextDueDate() {
    val start = LocalDate.of(2026, 10, 5); val today = LocalDate.of(2026, 10, 27)
    assertEquals(LocalDate.of(2026, 11, 5), InterestCalculator.nextDueDate(start, MONTHLY, today))
    assertEquals(LocalDate.of(2026, 11, 2), InterestCalculator.nextDueDate(start, BIWEEKLY, today))
    assertEquals(LocalDate.of(2026, 11, 5), InterestCalculator.nextDueDate(start, MONTHLY, LocalDate.of(2026, 11, 5))) // hạn đúng hôm nay vẫn tính là hôm nay
    assertEquals(LocalDate.of(2026, 2, 28), InterestCalculator.nextDueDate(LocalDate.of(2026, 1, 31), MONTHLY, LocalDate.of(2026, 2, 10)))
}
```
In `ValidatorsTest`: `amountError(true, 9_999, 0) == BELOW_MIN`, `amountError(true, 100_000_001, 0) == ABOVE_DEPOSIT_MAX`, `amountError(true, 100_000_000, 0) == null`, `amountError(false, 500_000, 499_999) == INSUFFICIENT_FUNDS`, `amountError(false, 500_000, 500_000) == null`. Add `acceptsCommaDecimal`: `parseRate("5,0") == 5.0`, `parseRate("5.0") == 5.0`, `parseRate(" 4,5 ") == 4.5`. Add `rejectsOutOfRange`: `parseRate("0")`, `parseRate("30,01")`, `parseRate("abc")` and `parseRate("")` are all null, and `parseRate("30") == 30.0`. Add `parseAmountHandlesSeparatorsEmptyAndOverflow`: `"2.000.000"` → 2_000_000, `"2000000"` → 2_000_000, `""` → 0, `"000"` → 0, `"9".repeat(20)` → 999_999_999_999.

- [ ] **Step 2: Run and confirm they fail.** Run `./gradlew testDebugUnitTest --tests '*InterestCalculatorTest*' --tests '*ValidatorsTest*'`. Expected: compilation failure (unresolved references).

- [ ] **Step 3: Implement.** Use the annuity formula `P·r / (1 − (1+r)^−n)` from spec §4, rounding up with `ceil`. For monthly periods, `r = rate/100/12` and `n = termMonths`. For biweekly, `r = rate/100/26` and `n = (termMonths * 26 / 12.0).roundToInt()`. When `rate == 0`, return `ceil(P / n)`. `totalInterest = installment × periods − principal`. `nextDueDate` returns the first `start.plusMonths(k)` (monthly, always from `start` so the 31st clamps instead of drifting) or `start.plusDays(14k)` with k ≥ 1 that is not before `today`. Saving interest is `floor(balance × rate / 100 / 12)`.

- [ ] **Step 4: Run the tests again.** Same command. Expected: PASS.

- [ ] **Step 5: Commit.** `git add app/src/main/java/com/flux/bank/domain/InterestCalculator.kt app/src/main/java/com/flux/bank/core/validation/Validators.kt app/src/test/java/com/flux/bank/domain/InterestCalculatorTest.kt app/src/test/java/com/flux/bank/core/ValidatorsTest.kt && git commit -m "feat: interest and installment math, amount and rate validation"`

---

### Task 2: Data layer — transactions, rates, account operations

**Files:**
- Create: `app/src/main/java/com/flux/bank/data/model/Transaction.kt`, `app/src/main/java/com/flux/bank/data/model/Rates.kt`, `app/src/main/java/com/flux/bank/data/repository/TransactionRepository.kt`, `app/src/main/java/com/flux/bank/data/repository/RatesRepository.kt`
- Modify: `app/src/main/java/com/flux/bank/data/model/BankAccount.kt`, `app/src/main/java/com/flux/bank/data/repository/AccountRepository.kt`, `app/src/main/java/com/flux/bank/FluxApp.kt`, `app/src/main/res/values/strings.xml` (`account_checking/saving/mortgage` → "Thanh toán", "Tiết kiệm", "Vay thế chấp")
- Test: `app/src/test/java/com/flux/bank/data/ModelDefaultsTest.kt`, `app/src/test/java/com/flux/bank/fakes/Fakes.kt`

**Interfaces:**
- Produces:
  - `enum class TransactionType { DEPOSIT, WITHDRAW }`. It has `val isIncoming: Boolean` (true for DEPOSIT). F4/F5 will add more values.
  - `data class Transaction(val id: String = "", val accountId: String = "", val ownerUid: String = "", val accountNumber: String = "", val type: TransactionType = TransactionType.DEPOSIT, val amount: Long = 0, val balanceAfter: Long = 0, val description: String = "", val createdAt: Long = System.currentTimeMillis())`
  - `data class Rates(val savingRate: Double = 4.5, val mortgageRate: Double = 8.5, val updatedAt: Long = 0, val updatedBy: String = "", val updatedByName: String = "")`
  - `BankAccount` gains `val lastTxId: String? = null`, `val loanAmount: Long = 0`, `val annualRate: Double = 0.0`, `val termMonths: Int = 0`, `val startDate: Long = 0`.
  - `class InsufficientFundsException : Exception()`, in `AccountRepository.kt`.
  - `AccountRepository` adds:
    - `suspend fun getAccount(accountId: String): Result<BankAccount>`
    - `suspend fun openSaving(ownerUid: String): Result<BankAccount>`
    - `suspend fun openMortgage(ownerUid: String, loanAmount: Long, termMonths: Int, annualRate: Double): Result<BankAccount>`
    - `suspend fun deposit(accountId: String, amount: Long): Result<Transaction>`
    - `suspend fun withdraw(accountId: String, amount: Long): Result<Transaction>`
  - `interface TransactionRepository { suspend fun listRecent(ownerUid: String, limit: Long = 100): Result<List<Transaction>> }`. Results are newest first.
  - `interface RatesRepository { suspend fun get(): Result<Rates>; suspend fun save(rates: Rates): Result<Unit> }`. `get()` returns `Rates()` when `config/rates` is missing.
  - `FluxApp.transactionRepository`, `FluxApp.ratesRepository`.
  - Fakes: `FakeAccountRepository` implements the new methods in memory. It records `deposits`/`withdrawals`, has `var failNext: Throwable?`, and throws `InsufficientFundsException` when a withdrawal exceeds the balance. Also `FakeTransactionRepository(val items: MutableList<Transaction>)` and `FakeRatesRepository(var rates: Rates = Rates())` with a `saved` list.

- [ ] **Step 1: Write a failing test.** In `ModelDefaultsTest`, add `transactionAndRatesHaveFirestoreDefaults`: `Transaction()` and `Rates()` construct, `Rates().savingRate == 4.5`, `Rates().mortgageRate == 8.5`, and `BankAccount().lastTxId == null`.
- [ ] **Step 2: Run it.** `./gradlew testDebugUnitTest --tests '*ModelDefaultsTest*'`. Expected: FAIL (unresolved).
- [ ] **Step 3: Implement the models, interfaces, Firestore implementations and fakes.**
  - Extract the existing `uniqueAccountNumber` + `doc.set` path into `private suspend fun create(account: BankAccount): BankAccount`, and reuse it for checking, saving and mortgage. Mortgage sets `balance = loanAmount` and `startDate = now`.
  - `deposit`/`withdraw` share `private suspend fun move(accountId, amount, type)`. It runs `db.runTransaction`: read the account, reject MORTGAGE with `IllegalStateException`, compute the new balance, throw `InsufficientFundsException` when it would go below 0, then `update(account, "balance", "lastTxId")` and `set(transactions.document(), tx)` with `id = doc.id` and `accountNumber` copied. If Firestore wraps the thrown exception, unwrap it so callers can match `InsufficientFundsException`.
  - `listRecent` uses `whereEqualTo("ownerUid") + orderBy("createdAt", DESC) + limit`.
  - `RatesRepository` reads and writes the document `config/rates`.
  - Collection names go in `companion` constants, like the existing repositories.
- [ ] **Step 4: Run the full suite.** `./gradlew testDebugUnitTest`. Expected: PASS (all old tests plus the new one).
- [ ] **Step 5: Commit.** `git commit -m "feat: transactions, rates and account operations in data layer"` (add only the listed paths).

---

### Task 3: Security Rules and README

**Files:**
- Modify: `firestore.rules`, `README.md`

**Interfaces:**
- Consumes: the field names from Task 2 (`balance`, `lastTxId`, `type`, `accountId`, `ownerUid`, `amount`, `balanceAfter`, `loanAmount`, `termMonths`, `savingRate`, `mortgageRate`).

- [ ] **Step 1: Write the rules exactly as spec §6 describes.**
  - `accounts` update: `isOfficer()` is never allowed to update; only the owner can, through `ownerMoneyMoveAllowed(accountId)`. That function binds `tx = getAfter(/transactions/$(request.resource.data.lastTxId)).data` and requires:
    - `!exists(/transactions/$(request.resource.data.lastTxId))`
    - `changedKeys().hasOnly(['balance','lastTxId'])`
    - `resource.data.type in ['CHECKING','SAVING']`
    - `tx.accountId == accountId`, `tx.ownerUid == request.auth.uid`, `tx.balanceAfter == request.resource.data.balance`
    - the DEPOSIT/WITHDRAW arithmetic (the WITHDRAW branch also requires `>= 0`).
  - `accounts` create (officer): `balance is int && balance >= 0`. For MORTGAGE, also `loanAmount is int && loanAmount > 0 && termMonths >= 1 && termMonths <= 360`.
  - `transactions/{txId}`:
    - read by owner or officer
    - create when `ownerUid == auth.uid`, `type in ['DEPOSIT','WITHDRAW']`, `amount is int && amount >= 10000`, DEPOSIT `amount <= 100000000`, and `getAfter(/accounts/$(request.resource.data.accountId)).data.lastTxId == txId`
    - update and delete never allowed.
  - `config/rates`: read when `signedIn()`. Write when `isOfficer()` and both rates are numbers in (0, 30].
- [ ] **Step 2: Update the README.**
  - Re-publish the rules.
  - Create the composite index `transactions`: `ownerUid` Ascending, `createdAt` Descending. Give both paths: Console → Firestore → Indexes, or the link printed in Logcat the first time the History tab loads.
  - Add Rules Playground checks, each with its expected result:
    - owner updates `balance` without a transaction → denied
    - customer writes `config/rates` → denied
    - customer creates a transaction whose account `lastTxId` differs → denied
    - a withdrawal leaving the balance at −1 → denied
  - Add the F3 demo script from spec §2.
- [ ] **Step 3: Verify.** Paste the rules into Console → Rules → the editor shows no syntax error (the owner publishes). Run the Playground checks after Tasks 4–7 have produced real documents.
- [ ] **Step 4: Commit.** `git add firestore.rules README.md && git commit -m "feat: rules for balance moves, transactions and rates"`

---

### Task 4: Officer — Rates tab (screen 6)

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/officer/rates/RatesViewModel.kt`, `RatesFragment.kt`, `EditRateSheet.kt`, plus layouts `fragment_rates.xml`, `sheet_edit_rate.xml`
- Modify: `OfficerMainActivity.fragmentFor()` (`RATES -> RatesFragment()`), `strings.xml`
- Test: `app/src/test/java/com/flux/bank/ui/officer/RatesViewModelTest.kt`

**Interfaces:**
- Consumes: `RatesRepository`, `UserRepository.getUser` (for the officer's name), `Validators.parseRate`, `InterestCalculator.monthlySavingInterest`.
- Produces:
  - `enum class RateKind { SAVING, MORTGAGE }`
  - `data class RatesState(val rates: Rates = Rates(), val loading: Boolean = true, @StringRes val errorRes: Int? = null, val saving: Boolean = false)`
  - `RatesViewModel(rates: RatesRepository, auth: AuthRepository, users: UserRepository, clock: () -> Long)` with:
    - `load()`
    - `fun examplePreview(kind: RateKind, input: String): Long?`: the monthly interest on 100.000.000 ₫ for SAVING, or the monthly installment for a 1 tỷ / 240-month loan for MORTGAGE. Null when the input is invalid.
    - `fun save(kind: RateKind, input: String)`
    - `val saved: LiveData<Event<Unit>>`

- [ ] **Step 1: Write failing tests.**
  - `loadShowsDefaultsWhenConfigMissing`: `Rates()` → 4.5 / 8.5.
  - `acceptsCommaDecimal`: `save(SAVING, "5,0")`. The fake stores `savingRate == 5.0`, `mortgageRate` is unchanged, `updatedBy == officer uid`, `updatedByName == officer fullName`, and `updatedAt == clock()`.
  - `rejectsOutOfRange`: `save(SAVING, "30,01")` and `save(MORTGAGE, "0")` set `errorRes == R.string.err_rate_range` and the fake's `saved` list is empty.
  - `previewUsesExampleAmount`: `examplePreview(SAVING, "5") == 416_666L` and `examplePreview(SAVING, "x") == null`.
- [ ] **Step 2: Run them.** `./gradlew testDebugUnitTest --tests '*RatesViewModelTest*'`. Expected: FAIL.
- [ ] **Step 3: Implement the ViewModel, then the UI per mockup screen 6.**
  - Two tiles. Tapping a tile opens `EditRateSheet` (`Widget.Flux.BottomSheet`, decimal input, live preview line, Save button).
  - Copy: "Áp dụng ngay cho mọi tài khoản tiết kiệm" / "Chỉ áp dụng cho khoản vay mở sau khi sửa", and "sửa bởi {name}, {dd/MM HH:mm}".
  - Format rates with `String.format(Locale("vi"), "%.2f")`, trim trailing zeros (`4,5`).
- [ ] **Step 4: Run the tests again, then `./gradlew assembleDebug`.** Expected: PASS and BUILD SUCCESSFUL.
- [ ] **Step 5: Commit.** `git commit -m "feat: officer rates tab"`

---

### Task 5: Officer — customer accounts and opening Saving/Mortgage (screen 7)

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/officer/customers/OpenAccountViewModel.kt`, `OpenAccountFragment.kt`, `fragment_open_account.xml`, `item_customer_account.xml`
- Modify: `CustomerDetailFragment.kt` + its layout. Replace the single account line (`tvAccountLabel/tvAccountBalance`) with an "Tài khoản" RecyclerView of all accounts (type label, `8868 …` number, balance, or "Dư nợ" for a mortgage) and a "Mở tài khoản" button that opens `OpenAccountFragment.newInstance(uid, name, phone)` and reloads on return. Also `strings.xml`.
- Test: `app/src/test/java/com/flux/bank/ui/officer/customers/OpenAccountViewModelTest.kt`

**Interfaces:**
- Consumes: `AccountRepository.openSaving/openMortgage`, `RatesRepository.get`, `InterestCalculator.installment`.
- Produces:
  - `data class OpenAccountState(val type: AccountType = AccountType.SAVING, val mortgageRate: Double = 8.5, val loanAmount: Long = 0, val termMonths: Int = 240, val monthly: Long? = null, val biweekly: Long? = null, @StringRes val errorRes: Int? = null, val submitting: Boolean = false)`
  - `OpenAccountViewModel(accounts, rates)` with:
    - `load()`
    - `selectType(AccountType)`
    - `setLoanAmount(input: String)` (uses `Validators.parseAmount` from Task 1)
    - `setTerm(months: Int)`
    - `submit(ownerUid: String)`
    - `val opened: LiveData<Event<BankAccount>>`

- [ ] **Step 1: Write failing tests.**
  - `openSavingCallsRepository`
  - `mortgagePreviewUsesPolicyRate`: rates 8.5, amount 1_500_000_000, term 240 → `monthly == 13_017_349`, `biweekly == 6_003_662`
  - `rejectsZeroAmountAndBadTerm`: amount 0 → `err_loan_amount`, term 0 or 361 → `err_loan_term`, no repository call
  - `secondSubmitIgnoredWhileSubmitting`
- [ ] **Step 2: Run them.** `./gradlew testDebugUnitTest --tests '*OpenAccountViewModelTest*'`. Expected: FAIL.
- [ ] **Step 3: Implement the ViewModel, then the UI per mockup screen 7.**
  - Segmented Saving/Mortgage control.
  - Term chips 12/60/120/240 plus a custom number field.
  - The rate row is read-only.
  - The primary button reads "Mở tài khoản tiết kiệm" or "Mở khoản vay".
- [ ] **Step 4: Run the tests again, then `assembleDebug`.** Expected: PASS and BUILD SUCCESSFUL.
- [ ] **Step 5: Commit.** `git commit -m "feat: officer opens saving and mortgage accounts"`

---

### Task 6: Customer — account detail for each type (screens 1, 2)

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/account/AccountDetailViewModel.kt`, `AccountDetailFragment.kt`, `fragment_account_detail.xml`, `item_transaction.xml`, `TransactionRowAdapter.kt` (reused by Task 8)
- Modify:
  - `HomeViewModel.AccountCardUi`: add `accountId: String` and `@StringRes balanceLabel: Int`. The label is `card_outstanding` ("Dư nợ") for MORTGAGE and the existing `card_available_balance` otherwise.
  - `AccountCardAdapter`: add `onCardClick: (String) -> Unit` and bind `tvBalanceLabel`.
  - `HomeFragment`: a card click calls `screenHost.openScreen(AccountDetailFragment.newInstance(accountId))`.
  - `strings.xml`.
- Test: `app/src/test/java/com/flux/bank/ui/customer/AccountDetailViewModelTest.kt`, extend `HomeViewModelTest`

**Interfaces:**
- Consumes: `AccountRepository.getAccount`, `RatesRepository.get`, `TransactionRepository.listRecent`, `InterestCalculator`, `PaymentFrequency`.
- Produces:
  - `data class SavingInfo(val ratePercent: Double, val monthlyInterest: Long, val yearlyInterest: Long)`
  - `data class MortgageInfo(val frequency: PaymentFrequency, val installment: Long, val periods: Int, val nextDue: LocalDate, val totalInterest: Long, val totalPayable: Long)`
  - `data class AccountDetailState(val account: BankAccount? = null, val saving: SavingInfo? = null, val mortgage: MortgageInfo? = null, val recent: List<Transaction> = emptyList(), val loading: Boolean = true)`
  - `AccountDetailViewModel(accounts, rates, transactions, auth, today: () -> LocalDate)` with `load(accountId)` and `setFrequency(PaymentFrequency)`
  - `TransactionRowAdapter(onClick: (Transaction) -> Unit)` with `submit(List<Transaction>)`. A row shows the type label, the subtitle `"{account label} · HH:mm"` or the date, and the signed amount (Emerald when incoming).

- [ ] **Step 1: Write failing tests.**
  - `savingShowsLiveRate`: balance 120_000_000, rates 4.5 → `SavingInfo(4.5, 450_000, 5_400_000)`. Changing the fake rate to 5.0 and calling `load()` again gives 500_000.
  - `checkingHasNoInterestBlock`: both `saving` and `mortgage` are null.
  - `mortgageToggle`: start 2026-10-05, today 2026-10-27. MONTHLY → 13_017_349 / 240 / 2026-11-05. BIWEEKLY → 6_003_662 / 520 / 2026-11-02 / totalInterest 1_621_904_240.
  - `recentFiltersByAccountAndTakesFive`
  - In `HomeViewModelTest`: `mortgageCardUsesOutstandingLabel`.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement the ViewModel, then the UI per mockup screens 1–2.**
  - Checking/Saving: Titanium card (with a rate pill for Saving), copy-number button (`ClipboardManager` plus a snackbar "Đã sao chép"), the Nạp/Rút buttons (wired in Task 7; until then they are disabled), the interest tile for Saving, and the recent list.
  - Mortgage: outstanding tile, segmented control, installment tile and key-value rows. No Nạp/Rút buttons.
  - Reload in `onResume` so balances refresh after returning from a deposit.
- [ ] **Step 4: Run the tests again, then `assembleDebug`.** Expected: PASS and BUILD SUCCESSFUL.
- [ ] **Step 5: Commit.** `git commit -m "feat: account detail views for checking, saving and mortgage"`

---

### Task 7: Customer — deposit / withdraw and receipt (screens 3, 4)

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/account/MoneyMoveViewModel.kt`, `MoneyMoveFragment.kt`, `ReceiptFragment.kt`, `fragment_money_move.xml`, `fragment_receipt.xml`
- Modify: `AccountDetailFragment` (enable the Nạp/Rút buttons), `HomeFragment` (utilities `NAP_TIEN` → `MoneyMoveFragment.newInstance(deposit = true)`, `RUT_TIEN` → `deposit = false`), `strings.xml`
- Test: `app/src/test/java/com/flux/bank/ui/customer/MoneyMoveViewModelTest.kt`

**Interfaces:**
- Consumes: `AccountRepository.listByOwner/deposit/withdraw`, `InsufficientFundsException`, `UserRepository.getUser` (kycStatus), `Validators.amountError/parseAmount`, `TransactionAuthSheet.show` / `KEY_VERIFIED`.
- Produces:
  - `data class MoneyMoveState(val deposit: Boolean, val accounts: List<BankAccount> = emptyList(), val selectedId: String? = null, val amount: Long = 0, val balanceAfter: Long? = null, val amountError: AmountError? = null, val kycBlocked: Boolean = false, val submitting: Boolean = false, @StringRes val errorRes: Int? = null)`. `accounts` holds only CHECKING/SAVING. `selectedId` defaults to the `preselectedAccountId` argument, or else to the first checking account.
  - `MoneyMoveViewModel(deposit: Boolean, accounts, users, auth)` with:
    - `load(preselectedAccountId: String?)`
    - `select(accountId)`
    - `setAmount(input: String)`
    - `fun confirm()`: validates, then emits `askAuth: LiveData<Event<Long>>` (the amount)
    - `fun onAuthResult(verified: Boolean)`
    - `val done: LiveData<Event<Transaction>>`
  - `ReceiptFragment.newInstance(tx: Transaction, accountLabel: String, viewOnly: Boolean)`. It stores the fields as bundle primitives. Its "Mã giao dịch" is `tx.id.take(10).uppercase()`.

- [ ] **Step 1: Write failing tests.**
  - `notVerifiedIsBlocked`: kyc PENDING → `kycBlocked == true` and `confirm()` emits nothing.
  - `withdrawOverBalanceShowsError`: balance 499_999, amount 500_000 → `amountError == INSUFFICIENT_FUNDS`, no `askAuth`.
  - `balanceAfterPreview`: deposit 2_000_000 into 120_000_000 → 122_000_000.
  - `verifiedAuthDepositsOnceAndEmitsReceipt`: `confirm()`, then `onAuthResult(true)` → one fake deposit with the right id and amount, and `done` emits.
  - `secondConfirmWhileSubmittingIsIgnored`: confirm, onAuthResult(true), confirm, onAuthResult(true) while the fake deposit is suspended → exactly one deposit.
  - `cancelledAuthResetsWithoutWriting`: onAuthResult(false) → no deposit, `submitting == false`.
  - `insufficientFundsFromRepositoryShowsMessage`: the fake throws `InsufficientFundsException` → `errorRes == R.string.err_insufficient_funds`, no `done`.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement the ViewModel, then the UI per mockup screens 3–4.**
  - Account chips show "{label} · {balance}".
  - The amount field uses a thousands-separator `TextWatcher`.
  - Quick chips: 100.000 / 500.000 / 1.000.000 / 5.000.000.
  - The source row reads "Thẻ liên kết (giả lập)" or "Nhận tiền mặt (giả lập)". The info note follows the mockup.
  - When `kycBlocked`, show an inline notice with a button that opens `EkycCaptureFragment` (same call as `HomeFragment.cardKyc`).
  - `askAuth` → `TransactionAuthSheet.show(childFragmentManager, AuthSummary(title, listOf("Tài khoản" to …, "Số tiền" to …), amount), REQ)`.
  - `done` → `parentFragmentManager.popBackStack()`, then open `ReceiptFragment` with `viewOnly = false`. Its buttons: "Về trang chủ" (`showTab(HOME)`) and "Xem lịch sử" (`showTab(HISTORY)`).
- [ ] **Step 4: Run the tests again, then `assembleDebug`.** Expected: PASS and BUILD SUCCESSFUL.
- [ ] **Step 5: Commit.** `git commit -m "feat: deposit and withdraw with OTP auth and receipt"`

---

### Task 8: History tab, home recent list, Tiết kiệm utility (screen 5)

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/history/HistoryViewModel.kt`, `HistoryFragment.kt`, `fragment_history.xml`, `item_history_header.xml`
- Modify:
  - `CustomerMainActivity.fragmentFor()`: `HISTORY -> HistoryFragment()`.
  - `HomeViewModel`/`HomeFragment`: `recent: List<Transaction>` holds the 3 newest and replaces `cardEmpty` when it is non-empty. Utility `TIET_KIEM` opens the first SAVING account's detail. When there is none, it opens `ComingSoonFragment.newInstance(..., messageRes = R.string.saving_none)` ("Liên hệ nhân viên để mở tài khoản tiết kiệm").
  - `ComingSoonFragment`: add an optional `@StringRes messageRes: Int? = null` that replaces the body text.
  - `strings.xml`.
- Test: `app/src/test/java/com/flux/bank/ui/customer/HistoryViewModelTest.kt`, extend `HomeViewModelTest`

**Interfaces:**
- Consumes: `TransactionRepository.listRecent`, `AccountRepository.listByOwner`, `TransactionRowAdapter` (Task 6), `ReceiptFragment.newInstance(..., viewOnly = true)` (Task 7).
- Produces:
  - `sealed class DayLabel { object Today; object Yesterday; data class On(val date: LocalDate) }`
  - `sealed class HistoryRow { data class Header(val day: DayLabel); data class Item(val tx: Transaction) }`
  - `HistoryViewModel(transactions, accounts, auth, zone: ZoneId, today: () -> LocalDate)` with `load()`, `filter(accountId: String?)`, `val rows: LiveData<List<HistoryRow>>` and `val chips: LiveData<List<BankAccount>>`. `chips` holds the accounts that have transactions.

- [ ] **Step 1: Write failing tests.**
  - `groupsByLocalDay`: zone `Asia/Ho_Chi_Minh`, today 2026-10-27. Transactions at 2026-10-27 00:01, 2026-10-26 23:59 and 2026-10-24 18:40 → rows `[Header(Today), Item, Header(Yesterday), Item, Header(On(2026-10-24)), Item]`.
  - `filterByAccount`
  - `emptyHistoryHasNoRows`
  - In `HomeViewModelTest`: `homeShowsThreeNewest`.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement the ViewModel, then the UI per mockup screen 5.**
  - Chips: "Tất cả" plus one per account label.
  - Headers use the `SectionLabel` style, with dates as `dd/MM/yyyy`.
  - Tapping a row opens the receipt in view-only mode.
  - The empty state uses the existing empty-state style with "Chưa có giao dịch nào".
  - Reload in `onResume`.
- [ ] **Step 4: Run the tests again, then `assembleDebug lintDebug`.** Expected: PASS, BUILD SUCCESSFUL, 0 lint errors.
- [ ] **Step 5: Commit.** `git commit -m "feat: transaction history tab and home recent list"`

---

### Task 9: Docs and on-device verification

**Files:**
- Modify: `PROJECT_CONTEXT.md` (Feature 3 section, `transactions` and `config/rates` in the data table), `HANDOFF.md` (status, new API for F4: `AccountRepository.deposit/withdraw` pattern, the `lastTxId` rules contract F4 must extend), `README.md` if Task 3 missed anything.

- [ ] **Step 1: Run the full check.** `./gradlew testDebugUnitTest assembleDebug lintDebug`. Expected: all pass, 0 lint errors. `git grep '"demo"'` is empty.
- [ ] **Step 2: Hand off to the owner.** Ask them to publish the rules and create the index (README). Then install on the phone: `adb install -r -g app/build/outputs/apk/debug/app-debug.apk`.
- [ ] **Step 3: Run the spec §2 demo on the device.**
  1. The officer opens a Saving and a 1,5 tỷ / 240-month Mortgage for a VERIFIED customer.
  2. The customer deposits 2.000.000 into Saving (OTP), then 10.000.000 (OTP + face).
  3. Withdraw more than the balance: blocked.
  4. The officer sets the saving rate to 5: the customer's detail shows 500.000 ₫/tháng for 120 tr.
  5. Toggle the mortgage between monthly and two-week.
  6. The History tab shows the deposits grouped by day.

  Run the README Playground checks. Record the results in HANDOFF.
- [ ] **Step 4: Commit.** `git add PROJECT_CONTEXT.md HANDOFF.md README.md && git commit -m "docs: feature 3 status and F4 handoff notes"`
