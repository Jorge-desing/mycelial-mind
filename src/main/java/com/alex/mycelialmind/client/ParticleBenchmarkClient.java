package com.alex.mycelialmind.client;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Repeatable client-only scenes used by the particle visual smoke task. */
public final class ParticleBenchmarkClient {
    private static final int START_TICK = 80;
    private static final int SEGMENT_TICKS = 80;
    private static final int WARMUP_TICKS = 20;
    private static final List<Long> FRAME_INTERVALS = new ArrayList<>();
    private static int activeSegment = -1;
    private static int ticksInSegment;
    private static long previousFrame;

    private ParticleBenchmarkClient() {}

    static void tick(Minecraft mc, int tick) {
        if (tick == START_TICK - 1) {
            mc.options.enableVsync().set(false);
            mc.options.framerateLimit().set(260);
            mc.options.hideGui = true;
        }
        if (tick < START_TICK) return;
        int segment = (tick - START_TICK) / SEGMENT_TICKS;
        if (segment >= 9) {
            report(mc);
            activeSegment = -1;
            mc.stop();
            return;
        }
        if (segment != activeSegment) {
            report(mc);
            activeSegment = segment;
            mc.particleEngine.setLevel(mc.level);
            FRAME_INTERVALS.clear();
            previousFrame = 0;
        }
        ticksInSegment = (tick - START_TICK) % SEGMENT_TICKS;
        int scene = segment % 3;
        double x = mc.player.getX();
        double y = mc.player.getY() + 1.0D;
        double z = mc.player.getZ();
        if (scene == 0 && ticksInSegment % 10 == 0) {
            mc.particleEngine.createParticle(ModParticles.RECOVERY.get(), x, y, z + 3.0D, 0.65D, 1.8D, 0.0D);
        } else if (scene == 1) {
            for (int i = 0; i < 4; i++) {
                mc.particleEngine.createParticle(ModParticles.IMPACT.get(), x + i - 1.5D, y, z + 3.0D,
                        0.0D, 0.2D, -1.0D);
            }
            for (int i = 0; i < 2; i++) {
                mc.particleEngine.createParticle(ModParticles.PURGE.get(), x + i, y, z + 4.0D,
                        0.0D, 0.0D, 0.0D);
            }
        } else if (scene == 2) {
            RandomSource random = RandomSource.create(481516L + ticksInSegment);
            for (int i = 0; i < 120; i++) {
                mc.particleEngine.createParticle(ModParticles.SPORE_MOTE.get(),
                        x + (random.nextDouble() - 0.5D) * 40.0D,
                        y + random.nextDouble() * 8.0D,
                        z + 17.0D + random.nextDouble() * 28.0D,
                        0.0D, 0.0D, 0.0D);
            }
        }
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        if (activeSegment < 0 || ticksInSegment < WARMUP_TICKS) return;
        long now = System.nanoTime();
        if (previousFrame != 0L) FRAME_INTERVALS.add(now - previousFrame);
        previousFrame = now;
    }

    private static void report(Minecraft mc) {
        if (activeSegment < 0 || FRAME_INTERVALS.isEmpty()) return;
        Collections.sort(FRAME_INTERVALS);
        int size = FRAME_INTERVALS.size();
        double median = FRAME_INTERVALS.get(size / 2) / 1_000_000.0D;
        double p95 = FRAME_INTERVALS.get((int) Math.ceil(size * 0.95D) - 1) / 1_000_000.0D;
        MycelialMind.LOGGER.info("Particle benchmark pass={} scene={} frames={} median_ms={} p95_ms={} live={}",
                activeSegment / 3 + 1, switch (activeSegment % 3) {
                    case 0 -> "healing";
                    case 1 -> "combat";
                    default -> "ambient";
                }, size, median, p95, mc.particleEngine.countParticles());
    }
}
