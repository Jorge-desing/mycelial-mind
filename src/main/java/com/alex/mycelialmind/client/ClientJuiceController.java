package com.alex.mycelialmind.client;

import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.entity.MycelialBossMob;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossCombatState;
import com.alex.mycelialmind.hive.UxJuiceEngine;
import com.alex.mycelialmind.network.ClientHiveCache;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side controller coordinating micro camera shake and tactile combat triggers.
 */
public final class ClientJuiceController {
    private static float shakeIntensity = 0.0F;
    private static double lastThreat = -1.0D;
    private static int lastSuppressedSec = 0;
    private static int lastHurtTime = 0;
    private static float timeTicks = 0.0F;

    private ClientJuiceController() {
    }

    public static void reset() {
        shakeIntensity = 0.0F;
        lastThreat = -1.0D;
        lastSuppressedSec = 0;
        lastHurtTime = 0;
        timeTicks = 0.0F;
    }

    public static void triggerShake(float amount) {
        if (Float.isFinite(amount) && amount > 0.0F) {
            shakeIntensity = Math.min(1.0F, Math.max(shakeIntensity, amount));
        }
    }

    public static void tick(@Nullable Player player) {
        timeTicks += 1.0F;
        shakeIntensity = UxJuiceEngine.computeShakeDecay(shakeIntensity, 0.075F);
        if (player == null) {
            return;
        }
        if (player.hurtTime > lastHurtTime && player.hurtTime >= 7) {
            triggerShake(0.38F);
        }
        lastHurtTime = player.hurtTime;

        int supSec = ClientHiveCache.getSuppressionSecondsRemaining();
        if (supSec > lastSuppressedSec + 2) {
            triggerShake(0.42F);
        }
        lastSuppressedSec = supSec;

        double threat = ClientHiveCache.getThreatLevel();
        if (lastThreat >= 0.0D && threat - lastThreat >= 4.0D) {
            triggerShake(0.30F);
        }
        lastThreat = threat;

        if (player.level() != null && ((int) timeTicks & 3) == 0) {
            for (MycelialBossMob boss : player.level().getEntitiesOfClass(
                    MycelialBossMob.class, player.getBoundingBox().inflate(24.0D), MycelialBossMob::isAlive)) {
                if (boss.getCombatState() == BossCombatState.ACTIVE) {
                    triggerShake(0.24F);
                    break;
                }
            }
        }
    }

    public static void applyCameraShake(ViewportEvent.ComputeCameraAngles event) {
        float scale = (float) MycelialClientConfig.screenShakeScale();
        if (shakeIntensity <= 0.001F || scale <= 0.0F) {
            return;
        }
        float[] delta = UxJuiceEngine.computeShakeAngles(
                shakeIntensity, timeTicks + (float) event.getPartialTick(), scale);
        event.setYaw(event.getYaw() + delta[0]);
        event.setPitch(event.getPitch() + delta[1]);
        event.setRoll(event.getRoll() + delta[2]);
    }
}
