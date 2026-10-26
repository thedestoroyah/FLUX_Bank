# UI Foundation + Feature 1 & 2 Rework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild FLUX Bank's UI on an extended design system with customer/officer tab shells, and complete Feature 1 (profiles) and Feature 2 (eKYC, biometric lock, OTP, face verification) per the approved spec.

**Architecture:** One main Activity per role hosting tab Fragments. Every screen goes through a ViewModel, and every ViewModel goes through repository interfaces that have Firestore implementations. Pure logic (validation, money, OTP, lock policy, face math) lives in Android-free classes with JUnit tests. A tiny service locator in `FluxApp` wires everything together; there is no DI library.

**Tech Stack:** Kotlin, XML + ViewBinding, Material 1.14, ConstraintLayout, RecyclerView, Lifecycle/ViewModel/LiveData, Coroutines, Firebase Auth + Firestore (Spark), AndroidX Biometric, ML Kit Face Detection (bundled), LiteRT/TFLite + MobileFaceNet, CameraX.

**Spec:** `docs/superpowers/specs/2026-10-26-ui-foundation-f1-f2-design.md` (read it before any task; section numbers below refer to it). Approved home + OTP mockup: `docs/superpowers/specs/mockups/2026-10-26-home-and-otp-sheet.html`.

## Global Constraints

- Build/test command (the system JDK is required; the VSCode JRE lacks `jlink`): `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew testDebugUnitTest` and `... ./gradlew assembleDebug`, run from `FLUX_Bank/`.
- minSdk 26, targetSdk 37, package `com.flux.bank`. Keep XML + ViewBinding; no Compose.
- Firebase **Spark only**: Auth (email/password) + Firestore. No Storage, no Cloud Functions. Images are stored as Base64 in Firestore.
- Every new library must be free, on-device, and Apache/MIT/OFL licensed.
- **Light theme only**. Delete `res/values-night/`. Base the theme on `Theme.Material3.Light.NoActionBar`.
- All user-visible text is **Vietnamese** and lives in `res/values/strings.xml`. No hardcoded strings in layouts or code.
- Money is `Long` VND, displayed as `48.250.000 ₫` (dot thousands separator, space, `₫`).
- Colors come only from `colors.xml` tokens. Text uses only the `TextAppearance.Flux.*` styles; minimum 12sp; Inter font; amounts use `fontFeatureSettings="tnum"`.
- Spacing comes only from `dimens.xml` (`space_4`…`space_32`). Tiles use elevation 0 with a 1dp `#E5E7EB` stroke. Only the Titanium card and bottom sheets have a shadow.
- Emerald `#10B981` is used **only** on interactive accents: center QR button, selected tab, card page indicator, incoming amounts, active OTP cell, and focus strokes. Primary action buttons are Obsidian `#0A0D12`.
- Touch targets are ≥ 48dp. Lists use RecyclerView.
- High-value threshold: amounts **≥ 10_000_000 VND** require the face step.
- OTP: 6 digits, `SecureRandom`, valid 120 s, single use, bound to one transaction id, max 5 wrong attempts.
- Lock: on cold start, and on resume after **≥ 60_000 ms** in background, only when biometrics are enabled and a Firebase session exists. Applies to both roles.
- Firestore boolean fields have no `is` prefix (`biometricEnabled`, `mustChangePassword`).
- Comments and KDoc are written in Vietnamese. Identifiers are in English.
- Commit after each task with a conventional message (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).
- If time runs short, defer in this order (spec §5.5): skeleton loading + haptics → officer-assisted eKYC → blink check. Never cut MobileFaceNet matching.
- **No UI layout work (Tasks 8–16) may start until Task 1's mockups are approved by the owner.** Tasks 2–7 may run before approval.

## Review Focus

1. **OTP edge cases:** a code entered after 120 s, a code from a different transaction, the 6th attempt after 5 wrong ones, and a resend (the old code must stop working). Each is rejected with its own Vietnamese message. Pinned in Task 4.
2. **Duplicate phone or CCCD:** an officer creating or editing a customer, or a customer changing their phone, to a number another customer already owns. This must be rejected before any write. The customer's own current number is not a duplicate. Pinned in Tasks 3 and 6.
3. **High-value boundary and missing eKYC:** exactly 10.000.000 ₫ requires the face step (9.999.999 does not). A customer who is not VERIFIED, or has no embedding, gets a clear block instead of a crash. Pinned in Task 12.
4. **Partial failure while onboarding a customer:** the Auth user is created but the Firestore profile or account write fails. The orphan Auth user must be deleted, the error shown, and the officer must still be signed in. Pinned in Task 6.
5. **Notifications denied (Android 13+) or no biometric hardware:** the OTP must still reach the user through an in-app simulated-SMS banner. Without enrolled biometrics, the toggle is disabled and the lock never engages, so the user is never locked out. Pinned in Tasks 4 and 8.

---

### Task 1: Mockups for remaining screens — owner approval gate

**Files:**
- Create: `docs/superpowers/specs/mockups/2026-10-26-screens-*.html` (one file per group below, standalone HTML with `<!doctype html>`, reusing the CSS/SVG-symbol approach of `2026-10-26-home-and-otp-sheet.html`)

Groups (one HTML page each, in the visual companion, using phone frames at 310×660 with the approved tokens):
1. `auth`: Login, Unlock (biometric lock screen), First-login change password.
2. `customer-profile`: Cá nhân tab (profile header, info rows, "Bảo mật" section: biometric toggle, "Kiểm tra xác thực khuôn mặt", "Đổi mật khẩu", "Đăng xuất"), Edit profile (editable: SĐT, địa chỉ, avatar; read-only: họ tên, CCCD, email), Coming-soon screen.
3. `ekyc`: Customer eKYC status + capture (CCCD + selfie, rejected-reason banner), Face-scan step inside the bottom sheet (camera circle, "Hãy chớp mắt", progress, match result).
4. `officer`: Tổng quan tab, Khách hàng tab (search + list), Create customer form, Customer detail/edit (with "Gửi email đặt lại mật khẩu", "Chụp eKYC hộ khách"), eKYC review (CCCD + selfie side by side, approve/reject, reject-reason picker sheet).

