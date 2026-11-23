"""Java EntityModel<T> code emitter for Minecraft 1.21.1 / NeoForge 21.1."""

from __future__ import annotations

from typing import List, Set, Tuple
from .uv_packer import ModelDef, PartNode

# Canonical set of animation roles understood by this emitter (guards typos in model_defs).
ANIM_ROLES = frozenset({
    "head", "jaw", "mandible_r", "mandible_l",
    "biped_leg_r", "biped_leg_l", "biped_arm_r", "biped_arm_l", "biped_shin",
    "smash_arm", "combat_arm_r", "combat_arm_l", "mantis_arm_r", "mantis_arm_l", "forearm",
    "breath_chest", "quad_leg_a", "quad_leg_b",
    "arachnid_coxa_r", "arachnid_coxa_l", "arachnid_tibia",
    "sac_pulse", "piston_pump", "orbit_ring_a", "orbit_ring_b", "orbit_orb",
    "petal_flare", "shield_plate", "rotor_spin", "drill_spin",
    "tendril_sway", "tail_chain", "mantle_wing_r", "mantle_wing_l",
    "float_bob", "leech_wave",
})


def _camel(name: str) -> str:
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def _fmt_f(v: float) -> str:
    return f"{v:.3f}".rstrip("0").rstrip(".") + ("F" if "." in f"{v:.3f}".rstrip("0").rstrip(".") else ".0F")


def _collect_anim_nodes(parts: Tuple[PartNode, ...], parent_var: str, out: List[Tuple[str, PartNode, str]]) -> None:
    for p in parts:
        field_var = _camel(p.name)
        out.append((field_var, p, parent_var))
        _collect_anim_nodes(p.children, f"this.{field_var}", out)


