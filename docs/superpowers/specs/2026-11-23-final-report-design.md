# FLUX Bank — Báo cáo cuối kỳ

- Ngày: 2026-11-23 · Hạn nộp: 2026-12-01 · Nhánh: `docs/report`
- Trạng thái: **đã duyệt 2026-11-23** (qua builder check). Nền tảng: F1–F7 + chuẩn bị Play đã trên `main` (khớp `origin/main`). Ngày 2026-11-23: 391/391 unit test, 49/49 rules test, `assembleDebug` + `lintDebug` (0 lỗi) đều đạt.
- Ký hiệu: **[Đề]** yêu cầu đề / barem · **[Chủ dự án]** chủ dự án quyết · **[Claude]** Claude tự quyết, chủ dự án sửa được khi duyệt.

## 1. Mục đích

Đề yêu cầu một báo cáo gồm *Analysis and Design* (nguyên tắc, pattern, class diagram, ERD, use case, sơ đồ thành phần/hạ tầng), *Implementation* (cấu trúc code, công nghệ) và *User Interface* (trình bày mọi chức năng) **[Đề]**. Barem `docs/Marking.xlsx` chấm trực tiếp trên báo cáo 5/10 điểm:

| Mục barem | Điểm | Mức tối đa đòi hỏi |
|---|---|---|
| Architecture | 1 | Kiến trúc, công nghệ, **giải thích lý do chọn**, rõ ràng đầy đủ |
| Class diagrams | 1 | Đúng, đủ class diagram **và các mối quan hệ** |
| UI | 1 | Giao diện đẹp, chuyên nghiệp (thể hiện qua ảnh + video) |
| Database design | 1 | Mô tả đủ **các bảng và quan hệ giữa các bảng** |
| Hình thức báo cáo, demo | 1 | **Mẫu báo cáo của Khoa**, mục lục, không lỗi chính tả, tài liệu tham khảo đầy đủ |

7 tính năng (5 điểm) chấm qua app và video; báo cáo trình bày chúng để người chấm thấy đủ yêu cầu của đề.

## 2. Mục tiêu, không làm, phản mục tiêu

**Mục tiêu**
- Một file `.docx` theo mẫu Khoa, tiếng Việt, mở bằng ONLYOFFICE, cập nhật mục lục rồi xuất PDF là nộp được.
- Mỗi mục barem có một chương/mục trả lời thẳng, người chấm tìm thấy ngay.
- Mọi con số, tên class, luồng xử lý trong báo cáo **khớp code trên `main`** và tài liệu trong repo.
- Dựng lại được bằng một lệnh khi sửa văn bản hoặc thay ảnh.

**Không làm** **[Claude]**
- Không sửa code app (lỗi nhỏ trong HANDOFF §7 chỉ ghi vào phần hạn chế nếu đáng kể).
- Không làm slide thuyết trình (đề không yêu cầu).
- Không viết kịch bản video trong đợt này: đề không đặt yêu cầu riêng cho video, chủ dự án tính sau khi báo cáo xong **[Chủ dự án]**; danh sách màn hình ở §6.4 dùng lại được.
- Không chụp ảnh UI trong đợt viết: ảnh chụp sau khi chủ dự án dọn máy (tắt nút trợ năng nổi, bật Không làm phiền, đổi tên khách thử). Trong lúc chờ, báo cáo dùng **ảnh giữ chỗ** có ghi rõ tên màn cần chụp.

**Phản mục tiêu**
- Bịa tính năng, số liệu hoặc kết quả chưa từng chạy (ví dụ: nói app đã lên Play production, SMS thật, Cloud Functions).
- Văn giọng AI: câu sáo rỗng, liệt kê ba ý cho đủ nhịp, "không chỉ… mà còn…", từ đệm "một cách", văn dịch. Soát bằng skill `vn-humanizer`.
- Lộ bí mật hoặc dữ liệu riêng: mật khẩu keystore, mật khẩu tài khoản thử, `vnpay.hash.secret`, debug token App Check, API key, vị trí nhà chủ dự án trên ảnh bản đồ.
- Sơ đồ quá dày không đọc được khi in A4.
- Lệch mẫu Khoa (lề, font, số trang, kiểu chú thích) làm mất điểm hình thức.

