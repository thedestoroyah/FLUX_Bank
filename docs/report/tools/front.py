"""Điền phần mở đầu của mẫu báo cáo Khoa (bìa, cảm ơn, cam đoan, tóm tắt, mục lục, viết tắt)."""
from __future__ import annotations

import copy
import re
import unicodedata
from dataclasses import dataclass
from pathlib import Path

from docx import Document
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls, qn
from docx.text.paragraph import Paragraph

STUDENT = "PHẠM HUỲNH QUỐC ĐẠT - 52300098"
KIND = "BÁO CÁO CUỐI KỲ"
COURSE = "PHÁT TRIỂN ỨNG DỤNG DI ĐỘNG"
SUPERVISOR = "ThS. Võ Văn Thành"
CITY_YEAR = "THÀNH PHỐ HỒ CHÍ MINH, NĂM 2026"
TITLE_VI = "FLUX BANK – ỨNG DỤNG NGÂN HÀNG DI ĐỘNG TRÊN ANDROID"
TITLE_EN = "FLUX BANK – A MOBILE BANKING APP FOR ANDROID"

TEMPLATE_SUPERVISOR = "ThS. Trần Đại Nhân"
TOC_PLACEHOLDER = "(Mở bằng ONLYOFFICE và cập nhật trường để tạo danh mục này.)"


@dataclass(frozen=True)
class FrontMatter:
    """Nội dung phần mở đầu; trường bìa mặc định theo hằng số ở trên."""

    thanks: list[str]
    summary_vi: list[str]
    summary_en: list[str]
    abbreviations: list[tuple[str, str]]
    ai_note: str
    student: str = STUDENT
    kind: str = KIND
    course: str = COURSE
    supervisor: str = SUPERVISOR
    city_year: str = CITY_YEAR
    title_vi: str = TITLE_VI
    title_en: str = TITLE_EN


def _sections(md: str) -> dict[str, list[str]]:
    """Tách file front.md theo tiêu đề `## TÊN`, trả về các dòng không rỗng của từng mục."""
    out: dict[str, list[str]] = {}
    current = None
    for line in md.splitlines():
        m = re.match(r"^##\s+(.+?)\s*$", line)
        if m:
            current = m.group(1).upper()
            out[current] = []
        elif current and line.strip():
            out[current].append(line.strip())
    return out


def load_front(md: Path) -> FrontMatter:
    """Đọc `src/front.md` (mục LỜI CẢM ƠN, TÓM TẮT, ABSTRACT, CHỮ VIẾT TẮT, GHI CHÚ AI).

    Raises:
        KeyError: thiếu một mục bắt buộc.
    """
    s = _sections(md.read_text(encoding="utf-8"))
    rows = [r for r in s["CHỮ VIẾT TẮT"] if r.startswith("|") and not re.match(r"^\|[\s:|-]+\|$", r)]
    abbreviations = []
    for r in rows[1:]:  # bỏ hàng tiêu đề của bảng Markdown
        cells = [c.strip() for c in r.strip("|").split("|")]
        abbreviations.append((cells[0], cells[1]))
    return FrontMatter(
        thanks=s["LỜI CẢM ƠN"],
        summary_vi=s["TÓM TẮT"],
        summary_en=s["ABSTRACT"],
        abbreviations=abbreviations,
        ai_note=" ".join(s["GHI CHÚ AI"]),
    )


_KEEP_IN_RUN = (qn("w:br"), qn("w:drawing"), qn("w:lastRenderedPageBreak"))


def _set_text(p: Paragraph, text: str) -> None:
    """Đổi chữ của đoạn, giữ định dạng run đầu tiên.

    Run chứa ngắt trang hoặc hình (logo bìa) được giữ lại, chỉ bỏ phần chữ của nó.
    """
    runs = p.runs
    if not runs:
        p.add_run(text)
        return
    runs[0].text = text
    for r in runs[1:]:
        if any(r._r.find(tag) is not None for tag in _KEEP_IN_RUN):
            for t in r._r.findall(qn("w:t")) + r._r.findall(qn("w:tab")):
                r._r.remove(t)
        else:
            r._r.getparent().remove(r._r)


def _clone_after(p: Paragraph, text: str) -> Paragraph:
    """Chèn sau `p` một bản sao (cùng style, định dạng run đầu) mang chữ mới."""
    new = copy.deepcopy(p._p)
    p._p.addnext(new)
    para = Paragraph(new, p._parent)
    _set_text(para, text)
    return para


def _fill_block(anchor: Paragraph, lines: list[str]) -> None:
    """Thay đoạn `anchor` bằng các đoạn `lines` cùng định dạng."""
    _set_text(anchor, lines[0])
    last = anchor
    for line in lines[1:]:
        last = _clone_after(last, line)


