---
name: FLUX Bank
description: Ứng dụng ngân hàng số Android, giao diện sáng kiểu bento, chữ obsidian, emerald chỉ cho điểm tương tác.
colors:
  canvas: "#F5F5F7"
  surface: "#FFFFFF"
  bento-subcard: "#F9FAFB"
  border-hairline: "#E5E7EB"
  border-subtle: "#F1F3F5"
  obsidian: "#0A0D12"
  obsidian-light: "#1E293B"
  text-primary: "#0F172A"
  text-secondary: "#64748B"
  text-tertiary: "#94A3B8"
  emerald: "#10B981"
  emerald-dark: "#059669"
  emerald-light: "#ECFDF5"
  mint: "#E7F8EF"
  titanium-mid: "#F0F1F3"
  titanium-end: "#E1E3E8"
  titanium-border: "#D1D5DB"
  error: "#EF4444"
  error-dark: "#DC2626"
  error-bg: "#FEF2F2"
  warning-dark: "#B45309"
  warning-bg: "#FEF3C7"
  cat-emerald: "#10B981"
  cat-gold: "#D97706"
  cat-blue: "#0071E3"
  cat-sky: "#0EA5E9"
  cat-rose: "#E11D48"
  cat-violet: "#7C3AED"
  cat-emerald-bg: "#10B9811A"
  cat-gold-bg: "#D977061A"
  cat-blue-bg: "#0071E31A"
  cat-sky-bg: "#0EA5E91A"
  cat-rose-bg: "#E11D481A"
  cat-violet-bg: "#7C3AED1A"
typography:
  display:
    fontFamily: "Inter, Roboto, sans-serif"
    fontSize: "28px"
    fontWeight: 700
    letterSpacing: "-0.02em"
  amount:
    fontFamily: "Inter, Roboto, sans-serif"
    fontSize: "28px"
    fontWeight: 700
    letterSpacing: "-0.02em"
    fontFeature: "tnum"
  title-l:
    fontFamily: "Inter, Roboto, sans-serif"
    fontSize: "20px"
    fontWeight: 600
  title-m:
    fontFamily: "Inter, Roboto, sans-serif"
    fontSize: "16px"
    fontWeight: 600
  amount-small:
    fontFamily: "Inter, Roboto, sans-serif"
    fontSize: "16px"
    fontWeight: 600
    fontFeature: "tnum"
  body:
    fontFamily: "Inter, Roboto, sans-serif"
    fontSize: "14px"
    fontWeight: 400
  label:
    fontFamily: "Inter, Roboto, sans-serif"
    fontSize: "13px"
    fontWeight: 500
  caption:
    fontFamily: "Inter, Roboto, sans-serif"
    fontSize: "12px"
    fontWeight: 400
  section-label:
    fontFamily: "Inter, Roboto, sans-serif"
    fontSize: "12px"
    fontWeight: 700
    letterSpacing: "0.04em"
rounded:
  otp: "12px"
  chip: "14px"
  button: "14px"
  qr: "18px"
  tile: "20px"
  card: "24px"
  hero-tile: "32px"
  pill: "999px"
spacing:
  space-4: "4px"
  space-8: "8px"
  space-12: "12px"
  space-16: "16px"
  space-24: "24px"
  space-32: "32px"
components:
  button-primary:
    backgroundColor: "{colors.obsidian}"
    textColor: "{colors.surface}"
    typography: "{typography.title-m}"
    rounded: "{rounded.button}"
    height: "52px"
  button-secondary:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text-primary}"
    typography: "{typography.title-m}"
    rounded: "{rounded.button}"
    height: "52px"
  button-destructive:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.error-dark}"
    typography: "{typography.title-m}"
    rounded: "{rounded.button}"
    height: "52px"
  button-text:
    textColor: "{colors.emerald-dark}"
    typography: "{typography.label}"
    height: "48px"
  card:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.tile}"
  text-field:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.button}"
  icon-tile:
    rounded: "{rounded.chip}"
    size: "48px"
  filter-chip:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text-primary}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
  filter-chip-selected:
    backgroundColor: "{colors.obsidian}"
    textColor: "{colors.surface}"
  status-pill:
    backgroundColor: "{colors.surface}"
    typography: "{typography.caption}"
    rounded: "{rounded.pill}"
    padding: "4px 8px"
  otp-cell:
    backgroundColor: "{colors.canvas}"
    rounded: "{rounded.otp}"
  otp-cell-active:
    backgroundColor: "{colors.surface}"
  bottom-nav:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text-tertiary}"
    typography: "{typography.caption}"
  bottom-nav-active:
    textColor: "{colors.emerald-dark}"
  qr-button:
    backgroundColor: "{colors.emerald}"
    textColor: "{colors.surface}"
    rounded: "{rounded.qr}"
  bottom-sheet:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.card}"