## 3. Ràng buộc

- Làm một mình; đề ghi nhóm 2–3 người → báo cáo nói rõ là cá nhân **[Chủ dự án]**.
- Google Play: giảng viên trả lời 2026-11-13 là tuỳ chọn, chỉ cộng điểm → báo cáo trình bày phần đã chuẩn bị (AAB ký, chính sách, ảnh Play) và trạng thái thật lúc nộp **[Chủ dự án]**.
- **Công cụ AI: nói thẳng** **[Chủ dự án]**. Chương 1 có mục riêng kể tên và việc dùng: Google Antigravity (viết code F1–F2 ban đầu), Claude Code (spec, plan, viết code F3–F7 và làm lại F1–F2, review toàn nhánh, soạn báo cáo). Ghi rõ phần chủ dự án: chọn phạm vi và công nghệ, duyệt spec + mockup + plan, cấu hình Firebase/VNPay/Play Console, chạy thử trên máy thật, quyết định sửa lỗi. Ở chương 2 và 6, "review toàn nhánh" ghi đúng là do agent AI review độc lập, không viết như thể có người review. Trang cam đoan giữ văn mẫu, thêm một câu dẫn tới mục công cụ AI **[Claude]**.
- Không có phụ lục cài đặt/tài khoản demo: đề và barem chỉ cần báo cáo + video, không nộp APK **[Chủ dự án]**.
- Mẫu: `Mobile_App_Development/Mid_Exam/notes/BAO_CAO.template-goc.docx` **[Chủ dự án]**. Bìa: Phạm Huỳnh Quốc Đạt – 52300098, "BÁO CÁO CUỐI KỲ", "PHÁT TRIỂN ỨNG DỤNG DI ĐỘNG", người hướng dẫn **ThS. Võ Văn Thành**, TP. Hồ Chí Minh, năm 2026 **[Chủ dự án]**.
- Mẫu có sẵn: Heading 1–4 (Heading 1 = chương), style `Caption` với trường đánh số `Hình`/`Bảng`, ba trường mục lục (`TOC \o "1-3"`, `TOC \c "Hình"`, `TOC \c "Bảng"`), style `Nội dung văn bản` cho đoạn văn, 5 section (bìa, bìa lót, phần mở đầu số La Mã, phần thân số thường), A4, lề trái 3,5 cm / phải 2 cm, Times New Roman 13.
- Công cụ có trên máy: pandoc, Java 21/25, Python qua `uv run --with …`, ONLYOFFICE. Không có: LibreOffice, PlantUML, graphviz, Node.
- Chủ dự án quen sửa tay trong ONLYOFFICE; với cách dựng bằng script, **sửa tay chỉ ở bước cuối** (cập nhật mục lục, xuất PDF), mọi sửa nội dung đi qua file nguồn.

## 4. Cách làm (Approaches)

Chủ dự án chọn **A — Claude viết trọn văn** (thay vì B — Claude làm dàn ý, chủ dự án tự viết như giữa kỳ), vì còn 8 ngày và phải quay video **[Chủ dự án]**.

Cách dựng file, ba phương án đã cân nhắc:
1. **Markdown + PlantUML → pandoc (style từ mẫu Khoa) → script Python ghép vào phần mở đầu của mẫu.** Sửa một câu hay thay ảnh thì chạy lại một lệnh; văn bản trong git, xem được thay đổi. Tốn công lần đầu để ra đúng định dạng. — **chọn** **[Chủ dự án]**.
2. Script python-docx ghi thẳng vào mẫu: kiểm soát định dạng tốt nhất nhưng văn bị chôn trong code, khó đọc, khó sửa.
3. Dựng một lần rồi chủ dự án sửa tay: tự do nhưng mỗi lần đổi ảnh/số liệu phải thay tay; ảnh UI chắc chắn còn chụp lại nên bất lợi.

## 5. Cấu trúc báo cáo **[Claude]**

