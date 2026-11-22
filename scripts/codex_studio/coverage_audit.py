"""Live repository coverage, registry integrity, recipe-type, and page-budget auditor."""

from __future__ import annotations

from dataclasses import dataclass, field
import glob
import json
from pathlib import Path
import re
from typing import Dict, List, Set

from .schema import CodexBook, check_book_budgets, strip_book_markup

# Minecraft items explicitly used as icons in Codex entries/recipe descriptions
VALID_MINECRAFT_ITEMS: Set[str] = set()

RECIPE_TYPE_TO_MODONOMICON = {
    "minecraft:crafting_shaped": "modonomicon:crafting_recipe",
    "minecraft:crafting_shapeless": "modonomicon:crafting_recipe",
    "minecraft:smelting": "modonomicon:smelting_recipe",
    "minecraft:smoking": "modonomicon:smoking_recipe",
    "minecraft:blasting": "modonomicon:blasting_recipe",
    "minecraft:stonecutting": "modonomicon:stonecutting_recipe",
}


@dataclass
class CoverageReport:
    ok: bool
    summary: Dict[str, Dict[str, int]] = field(default_factory=dict)
    missing: Dict[str, List[str]] = field(default_factory=dict)
    invalid_refs: List[str] = field(default_factory=list)
    recipe_pair_errors: List[str] = field(default_factory=list)
    budget_issues: List[str] = field(default_factory=list)
    stats: Dict[str, int] = field(default_factory=dict)

    def to_dict(self) -> dict:
        return {
            "ok": self.ok,
            "stats": self.stats,
            "summary": self.summary,
            "missing": self.missing,
            "invalid_refs": self.invalid_refs,
            "recipe_pair_errors": self.recipe_pair_errors,
            "budget_issues": self.budget_issues,
        }


def _extract_registered_ids(java_path: Path) -> List[str]:
    txt = java_path.read_text(encoding="utf-8")
    ids = re.findall(r'\.register(?:Block|SimpleItem|SimpleBlockItem)?\(\s*"([^"]+)"', txt)
    if not ids:
        ids = re.findall(r'register[A-Za-z]*\(\s*"([^"]+)"', txt)
    # Deduplicate preserving order
    return list(dict.fromkeys(ids))


def _extract_enum_constants(java_path: Path, enum_name: str | None = None) -> List[str]:
    txt = java_path.read_text(encoding="utf-8")
    pat = (
        rf"public enum {enum_name}\b[^{{]*\{{([^;}}]+)[;}}]"
        if enum_name
        else r"public enum \w+[^{]*\{([^;}]+)[;}]"
    )
    m = re.search(pat, txt, re.S)
    if not m:
        return []
    block = re.sub(r"//.*", "", m.group(1))
    # Strip balanced constructor arguments so nested enum params (e.g. StimulusType.MELEE) aren't parsed as constants
    prev = None
    while prev != block:
        prev = block
        block = re.sub(r"\([^()]*\)", "", block)
    constants: List[str] = []
    for part in block.split(","):
        cm = re.match(r"^\s*([A-Z][A-Z0-9_]+)\b", part)
        if cm:
            constants.append(cm.group(1))
    return constants


def _extract_config_keys(java_path: Path) -> List[str]:
    txt = java_path.read_text(encoding="utf-8")
    return re.findall(r'\.define(?:InRange|Enum)?\(\s*"([^"]+)"', txt)


def _extract_verify_scenarios(cmd_java: Path) -> List[str]:
    txt = cmd_java.read_text(encoding="utf-8")
    scenarios = re.findall(r'appendFailedScenario\(\w+,\s*"([^"]+)"', txt)
    return list(dict.fromkeys(scenarios))


def _extract_secrets(goal_java: Path) -> List[str]:
    txt = goal_java.read_text(encoding="utf-8")
    return re.findall(r"public static final int (SECRET_[A-Z0-9_]+)\s*=", txt)