- [ ] **Step 1:** Restart the companion if it has stopped: `bash /home/thedestoroyah/.claude/plugins/cache/claude-plugins-official/superpowers/7.0.0/skills/brainstorming/scripts/start-server.sh --project-dir "$(pwd)" --open`, then push group 1.
- [ ] **Step 2:** Iterate on owner feedback until the owner explicitly approves each group. Push groups 2–4 in order. Use `-v2`, `-v3` filenames for revisions.
- [ ] **Step 3:** Copy each approved fragment into `docs/superpowers/specs/mockups/2026-10-26-screens-<group>.html` as a standalone document. Add a "Mockup đã duyệt" list to the spec, under §4, linking the files.
- [ ] **Step 4: Commit** `git add docs && git commit -m "docs: approved mockups for remaining screens"`

**Gate:** do not start Task 8 until all four groups are approved.

---

### Task 2: Build setup, app class, and design-system resources

**Files:**
- Modify: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `res/values/colors.xml`, `res/values/themes.xml`
- Create: `app/src/main/java/com/flux/bank/FluxApp.kt`, `res/values/dimens.xml`, `res/values/styles.xml`, `res/values/type.xml`, `res/font/inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`, `res/font/inter.xml`, `res/drawable/ic_*.xml` (icons listed below), `res/drawable/bg_home_mint.xml`, `res/drawable/bg_icon_tile.xml`
- Delete: `res/values-night/themes.xml`, `MainActivity.kt`, `res/layout/activity_main.xml`, `bg_header_gradient.xml`, `bg_card_gradient.xml`
- Test: build only

**Interfaces:**
- Produces: `FluxApp` (registered as `android:name=".FluxApp"`), exposing `lateinit` properties filled in by later tasks. Resource names that every later layout uses:
  - Colors (add): `flux_mint #E7F8EF`, `flux_emerald_dark #059669`, `cat_emerald #10B981`, `cat_gold #D97706`, `cat_blue #0071E3`, `cat_sky #0EA5E9`, `cat_rose #E11D48`, `cat_violet #7C3AED`, `flux_scrim #8C0A0D12`.
  - Dimens: `space_4/8/12/16/24/32`, `radius_tile 20dp`, `radius_card 24dp`, `radius_chip 14dp`, `touch_min 48dp`, `icon_tile 48dp`.
  - Text appearances: `TextAppearance.Flux.Display` (28sp bold), `.TitleL` (20sp semibold), `.TitleM` (16sp semibold), `.Body` (14sp regular), `.Label` (13sp medium), `.Caption` (12sp regular, `flux_text_secondary`), `.Amount` (Display + `tnum`).
  - Widget styles: `Widget.Flux.Card` (MaterialCardView, white, stroke 1dp hairline, radius_tile, elevation 0), `Widget.Flux.Button.Primary` (Obsidian, 52dp, radius 14dp), `Widget.Flux.Button.Secondary` (outlined), `Widget.Flux.TextField` (OutlinedBox, radius 14dp, focus stroke emerald), `Widget.Flux.IconTile` (48dp, radius_chip), `Widget.Flux.StatusPill`, `ThemeOverlay.Flux.BottomSheet` (top radius 24dp, scrim `flux_scrim`).
  - Icons (Phosphor duotone, MIT, converted to VectorDrawable, black, tinted per use via `app:tint`): `ic_plus, ic_arrow_down, ic_lightning, ic_drop, ic_phone, ic_airplane, ic_film, ic_bed, ic_bag, ic_piggy, ic_map_pin, ic_sparkle, ic_house, ic_arrows_swap, ic_scan, ic_list, ic_user, ic_bell, ic_eye, ic_eye_slash, ic_shield_check, ic_backspace`.
- Dependencies to add (inline in `build.gradle.kts`, the same style as the existing Firebase lines): `androidx.fragment:fragment-ktx:1.8.5`, `androidx.recyclerview:recyclerview:1.3.2`, `androidx.lifecycle:lifecycle-process:2.8.6`, `com.google.mlkit:face-detection:16.1.7`, CameraX `camera-core/camera-camera2/camera-lifecycle/camera-view:1.4.1`, LiteRT (`com.google.ai.edge.litert:litert`, latest stable with 16 KB page-size support; verify the version on Maven Central). Test: `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1`, `androidx.arch.core:core-testing:2.2.0`. Remove the unused `glide`, `retrofit*`, `okhttp*`, and `play-services-maps/location` lines (later features re-add them when needed).

- [ ] **Step 1:** Add the dependencies and `FluxApp`, register it in the manifest, delete `MainActivity` and its manifest entry.
- [ ] **Step 2:** Add the fonts (Inter from Google Fonts, OFL; copy `OFL.txt` to `docs/licenses/`), colors, dimens, type, styles, and icons. Switch the theme parent to `Theme.Material3.Light.NoActionBar` with `fontFamily=@font/inter`. Delete `values-night`.
- [ ] **Step 3:** Run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleDebug`. Expected: `BUILD SUCCESSFUL`. Old layouts may still reference removed drawables; point them at the new ones or leave them, because Tasks 8–16 replace them.
- [ ] **Step 4: Commit** `chore: design-system resources, FluxApp, dependency cleanup`

---

### Task 3: Money formatting and input validators

**Files:**
- Create: `app/src/main/java/com/flux/bank/core/format/MoneyFormatter.kt`, `core/validation/Validators.kt`
- Test: `app/src/test/java/com/flux/bank/core/MoneyFormatterTest.kt`, `ValidatorsTest.kt`

**Interfaces:**
- Produces:
  - `object MoneyFormatter { fun formatVnd(amount: Long): String; fun formatSignedVnd(amount: Long): String }`. Signed output is `+2.000.000` / `−55.000` (U+2212), with no currency suffix, for list rows.
  - `object Validators { fun isValidEmail(s: String): Boolean; fun normalizePhone(s: String): String; fun isValidVnPhone(s: String): Boolean; fun isValidNationalId(s: String): Boolean; fun isStrongEnoughPassword(s: String): Boolean }`

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun formatsVnd() {
    assertEquals("48.250.000 ₫", MoneyFormatter.formatVnd(48_250_000))
    assertEquals("0 ₫", MoneyFormatter.formatVnd(0))
    assertEquals("+2.000.000", MoneyFormatter.formatSignedVnd(2_000_000))
    assertEquals("−55.000", MoneyFormatter.formatSignedVnd(-55_000))
}
@Test fun validatesVnPhone() {
    assertEquals("0912345678", Validators.normalizePhone(" 0912 345 678 "))
    assertTrue(Validators.isValidVnPhone("0912345678"))
    assertFalse(Validators.isValidVnPhone("912345678"))      // thiếu số 0
    assertFalse(Validators.isValidVnPhone("09123456789"))    // 11 số
    assertFalse(Validators.isValidVnPhone("09123a5678"))
}
@Test fun validatesNationalIdAndEmail() {
    assertTrue(Validators.isValidNationalId("079203001234"))
    assertFalse(Validators.isValidNationalId("07920300123"))
    assertTrue(Validators.isValidEmail("an.nguyen@gmail.com"))
    assertFalse(Validators.isValidEmail("an.nguyen@"))
}
@Test fun passwordRule() {   // ≥ 8 ký tự, có chữ và số
    assertTrue(Validators.isStrongEnoughPassword("flux2026"))
    assertFalse(Validators.isStrongEnoughPassword("12345678"))
    assertFalse(Validators.isStrongEnoughPassword("abc1"))
}
```

- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*MoneyFormatterTest' --tests '*ValidatorsTest'`. Expected: FAIL (unresolved references).
- [ ] **Step 3:** Implement. Use `java.util.regex` for email (not `android.util.Patterns`, so it stays JVM-testable). The phone format is `^0\d{9}$` after `normalizePhone` strips whitespace. CCCD is `^\d{12}$`.
- [ ] **Step 4:** Run the tests again. Expected: PASS.
- [ ] **Step 5: Commit** `feat: VND money formatter and input validators`

---

### Task 4: OTP manager v2 and simulated-SMS delivery

**Files:**
- Modify (rewrite): `app/src/main/java/com/flux/bank/utils/OtpManager.kt` → move to `core/security/OtpManager.kt`
- Create: `core/security/OtpNotifier.kt`, `ui/common/SimulatedSmsBanner.kt` (+ `res/layout/view_sms_banner.xml`)
- Modify: `AndroidManifest.xml` (add `POST_NOTIFICATIONS`)
- Test: rewrite `app/src/test/java/com/flux/bank/SecurityUnitTest.kt` → `core/OtpManagerTest.kt`

**Interfaces:**
- Produces:
  - `enum class OtpResult { VALID, WRONG, EXPIRED, LOCKED, NO_ACTIVE }`
  - `class OtpManager(private val now: () -> Long = System::currentTimeMillis)` with `fun issue(transactionId: String): String` (replaces any active code), `fun verify(transactionId: String, code: String): OtpResult`, `fun remainingSeconds(): Long`, `fun attemptsLeft(): Int`, and constants `VALIDITY_MS = 120_000L`, `MAX_ATTEMPTS = 5`. One instance lives at `FluxApp.otpManager`.
  - `class OtpNotifier(context: Context) { fun deliver(code: String) }`. It posts the notification `"FLUX: Mã OTP của bạn là 481 203. Không chia sẻ mã này cho bất kỳ ai."` (code grouped as 3+3) on channel `otp_sms`. Its PendingIntent reopens the current task with extra `EXTRA_OTP_CODE`. **Fallback:** when `POST_NOTIFICATIONS` is not granted, it emits on `val inAppSms: LiveData<Event<String>>`. The hosting Activity observes this and shows `SimulatedSmsBanner` (a top banner styled as an SMS, tap to fill).

- [ ] **Step 1: Write the failing tests** (fake clock `var t = 0L; OtpManager { t }`):

```kotlin
@Test fun validOnceThenNoActive() { val c = m.issue("tx1"); assertEquals(OtpResult.VALID, m.verify("tx1", c)); assertEquals(OtpResult.NO_ACTIVE, m.verify("tx1", c)) }
@Test fun boundToTransaction() { val c = m.issue("tx1"); assertEquals(OtpResult.WRONG, m.verify("tx2", c)) }
@Test fun expiresAt120s() { val c = m.issue("tx1"); t = 120_001; assertEquals(OtpResult.EXPIRED, m.verify("tx1", c)) }
@Test fun stillValidAt120s() { val c = m.issue("tx1"); t = 120_000; assertEquals(OtpResult.VALID, m.verify("tx1", c)) }
@Test fun locksAfterFiveWrong() { val c = m.issue("tx1"); repeat(5) { m.verify("tx1", "000000".takeIf { it != c } ?: "111111") }; assertEquals(OtpResult.LOCKED, m.verify("tx1", c)) }
@Test fun resendInvalidatesOld() { val old = m.issue("tx1"); val new = m.issue("tx1"); if (old != new) assertEquals(OtpResult.WRONG, m.verify("tx1", old)); assertEquals(OtpResult.VALID, m.verify("tx1", new)) }
@Test fun sixDigits() { repeat(50) { assertTrue(m.issue("t").matches(Regex("\\d{6}"))) } }
```

- [ ] **Step 2:** Run `--tests '*OtpManagerTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement `OtpManager`: keep `transactionId`, code, `expiresAt`, and `wrongAttempts`. A wrong transaction id counts as a wrong attempt. Pad the code to 6 digits with `"%06d"`. Then implement `OtpNotifier`: create the channel in `FluxApp.onCreate`, and handle the fallback path as described above.
- [ ] **Step 4:** Run the tests. Expected: PASS. Delete the old `utils/OtpManager.kt` and the old OTP tests in `SecurityUnitTest.kt` (keep any still-valid KYC constant tests by moving them to the Task 5 model tests).
- [ ] **Step 5: Commit** `feat: transaction-bound OTP with simulated SMS delivery`

---

### Task 5: Data models and repositories

**Files:**
- Modify: `data/model/User.kt`, `data/repository/AuthRepository.kt`
- Create: `data/model/KycRecord.kt`, `data/model/BankAccount.kt`, `data/repository/UserRepository.kt`, `KycRepository.kt`, `AccountRepository.kt`
- Modify: `FluxApp.kt` (lazy repository instances)
- Test: `app/src/test/java/com/flux/bank/data/ModelDefaultsTest.kt` (replaces `UserUnitTest.kt`)

**Interfaces:**
- Produces (each repository is an interface plus a `Firestore…` implementation in the same file; all calls are `suspend` and return `Result<T>`):
  - `data class User(uid, email, fullName, phoneNumber, nationalId, address, avatarBase64: String? = null, role: UserRole = CUSTOMER, kycStatus: String = KycStatus.NOT_SUBMITTED, kycRejectReason: String? = null, biometricEnabled: Boolean = false, mustChangePassword: Boolean = false, createdAt: Long, createdBy: String? = null)`. Remove `idCardBase64`/`faceImageBase64`.
  - `data class KycRecord(uid, idCardBase64, faceImageBase64, faceEmbedding: List<Double> = emptyList(), submittedAt: Long, submittedBy: String, reviewedBy: String? = null, reviewedAt: Long? = null)` → `kyc/{uid}`
  - `enum class AccountType { CHECKING, SAVING, MORTGAGE }`; `data class BankAccount(accountId, ownerUid, type, accountNumber, balance: Long = 0, createdAt)` → `accounts/{accountId}`
  - `interface AuthRepository { suspend fun login(email, pw): Result<String /*uid*/>; fun currentUid(): String?; fun logout(); suspend fun changePassword(newPw): Result<Unit>; suspend fun sendPasswordReset(email): Result<Unit>; suspend fun createAuthUser(email, tempPw): Result<String>; suspend fun deleteCreatedAuthUser(): Result<Unit> }`. `createAuthUser` uses a secondary `FirebaseApp` named `"onboarding"`, built from `FirebaseApp.getInstance().options`. It signs that secondary instance out after creation. `deleteCreatedAuthUser` deletes the user still signed in on the secondary instance (used only for rollback).
  - `interface UserRepository { getUser(uid): Result<User>; createProfile(user): Result<Unit>; updateFields(uid, fields: Map<String, Any?>): Result<Unit>; listCustomers(): Result<List<User>>; findByPhone(phone): Result<User?>; findByNationalId(id): Result<User?>; countCustomers(): Result<Int> }`. `// shortcut: listCustomers loads all and filters on the client; switch to an index or search service when there are more than a few hundred customers.`
  - `interface KycRepository { submit(record: KycRecord): Result<Unit>` (writes `kyc/{uid}` and sets `users.kycStatus=PENDING`, `kycRejectReason=null` in one batch)`; get(uid): Result<KycRecord?>; pendingQueue(): Result<List<Pair<User, KycRecord>>>; review(uid, approve: Boolean, reason: String?, officerUid): Result<Unit> }`
  - `interface AccountRepository { createChecking(ownerUid): Result<BankAccount>; listByOwner(ownerUid): Result<List<BankAccount>> }`. Account number: `"8868"` + 8 random digits; retry once if a document with that number exists.

