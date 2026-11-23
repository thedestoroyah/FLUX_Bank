# Báo cáo cuối kỳ FLUX Bank — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dựng file `FLUX_Bank_BaoCao.docx` theo mẫu Khoa từ Markdown + PlantUML, có đủ 7 chương tiếng Việt, sơ đồ và ảnh giữ chỗ, bản nháp đầy đủ trước 2026-11-26.

**Architecture:** Văn bản ở `docs/report/src/*.md`, sơ đồ ở `docs/report/diagrams/*.puml`. `build.py` xuất sơ đồ (PlantUML + graphviz), tạo ảnh giữ chỗ còn thiếu, chạy pandoc với mẫu Khoa làm `--reference-doc`, hậu xử lý (style, caption có trường SEQ, lưới ảnh điện thoại), điền phần mở đầu của mẫu, ghép phần thân vào section cuối bằng docxcompose, rồi (tuỳ chọn) xuất PDF bằng `x2t` của ONLYOFFICE để soát trang. `check.py` soát nội dung (trích dẫn, ảnh, chữ sót của mẫu, bí mật, chính tả).

**Tech Stack:** pandoc 3.11 (có Lua filter), Python qua `uv run` (python-docx 1.1.2, docxcompose 1.4.0, pillow, pytest), PlantUML jar + graphviz 16, ONLYOFFICE `x2t`, hunspell + từ điển `dictionary-vi`.

**Spec:** `docs/superpowers/specs/2026-11-23-final-report-design.md`

## Global Constraints

- Mọi đường dẫn tương đối tính từ `FLUX_Bank/`. Nhánh `docs/report`. Commit dạng `docs:` / `chore:`; chỉ `git add docs/report docs/superpowers` (không `.idea/*`).
- Lệnh chạy chung (gọi tắt là `RUN`): `uv run --with python-docx==1.1.2 --with docxcompose==1.4.0 --with pillow --with pytest python`.
- Mẫu Khoa: sao chép `../../Mid_Exam/notes/BAO_CAO.template-goc.docx` thành `docs/report/template/BAO_CAO.template-goc.docx` (đường dẫn gốc tính từ `Final_Exam/`: `Mobile_App_Development/Mid_Exam/notes/`).
- Style của mẫu (theo tên): `Heading 1`–`Heading 4` (Heading 1 tự đánh "CHƯƠNG n." bằng numbering), `Nội dung văn bản` (đoạn văn), `Caption`, `table of figures`, `Tiêu đề các trang mở đầu`, `Body Text`.
- Caption phải giống hệt mẫu: `Hình ` + trường `STYLEREF 1 \s` + `.` + trường `SEQ Hình \* ARABIC \s 1` + `: ` + chữ; bảng dùng `Bảng` và `SEQ Bảng \* ARABIC \s 1`. Caption hình đặt **dưới** hình, caption bảng đặt **trên** bảng (như mẫu).
- Khổ chữ: A4, lề trái 3,5 cm, phải 2 cm → bề rộng tối đa của ảnh **15,5 cm**; chiều cao ảnh tối đa **22 cm**.
- Bìa: `PHẠM HUỲNH QUỐC ĐẠT - 52300098` (một dòng, xoá dòng sinh viên 2), `BÁO CÁO CUỐI KỲ`, `PHÁT TRIỂN ỨNG DỤNG DI ĐỘNG`, `ThS. Võ Văn Thành`, `THÀNH PHỐ HỒ CHÍ MINH, NĂM 2026`. Tên đề tài: `FLUX BANK – ỨNG DỤNG NGÂN HÀNG DI ĐỘNG TRÊN ANDROID`; title: `FLUX BANK – A MOBILE BANKING APP FOR ANDROID`.
- Văn: tiếng Việt, ngôi "tôi", thuật ngữ thông dụng giữ tiếng Anh (lần đầu giải thích), tên class/field viết đúng như code trong `` `backtick` ``. Không viết "Chương n" trong tiêu đề `#`.
- Công cụ AI nói thẳng (spec §3): chương 1 có mục "Công cụ AI hỗ trợ"; mọi chỗ nói review ghi là agent AI review độc lập.
- Không bịa: mọi số liệu lấy từ code trên `main`, `PROJECT_CONTEXT.md`, `HANDOFF.md`, `README.md`, spec F1–F7, `firestore.rules`, `rules-test/`. Số đã kiểm 2026-11-23: **391** unit test, **49** rules test, lint 0 lỗi, 148 file Kotlin / ~11.900 dòng.
- Không đưa bí mật: giá trị trong `local.properties` (`vnpay.hash.secret`, `release.*password`), `current_key` trong `app/google-services.json`, debug token App Check, mật khẩu tài khoản thử. Không ảnh bản đồ lộ vị trí nhà.
- Độ dài toàn bài 14.000–17.000 từ (không tính phần mở đầu và tài liệu tham khảo).
- Tài liệu tham khảo kiểu IEEE: trong bài ghi `[n]`, danh sách ở `src/08-tai-lieu.md` dạng `[n] Tác giả, "Tên," *Nguồn*, năm. URL (truy cập 23/11/2026).` Chỉ nguồn thật.

