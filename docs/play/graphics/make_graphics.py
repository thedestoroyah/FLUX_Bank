"""Dựng hình ảnh trang Google Play cho FLUX Bank (Demo) từ tài nguyên có sẵn của app.

Chạy từ thư mục FLUX_Bank:
    python3 -I docs/play/graphics/make_graphics.py icon
    python3 -I docs/play/graphics/make_graphics.py feature
    python3 -I docs/play/graphics/make_graphics.py screenshots <thư mục ảnh thô từ adb>

Cần: Inkscape (vẽ vector icon), Pillow. Font Inter lấy từ app/src/main/res/font.
"""
import re
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "docs/play/graphics"
DRAWABLE = ROOT / "app/src/main/res/drawable"
FONT = ROOT / "app/src/main/res/font"

# Màu theo DESIGN.md
OBSIDIAN = "#0A0D12"
MUTED = "#64748B"
MINT = (0xE7, 0xF8, 0xEF)
CANVAS = (0xF5, 0xF5, 0xF7)

# Giới hạn ảnh chụp của Play: mỗi cạnh 320–3840 px, cạnh dài không quá 2 lần cạnh ngắn
SHOT_MIN, SHOT_MAX = 320, 3840


def _paths(xml_name: str) -> list[tuple[str, str]]:
    """Lấy (pathData, fillColor #RRGGBB) của các path có màu đặc trong một vector drawable."""
    text = (DRAWABLE / xml_name).read_text(encoding="utf8")
    found = re.findall(r'android:fillColor="#FF([0-9A-Fa-f]{6})"\s+android:pathData="([^"]+)"', text)
    return [(data, f"#{color}") for color, data in found]


def icon_svg() -> str:
    """SVG của icon app: nền titan (gradient như ic_launcher_background) + chữ F và chấm emerald.

    Chỉ lấy vùng an toàn 72×72 giữa viewport 108 của adaptive icon, để chữ F to như icon trên máy.
    """
    shapes = "".join(f'<path d="{d}" fill="{c}"/>' for d, c in _paths("ic_launcher_foreground.xml"))
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="18 18 72 72">'
        '<defs><linearGradient id="g" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse">'
        '<stop offset="0" stop-color="#FFFFFF"/><stop offset="0.55" stop-color="#EFF1F4"/>'
        '<stop offset="1" stop-color="#DCDFE5"/></linearGradient></defs>'
        f'<rect x="0" y="0" width="108" height="108" fill="url(#g)"/>{shapes}</svg>'
    )


def render_icon(size: int) -> Image.Image:
    """Vẽ icon ra ảnh RGBA cạnh [size] bằng Inkscape."""
    with tempfile.TemporaryDirectory() as tmp:
        svg, png = Path(tmp) / "icon.svg", Path(tmp) / "icon.png"
        svg.write_text(icon_svg(), encoding="utf8")
        subprocess.run(
            ["inkscape", str(svg), "--export-type=png", f"--export-filename={png}", "-w", str(size), "-h", str(size)],
            check=True, capture_output=True,
        )
        return Image.open(png).convert("RGBA")


def make_icon() -> Path:
    img = render_icon(512)
    assert img.size == (512, 512) and img.mode == "RGBA"
    path = OUT / "icon-512.png"
    img.save(path)
    return path


def make_feature() -> Path:
    """Ảnh nổi bật 1024×500: dải mint nhạt dần về canvas (như trang chủ), icon bên trái, tên và một dòng mô tả."""
    w, h = 1024, 500
    img = Image.new("RGB", (w, h), CANVAS)
    draw = ImageDraw.Draw(img)
    for y in range(h):
        t = y / (h - 1)
        draw.line([(0, y), (w, y)], fill=tuple(round(MINT[i] + (CANVAS[i] - MINT[i]) * t) for i in range(3)))

    icon = render_icon(220)
    mask = Image.new("L", icon.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, *icon.size], radius=52, fill=255)
    img.paste(icon, (96, (h - 220) // 2), mask)

    title = ImageFont.truetype(str(FONT / "inter_bold.ttf"), 92)
    sub = ImageFont.truetype(str(FONT / "inter_medium.ttf"), 34)
    x = 96 + 220 + 64
    draw.text((x, 168), "FLUX Bank", font=title, fill=OBSIDIAN)
    draw.text((x, 290), "Ngân hàng số · đồ án demo", font=sub, fill=MUTED)

    assert img.size == (1024, 500) and img.mode == "RGB"
    path = OUT / "feature-1024x500.png"
    img.save(path)
    return path


def crop_to_ratio(img: Image.Image) -> Image.Image:
    """Cắt đều trên/dưới (thanh trạng thái, vùng cử chỉ) cho tới khi cạnh dài ≤ 2 × cạnh ngắn."""
    w, h = img.size
    max_h = 2 * w
    if h > max_h:
        top = (h - max_h) // 2
        img = img.crop((0, top, w, top + max_h))
    w, h = img.size
    assert SHOT_MIN <= min(w, h) and max(w, h) <= SHOT_MAX, (w, h)
    assert max(w, h) <= 2 * min(w, h), (w, h)
    return img


def make_screenshots(raw_dir: str) -> list[Path]:
    """Cắt mọi PNG trong [raw_dir] (theo thứ tự tên) thành screenshot-01.png, -02… đúng tỉ lệ Play."""
    raws = sorted(Path(raw_dir).glob("*.png"))
    assert raws, f"Không có ảnh PNG trong {raw_dir}"
    out = []
    for i, raw in enumerate(raws, start=1):
        path = OUT / f"screenshot-{i:02d}.png"
        crop_to_ratio(Image.open(raw).convert("RGB")).save(path)
        out.append(path)
    return out


if __name__ == "__main__":
    assert crop_to_ratio(Image.new("RGB", (1268, 2756))).size == (1268, 2536)  # tự kiểm: màn Xiaomi
    cmd = sys.argv[1] if len(sys.argv) > 1 else ""
    if cmd == "icon":
        print(make_icon())
    elif cmd == "feature":
        print(make_feature())
    elif cmd == "screenshots" and len(sys.argv) > 2:
        print("\n".join(map(str, make_screenshots(sys.argv[2]))))
    else:
        sys.exit(__doc__)
