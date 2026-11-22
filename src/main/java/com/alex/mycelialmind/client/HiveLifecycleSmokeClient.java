package com.alex.mycelialmind.client;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.HiveBlackboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in integrated-world lifecycle regression, using an isolated run directory. */
public final class HiveLifecycleSmokeClient {
    private static volatile int stage;
    private static volatile ServerLevel previousLevel;
    private static volatile String failure;
    private static int ticks;
    private static int holdTicks;

    private HiveLifecycleSmokeClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (Boolean.getBoolean("mycelialmind.hiveLifecycleForceFailure")) {
            failure = "forced failure for exit-code verification";
        }
        if (++ticks > 6000 || failure != null) {
            throw new IllegalStateException("HiveLifecycleSmoke FAILED: " + (failure != null ? failure : "timeout at stage " + stage));
        }
        if ((stage == 0 || stage == 3) && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
            int world = stage == 0 ? 1 : 2;
            stage = world == 1 ? 1 : 4;
            LevelSettings settings = new LevelSettings("hive_lifecycle_" + world, GameType.CREATIVE, false,
                    Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("hive_lifecycle_" + world + "_" + System.currentTimeMillis(),
                    settings, new WorldOptions(481516L, true, false),
                    reg -> reg.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            return;
        }
        if (stage == 1 && mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
            stage = 2;
            mc.getSingleplayerServer().execute(() -> {
                ServerLevel level = mc.getSingleplayerServer().overworld();
                BlockPos target = new BlockPos(12, 70, 15);
                Vec3 targetVec = Vec3.atCenterOf(target);
                HiveBlackboard.reportTarget(level, target);
                HiveBlackboard.reportNodeLoss(level, target);
                HiveBlackboard.registerCarrion(level, targetVec, 200);
                HiveBlackboard.getInfluenceMap(level).addTrauma(target, 0.70F);
                if (!target.equals(HiveBlackboard.getRecentTarget(level))
                        || !target.equals(HiveBlackboard.getRallyNodePos(level))
                        || HiveBlackboard.findNearestCarrion(level, targetVec, 8.0D) == null
                        || HiveBlackboard.getInfluenceMap(level).sample(target).trauma() < 0.50F) {
                    failure = "world A did not retain its reports, carrion, or trauma";
                } else {
                    previousLevel = level;
                    stage = 21;
                }
            });
        } else if (stage == 21) {
            stage = 22;
            mc.level.disconnect();
            mc.disconnect(new TitleScreen());
            BlockPos target = new BlockPos(12, 70, 15);
            Vec3 targetVec = Vec3.atCenterOf(target);
            if (HiveBlackboard.getRecentTarget(previousLevel) != null
                    || HiveBlackboard.getRallyNodePos(previousLevel) != null
                    || HiveBlackboard.findNearestCarrion(previousLevel, targetVec, 8.0D) != null
                    || HiveBlackboard.getInfluenceMap(previousLevel).sample(target).trauma() != 0.0F) {
                failure = "world A reports, carrion, or trauma survived actual unload";
            } else {
                MycelialMind.LOGGER.info("HiveLifecycleSmoke: world A unloaded and reports/carrion/trauma cleared");
                stage = 3;
            }
        } else if (stage == 4 && mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
            stage = 5;
            mc.getSingleplayerServer().execute(() -> {
                ServerLevel level = mc.getSingleplayerServer().overworld();
                BlockPos target = new BlockPos(12, 70, 15);
                Vec3 targetVec = Vec3.atCenterOf(target);
                if (level == previousLevel || !level.dimension().equals(previousLevel.dimension())
                        || HiveBlackboard.getRecentTarget(level) != null
                        || HiveBlackboard.getRallyNodePos(level) != null
                        || HiveBlackboard.findNearestCarrion(level, targetVec, 8.0D) != null
                        || HiveBlackboard.getInfluenceMap(level).sample(target).trauma() != 0.0F) {
                    failure = "world B inherited reports/carrion/trauma or did not use a distinct level in the same dimension";
                } else {
                    stage = 6;
                }
            });
        } else if (stage == 6) {
            if (holdTicks++ < Integer.getInteger("mycelialmind.hiveLifecycleHoldTicks", 0)) return;
            MycelialMind.LOGGER.info("HiveLifecycleSmoke PASSED: two integrated worlds in one JVM; unload cleared world A and world B is isolated");
            mc.stop();
        }
    }
}