- [ ] **Step 1: Write the failing test** `ModelDefaultsTest`: assert `User()` defaults (`kycStatus == NOT_SUBMITTED`, `biometricEnabled == false`, `mustChangePassword == false`, `role == CUSTOMER`), and assert `BankAccount().balance == 0L && type == CHECKING`.
- [ ] **Step 2:** Run it. Expected: FAIL.
- [ ] **Step 3:** Implement the models, interfaces, Firestore implementations, and `FluxApp` wiring. Remove the old `AuthRepository` methods that now live elsewhere. Keep the old Activities compiling, even crudely, because later tasks replace them.
- [ ] **Step 4:** Run `testDebugUnitTest` and `assembleDebug`. Expected: both pass.
- [ ] **Step 5: Commit** `refactor: split repositories and add KYC/account models`

---

### Task 6: Customer onboarding service (officer create / edit rules)

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/CustomerOnboardingService.kt`
- Test: `app/src/test/java/com/flux/bank/domain/CustomerOnboardingServiceTest.kt` (with in-memory fakes of the Task 5 interfaces in `src/test/.../fakes/`)

**Interfaces:**
- Consumes: `AuthRepository`, `UserRepository`, `AccountRepository`, `Validators`.
- Produces:
  - `data class NewCustomerInput(fullName, email, phone, nationalId, address, tempPassword)`
  - `sealed class OnboardingError { object InvalidEmail; object InvalidPhone; object InvalidNationalId; object WeakPassword; object EmptyName; object PhoneTaken; object NationalIdTaken; data class Failed(val cause: Throwable) }` (each is a subclass of `OnboardingError`)
  - `class CustomerOnboardingService(auth, users, accounts, officerUid: () -> String)` with:
    - `suspend fun create(input): Result<User>`: validate → duplicate checks → `createAuthUser` → `createProfile(mustChangePassword = true, createdBy = officer)` → `createChecking`. On any failure after the Auth user exists, call `deleteCreatedAuthUser()` (and delete the profile if it was written), then return the failure.
    - `suspend fun validateEdit(uid, phone, nationalId): OnboardingError?`: the same duplicate rules, where a match on `uid` itself is allowed.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun createsProfileAndChecking() // users has 1 CUSTOMER with mustChangePassword=true, accounts has 1 CHECKING balance 0
@Test fun rejectsDuplicatePhoneBeforeAnyWrite() // existing user phone 0912345678 → PhoneTaken; fakeAuth.created is empty
@Test fun rejectsDuplicateNationalId()
@Test fun rollsBackAuthUserWhenProfileWriteFails() // fakeUsers.failNextCreate = true → Result.failure; fakeAuth.deleted == true
@Test fun rollsBackWhenAccountCreateFails() // profile removed and auth user deleted
@Test fun editAllowsOwnPhone() // validateEdit(uid of owner, same phone, same id) == null
@Test fun editRejectsOthersPhone()
@Test fun validationErrorsMapped() // invalid email → InvalidEmail, "abc1" → WeakPassword, blank name → EmptyName
```

