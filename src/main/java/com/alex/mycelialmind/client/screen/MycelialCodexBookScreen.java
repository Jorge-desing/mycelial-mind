package com.alex.mycelialmind.client.screen;

import com.alex.mycelialmind.data.CodexBookData;
import com.alex.mycelialmind.data.CodexReloadListener;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.network.ClientDiscoveryCache;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Native standalone book GUI rendering codex_book.json with 12 chapter tabs,
 * progressive unlock_gate checks, spoiler toggle, and item/recipe/entity widgets.
 */
public class MycelialCodexBookScreen extends Screen {
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 224;

    @Nullable
    private CodexBookData book;
    private int chapterIdx = 0;
    private int entryIdx = 0;
    private int pageIdx = 0;
    private boolean showSpoilers = false;

    public MycelialCodexBookScreen() {
        super(Component.translatable("book.mycelialmind.mycelial_codex.name"));
    }

    @Override
    protected void init() {
        this.book = CodexReloadListener.getBook();
        rebuildNav();
    }

    private void rebuildNav() {
        this.clearWidgets();
        if (book == null || book.chapters().isEmpty()) {
            this.addRenderableWidget(Button.builder(net.minecraft.network.chat.CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(width / 2 - 40, height / 2 + 60, 80, 20).build());
            return;
        }

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Chapter navigation
        this.addRenderableWidget(Button.builder(Component.translatable("gui.mycelialmind.codex.prev_chapter"), b -> {
            chapterIdx = (chapterIdx - 1 + book.chapters().size()) % book.chapters().size();
            entryIdx = 0;
            pageIdx = 0;
            rebuildNav();
        }).bounds(left + 8, top + 6, 46, 18).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.mycelialmind.codex.next_chapter"), b -> {
            chapterIdx = (chapterIdx + 1) % book.chapters().size();
            entryIdx = 0;
            pageIdx = 0;
            rebuildNav();
        }).bounds(left + 58, top + 6, 46, 18).build());

        // Spoiler toggle
        this.addRenderableWidget(Button.builder(
                Component.translatable(showSpoilers
                        ? "gui.mycelialmind.codex.spoilers_on"
                        : "gui.mycelialmind.codex.spoilers_off"),
                b -> {
                    showSpoilers = !showSpoilers;
                    rebuildNav();
                }).bounds(left + PANEL_W - 118, top + 6, 86, 18).build());

