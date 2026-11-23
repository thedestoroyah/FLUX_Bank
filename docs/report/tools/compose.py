"""Ghép phần thân (docx do pandoc dựng) vào sau phần mở đầu của mẫu."""
from pathlib import Path

from docx import Document
from docxcompose.composer import Composer


def compose(front_docx: Path, body_docx: Path, out: Path) -> Path:
    """Nối `body_docx` vào cuối `front_docx` (section đánh số trang thường) và ghi ra `out`."""
    composer = Composer(Document(str(front_docx)))
    composer.append(Document(str(body_docx)))
    composer.save(str(out))
    return out
