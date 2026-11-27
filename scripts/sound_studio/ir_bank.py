"""Acoustic Impulse Response (IR) bank for Pedalboard convolution mastering.

Maintains four deterministic 44.1 kHz mono 16-bit PCM room/cavern Impulse
Response `.wav` files in ``scripts/sound_studio/irs/``. Any pre-existing `.wav`
files in that directory (e.g. real drop-in hardware or EchoThief/OpenAIR IRs)
are preserved unless ``force=True``.
"""

from __future__ import annotations

import hashlib
from pathlib import Path
import wave

import numpy as np
from scipy import signal

SAMPLE_RATE = 44100
IRS_DIR = Path(__file__).resolve().parent / "irs"

IR_SPACE_NAMES: tuple[str, ...] = (
    "abyssal_cavern",
    "monolith_vault",
    "fungal_chamber",
    "purifier_hall",
    "spore_canopy",
    "dimensional_rift",
)


def _deterministic_seed(name: str) -> int:
    digest = hashlib.sha256(f"mycelialmind:ir:{name}".encode("utf-8")).digest()
    return int.from_bytes(digest[:8], "little")


def _lowpass(sig: np.ndarray, cutoff_hz: float, order: int = 2) -> np.ndarray:
    wn = float(np.clip(cutoff_hz / (0.5 * SAMPLE_RATE), 0.001, 0.96))
    sos = signal.butter(order, wn, btype="lowpass", output="sos")
    return signal.sosfilt(sos, sig)


def _highpass(sig: np.ndarray, cutoff_hz: float, order: int = 2) -> np.ndarray:
    wn = float(np.clip(cutoff_hz / (0.5 * SAMPLE_RATE), 0.001, 0.96))
    sos = signal.butter(order, wn, btype="highpass", output="sos")
    return signal.sosfilt(sos, sig)


