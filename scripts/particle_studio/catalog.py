"""Declarative catalog of every Mycelial Mind particle sprite.

Each entry is the single source of truth for the particle studio: sprite size,
frame count, silhouette recipe, palette, render mode and motion archetype.
``ModParticles.java`` must expose exactly the ids listed here (the studio
validates both directions), and ``particles/<id>.json`` must list the emitted
frames in order.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, Tuple


@dataclass(frozen=True)
class ParticleSpec:
    id: str
    family: str
    size: int
    frames: int
    shape: str
    palette: str
    accent: str
    render: str  # "additive" | "translucent"
    archetype: str
    description_es: str
    description_en: str
    params: Dict[str, float] = field(default_factory=dict)


def _p(**kwargs: float) -> Dict[str, float]:
    return kwargs


SPECS: Tuple[ParticleSpec, ...] = (
    # ------------------------------------------------------------------
    # Legacy ids (kept for ModEffects / Codex / save compatibility).
    # ------------------------------------------------------------------
    ParticleSpec(
        "infection", "estado", 32, 3, "spore", "mycelial_violet", "elite_crimson",
        "translucent", "drift",
        "Racimo de 3 lóbulos con cilios, núcleo magenta y halo violeta.",
        "Three-lobed spore cluster with cilia, magenta nucleus and violet halo.",
        _p(lobes=3, cilia=5, nucleus=0.30, seed=11, squash=0.86),
    ),
    ParticleSpec(
        "rot", "estado", 32, 3, "teardrop", "necrotic_umber", "spore_chartreuse",
        "translucent", "fall",
        "Gota necrótica umber con fisuras chartreuse y goteo carmesí.",
        "Umber necrotic droplet with chartreuse fissures and crimson drip.",
        _p(seed=23, fissures=3, tail=0.66),
    ),
    ParticleSpec(
        "insight", "estado", 32, 3, "crystal", "venom_emerald", "warm_ivory",
        "additive", "rise",
        "Cristal esmeralda de 4 puntas con vena rúnica y catch especular.",
        "Four-point emerald crystal with rune vein and specular catch.",
        _p(points=4, rune=1, seed=31),
    ),
    ParticleSpec(
        "vision", "estado", 32, 3, "reticle", "biolum_cyan", "mutant_amber",
        "additive", "orbit",
        "Iris térmico cian con 4 muescas de cuadrante y pupila ámbar.",
        "Cyan thermal iris with four quadrant notches and amber pupil.",
        _p(ticks=4, seed=41),
    ),
    ParticleSpec(
        "recovery", "consumible", 32, 3, "arc", "spore_chartreuse", "warm_ivory",
        "additive", "arc",
        "Esquirla de arco verde-lima con núcleo blanco y mota satélite.",
        "Lime-green arc shard with white core and satellite mote.",
        _p(seed=53, satellites=2),
    ),
    ParticleSpec(
        "purge", "consumible", 32, 3, "cross", "purifier_glow", "spore_chartreuse",
        "additive", "burst",
        "Fragmento de cruz clínica cian con núcleo verde y satélites.",
        "Clinical cyan cross fragment with green core and satellites.",
        _p(seed=61),
    ),
    ParticleSpec(
        "scan", "consumible", 32, 3, "chevron", "biolum_cyan", "purifier_glow",
        "additive", "burst",
        "Chevrón de barrido azul con cola de datos y doble punta.",
        "Blue scan chevron with data tail and twin tip.",
        _p(seed=71, tails=3),
    ),
    ParticleSpec(
        "serum_melee", "suero", 32, 3, "slash", "leech_crimson", "warm_ivory",
        "additive", "burst",
        "Tajo carmesí con borde blanco y virutas.",
        "Crimson slash with white edge and chips.",
        _p(seed=83),
    ),
    ParticleSpec(
        "serum_projectile", "suero", 32, 3, "dart", "mutant_amber", "warm_ivory",
        "additive", "burst",
        "Dardo ámbar con aletas traseras y punta brillante.",
        "Amber dart with rear fins and bright tip.",
        _p(seed=89),
    ),
    ParticleSpec(
        "serum_fire", "suero", 32, 3, "ember", "ember_orange", "basalt_slate",
        "additive", "burst",
        "Ascua naranja con costra oscura y chispas.",
        "Orange ember with dark crust and sparks.",
        _p(seed=97),
    ),
    ParticleSpec(
        "serum_explosion", "suero", 32, 3, "wave_crystal", "purifier_glow", "spectral_bone",
        "additive", "burst",
        "Cristal de onda azul con fractura radial.",
        "Blue wave crystal with radial fracture.",
        _p(seed=101),
    ),
    ParticleSpec(
        "serum_magic", "suero", 32, 3, "rune", "predator_amethyst", "royal_gold",
        "additive", "burst",
        "Glifo violeta flotante con anillo rúnico.",
        "Floating violet glyph with rune ring.",
        _p(seed=103, points=6),
    ),
    ParticleSpec(
        "engram", "hive", 32, 4, "hex_glyph", "royal_gold", "mutant_amber",
        "additive", "glyph",
        "Hexágono ámbar con circuito interno y 3 pines.",
        "Amber hexagon with inner circuit and three pins.",
        _p(seed=107, pins=3),
    ),
    ParticleSpec(
        "impact", "proyectil", 32, 4, "star", "purifier_glow", "warm_ivory",
        "additive", "ring",
        "Estrella menta de 8 púas con núcleo blanco.",
        "Mint eight-point impact star with white core.",
        _p(seed=109, points=8),
    ),
    ParticleSpec(
        "trail", "proyectil", 32, 3, "wisp", "purifier_glow", "spore_chartreuse",
        "translucent", "drift",
        "Espiral de esporas menta con fizz y gota.",
        "Mint spore spiral with fizz and droplet.",
        _p(seed=113),
    ),
    # ------------------------------------------------------------------
    # New families (organic hive, combat, neural, bosses, purification).
    # ------------------------------------------------------------------
    ParticleSpec(
        "spore_mote", "hive", 32, 3, "spore", "spore_chartreuse", "warm_ivory",
        "translucent", "drift",
        "Espora lobulada con cilios y núcleo bioluminiscente.",
        "Lobed spore with cilia and bioluminescent nucleus.",
        _p(lobes=2, cilia=3, nucleus=0.22, seed=127, squash=1.0),
    ),
    ParticleSpec(
        "spore_cloud", "hive", 64, 4, "mist", "mycelial_violet", "chitin_bone",
        "translucent", "mist",
        "Nube FBM con filamentos deshilachados.",
        "FBM cloud with frayed filaments.",
        _p(seed=131, density=0.55, strands=3, sx=1.10, sy=0.85),
    ),
    ParticleSpec(
        "spore_fall", "mundo", 32, 3, "spore", "spore_chartreuse", "biolum_cyan",
        "translucent", "fall",
        "Espora con aleta y estela corta.",
        "Spore with fin and short wake.",
        _p(lobes=2, cilia=2, nucleus=0.18, seed=137, squash=0.70, fin=1, wake=1),
    ),
    ParticleSpec(
        "bio_lume", "mundo", 32, 3, "spore", "spore_chartreuse", "warm_ivory",
        "additive", "drift",
        "Mota bioluminiscente chartreuse con anillo y destello estelar.",
        "Chartreuse bioluminescent mote with ring and star glint.",
        _p(lobes=1, cilia=0, nucleus=0.45, seed=139, star=1),
    ),
    ParticleSpec(
        "amber_glint", "mundo", 32, 3, "crystal", "mutant_amber", "warm_ivory",
        "additive", "spark",
        "Gema ámbar facetada con destello en cruz.",
        "Faceted amber gem with cross glint.",
        _p(points=6, rune=0, seed=149),
    ),
    ParticleSpec(
        "chitin_shard", "combate", 32, 3, "crystal", "chitin_bone", "basalt_slate",
        "translucent", "tumble",
        "Esquirla beige angular con bisel y fractura.",
        "Angular beige shard with bevel and fracture.",
        _p(points=3, rune=0, seed=151, fracture=1),
    ),
    ParticleSpec(
        "acid_droplet", "combate", 32, 3, "teardrop", "acid_lime", "purifier_glow",
        "translucent", "arc",
        "Gota lima con cáustica interna y borde brillante.",
        "Lime droplet with internal caustic and bright rim.",
        _p(seed=157, fissures=0, tail=0.30, beads=2),
    ),
    ParticleSpec(
        "acid_splash", "combate", 32, 4, "splash", "acid_lime", "purifier_glow",
        "translucent", "burst",
        "Salpicadura radial de 8 gotas con anillo de vapor.",
        "Radial splash of eight droplets with vapour ring.",
        _p(seed=163, drops=8),
    ),
    ParticleSpec(
        "necrotic_mist", "combate", 32, 4, "mist", "necrotic_umber", "leech_crimson",
        "translucent", "mist",
        "Bruma umber/carmesí con filamentos necróticos.",
        "Umber/crimson mist with necrotic filaments.",
        _p(seed=167, density=0.60, strands=4, sx=0.85, sy=1.15),
    ),
    ParticleSpec(
        "synapse_spark", "hive", 32, 4, "spark", "biolum_cyan", "spectral_bone",
        "additive", "spark",
        "Chispa de 4 puntas con tendril bifurcado.",
        "Four-point spark with forked tendril.",
        _p(seed=173, fork=1),
    ),
    ParticleSpec(
        "hive_pulse", "hive", 64, 5, "ring", "mycelial_violet", "royal_gold",
        "additive", "ring",
        "Anillo rúnico con 8 muescas y glifos.",
        "Rune ring with eight notches and glyphs.",
        _p(seed=179, notches=8, glyphs=1),
    ),
    ParticleSpec(
        "psychic_wave", "hive", 64, 4, "ring", "predator_amethyst", "spectral_bone",
        "additive", "ring",
        "Ondas concéntricas distorsionadas con runas.",
        "Distorted concentric waves with runes.",
        _p(seed=181, notches=0, glyphs=2, wobble=1, rings=3, spacing=0.28, wobble_amp=0.075),
    ),
    ParticleSpec(
        "tide_mote", "jefe", 32, 4, "spore", "tide_violet", "spectral_bone",
        "additive", "vortex",
        "Espora violeta con halo giratorio y satélites.",
        "Violet spore with spinning halo and satellites.",
        _p(lobes=2, cilia=3, nucleus=0.30, seed=191, halo=1),
    ),
    ParticleSpec(
        "hive_ember", "jefe", 32, 3, "ember", "ember_orange", "mycelial_violet",
        "additive", "rise",
        "Brasa con humo fúngico y núcleo naranja.",
        "Ember with fungal smoke and orange core.",
        _p(seed=193),
    ),
    ParticleSpec(
        "purifier_cross", "purificacion", 32, 4, "cross", "purifier_glow", "warm_ivory",
        "additive", "ring",
        "Cruz de luz clínica con hexágono y 4 destellos.",
        "Clinical light cross with hexagon and four glints.",
        _p(seed=197, hexagon=1),
    ),
    ParticleSpec(
        "thermal_reticle", "purificacion", 64, 4, "reticle", "purifier_glow", "mutant_amber",
        "additive", "ring",
        "Retícula con anillo, ticks y cruz.",
        "Reticle with ring, ticks and cross.",
        _p(ticks=8, seed=199, cross=1),
    ),
    ParticleSpec(
        "histamine_pulse", "homeostasis", 32, 4, "spike_pulse", "histamine_rose", "elite_crimson",
        "translucent", "burst",
        "Estallido de espinas rosa con anillo de histamina.",
        "Pink spine burst with histamine ring.",
        _p(seed=211, spikes=12),
    ),
    ParticleSpec(
        "hive_dust", "hive", 32, 3, "dust", "warm_ivory", "spectral_bone",
        "additive", "drift",
        "Polvo marfil neutro con núcleo blanco (tintable por cepa).",
        "Neutral ivory dust with white core (strain-tinted).",
        _p(seed=223),
    ),
    ParticleSpec(
        "desync_fragment", "hive", 32, 4, "rune", "predator_amethyst", "biolum_cyan",
        "additive", "spark",
        "Fragmentos de píxel glitch púrpura/cian con desplazamiento.",
        "Purple/cyan glitch pixel fragments with drift.",
        _p(seed=227, points=5, glitch=1),
    ),
)

SPECS_BY_ID: Dict[str, ParticleSpec] = {s.id: s for s in SPECS}

FAMILY_ORDER = ("estado", "consumible", "suero", "proyectil", "hive", "combate", "jefe", "purificacion", "homeostasis", "mundo")

__all__ = ["ParticleSpec", "SPECS", "SPECS_BY_ID", "FAMILY_ORDER"]
