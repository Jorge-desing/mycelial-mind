"""Automated visual regression checker using SSIM and 64-bit DCT pHash."""

from __future__ import annotations

import argparse
import math
import os
import shutil
import sys
import warnings
import numpy as np
from PIL import Image

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DEFAULT_BASELINE = os.path.join(REPO_ROOT, "scripts", "model_studio", "exports", "baseline_visual_smoke.png")
DEFAULT_CURRENT = os.path.join(REPO_ROOT, "run", "screenshots", "visual_smoke_mobs.png")


def _load_gray(path: str, size: tuple[int, int] | None = None) -> np.ndarray:
    with Image.open(path) as src:
        im = src.convert("L")
        if size is not None and im.size != size:
            im = im.resize(size, Image.Resampling.BILINEAR)
        return np.asarray(im, dtype=np.float64)


def _box_blur(img: np.ndarray, k: int = 8) -> np.ndarray:
    pad = k // 2
    padded = np.pad(img, ((pad, pad), (pad, pad)), mode="edge")
    cs = np.cumsum(np.cumsum(padded, axis=0), axis=1)
    # Pad top/left with 0 for exact prefix-sum window subtraction
    cs = np.pad(cs, ((1, 0), (1, 0)), mode="constant", constant_values=0.0)
    h, w = img.shape
    return (cs[k : k + h, k : k + w] - cs[:h, k : k + w] - cs[k : k + h, :w] + cs[:h, :w]) / float(k * k)


def compute_ssim(img_a: np.ndarray, img_b: np.ndarray) -> float:
    """Compute mean Structural Similarity Index (SSIM) in [-1.0, 1.0] using scikit-image when available."""
    if img_a.shape != img_b.shape:
        raise ValueError(f"Shape mismatch: {img_a.shape} vs {img_b.shape}")
    try:
        from skimage.metrics import structural_similarity
        return float(structural_similarity(img_a, img_b, data_range=255.0))
    except ImportError:
        c1 = (0.01 * 255.0) ** 2
        c2 = (0.03 * 255.0) ** 2

        mu_a = _box_blur(img_a, 8)
        mu_b = _box_blur(img_b, 8)
        mu_a2 = mu_a * mu_a
        mu_b2 = mu_b * mu_b
        mu_ab = mu_a * mu_b

        sigma_a2 = np.maximum(0.0, _box_blur(img_a * img_a, 8) - mu_a2)
        sigma_b2 = np.maximum(0.0, _box_blur(img_b * img_b, 8) - mu_b2)
        sigma_ab = _box_blur(img_a * img_b, 8) - mu_ab

        num = (2.0 * mu_ab + c1) * (2.0 * sigma_ab + c2)
        den = (mu_a2 + mu_b2 + c1) * (sigma_a2 + sigma_b2 + c2)
        return float(np.mean(num / np.maximum(den, 1e-12)))


_LPIPS_MODEL = None


def compute_lpips(baseline_path: str, current_path: str) -> float | None:
    """Compute AlexNet LPIPS perceptual distance (lower = more similar), or None if lpips/torch unavailable."""
    global _LPIPS_MODEL
    if os.path.abspath(baseline_path) == os.path.abspath(current_path):
        return 0.0

    with Image.open(baseline_path) as b_im, Image.open(current_path) as c_im:
        b_rgb = b_im.convert("RGB")
        c_rgb = c_im.convert("RGB")
        if c_rgb.size != b_rgb.size:
            c_rgb = c_rgb.resize(b_rgb.size, Image.Resampling.BILINEAR)
        # Resize very large screenshots down to max 512px for fast CPU evaluation
        if max(b_rgb.size) > 512:
            scale = 512.0 / max(b_rgb.size)
            new_sz = (max(64, int(b_rgb.width * scale)), max(64, int(b_rgb.height * scale)))
            b_rgb = b_rgb.resize(new_sz, Image.Resampling.BILINEAR)
            c_rgb = c_rgb.resize(new_sz, Image.Resampling.BILINEAR)
        b_arr = np.asarray(b_rgb, dtype=np.uint8)
        c_arr = np.asarray(c_rgb, dtype=np.uint8)

    if np.array_equal(b_arr, c_arr):
        return 0.0

    try:
        import torch
        import lpips
    except ImportError:
        return None
    if _LPIPS_MODEL is None:
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            _LPIPS_MODEL = lpips.LPIPS(net="alex", verbose=False)
        _LPIPS_MODEL.eval()

    b_t = torch.from_numpy(b_arr.astype(np.float32)).permute(2, 0, 1).unsqueeze(0) / 127.5 - 1.0
    c_t = torch.from_numpy(c_arr.astype(np.float32)).permute(2, 0, 1).unsqueeze(0) / 127.5 - 1.0
    with torch.no_grad():
        return float(_LPIPS_MODEL(b_t, c_t).item())


