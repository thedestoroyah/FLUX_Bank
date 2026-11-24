"""Test hậu xử lý phần thân: caption có trường, tham chiếu, lưới ảnh, co ảnh, sơ đồ, ảnh giữ chỗ."""
import os
import re
import time

import pytest
from docx import Document
from docx.oxml.ns import qn
from PIL import Image

from conftest import TEMPLATE

EMU_PER_CM = 360000
MD = """\
# CHƯƠNG MỘT

Xem @tbl:t1 và @fig:wide.

Table: Bảng thử nghiệm {#tbl:t1}

| Cột A | Cột B |
|---|---|
| 1 | 2 |

![Ảnh rộng](wide.png){#fig:wide width=15cm}

# CHƯƠNG HAI

Xem @fig:tall và @fig:phones.

![Ảnh cao](tall.png){#fig:tall}

::: {#fig:phones .phones caption="Ba màn hình điện thoại"}
![](p1.png)
![](p2.png)
![](p3.png)
:::

# TÀI LIỆU THAM KHẢO {.unnumbered}

[1] Một nguồn.
"""


def _instr(p):
    return "".join(i.text or "" for i in p._p.iter(qn("w:instrText")))


@pytest.fixture(scope="module")
def body(tmp_path_factory):
    from tools.body import pandoc_body, postprocess

    tmp = tmp_path_factory.mktemp("body")
    Image.new("RGB", (4000, 300), "gray").save(tmp / "wide.png")
    Image.new("RGB", (300, 4000), "gray").save(tmp / "tall.png")
    for n in (1, 2, 3):
        Image.new("RGB", (1268, 2756), "white").save(tmp / f"p{n}.png")
    (tmp / "doc.md").write_text(MD, encoding="utf-8")
    out = pandoc_body([tmp / "doc.md"], TEMPLATE, [tmp], tmp / "body.docx")
    postprocess(out)
    return Document(str(out))


def _captions(doc):
    return [p for p in doc.paragraphs if p.style.name == "Caption"]


def test_caption_fields(body):
    caps = _captions(body)
    figs = [p for p in caps if p.text.startswith("Hình ")]
    tabs = [p for p in caps if p.text.startswith("Bảng ")]
    assert len(figs) == 3 and len(tabs) == 1
    for p in figs:
        assert "STYLEREF 1 \\s" in _instr(p) and "SEQ Hình \\* ARABIC \\s 1" in _instr(p)
    assert "SEQ Bảng \\* ARABIC \\s 1" in _instr(tabs[0])
    assert tabs[0]._p.getnext().tag == qn("w:tbl")


def test_crossrefs_resolved(body):
    text = "\n".join(p.text for p in body.paragraphs)
    assert "Xem Bảng 1.1 và Hình 1.1." in text
    assert "Xem Hình 2.1 và Hình 2.2." in text
    assert "@fig:" not in text and "@tbl:" not in text


def test_numbering_resets_per_chapter(body):
    figs = [p.text for p in _captions(body) if p.text.startswith("Hình ")]
    assert figs == ["Hình 1.1: Ảnh rộng", "Hình 2.1: Ảnh cao", "Hình 2.2: Ba màn hình điện thoại"]


def test_unknown_ref_fails(tmp_path):
    from tools.body import pandoc_body

    (tmp_path / "bad.md").write_text("# CHƯƠNG\n\nXem @fig:khong-co.\n", encoding="utf-8")
    with pytest.raises(RuntimeError, match="khong-co"):
        pandoc_body([tmp_path / "bad.md"], TEMPLATE, [tmp_path], tmp_path / "bad.docx")


def test_phones_grid(body):
    grids = [t for t in body.tables if len(t.rows) == 1 and len(t.columns) == 3]
    assert len(grids) == 1
    tbl = grids[0]._tbl
    borders = tbl.find(qn("w:tblPr")).find(qn("w:tblBorders"))
    assert borders is not None
    assert all(b.get(qn("w:val")) in ("none", "nil") for b in borders)
    extents = [int(e.get("cx")) for e in tbl.iter("{http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing}extent")]
    assert len(extents) == 3
    assert all(abs(cx - 4.6 * EMU_PER_CM) < 0.05 * EMU_PER_CM for cx in extents)
    nxt = tbl.getnext()
    assert "Ba màn hình điện thoại" in "".join(t.text or "" for t in nxt.iter(qn("w:t")))


def test_unnumbered_heading(body):
    h = next(p for p in body.paragraphs if p.text == "TÀI LIỆU THAM KHẢO")
    num = h._p.find(qn("w:pPr")).find(qn("w:numPr"))
    assert num is not None and num.find(qn("w:numId")).get(qn("w:val")) == "0"


def test_images_fit_page(body):
    ns = "{http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing}extent"
    extents = list(body.element.body.iter(ns))
    assert extents
    for e in extents:
        assert int(e.get("cx")) <= 15.5 * EMU_PER_CM + 1
        assert int(e.get("cy")) <= 22 * EMU_PER_CM + 1


