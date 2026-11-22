package com.alex.mycelialmind.world;

import com.alex.mycelialmind.world.noise.FastNoiseLite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Deterministic FastNoiseLite (Auburn/FastNoiseLite, MIT) wrapper for
 * Cellular (Worley F1 / Distance2Sub) and Domain-Warped OpenSimplex2 noise.
 */
public final class MycelialFastNoise {
    private MycelialFastNoise() {
    }

    public record CellularSample(double f1, double edge) {
    }

    private static FastNoiseLite createCellular(long seed, double warpAmp) {
        int s = (int) (seed ^ (seed >>> 32));
        FastNoiseLite fnl = new FastNoiseLite(s);
        fnl.SetNoiseType(FastNoiseLite.NoiseType.Cellular);
        fnl.SetFrequency(1.0F);
        fnl.SetCellularDistanceFunction(FastNoiseLite.CellularDistanceFunction.EuclideanSq);
        fnl.SetCellularReturnType(FastNoiseLite.CellularReturnType.Distance);
        if (warpAmp > 0.0D) {
            fnl.SetDomainWarpType(FastNoiseLite.DomainWarpType.OpenSimplex2);
            fnl.SetDomainWarpAmp((float) warpAmp);
        }
        return fnl;
    }

    public static CellularSample cellular2D(double x, double z, long seed) {
        return domainWarpedCellular2D(x, z, seed, 0.0D);
    }

    public static CellularSample domainWarpedCellular2D(double x, double z, long seed, double warpAmp) {
        FastNoiseLite.Vector2 pos = new FastNoiseLite.Vector2((float) x, (float) z);
        FastNoiseLite f1Gen = createCellular(seed, warpAmp);
        if (warpAmp > 0.0D) {
            f1Gen.DomainWarp(pos);
        }
        // FastNoiseLite Cellular returns [-1, 0] or [0, 1] depending on return type; normalize to [0, 1]
        double f1Raw = f1Gen.GetNoise(pos.x, pos.y);
        double f1Norm = Mth.clamp((f1Raw + 1.0D) * 0.5D, 0.0D, 1.0D);

        FastNoiseLite edgeGen = createCellular(seed, 0.0D);
        edgeGen.SetCellularReturnType(FastNoiseLite.CellularReturnType.Distance2Sub);
        double d2Sub = edgeGen.GetNoise(pos.x, pos.y);
        double edgeNorm = Mth.clamp(1.0D - Math.abs(d2Sub + 1.0D), 0.0D, 1.0D);
        return new CellularSample(f1Norm, edgeNorm);
    }

    private static final ThreadLocal<FastNoiseLite> FLOW_NOISE = ThreadLocal.withInitial(() -> {
        FastNoiseLite fnl = new FastNoiseLite(0);
        fnl.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        fnl.SetFractalType(FastNoiseLite.FractalType.FBm);
        fnl.SetFractalOctaves(2);
        fnl.SetFrequency(0.065F);
        return fnl;
    });

    /**
     * Smooth, deterministic horizontal flow vector in [-1, 1] using OpenSimplex2 FBm
     * for organic swarm flanking, patrol spread, and spore weaving.
     */
    public static Vec3 organicFlow2D(double x, double z, long seed, double timeSeconds) {
        FastNoiseLite fnl = FLOW_NOISE.get();
        fnl.SetSeed((int) (seed ^ (seed >>> 32)));
        float phase = (float) (timeSeconds * 0.35D);
        float fx = fnl.GetNoise((float) x + phase, (float) z - phase * 0.5F);
        float fz = fnl.GetNoise((float) x + 137.3F - phase * 0.5F, (float) z - 91.7F + phase);
        return new Vec3(Mth.clamp(fx, -1.0F, 1.0F), 0.0D, Mth.clamp(fz, -1.0F, 1.0F));
    }

    /**
     * Deterministic [0.0, 1.0] territory foraging suitability sampled via OpenSimplex2 FBm.
     */
    public static double foragingAffinity(double x, double z, long seed) {
        FastNoiseLite fnl = FLOW_NOISE.get();
        fnl.SetSeed((int) (seed ^ (seed >>> 32)));
        float raw = fnl.GetNoise((float) x * 0.65F, (float) z * 0.65F);
        return Mth.clamp((raw + 1.0F) * 0.5D, 0.0D, 1.0D);
    }
}
