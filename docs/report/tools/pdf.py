"""Xuất PDF không cần giao diện bằng bộ chuyển đổi x2t của ONLYOFFICE (để soát trang)."""
import subprocess
import tempfile
from pathlib import Path
from xml.sax.saxutils import escape

CONVERTER_DIR = Path("/opt/onlyoffice/desktopeditors/converter")
FONTS_JS = Path.home() / ".local/share/onlyoffice/desktopeditors/data/fonts/AllFonts.js"
FORMAT_PDF = 513


def to_pdf(docx: Path, pdf: Path) -> Path:
    """Chuyển `docx` thành `pdf`.

    Lưu ý: x2t không cập nhật trường mục lục; PDF này chỉ để soát bố cục.
    Raises:
        RuntimeError: x2t báo lỗi hoặc không tạo ra file.
    """
    with tempfile.TemporaryDirectory(dir=pdf.parent) as tmp:
        params = Path(tmp) / "params.xml"
        params.write_text(
            '<?xml version="1.0" encoding="utf-8"?><TaskQueueDataConvert>'
            f"<m_sFileFrom>{escape(str(docx.resolve()))}</m_sFileFrom>"
            f"<m_sFileTo>{escape(str(pdf.resolve()))}</m_sFileTo>"
            f"<m_nFormatTo>{FORMAT_PDF}</m_nFormatTo>"
            f"<m_sAllFontsPath>{escape(str(FONTS_JS))}</m_sAllFontsPath>"
            f"<m_sFontDir>{escape(str(FONTS_JS.parent))}</m_sFontDir>"
            f"<m_sTempDir>{escape(tmp)}</m_sTempDir>"
            "</TaskQueueDataConvert>",
            encoding="utf-8",
        )
        if pdf.exists():
            pdf.unlink()
        res = subprocess.run(["./x2t", str(params)], cwd=CONVERTER_DIR,
                             capture_output=True, text=True, timeout=300)
    if res.returncode != 0 or not pdf.exists():
        raise RuntimeError(f"x2t lỗi ({res.returncode}): {res.stderr[-2000:]}")
    return pdf
