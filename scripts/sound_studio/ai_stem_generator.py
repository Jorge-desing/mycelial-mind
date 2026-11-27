"""RTX 4060 GPU stem generator & 3-band HPSS spectral hybrid mixer backed by Stable Audio Open Small.

Generates 95 curated bio-organic archetype stems (steps=28, bfloat16 on CUDA)
covering all 226 Mycelial Mind sound events, cached as 44.1 kHz mono 16-bit PCM
`.wav` files in ``scripts/sound_studio/ai_stems/``.

Combines neural stems with the 5 specialized procedural engines via:
  - Soft DSP sub/punch anchoring with natural neural attacks (5–16 ms attack guard)
  - STFT Wiener Harmonic/Percussive separation (HPSS) preserving texture cleanly
  - Macro 3-band (sub / body / air) contour shaping (de-vocoded 0.38 floor + 0.62 envelope)
  - Active-body palindrome looping & circular wrap-padded crossfade for multi-second seamless loops
  - Deterministic per-variation time rotation & pitch resampling for low correlation + automated stem QC
"""

from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
import hashlib
import json
import math
import os
from pathlib import Path
import threading
import wave

import numpy as np
from scipy import ndimage, signal

from ._cuda_compat import cuda_dsp as _cuda

_gpu_blend_ai_stem = _cuda.gpu_blend_ai_stem
_gpu_dualband_blend_shape = _cuda.gpu_dualband_blend_shape
_preload_ai_stems_gpu = _cuda.preload_ai_stems_gpu

SAMPLE_RATE = 44100
AI_STEMS_DIR = Path(__file__).resolve().parent / "ai_stems"
STEM_MANIFEST_PATH = AI_STEMS_DIR / "stem_manifest.json"
INFERENCE_STEPS = 28
INFERENCE_BATCH_SIZE = 13


@dataclass(frozen=True)
class StemArchetype:
    stem_id: str
    group: str  # "boss" | "entity" | "surface" | "block_func" | "item" | "system_env"
    prompt: str
    seconds_total: float
    band_lo_hz: float
    band_hi_hz: float
    mix_weight: float
    sound_ids: tuple[str, ...]
    attack_guard_ms: float = 20.0
    seamless_loop: bool = False


