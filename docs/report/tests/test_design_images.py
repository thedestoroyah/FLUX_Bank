"""Test dựng hình design system từ DESIGN.md."""
from pathlib import Path

from PIL import Image

REPO = Path(__file__).resolve().parents[3]
DESIGN = REPO / "DESIGN.md"
FONTS = REPO / "app" / "src" / "main" / "res" / "font"


def test_parse_colors_reads_frontmatter():
    from tools.design_images import parse_colors

    colors = parse_colors(DESIGN)
    assert colors["obsidian"] == "#0A0D12"
    assert colors["emerald"] == "#10B981"
    assert colors["canvas"] == "#F5F5F7"
    assert len(colors) >= 30


def test_make_design_images_creates_pngs_with_swatch_colors(tmp_path):
    from tools.design_images import make_design_images

    made = make_design_images(DESIGN, FONTS, tmp_path)
    names = sorted(p.name for p in made)
    assert names == ["ds-colors.png", "ds-type.png"]
    img = Image.open(tmp_path / "ds-colors.png").convert("RGB")
    present = {rgb for _, rgb in img.getcolors(maxcolors=1_000_000)}
    assert (0x10, 0xB9, 0x81) in present  # emerald
    assert (0x0A, 0x0D, 0x12) in present  # obsidian
    assert img.size[0] >= 1200


def test_design_images_follow_design_md(tmp_path):
    """Đổi một màu trong DESIGN.md thì hình đổi theo (không vẽ cứng)."""
    from tools.design_images import make_design_images

    md = tmp_path / "DESIGN.md"
    md.write_text(DESIGN.read_text(encoding="utf-8").replace('emerald: "#10B981"', 'emerald: "#FF00FF"', 1),
                  encoding="utf-8")
    out = tmp_path / "out"
    make_design_images(md, FONTS, out)
    img = Image.open(out / "ds-colors.png").convert("RGB")
    assert (0xFF, 0x00, 0xFF) in {rgb for _, rgb in img.getcolors(maxcolors=1_000_000)}
