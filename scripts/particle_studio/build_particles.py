"""Build and validate Mycelial Mind's procedural particle sprites and descriptors.

Usage:
    python scripts/particle_studio/build_particles.py --all --export-extras --check
    python scripts/particle_studio/build_particles.py --check
"""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import os
import re
import sys
from typing import Dict, List

import numpy as np
from PIL import Image, ImageDraw

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from scripts.particle_studio.catalog import FAMILY_ORDER, SPECS, ParticleSpec
from scripts.particle_studio.generators import render_spec
from scripts.texture_studio.sdf_engine import save_rgba

ASSETS = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "mycelialmind")
TEX_DIR = os.path.join(ASSETS, "textures", "particle")
JSON_DIR = os.path.join(ASSETS, "particles")
EXPORTS_DIR = os.path.join(os.path.dirname(__file__), "exports")
REGISTRY_JAVA = os.path.join(
    REPO_ROOT, "src", "main", "java", "com", "alex", "mycelialmind", "registry", "ModParticles.java"
)
MANIFEST_PATH = os.path.join(EXPORTS_DIR, "particle_manifest.json")
CONTACT_SHEET_PATH = os.path.join(EXPORTS_DIR, "particle_contact_sheet.png")
GALLERY_PATH = os.path.join(EXPORTS_DIR, "particle_gallery.html")


def frame_names(spec: ParticleSpec) -> List[str]:
    if spec.frames <= 1:
        return [spec.id]
    return [f"{spec.id}_{i}" for i in range(spec.frames)]


def json_payload(spec: ParticleSpec) -> dict:
    return {"textures": [f"mycelialmind:{name}" for name in frame_names(spec)]}


def registered_ids() -> List[str]:
    with open(REGISTRY_JAVA, encoding="utf-8") as fh:
        text = fh.read()
    ids = re.findall(r'register[A-Za-z]*\(\s*"([^"]+)"\s*\)', text)
    return list(dict.fromkeys(ids))


def frame_digest(rgba: np.ndarray) -> str:
    return hashlib.sha256(np.ascontiguousarray(rgba).tobytes()).hexdigest()


def _luminance(rgba: np.ndarray) -> np.ndarray:
    rgb = rgba[..., :3].astype(np.float32)
    return 0.2126 * rgb[..., 0] + 0.7152 * rgb[..., 1] + 0.0722 * rgb[..., 2]


def validate_sprite(spec: ParticleSpec, frames: List[np.ndarray]) -> List[str]:
    errors: List[str] = []
    if len(frames) != spec.frames:
        errors.append(f"{spec.id}: expected {spec.frames} frames, generated {len(frames)}")
    for idx, rgba in enumerate(frames):
        if rgba.shape != (spec.size, spec.size, 4):
            errors.append(f"{spec.id}[{idx}]: bad shape {rgba.shape}")
            continue
        if rgba[0, 0, 3] or rgba[0, -1, 3] or rgba[-1, 0, 3] or rgba[-1, -1, 3]:
            errors.append(f"{spec.id}[{idx}]: opaque corner (reads as a square blob)")
        coverage = float(np.count_nonzero(rgba[..., 3] > 16)) / float(spec.size * spec.size)
        if coverage < 0.02:
            errors.append(f"{spec.id}[{idx}]: alpha coverage {coverage:.3f} too low")
        elif coverage > 0.92:
            errors.append(f"{spec.id}[{idx}]: alpha coverage {coverage:.3f} too high")
        opaque = rgba[..., 3] > 64
        if int(np.count_nonzero(opaque)) >= 12:
            lum = _luminance(rgba)[opaque]
            bins = np.unique(np.clip((lum / 32.0).astype(np.int32), 0, 7))
            if len(bins) < 3:
                errors.append(f"{spec.id}[{idx}]: grayscale legibility has only {len(bins)} luminance bands")
            if float(lum.max() - lum.min()) < 45.0:
                errors.append(f"{spec.id}[{idx}]: grayscale contrast {float(lum.max() - lum.min()):.1f} < 45")
    if spec.frames > 1:
        for i in range(1, len(frames)):
            diff = float(np.mean(np.abs(frames[i].astype(np.int16) - frames[i - 1].astype(np.int16))))
            if diff < 0.35:
                errors.append(f"{spec.id}: frames {i - 1}/{i} are visually identical (diff {diff:.3f})")
    return errors


