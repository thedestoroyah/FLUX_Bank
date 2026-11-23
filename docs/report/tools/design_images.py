"""Dựng hình minh hoạ design system (bảng màu, thang chữ) từ `DESIGN.md` và font Inter của app."""
from __future__ import annotations

import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

COLOR_LINE = re.compile(r'^\s{2}([a-z0-9-]+):\s*"(#[0-9A-Fa-f]{6,8})"\s*$')
DARK_TEXT, LIGHT_TEXT = "#0F172A", "#FFFFFF"

# Nhóm hiển thị trong hình: (tiêu đề, danh sách tên màu theo DESIGN.md)
COLOR_GROUPS = [
    ("Nền và chữ", ["canvas", "surface", "bento-subcard", "border-hairline", "text-primary",
                    "text-secondary", "text-tertiary"]),
    ("Nút chính và điểm tương tác", ["obsidian", "obsidian-light", "emerald", "emerald-dark",
                                     "emerald-light", "mint"]),
    ("Thẻ Titanium", ["surface", "titanium-mid", "titanium-end", "titanium-border"]),
    ("Sáu màu nhóm tiện ích", ["cat-emerald", "cat-gold", "cat-blue", "cat-sky", "cat-rose", "cat-violet"]),
    ("Trạng thái", ["error", "error-dark", "error-bg", "warning-dark", "warning-bg"]),
]

TYPE_SAMPLES = [
    ("Display / Amount", "inter_bold.ttf", 28, "48.250.000 ₫"),
    ("Title L", "inter_semibold.ttf", 20, "Chuyển tiền trong FLUX"),
    ("Title M / Amount Small", "inter_semibold.ttf", 16, "Chuyển cho Trần Thị Bình   500.000 ₫"),
    ("Body", "inter_regular.ttf", 14, "Nội dung giao dịch, dòng key và value của biên lai"),
    ("Label", "inter_medium.ttf", 13, "Tiết kiệm   Chi nhánh   Trợ lý AI"),
    ("Caption", "inter_regular.ttf", 12, "Hôm nay, 09:41 · Mã giao dịch"),
    ("Section Label", "inter_bold.ttf", 12, "GIAO DỊCH GẦN ĐÂY"),
]


def parse_colors(design_md: Path) -> dict[str, str]:
    """Đọc khối `colors:` trong phần frontmatter của DESIGN.md → {tên: "#RRGGBB[AA]"}."""
    colors: dict[str, str] = {}
    in_block = False
    for line in design_md.read_text(encoding="utf-8").splitlines():
        if line.strip() == "colors:":
            in_block = True
            continue
        if in_block:
            m = COLOR_LINE.match(line)
            if m:
                colors[m.group(1)] = m.group(2).upper()
            elif line and not line.startswith(" "):
                break
    return colors


def _rgb(hex_color: str) -> tuple[int, int, int]:
    """Chuyển "#RRGGBB" hoặc "#RRGGBBAA" (alpha bị bỏ, trộn với trắng) sang RGB."""
    r, g, b = (int(hex_color[i:i + 2], 16) for i in (1, 3, 5))
    if len(hex_color) == 9:
        a = int(hex_color[7:9], 16) / 255
        return tuple(round(c * a + 255 * (1 - a)) for c in (r, g, b))
    return r, g, b


def _text_on(rgb: tuple[int, int, int]) -> str:
    luminance = 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]
    return DARK_TEXT if luminance > 140 else LIGHT_TEXT


def _font(fonts: Path, name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(fonts / name), size)


def _colors_image(colors: dict[str, str], fonts: Path, out: Path) -> Path:
    cell_w, cell_h, gap, margin = 190, 120, 14, 30
    cols = 6
    title_font, name_font, hex_font = (_font(fonts, "inter_bold.ttf", 22), _font(fonts, "inter_semibold.ttf", 15),
                                       _font(fonts, "inter_regular.ttf", 14))
    height = margin
    for _, names in COLOR_GROUPS:
        height += 40 + ((len(names) + cols - 1) // cols) * (cell_h + gap) + 10
    width = margin * 2 + cols * cell_w + (cols - 1) * gap
    img = Image.new("RGB", (width, height + margin), "white")
    d = ImageDraw.Draw(img)
    y = margin
    for title, names in COLOR_GROUPS:
        d.text((margin, y), title, fill=DARK_TEXT, font=title_font)
        y += 40
        for i, name in enumerate(names):
            x = margin + (i % cols) * (cell_w + gap)
            yy = y + (i // cols) * (cell_h + gap)
            rgb = _rgb(colors[name])
            d.rounded_rectangle([x, yy, x + cell_w, yy + cell_h], radius=14, fill=rgb, outline="#D1D5DB")
            ink = _text_on(rgb)
            d.text((x + 12, yy + 12), name, fill=ink, font=name_font)
            d.text((x + 12, yy + cell_h - 30), colors[name], fill=ink, font=hex_font)
        y += ((len(names) + cols - 1) // cols) * (cell_h + gap) + 10
    path = out / "ds-colors.png"
    img.save(path)
    return path


def _type_image(fonts: Path, out: Path) -> Path:
    width, margin, row_h = 1300, 30, 86
    label_font = _font(fonts, "inter_regular.ttf", 15)
    img = Image.new("RGB", (width, margin * 2 + row_h * len(TYPE_SAMPLES)), "white")
    d = ImageDraw.Draw(img)
    for i, (label, font_file, size, sample) in enumerate(TYPE_SAMPLES):
        y = margin + i * row_h
        d.text((margin, y + 18), f"{label}\n{size} sp", fill="#64748B", font=label_font)
        d.text((340, y + 12), sample, fill=DARK_TEXT, font=_font(fonts, font_file, size * 2))
        d.line([margin, y + row_h - 6, width - margin, y + row_h - 6], fill="#E5E7EB", width=1)
    path = out / "ds-type.png"
    img.save(path)
    return path


def make_design_images(design_md: Path, fonts: Path, out_dir: Path) -> list[Path]:
    """Dựng `ds-colors.png` và `ds-type.png` vào `out_dir`; ghi đè mỗi lần để luôn khớp DESIGN.md.

    Args:
        design_md: đường dẫn DESIGN.md (nguồn màu).
        fonts: thư mục chứa các file font Inter của app (`res/font`).
        out_dir: thư mục ghi ảnh.
    Raises:
        KeyError: DESIGN.md thiếu một màu mà hình cần.
    """
    out_dir.mkdir(parents=True, exist_ok=True)
    colors = parse_colors(design_md)
    return [_colors_image(colors, fonts, out_dir), _type_image(fonts, out_dir)]
