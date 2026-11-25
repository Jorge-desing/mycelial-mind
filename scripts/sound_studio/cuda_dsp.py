"""RTX CUDA acceleration kernels, per-thread streams, and VRAM caches for SoundStudio.

Important: ``scripts/mcp/.venv`` does not bundle PyTorch. All ``torch`` imports
are lazy inside ``get_cuda_torch()`` so CPU-only environments fall back cleanly
to NumPy/SciPy without importing or initializing ``torch``.
"""

from __future__ import annotations

import math
import threading
import numpy as np

_CUDA_INIT_LOCK = threading.Lock()
_CUDA_CHECKED = False
_TORCH_MOD = None
_CUDA_READY = False
_DEVICE_NAME = ""

_THREAD_LOCAL = threading.local()
_CACHE_LOCK = threading.Lock()
_FREQS_CACHE: dict[tuple[int, int], object] = {}
_STEM_GPU_CACHE: dict[str, object] = {}
_IR_GPU_CACHE: dict[str, object] = {}
_IR_FFT_CACHE: dict[tuple[str, int], object] = {}


def get_cuda_torch():
    """Return the ``torch`` module iff PyTorch + CUDA are available; else ``None``."""
    global _CUDA_CHECKED, _TORCH_MOD, _CUDA_READY, _DEVICE_NAME
    if _CUDA_CHECKED:
        return _TORCH_MOD if _CUDA_READY else None
    with _CUDA_INIT_LOCK:
        if not _CUDA_CHECKED:
            try:
                import torch  # noqa: PLC0415

                if torch.cuda.is_available():
                    _TORCH_MOD = torch
                    _CUDA_READY = True
                    _DEVICE_NAME = str(torch.cuda.get_device_name(0))
            except Exception:
                _TORCH_MOD = None
                _CUDA_READY = False
                _DEVICE_NAME = ""
            finally:
                _CUDA_CHECKED = True
    return _TORCH_MOD if _CUDA_READY else None


def is_cuda_available() -> bool:
    """Return True iff CUDA PyTorch acceleration is ready."""
    return get_cuda_torch() is not None


def get_device_name() -> str:
    """Return CUDA device name or empty string when running on CPU."""
    get_cuda_torch()
    return _DEVICE_NAME


def get_thread_stream(torch=None):
    """Return a dedicated ``torch.cuda.Stream`` per worker thread to overlap GPU kernels."""
    t_mod = torch if torch is not None else get_cuda_torch()
    if t_mod is None:
        return None
    stream = getattr(_THREAD_LOCAL, "cuda_stream", None)
    if stream is None:
        stream = t_mod.cuda.Stream()
        _THREAD_LOCAL.cuda_stream = stream
    return stream


def warmup_cuda() -> str:
    """Initialize CUDA context once on the main thread and return active device name."""
    torch = get_cuda_torch()
    if torch is None:
        return ""
    with torch.inference_mode():
        _ = torch.zeros(1, device="cuda")
        torch.cuda.synchronize()
    return _DEVICE_NAME


def clear_gpu_caches() -> None:
    """Release all cached VRAM stem, IR, and frequency tensors."""
    with _CACHE_LOCK:
        _FREQS_CACHE.clear()
        _STEM_GPU_CACHE.clear()
        _IR_GPU_CACHE.clear()
        _IR_FFT_CACHE.clear()
    torch = get_cuda_torch() if _CUDA_CHECKED else None
    if torch is not None:
        try:
            torch.cuda.empty_cache()
        except Exception:
            pass


def _next_pow2(n: int) -> int:
    return 1 if n <= 1 else (1 << (n - 1).bit_length())


def _get_freqs_tensor(torch, n_fft: int, sample_rate: int):
    key = (int(n_fft), int(sample_rate))
    cached = _FREQS_CACHE.get(key)
    if cached is not None:
        return cached
    with _CACHE_LOCK:
        cached = _FREQS_CACHE.get(key)
        if cached is None:
            cached = torch.fft.rfftfreq(int(n_fft), d=1.0 / float(sample_rate), device="cuda")
            torch.cuda.current_stream().synchronize()
            _FREQS_CACHE[key] = cached
        return cached


def preload_ai_stems_gpu(stems: dict[str, np.ndarray]) -> int:
    """Upload cached mono AI stems into VRAM once to avoid per-sound H2D copies."""
    torch = get_cuda_torch()
    if torch is None:
        return 0
    uploaded = 0
    with torch.inference_mode():
        with _CACHE_LOCK:
            for stem_id, samples in stems.items():
                arr = np.ascontiguousarray(samples, dtype=np.float32)
                cached = _STEM_GPU_CACHE.get(stem_id)
                if cached is not None and cached.numel() == arr.size:
                    continue
                _STEM_GPU_CACHE[stem_id] = torch.as_tensor(arr, device="cuda")
                uploaded += 1
        torch.cuda.synchronize()
    return uploaded


