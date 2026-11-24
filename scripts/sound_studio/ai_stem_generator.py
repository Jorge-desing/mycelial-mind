"""RTX 4060 GPU stem generator & multiband spectral hybrid mixer backed by Stable Audio Open Small.

Generates 52 curated acoustic archetype stems (steps=14, bfloat16 on CUDA)
covering all 181 Mycelial Mind sound events, cached as 44.1 kHz mono 16-bit PCM
`.wav` files in ``scripts/sound_studio/ai_stems/``.

Combines neural stems with the 5 specialized procedural engines via:
  - Pure DSP attack transient preservation during the initial 20–28 ms
  - Dual-band envelope-following modulation so 3-act timing and material signatures stay crisp
  - Circular equal-power crossfade for multi-second seamless biome ambient loops
  - Deterministic per-variation time rotation & pitch resampling for low correlation
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
from scipy import signal

try:
    from .cuda_dsp import (
        gpu_blend_ai_stem as _gpu_blend_ai_stem,
        gpu_dualband_blend_shape as _gpu_dualband_blend_shape,
        preload_ai_stems_gpu as _preload_ai_stems_gpu,
    )
except ImportError:
    try:
        from cuda_dsp import (
            gpu_blend_ai_stem as _gpu_blend_ai_stem,
            gpu_dualband_blend_shape as _gpu_dualband_blend_shape,
            preload_ai_stems_gpu as _preload_ai_stems_gpu,
        )
    except ImportError:
        from sound_studio.cuda_dsp import (
            gpu_blend_ai_stem as _gpu_blend_ai_stem,
            gpu_dualband_blend_shape as _gpu_dualband_blend_shape,
            preload_ai_stems_gpu as _preload_ai_stems_gpu,
        )

SAMPLE_RATE = 44100
AI_STEMS_DIR = Path(__file__).resolve().parent / "ai_stems"
STEM_MANIFEST_PATH = AI_STEMS_DIR / "stem_manifest.json"
INFERENCE_STEPS = 14
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
    # 14 Boss & Ritual Cinematics (covers all 17 BOSS_CINEMATIC_IDS)
    # =========================================================================
    StemArchetype(
        stem_id="boss_behemoth_roar",
        group="boss",
        prompt="Colossal tectonic kaiju monster roar, deep chest glottal growl, cavern earth rumble, dry cinematic foley",
        seconds_total=3.6,
        band_lo_hz=30.0,
        band_hi_hz=1850.0,
        mix_weight=0.35,
        sound_ids=("spore_behemoth_roar",),
    ),
    StemArchetype(
        stem_id="boss_behemoth_hurt",
        group="boss",
        prompt="Giant armored beast pained grunt, heavy chitin plate crunch, deep sub stagger impact, dry foley",
        seconds_total=2.4,
        band_lo_hz=34.0,
        band_hi_hz=2200.0,
        mix_weight=0.32,
        sound_ids=("spore_behemoth_hurt",),
    ),
    StemArchetype(
        stem_id="boss_behemoth_slam",
        group="boss",
        prompt="Colossal boulder fist ground slam explosion, cracking limestone crust shockwave, seismic footstep thud",
        seconds_total=3.2,
        band_lo_hz=28.0,
        band_hi_hz=2000.0,
        mix_weight=0.34,
        sound_ids=("spore_behemoth_slam", "mob_step_colossal"),
    ),
    StemArchetype(
        stem_id="boss_behemoth_death",
        group="boss",
        prompt="Towering monster dying bellow collapsing onto cavern stone, cascading rock debris avalanche, sub rumble",
        seconds_total=3.8,
        band_lo_hz=28.0,
        band_hi_hz=1950.0,
        mix_weight=0.34,
        sound_ids=("spore_behemoth_death",),
    ),
    StemArchetype(
        stem_id="boss_hive_core_resonance",
        group="boss",
        prompt="Alien psychic monolith Reaper brass horn braaam, deep harmonic metallic drone, ominous sci-fi SFX",
        seconds_total=3.6,
        band_lo_hz=40.0,
        band_hi_hz=4200.0,
        mix_weight=0.34,
        sound_ids=("hive_core_resonance",),
    ),
    StemArchetype(
        stem_id="boss_hive_core_hurt",
        group="boss",
        prompt="Resonant metallic crystal core cracking recoil, descending brass groan, electric arc spark overload",
        seconds_total=2.4,
        band_lo_hz=55.0,
        band_hi_hz=5400.0,
        mix_weight=0.32,
        sound_ids=("hive_core_hurt",),
    ),
    StemArchetype(
        stem_id="boss_hive_core_shield",
        group="boss",
        prompt="Prismatic geodesic energy barrier lock-on chime, ascending crystal resonance, high tension static repulsion",
        seconds_total=2.8,
        band_lo_hz=90.0,
        band_hi_hz=7200.0,
        mix_weight=0.30,
        sound_ids=("hive_core_shield",),
    ),
    StemArchetype(
        stem_id="boss_hive_core_death",
        group="boss",
        prompt="Catastrophic alien monolith meltdown siren, vacuum implosion, shattering obsidian prisms and dying horn",
        seconds_total=4.0,
        band_lo_hz=36.0,
        band_hi_hz=5800.0,
        mix_weight=0.34,
        sound_ids=("hive_core_death",),
    ),
    StemArchetype(
        stem_id="boss_matriarch_wail",
        group="boss",
        prompt="Haunting eldritch soprano banshee wail, dissonant ghostly female choir scream, dimensional portal shimmer",
        seconds_total=3.6,
        band_lo_hz=180.0,
        band_hi_hz=7800.0,
        mix_weight=0.35,
        sound_ids=("apex_matriarch_wail",),
    ),
    StemArchetype(
        stem_id="boss_matriarch_hurt",
        group="boss",
        prompt="Sharp obsidian crystal shriek snap, discordant phantom soprano gasp, brittle glass chitin crack",
        seconds_total=2.4,
        band_lo_hz=220.0,
        band_hi_hz=8200.0,
        mix_weight=0.32,
        sound_ids=("apex_matriarch_hurt",),
    ),
    StemArchetype(
        stem_id="boss_matriarch_barrage",
        group="boss",
        prompt="Rapid volley of dimensional plasma energy bolts crackling through air, crystalline whip-crack shriek",
        seconds_total=2.8,
        band_lo_hz=140.0,
        band_hi_hz=8600.0,
        mix_weight=0.32,
        sound_ids=("apex_matriarch_barrage",),
    ),
    StemArchetype(
        stem_id="boss_matriarch_death",
        group="boss",
        prompt="Anguished eldritch choir crescendo into gravitational void rupture, crystal veil shattering and abyss echo",
        seconds_total=4.0,
        band_lo_hz=55.0,
        band_hi_hz=7800.0,
        mix_weight=0.34,
        sound_ids=("apex_matriarch_death",),
    ),
    StemArchetype(
        stem_id="ritual_monolith_awaken",
        group="boss",
        prompt="Ancient carved stone temple slabs grinding open, deep bronze monolith bell toll, subterranean warhorn blast",
        seconds_total=3.8,
        band_lo_hz=34.0,
        band_hi_hz=2800.0,
        mix_weight=0.33,
        sound_ids=("hive_altar_awaken", "fungal_tide_horn"),
    ),
    StemArchetype(
        stem_id="ritual_dimensional_rift",
        group="boss",
        prompt="Urgent sci-fi power surge alarm, reverse vacuum suction cut, massive cinematic sub bass drop and choir shockwave",
        seconds_total=3.6,
        band_lo_hz=55.0,
        band_hi_hz=6800.0,
        mix_weight=0.33,
        sound_ids=("matriarch_altar_awaken", "boss_phase_shift"),
    ),
    # =========================================================================
    # 16 Entity & Footstep Stems (covers all 61 ENTITY_SYNTH_IDS)
    # =========================================================================
    StemArchetype(
        stem_id="sporeling_chitter_insect",
        group="entity",
        prompt="Tiny insectoid beetle chittering, rapid hollow chitin mandible clicks, small creature squeak",
        seconds_total=2.4,
        band_lo_hz=280.0,
        band_hi_hz=9200.0,
        mix_weight=0.25,
        sound_ids=("sporeling_chitter", "sporeling_hurt", "sporeling_death", "sporeling_burst"),
    ),
    StemArchetype(
        stem_id="chitin_brute_growl_heavy",
        group="entity",
        prompt="Heavy armored monster guttural growl, grinding insectoid carapace plates, heavy thud",
        seconds_total=2.8,
        band_lo_hz=36.0,
        band_hi_hz=2400.0,
        mix_weight=0.28,
        sound_ids=("chitin_brute_growl", "chitin_brute_hurt", "chitin_brute_death", "chitin_brute_smash"),
    ),
    StemArchetype(
        stem_id="stalker_predator_hiss",
        group="entity",
        prompt="Sinister predatory insectoid hiss, sharp scythe chitin blade swipe, stalking breath",
        seconds_total=2.6,
        band_lo_hz=180.0,
        band_hi_hz=8800.0,
        mix_weight=0.26,
        sound_ids=("stalker_hiss", "stalker_hurt", "stalker_death", "stalker_pounce"),
    ),
    StemArchetype(
        stem_id="acid_spitter_wet_gurgle",
        group="entity",
        prompt="Wet slimy organic bubbling acid sac gurgle, viscous mucus squelch, corrosive hiss",
        seconds_total=2.6,
        band_lo_hz=95.0,
        band_hi_hz=5400.0,
        mix_weight=0.26,
        sound_ids=("acid_spitter_gurgle", "acid_spitter_hurt", "acid_spitter_death", "acid_spitter_spit"),
    ),
    StemArchetype(
        stem_id="burrower_chitin_grind",
        group="entity",
        prompt="Scraping insect claws grinding against coarse basalt rock, crumbling earth gravel tunnel",
        seconds_total=2.8,
        band_lo_hz=65.0,
        band_hi_hz=6200.0,
        mix_weight=0.26,
        sound_ids=("burrower_grind", "burrower_hurt", "burrower_death", "burrower_tunnel"),
    ),
    StemArchetype(
        stem_id="pyro_shroom_ember_sizzle",
        group="entity",
        prompt="Smoldering glowing embers crackling, pressurized fungal gas hiss and dry thermal fuse",
        seconds_total=2.8,
        band_lo_hz=160.0,
        band_hi_hz=8600.0,
        mix_weight=0.26,
        sound_ids=("pyro_shroom_crackle", "pyro_shroom_hurt", "pyro_shroom_death", "pyro_shroom_ignite"),
    ),
    StemArchetype(
        stem_id="synapse_weaver_crystal_hum",
        group="entity",
        prompt="Floating telepathic crystal chime hum, subtle electric synapse buzz, eerie harmonic glass resonance",
        seconds_total=2.8,
        band_lo_hz=150.0,
        band_hi_hz=7600.0,
        mix_weight=0.25,
        sound_ids=("synapse_weaver_hum", "synapse_weaver_hurt", "synapse_weaver_death", "synapse_weaver_buff"),
    ),
    StemArchetype(
        stem_id="goliath_carrier_hollow_thud",
        group="entity",
        prompt="Massive hollow bloated sack thud, wet subterranean sloshing, heavy organic groan",
        seconds_total=3.0,
        band_lo_hz=32.0,
        band_hi_hz=2200.0,
        mix_weight=0.28,
        sound_ids=("goliath_carrier_thud", "goliath_carrier_hurt", "goliath_carrier_death", "goliath_carrier_spawn"),
    ),
    StemArchetype(
        stem_id="leech_parasite_shriek",
        group="entity",
        prompt="High-pitched frantic parasite screech, wet suction siphon slurp, brittle insect chitin",
        seconds_total=2.4,
        band_lo_hz=320.0,
        band_hi_hz=9600.0,
        mix_weight=0.24,
        sound_ids=("leech_parasite_screech", "leech_parasite_hurt", "leech_parasite_death", "leech_parasite_siphon"),
    ),
    StemArchetype(
        stem_id="colony_warden_deep_bellow",
        group="entity",
        prompt="Deep subterranean sentinel horn bellow, heavy chitin shield impact shockwave",
        seconds_total=3.0,
        band_lo_hz=34.0,
        band_hi_hz=2800.0,
        mix_weight=0.28,
        sound_ids=("colony_warden_bellow", "colony_warden_hurt", "colony_warden_death", "colony_warden_shockwave"),
    ),
    StemArchetype(
        stem_id="spore_architect_pulse",
        group="entity",
        prompt="Precision bio-organic pulse beacon, wet fleshy node construction snap",
        seconds_total=2.6,
        band_lo_hz=120.0,
        band_hi_hz=6800.0,
        mix_weight=0.25,
        sound_ids=("spore_architect_pulse", "spore_architect_hurt", "spore_architect_death", "spore_architect_construct"),
    ),
    StemArchetype(
        stem_id="synaptic_mimic_vocal",
        group="entity",
        prompt="Uncanny shifting double-voice alien echo, sinister whispered formant glide, sharp chitin strike",
        seconds_total=2.6,
        band_lo_hz=160.0,
        band_hi_hz=7400.0,
        mix_weight=0.26,
        sound_ids=("synaptic_mimic_echo", "synaptic_mimic_hurt", "synaptic_mimic_death", "synaptic_mimic_strike"),
    ),
    StemArchetype(
        stem_id="chimera_beast_growl",
        group="entity",
        prompt="Fierce agile biomechanical beast growl and roar, snapping muscle tendons, wild snarl",
        seconds_total=2.8,
        band_lo_hz=45.0,
        band_hi_hz=4400.0,
        mix_weight=0.28,
        sound_ids=("mycelial_chimera_growl", "mycelial_chimera_hurt", "mycelial_chimera_death", "mycelial_chimera_strike"),
    ),
    StemArchetype(
        stem_id="chimera_alpha_roar",
        group="entity",
        prompt="Apex agile monster combat war roar, reverberating cavern beast snarl, fierce bite",
        seconds_total=2.8,
        band_lo_hz=42.0,
        band_hi_hz=4600.0,
        mix_weight=0.30,
        sound_ids=("chimera_roar",),
    ),
    StemArchetype(
        stem_id="purifier_drone_servo_thruster",
        group="entity",
        prompt="Clean miniature futuristic quadcopter servo hum, crisp clinical electronic telemetry chirp, metallic impact",
        seconds_total=2.4,
        band_lo_hz=220.0,
        band_hi_hz=8800.0,
        mix_weight=0.14,
        attack_guard_ms=26.0,
        sound_ids=(
            "purifier_drone_hum",
            "purifier_drone_hurt",
            "purifier_drone_death",
            "purifier_drone_fire",
            "colony_symbiont_ambient",
            "colony_symbiont_work",
            "colony_symbiont_promote",
        ),
    ),
    StemArchetype(
        stem_id="anatomical_chitin_footsteps",
        group="entity",
        prompt="Dry insect tarsus claw footsteps tapping briskly on dark cave floor, crisp close foley",
        seconds_total=2.2,
        band_lo_hz=140.0,
        band_hi_hz=8200.0,
        mix_weight=0.12,
        attack_guard_ms=24.0,
        sound_ids=("mob_step_chitin", "mob_step_arachnid", "mob_step_heavy", "mob_step_hover"),
    ),
    # =========================================================================
    # 7 Physical Block Surface Families (covers all 35 BLOCK_PHYSICAL_SOUNDS)
    # =========================================================================
    StemArchetype(
        stem_id="surf_mycelium_wet_moss",
        group="surface",
        prompt="Damp spongy forest moss turf tearing softly, wet fibrous fungal soil squish, close foley",
        seconds_total=2.4,
        band_lo_hz=45.0,
        band_hi_hz=1650.0,
        mix_weight=0.13,
        attack_guard_ms=28.0,
        sound_ids=("block_mycelium_break", "block_mycelium_step", "block_mycelium_place", "block_mycelium_hit", "block_mycelium_fall"),
    ),
    StemArchetype(
        stem_id="surf_synaptic_quartz_chime",
        group="surface",
        prompt="Struck resonant quartz crystal slab, crisp glassy harmonic ping, brittle crystalline shatter",
        seconds_total=2.4,
        band_lo_hz=550.0,
        band_hi_hz=9200.0,
        mix_weight=0.12,
        attack_guard_ms=28.0,
        sound_ids=("block_synaptic_node_break", "block_synaptic_node_step", "block_synaptic_node_place", "block_synaptic_node_hit", "block_synaptic_node_fall"),
    ),
    StemArchetype(
        stem_id="surf_chitin_hollow_shell",
        group="surface",
        prompt="Dry hollow beetle exoskeleton shell snapping, brittle insect chitin cracking and rattling",
        seconds_total=2.4,
        band_lo_hz=520.0,
        band_hi_hz=8800.0,
        mix_weight=0.12,
        attack_guard_ms=28.0,
        sound_ids=("block_chitin_break", "block_chitin_step", "block_chitin_place", "block_chitin_hit", "block_chitin_fall"),
    ),
    StemArchetype(
        stem_id="surf_mycelial_stone_basalt",
        group="surface",
        prompt="Sharp chisel striking coarse dark volcanic basalt rock, gritty slate rubble gravel tumble",
        seconds_total=2.4,
        band_lo_hz=95.0,
        band_hi_hz=6800.0,
        mix_weight=0.11,
        attack_guard_ms=28.0,
        sound_ids=("block_mycelial_stone_break", "block_mycelial_stone_step", "block_mycelial_stone_place", "block_mycelial_stone_hit", "block_mycelial_stone_fall"),
    ),
    StemArchetype(
        stem_id="surf_purification_alloy_plate",
        group="surface",
        prompt="Precision stainless steel armor plate ring, crisp clean metallic clink and brushed alloy lock",
        seconds_total=2.4,
        band_lo_hz=680.0,
        band_hi_hz=9000.0,
        mix_weight=0.11,
        attack_guard_ms=28.0,
        sound_ids=("block_purification_tech_break", "block_purification_tech_step", "block_purification_tech_place", "block_purification_tech_hit", "block_purification_tech_fall"),
    ),
    StemArchetype(
        stem_id="surf_antibody_cyst_visceral",
        group="surface",
        prompt="Viscous organic fluid sac squelch, taut membrane rupturing wetly with sticky droplets",
        seconds_total=2.4,
        band_lo_hz=75.0,
        band_hi_hz=4200.0,
        mix_weight=0.14,
        attack_guard_ms=26.0,
        sound_ids=("block_antibody_cyst_break", "block_antibody_cyst_step", "block_antibody_cyst_place", "block_antibody_cyst_hit", "block_antibody_cyst_fall"),
    ),
    StemArchetype(
        stem_id="surf_hive_altar_monolith",
        group="surface",
        prompt="Massive subterranean carved stone monolith impact thud, low resonant granite temple toll",
        seconds_total=2.6,
        band_lo_hz=30.0,
        band_hi_hz=1350.0,
        mix_weight=0.14,
        attack_guard_ms=28.0,
        sound_ids=("block_hive_altar_break", "block_hive_altar_step", "block_hive_altar_place", "block_hive_altar_hit", "block_hive_altar_fall"),
    ),
    # =========================================================================
    # 4 Functional Blocks (covers remaining 12 BLOCK_FUNCTIONAL_SOUNDS)
    # =========================================================================
    StemArchetype(
        stem_id="func_spore_vent_bulb",
        group="block_func",
        prompt="Pressurized underground steam fungal geyser puff, wet organic spore pod pop, dry chitin crumble",
        seconds_total=2.6,
        band_lo_hz=110.0,
        band_hi_hz=6400.0,
        mix_weight=0.18,
        attack_guard_ms=24.0,
        sound_ids=("spore_vent_puff", "spore_bulb_burst", "ephemeral_chitin_crumble"),
    ),
    StemArchetype(
        stem_id="func_purifier_turret_pylon",
        group="block_func",
        prompt="Clinical futuristic energy turret shot, ascending power lock chime, pneumatic mechanical craft latch",
        seconds_total=2.6,
        band_lo_hz=260.0,
        band_hi_hz=9200.0,
        mix_weight=0.15,
        attack_guard_ms=26.0,
        sound_ids=("purification_pylon_pulse", "purification_turret_fire", "purification_turret_upgrade", "purification_turret_repair", "synaptic_bench_synthesize"),
    ),
    StemArchetype(
        stem_id="func_synaptic_symbiotic_node",
        group="block_func",
        prompt="Crystalline energy node awakening chime, warm shimmering harmonic chord, whooshing portal teleport",
        seconds_total=2.8,
        band_lo_hz=120.0,
        band_hi_hz=7600.0,
        mix_weight=0.22,
        attack_guard_ms=24.0,
        sound_ids=(
            "synaptic_node_mature",
            "symbiotic_node_charge",
            "symbiotic_node_teleport",
            "sovereign_nexus_pulse",
            "colony_dialect_tone_1",
            "colony_dialect_tone_2",
            "colony_dialect_tone_3",
        ),
    ),
    StemArchetype(
        stem_id="func_mycelial_rift_portal",
        group="block_func",
        prompt="Dark subterranean portal rift drone, low throbbing subspace energy hum with cosmic static whisper",
        seconds_total=3.2,
        band_lo_hz=38.0,
        band_hi_hz=3400.0,
        mix_weight=0.28,
        sound_ids=("mycelial_rift_hum",),
    ),
    # =========================================================================
    # 5 Item, Weapon, Relic & Projectile Stems (covers all 26 ITEM_PROJECTILE_SOUNDS)
    # =========================================================================
    StemArchetype(
        stem_id="item_purifier_scanner_tech",
        group="item",
        prompt="Crisp medical bio-scanner radar ping, high-tech thermal pulse sweep, clean spray fizz and electronic deploy",
        seconds_total=2.4,
        band_lo_hz=280.0,
        band_hi_hz=9600.0,
        mix_weight=0.14,
        attack_guard_ms=26.0,
        sound_ids=("bio_scanner_ping", "bio_scanner_thermal_pulse", "spore_antidote_cure", "neural_decoy_deploy", "purifier_drone_deploy", "command_staff_mark"),
    ),
    StemArchetype(
        stem_id="item_symbiotic_melee_blades",
        group="item",
        prompt="Razor-sharp organic blade whoosh slash, interlocking chitin plate armor clasp, resonant metallic harmonic",
        seconds_total=2.4,
        band_lo_hz=140.0,
        band_hi_hz=8800.0,
        mix_weight=0.20,
        attack_guard_ms=22.0,
        sound_ids=("symbiotic_blade_slash", "enzyme_lance_thrust", "chitin_armor_equip", "chitin_armor_evolve"),
    ),
    StemArchetype(
        stem_id="item_ballistic_launcher_grenade",
        group="item",
        prompt="Pneumatic launcher thump, crisp clean purifier shockwave grenade blast, acid splash sizzle",
        seconds_total=2.6,
        band_lo_hz=55.0,
        band_hi_hz=8400.0,
        mix_weight=0.20,
        attack_guard_ms=22.0,
        sound_ids=("spore_launcher_fire", "purifier_grenade_throw", "purifier_grenade_detonate", "acid_spit_impact", "neutralizing_spore_impact"),
    ),
    StemArchetype(
        stem_id="item_visceral_mutagen_serum",
        group="item",
        prompt="Organic glass syringe injection, pressurized bio-fluid squelch, pheromone mist spray, egg shell crack",
        seconds_total=2.6,
        band_lo_hz=95.0,
        band_hi_hz=6400.0,
        mix_weight=0.22,
        attack_guard_ms=22.0,
        sound_ids=("bio_extractor_draw", "counter_serum_inject", "symbiotic_mutagen_implant", "truce_pheromone_spray", "chimera_ovum_hatch"),
    ),
    StemArchetype(
        stem_id="item_eldritch_relic_catalyst",
        group="item",
        prompt="Arcane eldritch relic surge, dimensional rift tearing open with sharp crystalline chime and sub drop",
        seconds_total=3.0,
        band_lo_hz=60.0,
        band_hi_hz=7800.0,
        mix_weight=0.26,
        attack_guard_ms=20.0,
        sound_ids=("synaptic_graft_splice", "volatile_engram_pulse", "schism_injector_sting", "quorum_seal_pact", "tide_beacon_activate", "rift_catalyst_ignite", "overmind_crown_surge"),
    ),
    # =========================================================================
    # 6 System, Homeostasis & 3-Biome Soundscape Stems (covers remaining 30 SYSTEM_ENV_IDS)
    # =========================================================================
    StemArchetype(
        stem_id="env_biome_caverns_abyss",
        group="system_env",
        prompt="Deep subterranean cave wind ambience, slow water spore drips in darkness, distant rock creak",
        seconds_total=6.2,
        band_lo_hz=34.0,
        band_hi_hz=3200.0,
        mix_weight=0.34,
        attack_guard_ms=35.0,
        seamless_loop=True,
        sound_ids=("biome_caverns_loop", "biome_caverns_mood", "biome_caverns_additions", "fungal_tide_ambience"),
    ),
    StemArchetype(
        stem_id="env_biome_sanctum_arterial",
        group="system_env",
        prompt="Dark organic cathedral ambience, slow subterranean biological heartbeat throb, eerie neural shimmer",
        seconds_total=6.2,
        band_lo_hz=36.0,
        band_hi_hz=4400.0,
        mix_weight=0.34,
        attack_guard_ms=35.0,
        seamless_loop=True,
        sound_ids=("biome_sanctum_loop", "biome_sanctum_mood", "biome_sanctum_additions", "homeostasis_heartbeat"),
    ),
    StemArchetype(
        stem_id="env_biome_forest_canopy",
        group="system_env",
        prompt="Bioluminescent mushroom forest night wind rustling giant caps, mystical shimmering spores floating",
        seconds_total=6.2,
        band_lo_hz=60.0,
        band_hi_hz=5800.0,
        mix_weight=0.34,
        attack_guard_ms=35.0,
        seamless_loop=True,
        sound_ids=("biome_forest_loop", "biome_forest_mood", "biome_forest_additions", "mycorrhizal_tremor"),
    ),
    StemArchetype(
        stem_id="sys_hive_evolution_surge",
        group="system_env",
        prompt="Dark ascending alien hive evolution fanfare, pulsing synaptic power surge and crystalline chord swell",
        seconds_total=3.2,
        band_lo_hz=55.0,
        band_hi_hz=6800.0,
        mix_weight=0.28,
        attack_guard_ms=22.0,
        sound_ids=("hive_mutate_tier2", "synaptic_pulse", "immune_surge", "mob_promote_mutated", "mob_promote_elite", "hive_trait_adapt", "hive_synergy_unlock", "hive_threat_milestone"),
    ),
    StemArchetype(
        stem_id="sys_visceral_assimilation",
        group="system_env",
        prompt="Corrosive acid rain sizzle on damp stone, rapid organic tissue burrowing and sinister creature feeding",
        seconds_total=2.6,
        band_lo_hz=85.0,
        band_hi_hz=7600.0,
        mix_weight=0.24,
        attack_guard_ms=22.0,
        sound_ids=("mob_dawn_retreat", "infected_mob_convert", "antibody_cyst_encapsulate", "acid_rain_sizzle", "necrophagy_feast", "swarm_flank_call"),
    ),
    StemArchetype(
        stem_id="sys_purifier_emp_suppression",
        group="system_env",
        prompt="Heavy futuristic electromagnetic pulse capacitor discharge, servo stall fizz, clinical containment hum",
        seconds_total=2.8,
        band_lo_hz=120.0,
        band_hi_hz=8800.0,
        mix_weight=0.16,
        attack_guard_ms=26.0,
        seamless_loop=True,
        sound_ids=("suppressor_discharge", "squad_desync", "corrosive_jam", "purification_pylon_hum"),
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
    peak_after = float(np.max(np.abs(w)))
    if peak_after > 1e-7:
        w *= 0.90 / peak_after
    fade = min(len(w) // 8, int(0.025 * SAMPLE_RATE))
    if fade > 4:
        w[:fade] *= np.linspace(0.0, 1.0, fade)
        w[-fade:] *= np.linspace(1.0, 0.0, fade)
    return np.clip(w, -0.95, 0.95)


def ensure_ai_stems(stems_dir: Path | None = None, *, force_regen: bool = False) -> dict[str, Path]:
    """Ensure all 52 RTX 4060 Stable Audio Open Small stems exist locally, generating missing ones via CUDA."""
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
        changed = manifest_data.get("mapped_sound_events") != len(SOUND_TO_ARCHETYPE)
        stems_meta = manifest_data.get("stems", {})
        for arch in AI_STEM_ARCHETYPES:
            entry = stems_meta.get(arch.stem_id)
            desired_ids = list(arch.sound_ids)
            if isinstance(entry, dict) and entry.get("sound_ids") != desired_ids:
                entry["sound_ids"] = desired_ids
                changed = True
        if changed:
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
        entry = {
            "group": arch.group,
            "prompt": arch.prompt,
            "seed": batch_seed,
            "batch_index": idx_b,
            "seconds_total": arch.seconds_total,
            "duration_secs": round(len(clean) / float(SAMPLE_RATE), 3),
            "rms_dbfs": round(float(20.0 * np.log10(max(float(np.sqrt(np.mean(clean * clean))), 1e-6))), 2),
            "peak_dbfs": round(float(20.0 * np.log10(max(float(np.max(np.abs(clean))), 1e-6))), 2),
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
            final_entries[arch.stem_id] = stem_entries[arch.stem_id]
        elif wav_path.is_file():
            clean = _read_wav_mono(wav_path)
            final_entries[arch.stem_id] = {
                "group": arch.group,
                "prompt": arch.prompt,
                "seed": _seed_for_stem(arch.stem_id),
                "seconds_total": arch.seconds_total,
                "duration_secs": round(len(clean) / float(SAMPLE_RATE), 3),
                "rms_dbfs": round(float(20.0 * np.log10(max(float(np.sqrt(np.mean(clean * clean))), 1e-6))), 2),
                "peak_dbfs": round(float(20.0 * np.log10(max(float(np.max(np.abs(clean))), 1e-6))), 2),
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


def _circular_loop_crossfade(wave: np.ndarray, crossfade_ms: float = 120.0) -> np.ndarray:
    """Apply an equal-power circular crossfade so multi-second ambient loops wrap without seams."""
    n = len(wave)
    cf = min(n // 5, int(SAMPLE_RATE * crossfade_ms / 1000.0))
    if cf < 32:
        return wave
    out = wave.copy()
    theta = np.linspace(0.0, 0.5 * np.pi, cf, dtype=np.float64)
    fade_in = np.sin(theta)
    fade_out = np.cos(theta)
    head = out[:cf].copy()
    tail = out[-cf:].copy()
    out[:cf] = head * fade_in + tail * fade_out
    out[-cf:] = tail * fade_out + head * fade_in
    return out


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

    src_needed = max(64, int(round(n * pitch_ratio)))
    if src_needed > len(rolled):
        reps = int(np.ceil(src_needed / float(len(rolled))))
        tiled = np.tile(rolled, reps)[:src_needed]
    else:
        tiled = rolled[:src_needed]

    resampled = np.interp(
        np.linspace(0.0, 1.0, n, endpoint=False),
        np.linspace(0.0, 1.0, len(tiled), endpoint=False),
        tiled,
    )
    if arch.seamless_loop and n >= int(2.2 * SAMPLE_RATE):
        resampled = _circular_loop_crossfade(resampled, crossfade_ms=120.0)

    # Try batched RTX CUDA FFT dual-band filtering & envelope following; fall back to CPU SciPy cleanly
    shaped = _gpu_dualband_blend_shape(
        resampled,
        dsp_wave,
        sample_rate=SAMPLE_RATE,
        band_lo_hz=arch.band_lo_hz,
        band_hi_hz=arch.band_hi_hz,
    )
    if shaped is None:
        # Dual-band spectral split + dual envelope following so body weight & upper shimmer never smear each other
        split_hz = min(420.0, max(arch.band_lo_hz * 1.4, arch.band_hi_hz * 0.38))
        if split_hz > arch.band_lo_hz + 25.0 and arch.band_hi_hz > split_hz + 80.0:
            stem_lo = _bandpass(resampled, arch.band_lo_hz, split_hz)
            stem_hi = _bandpass(resampled, max(arch.band_lo_hz, split_hz * 0.85), arch.band_hi_hz)
            dsp_lo = _bandpass(dsp_wave, arch.band_lo_hz, split_hz)
            dsp_hi = _bandpass(dsp_wave, max(arch.band_lo_hz, split_hz * 0.85), arch.band_hi_hz)
            env_lo = _envelope_follower(dsp_lo, window_ms=22.0)
            env_hi = _envelope_follower(dsp_hi, window_ms=14.0)
            shaped = stem_lo * env_lo + stem_hi * env_hi
        else:
            filtered = _bandpass(resampled, arch.band_lo_hz, arch.band_hi_hz)
            shaped = filtered * _envelope_follower(dsp_wave, window_ms=18.0)

    # Preserve initial pure DSP attack transient (20-28 ms) for crisp physical/material identity
    guard_samples = min(n // 2, int(SAMPLE_RATE * arch.attack_guard_ms / 1000.0))
    if guard_samples > 8:
        shaped[:guard_samples] *= np.linspace(0.0, 1.0, guard_samples) ** 1.6

    dsp_rms = float(np.sqrt(np.mean(dsp_wave * dsp_wave))) + 1e-8
    stem_rms = float(np.sqrt(np.mean(shaped * shaped)))
    if stem_rms < 1e-7:
        return dsp_wave

    matched_stem = shaped * (dsp_rms / stem_rms)
    w_stem = arch.mix_weight
    w_dsp = 1.0 - (w_stem * 0.55)
    return (dsp_wave * w_dsp) + (matched_stem * w_stem)