- [ ] **Step 2:** Run `--tests '*CustomerOnboardingServiceTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement. Add `deleteProfile(uid)` to `UserRepository` if rollback needs it (update the Task 5 interface and its fake).
- [ ] **Step 4:** Run the tests. Expected: PASS.
- [ ] **Step 5: Commit** `feat: customer onboarding with duplicate checks and rollback`

---

### Task 7: Firestore security rules and officer seeding guide

**Files:**
- Create: `firestore.rules`, `README.md` (project root of `FLUX_Bank/`)

Rules (spec §5.2):
- `isOfficer()` = `get(/databases/$(db)/documents/users/$(request.auth.uid)).data.role == 'BANK_OFFICER'`.
- `users/{uid}`: read if owner or officer. Create only if officer. Update if officer; or if owner **and** `request.resource.data.diff(resource.data).affectedKeys().hasOnly(['phoneNumber','address','avatarBase64','biometricEnabled','mustChangePassword'])`. The owner may only set `mustChangePassword` to `false`. Delete only if officer (needed for rollback).
- `kyc/{uid}`: read if owner or officer. Write if officer, or if owner and the write keeps reviewer fields unset. Only an officer may set `reviewedBy`.
- `users.kycStatus`: owner may set only `PENDING` (through the batch in `KycRepository.submit`). Add `kycStatus` and `kycRejectReason` to the owner's allowed keys **only** when the new `kycStatus == 'PENDING'` and `kycRejectReason == null`.
- `accounts/{id}`: read if owner (`resource.data.ownerUid == request.auth.uid`) or officer. Create only if officer. Update and delete denied for everyone (Feature 4 will open balance updates through transactions).

- [ ] **Step 1:** Write `firestore.rules`.
- [ ] **Step 2:** Write `README.md`: build command (with `JAVA_HOME`), how to deploy the rules (Firebase Console → Firestore → Rules → paste → Publish), how to seed the first officer (Console → Authentication → Add user → copy UID → Firestore → `users/{UID}` with fields `uid, email, fullName, phoneNumber, nationalId, address, role="BANK_OFFICER", kycStatus="VERIFIED", biometricEnabled=false, mustChangePassword=false, createdAt=<ms>`), and the demo flow.
- [ ] **Step 3:** Ask the owner to publish the rules in the Console and seed one officer. Verify with the Console's **Rules Playground**: a customer reading another customer's `users` document → denied; a customer updating their own `fullName` → denied; a customer updating their own `address` → allowed; an officer updating `fullName` → allowed.
- [ ] **Step 4: Commit** `feat: Firestore security rules and setup README`

---

### Task 8: Login, unlock, first-login password change, app lock

*(Requires Task 1 approval.)*

**Files:**
- Create: `core/security/LockPolicy.kt`, `core/security/AppLockObserver.kt`, `ui/auth/UnlockActivity.kt`, `ui/auth/ChangePasswordActivity.kt` (+ ViewModel), layouts `activity_login.xml` (rewrite), `activity_unlock.xml`, `activity_change_password.xml`
- Modify: `ui/auth/LoginActivity.kt`, `ui/auth/AuthViewModel.kt`, `utils/SessionManager.kt`, `utils/BiometricHelper.kt`, `FluxApp.kt`
- Test: `core/LockPolicyTest.kt`, `ui/auth/AuthViewModelTest.kt`

**Interfaces:**
- Produces:
  - `class LockPolicy(val timeoutMs: Long = 60_000L) { fun onColdStart(hasSession: Boolean, biometricEnabled: Boolean, biometricAvailable: Boolean): Boolean; fun onForeground(backgroundedAtMs: Long?, nowMs: Long, hasSession: Boolean, biometricEnabled: Boolean, biometricAvailable: Boolean): Boolean }`
  - `AppLockObserver` (a `ProcessLifecycleOwner` observer registered in `FluxApp`). It records the time the app goes to the background. On foreground, if `LockPolicy` says lock, it starts `UnlockActivity` with `FLAG_ACTIVITY_NEW_TASK | CLEAR_TOP`.
  - `AuthUiState` gains a `NavigateToChangePassword` state. `AuthViewModel.login` routes: `mustChangePassword` → change password; otherwise by role.
  - `SessionManager` keeps only `biometricEnabled` (per uid) and the last email. The Firebase session is the source of truth.
  - `RoleRouter.start(activity, user)`: one function used by Login, Unlock, and ChangePassword to open the right main Activity.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun coldStartLocksOnlyWithSessionAndBiometric() { assertTrue(p.onColdStart(true, true, true)); assertFalse(p.onColdStart(true, false, true)); assertFalse(p.onColdStart(false, true, true)) }
@Test fun neverLocksWithoutBiometricHardware() { assertFalse(p.onColdStart(true, true, false)); assertFalse(p.onForeground(0, 999_999, true, true, false)) }
@Test fun foregroundLocksAtExactly60s() { assertTrue(p.onForeground(0, 60_000, true, true, true)); assertFalse(p.onForeground(0, 59_999, true, true, true)) }
@Test fun foregroundWithoutBackgroundTimestampDoesNotLock() { assertFalse(p.onForeground(null, 10, true, true, true)) }
// AuthViewModelTest (InstantTaskExecutorRule + StandardTestDispatcher, fake repos):
@Test fun firstLoginRoutesToChangePassword()
@Test fun officerRoutesToOfficer()
@Test fun blankEmailShowsVietnameseError() // state is Error(R.string.err_email_required)
```

- [ ] **Step 2:** Run the tests. Expected: FAIL.
- [ ] **Step 3:** Implement `LockPolicy`, the ViewModel changes, `AppLockObserver`, and the three screens per the approved `auth` mockups. `UnlockActivity` shows the biometric prompt automatically, plus a "Đăng nhập tài khoản khác" link that signs out and goes to Login. Launch routing in `LoginActivity.onCreate`: if a session exists and the policy says lock → Unlock; if a session exists and no lock → `RoleRouter`. Disable the biometric option when `BiometricHelper.canAuthenticate` is false.
- [ ] **Step 4:** Run the tests and `assembleDebug`. Expected: PASS. Then check on a device: password login; kill the app and reopen → unlock prompt; background for 60 s → unlock; background for 10 s → no lock.
- [ ] **Step 5: Commit** `feat: redesigned login, biometric app lock, first-login password change`

---

### Task 9: Customer shell, Home tab, Coming-soon screen

*(Requires Task 1 approval.)*