AI_STEM_ARCHETYPES: tuple[StemArchetype, ...] = (
    # =========================================================================
    # 17 Boss & Ritual Cinematics (covers all 21 BOSS_CINEMATIC_IDS)
    # =========================================================================
    StemArchetype(
        stem_id="boss_behemoth_roar",
        group="boss",
        prompt="Colossal subterranean cavern beast bellow roar, deep guttural chest vocal folds, stone earth rumble, close dry foley",
        seconds_total=3.6,
        band_lo_hz=30.0,
        band_hi_hz=1950.0,
        mix_weight=0.58,
        attack_guard_ms=12.0,
        sound_ids=("spore_behemoth_roar",),
    ),
    StemArchetype(
        stem_id="boss_behemoth_hurt",
        group="boss",
        prompt="Giant armored cave beast pained visceral grunt, heavy bone shell fracture crunch, deep stagger thud, close dry foley",
        seconds_total=2.4,
        band_lo_hz=34.0,
        band_hi_hz=2300.0,
        mix_weight=0.54,
        attack_guard_ms=8.0,
        sound_ids=("spore_behemoth_hurt",),
    ),
    StemArchetype(
        stem_id="boss_behemoth_slam",
        group="boss",
        prompt="Colossal boulder fist ground slam detonation, shattering limestone crust shockwave, deep cave thud, close dry foley",
        seconds_total=3.2,
        band_lo_hz=28.0,
        band_hi_hz=2100.0,
        mix_weight=0.56,
        attack_guard_ms=6.0,
        sound_ids=("spore_behemoth_slam",),
    ),
    StemArchetype(
        stem_id="boss_behemoth_death",
        group="boss",
        prompt="Towering cave beast dying agonizing bellow collapsing onto bedrock, cascading rock avalanche, deep chest exhale",
        seconds_total=3.8,
        band_lo_hz=28.0,
        band_hi_hz=2050.0,
        mix_weight=0.58,
        attack_guard_ms=12.0,
        sound_ids=("spore_behemoth_death",),
    ),
    StemArchetype(
        stem_id="boss_hive_core_resonance",
        group="boss",
        prompt="Deep subterranean obsidian monolith warhorn boom, bowed low bronze cello drone, dark sculk cavern resonance",
        seconds_total=3.6,
        band_lo_hz=40.0,
        band_hi_hz=4400.0,
        mix_weight=0.54,
        attack_guard_ms=12.0,
        sound_ids=("hive_core_resonance",),
    ),
    StemArchetype(
        stem_id="boss_hive_core_hurt",
        group="boss",
        prompt="Resonant obsidian crystal core cracking recoil, descending heavy bronze groan, amethyst shard splinter snap",
        seconds_total=2.4,
        band_lo_hz=55.0,
        band_hi_hz=5600.0,
        mix_weight=0.50,
        attack_guard_ms=8.0,
        sound_ids=("hive_core_hurt",),
    ),
    StemArchetype(
        stem_id="boss_hive_core_shield",
        group="boss",
        prompt="Ascending amethyst crystal chime barrier hum, taut glassy resonance wall, deep bell harmonic ring",
        seconds_total=2.8,
        band_lo_hz=90.0,
        band_hi_hz=7400.0,
        mix_weight=0.48,
        attack_guard_ms=10.0,
        sound_ids=("hive_core_shield",),
    ),
    StemArchetype(
        stem_id="boss_hive_core_death",
        group="boss",
        prompt="Shattering obsidian monolith pillar collapse, deep bronze warhorn groan, cascading basalt crystal debris",
        seconds_total=4.0,
        band_lo_hz=36.0,
        band_hi_hz=5800.0,
        mix_weight=0.56,
        attack_guard_ms=12.0,
        sound_ids=("hive_core_death",),
    ),
    StemArchetype(
        stem_id="boss_matriarch_wail",
        group="boss",
        prompt="Haunting ghostly cavern banshee shriek, dissonant bowed cello and ethereal breath scream, deep crystal resonance",
        seconds_total=3.6,
        band_lo_hz=180.0,
        band_hi_hz=7800.0,
        mix_weight=0.56,
        attack_guard_ms=12.0,
        sound_ids=("apex_matriarch_wail",),
    ),
    StemArchetype(
        stem_id="boss_matriarch_hurt",
        group="boss",
        prompt="Sharp obsidian crystal shriek snap, discordant vocal gasp, brittle glass bone chitin crack, close dry foley",
        seconds_total=2.4,
        band_lo_hz=220.0,
        band_hi_hz=8200.0,
        mix_weight=0.52,
        attack_guard_ms=8.0,
        sound_ids=("apex_matriarch_hurt",),
    ),
    StemArchetype(
        stem_id="boss_matriarch_barrage",
        group="boss",
        prompt="Rapid volley of amethyst crystal shards and sharp whip-crack gusts tearing air, glassy snap and breath hiss",
        seconds_total=2.8,
        band_lo_hz=140.0,
        band_hi_hz=8600.0,
        mix_weight=0.52,
        attack_guard_ms=6.0,
        sound_ids=("apex_matriarch_barrage",),
    ),
    StemArchetype(
        stem_id="boss_matriarch_death",
        group="boss",
        prompt="Anguished ghostly cavern choir crescendo into abyssal stone collapse, shattering amethyst veil and deep drop",
        seconds_total=4.0,
        band_lo_hz=55.0,
        band_hi_hz=7800.0,
        mix_weight=0.58,
        attack_guard_ms=12.0,
        sound_ids=("apex_matriarch_death",),
    ),
    StemArchetype(
        stem_id="ritual_monolith_awaken",
        group="boss",
        prompt="Ancient carved stone temple slabs grinding open, deep bronze monolith bell toll, subterranean warhorn",
        seconds_total=3.8,
        band_lo_hz=34.0,
        band_hi_hz=3000.0,
        mix_weight=0.54,
        attack_guard_ms=12.0,
        sound_ids=("hive_altar_awaken", "fungal_tide_horn"),
    ),
    StemArchetype(
        stem_id="ritual_dimensional_rift",
        group="boss",
        prompt="Ancient stone portal arch cracking open, rushing subterranean wind suction cut, deep tectonic sub thud",
        seconds_total=3.6,
        band_lo_hz=55.0,
        band_hi_hz=6800.0,
        mix_weight=0.52,
        attack_guard_ms=10.0,
        sound_ids=("matriarch_altar_awaken", "boss_phase_shift"),
    ),
    StemArchetype(
        stem_id="boss_weakpoint_impact",
        group="boss",
        prompt="Heavy monster bone weak-point rupture crunch followed by hard shell armor ricochet clang, dry close foley",
        seconds_total=2.2,
        band_lo_hz=42.0,
        band_hi_hz=6800.0,
        mix_weight=0.52,
        attack_guard_ms=6.0,
        sound_ids=("boss_weakpoint_crit", "boss_carapace_ricochet"),
    ),
    StemArchetype(
        stem_id="colony_raid_signal",
        group="boss",
        prompt="Distant menacing carved bone warhorn blast across cavern, urgent hollow wooden chitin rattle, close foley",
        seconds_total=3.2,
        band_lo_hz=36.0,
        band_hi_hz=3800.0,
        mix_weight=0.54,
        attack_guard_ms=10.0,
        sound_ids=("colony_raid_horn",),
    ),
    StemArchetype(
        stem_id="boss_tactical_stagger",
        group="boss",
        prompt="Sharp amethyst crystal chime warning followed by colossal shattered bone armor stagger collapse, dry foley",
        seconds_total=2.6,
        band_lo_hz=36.0,
        band_hi_hz=6200.0,
        mix_weight=0.52,
        attack_guard_ms=8.0,
        sound_ids=("boss_telegraph_lock", "boss_stagger_break"),
    ),
    # =========================================================================
    # 33 Entity & Footstep Stems (covers all 72 ENTITY_SYNTH_IDS & footsteps)
    # =========================================================================
    StemArchetype(
        stem_id="sporeling_chitter_insect",
        group="entity",
        prompt="Tiny insectoid beetle vocal chittering, hollow bone chitin mandible clicks, small wet squeak, close dry foley",
        seconds_total=2.2,
        band_lo_hz=280.0,
        band_hi_hz=9200.0,
        mix_weight=0.54,
        attack_guard_ms=6.0,
        sound_ids=("sporeling_chitter", "sporeling_hurt", "sporeling_death"),
    ),
    StemArchetype(
        stem_id="sporeling_pod_burst",
        group="entity",
        prompt="Small pressurized wet fungal pod bursting pop, damp moss and sticky spore splatter, close dry foley",
        seconds_total=1.8,
        band_lo_hz=120.0,
        band_hi_hz=7600.0,
        mix_weight=0.56,
        attack_guard_ms=4.0,
        sound_ids=("sporeling_burst",),
    ),
    StemArchetype(
        stem_id="chitin_brute_growl_heavy",
        group="entity",
        prompt="Heavy armored cave beetle beast guttural chest growl, rough nasal snort, deep wooden rattle, close dry foley",
        seconds_total=2.8,
        band_lo_hz=36.0,
        band_hi_hz=2600.0,
        mix_weight=0.58,
        attack_guard_ms=10.0,
        sound_ids=("chitin_brute_growl", "chitin_brute_hurt", "chitin_brute_death"),
    ),
    StemArchetype(
        stem_id="chitin_brute_ground_smash",
        group="entity",
        prompt="Heavy bone-armored fist smashing cave rock floor, loud carapace crunch and earth thud, close dry foley",
        seconds_total=2.2,
        band_lo_hz=32.0,
        band_hi_hz=4200.0,
        mix_weight=0.58,
        attack_guard_ms=4.0,
        sound_ids=("chitin_brute_smash",),
    ),
    StemArchetype(
        stem_id="stalker_predator_hiss",
        group="entity",
        prompt="Sinister predatory cave creature raspy hiss, wet rasping throat click, stalking breath, close dry foley",
        seconds_total=2.4,
        band_lo_hz=180.0,
        band_hi_hz=8800.0,
        mix_weight=0.54,
        attack_guard_ms=6.0,
        sound_ids=("stalker_hiss", "stalker_hurt", "stalker_death"),
    ),
    StemArchetype(
        stem_id="stalker_scythe_pounce",
        group="entity",
        prompt="Fast bone scythe claw whipping through cave air, sharp predator leap crunch, close dry foley",
        seconds_total=1.8,
        band_lo_hz=160.0,
        band_hi_hz=9200.0,
        mix_weight=0.54,
        attack_guard_ms=4.0,
        sound_ids=("stalker_pounce",),
    ),
    StemArchetype(
        stem_id="acid_spitter_wet_gurgle",
        group="entity",
        prompt="Wet slimy organic bubbling throat gurgle, viscous mucus squelch in cheek sac, close dry creature foley",
        seconds_total=2.4,
        band_lo_hz=95.0,
        band_hi_hz=5400.0,
        mix_weight=0.56,
        attack_guard_ms=8.0,
        sound_ids=("acid_spitter_gurgle", "acid_spitter_hurt", "acid_spitter_death"),
    ),
    StemArchetype(
        stem_id="acid_spitter_corrosive_spit",
        group="entity",
        prompt="Pressurized wet saliva hawk spit globule, damp acid sizzle burst, sharp organic eject, close dry foley",
        seconds_total=1.8,
        band_lo_hz=140.0,
        band_hi_hz=8400.0,
        mix_weight=0.54,
        attack_guard_ms=4.0,
        sound_ids=("acid_spitter_spit",),
    ),
    StemArchetype(
        stem_id="burrower_chitin_grind",
        group="entity",
        prompt="Grinding bone mandibles scraping basalt stone, muffled earthen beast grunt, close dry foley",
        seconds_total=2.6,
        band_lo_hz=65.0,
        band_hi_hz=6200.0,
        mix_weight=0.56,
        attack_guard_ms=8.0,
        sound_ids=("burrower_grind", "burrower_hurt", "burrower_death"),
    ),
    StemArchetype(
        stem_id="burrower_tunnel_dig",
        group="entity",
        prompt="Rapid heavy claws tearing through gravel soil and basalt rubble, crumbling rocks, close dry foley",
        seconds_total=2.2,
        band_lo_hz=55.0,
        band_hi_hz=6800.0,
        mix_weight=0.56,
        attack_guard_ms=5.0,
        sound_ids=("burrower_tunnel",),
    ),
    StemArchetype(
        stem_id="pyro_shroom_ember_sizzle",
        group="entity",
        prompt="Smoldering hot charcoal embers crackling inside wet fungal cap, nether fire gas hiss, close dry foley",
        seconds_total=2.6,
        band_lo_hz=160.0,
        band_hi_hz=8600.0,
        mix_weight=0.54,
        attack_guard_ms=8.0,
        sound_ids=("pyro_shroom_crackle", "pyro_shroom_hurt", "pyro_shroom_death"),
    ),
    StemArchetype(
        stem_id="pyro_shroom_gas_ignite",
        group="entity",
        prompt="Whooshing pressurized nether fire puff ignition burst, crisp flame flare pop, close dry foley",
        seconds_total=1.9,
        band_lo_hz=90.0,
        band_hi_hz=8800.0,
        mix_weight=0.54,
        attack_guard_ms=4.0,
        sound_ids=("pyro_shroom_ignite",),
    ),
    StemArchetype(
        stem_id="synapse_weaver_crystal_hum",
        group="entity",
        prompt="Floating amethyst crystal hum, delicate dry insect wing whisper, warm glassy bell murmur, close dry foley",
        seconds_total=2.6,
        band_lo_hz=150.0,
        band_hi_hz=7600.0,
        mix_weight=0.50,
        attack_guard_ms=10.0,
        sound_ids=("synapse_weaver_hum", "synapse_weaver_hurt", "synapse_weaver_death"),
    ),
    StemArchetype(
        stem_id="synapse_weaver_psychic_buff",
        group="entity",
        prompt="Ascending struck amethyst crystal block ping, warm shimmering cave bell swell, close dry foley",
        seconds_total=2.1,
        band_lo_hz=220.0,
        band_hi_hz=8400.0,
        mix_weight=0.48,
        attack_guard_ms=8.0,
        sound_ids=("synapse_weaver_buff",),
    ),
    StemArchetype(
        stem_id="goliath_carrier_hollow_thud",
        group="entity",
        prompt="Massive hollow bloated belly groan, wet fluid sloshing inside deep carcass, close dry animal foley",
        seconds_total=2.8,
        band_lo_hz=32.0,
        band_hi_hz=2400.0,
        mix_weight=0.60,
        attack_guard_ms=10.0,
        sound_ids=("goliath_carrier_thud", "goliath_carrier_hurt", "goliath_carrier_death"),
    ),
    StemArchetype(
        stem_id="goliath_carrier_egg_spawn",
        group="entity",
        prompt="Wet organic brood sac birthing squelch, slippery damp membrane tearing pop, close dry foley",
        seconds_total=2.0,
        band_lo_hz=60.0,
        band_hi_hz=4800.0,
        mix_weight=0.56,
        attack_guard_ms=6.0,
        sound_ids=("goliath_carrier_spawn",),
    ),
    StemArchetype(
        stem_id="leech_parasite_shriek",
        group="entity",
        prompt="High-pitched frantic small cave parasite screech, wet clicking jaws, brittle bone snap, close dry foley",
        seconds_total=2.2,
        band_lo_hz=320.0,
        band_hi_hz=9600.0,
        mix_weight=0.50,
        attack_guard_ms=6.0,
        sound_ids=("leech_parasite_screech", "leech_parasite_hurt", "leech_parasite_death"),
    ),
    StemArchetype(
        stem_id="leech_parasite_siphon_slurp",
        group="entity",
        prompt="Wet viscous liquid siphon slurping suction pull through damp organic tube, sticky latch, close dry foley",
        seconds_total=1.8,
        band_lo_hz=120.0,
        band_hi_hz=7200.0,
        mix_weight=0.54,
        attack_guard_ms=5.0,
        sound_ids=("leech_parasite_siphon",),
    ),
    StemArchetype(
        stem_id="colony_warden_deep_bellow",
        group="entity",
        prompt="Deep subterranean cave guardian chest horn bellow, heavy armored throat rattle, sculk heartbeat, close dry foley",
        seconds_total=2.8,
        band_lo_hz=34.0,
        band_hi_hz=2800.0,
        mix_weight=0.58,
        attack_guard_ms=10.0,
        sound_ids=("colony_warden_bellow", "colony_warden_hurt", "colony_warden_death"),
    ),
    StemArchetype(
        stem_id="colony_warden_shield_shock",
        group="entity",
        prompt="Heavy armored bone shield slamming down onto stone with deep ground thud, close dry foley",
        seconds_total=2.2,
        band_lo_hz=32.0,
        band_hi_hz=4600.0,
        mix_weight=0.56,
        attack_guard_ms=4.0,
        sound_ids=("colony_warden_shockwave",),
    ),
    StemArchetype(
        stem_id="spore_architect_pulse",
        group="entity",
        prompt="Continuous steady wet sculk throat clicks, bubbling damp fungal membrane and dry bone tap, close dry foley",
        seconds_total=2.6,
        band_lo_hz=120.0,
        band_hi_hz=6800.0,
        mix_weight=0.52,
        attack_guard_ms=8.0,
        sound_ids=("spore_architect_pulse", "spore_architect_hurt", "spore_architect_death", "spore_architect_construct"),
    ),
    StemArchetype(
        stem_id="synaptic_mimic_vocal",
        group="entity",
        prompt="Uncanny shifting double-voice hollow bone croak, damp cave whisper glide, sharp wooden snap, close dry foley",
        seconds_total=2.6,
        band_lo_hz=160.0,
        band_hi_hz=7400.0,
        mix_weight=0.52,
        attack_guard_ms=10.0,
        sound_ids=("synaptic_mimic_echo", "synaptic_mimic_hurt", "synaptic_mimic_death", "synaptic_mimic_strike"),
    ),
    StemArchetype(
        stem_id="chimera_beast_growl",
        group="entity",
        prompt="Fierce agile cave hound beast growl and snap, wet lunging jaw bite snarls, heavy paw thud, close dry foley",
        seconds_total=2.8,
        band_lo_hz=45.0,
        band_hi_hz=4600.0,
        mix_weight=0.56,
        attack_guard_ms=8.0,
        sound_ids=("mycelial_chimera_growl", "mycelial_chimera_hurt", "mycelial_chimera_death", "mycelial_chimera_strike"),
    ),
    StemArchetype(
        stem_id="chimera_alpha_roar",
        group="entity",
        prompt="Apex agile cave predator combat war roar, fierce wild beast vocal snarl, resonant chest bite, dry foley",
        seconds_total=2.8,
        band_lo_hz=42.0,
        band_hi_hz=4800.0,
        mix_weight=0.58,
        attack_guard_ms=10.0,
        sound_ids=("chimera_roar",),
    ),
    StemArchetype(
        stem_id="purifier_drone_servo_thruster",
        group="entity",
        prompt="Copper clockwork automaton whirring wings, crisp brass spring ratchet click and small amethyst bell chime, close dry foley",
        seconds_total=2.4,
        band_lo_hz=220.0,
        band_hi_hz=8800.0,
        mix_weight=0.42,
        attack_guard_ms=8.0,
        sound_ids=("purifier_drone_hum", "purifier_drone_hurt", "purifier_drone_death", "purifier_drone_fire"),
    ),
    StemArchetype(
        stem_id="colony_symbiont_organic",
        group="entity",
        prompt="Gentle glowing cave insect cricket trill, soft damp soil digging rustle, warm wooden marimba chime, dry foley",
        seconds_total=2.4,
        band_lo_hz=150.0,
        band_hi_hz=7600.0,
        mix_weight=0.52,
        attack_guard_ms=8.0,
        sound_ids=("colony_symbiont_ambient", "colony_symbiont_work", "colony_symbiont_promote"),
    ),
    StemArchetype(
        stem_id="step_chitin_tarsus",
        group="entity",
        prompt="Steady brisk insect tarsus claws tapping rapidly on damp cave stone and moss floor, close dry footsteps foley",
        seconds_total=1.6,
        band_lo_hz=220.0,
        band_hi_hz=8600.0,
        mix_weight=0.54,
        attack_guard_ms=4.0,
        sound_ids=("mob_step_chitin",),
    ),
    StemArchetype(
        stem_id="step_arachnid_scurry",
        group="entity",
        prompt="Fast spindly cave spider claws skittering briskly on gritty basalt stone, dry brittle taps, close dry foley",
        seconds_total=1.6,
        band_lo_hz=260.0,
        band_hi_hz=9000.0,
        mix_weight=0.54,
        attack_guard_ms=4.0,
        sound_ids=("mob_step_arachnid",),
    ),
    StemArchetype(
        stem_id="step_heavy_carapace",
        group="entity",
        prompt="Heavy armored creature foot stomping onto cave bedrock with dull bone plate thud, close dry foley",
        seconds_total=1.8,
        band_lo_hz=42.0,
        band_hi_hz=3600.0,
        mix_weight=0.56,
        attack_guard_ms=4.0,
        sound_ids=("mob_step_heavy",),
    ),
    StemArchetype(
        stem_id="step_hover_flutter",
        group="entity",
        prompt="Soft airy leather wing beat swooshes and gentle rustling membrane breeze, close dry foley",
        seconds_total=1.8,
        band_lo_hz=140.0,
        band_hi_hz=7200.0,
        mix_weight=0.50,
        attack_guard_ms=6.0,
        sound_ids=("mob_step_hover",),
    ),
    StemArchetype(
        stem_id="step_colossal_seismic",
        group="entity",
        prompt="Colossal subterranean warden beast heavy stomp on deep basalt bedrock, earthy floor tremor thud, close dry foley",
        seconds_total=2.0,
        band_lo_hz=28.0,
        band_hi_hz=2200.0,
        mix_weight=0.58,
        attack_guard_ms=4.0,
        sound_ids=("mob_step_colossal",),
    ),
    StemArchetype(
        stem_id="sinkhole_glider_wing",
        group="entity",
        prompt="Bioluminescent cave glider leather membrane wing flap, soft airy vocal trill, close dry creature foley",
        seconds_total=2.4,
        band_lo_hz=140.0,
        band_hi_hz=7800.0,
        mix_weight=0.52,
        attack_guard_ms=8.0,
        sound_ids=("spore_glider_ambient", "spore_glider_hurt", "spore_glider_death"),
    ),
    StemArchetype(
        stem_id="sinkhole_root_keeper",
        group="entity",
        prompt="Deep earthen ancient cave root guardian wooden creak and chest growl, heavy bark slam, close dry foley",
        seconds_total=2.8,
        band_lo_hz=34.0,
        band_hi_hz=3400.0,
        mix_weight=0.58,
        attack_guard_ms=8.0,
        sound_ids=("root_keeper_ambient", "root_keeper_hurt", "root_keeper_death", "root_keeper_slam"),
    ),
    # =========================================================================
    # 7 Physical Block Surface Families (covers all 35 BLOCK_PHYSICAL_SOUNDS)
    # =========================================================================
    StemArchetype(
        stem_id="surf_mycelium_wet_moss",
        group="surface",
        prompt="Damp spongy forest moss block turf tearing softly, wet fibrous soil squish, close dry foley",
        seconds_total=2.4,
        band_lo_hz=45.0,
        band_hi_hz=2200.0,
        mix_weight=0.54,
        attack_guard_ms=6.0,
        sound_ids=("block_mycelium_break", "block_mycelium_step", "block_mycelium_place", "block_mycelium_hit", "block_mycelium_fall"),
    ),
    StemArchetype(
        stem_id="surf_synaptic_quartz_chime",
        group="surface",
        prompt="Struck resonant amethyst quartz block, crisp glassy crystal chime ping, brittle mineral fracture, close dry foley",
        seconds_total=2.4,
        band_lo_hz=480.0,
        band_hi_hz=9200.0,
        mix_weight=0.48,
        attack_guard_ms=5.0,
        sound_ids=("block_synaptic_node_break", "block_synaptic_node_step", "block_synaptic_node_place", "block_synaptic_node_hit", "block_synaptic_node_fall"),
    ),
    StemArchetype(
        stem_id="surf_chitin_hollow_shell",
        group="surface",
        prompt="Dry hollow bone block shell snapping, brittle insect chitin cracking and tapping on stone, close dry foley",
        seconds_total=2.4,
        band_lo_hz=450.0,
        band_hi_hz=8800.0,
        mix_weight=0.52,
        attack_guard_ms=5.0,
        sound_ids=("block_chitin_break", "block_chitin_step", "block_chitin_place", "block_chitin_hit", "block_chitin_fall"),
    ),
    StemArchetype(
        stem_id="surf_mycelial_stone_basalt",
        group="surface",
        prompt="Sharp stone pickaxe striking coarse dark deepslate basalt block, gritty gravel crunch, close dry foley",
        seconds_total=2.4,
        band_lo_hz=95.0,
        band_hi_hz=7200.0,
        mix_weight=0.50,
        attack_guard_ms=5.0,
        sound_ids=("block_mycelial_stone_break", "block_mycelial_stone_step", "block_mycelial_stone_place", "block_mycelial_stone_hit", "block_mycelial_stone_fall"),
    ),
    StemArchetype(
        stem_id="surf_purification_alloy_plate",
        group="surface",
        prompt="Hammered brushed copper block plate ring, crisp bronze anvil clink and taut spring latch, close dry foley",
        seconds_total=2.4,
        band_lo_hz=620.0,
        band_hi_hz=9200.0,
        mix_weight=0.42,
        attack_guard_ms=6.0,
        sound_ids=("block_purification_tech_break", "block_purification_tech_step", "block_purification_tech_place", "block_purification_tech_hit", "block_purification_tech_fall"),
    ),
    StemArchetype(
        stem_id="surf_antibody_cyst_visceral",
        group="surface",
        prompt="Viscous organic fluid sac squelch, wet sculk block membrane rupturing with sticky droplets, close dry foley",
        seconds_total=2.4,
        band_lo_hz=75.0,
        band_hi_hz=4800.0,
        mix_weight=0.54,
        attack_guard_ms=5.0,
        sound_ids=("block_antibody_cyst_break", "block_antibody_cyst_step", "block_antibody_cyst_place", "block_antibody_cyst_hit", "block_antibody_cyst_fall"),
    ),
    StemArchetype(
        stem_id="surf_hive_altar_monolith",
        group="surface",
        prompt="Massive subterranean carved stone altar impact thud, low resonant deepslate block knock, close dry foley",
        seconds_total=2.6,
        band_lo_hz=30.0,
        band_hi_hz=1800.0,
        mix_weight=0.52,
        attack_guard_ms=6.0,
        sound_ids=("block_hive_altar_break", "block_hive_altar_step", "block_hive_altar_place", "block_hive_altar_hit", "block_hive_altar_fall"),
    ),
    # =========================================================================
    # 8 Functional Blocks & Colony Dialects (covers all 24 BLOCK_FUNCTIONAL_SOUNDS)
    # =========================================================================
    StemArchetype(
        stem_id="func_spore_vent_bulb",
        group="block_func",
        prompt="Pressurized underground steam fungal geyser puff, wet organic spore pod pop, dry bone crumble, close foley",
        seconds_total=2.4,
        band_lo_hz=110.0,
        band_hi_hz=6800.0,
        mix_weight=0.52,
        attack_guard_ms=6.0,
        sound_ids=("spore_vent_puff", "spore_bulb_burst", "ephemeral_chitin_crumble"),
    ),
    StemArchetype(
        stem_id="func_purifier_turret_pylon",
        group="block_func",
        prompt="Spring-loaded copper piston bolt shot, struck amethyst crystal block ping and brass gear craft latch, close dry foley",
        seconds_total=2.4,
        band_lo_hz=260.0,
        band_hi_hz=9200.0,
        mix_weight=0.42,
        attack_guard_ms=6.0,
        sound_ids=("purification_pylon_pulse", "purification_turret_fire", "purification_turret_upgrade", "purification_turret_repair", "synaptic_bench_synthesize"),
    ),
    StemArchetype(
        stem_id="func_synaptic_symbiotic_node",
        group="block_func",
        prompt="Warm living amethyst crystal hearth chime, damp root creak and soft rushing cave wind pulse, close dry foley",
        seconds_total=2.6,
        band_lo_hz=120.0,
        band_hi_hz=7600.0,
        mix_weight=0.50,
        attack_guard_ms=8.0,
        sound_ids=("synaptic_node_mature", "symbiotic_node_charge", "symbiotic_node_teleport", "sovereign_nexus_pulse"),
    ),
    StemArchetype(
        stem_id="colony_dialect_harmonic_chords",
        group="block_func",
        prompt="Delicate amethyst crystal block and wooden marimba tone motif, gentle cave bell note, close dry studio foley",
        seconds_total=2.2,
        band_lo_hz=180.0,
        band_hi_hz=7800.0,
        mix_weight=0.48,
        attack_guard_ms=8.0,
        sound_ids=("colony_dialect_tone_1", "colony_dialect_tone_2", "colony_dialect_tone_3"),
    ),
    StemArchetype(
        stem_id="func_mycelial_rift_portal",
        group="block_func",
        prompt="Deep subterranean obsidian portal arch hum loop, eerie echoing nether cave wind drone and embers",
        seconds_total=3.2,
        band_lo_hz=38.0,
        band_hi_hz=3400.0,
        mix_weight=0.54,
        attack_guard_ms=24.0,
        seamless_loop=True,
        sound_ids=("mycelial_rift_hum",),
    ),
    StemArchetype(
        stem_id="reclaimed_bloom_chime",
        group="block_func",
        prompt="Bioluminescent cave flower harvest chime, gentle crisp leaf stem snap, sparkling dewdrop ping, close foley",
        seconds_total=2.0,
        band_lo_hz=180.0,
        band_hi_hz=8200.0,
        mix_weight=0.50,
        attack_guard_ms=6.0,
        sound_ids=("reclaimed_bloom_harvest", "reclaimed_bloom_pulse"),
    ),
    StemArchetype(
        stem_id="colony_dialect_resonance",
        group="block_func",
        prompt="Resonant amethyst crystal triad chord, damp wooden click and dull stone discordant thud, close dry foley",
        seconds_total=2.4,
        band_lo_hz=110.0,
        band_hi_hz=6800.0,
        mix_weight=0.48,
        attack_guard_ms=8.0,
        sound_ids=("colony_dialect_mastered", "colony_dialect_conflict", "colony_reconstruction_pulse"),
    ),
    StemArchetype(
        stem_id="eco_counterplay_amber",
        group="block_func",
        prompt="Heavy armored beast slamming chitin stone pillar stun crunch, steam ember quench hiss, amber crystal ring, dry foley",
        seconds_total=2.4,
        band_lo_hz=60.0,
        band_hi_hz=7200.0,
        mix_weight=0.54,
        attack_guard_ms=6.0,
        sound_ids=("brute_wall_stun", "pyro_shroom_defuse", "amber_vein_resonate"),
    ),
    # =========================================================================
    # 11 Item, Weapon, Relic & Projectile Stems (covers all 33 ITEM_PROJECTILE_SOUNDS)
    # =========================================================================
    StemArchetype(
        stem_id="item_purifier_scanner_tech",
        group="item",
        prompt="Small wooden and copper compass click, warm resonant amethyst crystal ping, brisk glass bottle mist spray, close dry foley",
        seconds_total=2.2,
        band_lo_hz=280.0,
        band_hi_hz=9600.0,
        mix_weight=0.42,
        attack_guard_ms=6.0,
        sound_ids=("bio_scanner_ping", "bio_scanner_thermal_pulse", "spore_antidote_cure", "neural_decoy_deploy", "purifier_drone_deploy", "command_staff_mark"),
    ),
    StemArchetype(
        stem_id="item_symbiotic_melee_blades",
        group="item",
        prompt="Razor-sharp bone blade whoosh slash, crisp piercing chitin lance thrust in air, close dry combat foley",
        seconds_total=2.0,
        band_lo_hz=160.0,
        band_hi_hz=8800.0,
        mix_weight=0.50,
        attack_guard_ms=4.0,
        sound_ids=("symbiotic_blade_slash", "enzyme_lance_thrust"),
    ),
    StemArchetype(
        stem_id="item_chitin_armor_clasp",
        group="item",
        prompt="Interlocking bone leather and chitin armor plate buckle snap, rigid plates locking tight, close dry foley",
        seconds_total=2.0,
        band_lo_hz=120.0,
        band_hi_hz=7800.0,
        mix_weight=0.50,
        attack_guard_ms=5.0,
        sound_ids=("chitin_armor_equip", "chitin_armor_evolve"),
    ),
    StemArchetype(
        stem_id="item_ballistic_launcher_grenade",
        group="item",
        prompt="Wooden crossbow and brass tube mortar thump, sharp gunpowder flash crack and stone debris scatter, close dry foley",
        seconds_total=2.4,
        band_lo_hz=55.0,
        band_hi_hz=8600.0,
        mix_weight=0.46,
        attack_guard_ms=6.0,
        sound_ids=("spore_launcher_fire", "purifier_grenade_throw", "purifier_grenade_detonate"),
    ),
    StemArchetype(
        stem_id="item_projectile_wet_impact",
        group="item",
        prompt="Corrosive wet acid globule splatter impact sizzle and damp spore pod burst splash, close dry foley",
        seconds_total=1.8,
        band_lo_hz=95.0,
        band_hi_hz=8200.0,
        mix_weight=0.52,
        attack_guard_ms=4.0,
        sound_ids=("acid_spit_impact", "neutralizing_spore_impact"),
    ),
    StemArchetype(
        stem_id="item_visceral_mutagen_serum",
        group="item",
        prompt="Viscous liquid rushing through glass syringe needle, dense wet bubbling bio-fluid squelch under skin, sustained fizz, close dry foley",
        seconds_total=2.2,
        band_lo_hz=110.0,
        band_hi_hz=7200.0,
        mix_weight=0.50,
        attack_guard_ms=6.0,
        sound_ids=("bio_extractor_draw", "counter_serum_inject", "symbiotic_mutagen_implant"),
    ),
    StemArchetype(
        stem_id="item_pheromone_ovum_flesh",
        group="item",
        prompt="Soft misty potion splash spray hiss and wet organic leathery egg shell cracking open, close dry foley",
        seconds_total=2.2,
        band_lo_hz=90.0,
        band_hi_hz=6800.0,
        mix_weight=0.52,
        attack_guard_ms=6.0,
        sound_ids=("truce_pheromone_spray", "chimera_ovum_hatch"),
    ),
    StemArchetype(
        stem_id="item_eldritch_relic_catalyst",
        group="item",
        prompt="Wet organic graft splice snap, sharp amethyst glass sting and warm wax seal pact thump, close foley",
        seconds_total=2.6,
        band_lo_hz=75.0,
        band_hi_hz=7600.0,
        mix_weight=0.50,
        attack_guard_ms=7.0,
        sound_ids=("synaptic_graft_splice", "volatile_engram_pulse", "schism_injector_sting", "quorum_seal_pact"),
    ),
    StemArchetype(
        stem_id="item_eldritch_beacon_crown",
        group="item",
        prompt="Nether beacon flare ignition, crackling amethyst crystal surge and deep stone crown chime hum",
        seconds_total=3.0,
        band_lo_hz=55.0,
        band_hi_hz=7800.0,
        mix_weight=0.52,
        attack_guard_ms=10.0,
        sound_ids=("tide_beacon_activate", "rift_catalyst_ignite", "overmind_crown_surge"),
    ),
    StemArchetype(
        stem_id="sinkhole_sap_elixir",
        group="item",
        prompt="Rich thick honeyed sap pouring into glass potion bottle, warm golden chime, bouncy fungal cap thrum",
        seconds_total=2.2,
        band_lo_hz=120.0,
        band_hi_hz=7600.0,
        mix_weight=0.50,
        attack_guard_ms=6.0,
        sound_ids=("root_sap_harvest", "root_cap_bounce", "resonant_elixir_drink"),
    ),
    StemArchetype(
        stem_id="ui_bio_tactile_tech",
        group="item",
        prompt="Dry parchment book page turn tap, crisp wooden UI button click and small delicate amethyst note chime, close dry foley",
        seconds_total=1.6,
        band_lo_hz=240.0,
        band_hi_hz=9200.0,
        mix_weight=0.40,
        attack_guard_ms=5.0,
        sound_ids=("ui_codex_tab", "ui_action_confirm", "station_upkeep_low"),
    ),
    # =========================================================================
    # 19 System, Homeostasis, Biome Loops & One-Shot Stems (covers all 41 SYSTEM_ENV_IDS)
    # =========================================================================
    StemArchetype(
        stem_id="env_biome_caverns_loop",
        group="system_env",
        prompt="Continuous deep subterranean cave wind bed, dark humid cavern draft air loop, distant deep earth rumble",
        seconds_total=6.2,
        band_lo_hz=34.0,
        band_hi_hz=3000.0,
        mix_weight=0.58,
        attack_guard_ms=26.0,
        seamless_loop=True,
        sound_ids=("biome_caverns_loop", "fungal_tide_ambience"),
    ),
    StemArchetype(
        stem_id="env_biome_caverns_fx",
        group="system_env",
        prompt="Eerie distant subterranean cave rock groan, wet spore droplets echoing in deep basalt vault, dry foley",
        seconds_total=3.6,
        band_lo_hz=45.0,
        band_hi_hz=4200.0,
        mix_weight=0.54,
        attack_guard_ms=10.0,
        sound_ids=("biome_caverns_mood", "biome_caverns_additions"),
    ),
    StemArchetype(
        stem_id="env_biome_sanctum_loop",
        group="system_env",
        prompt="Dark living organic cathedral ambience loop, slow subterranean sculk heartbeat throb, cavern breath",
        seconds_total=6.2,
        band_lo_hz=36.0,
        band_hi_hz=4000.0,
        mix_weight=0.58,
        attack_guard_ms=26.0,
        seamless_loop=True,
        sound_ids=("biome_sanctum_loop", "homeostasis_heartbeat"),
    ),
    StemArchetype(
        stem_id="env_biome_sanctum_fx",
        group="system_env",
        prompt="Deep living arterial cave membrane sigh and distant shimmering glass bell chord one-shot",
        seconds_total=3.4,
        band_lo_hz=48.0,
        band_hi_hz=4800.0,
        mix_weight=0.52,
        attack_guard_ms=10.0,
        sound_ids=("biome_sanctum_mood", "biome_sanctum_additions"),
    ),
    StemArchetype(
        stem_id="env_biome_forest_loop",
        group="system_env",
        prompt="Bioluminescent giant mushroom forest night breeze loop, soft rustling fungal gills, mystical cave air",
        seconds_total=6.2,
        band_lo_hz=60.0,
        band_hi_hz=5600.0,
        mix_weight=0.58,
        attack_guard_ms=26.0,
        seamless_loop=True,
        sound_ids=("biome_forest_loop",),
    ),
    StemArchetype(
        stem_id="env_biome_forest_fx",
        group="system_env",
        prompt="Sparkling bioluminescent spore burst, soft creaking fungal wooden stalks and low underground root tremor",
        seconds_total=3.2,
        band_lo_hz=45.0,
        band_hi_hz=6200.0,
        mix_weight=0.52,
        attack_guard_ms=10.0,
        sound_ids=("biome_forest_mood", "biome_forest_additions", "mycorrhizal_tremor"),
    ),
    StemArchetype(
        stem_id="sys_hive_evolution_surge",
        group="system_env",
        prompt="Dark deep subterranean sculk rumble swell, rising amethyst crystal resonance and hollow bone rattle",
        seconds_total=3.0,
        band_lo_hz=55.0,
        band_hi_hz=6800.0,
        mix_weight=0.52,
        attack_guard_ms=10.0,
        sound_ids=("hive_mutate_tier2", "synaptic_pulse", "immune_surge", "mob_promote_mutated", "mob_promote_elite"),
    ),
    StemArchetype(
        stem_id="sys_hive_tactical_adapt",
        group="system_env",
        prompt="Menacing deep cave sculk sensor throb, brittle amethyst chime lock and distant stone gong toll",
        seconds_total=2.8,
        band_lo_hz=50.0,
        band_hi_hz=6400.0,
        mix_weight=0.50,
        attack_guard_ms=10.0,
        sound_ids=("hive_trait_adapt", "hive_synergy_unlock", "hive_threat_milestone"),
    ),
    StemArchetype(
        stem_id="sys_visceral_assimilation",
        group="system_env",
        prompt="Rapid organic burrowing tunnel retreat, wet cellular conversion squelch and taut cyst snap, dry foley",
        seconds_total=2.4,
        band_lo_hz=70.0,
        band_hi_hz=5800.0,
        mix_weight=0.54,
        attack_guard_ms=8.0,
        sound_ids=("mob_dawn_retreat", "infected_mob_convert", "antibody_cyst_encapsulate"),
    ),
    StemArchetype(
        stem_id="sys_acid_rain_necrophagy",
        group="system_env",
        prompt="Corrosive acid rain sizzling on damp cave stone, wet sinister creature feeding crunch and shrill swarm call",
        seconds_total=2.4,
        band_lo_hz=110.0,
        band_hi_hz=8400.0,
        mix_weight=0.52,
        attack_guard_ms=6.0,
        sound_ids=("acid_rain_sizzle", "necrophagy_feast", "swarm_flank_call"),
    ),
    StemArchetype(
        stem_id="sys_purifier_emp_pulse",
        group="system_env",
        prompt="Heavy copper lightning rod crackle snap, resonant bronze spring recoil rattle and amethyst hum cut, close dry foley",
        seconds_total=2.4,
        band_lo_hz=110.0,
        band_hi_hz=8800.0,
        mix_weight=0.42,
        attack_guard_ms=6.0,
        sound_ids=("suppressor_discharge", "squad_desync", "corrosive_jam"),
    ),
    StemArchetype(
        stem_id="sys_purifier_pylon_bed",
        group="system_env",
        prompt="Steady warm resonant amethyst crystal and humming copper coil beacon drone loop, soft cave hum",
        seconds_total=5.0,
        band_lo_hz=85.0,
        band_hi_hz=4400.0,
        mix_weight=0.44,
        attack_guard_ms=24.0,
        seamless_loop=True,
        sound_ids=("purification_pylon_hum",),
    ),
    StemArchetype(
        stem_id="primordial_biolum_pulse",
        group="system_env",
        prompt="Deep ancient glowing cave root aura pulse, warm resonant stone and amethyst sub throb",
        seconds_total=2.8,
        band_lo_hz=55.0,
        band_hi_hz=5800.0,
        mix_weight=0.52,
        attack_guard_ms=10.0,
        sound_ids=("primordial_presence_pulse",),
    ),
    StemArchetype(
        stem_id="sovereign_nexus_bed",
        group="system_env",
        prompt="Warm subterranean amethyst crystal hearth drone loop, soft cave breeze and living wood resonance",
        seconds_total=5.0,
        band_lo_hz=65.0,
        band_hi_hz=4400.0,
        mix_weight=0.52,
        attack_guard_ms=26.0,
        seamless_loop=True,
        sound_ids=("sovereign_nexus_hum",),
    ),
    StemArchetype(
        stem_id="boss_tension_bed",
        group="system_env",
        prompt="Dark subterranean cave boss tension drone, slow pulsing deep heartbeat bed, ominous cavern wind",
        seconds_total=5.2,
        band_lo_hz=32.0,
        band_hi_hz=2200.0,
        mix_weight=0.54,
        attack_guard_ms=26.0,
        seamless_loop=True,
        sound_ids=("boss_combat_tension_bed",),
    ),
    StemArchetype(
        stem_id="sinkhole_updraft_column",
        group="system_env",
        prompt="Steady vertical subterranean wind chimney updraft loop, warm rushing cavern air column with spore hum",
        seconds_total=5.0,
        band_lo_hz=45.0,
        band_hi_hz=4600.0,
        mix_weight=0.54,
        attack_guard_ms=26.0,
        seamless_loop=True,
        sound_ids=("spore_updraft_vent_pulse", "sinkhole_updraft_loop"),
    ),
    StemArchetype(
        stem_id="adaptive_hive_tension_bed",
        group="system_env",
        prompt="Subtle ominous subterranean exploration drone loop, soft breathing cave wind and warm distant glass chime air",
        seconds_total=5.2,
        band_lo_hz=38.0,
        band_hi_hz=3200.0,
        mix_weight=0.52,
        attack_guard_ms=26.0,
        seamless_loop=True,
        sound_ids=("hive_tension_bed_low",),
    ),
    StemArchetype(
        stem_id="adaptive_swarm_combat_bed",
        group="system_env",
        prompt="Driving rhythmic subterranean hive skirmish loop, deep hollow wooden percussion bed and low tension cave drum",
        seconds_total=5.0,
        band_lo_hz=40.0,
        band_hi_hz=3600.0,
        mix_weight=0.52,
        attack_guard_ms=26.0,
        seamless_loop=True,
        sound_ids=("hive_swarm_combat_bed",),
    ),
    StemArchetype(
        stem_id="cavern_spatial_micro_ambience",
        group="system_env",
        prompt="Cave water spore droplet ping, deep wooden root creak, eerie cavern wind whisper and amethyst echo, dry foley",
        seconds_total=2.8,
        band_lo_hz=75.0,
        band_hi_hz=7200.0,
        mix_weight=0.52,
        attack_guard_ms=8.0,
        sound_ids=("cavern_spore_drip", "subterranean_hyphal_creak", "deep_mycelium_whisper", "sanctum_harmonic_chime"),
    ),
)

