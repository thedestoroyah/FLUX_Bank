"""Test phần mở đầu (bìa, cam đoan, mục lục) và việc ghép phần thân vào mẫu Khoa."""
import zipfile

from docx import Document
from docx.oxml.ns import qn


def _texts(path):
    return [p.text for p in Document(str(path)).paragraphs]


def _xml(path):
    with zipfile.ZipFile(path) as z:
        return z.read("word/document.xml").decode("utf-8")


def test_cover_has_final_fields(built):
    _, out = built
    text = "\n".join(_texts(out))
    assert text.count("PHẠM HUỲNH QUỐC ĐẠT - 52300098") == 2
    assert text.count("BÁO CÁO CUỐI KỲ") == 2
    assert text.count("PHÁT TRIỂN ỨNG DỤNG DI ĐỘNG") == 2
    # Bìa lót ghi người hướng dẫn một lần; trang cam đoan của mẫu nhắc lại tên đó.
    assert sum(1 for t in _texts(out) if t.strip() == "ThS. Võ Văn Thành") == 1
    assert text.count("ThS. Võ Văn Thành") == 2


def test_template_leftovers_removed(built):
    _, out = built
    text = "\n".join(_texts(out))
    for leftover in ["SINH VIÊN 2", "72100008", "GIỮA KỲ", "Trần Đại Nhân", "RNN",
                     "Transformer", "Time New Romans", "AAAAA", "VLSP", "Vaswani", "……"]:
        assert leftover not in text, leftover


def test_toc_fields_kept(built):
    _, out = built
    xml = _xml(out).replace("&quot;", '"')
    assert 'TOC \\o "1-3" \\h \\z \\u' in xml
    assert 'TOC \\h \\z \\c "Hình"' in xml
    assert 'TOC \\h \\z \\c "Bảng"' in xml


def test_body_after_last_section(built):
    _, out = built
    doc = Document(str(out))
    body = doc.element.body
    sect_breaks = 0
    found_after = None
    for el in body.iterchildren():
        if el.tag == qn("w:p"):
            ppr = el.find(qn("w:pPr"))
            if ppr is not None and ppr.find(qn("w:sectPr")) is not None:
                sect_breaks += 1
                continue
            text = "".join(t.text or "" for t in el.iter(qn("w:t")))
            if text == "Chương tạm thử nghiệm":
                found_after = sect_breaks
    assert found_after == 4  # sau 4 section break trong đoạn → nằm ở section thứ 5
    heading = next(p for p in doc.paragraphs if p.text == "Chương tạm thử nghiệm")
    assert heading.style.name == "Heading 1"
    assert len(body.findall(".//" + qn("w:sectPr"))) == 5


def test_ai_note_on_declaration_page(built):
    front, out = built
    texts = _texts(out)
    fraud = next(i for i, t in enumerate(texts) if t.startswith("Nếu phát hiện có bất kỳ sự gian lận"))
    note = texts.index(front.ai_note)
    date = next(i for i, t in enumerate(texts) if i > fraud and t.strip().startswith("TP. Hồ Chí Minh, ngày"))
    assert fraud < note < date


def test_front_sections_filled(built):
    front, out = built
    text = "\n".join(_texts(out))
    assert "Tôi xin cảm ơn thầy hướng dẫn." in text
    assert "Tóm tắt tiếng Việt của báo cáo." in text
    assert "English abstract of the report." in text
    assert front.title_vi in text and front.title_en in text
    table_text = "\n".join(c.text for t in Document(str(out)).tables for r in t.rows for c in r.cells)
    assert "One-Time Password" in table_text


def _has_page_break(p):
    return any(b.get(qn("w:type")) == "page" for b in p._p.iter(qn("w:br")))


def test_cover_page_break_kept(built):
    _, out = built
    paras = Document(str(out)).paragraphs
    first_city = next(p for p in paras if p.text.startswith("THÀNH PHỐ HỒ CHÍ MINH"))
    assert _has_page_break(first_city)


def test_lists_start_on_new_page(built):
    _, out = built
    paras = Document(str(out)).paragraphs
    for title in ["DANH MỤC HÌNH VẼ", "DANH MỤC BẢNG BIỂU", "DANH MỤC CÁC CHỮ VIẾT TẮT"]:
        i = next(k for k, p in enumerate(paras) if p.text.strip() == title)
        assert _has_page_break(paras[i - 1]), title


def test_ai_note_not_bold(built):
    front, out = built
    note = next(p for p in Document(str(out)).paragraphs if p.text == front.ai_note)
    assert not any(r.bold for r in note.runs)


def test_signature_date_lines_untouched(built):
    _, out = built
    dates = [p.text for p in Document(str(out)).paragraphs if "TP. Hồ Chí Minh, ngày" in p.text]
    assert len(dates) == 2 and all("20.." in d for d in dates)