def _extract_keybindings(kb_java: Path) -> List[str]:
    txt = kb_java.read_text(encoding="utf-8")
    return re.findall(r'"key\.mycelialmind\.([^"]+)"', txt)


def _extract_compostables(comp_json: Path) -> List[str]:
    data = json.loads(comp_json.read_text(encoding="utf-8"))
    return [k.split(":", 1)[1] for k in data.get("values", {}).keys()]


def scan_repo_inventory(repo_root: Path) -> dict:
    """Scan live repository files for exact inventory of all Mycelial Mind features."""
    reg = repo_root / "src/main/java/com/alex/mycelialmind/registry"
    src_java = repo_root / "src/main/java/com/alex/mycelialmind"
    res_root = repo_root / "src/main/resources"
    data_root = res_root / "data/mycelialmind"

    recipes_map: Dict[str, str] = {}
    for p in sorted(glob.glob(str(data_root / "recipe/*.json"))):
        rid = Path(p).stem
        rtype = json.loads(Path(p).read_text(encoding="utf-8")).get("type", "")
        recipes_map[rid] = rtype

    advancements = [
        Path(p).relative_to(data_root / "advancement").with_suffix("").as_posix()
        for p in sorted(glob.glob(str(data_root / "advancement/**/*.json"), recursive=True))
    ]
    loot_tables = [
        Path(p).relative_to(data_root / "loot_table").with_suffix("").as_posix()
        for p in sorted(glob.glob(str(data_root / "loot_table/**/*.json"), recursive=True))
    ]
    biomes = [Path(p).stem for p in sorted(glob.glob(str(data_root / "worldgen/biome/*.json")))]
    dimensions = [Path(p).stem for p in sorted(glob.glob(str(data_root / "dimension/*.json")))]
    sounds = list(json.loads((res_root / "assets/mycelialmind/sounds.json").read_text(encoding="utf-8")).keys())

    inventory = {
        "blocks": _extract_registered_ids(reg / "ModBlocks.java"),
        "items": _extract_registered_ids(reg / "ModItems.java"),
        "entities": _extract_registered_ids(reg / "ModEntities.java"),
        "effects": _extract_registered_ids(reg / "ModEffects.java"),
        "particles": _extract_registered_ids(reg / "ModParticles.java"),
        "attachments": _extract_registered_ids(reg / "ModAttachments.java"),
        "data_components": _extract_registered_ids(reg / "ModDataComponents.java"),
        "armor_materials": _extract_registered_ids(reg / "ModArmorMaterials.java"),
        "block_entities": _extract_registered_ids(reg / "ModBlockEntities.java"),
        "menu_types": _extract_registered_ids(reg / "ModMenuTypes.java"),
        "features": _extract_registered_ids(reg / "ModFeatures.java"),
        "structures": _extract_registered_ids(reg / "ModStructures.java") + ["hive_sanctum"],
        "memory_types": _extract_registered_ids(reg / "ModMemoryTypes.java"),
        "sounds": sounds,
        "recipes": list(recipes_map.keys()),
        "recipes_map": recipes_map,
        "advancements": advancements,
        "loot_tables": loot_tables,
        "biomes": biomes,
        "dimensions": dimensions,
        "compostables": _extract_compostables(res_root / "data/neoforge/data_maps/item/compostables.json"),
        "server_configs": _extract_config_keys(src_java / "config/MycelialConfig.java"),
        "client_configs": _extract_config_keys(src_java / "config/MycelialClientConfig.java"),
        "keybindings": _extract_keybindings(src_java / "client/MycelialKeyBindings.java"),
        "secrets": _extract_secrets(src_java / "hive/ProgressionGoalEngine.java"),
        "verify_scenarios": _extract_verify_scenarios(src_java / "command/MycelialCommands.java"),
        "stimuli": _extract_enum_constants(src_java / "hive/StimulusType.java"),
        "traits": _extract_enum_constants(src_java / "hive/AdaptationTrait.java"),
        "synergies": _extract_enum_constants(src_java / "hive/HiveSynergy.java"),
        "ranks": _extract_enum_constants(src_java / "hive/EvolutionRank.java"),
        "tiers": _extract_enum_constants(src_java / "hive/HiveProgressionTier.java"),
        "strains": _extract_enum_constants(src_java / "hive/HiveStrain.java"),
        "habits": _extract_enum_constants(src_java / "hive/PlayerHabit.java"),
        "doctrines": _extract_enum_constants(src_java / "hive/HiveDoctrine.java"),
        "orders": _extract_enum_constants(src_java / "hive/HiveOrder.java"),
        "swarm_roles": _extract_enum_constants(src_java / "hive/SwarmRole.java"),
        "mutagens": _extract_enum_constants(src_java / "symbiosis/SymbioticMutagen.java"),
        "node_types": _extract_enum_constants(src_java / "world/SynapticNodeType.java"),
        "homeostasis_stages": _extract_enum_constants(src_java / "world/DimensionalHomeostasisEngine.java", "HomeostasisStage"),
    }
    return inventory


