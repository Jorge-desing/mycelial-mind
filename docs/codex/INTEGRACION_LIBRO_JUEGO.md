# Guía de Integración del Códice al Juego (`Mycelial Mind v2.0.0`)

Este documento explica cómo integrar dentro de **Minecraft 1.21.1 / NeoForge 21.1.267** el Códice generado por [`scripts/codex_studio/`](../../scripts/codex_studio/build_codex.py), preservando la arquitectura aislada actual (`213/213` `@GameTest`, `179/179` JUnit y `2356/2356` claves de idioma intactas) y ofreciendo **3 vías listas para usar** (la **Vía 1** nativa ya está integrada en el mod con `MycelialCodexItem` y `MycelialCodexBookScreen`).

---

## Resumen de Artefactos Generados

| Artefacto | Ruta | Contenido |
| --- | --- | --- |
| Enciclopedia Maestra Markdown | [`docs/codex/CODICE_MICELIAL.md`](CODICE_MICELIAL.md) | 12 capítulos, 62 entradas, 256 páginas etiquetadas (`[Libro: Cap.X / id / Pág.Y]`), spoilers en `<details>` |
| Exportación Nativa Standalone | [`docs/codex/book_export/native/`](book_export/native/data/mycelialmind/codex/codex_book.json) | `codex_book.json` + `es_es.codex.json` / `en_us.codex.json` (100% simétricos) |
| Exportación Modonomicon `1.123.1` | [`docs/codex/book_export/modonomicon/`](book_export/modonomicon/data/mycelialmind/modonomicon/books/mycelial_codex/book.json) | `book.json`, 12 categorías, 62 entradas `(x, y)` sin colisiones, recetas tipadas, entidades 3D y `*.modonomicon.json` |
| Libros Escritos Vanilla | [`docs/codex/book_export/vanilla_written_books/`](book_export/vanilla_written_books/give_commands_es.txt) | Volúmenes `es_es` (`73, 77, 77, 81` págs.) y `en_us` (`71, 72, 91, 56` págs.), estrictos `<= 100` págs/tomo |
| Reporte de Auditoría Live | [`docs/codex/book_export/coverage_report.json`](book_export/coverage_report.json) | Verificación automática 100% contra registros, configuraciones, recetas, botines y comandos |

---

## Vía 1 — Libro Nativo Standalone (Recomendada, Cero Dependencias Externas)

Esta vía añade el ítem físico **`mycelialmind:mycelial_codex`** con pantalla GUI propia de doble página, aprovechando que las **256 páginas** ya están medidas al píxel (`136px` de ancho por columna, `<= 14` líneas para texto y `<= 7` líneas bajo widgets).

### 1.1 Recursos e Idioma
1. Copiar `docs/codex/book_export/native/data/mycelialmind/codex/codex_book.json` a `src/main/resources/data/mycelialmind/codex/codex_book.json`.
2. Fusionar todas las claves de `docs/codex/book_export/native/assets/mycelialmind/lang/es_es.codex.json` y `en_us.codex.json` dentro de `src/main/resources/assets/mycelialmind/lang/es_es.json` y `en_us.json` respectivamente (verificando paridad exacta con las claves existentes).

### 1.2 Registro del Ítem y Receta
1. En `ModItems.java`:
   ```java
   public static final DeferredItem<Item> MYCELIAL_CODEX = ITEMS.register("mycelial_codex",
           () -> new MycelialCodexItem(new Item.Properties().stacksTo(1)));
   ```
2. Añadir `ModItems.MYCELIAL_CODEX.get()` en `ModCreativeTabs.java` (`mycelial_tab`).
3. Añadir receta shapeless sugerida (`src/generated/resources/data/mycelialmind/recipe/mycelial_codex.json`, emitida vía DataGen): `minecraft:book` + `mycelialmind:latent_spore` + `mycelialmind:mycelial_biomass` (y opcionalmente entregar 1 ejemplar al entrar por primera vez usando un flag booleano persistido en `PlayerDiscoveryData` o enlace desde `BioScannerCodexScreen`).

### 1.3 Carga de Datos y Apertura en Cliente
- **Opción simple en cliente:** cargar `ResourceLocation.fromNamespaceAndPath("mycelialmind", "codex/codex_book.json")` o leer desde `AddReloadListenerEvent` (`SimpleJsonResourceReloadListener`) sincronizado en login.
- **Puente Seguro Server/Client:** al hacer clic derecho en `MycelialCodexItem.use(...)`, invocar un puente aislado `ClientCodexOpener.openBookCodex()` (siguiendo el mismo patrón de `BioScannerItem` + `ClientCodexOpener.openBioCodex()` para no cargar `Screen` en servidores dedicados).

### 1.4 Pantalla `MycelialCodexBookScreen` y Desbloqueo Progresivo (`unlock_gate`)
Cada capítulo y entrada exportada incluye `"unlock_gate"` e `"is_spoiler"`:
- `"always"`: desbloqueado desde el inicio (Cap. 1–12 base, Pistas de Secretos).
- `"act_2"` / `"act_3"`: desbloquear cuando `ClientHiveCache.getThreatLevel() >= 25.0` o `>= 50.0` (`HiveProgressionTier`).
- `"deep_mycelium"`: desbloquear cuando `ClientHiveCache.isDeepMyceliumBreached()` sea `true`.
- `"secret_hint"` vs `"secret_spoiler"` (`is_spoiler: true`): mostrar pistas siempre; ocultar soluciones exactas bajo botón explícito `[Revelar Spoiler]` o si el jugador ya descubrió el secreto en `ClientDiscoveryCache`.
- `"admin"`: pestaña técnica (`/mycelial` + `verify` + `config`), visible siempre en modo Creativo o con botón `[Modo Técnico]`.

