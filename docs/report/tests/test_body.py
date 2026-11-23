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