**Files:**
- Rewrite: `ui/customer/CustomerMainActivity.kt`, `res/layout/activity_customer_main.xml`
- Create: `ui/customer/home/HomeFragment.kt`, `HomeViewModel.kt`, `AccountCardAdapter.kt` (ViewPager2 pages), `UtilityGridAdapter.kt`, `ui/common/ComingSoonFragment.kt`, layouts `fragment_home.xml`, `item_account_card.xml`, `item_utility.xml`, `fragment_coming_soon.xml`, `res/menu/customer_tabs.xml`, `ui/customer/Utility.kt`
- Test: `ui/customer/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: `UserRepository.getUser`, `AccountRepository.listByOwner`, `MoneyFormatter`.
- Produces:
  - `enum class Utility(@StringRes label, @DrawableRes icon, @ColorRes tint)` with exactly 12 entries, in order: NAP_TIEN (plus, emerald), RUT_TIEN (arrow_down, emerald), TIEN_DIEN (lightning, gold), TIEN_NUOC (drop, blue), NAP_DT (phone, blue), VE_MAY_BAY (airplane, sky), VE_PHIM (film, rose), KHACH_SAN (bed, violet), MUA_SAM (bag, rose), TIET_KIEM (piggy, emerald), CHI_NHANH (map_pin, blue), TRO_LY_AI (sparkle, violet). In this task, every utility opens `ComingSoonFragment.newInstance(label)`.
  - `CustomerMainActivity.showTab(tab: CustomerTab)` and `fun openScreen(fragment: Fragment)` (adds the fragment to the back stack). Later tasks use these to navigate.
  - `HomeUiState(greeting: Int /*string res by hour*/, firstName, accounts: List<AccountCardUi>, balanceHidden: Boolean)`; `AccountCardUi(typeLabel, maskedNumber /* "•••• 1234" */, balanceText, hidden)`.

Layout requirements (spec §4.2 and the approved mockup):
- Tab bar: Trang chủ · Chuyển tiền · [QR center button] · Lịch sử · Cá nhân. Build it as `BottomNavigationView` with a disabled center placeholder item, plus a 56dp emerald `FloatingActionButton`-style view overlapping it. Selected tab color is `flux_emerald_dark`. Chuyển tiền, QR, and Lịch sử open `ComingSoonFragment`. Cá nhân opens a placeholder until Task 14.
- Home: `bg_home_mint` gradient on the top ~40%. Greeting row: "Chào buổi sáng/chiều/tối" (by hour: <12, <18, else) + first name + bell. The ViewPager2 card uses `layout_constraintDimensionRatio="1.586:1"`, has an emerald dot indicator, and an eye toggle that hides the balance as `••••••• ₫`. Below it: the 4×3 grid (`GridLayoutManager(4)`), then a "Giao dịch gần đây" header and an empty state ("Chưa có giao dịch"). The whole screen sits in a `NestedScrollView`. Use skeleton placeholders while loading.

- [ ] **Step 1: Write the failing tests**: greeting resource for 08:00, 14:00, 20:00; a balance of 48_250_000 is shown as `"48.250.000 ₫"`; the toggle hides it; account number `886812345678` is masked as `"•••• 5678"`.
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement the ViewModel, then the layouts and fragments.
- [ ] **Step 4:** Run the tests and `assembleDebug`. Expected: PASS. Then on a device, compare against the mockup side by side: card ratio, grid order, tab order, emerald usage.
- [ ] **Step 5: Commit** `feat: customer tab shell and home screen`

---

### Task 10: Transaction auth sheet — OTP step with custom keypad

*(Requires Task 1 approval.)*

**Files:**
- Create: `ui/auth/sheet/TransactionAuthSheet.kt` (`BottomSheetDialogFragment`), `TransactionAuthViewModel.kt`, `ui/common/NumericKeypadView.kt`, `ui/common/OtpCellsView.kt`, layouts `sheet_transaction_auth.xml`, `view_numeric_keypad.xml`
- Test: `ui/auth/sheet/TransactionAuthViewModelTest.kt`

**Interfaces:**
- Consumes: `FluxApp.otpManager`, `OtpNotifier`.
- Produces:
  - `data class AuthSummary(val title: String, val rows: List<Pair<String, String>>, val amount: Long?)`
  - `TransactionAuthSheet.show(fm: FragmentManager, summary: AuthSummary, requestKey: String)`. The result goes back through the Fragment Result API under `requestKey`, as bundle key `"verified": Boolean`. When `summary.amount >= 10_000_000`, the sheet continues to the face step (Task 12) before returning `true`.
  - `TransactionAuthViewModel` states: `EnteringOtp(cells, remainingSec, error?)`, `FaceStep`, `Verified`, `Locked`. Error strings map from `OtpResult`.

UI: scrim `flux_scrim`; title "Xác nhận giao dịch"; summary rows; 6 cells (active cell has an emerald stroke); "Gửi lại mã sau mm:ss" becomes a "Gửi lại mã" link at 0; the in-sheet keypad (digits, backspace) **never opens the system IME** (`showSoftInputOnFocus = false`, no EditText focus). The Obsidian "Xác nhận" button is enabled at 6 digits. Tapping the OTP notification or the in-app banner fills the cells.

- [ ] **Step 1: Write the failing tests**: issuing on open sends exactly one OTP to the notifier fake; entering the correct code → `Verified` for amount `null` and for `9_999_999`; → `FaceStep` for `10_000_000`; a wrong code shows `attemptsLeft` in the error; after 5 wrong → `Locked`; resend issues a new code and resets the countdown to 120.
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement the ViewModel, views, and sheet. Inject a test-only `OtpNotifier` seam through a `(String) -> Unit` parameter.
- [ ] **Step 4:** Run the tests and `assembleDebug`. Expected: PASS. On a device, the sheet must not move when typing. Deny notifications and check that the in-app banner appears.
- [ ] **Step 5: Commit** `feat: transaction auth bottom sheet with OTP keypad`

---

### Task 11: Face pipeline (detection, liveness, embedding, matching)

**Files:**
- Create: `face/FaceMatcher.kt`, `face/BlinkDetector.kt`, `face/FaceEmbedder.kt`, `face/FaceDetectorHelper.kt`, `app/src/main/assets/mobilefacenet.tflite`, `docs/licenses/mobilefacenet.txt`
- Test: `face/FaceMatcherTest.kt`, `face/BlinkDetectorTest.kt`

**Interfaces:**
- Produces:
  - `object FaceMatcher { const val MATCH_THRESHOLD: Float; fun cosine(a: FloatArray, b: FloatArray): Float; fun isMatch(a: FloatArray, b: FloatArray): Boolean /* cosine >= threshold */ }`. Different lengths or a zero vector → `IllegalArgumentException`.
  - `class BlinkDetector(open: Float = 0.8f, closed: Float = 0.2f) { fun onFrame(leftOpen: Float?, rightOpen: Float?): Boolean /* true once the open→closed→open sequence completes */; fun reset() }`. Null probabilities are ignored.
  - `class FaceDetectorHelper { suspend fun detect(bitmap: Bitmap): List<Face>; fun analyzer(onFace: (Face?, Bitmap) -> Unit): ImageAnalysis.Analyzer }` (ML Kit, `PERFORMANCE_MODE_FAST`, `CLASSIFICATION_MODE_ALL`).
  - `class FaceEmbedder(context: Context) : Closeable { fun embed(face: Bitmap): FloatArray /* L2-normalized */ }`. Cropping is done by the caller via `cropFace(bitmap, face.boundingBox)` (a top-level function in the same file, 10% margin). Input size follows the chosen model (typically 112×112).

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun cosineIdentityIsOne() { assertEquals(1f, FaceMatcher.cosine(floatArrayOf(1f,2f), floatArrayOf(1f,2f)), 1e-5f) }
@Test fun cosineOrthogonalIsZero() { assertEquals(0f, FaceMatcher.cosine(floatArrayOf(1f,0f), floatArrayOf(0f,1f)), 1e-5f) }
@Test(expected = IllegalArgumentException::class) fun lengthMismatchThrows() { FaceMatcher.cosine(floatArrayOf(1f), floatArrayOf(1f, 0f)) }
@Test fun blinkSequence() { val b = BlinkDetector(); assertFalse(b.onFrame(.9f,.9f)); assertFalse(b.onFrame(.1f,.1f)); assertTrue(b.onFrame(.9f,.9f)) }
@Test fun noBlinkWithoutClose() { val b = BlinkDetector(); repeat(10) { assertFalse(b.onFrame(.9f,.9f)) } }
@Test fun nullsIgnored() { val b = BlinkDetector(); b.onFrame(.9f,.9f); b.onFrame(null,null); b.onFrame(.1f,.1f); assertTrue(b.onFrame(.9f,.9f)) }
```

- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement. Source a MobileFaceNet `.tflite` that has a permissive license, and record the source URL and license in `docs/licenses/mobilefacenet.txt`.
- [ ] **Step 4: Threshold calibration.** Add a debug-only instrumented helper, or do it manually: embed 3 photos each of 2 different people, and log the same-person and different-person cosines. Set `MATCH_THRESHOLD` midway between the lowest same-person score and the highest different-person score. Record the numbers in a KDoc comment on the constant.
- [ ] **Step 5:** Run the unit tests and `assembleDebug`. Expected: PASS.
- [ ] **Step 6: Commit** `feat: on-device face detection, blink liveness and MobileFaceNet matching`

---

### Task 12: Face step in the auth sheet + "Kiểm tra xác thực khuôn mặt"

*(Requires Task 1 approval.)*

**Files:**
- Create: `ui/auth/sheet/FaceStepController.kt` (CameraX preview + analyzer inside the sheet), `domain/FaceVerificationPolicy.kt`
- Modify: `TransactionAuthSheet.kt`, `TransactionAuthViewModel.kt`, `sheet_transaction_auth.xml`
- Test: `domain/FaceVerificationPolicyTest.kt`, extend `TransactionAuthViewModelTest.kt`

**Interfaces:**
- Consumes: Task 11 classes, `KycRepository.get`, `UserRepository.getUser`.
- Produces:
  - `object FaceVerificationPolicy { const val HIGH_VALUE_THRESHOLD = 10_000_000L; fun requiresFace(amount: Long?): Boolean; fun precheck(user: User, kyc: KycRecord?): FacePrecheck }` with `enum FacePrecheck { OK, NOT_VERIFIED, NO_EMBEDDING }`.
  - Sheet constant `MAX_FACE_ATTEMPTS = 3`. After 3 failed matches the result is `verified=false`, with the message "Không xác thực được khuôn mặt".

- [ ] **Step 1: Write the failing tests**: `requiresFace(10_000_000) == true`, `requiresFace(9_999_999) == false`, `requiresFace(null) == false`; `precheck` returns `NOT_VERIFIED` for `kycStatus=PENDING` and `NO_EMBEDDING` for VERIFIED with an empty list; the ViewModel goes to an error state (no camera start) on a precheck failure; three mismatches → `verified=false`.
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement the face step: request the camera permission and use the front camera. Prompt "Hãy chớp mắt". After the blink, take the best frame with exactly one face, embed it, and match it. Show a success check with haptic feedback, or "Không khớp, thử lại (còn n lần)".
- [ ] **Step 4:** Add an entry point for Task 14 to call: `TransactionAuthSheet.show(fm, AuthSummary(title = "Kiểm tra xác thực khuôn mặt", rows = emptyList(), amount = FaceVerificationPolicy.HIGH_VALUE_THRESHOLD), key)`.
- [ ] **Step 5:** Run the tests and `assembleDebug`. Expected: PASS. On a device: own face → pass; another person or a photo of a different person → rejected.
- [ ] **Step 6: Commit** `feat: face verification step for high-value transactions`

---

### Task 13: eKYC capture (customer) at full resolution

*(Requires Task 1 approval.)*

**Files:**
- Rewrite: `ui/customer/EkycActivity.kt` → `ui/ekyc/EkycCaptureFragment.kt`, `ui/ekyc/EkycViewModel.kt`, layout `fragment_ekyc_capture.xml`
- Modify: `utils/ImageUtils.kt` (add `decodeSampled(context, uri, maxDim)`), `AndroidManifest.xml` (FileProvider), create `res/xml/file_paths.xml`
- Test: `ui/ekyc/EkycViewModelTest.kt`

**Interfaces:**
- Consumes: `KycRepository.submit/get`, `FaceDetectorHelper`, `FaceEmbedder`, `cropFace`.
- Produces: `EkycCaptureFragment.newInstance(targetUid: String, submittedBy: String)`. The same fragment serves self-capture (`targetUid == submittedBy`) and officer-assisted capture (Task 16). On success it pops itself and posts the fragment result `"ekyc_submitted"`.

Behavior: capture with `ActivityResultContracts.TakePicture` to a FileProvider URI (full resolution). Decode with downsampling to a max of 1024px (JPEG q80) for storage. Find exactly one face in the selfie; if there are 0 or more than 1 faces, show "Ảnh selfie cần có đúng 1 khuôn mặt". Compute the embedding before submitting. Show status (NOT_SUBMITTED/PENDING/VERIFIED/REJECTED + reason) at the top per the mockup.

- [ ] **Step 1: Write the failing tests** (with a fake face-analysis seam `suspend (Bitmap) -> FaceAnalysis` where `FaceAnalysis = NoFace | MultipleFaces | One(embedding)`): submit without an ID photo → error; a selfie with 2 faces → error and no repository call; a valid submit → `KycRepository.submit` receives a non-empty `faceEmbedding` and `submittedBy`; a REJECTED status exposes the reason text.
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement and register the FileProvider. Launch from the Home eKYC prompt and from the Cá nhân tab (Task 14) via `openScreen`.
- [ ] **Step 4:** Run the tests and `assembleDebug`. Expected: PASS. On a device, after submitting, check that the `kyc/{uid}` document is under 1 MB in the Console.
- [ ] **Step 5: Commit** `feat: full-resolution eKYC capture with face embedding`

---

### Task 14: Customer profile tab (view, edit, security)

*(Requires Task 1 approval.)*

**Files:**
- Create: `ui/customer/profile/ProfileFragment.kt`, `ProfileViewModel.kt`, `EditProfileFragment.kt`, `EditProfileViewModel.kt`, layouts `fragment_profile.xml`, `fragment_edit_profile.xml`
- Test: `ui/customer/profile/EditProfileViewModelTest.kt`

**Interfaces:**
- Consumes: `UserRepository`, `CustomerOnboardingService.validateEdit`, `TransactionAuthSheet`, `SessionManager`, `BiometricHelper`, `EkycCaptureFragment`, `AuthRepository.logout`.