def _mask_digest(rgba: np.ndarray, target: int = 16) -> str:
    mask = (rgba[..., 3] > 24).astype(np.uint8) * 255
    small = np.asarray(Image.fromarray(mask).resize((target, target), Image.BILINEAR)) > 64
    return hashlib.sha256(np.ascontiguousarray(small).tobytes()).hexdigest()


def validate_catalog(rendered: Dict[str, List[np.ndarray]]) -> List[str]:
    errors: List[str] = []
    ids = [s.id for s in SPECS]
    if len(ids) != len(set(ids)):
        errors.append("catalog contains duplicate particle ids")
    registry = registered_ids()
    if set(registry) != set(ids):
        missing = sorted(set(registry) - set(ids))
        extra = sorted(set(ids) - set(registry))
        errors.append(f"registry/catalog mismatch: missing={missing} extra={extra}")
    seen: Dict[str, str] = {}
    for spec in SPECS:
        digest = _mask_digest(rendered[spec.id][0])
        if digest in seen:
            errors.append(f"silhouette duplicate: {spec.id} == {seen[digest]}")
        seen[digest] = spec.id
    return errors


def validate_on_disk(rendered: Dict[str, List[np.ndarray]]) -> List[str]:
    errors: List[str] = []
    os.makedirs(TEX_DIR, exist_ok=True)
    os.makedirs(JSON_DIR, exist_ok=True)
    for spec in SPECS:
        json_path = os.path.join(JSON_DIR, f"{spec.id}.json")
        if not os.path.isfile(json_path):
            errors.append(f"missing descriptor {json_path}")
            continue
        try:
            with open(json_path, encoding="utf-8") as fh:
                payload = json.loads(fh.read())
        except (OSError, json.JSONDecodeError) as exc:
            errors.append(f"{json_path}: {exc}")
            continue
        expected = [f"mycelialmind:{name}" for name in frame_names(spec)]
        if payload.get("textures") != expected:
            errors.append(f"{json_path}: textures {payload.get('textures')} != {expected}")
        for idx, rgba in enumerate(rendered[spec.id]):
            png = os.path.join(TEX_DIR, frame_names(spec)[idx] + ".png")
            if not os.path.isfile(png):
                errors.append(f"missing sprite {png}")
                continue
            with Image.open(png) as image:
                disk = np.asarray(image.convert("RGBA"))
            if disk.shape != rgba.shape:
                errors.append(f"{png}: size {disk.shape[1]}x{disk.shape[0]} != {spec.size}x{spec.size}")
            elif frame_digest(disk) != frame_digest(rgba):
                errors.append(f"{png}: pixels are stale (rerun --all)")
    known_textures = {name for spec in SPECS for name in frame_names(spec)}
    known_json = {spec.id for spec in SPECS}
    for name in sorted(os.listdir(TEX_DIR)):
        if name.endswith(".png") and name[:-4] not in known_textures:
            errors.append(f"orphan sprite textures/particle/{name}")
        if name.endswith(".mcmeta"):
            errors.append(f"particles must animate through sprite lists, found {name}")
    for name in sorted(os.listdir(JSON_DIR)):
        if name.endswith(".json") and name[:-5] not in known_json:
            errors.append(f"orphan descriptor particles/{name}")
    return errors


def build_manifest(rendered: Dict[str, List[np.ndarray]]) -> dict:
    specs = {}
    for spec in SPECS:
        specs[spec.id] = {
            "family": spec.family,
            "size": spec.size,
            "frames": spec.frames,
            "shape": spec.shape,
            "palette": spec.palette,
            "accent": spec.accent,
            "render": spec.render,
            "archetype": spec.archetype,
            "textures": [f"mycelialmind:{name}" for name in frame_names(spec)],
            "frame_sha256": [frame_digest(rgba) for rgba in rendered[spec.id]],
            "description_es": spec.description_es,
            "description_en": spec.description_en,
        }
    return {
        "ok": True,
        "particle_count": len(SPECS),
        "sprite_count": sum(s.frames for s in SPECS),
        "families": list(FAMILY_ORDER),
        "specs": specs,
    }