def _get_stem_gpu(torch, stem_id: str, stem_np: np.ndarray):
    cached = _STEM_GPU_CACHE.get(stem_id)
    if cached is not None and cached.numel() == len(stem_np):
        return cached
    with _CACHE_LOCK:
        cached = _STEM_GPU_CACHE.get(stem_id)
        if cached is None or cached.numel() != len(stem_np):
            arr = np.ascontiguousarray(stem_np, dtype=np.float32)
            cached = torch.as_tensor(arr, device="cuda")
            torch.cuda.current_stream().synchronize()
            _STEM_GPU_CACHE[stem_id] = cached
        return cached


def preload_ir_bank_gpu(ir_waves: dict[str, np.ndarray]) -> int:
    """Upload the room Impulse Response bank to VRAM once per build."""
    torch = get_cuda_torch()
    if torch is None:
        return 0
    uploaded = 0
    with torch.inference_mode():
        with _CACHE_LOCK:
            for space_name, samples in ir_waves.items():
                arr = np.ascontiguousarray(samples, dtype=np.float32)
                cached = _IR_GPU_CACHE.get(space_name)
                if cached is not None and cached.numel() == arr.size:
                    continue
                _IR_GPU_CACHE[space_name] = torch.as_tensor(arr, device="cuda")
                # Drop any stale precomputed spectra for replaced IRs
                stale = [k for k in _IR_FFT_CACHE if k[0] == space_name]
                for k in stale:
                    del _IR_FFT_CACHE[k]
                uploaded += 1
        torch.cuda.synchronize()
    return uploaded


def _get_ir_rfft_tensor(torch, ir_space: str, n_fft: int, ir_wave_fallback: np.ndarray | None = None):
    key = (ir_space, int(n_fft))
    cached = _IR_FFT_CACHE.get(key)
    if cached is not None:
        return cached
    with _CACHE_LOCK:
        cached = _IR_FFT_CACHE.get(key)
        if cached is not None:
            return cached
        with torch.inference_mode():
            ir_t = _IR_GPU_CACHE.get(ir_space)
            if ir_t is None:
                if ir_wave_fallback is None:
                    return None
                ir_t = torch.as_tensor(np.ascontiguousarray(ir_wave_fallback, dtype=np.float32), device="cuda")
                _IR_GPU_CACHE[ir_space] = ir_t
            cached = torch.fft.rfft(ir_t, n=int(n_fft))
            torch.cuda.current_stream().synchronize()
            _IR_FFT_CACHE[key] = cached
        return cached


def gpu_fftconvolve_1d(sig: np.ndarray, ir: np.ndarray, *, min_conv_len: int = 4096) -> np.ndarray | None:
    """Linear 1D convolution via CUDA ``torch.fft.rfft``/``irfft`` truncated to ``len(sig)``."""
    n_sig = len(sig)
    n_ir = len(ir)
    n_conv = n_sig + n_ir - 1
    if n_sig == 0 or n_ir == 0 or n_conv < min_conv_len:
        return None
    torch = get_cuda_torch()
    if torch is None:
        return None

    n_fft = _next_pow2(n_conv)
    stream = get_thread_stream(torch)
    with torch.inference_mode(), torch.cuda.stream(stream):
        s_t = torch.as_tensor(np.ascontiguousarray(sig, dtype=np.float32), device="cuda")
        k_t = torch.as_tensor(np.ascontiguousarray(ir, dtype=np.float32), device="cuda")
        wet_t = torch.fft.irfft(torch.fft.rfft(s_t, n=n_fft) * torch.fft.rfft(k_t, n=n_fft), n=n_fft)[:n_sig]
        return np.asarray(wet_t.cpu().numpy(), dtype=np.float64)


def gpu_ir_convolve(
    sig: np.ndarray,
    ir_space: str,
    ir_wave_fallback: np.ndarray | None = None,
    *,
    mix: float = 0.11,
    min_cuda_len: int = 2048,
) -> np.ndarray | None:
    """Apply room IR convolution on the worker's CUDA stream using VRAM-cached IR spectra."""
    n_sig = len(sig)
    if n_sig < min_cuda_len:
        return None
    torch = get_cuda_torch()
    if torch is None:
        return None

    ir_t = _IR_GPU_CACHE.get(ir_space)
    if ir_t is None:
        if ir_wave_fallback is None:
            return None
        ir_len = len(ir_wave_fallback)
    else:
        ir_len = int(ir_t.numel())
    if ir_len == 0:
        return None

    n_fft = _next_pow2(n_sig + ir_len - 1)
    ir_spec = _get_ir_rfft_tensor(torch, ir_space, n_fft, ir_wave_fallback)
    if ir_spec is None:
        return None

    mix_f = float(np.clip(mix, 0.0, 1.0))
    stream = get_thread_stream(torch)
    with torch.inference_mode(), torch.cuda.stream(stream):
        s_t = torch.as_tensor(np.ascontiguousarray(sig, dtype=np.float32), device="cuda")
        wet_t = torch.fft.irfft(torch.fft.rfft(s_t, n=n_fft) * ir_spec, n=n_fft)[:n_sig]
        out_t = (1.0 - mix_f) * s_t + mix_f * wet_t
        return np.asarray(out_t.cpu().numpy(), dtype=np.float64)