SOUND_TO_ARCHETYPE: dict[str, StemArchetype] = {
    sid: arch for arch in AI_STEM_ARCHETYPES for sid in arch.sound_ids
}

_STEM_CACHE_LOCK = threading.Lock()
_STEM_WAVE_CACHE: dict[str, np.ndarray] = {}


def is_ai_hybrid(sound_id: str) -> bool:
    """Return True iff sound_id maps to an RTX 4060 Stable Audio Open Small archetype stem."""
    return sound_id in SOUND_TO_ARCHETYPE


def get_stem_id(sound_id: str) -> str:
    """Return the mapped AI stem_id for sound_id (or empty string if unmapped)."""
    arch = SOUND_TO_ARCHETYPE.get(sound_id)
    return arch.stem_id if arch is not None else ""


def _seed_for_stem(stem_id: str) -> int:
    digest = hashlib.sha256(f"mycelialmind:sao-small:v2:{stem_id}".encode("utf-8")).digest()
    return int.from_bytes(digest[:4], "little") & 0x7FFFFFFF


def _bandpass(sig: np.ndarray, low_hz: float, high_hz: float) -> np.ndarray:
    lo = float(np.clip(low_hz / (0.5 * SAMPLE_RATE), 0.0008, 0.94))
    hi = float(np.clip(high_hz / (0.5 * SAMPLE_RATE), lo + 0.003, 0.96))
    sos = signal.butter(2, [lo, hi], btype="bandpass", output="sos")
    return signal.sosfilt(sos, sig)