def _synthesize_ir_profile(space_name: str) -> np.ndarray:
    rng = np.random.default_rng(_deterministic_seed(space_name))
    profiles = {
        "abyssal_cavern": {
            "duration": 1.35,
            "early_ms": (17.0, 29.0, 39.0, 59.0, 71.0, 119.0, 178.0, 223.0),
            "modes_hz": (68.0, 142.0, 285.0),
            "mode_taus": (0.48, 0.36, 0.24),
            "lp_hz": 1850.0,
            "hp_hz": 32.0,
            "tail_tau": 0.42,
            "velvet_density": 1750.0,
            "diffusion_mix": 0.85,
        },
        "monolith_vault": {
            "duration": 1.45,
            "early_ms": (21.0, 37.0, 53.0, 79.0, 97.0, 163.0, 236.0, 277.0),
            "modes_hz": (220.0, 554.0, 1108.0, 2480.0),
            "mode_taus": (0.52, 0.44, 0.34, 0.22),
            "lp_hz": 5400.0,
            "hp_hz": 40.0,
            "tail_tau": 0.46,
            "velvet_density": 2375.0,
            "diffusion_mix": 0.80,
        },
        "fungal_chamber": {
            "duration": 0.72,
            "early_ms": (9.0, 17.0, 23.0, 37.0, 47.0, 82.0, 107.0),
            "modes_hz": (118.0, 295.0, 610.0),
            "mode_taus": (0.24, 0.17, 0.12),
            "lp_hz": 2600.0,
            "hp_hz": 45.0,
            "tail_tau": 0.21,
            "velvet_density": 2750.0,
            "diffusion_mix": 0.76,
        },
        "purifier_hall": {
            "duration": 0.58,
            "early_ms": (5.0, 11.0, 13.0, 23.0, 29.0, 51.0, 79.0),
            "modes_hz": (380.0, 920.0, 1860.0),
            "mode_taus": (0.20, 0.16, 0.11),
            "lp_hz": 7600.0,
            "hp_hz": 75.0,
            "tail_tau": 0.18,
            "velvet_density": 3250.0,
            "diffusion_mix": 0.72,
        },
        "spore_canopy": {
            "duration": 1.10,
            "early_ms": (14.0, 23.0, 33.0, 47.0, 64.0, 108.0, 162.0),
            "modes_hz": (145.0, 340.0, 780.0),
            "mode_taus": (0.36, 0.28, 0.20),
            "lp_hz": 4200.0,
            "hp_hz": 55.0,
            "tail_tau": 0.34,
            "velvet_density": 2125.0,
            "diffusion_mix": 0.80,
        },
        "dimensional_rift": {
            "duration": 1.60,
            "early_ms": (27.0, 43.0, 67.0, 97.0, 124.0, 198.0, 284.0, 331.0),
            "modes_hz": (185.0, 523.0, 1175.0, 2960.0),
            "mode_taus": (0.62, 0.50, 0.38, 0.26),
            "lp_hz": 6800.0,
            "hp_hz": 35.0,
            "tail_tau": 0.54,
            "velvet_density": 2625.0,
            "diffusion_mix": 0.84,
        },
    }
    cfg = profiles[space_name]
    n = int(round(float(cfg["duration"]) * SAMPLE_RATE))
    t = np.arange(n, dtype=np.float64) / SAMPLE_RATE

    # Crisp initial early reference + dense organic specular reflections
    ir = np.zeros(n, dtype=np.float64)
    ir[0] = 0.22
    for idx, ms in enumerate(cfg["early_ms"]):
        pos = min(n - 1, max(1, int(round(ms * SAMPLE_RATE / 1000.0))))
        sign = 1.0 if idx % 2 == 0 else -1.0
        amp = 0.52 * (0.80**idx) * rng.uniform(0.88, 1.12)
        ir[pos] += sign * amp
    if n > 4:
        # Organic wall-surface scatter smoothing across specular early taps without smearing dry direct tap
        ir[1:] = np.convolve(ir[1:], np.array([0.2, 0.6, 0.2], dtype=np.float64), mode="same")

    # Velvet noise + Gaussian dense late tail
    velvet_count = max(64, int(cfg["velvet_density"] * cfg["duration"]))
    velvet = np.zeros(n, dtype=np.float64)
    start_sample = int(SAMPLE_RATE * cfg["early_ms"][0] / 1000.0)
    if start_sample < n - 16:
        positions = rng.integers(start_sample, n, size=velvet_count)
        signs = rng.choice([-1.0, 1.0], size=velvet_count)
        np.add.at(velvet, positions, signs * 0.45)

    dense_noise = rng.normal(0.0, 0.35, n)
    dense_noise[:start_sample] *= np.linspace(0.0, 1.0, max(1, start_sample)) ** 2
    diffuse = (velvet + dense_noise) * np.exp(-t / float(cfg["tail_tau"]))

    # Room/chamber modal resonances
    modal = np.zeros(n, dtype=np.float64)
    for f_hz, tau in zip(cfg["modes_hz"], cfg["mode_taus"]):
        phase = rng.uniform(0.0, 2.0 * np.pi)
        modal += 0.14 * np.sin(2.0 * np.pi * f_hz * t + phase) * np.exp(-t / tau)
    modal[:start_sample] *= np.linspace(0.0, 1.0, max(1, start_sample))

    tail = _lowpass(diffuse * float(cfg["diffusion_mix"]) + modal, float(cfg["lp_hz"]), order=2)
    combined = ir + _highpass(tail, float(cfg["hp_hz"]), order=2)

    # Smooth tail fade-out at end to avoid truncation clicks
    fade_out = min(n // 4, int(SAMPLE_RATE * 0.08))
    if fade_out > 4:
        combined[-fade_out:] *= np.linspace(1.0, 0.0, fade_out) ** 2

    # Unit-energy normalize for predictable convolution mix loudness, while avoiding peak clip
    l2 = float(np.sqrt(np.sum(combined * combined)))
    if l2 > 1e-8:
        combined /= l2
    peak = float(np.max(np.abs(combined)))
    if peak > 0.92:
        combined *= 0.92 / peak
    return combined


def _write_mono_wav16(path: Path, samples: np.ndarray, sample_rate: int = SAMPLE_RATE) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    pcm = np.round(np.clip(samples, -1.0, 1.0) * 32767.0).astype("<i2")
    with wave.open(str(path), "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sample_rate)
        wf.writeframes(pcm.tobytes())


def ensure_ir_bank(irs_dir: Path | None = None, *, force: bool = False) -> dict[str, Path]:
    """Ensure the 6 room Impulse Response .wav files exist, preserving drop-in .wav overrides."""
    target_dir = irs_dir or IRS_DIR
    target_dir.mkdir(parents=True, exist_ok=True)
    resolved: dict[str, Path] = {}
    for space_name in IR_SPACE_NAMES:
        wav_path = target_dir / f"{space_name}.wav"
        if force or not wav_path.is_file() or wav_path.stat().st_size < 128:
            samples = _synthesize_ir_profile(space_name)
            _write_mono_wav16(wav_path, samples, SAMPLE_RATE)
        resolved[space_name] = wav_path
    return resolved


def select_ir_space(sound_id: str, species: str, loudness_group: str, category: str = "") -> str:
    """Select the most fitting acoustic space IR across 6 environments for a sound event."""
    sid = sound_id.lower()
    sp = species.lower()

    if any(
        k in sid
        for k in (
            "apex_matriarch",
            "matriarch_altar",
            "rift",
            "portal",
            "symbiotic_node_teleport",
            "boss_phase_shift",
            "overmind_crown_surge",
            "schism_injector_sting",
            "volatile_engram_pulse",
        )
    ):
        return "dimensional_rift"

    if any(
        k in sid
        for k in (
            "biome_forest",
            "block_mycelium",
            "spore_vent_puff",
            "spore_bulb_burst",
            "sporeling",
            "truce_pheromone_spray",
            "tide_beacon_activate",
            "reclaimed_bloom",
            "spore_glider",
            "root_sap_harvest",
            "root_cap_bounce",
            "resonant_elixir_drink",
            "amber_vein_resonate",
        )
    ):
        return "spore_canopy"

    if any(
        k in sid
        for k in (
            "hive_core",
            "hive_altar",
            "biome_sanctum",
            "synaptic_node",
            "synapse_weaver",
            "synaptic_mimic",
            "spore_architect",
            "quorum_seal_pact",
            "synaptic_graft_splice",
            "sovereign_nexus",
            "colony_dialect",
            "colony_reconstruction",
            "primordial_presence",
            "boss_telegraph_lock",
            "sanctum_harmonic_chime",
            "deep_mycelium_whisper",
        )
    ) or sp == "synaptic_eldritch" or "monolith" in sid:
        return "monolith_vault"

    if any(
        k in sid
        for k in (
            "spore_behemoth",
            "fungal_tide",
            "biome_caverns",
            "goliath_carrier",
            "burrower",
            "block_mycelial_stone",
            "block_hive_altar",
            "colossal",
            "boss_weakpoint",
            "boss_stagger_break",
            "boss_combat_tension",
            "hive_tension_bed",
            "hive_swarm_combat",
            "colony_raid_horn",
            "brute_wall_stun",
            "root_keeper",
            "sinkhole_updraft",
            "spore_updraft_vent",
            "cavern_spore_drip",
            "subterranean_hyphal_creak",
        )
    ) or sp == "colossal_organic":
        return "abyssal_cavern"

    if any(sp_k in sp for sp_k in ("purifier", "purification")) or any(
        k in sid for k in ("turret", "pylon", "bench", "drone", "scanner", "neutraliz", "grenade", "launcher", "antidote", "chitin_armor")
    ):
        return "purifier_hall"
    if category in ("item", "projectile") and sp not in ("wet_visceral", "pyro_fungal"):
        return "purifier_hall"

    return "fungal_chamber"