### Quy ước Markdown (mọi chương dùng)

| Cần | Viết |
|---|---|
| Chương | `# Kiến trúc hệ thống và công nghệ` (không số); chương không đánh số: `# TÀI LIỆU THAM KHẢO {.unnumbered}` |
| Mục | `##`, `###` (không gõ số, numbering của mẫu tự đánh) |
| Hình | `![Kiến trúc tổng thể của FLUX Bank](diagrams/architecture.png){#fig:architecture width=15cm}` (đoạn riêng; caption không có chữ "Hình") |
| Nhóm ảnh điện thoại | `::: {#fig:home .phones caption="Trang chủ: (a) …; (b) …"}` + 2–3 dòng `![](ui/<id>.png)` + `:::` |
| Bảng | pipe table, dòng `Table: Các collection Firestore {#tbl:collections}` ngay trên bảng |
| Tham chiếu hình/bảng | `@fig:architecture`, `@tbl:collections` → dựng ra chữ "Hình 2.1", "Bảng 3.4" |
| Ảnh | `diagrams/<id>.png` (sinh ra trong `docs/report/out/diagrams/`), `ui/<id>.png` (ở `docs/report/images/ui/`) |
| Code | khối ```` ```kotlin ```` ≤ 15 dòng, trích code thật |

## Review Focus

1. Danh mục hình/bảng trống hoặc số hình không reset theo chương khi chủ dự án bấm cập nhật trường trong ONLYOFFICE → test ở Task 2 kiểm caption có đúng `STYLEREF 1 \s` + `SEQ Hình \* ARABIC \s 1`, style `Caption`.
2. Chương không đánh số ("TÀI LIỆU THAM KHẢO") bị gắn "CHƯƠNG 8." hoặc tiêu đề bị lặp chữ "Chương" → test ở Task 2 (numPr tắt cho `.unnumbered`) và kiểm ở Task 3 (`#` không bắt đầu bằng "Chương"/số).
3. Ảnh tràn lề hoặc sơ đồ cao quá trang → test ở Task 2 (extent ≤ 15,5 × 22 cm sau khi co).
4. Bí mật lọt vào báo cáo → check ở Task 3 đọc giá trị bí mật thật từ `local.properties`/`google-services.json` lúc chạy và báo nếu xuất hiện trong `src/` hoặc docx (không in giá trị).
5. Chữ sót của mẫu giữa kỳ (sinh viên 2, Trần Đại Nhân, RNN/Transformer, "(Time New Romans – 13)", "……") → test ở Task 1.

---

### Task 1: Khung dựng — phần mở đầu và ghép phần thân