Renderizadores por `"type"` dentro de `codex_book.json`:
- `"text"` / `"callout"`: título en `title_key`, cuerpo en `text_key` (en `"callout"`, dibujar marco con `accent_color`).
- `"spotlight"`: `graphics.renderItem(BuiltInRegistries.ITEM.get(itemId).getDefaultInstance(), x, y)` arriba + texto abajo (`<= 7` líneas).
- `"recipe"`: consultar `Minecraft.getInstance().level.getRecipeManager().byKey(ResourceLocation.parse(recipeId))` y dibujar cuadrícula `3×3` o icono de horno/cortapiedras.
- `"entity"`: instanciar entidad vía `EntityType.byString(entityId)` y dibujar vista previa 3D rotatoria con `InventoryScreen.renderEntityInInventoryFollowsMouse(...)`.

### 1.5 Arte Visual y Sonido del Libro
Generar textura HD PBR y modelo 3D opcional del libro con los estudios del proyecto vía `CodeMode`:
- `scripts/texture_studio/` (textura `item/mycelial_codex.png` + mapas `_n`/`_s` para Iris).
- `scripts/sound_studio/` (`book_open`, `book_page_turn`).

---

## Vía 2 — Integración Modonomicon (`1.123.1`, Soporte Opcional ya Preparado)

El proyecto ya incluye `compileOnly "com.klikli_dev:modonomicon-1.21.1-neoforge:1.123.1"` en `build.gradle`, `MycelialModonomiconCompat.CODEX_BOOK_ID` (`mycelialmind:mycelial_codex`) y el esqueleto `src/main/resources/data/mycelialmind/modonomicon/books/mycelial_codex/book.json`.

### Pasos para activarlo
1. Copiar `docs/codex/book_export/modonomicon/data/mycelialmind/modonomicon/books/mycelial_codex/` (incluyendo `book.json`, las **12** carpetas/archivos en `categories/` y las **62** entradas en `entries/<categoria>/`) dentro de `src/main/resources/data/mycelialmind/modonomicon/books/mycelial_codex/`.
2. Fusionar `docs/codex/book_export/modonomicon/assets/mycelialmind/lang/{es_es,en_us}.modonomicon.json` dentro de los archivos de idioma del mod.
3. Si deseas que Modonomicon genere automáticamente un ítem físico de libro en `mycelial_tab`, cambia en `book.json`:
   ```json
   "generate_book_item": true
   ```
   O mantén `"generate_book_item": false` y abre el libro programáticamente desde `MycelialCodexItem` cuando `MycelialModonomiconCompat.isModonomiconLoaded()` sea `true`, con fallback a `MycelialCodexBookScreen` (Vía 1) en instalaciones sin Modonomicon.

> **Compatibilidad verificada contra bytecode de `modonomicon-1.21.1-neoforge-1.123.1.jar`:** cada pareja de recetas compartidas en una sola página utiliza estrictamente el mismo tipo (`modonomicon:crafting_recipe`, `smelting_recipe`, `smoking_recipe`, `blasting_recipe` o `stonecutting_recipe`), y todas las coordenadas `(x, y)` de nodos padre/hijo dentro de cada categoría son únicas sin solapamiento.

---

## Vía 3 — Libros Escritos Vanilla (`written_book` y Atriles sin Cambios Java)

Ideal para mapas de aventura, paquetes de datos o servidores Vanilla+ sin añadir nuevas clases GUI:
1. Cada volumen en [`docs/codex/book_export/vanilla_written_books/`](book_export/vanilla_written_books/) cumple los límites estrictos de **Minecraft 1.21.1** (`written_book_content`):
   - **Máximo 100 páginas por volumen** (`es_es`: 4 tomos de `73, 77, 77, 81` págs.; `en_us`: 4 tomos de `71, 72, 91, 56` págs.).
   - **Máximo 14 líneas por página** medidas a **`114px`** de ancho de fuente Vanilla y **<= 250 caracteres** por página.
   - Títulos de tomo **<= 32 caracteres**.
2. Entrega en juego:
   - Copia los comandos de `give_commands_es.txt` (o `give_commands_en.txt`) dentro de un **Bloque de Comandos** (`command_block`) en juego para evitar el límite de 256 caracteres del chat de jugador.
   - O incrusta el campo `"component"` de `vol_01.json`..`vol_04.json` en una tabla de botín usando la función `minecraft:set_components` sobre `minecraft:written_book`.

---

## Regeneración y Auditoría Continua del Códice

Cuando el mod agregue nuevos bloques, ítems, entidades, configuraciones, recetas, sonidos o secretos en el futuro, actualiza el capítulo correspondiente en `scripts/codex_studio/content_part*.py` y ejecuta:

```powershell
# Reexportar los 4 formatos y validar cobertura al 100%, IDs, coordenadas y presupuesto de líneas/píxeles
scripts\mcp\.venv\Scripts\python.exe scripts\codex_studio\build_codex.py --all --check

# Ejecutar la suite automatizada de Codex Studio
scripts\mcp\.venv\Scripts\python.exe -m unittest discover -s scripts/codex_studio/tests -v
```

Si algún registro nuevo en Java o datapack falta en el texto de las páginas, `build_codex.py --check` fallará inmediatamente listando el símbolo exacto faltante y la ubicación en [`coverage_report.json`](book_export/coverage_report.json).
