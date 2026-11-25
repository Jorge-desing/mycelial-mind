package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.AntibodyCystBlock;
import com.alex.mycelialmind.block.BioluminescentMycelialCapBlock;
import com.alex.mycelialmind.block.HyphalVeinBlock;
import com.alex.mycelialmind.block.HyphalVeinBlock.VeinConnection;
import com.alex.mycelialmind.block.MycelialTendrilsBlock;
import com.alex.mycelialmind.block.ReclaimedBloomBlock;
import com.alex.mycelialmind.block.RootCapShelfBlock;
import com.alex.mycelialmind.block.RootSapMossBlock;
import com.alex.mycelialmind.block.SporeUpdraftVentBlock;
import com.alex.mycelialmind.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;
import java.util.function.Supplier;

/**
 * Generates all 29 blockstates under {@code assets/mycelialmind/blockstates/*.json}, referencing
 * the curated static/3D block models preserved in {@code src/main/resources/assets/mycelialmind/models/block/}.
 */
public final class MycelialBlockStateProvider extends BlockStateProvider {
    private static final List<String> RECLAIMED_BLOOM_MODELS = List.of(
            "reclaimed_bloom",
            "reclaimed_bloom_melee",
            "reclaimed_bloom_projectile",
            "reclaimed_bloom_fire",
            "reclaimed_bloom_explosion",
            "reclaimed_bloom_magic"
    );

    public MycelialBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MycelialMind.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        registerSimpleExistingBlock(ModBlocks.BIO_CHITIN_LATTICE, "bio_chitin_lattice");
        registerSimpleExistingBlock(ModBlocks.CREEPING_MYCELIUM, "creeping_mycelium");
        registerSimpleExistingBlock(ModBlocks.EPHEMERAL_CHITIN, "ephemeral_chitin");
        registerSimpleExistingBlock(ModBlocks.HIVE_ALTAR, "hive_altar");
        registerSimpleExistingBlock(ModBlocks.INCUBATOR_NODE, "incubator_node");
        registerSimpleExistingBlock(ModBlocks.LUMINESCENT_AMBER_VEIN, "luminescent_amber_vein");
        registerSimpleExistingBlock(ModBlocks.MATRIARCH_ALTAR, "matriarch_altar");
        registerSimpleExistingBlock(ModBlocks.MYCELIAL_ARCH_BRICK, "mycelial_arch_brick");
        registerSimpleExistingBlock(ModBlocks.MYCELIAL_NODE, "mycelial_node");
        registerSimpleExistingBlock(ModBlocks.MYCELIAL_RIFT, "mycelial_rift");
        registerSimpleExistingBlock(ModBlocks.MYCELIAL_STONE, "mycelial_stone");
        registerSimpleExistingBlock(ModBlocks.PURIFICATION_PYLON, "purification_pylon");
        registerSimpleExistingBlock(ModBlocks.PURIFICATION_TURRET, "purification_turret");
        registerSimpleExistingBlock(ModBlocks.REINFORCED_BIO_BARRIER, "reinforced_bio_barrier");
        registerSimpleExistingBlock(ModBlocks.SENTINEL_NODE, "sentinel_node");
        registerSimpleExistingBlock(ModBlocks.SOVEREIGN_NEXUS, "sovereign_nexus");
        registerSimpleExistingBlock(ModBlocks.SPORE_BULB, "spore_bulb");
        registerSimpleExistingBlock(ModBlocks.SPORE_VENT, "spore_vent");
        registerSimpleExistingBlock(ModBlocks.SYMBIOTIC_NODE, "symbiotic_node");
        registerSimpleExistingBlock(ModBlocks.SYNAPTIC_RESEARCH_BENCH, "synaptic_research_bench");

        registerBooleanVariant(ModBlocks.ANTIBODY_CYST.get(), AntibodyCystBlock.ACTIVE,
                "antibody_cyst", "antibody_cyst_withered");
        registerBooleanVariant(ModBlocks.ROOT_SAP_MOSS.get(), RootSapMossBlock.SAP_READY,
                "root_sap_moss", "root_sap_moss_depleted");
        registerBooleanVariant(ModBlocks.SPORE_UPDRAFT_VENT.get(), SporeUpdraftVentBlock.ACTIVE,
                "spore_updraft_vent", "spore_updraft_vent_inactive");

        registerUniformBooleanStates(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get(),
                BioluminescentMycelialCapBlock.MATURE, "bioluminescent_mycelial_cap");
        registerUniformBooleanStates(ModBlocks.MYCELIAL_TENDRILS.get(),
                MycelialTendrilsBlock.MATURE, "mycelial_tendrils");