**Files:**
- Create: `docs/report/build.py`, `docs/report/tools/__init__.py`, `docs/report/tools/front.py`, `docs/report/tools/compose.py`, `docs/report/tools/pdf.py`, `docs/report/tests/test_front_compose.py`, `docs/report/template/BAO_CAO.template-goc.docx`, `docs/report/src/front.md`, `docs/report/src/01-gioi-thieu.md` (một đoạn tạm), `docs/report/.gitignore` (`out/`, `__pycache__/`, `.pytest_cache/`)

**Interfaces:**
- Produces:
  - `tools.front.FrontMatter` (dataclass, frozen): `student: str`, `kind: str`, `course: str`, `supervisor: str`, `city_year: str`, `title_vi: str`, `title_en: str`, `thanks: list[str]`, `summary_vi: list[str]`, `summary_en: list[str]`, `abbreviations: list[tuple[str, str]]`, `ai_note: str`.
  - `tools.front.load_front(md: Path) -> FrontMatter` — đọc `src/front.md` (các mục `## LỜI CẢM ƠN`, `## TÓM TẮT`, `## ABSTRACT`, `## CHỮ VIẾT TẮT` dạng bảng 2 cột, `## GHI CHÚ AI`); trường bìa là hằng số theo Global Constraints.
  - `tools.front.fill_front(template: Path, front: FrontMatter, out: Path) -> Path` — thay chữ bìa/bìa lót (tìm theo nội dung, không theo chỉ số), xoá đoạn "HỌ VÀ TÊN SINH VIÊN 2…", điền lời cảm ơn (ngôi "Tôi"), thêm `ai_note` làm đoạn cuối trang cam đoan (trước dòng ngày), điền tóm tắt/abstract thay "(Time New Romans – 13)", thay bảng chữ viết tắt, **xoá toàn bộ phần thân mẫu**: mọi phần tử sau đoạn chứa section break thứ 5 cho tới `w:sectPr` cuối. Giữ nguyên 3 trường TOC.
  - `tools.compose.compose(front_docx: Path, body_docx: Path, out: Path) -> Path` — `docxcompose.Composer` nối phần thân vào cuối (section đánh số thường).
  - `tools.pdf.to_pdf(docx: Path, pdf: Path) -> Path` — `x2t` với file XML tham số (`m_nFormatTo` 513, `m_sAllFontsPath` = `~/.local/share/onlyoffice/desktopeditors/data/fonts/AllFonts.js`, `m_sFontDir` = thư mục đó, `m_sTempDir` trong `out/`), chạy từ `/opt/onlyoffice/desktopeditors/converter`; lỗi → `RuntimeError` kèm stderr.
  - CLI: `RUN docs/report/build.py [--pdf]` → `docs/report/out/FLUX_Bank_BaoCao.docx` (+ `.pdf`). Task này: pandoc chạy thẳng `src/0*.md` với `--reference-doc` (Task 2 thêm hậu xử lý).

- [ ] **Step 1: Viết test** `test_front_compose.py` (fixture dựng một lần với `front.md` tối thiểu và một chương tạm):
  - `test_cover_has_final_fields`: text docx chứa `PHẠM HUỲNH QUỐC ĐẠT - 52300098`, `BÁO CÁO CUỐI KỲ`, `ThS. Võ Văn Thành`; mỗi chuỗi bìa xuất hiện đúng 2 lần trừ người hướng dẫn (1 lần).
  - `test_template_leftovers_removed`: không còn `SINH VIÊN 2`, `72100008`, `GIỮA KỲ`, `Trần Đại Nhân`, `RNN`, `Transformer`, `Time New Romans`, `AAAAA`, `VLSP`, `Vaswani`, `……`.
  - `test_toc_fields_kept`: XML có đủ `TOC \o "1-3" \h \z \u`, `TOC \h \z \c "Hình"`, `TOC \h \z \c "Bảng"`.
  - `test_body_after_last_section`: tiêu đề chương tạm nằm sau section break thứ 5, có style `Heading 1`; tổng số `w:sectPr` = 5.
  - `test_ai_note_on_declaration_page`: `ai_note` nằm giữa đoạn "Nếu phát hiện có bất kỳ sự gian lận" và dòng "TP. Hồ Chí Minh, ngày".