def _dct_matrix(n: int) -> np.ndarray:
    mat = np.zeros((n, n), dtype=np.float64)
    for k in range(n):
        alpha = math.sqrt(1.0 / n) if k == 0 else math.sqrt(2.0 / n)
        for i in range(n):
            mat[k, i] = alpha * math.cos(math.pi * (2 * i + 1) * k / (2.0 * n))
    return mat


_DCT_32 = _dct_matrix(32)


def _compute_phash_from_gray(gray_l: Image.Image) -> int:
    resized = gray_l if gray_l.size == (32, 32) else gray_l.resize((32, 32), Image.Resampling.BILINEAR)
    gray = np.asarray(resized, dtype=np.float64)
    dct = _DCT_32 @ gray @ _DCT_32.T
    low = dct[:8, :8].flatten()
    med = float(np.median(low[1:]))
    bits = (low > med).astype(np.uint64)
    h = 0
    for idx, b in enumerate(bits):
        if int(b):
            h |= 1 << idx
    return h


def compute_phash(path: str) -> int:
    """Compute 64-bit DCT perceptual hash for an image."""
    with Image.open(path) as src:
        return _compute_phash_from_gray(src.convert("L"))


def hamming_distance(h1: int, h2: int) -> int:
    return bin(h1 ^ h2).count("1")


def compare_images(baseline_path: str, current_path: str) -> tuple[float, int]:
    with Image.open(baseline_path) as base_im:
        base_size = base_im.size
    base_gray = _load_gray(baseline_path)
    curr_gray = _load_gray(current_path, size=base_size)
    ssim = compute_ssim(base_gray, curr_gray)
    dist = hamming_distance(compute_phash(baseline_path), compute_phash(current_path))
    return ssim, dist


def compute_diff_regions(
    img_a: np.ndarray,
    img_b: np.ndarray,
    threshold: float = 18.0,
    grid_size: int = 16,
) -> list[tuple[int, int, int, int, float]]:
    """Detect localized grid cells where mean or max absolute luminance delta exceeds threshold."""
    if img_a.shape != img_b.shape:
        raise ValueError(f"Shape mismatch: {img_a.shape} vs {img_b.shape}")
    diff = np.abs(img_a.astype(np.float64) - img_b.astype(np.float64))
    h, w = diff.shape
    regions: list[tuple[int, int, int, int, float]] = []
    for y0 in range(0, h, grid_size):
        y1 = min(h, y0 + grid_size)
        for x0 in range(0, w, grid_size):
            x1 = min(w, x0 + grid_size)
            patch = diff[y0:y1, x0:x1]
            max_delta = float(patch.max())
            if max_delta >= threshold and float(patch.mean()) >= threshold * 0.25:
                regions.append((x0, y0, x1, y1, max_delta))
    return regions


