"""Complete 167-event sound catalog for Mycelial Mind's dark bio-horror & clinical tech studio."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class SoundSpec:
    sound_id: str
    java_const: str
    category: str
    recipe: str
    duration: float
    base_freq: float
    mod_freq: float
    variant: str
    species: str
    loudness_group: str
    variations: int
    attenuation_distance: int
    seed_offset: int
    volume: float
    pitch: float
    en_subtitle: str
    es_subtitle: str


def _s(
    sound_id: str,
    category: str,
    recipe: str,
    duration: float,
    base_freq: float,
    mod_freq: float,
    en_subtitle: str,
    es_subtitle: str,
    *,
    java_const: str | None = None,
    variant: str = "default",
    species: str = "default",
    loudness_group: str = "combat",
    variations: int = 2,
    attenuation_distance: int = 16,
    seed_offset: int = 0,
    volume: float = 0.9,
    pitch: float = 1.0,
) -> SoundSpec:
    return SoundSpec(
        sound_id=sound_id,
        java_const=java_const or sound_id.upper(),
        category=category,
        recipe=recipe,
        duration=duration,
        base_freq=base_freq,
        mod_freq=mod_freq,
        variant=variant,
        species=species,
        loudness_group=loudness_group,
        variations=variations,
        attenuation_distance=attenuation_distance,
        seed_offset=seed_offset,
        volume=volume,
        pitch=pitch,
        en_subtitle=en_subtitle,
        es_subtitle=es_subtitle,
    )


def _surface_family(
    prefix: str,
    variant: str,
    base_freq: float,
    en_label: str,
    es_label: str,
    seed_base: int,
) -> list[SoundSpec]:
    actions = [
        ("break", 0.46, 0.92, 0.95, "block_heavy", 16, f"{en_label} broken", f"{es_label} roto"),
        ("step", 0.22, 1.08, 0.68, "footstep", 12, f"Footsteps on {en_label.lower()}", f"Pasos sobre {es_label.lower()}"),
        ("place", 0.32, 0.88, 0.88, "block_light", 16, f"{en_label} placed", f"{es_label} colocado"),
        ("hit", 0.18, 1.04, 0.74, "block_light", 12, f"Striking {en_label.lower()}", f"Golpeando {es_label.lower()}"),
        ("fall", 0.44, 0.78, 0.86, "block_heavy", 16, f"Landed on {en_label.lower()}", f"Caída sobre {es_label.lower()}"),
    ]
    out: list[SoundSpec] = []
    for idx, (act, dur, freq_mult, vol, lgroup, atten, en_sub, es_sub) in enumerate(actions):
        out.append(
            _s(
                f"{prefix}_{act}",
                "block_physical",
                f"surface_{act}",
                dur,
                base_freq * freq_mult,
                7.2 + idx * 1.9,
                en_sub,
                es_sub,
                variant=variant,
                species=variant,
                loudness_group=lgroup,
                variations=4,
                attenuation_distance=atten,
                seed_offset=seed_base + idx,
                volume=vol,
            )
        )
    return out


ENTITY_SOUNDS: list[SoundSpec] = [
    # 1. Sporeling (insectoid_chitin)
    _s("sporeling_chitter", "entity", "chitter", 0.50, 680.0, 14.0, "Sporeling chitters", "Espórulo chilla", species="insectoid_chitin", loudness_group="ambient", variations=3, attenuation_distance=16, seed_offset=101, volume=0.85),
    _s("sporeling_hurt", "entity", "hurt", 0.28, 610.0, 17.0, "Sporeling hurts", "Espórulo herido", species="insectoid_chitin", loudness_group="combat", variations=3, attenuation_distance=16, seed_offset=102, volume=0.88),
    _s("sporeling_death", "entity", "death", 0.58, 540.0, 11.0, "Sporeling dies", "Espórulo muere", species="insectoid_chitin", loudness_group="death", variations=2, attenuation_distance=18, seed_offset=103, volume=0.90),
    _s("sporeling_burst", "entity", "projectile_pop", 0.42, 410.0, 14.0, "Sporeling releases spore pulse", "Espórulo libera pulso de esporas", species="insectoid_chitin", loudness_group="combat", variations=2, attenuation_distance=20, seed_offset=104, volume=0.92),

    # 2. Chitin Brute (colossal_organic)
    _s("chitin_brute_growl", "entity", "growl", 0.88, 78.0, 5.2, "Chitin Brute growls", "Bruto de Quitina gruñe", species="colossal_organic", loudness_group="ambient", variations=3, attenuation_distance=24, seed_offset=111, volume=0.94),
    _s("chitin_brute_hurt", "entity", "hurt", 0.38, 115.0, 8.5, "Chitin Brute hurts", "Bruto de Quitina herido", species="colossal_organic", loudness_group="combat", variations=3, attenuation_distance=22, seed_offset=112, volume=0.92),
    _s("chitin_brute_death", "entity", "death", 0.92, 92.0, 5.5, "Chitin Brute dies", "Bruto de Quitina muere", species="colossal_organic", loudness_group="death", variations=2, attenuation_distance=24, seed_offset=113, volume=0.95),
    _s("chitin_brute_smash", "entity", "heavy_thud", 0.56, 68.0, 6.5, "Chitin Brute slams", "Bruto de Quitina embiste", species="colossal_organic", loudness_group="combat", variations=2, attenuation_distance=26, seed_offset=114, volume=0.96),

    # 3. Mycelial Stalker (insectoid_chitin)
    _s("stalker_hiss", "entity", "hiss", 0.64, 520.0, 9.0, "Mycelial Stalker hisses", "Acechador Micelial sisea", species="insectoid_chitin", loudness_group="ambient", variations=3, attenuation_distance=20, seed_offset=121, volume=0.88),
    _s("stalker_hurt", "entity", "hurt", 0.32, 460.0, 14.0, "Mycelial Stalker hurts", "Acechador Micelial herido", species="insectoid_chitin", loudness_group="combat", variations=3, attenuation_distance=18, seed_offset=122, volume=0.88),
    _s("stalker_death", "entity", "death", 0.70, 380.0, 9.5, "Mycelial Stalker dies", "Acechador Micelial muere", species="insectoid_chitin", loudness_group="death", variations=2, attenuation_distance=20, seed_offset=123, volume=0.92),
    _s("stalker_pounce", "entity", "weapon_slash", 0.38, 510.0, 12.0, "Mycelial Stalker strikes", "Acechador Micelial ataca", species="insectoid_chitin", loudness_group="combat", variations=2, attenuation_distance=20, seed_offset=124, volume=0.94),

    # 4. Acid Spitter (wet_visceral)
    _s("acid_spitter_gurgle", "entity", "gurgle", 0.66, 230.0, 11.0, "Acid Spitter gurgles", "Escupidor Ácido gorgotea", species="wet_visceral", loudness_group="ambient", variations=3, attenuation_distance=18, seed_offset=131, volume=0.88),
    _s("acid_spitter_hurt", "entity", "hurt", 0.32, 280.0, 12.5, "Acid Spitter hurts", "Escupidor Ácido herido", species="wet_visceral", loudness_group="combat", variations=3, attenuation_distance=18, seed_offset=132, volume=0.88),
    _s("acid_spitter_death", "entity", "death", 0.70, 215.0, 8.5, "Acid Spitter dies", "Escupidor Ácido muere", species="wet_visceral", loudness_group="death", variations=2, attenuation_distance=20, seed_offset=133, volume=0.92),
    _s("acid_spitter_spit", "entity", "projectile_pop", 0.34, 330.0, 15.0, "Acid Spitter fires", "Escupidor Ácido dispara", species="wet_visceral", loudness_group="combat", variations=3, attenuation_distance=22, seed_offset=134, volume=0.92),

    # 5. Burrower (insectoid_chitin)
    _s("burrower_grind", "entity", "grind", 0.68, 175.0, 11.5, "Burrower grinds chitin", "Excavador rechina quitina", species="insectoid_chitin", loudness_group="ambient", variations=3, attenuation_distance=20, seed_offset=141, volume=0.90),
    _s("burrower_hurt", "entity", "hurt", 0.32, 225.0, 13.5, "Burrower hurts", "Excavador herido", species="insectoid_chitin", loudness_group="combat", variations=3, attenuation_distance=18, seed_offset=142, volume=0.90),
    _s("burrower_death", "entity", "death", 0.74, 160.0, 8.5, "Burrower dies", "Excavador muere", species="insectoid_chitin", loudness_group="death", variations=2, attenuation_distance=20, seed_offset=143, volume=0.92),
    _s("burrower_tunnel", "entity", "heavy_thud", 0.50, 108.0, 12.0, "Burrower excavates", "Excavador perfora roca", species="insectoid_chitin", loudness_group="combat", variations=2, attenuation_distance=24, seed_offset=144, volume=0.92),

    # 6. Pyro-Shroom (pyro_fungal)
    _s("pyro_shroom_crackle", "entity", "crackle", 0.62, 360.0, 14.0, "Pyro-Shroom crackles", "Piro-Hongo crepita", species="pyro_fungal", loudness_group="ambient", variations=3, attenuation_distance=18, seed_offset=151, volume=0.86),
    _s("pyro_shroom_hurt", "entity", "hurt", 0.30, 410.0, 15.0, "Pyro-Shroom hurts", "Piro-Hongo herido", species="pyro_fungal", loudness_group="combat", variations=3, attenuation_distance=18, seed_offset=152, volume=0.88),
    _s("pyro_shroom_death", "entity", "death", 0.68, 280.0, 9.5, "Pyro-Shroom dies", "Piro-Hongo muere", species="pyro_fungal", loudness_group="death", variations=2, attenuation_distance=20, seed_offset=153, volume=0.92),
    _s("pyro_shroom_ignite", "entity", "sizzle", 0.76, 460.0, 16.0, "Pyro-Shroom primes fuse", "Piro-Hongo prepara ignición", species="pyro_fungal", loudness_group="combat", variations=2, attenuation_distance=24, seed_offset=154, volume=0.95),

    # 7. Synapse Weaver (synaptic_eldritch)
    _s("synapse_weaver_hum", "entity", "crystal_hum", 0.80, 390.0, 4.5, "Synapse Weaver hums", "Tejedor de Sinapsis zumba", species="synaptic_eldritch", loudness_group="ambient", variations=3, attenuation_distance=20, seed_offset=161, volume=0.86),
    _s("synapse_weaver_hurt", "entity", "hurt", 0.32, 480.0, 12.0, "Synapse Weaver hurts", "Tejedor de Sinapsis herido", species="synaptic_eldritch", loudness_group="combat", variations=3, attenuation_distance=18, seed_offset=162, volume=0.88),
    _s("synapse_weaver_death", "entity", "death", 0.76, 360.0, 7.5, "Synapse Weaver dies", "Tejedor de Sinapsis muere", species="synaptic_eldritch", loudness_group="death", variations=2, attenuation_distance=22, seed_offset=163, volume=0.92),
    _s("synapse_weaver_buff", "entity", "bio_pulse", 0.58, 460.0, 8.5, "Synapse Weaver weaves pulse", "Tejedor de Sinapsis teje pulso", species="synaptic_eldritch", loudness_group="combat", variations=2, attenuation_distance=22, seed_offset=164, volume=0.90),

    # 8. Goliath Carrier (wet_visceral)
    _s("goliath_carrier_thud", "entity", "heavy_thud", 0.78, 62.0, 4.0, "Goliath Carrier thuds", "Portador Goliat retumba", species="wet_visceral", loudness_group="ambient", variations=3, attenuation_distance=28, seed_offset=171, volume=0.95),
    _s("goliath_carrier_hurt", "entity", "hurt", 0.42, 102.0, 7.5, "Goliath Carrier hurts", "Portador Goliat herido", species="wet_visceral", loudness_group="combat", variations=3, attenuation_distance=24, seed_offset=172, volume=0.92),
    _s("goliath_carrier_death", "entity", "death", 1.05, 82.0, 4.5, "Goliath Carrier dies", "Portador Goliat muere", species="wet_visceral", loudness_group="death", variations=2, attenuation_distance=28, seed_offset=173, volume=0.96),
    _s("goliath_carrier_spawn", "entity", "gurgle", 0.64, 140.0, 7.5, "Goliath Carrier unleashes brood", "Portador Goliat libera crías", species="wet_visceral", loudness_group="combat", variations=2, attenuation_distance=26, seed_offset=174, volume=0.94),

    # 9. Leech Parasite (insectoid_chitin)
    _s("leech_parasite_screech", "entity", "screech", 0.48, 790.0, 16.0, "Leech Parasite screeches", "Parásito Sanguijuela chilla", species="insectoid_chitin", loudness_group="ambient", variations=3, attenuation_distance=16, seed_offset=181, volume=0.86),
    _s("leech_parasite_hurt", "entity", "hurt", 0.26, 710.0, 18.0, "Leech Parasite hurts", "Parásito Sanguijuela herido", species="insectoid_chitin", loudness_group="combat", variations=3, attenuation_distance=16, seed_offset=182, volume=0.88),
    _s("leech_parasite_death", "entity", "death", 0.50, 610.0, 12.0, "Leech Parasite dies", "Parásito Sanguijuela muere", species="insectoid_chitin", loudness_group="death", variations=2, attenuation_distance=18, seed_offset=183, volume=0.90),
    _s("leech_parasite_siphon", "entity", "feast", 0.40, 410.0, 13.5, "Leech Parasite siphons", "Parásito Sanguijuela drena", species="insectoid_chitin", loudness_group="combat", variations=2, attenuation_distance=18, seed_offset=184, volume=0.88),

    # 10. Colony Warden (colossal_organic)
    _s("colony_warden_bellow", "entity", "bellow", 0.96, 72.0, 3.6, "Colony Warden bellows", "Guardián de la Colonia brama", species="colossal_organic", loudness_group="ambient", variations=3, attenuation_distance=26, seed_offset=191, volume=0.94),
    _s("colony_warden_hurt", "entity", "hurt", 0.40, 112.0, 7.8, "Colony Warden hurts", "Guardián de la Colonia herido", species="colossal_organic", loudness_group="combat", variations=3, attenuation_distance=24, seed_offset=192, volume=0.92),
    _s("colony_warden_death", "entity", "death", 1.05, 86.0, 4.8, "Colony Warden dies", "Guardián de la Colonia muere", species="colossal_organic", loudness_group="death", variations=2, attenuation_distance=28, seed_offset=193, volume=0.96),
    _s("colony_warden_shockwave", "entity", "explosion_purge", 0.80, 78.0, 5.5, "Colony Warden unleashes shockwave", "Guardián de la Colonia desata onda", species="colossal_organic", loudness_group="combat", variations=2, attenuation_distance=30, seed_offset=194, volume=0.96),

    # 11. Spore Architect (synaptic_eldritch)
    _s("spore_architect_pulse", "entity", "bio_pulse", 0.62, 310.0, 9.0, "Spore Architect pulses", "Arquitecto de Esporas pulsa", species="synaptic_eldritch", loudness_group="ambient", variations=3, attenuation_distance=20, seed_offset=201, volume=0.88),
    _s("spore_architect_hurt", "entity", "hurt", 0.32, 345.0, 11.5, "Spore Architect hurts", "Arquitecto de Esporas herido", species="synaptic_eldritch", loudness_group="combat", variations=3, attenuation_distance=18, seed_offset=202, volume=0.88),
    _s("spore_architect_death", "entity", "death", 0.72, 275.0, 7.8, "Spore Architect dies", "Arquitecto de Esporas muere", species="synaptic_eldritch", loudness_group="death", variations=2, attenuation_distance=22, seed_offset=203, volume=0.90),
    _s("spore_architect_construct", "entity", "surface_place", 0.56, 210.0, 10.5, "Spore Architect raises node", "Arquitecto de Esporas erige nodo", variant="synaptic_node", species="synaptic_eldritch", loudness_group="combat", variations=2, attenuation_distance=24, seed_offset=204, volume=0.92),

    # 12. Synaptic Mimic (synaptic_eldritch)
    _s("synaptic_mimic_echo", "entity", "crystal_hum", 0.68, 445.0, 7.2, "Synaptic Mimic echoes", "Mímico Sináptico resuena", species="synaptic_eldritch", loudness_group="ambient", variations=3, attenuation_distance=20, seed_offset=211, volume=0.88),
    _s("synaptic_mimic_hurt", "entity", "hurt", 0.32, 495.0, 12.5, "Synaptic Mimic hurts", "Mímico Sináptico herido", species="synaptic_eldritch", loudness_group="combat", variations=3, attenuation_distance=18, seed_offset=212, volume=0.90),
    _s("synaptic_mimic_death", "entity", "death", 0.72, 395.0, 9.0, "Synaptic Mimic dies", "Mímico Sináptico muere", species="synaptic_eldritch", loudness_group="death", variations=2, attenuation_distance=22, seed_offset=213, volume=0.92),
    _s("synaptic_mimic_strike", "entity", "weapon_slash", 0.40, 420.0, 13.5, "Synaptic Mimic shifts stance", "Mímico Sináptico ejecuta postura", species="synaptic_eldritch", loudness_group="combat", variations=2, attenuation_distance=20, seed_offset=214, volume=0.92),

    # 13. Spore Behemoth (Mini-Boss, colossal_organic)
    _s("spore_behemoth_roar", "entity", "bellow", 2.35, 54.0, 4.2, "Spore Behemoth roars", "Behemot de Esporas ruge", species="colossal_organic", loudness_group="boss_cinematic", variations=3, attenuation_distance=50, seed_offset=221, volume=1.0),
    _s("spore_behemoth_hurt", "entity", "hurt", 0.72, 78.0, 6.8, "Spore Behemoth hurts", "Behemot de Esporas herido", species="colossal_organic", loudness_group="combat", variations=3, attenuation_distance=40, seed_offset=222, volume=0.98),
    _s("spore_behemoth_death", "entity", "death", 2.85, 58.0, 3.8, "Spore Behemoth dies", "Behemot de Esporas muere", species="colossal_organic", loudness_group="boss_cinematic", variations=2, attenuation_distance=56, seed_offset=223, volume=1.0),
    _s("spore_behemoth_slam", "entity", "heavy_thud", 1.18, 50.0, 5.2, "Spore Behemoth slams", "Behemot de Esporas sacude el suelo", species="colossal_organic", loudness_group="boss_cinematic", variations=2, attenuation_distance=48, seed_offset=224, volume=1.0),

    # 14. Hive Core (Boss, synaptic_eldritch)
    _s("hive_core_resonance", "entity", "rift_hum", 2.45, 92.0, 3.0, "Hive Core resonates", "Núcleo de la Colmena resuena", species="synaptic_eldritch", loudness_group="boss_cinematic", variations=3, attenuation_distance=54, seed_offset=231, volume=1.0),
    _s("hive_core_hurt", "entity", "hurt", 0.72, 145.0, 8.0, "Hive Core hurts", "Núcleo de la Colmena herido", species="synaptic_eldritch", loudness_group="combat", variations=3, attenuation_distance=44, seed_offset=232, volume=0.98),
    _s("hive_core_death", "entity", "death", 3.10, 98.0, 4.0, "Hive Core collapses", "Núcleo de la Colmena colapsa", species="synaptic_eldritch", loudness_group="boss_cinematic", variations=2, attenuation_distance=60, seed_offset=233, volume=1.0),
    _s("hive_core_shield", "entity", "crystal_hum", 1.05, 265.0, 9.5, "Hive Core shield reacts", "Escudo del Núcleo reacciona", species="synaptic_eldritch", loudness_group="boss_cinematic", variations=2, attenuation_distance=48, seed_offset=234, volume=1.0),

    # 15. Apex Matriarch (Dimensional Boss, synaptic_eldritch)
    _s("apex_matriarch_wail", "entity", "screech", 2.55, 185.0, 4.8, "Apex Matriarch wails", "Matriarca Suprema clama", species="synaptic_eldritch", loudness_group="boss_cinematic", variations=3, attenuation_distance=58, seed_offset=241, volume=1.0),
    _s("apex_matriarch_hurt", "entity", "hurt", 0.74, 205.0, 8.5, "Apex Matriarch hurts", "Matriarca Suprema herida", species="synaptic_eldritch", loudness_group="combat", variations=3, attenuation_distance=46, seed_offset=242, volume=0.98),
    _s("apex_matriarch_death", "entity", "death", 3.25, 165.0, 4.2, "Apex Matriarch perishes", "Matriarca Suprema perece", species="synaptic_eldritch", loudness_group="boss_cinematic", variations=2, attenuation_distance=64, seed_offset=243, volume=1.0),
    _s("apex_matriarch_barrage", "entity", "projectile_pop", 1.10, 215.0, 12.0, "Apex Matriarch fires spore barrage", "Matriarca Suprema lanza descarga", species="synaptic_eldritch", loudness_group="boss_cinematic", variations=2, attenuation_distance=54, seed_offset=244, volume=1.0),

    # 16. Purifier Drone (Ally, clinical purifier_tech)
    _s("purifier_drone_hum", "entity", "tech_chime", 0.56, 610.0, 6.0, "Purifier Drone hums", "Dron Purificador zumba", species="purifier_tech", loudness_group="ambient", variations=3, attenuation_distance=16, seed_offset=251, volume=0.75),
    _s("purifier_drone_hurt", "entity", "hurt", 0.30, 490.0, 14.0, "Purifier Drone damaged", "Dron Purificador dañado", species="purifier_tech", loudness_group="combat", variations=3, attenuation_distance=18, seed_offset=252, volume=0.88),
    _s("purifier_drone_death", "entity", "death", 0.68, 410.0, 10.5, "Purifier Drone powers down", "Dron Purificador se apaga", species="purifier_tech", loudness_group="death", variations=2, attenuation_distance=20, seed_offset=253, volume=0.92),
    _s("purifier_drone_fire", "entity", "tech_chime", 0.28, 840.0, 12.0, "Purifier Drone fires spore", "Dron Purificador dispara espora", species="purifier_tech", loudness_group="combat", variations=3, attenuation_distance=20, seed_offset=254, volume=0.86),

    # 17. Mycelial Chimera (Ally, colossal_organic)
    _s("mycelial_chimera_growl", "entity", "growl", 0.74, 125.0, 6.2, "Mycelial Chimera growls", "Quimera Micelial gruñe", species="colossal_organic", loudness_group="ambient", variations=3, attenuation_distance=20, seed_offset=261, volume=0.88),
    _s("mycelial_chimera_hurt", "entity", "hurt", 0.34, 165.0, 10.0, "Mycelial Chimera hurts", "Quimera Micelial herida", species="colossal_organic", loudness_group="combat", variations=3, attenuation_distance=20, seed_offset=262, volume=0.90),
    _s("mycelial_chimera_death", "entity", "death", 0.88, 120.0, 5.8, "Mycelial Chimera falls", "Quimera Micelial cae", species="colossal_organic", loudness_group="death", variations=2, attenuation_distance=24, seed_offset=263, volume=0.94),
    _s("chimera_roar", "entity", "bellow", 0.90, 138.0, 7.2, "Mycelial Chimera roars", "Quimera Micelial ruge", species="colossal_organic", loudness_group="combat", variations=2, attenuation_distance=26, seed_offset=264, volume=0.94),
    _s("mycelial_chimera_strike", "entity", "weapon_slash", 0.44, 250.0, 11.5, "Mycelial Chimera unleashes gene", "Quimera Micelial activa gen", species="colossal_organic", loudness_group="combat", variations=2, attenuation_distance=22, seed_offset=265, volume=0.92),

    # 18. Anatomical Steps (5, variations=4)
    _s("mob_step_chitin", "entity", "step_chitin", 0.19, 390.0, 11.5, "Chitinous footsteps", "Pasos quitinosos", species="insectoid_chitin", loudness_group="footstep", variations=4, attenuation_distance=12, seed_offset=271, volume=0.60),
    _s("mob_step_arachnid", "entity", "step_arachnid", 0.23, 510.0, 14.5, "Skittering footsteps", "Pasos arácnidos", species="insectoid_chitin", loudness_group="footstep", variations=4, attenuation_distance=12, seed_offset=272, volume=0.62),
    _s("mob_step_heavy", "entity", "step_heavy", 0.30, 105.0, 6.0, "Heavy footsteps", "Pasos pesados", species="colossal_organic", loudness_group="footstep", variations=4, attenuation_distance=16, seed_offset=273, volume=0.76),
    _s("mob_step_colossal", "entity", "step_colossal", 0.68, 52.0, 4.0, "Colossal footsteps", "Pasos colosales", species="colossal_organic", loudness_group="footstep", variations=4, attenuation_distance=32, seed_offset=274, volume=0.96),
    _s("mob_step_hover", "entity", "step_hover", 0.24, 360.0, 8.0, "Hovering thrusters", "Zumbido de levitación", species="purifier_tech", loudness_group="footstep", variations=4, attenuation_distance=12, seed_offset=275, volume=0.55),
]

BLOCK_PHYSICAL_SOUNDS: list[SoundSpec] = [
    *_surface_family("block_mycelium", "mycelium", 155.0, "Mycelial flora", "Flora micelial", 300),
    *_surface_family("block_synaptic_node", "synaptic_node", 520.0, "Synaptic node", "Nodo sináptico", 320),
    *_surface_family("block_chitin", "chitin", 480.0, "Chitin", "Quitina", 340),
    *_surface_family("block_mycelial_stone", "mycelial_stone", 140.0, "Mycelial stone", "Piedra micelial", 360),
    *_surface_family("block_purification_tech", "purification_tech", 740.0, "Purification tech", "Tecnología purificadora", 380),
    *_surface_family("block_antibody_cyst", "antibody_cyst", 185.0, "Antibody cyst", "Quiste anticuerpo", 400),
    *_surface_family("block_hive_altar", "hive_altar", 112.0, "Hive altar", "Altar colmena", 420),
]

BLOCK_FUNCTIONAL_SOUNDS: list[SoundSpec] = [
    _s("spore_vent_puff", "block_functional", "block_functional", 0.48, 290.0, 10.0, "Spore Vent puffs", "Respiradero libera esporas", species="wet_visceral", loudness_group="ambient", variations=3, attenuation_distance=14, seed_offset=501, volume=0.78),
    _s("purification_pylon_pulse", "block_functional", "block_functional", 0.58, 520.0, 7.0, "Purification Pylon pulses", "Pilón de Purificación pulsa", species="purifier_tech", loudness_group="ambient", variations=2, attenuation_distance=18, seed_offset=502, volume=0.82),
    _s("purification_turret_fire", "block_functional", "block_functional", 0.24, 760.0, 14.0, "Purification Turret fires", "Torreta de Purificación dispara", species="purifier_tech", loudness_group="combat", variations=3, attenuation_distance=22, seed_offset=503, volume=0.90),
    _s("purification_turret_upgrade", "block_functional", "block_functional", 0.62, 650.0, 9.0, "Purification Turret upgraded", "Torreta de Purificación mejorada", species="purifier_tech", loudness_group="ui_item", variations=2, attenuation_distance=18, seed_offset=504, volume=0.92),
    _s("purification_turret_repair", "block_functional", "block_functional", 0.48, 380.0, 11.0, "Purification Turret unjammed", "Torreta de Purificación reparada", species="purifier_tech", loudness_group="ui_item", variations=2, attenuation_distance=16, seed_offset=505, volume=0.88),
    _s("synaptic_node_mature", "block_functional", "block_functional", 0.86, 260.0, 8.0, "Synaptic Node matures", "Nodo Sináptico madura", species="synaptic_eldritch", loudness_group="combat", variations=2, attenuation_distance=26, seed_offset=506, volume=0.92),
    _s("symbiotic_node_charge", "block_functional", "block_functional", 0.56, 540.0, 6.0, "Symbiotic Node recharged", "Nodo Simbiótico recargado", species="bio_relic", loudness_group="ui_item", variations=2, attenuation_distance=16, seed_offset=507, volume=0.86),
    _s("symbiotic_node_teleport", "block_functional", "block_functional", 0.72, 370.0, 8.5, "Mycorrhizal travel completes", "Viaje micorrízico completado", species="synaptic_eldritch", loudness_group="combat", variations=2, attenuation_distance=24, seed_offset=508, volume=0.90),
    _s("spore_bulb_burst", "block_functional", "block_functional", 0.50, 210.0, 12.0, "Spore Bulb bursts", "Bulbo de Esporas estalla", species="wet_visceral", loudness_group="combat", variations=2, attenuation_distance=18, seed_offset=509, volume=0.90),
    _s("ephemeral_chitin_crumble", "block_functional", "block_functional", 0.52, 280.0, 10.0, "Ephemeral Chitin crumbles", "Quitina Efímera se desmorona", variant="chitin", species="insectoid_chitin", loudness_group="block_light", variations=2, attenuation_distance=14, seed_offset=510, volume=0.75),
    _s("mycelial_rift_hum", "block_functional", "block_functional", 1.35, 125.0, 3.2, "Mycelial Rift hums", "Grieta Micelial resuena", species="synaptic_eldritch", loudness_group="ambient", variations=2, attenuation_distance=24, seed_offset=511, volume=0.80),
    _s("hive_altar_awaken", "block_functional", "block_functional", 2.75, 74.0, 4.5, "Hive Core awakens", "El Núcleo de la Colmena despierta", species="colossal_organic", loudness_group="boss_cinematic", variations=1, attenuation_distance=56, seed_offset=512, volume=1.0),
    _s("matriarch_altar_awaken", "block_functional", "block_functional", 2.95, 112.0, 4.2, "Apex Matriarch awakens", "La Matriarca Suprema despierta", species="synaptic_eldritch", loudness_group="boss_cinematic", variations=1, attenuation_distance=60, seed_offset=513, volume=1.0),
    _s("synaptic_bench_synthesize", "block_functional", "block_functional", 0.68, 570.0, 8.5, "Synaptic synthesis completes", "Síntesis sináptica completada", species="purifier_tech", loudness_group="ui_item", variations=2, attenuation_distance=14, seed_offset=514, volume=0.88),
]

ITEM_PROJECTILE_SOUNDS: list[SoundSpec] = [
    _s("bio_scanner_ping", "item", "tech_chime", 0.24, 910.0, 10.0, "Bio-Scanner pings", "Bio-Escáner analiza", species="purifier_tech", loudness_group="ui_item", variations=3, attenuation_distance=12, seed_offset=601, volume=0.82),
    _s("bio_scanner_thermal_pulse", "item", "bio_pulse", 0.48, 620.0, 12.0, "Thermal pulse emitted", "Pulso térmico emitido", species="purifier_tech", loudness_group="combat", variations=2, attenuation_distance=20, seed_offset=602, volume=0.88),
    _s("bio_extractor_draw", "item", "gurgle", 0.46, 360.0, 13.5, "Genetic sample extracted", "Muestra genética extraída", species="wet_visceral", loudness_group="ui_item", variations=2, attenuation_distance=14, seed_offset=603, volume=0.88),
    _s("symbiotic_blade_slash", "item", "weapon_slash", 0.36, 390.0, 11.0, "Symbiotic arc unleashed", "Corte simbiótico liberado", species="bio_relic", loudness_group="combat", variations=2, attenuation_distance=18, seed_offset=604, volume=0.92),
    _s("enzyme_lance_thrust", "item", "weapon_slash", 0.40, 500.0, 14.5, "Enzyme Lance thrusts", "Lanza de Enzimas embiste", species="bio_relic", loudness_group="combat", variations=2, attenuation_distance=18, seed_offset=605, volume=0.92),
    _s("spore_launcher_fire", "item", "projectile_pop", 0.28, 440.0, 14.0, "Spore Launcher fires", "Lanzador de Esporas dispara", species="purifier_tech", loudness_group="combat", variations=3, attenuation_distance=20, seed_offset=606, volume=0.90),
    _s("purifier_grenade_throw", "item", "weapon_slash", 0.25, 390.0, 9.0, "Purifier Grenade thrown", "Granada Purificadora lanzada", species="purifier_tech", loudness_group="ui_item", variations=2, attenuation_distance=16, seed_offset=607, volume=0.84),
    _s("purifier_grenade_detonate", "projectile", "explosion_purge", 0.72, 165.0, 7.5, "Purifier Grenade detonates", "Granada Purificadora detona", species="purifier_tech", loudness_group="combat", variations=2, attenuation_distance=28, seed_offset=608, volume=0.96),
    _s("acid_spit_impact", "projectile", "sizzle", 0.30, 310.0, 15.0, "Acid spit splashes", "Saliva ácida impacta", species="wet_visceral", loudness_group="combat", variations=3, attenuation_distance=18, seed_offset=609, volume=0.84),
    _s("neutralizing_spore_impact", "projectile", "projectile_pop", 0.30, 580.0, 13.0, "Neutralizing spore bursts", "Espora neutralizante impacta", species="purifier_tech", loudness_group="combat", variations=3, attenuation_distance=18, seed_offset=610, volume=0.86),
    _s("chitin_armor_equip", "item", "surface_place", 0.30, 340.0, 9.0, "Chitin armor equips", "Armadura de quitina equipada", variant="chitin", species="insectoid_chitin", loudness_group="ui_item", variations=2, attenuation_distance=12, seed_offset=611, volume=0.88),
    _s("chitin_armor_evolve", "item", "crystal_hum", 0.70, 440.0, 7.5, "Chitin armor evolves", "Armadura de quitina evoluciona", species="bio_relic", loudness_group="combat", variations=2, attenuation_distance=18, seed_offset=612, volume=0.92),
    _s("spore_antidote_cure", "item", "tech_chime", 0.44, 640.0, 7.5, "Antidote purges infection", "Antídoto purga infección", species="purifier_tech", loudness_group="ui_item", variations=2, attenuation_distance=14, seed_offset=613, volume=0.88),
    _s("counter_serum_inject", "item", "bio_pulse", 0.42, 430.0, 10.5, "Counter-Serum injected", "Suero anti-rasgo inyectado", species="bio_relic", loudness_group="ui_item", variations=2, attenuation_distance=14, seed_offset=614, volume=0.90),
    _s("symbiotic_mutagen_implant", "item", "bio_pulse", 0.60, 295.0, 9.0, "Symbiotic Mutagen implanted", "Mutágeno simbiótico implantado", species="wet_visceral", loudness_group="ui_item", variations=2, attenuation_distance=14, seed_offset=615, volume=0.92),
    _s("synaptic_graft_splice", "item", "crystal_hum", 0.56, 420.0, 7.0, "Synaptic Graft splices node", "Injerto sináptico reprograma nodo", species="synaptic_eldritch", loudness_group="ui_item", variations=2, attenuation_distance=16, seed_offset=616, volume=0.90),
    _s("neural_decoy_deploy", "item", "tech_chime", 0.42, 700.0, 11.0, "Neural Decoy deployed", "Señuelo neuronal desplegado", species="purifier_tech", loudness_group="ui_item", variations=2, attenuation_distance=18, seed_offset=617, volume=0.86),
    _s("volatile_engram_pulse", "item", "crystal_hum", 0.54, 520.0, 12.5, "Engram memory discharges", "Engrama de memoria descarga", species="synaptic_eldritch", loudness_group="combat", variations=2, attenuation_distance=20, seed_offset=618, volume=0.88),
    _s("schism_injector_sting", "item", "screech", 0.44, 550.0, 14.0, "Schism Injector fractures strain", "Inyector de Cisma divide cepa", species="synaptic_eldritch", loudness_group="combat", variations=2, attenuation_distance=20, seed_offset=619, volume=0.90),
    _s("truce_pheromone_spray", "item", "hiss", 0.48, 460.0, 8.0, "Truce Pheromone diffuses", "Feromona de tregua dispersada", species="wet_visceral", loudness_group="ui_item", variations=2, attenuation_distance=16, seed_offset=620, volume=0.86),
    _s("quorum_seal_pact", "item", "crystal_hum", 0.62, 380.0, 6.0, "Quorum Seal invokes pact", "Sello de Quórum sella pacto", species="synaptic_eldritch", loudness_group="ui_item", variations=2, attenuation_distance=18, seed_offset=621, volume=0.90),
    _s("chimera_ovum_hatch", "item", "hatch", 0.68, 290.0, 9.5, "Chimera Ovum hatches", "Huevo de Quimera eclosiona", species="colossal_organic", loudness_group="combat", variations=2, attenuation_distance=20, seed_offset=622, volume=0.92),
    _s("purifier_drone_deploy", "item", "tech_chime", 0.52, 680.0, 9.0, "Purifier Drone activated", "Dron Purificador activado", species="purifier_tech", loudness_group="ui_item", variations=2, attenuation_distance=16, seed_offset=623, volume=0.90),
    _s("tide_beacon_activate", "item", "portal_ignite", 0.78, 285.0, 7.0, "Tide Beacon surges", "Faro de Marea se activa", species="bio_relic", loudness_group="combat", variations=2, attenuation_distance=32, seed_offset=624, volume=0.95),
    _s("rift_catalyst_ignite", "item", "portal_ignite", 1.05, 200.0, 5.5, "Synaptic Rift tears open", "Grieta Sináptica desgarrada", species="synaptic_eldritch", loudness_group="boss_cinematic", variations=1, attenuation_distance=44, seed_offset=625, volume=0.98),
    _s("overmind_crown_surge", "item", "explosion_purge", 0.96, 175.0, 6.5, "Overmind Crown surges", "Corona de la Supramente pulsa", species="synaptic_eldritch", loudness_group="boss_cinematic", variations=1, attenuation_distance=46, seed_offset=626, volume=0.98),
]

SYSTEM_HIVE_SOUNDS: list[SoundSpec] = [
    _s("hive_mutate_tier2", "system", "screech", 1.15, 195.0, 6.0, "Hive Mind evolves", "La Mente Colmena evoluciona", species="synaptic_eldritch", loudness_group="boss_cinematic", variations=1, attenuation_distance=48, seed_offset=701, volume=0.98),
    _s("synaptic_pulse", "system", "bio_pulse", 0.58, 330.0, 8.0, "Synaptic network pulses", "Pulso de la red sináptica", species="synaptic_eldritch", loudness_group="combat", variations=2, attenuation_distance=26, seed_offset=702, volume=0.90),
    _s("boss_phase_shift", "system", "explosion_purge", 2.40, 105.0, 5.0, "Hive Core shifts phase", "El Núcleo de la Colmena cambia de fase", species="synaptic_eldritch", loudness_group="boss_cinematic", variations=1, attenuation_distance=60, seed_offset=703, volume=1.0),
    _s("suppressor_discharge", "system", "tech_chime", 0.72, 530.0, 9.0, "Synaptic Suppressor discharges", "Supresor Sináptico descarga", species="purifier_tech", loudness_group="combat", variations=2, attenuation_distance=32, seed_offset=704, volume=0.94),
    _s("fungal_tide_horn", "system", "bellow", 2.95, 68.0, 3.2, "Fungal Tide erupts", "Estalla la Marea Fúngica", species="colossal_organic", loudness_group="boss_cinematic", variations=1, attenuation_distance=64, seed_offset=705, volume=1.0),
    _s("squad_desync", "system", "death", 0.56, 390.0, 10.5, "Synaptic squad desynchronizes", "Escuadrón sináptico desincronizado", species="purifier_tech", loudness_group="combat", variations=2, attenuation_distance=24, seed_offset=706, volume=0.90),
    _s("corrosive_jam", "system", "sizzle", 0.54, 370.0, 13.5, "Corrosive spores jam machinery", "Esporas corrosivas atascan maquinaria", species="wet_visceral", loudness_group="combat", variations=2, attenuation_distance=20, seed_offset=707, volume=0.88),
    _s("immune_surge", "system", "bellow", 1.20, 98.0, 5.5, "Dimensional immune surge", "Oleada inmune dimensional", species="colossal_organic", loudness_group="boss_cinematic", variations=1, attenuation_distance=48, seed_offset=708, volume=0.96),
    _s("mob_promote_mutated", "system", "bio_pulse", 0.64, 375.0, 8.5, "Organism mutates", "Organismo muta a Mutado", species="synaptic_eldritch", loudness_group="combat", variations=2, attenuation_distance=24, seed_offset=709, volume=0.90),
    _s("mob_promote_elite", "system", "portal_ignite", 0.88, 225.0, 7.5, "Organism ascends to Elite", "Organismo asciende a Élite", species="synaptic_eldritch", loudness_group="boss_cinematic", variations=2, attenuation_distance=30, seed_offset=710, volume=0.96),
    _s("mob_dawn_retreat", "system", "surface_break", 0.48, 175.0, 7.0, "Organism burrows from daylight", "Organismo se oculta bajo tierra", variant="mycelium", species="insectoid_chitin", loudness_group="ambient", variations=2, attenuation_distance=16, seed_offset=711, volume=0.80),
    _s("infected_mob_convert", "system", "hatch", 0.74, 225.0, 8.8, "Infected creature assimilated", "Criatura infectada asimilada", species="wet_visceral", loudness_group="combat", variations=2, attenuation_distance=22, seed_offset=712, volume=0.92),
    _s("mycorrhizal_tremor", "system", "bio_pulse", 0.38, 195.0, 12.5, "Mycorrhizal network vibrates", "La red micorrízica vibra", species="synaptic_eldritch", loudness_group="ambient", variations=3, attenuation_distance=18, seed_offset=713, volume=0.76),
    _s("homeostasis_heartbeat", "system", "heartbeat", 0.74, 58.0, 3.0, "Deep Mycelium pulses", "El Micelio Profundo late", species="wet_visceral", loudness_group="ambient", variations=2, attenuation_distance=28, seed_offset=714, volume=0.92),
    _s("antibody_cyst_encapsulate", "system", "surface_place", 0.40, 150.0, 8.0, "Antibody cyst encapsulates", "Quiste anticuerpo encapsula", variant="antibody_cyst", species="wet_visceral", loudness_group="block_heavy", variations=2, attenuation_distance=18, seed_offset=715, volume=0.84),
    _s("acid_rain_sizzle", "system", "sizzle", 0.34, 540.0, 16.0, "Acid rain corrodes", "La lluvia ácida corroe", species="wet_visceral", loudness_group="ambient", variations=3, attenuation_distance=16, seed_offset=716, volume=0.72),
    _s("necrophagy_feast", "system", "feast", 0.48, 200.0, 11.5, "Organism consumes carrion", "Organismo consume biomasa", species="wet_visceral", loudness_group="combat", variations=3, attenuation_distance=18, seed_offset=717, volume=0.84),
    _s("swarm_flank_call", "system", "chitter", 0.36, 540.0, 15.0, "Swarm signals flank", "Enjambre coordina flanqueo", species="insectoid_chitin", loudness_group="combat", variations=3, attenuation_distance=22, seed_offset=718, volume=0.82),
]

BIOME_AMBIENT_AND_ALERT_SOUNDS: list[SoundSpec] = [
    # 1. Deep Mycelium Caverns biome soundscape (3)
    _s("biome_caverns_loop", "biome_ambient", "system_env", 5.50, 92.0, 2.4, "Mycelial caverns hum", "Cavernas miceliales resuenan", species="colossal_organic", loudness_group="ambient", variations=2, attenuation_distance=32, seed_offset=801, volume=0.78),
    _s("biome_caverns_mood", "biome_ambient", "system_env", 3.20, 74.0, 3.6, "Subterranean chitin echoes", "Ecos subterráneos de quitina", species="colossal_organic", loudness_group="ambient", variations=3, attenuation_distance=48, seed_offset=802, volume=0.88),
    _s("biome_caverns_additions", "biome_ambient", "system_env", 1.40, 240.0, 8.5, "Spores drip in the dark", "Goteo de esporas en la penumbra", species="wet_visceral", loudness_group="ambient", variations=4, attenuation_distance=24, seed_offset=803, volume=0.76),

    # 2. Hive Heart Sanctum biome soundscape (3)
    _s("biome_sanctum_loop", "biome_ambient", "system_env", 5.50, 118.0, 2.8, "Hive Sanctum arterial pulse", "Pulso arterial del Santuario Colmena", species="synaptic_eldritch", loudness_group="ambient", variations=2, attenuation_distance=32, seed_offset=804, volume=0.80),
    _s("biome_sanctum_mood", "biome_ambient", "system_env", 3.40, 156.0, 4.2, "Synaptic choir murmurs", "Murmullo del coro sináptico", species="synaptic_eldritch", loudness_group="ambient", variations=3, attenuation_distance=48, seed_offset=805, volume=0.90),
    _s("biome_sanctum_additions", "biome_ambient", "system_env", 1.50, 340.0, 10.0, "Neural veins discharge", "Venas neuronales descargan", species="synaptic_eldritch", loudness_group="ambient", variations=4, attenuation_distance=24, seed_offset=806, volume=0.78),

    # 3. Latent Spore Forest biome soundscape (3)
    _s("biome_forest_loop", "biome_ambient", "system_env", 5.50, 168.0, 3.1, "Spore canopy rustles", "Dosel de esporas susurra", species="bio_relic", loudness_group="ambient", variations=2, attenuation_distance=32, seed_offset=807, volume=0.76),
    _s("biome_forest_mood", "biome_ambient", "system_env", 3.10, 210.0, 5.0, "Distant fungal canopy call", "Llamado lejano del bosque fúngico", species="bio_relic", loudness_group="ambient", variations=3, attenuation_distance=48, seed_offset=808, volume=0.86),
    _s("biome_forest_additions", "biome_ambient", "system_env", 1.30, 480.0, 12.0, "Bioluminescent spores drift", "Esporas bioluminiscentes flotan", species="insectoid_chitin", loudness_group="ambient", variations=4, attenuation_distance=24, seed_offset=809, volume=0.75),

    # 4. Positional Loops & Hive Adaptation Alerts (5)
    _s("fungal_tide_ambience", "biome_ambient", "system_env", 3.80, 86.0, 4.0, "Fungal Tide storm howls", "Tormenta de la Marea Fúngica ruge", species="colossal_organic", loudness_group="ambient", variations=2, attenuation_distance=32, seed_offset=810, volume=0.85),
    _s("purification_pylon_hum", "biome_ambient", "system_env", 2.40, 420.0, 6.0, "Purification field hums", "Campo de purificación zumba", species="purifier_tech", loudness_group="ambient", variations=2, attenuation_distance=16, seed_offset=811, volume=0.72),
    _s("hive_trait_adapt", "system", "system_env", 0.95, 390.0, 9.5, "Hive adapts new trait", "La Colmena adapta un nuevo rasgo", species="synaptic_eldritch", loudness_group="ui_item", variations=2, attenuation_distance=24, seed_offset=812, volume=0.92),
    _s("hive_synergy_unlock", "system", "system_env", 1.35, 245.0, 7.2, "Hive unlocks elite synergy", "La Colmena desbloquea sinergia de élite", species="synaptic_eldritch", loudness_group="combat", variations=2, attenuation_distance=32, seed_offset=813, volume=0.96),
    _s("hive_threat_milestone", "system", "system_env", 1.20, 175.0, 8.0, "Hive threat escalates", "La amenaza de la Colmena escala", species="colossal_organic", loudness_group="combat", variations=2, attenuation_distance=28, seed_offset=814, volume=0.95),
]

SOUND_CATALOG: list[SoundSpec] = [
    *ENTITY_SOUNDS,
    *BLOCK_PHYSICAL_SOUNDS,
    *BLOCK_FUNCTIONAL_SOUNDS,
    *ITEM_PROJECTILE_SOUNDS,
    *SYSTEM_HIVE_SOUNDS,
    *BIOME_AMBIENT_AND_ALERT_SOUNDS,
]

SOUND_BY_ID: dict[str, SoundSpec] = {spec.sound_id: spec for spec in SOUND_CATALOG}
assert len(SOUND_BY_ID) == len(SOUND_CATALOG), "Duplicate sound_id detected in SOUND_CATALOG"