- [ ] **Step 2:** `cd docs/report && RUN -m pytest tests/test_front_compose.py -q` → FAIL (module chưa có).
- [ ] **Step 3:** Hiện thực các hàm ở trên + `build.py`.
- [ ] **Step 4:** Chạy lại test → PASS. Chạy `RUN docs/report/build.py --pdf`, `pdftoppm -r 50 -png` 6 trang đầu vào scratchpad, xem bằng mắt: bìa, bìa lót, cảm ơn, cam đoan, tóm tắt, mục lục đúng chỗ.
- [ ] **Step 5:** Commit `chore: report build skeleton (front matter + compose)`.

### Task 2: Hậu xử lý phần thân — caption, tham chiếu, ảnh, sơ đồ

**Files:**
- Create: `docs/report/tools/figures.lua` (Lua filter), `docs/report/tools/body.py`, `docs/report/tools/diagrams.py`, `docs/report/tools/placeholders.py`, `docs/report/ui-shots.md`, `docs/report/diagrams/_style.iuml`, `docs/report/diagrams/sample.puml` (xoá ở Task 4), `docs/report/tests/test_body.py`
- Modify: `docs/report/build.py`

**Interfaces:**
- Consumes: `fill_front`, `compose`, `to_pdf` (Task 1).
- Produces:
  - `figures.lua`: đánh số hình/bảng theo chương (đếm lại từ 1 sau mỗi `#` có số), thay `@fig:id`/`@tbl:id` bằng chữ "Hình c.n"/"Bảng c.n"; id không tồn tại → lỗi dừng pandoc với tên id. Div `.phones` → bảng 1 hàng không viền, mỗi ô một ảnh rộng 5 cm (2 ảnh) hoặc 4,6 cm (3 ảnh), kèm caption của div. Header `.unnumbered` giữ class để `body.py` nhận ra.
  - `tools.body.pandoc_body(md_files: list[Path], reference: Path, resource_path: list[Path], out: Path) -> Path` — pandoc `--from markdown --lua-filter tools/figures.lua --reference-doc … --resource-path …`.
  - `tools.body.postprocess(docx: Path) -> None` — đoạn văn thường (`Body Text`, `First Paragraph`, `Compact` ngoài bảng) → `Nội dung văn bản`; caption hình/bảng → style `Caption` có trường như Global Constraints (bỏ chữ số tĩnh); heading `.unnumbered` → tắt numbering (`w:numPr` với `numId` 0); ảnh co theo tỉ lệ về ≤ 15,5 × 22 cm; bảng thường có viền đơn 0,5 pt; khối code font `Consolas`/`Courier New` 10 pt.
  - `tools.diagrams.render_all(src: Path, out: Path) -> list[Path]` — `PLANTUML_VERSION` = bản phát hành mới nhất trên Maven Central (`net.sourceforge.plantuml:plantuml`) đã ra ≥ 2 tuần lúc thực hiện, ghi thành hằng; jar tải về `~/.cache/plantuml/plantuml-<ver>.jar` nếu chưa có; xuất PNG `-Sdpi=200` (bỏ qua file `_*.iuml`); chỉ xuất lại khi sha256 nguồn đổi (lưu `out/diagrams/.hashes.json`); lỗi cú pháp → `RuntimeError` có tên file.
  - `diagrams/_style.iuml`: skinparam dùng chung (font Times New Roman 12, đen trắng + một màu nhấn emerald `#10B981`, `linetype ortho` cho class/component).
  - `tools.placeholders.make_placeholders(shots: Path, out_dir: Path) -> list[Path]` — đọc `ui-shots.md` (bảng `id | màn hình | ghi chú`), tạo PNG 1268×2756 nền `#E5E7EB`, khung, chữ id + tên màn ở giữa, **chỉ cho id chưa có file** (không ghi đè ảnh thật).
  - `ui-shots.md`: danh sách ảnh theo spec §6.4, id dạng `<nhóm>-<màn>` (ví dụ `auth-login`, `cus-home`, `cus-transfer-hub`, `off-ekyc-review`), dùng lại cho kịch bản video.

