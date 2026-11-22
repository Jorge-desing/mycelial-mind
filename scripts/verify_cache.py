"""Deterministic SHA-256 input-stamp cache for Verify-Mod.ps1."""

from __future__ import annotations

import argparse
import hashlib
import os
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
STAMP_DIR = REPO_ROOT / "build" / ".verify-stamps"


def _iter_files(patterns: list[str]) -> list[Path]:
    files: set[Path] = set()
    for pat in patterns:
        for p in REPO_ROOT.glob(pat):
            if p.is_file() and "__pycache__" not in p.parts and p.suffix != ".pyc":
                files.add(p)
    return sorted(files, key=lambda p: p.relative_to(REPO_ROOT).as_posix())


def _compute_digest(patterns: list[str]) -> str:
    h = hashlib.sha256()
    for path in _iter_files(patterns):
        rel = path.relative_to(REPO_ROOT).as_posix().encode("utf-8")
        h.update(rel)
        h.update(b"\x00")
        h.update(path.read_bytes())
        h.update(b"\x00")
    return h.hexdigest()


def _outputs_present(scope: str) -> bool:
    if scope == "models":
        manifest = REPO_ROOT / "scripts" / "model_studio" / "exports" / "uv_manifest.json"
        model_dir = REPO_ROOT / "src" / "main" / "java" / "com" / "alex" / "mycelialmind" / "client" / "model"
        return manifest.is_file() and model_dir.is_dir() and any(model_dir.glob("*.java"))
    if scope == "textures":
        tex_dir = REPO_ROOT / "src" / "main" / "resources" / "assets" / "mycelialmind" / "textures"
        sheet = REPO_ROOT / "scripts" / "texture_studio" / "exports" / "contact_sheet_studio.png"
        return tex_dir.is_dir() and sheet.is_file() and any(tex_dir.rglob("*.png"))
    if scope == "sounds":
        exports = REPO_ROOT / "scripts" / "sound_studio" / "exports"
        manifest = exports / "sound_manifest.json"
        quality_report = exports / "sound_quality_report.json"
        audition_html = exports / "sound_audition.html"
        sounds_json = REPO_ROOT / "src" / "main" / "resources" / "assets" / "mycelialmind" / "sounds.json"
        sounds_dir = REPO_ROOT / "src" / "main" / "resources" / "assets" / "mycelialmind" / "sounds"
        return (
            manifest.is_file()
            and quality_report.is_file()
            and audition_html.is_file()
            and sounds_json.is_file()
            and sounds_dir.is_dir()
            and any(sounds_dir.glob("*.ogg"))
        )
    if scope == "particles":
        manifest = REPO_ROOT / "scripts" / "particle_studio" / "exports" / "particle_manifest.json"
        particles_dir = REPO_ROOT / "src" / "main" / "resources" / "assets" / "mycelialmind" / "particles"
        particle_tex_dir = REPO_ROOT / "src" / "main" / "resources" / "assets" / "mycelialmind" / "textures" / "particle"
        return (
            manifest.is_file()
            and particles_dir.is_dir()
            and any(particles_dir.glob("*.json"))
            and particle_tex_dir.is_dir()
            and any(particle_tex_dir.glob("*.png"))
        )
    if scope == "datagen":
        gen_dir = REPO_ROOT / "src" / "generated" / "resources"
        return gen_dir.is_dir() and any(gen_dir.rglob("*"))
    return False


SCOPES: dict[str, list[str]] = {
    "models": [
        "scripts/model_studio/**/*.py",
    ],
    "textures": [
        "scripts/texture_studio/**/*.py",
        "scripts/model_studio/exports/uv_manifest.json",
    ],
    "sounds": [
        "scripts/sound_studio/**/*.py",
    ],
    "particles": [
        "scripts/particle_studio/**/*.py",
        "src/main/java/com/alex/mycelialmind/registry/ModParticles.java",
    ],
    "datagen": [
        "src/main/java/com/alex/mycelialmind/datagen/**/*.java",
        "src/main/java/com/alex/mycelialmind/registry/**/*.java",
        "gradle.properties",
        "build.gradle",
    ],
}


def main() -> int:
    parser = argparse.ArgumentParser(description="Manage SHA-256 verification stamps.")
    sub = parser.add_subparsers(dest="action", required=True)

    for action in ("check", "update"):
        sp = sub.add_parser(action)
        sp.add_argument("scope", choices=sorted(SCOPES))
    sub.add_parser("clear")

    args = parser.parse_args()
    if args.action == "clear":
        if STAMP_DIR.exists():
            for f in STAMP_DIR.glob("*.sha256"):
                f.unlink()
        return 0

    digest = _compute_digest(SCOPES[args.scope])
    stamp_file = STAMP_DIR / f"{args.scope}.sha256"

    if args.action == "check":
        if not stamp_file.is_file() or not _outputs_present(args.scope):
            return 1
        stored = stamp_file.read_text(encoding="utf-8").strip()
        return 0 if stored == digest else 1

    STAMP_DIR.mkdir(parents=True, exist_ok=True)
    stamp_file.write_text(digest + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
