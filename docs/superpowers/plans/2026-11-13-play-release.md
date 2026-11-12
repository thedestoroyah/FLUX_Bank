# Play Release Preparation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Có AAB đã ký, App Check (Play Integrity / debug), dòng chú thích demo, tài liệu Play Console (chính sách bảo mật, câu trả lời form, tin mời người thử) và hình ảnh trang Play, để chủ dự án tải lên closed testing trong một buổi.

**Architecture:** Cấu hình ký đọc từ `local.properties` (thiếu thì chỉ task đóng gói release báo lỗi). App Check cài qua một hàm `installAppCheck(app)` có hai bản theo build type (`src/debug`, `src/release`) nên debug provider không lọt vào release. Tài liệu và hình ảnh nằm trong `docs/play/`.

**Tech Stack:** Gradle Kotlin DSL (AGP 9.4.1), Firebase BoM 34.19.0 (`firebase-appcheck-playintegrity`, `firebase-appcheck-debug`), `keytool`/`jarsigner`, Python PIL + Inkscape cho hình ảnh, `gh` cho Gist.

**Spec:** [docs/superpowers/specs/2026-11-13-play-release-design.md](../specs/2026-11-13-play-release-design.md)

## Global Constraints

- Không commit: keystore (`*.jks`, `*.keystore`), mật khẩu, mật khẩu tài khoản demo, khoá VNPay. Trước mỗi commit chạy `git diff --cached | grep -iE "password|storePassword|\.jks" ` và chỉ chấp nhận khớp ở tên khoá property / tài liệu hướng dẫn, không có giá trị thật.
- Khoá upload: `~/keystores/flux-upload.jks`, PKCS12, alias `upload`, hiệu lực 25 năm (9125 ngày). Property trong `local.properties`: `release.store.file`, `release.store.password`, `release.key.alias`, `release.key.password`.
- `versionCode = 1`, `versionName = "1.0"`, `applicationId = "com.flux.bank"` (đã kiểm 2026-11-13: chưa có trên Play).
- Tên trên Play: `FLUX Bank (Demo)`. Tên trong máy giữ `FLUX Bank`.
- Dòng chú thích màn đăng nhập (đúng chữ): `Ứng dụng demo cho đồ án môn học · tiền và giao dịch đều là giả lập`.
- App Check chỉ Enforce cho Firebase AI Logic; không cho Firestore/Auth.
- Hình ảnh: icon 512×512 PNG 32-bit; ảnh nổi bật 1024×500 PNG/JPG không trong suốt; ảnh chụp mỗi cạnh 320–3840 px, cạnh dài ≤ 2 × cạnh ngắn.
- Store listing: tên ≤ 30 ký tự, mô tả ngắn ≤ 80, mô tả đầy đủ ≤ 4000.
- Không làm hỏng bản debug: `./gradlew testDebugUnitTest assembleDebug lintDebug` xanh (391 test) sau mỗi task có code.
- Đăng Gist công khai chỉ sau khi chủ dự án đồng ý trong chat.

## Review Focus

1. Máy không có property ký (máy giảng viên clone repo, CI) → `assembleDebug` và unit test vẫn chạy; chỉ `bundleRelease` báo lỗi nêu tên property thiếu (Task 1, bước kiểm).
2. Debug provider của App Check lọt vào release → `releaseRuntimeClasspath` không có `firebase-appcheck-debug` (Task 2, bước kiểm).
3. Bản release cài bằng adb (không qua Play) hoặc trước khi đăng ký Play Integrity → Trợ lý dùng chế độ cơ bản, không crash (Task 2, kiểm trên máy với bản release ký bằng khoá upload).
4. Ảnh chụp màn hình 1268×2756 vượt 2:1 → script cắt và tự `assert` kích thước/tỉ lệ trước khi lưu (Task 5).
5. Ảnh chụp lộ dữ liệu thật (tên, số TK thật của chủ dự án) → chỉ chụp bằng khách demo; soát từng ảnh trước khi commit (Task 5).

---

### Task 1: Cấu hình ký bản release

**Files:**
- Modify: `app/build.gradle.kts` (khối `android { … }`: thêm `signingConfigs`, `buildTypes.release.signingConfig`)
- Modify: `README.md` (mục "Thiết lập Firebase": thêm mục 9 "Ký bản phát hành")

**Interfaces:**
- Produces: `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab` ký bằng khoá upload; các property ở Global Constraints.