def _bandpass_mask_torch(torch, freqs, lo_hz: float, hi_hz: float, sample_rate: int):
    """Smooth zero-phase raised-cosine bandpass mask on GPU."""
    nyq = 0.5 * float(sample_rate)
    lo = float(np.clip(lo_hz, 18.0, 0.90 * nyq))
    hi = float(np.clip(hi_hz, lo + 30.0, 0.96 * nyq))
    bw = hi - lo
    lo_w = min(lo * 0.35, bw * 0.25)
    hi_w = min(hi * 0.22, bw * 0.30, max(12.0, 0.985 * nyq - hi))

    lo0 = max(4.0, lo - lo_w)
    lo1 = lo + 0.35 * lo_w
    hi0 = max(lo1 + 4.0, hi - 0.35 * hi_w)
    hi1 = min(0.985 * nyq, hi + hi_w)

    pass_lo = 0.5 * (1.0 - torch.cos(torch.pi * torch.clamp((freqs - lo0) / max(1e-3, lo1 - lo0), 0.0, 1.0)))
    pass_hi = 0.5 * (1.0 + torch.cos(torch.pi * torch.clamp((freqs - hi0) / max(1e-3, hi1 - hi0), 0.0, 1.0)))
    mask = pass_lo * pass_hi
    mask[0] = 0.0
    return mask


