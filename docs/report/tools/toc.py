"""Điền sẵn nội dung mục lục, danh mục hình và danh mục bảng (kèm số trang) vào file docx.

Các trường TOC vẫn giữ nguyên lệnh nên làm mới được trong Word hay ONLYOFFICE; phần điền sẵn là kết quả
của trường, để người mở file thấy ngay danh mục đầy đủ mà không phải cập nhật. Số trang lấy từ PDF do
ONLYOFFICE (`x2t`) dàn trang, cùng bộ dàn trang với ONLYOFFICE Desktop.
"""
from __future__ import annotations

import re
import subprocess
import unicodedata
from dataclasses import dataclass, field
from pathlib import Path

from docx import Document
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls, qn
from docx.text.paragraph import Paragraph

TAB_POS_TWIPS = 8788  # bề rộng chữ: 21 cm trừ lề trái 3,5 cm và lề phải 2 cm
BOOKMARK_PREFIX = "_Toc9"
HEADING_LEVELS = {"Heading 1": 1, "Heading 2": 2, "Heading 3": 3}
ROMAN_OR_ARABIC = re.compile(r"^(?:[ivxlcdm]+|\d+)$")


@dataclass
class Entry:
    """Một dòng của danh mục: tiêu đề (heading) hoặc caption hình/bảng."""

    key: str
    kind: str  # "heading", "figure", "table"
    level: int
    text: str
    paragraph: object = field(default=None, compare=False, repr=False)


@dataclass
class Page:
    """Một trang PDF: nhãn số trang hiển thị ("", "iv", "12") và chữ trên trang."""

    label: str
    text: str


def _norm(s: str) -> str:
    return re.sub(r"\s+", " ", unicodedata.normalize("NFC", s)).strip()


def _is_unnumbered(p: Paragraph) -> bool:
    ppr = p._p.pPr
    num = ppr.find(qn("w:numPr")) if ppr is not None else None
    if num is None:
        return False
    num_id = num.find(qn("w:numId"))
    return num_id is not None and num_id.get(qn("w:val")) == "0"


def collect_entries(doc) -> list[Entry]:
    """Duyệt tài liệu theo thứ tự, trả về các tiêu đề (cấp 1–3, kèm số chương mục) và caption hình, bảng."""
    entries: list[Entry] = []
    counters = [0, 0, 0]
    for p in doc.paragraphs:
        name = p.style.name
        text = _norm(p.text)
        if not text:
            continue
        key = f"e{len(entries)}"
        if name in HEADING_LEVELS:
            level = HEADING_LEVELS[name]
            if level == 1 and _is_unnumbered(p):
                label = text
            else:
                counters[level - 1] += 1
                for i in range(level, 3):
                    counters[i] = 0
                number = ".".join(str(c) for c in counters[:level])
                label = f"CHƯƠNG {number}. {text}" if level == 1 else f"{number} {text}"
            entries.append(Entry(key, "heading", level, label, p))
        elif name == "Caption" and text.startswith(("Hình ", "Bảng ")):
            kind = "figure" if text.startswith("Hình ") else "table"
            entries.append(Entry(key, kind, 0, text, p))
    return entries


def parse_pages(pdf_text: str) -> list[Page]:
    """Tách đầu ra của `pdftotext` (các trang cách nhau bằng ký tự form feed) thành danh sách `Page`."""
    chunks = pdf_text.split("\f")
    if len(chunks) > 1 and chunks[-1].strip() == "":
        chunks.pop()
    pages = []
    for chunk in chunks:
        lines = chunk.splitlines()
        label, start = "", 0
        for i, line in enumerate(lines):
            if line.strip():
                if ROMAN_OR_ARABIC.match(line.strip()):
                    label, start = line.strip(), i + 1
                break
        pages.append(Page(label, "\n".join(lines[start:])))
    return pages


def pdf_pages(pdf: Path) -> list[Page]:
    """Đọc PDF bằng `pdftotext` và trả về từng trang cùng nhãn số trang."""
    out = subprocess.run(["pdftotext", "-layout", str(pdf), "-"], capture_output=True, text=True, check=True)
    return parse_pages(out.stdout)


def find_pages(entries: list[Entry], pages: list[Page]) -> dict[str, str]:
    """Tìm nhãn số trang của từng mục.

    Mục "DANH MỤC …" nằm ở phần mở đầu (số La Mã): lấy trang cuối cùng chứa nó, vì mục lục cũng nhắc tên mục.
    Các mục còn lại chỉ tìm trong phần thân (số thường), theo thứ tự, bắt đầu từ trang của mục trước.

    Raises:
        ValueError: không tìm thấy một mục trong PDF.
    """
    front = [p for p in pages if p.label and not p.label.isdigit()]
    body = [p for p in pages if p.label.isdigit()]
    norm_body = [_norm(p.text) for p in body]
    norm_front = [_norm(p.text) for p in front]
    labels: dict[str, str] = {}
    cursor = 0
    for e in entries:
        needle = _norm(e.text)[:70]
        if e.kind == "heading" and e.text.startswith("DANH MỤC"):
            hits = [front[i].label for i, t in enumerate(norm_front) if needle in t]
            if not hits:
                raise ValueError(f"không thấy mục {e.text!r} ở phần mở đầu của PDF")
            labels[e.key] = hits[-1]
            continue
        for i in range(cursor, len(body)):
            if needle in norm_body[i]:
                labels[e.key] = body[i].label
                cursor = i
                break
        else:
            raise ValueError(f"không thấy mục {e.text!r} trong phần thân của PDF")
    return labels