def _save_diff_heatmap_from_gray(
    base_gray: np.ndarray,
    curr_gray: np.ndarray,
    out_path: str,
    threshold: float = 18.0,
) -> list[tuple[int, int, int, int, float]]:
    regions = compute_diff_regions(base_gray, curr_gray, threshold=threshold)

    diff = np.abs(base_gray - curr_gray)
    heat = np.zeros((base_gray.shape[0], base_gray.shape[1], 3), dtype=np.uint8)
    heat[..., 0] = np.clip(diff * 2.5, 0, 255).astype(np.uint8)
    heat[..., 1] = np.clip(curr_gray * 0.45, 0, 255).astype(np.uint8)
    heat[..., 2] = np.clip(base_gray * 0.45, 0, 255).astype(np.uint8)

    try:
        import cv2
        u_diff = np.clip(diff, 0, 255).astype(np.uint8)
        _, mask = cv2.threshold(u_diff, int(threshold), 255, cv2.THRESH_BINARY)
        contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        for cnt in contours:
            x, y, cw, ch = cv2.boundingRect(cnt)
            if cw * ch >= 16:
                cv2.rectangle(heat, (x, y), (x + cw, y + ch), (255, 255, 0), 1)
    except ImportError:
        pass

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    Image.fromarray(heat, mode="RGB").save(out_path)
    return regions


def save_diff_heatmap(
    baseline_path: str,
    current_path: str,
    out_path: str,
    threshold: float = 18.0,
) -> list[tuple[int, int, int, int, float]]:
    """Generate a diagnostic RGB diff heatmap highlighting regressed pixel regions."""
    with Image.open(baseline_path) as base_im:
        base_size = base_im.size
    base_gray = _load_gray(baseline_path)
    curr_gray = _load_gray(current_path, size=base_size)
    return _save_diff_heatmap_from_gray(base_gray, curr_gray, out_path, threshold=threshold)


def main() -> int:
    parser = argparse.ArgumentParser(description="Automated SSIM + pHash + LPIPS visual regression checker")
    parser.add_argument("--baseline", default=DEFAULT_BASELINE)
    parser.add_argument("--current", default=DEFAULT_CURRENT)
    parser.add_argument("--update-baseline", action="store_true")
    parser.add_argument("--min-ssim", type=float, default=0.90)
    parser.add_argument("--max-hamming", type=int, default=6)
    parser.add_argument("--max-lpips", type=float, default=0.35)
    parser.add_argument(
        "--heatmap",
        default=os.path.join(REPO_ROOT, "artifacts", "diagnostics", "visual_diff_heatmap.png"),
    )
    args = parser.parse_args()

    if not os.path.isfile(args.current):
        print(f"ERROR: Current screenshot not found: {args.current}")
        return 1

    if args.update_baseline:
        os.makedirs(os.path.dirname(args.baseline), exist_ok=True)
        shutil.copy2(args.current, args.baseline)
        print(f"Updated baseline screenshot: {args.baseline}")
        return 0

    if not os.path.isfile(args.baseline):
        print(f"ERROR: Baseline not found: {args.baseline}. Run with --update-baseline first.")
        return 1

    with Image.open(args.baseline) as b_src, Image.open(args.current) as c_src:
        b_l = b_src.convert("L")
        c_l_orig = c_src.convert("L")
        c_l_matched = c_l_orig if c_l_orig.size == b_l.size else c_l_orig.resize(b_l.size, Image.Resampling.BILINEAR)
        base_gray = np.asarray(b_l, dtype=np.float64)
        curr_gray = np.asarray(c_l_matched, dtype=np.float64)
        ssim = compute_ssim(base_gray, curr_gray)
        dist = hamming_distance(_compute_phash_from_gray(b_l), _compute_phash_from_gray(c_l_orig))
    regions = _save_diff_heatmap_from_gray(base_gray, curr_gray, args.heatmap)
    lpips_val = compute_lpips(args.baseline, args.current)
    lpips_str = f", LPIPS={lpips_val:.4f} (max {args.max_lpips:.2f})" if lpips_val is not None else ""
    print(f"Visual Regression -> SSIM={ssim:.4f} (min {args.min_ssim:.2f}), pHash Hamming={dist} (max {args.max_hamming}), diff_regions={len(regions)}{lpips_str}")
    if ssim < args.min_ssim or dist > args.max_hamming or (lpips_val is not None and lpips_val > args.max_lpips):
        print(f"FAILED: Visual regression threshold exceeded! Heatmap: {args.heatmap}")
        return 2
    print("PASSED: Visual regression within thresholds.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
