package com.alex.mycelialmind.client.particle;

import com.alex.mycelialmind.config.MycelialClientConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

/**
 * Additive glow render pass for emissive particles.
 *
 * <p>NeoForge sorts non-vanilla render types after every vanilla one, so this
 * single pass renders last and {@code ParticleEngine} restores
 * {@code depthMask(true)} / {@code disableBlend()} afterwards. Glow is disabled
 * by the client config to fall back to the vanilla translucent sheet.</p>
 */
public final class MycelialParticleRenderTypes {
    public static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "MYCELIAL_ADDITIVE";
        }
    };

    public static ParticleRenderType resolve(ParticleSpec.Style style) {
        if (style == ParticleSpec.Style.ADDITIVE && MycelialClientConfig.enableAdditiveParticleGlow()) {
            return ADDITIVE;
        }
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    private MycelialParticleRenderTypes() {}
}