        // Close button
        this.addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(left + PANEL_W - 26, top + 6, 18, 18).build());

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.isCreative() || hasItemInInventory(mc.player, ModItems.BIO_SCANNER.get()))) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.mycelialmind.codex.open_scanner"),
                    b -> ClientCodexOpener.openBioCodex())
                    .bounds(left + 8, top + PANEL_H - 26, 62, 18).build());
        }

        CodexBookData.Chapter ch = book.chapters().get(chapterIdx);
        List<CodexBookData.Entry> entries = ch.entries();
        if (!entries.isEmpty()) {
            int ey = top + 34;
            for (int i = 0; i < entries.size() && i < 10; i++) {
                final int idx = i;
                CodexBookData.Entry e = entries.get(i);
                boolean unlocked = isGateUnlocked(e.unlockGate());
                String label = (unlocked ? "" : "[?] ") + Component.translatable(e.titleKey()).getString();
                if (label.length() > 18) {
                    label = label.substring(0, 16) + "..";
                }
                this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                    entryIdx = idx;
                    pageIdx = 0;
                    rebuildNav();
                }).bounds(left + 8, ey + i * 17, 108, 15).build());
            }

            CodexBookData.Entry curEntry = entries.get(Math.min(entryIdx, entries.size() - 1));
            int totalPages = Math.max(1, curEntry.pages().size());
            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                if (pageIdx > 0) pageIdx--;
            }).bounds(left + 128, top + PANEL_H - 26, 28, 18).build());

            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                if (pageIdx + 1 < totalPages) pageIdx++;
            }).bounds(left + PANEL_W - 38, top + PANEL_H - 26, 28, 18).build());
        }
    }

    private static boolean hasItemInInventory(Player player, Item item) {
        if (player.getMainHandItem().is(item) || player.getOffhandItem().is(item)) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGateUnlocked(String gate) {
        Minecraft mc = Minecraft.getInstance();
        return ProgressionGoalEngine.isCodexGateUnlocked(
                gate,
                ClientDiscoveryCache.getEffectiveActTier().actNumber(),
                ClientHiveCache.isDeepMyceliumBreached(),
                mc.player != null && mc.player.hasPermissions(2),
                mc.player != null && mc.player.isCreative()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xE614101D);
        graphics.renderOutline(left, top, PANEL_W, PANEL_H, 0xFF9B4DFF);

        if (book == null || book.chapters().isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.mycelialmind.codex.not_loaded"),
                    this.width / 2, this.height / 2, 0xFFFF5555);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        CodexBookData.Chapter ch = book.chapters().get(chapterIdx);
        Component chHeader = Component.translatable("gui.mycelialmind.codex.chapter_header",
                ch.number(), Component.translatable(ch.titleKey()));
        graphics.drawString(this.font, chHeader, left + 112, top + 11, 0xFFE0C3FC, true);

        if (!ch.entries().isEmpty()) {
            CodexBookData.Entry entry = ch.entries().get(Math.min(entryIdx, ch.entries().size() - 1));
            int contentX = left + 126;
            int contentW = PANEL_W - 136;

            if (!isGateUnlocked(entry.unlockGate())) {
                graphics.drawString(this.font, Component.translatable(entry.titleKey()), contentX, top + 36, 0xFFFFB703, true);
                graphics.drawString(this.font,
                        Component.translatable("gui.mycelialmind.codex.locked", entry.unlockGate()),
                        contentX, top + 56, 0xFFADB5BD, false);
            } else if (!entry.pages().isEmpty()) {
                CodexBookData.Page page = entry.pages().get(Math.min(pageIdx, entry.pages().size() - 1));
                graphics.drawString(this.font, Component.translatable(page.titleKey()), contentX, top + 34, 0xFF72EFDD, true);

                int textY = top + 50;
                if (page.itemId() != null && "spotlight".equals(page.type())) {
                    ItemStack stack = resolveItem(page.itemId());
                    graphics.renderItem(stack, contentX + contentW / 2 - 8, textY);
                    textY += 24;
                } else if ("recipe".equals(page.type()) && page.recipeId() != null) {
                    graphics.drawString(this.font,
                            Component.translatable("gui.mycelialmind.codex.recipe_badge", page.recipeId()),
                            contentX, textY, 0xFFFFD166, false);
                    textY += 14;
                } else if ("entity".equals(page.type()) && page.entityId() != null) {
                    graphics.drawString(this.font,
                            Component.translatable("gui.mycelialmind.codex.entity_badge", page.entityId()),
                            contentX, textY, 0xFF06D6A0, false);
                    textY += 14;
                }

                if (page.isSpoiler() && !showSpoilers) {
                    graphics.drawString(this.font,
                            Component.translatable("gui.mycelialmind.codex.spoiler_hidden"),
                            contentX, textY, 0xFFFFB703, false);
                } else {
                    Component body = Component.translatable(page.textKey());
                    List<FormattedCharSequence> lines = new ArrayList<>(this.font.split(body, contentW));
                    for (int i = 0; i < lines.size() && textY + i * 10 < top + PANEL_H - 32; i++) {
                        graphics.drawString(this.font, lines.get(i), contentX, textY + i * 10, 0xFFEDE7F6, false);
                    }
                }

                String pageCounter = (pageIdx + 1) + " / " + entry.pages().size();
                graphics.drawCenteredString(this.font, pageCounter, left + 128 + contentW / 2, top + PANEL_H - 21, 0xFFADB5BD);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static ItemStack resolveItem(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) {
            return new ItemStack(BuiltInRegistries.ITEM.get(rl));
        }
        return new ItemStack(Items.BOOK);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
