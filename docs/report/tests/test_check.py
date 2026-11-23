"""Test check.py trên thư mục báo cáo tạm."""
import json

import pytest

import check  # noqa

SECRET = "s3cr3t-value-123"


@pytest.fixture
def report(tmp_path):
    """Một báo cáo tối thiểu hợp lệ + repo giả có local.properties."""
    rep = tmp_path / "docs" / "report"
    (rep / "src").mkdir(parents=True)
    (rep / "diagrams").mkdir()
    (rep / "images" / "ui").mkdir(parents=True)
    (rep / "src" / "02-kien-truc.md").write_text(
        "# KIẾN TRÚC\n\n## Tầng dữ liệu\n\nTheo [1] thì ổn.\n\n"
        "![Sơ đồ](diagrams/arch.png){#fig:arch}\n", encoding="utf-8")
    (rep / "src" / "08-tai-lieu.md").write_text(
        "# TÀI LIỆU THAM KHẢO {.unnumbered}\n\n[1] A. B, \"C,\" D, 2026.\n", encoding="utf-8")
    (rep / "diagrams" / "arch.puml").write_text("@startuml\nA->B\n@enduml\n", encoding="utf-8")
    (tmp_path / "local.properties").write_text(f"vnpay.hash.secret={SECRET}\n", encoding="utf-8")
    return rep, tmp_path


def test_clean_report_passes(report):
    rep, root = report
    assert check.run(rep, root) == []


def test_missing_reference(report):
    rep, root = report
    (rep / "src" / "02-kien-truc.md").write_text(
        "# KIẾN TRÚC\n\nTheo [1] và [2].\n![x](diagrams/arch.png){#fig:arch}\n", encoding="utf-8")
    assert any("[2]" in e and "tài liệu" in e for e in check.run(rep, root))


def test_unused_reference(report):
    rep, root = report
    with open(rep / "src" / "08-tai-lieu.md", "a", encoding="utf-8") as f:
        f.write("\n[2] E. F, \"G,\" H, 2026.\n")
    assert any("[2]" in e and "không được trích" in e for e in check.run(rep, root))


def test_missing_image(report):
    rep, root = report
    with open(rep / "src" / "02-kien-truc.md", "a", encoding="utf-8") as f:
        f.write("\n![](ui/khong-co.png)\n")
    assert any("khong-co.png" in e for e in check.run(rep, root))


def test_unused_diagram(report):
    rep, root = report
    (rep / "diagrams" / "thua.puml").write_text("@startuml\n@enduml\n", encoding="utf-8")
    assert any("thua.puml" in e for e in check.run(rep, root))


@pytest.mark.parametrize("heading", ["# Chương 2. Kiến trúc", "# 2. KIẾN TRÚC", "# kiến trúc"])
def test_bad_chapter_heading(report, heading):
    rep, root = report
    (rep / "src" / "02-kien-truc.md").write_text(
        f"{heading}\n\nTheo [1].\n![x](diagrams/arch.png){{#fig:arch}}\n", encoding="utf-8")
    assert any("tiêu đề" in e for e in check.run(rep, root))


def test_numbered_subheading(report):
    rep, root = report
    with open(rep / "src" / "02-kien-truc.md", "a", encoding="utf-8") as f:
        f.write("\n## 2.1 Tầng giao diện\n")
    assert any("tiêu đề" in e and "2.1" in e for e in check.run(rep, root))


def test_secret_detected_without_printing_value(report):
    rep, root = report
    with open(rep / "src" / "02-kien-truc.md", "a", encoding="utf-8") as f:
        f.write(f"\nKhoá là {SECRET}.\n")
    errors = check.run(rep, root)
    assert any("vnpay.hash.secret" in e for e in errors)
    assert not any(SECRET in e for e in errors)


def test_known_literal_secret_detected(report):
    rep, root = report
    with open(rep / "src" / "02-kien-truc.md", "a", encoding="utf-8") as f:
        f.write("\nToken ef229bcd-a901-41f5-bad0-ad5c06d4ba9f.\n")
    assert any("debug token" in e for e in check.run(rep, root))


def test_secret_in_docx_detected(report, tmp_path):
    from docx import Document

    rep, root = report
    out = rep / "out"
    out.mkdir()
    doc = Document()
    doc.add_paragraph(f"giá trị {SECRET}")
    doc.save(str(out / "FLUX_Bank_BaoCao.docx"))
    assert any("vnpay.hash.secret" in e and "docx" in e for e in check.run(rep, root))


def test_template_leftover_in_docx(report):
    from docx import Document

    rep, root = report
    out = rep / "out"
    out.mkdir()
    doc = Document()
    doc.add_paragraph("Trần Đại Nhân và Transformer")
    doc.save(str(out / "FLUX_Bank_BaoCao.docx"))
    errors = check.run(rep, root)
    assert any("Trần Đại Nhân" in e for e in errors) and any("Transformer" in e for e in errors)


def test_word_counts_reported(report):
    rep, _ = report
    counts = check.word_counts(rep)
    assert counts["02-kien-truc.md"] > 0 and "08-tai-lieu.md" in counts


def test_spell_keeps_capitalized_vietnamese_words(report):
    rep, _ = report
    (rep / "src" / "03-x.md").write_text("# X\n\nBảng Điểm Một `FluxApp` MoneyFormatter.\n", encoding="utf-8")
    flagged = check.spell(rep)
    assert not any(w.endswith((": ản", ": ểm", ": ột", ": ảng")) for w in flagged), flagged


def test_spell_skips_reference_list(report):
    rep, _ = report
    flagged = check.spell(rep)
    assert not any(w.startswith("08-tai-lieu.md") for w in flagged), flagged


def test_spell_checks_only_last_cell_of_table_rows(report):
    rep, _ = report
    (rep / "src" / "03-x.md").write_text(
        "# X\n\n| zxqvtn | wkjhgf | Mô tả bình thường |\n", encoding="utf-8")
    flagged = check.spell(rep)
    assert not any(w.endswith((": zxqvtn", ": wkjhgf")) for w in flagged), flagged


def test_duplicate_label_before_reference(report):
    rep, root = report
    with open(rep / "src" / "02-kien-truc.md", "a", encoding="utf-8") as f:
        f.write("\nXem Hình @fig:arch.\n")
    assert any("lặp chữ" in e and "@fig:arch" in e for e in check.run(rep, root))