- [ ] **Step 1: Viết test** `test_body.py` (dựng từ một md mẫu trong test có 2 chương, 2 hình, 1 bảng, 1 nhóm `.phones` 3 ảnh, 1 `@fig`, 1 `@tbl`, 1 chương `.unnumbered`, 1 ảnh 4000×300 px và 1 ảnh 300×4000 px):
  - `test_caption_fields`: mỗi caption hình có style `Caption`, instrText chứa `STYLEREF 1 \s` và `SEQ Hình \* ARABIC \s 1`, chữ bắt đầu `Hình `; caption bảng dùng `SEQ Bảng` và nằm trước `w:tbl`.
  - `test_crossrefs_resolved`: text có "Hình 2.1" và "Bảng 1.1" đúng vị trí, không còn `@fig:`/`@tbl:`.
  - `test_numbering_resets_per_chapter`: hình đầu chương 2 là 2.1.
  - `test_unknown_ref_fails`: `@fig:khong-co` → `pandoc_body` ném lỗi có `khong-co`.
  - `test_phones_grid`: một `w:tbl` không viền, 3 ô, mỗi ô một ảnh rộng 4,6 cm, 1 caption.
  - `test_unnumbered_heading`: heading "TÀI LIỆU THAM KHẢO" có `numId` 0.
  - `test_images_fit_page`: mọi `wp:extent` ≤ 15,5 cm ngang và ≤ 22 cm dọc.
  - `test_body_style`: đoạn văn thường có style `Nội dung văn bản`.
  - `test_placeholders_do_not_overwrite`: file đã tồn tại giữ nguyên byte; file thiếu được tạo đúng 1268×2756.
  - `test_diagram_rerender_only_on_change`: chạy `render_all` 2 lần, lần 2 không đổi mtime PNG; sửa nguồn thì đổi.
- [ ] **Step 2:** `cd docs/report && RUN -m pytest tests -q` → test mới FAIL.
- [ ] **Step 3:** Hiện thực; `build.py` nối: placeholders → diagrams → pandoc_body (`resource_path=[out, images]`) → postprocess → fill_front → compose → (to_pdf).
- [ ] **Step 4:** Test PASS; dựng `--pdf`, xem trang có bảng, nhóm ảnh, sơ đồ mẫu; mở `out/FLUX_Bank_BaoCao.docx` bằng ONLYOFFICE không cần — PDF đủ.
- [ ] **Step 5:** Commit `chore: report body post-processing, diagrams and placeholders`.

### Task 3: Soát nội dung — `check.py`

**Files:**
- Create: `docs/report/check.py`, `docs/report/tools/spell-allow.txt`, `docs/report/tests/test_check.py`