        registerChitinPillar();
        registerRootCapShelf();
        registerReclaimedBloom();
        registerHyphalVein();
    }

    private void registerSimpleExistingBlock(Supplier<? extends Block> block, String modelName) {
        simpleBlock(block.get(), existingBlockModel(modelName));
    }

    private void registerBooleanVariant(
            Block block,
            BooleanProperty property,
            String trueModelName,
            String falseModelName
    ) {
        ModelFile trueModel = existingBlockModel(trueModelName);
        ModelFile falseModel = existingBlockModel(falseModelName);
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        builder.partialState().with(property, true).modelForState().modelFile(trueModel).addModel();
        builder.partialState().with(property, false).modelForState().modelFile(falseModel).addModel();
    }

    private void registerUniformBooleanStates(Block block, BooleanProperty property, String modelName) {
        ModelFile model = existingBlockModel(modelName);
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        builder.partialState().with(property, true).modelForState().modelFile(model).addModel();
        builder.partialState().with(property, false).modelForState().modelFile(model).addModel();
    }

    private void registerChitinPillar() {
        ModelFile vertical = existingBlockModel("chitin_pillar");
        ModelFile horizontal = existingBlockModel("chitin_pillar_horizontal");
        VariantBlockStateBuilder builder = getVariantBuilder(ModBlocks.CHITIN_PILLAR.get());
        builder.partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.Y)
                .modelForState().modelFile(vertical).addModel();
        builder.partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.Z)
                .modelForState().modelFile(horizontal).rotationX(90).addModel();
        builder.partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.X)
                .modelForState().modelFile(horizontal).rotationX(90).rotationY(90).addModel();
    }

    private void registerRootCapShelf() {
        ModelFile shelf = existingBlockModel("root_cap_shelf");
        VariantBlockStateBuilder builder = getVariantBuilder(ModBlocks.ROOT_CAP_SHELF.get());
        addHorizontalFacing(builder, Direction.NORTH, shelf, 0);
        addHorizontalFacing(builder, Direction.EAST, shelf, 90);
        addHorizontalFacing(builder, Direction.SOUTH, shelf, 180);
        addHorizontalFacing(builder, Direction.WEST, shelf, 270);
    }

    private static void addHorizontalFacing(
            VariantBlockStateBuilder builder,
            Direction facing,
            ModelFile model,
            int rotationY
    ) {
        var modelBuilder = builder.partialState().with(RootCapShelfBlock.FACING, facing)
                .modelForState().modelFile(model);
        if (rotationY != 0) {
            modelBuilder.rotationY(rotationY);
        }
        modelBuilder.addModel();
    }

    private void registerReclaimedBloom() {
        VariantBlockStateBuilder builder = getVariantBuilder(ModBlocks.RECLAIMED_BLOOM.get());
        for (int palette = 0; palette < RECLAIMED_BLOOM_MODELS.size(); palette++) {
            ModelFile model = existingBlockModel(RECLAIMED_BLOOM_MODELS.get(palette));
            for (boolean mature : new boolean[]{true, false}) {
                builder.partialState()
                        .with(ReclaimedBloomBlock.MATURE, mature)
                        .with(ReclaimedBloomBlock.PALETTE, palette)
                        .modelForState().modelFile(model).addModel();
            }
        }
    }

    private void registerHyphalVein() {
        ModelFile dot = existingBlockModel("hyphal_vein_dot");
        ModelFile side = existingBlockModel("hyphal_vein_side");
        ModelFile up = existingBlockModel("hyphal_vein_up");

        MultiPartBlockStateBuilder builder = getMultipartBuilder(ModBlocks.HYPHAL_VEIN.get());
        builder.part().modelFile(dot).addModel().end();

        addHyphalArm(builder, HyphalVeinBlock.NORTH, side, 0, true);
        addHyphalArm(builder, HyphalVeinBlock.EAST, side, 90, true);
        addHyphalArm(builder, HyphalVeinBlock.SOUTH, side, 180, true);
        addHyphalArm(builder, HyphalVeinBlock.WEST, side, 270, true);

        addHyphalArm(builder, HyphalVeinBlock.NORTH, up, 0, false);
        addHyphalArm(builder, HyphalVeinBlock.EAST, up, 90, false);
        addHyphalArm(builder, HyphalVeinBlock.SOUTH, up, 180, false);
        addHyphalArm(builder, HyphalVeinBlock.WEST, up, 270, false);
    }

    private static void addHyphalArm(
            MultiPartBlockStateBuilder builder,
            EnumProperty<VeinConnection> property,
            ModelFile model,
            int rotationY,
            boolean includeSide
    ) {
        var configured = builder.part().modelFile(model);
        if (rotationY != 0) {
            configured.rotationY(rotationY);
        }
        var part = configured.addModel();
        if (includeSide) {
            part.condition(property, VeinConnection.SIDE, VeinConnection.UP);
        } else {
            part.condition(property, VeinConnection.UP);
        }
        part.end();
    }

    private ModelFile existingBlockModel(String name) {
        return models().getExistingFile(modLoc("block/" + name));
    }
}