def build_contact_sheet(rendered: Dict[str, List[np.ndarray]]) -> None:
    os.makedirs(EXPORTS_DIR, exist_ok=True)
    scale, pad, label_h, max_w = 4, 12, 16, 1680
    groups = {fam: [s for s in SPECS if s.family == fam] for fam in FAMILY_ORDER}
    # Flow-layout rows so 64px rings do not overlap their 32px neighbours.
    layout = []
    for fam in FAMILY_ORDER:
        group = groups[fam]
        if not group:
            continue
        rows = []
        row: List[ParticleSpec] = []
        row_w = 0
        for spec in group:
            block_w = spec.frames * spec.size * scale + pad
            if row and row_w + block_w > max_w:
                rows.append(row)
                row, row_w = [], 0
            row.append(spec)
            row_w += block_w
        if row:
            rows.append(row)
        layout.append((fam, rows))

    def row_height(row: List[ParticleSpec]) -> int:
        tallest = max(spec.size for spec in row)
        return tallest * scale + label_h + pad

    height = pad
    for _fam, rows in layout:
        height += 24 + sum(row_height(r) for r in rows)
    canvas = Image.new("RGBA", (max_w, height + pad), (16, 16, 22, 255))
    draw = ImageDraw.Draw(canvas)
    y = pad
    for fam, rows in layout:
        count = sum(len(r) for r in rows)
        draw.text((pad, y), f"{fam.upper()}  ({count})", fill=(190, 225, 255, 255))
        y += 24
        for row in rows:
            cell_h = max(spec.size for spec in row) * scale
            x = pad
            for spec in row:
                block_w = spec.frames * spec.size * scale
                for fi, rgba in enumerate(rendered[spec.id]):
                    bw = spec.size * scale
                    im = Image.fromarray(rgba).resize((bw, bw), Image.NEAREST)
                    chk = Image.new("RGBA", (bw, bw))
                    chk_draw = ImageDraw.Draw(chk)
                    for cy in range(0, bw, 10):
                        for cx in range(0, bw, 10):
                            shade = (44, 44, 54, 255) if ((cx // 10 + cy // 10) % 2 == 0) else (32, 32, 40, 255)
                            chk_draw.rectangle([cx, cy, cx + 9, cy + 9], fill=shade)
                    chk.alpha_composite(im)
                    canvas.paste(chk, (x + fi * bw, y + cell_h - bw))
                draw.rectangle(
                    [x - 1, y + cell_h - spec.size * scale - 1,
                     x + block_w, y + cell_h],
                    outline=(88, 92, 116, 255),
                )
                draw.text((x, y + cell_h + 1), f"{spec.id} [{spec.frames}]", fill=(210, 214, 230, 255))
                x += block_w + pad
            y += cell_h + label_h + pad
    canvas.save(CONTACT_SHEET_PATH)


def build_gallery(manifest: dict) -> None:
    os.makedirs(EXPORTS_DIR, exist_ok=True)
    rel_tex = os.path.relpath(TEX_DIR, EXPORTS_DIR).replace("\\", "/")
    cards = []
    for spec in SPECS:
        frames = "".join(
            f'<img class="frame" data-i="{i}" src="{rel_tex}/{name}.png" alt="{spec.id} frame {i}">'
            for i, name in enumerate(frame_names(spec))
        )
        cards.append(
            f'<article class="card" data-family="{spec.family}">'
            f'<div class="view" data-frames="{spec.frames}">{frames}</div>'
            f'<h3>{spec.id}</h3>'
            f'<p>{html.escape(spec.description_es)}</p>'
            f'<p class="meta">{spec.size}px · {spec.frames}f · {spec.render} · {spec.archetype}</p>'
            f"</article>"
        )
    filters = "".join(f'<button data-f="{fam}">{fam}</button>' for fam in FAMILY_ORDER)
    page = f"""<!doctype html><html lang="es"><head><meta charset="utf-8">
<title>Mycelial Mind · Particle Gallery</title>
<style>
body{{background:#12121a;color:#d9def0;font:14px system-ui,Segoe UI,sans-serif;margin:24px}}
h1{{font-size:20px}}
.cards{{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:14px;margin-top:16px}}
.card{{background:#1b1b26;border:1px solid #2e2e40;border-radius:10px;padding:10px}}
.view{{position:relative;height:128px;display:flex;align-items:center;justify-content:center;background:
repeating-conic-gradient(#20202c 0% 25%,#181822 0% 50%) 0 0/18px 18px}}
.view img{{width:112px;height:112px;image-rendering:pixelated;position:absolute;opacity:0}}
.view img.on{{opacity:1}}
h3{{margin:8px 0 4px;font-size:14px;color:#a8e0ff}}
p{{margin:2px 0;color:#9aa2b8;font-size:12px}}
.meta{{color:#7f879c}}
button{{background:#23233a;color:#cfd6ff;border:1px solid #3a3a58;border-radius:6px;padding:4px 10px;margin-right:6px;cursor:pointer}}
</style></head><body>
<h1>Mycelial Mind · Particle Gallery ({manifest['particle_count']} tipos / {manifest['sprite_count']} sprites)</h1>
<p>Frames animados en bucle (180&nbsp;ms). Fondo oscuro para revisar legibilidad y siluetas.</p>
<div>{filters}</div>
<div class="cards">{''.join(cards)}</div>
<script>
const timers = new Map();
document.querySelectorAll('.view').forEach(v => {{
  const imgs = [...v.querySelectorAll('img')];
  let i = 0;
  setInterval(() => {{
    imgs.forEach((im, k) => im.classList.toggle('on', k === i));
    i = (i + 1) % imgs.length;
  }}, 180);
  imgs[0].classList.add('on');
}});
document.querySelectorAll('button[data-f]').forEach(b => b.addEventListener('click', () => {{
  document.querySelectorAll('.card').forEach(c => {{
    c.style.display = (b.dataset.f === 'all' || c.dataset.family === b.dataset.f) ? '' : 'none';
  }});
}}));
</script></body></html>"""
    with open(GALLERY_PATH, "w", encoding="utf-8") as fh:
        fh.write(page)


def main() -> int:
    parser = argparse.ArgumentParser(description="Mycelial Mind procedural Particle Studio")
    parser.add_argument("--all", action="store_true", help="Regenerate sprites, descriptors and manifest")
    parser.add_argument("--export-extras", action="store_true", help="Export the contact sheet and HTML gallery")
    parser.add_argument("--check", action="store_true", help="Validate assets and registry parity")
    args = parser.parse_args()

    rendered = {spec.id: render_spec(spec) for spec in SPECS}
    errors: List[str] = []
    for spec in SPECS:
        errors.extend(validate_sprite(spec, rendered[spec.id]))
    errors.extend(validate_catalog(rendered))

    if args.all:
        os.makedirs(TEX_DIR, exist_ok=True)
        os.makedirs(JSON_DIR, exist_ok=True)
        for spec in SPECS:
            for idx, rgba in enumerate(rendered[spec.id]):
                save_rgba(rgba, os.path.join(TEX_DIR, frame_names(spec)[idx] + ".png"))
            with open(os.path.join(JSON_DIR, f"{spec.id}.json"), "w", encoding="utf-8") as fh:
                json.dump(json_payload(spec), fh)
        known = {name for spec in SPECS for name in frame_names(spec)}
        existing = {name[:-4] for name in os.listdir(TEX_DIR) if name.endswith(".png")}
        for name in sorted(existing - known):
            os.remove(os.path.join(TEX_DIR, name + ".png"))
        manifest = build_manifest(rendered)
        os.makedirs(EXPORTS_DIR, exist_ok=True)
        with open(MANIFEST_PATH, "w", encoding="utf-8") as fh:
            json.dump(manifest, fh, indent=2)
        print(f"Particle Studio: wrote {len(SPECS)} descriptors and {sum(s.frames for s in SPECS)} sprites.")
        if args.export_extras:
            build_contact_sheet(rendered)
            build_gallery(manifest)
            print("Exported particle contact sheet and HTML gallery:", EXPORTS_DIR)

    if args.check:
        errors.extend(validate_on_disk(rendered))

    if errors:
        print(f"Particle Studio checks FAILED ({len(errors)} issues):")
        for err in errors:
            print("  -", err)
        return 1

    if not args.check and not args.all:
        print("Nothing to do; pass --all and/or --check.")
        return 0

    if args.check:
        print(
            f"Particle Studio checks PASSED: {len(SPECS)} types, "
            f"{sum(s.frames for s in SPECS)} sprites, {len(registered_ids())} registry ids, "
            "unique silhouettes, grayscale legibility, deterministic pixels."
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