**Interfaces:**
- Produces: `check.run(report_dir: Path, repo_root: Path) -> list[str]` (danh sách lỗi, rỗng = đạt) và CLI `RUN docs/report/check.py [--spell]` in lỗi + bảng số từ mỗi chương + tổng, exit 1 nếu có lỗi. `build.py` gọi `check.run` trước khi dựng và dừng nếu có lỗi.
  - Trích dẫn: mọi `[n]` trong chương có mục trong `08-tai-lieu.md`; mọi mục được trích ít nhất một lần; số liên tục từ 1.
  - Ảnh: mọi `ui/…`/`diagrams/…` tồn tại (ui trong `images/ui/`, diagrams có file `.puml` cùng tên); mọi `.puml` (trừ `_*`) được dùng.
  - Tiêu đề: `#` không bắt đầu bằng "Chương"/"CHƯƠNG"/chữ số; `##`/`###` không bắt đầu bằng số.
  - Bí mật: đọc lúc chạy các giá trị `vnpay.hash.secret`, `vnpay.tmn.code`, `release.store.password`, `release.key.password` từ `local.properties`, `current_key` từ `app/google-services.json`, cộng hằng `ef229bcd-a901-41f5-bad0-ad5c06d4ba9f` và các mật khẩu tài khoản thử ghi trong `HANDOFF.md` (`Binh2026a`); báo "lộ bí mật: <tên khoá>" nếu xuất hiện trong `src/`, `diagrams/` hoặc text của docx đầu ra; **không in giá trị**. File không tồn tại thì bỏ qua khoá đó.
  - Chữ sót mẫu: danh sách chuỗi của Task 1 không có trong docx đầu ra.
  - `--spell`: hunspell với từ điển `dictionary-vi` (npm của wooorm, ghim bản mới nhất ≥ 2 tuần, tải `index.aff`/`index.dic` qua jsDelivr về `~/.cache/hunspell-vi/`), bỏ code, backtick, URL, từ trong `spell-allow.txt`; in từ nghi sai kèm file:dòng (không làm exit 1).
- [ ] **Step 1: Viết test** `test_check.py` trên thư mục tạm: `test_missing_reference`, `test_unused_reference`, `test_missing_image`, `test_unused_diagram`, `test_heading_with_chapter_word`, `test_secret_detected_without_printing_value` (giá trị giả trong `local.properties` tạm; message có tên khoá, không có giá trị), `test_clean_report_passes`.
- [ ] **Step 2:** Chạy → FAIL. **Step 3:** Hiện thực. **Step 4:** PASS; `RUN docs/report/check.py --spell` trên repo thật chạy được.
- [ ] **Step 5:** Commit `chore: report content checks`.

---

**Quy trình chung cho Task 4–9 (mỗi chương):**
1. Đọc nguồn sự thật liệt kê trong task (code thật, không chỉ tài liệu); ghi lại số/tên cần dùng.
2. Viết sơ đồ `.puml` (include `_style.iuml`) và chương `.md` theo quy ước.
3. `RUN docs/report/check.py` → 0 lỗi; `RUN docs/report/build.py --pdf` → dựng được.
4. `pdftoppm -r 60` các trang của chương vào scratchpad, xem: sơ đồ đọc được ở cỡ A4 (chữ không nhỏ hơn ~8 pt khi in), không tràn, bảng không vỡ. Sơ đồ quá dày thì tách.
5. Soát giọng văn bằng skill `vn-humanizer` (sửa trong `.md`), chạy `check.py --spell`, sửa lỗi thật / thêm thuật ngữ vào `spell-allow.txt`.
6. Commit `docs: report chapter N …`.

### Task 4: Chương 2 — Kiến trúc hệ thống và công nghệ (barem Architecture)

**Files:** Create `src/02-kien-truc.md`; `diagrams/architecture.puml` (component: app Android ↔ Firebase Auth, Firestore + Rules, AI Logic/Gemini, VNPay sandbox, OSM tiles, OSRM; ML Kit/LiteRT trong máy), `diagrams/layers.puml` (UI → ViewModel → Repository/domain → Firebase, `FluxApp` cấp phát), `diagrams/deployment.puml`. Xoá `diagrams/sample.puml`.
**Nguồn:** `PROJECT_CONTEXT.md`, `FluxApp.kt`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `settings.gradle.kts`, `AndroidManifest.xml`, `ScreenHost.kt`, spec F1–F2 (§ kiến trúc), F6 (lý do OSM), F7 (lý do AI Logic, model), play-release (App Check, ký).
**Nội dung bắt buộc:** các mục ở spec §5 Chương 2; bảng công nghệ cột *Công nghệ · Phiên bản · Dùng cho · Lý do chọn · Phương án đã loại* (phiên bản lấy từ file build); mục "Nguyên tắc và thực hành" (tách tầng, phụ thuộc interface + fake trong test, bất biến số dư bằng transaction + rules, TDD, spec → plan, mockup duyệt trước, review toàn nhánh bởi agent AI). Khoảng 2.500 từ.