- [ ] **Step 1: Kiểm trạng thái hiện tại (RED)** — `./gradlew bundleRelease`; ghi lại kết quả (dự kiến: AAB **không ký**, hoặc ký debug). Đây là hành vi cần thay.
- [ ] **Step 2: Viết cấu hình** — đọc 4 property từ `localProps` (đã có ở đầu file). Đủ cả 4 → `signingConfigs.create("release") { storeFile = file(...); storePassword; keyAlias; keyPassword }` và `buildTypes.release.signingConfig = signingConfigs.getByName("release")`. Thiếu → không tạo signingConfig, và đăng ký lỗi lúc **thực thi** cho các task có tên `bundleRelease`, `assembleRelease`, `packageRelease*`: `GradleException("Thiếu cấu hình ký release trong local.properties: <các key thiếu>. Xem README mục 9.")`. Lambda chỉ giữ chuỗi thông báo (tương thích configuration cache). Comment tiếng Việt giải thích vì sao lỗi lúc thực thi chứ không lúc cấu hình (để máy không có khoá vẫn build debug).
- [ ] **Step 3: Kiểm thiếu khoá** — chưa thêm property: `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL, 391 test; `./gradlew bundleRelease` → FAILED với đúng thông báo có liệt kê 4 key.
- [ ] **Step 4: Kiểm đủ khoá bằng keystore thử** — tạo keystore thử trong scratchpad (`keytool -genkeypair -storetype PKCS12 -keystore <scratchpad>/test.jks -alias upload -keyalg RSA -keysize 2048 -validity 9125 -storepass testpass -keypass testpass -dname "CN=Test"`), tạm thêm 4 property trỏ vào nó, `./gradlew bundleRelease` → BUILD SUCCESSFUL; `jarsigner -verify -verbose:summary app/build/outputs/bundle/release/app-release.aab` → `jar verified` và chủ thể `CN=Test`. **Xoá 4 property thử khỏi `local.properties`** sau khi kiểm.
- [ ] **Step 5: README mục 9** — lệnh `keytool` thật cho chủ dự án (đường dẫn `~/keystores/flux-upload.jks`, alias `upload`, 9125 ngày, `-dname` gợi ý `CN=<Họ tên>, O=TDTU, C=VN`, không ghi mật khẩu), 4 dòng property mẫu với giá trị `<…>`, nhắc sao lưu keystore + mật khẩu ra nơi khác, nhắc tăng `versionCode` mỗi lần tải lên.
- [ ] **Step 6: Commit** — kiểm secret theo Global Constraints → `git add app/build.gradle.kts README.md` → `chore: release signing config from local.properties`.

### Task 2: App Check (Play Integrity cho release, debug provider cho debug)

**Files:**
- Modify: `app/build.gradle.kts` (dependencies)
- Create: `app/src/debug/java/com/flux/bank/AppCheckSetup.kt`, `app/src/release/java/com/flux/bank/AppCheckSetup.kt`
- Modify: `app/src/main/java/com/flux/bank/FluxApp.kt:80-85` (`onCreate` gọi `installAppCheck(this)` ngay sau `instance = this`)
- Modify: `README.md` (mục 8 Trợ lý AI: thay đoạn "tạm tắt App Check" bằng 4 bước bật theo spec §3.2), `PROJECT_CONTEXT.md` (dòng App Check của F7)

**Interfaces:**
- Produces: `fun installAppCheck(app: FluxApp)` (cùng package `com.flux.bank`, top-level, một bản mỗi source set). Bản debug: `Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())`. Bản release: `PlayIntegrityAppCheckProviderFactory.getInstance()`. KDoc tiếng Việt nói bản nào dùng khi nào và debug token xem ở Logcat tag `DebugAppCheckProvider`.

- [ ] **Step 1: Dependencies** — `implementation("com.google.firebase:firebase-appcheck-playintegrity")`, `debugImplementation("com.google.firebase:firebase-appcheck-debug")` (theo BoM, comment tiếng Việt).
- [ ] **Step 2: Hai file `AppCheckSetup.kt` + gọi trong `FluxApp`.**
- [ ] **Step 3: Kiểm build** — `./gradlew testDebugUnitTest assembleDebug lintDebug` xanh (391); `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -c appcheck-debug` → `0`; cùng lệnh với `debugRuntimeClasspath` → ≥ 1.
- [ ] **Step 4: Kiểm trên Xiaomi (bản debug)** — cài, mở Trợ lý, hỏi một câu; `adb logcat -d | grep -i "DebugAppCheckProvider"` có dòng chứa debug token. Lúc này App Check AI Logic vẫn Unenforce nên AI vẫn trả lời (không hồi quy). Ghi token cho chủ dự án đăng ký (Task 6).
- [ ] **Step 5: Kiểm bản release cài tay** — với keystore thử của Task 1 (tạm thêm property lại, xong xoá): `./gradlew assembleRelease`, cài lên Xiaomi (gỡ bản debug trước vì khác chữ ký), mở Trợ lý hỏi một câu → app không crash; AI trả lời (chưa Enforce) hoặc chế độ cơ bản. Gỡ bản release, cài lại bản debug.
- [ ] **Step 6: Commit** — kiểm secret → `feat: App Check with Play Integrity (release) and debug provider (debug)`.

### Task 3: Dòng chú thích "demo" ở màn đăng nhập

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (thêm `login_demo_note`), `app/src/main/res/layout/activity_login.xml` (TextView `tvDemoNote` ngay dưới `tvSecureNote`)

- [ ] **Step 1:** `login_demo_note` = chuỗi ở Global Constraints. `tvDemoNote`: `TextAppearance.Flux.Caption`, màu `@color/flux_text_secondary`, căn giữa, `marginTop` `@dimen/space_4`; chuyển ràng buộc `layout_constraintBottom_toBottomOf="parent"` từ `tvSecureNote` sang `tvDemoNote` (chuỗi dọc giữ nguyên bias 1).
- [ ] **Step 2: Kiểm** — `./gradlew assembleDebug lintDebug` (0 error); cài lên Xiaomi, đăng xuất, chụp màn đăng nhập: dòng chú thích hiện dưới "Phiên đăng nhập được mã hoá", không bị cắt, không đè bàn phím khi gõ.
- [ ] **Step 3: Commit** — `feat: demo disclaimer on login screen`.

### Task 4: Tài liệu cho Play Console

**Files:**
- Create: `docs/play/privacy-policy.md`, `docs/play/play-console-answers.md`, `docs/play/tester-invite.md`

**Interfaces:**
- Produces: nội dung chủ dự án dán vào Play Console; `privacy-policy.md` là nguồn để đăng Gist (Task 6).

- [ ] **Step 1: `privacy-policy.md`** — song ngữ (Việt trước, Anh sau), đủ mục spec §3.5. Email liên hệ để `<EMAIL_LIÊN_HỆ>` (chủ dự án chọn ở Task 6). Ngày hiệu lực 2026-11-13.
- [ ] **Step 2: `play-console-answers.md`** — theo thứ tự các mục trong Play Console → *App content* rồi *Store listing*: Privacy policy (link Gist), App access (hướng dẫn đăng nhập khách demo, mật khẩu để `<…>`, ghi chú OTP qua thông báo và giới hạn < 10.000.000 ₫), Ads (không), Content rating (câu trả lời IARC), Target audience (18+), Data safety (bảng: loại dữ liệu · thu thập/chia sẻ · bắt buộc/tuỳ chọn · mục đích · bên nhận — gồm Name, Email, Phone, Address, Other IDs (CCCD), Photos, Financial info (other), Precise location, Other in-app messages (chat Trợ lý), Device or other IDs (Firebase/App Check); không khai App activity vì app không ghi lại thao tác), Financial features (Money transfer and wire services; Mobile payments and digital wallets), Government/News/Health (không), Store listing (tên `FLUX Bank (Demo)`, mô tả ngắn, mô tả đầy đủ có câu mở đầu "đồ án môn học, tiền giả lập", danh mục Finance), Countries (Việt Nam). Mỗi câu chữ có đếm ký tự cho trường bị giới hạn.
- [ ] **Step 3: `tester-invite.md`** — tin nhắn Zalo/Messenger ngắn: link opt-in `<LINK>`, cách cài từ Play, tài khoản khách demo `<…>`, 4 việc nên thử (chuyển tiền < 10 tr, trả tiền điện, xem chi nhánh, hỏi Trợ lý), nhắc **giữ app và không rời chương trình thử 14 ngày**.
- [ ] **Step 4: Kiểm** — `python3 -I` đếm ký tự tên/mô tả ngắn/mô tả đầy đủ trong `play-console-answers.md` ≤ giới hạn; `grep -n "<" docs/play/*.md` chỉ còn các ô chờ chủ dự án điền.
- [ ] **Step 5: Commit** — `docs: Play Console answers, privacy policy, tester invite`.

### Task 5: Hình ảnh trang Play

**Files:**
- Create: `docs/play/graphics/icon-512.png`, `docs/play/graphics/feature-1024x500.png`, `docs/play/graphics/screenshot-0{1..6}.png`, `docs/play/graphics/make_graphics.py` (script dựng icon, ảnh nổi bật, cắt ảnh chụp)

**Interfaces:**
- Consumes: vector `app/src/main/res/drawable/ic_launcher_{foreground,background}.xml`; ảnh chụp thô từ Xiaomi (khách demo, do chủ dự án tạo ở Task 6 bước 2).

- [ ] **Step 1: Icon** — `make_graphics.py icon`: đọc `pathData`/`fillColor` từ hai vector (viewport 108), ghi SVG tạm, `inkscape` xuất 512×512; `assert` ảnh 512×512, mode RGBA.
- [ ] **Step 2: Ảnh nổi bật** — `make_graphics.py feature`: SVG 1024×500 nền mint `#E7F8EF` → `#F5F5F7`, icon bên trái, chữ "FLUX Bank" (Inter nếu có trong `fc-list`, không thì sans-serif mặc định) màu Obsidian `#0A0D12` và dòng "Ngân hàng số · đồ án demo"; xuất PNG không trong suốt; `assert` 1024×500. **Cho chủ dự án xem ảnh, chờ đồng ý.**
- [ ] **Step 3: Ảnh chụp** (sau khi có khách demo) — đăng nhập khách demo trên Xiaomi, chụp `adb exec-out screencap -p` 6 màn: trang chủ, chuyển tiền, tiện ích Nạp ĐT, lịch sử, bản đồ chi nhánh, Trợ lý (đã có một câu trả lời kèm nút). `make_graphics.py screenshots <thư mục ảnh thô>`: cắt đều trên/dưới về 1268×2536 (bỏ thanh trạng thái và vùng cử chỉ), `assert` mỗi cạnh trong 320–3840 và cạnh dài ≤ 2 × cạnh ngắn. Tắt bong bóng chat Zalo/Messenger nổi trước khi chụp.
- [ ] **Step 4: Soát** — mở từng ảnh, kiểm không có tên/số TK thật của chủ dự án, không có bong bóng ứng dụng khác.
- [ ] **Step 5: Commit** — `docs: Play store graphics`.

### Task 6: Việc của chủ dự án + bàn giao

**Files:**
- Modify: `HANDOFF.md` (mục 2, 3, 5, 9), `docs/play/play-console-answers.md` (điền link Gist khi có)

Các bước do chủ dự án làm, Claude hướng dẫn và kiểm từng bước:
- [ ] **Step 1: Keystore thật** — chủ dự án chạy lệnh `keytool` ở README mục 9 (tự gõ mật khẩu), thêm 4 property; Claude chạy `./gradlew bundleRelease` + `jarsigner -verify` (chủ thể không phải `CN=Test`).
- [ ] **Step 2: Khách demo + nhân viên demo** — chủ dự án tạo trong app (nhân viên tạo khách → khách đổi mật khẩu → eKYC → nhân viên duyệt → mở Tiết kiệm → nạp vài giao dịch); làm xong mới chụp ảnh Task 5 bước 3.
- [ ] **Step 3: Gist** — hỏi chủ dự án email liên hệ và đồng ý đăng; `gh gist create --public docs/play/privacy-policy.md -d "FLUX Bank (Demo) — Privacy Policy"`; ghi link vào `play-console-answers.md`.
- [ ] **Step 4: Play Console** — tạo app, điền theo `play-console-answers.md`, tải `app-release.aab` lên **Closed testing**, thêm người thử (danh sách Gmail), gửi đi duyệt.
- [ ] **Step 5: App Check** — lấy SHA-256 khoá ký app (Play Console → Test and release → App integrity) + SHA-256 khoá upload (`keytool -list -v -keystore ~/keystores/flux-upload.jks`) → Firebase Console → App Check → Play Integrity; đăng ký debug token (Task 2 bước 4); kiểm Trợ lý bản debug vẫn trả lời bằng AI; **rồi mới Enforce** App Check cho Firebase AI Logic; hỏi lại một câu trên bản debug.
- [ ] **Step 6: HANDOFF** — ghi trạng thái phát hành, nơi giữ keystore (không ghi mật khẩu), ngày bắt đầu 14 ngày thử kín, việc còn lại → commit `docs: handoff after Play release prep`.