Behavior: the profile header shows the avatar (Base64), name, a masked email, and a KYC pill. The info rows are read-only. In the "Bảo mật" section:
- The biometric switch is disabled with a caption when no hardware or nothing is enrolled. Turning it on requires a successful BiometricPrompt first, then it writes `biometricEnabled` to Firestore and `SessionManager`.
- "Kiểm tra xác thực khuôn mặt" uses the Task 12 entry point.
- "Đổi mật khẩu" opens `ChangePasswordActivity`.
- "Đăng xuất" signs out, clears the session, and goes to Login.

Edit profile: phone, address, and avatar (gallery picker, 256px). Name, CCCD, and email are shown read-only with the caption "Liên hệ nhân viên để thay đổi". A phone change → `validateEdit` → `TransactionAuthSheet` (OTP; summary row "Số điện thoại mới") → `updateFields`. Address or avatar alone saves without OTP.

- [ ] **Step 1: Write the failing tests**: an unchanged phone saves directly without requesting auth; a changed phone emits `RequestAuth` and does not write until `onAuthResult(true)`; `onAuthResult(false)` does not write; a phone taken by another user → error, no auth request; an invalid phone → error.
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run the tests and `assembleDebug`. Expected: PASS. On a device: change the phone through OTP; change only the address; toggle biometrics on and off.
- [ ] **Step 5: Commit** `feat: customer profile tab with OTP-protected phone change`

---

### Task 15: Officer shell, overview, customer list, create / detail / edit

*(Requires Task 1 approval.)*

**Files:**
- Rewrite: `ui/officer/OfficerMainActivity.kt`, `activity_officer_main.xml`, `res/menu/officer_tabs.xml`
- Create: `ui/officer/overview/OverviewFragment.kt` (+VM), `ui/officer/customers/CustomerListFragment.kt` (+VM, adapter, `item_customer.xml`), `CreateCustomerFragment.kt` (+VM), `CustomerDetailFragment.kt` (+VM), layouts
- Test: `ui/officer/customers/CustomerListViewModelTest.kt`, `CreateCustomerViewModelTest.kt`, `CustomerDetailViewModelTest.kt`

**Interfaces:**
- Consumes: `CustomerOnboardingService`, `UserRepository`, `KycRepository.pendingQueue` (count), `AuthRepository.sendPasswordReset`, `EkycCaptureFragment`.
- Produces: `OfficerMainActivity.openScreen(fragment)` (same contract as the customer shell). Tabs: Tổng quan · Khách hàng · eKYC · Lãi suất (ComingSoon) · Cá nhân (officer profile: name, email, biometric toggle, Đăng xuất; reuse the `ProfileFragment` layout pieces).

Behavior:
- Overview shows tiles "Khách hàng" (count) and "eKYC chờ duyệt" (count, taps to the eKYC tab), plus a "Tạo khách hàng" primary button.
- The list has a search field that filters by name (accent-insensitive, using `java.text.Normalizer`), phone, or email. Rows show name, phone, and KYC pill.
- Create form: name, email, phone, CCCD, address, temp password (with a "Tạo ngẫu nhiên" helper that produces a 10-character password with letters and digits). Each `OnboardingError` maps to the error text of its field. On success, show a dialog with the temp password and a copy button.
- Detail: editable name, phone, CCCD, address (email read-only), validated with `validateEdit`. Buttons: "Lưu", "Gửi email đặt lại mật khẩu", "Chụp eKYC hộ khách" (opens `EkycCaptureFragment.newInstance(uid, officerUid)`).

- [ ] **Step 1: Write the failing tests**: search `"nguyen"` matches "Nguyễn Văn An"; search `"0912"` matches by phone; create maps `PhoneTaken` to the phone field error; a successful create exposes the temp password once; detail save with another customer's CCCD → field error and no write.
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run the tests and `assembleDebug`. Expected: PASS. On a device: create a customer → the officer is still signed in → the new customer logs in → is forced to change the password → Home shows a Checking card with `0 ₫`.
- [ ] **Step 5: Commit** `feat: officer console with customer onboarding and editing`

---

### Task 16: Officer eKYC review with reject reason

*(Requires Task 1 approval.)*

**Files:**
- Rewrite: `ui/officer/OfficerEkycReviewActivity.kt` → `ui/officer/ekyc/EkycReviewFragment.kt` (+VM), `RejectReasonSheet.kt`, layouts; delete `activity_officer_ekyc_review.xml`
- Test: `ui/officer/ekyc/EkycReviewViewModelTest.kt`

**Interfaces:**
- Consumes: `KycRepository.pendingQueue/review`, `ImageUtils.base64ToBitmap`.

Behavior: a queue list (name, submitted time, "Nhân viên chụp hộ" tag when `submittedBy != uid`). The detail view shows the CCCD and selfie side by side (tap to zoom) and the "Duyệt" / "Từ chối" buttons. Reject opens `RejectReasonSheet` with preset reasons ("Ảnh CCCD mờ", "Ảnh selfie không rõ mặt", "Thông tin không khớp", "Khác…" plus free text). The reason is required. Approve and reject write `reviewedBy`/`reviewedAt` and remove the item from the queue.

- [ ] **Step 1: Write the failing tests**: reject without a reason → error, no repository call; reject with "Khác…" and blank text → error; approve calls `review(uid, true, null, officerUid)`; after a decision the item leaves the list and the counter decrements.
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run the tests and `assembleDebug`. Expected: PASS. On a device: reject with a reason → the customer sees the reason on the eKYC screen → resubmits → approve → the face check in Cá nhân works.
- [ ] **Step 5: Commit** `feat: eKYC review queue with reject reasons`

---

### Task 17: Cleanup, docs, full manual run

**Files:**
- Delete: any now-unused old layouts and drawables (`activity_ekyc.xml`, old `bg_*`/`ic_*` not referenced), `utils/OtpManager.kt` if still present, `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`
- Modify: `PROJECT_CONTEXT.md` (new architecture, design-system rules, collections, the lock/OTP/face behavior, and the known Spark limitation), `README.md`

- [ ] **Step 1:** `grep -rn 'android:text="[^@]' app/src/main/res/layout` → expected: no matches (all text is in strings.xml). `grep -rn '#[0-9A-Fa-f]\{6\}' app/src/main/res/layout` → expected: no matches (colors are tokens).
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest assembleDebug lintDebug`. Expected: tests and build pass; no new lint errors of `Error` severity.
- [ ] **Step 3:** Run the spec §8 manual flow end to end on a real device, and note any failure as a fix task before closing: create customer → first login → change password → eKYC → officer approve → enable biometrics → lock and unlock → phone change with OTP → face check (self passes, another person fails).
- [ ] **Step 4: Commit** `chore: remove legacy UI, update project docs`
