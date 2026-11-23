"""Xuất sơ đồ PlantUML (*.puml) ra PNG, chỉ xuất lại khi nguồn đổi."""
from __future__ import annotations

import hashlib
import json
import subprocess
import urllib.request
from pathlib import Path

# Bản phát hành mới nhất đã ra ≥ 2 tuần lúc lập (2026-11-23): 1.2026.9 (17/10/2026).
PLANTUML_VERSION = "1.2026.9"
JAR_URL = ("https://repo1.maven.org/maven2/net/sourceforge/plantuml/plantuml/"
           f"{PLANTUML_VERSION}/plantuml-{PLANTUML_VERSION}.jar")
JAR = Path.home() / ".cache" / "plantuml" / f"plantuml-{PLANTUML_VERSION}.jar"
DPI = 200


def ensure_jar() -> Path:
    """Tải jar PlantUML về cache nếu chưa có, kiểm SHA-1 theo Maven Central."""
    if JAR.exists():
        return JAR
    JAR.parent.mkdir(parents=True, exist_ok=True)
    data = urllib.request.urlopen(JAR_URL, timeout=60).read()
    expected = urllib.request.urlopen(JAR_URL + ".sha1", timeout=60).read().decode().split()[0]
    if hashlib.sha1(data).hexdigest() != expected:
        raise RuntimeError("PlantUML jar sai SHA-1, không dùng")
    JAR.write_bytes(data)
    return JAR


def _digest(src: Path, shared: bytes) -> str:
    return hashlib.sha256(shared + src.read_bytes()).hexdigest()


def render_all(src: Path, out: Path) -> list[Path]:
    """Xuất mọi `src/*.puml` (trừ file bắt đầu bằng `_`) thành `out/<tên>.png`.

    File `_*.iuml` dùng chung (style) được tính vào mã băm, đổi style thì xuất lại hết.
    Returns:
        Danh sách PNG tương ứng, theo thứ tự tên file.
    Raises:
        RuntimeError: PlantUML báo lỗi, thông báo có tên file nguồn.
    """
    out.mkdir(parents=True, exist_ok=True)
    hashes_file = out / ".hashes.json"
    hashes = json.loads(hashes_file.read_text()) if hashes_file.exists() else {}
    shared = b"".join(p.read_bytes() for p in sorted(src.glob("_*.iuml")))
    pngs = []
    for puml in sorted(p for p in src.glob("*.puml") if not p.name.startswith("_")):
        png = out / f"{puml.stem}.png"
        digest = _digest(puml, shared)
        if not (png.exists() and hashes.get(puml.name) == digest):
            res = subprocess.run(
                ["java", "-Djava.awt.headless=true", "-jar", str(ensure_jar()), "-tpng",
                 f"-Sdpi={DPI}", "-charset", "UTF-8", "-o", str(out.resolve()), str(puml)],
                capture_output=True, text=True,
            )
            if res.returncode != 0 or not png.exists():
                raise RuntimeError(f"{puml.name}: PlantUML lỗi: {(res.stderr or res.stdout).strip()}")
            hashes[puml.name] = digest
        pngs.append(png)
    hashes_file.write_text(json.dumps(hashes, indent=1, sort_keys=True))
    return pngs
