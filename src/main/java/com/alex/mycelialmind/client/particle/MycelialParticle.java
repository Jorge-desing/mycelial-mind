package com.alex.mycelialmind.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Spec-driven particle used by every Mycelial Mind silhouette.
 *
 * <p>Movement, sprite animation, size curve, tint and light behaviour all come
 * from a {@link ParticleSpec}. The tuned scale contract from earlier passes is
 * preserved: the quad shrinks 100% -> 80% over its life and the near-camera
 * factor can only decrease (never regrow).</p>
 */
public class MycelialParticle extends TextureSheetParticle {
    private final ParticleSpec spec;
    private final SpriteSet sprites;
    private final float originalSize;
    private final double baseXd;
    private final double baseYd;
    private final double baseZd;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final float phase;
    private final int framePhase;
    private final float tintJitter;
    private float proximityScale = 1.0F;
    private float tintRed;
    private float tintGreen;
    private float tintBlue;
    private net.minecraft.client.renderer.texture.TextureAtlasSprite animationSprite;
    private int delayTicks;
    private boolean bounced;

    public MycelialParticle(ClientLevel level, double x, double y, double z,
                            double xd, double yd, double zd,
                            SpriteSet sprites, ParticleSpec spec) {
        super(level, x, y, z);
        this.spec = spec;
        this.sprites = sprites;
        this.lifetime = spec.lifetime(this.random);
        this.originalSize = spec.baseSize();
        this.quadSize = spec.baseSize();
        this.baseXd = xd;
        this.baseYd = yd;
        this.baseZd = zd;
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        this.phase = this.random.nextFloat() * Mth.TWO_PI;
        this.framePhase = spec.frames() <= 1 ? 0 : this.random.nextInt(spec.frames());
        this.tintJitter = 0.90F + this.random.nextFloat() * 0.20F;
        this.tintRed = spec.tintRed();
        this.tintGreen = spec.tintGreen();
        this.tintBlue = spec.tintBlue();
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.setSprite(sprites.get(this.random));
        this.applyTint(1.0F);
        this.alpha = this.delayTicks > 0 ? 0.0F : 1.0F;
    }

    public MycelialParticle tinted(float red, float green, float blue) {
        this.tintRed = red;
        this.tintGreen = green;
        this.tintBlue = blue;
        this.applyTint(1.0F);
        return this;
    }

    public MycelialParticle delayed(int ticks) {
        this.delayTicks = Math.max(0, ticks);
        this.alpha = 0.0F;
        return this;
    }

    private void applyTint(float brightness) {
        this.setColor(this.tintRed * this.tintJitter * brightness,
                this.tintGreen * this.tintJitter * brightness,
                this.tintBlue * this.tintJitter * brightness);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return MycelialParticleRenderTypes.resolve(this.spec.style());
    }