def test_body_style(body):
    p = next(p for p in body.paragraphs if p.text.startswith("Xem Bảng"))
    assert p.style.name == "Nội dung văn bản"


def test_placeholders_do_not_overwrite(tmp_path):
    from tools.placeholders import make_placeholders

    shots = tmp_path / "ui-shots.md"
    shots.write_text("| id | màn hình | ghi chú |\n|---|---|---|\n| cus-home | Trang chủ | |\n"
                     "| cus-history | Lịch sử | |\n", encoding="utf-8")
    out = tmp_path / "ui"
    out.mkdir()
    (out / "cus-home.png").write_bytes(b"real")
    made = make_placeholders(shots, out)
    assert (out / "cus-home.png").read_bytes() == b"real"
    assert made == [out / "cus-history.png"]
    assert Image.open(out / "cus-history.png").size == (1268, 2756)


def test_diagram_rerender_only_on_change(tmp_path):
    from tools.diagrams import render_all

    src, out = tmp_path / "src", tmp_path / "out"
    src.mkdir()
    (src / "a.puml").write_text("@startuml\nA -> B\n@enduml\n", encoding="utf-8")
    (png,) = render_all(src, out)
    first = png.stat().st_mtime_ns
    time.sleep(0.05)
    render_all(src, out)
    assert png.stat().st_mtime_ns == first
    (src / "a.puml").write_text("@startuml\nA -> C\n@enduml\n", encoding="utf-8")
    render_all(src, out)
    assert png.stat().st_mtime_ns != first


def test_diagram_syntax_error_names_file(tmp_path):
    from tools.diagrams import render_all

    src = tmp_path / "src"
    src.mkdir()
    (src / "hong.puml").write_text("@startuml\nclass {{{\n@enduml\n", encoding="utf-8")
    with pytest.raises(RuntimeError, match="hong.puml"):
        render_all(src, tmp_path / "out")


def test_table_cells_use_template_style(body):
    table = next(t for t in body.tables if t.rows[0].cells[0].text == "Cột A")
    styles = {p.style.name for c in table._cells for p in c.paragraphs}
    assert styles == {"Bảng biểu - nội dung"}


def test_tables_span_text_width(body):
    table = next(t for t in body.tables if t.rows[0].cells[0].text == "Cột A")
    tbl_w = table._tbl.find(qn("w:tblPr")).find(qn("w:tblW"))
    assert tbl_w.get(qn("w:type")) == "pct" and tbl_w.get(qn("w:w")) == "5000"


def test_chapters_start_on_new_page(body):
    heads = [p for p in body.paragraphs if p.style.name == "Heading 1"]
    assert len(heads) == 3
    for h in heads:
        assert h._p.pPr.find(qn("w:pageBreakBefore")) is not None, h.text


def test_table_cells_left_aligned_without_indent(body):
    table = next(t for t in body.tables if t.rows[0].cells[0].text == "Cột A")
    for c in table._cells:
        for p in c.paragraphs:
            ppr = p._p.pPr
            assert ppr.find(qn("w:jc")).get(qn("w:val")) == "left"
            assert ppr.find(qn("w:ind")).get(qn("w:firstLine")) == "0"


def test_table_rows_do_not_split_across_pages(body):
    table = next(t for t in body.tables if t.rows[0].cells[0].text == "Cột A")
    for row in table.rows:
        assert row._tr.trPr.find(qn("w:cantSplit")) is not None


PPR_ORDER = ["pStyle", "keepNext", "keepLines", "pageBreakBefore", "framePr", "widowControl", "numPr",
             "suppressLineNumbers", "pBdr", "shd", "tabs", "suppressAutoHyphens", "kinsoku", "wordWrap",
             "overflowPunct", "topLinePunct", "autoSpaceDE", "autoSpaceDN", "bidi", "adjustRightInd",
             "snapToGrid", "spacing", "ind", "contextualSpacing", "mirrorIndents", "suppressOverlap", "jc",
             "textDirection", "textAlignment", "textboxTightWrap", "outlineLvl", "divId", "cnfStyle",
             "rPr", "sectPr", "pPrChange"]


def test_paragraph_properties_follow_schema_order(body):
    ppr_tag = qn("w:pPr")
    for ppr in body.element.body.iter(ppr_tag):
        names = [c.tag.split("}")[1] for c in ppr]
        idx = [PPR_ORDER.index(n) for n in names]
        assert idx == sorted(idx), names


TBLPR_ORDER = ["tblStyle", "tblpPr", "tblOverlap", "bidiVisual", "tblStyleRowBandSize", "tblStyleColBandSize",
               "tblW", "jc", "tblCellSpacing", "tblInd", "tblBorders", "shd", "tblLayout", "tblCellMar",
               "tblLook", "tblCaption", "tblDescription"]


