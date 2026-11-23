package com.alex.mycelialmind.client.hud;

import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Diegetic HUD toast manager displaying Act transitions, absorbed Field Engrams,
 * discovered Secrets, and Post-Matriarch Cycle milestones with smooth slide/fade.
 */
public final class DiegeticToastManager {
    public static final int DEFAULT_DURATION_MS = 4500;
    public static final int FADE_IN_MS = 320;
    public static final int FADE_OUT_MS = 520;
    private static final int MAX_QUEUE = 8;

    public enum ToastCategory {
        ACT_TRANSITION(0xFFFFD166, "toast.mycelialmind.category.act"),
        FIELD_ENGRAM(0xFF72EFDD, "toast.mycelialmind.category.engram"),
        SECRET_UNLOCKED(0xFFFFB703, "toast.mycelialmind.category.secret"),
        CYCLE_EVENT(0xFFFF4D8D, "toast.mycelialmind.category.cycle");

        private final int accentArgb;
        private final String tagKey;

        ToastCategory(int accentArgb, String tagKey) {
            this.accentArgb = accentArgb;
            this.tagKey = tagKey;
        }

        public int accentArgb() {
            return accentArgb;
        }

        public String tagKey() {
            return tagKey;
        }
    }

    public record ToastEntry(
            ToastCategory category,
            Component title,
            Component subtitle,
            int durationMs
    ) {
    }

    private static final Deque<ToastEntry> QUEUE = new ArrayDeque<>();
    private static ToastEntry activeToast = null;
    private static long activeStartMs = 0L;

    private DiegeticToastManager() {
    }

    public static synchronized void clear() {
        QUEUE.clear();
        activeToast = null;
        activeStartMs = 0L;
    }

    public static synchronized void enqueue(ToastCategory category, Component title, Component subtitle) {
        if (category == null || title == null) {
            return;
        }
        if (QUEUE.size() >= MAX_QUEUE) {
            QUEUE.pollFirst();
        }
        QUEUE.addLast(new ToastEntry(category, title, subtitle != null ? subtitle : Component.empty(), DEFAULT_DURATION_MS));
    }

    public static synchronized int getPendingCount() {
        return QUEUE.size() + (activeToast != null ? 1 : 0);
    }

    /**
     * Pure fade & slide animation math in {@code [alpha (0..1), slideOffsetPx]}.
     */
    public static float[] computeToastAlphaAndSlide(long elapsedMs, int durationMs) {
        if (elapsedMs < 0L || durationMs <= 0 || elapsedMs >= durationMs) {
            return new float[]{0.0F, 12.0F};
        }
        float alpha = 1.0F;
        float slide = 0.0F;
        if (elapsedMs < FADE_IN_MS) {
            float t = (float) elapsedMs / (float) FADE_IN_MS;
            alpha = t;
            slide = (1.0F - t) * 14.0F;
        } else if (elapsedMs > durationMs - FADE_OUT_MS) {
            float rem = (float) (durationMs - elapsedMs) / (float) FADE_OUT_MS;
            alpha = Math.max(0.0F, Math.min(1.0F, rem));
            slide = (1.0F - alpha) * 8.0F;
        }
        return new float[]{Math.max(0.0F, Math.min(1.0F, alpha)), slide};
    }

    public static void render(GuiGraphics graphics, Font font, int screenW, int screenH) {
        long now = System.currentTimeMillis();
        ToastEntry current;
        long startMs;
        boolean newlyPopped = false;
        synchronized (DiegeticToastManager.class) {
            if (activeToast != null && (now - activeStartMs) >= activeToast.durationMs()) {
                activeToast = null;
            }
            if (activeToast == null && !QUEUE.isEmpty()) {
                activeToast = QUEUE.pollFirst();
                activeStartMs = now;
                newlyPopped = true;
            }
            current = activeToast;
            startMs = activeStartMs;
        }
        if (newlyPopped && current != null) {
            playToastSound(current.category());
        }
        if (current == null) {
            return;
        }

        long elapsed = Math.max(0L, now - startMs);
        float[] anim = computeToastAlphaAndSlide(elapsed, current.durationMs());
        float alpha = anim[0];
        if (alpha <= 0.03F) {
            return;
        }
        int slide = Math.round(anim[1]);

        int cardW = 192;
        int cardH = 34;
        boolean hudTopRight = MycelialClientConfig.hudAnchor() == MycelialClientConfig.HudAnchor.TOP_RIGHT;
        int baseX = hudTopRight ? (screenW - cardW) / 2 : (screenW - cardW - 10);
        int baseY = hudTopRight ? Math.max(24, screenH - 76) : 26;
        int x = baseX + (hudTopRight ? 0 : slide);
        int y = baseY + (hudTopRight ? slide : 0);

        int alphaByte = Math.max(8, Math.min(255, Math.round(alpha * 225.0F)));
        int bg = (alphaByte << 24) | 0x150B24;
        int accent = (Math.max(16, Math.round(alpha * 255.0F)) << 24) | (current.category().accentArgb() & 0xFFFFFF);

        graphics.fill(x, y, x + cardW, y + cardH, bg);
        graphics.renderOutline(x, y, cardW, cardH, accent);
        graphics.fill(x, y, x + 3, y + cardH, accent);

        Component tag = Component.translatable(current.category().tagKey());
        drawScaled(graphics, font, tag, x + 7, y + 4, cardW - 14, accent);
        drawScaled(graphics, font, current.title(), x + 7, y + 14, cardW - 14,
                (Math.round(alpha * 255.0F) << 24) | 0xFFFFFF);
        if (!current.subtitle().getString().isEmpty()) {
            drawScaled(graphics, font, current.subtitle(), x + 7, y + 23, cardW - 14,
                    (Math.round(alpha * 240.0F) << 24) | 0xC7BCE0);
        }
    }

    private static void drawScaled(GuiGraphics graphics, Font font, Component text, int x, int y, int maxW, int color) {
        int tw = font.width(text);
        if (tw <= maxW || maxW <= 4) {
            graphics.drawString(font, text, x, y, color, true);
        } else {
            float scale = Math.max(0.65F, (float) maxW / (float) tw);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, text, 0, 0, color, true);
            graphics.pose().popPose();
        }
    }

    private static void playToastSound(ToastCategory category) {
        if (!MycelialClientConfig.hiveAdaptationAudioAlerts()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getSoundManager() != null) {
            float pitch = switch (category) {
                case ACT_TRANSITION -> 1.15F;
                case FIELD_ENGRAM -> 1.38F;
                case SECRET_UNLOCKED -> 1.48F;
                case CYCLE_EVENT -> 0.92F;
            };
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SYNAPTIC_PULSE.get(), pitch, 0.55F));
        }
    }
}