---

# Design System: FLUX Bank

Nguồn sự thật là code Android trong `app/src/main/res/values/` (`colors.xml`, `dimens.xml`, `type.xml`, `styles.xml`, `themes.xml`). Giá trị `px` trong frontmatter tương ứng `dp`/`sp` trên máy và dùng trực tiếp cho mockup HTML. Mockup đã duyệt: `docs/superpowers/specs/mockups/*.html`.

## Overview

**Creative North Star: "The Titanium Ledger"**

FLUX trông như một cuốn sổ cái làm bằng titan: nền gần trắng, chữ obsidian đậm, mọi thứ thẳng hàng và ít trang trí. Độ tin cậy đến từ sự chính xác (số tiền cột đều, khoảng cách theo thang 4, viền mảnh 1dp), không đến từ màu mè. Vật thể "quý" duy nhất trên màn hình là thẻ Titanium ánh kim ở trang chủ; mọi thứ khác lùi lại làm nền.

Màu xuất hiện theo hai cách và chỉ hai cách: emerald đánh dấu chỗ người dùng có thể chạm hoặc đang tương tác (tab đang chọn, nút QR, ô đang nhập, viền focus, tiền vào), còn sáu màu nhóm tiện ích tô icon duotone trên nền cùng tông 10%. Nút hành động chính luôn màu đen obsidian, không phải emerald.

Đây là giao diện Operate: khách hàng và nhân viên đến để làm xong một việc (chuyển tiền, duyệt eKYC). Mật độ vừa phải, bố cục quen thuộc của app ngân hàng, toàn bộ chữ tiếng Việt, tiền VND dạng `48.250.000 ₫`. Chỉ có giao diện sáng.

**Key Characteristics:**
- Nền canvas `#F5F5F7`, bề mặt trắng, tile phẳng viền hairline, không bóng.
- Chữ Inter, 6 cấp, nhỏ nhất 12sp; số tiền dùng chữ số đều (`tnum`).
- Emerald hiếm và có nghĩa: nó luôn trả lời câu "chạm vào đâu?".
- Thẻ Titanium tỉ lệ thẻ thật 1.586:1 trên dải mint là chữ ký của trang chủ.
- Icon Phosphor: duotone cho tính năng, regular cho khung giao diện.

## Colors

Bảng màu trung tính lạnh, gần như đơn sắc, với một màu nhấn xanh ngọc và sáu màu nhóm chỉ sống bên trong ô icon.

### Primary
- **Obsidian Ink** (`obsidian`): nút chính, chip lọc đang chọn, chữ trên thẻ. Là "mực" của sổ cái. Trạng thái tắt của nút chính giữ màu này ở độ mờ 35%.
- **Ledger Emerald** (`emerald`): điểm tương tác: nút QR nổi giữa thanh tab (kèm bóng emerald), viền focus ô nhập, ô OTP đang nhập, số tiền vào. **Deep Emerald** (`emerald-dark`) cho chữ: tab đang chọn, nút chữ, nhãn hint đang focus, vì emerald gốc không đủ tương phản trên nền trắng.
- **Mint Wash** (`mint`): chỉ dùng cho dải gradient phía sau thẻ trang chủ, nhạt dần về `canvas`. **Emerald Mist** (`emerald-light`) cho nền nhạt của thông báo thành công.

### Secondary
- **Category Hues** (`cat-*`): sáu màu nhóm tiện ích (emerald, gold, blue, sky, rose, violet). Mỗi màu đi cặp với bản 10% alpha (`cat-*-bg`) làm nền ô icon. Ngoài lưới tiện ích và icon tính năng thì không dùng.

### Tertiary
- **Titanium Pearl** (`surface` → `titanium-mid` → `titanium-end`, viền `titanium-border`): gradient chéo 315° của thẻ tài khoản. Không dùng cho bề mặt nào khác.