def test_table_properties_follow_schema_order(body):
    for tbl_pr in body.element.body.iter(qn("w:tblPr")):
        idx = [TBLPR_ORDER.index(c.tag.split("}")[1]) for c in tbl_pr]
        assert idx == sorted(idx), [c.tag.split("}")[1] for c in tbl_pr]


def test_table_text_is_11pt(body):
    from docx.shared import Pt

    table = next(t for t in body.tables if t.rows[0].cells[0].text == "Cột A")
    runs = [r for c in table._cells for p in c.paragraphs for r in p.runs]
    assert runs and all(r.font.size == Pt(11) for r in runs)


def test_headings_keep_with_next(body):
    heads = [p for p in body.paragraphs if p.style.name.startswith("Heading")]
    assert heads and all(p.paragraph_format.keep_with_next for p in heads)


def test_table_caption_stays_with_table(body):
    cap = next(p for p in body.paragraphs if p.text.startswith("Bảng 1.1"))
    assert cap.paragraph_format.keep_with_next


def test_short_table_stays_on_one_page(body):
    table = next(t for t in body.tables if t.rows[0].cells[0].text == "Cột A")
    first_rows = table.rows[:-1]
    assert all(p.paragraph_format.keep_with_next for r in first_rows for c in r.cells for p in c.paragraphs)
    last = table.rows[-1]
    assert not any(p.paragraph_format.keep_with_next for c in last.cells for p in c.paragraphs)


def test_figure_stays_with_its_caption(body):
    for cap in (p for p in body.paragraphs if p.text.startswith("Hình ")):
        prev = cap._p.getprevious()
        if prev is not None and prev.tag == qn("w:p") and prev.find(".//" + WP_INLINE) is not None:
            from docx.text.paragraph import Paragraph
            assert Paragraph(prev, cap._parent).paragraph_format.keep_with_next, cap.text


WP_INLINE = "{http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing}inline"


def test_phone_grid_stays_with_caption(body):
    grid = next(t for t in body.tables if len(t.rows) == 1 and len(t.columns) == 3)
    assert all(p.paragraph_format.keep_with_next for c in grid._cells for p in c.paragraphs)


def test_table_with_many_rows_is_allowed_to_split(body):
    # Chỉ bảng ngắn (≤ 5 hàng) được giữ nguyên khối; test này khoá ngưỡng đó.
    from tools.body import SHORT_TABLE_ROWS
    assert SHORT_TABLE_ROWS == 5


def test_code_blocks_use_monospace_10pt(tmp_path):
    from docx.shared import Pt
    from tools.body import pandoc_body, postprocess

    (tmp_path / "c.md").write_text("# CHƯƠNG\n\n```kotlin\nval x = 1\nfun f() = x\n```\n", encoding="utf-8")
    out = pandoc_body([tmp_path / "c.md"], TEMPLATE, [tmp_path], tmp_path / "c.docx")
    postprocess(out)
    doc = Document(str(out))
    code_styles = [s for s in doc.styles if s.name == "Source Code" or s.name == "Verbatim Char" or s.name.endswith("Tok")]
    assert code_styles
    for s in code_styles:
        assert s.font.name == "Courier New" and s.font.size == Pt(10), s.name


@pytest.fixture(scope="module")
def long_table(tmp_path_factory):
    """Bảng 8 hàng (dài hơn SHORT_TABLE_ROWS) sau postprocess."""
    from tools.body import pandoc_body, postprocess

    tmp = tmp_path_factory.mktemp("longtable")
    rows = "\n".join(f"| {i} | giá trị {i} |" for i in range(8))
    md = f"# CHƯƠNG MỘT\n\nTable: Bảng dài {{#tbl:long}}\n\n| Khoá | Giá trị |\n|---|---|\n{rows}\n"
    (tmp / "doc.md").write_text(md, encoding="utf-8")
    out = pandoc_body([tmp / "doc.md"], TEMPLATE, [tmp], tmp / "body.docx")
    postprocess(out)
    return Document(str(out)).tables[0]


def test_long_table_repeats_header_row(long_table):
    assert long_table.rows[0]._tr.trPr.find(qn("w:tblHeader")) is not None
    assert all(r._tr.trPr.find(qn("w:tblHeader")) is None for r in long_table.rows[1:])


def test_long_table_header_stays_with_first_data_row(long_table):
    header = long_table.rows[0]
    assert all(p.paragraph_format.keep_with_next for c in header.cells for p in c.paragraphs)
    last = long_table.rows[-1]
    assert not any(p.paragraph_format.keep_with_next for c in last.cells for p in c.paragraphs)


def test_reference_entries_use_hanging_indent(body):
    entry = next(p for p in body.paragraphs if p.text.startswith("[1] Một nguồn"))
    fmt = entry.paragraph_format
    assert fmt.left_indent is not None and fmt.left_indent > 0
    assert fmt.first_line_indent == -fmt.left_indent