def _read_wav_mono(path: Path) -> np.ndarray:
    with wave.open(str(path), "rb") as wf:
        channels = wf.getnchannels()
        frames = wf.readframes(wf.getnframes())
    pcm = np.frombuffer(frames, dtype="<i2").astype(np.float64) / 32768.0
    if channels > 1:
        pcm = pcm.reshape(-1, channels).mean(axis=1)
    return pcm


def _write_wav_mono(path: Path, samples: np.ndarray) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    pcm = np.round(np.clip(samples, -1.0, 1.0) * 32767.0).astype("<i2")
    with wave.open(str(path), "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(pcm.tobytes())


def _resolve_hf_repo() -> str:
    has_token = bool(os.environ.get("HF_TOKEN") or os.environ.get("HUGGING_FACE_HUB_TOKEN"))
    if not has_token:
        tok_file = Path.home() / ".cache" / "huggingface" / "token"
        has_token = tok_file.is_file() and bool(tok_file.read_text(encoding="utf-8", errors="ignore").strip())
    return "stabilityai/stable-audio-open-small" if has_token else "joeriben/stable-audio-open-small"


def _postprocess_generated_stem(raw_mono: np.ndarray, arch: StemArchetype) -> np.ndarray:
    w = np.nan_to_num(raw_mono.astype(np.float64), nan=0.0, posinf=0.0, neginf=0.0)
    w = w - np.mean(w)

    peak = float(np.max(np.abs(w)))
    if peak > 1e-6:
        idx_active = np.flatnonzero(np.abs(w) >= peak * 0.04)
        if len(idx_active) > 0:
            first = max(0, int(idx_active[0]) - int(0.012 * SAMPLE_RATE))
            w = w[first:]

    target_len = max(int(0.8 * SAMPLE_RATE), int(round(arch.seconds_total * SAMPLE_RATE)))
    if len(w) > target_len:
        w = w[:target_len]
    elif len(w) < target_len:
        w = np.pad(w, (0, target_len - len(w)))

    w = _bandpass(w, arch.band_lo_hz, arch.band_hi_hz)
    w = w - np.mean(w)
    fade = min(len(w) // 8, int(0.025 * SAMPLE_RATE))
    if fade > 4:
        w[:fade] *= np.linspace(0.0, 1.0, fade)
        w[-fade:] *= np.linspace(1.0, 0.0, fade)
    peak_mid = float(np.max(np.abs(w)))
    if peak_mid > 1e-7:
        w *= 0.90 / peak_mid
    rms_now = float(np.sqrt(np.mean(w * w)))
    if 1e-6 < rms_now < 0.032:
        gain = min(3.5, 0.040 / rms_now)
        w = np.tanh(w * gain) / np.tanh(gain)
    peak_after = float(np.max(np.abs(w)))
    if peak_after > 1e-7:
        w *= 0.90 / peak_after
    return np.clip(w, -0.95, 0.95)


def audit_stem_quality(samples: np.ndarray, sample_rate: int = SAMPLE_RATE) -> dict[str, object]:
    """Audit a cached mono stem for silence ratio, clipping, DC offset, and active energy coverage."""
    w = np.asarray(samples, dtype=np.float64)
    n = len(w)
    if n < 256:
        return {
            "ok": False,
            "rms_dbfs": -96.0,
            "peak_dbfs": -96.0,
            "active_ratio": 0.0,
            "silence_tail_ratio": 1.0,
            "clipped_samples": 0,
            "dc_offset": 0.0,
            "issues": ["stem too short"],
        }
    abs_w = np.abs(w)
    peak = float(np.max(abs_w))
    rms = float(np.sqrt(np.mean(w * w)))
    rms_db = float(20.0 * np.log10(max(rms, 1e-6)))
    peak_db = float(20.0 * np.log10(max(peak, 1e-6)))
    dc = float(np.mean(w))
    clipped = int(np.sum(abs_w >= 0.995))

    block = 2048
    n_blocks = max(1, n // block)
    if n >= block:
        blocks = w[: n_blocks * block].reshape(n_blocks, block)
        b_rms = np.sqrt(np.mean(blocks * blocks, axis=1))
        b_peak = float(np.max(b_rms))
        active_mask = b_rms >= (0.06 * b_peak) if b_peak > 1e-7 else np.zeros(n_blocks, dtype=bool)
        active_ratio = float(np.mean(active_mask))
        active_idx = np.flatnonzero(active_mask)
        silence_tail_ratio = float((n_blocks - 1 - int(active_idx[-1])) / float(n_blocks)) if len(active_idx) > 0 else 1.0
    else:
        active_ratio = 1.0 if peak > 1e-4 else 0.0
        silence_tail_ratio = 0.0

    issues: list[str] = []
    if peak_db < -18.0:
        issues.append(f"peak too low ({peak_db:.1f} dBFS)")
    if rms_db < -38.0:
        issues.append(f"rms too low ({rms_db:.1f} dBFS)")
    if active_ratio < 0.03:
        issues.append(f"active_ratio too low ({active_ratio:.2f})")
    if abs(dc) > 0.02:
        issues.append(f"dc_offset too high ({dc:.4f})")
    if clipped > 8:
        issues.append(f"clipped_samples={clipped}")

    return {
        "ok": len(issues) == 0,
        "rms_dbfs": round(rms_db, 2),
        "peak_dbfs": round(peak_db, 2),
        "active_ratio": round(active_ratio, 3),
        "silence_tail_ratio": round(silence_tail_ratio, 3),
        "clipped_samples": clipped,
        "dc_offset": round(dc, 5),
        "duration_secs": round(n / float(sample_rate), 3),
        "issues": issues,
    }


def audit_all_stems(stems_dir: Path | None = None) -> dict[str, object]:
    """Audit every registered AI stem on disk and return aggregate QC telemetry."""
    out_dir = stems_dir or AI_STEMS_DIR
    per_stem: dict[str, dict[str, object]] = {}
    failed: list[str] = []
    missing_files: list[str] = []
    for arch in AI_STEM_ARCHETYPES:
        wav_path = out_dir / f"{arch.stem_id}.wav"
        if not wav_path.is_file():
            missing_files.append(arch.stem_id)
            continue
        qc = audit_stem_quality(_read_wav_mono(wav_path), SAMPLE_RATE)
        per_stem[arch.stem_id] = qc
        if not qc["ok"]:
            failed.append(arch.stem_id)
    return {
        "ok": len(missing_files) == 0 and len(failed) == 0,
        "stem_count": len(AI_STEM_ARCHETYPES),
        "checked_count": len(per_stem),
        "missing_files": missing_files,
        "failed_stems": failed,
        "stems": per_stem,
    }


def ensure_ai_stems(stems_dir: Path | None = None, *, force_regen: bool = False) -> dict[str, Path]:
    """Ensure all RTX 4060 Stable Audio Open Small stems exist locally, generating missing ones via CUDA."""
    out_dir = stems_dir or AI_STEMS_DIR
    manifest_path = out_dir / "stem_manifest.json"
    out_dir.mkdir(parents=True, exist_ok=True)

    missing = [
        arch
        for arch in AI_STEM_ARCHETYPES
        if force_regen or not (out_dir / f"{arch.stem_id}.wav").is_file() or (out_dir / f"{arch.stem_id}.wav").stat().st_size < 1024
    ]
    if not missing and manifest_path.is_file():
        manifest_data = json.loads(manifest_path.read_text(encoding="utf-8"))
        changed = (
            manifest_data.get("mapped_sound_events") != len(SOUND_TO_ARCHETYPE)
            or manifest_data.get("stem_count") != len(AI_STEM_ARCHETYPES)
        )
        stems_meta = manifest_data.get("stems", {})
        for arch in AI_STEM_ARCHETYPES:
            entry = stems_meta.get(arch.stem_id)
            desired_ids = list(arch.sound_ids)
            if isinstance(entry, dict):
                if entry.get("sound_ids") != desired_ids:
                    entry["sound_ids"] = desired_ids
                    changed = True
                if "qc" not in entry:
                    entry["qc"] = audit_stem_quality(_read_wav_mono(out_dir / f"{arch.stem_id}.wav"), SAMPLE_RATE)
                    changed = True
        if changed:
            manifest_data["stem_count"] = len(AI_STEM_ARCHETYPES)
            manifest_data["mapped_sound_events"] = len(SOUND_TO_ARCHETYPE)
            manifest_path.write_text(json.dumps(manifest_data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        return {arch.stem_id: out_dir / f"{arch.stem_id}.wav" for arch in AI_STEM_ARCHETYPES}

    import torch
    from huggingface_hub import hf_hub_download
    from safetensors.torch import load_file
    from stable_audio_tools.inference.generation import generate_diffusion_cond
    from stable_audio_tools.models.factory import create_model_from_config

    if not torch.cuda.is_available():
        raise RuntimeError("CUDA GPU not available; RTX 4060 CUDA is required to generate Stable Audio Open Small stems.")

    device = "cuda"
    device_name = torch.cuda.get_device_name(0)
    repo_id = _resolve_hf_repo()
    cfg_path = hf_hub_download(repo_id=repo_id, filename="model_config.json")
    ckpt_path = hf_hub_download(repo_id=repo_id, filename="model.safetensors")

    model_config = json.loads(Path(cfg_path).read_text(encoding="utf-8"))
    sample_rate = int(model_config.get("sample_rate", SAMPLE_RATE))
    sample_size = int(model_config.get("sample_size", 524288))

    model = create_model_from_config(model_config)
    state_dict = load_file(ckpt_path, device=device)
    model.load_state_dict(state_dict, strict=False, assign=True)
    del state_dict
    model = model.eval().to(device=device)
    # T5Conditioner stores HF T5 in __dict__["model"] initialized to fp16, which overflows
    # in transformers 5.x sdpa_attention; cast it explicitly to float32 alongside conditioner.
    if getattr(model, "conditioner", None) is not None:
        model.conditioner = model.conditioner.to(device=device, dtype=torch.float32)
        for cond in getattr(model.conditioner, "conditioners", {}).values():
            t5_inner = cond.__dict__.get("model")
            if t5_inner is not None and hasattr(t5_inner, "to"):
                t5_inner.to(device=device, dtype=torch.float32)
    # Use bfloat16 on Ada Lovelace (RTX 4060) for diffusion + VAE decode speed & low VRAM (~2 GB)
    if getattr(model, "model", None) is not None:
        model.model = model.model.to(dtype=torch.bfloat16)
    if getattr(model, "pretransform", None) is not None:
        model.pretransform = model.pretransform.to(dtype=torch.bfloat16)

    stem_entries: dict[str, dict[str, object]] = {}
    if manifest_path.is_file() and not force_regen:
        try:
            old = json.loads(manifest_path.read_text(encoding="utf-8"))
            stem_entries = dict(old.get("stems", {}))
        except Exception:
            stem_entries = {}

    def _finalize_stem_item(arch: StemArchetype, mono_t: np.ndarray, batch_seed: int, idx_b: int):
        if sample_rate != SAMPLE_RATE:
            new_len = int(round(len(mono_t) * SAMPLE_RATE / float(sample_rate)))
            mono_t = np.interp(
                np.linspace(0.0, 1.0, new_len, endpoint=False),
                np.linspace(0.0, 1.0, len(mono_t), endpoint=False),
                mono_t,
            )
        clean = _postprocess_generated_stem(mono_t, arch)
        wav_path = out_dir / f"{arch.stem_id}.wav"
        _write_wav_mono(wav_path, clean)
        with _STEM_CACHE_LOCK:
            _STEM_WAVE_CACHE[arch.stem_id] = clean
        qc = audit_stem_quality(clean, SAMPLE_RATE)
        entry = {
            "group": arch.group,
            "prompt": arch.prompt,
            "seed": batch_seed,
            "batch_index": idx_b,
            "seconds_total": arch.seconds_total,
            "duration_secs": round(len(clean) / float(SAMPLE_RATE), 3),
            "rms_dbfs": qc["rms_dbfs"],
            "peak_dbfs": qc["peak_dbfs"],
            "qc": qc,
            "sound_ids": list(arch.sound_ids),
        }
        return arch.stem_id, entry

    ds_ratio = int(getattr(getattr(model, "pretransform", None), "downsampling_ratio", 2048))
    missing_sorted = sorted(missing, key=lambda a: (float(a.seconds_total), a.stem_id))
    post_futures = []
    with ThreadPoolExecutor(max_workers=4) as io_pool:
        for start in range(0, len(missing_sorted), INFERENCE_BATCH_SIZE):
            batch = missing_sorted[start : start + INFERENCE_BATCH_SIZE]
            batch_seed = _seed_for_stem(batch[0].stem_id)
            conditioning = [{"prompt": arch.prompt, "seconds_total": float(arch.seconds_total)} for arch in batch]
            with torch.inference_mode(), torch.amp.autocast("cuda", dtype=torch.bfloat16):
                latents = generate_diffusion_cond(
                    model,
                    steps=INFERENCE_STEPS,
                    cfg_scale=1.0,
                    conditioning=conditioning,
                    batch_size=len(batch),
                    sample_size=sample_size,
                    sample_rate=sample_rate,
                    sampler_type="pingpong",
                    seed=batch_seed,
                    device=device,
                    return_latents=True,
                )

            # Separate DiT from sliced VAE decode to stay under RTX 4060 8 GB VRAM with zero WDDM spill
            max_sec = max(float(arch.seconds_total) for arch in batch)
            need_lat = min(int(latents.shape[-1]), int(math.ceil((max_sec + 1.0) * sample_rate / float(ds_ratio))))
            decode_mb = 1 if need_lat > 120 else 2
            torch.backends.cudnn.benchmark = False
            torch.backends.cuda.matmul.allow_tf32 = True
            torch.backends.cudnn.allow_tf32 = True

            decoded_chunks: list[np.ndarray] = []
            with torch.inference_mode(), torch.amp.autocast("cuda", dtype=torch.bfloat16):
                if getattr(model, "pretransform", None) is not None:
                    for mb_start in range(0, len(batch), decode_mb):
                        lat_slice = latents[mb_start : mb_start + decode_mb, :, :need_lat]
                        wav_dec = model.pretransform.decode(lat_slice)
                        decoded_chunks.append(wav_dec.float().mean(dim=1).cpu().numpy())
                        del wav_dec
                else:
                    decoded_chunks.append(latents.float().mean(dim=1).cpu().numpy())
            del latents

            mono_batch = np.concatenate(decoded_chunks, axis=0) if len(decoded_chunks) > 1 else decoded_chunks[0]
            for idx_b, arch in enumerate(batch):
                post_futures.append(io_pool.submit(_finalize_stem_item, arch, mono_batch[idx_b], batch_seed, idx_b))

        for fut in post_futures:
            sid, entry = fut.result()
            stem_entries[sid] = entry

    # Rebuild manifest with current archetype entries only
    final_entries: dict[str, dict[str, object]] = {}
    for arch in AI_STEM_ARCHETYPES:
        wav_path = out_dir / f"{arch.stem_id}.wav"
        if arch.stem_id in stem_entries:
            ent = dict(stem_entries[arch.stem_id])
            ent["sound_ids"] = list(arch.sound_ids)
            if wav_path.is_file():
                ent["qc"] = audit_stem_quality(_read_wav_mono(wav_path), SAMPLE_RATE)
            final_entries[arch.stem_id] = ent
        elif wav_path.is_file():
            clean = _read_wav_mono(wav_path)
            qc = audit_stem_quality(clean, SAMPLE_RATE)
            final_entries[arch.stem_id] = {
                "group": arch.group,
                "prompt": arch.prompt,
                "seed": _seed_for_stem(arch.stem_id),
                "seconds_total": arch.seconds_total,
                "duration_secs": round(len(clean) / float(SAMPLE_RATE), 3),
                "rms_dbfs": qc["rms_dbfs"],
                "peak_dbfs": qc["peak_dbfs"],
                "qc": qc,
                "sound_ids": list(arch.sound_ids),
            }

    manifest = {
        "model_repo": repo_id,
        "device_name": device_name,
        "torch_version": torch.__version__,
        "dtype": "bfloat16",
        "steps": INFERENCE_STEPS,
        "batch_size": INFERENCE_BATCH_SIZE,
        "vae_sliced_decode": True,
        "sampler_type": "pingpong",
        "sample_rate": SAMPLE_RATE,
        "stem_count": len(AI_STEM_ARCHETYPES),
        "mapped_sound_events": len(SOUND_TO_ARCHETYPE),
        "stems": {k: final_entries[k] for k in sorted(final_entries)},
    }
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    del model
    torch.cuda.empty_cache()
    return {arch.stem_id: out_dir / f"{arch.stem_id}.wav" for arch in AI_STEM_ARCHETYPES}


def _load_cached_stem(stem_id: str, stems_dir: Path | None = None) -> np.ndarray | None:
    with _STEM_CACHE_LOCK:
        cached = _STEM_WAVE_CACHE.get(stem_id)
    if cached is not None:
        return cached
    wav_path = (stems_dir or AI_STEMS_DIR) / f"{stem_id}.wav"
    if not wav_path.is_file():
        return None
    samples = _read_wav_mono(wav_path)
    with _STEM_CACHE_LOCK:
        _STEM_WAVE_CACHE[stem_id] = samples
    return samples


def preload_all_stems(stems_dir: Path | None = None) -> int:
    """Load all cached archetype stems into RAM & VRAM once before parallel synthesis."""
    target_dir = stems_dir or AI_STEMS_DIR
    loaded: dict[str, np.ndarray] = {}
    for arch in AI_STEM_ARCHETYPES:
        samples = _load_cached_stem(arch.stem_id, stems_dir=target_dir)
        if samples is not None:
            loaded[arch.stem_id] = samples
    return _preload_ai_stems_gpu(loaded) if loaded else 0


def _envelope_follower(wave: np.ndarray, window_ms: float = 18.0) -> np.ndarray:
    """Compute a smooth [0, 1] envelope follower tracking the exact multi-act dynamics of wave."""
    n = len(wave)
    if n == 0:
        return np.zeros(0, dtype=np.float64)
    win = max(8, int(SAMPLE_RATE * window_ms / 1000.0))
    rect = np.abs(wave)
    kernel = np.ones(win, dtype=np.float64) / float(win)
    env = np.convolve(rect, kernel, mode="same")
    peak = float(np.max(env))
    if peak < 1e-8:
        return np.zeros(n, dtype=np.float64)
    return np.clip(env / peak, 0.0, 1.0)


def _cpu_hpss_harmonic_soft(sig: np.ndarray, sample_rate: int = SAMPLE_RATE, strength: float = 0.60) -> np.ndarray:
    """CPU STFT Wiener Harmonic/Percussive separation matching cuda_dsp._gpu_hpss_harmonic_soft."""
    n = len(sig)
    if strength <= 1e-4 or n < 1024:
        return sig
    _, _, zxx = signal.stft(sig, fs=sample_rate, window="hann", nperseg=1024, noverlap=768)
    mag = np.abs(zxx)
    h_est = ndimage.uniform_filter1d(mag, size=9, axis=1, mode="constant")
    p_est = ndimage.uniform_filter1d(mag, size=9, axis=0, mode="constant")
    h2 = h_est * h_est
    p2 = p_est * p_est
    mask_h = h2 / (h2 + p2 + 1e-9)
    _, out = signal.istft(zxx * ((1.0 - float(strength)) + float(strength) * mask_h), fs=sample_rate, window="hann", nperseg=1024, noverlap=768)
    if len(out) >= n:
        return np.asarray(out[:n], dtype=np.float64)
    return np.pad(np.asarray(out, dtype=np.float64), (0, n - len(out)))


def _cpu_align_stem_spectral_tilt(
    stem_wave: np.ndarray,
    dsp_wave: np.ndarray,
    sample_rate: int = SAMPLE_RATE,
    strength: float = 0.45,
) -> np.ndarray:
    """Match cuda_dsp._gpu_align_spectral_tilt on CPU so AI stems inherit DSP spectral balance cleanly."""
    n = len(stem_wave)
    if strength <= 1e-4 or n < 512 or len(dsp_wave) != n:
        return stem_wave
    s_spec = np.fft.rfft(stem_wave)
    d_spec = np.fft.rfft(dsp_wave)
    freqs = np.fft.rfftfreq(n, 1.0 / float(sample_rate))
    s_pwr = (np.abs(s_spec) ** 2) + 1e-12
    d_pwr = (np.abs(d_spec) ** 2) + 1e-12
    c_stem = float(np.sum(freqs * s_pwr) / np.sum(s_pwr))
    c_dsp = float(np.sum(freqs * d_pwr) / np.sum(d_pwr))
    ratio = float(np.clip(c_dsp / max(60.0, c_stem), 0.55, 1.85))
    tilt_exp = float(np.clip(np.log2(ratio) * float(strength), -0.55, 0.55))
    pivot = float(np.clip(c_dsp, 220.0, 4200.0))
    tilt = np.clip((np.maximum(freqs, 40.0) / pivot) ** tilt_exp, 0.55, 1.75)
    return np.asarray(np.fft.irfft(s_spec * tilt, n=n), dtype=np.float64)


def _prepare_seamless_stem_body(stem: np.ndarray, sample_rate: int = SAMPLE_RATE) -> np.ndarray:
    """Trim silent fade tails and build a continuous palindrome loop from the steady active stem body."""
    n = len(stem)
    block = 2048
    if n < block * 2:
        return stem
    n_blocks = n // block
    blocks = stem[: n_blocks * block].reshape(n_blocks, block)
    block_rms = np.sqrt(np.mean(blocks * blocks, axis=1))
    peak_rms = float(np.max(block_rms))
    if peak_rms > 1e-8:
        active_idx = np.flatnonzero(block_rms >= 0.28 * peak_rms)
        if len(active_idx) > 0:
            start = int(active_idx[0]) * block
            end = int(active_idx[-1] + 1) * block
            active = stem[start:end] if (end - start) >= int(0.5 * sample_rate) else stem
        else:
            active = stem
    else:
        active = stem
    return np.ascontiguousarray(np.concatenate([active, active[::-1]]))


def blend_with_ai_stem(
    dsp_wave: np.ndarray,
    sound_id: str,
    *,
    variation_idx: int = 0,
    rng: np.random.Generator | None = None,
    stems_dir: Path | None = None,
) -> np.ndarray:
    """Layer a cached RTX 4060 neural stem onto dsp_wave using dual-band spectral fusion & attack preservation."""
    arch = SOUND_TO_ARCHETYPE.get(sound_id)
    if arch is None or len(dsp_wave) < 64:
        return dsp_wave
    stem = _load_cached_stem(arch.stem_id, stems_dir=stems_dir)
    if stem is None or len(stem) < 128:
        return dsp_wave

    if arch.seamless_loop:
        stem = _prepare_seamless_stem_body(stem, SAMPLE_RATE)

    if rng is None:
        rng = np.random.default_rng(_seed_for_stem(f"{sound_id}:v{variation_idx}"))

    n = len(dsp_wave)
    pitch_ratio = float(rng.uniform(0.90, 1.11)) if variation_idx > 0 else float(rng.uniform(0.97, 1.03))
    rot_frac = ((variation_idx * 0.29) + float(rng.uniform(0.03, 0.22))) % 0.85

    gpu_mixed = _gpu_blend_ai_stem(
        dsp_wave,
        arch.stem_id,
        stem,
        sample_rate=SAMPLE_RATE,
        band_lo_hz=arch.band_lo_hz,
        band_hi_hz=arch.band_hi_hz,
        pitch_ratio=pitch_ratio,
        rot_frac=rot_frac,
        attack_guard_ms=arch.attack_guard_ms,
        seamless_loop=arch.seamless_loop,
        mix_weight=arch.mix_weight,
    )
    if gpu_mixed is not None:
        return gpu_mixed

    start_idx = int(rot_frac * max(1, len(stem) - 64))
    rolled = np.roll(stem, -start_idx)

    loop_cf = 0
    if arch.seamless_loop and n >= int(1.2 * SAMPLE_RATE):
        cf_cand = min(n // 4, int(SAMPLE_RATE * 0.260))
        if cf_cand % 2 == 1:
            cf_cand -= 1
        if cf_cand >= 32:
            loop_cf = cf_cand
    n_resample = n + loop_cf

    src_needed = max(64, int(round(n_resample * pitch_ratio)))
    if src_needed > len(rolled):
        reps = int(np.ceil(src_needed / float(len(rolled))))
        tiled = np.tile(rolled, reps)[:src_needed]
    else:
        tiled = rolled[:src_needed]

    resampled = np.interp(
        np.linspace(0.0, 1.0, n_resample, endpoint=False),
        np.linspace(0.0, 1.0, len(tiled), endpoint=False),
        tiled,
    )
    if loop_cf >= 32:
        half = loop_cf // 2
        theta = np.linspace(0.0, 0.5 * np.pi, loop_cf, dtype=np.float64)
        bridge = resampled[n : n + loop_cf] * np.cos(theta) + resampled[:loop_cf] * np.sin(theta)
        resampled = np.concatenate([bridge[half:], resampled[loop_cf:n], bridge[:half]])

    dsp_in_src = dsp_wave
    res_in_src = resampled
    wrap = min(n // 4, 8192) if (arch.seamless_loop and n >= 4096) else 0
    # Try batched RTX CUDA FFT 3-band filtering, HPSS & envelope following; fall back to CPU SciPy cleanly
    if wrap > 0:
        res_in = np.concatenate([res_in_src[-wrap:], res_in_src, res_in_src[:wrap]])
        dsp_in = np.concatenate([dsp_in_src[-wrap:], dsp_in_src, dsp_in_src[:wrap]])
    else:
        res_in = res_in_src
        dsp_in = dsp_in_src

    shaped = _gpu_dualband_blend_shape(
        res_in,
        dsp_in,
        sample_rate=SAMPLE_RATE,
        band_lo_hz=arch.band_lo_hz,
        band_hi_hz=arch.band_hi_hz,
        apply_hpss=not arch.seamless_loop,
    )
    if shaped is None:
        if arch.seamless_loop:
            # Align spectral tilt on the exact periodic n-sample loop first so FFT leaves zero seam jump
            aligned_core = _cpu_align_stem_spectral_tilt(res_in_src, dsp_in_src, SAMPLE_RATE, strength=0.35)
            src_in = (
                np.concatenate([aligned_core[-wrap:], aligned_core, aligned_core[:wrap]])
                if wrap > 0
                else aligned_core
            )
        else:
            src_in = _cpu_hpss_harmonic_soft(res_in, SAMPLE_RATE, strength=0.60)
            src_in = _cpu_align_stem_spectral_tilt(src_in, dsp_in, SAMPLE_RATE, strength=0.45)
        s1 = min(240.0, max(arch.band_lo_hz * 1.45, arch.band_hi_hz * 0.12))
        s2 = min(1800.0, max(s1 * 2.2, arch.band_hi_hz * 0.42))
        tri = (s1 > arch.band_lo_hz + 25.0) and (s2 > s1 + 140.0) and (arch.band_hi_hz > s2 + 220.0)
        split_hz = min(420.0, max(arch.band_lo_hz * 1.4, arch.band_hi_hz * 0.38))
        if tri:
            b2_lo = max(arch.band_lo_hz, s1 * 0.85)
            b3_lo = max(b2_lo + 40.0, s2 * 0.85)
            stem_sub = _bandpass(src_in, arch.band_lo_hz, s1)
            stem_body = _bandpass(src_in, b2_lo, s2)
            stem_air = _bandpass(src_in, b3_lo, arch.band_hi_hz)
            dsp_sub = _bandpass(dsp_in, arch.band_lo_hz, s1)
            dsp_body = _bandpass(dsp_in, b2_lo, s2)
            dsp_air = _bandpass(dsp_in, b3_lo, arch.band_hi_hz)
            env_sub = 0.38 + 0.62 * _envelope_follower(dsp_sub, window_ms=55.0)
            env_body = 0.38 + 0.62 * _envelope_follower(dsp_body, window_ms=42.0)
            env_air = 0.38 + 0.62 * _envelope_follower(dsp_air, window_ms=30.0)
            shaped = (
                stem_sub * env_sub
                + stem_body * env_body
                + stem_air * env_air
            )
        elif split_hz > arch.band_lo_hz + 25.0 and arch.band_hi_hz > split_hz + 80.0:
            stem_lo = _bandpass(src_in, arch.band_lo_hz, split_hz)
            stem_hi = _bandpass(src_in, max(arch.band_lo_hz, split_hz * 0.85), arch.band_hi_hz)
            dsp_lo = _bandpass(dsp_in, arch.band_lo_hz, split_hz)
            dsp_hi = _bandpass(dsp_in, max(arch.band_lo_hz, split_hz * 0.85), arch.band_hi_hz)
            env_lo = 0.38 + 0.62 * _envelope_follower(dsp_lo, window_ms=48.0)
            env_hi = 0.38 + 0.62 * _envelope_follower(dsp_hi, window_ms=34.0)
            shaped = stem_lo * env_lo + stem_hi * env_hi
        else:
            filtered = _bandpass(src_in, arch.band_lo_hz, arch.band_hi_hz)
            shaped = filtered * (0.38 + 0.62 * _envelope_follower(dsp_in, window_ms=40.0))
    cf_cpu = 0
    if arch.seamless_loop and wrap >= int(SAMPLE_RATE * 0.060) + 64 and n > 4410:
        cf_cand = min(wrap - 32, n // 4, int(SAMPLE_RATE * 0.060))
        if cf_cand % 2 == 1:
            cf_cand -= 1
        if cf_cand >= 32:
            cf_cpu = cf_cand

    if wrap > 0:
        shaped = shaped[wrap : wrap + n + cf_cpu]

    # Preserve initial pure DSP attack transient (6-14 ms for one-shots) while keeping stem body natural
    if not arch.seamless_loop:
        guard_samples = min(n // 2, int(SAMPLE_RATE * arch.attack_guard_ms / 1000.0))
        if guard_samples > 8:
            shaped[:guard_samples] *= np.linspace(0.0, 1.0, guard_samples) ** 1.6

    dsp_match = np.concatenate([dsp_wave, dsp_wave[:cf_cpu]]) if cf_cpu > 0 else dsp_wave
    dsp_rms = float(np.sqrt(np.mean(dsp_wave * dsp_wave))) + 1e-8
    stem_core = shaped[:n]
    stem_rms = float(np.sqrt(np.mean(stem_core * stem_core)))
    if stem_rms < 1e-7:
        return dsp_wave

    matched_stem = shaped * (dsp_rms / stem_rms)
    w_stem = arch.mix_weight
    w_dsp = 1.0 - (w_stem * 0.78)
    out_mix = (dsp_match * w_dsp) + (matched_stem * w_stem)
    if cf_cpu > 0 and len(out_mix) == n + cf_cpu:
        from scripts.sound_studio.dsp_engine import fold_seamless_loop

        return fold_seamless_loop(out_mix, n, crossfade_ms=(cf_cpu * 1000.0) / float(SAMPLE_RATE))
    return out_mix[:n]
