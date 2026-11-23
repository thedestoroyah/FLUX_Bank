"""Fixture dùng chung: đường dẫn báo cáo và dựng thử phần mở đầu + phần thân tạm."""
import subprocess
import sys
from pathlib import Path

import pytest

REPORT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPORT))

TEMPLATE = REPORT / "template" / "BAO_CAO.template-goc.docx"

FRONT_MD = """\
## LỜI CẢM ƠN

Tôi xin cảm ơn thầy hướng dẫn.

## TÓM TẮT

Tóm tắt tiếng Việt của báo cáo.

## ABSTRACT

English abstract of the report.

## CHỮ VIẾT TẮT

| Viết tắt | Ý nghĩa |
|---|---|
| OTP | One-Time Password |
| MVVM | Model-View-ViewModel |

## GHI CHÚ AI

Các công cụ AI hỗ trợ được trình bày ở mục 1.5.
"""

BODY_MD = "# Chương tạm thử nghiệm\n\nĐoạn văn của chương tạm.\n"


@pytest.fixture(scope="session")
def built(tmp_path_factory):
    """Dựng một báo cáo nhỏ: phần mở đầu thật của mẫu + một chương tạm."""
    from tools.compose import compose
    from tools.front import fill_front, load_front

    tmp = tmp_path_factory.mktemp("build")
    (tmp / "front.md").write_text(FRONT_MD, encoding="utf-8")
    (tmp / "body.md").write_text(BODY_MD, encoding="utf-8")
    front = load_front(tmp / "front.md")
    front_docx = fill_front(TEMPLATE, front, tmp / "front.docx")
    body_docx = tmp / "body.docx"
    subprocess.run(
        ["pandoc", str(tmp / "body.md"), "--reference-doc", str(TEMPLATE), "-o", str(body_docx)],
        check=True,
    )
    out = compose(front_docx, body_docx, tmp / "report.docx")
    return front, out
