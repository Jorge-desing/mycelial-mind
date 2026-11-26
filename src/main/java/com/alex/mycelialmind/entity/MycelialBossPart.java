package com.alex.mycelialmind.entity;

import com.alex.mycelialmind.hive.BossCombatArchitectureEngine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * Native NeoForge multi-part sub-hitbox for Mycelial Mind bosses.
 * Supports dorsal weak-point critical hits and armored frontal carapace mitigation.
 */
public final class MycelialBossPart extends PartEntity<MycelialMob> {
    public enum PartRole {
        WEAK_POINT("weak_point", 1.35F),
        ARMORED_CARAPACE("armored_carapace", 0.75F);

        private final String id;
        private final float damageMultiplier;

        PartRole(String id, float damageMultiplier) {
            this.id = id;
            this.damageMultiplier = damageMultiplier;
        }

        public String id() {
            return id;
        }

        public float damageMultiplier() {
            return damageMultiplier;
        }
    }

    private final MycelialMob parentMob;
    private final String partName;
    private final PartRole role;
    private final EntityDimensions size;

    public MycelialBossPart(MycelialMob parentMob, String partName, PartRole role, float width, float height) {
        super(parentMob);
        this.parentMob = parentMob;
        this.partName = partName;
        this.role = role;
        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
    }

    public String getPartName() {
        return partName;
    }

    public PartRole getRole() {
        return role;
    }

    public static float computePartModifiedDamage(
            PartRole role,
            boolean bypassesArmor,
            boolean isMagic,
            float baseAmount
    ) {
        return computePartModifiedDamage(role, false, bypassesArmor, isMagic, baseAmount);
    }

    public static float computePartModifiedDamage(
            PartRole role,
            boolean carapaceBroken,
            boolean bypassesArmor,
            boolean isMagic,
            float baseAmount
    ) {
        return BossCombatArchitectureEngine.computeBossPartDamageWithCarapace(
                role == PartRole.WEAK_POINT,
                role == PartRole.ARMORED_CARAPACE,
                carapaceBroken,
                bypassesArmor,
                isMagic,
                baseAmount
        );
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    public boolean isPickable() {
        return this.parentMob.isAlive() && !this.parentMob.isRemoved();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        boolean carapaceBroken = this.parentMob instanceof MycelialBossMob boss && boss.isCarapaceBroken();
        float modified = computePartModifiedDamage(
                this.role,
                carapaceBroken,
                source.is(DamageTypeTags.BYPASSES_ARMOR),
                source.is(DamageTypeTags.WITCH_RESISTANT_TO),
                amount
        );
        return this.parentMob.hurtFromBossPart(this, source, modified);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return this.parentMob.interact(player, hand);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || this.parentMob == entity;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
