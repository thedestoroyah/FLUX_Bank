"""Test điền sẵn nội dung mục lục, danh mục hình và danh mục bảng (kèm số trang)."""
import re
import zipfile

import pytest
from docx import Document
from docx.oxml.ns import qn

from conftest import TEMPLATE

MD = """\
# GIỚI THIỆU

## Bối cảnh

Đoạn văn.

![Ảnh một](a.png){#fig:a width=3cm}

### Chi tiết nhỏ

Table: Bảng một {#tbl:t}

| A | B |
|---|---|
| 1 | 2 |

# KIẾN TRÚC

## Tầng dữ liệu

![Ảnh hai](a.png){#fig:b width=3cm}

# TÀI LIỆU THAM KHẢO {.unnumbered}

[1] Một nguồn.
"""


@pytest.fixture(scope="module")
def populated(tmp_path_factory):
    from PIL import Image
    from tools.body import pandoc_body, postprocess
    from tools.compose import compose
    from tools.front import fill_front, load_front
    from tools.toc import collect_entries, populate

    tmp = tmp_path_factory.mktemp("toc")
    Image.new("RGB", (300, 200), "gray").save(tmp / "a.png")
    (tmp / "front.md").write_text(
        "## LỜI CẢM ƠN\n\nCảm ơn.\n\n## TÓM TẮT\n\nTóm tắt.\n\n## ABSTRACT\n\nAbstract.\n\n"
        "## CHỮ VIẾT TẮT\n\n| Viết tắt | Ý nghĩa |\n|---|---|\n| OTP | One-Time Password |\n\n"
        "## GHI CHÚ AI\n\nGhi chú.\n", encoding="utf-8")
    (tmp / "doc.md").write_text(MD, encoding="utf-8")
    front = fill_front(TEMPLATE, load_front(tmp / "front.md"), tmp / "front.docx")
    body = pandoc_body([tmp / "doc.md"], TEMPLATE, [tmp], tmp / "body.docx")
    postprocess(body)
    docx = compose(front, body, tmp / "report.docx")
    entries = collect_entries(Document(str(docx)))
    labels = {e.key: str(10 + i) for i, e in enumerate(entries)}
    out = tmp / "filled.docx"
    populate(docx, out, labels)
    return entries, labels, out


def _xml(path):
    return zipfile.ZipFile(path).read("word/document.xml").decode("utf-8")


def test_collect_entries_numbers_and_kinds(populated):
    entries, _, _ = populated
    heads = [e.text for e in entries if e.kind == "heading"]
    assert "CHƯƠNG 1. GIỚI THIỆU" in heads
    assert "1.1 Bối cảnh" in heads
    assert "1.1.1 Chi tiết nhỏ" in heads
    assert "CHƯƠNG 2. KIẾN TRÚC" in heads
    assert "2.1 Tầng dữ liệu" in heads
    assert "TÀI LIỆU THAM KHẢO" in heads
    assert "DANH MỤC HÌNH VẼ" in heads
    figs = [e.text for e in entries if e.kind == "figure"]
    assert figs == ["Hình 1.1: Ảnh một", "Hình 2.1: Ảnh hai"]
    assert [e.text for e in entries if e.kind == "table"] == ["Bảng 1.1: Bảng một"]


def test_each_list_filled_with_entries_and_pages(populated):
    entries, labels, out = populated
    text = "\n".join(p.text for p in Document(str(out)).paragraphs)
    for e in entries:
        assert e.text in text, e.text
    fig = next(e for e in entries if e.kind == "figure")
    line = next(p.text for p in Document(str(out)).paragraphs
                if p.style.name == "table of figures" and fig.text in p.text)
    assert line.rstrip().endswith(labels[fig.key])


def test_fields_stay_updatable_and_balanced(populated):
    _, _, out = populated
    x = _xml(out).replace("&quot;", '"')
    for instr in ('TOC \\o "1-3" \\h \\z \\u', 'TOC \\h \\z \\c "Hình"', 'TOC \\h \\z \\c "Bảng"'):
        assert instr in x
    begins = x.count('w:fldCharType="begin"')
    ends = x.count('w:fldCharType="end"')
    seps = x.count('w:fldCharType="separate"')
    assert begins == ends == seps
    assert "w:dirty" not in x


def test_hyperlink_anchors_have_bookmarks(populated):
    _, _, out = populated
    x = _xml(out)
    anchors = set(re.findall(r'w:anchor="(_Toc\d+)"', x))
    bookmarks = set(re.findall(r'<w:bookmarkStart [^>]*w:name="(_Toc\d+)"', x))
    assert anchors and anchors <= bookmarks
    ids = re.findall(r'<w:bookmarkStart w:id="(\d+)"', x)
    assert len(ids) == len(set(ids))


def test_pageref_fields_carry_target_and_page(populated):
    entries, labels, out = populated
    x = _xml(out)
    refs = re.findall(r"PAGEREF (_Toc\d+) \\h", x)
    assert len(refs) == len(entries)
    assert all(lbl in x for lbl in labels.values())


def test_entry_paragraphs_use_template_toc_styles(populated):
    _, _, out = populated
    names = {p.style.name for p in Document(str(out)).paragraphs}
    assert {"toc 1", "toc 2", "toc 3", "table of figures"} <= names


def test_parse_pdf_pages_reads_labels():
    from tools.toc import parse_pages

    text = "\n\n\f\f   i  \nLỜI CẢM ƠN\nx\f  vii \nDANH MỤC\f  1 \nCHƯƠNG 1\f 2\nabc"
    pages = parse_pages(text)
    assert [p.label for p in pages] == ["", "", "i", "vii", "1", "2"]
    assert pages[2].text.startswith("LỜI CẢM ƠN")


def test_find_pages_uses_body_pages_and_last_front_page():
    from tools.toc import Entry, Page, find_pages

    pages = [Page("", ""), Page("i", "LỜI CẢM ƠN"), Page("v", "MỤC LỤC DANH MỤC HÌNH VẼ Hình 1.1: Ảnh một 1"),
             Page("vi", "DANH MỤC HÌNH VẼ Hình 1.1: Ảnh một 1"), Page("1", "CHƯƠNG 1. GIỚI THIỆU 1.1 Bối cảnh"),
             Page("2", "Hình 1.1: Ảnh một chú thích"), Page("3", "CHƯƠNG 2. KIẾN TRÚC")]
    entries = [Entry("a", "heading", 1, "DANH MỤC HÌNH VẼ"), Entry("b", "heading", 1, "CHƯƠNG 1. GIỚI THIỆU"),
               Entry("c", "heading", 2, "1.1 Bối cảnh"), Entry("d", "figure", 0, "Hình 1.1: Ảnh một"),
               Entry("e", "heading", 1, "CHƯƠNG 2. KIẾN TRÚC")]
    labels = find_pages(entries, pages)
    assert labels == {"a": "vi", "b": "1", "c": "1", "d": "2", "e": "3"}


def _has_page_break(p):
    return any(b.get(qn("w:type")) == "page" for b in p._p.iter(qn("w:br")))


def test_lists_still_start_on_new_pages(populated):
    _, _, out = populated
    paras = Document(str(out)).paragraphs
    for title in ["DANH MỤC HÌNH VẼ", "DANH MỤC BẢNG BIỂU", "DANH MỤC CÁC CHỮ VIẾT TẮT"]:
        i = next(k for k, p in enumerate(paras) if p.text.strip() == title and p.style.name == "Heading 1")
        assert _has_page_break(paras[i - 1]), title
