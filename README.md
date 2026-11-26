# Mycelial Mind

[![Verify Mycelial Mind](https://github.com/Jorge-desing/mycelial-mind/actions/workflows/verify.yml/badge.svg)](https://github.com/Jorge-desing/mycelial-mind/actions/workflows/verify.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![NeoForge 21.1.267](https://img.shields.io/badge/NeoForge-21.1.267-orange)

**Mycelial Mind** (`mycelialmind`) es un mod para **Minecraft 1.21.1** (**NeoForge 21.1.267**) sobre un organismo micelial subterráneo adaptativo que observa tus tácticas de combate y defensa, aprende de ellas en tiempo real y hace evolucionar a sus criaturas, su red neuronal y su territorio.

> **Versión actual:** `2.0.0` · **Minecraft:** `1.21.1` · **Loader:** NeoForge `21.1.267` · **Java:** `21` · **Dependencias externas:** Ninguna (motor nativo `HiveBrain` integrado) · **Licencia:** `MIT` · **Autor:** `Alex`

---

## Descripción general

A diferencia de los enemigos estáticos tradicionales, la **Mente Colmena** (*Hive Mind*) registra cómo luchas en cada dimensión (cuerpo a cuerpo, proyectiles, fuego, explosiones o magia) y responde de forma global:

- **Memoria, Evolución y Adaptación Dinámica:** Cada tipo de daño alimenta los estímulos de la Colmena y eleva el nivel de amenaza (`0–100%`), progresando por 4 actos (`DORMANT`, `AWAKENED`, `ADAPTIVE`, `OVERMIND`). Las criaturas desbloquean rasgos defensivos de **Nivel 1**, comportamientos tácticos de **Nivel 2**, **sinergias de élite** y contra-adaptaciones a hábitos del jugador (`HabitTelemetryTracker`), con memoria volátil en nodos enemigos purgable en `VolatileEngramItem` y señuelos/injertos pavlovianos (`FalseEngramInjectorItem`).
- **IA Táctica, Campo Vivo y Motor Nativo `HiveBrain`:** Las unidades coordinan escuadrones mediante doctrinas (`HiveDoctrine`), mapa de influencia `16x16` (`HiveInfluenceMap`), árbol reactivo `gdxAI` (`HiveTacticalTree`), flujo `FastNoiseLite`, evaluación de utilidad, y perfiles nativos `HiveBrain` sin Access Transformers ni Mixins (sensores de entidades/jugadores/proyectiles/rutas inalcanzables, compuestos secuenciales/paralelos/temporizados y comportamientos tácticos en mobs, jefes y aliados), terraformación efímera, memoria de zonas letales y necrófagos de campo.
- **Guerra Civil Sináptica, Diplomacia Química, Simbiosis y Bio-Forja:** Las cepas rivalizan bajo cisma (`ORTHODOX`/`AMBER`/`CRIMSON` con daño de proyectiles ácidos entre cepas rivales), el jugador gestiona reputación (`PlayerDiplomacyData`), escoltas temporales (`QuorumSealItem` / `AlliedEscortSavedData`), mutágenos simbióticos con atributos sincronizados (`SymbiosisAttributeSync`), injertos de nodos `SYMBIOTIC` (`SynapticGraftItem`) y crianza de `MycelialChimeraEntity` (`ChimeraOwnershipSavedData`).
- **Evolución de Unidades:** Las criaturas pueden nacer o ser promovidas en vivo a rangos `BASE`, `MUTATED` y `ELITE`, con cambios de escala, estadísticas, efectos visuales y botín adaptativo multi-pool.
- **Red Sináptica y Territorio:** El micelio se propaga y se articula mediante nodos interconectados (`mycelial_node`, `incubator_node`, `sentinel_node`) que refuerzan a las tropas y maduran de forma autónoma.
- **Marea Fúngica y Jefes:** Al alcanzar niveles altos de amenaza se desencadena la **Marea Fúngica**, acompañada por el mini-jefe **Spore Behemoth** y los encuentros de jefe contra el **Hive Core** y la **Apex Matriarch** (cuya derrota activa **Remisión Post-Matriarca** entre dimensiones).
- **Contraofensiva, Biotecnología y Arsénico/Arsenal:**
  - **Contención y defensa:** Protege tu base con **Pilones de Purificación** (`purification_pylon`), **Torretas de Purificación** mejorables (`Tier 1 -> 3`) y **Drones Purificadores** aliados (`purifier_drone`).
  - **Análisis y Códice:** Usa el **Bio-Escáner** (`bio_scanner`, con visión sináptica 3D) para consultar el HUD, inspeccionar bloques/entidades y abrir el Códice con radar territorial 2D, mapa de calor homeostático y compatibilidad opcional JEI/EMI/Jade.
  - **Arsenal evolutivo:** Extrae muestras genéticas con `bio_extractor`, sintetiza en la **Mesa de Investigación Sináptica** (`synaptic_research_bench`), evoluciona la **Armadura de Quitina** (`BASE -> MUTATED -> ELITE`) y empuna `symbiotic_blade`, `enzyme_lance`, `neutralizing_spore_launcher`, `purifier_grenade` y `synaptic_suppressor`.
- **Exploración y Homeostasis Dimensional (`mycelialmind:deep_mycelium`):** Descubre brotes miceliales, colmenas de superficie y el santuario `hive_sanctum` en el Overworld, y cruza mediante `mycelial_rift` a `deep_mycelium`, una dimensión viva con homeostasis por chunk (`STABLE`/`IRRITATED`/`IMMUNE_RESPONSE`), sensibilización antigénica, difusión inflamatoria, lluvia ácida enzimática, `AntibodyCystBlock` y patrullas leucocitarias.
- **Idiomas:** Localización completa bilingüe (`en_us` / `es_es`, 2305 claves sincronizadas por idioma), logros y recetas integradas.

---

## Requisitos

- **Minecraft:** `1.21.1` (`[1.21.1, 1.22)`)
- **NeoForge:** `21.1.176` o superior compatible dentro de `[21.1.0,)`
- **Java:** JDK `21` (para compilación y entorno de desarrollo)

---

## Compatibilidad opcional con otros mods

Versiones probadas en servidor con Minecraft 1.21.1 y NeoForge 21.1.267: Curios 9.6.0, Create 6.0.10 y Farmer’s Delight 1.3.4.

- **Curios API** permite equipar el bioescáner en el cinturón para consultar el HUD y abrir el Códice con `K`. El pulso y la inspección mediante clic derecho requieren sostener el escáner.
- **Create** permite cortar piedra micelial en ladrillos de arco y placas de quitina en celosías bioquitinosas en la sierra mecánica, siempre que su opción de recetas de cortapiedras esté activada.
- **Farmer’s Delight** permite cortar un zarcillo micelial con un cuchillo en la tabla de cortar para obtener una biomasa micelial.

### Optional mod compatibility

Server-tested versions on Minecraft 1.21.1 and NeoForge 21.1.267: Curios 9.6.0, Create 6.0.10 and Farmer’s Delight 1.3.4.

- **Curios API** lets you equip the bio-scanner in the belt slot to view its HUD and open the Codex with `K`. Hold the scanner to use its pulse and right-click inspections.
- **Create** can cut mycelial stone into arch bricks and chitin plates into bio-chitin lattice on the mechanical saw when stonecutting recipes are enabled in Create's configuration.
- **Farmer’s Delight** lets you cut a mycelial tendril with a knife on the cutting board to obtain one mycelial biomass.

---

## Compilación y ejecución (Desarrollo)

Desde la raíz del proyecto:

```powershell
# Compilar el mod
.\gradlew.bat build

# Lanzar cliente de desarrollo
.\gradlew.bat runClient

# Lanzar servidor dedicado de pruebas
.\gradlew.bat runServer

# Ejecutar pruebas automatizadas (@GameTest)
.\gradlew.bat runGameTestServer

# Ejecutar pruebas unitarias y de mutación
.\gradlew.bat test pitest

# Pipeline completo (build, datagen, GameTests, smoke RCON y regresión visual)
.\scripts\Verify-Mod.ps1 -IncludeServerSmoke -IncludeVisualSmoke
```

El paquete listo para distribución se genera bajo [`dist/`](dist/mycelialmind-2.0.0-mc1.21.1-neoforge/) (`mycelialmind-2.0.0.jar` standalone sin dependencias externas obligatorias, hash SHA-256 verificado en `INSTALACION.txt` y empaquetado en `dist/mycelialmind-2.0.0-mc1.21.1-neoforge.zip`).

Dentro del juego (con permisos de operador) están disponibles comandos de diagnóstico y prueba bajo `/mycelial` (`status`, `reset`, `stimulus`, `symbiosis`, `diplomacy`, `schism`, `habits`, `homeostasis`, `verify`, `worldgen audit`).

---

## Documentación técnica del repositorio

- [AGENTS.md](AGENTS.md) — Arquitectura detallada, inventario de contenido y reglas del proyecto.
- [MEMORY.md](MEMORY.md) — Bitácora cronológica de desarrollo y verificaciones (`append-only`).
- [docs/codex/CODICE_MICELIAL.md](docs/codex/CODICE_MICELIAL.md) — Códice enciclopédico completo de **Mycelial Mind v2.0.0** (12 capítulos, 62 entradas, 254 páginas).
- [docs/codex/INTEGRACION_LIBRO_JUEGO.md](docs/codex/INTEGRACION_LIBRO_JUEGO.md) — Guía de integración del libro en Minecraft (`Native`, `Modonomicon 1.123.0` y `Vanilla written_book`).
- [scripts/mcp/README.md](scripts/mcp/README.md) — Documentación del servidor MCP `mycelial-mind-dev` (FastMCP `CodeMode`) para auditoría, Gradle, JFR, MC Pilot y control en vivo.
