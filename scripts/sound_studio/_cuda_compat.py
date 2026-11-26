"""Single-point import resolver for sound_studio.cuda_dsp across package and direct CLI execution."""

from __future__ import annotations

try:
    from . import cuda_dsp as _cuda_dsp
except ImportError:
    try:
        import cuda_dsp as _cuda_dsp
    except ImportError:
        from sound_studio import cuda_dsp as _cuda_dsp

cuda_dsp = _cuda_dsp