Độ dài mục tiêu khoảng 14.000–17.000 từ (giữa kỳ ~10.000 từ / 46 trang; cuối kỳ nhiều tính năng hơn và nhiều sơ đồ hơn).

**Phần mở đầu (lấy từ mẫu, thay nội dung):** bìa, bìa lót có người hướng dẫn, Lời cảm ơn, trang cam đoan (giữ nguyên văn mẫu, thay tên người hướng dẫn), Tóm tắt (VI) + Abstract (EN) khoảng 200 từ mỗi bản, Mục lục, Danh mục hình, Danh mục bảng, Danh mục chữ viết tắt.

**Chương 1. Giới thiệu** — bối cảnh ngân hàng số; mục tiêu; bảng đối chiếu 7 yêu cầu của đề ↔ chỗ đáp ứng trong app; phạm vi (giả lập: tiền, liên ngân hàng, tiện ích, chi nhánh; làm cá nhân; Play tuỳ chọn); công cụ; **mục "Công cụ AI hỗ trợ"** (§3); bố cục báo cáo.

**Chương 2. Kiến trúc hệ thống và công nghệ** (→ barem *Architecture*)
- Sơ đồ kiến trúc tổng thể: app Android ↔ Firebase Auth, Firestore + Security Rules, Firebase AI Logic (Gemini), VNPay sandbox, OSM tile + OSRM, ML Kit/LiteRT chạy trên máy.
- Kiến trúc trong app: MVVM, Repository (interface + Firestore impl), service locator `FluxApp` (lý do không dùng DI library), tầng `domain` thuần Kotlin, Single-Activity-per-role + `ScreenHost`.
- Nguyên tắc và thực hành: tách tầng, phụ thuộc vào interface (test bằng fake), bất biến số dư qua Firestore transaction + rules, TDD, review toàn nhánh, spec → plan trước khi code, mockup duyệt trước khi làm UI.
- Bảng công nghệ + **lý do chọn** + phương án đã loại: Kotlin + XML/ViewBinding (vs Compose), Firebase Spark (vs SQLite / Blaze), OTP giả lập (vì Spark không SMS), ML Kit + MobileFaceNet (vs dịch vụ eKYC trả phí), VNPay sandbox (vs Stripe), OSMDroid + OSRM (vs Google Maps — `OR_BACR2_59`), Gemini qua AI Logic (không API key trong APK; model `gemini-3.5-flash-lite` vì hạn mức free tier), Phosphor + Inter. Phiên bản lấy từ `gradle/libs.versions.toml` và `app/build.gradle.kts`.
- Sơ đồ triển khai/hạ tầng (deployment diagram).

**Chương 3. Phân tích và thiết kế** (→ *Class diagrams*, *Database design*)
- 3.1 Tác nhân và use case: Khách hàng, Nhân viên; hệ thống ngoài (VNPay, Gemini, OSRM). Sơ đồ use case tổng quát + sơ đồ theo nhóm tính năng; bảng đặc tả cho 5–6 use case chính (đăng nhập + khoá sinh trắc, tạo khách, eKYC, chuyển tiền, thanh toán hoá đơn, hỏi trợ lý).
- 3.2 Class diagram, chia để đọc được trên A4:
  - model (`User`, `KycRecord`, `BankAccount`, `Transaction`, `ServicePayment`, `Rates`, `Bank`, `Payee` + enum);
  - repository (interface ↔ Firestore impl, quan hệ với model);
  - domain (policy, calculator, onboarding service, utility, branch, assistant);
  - security & xác thực giao dịch (`OtpManager`, `LockPolicy`, `AppLockObserver`, `TransactionAuthSheet`, `FaceMatcher`, `BlinkDetector`, `FaceEmbedder`);
  - UI ↔ ViewModel ↔ repository cho một tính năng tiêu biểu (chuyển tiền) và tổng quan composition root `FluxApp`.
  - Mỗi sơ đồ có đoạn giải thích quan hệ (kế thừa, hiện thực interface, association, dependency, composition).