def _reset_field(p: Paragraph, instr: str) -> None:
    """Thay một trường TOC (kể cả kết quả cũ trải nhiều đoạn) bằng trường rỗng cần cập nhật."""
    first = p._p
    # Xoá các đoạn kết quả cũ tới khi trường TOC đóng; kết quả chứa trường PAGEREF lồng
    # bên trong nên phải đếm độ lồng begin/end thay vì dừng ở "end" đầu tiên.
    depth = 0
    lost_page_break = False
    node = first
    while node is not None:
        for f in node.iter(qn("w:fldChar")):
            kind = f.get(qn("w:fldCharType"))
            depth += 1 if kind == "begin" else -1 if kind == "end" else 0
        nxt = node.getnext()
        if node is not first:
            lost_page_break |= any(b.get(qn("w:type")) == "page" for b in node.iter(qn("w:br")))
            node.getparent().remove(node)
        if depth <= 0:
            break
        node = nxt
    for child in list(first):
        if child.tag != qn("w:pPr"):
            first.remove(child)
    runs = (
        f'<w:r {nsdecls("w")}><w:fldChar w:fldCharType="begin" w:dirty="true"/></w:r>'
        f'<w:r {nsdecls("w")}><w:instrText xml:space="preserve"> {instr} </w:instrText></w:r>'
        f'<w:r {nsdecls("w")}><w:fldChar w:fldCharType="separate"/></w:r>'
        f'<w:r {nsdecls("w")}><w:t>{TOC_PLACEHOLDER}</w:t></w:r>'
        f'<w:r {nsdecls("w")}><w:fldChar w:fldCharType="end"/></w:r>'
    )
    if lost_page_break:  # ngắt trang nằm trong đoạn kết quả cũ: đặt lại sau trường
        runs += f'<w:r {nsdecls("w")}><w:br w:type="page"/></w:r>'
    for r in parse_xml(f'<w:root {nsdecls("w")}>{runs}</w:root>'):
        first.append(r)


def _fill_abbreviations(doc: Document, rows: list[tuple[str, str]]) -> None:
    table = doc.tables[0]  # bảng chữ viết tắt là bảng đầu tiên của mẫu
    proto = copy.deepcopy(table.rows[0]._tr)
    for r in list(table.rows):
        table._tbl.remove(r._tr)
    for short, full in rows:
        tr = copy.deepcopy(proto)
        table._tbl.append(tr)
        cells = table.rows[-1].cells
        _set_text(cells[0].paragraphs[0], short)
        _set_text(cells[1].paragraphs[0], full)


def _drop_template_body(doc: Document) -> None:
    """Xoá phần thân mẫu: mọi phần tử sau đoạn chứa section break cuối, trừ sectPr của body."""
    body = doc.element.body
    children = list(body.iterchildren())
    last_break = max(
        i for i, el in enumerate(children)
        if el.tag == qn("w:p") and el.find(qn("w:pPr")) is not None
        and el.find(qn("w:pPr")).find(qn("w:sectPr")) is not None
    )
    for el in children[last_break + 1:]:
        if el.tag != qn("w:sectPr"):
            body.remove(el)


def fill_front(template: Path, front: FrontMatter, out: Path) -> Path:
    """Tạo bản sao mẫu đã điền phần mở đầu, không còn phần thân mẫu.

    Args:
        template: file .docx mẫu Khoa.
        front: nội dung phần mở đầu.
        out: nơi ghi file kết quả.
    Returns:
        `out`.
    """
    doc = Document(str(template))
    paras = list(doc.paragraphs)
    for p in paras:
        t = p.text.strip()
        if t.startswith("HỌ VÀ TÊN SINH VIÊN 2"):
            p._p.getparent().remove(p._p)
        elif t.startswith("PHẠM HUỲNH QUỐC ĐẠT"):
            _set_text(p, front.student)
        elif t == "BÁO CÁO GIỮA KỲ":
            _set_text(p, front.kind)
        elif t == "PHÁT TRIỂN ỨNG DỤNG DI ĐỘNG":
            _set_text(p, front.course)
        elif t == TEMPLATE_SUPERVISOR:
            _set_text(p, front.supervisor)
        elif t.startswith("THÀNH PHỐ HỒ CHÍ MINH, NĂM"):
            _set_text(p, front.city_year)
        elif t.startswith("Chúng em xin chân thành cảm ơn"):
            _fill_block(p, front.thanks)
        elif t.startswith("Tôi xin cam đoan"):
            _set_text(p, p.text.replace(TEMPLATE_SUPERVISOR, front.supervisor))
        elif t.startswith("Ngoài ra, trong Dự án"):
            plain_declaration = p  # đoạn cam đoan không in đậm, dùng làm khuôn cho ghi chú AI
        elif t.startswith("Nếu phát hiện có bất kỳ sự gian lận"):
            fraud = p
        elif t == "TÊN ĐỀ TÀI":
            _set_text(p, front.title_vi)
        elif t == "TITLE":
            _set_text(p, front.title_en)
    note = copy.deepcopy(plain_declaration._p)
    fraud._p.addnext(note)
    _set_text(Paragraph(note, fraud._parent), front.ai_note)

    # Hai đoạn "(Time New Romans – 13)": lần lượt là tóm tắt tiếng Việt rồi tiếng Anh.
    placeholders = [p for p in doc.paragraphs if p.text.startswith("(Time New Romans")]
    _fill_block(placeholders[0], front.summary_vi)
    _fill_block(placeholders[1], front.summary_en)

    for p in list(doc.paragraphs):
        if p._p.getparent() is None:
            continue  # đoạn kết quả của một trường TOC đã bị xoá
        toc = [i.text for i in p._p.iter(qn("w:instrText")) if i.text and i.text.strip().startswith("TOC")]
        if toc:
            # Mẫu lưu "Bảng" dạng NFD; caption sinh ra dùng NFC nên chuẩn hoá để TOC \c khớp SEQ.
            _reset_field(p, unicodedata.normalize("NFC", " ".join(toc[0].split())))

    _fill_abbreviations(doc, front.abbreviations)
    _drop_template_body(doc)
    doc.save(str(out))
    return out