### Neutral
- **Gallery Canvas** (`canvas`): nền cửa sổ và nền ô OTP chưa nhập.
- **Paper White** (`surface`): card, sheet, thanh tab, ô nhập.
- **Bento Subcard** (`bento-subcard`): khối phụ lồng trong card.
- **Hairline** (`border-hairline`): viền 1dp của tile, nút phụ, chip, ô nhập. **Subtle Rule** (`border-subtle`): đường kẻ chia dòng trong danh sách.
- **Slate Ink / Slate / Mist Slate** (`text-primary` / `text-secondary` / `text-tertiary`): chữ chính, chữ phụ và caption, nhãn section và icon tab chưa chọn.
- **Semantic**: `error` / `error-dark` / `error-bg` cho lỗi và nút xoá; `warning-dark` / `warning-bg` cho trạng thái chờ duyệt.

### Named Rules
**The Emerald Means Touch Rule.** Emerald chỉ xuất hiện ở chỗ người dùng chạm, đang tương tác, hoặc ở tiền vào. Một khối trang trí màu emerald là lỗi.

**The Black Button Rule.** Nút hành động chính luôn màu obsidian. Không bao giờ có nút chính màu emerald.

**The Category Cage Rule.** Màu `cat-*` chỉ sống bên trong ô icon 48dp. Không tô chữ, nền màn hay viền card bằng màu nhóm.

## Typography

**Display Font:** Inter Bold (với Roboto, sans-serif)
**Body Font:** Inter Regular / Medium / SemiBold (với Roboto, sans-serif)

**Character:** Một họ chữ duy nhất, phân cấp bằng độ đậm và cỡ chứ không bằng font khác. Gọn, trung tính, dễ đọc số.

### Hierarchy
- **Display** (700, 28sp, -0.02em): tiêu đề lớn của màn, số dư. Bản **Amount** cùng cỡ thêm `tnum` cho mọi số tiền lớn.
- **Title L** (600, 20sp): tiêu đề màn phụ, tiêu đề sheet.
- **Title M** (600, 16sp): tiêu đề card, chữ trên nút. Bản **Amount Small** thêm `tnum` cho số tiền trong dòng giao dịch.
- **Body** (400, 14sp): nội dung, dòng key–value của biên lai.
- **Label** (500, 13sp): chip, nút chữ, nhãn ô tiện ích.
- **Caption** (400, 12sp, màu `text-secondary`): mô tả phụ, nhãn tab, thời gian.
- **Section Label** (700, 12sp, 0.04em, VIẾT HOA, màu `text-tertiary`): nhãn nhóm phía trên mỗi khối, cách khối trên 16dp, khối dưới 8dp.

### Named Rules
**The Tabular Money Rule.** Mọi số tiền dùng `Amount` hoặc `AmountSmall` (`tnum`) để các cột số thẳng hàng. Không hiện tiền bằng `Body`.

**The 12sp Floor Rule.** Không chữ nào nhỏ hơn 12sp, kể cả nhãn tab.

## Layout

Bố cục một cột, lề ngang 16dp, khoảng cách chỉ lấy từ thang 4 (`space-4` … `space-32`). Danh sách luôn là RecyclerView; vùng chạm tối thiểu 48dp, nút cao 52dp.

Trang chủ khách hàng theo thứ tự: dải mint với thẻ Titanium chiếm phần trên màn, lưới tiện ích 4×3 trong một card, rồi "Giao dịch gần đây". Khung khách hàng có thanh tab 5 mục (Trang chủ · Chuyển tiền · Quét QR nổi giữa · Lịch sử · Cá nhân); khung nhân viên có 5 tab (Tổng quan · Khách hàng · eKYC · Lãi suất · Cá nhân). Màn phụ mở bằng `ScreenHost.openScreen()`, ẩn thanh tab và có nút back ở header.

Bàn phím không được che sheet hay nút xác nhận: sheet dùng `adjustNothing` và tự xử lý inset (`KeyboardInsets`).

### Named Rules
**The Scale-Only Rule.** Chỉ dùng `dimens.xml`; không viết cứng dp hay mã màu trong layout.

## Elevation & Depth

Hệ thống phẳng theo mặc định: chiều sâu đến từ phân lớp tông (canvas `#F5F5F7` → card trắng → subcard `#F9FAFB`) và viền hairline. Bóng đổ chỉ dành cho hai vật thể "nổi" và luôn có màu emerald: thẻ Titanium và nút QR giữa thanh tab (elevation 8dp, `outlineSpotShadowColor` emerald). Sheet xác thực nổi lên bằng lớp phủ tối 55% phía sau, không bằng bóng.

### Shadow Vocabulary
- **Emerald Lift** (elevation 8dp, spot shadow `emerald`): thẻ tài khoản ở trang chủ, nút QR nổi.
- **Scrim** (dim 0.55): phía sau mọi bottom sheet.

