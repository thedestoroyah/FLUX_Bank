"""Tạo ảnh giữ chỗ cho các màn UI chưa chụp, theo danh sách trong ui-shots.md."""
from __future__ import annotations

import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

SIZE = (1268, 2756)  # khung chụp màn hình của Xiaomi dùng để demo
BG, FG = "#E5E7EB", "#374151"


def read_shots(shots: Path) -> list[tuple[str, str]]:
    """Đọc bảng `| id | màn hình | ghi chú |`, trả về [(id, tên màn)]."""
    rows = []
    for line in shots.read_text(encoding="utf-8").splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if not line.startswith("|") or len(cells) < 2 or cells[0] in ("id", "") or set(cells[0]) <= set("-: "):
            continue
        rows.append((cells[0], cells[1]))
    return rows


def _font(size: int) -> ImageFont.FreeTypeFont:
    path = subprocess.run(["fc-match", "-f", "%{file}", "DejaVu Sans"],
                          capture_output=True, text=True).stdout.strip()
    return ImageFont.truetype(path, size) if path else ImageFont.load_default(size=size)


def _draw(path: Path, shot_id: str, title: str) -> None:
    img = Image.new("RGB", SIZE, BG)
    d = ImageDraw.Draw(img)
    d.rectangle([20, 20, SIZE[0] - 20, SIZE[1] - 20], outline=FG, width=8)
    for text, size, y in (("ẢNH GIỮ CHỖ", 70, 1100), (title, 64, 1300), (shot_id, 48, 1450)):
        f = _font(size)
        w = d.textlength(text, font=f)
        d.text(((SIZE[0] - w) / 2, y), text, fill=FG, font=f)
    img.save(path)


def make_placeholders(shots: Path, out_dir: Path) -> list[Path]:
    """Tạo `out_dir/<id>.png` cho id chưa có file; không bao giờ ghi đè ảnh đã có.

    Returns:
        Các file vừa tạo.
    """
    out_dir.mkdir(parents=True, exist_ok=True)
    made = []
    for shot_id, title in read_shots(shots):
        path = out_dir / f"{shot_id}.png"
        if not path.exists():
            _draw(path, shot_id, title)
            made.append(path)
    return made