### Task 5: Chương 3 (phần 1) — Use case và class diagram (barem Class diagrams)

**Files:** Create `src/03-phan-tich-thiet-ke.md` (mục 3.1–3.2); `diagrams/usecase-overview.puml`, `diagrams/usecase-customer.puml`, `diagrams/usecase-officer.puml`, `diagrams/class-model.puml`, `diagrams/class-repository.puml`, `diagrams/class-domain.puml`, `diagrams/class-security.puml`, `diagrams/class-transfer-ui.puml`, `diagrams/class-fluxapp.puml`.
**Nguồn:** `data/model/`, `data/repository/`, `domain/` (kể cả `utility/`, `branch/`, `assistant/`), `core/security/`, `face/`, `ui/auth/sheet/`, `ui/customer/transfer/`, `FluxApp.kt`, `ui/customer/Utility.kt`.
**Nội dung bắt buộc:** tác nhân Khách hàng, Nhân viên + hệ thống ngoài; bảng đặc tả 6 use case (tên, tác nhân, tiền điều kiện, luồng chính, luồng phụ/ngoại lệ, hậu điều kiện): đăng nhập + khoá sinh trắc, tạo khách, eKYC, chuyển tiền, thanh toán hoá đơn, hỏi trợ lý. Class diagram ghi thuộc tính/phương thức chính đúng tên trong code, đủ loại quan hệ (kế thừa, hiện thực interface, association có bản số, composition, dependency); mỗi sơ đồ có đoạn giải thích quan hệ. Khoảng 3.000 từ.

### Task 6: Chương 3 (phần 2) — Sơ đồ tuần tự và thiết kế CSDL (barem Database design)

**Files:** Modify `src/03-phan-tich-thiet-ke.md` (mục 3.3–3.4); `diagrams/seq-transfer.puml`, `diagrams/seq-ekyc.puml`, `diagrams/seq-vnpay.puml`, `diagrams/seq-assistant.puml`, `diagrams/erd.puml` (ký hiệu chân quạ, collection = thực thể, field tham chiếu = quan hệ có bản số), tuỳ chọn `diagrams/state-otp.puml`.
**Nguồn:** `firestore.rules`, `rules-test/rules.test.mjs`, `FirestoreAccountRepository` (deposit/withdraw/transfer/transferExternal/pay), `FirestoreUserRepository` (`phoneIndex`), `PayeeRepository`, `OtpManager`, `TransactionAuthSheet`/`TransactionAuthViewModel`, `FaceStepController`, `EkycViewModel`, `VnPay.kt`, `VnPayFragment`, `AssistantViewModel`, `GeminiAssistantClient`, `AssistantFallback`, README mục index.
**Nội dung bắt buộc:** lý do chọn Firestore; ERD; một bảng field cho mỗi collection (`users`, `phoneIndex`, `kyc`, `accounts`, `transactions`, `accountIndex`, `config/rates`: *Field · Kiểu · Bắt buộc · Mô tả*); composite index; bảng bất biến Security Rules (*Bất biến · Rule · Ca test*). Khoảng 3.000 từ.

### Task 7: Chương 4 — Hiện thực

**Files:** Create `src/04-hien-thuc.md`; `diagrams/packages.puml` (cây package `com.flux.bank`).
**Nguồn:** cây `app/src/main/java/com/flux/bank/`, `app/src/{debug,release}/`, README (build, `local.properties`), spec + plan F1–F7, các file code được trích.
**Nội dung bắt buộc:** cấu trúc package và vai trò; build/cấu hình (debug/release, ký, App Check, khoá VNPay trong `local.properties` — chỉ tên khoá); mỗi F1–F7 một mục `##` (luồng, class tham gia, 1–2 đoạn code thật ≤ 15 dòng, đối chiếu yêu cầu đề); mục bảo mật tổng hợp (Auth, rules, 2FA OTP, quét mặt ≥ 10.000.000 ₫, khoá app ≥ 60 s, dữ liệu gửi bên thứ ba). Khoảng 4.000 từ.

