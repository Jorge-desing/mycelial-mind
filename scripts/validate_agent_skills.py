"""Validate all workspace .agents/skills/*/SKILL.md against Open Agent Skills conventions."""

from __future__ import annotations

import os
import re
import sys

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SKILLS_DIR = os.path.join(REPO_ROOT, ".agents", "skills")
NAME_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")


def parse_frontmatter(text: str) -> dict[str, str]:
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        raise ValueError("Missing opening YAML frontmatter '---'")
    end_idx = None
    for i in range(1, len(lines)):
        if lines[i].strip() == "---":
            end_idx = i
            break
    if end_idx is None:
        raise ValueError("Missing closing YAML frontmatter '---'")

    data: dict[str, str] = {}
    current_key: str | None = None
    buf: list[str] = []
    for line in lines[1:end_idx]:
        if ":" in line and not line.startswith((" ", "\t")):
            if current_key is not None:
                data[current_key] = " ".join(buf).strip()
            k, v = line.split(":", 1)
            current_key = k.strip()
            v_clean = v.strip()
            buf = [] if v_clean in (">-", "|", ">", "|-") else [v_clean]
        elif current_key is not None:
            buf.append(line.strip())
    if current_key is not None:
        data[current_key] = " ".join(buf).strip()
    return data


def validate_all() -> int:
    if not os.path.isdir(SKILLS_DIR):
        print(f"ERROR: Missing skills dir: {SKILLS_DIR}")
        return 1
    entries = sorted(
        d for d in os.listdir(SKILLS_DIR)
        if os.path.isdir(os.path.join(SKILLS_DIR, d))
    )
    if not entries:
        print("ERROR: No skill folders found.")
        return 1

    for folder in entries:
        skill_md = os.path.join(SKILLS_DIR, folder, "SKILL.md")
        if not os.path.isfile(skill_md):
            raise AssertionError(f"Missing SKILL.md in {folder}")
        with open(skill_md, "r", encoding="utf-8") as f:
            content = f.read()
        fm = parse_frontmatter(content)
        name = fm.get("name", "")
        desc = fm.get("description", "")
        assert NAME_RE.match(name), f"{skill_md}: invalid name '{name}'"
        assert name == folder, f"{skill_md}: name '{name}' != folder '{folder}'"
        assert len(desc) >= 15, f"{skill_md}: description too short"
        print(f"OK: .agents/skills/{folder}/SKILL.md (name={name})")
    print(f"Validated {len(entries)} Open Agent Skills successfully.")
    return 0


if __name__ == "__main__":
    sys.exit(validate_all())
