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
from tools.toc import collect_entries, find_pages, pdf_pages, populate  # noqa: E402

TEMPLATE = REPORT / "template" / "BAO_CAO.template-goc.docx"
SRC = REPORT / "src"
OUT = REPORT / "out"
IMAGES = REPORT / "images"
DIAGRAMS = REPORT / "diagrams"
SHOTS = REPORT / "ui-shots.md"
DESIGN_MD = REPORT.parents[1] / "DESIGN.md"
APP_FONTS = REPORT.parents[1] / "app" / "src" / "main" / "res" / "font"
OUTPUT_NAME = "FLUX_Bank_BaoCao"
MAX_PAGINATION_PASSES = 5


def chapter_files() -> list[Path]:
    """Các chương theo thứ tự tên file `0N-*.md` (không gồm front.md)."""
    return sorted(SRC.glob("[0-9][0-9]-*.md"))


def build(quick: bool) -> Path:
    """Dựng toàn bộ báo cáo; trả về đường dẫn file .docx."""
    OUT.mkdir(exist_ok=True)
    # Sinh ảnh và sơ đồ trước để bước soát nội dung thấy đủ file.
    make_design_images(DESIGN_MD, APP_FONTS, IMAGES / "ui")
    make_placeholders(SHOTS, IMAGES / "ui")
    render_all(DIAGRAMS, OUT / "diagrams")
    # Xoá bản dựng cũ để lần soát đầu chỉ đọc nguồn Markdown; docx mới được soát lại ở cuối.
    (OUT / f"{OUTPUT_NAME}.docx").unlink(missing_ok=True)
    _fail_on(check.run(REPORT, REPORT.parents[1]))
    front = fill_front(TEMPLATE, load_front(SRC / "front.md"), OUT / "front.docx")
    body = pandoc_body(chapter_files(), TEMPLATE, [OUT, IMAGES], OUT / "body.docx")
    postprocess(body)
    base = compose(front, body, OUT / "base.docx")
    if quick:
        return base
    final = fill_lists(base)
    _fail_on(check.run(REPORT, REPORT.parents[1]))  # lần này có docx: soát bí mật và chữ sót của mẫu
    return final


def _fail_on(errors: list[str]) -> None:
    """Dừng build với danh sách lỗi của check.run nếu có."""
    if errors:
        raise SystemExit("Soát nội dung thất bại:\n  " + "\n  ".join(errors))


def fill_lists(base: Path) -> Path:
    """Điền sẵn mục lục, danh mục hình và bảng kèm số trang thật.

    Số trang chỉ biết sau khi dàn trang, nên lặp: điền → xuất PDF → đọc số trang → điền lại, cho tới khi
    số trang không đổi (độ dài của chính các danh mục làm dịch trang của phần mở đầu).
    """
    from docx import Document

    final = OUT / f"{OUTPUT_NAME}.docx"
    pdf = OUT / f"{OUTPUT_NAME}.pdf"
    entries = collect_entries(Document(str(base)))
    labels = {e.key: "0" for e in entries}
    for _ in range(MAX_PAGINATION_PASSES):
        populate(base, final, labels)
        to_pdf(final, pdf)
        new = find_pages(entries, pdf_pages(pdf))
        if new == labels:
            return final
        labels = new
    raise SystemExit("Số trang của các danh mục không ổn định sau nhiều lần dàn trang")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--pdf", action="store_true", help="giữ để tương thích; PDF luôn được xuất vì cần số trang")
    parser.add_argument("--quick", action="store_true", help="bỏ qua bước điền danh mục (nhanh, danh mục còn trống)")
    args = parser.parse_args()
    print(build(args.quick))


if __name__ == "__main__":
    main()
