"""Soát nội dung báo cáo trước khi dựng: trích dẫn, ảnh, tiêu đề, bí mật, chữ sót của mẫu.

Chạy (từ FLUX_Bank/): uv run --with python-docx==1.1.2 python docs/report/check.py [--spell]
Thoát mã 1 nếu có lỗi; in bảng số từ mỗi chương.
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import urllib.request
import zipfile
from pathlib import Path

REPORT = Path(__file__).resolve().parent
sys.path.insert(0, str(REPORT))

DOCX_NAME = "FLUX_Bank_BaoCao.docx"
REFS_FILE = "08-tai-lieu.md"
TEMPLATE_LEFTOVERS = ["SINH VIÊN 2", "72100008", "GIỮA KỲ", "Trần Đại Nhân", "RNN", "Transformer",
                      "Time New Romans", "AAAAA", "VLSP", "Vaswani", "……"]
APPCHECK_DEBUG_TOKEN = "ef229bcd-a901-41f5-bad0-ad5c06d4ba9f"
DEMO_PASSWORD_FILE = "HANDOFF.md"
DEMO_PASSWORD_RE = re.compile(r"mật khẩu đã đổi thành `([^`]+)`")
SECRET_PROPS = ["vnpay.hash.secret", "vnpay.tmn.code", "release.store.password", "release.key.password"]
MIN_SECRET_LEN = 6

DICT_VERSION = "3.0.0"  # dictionary-vi (ra 2023-11-03)
DICT_DIR = Path.home() / ".cache" / "hunspell-vi"
DICT_URL = f"https://cdn.jsdelivr.net/npm/dictionary-vi@{DICT_VERSION}/index."

IMAGE_RE = re.compile(r"!\[[^\]]*\]\(([^)\s]+)\)")
CITE_RE = re.compile(r"\[(\d+)\]")
REF_LINE_RE = re.compile(r"^\[(\d+)\]\s+\S", re.M)


def _chapters(report: Path) -> list[Path]:
    return sorted(report.glob("src/[0-9][0-9]-*.md"))


def _read(p: Path) -> str:
    return p.read_text(encoding="utf-8")


def _secrets(repo: Path) -> dict[str, str]:
    """Đọc các giá trị bí mật lúc chạy (không bao giờ in giá trị)."""
    found: dict[str, str] = {}
    props = repo / "local.properties"
    if props.exists():
        for line in _read(props).splitlines():
            key, _, value = line.partition("=")
            if key.strip() in SECRET_PROPS and len(value.strip()) >= MIN_SECRET_LEN:
                found[key.strip()] = value.strip()
    gs = repo / "app" / "google-services.json"
    if gs.exists():
        for client in json.loads(_read(gs)).get("client", []):
            for k in client.get("api_key", []):
                if k.get("current_key"):
                    found["current_key (google-services.json)"] = k["current_key"]
    handoff = repo / DEMO_PASSWORD_FILE
    if handoff.exists():
        for i, pw in enumerate(DEMO_PASSWORD_RE.findall(_read(handoff))):
            found[f"mật khẩu tài khoản thử #{i + 1}"] = pw
    found["debug token App Check"] = APPCHECK_DEBUG_TOKEN
    return found


def _docx_text(path: Path) -> str:
    with zipfile.ZipFile(path) as z:
        return z.read("word/document.xml").decode("utf-8")


def _check_references(report: Path) -> list[str]:
    errors = []
    refs_path = report / "src" / REFS_FILE
    listed = {int(n) for n in REF_LINE_RE.findall(_read(refs_path))} if refs_path.exists() else set()
    cited: set[int] = set()
    for ch in _chapters(report):
        if ch.name == REFS_FILE:
            continue
        for n in map(int, CITE_RE.findall(re.sub(r"\]\(|\)\{", " ", _read(ch)))):
            cited.add(n)
    for n in sorted(cited - listed):
        errors.append(f"trích dẫn [{n}] không có trong danh sách tài liệu tham khảo")
    for n in sorted(listed - cited):
        errors.append(f"tài liệu [{n}] không được trích trong bài")
    if listed and sorted(listed) != list(range(1, max(listed) + 1)):
        errors.append("số tài liệu tham khảo không liên tục từ 1")
    return errors


def _check_images(report: Path) -> list[str]:
    errors, used_diagrams = [], set()
    for ch in _chapters(report):
        for target in IMAGE_RE.findall(_read(ch)):
            if target.startswith("diagrams/"):
                stem = Path(target).stem
                used_diagrams.add(stem)
                if not (report / "diagrams" / f"{stem}.puml").exists():
                    errors.append(f"{ch.name}: sơ đồ {target} không có file .puml")
            elif target.startswith("ui/"):
                if not (report / "images" / target).exists() and Path(target).stem not in _shot_ids(report):
                    errors.append(f"{ch.name}: ảnh {target} không có file và không có trong ui-shots.md")
            elif not target.startswith("http") and not (report / "images" / target).exists():
                errors.append(f"{ch.name}: ảnh {target} không tồn tại")
    for puml in sorted((report / "diagrams").glob("*.puml")):
        if not puml.name.startswith("_") and puml.stem not in used_diagrams:
            errors.append(f"sơ đồ {puml.name} không được dùng trong bài")
    return errors


def _shot_ids(report: Path) -> set[str]:
    from tools.placeholders import read_shots

    shots = report / "ui-shots.md"
    return {i for i, _ in read_shots(shots)} if shots.exists() else set()


def _check_headings(report: Path) -> list[str]:
    errors = []
    for ch in _chapters(report):
        for lineno, line in enumerate(_read(ch).splitlines(), 1):
            m = re.match(r"^(#{1,4})\s+(.*?)(\s*\{.*\})?\s*$", line)
            if not m:
                continue
            level, title = len(m.group(1)), m.group(2)
            where = f"{ch.name}:{lineno}"
            if re.match(r"^(chương\b|\d)", title, re.I):
                errors.append(f"{where}: tiêu đề không được bắt đầu bằng 'Chương' hoặc số: {title!r}")
            elif level == 1 and title != title.upper():
                errors.append(f"{where}: tiêu đề chương phải viết HOA (theo mẫu): {title!r}")
    return errors


def _check_secrets(report: Path, repo: Path) -> list[str]:
    errors = []
    targets = {str(p.relative_to(report)): _read(p)
               for p in [*_chapters(report), report / "src" / "front.md", *report.glob("diagrams/*")]
               if p.exists() and p.is_file()}
    docx = report / "out" / DOCX_NAME
    if docx.exists():
        targets[f"out/{DOCX_NAME} (docx)"] = _docx_text(docx)
    for name, value in _secrets(repo).items():
        for where, text in targets.items():
            if value in text:
                errors.append(f"lộ bí mật: {name} trong {where}")
    return errors


def _check_leftovers(report: Path) -> list[str]:
    docx = report / "out" / DOCX_NAME
    if not docx.exists():
        return []
    xml = re.sub(r"<[^>]+>", "", _docx_text(docx))
    return [f"chữ sót của mẫu trong docx: {s!r}" for s in TEMPLATE_LEFTOVERS if s in xml]


def word_counts(report: Path) -> dict[str, int]:
    """Số từ của mỗi file chương trong `src/` (bỏ khối code và dòng ảnh)."""
    counts = {}
    for ch in _chapters(report):
        text = re.sub(r"```.*?```", "", _read(ch), flags=re.S)
        text = IMAGE_RE.sub("", text)
        counts[ch.name] = len(re.findall(r"\w+", text))
    return counts


def run(report_dir: Path, repo_root: Path) -> list[str]:
    """Chạy mọi phép soát; trả về danh sách lỗi (rỗng = đạt)."""
    return [*_check_references(report_dir), *_check_images(report_dir), *_check_headings(report_dir),
            *_check_secrets(report_dir, repo_root), *_check_leftovers(report_dir)]


def _ensure_dictionary() -> Path:
    DICT_DIR.mkdir(parents=True, exist_ok=True)
    for ext in ("aff", "dic"):
        target = DICT_DIR / f"vi.{ext}"
        if not target.exists():
            target.write_bytes(urllib.request.urlopen(DICT_URL + ext, timeout=60).read())
    return DICT_DIR / "vi"


def spell(report: Path) -> list[str]:
    """Chạy hunspell (từ điển tiếng Việt) trên văn bản; trả về "file:dòng: từ" nghi sai."""
    base = _ensure_dictionary()
    allow = {w.strip().lower() for w in (REPORT / "tools" / "spell-allow.txt").read_text(encoding="utf-8").splitlines()
             if w.strip() and not w.startswith("#")}
    out = []
    for ch in _chapters(report):
        for lineno, line in enumerate(_read(ch).splitlines(), 1):
            clean = re.sub(r"`[^`]*`|\(https?://[^)]*\)|https?://\S+|\{[^}]*\}|!\[[^\]]*\]\([^)]*\)", " ", line)
            # Bỏ token ASCII là tên code/viết tắt (có chữ hoa giữa từ, số, _ . /); không đụng chữ có dấu.
            tokens = [t.strip(".,;:!?()\"'") for t in clean.split()]
            clean = " ".join(t for t in tokens if t and not (t.isascii() and re.search(r"[A-Z0-9_./]", t[1:])))
            words = [w for w in re.findall(r"[^\W\d_]+", clean) if w.lower() not in allow]
            if not words:
                continue
            res = subprocess.run(["hunspell", "-d", str(base), "-l"], input=" ".join(words),
                                 capture_output=True, text=True)
            out += [f"{ch.name}:{lineno}: {w}" for w in sorted(set(res.stdout.split()))]
    return out


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--spell", action="store_true", help="soát chính tả bằng hunspell (chỉ cảnh báo)")
    args = parser.parse_args()
    repo = REPORT.parents[1]
    errors = run(REPORT, repo)
    counts = word_counts(REPORT)
    for name, n in counts.items():
        print(f"{n:>7}  {name}")
    print(f"{sum(counts.values()):>7}  TỔNG")
    for e in errors:
        print("LỖI:", e)
    if args.spell:
        for w in spell(REPORT):
            print("chính tả?", w)
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