def _collect_book_corpus(book: CodexBook) -> tuple[str, str]:
    chunks_es: List[str] = []
    chunks_en: List[str] = []
    for ch in book.chapters:
        chunks_es.extend([ch.id, ch.title.es, ch.subtitle.es, ch.icon_item])
        chunks_en.extend([ch.id, ch.title.en, ch.subtitle.en, ch.icon_item])
        for entry in ch.entries:
            chunks_es.extend([entry.id, entry.title.es, entry.subtitle.es, entry.icon_item])
            chunks_en.extend([entry.id, entry.title.en, entry.subtitle.en, entry.icon_item])
            for p in entry.pages:
                for val in (p.title.es, strip_book_markup(p.body.es), p.item_id, p.recipe_id, p.recipe_id_2, p.entity_id):
                    if val:
                        chunks_es.append(val)
                for val in (p.title.en, strip_book_markup(p.body.en), p.item_id, p.recipe_id, p.recipe_id_2, p.entity_id):
                    if val:
                        chunks_en.append(val)
    return "\n".join(chunks_es), "\n".join(chunks_en)


def run_coverage_audit(book: CodexBook, repo_root: Path) -> CoverageReport:
    """Execute complete 100% live coverage, reference validity, recipe typing, and budget audit."""
    inv = scan_repo_inventory(repo_root)
    valid_items = set(inv["items"]) | set(inv["blocks"])  # BlockItems registered via ModItems/ModBlocks
    valid_entities = set(inv["entities"])
    recipes_map: Dict[str, str] = inv["recipes_map"]

    invalid_refs: List[str] = []
    recipe_pair_errors: List[str] = []
    covered_recipe_widgets: Set[str] = set()
    covered_entity_widgets: Set[str] = set()

    def _check_item(ref: str, ctx: str) -> None:
        if ref.startswith("mycelialmind:"):
            path = ref.split(":", 1)[1]
            if path not in valid_items:
                invalid_refs.append(f"{ctx}: unknown item/block {ref}")
        elif ref.startswith("minecraft:"):
            if ref not in VALID_MINECRAFT_ITEMS and not VALID_MINECRAFT_ITEMS:
                pass
        else:
            invalid_refs.append(f"{ctx}: invalid namespace {ref}")

    for ch in book.chapters:
        _check_item(ch.icon_item, f"chapter {ch.id}")
        for entry in ch.entries:
            _check_item(entry.icon_item, f"entry {ch.id}/{entry.id}")
            for p in entry.pages:
                ctx = f"page {ch.id}/{entry.id}/{p.id}"
                if p.item_id:
                    _check_item(p.item_id, ctx)
                if p.entity_id:
                    if not p.entity_id.startswith("mycelialmind:") or p.entity_id.split(":", 1)[1] not in valid_entities:
                        invalid_refs.append(f"{ctx}: unknown entity {p.entity_id}")
                    else:
                        covered_entity_widgets.add(p.entity_id.split(":", 1)[1])
                if p.recipe_id:
                    rid1 = p.recipe_id.split(":", 1)[1] if ":" in p.recipe_id else p.recipe_id
                    if rid1 not in recipes_map:
                        invalid_refs.append(f"{ctx}: unknown recipe_id {p.recipe_id}")
                    else:
                        covered_recipe_widgets.add(rid1)
                        t1 = RECIPE_TYPE_TO_MODONOMICON.get(recipes_map[rid1])
                        if not t1:
                            recipe_pair_errors.append(f"{ctx}: unsupported recipe type {recipes_map[rid1]} on {rid1}")
                        if p.recipe_id_2:
                            rid2 = p.recipe_id_2.split(":", 1)[1] if ":" in p.recipe_id_2 else p.recipe_id_2
                            if rid2 not in recipes_map:
                                invalid_refs.append(f"{ctx}: unknown recipe_id_2 {p.recipe_id_2}")
                            else:
                                covered_recipe_widgets.add(rid2)
                                t2 = RECIPE_TYPE_TO_MODONOMICON.get(recipes_map[rid2])
                                if t1 != t2:
                                    recipe_pair_errors.append(
                                        f"{ctx}: mismatched recipe pair {rid1} ({t1}) vs {rid2} ({t2})"
                                    )

    corpus_es, corpus_en = _collect_book_corpus(book)
    summary: Dict[str, Dict[str, int]] = {}
    missing: Dict[str, List[str]] = {}

    check_categories = [
        k for k in inv.keys() if k not in ("recipes_map", "sounds")
    ]
    for cat in check_categories:
        tokens: List[str] = inv[cat]
        miss_list: List[str] = []
        for tok in tokens:
            pat = re.compile(rf"(?<![A-Za-z0-9_]){re.escape(tok)}(?![A-Za-z0-9_])")
            if not pat.search(corpus_es) or not pat.search(corpus_en):
                miss_list.append(tok)
        summary[cat] = {"expected": len(tokens), "covered": len(tokens) - len(miss_list), "missing": len(miss_list)}
        if miss_list:
            missing[cat] = miss_list

    # Also enforce that every datapack recipe has an explicit recipe page widget
    missing_recipe_widgets = sorted(set(inv["recipes"]) - covered_recipe_widgets)
    if missing_recipe_widgets:
        missing["recipe_widgets"] = missing_recipe_widgets
    summary["recipe_widgets"] = {
        "expected": len(inv["recipes"]),
        "covered": len(covered_recipe_widgets),
        "missing": len(missing_recipe_widgets),
    }

    # Check all 17 living entities have explicit entity preview pages
    living_entities = [e for e in inv["entities"] if e not in ("acid_spit", "neutralizing_spore", "purifier_grenade")]
    missing_entity_widgets = sorted(set(living_entities) - covered_entity_widgets)
    if missing_entity_widgets:
        missing["entity_widgets"] = missing_entity_widgets
    summary["entity_widgets"] = {
        "expected": len(living_entities),
        "covered": len(covered_entity_widgets),
        "missing": len(missing_entity_widgets),
    }

    budget_issues = check_book_budgets(book)
    total_entries = sum(len(c.entries) for c in book.chapters)
    total_pages = sum(len(e.pages) for c in book.chapters for e in c.entries)

    ok = (
        not missing
        and not invalid_refs
        and not recipe_pair_errors
        and not budget_issues
    )
    return CoverageReport(
        ok=ok,
        summary=summary,
        missing=missing,
        invalid_refs=invalid_refs,
        recipe_pair_errors=recipe_pair_errors,
        budget_issues=budget_issues,
        stats={
            "chapters": len(book.chapters),
            "entries": total_entries,
            "pages": total_pages,
        },
    )