- 3.3 Sơ đồ tuần tự: chuyển tiền trong FLUX (validate → OTP → mặt nếu ≥ 10 tr → Firestore transaction 4 document → rules); eKYC; nạp tiền VNPay; hỏi trợ lý (AI + chế độ cơ bản). Thêm sơ đồ trạng thái OTP hoặc khoá app nếu còn chỗ.
- 3.4 Thiết kế CSDL: lý do chọn Firestore (NoSQL document); **sơ đồ ER** (collection = thực thể, field tham chiếu = quan hệ, ghi bản số); bảng mô tả field từng collection (`users`, `phoneIndex`, `kyc`, `accounts`, `transactions`, `accountIndex`, `config/rates`); index; các bất biến Security Rules bảo vệ (đổi số dư phải có giao dịch mới khớp, TRANSFER_IN phải có OUT cặp, hoá đơn trả một lần…).

**Chương 4. Hiện thực** (→ *Implementation* của đề)
- Cấu trúc thư mục/package; build, cấu hình (`local.properties`, flavor debug/release, App Check).
- Mỗi tính năng F1–F7 một mục: luồng chính, class tham gia, đoạn code ngắn (≤ 15 dòng, trích code thật) cho chỗ quan trọng, cách đáp ứng yêu cầu đề.
- Bảo mật tổng hợp: Auth, rules, 2FA, eKYC, khoá app, dữ liệu gửi bên thứ ba.

**Chương 5. Giao diện người dùng** (→ *UI*)
- Design system "The Titanium Ledger": màu, chữ, khoảng cách, icon, quy tắc emerald, có ảnh minh hoạ (mockup/ảnh token).
- Ảnh màn hình theo từng tính năng, cả khách hàng và nhân viên (§6.4), mỗi hình có chú thích nói người dùng làm được gì.

**Chương 6. Kiểm thử và đánh giá** — môi trường (Xiaomi, Android 16, emulator Firestore); unit test 391 theo nhóm; rules test 49; kiểm thử trên máy thật theo từng feature (HANDOFF §3); quy trình review; bảng tự đánh giá theo barem; hạn chế đã biết (PROJECT_CONTEXT "Giới hạn đã biết", HANDOFF §8); trạng thái Google Play.

**Chương 7. Kết luận và hướng phát triển** — kết quả, bài học, hướng nâng cấp (Cloud Functions kiểm OTP/token, App Check Enforce, liên ngân hàng thật, lưu chat…).

**Tài liệu tham khảo** — kiểu IEEE đánh số [n], trích trong bài. Nguồn chính: Android Developers (MVVM/Architecture guide, ViewBinding, BiometricPrompt), Firebase (Auth, Firestore, Security Rules, AI Logic, App Check), ML Kit Face Detection, MobileFaceNet (Chen et al., 2018), LiteRT, VNPay sandbox docs, OSMDroid, OSRM, OpenStreetMap, ZXing, Phosphor, Inter, Material Components. Chỉ ghi nguồn thật đã dùng.

## 6. Cách dựng

### 6.1 Thư mục **[Claude]**
```
FLUX_Bank/docs/report/
  src/00-front.md … 07-ket-luan.md, refs.md   # văn bản
  diagrams/*.puml                             # sơ đồ nguồn
  images/                                     # ảnh UI, ảnh giữ chỗ, PNG sơ đồ đã xuất
  template/BAO_CAO.template-goc.docx          # bản sao mẫu Khoa
  build.py                                    # dựng report
  out/FLUX_Bank_BaoCao.docx                   # gitignore
```
Lệnh: `uv run --with python-docx --with docxcompose python docs/report/build.py`.

