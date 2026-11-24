package com.alex.mycelialmind.block;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.projectile.AcidSpitProjectile;
import com.alex.mycelialmind.entity.projectile.NeutralizingSporeProjectile;
import com.alex.mycelialmind.entity.projectile.PurifierGrenadeProjectile;
import com.alex.mycelialmind.hive.EcosystemPurposeEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Permeable defensive embrasure lattice: blocks solid entities and hostile Acid Spit,
 * shelters against ceiling acid rain beneath it, and allows purifying/allied projectiles
 * to pass freely through its apertures.
 */
public class BioChitinLatticeBlock extends Block {
    public BioChitinLatticeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        if (context instanceof EntityCollisionContext ecc && ecc.getEntity() instanceof Projectile proj) {
            boolean isPurifying = proj instanceof NeutralizingSporeProjectile
                    || proj instanceof PurifierGrenadeProjectile;
            boolean ownedByHostile = proj instanceof AcidSpitProjectile
                    || (proj.getOwner() instanceof MycelialMob mm && !mm.isAlliedEscort());
            if (EcosystemPurposeEngine.shouldProjectilePassThroughLattice(isPurifying, ownedByHostile)) {
                return Shapes.empty();
            }
        }
        return super.getCollisionShape(state, level, pos, context);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.bio_chitin_lattice").withStyle(ChatFormatting.AQUA));
    }
}