### Named Rules
**The Two Floaters Rule.** Chỉ thẻ Titanium và nút QR được đổ bóng. Card, tile, nút thường luôn elevation 0.

## Shapes

Góc bo mềm, lớn dần theo kích thước vật thể: ô OTP 12dp, nút / ô nhập / ô icon 14dp, nút QR 18dp, tile 20dp, thẻ và đỉnh sheet 24dp, ô hero 32dp, chip và pill bo tròn hẳn. Viền luôn 1dp `border-hairline`; trạng thái focus dày lên 1.5dp và đổi sang emerald. Avatar dùng hình tròn.

## Components

### Buttons
- **Shape:** bo 14dp, cao 52dp, chữ Title M.
- **Primary:** nền obsidian, chữ trắng, ripple `obsidian-light`; tắt thì mờ 35%.
- **Secondary:** nền trắng, viền hairline, chữ `text-primary`.
- **Destructive:** như Secondary nhưng chữ `error-dark`, viền `error`.
- **Text:** chữ `emerald-dark`, Label 13sp, cao 48dp.

### Chips
- **Filter chip:** pill, viền hairline, chữ Label. Chưa chọn nền trắng; đang chọn nền obsidian chữ trắng, không có icon tick.
- **Status pill:** capsule trắng viền hairline, Caption đậm, padding 4×8dp (ví dụ trạng thái eKYC).

### Cards / Containers
- **Corner Style:** 20dp (tile), 24dp (thẻ, sheet).
- **Background:** trắng trên nền canvas; khối lồng dùng `bento-subcard`.
- **Shadow Strategy:** không bóng (xem Elevation).
- **Border:** 1dp `border-hairline`.
- **Internal Padding:** 16dp.

### Inputs / Fields
- **Style:** Material Outlined, nền trắng, viền 1dp hairline, bo 14dp, icon đầu/cuối màu `text-tertiary`.
- **Focus:** viền 1.5dp emerald, hint `emerald-dark`.
- **Error:** viền `error`, chữ lỗi `error-dark`.

### Navigation
- **Bottom nav:** nền trắng, elevation 0, luôn hiện nhãn (Caption 12sp), không có pill chỉ báo của Material 3. Chưa chọn `text-tertiary`, đang chọn `emerald-dark`.
- **QR button:** ô vuông bo 18dp màu emerald nổi giữa thanh tab, bóng emerald.

### Utility Tile (signature)
Ô icon 48dp bo 14dp, nền `cat-*-bg`, icon Phosphor duotone tô `cat-*`, nhãn Label bên dưới. Lưới 4 cột trong một card. Danh sách 12 ô và màu của từng ô nằm trong enum `ui/customer/Utility.kt`.

### Titanium Card (signature)
Thẻ tỉ lệ 1.586:1, gradient chéo trắng → `titanium-mid` → `titanium-end`, viền `titanium-border`, bo 24dp, bóng Emerald Lift, đặt trên dải mint. Chữ `text-primary`, số dư dạng Amount, có nút ẩn/hiện số dư (icon mắt).

### Transaction Auth Sheet (signature)
Bottom sheet trắng bo đỉnh 24dp trên scrim 55%: tóm tắt giao dịch (dòng key–value), 6 ô OTP (ô đang nhập viền emerald 1.5dp), bàn phím số tự vẽ (`NumericKeypadView`), banner "SMS giả lập". Giao dịch ≥ 10.000.000 ₫ thêm bước quét mặt. Mọi giao dịch mới phải đi qua sheet này.

## Do's and Don'ts

### Do:
- **Do** lấy màu từ `colors.xml`, khoảng cách từ `dimens.xml`, chữ từ `TextAppearance.Flux.*`, style từ `Widget.Flux.*`.
- **Do** viết mọi chữ hiển thị bằng tiếng Việt trong `strings.xml`, tiền dạng `48.250.000 ₫`.
- **Do** dùng icon Phosphor duotone cho tính năng mới, kèm cặp `cat-*` / `cat-*-bg` có sẵn.
- **Do** làm mockup HTML theo file này và chờ duyệt trước khi code giao diện.

### Don't:
- **Don't** dùng emerald cho nút chính, nền khối hay trang trí (The Emerald Means Touch Rule).
- **Don't** thêm bóng cho card hay tile; chỉ thẻ Titanium và nút QR được nổi.
- **Don't** làm giao diện tối hay thêm màu nhấn mới ngoài bảng màu này.
- **Don't** dùng chữ nhỏ hơn 12sp hay hiện tiền không có `tnum`.
- **Don't** viết cứng dp, sp hay mã màu trong layout.