def emit_java_model(model: ModelDef) -> str:
    nodes: List[Tuple[str, PartNode, str]] = []
    _collect_anim_nodes(model.parts, "root", nodes)

    # Ensure unique field names
    seen: Set[str] = set()
    field_decls: List[str] = []
    field_inits: List[str] = []
    root_children: List[str] = []

    def walk_init(parts: Tuple[PartNode, ...], parent_expr: str) -> None:
        for p in parts:
            var = _camel(p.name)
            if var in seen:
                var = f"{var}_{len(seen)}"
            seen.add(var)
            field_decls.append(f"    private final ModelPart {var};")
            field_inits.append(f'        this.{var} = {parent_expr}.getChild("{p.name}");')
            if parent_expr == "root":
                root_children.append(var)
            walk_init(p.children, f"this.{var}")

    walk_init(model.parts, "root")

    builder_lines: List[str] = []

    def walk_build(parts: Tuple[PartNode, ...], parent_def: str, indent: int = 8) -> None:
        sp = " " * indent
        for idx, p in enumerate(parts):
            def_var = f"part_{_camel(p.name)}_{indent}_{idx}"
            px, py, pz = p.pivot
            rx, ry, rz = p.rotation
            if rx == 0.0 and ry == 0.0 and rz == 0.0:
                pose = f"PartPose.offset({_fmt_f(px)}, {_fmt_f(py)}, {_fmt_f(pz)})"
            else:
                pose = (
                    f"PartPose.offsetAndRotation({_fmt_f(px)}, {_fmt_f(py)}, {_fmt_f(pz)}, "
                    f"{_fmt_f(rx)}, {_fmt_f(ry)}, {_fmt_f(rz)})"
                )
            cubes_expr = "CubeListBuilder.create()"
            cur_mirror = False
            for c in p.cubes:
                ox, oy, oz = c.origin
                w, h, d = c.size
                u, v = c.uv
                mirror_cmd = ""
                if c.mirror != cur_mirror:
                    mirror_cmd = f".mirror({'true' if c.mirror else 'false'})"
                    cur_mirror = c.mirror
                if c.inflation != 0.0:
                    cubes_expr += (
                        f"\n{sp}        {mirror_cmd}.texOffs({u}, {v}).addBox({_fmt_f(ox)}, {_fmt_f(oy)}, {_fmt_f(oz)}, "
                        f"{w}.0F, {h}.0F, {d}.0F, new CubeDeformation({_fmt_f(c.inflation)}))"
                    )
                else:
                    cubes_expr += (
                        f"\n{sp}        {mirror_cmd}.texOffs({u}, {v}).addBox({_fmt_f(ox)}, {_fmt_f(oy)}, {_fmt_f(oz)}, "
                        f"{w}.0F, {h}.0F, {d}.0F)"
                    )
            if p.children:
                builder_lines.append(f'{sp}PartDefinition {def_var} = {parent_def}.addOrReplaceChild("{p.name}",')
                builder_lines.append(f"{sp}        {cubes_expr},")
                builder_lines.append(f"{sp}        {pose});")
                walk_build(p.children, def_var, indent)
            else:
                builder_lines.append(f'{sp}{parent_def}.addOrReplaceChild("{p.name}",')
                builder_lines.append(f"{sp}        {cubes_expr},")
                builder_lines.append(f"{sp}        {pose});")

    walk_build(model.parts, "root")

    roles_present = {p.anim_role for _, p, _ in nodes}
    uses_spring = bool(roles_present & {"sac_pulse", "tendril_sway", "float_bob", "orbit_orb", "tail_chain", "mantle_wing_r", "mantle_wing_l"})
    uses_ik = "arachnid_tibia" in roles_present
    uses_rank = any(p.rank_min is not None for _, p, _ in nodes)
    uses_gene = any(p.gene_match is not None for _, p, _ in nodes)
    uses_phase = any(p.phase_min is not None for _, p, _ in nodes) or bool(roles_present & {"petal_flare", "shield_plate"})
    uses_combat = bool(roles_present & {
        "jaw", "mandible_r", "mandible_l", "biped_arm_r", "biped_arm_l",
        "smash_arm", "combat_arm_r", "combat_arm_l", "mantis_arm_r", "mantis_arm_l", "forearm",
        "piston_pump"
    })
    uses_aggro = bool(roles_present & {"combat_arm_r", "combat_arm_l", "mantis_arm_r", "mantis_arm_l"})

    anim_lines: List[str] = []
    if uses_rank:
        anim_lines.append("        boolean isMutatedOrHigher = false;")
        anim_lines.append("        boolean isEliteRank = false;")
        anim_lines.append("        if (entity instanceof com.alex.mycelialmind.entity.MycelialMob mycelialMob) {")
        anim_lines.append("            var rank = mycelialMob.getEvolutionRank();")
        anim_lines.append("            isMutatedOrHigher = rank != com.alex.mycelialmind.hive.EvolutionRank.BASE;")
        anim_lines.append("            isEliteRank = rank == com.alex.mycelialmind.hive.EvolutionRank.ELITE;")
        anim_lines.append("        } else if (entity instanceof com.alex.mycelialmind.entity.MycelialChimeraEntity chimeraRank) {")
        anim_lines.append("            var rank = chimeraRank.getEvolutionRank();")
        anim_lines.append("            isMutatedOrHigher = rank != com.alex.mycelialmind.hive.EvolutionRank.BASE;")
        anim_lines.append("            isEliteRank = rank == com.alex.mycelialmind.hive.EvolutionRank.ELITE;")
        anim_lines.append("        }")
    if uses_gene:
        anim_lines.append("        com.alex.mycelialmind.entity.MycelialChimeraEntity chimera = entity instanceof com.alex.mycelialmind.entity.MycelialChimeraEntity c ? c : null;")
    if uses_phase:
        anim_lines.append("        int bossPhase = 1;")
        if model.registry_id == "hive_core":
            anim_lines.append("        if (entity instanceof com.alex.mycelialmind.entity.HiveCoreBossEntity hiveBoss && hiveBoss.isShieldedPhaseTwo()) {")
            anim_lines.append("            bossPhase = 2;")
            anim_lines.append("        }")
        elif model.registry_id == "apex_matriarch":
            anim_lines.append("        if (entity instanceof com.alex.mycelialmind.entity.ApexMatriarchEntity matriarch) {")
            anim_lines.append("            bossPhase = matriarch.getMatriarchPhase();")
            anim_lines.append("        }")
    if uses_combat:
        anim_lines.append("        float attackSwing = Mth.sin(this.attackTime * (float) Math.PI);")
    if uses_aggro:
        anim_lines.append("        boolean aggressive = entity instanceof net.minecraft.world.entity.Mob mobEntity && mobEntity.isAggressive();")
        anim_lines.append("        float aggro = this.combatPose.sample(entity, aggressive, ageInTicks);")
    if uses_spring:
        anim_lines.append("        float[] sp = this.springState.computeIfAbsent(entity, e -> new float[]{0.0F, 0.0F, e.getYRot(), 0.0F, 0.0F, ageInTicks});")
        anim_lines.append("        float dt = Mth.clamp((ageInTicks - sp[5]) * 0.05F, 0.0F, 0.10F);")
        anim_lines.append("        sp[5] = ageInTicks;")
        anim_lines.append("        float dYaw = Mth.wrapDegrees(entity.getYRot() - sp[2]);")
        anim_lines.append("        if (dt > 0.0F) sp[2] = entity.getYRot();")
        anim_lines.append("        float dVert = (float) entity.getDeltaMovement().y;")
        anim_lines.append("        float yawGoal = Mth.clamp(dYaw * 0.016F, -0.42F, 0.42F);")
        anim_lines.append("        float pitchGoal = Mth.clamp(dVert * 0.38F, -0.42F, 0.42F);")
        anim_lines.append("        stepSpringDamperExact(sp, 0, yawGoal, 0.18F, dt);")
        anim_lines.append("        stepSpringDamperExact(sp, 3, pitchGoal, 0.22F, dt);")
        anim_lines.append("        float jiggle = damperExact(sp[0] + sp[3] * 0.6F, sp[0] + sp[3] * 0.6F, 0.08F, dt);")

    for var, p, _ in nodes:
        if p.rank_min == "MUTATED":
            anim_lines.append(f"        this.{var}.visible = isMutatedOrHigher;")
        elif p.rank_min == "ELITE":
            anim_lines.append(f"        this.{var}.visible = isEliteRank;")
        if p.gene_match is not None:
            anim_lines.append(f"        this.{var}.visible = chimera != null && chimera.hasGene(com.alex.mycelialmind.hive.AdaptationTrait.{p.gene_match});")
        if p.phase_min is not None:
            anim_lines.append(f"        this.{var}.visible = bossPhase >= {p.phase_min};")

        role = p.anim_role
        ph = _fmt_f(p.anim_phase)
        amp = _fmt_f(p.anim_amp)
        if role == "head":
            anim_lines.append(f"        this.{var}.yRot = netHeadYaw * ((float) Math.PI / 180F);")
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} + headPitch * ((float) Math.PI / 180F);")
        elif role == "jaw":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} + 0.08F + Mth.sin(ageInTicks * 0.16F + {ph}) * 0.12F + attackSwing * 0.42F;")
        elif role == "mandible_r":
            anim_lines.append(f"        this.{var}.yRot = {_fmt_f(p.rotation[1])} - (Mth.sin(ageInTicks * 0.18F + {ph}) * 0.14F + attackSwing * 0.45F);")
        elif role == "mandible_l":
            anim_lines.append(f"        this.{var}.yRot = {_fmt_f(p.rotation[1])} + (Mth.sin(ageInTicks * 0.18F + {ph}) * 0.14F + attackSwing * 0.45F);")
        elif role == "biped_leg_r":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} + Mth.cos(limbSwing * 0.6662F) * 1.1F * limbSwingAmount * {amp};")
        elif role == "biped_leg_l":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} + Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.1F * limbSwingAmount * {amp};")
        elif role == "biped_arm_r":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} + Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.85F * limbSwingAmount * {amp} - attackSwing * 1.15F;")
        elif role == "biped_arm_l":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} + Mth.cos(limbSwing * 0.6662F) * 0.85F * limbSwingAmount * {amp} - attackSwing * 0.65F;")
        elif role == "smash_arm":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} + Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.05F * limbSwingAmount - 0.18F + Mth.sin(ageInTicks * 0.09F + {ph}) * 0.08F - attackSwing * 1.45F;")
        elif role == "combat_arm_r":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} - aggro * 0.35F + Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.65F * limbSwingAmount + Mth.sin(ageInTicks * 0.09F + {ph}) * 0.07F - attackSwing * 1.35F;")
        elif role == "combat_arm_l":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} - aggro * 0.25F + Mth.cos(limbSwing * 0.6662F) * 0.55F * limbSwingAmount - Mth.sin(ageInTicks * 0.09F + {ph}) * 0.07F;")
            anim_lines.append(f"        this.{var}.yRot = {_fmt_f(p.rotation[1])};")
        elif role == "mantis_arm_r":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} - aggro * 0.22F + Mth.sin(ageInTicks * 0.12F + {ph}) * 0.14F - attackSwing * 1.25F;")
            anim_lines.append(f"        this.{var}.zRot = {_fmt_f(p.rotation[2])} + aggro * 0.18F + 0.06F;")
        elif role == "mantis_arm_l":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} - aggro * 0.22F + Mth.cos(ageInTicks * 0.12F + {ph}) * 0.14F - attackSwing * 1.25F;")
            anim_lines.append(f"        this.{var}.zRot = {_fmt_f(p.rotation[2])} - aggro * 0.18F - 0.06F;")
        elif role == "forearm":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} - 0.35F + Mth.sin(ageInTicks * 0.10F + {ph}) * 0.12F + attackSwing * 0.55F;")
        elif role == "biped_shin":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} + Math.max(0.0F, Mth.sin(limbSwing * 0.6662F + {ph})) * 0.35F * limbSwingAmount;")
        elif role == "breath_chest":
            anim_lines.append(f"        float br_{var} = 1.0F + Mth.sin(ageInTicks * 0.09F + {ph}) * 0.025F * {amp};")
            anim_lines.append(f"        this.{var}.xScale = br_{var};")
            anim_lines.append(f"        this.{var}.zScale = br_{var};")
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} + Mth.sin(ageInTicks * 0.09F + {ph}) * 0.03F;")
        elif role == "quad_leg_a":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} + Mth.cos(limbSwing * 0.6662F + {ph}) * 0.9F * limbSwingAmount * {amp};")
        elif role == "quad_leg_b":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} + Mth.cos(limbSwing * 0.6662F + (float) Math.PI + {ph}) * 0.9F * limbSwingAmount * {amp};")
        elif role == "arachnid_coxa_r":
            anim_lines.append(f"        float gait_{var} = Mth.cos(limbSwing * 0.85F + {ph});")
            anim_lines.append(f"        this.{var}.yRot = {_fmt_f(p.rotation[1])} + gait_{var} * 0.38F * limbSwingAmount;")
            anim_lines.append(f"        this.{var}.zRot = {_fmt_f(p.rotation[2])} - Math.max(0.0F, Mth.sin(limbSwing * 0.85F + {ph})) * 0.24F * limbSwingAmount;")
        elif role == "arachnid_coxa_l":
            anim_lines.append(f"        float gait_{var} = Mth.cos(limbSwing * 0.85F + {ph});")
            anim_lines.append(f"        this.{var}.yRot = {_fmt_f(p.rotation[1])} - gait_{var} * 0.38F * limbSwingAmount;")
            anim_lines.append(f"        this.{var}.zRot = {_fmt_f(p.rotation[2])} + Math.max(0.0F, Mth.sin(limbSwing * 0.85F + {ph})) * 0.24F * limbSwingAmount;")
        elif role == "arachnid_tibia":
            anim_lines.append(f"        float stepLift_{var} = Math.max(0.0F, Mth.sin(limbSwing * 0.85F + {ph})) * 1.45F * limbSwingAmount;")
            anim_lines.append(f"        float idleFlex_{var} = Mth.sin(ageInTicks * 0.08F + {ph}) * 0.35F;")
            anim_lines.append(f"        float ikReach_{var} = 6.2F + Mth.cos(limbSwing * 0.85F + {ph}) * 1.10F * limbSwingAmount + idleFlex_{var};")
            anim_lines.append(f"        float[] fabrik_{var} = solveFabrikChainAngles(ikReach_{var}, -1.5F + stepLift_{var}, new float[]{{4.5F, 5.5F}}, 4);")
            anim_lines.append(f"        this.{var}.zRot = {_fmt_f(p.rotation[2])} + (fabrik_{var}[1] - 1.15F) * 0.38F;")
        elif role == "piston_pump":
            anim_lines.append(f"        float piston_{var} = Mth.sin(ageInTicks * 0.22F + {ph}) * 0.06F * {amp};")
            anim_lines.append(f"        this.{var}.xScale = 1.0F + piston_{var} + attackSwing * 0.22F;")
            anim_lines.append(f"        this.{var}.yScale = 1.0F - piston_{var} * 0.82F + attackSwing * 0.10F;")
        elif role == "sac_pulse":
            anim_lines.append(f"        float pulse_{var} = 1.0F + Mth.sin(ageInTicks * 0.14F + {ph}) * 0.08F * {amp} + jiggle * 0.10F;")
            anim_lines.append(f"        this.{var}.xScale = pulse_{var};")
            anim_lines.append(f"        this.{var}.yScale = pulse_{var};")
            anim_lines.append(f"        this.{var}.zScale = pulse_{var};")
            anim_lines.append(f"        this.{var}.zRot = {_fmt_f(p.rotation[2])} + jiggle * 0.18F;")
        elif role == "orbit_ring_a":
            anim_lines.append(f"        this.{var}.yRot = ageInTicks * 0.08F * {amp};")
            anim_lines.append(f"        this.{var}.xRot = 0.35F + Mth.sin(ageInTicks * 0.05F + {ph}) * 0.15F;")
        elif role == "orbit_ring_b":
            anim_lines.append(f"        this.{var}.yRot = -ageInTicks * 0.07F * {amp};")
            anim_lines.append(f"        this.{var}.zRot = 0.45F + Mth.cos(ageInTicks * 0.05F + {ph}) * 0.15F;")
        elif role == "orbit_orb":
            anim_lines.append(f"        this.{var}.y = {_fmt_f(p.pivot[1])} + Mth.sin(ageInTicks * 0.16F + {ph}) * 1.4F * {amp} + jiggle * 1.2F;")
            anim_lines.append(f"        this.{var}.yRot = ageInTicks * 0.12F + {ph};")
        elif role == "petal_flare":
            anim_lines.append(f"        float flare_{var} = (bossPhase >= 2 ? 0.42F : 0.0F) + (bossPhase >= 3 ? 0.24F : 0.0F);")
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} - flare_{var} + Mth.sin(ageInTicks * 0.10F + {ph}) * 0.08F * {amp};")
        elif role == "shield_plate":
            anim_lines.append(f"        float pFlare_{var} = (bossPhase >= 2 ? 0.48F : 0.0F);")
            anim_lines.append(f"        this.{var}.zRot = {_fmt_f(p.rotation[2])} + pFlare_{var} * {amp} + Mth.cos(ageInTicks * 0.12F + {ph}) * 0.06F;")
        elif role == "rotor_spin":
            anim_lines.append(f"        this.{var}.yRot = ageInTicks * 0.65F * {amp} + {ph};")
        elif role == "drill_spin":
            anim_lines.append(f"        this.{var}.zRot = (ageInTicks * 0.45F + limbSwing * 0.35F) * {amp} + {ph};")
        elif role == "tendril_sway":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} + Mth.sin(ageInTicks * 0.12F + {ph}) * 0.25F * {amp} + jiggle * 0.25F;")
            anim_lines.append(f"        this.{var}.zRot = {_fmt_f(p.rotation[2])} + Mth.cos(ageInTicks * 0.10F + {ph}) * 0.15F * {amp} - jiggle * 0.20F;")
        elif role == "tail_chain":
            anim_lines.append(f"        this.{var}.xRot = {_fmt_f(p.rotation[0])} + Mth.sin(ageInTicks * 0.14F + {ph}) * 0.18F * {amp} + jiggle * 0.20F;")
            anim_lines.append(f"        this.{var}.yRot = {_fmt_f(p.rotation[1])} + Mth.cos(ageInTicks * 0.11F + limbSwing * 0.5F + {ph}) * 0.22F * {amp};")
        elif role == "mantle_wing_r":
            anim_lines.append(f"        this.{var}.zRot = {_fmt_f(p.rotation[2])} + Mth.sin(ageInTicks * 0.09F + {ph}) * 0.16F * {amp} + jiggle * 0.15F;")
        elif role == "mantle_wing_l":
            anim_lines.append(f"        this.{var}.zRot = {_fmt_f(p.rotation[2])} - Mth.sin(ageInTicks * 0.09F + {ph}) * 0.16F * {amp} - jiggle * 0.15F;")
        elif role == "float_bob":
            anim_lines.append(f"        this.{var}.y = {_fmt_f(p.pivot[1])} + Mth.sin(ageInTicks * 0.10F + {ph}) * 1.2F * {amp} + jiggle * 1.4F;")
        elif role == "leech_wave":
            anim_lines.append(f"        this.{var}.yRot = Mth.sin(ageInTicks * 0.18F + limbSwing * 0.7F + {ph}) * 0.22F * {amp};")

    if model.registry_id == "spore_behemoth":
        anim_lines.append("        if (entity instanceof com.alex.mycelialmind.entity.MycelialBossMob boss) {")
        anim_lines.append("            float bossProg = boss.getStateProgress();")
        anim_lines.append("            switch (boss.getCombatState()) {")
        anim_lines.append("                case WINDUP -> {")
        anim_lines.append("                    this.torso.xRot -= 0.22F * bossProg;")
        anim_lines.append("                    this.armR.xRot -= 1.05F * bossProg;")
        anim_lines.append("                    this.armL.xRot -= 0.85F * bossProg;")
        anim_lines.append("                }")
        anim_lines.append("                case ACTIVE -> {")
        anim_lines.append("                    this.torso.xRot += 0.28F;")
        anim_lines.append("                    this.armR.xRot += 0.85F;")
        anim_lines.append("                    this.armL.xRot += 0.65F;")
        anim_lines.append("                }")
        anim_lines.append("                case STAGGERED -> {")
        anim_lines.append("                    this.torso.xRot += 0.42F;")
        anim_lines.append("                    this.head.xRot += 0.35F;")
        anim_lines.append("                    this.armR.xRot += 0.32F;")
        anim_lines.append("                    this.armL.xRot += 0.32F;")
        anim_lines.append("                }")
        anim_lines.append("                default -> {")
        anim_lines.append("                    if (boss.isCarapaceBroken()) {")
        anim_lines.append("                        this.torso.xRot += 0.24F;")
        anim_lines.append("                    }")
        anim_lines.append("                }")
        anim_lines.append("            }")
        anim_lines.append("        }")
    elif model.registry_id == "hive_core":
        anim_lines.append("        if (entity instanceof com.alex.mycelialmind.entity.MycelialBossMob boss) {")
        anim_lines.append("            float bossProg = boss.getStateProgress();")
        anim_lines.append("            switch (boss.getCombatState()) {")
        anim_lines.append("                case WINDUP -> {")
        anim_lines.append("                    this.ringA.yRot += bossProg * 1.40F;")
        anim_lines.append("                    this.ringB.yRot -= bossProg * 1.40F;")
        anim_lines.append("                    this.crownChandelier.y -= 1.8F * bossProg;")
        anim_lines.append("                }")
        anim_lines.append("                case ACTIVE -> {")
        anim_lines.append("                    this.ringA.yRot += 1.65F;")
        anim_lines.append("                    this.ringB.yRot -= 1.65F;")
        anim_lines.append("                    this.coreBody.xRot += 0.16F;")
        anim_lines.append("                }")
        anim_lines.append("                case STAGGERED -> {")
        anim_lines.append("                    this.coreBody.xRot += 0.38F;")
        anim_lines.append("                    this.crownChandelier.y += 2.2F;")
        anim_lines.append("                }")
        anim_lines.append("                default -> {")
        anim_lines.append("                    if (boss.isCarapaceBroken()) {")
        anim_lines.append("                        this.coreBody.xRot += 0.22F;")
        anim_lines.append("                    }")
        anim_lines.append("                }")
        anim_lines.append("            }")
        anim_lines.append("        }")
    elif model.registry_id == "apex_matriarch":
        anim_lines.append("        if (entity instanceof com.alex.mycelialmind.entity.MycelialBossMob boss) {")
        anim_lines.append("            float bossProg = boss.getStateProgress();")
        anim_lines.append("            switch (boss.getCombatState()) {")
        anim_lines.append("                case WINDUP -> {")
        anim_lines.append("                    this.body.xRot -= 0.18F * bossProg;")
        anim_lines.append("                    this.upperArmR.xRot -= 1.15F * bossProg;")
        anim_lines.append("                    this.upperArmL.xRot -= 1.15F * bossProg;")
        anim_lines.append("                }")
        anim_lines.append("                case ACTIVE -> {")
        anim_lines.append("                    this.body.xRot += 0.24F;")
        anim_lines.append("                    this.upperArmR.xRot += 0.90F;")
        anim_lines.append("                    this.upperArmL.xRot += 0.90F;")
        anim_lines.append("                }")
        anim_lines.append("                case STAGGERED -> {")
        anim_lines.append("                    this.body.xRot += 0.40F;")
        anim_lines.append("                    this.head.xRot += 0.32F;")
        anim_lines.append("                    this.upperArmR.xRot += 0.35F;")
        anim_lines.append("                    this.upperArmL.xRot += 0.35F;")
        anim_lines.append("                }")
        anim_lines.append("                default -> {")
        anim_lines.append("                    if (boss.isCarapaceBroken()) {")
        anim_lines.append("                        this.body.xRot += 0.22F;")
        anim_lines.append("                    }")
        anim_lines.append("                }")
        anim_lines.append("            }")
        anim_lines.append("        }")

    if model.registry_id == "synaptic_mimic":
        anim_lines.append("        if (entity instanceof com.alex.mycelialmind.entity.SynapticMimicEntity mimic) {")
        anim_lines.append("            if (!mimic.isUnmasked()) {")
        anim_lines.append("                this.head.xRot += 0.14F;")
        anim_lines.append("                this.eliteEchoHalo.visible = false;")
        anim_lines.append("            }")
        anim_lines.append("            switch (mimic.getMimicStance()) {")
        anim_lines.append("                case BLADE_DANCER -> this.armR.xRot -= 0.42F;")
        anim_lines.append("                case SPORE_MARKSMAN -> {")
        anim_lines.append("                    this.armR.xRot -= 0.65F;")
        anim_lines.append("                    this.armL.xRot -= 0.35F;")
        anim_lines.append("                }")
        anim_lines.append("                case AEGIS_MIRROR -> {")
        anim_lines.append("                    this.armL.xRot -= 0.75F;")
        anim_lines.append("                    this.armL.yRot = 0.52F;")
        anim_lines.append("                }")
        anim_lines.append("            }")
        anim_lines.append("        }")
    elif model.registry_id == "colony_symbiont":
        anim_lines.append("        if (entity instanceof com.alex.mycelialmind.entity.ColonySymbiontEntity symbiont) {")
        anim_lines.append("            var caste = symbiont.getCaste();")
        anim_lines.append("            this.casteHarvesterScythe.visible = (caste == com.alex.mycelialmind.colony.SymbiontCaste.HARVESTER);")
        anim_lines.append("            this.casteExcavatorPick.visible = (caste == com.alex.mycelialmind.colony.SymbiontCaste.EXCAVATOR);")
        anim_lines.append("            this.casteCourierPack.visible = (caste == com.alex.mycelialmind.colony.SymbiontCaste.COURIER);")
        anim_lines.append("            this.casteArchitectHammer.visible = (caste == com.alex.mycelialmind.colony.SymbiontCaste.ARCHITECT);")
        anim_lines.append("            this.casteSentinelShield.visible = (caste == com.alex.mycelialmind.colony.SymbiontCaste.SENTINEL);")
        anim_lines.append("            if (symbiont.isResting() || symbiont.isStandby()) {")
        anim_lines.append("                this.head.xRot += 0.18F;")
        anim_lines.append("            }")
        anim_lines.append("        }")

    spring_import = "import java.util.Map;\nimport java.util.WeakHashMap;\n" if uses_spring else ""
    spring_field = "    private final Map<Entity, float[]> springState = new WeakHashMap<>();\n" if uses_spring else ""
    combat_field = "    private final CombatPoseTransition<Entity> combatPose = new CombatPoseTransition<>();\n" if uses_aggro else ""
    spring_helper = (
        "\n    private static float damperExact(float x, float goal, float halflife, float dt) {\n"
        "        float y = (4.0F * 0.6931472F) / Math.max(1.0E-4F, halflife);\n"
        "        float ydt = y * dt;\n"
        "        float e = 1.0F / (1.0F + ydt + 0.48F * ydt * ydt + 0.235F * ydt * ydt * ydt);\n"
        "        return goal + (x - goal) * e;\n"
        "    }\n\n"
        "    private static void stepSpringDamperExact(float[] state, int offset, float targetGoal, float halflife, float dt) {\n"
        "        float y = (4.0F * 0.6931472F) / Math.max(1.0E-4F, halflife);\n"
        "        float ydt = y * dt;\n"
        "        float e = 1.0F / (1.0F + ydt + 0.48F * ydt * ydt + 0.235F * ydt * ydt * ydt);\n"
        "        float j0 = state[offset] - targetGoal;\n"
        "        float j1 = state[offset + 1] + j0 * y;\n"
        "        state[offset] = Mth.clamp(e * (j0 + j1 * dt) + targetGoal, -0.45F, 0.45F);\n"
        "        state[offset + 1] = e * (state[offset + 1] - j1 * y * dt);\n"
        "    }\n"
        if uses_spring
        else ""
    )
    ik_helper = (
        "\n    private static float solveTwoBoneIK(float targetDist, float l1, float l2) {\n"
        "        float clamped = Mth.clamp(targetDist, Math.abs(l1 - l2) + 0.05F, l1 + l2 - 0.05F);\n"
        "        float cosAngle = (l1 * l1 + l2 * l2 - clamped * clamped) / (2.0F * l1 * l2);\n"
        "        return (float) Math.acos(Mth.clamp(cosAngle, -1.0F, 1.0F));\n"
        "    }\n\n"
        "    private static float[] solveFabrikChainAngles(float targetX, float targetY, float[] lengths, int iterations) {\n"
        "        int n = lengths.length;\n"
        "        float[] px = new float[n + 1];\n"
        "        float[] py = new float[n + 1];\n"
        "        for (int i = 0; i < n; i++) {\n"
        "            px[i + 1] = px[i] + lengths[i];\n"
        "            py[i + 1] = 0.0F;\n"
        "        }\n"
        "        for (int iter = 0; iter < Math.max(1, iterations); iter++) {\n"
        "            px[n] = targetX;\n"
        "            py[n] = targetY;\n"
        "            for (int i = n - 1; i >= 0; i--) {\n"
        "                float dx = px[i] - px[i + 1];\n"
        "                float dy = py[i] - py[i + 1];\n"
        "                float d = Math.max(1.0E-4F, (float) Math.sqrt(dx * dx + dy * dy));\n"
        "                px[i] = px[i + 1] + (dx / d) * lengths[i];\n"
        "                py[i] = py[i + 1] + (dy / d) * lengths[i];\n"
        "            }\n"
        "            px[0] = 0.0F;\n"
        "            py[0] = 0.0F;\n"
        "            for (int i = 0; i < n; i++) {\n"
        "                float dx = px[i + 1] - px[i];\n"
        "                float dy = py[i + 1] - py[i];\n"
        "                float d = Math.max(1.0E-4F, (float) Math.sqrt(dx * dx + dy * dy));\n"
        "                px[i + 1] = px[i] + (dx / d) * lengths[i];\n"
        "                py[i + 1] = py[i] + (dy / d) * lengths[i];\n"
        "            }\n"
        "        }\n"
        "        float[] angles = new float[n];\n"
        "        for (int i = 0; i < n; i++) {\n"
        "            angles[i] = (float) Math.atan2(py[i + 1] - py[i], px[i + 1] - px[i]);\n"
        "        }\n"
        "        return angles;\n"
        "    }\n"
        if uses_ik
        else ""
    )

    tex_w, tex_h = model.tex_size
    render_roots = "\n".join(
        f"        this.{r}.render(poseStack, buffer, packedLight, packedOverlay, color);" for r in root_children
    )

    return f'''package com.alex.mycelialmind.client.model;

import com.alex.mycelialmind.MycelialMind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
{spring_import}import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Generated by scripts/model_studio: {model.description}
 */
public class {model.class_name}<T extends Entity> extends EntityModel<T> {{
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "{model.registry_id}"), "main");

{chr(10).join(field_decls)}
{spring_field}{combat_field}
    public {model.class_name}(ModelPart root) {{
{chr(10).join(field_inits)}
    }}

    public static LayerDefinition createBodyLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

{chr(10).join(builder_lines)}

        return LayerDefinition.create(mesh, {tex_w}, {tex_h});
    }}

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {{
{chr(10).join(anim_lines)}
    }}
{spring_helper}{ik_helper}
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {{
{render_roots}
    }}
}}
'''