### Task 8: Chương 6 — Kiểm thử và đánh giá; Chương 1 — Giới thiệu; Chương 7 — Kết luận; Tài liệu tham khảo; phần mở đầu

**Files:** Create `src/06-kiem-thu.md`, `src/07-ket-luan.md`, `src/08-tai-lieu.md`; viết lại `src/01-gioi-thieu.md`, hoàn thiện `src/front.md`.
**Nguồn:** `app/src/test/` (đếm test theo nhóm từ kết quả `./gradlew testDebugUnitTest`: `app/build/test-results/testDebugUnitTest/*.xml`), `rules-test/`, HANDOFF §3 (kết quả chạy máy thật từng feature), §7–§8, PROJECT_CONTEXT "Giới hạn đã biết", đề `docs/FinalProject_2026_2027.pdf`, `docs/Marking.xlsx`, `docs/play/`.
**Nội dung bắt buộc:**
- Ch.6: môi trường (Xiaomi model `2511FPC34G`, Android 16, Firestore emulator), bảng unit test theo nhóm (tổng 391), rules test 49, bảng kiểm thử trên máy thật theo feature (đạt / chưa thử, đúng như HANDOFF), quy trình review bởi agent AI, hạn chế đã biết, trạng thái Google Play lúc viết (AAB đã ký, chưa tải lên, tuỳ chọn), bảng tự đánh giá theo barem. Khoảng 2.000 từ.
- Ch.1: theo spec §5; bảng đối chiếu 7 yêu cầu ↔ màn/mục; mục "Công cụ AI hỗ trợ" theo spec §3. Khoảng 1.500 từ.
- Ch.7: kết quả, bài học, hướng phát triển. Khoảng 600 từ.
- Tài liệu tham khảo: mọi nguồn được trích ở các chương (bổ sung `[n]` vào chương 2–4 nếu thiếu), kiểm URL tồn tại.
- `front.md`: lời cảm ơn (ngôi "Tôi", gửi ThS. Võ Văn Thành), tóm tắt ~200 từ, abstract ~200 từ, bảng chữ viết tắt (mọi viết tắt dùng trong bài), `ai_note` một câu dẫn tới mục 1.x.

### Task 9: Chương 5 — Giao diện người dùng; dựng bản nháp đầy đủ

**Files:** Create `src/05-giao-dien.md`; `images/ui/ds-*.png` (ảnh design system: bảng màu, thang chữ, icon — dựng từ `res/values/colors.xml`, `DESIGN.md` bằng pillow trong `tools/placeholders.py` hoặc chụp mockup HTML); dùng ảnh giữ chỗ theo `ui-shots.md`.
**Nguồn:** `DESIGN.md`, `res/values/` (colors, dimens, themes, styles), mockup trong `docs/superpowers/specs/mockups/`, `ui-shots.md`.
**Nội dung bắt buộc:** design system "The Titanium Ledger" (màu, chữ Inter, khoảng cách, icon Phosphor duotone/regular, quy tắc emerald, vùng chạm ≥ 48dp); khung điều hướng khách/nhân viên; mỗi nhóm màn ở `ui-shots.md` có trong một hình `.phones` với caption mô tả người dùng làm được gì. Khoảng 2.000 từ.
- [ ] Sau chương: dựng toàn bộ `--pdf`, xem mọi trang ở 50 dpi; `check.py --spell` toàn bài; tổng số từ 14.000–17.000.
- [ ] Cập nhật `HANDOFF.md` (mục trạng thái + cách dựng báo cáo + việc còn lại: chụp ảnh UI, cập nhật trường trong ONLYOFFICE, xuất PDF, video) và commit `docs: full report draft`.