    @Override
    public float getQuadSize(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null) {
            return super.getQuadSize(partialTick);
        }
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        double distance = camera.distanceToSqr(this.x, this.y, this.z);
        float current = (float) Mth.clamp(distance / 2.25D, 0.35F, 1.0F);
        this.proximityScale = Math.min(this.proximityScale, current);
        return super.getQuadSize(partialTick) * this.proximityScale;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return this.spec.fullbright() ? LightTexture.FULL_BRIGHT : super.getLightColor(partialTick);
    }

    @Override
    public void tick() {
        if (this.delayTicks > 0) {
            this.delayTicks--;
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            this.alpha = 0.0F;
            return;
        }
        this.oRoll = this.roll;
        this.roll += this.spec.spin();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        float progress = (float) this.age / (float) this.lifetime;
        this.updateMotion(progress);
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        this.quadSize = this.originalSize * Mth.lerp(progress, this.spec.sizeStart(), this.spec.sizeEnd());
        this.updateAlpha(progress);
        this.updateSprite();
        this.applyTint(1.0F - 0.15F * progress);
    }

    private void updateMotion(float progress) {
        switch (this.spec.archetype()) {
            case DRIFT -> {
                double wobble = this.age * 0.18D + this.phase;
                this.xd = this.baseXd * 0.92D + Math.sin(wobble) * 0.0022D;
                this.zd = this.baseZd * 0.92D + Math.cos(wobble) * 0.0022D;
                this.yd = this.baseYd * 0.50D + 0.0045D;
            }
            case RISE -> {
                this.xd = this.baseXd * 0.40D;
                this.zd = this.baseZd * 0.40D;
                this.yd = Math.min(0.050D, this.baseYd * 0.50D + this.age * 0.0012D);
            }
            case ORBIT -> {
                double angle = this.phase + this.age * 0.14D;
                double radius = 0.18D + 0.10D * Math.sin(this.phase * 1.7D);
                this.xd = (this.centerX + Math.cos(angle) * radius - this.x) * 0.35D;
                this.zd = (this.centerZ + Math.sin(angle) * radius - this.z) * 0.35D;
                this.yd = 0.004D + Math.sin(this.age * 0.20D) * 0.002D;
            }
            case VORTEX -> {
                double dx = this.x - this.centerX;
                double dz = this.z - this.centerZ;
                double radius = Math.max(0.05D, Math.sqrt(dx * dx + dz * dz));
                this.xd = (-dz / radius) * 0.024D + (-dx / radius) * 0.006D;
                this.zd = (dx / radius) * 0.024D + (-dz / radius) * 0.006D;
                this.yd = 0.012D;
            }
            case BURST -> {
                float sign = progress < 0.55F ? 1.0F : -1.6F;
                float damp = 1.0F - progress * 0.5F;
                this.xd = this.baseXd * sign * damp;
                this.zd = this.baseZd * sign * damp;
                this.yd = this.baseYd * sign * damp + 0.0035D;
            }
            case SPARK -> {
                float decay = 1.0F - progress * 0.5F;
                this.xd = this.baseXd * decay + Math.sin((this.age + this.phase) * 0.9D) * 0.004D;
                this.zd = this.baseZd * decay + Math.cos((this.age + this.phase) * 1.1D) * 0.004D;
                this.yd = this.baseYd * decay + Math.sin(this.age * 0.7D) * 0.002D;
            }
            case RING -> {
                this.xd *= 0.45D;
                this.yd *= 0.45D;
                this.zd *= 0.45D;
            }
            case GLYPH -> {
                this.xd = this.baseXd * 0.30D + Math.sin(this.age * 0.20D + this.phase) * 0.0015D;
                this.zd = this.baseZd * 0.30D + Math.cos(this.age * 0.20D + this.phase) * 0.0015D;
                this.yd = 0.010D;
            }
            case MIST -> {
                this.xd = this.baseXd * 0.30D;
                this.zd = this.baseZd * 0.30D;
                this.yd = 0.003D;
            }
            case ARC, TUMBLE -> {
                this.yd -= 0.010D * this.spec.gravity();
                this.xd *= 0.985D;
                this.zd *= 0.985D;
                this.checkGroundBounce();
            }
            case FALL -> {
                this.yd = -0.018D - 0.004D * Math.sin(this.age * 0.4D + this.phase);
                this.xd = this.baseXd * 0.35D + Math.sin(this.age * 0.3D + this.phase) * 0.003D;
                this.zd = this.baseZd * 0.35D + Math.cos(this.age * 0.3D + this.phase) * 0.003D;
                if (this.isOnGround()) {
                    this.remove();
                }
            }
        }
    }

    private void checkGroundBounce() {
        if (!this.isOnGround()) {
            return;
        }
        if (this.bounced) {
            this.remove();
            return;
        }
        this.bounced = true;
        this.yd = Math.abs(this.yd) * 0.32D;
        this.xd *= 0.70D;
        this.zd *= 0.70D;
    }

    private boolean isOnGround() {
        BlockPos below = BlockPos.containing(this.x, this.y - 0.06D, this.z);
        return !this.level.getBlockState(below).getCollisionShape(this.level, below).isEmpty();
    }

    private void updateAlpha(float progress) {
        float fadeIn = Math.min(1.0F, progress / 0.12F);
        float fadeOut = Math.min(1.0F, (1.0F - progress) / 0.30F);
        this.alpha = Math.max(0.0F, Math.min(fadeIn, fadeOut));
    }

    private void updateSprite() {
        if (this.spec.frames() > 1) {
            int animAge = Math.min(this.lifetime - 1, this.age + this.framePhase);
            var nextSprite = this.sprites.get(animAge, this.lifetime);
            if (nextSprite != this.animationSprite) {
                this.animationSprite = nextSprite;
                this.setSprite(nextSprite);
            }
        }
    }
}
