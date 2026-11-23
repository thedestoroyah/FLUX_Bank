"""Dựng báo cáo cuối kỳ FLUX Bank từ Markdown theo mẫu Khoa.

Chạy (từ FLUX_Bank/):
    uv run --with python-docx==1.1.2 --with docxcompose==1.4.0 --with "setuptools<80" --with pillow --with pytest \
        python docs/report/build.py [--pdf]
Kết quả: docs/report/out/FLUX_Bank_BaoCao.docx (+ .pdf để soát trang).
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

REPORT = Path(__file__).resolve().parent
sys.path.insert(0, str(REPORT))

import check  # noqa: E402
from tools.body import pandoc_body, postprocess  # noqa: E402
from tools.compose import compose  # noqa: E402
from tools.design_images import make_design_images  # noqa: E402
from tools.diagrams import render_all  # noqa: E402
from tools.front import fill_front, load_front  # noqa: E402
from tools.pdf import to_pdf  # noqa: E402
from tools.placeholders import make_placeholders  # noqa: E402

TEMPLATE = REPORT / "template" / "BAO_CAO.template-goc.docx"
SRC = REPORT / "src"
OUT = REPORT / "out"
IMAGES = REPORT / "images"
DIAGRAMS = REPORT / "diagrams"
SHOTS = REPORT / "ui-shots.md"
DESIGN_MD = REPORT.parents[1] / "DESIGN.md"
APP_FONTS = REPORT.parents[1] / "app" / "src" / "main" / "res" / "font"
OUTPUT_NAME = "FLUX_Bank_BaoCao"


def chapter_files() -> list[Path]:
    """Các chương theo thứ tự tên file `0N-*.md` (không gồm front.md)."""
    return sorted(SRC.glob("[0-9][0-9]-*.md"))


def build(pdf: bool) -> Path:
    """Dựng toàn bộ báo cáo; trả về đường dẫn file .docx."""
    OUT.mkdir(exist_ok=True)
    # Sinh ảnh và sơ đồ trước để bước soát nội dung thấy đủ file.
    make_design_images(DESIGN_MD, APP_FONTS, IMAGES / "ui")
    make_placeholders(SHOTS, IMAGES / "ui")
    render_all(DIAGRAMS, OUT / "diagrams")
    errors = check.run(REPORT, REPORT.parents[1])
    if errors:
        raise SystemExit("Soát nội dung thất bại:\n  " + "\n  ".join(errors))
    front = fill_front(TEMPLATE, load_front(SRC / "front.md"), OUT / "front.docx")
    body = pandoc_body(chapter_files(), TEMPLATE, [OUT, IMAGES], OUT / "body.docx")
    postprocess(body)
    docx = compose(front, body, OUT / f"{OUTPUT_NAME}.docx")
    if pdf:
        to_pdf(docx, OUT / f"{OUTPUT_NAME}.pdf")
    return docx


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--pdf", action="store_true", help="xuất thêm PDF bằng ONLYOFFICE x2t")
    args = parser.parse_args()
    print(build(args.pdf))


if __name__ == "__main__":
    main()
