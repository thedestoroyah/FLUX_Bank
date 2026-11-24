"""Dựng phần thân bằng pandoc rồi chỉnh cho đúng mẫu Khoa (style, caption có trường, ảnh, bảng)."""
from __future__ import annotations

import re
import subprocess
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls, qn
from docx.shared import Cm, Pt
from docx.text.paragraph import Paragraph

FILTER = Path(__file__).with_name("figures.lua")
EMU_PER_CM = 360000
MAX_W = int(15.5 * EMU_PER_CM)
MAX_H = int(22 * EMU_PER_CM)
BODY_STYLE = "Nội dung văn bản"
TABLE_CELL_STYLE = "Bảng biểu - nội dung"
TABLE_FONT_PT = 11
PANDOC_BODY_STYLES = {"Body Text", "First Paragraph", "Compact", "BodyText", "FirstParagraph"}
# pandoc ghi style theo id và không khai báo một số style (FirstParagraph, TableCaption),
# nên so cả id lẫn tên.
CAPTION_STYLES = {"Image Caption", "Table Caption", "ImageCaption", "TableCaption"}
CAPTION_RE = re.compile(r"^(Hình|Bảng) (\d+)\.(\d+): (.*)$", re.S)
WP = "{http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing}"
A = "{http://schemas.openxmlformats.org/drawingml/2006/main}"


def pandoc_body(md_files: list[Path], reference: Path, resource_path: list[Path], out: Path) -> Path:
    """Chạy pandoc (kèm figures.lua) với style của `reference`.

    Raises:
        RuntimeError: pandoc lỗi (ví dụ tham chiếu @fig/@tbl không tồn tại), kèm stderr.
    """
    res = subprocess.run(
        ["pandoc", *map(str, md_files), "--from", "markdown", "--lua-filter", str(FILTER),
         "--reference-doc", str(reference),
         "--resource-path", ":".join(str(p) for p in resource_path), "-o", str(out)],
        capture_output=True, text=True,
    )
    if res.returncode != 0:
        raise RuntimeError(f"pandoc lỗi: {res.stderr.strip()}")
    return out


def _field(instr: str, result: str) -> str:
    w = nsdecls("w")
    return (f'<w:r {w}><w:fldChar w:fldCharType="begin"/></w:r>'
            f'<w:r {w}><w:instrText xml:space="preserve"> {instr} </w:instrText></w:r>'
            f'<w:r {w}><w:fldChar w:fldCharType="separate"/></w:r>'
            f'<w:r {w}><w:t>{result}</w:t></w:r>'
            f'<w:r {w}><w:fldChar w:fldCharType="end"/></w:r>')


def _text_run(text: str) -> str:
    safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return f'<w:r {nsdecls("w")}><w:t xml:space="preserve">{safe}</w:t></w:r>'


def _rebuild_caption(p, caption_style) -> None:
    """Đổi đoạn "Hình c.n: chữ" thành caption của mẫu: "Hình {STYLEREF}.{SEQ}: chữ"."""
    m = CAPTION_RE.match(p.text)
    if not m:
        return
    kind, chap, num, text = m.groups()
    for child in list(p._p):
        if child.tag != qn("w:pPr"):
            p._p.remove(child)
    xml = (_text_run(f"{kind} ") + _field("STYLEREF 1 \\s", chap) + _text_run(".")
           + _field(f"SEQ {kind} \\* ARABIC \\s 1", num) + _text_run(f": {text}"))
    for r in parse_xml(f'<w:root {nsdecls("w")}>{xml}</w:root>'):
        p._p.append(r)
    p.style = caption_style


TBLPR_ORDER = ["tblStyle", "tblpPr", "tblOverlap", "bidiVisual", "tblStyleRowBandSize", "tblStyleColBandSize",
               "tblW", "jc", "tblCellSpacing", "tblInd", "tblBorders", "shd", "tblLayout", "tblCellMar",
               "tblLook", "tblCaption", "tblDescription"]