def _gpu_envelope_batch(torch, sigs, windows: list[int]):
    """Compute normalized ``[0, 1]`` moving-average envelopes via batched CUDA FFT."""
    b, n = sigs.shape
    max_win = max(windows)
    n_fft = _next_pow2(n + max_win - 1)
    kernels = torch.zeros((b, max_win), dtype=torch.float32, device="cuda")
    for idx, win in enumerate(windows):
        kernels[idx, :win] = 1.0 / float(win)

    conv_full = torch.fft.irfft(
        torch.fft.rfft(sigs.abs(), n=n_fft, dim=-1) * torch.fft.rfft(kernels, n=n_fft, dim=-1),
        n=n_fft,
        dim=-1,
    )
    sliced = torch.stack(
        [conv_full[idx, (win - 1) // 2 : (win - 1) // 2 + n] for idx, win in enumerate(windows)],
        dim=0,
    )
    peaks = sliced.amax(dim=-1, keepdim=True)
    return torch.where(
        peaks >= 1e-8,
        torch.clamp(sliced / peaks.clamp_min(1e-8), 0.0, 1.0),
        torch.zeros_like(sliced),
    )


def _gpu_hpss_harmonic_soft(torch, sig_t, sample_rate: int, strength: float = 0.60):
    """Fast batched CUDA STFT Wiener Harmonic/Percussive separation preserving harmonic body & texture."""
    n = int(sig_t.numel())
    if strength <= 1e-4 or n < 1024:
        return sig_t
    n_fft = 1024
    hop = 256
    win = torch.hann_window(n_fft, device="cuda", dtype=torch.float32)
    stft = torch.stft(sig_t, n_fft=n_fft, hop_length=hop, win_length=n_fft, window=win, return_complex=True)
    mag = stft.abs().unsqueeze(0).unsqueeze(0)
    h_est = torch.nn.functional.avg_pool2d(mag, kernel_size=(1, 9), stride=1, padding=(0, 4))
    p_est = torch.nn.functional.avg_pool2d(mag, kernel_size=(9, 1), stride=1, padding=(4, 0))
    h2 = h_est * h_est
    p2 = p_est * p_est
    mask_h = (h2 / (h2 + p2 + 1e-9)).squeeze(0).squeeze(0)
    blended = stft * ((1.0 - float(strength)) + float(strength) * mask_h)
    return torch.istft(blended, n_fft=n_fft, hop_length=hop, win_length=n_fft, window=win, length=n)


def _gpu_align_spectral_tilt(torch, stem_bp_t, dsp_bp_t, freqs, *, strength: float = 0.45):
    """Align AI stem spectral slope in CUDA FFT space toward DSP centroid so blends stay free of mud/harshness."""
    n = int(stem_bp_t.numel())
    if strength <= 1e-4 or n < 512:
        return stem_bp_t
    n_fft = _next_pow2(n)
    f_t = _get_freqs_tensor(torch, n_fft, int(freqs) if isinstance(freqs, int) else 44100)
    pair = torch.stack([stem_bp_t, dsp_bp_t], dim=0)
    spec = torch.fft.rfft(pair, n=n_fft, dim=-1)
    pwr = spec.abs() ** 2 + 1e-12
    c_stem = (f_t * pwr[0]).sum() / pwr[0].sum()
    c_dsp = (f_t * pwr[1]).sum() / pwr[1].sum()
    ratio = torch.clamp(c_dsp / torch.clamp(c_stem, min=60.0), 0.55, 1.85)
    tilt_exp = torch.clamp(torch.log2(ratio) * float(strength), -0.55, 0.55)
    pivot = torch.clamp(c_dsp, 220.0, 4200.0)
    tilt = torch.clamp((torch.clamp(f_t, min=40.0) / pivot) ** tilt_exp, 0.55, 1.75)
    return torch.fft.irfft(spec[0] * tilt, n=n_fft)[:n]


def _apply_bandpass_and_envelope_gpu(
    torch,
    resampled_t,
    dsp_t,
    *,
    sample_rate: int,
    band_lo_hz: float,
    band_hi_hz: float,
    apply_hpss: bool = False,
):
    """Shared GPU 3-band / 2-band / 1-band zero-phase FFT filtering + HPSS + spectral tilt alignment + envelope follower."""
    n = int(dsp_t.numel())
    src_t = _gpu_hpss_harmonic_soft(torch, resampled_t, sample_rate, strength=0.60) if apply_hpss else resampled_t
    if n >= 1024 and not apply_hpss:
        # Seamless beds still benefit from gentle spectral tilt alignment toward DSP bed centroid
        src_t = _gpu_align_spectral_tilt(torch, src_t, dsp_t, sample_rate, strength=0.35)
    elif n >= 1024:
        src_t = _gpu_align_spectral_tilt(torch, src_t, dsp_t, sample_rate, strength=0.45)
    s1 = min(240.0, max(band_lo_hz * 1.45, band_hi_hz * 0.12))
    s2 = min(1800.0, max(s1 * 2.2, band_hi_hz * 0.42))
    tri = (s1 > band_lo_hz + 25.0) and (s2 > s1 + 140.0) and (band_hi_hz > s2 + 220.0)
    split_hz = min(420.0, max(band_lo_hz * 1.4, band_hi_hz * 0.38))
    dual = (not tri) and (split_hz > band_lo_hz + 25.0) and (band_hi_hz > split_hz + 80.0)

    pad = min(4096, max(1024, n // 2))
    n_fft_bp = _next_pow2(n + pad)

    pair_t = torch.stack([src_t, dsp_t], dim=0)
    spec_pair = torch.fft.rfft(pair_t, n=n_fft_bp, dim=-1)
    freqs = _get_freqs_tensor(torch, n_fft_bp, sample_rate)

    if tri:
        b2_lo = max(band_lo_hz, s1 * 0.85)
        b3_lo = max(b2_lo + 40.0, s2 * 0.85)
        m_sub = _bandpass_mask_torch(torch, freqs, band_lo_hz, s1, sample_rate)
        m_body = _bandpass_mask_torch(torch, freqs, b2_lo, s2, sample_rate)
        m_air = _bandpass_mask_torch(torch, freqs, b3_lo, band_hi_hz, sample_rate)
        masks = torch.stack([m_sub, m_body, m_air], dim=0).unsqueeze(1)
        bands = torch.fft.irfft(spec_pair.unsqueeze(0) * masks, n=n_fft_bp, dim=-1)[..., :n]
        win_sub = max(8, int(sample_rate * 28.0 / 1000.0))
        win_body = max(8, int(sample_rate * 18.0 / 1000.0))
        win_air = max(8, int(sample_rate * 11.0 / 1000.0))
        envs = _gpu_envelope_batch(
            torch,
            torch.stack([bands[0, 1], bands[1, 1], bands[2, 1]], dim=0),
            [win_sub, win_body, win_air],
        )
        return (bands[0, 0] * envs[0]) + (bands[1, 0] * envs[1]) + (bands[2, 0] * envs[2])

    if dual:
        hi_start = max(band_lo_hz, split_hz * 0.85)
        m_lo = _bandpass_mask_torch(torch, freqs, band_lo_hz, split_hz, sample_rate)
        m_hi = _bandpass_mask_torch(torch, freqs, hi_start, band_hi_hz, sample_rate)
        masks = torch.stack([m_lo, m_hi], dim=0).unsqueeze(1)
        bands = torch.fft.irfft(spec_pair.unsqueeze(0) * masks, n=n_fft_bp, dim=-1)[..., :n]
        stem_lo, dsp_lo = bands[0, 0], bands[0, 1]
        stem_hi, dsp_hi = bands[1, 0], bands[1, 1]
        win_lo = max(8, int(sample_rate * 22.0 / 1000.0))
        win_hi = max(8, int(sample_rate * 14.0 / 1000.0))
        envs = _gpu_envelope_batch(torch, torch.stack([dsp_lo, dsp_hi], dim=0), [win_lo, win_hi])
        return (stem_lo * envs[0]) + (stem_hi * envs[1])

    m_bp = _bandpass_mask_torch(torch, freqs, band_lo_hz, band_hi_hz, sample_rate)
    stem_bp = torch.fft.irfft(spec_pair[0] * m_bp, n=n_fft_bp)[:n]
    win = max(8, int(sample_rate * 18.0 / 1000.0))
    env = _gpu_envelope_batch(torch, pair_t[1:2], [win])[0]
    return stem_bp * env


def gpu_dualband_blend_shape(
    resampled: np.ndarray,
    dsp_wave: np.ndarray,
    *,
    sample_rate: int,
    band_lo_hz: float,
    band_hi_hz: float,
    apply_hpss: bool = False,
    min_cuda_len: int = 2048,
) -> np.ndarray | None:
    """Compute shaped AI stem waveform via batched CUDA 3-band FFT bandpass, HPSS & envelope following."""
    n = len(dsp_wave)
    if n < min_cuda_len or len(resampled) != n:
        return None
    torch = get_cuda_torch()
    if torch is None:
        return None

    stream = get_thread_stream(torch)
    with torch.inference_mode(), torch.cuda.stream(stream):
        resampled_t = torch.as_tensor(np.ascontiguousarray(resampled, dtype=np.float32), device="cuda")
        dsp_t = torch.as_tensor(np.ascontiguousarray(dsp_wave, dtype=np.float32), device="cuda")
        shaped_t = _apply_bandpass_and_envelope_gpu(
            torch,
            resampled_t,
            dsp_t,
            sample_rate=sample_rate,
            band_lo_hz=band_lo_hz,
            band_hi_hz=band_hi_hz,
            apply_hpss=apply_hpss,
        )
        return np.asarray(shaped_t.cpu().numpy(), dtype=np.float64)


def gpu_blend_ai_stem(
    dsp_wave: np.ndarray,
    stem_id: str,
    stem_np: np.ndarray,
    *,
    sample_rate: int,
    band_lo_hz: float,
    band_hi_hz: float,
    pitch_ratio: float,
    rot_frac: float,
    attack_guard_ms: float,
    seamless_loop: bool,
    mix_weight: float,
    min_cuda_len: int = 2048,
) -> np.ndarray | None:
    """Full end-to-end GPU AI stem rotation, resampling, loop crossfade, bandpass, envelope & mix."""
    n = len(dsp_wave)
    if n < min_cuda_len or len(stem_np) < 128:
        return None
    torch = get_cuda_torch()
    if torch is None:
        return None

    stem_t = _get_stem_gpu(torch, stem_id, stem_np)
    stream = get_thread_stream(torch)
    with torch.inference_mode(), torch.cuda.stream(stream):
        m_stem = int(stem_t.numel())
        start_idx = int(rot_frac * max(1, m_stem - 64))
        rolled = torch.roll(stem_t, -start_idx, dims=0)

        loop_cf = 0
        if seamless_loop and n >= int(1.2 * sample_rate):
            cf_cand = min(n // 4, int(sample_rate * 0.260))
            if cf_cand % 2 == 1:
                cf_cand -= 1
            if cf_cand >= 32:
                loop_cf = cf_cand
        n_resample = n + loop_cf

        src_needed = max(64, int(round(n_resample * pitch_ratio)))
        if src_needed > m_stem:
            reps = int(math.ceil(src_needed / float(m_stem)))
            tiled = rolled.repeat(reps)[:src_needed]
        else:
            tiled = rolled[:src_needed]

        # Exact piecewise-linear interpolation matching np.interp on [0..src_needed-1] -> [0..n_resample-1]
        m_src = int(tiled.numel())
        if m_src == n_resample:
            resampled_t = tiled
        elif m_src <= 1:
            resampled_t = tiled[:1].expand(n_resample)
        else:
            pos = torch.arange(n_resample, device="cuda", dtype=torch.float32) * (float(m_src) / float(n_resample))
            pos = torch.clamp(pos, 0.0, float(m_src - 1))
            idx0 = pos.to(torch.long)
            idx1 = torch.clamp(idx0 + 1, max=m_src - 1)
            frac = pos - idx0.to(torch.float32)
            v0 = tiled[idx0]
            resampled_t = v0 + frac * (tiled[idx1] - v0)

        if loop_cf >= 32:
            half = loop_cf // 2
            theta = torch.linspace(0.0, 0.5 * math.pi, loop_cf, device="cuda", dtype=torch.float32)
            bridge = resampled_t[n : n + loop_cf] * torch.cos(theta) + resampled_t[:loop_cf] * torch.sin(theta)
            # Fold circular crossfade seamlessly around the wrap seam while preserving exact length n
            resampled_t = torch.cat([bridge[half:], resampled_t[loop_cf:n], bridge[:half]], dim=0)

        dsp_t = torch.as_tensor(np.ascontiguousarray(dsp_wave, dtype=np.float32), device="cuda")
        if seamless_loop and n >= 4096:
            wrap = min(n // 4, 4096)
            res_in = torch.cat([resampled_t[-wrap:], resampled_t, resampled_t[:wrap]], dim=0)
            dsp_in = torch.cat([dsp_t[-wrap:], dsp_t, dsp_t[:wrap]], dim=0)
            shaped_t = _apply_bandpass_and_envelope_gpu(
                torch,
                res_in,
                dsp_in,
                sample_rate=sample_rate,
                band_lo_hz=band_lo_hz,
                band_hi_hz=band_hi_hz,
                apply_hpss=False,
            )[wrap : wrap + n]
        else:
            shaped_t = _apply_bandpass_and_envelope_gpu(
                torch,
                resampled_t,
                dsp_t,
                sample_rate=sample_rate,
                band_lo_hz=band_lo_hz,
                band_hi_hz=band_hi_hz,
                apply_hpss=not seamless_loop,
            )

        if not seamless_loop:
            guard_samples = min(n // 2, int(sample_rate * attack_guard_ms / 1000.0))
            if guard_samples > 8:
                guard_ramp = torch.linspace(0.0, 1.0, guard_samples, device="cuda", dtype=torch.float32) ** 1.6
                shaped_t[:guard_samples] *= guard_ramp

        dsp_rms = float(torch.sqrt(torch.mean(dsp_t * dsp_t)).item()) + 1e-8
        stem_rms = float(torch.sqrt(torch.mean(shaped_t * shaped_t)).item())
        if stem_rms < 1e-7:
            return dsp_wave

        matched_stem = shaped_t * (dsp_rms / stem_rms)
        w_stem = float(mix_weight)
        w_dsp = 1.0 - (w_stem * 0.55)
        out_t = (dsp_t * w_dsp) + (matched_stem * w_stem)
        return np.asarray(out_t.cpu().numpy(), dtype=np.float64)


def gpu_harmonic_sum(
    phase_cycles: np.ndarray,
    freq_multipliers: np.ndarray | tuple[float, ...] | list[float],
    amplitudes: np.ndarray | tuple[float, ...] | list[float],
    phase_offsets: np.ndarray | tuple[float, ...] | list[float],
    *,
    min_work: int = 250_000,
    chunk_samples: int = 65536,
) -> np.ndarray | None:
    """Chunked GPU additive sine synthesis: ``sum_i amplitudes[i] * sin(2*pi*m[i]*phase_cycles + off[i])``."""
    n = len(phase_cycles)
    k = len(freq_multipliers)
    if n == 0 or k == 0 or (n * k) < min_work:
        return None
    torch = get_cuda_torch()
    if torch is None:
        return None

    mult_np = np.ascontiguousarray(freq_multipliers, dtype=np.float64).reshape(-1, 1)
    amp_np = np.ascontiguousarray(amplitudes, dtype=np.float32).reshape(-1, 1)
    off_np = np.ascontiguousarray(phase_offsets, dtype=np.float32).reshape(-1, 1)
    cyc_np = np.ascontiguousarray(phase_cycles, dtype=np.float64)

    stream = get_thread_stream(torch)
    with torch.inference_mode(), torch.cuda.stream(stream):
        pb64 = torch.as_tensor(cyc_np, device="cuda", dtype=torch.float64)
        fmult = torch.as_tensor(mult_np, device="cuda", dtype=torch.float64)
        w_all = torch.as_tensor(amp_np, device="cuda", dtype=torch.float32)
        ph_all = torch.as_tensor(off_np, device="cuda", dtype=torch.float32)

        out_t = torch.empty(n, device="cuda", dtype=torch.float32)
        two_pi = float(2.0 * math.pi)
        for start in range(0, n, max(4096, chunk_samples)):
            end = min(n, start + max(4096, chunk_samples))
            wrapped = torch.frac(fmult * pb64[start:end].unsqueeze(0)).to(torch.float32) * two_pi + ph_all
            out_t[start:end] = (torch.sin(wrapped) * w_all).sum(dim=0)
        return np.asarray(out_t.cpu().numpy(), dtype=np.float64)


def gpu_spectral_noise(
    white: np.ndarray,
    exponent: float,
    min_hz: float,
    sample_rate: int,
    *,
    min_cuda_len: int = 16384,
) -> np.ndarray | None:
    """Shape CPU-RNG white noise with a ``1 / max(freq, min_hz)^exponent`` curve on GPU."""
    n = len(white)
    if n < min_cuda_len:
        return None
    torch = get_cuda_torch()
    if torch is None:
        return None

    stream = get_thread_stream(torch)
    with torch.inference_mode(), torch.cuda.stream(stream):
        w_t = torch.as_tensor(np.ascontiguousarray(white, dtype=np.float32), device="cuda")
        freqs = _get_freqs_tensor(torch, n, sample_rate)
        scale = torch.clamp(freqs, min=float(min_hz)).pow(-float(exponent))
        scale[0] = 0.0
        shaped = torch.fft.irfft(torch.fft.rfft(w_t) * scale, n=n)
        std = torch.clamp(shaped.std(unbiased=False), min=1e-6)
        return np.asarray((shaped / std).cpu().numpy(), dtype=np.float64)


def _pad_waves_to_cuda(torch, waves: list[np.ndarray]):
    lengths = [len(w) for w in waves]
    max_len = max(lengths, default=0)
    k = len(waves)
    if k == 0 or max_len == 0:
        return None, lengths, 0
    if all(l == max_len for l in lengths):
        stacked = np.stack([np.ascontiguousarray(w, dtype=np.float32) for w in waves], axis=0)
    else:
        stacked = np.zeros((k, max_len), dtype=np.float32)
        for i, w in enumerate(waves):
            if len(w) > 0:
                stacked[i, : len(w)] = np.asarray(w, dtype=np.float32)
    return torch.as_tensor(stacked, device="cuda"), lengths, max_len


def _pairwise_xcorr_from_cuda(torch, w_t, lengths: list[int], max_lag_samples: int) -> float:
    k = len(lengths)
    if k < 2:
        return 0.0
    max_len = int(w_t.shape[1])
    if max_len == 0:
        return 0.0

    # Zero-mean each waveform over its true valid sample range, keeping zero-padding exact
    w0 = w_t.clone()
    norms = torch.empty(k, device="cuda", dtype=torch.float64)
    for i, l_i in enumerate(lengths):
        if l_i <= 0:
            w0[i].zero_()
            norms[i] = 0.0
        else:
            seg = w0[i, :l_i]
            seg -= seg.mean()
            if l_i < max_len:
                w0[i, l_i:].zero_()
            norms[i] = seg.to(torch.float64).square().sum()

    pairs_i: list[int] = []
    pairs_j: list[int] = []
    for i in range(k):
        for j in range(i + 1, k):
            pairs_i.append(i)
            pairs_j.append(j)
    idx_i = torch.tensor(pairs_i, device="cuda", dtype=torch.long)
    idx_j = torch.tensor(pairs_j, device="cuda", dtype=torch.long)

    denom = torch.sqrt(norms[idx_i] * norms[idx_j])
    n_fft_xc = 1 << int(math.ceil(math.log2(max(2, max_len * 2))))
    spec = torch.fft.rfft(w0, n=n_fft_xc, dim=-1)
    pair_xcorr = torch.fft.irfft(spec[idx_i] * torch.conj(spec[idx_j]), n=n_fft_xc, dim=-1)
    lag_win = torch.cat([pair_xcorr[:, -max_lag_samples:], pair_xcorr[:, : max_lag_samples + 1]], dim=-1)
    peaks = lag_win.abs().amax(dim=-1).to(torch.float64)
    corrs = torch.where(denom >= 1e-9, peaks / denom.clamp_min(1e-18), torch.zeros_like(peaks))
    return round(float(corrs.max().item()), 4)


def gpu_max_pairwise_correlation(
    waves: list[np.ndarray],
    *,
    max_lag_samples: int = 882,
    min_cuda_len: int = 1024,
) -> float | None:
    """Compute maximum pairwise lag-tolerant correlation across variations on GPU."""
    if len(waves) < 2:
        return 0.0
    if max((len(w) for w in waves), default=0) < min_cuda_len:
        return None
    torch = get_cuda_torch()
    if torch is None:
        return None

    stream = get_thread_stream(torch)
    with torch.inference_mode(), torch.cuda.stream(stream):
        w_t, lengths, _ = _pad_waves_to_cuda(torch, waves)
        if w_t is None:
            return 0.0
        return _pairwise_xcorr_from_cuda(torch, w_t, lengths, max_lag_samples)


def gpu_analyze_event_variations(
    waves: list[np.ndarray],
    *,
    sample_rate: int = 44100,
    max_lag_samples: int = 882,
    min_cuda_len: int = 1024,
) -> tuple[list[dict[str, float]], float] | None:
    """Compute base waveform/spectral metrics and pairwise lag correlation for an event's variations on GPU."""
    if not waves:
        return None
    n0 = len(waves[0])
    if n0 < min_cuda_len or not all(len(w) == n0 for w in waves):
        return None
    torch = get_cuda_torch()
    if torch is None:
        return None

    stream = get_thread_stream(torch)
    with torch.inference_mode(), torch.cuda.stream(stream):
        w_t, lengths, n = _pad_waves_to_cuda(torch, waves)
        if w_t is None:
            return None

        w64 = w_t.to(torch.float64)
        abs_w = w64.abs()
        peaks = abs_w.amax(dim=-1)
        full_rms = torch.sqrt(torch.mean(w64 * w64, dim=-1))
        dc = torch.mean(w64, dim=-1)
        edge_n = min(4, n)
        edge_s = abs_w[:, :edge_n].amax(dim=-1)
        edge_e = abs_w[:, -edge_n:].amax(dim=-1)
        zcr = (
            ((w64[:, :-1] * w64[:, 1:]) < 0.0).to(torch.float64).mean(dim=-1)
            if n > 1
            else torch.zeros(len(waves), device="cuda", dtype=torch.float64)
        )

        # Active RMS (matching dsp_engine.active_rms)
        gates = peaks.unsqueeze(-1) * 0.05
        act_mask = abs_w >= gates
        act_counts = act_mask.sum(dim=-1)
        act_sq = torch.where(act_mask, w64 * w64, torch.zeros_like(w64)).sum(dim=-1)
        fallback_act = full_rms
        computed_act = torch.sqrt(act_sq / torch.clamp(act_counts, min=1).to(torch.float64))
        act_rms = torch.where((peaks >= 1e-9) & (act_counts >= 8), computed_act, fallback_act)
        act_rms = torch.where(peaks >= 1e-9, act_rms, torch.zeros_like(act_rms))

        # Batched FFT spectral descriptors in float64 to match numpy.fft precision
        spec = torch.fft.rfft(w_t, n=n, dim=-1)
        power = spec.abs().to(torch.float64).square()
        total_pow = power.sum(dim=-1) + 1e-12
        freqs = _get_freqs_tensor(torch, n, sample_rate).to(torch.float64)
        centroids = (power * freqs.unsqueeze(0)).sum(dim=-1) / total_pow
        low_mask = freqs < 250.0
        high_mask = freqs > 4000.0
        low_ratio = power[:, low_mask].sum(dim=-1) / total_pow
        high_ratio = power[:, high_mask].sum(dim=-1) / total_pow

        band_mask = (freqs >= 80.0) & (freqs <= 6000.0)
        if bool(band_mask.any().item()):
            band = power[:, band_mask]
            arith = band.mean(dim=-1) + 1e-18
            geom = torch.exp(torch.log(band + 1e-18).mean(dim=-1))
            flatness = torch.clamp(geom / arith, 0.0, 1.0)
        else:
            flatness = torch.zeros(len(waves), device="cuda", dtype=torch.float64)

        max_corr = _pairwise_xcorr_from_cuda(torch, w_t, lengths, max_lag_samples)

        stats_cpu = torch.stack(
            [
                full_rms,
                act_rms,
                peaks,
                dc,
                edge_s,
                edge_e,
                centroids,
                flatness,
                zcr,
                low_ratio,
                high_ratio,
            ],
            dim=1,
        ).cpu().numpy()

    dur_round = round(n / float(sample_rate), 3)
    out_metrics: list[dict[str, float]] = []
    for row in stats_cpu:
        rms_db = float(20.0 * math.log10(max(float(row[0]), 1e-6)))
        act_db = float(20.0 * math.log10(max(float(row[1]), 1e-6)))
        peak_db = float(20.0 * math.log10(max(float(row[2]), 1e-6)))
        out_metrics.append(
            {
                "rms_dbfs": round(rms_db, 2),
                "active_rms_dbfs": round(act_db, 2),
                "peak_dbfs": round(peak_db, 2),
                "crest_db": round(peak_db - act_db, 2),
                "dc_offset": round(float(row[3]), 5),
                "edge_start_abs": round(float(row[4]), 5),
                "edge_end_abs": round(float(row[5]), 5),
                "spectral_centroid_hz": round(float(row[6]), 1),
                "spectral_flatness": round(float(row[7]), 4),
                "zero_crossing_rate": round(float(row[8]), 4),
                "low_band_ratio": round(float(row[9]), 3),
                "high_band_ratio": round(float(row[10]), 3),
                "duration_secs": dur_round,
            }
        )
    return out_metrics, max_corr
