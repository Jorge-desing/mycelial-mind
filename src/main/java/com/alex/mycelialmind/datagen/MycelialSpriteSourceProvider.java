package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SpriteSourceProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Generates {@code assets/minecraft/atlases/blocks.json} so 3D voxel textures under
 * {@code assets/mycelialmind/textures/model_atlas/} are stitched into the block atlas.
 */
public final class MycelialSpriteSourceProvider extends SpriteSourceProvider {
    public MycelialSpriteSourceProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, MycelialMind.MODID, existingFileHelper);
    }

    @Override
    protected void gather() {
        atlas(BLOCKS_ATLAS).addSource(new DirectoryLister("model_atlas", "model_atlas/"));
    }
}