### 6.2 Quy trình dựng **[Claude]**
1. Xuất sơ đồ: PlantUML jar (ghim phiên bản, tải từ Maven Central, để ngoài repo trong `~/.cache`), PNG đủ nét để in. Dùng graphviz (đã cài 2026-11-23) cho layout.
2. pandoc dựng phần thân với `--reference-doc` là mẫu Khoa → Heading/đoạn văn/bảng mang style của mẫu.
3. `build.py` hậu xử lý: chú thích hình/bảng thành style `Caption` có trường `SEQ Hình`/`SEQ Bảng` đánh theo chương (Hình 3.2); đoạn văn dùng `Nội dung văn bản`; bảng có viền theo mẫu; ảnh co theo khổ chữ; ảnh điện thoại xếp 2–3 ảnh một hàng bằng bảng không viền.
4. Ghép: giữ phần mở đầu của mẫu (thay chữ trên bìa, lời cảm ơn, tóm tắt…), xoá phần thân mẫu (chương RNN/Transformer), chèn phần thân mới vào section đánh số thường; giữ ba trường mục lục.
5. Chủ dự án mở trong ONLYOFFICE → cập nhật toàn bộ trường (mục lục, danh mục hình/bảng) → soát → xuất PDF.

### 6.3 Viết **[Claude]**
- Nguồn sự thật: code trên `main`, `PROJECT_CONTEXT.md`, `HANDOFF.md`, `README.md`, 7 spec trong `docs/superpowers/specs/`, `firestore.rules`, `rules-test/`. Tên class/field trích đúng như code.
- Văn: tiếng Việt, ngôi "tôi" (cá nhân), thuật ngữ kỹ thuật giữ tiếng Anh khi thông dụng, lần đầu có giải thích; mỗi chương soát bằng `vn-humanizer` và soát chính tả.
- Không đưa bí mật (§2 phản mục tiêu); tài khoản thử chỉ ghi email/vai trò, không ghi mật khẩu.

### 6.4 Danh sách ảnh UI cần chụp (chụp sau, dùng chung cho kịch bản video)
- Chung: đăng nhập (dòng chú thích demo), đổi mật khẩu lần đầu, mở khoá sinh trắc.
- Khách hàng: trang chủ; chi tiết Checking / Saving / Mortgage; nạp, rút, biên lai; lịch sử + "Giao dịch tháng N"; chuyển tiền (hub, trong FLUX tra tên, liên ngân hàng); sheet OTP + SMS giả lập; bước quét mặt; VNPay WebView; QR quét / mã của tôi; tiện ích (hoá đơn điện, đã thanh toán, nạp ĐT, danh mục vé/khách sạn, biên lai); chi nhánh (gợi ý đường đi bộ, danh sách, thiếu quyền); trợ lý (AI trả lời + nút, chế độ cơ bản, tin chứa OTP); hồ sơ, sửa SĐT qua OTP.
- Nhân viên: tổng quan; danh sách/tìm khách; tạo khách; chi tiết + sửa khách; mở Saving/Mortgage; chụp eKYC hộ; duyệt/từ chối eKYC; tab Lãi suất.
- Ảnh bản đồ không được lộ vị trí nhà chủ dự án (dùng vị trí giả lập hoặc chụp tại TDTU).

### 6.5 Ảnh UI và nhịp duyệt **[Claude]**
- Đợt viết chỉ đặt **ảnh giữ chỗ** (khung xám ghi tên màn, file `images/ui/<feature>-<man>.png`), không dùng 6 ảnh Play vì đã bị cắt khung. Claude chụp qua adb trên Xiaomi ở một phiên riêng sau khi chủ dự án dọn máy; chụp xong chỉ cần thay file cùng tên rồi dựng lại. Chương 5 viết sẵn chú thích theo danh sách §6.4.
- Claude viết liền một mạch tới bản nháp đầy đủ (mọi chương + sơ đồ + ảnh giữ chỗ, dựng được .docx), chủ dự án duyệt một lần trên bản nháp đó. Mục tiêu bản nháp: **2026-11-26**, để còn thời gian cho video.

## 7. Để lại cho người làm

- Chọn câu chữ, ví dụ minh hoạ, đoạn code trích, số lượng sơ đồ tuần tự cụ thể, bố cục từng mục con.
- Chi tiết kỹ thuật của `build.py` (thư viện ghép, cách thay chữ trên bìa) miễn ra đúng mẫu.
- Thứ tự viết chương (đề xuất: 2 → 3 → 4 → 6 → 1 → 7 → 5 khi có ảnh → phần mở đầu).