def _style_id(doc, name: str) -> str:
    return next(s.style_id for s in doc.styles if s.name == name)


def _run(inner: str) -> str:
    return f"<w:r>{inner}</w:r>"


def _fld(kind: str) -> str:
    return _run(f'<w:fldChar w:fldCharType="{kind}"/>')


def _instr(text: str) -> str:
    return _run(f'<w:instrText xml:space="preserve"> {text} </w:instrText>')


def _esc(s: str) -> str:
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def _entry_paragraph(style_id: str, anchor: str, text: str, page: str, head: str, tail: str) -> str:
    link = (f'<w:hyperlink w:anchor="{anchor}" w:history="1">'
            + _run(f'<w:t xml:space="preserve">{_esc(text)}</w:t>') + _run("<w:tab/>")
            + _fld("begin") + _instr(f"PAGEREF {anchor} \\h") + _fld("separate")
            + _run(f"<w:t>{_esc(page)}</w:t>") + _fld("end") + "</w:hyperlink>")
    return (f'<w:p {nsdecls("w")}><w:pPr><w:pStyle w:val="{style_id}"/>'
            f'<w:tabs><w:tab w:val="right" w:leader="dot" w:pos="{TAB_POS_TWIPS}"/></w:tabs></w:pPr>'
            f"{head}{link}{tail}</w:p>")


def _next_bookmark_id(body) -> int:
    ids = [int(b.get(qn("w:id"))) for b in body.iter(qn("w:bookmarkStart")) if (b.get(qn("w:id")) or "").isdigit()]
    return max(ids, default=0) + 1


def _add_bookmark(p: Paragraph, name: str, bm_id: int) -> None:
    start = parse_xml(f'<w:bookmarkStart {nsdecls("w")} w:id="{bm_id}" w:name="{name}"/>')
    end = parse_xml(f'<w:bookmarkEnd {nsdecls("w")} w:id="{bm_id}"/>')
    ppr = p._p.pPr
    if ppr is not None:
        ppr.addnext(start)
    else:
        p._p.insert(0, start)
    p._p.append(end)


def _field_instruction(p: Paragraph) -> str:
    return "".join(i.text or "" for i in p._p.iter(qn("w:instrText"))).strip()


def _has_page_break(node) -> bool:
    return any(b.get(qn("w:type")) == "page" for b in node.iter(qn("w:br")))


def _remove_following_field_paragraphs(first: Paragraph) -> bool:
    """Xoá các đoạn kết quả cũ của một trường TOC trải nhiều đoạn (đếm độ lồng begin/end).

    Returns:
        True nếu vùng kết quả cũ chứa ngắt trang (danh mục kế tiếp phải bắt đầu ở trang mới).
    """
    depth, node, had_break = 0, first._p, _has_page_break(first._p)
    while node is not None:
        for f in node.iter(qn("w:fldChar")):
            kind = f.get(qn("w:fldCharType"))
            depth += 1 if kind == "begin" else -1 if kind == "end" else 0
        nxt = node.getnext()
        if node is not first._p:
            had_break = had_break or _has_page_break(node)
            node.getparent().remove(node)
        if depth <= 0:
            return had_break
        node = nxt
    return had_break


def populate(docx: Path, out: Path, labels: dict[str, str]) -> Path:
    """Điền nội dung cho ba trường TOC của `docx` rồi ghi ra `out`.

    Args:
        docx: file đã dựng, các trường TOC còn là chỗ giữ chỗ.
        out: file kết quả.
        labels: nhãn số trang theo `Entry.key` (từ `find_pages`).
    """
    doc = Document(str(docx))
    entries = collect_entries(doc)
    body = doc.element.body
    bm_id = _next_bookmark_id(body)
    anchors = {}
    for i, e in enumerate(entries):
        anchors[e.key] = f"{BOOKMARK_PREFIX}{i + 1:07d}"
        _add_bookmark(e.paragraph, anchors[e.key], bm_id + i)

    wanted = {
        "main": ("toc", [e for e in entries if e.kind == "heading"]),
        "figure": ("table of figures", [e for e in entries if e.kind == "figure"]),
        "table": ("table of figures", [e for e in entries if e.kind == "table"]),
    }
    for p in list(doc.paragraphs):
        if p._p.getparent() is None:
            continue
        instr = unicodedata.normalize("NFC", _field_instruction(p))
        if not instr.startswith("TOC"):
            continue
        kind = "main" if "\\o" in instr else "figure" if 'c "Hình"' in instr else "table"
        style_prefix, items = wanted[kind]
        had_break = _remove_following_field_paragraphs(p)
        begin = _fld("begin") + _instr(instr.replace("\xa0", " ")) + _fld("separate")
        paragraphs = []
        for n, e in enumerate(items):
            style = f"toc {e.level}" if kind == "main" else style_prefix
            head = begin if n == 0 else ""
            tail = _fld("end") if n == len(items) - 1 else ""
            paragraphs.append(_entry_paragraph(_style_id(doc, style), anchors[e.key], e.text, labels[e.key], head, tail))
        if not paragraphs:
            paragraphs.append(f'<w:p {nsdecls("w")}>{begin}{_fld("end")}</w:p>')
        if had_break:  # giữ ngắt trang sau danh mục: danh mục kế tiếp bắt đầu ở trang mới
            paragraphs.append(f'<w:p {nsdecls("w")}><w:r><w:br w:type="page"/></w:r></w:p>')
        anchor = p._p
        for xml in paragraphs:
            new = parse_xml(xml)
            anchor.addprevious(new)
        anchor.getparent().remove(anchor)
    doc.save(str(out))
    return out
