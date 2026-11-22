---
name: mycelial-particle-studio
description: Workflow para diseñar, generar, validar y verificar las 34 partículas procedurales de Mycelial Mind (scripts/particle_studio/, client/particle/, presupuestos, render aditivo, galería visual y paridad con el Códice). Úsala al crear o retocar sprites de partículas, arquetipos de movimiento, emisores cliente, config [particles] o auditorías de partículas.
---

# Mycelial Particle Studio

## 1. Fuentes de verdad

- `scripts/particle_studio/catalog.py`: 34 `ParticleSpec` declarativos (id, familia, tamaño, frames, forma, paleta, render, arquetipo, parámetros). **Toda partícula registrada debe existir aquí y viceversa.**
- `src/main/java/com/alex/mycelialmind/registry/ModParticles.java`: registro (34 tipos; `hive_dust` es `ParticleType<ColorParticleOption>`).
- `scripts/particle_studio/generators.py`: pintores SDF por forma; `scripts/particle_studio/build_particles.py` genera `textures/particle/*.png`, `particles/<id>.json`, `exports/particle_manifest.json`, hoja de contacto y `particle_gallery.html`.
- Cliente: `client/particle/` (`ParticleSpec`, `ParticleArchetype`, `MycelialParticle`, `MycelialEmissions`, `MycelialParticleSpecs`, `MycelialParticleProviders`, `MycelialParticleRenderTypes`).
- Servidor: `util/MycelialParticles` (fachada; nunca usar `ParticleTypes` vanilla) y `util/ParticleEmissionBudget`.

## 2. Regenerar y validar

```powershell
python scripts/particle_studio/build_particles.py --all --export-extras --check
python -m unittest discover -s scripts/particle_studio/tests -v
```

- `--check` valida: paridad registro↔catálogo↔JSON↔PNG, tamaño/frames, esquinas transparentes, cobertura alfa, legibilidad en grises, frames distintos, siluetas únicas y determinismo (píxeles idénticos al regenerar).
- `--all` sin `--check` no valida; usar ambos tras cada cambio de arte.
- Antes de tocar `palette_oklab.py`, `sdf_engine.py` o `compositor.py`, lee la skill `mycelial-texture-studio`; el studio de partículas reutiliza esas funciones.

## 3. Reglas de diseño (no negociables)

- Silueta reconocible a 1 bloque y en escala de grises; cada familia con firma de movimiento propia (`ParticleArchetype`).
- Máximo **24** partículas por emisión y **48** por escena (64 solo jefes); ≤3 subpartículas; vida 10–40 ticks.
- Escala: `quadSize` 0.065–0.2 en motas y 0.35–2.4 en anillos; tamaño 100 %→80 %; proximidad monótona hasta 0.35; **nunca** crecer al alejarse.
- Presupuestos ya acordados que deben conservarse: antídoto 20 (2 arcos de 10), purga 24 (16 cian + 8 verde), escáner 12+12 con eco de 4 ticks, sueros 10 (5 pares), engrama 3, impacto 12.
- Sprite HD `32×32` (64 px solo anillos/brumas) con núcleo emisivo y 3–5 niveles de luminancia; animación por **lista de frames** en el JSON (sin `.mcmeta`).
- Todo glow usa el **único** `ParticleRenderType` aditivo; si `enableAdditiveParticleGlow=false` cae a `PARTICLE_SHEET_TRANSLUCENT`.

## 4. Añadir o retocar un tipo

1. Añade/edita la entrada en `catalog.py` (id, tamaño, frames, `shape`, paleta, render, arquetipo).
2. Registra el id en `ModParticles.java` (`register` o `registerColorable` para tintables) y en `MycelialParticleSpecs` (vida, tamaño, emisor si procede).
3. Ejecuta `--all --check`; revisa `exports/particle_contact_sheet.png` y `particle_gallery.html`.
4. Añade el id a `scripts/codex_studio/content_part1.py` (`registry_refs` + página) y ejecuta `python scripts/codex_studio/build_codex.py --all --check`.
5. Verifica en juego con `runVisualSmoke -PparticleVisualSmoke=true` (galería `particle_gallery_644.png` / `652.png`).

## 5. Emisores y red

- Los emisores se ejecutan en cliente (`MycelialEmissions`) para no multiplicar paquetes; el contrato de `ServerLevel#sendParticles` se conserva: `count=0` = un único emisor y `xd/yd/zd` codifican radio/altura normal/vector de impacto.
- `hive_dust` se envía con `ColorParticleOption.create(ModParticles.HIVE_DUST.get(), argb)` (cepas con `HiveStrain.argbColor()`).

## 6. Verificación obligatoria

- CodeMode: `run_particle_studio`, `run_texture_studio`, `audit_mod_assets` (debe dar `vanilla_particle_hits == 0`, `missing_particle_assets == []`), `run_gradle_task(["build","test","pitest","runGameTestServer"])` y `run_verify_mod(include_server_smoke=True)`.
- GameTest `particleRegistryAssetsAreCompleteAndUnique` (149/149) y `/mycelial verify polish` cubren recursos y registro.
- Capturas: `artifacts/diagnostics/particle-visual-client/screenshots/particle_gallery_*.png`; `check_visual_regression` no debe degradarse sin revisar el diff.
- Al cerrar, anexa la entrada en `MEMORY.md` (append-only) y actualiza las skills afectadas.