def _put_tbl_pr(tbl, element) -> None:
    """Đặt `element` vào `w:tblPr` đúng thứ tự schema OOXML, thay phần tử cùng tên nếu có."""
    tbl_pr = tbl.find(qn("w:tblPr"))
    name = element.tag.split("}")[1]
    old = tbl_pr.find(element.tag)
    if old is not None:
        tbl_pr.remove(old)
    later = TBLPR_ORDER[TBLPR_ORDER.index(name) + 1:]
    for child in tbl_pr:
        if child.tag.split("}")[1] in later:
            child.addprevious(element)
            return
    tbl_pr.append(element)


SHORT_TABLE_ROWS = 5


def _keep_together(doc) -> None:
    """Giữ caption bảng với bảng, bảng ngắn trên một trang, hàng tiêu đề bảng dài với hàng sau nó, ảnh với caption bên dưới."""
    for p in doc.paragraphs:
        if p.style.name == "Caption" and p.text.startswith("Bảng "):
            p.paragraph_format.keep_with_next = True
        nxt = p._p.getnext()
        is_figure_caption = p.style.name == "Caption" and p.text.startswith("Hình ")
        prev = p._p.getprevious()
        if is_figure_caption and prev is not None and prev.tag == qn("w:p") \
                and prev.find(f".//{WP}inline") is not None:
            Paragraph(prev, p._parent).paragraph_format.keep_with_next = True
    for table in doc.tables:
        if _is_phone_grid(table):  # lưới ảnh dính với caption ngay sau nó
            for c in table._cells:
                for p in c.paragraphs:
                    p.paragraph_format.keep_with_next = True
            continue
        if len(table.rows) > SHORT_TABLE_ROWS:
            # Bảng dài được phép ngắt trang, nhưng hàng tiêu đề không bị bỏ lẻ cuối trang.
            for c in table.rows[0].cells:
                for p in c.paragraphs:
                    p.paragraph_format.keep_with_next = True
            continue
        for row in table.rows[:-1]:
            for c in row.cells:
                for p in c.paragraphs:
                    p.paragraph_format.keep_with_next = True


REFERENCE_ENTRY_RE = re.compile(r"^\[\d+\]\s")
REFERENCE_INDENT = Cm(1)


def _hang_references(doc) -> None:
    """Mục tài liệu tham khảo `[n] ...` thụt dòng treo kiểu IEEE: dòng đầu sát lề, các dòng sau thụt vào."""
    for p in doc.paragraphs:
        if REFERENCE_ENTRY_RE.match(p.text):
            p.paragraph_format.left_indent = REFERENCE_INDENT
            p.paragraph_format.first_line_indent = -REFERENCE_INDENT


def _set_borders(tbl, val: str) -> None:
    sides = "".join(f'<w:{s} w:val="{val}" w:sz="4" w:space="0" w:color="auto"/>'
                    for s in ("top", "left", "bottom", "right", "insideH", "insideV"))
    _put_tbl_pr(tbl, parse_xml(f'<w:tblBorders {nsdecls("w")}>{sides}</w:tblBorders>'))


def _plain_cells(table) -> None:
    """Ô bảng căn trái, không thụt đầu dòng; hàng không bị cắt đôi giữa hai trang."""
    for c in table._cells:
        for p in c.paragraphs:
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            p.paragraph_format.first_line_indent = 0
            for r in p.runs:
                r.font.size = Pt(TABLE_FONT_PT)
    for row in table.rows:
        trpr = row._tr.get_or_add_trPr()
        if trpr.find(qn("w:cantSplit")) is None:
            trpr.insert(0, parse_xml(f'<w:cantSplit {nsdecls("w")}/>'))


def _full_width(tbl) -> None:
    """Bảng thường giãn hết khổ chữ (kiểu pct, 5000 = 100%)."""
    _put_tbl_pr(tbl, parse_xml(f'<w:tblW {nsdecls("w")} w:type="pct" w:w="5000"/>'))


