"""Root assembler for the complete Mycelial Mind (v2.0.0) Codex."""

from __future__ import annotations

from .content_part1 import build_chapters_1_to_4
from .content_part2 import build_chapters_5_to_8
from .content_part3 import build_chapters_9_to_12
from .helpers import lt
from .schema import CodexBook


def build_codex_book() -> CodexBook:
    """Assemble all 12 chapters of the complete bilingual Mycelial Mind Codex."""
    chapters = (
        *build_chapters_1_to_4(),
        *build_chapters_5_to_8(),
        *build_chapters_9_to_12(),
    )
    return CodexBook(
        id="mycelial_codex",
        title=lt("Códice de la Colmena Viva", "Codex of the Living Hive"),
        subtitle=lt(
            "Enciclopedia completa de Mycelial Mind v2.0.0 (NeoForge 21.1.176)",
            "Complete Encyclopedia of Mycelial Mind v2.0.0 (NeoForge 21.1.176)",
        ),
        tooltip=lt(
            "Manual integral de evolución colmena, IA táctica, bestiario, biotecnología, Homeostasis y comandos.",
            "Comprehensive manual covering Hive evolution, tactical AI, bestiary, biotech, Homeostasis, and commands.",
        ),
        icon_item="mycelialmind:bio_scanner",
        chapters=chapters,
    )
