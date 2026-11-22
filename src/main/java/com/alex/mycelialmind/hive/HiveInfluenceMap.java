package com.alex.mycelialmind.hive;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coarse O(1) chunk-section spatial influence grid tracking Mycelial control,
 * combat threat, and pheromone trails with iterative propagation and decay.
 */
public final class HiveInfluenceMap {
    public static final int MAX_CELLS = 512;
    private static final float CARDINAL_DIFFUSION = 0.35F;
    private static final float PRUNE_EPSILON = 0.01F;

    public static final class Cell {
        private float control;
        private float threat;
        private float pheromone;
        private float trauma;

        public float control() {
            return control;
        }

        public float threat() {
            return threat;
        }

        public float pheromone() {
            return pheromone;
        }

        public float trauma() {
            return trauma;
        }

        private void add(float dControl, float dThreat, float dPheromone) {
            this.control = Mth.clamp(this.control + dControl, 0.0F, 1.0F);
            this.threat = Mth.clamp(this.threat + dThreat, 0.0F, 1.0F);
            this.pheromone = Mth.clamp(this.pheromone + dPheromone, 0.0F, 1.0F);
        }

        private void addTrauma(float dTrauma) {
            this.trauma = Mth.clamp(this.trauma + dTrauma, 0.0F, 1.0F);
        }

        private void scale(float factor) {
            this.control *= factor;
            this.threat *= factor;
            this.pheromone *= factor;
            this.trauma *= Math.min(0.98F, factor + 0.04F);
        }

        private boolean isEmpty() {
            return control < PRUNE_EPSILON && threat < PRUNE_EPSILON
                    && pheromone < PRUNE_EPSILON && trauma < PRUNE_EPSILON;
        }
    }

    private final Map<Long, Cell> cells = new ConcurrentHashMap<>();

    public void addInfluence(BlockPos pos, float controlDelta, float threatDelta, float pheromoneDelta) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        addCellDelta(cx, cz, controlDelta, threatDelta, pheromoneDelta);

        float diffC = controlDelta * CARDINAL_DIFFUSION;
        float diffT = threatDelta * CARDINAL_DIFFUSION;
        float diffP = pheromoneDelta * CARDINAL_DIFFUSION;
        if (Math.abs(diffC) > 1.0E-4F || Math.abs(diffT) > 1.0E-4F || Math.abs(diffP) > 1.0E-4F) {
            addCellDelta(cx + 1, cz, diffC, diffT, diffP);
            addCellDelta(cx - 1, cz, diffC, diffT, diffP);
            addCellDelta(cx, cz + 1, diffC, diffT, diffP);
            addCellDelta(cx, cz - 1, diffC, diffT, diffP);
        }
    }

    private void addCellDelta(int cx, int cz, float dControl, float dThreat, float dPheromone) {
        long key = ChunkPos.asLong(cx, cz);
        Cell cell = cells.get(key);
        if (cell == null) {
            if (cells.size() >= MAX_CELLS) {
                return;
            }
            cell = new Cell();
            cells.put(key, cell);
        }
        cell.add(dControl, dThreat, dPheromone);
    }

    public void addTrauma(BlockPos pos, float traumaDelta) {
        if (traumaDelta <= 0.0F) {
            return;
        }
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        addCellTrauma(cx, cz, traumaDelta);
        float neighborDelta = traumaDelta * 0.25F;
        addCellTrauma(cx + 1, cz, neighborDelta);
        addCellTrauma(cx - 1, cz, neighborDelta);
        addCellTrauma(cx, cz + 1, neighborDelta);
        addCellTrauma(cx, cz - 1, neighborDelta);
    }

    private void addCellTrauma(int cx, int cz, float dTrauma) {
        long key = ChunkPos.asLong(cx, cz);
        Cell cell = cells.get(key);
        if (cell == null) {
            if (cells.size() >= MAX_CELLS) {
                return;
            }
            cell = new Cell();
            cells.put(key, cell);
        }
        cell.addTrauma(dTrauma);
    }

    public Vec3 sampleTraumaEscapeVector(BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        double dx = cellTrauma(cx - 1, cz) - cellTrauma(cx + 1, cz);
        double dz = cellTrauma(cx, cz - 1) - cellTrauma(cx, cz + 1);
        Vec3 raw = new Vec3(dx, 0.0D, dz);
        if (raw.lengthSqr() > 1.0E-6D) {
            return raw.normalize();
        }
        return cellTrauma(cx, cz) >= LivingBattlefieldSystem.TRAUMA_AVOID_THRESHOLD
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : Vec3.ZERO;
    }

    private float cellTrauma(int cx, int cz) {
        Cell cell = cells.get(ChunkPos.asLong(cx, cz));
        return cell != null ? cell.trauma() : 0.0F;
    }

    public Cell sample(BlockPos pos) {
        Cell cell = cells.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        return cell != null ? cell : new Cell();
    }

    public Vec3 samplePheromoneGradient(BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        float pxPos = cellScore(cx + 1, cz);
        float pxNeg = cellScore(cx - 1, cz);
        float pzPos = cellScore(cx, cz + 1);
        float pzNeg = cellScore(cx, cz - 1);
        double gx = pxPos - pxNeg;
        double gz = pzPos - pzNeg;
        Vec3 raw = new Vec3(gx, 0.0D, gz);
        return raw.lengthSqr() > 1.0E-6D ? raw.normalize() : Vec3.ZERO;
    }

    private float cellScore(int cx, int cz) {
        Cell cell = cells.get(ChunkPos.asLong(cx, cz));
        if (cell == null) {
            return 0.0F;
        }
        return cell.pheromone + cell.threat * 0.75F;
    }

    public void stepDecay(float factor) {
        float clamped = Mth.clamp(factor, 0.0F, 1.0F);
        Iterator<Map.Entry<Long, Cell>> it = cells.entrySet().iterator();
        while (it.hasNext()) {
            Cell cell = it.next().getValue();
            cell.scale(clamped);
            if (cell.isEmpty()) {
                it.remove();
            }
        }
    }

    public int cellCount() {
        return cells.size();
    }

    public void clear() {
        cells.clear();
    }
}