def _fit_images(root) -> None:
    """Co ảnh theo tỉ lệ để không vượt khổ chữ 15,5 × 22 cm."""
    for inline in root.iter(f"{WP}inline", f"{WP}anchor"):
        ext = inline.find(f"{WP}extent")
        cx, cy = int(ext.get("cx")), int(ext.get("cy"))
        scale = min(1.0, MAX_W / cx, MAX_H / cy)
        if scale >= 1.0:
            continue
        new_cx, new_cy = int(cx * scale), int(cy * scale)
        ext.set("cx", str(new_cx))
        ext.set("cy", str(new_cy))
        for a_ext in inline.iter(f"{A}ext"):
            if a_ext.get("cx") is not None:
                a_ext.set("cx", str(new_cx))
                a_ext.set("cy", str(new_cy))


def _mark_unnumbered(doc) -> None:
    """Tắt đánh số "CHƯƠNG n." cho tiêu đề {.unnumbered}.

    figures.lua đặt id "unnumbered-k"; pandoc ghi bookmark đó ngay trước đoạn tiêu đề.
    """
    pending = False
    for el in doc.element.body.iterchildren():
        if el.tag == qn("w:bookmarkStart"):
            pending = (el.get(qn("w:name")) or "").startswith("unnumbered-")
        elif el.tag == qn("w:p"):
            names = [b.get(qn("w:name")) or "" for b in el.iter(qn("w:bookmarkStart"))]
            if pending or any(n.startswith("unnumbered-") for n in names):
                num_pr = el.get_or_add_pPr().get_or_add_numPr()
                num_pr.get_or_add_ilvl().val = 0
                num_pr.get_or_add_numId().val = 0
            pending = False


def _is_phone_grid(table) -> bool:
    """Lưới ảnh điện thoại: bảng một hàng, mỗi ô đúng một hình và không có chữ."""
    if len(table.rows) != 1:
        return False
    return all(not c.text.strip() and len(list(c._tc.iter(f"{WP}inline"))) == 1 for c in table._cells)


def _style(doc, name: str):
    """Tra style theo tên hiển thị; python-docx tra "Caption" theo tên nội bộ "caption" nên hụt."""
    return next(s for s in doc.styles if s.name == name)


def postprocess(docx: Path) -> None:
    """Chỉnh file pandoc dựng cho khớp mẫu Khoa (ghi đè tại chỗ)."""
    doc = Document(str(docx))
    caption_style = _style(doc, "Caption")
    body_style = _style(doc, BODY_STYLE)
    cell_style = _style(doc, TABLE_CELL_STYLE)
    for p in doc.paragraphs:
        ppr = p._p.pPr
        style_id = ppr.pStyle.val if ppr is not None and ppr.pStyle is not None else ""
        name = {p.style.name, style_id}
        if name & CAPTION_STYLES:
            _rebuild_caption(p, caption_style)
        elif name & PANDOC_BODY_STYLES:
            p.style = body_style
        elif p._p.find(f".//{WP}inline") is not None:
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    for table in doc.tables:
        if _is_phone_grid(table):
            _set_borders(table._tbl, "none")
            for c in table._cells:
                for p in c.paragraphs:
                    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        else:
            _set_borders(table._tbl, "single")
            _full_width(table._tbl)
            for c in table._cells:
                for p in c.paragraphs:
                    p.style = cell_style
            _plain_cells(table)
    for style in doc.styles:  # khối code: font đơn cách 10 pt, gồm cả các style tô màu cú pháp (…Tok)
        if style.name in ("Source Code", "Verbatim Char") or style.name.endswith("Tok"):
            style.font.name, style.font.size = "Courier New", Pt(10)
    for p in doc.paragraphs:
        if p.style.name.startswith("Heading"):
            p.paragraph_format.keep_with_next = True  # tiêu đề không bị bỏ lẻ cuối trang
        if p.style.name == "Heading 1":  # mẫu Khoa: mỗi chương bắt đầu ở trang mới
            p.paragraph_format.page_break_before = True
    _hang_references(doc)
    _keep_together(doc)
    _mark_unnumbered(doc)
    _fit_images(doc.element.body)
    doc.save(str(docx))
