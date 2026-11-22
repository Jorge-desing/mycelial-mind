package com.alex.mycelialmind.client.screen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.data.CodexBookData;
import com.alex.mycelialmind.data.CodexReloadListener;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.network.ClientDiscoveryCache;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Custom bio-mycelial double-spread Codex Book GUI rendering {@code codex_book.json}
 * with procedural chitin/vellum textures ({@code textures/gui/codex_book.png}),
 * 12 interactive chapter icon ribbon tabs, custom entry index cards with item icons,
 * inline markdown styling (**bold** and `code`), recipe crafting/station grids with live
 * hover tooltips, 3D rotating entity containment previews, callout frames, and audio cues.
 */
public class MycelialCodexBookScreen extends Screen {
    private static final ResourceLocation CODEX_GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/gui/codex_book.png");

    private static final int PANEL_W = 368;
    private static final int PANEL_H = 228;
    private static final int INDEX_X_OFFSET = 12;
    private static final int INDEX_W = 112;
    private static final int SPINE_X_OFFSET = 126;
    private static final int SPINE_W = 8;
    private static final int PAGE_X_OFFSET = 136;
    private static final int PAGE_W = 220;

    @Nullable
    private CodexBookData book;
    private int chapterIdx = 0;
    private int entryIdx = 0;
    private int pageIdx = 0;
    private int textScrollOffset = 0;
    private int maxTextScroll = 0;
    private boolean showSpoilers = false;

    private final Map<String, LivingEntity> entityPreviewCache = new HashMap<>();
    private final List<ChapterTabHitbox> chapterTabs = new ArrayList<>();
    private final List<EntryCardHitbox> entryCards = new ArrayList<>();
    @Nullable
    private ItemStack hoveredRecipeOrSpotlightStack = null;
    @Nullable
    private Component hoveredCustomTooltip = null;

    private record ChapterTabHitbox(int index, int x, int y, int w, int h, CodexBookData.Chapter chapter, ItemStack icon) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private record EntryCardHitbox(int index, int x, int y, int w, int h, CodexBookData.Entry entry, ItemStack icon, boolean unlocked) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

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
        this.chapterTabs.clear();
        this.entryCards.clear();
        this.textScrollOffset = 0;

        if (book == null || book.chapters().isEmpty()) {
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(width / 2 - 40, height / 2 + 60, 80, 20).build());
            return;
        }

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Top-bar Chapter < / > cycle buttons
        this.addRenderableWidget(Button.builder(Component.translatable("gui.mycelialmind.codex.prev_chapter"), b -> {
            selectChapter((chapterIdx - 1 + book.chapters().size()) % book.chapters().size());
        }).bounds(left + 12, top + 8, 52, 16).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.mycelialmind.codex.next_chapter"), b -> {
            selectChapter((chapterIdx + 1) % book.chapters().size());
        }).bounds(left + 72, top + 8, 52, 16).build());

        // Spoiler toggle button
        this.addRenderableWidget(Button.builder(
                Component.translatable(showSpoilers
                        ? "gui.mycelialmind.codex.spoilers_on"
                        : "gui.mycelialmind.codex.spoilers_off"),
                b -> {
                    showSpoilers = !showSpoilers;
                    playUiSound(ModSounds.BIO_SCANNER_PING.get(), 1.15F);
                    rebuildNav();
                }).bounds(left + PANEL_W - 114, top + 8, 82, 16).build());

        // Close button
        this.addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(left + PANEL_W - 28, top + 8, 18, 16).build());

        // Bidirectional Bio-Scanner telemetry shortcut
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.isCreative() || hasItemInInventory(mc.player, ModItems.BIO_SCANNER.get()))) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.mycelialmind.codex.open_scanner"),
                    b -> {
                        playUiSound(ModSounds.BIO_SCANNER_PING.get(), 1.0F);
                        ClientCodexOpener.openBioCodex();
                    })
                    .bounds(left + 14, top + PANEL_H - 25, 108, 16).build());
        }

        // Build 12 Chapter Ribbon Tabs along the top spine edge
        List<CodexBookData.Chapter> chapters = book.chapters();
        int tabStartX = left + 18;
        int tabY = top - 17;
        for (int i = 0; i < chapters.size(); i++) {
            CodexBookData.Chapter ch = chapters.get(i);
            ItemStack icon = resolveItem(ch.iconItem());
            chapterTabs.add(new ChapterTabHitbox(i, tabStartX + i * 28, tabY, 24, 18, ch, icon));
        }

        // Build custom Entry Index Cards on the left vellum spread
        CodexBookData.Chapter ch = chapters.get(chapterIdx);
        List<CodexBookData.Entry> entries = ch.entries();
        if (!entries.isEmpty()) {
            int cardX = left + INDEX_X_OFFSET + 3;
            int cardY = top + 46;
            int cardW = INDEX_W - 6;
            int cardH = 17;
            for (int i = 0; i < entries.size() && i < 8; i++) {
                CodexBookData.Entry e = entries.get(i);
                boolean unlocked = isGateUnlocked(e.unlockGate());
                ItemStack icon = resolveItem(e.iconItem());
                entryCards.add(new EntryCardHitbox(i, cardX, cardY + i * 19, cardW, cardH, e, icon, unlocked));
            }

            // Page < and > navigation buttons on the right reading spread
            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> turnPage(-1))
                    .bounds(left + PAGE_X_OFFSET + 6, top + PANEL_H - 25, 28, 16).build());

            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> turnPage(1))
                    .bounds(left + PANEL_W - 42, top + PANEL_H - 25, 28, 16).build());
        }
    }

    private void selectChapter(int newChapterIdx) {
        if (book == null || book.chapters().isEmpty()) {
            return;
        }
        this.chapterIdx = Mth.clamp(newChapterIdx, 0, book.chapters().size() - 1);
        this.entryIdx = 0;
        this.pageIdx = 0;
        this.textScrollOffset = 0;
        playUiSound(ModSounds.SYNAPTIC_PULSE.get(), 1.25F);
        rebuildNav();
    }

    private void selectEntry(int newEntryIdx) {
        if (book == null || book.chapters().isEmpty()) {
            return;
        }
        List<CodexBookData.Entry> entries = book.chapters().get(chapterIdx).entries();
        if (entries.isEmpty()) {
            return;
        }
        this.entryIdx = Mth.clamp(newEntryIdx, 0, entries.size() - 1);
        this.pageIdx = 0;
        this.textScrollOffset = 0;
        playUiSound(SoundEvents.BOOK_PAGE_TURN, 1.05F);
    }

    private void turnPage(int delta) {
        if (book == null || book.chapters().isEmpty()) {
            return;
        }
        List<CodexBookData.Entry> entries = book.chapters().get(chapterIdx).entries();
        if (entries.isEmpty()) {
            return;
        }
        CodexBookData.Entry curEntry = entries.get(Math.min(entryIdx, entries.size() - 1));
        int totalPages = Math.max(1, curEntry.pages().size());
        int next = Mth.clamp(this.pageIdx + delta, 0, totalPages - 1);
        if (next != this.pageIdx) {
            this.pageIdx = next;
            this.textScrollOffset = 0;
            playUiSound(SoundEvents.BOOK_PAGE_TURN, delta > 0 ? 1.0F : 0.92F);
        }
    }

    private static void playUiSound(SoundEvent sound, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, 0.75F));
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (ChapterTabHitbox tab : this.chapterTabs) {
                if (tab.contains(mouseX, mouseY)) {
                    selectChapter(tab.index());
                    return true;
                }
            }
            for (EntryCardHitbox card : this.entryCards) {
                if (card.contains(mouseX, mouseY)) {
                    selectEntry(card.index());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = (this.width - PANEL_W) / 2;
        if (mouseX >= left + PAGE_X_OFFSET) {
            if (this.maxTextScroll > 0) {
                int nextScroll = Mth.clamp(this.textScrollOffset - (int) Math.signum(scrollY), 0, this.maxTextScroll);
                if (nextScroll != this.textScrollOffset) {
                    this.textScrollOffset = nextScroll;
                    return true;
                }
            }
            if (scrollY < 0) {
                turnPage(1);
                return true;
            } else if (scrollY > 0) {
                turnPage(-1);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            turnPage(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            turnPage(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP && this.entryIdx > 0) {
            selectEntry(this.entryIdx - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN && book != null && !book.chapters().isEmpty()) {
            int count = book.chapters().get(chapterIdx).entries().size();
            if (this.entryIdx + 1 < count) {
                selectEntry(this.entryIdx + 1);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.hoveredRecipeOrSpotlightStack = null;
        this.hoveredCustomTooltip = null;

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        renderChapterRibbonTabs(graphics, mouseX, mouseY);
        renderBookTomeFrame(graphics, left, top);

        if (book == null || book.chapters().isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.mycelialmind.codex.not_loaded"),
                    this.width / 2, this.height / 2, 0xFFFF5555);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        CodexBookData.Chapter ch = book.chapters().get(chapterIdx);
        renderLeftIndexSpread(graphics, left, top, ch, mouseX, mouseY);
        renderRightReadingSpread(graphics, left, top, ch, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.hoveredRecipeOrSpotlightStack != null && !this.hoveredRecipeOrSpotlightStack.isEmpty()) {
            graphics.renderTooltip(this.font, this.hoveredRecipeOrSpotlightStack, mouseX, mouseY);
        } else if (this.hoveredCustomTooltip != null) {
            graphics.renderTooltip(this.font, this.hoveredCustomTooltip, mouseX, mouseY);
        }
    }

    private void renderChapterRibbonTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        for (ChapterTabHitbox tab : this.chapterTabs) {
            boolean active = tab.index() == this.chapterIdx;
            boolean hovered = tab.contains(mouseX, mouseY);
            int ty = active ? tab.y() - 2 : (hovered ? tab.y() - 1 : tab.y());
            int u = active ? 90 : 68;
            graphics.blit(CODEX_GUI_TEXTURE, tab.x(), ty, u, 204, 20, 18, 256, 256);
            graphics.fill(tab.x() + 20, ty, tab.x() + tab.w(), ty + 18, active ? 0xFF6B3FA0 : 0xFF2B223C);
            if (active || hovered) {
                graphics.renderOutline(tab.x(), ty, tab.w(), 18, active ? 0xFFB5E48C : 0xFF9B4DFF);
            }
            graphics.pose().pushPose();
            graphics.pose().translate(tab.x() + 4, ty + 1, 0);
            graphics. renderItem(tab.icon(), 0, 0);
            graphics.pose().popPose();

            if (hovered) {
                this.hoveredCustomTooltip = Component.translatable("gui.mycelialmind.codex.chapter_header",
                        tab.chapter().number(), Component.translatable(tab.chapter().titleKey()));
            }
        }
    }

    private void renderBookTomeFrame(GuiGraphics graphics, int left, int top) {
        // Outer obsidian-chitin double board & bevelled shadow
        graphics.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xCC09060E);
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF171124);
        graphics.renderOutline(left, top, PANEL_W, PANEL_H, 0xFF6C5298);
        graphics.renderOutline(left + 2, top + 2, PANEL_W - 4, PANEL_H - 4, 0xFF9D75CB);

        // Glowing synaptic trace around perimeter
        graphics.fill(left + 12, top + 4, left + PANEL_W - 12, top + 5, 0xFF7B2CBF);
        graphics.fill(left + 12, top + PANEL_H - 5, left + PANEL_W - 12, top + PANEL_H - 4, 0xFF7B2CBF);

        // Left index vellum panel sampled from procedural codex_book.png
        int indexX = left + INDEX_X_OFFSET;
        int indexY = top + 28;
        int indexH = PANEL_H - 56;
        graphics.blit(CODEX_GUI_TEXTURE, indexX, indexY, INDEX_W, indexH, 9, 9, 76, 182, 256, 256);
        graphics.renderOutline(indexX, indexY, INDEX_W, indexH, 0xFF5A3E85);

        // Central synaptic spine seam sampled from procedural codex_book.png
        int spineX = left + SPINE_X_OFFSET;
        graphics.blit(CODEX_GUI_TEXTURE, spineX, top + 6, SPINE_W, PANEL_H - 12, 85, 7, 7, 186, 256, 256);

        // Right reading parchment spread sampled from procedural codex_book.png
        int pageX = left + PAGE_X_OFFSET;
        int pageY = top + 28;
        int pageH = PANEL_H - 56;
        graphics.blit(CODEX_GUI_TEXTURE, pageX, pageY, PAGE_W, pageH, 92, 9, 155, 182, 256, 256);
        graphics.renderOutline(pageX, pageY, PAGE_W, pageH, 0xFF7A56B0);

        // Four brass & bioluminescent corner clasps from codex_book.png
        graphics.blit(CODEX_GUI_TEXTURE, left + 1, top + 1, 2, 2, 10, 10, 256, 256);
        graphics.blit(CODEX_GUI_TEXTURE, left + PANEL_W - 11, top + 1, 243, 2, 10, 10, 256, 256);
        graphics.blit(CODEX_GUI_TEXTURE, left + 1, top + PANEL_H - 11, 2, 187, 10, 10, 256, 256);
        graphics.blit(CODEX_GUI_TEXTURE, left + PANEL_W - 11, top + PANEL_H - 11, 243, 187, 10, 10, 256, 256);
    }

    private void renderLeftIndexSpread(GuiGraphics graphics, int left, int top, CodexBookData.Chapter ch, int mouseX, int mouseY) {
        int indexX = left + INDEX_X_OFFSET;
        int indexY = top + 28;

        // Chapter miniature badge + index header inside left vellum
        graphics.blit(CODEX_GUI_TEXTURE, indexX + 4, indexY + 3, 12, 12, 112, 204, 16, 16, 256, 256);
        String chNum = "Cap. " + ch.number() + " (" + ch.entries().size() + ")";
        drawScaledText(graphics, Component.literal(chNum), indexX + 19, indexY + 5, INDEX_W - 24, 0xFFD8B4F8, true);
        graphics.fill(indexX + 5, indexY + 16, indexX + INDEX_W - 5, indexY + 17, 0xFF6C46A2);

        for (EntryCardHitbox card : this.entryCards) {
            boolean selected = card.index() == this.entryIdx;
            boolean hovered = card.contains(mouseX, mouseY);

            int bg = selected ? 0xCC3B235E : (hovered ? 0xB0291944 : 0x88180F29);
            int border = selected ? 0xFFB5E48C : (hovered ? 0xFF9B4DFF : 0xFF473168);
            graphics.fill(card.x(), card.y(), card.x() + card.w(), card.y() + card.h(), bg);
            graphics.renderOutline(card.x(), card.y(), card.w(), card.h(), border);

            // Active selection left indicator bar
            if (selected) {
                graphics.fill(card.x(), card.y(), card.x() + 2, card.y() + card.h(), 0xFFB5E48C);
            }

            // Render scaled 12x12 entry icon
            graphics.pose().pushPose();
            graphics.pose().translate(card.x() + 3, card.y() + 2.5F, 0);
            graphics.pose().scale(0.75F, 0.75F, 1.0F);
            graphics.renderItem(card.icon(), 0, 0);
            graphics.pose().popPose();

            // Entry title
            String prefix = card.unlocked() ? "" : "[?] ";
            Component titleComp = Component.literal(prefix).append(Component.translatable(card.entry().titleKey()));
            int textColor = !card.unlocked() ? 0xFFFFB703 : (selected ? 0xFF9EF01A : 0xFFEDE7F6);
            drawClippedOrScaledText(graphics, titleComp, card.x() + 18, card.y() + 5, card.w() - 21, textColor, selected);

            if (hovered) {
                this.hoveredCustomTooltip = Component.translatable(card.entry().titleKey());
            }
        }
    }

    private void renderRightReadingSpread(GuiGraphics graphics, int left, int top, CodexBookData.Chapter ch, int mouseX, int mouseY) {
        // Chapter Header Banner on top bar
        Component chHeader = Component.translatable("gui.mycelialmind.codex.chapter_header",
                ch.number(), Component.translatable(ch.titleKey()));
        drawScaledText(graphics, chHeader, left + PAGE_X_OFFSET, top + 12, 114, 0xFFE0C3FC, true);

        if (ch.entries().isEmpty()) {
            return;
        }

        CodexBookData.Entry entry = ch.entries().get(Math.min(entryIdx, ch.entries().size() - 1));
        int contentX = left + PAGE_X_OFFSET + 8;
        int contentW = PAGE_W - 16;
        int pageTop = top + 33;
        int pageBottom = top + PANEL_H - 32;

        if (!isGateUnlocked(entry.unlockGate())) {
            graphics.blit(CODEX_GUI_TEXTURE, contentX, pageTop, 16, 16, 112, 204, 16, 16, 256, 256);
            drawScaledText(graphics, Component.translatable(entry.titleKey()), contentX + 20, pageTop + 4, contentW - 22, 0xFFFFB703, true);
            graphics.fill(contentX, pageTop + 20, contentX + contentW, pageTop + 21, 0xFF8C5828);

            int boxY = pageTop + 30;
            graphics.fill(contentX, boxY, contentX + contentW, boxY + 46, 0xAA261810);
            graphics.renderOutline(contentX, boxY, contentW, 46, 0xFFFFB703);
            Component lockedMsg = Component.translatable("gui.mycelialmind.codex.locked", entry.unlockGate());
            List<FormattedCharSequence> lines = this.font.split(lockedMsg, contentW - 12);
            for (int i = 0; i < lines.size(); i++) {
                graphics.drawString(this.font, lines.get(i), contentX + 6, boxY + 8 + i * 10, 0xFFFFD166, false);
            }
            return;
        }

        if (entry.pages().isEmpty()) {
            return;
        }

        CodexBookData.Page page = entry.pages().get(Math.min(pageIdx, entry.pages().size() - 1));

        // Page Header with Entry Icon & Page Title
        ItemStack headerIcon = page.itemId() != null ? resolveItem(page.itemId()) : resolveItem(entry.iconItem());
        graphics.pose().pushPose();
        graphics.pose().translate(contentX, pageTop - 1, 0);
        graphics.pose().scale(0.85F, 0.85F, 1.0F);
        graphics.renderItem(headerIcon, 0, 0);
        graphics.pose().popPose();

        drawScaledText(graphics, Component.translatable(page.titleKey()), contentX + 17, pageTop + 3, contentW - 18, 0xFF72EFDD, true);
        graphics.fill(contentX, pageTop + 15, contentX + contentW, pageTop + 16, 0xFF6B3FA0);
        graphics.fill(contentX + contentW / 2 - 8, pageTop + 15, contentX + contentW / 2 + 8, pageTop + 16, 0xFF9EF01A);

        int textY = pageTop + 20;
        String type = page.type() != null ? page.type() : "text";

        if ("spotlight".equals(type) && page.itemId() != null) {
            textY = renderSpotlightWidget(graphics, page, contentX, textY, contentW, mouseX, mouseY);
        } else if ("recipe".equals(type) && page.recipeId() != null) {
            textY = renderRecipeWidget(graphics, page, contentX, textY, contentW, mouseX, mouseY);
        } else if ("entity".equals(type) && page.entityId() != null) {
            textY = renderEntityWidget(graphics, page, contentX, textY, contentW, mouseX, mouseY);
        }

        if (page.isSpoiler() && !showSpoilers) {
            graphics.fill(contentX, textY, contentX + contentW, textY + 26, 0xAA2A1810);
            graphics.renderOutline(contentX, textY, contentW, 26, 0xFFFFB703);
            List<FormattedCharSequence> spLines = this.font.split(
                    Component.translatable("gui.mycelialmind.codex.spoiler_hidden"), contentW - 8);
            for (int i = 0; i < spLines.size() && i < 2; i++) {
                graphics.drawString(this.font, spLines.get(i), contentX + 4, textY + 4 + i * 10, 0xFFFFB703, false);
            }
            this.maxTextScroll = 0;
        } else {
            renderFormattedBodyText(graphics, page, type, contentX, textY, contentW, pageBottom);
        }

        // Page counter footer between < and >
        String pageCounter = (pageIdx + 1) + " / " + entry.pages().size();
        graphics.drawCenteredString(this.font, pageCounter, left + PAGE_X_OFFSET + PAGE_W / 2, top + PANEL_H - 21, 0xFFD8B4F8);
    }

    private int renderSpotlightWidget(GuiGraphics graphics, CodexBookData.Page page, int contentX, int y, int contentW, int mouseX, int mouseY) {
        ItemStack stack = resolveItem(page.itemId());
        int frameX = contentX + 4;
        int frameY = y;
        graphics.blit(CODEX_GUI_TEXTURE, frameX, frameY, 20, 204, 22, 22, 256, 256);
        graphics.renderItem(stack, frameX + 3, frameY + 3);

        // Item Name & Registry ID next to pedestal
        drawScaledText(graphics, stack.getHoverName(), frameX + 28, frameY + 3, contentW - 34, 0xFFFFE6A7, true);
        drawScaledText(graphics, Component.literal(page.itemId()), frameX + 28, frameY + 13, contentW - 34, 0xFF9D84B7, false);

        if (mouseX >= frameX && mouseX < frameX + 22 && mouseY >= frameY && mouseY < frameY + 22) {
            this.hoveredRecipeOrSpotlightStack = stack;
        }
        return y + 26;
    }

    private int renderRecipeWidget(GuiGraphics graphics, CodexBookData.Page page, int contentX, int y, int contentW, int mouseX, int mouseY) {
        boolean hasSecond = page.recipeId2() != null && !page.recipeId2().isBlank();
        int boxH = 56;
        graphics.fill(contentX, y, contentX + contentW, y + boxH, 0x88130C21);
        graphics.renderOutline(contentX, y, contentW, boxH, 0xFF6B3FA0);

        if (hasSecond) {
            int halfW = (contentW - 6) / 2;
            renderSingleRecipeCard(graphics, page.recipeId(), contentX + 2, y + 2, halfW, boxH - 4, mouseX, mouseY, true);
            graphics.fill(contentX + halfW + 3, y + 4, contentX + halfW + 4, y + boxH - 4, 0xFF4A306D);
            renderSingleRecipeCard(graphics, page.recipeId2(), contentX + halfW + 5, y + 2, halfW, boxH - 4, mouseX, mouseY, true);
        } else {
            renderSingleRecipeCard(graphics, page.recipeId(), contentX + 4, y + 2, contentW - 8, boxH - 4, mouseX, mouseY, false);
        }

        // Keep localized recipe_badge reference and subtle footer caption
        Component badge = Component.translatable("gui.mycelialmind.codex.recipe_badge", page.recipeId());
        drawScaledText(graphics, badge, contentX + 4, y + boxH - 9, contentW - 8, 0xFF9D84B7, false);
        return y + boxH + 4;
    }

    private void renderSingleRecipeCard(
            GuiGraphics graphics,
            String recipeIdStr,
            int rx,
            int ry,
            int rw,
            int rh,
            int mouseX,
            int mouseY,
            boolean compact
    ) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        ResourceLocation rl = ResourceLocation.tryParse(recipeIdStr);
        Optional<RecipeHolder<?>> holderOpt = (level != null && rl != null)
                ? level.getRecipeManager().byKey(rl)
                : Optional.empty();

        if (holderOpt.isEmpty() || level == null) {
            ItemStack fallback = resolveItem(recipeIdStr);
            int fx = rx + rw / 2 - 11;
            int fy = ry + 10;
            graphics.blit(CODEX_GUI_TEXTURE, fx, fy, 20, 204, 22, 22, 256, 256);
            graphics.renderItem(fallback, fx + 3, fy + 3);
            if (mouseX >= fx && mouseX < fx + 22 && mouseY >= fy && mouseY < fy + 22) {
                this.hoveredRecipeOrSpotlightStack = fallback;
            }
            return;
        }

        Recipe<?> recipe = holderOpt.get().value();
        ItemStack result = recipe.getResultItem(level.registryAccess());
        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        float scale = compact ? 0.78F : 0.85F;
        int cell = Math.round(16 * scale);

        if (recipe instanceof CraftingRecipe) {
            int gridW = 3;
            int gridH = 3;
            if (recipe instanceof ShapedRecipe shaped) {
                gridW = shaped.getWidth();
                gridH = shaped.getHeight();
            }
            int gx0 = rx + (compact ? 2 : 14);
            int gy0 = ry + 1;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int sx = gx0 + c * cell;
                    int sy = gy0 + r * cell;
                    ItemStack ingStack = ItemStack.EMPTY;
                    if (recipe instanceof ShapedRecipe) {
                        if (c < gridW && r < gridH) {
                            int idx = r * gridW + c;
                            if (idx < ingredients.size()) {
                                ingStack = pickCycledIngredient(ingredients.get(idx));
                            }
                        }
                    } else {
                        int idx = r * 3 + c;
                        if (idx < ingredients.size()) {
                            ingStack = pickCycledIngredient(ingredients.get(idx));
                        }
                    }
                    renderScaledSlot(graphics, ingStack, sx, sy, scale, false, mouseX, mouseY);
                }
            }
            int arrowX = gx0 + 3 * cell + (compact ? 3 : 8);
            int arrowY = gy0 + cell;
            graphics.blit(CODEX_GUI_TEXTURE, arrowX, arrowY, Math.round(18 * scale), Math.round(14 * scale), 44, 204, 22, 16, 256, 256);

            int outX = arrowX + Math.round(20 * scale);
            int outY = gy0 + cell - 2;
            renderScaledSlot(graphics, result, outX, outY, scale, true, mouseX, mouseY);
        } else {
            // Single-input station recipe (Furnace / Smoker / Blast Furnace / Stonecutter)
            int inX = rx + (compact ? 8 : 26);
            int inY = ry + 11;
            ItemStack inStack = ingredients.isEmpty() ? ItemStack.EMPTY : pickCycledIngredient(ingredients.get(0));
            renderScaledSlot(graphics, inStack, inX, inY, 0.95F, false, mouseX, mouseY);

            int arrowX = inX + 22;
            graphics.blit(CODEX_GUI_TEXTURE, arrowX, inY + 1, 20, 15, 44, 204, 22, 16, 256, 256);

            int outX = arrowX + 24;
            renderScaledSlot(graphics, result, outX, inY - 2, 0.95F, true, mouseX, mouseY);
        }
    }

    private void renderScaledSlot(
            GuiGraphics graphics,
            ItemStack stack,
            int x,
            int y,
            float scale,
            boolean isOutput,
            int mouseX,
            int mouseY
    ) {
        int boxSize = Math.round((isOutput ? 20 : 16) * scale);
        if (isOutput) {
            graphics.blit(CODEX_GUI_TEXTURE, x, y, boxSize, boxSize, 20, 204, 22, 22, 256, 256);
        } else {
            graphics.blit(CODEX_GUI_TEXTURE, x, y, boxSize, boxSize, 0, 204, 18, 18, 256, 256);
        }

        if (!stack.isEmpty()) {
            graphics.pose().pushPose();
            float itemOffset = isOutput ? 2.0F * scale : 0.5F * scale;
            graphics.pose().translate(x + itemOffset, y + itemOffset, 0);
            graphics.pose().scale(scale * 0.92F, scale * 0.92F, 1.0F);
            graphics.renderItem(stack, 0, 0);
            graphics.renderItemDecorations(this.font, stack, 0, 0);
            graphics.pose().popPose();

            if (mouseX >= x && mouseX < x + boxSize && mouseY >= y && mouseY < y + boxSize) {
                this.hoveredRecipeOrSpotlightStack = stack;
            }
        }
    }

    private static ItemStack pickCycledIngredient(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) {
            return ItemStack.EMPTY;
        }
        int cycle = (int) ((System.currentTimeMillis() / 1200L) % items.length);
        return items[cycle];
    }

    private int renderEntityWidget(GuiGraphics graphics, CodexBookData.Page page, int contentX, int y, int contentW, int mouseX, int mouseY) {
        int boxW = 54;
        int boxH = 50;
        graphics.fill(contentX, y, contentX + boxW, y + boxH, 0xAA100A1C);
        graphics.renderOutline(contentX, y, boxW, boxH, 0xFF06D6A0);
        graphics.fill(contentX + 2, y + boxH - 10, contentX + boxW - 2, y + boxH - 3, 0x55245E4E);

        LivingEntity living = getOrCreatePreviewEntity(page.entityId());
        if (living != null) {
            try {
                float bbMax = Math.max(0.8F, Math.max(living.getBbWidth(), living.getBbHeight()));
                int entityScale = Mth.clamp(Math.round(24.0F / bbMax), 9, 22);
                float angleX = (float) Math.atan((contentX + boxW / 2.0F - mouseX) / 40.0F);
                float angleY = (float) Math.atan((y + boxH / 2.0F - mouseY) / 40.0F);
                InventoryScreen.renderEntityInInventoryFollowsAngle(
                        graphics,
                        contentX + 2,
                        y + 2,
                        contentX + boxW - 2,
                        y + boxH - 2,
                        entityScale,
                        0.0625F,
                        angleX,
                        angleY,
                        living
                );
            } catch (Exception ignored) {
                ItemStack egg = resolveSpawnEggOrFallback(page.entityId());
                graphics.renderItem(egg, contentX + boxW / 2 - 8, y + boxH / 2 - 8);
            }
        } else {
            ItemStack egg = resolveSpawnEggOrFallback(page.entityId());
            graphics.renderItem(egg, contentX + boxW / 2 - 8, y + boxH / 2 - 8);
        }

        // Specimen Telemetry Card to the right of the containment frame
        int infoX = contentX + boxW + 6;
        int infoW = contentW - boxW - 6;
        graphics.fill(infoX, y, infoX + infoW, y + boxH, 0x66160F26);
        graphics.renderOutline(infoX, y, infoW, boxH, 0xFF483270);

        Component badge = Component.translatable("gui.mycelialmind.codex.entity_badge", page.entityId());
        drawScaledText(graphics, badge, infoX + 4, y + 5, infoW - 8, 0xFF06D6A0, true);

        if (living != null) {
            drawScaledText(graphics, living.getName(), infoX + 4, y + 17, infoW - 8, 0xFFEDE7F6, true);
            String hpText = "HP: " + Math.round(living.getMaxHealth()) + "  |  "
                    + String.format("%.1fm x %.1fm", living.getBbWidth(), living.getBbHeight());
            drawScaledText(graphics, Component.literal(hpText), infoX + 4, y + 29, infoW - 8, 0xFFFFD166, false);
        }
        drawScaledText(graphics, Component.literal(page.entityId()), infoX + 4, y + 40, infoW - 8, 0xFF9D84B7, false);

        return y + boxH + 4;
    }

    @Nullable
    private LivingEntity getOrCreatePreviewEntity(String entityId) {
        if (entityId == null) {
            return null;
        }
        if (this.entityPreviewCache.containsKey(entityId)) {
            return this.entityPreviewCache.get(entityId);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(entityId);
        if (rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            try {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
                Entity created = type.create(mc.level);
                if (created instanceof LivingEntity living) {
                    living.yBodyRot = 210.0F;
                    living.setYRot(210.0F);
                    living.yHeadRot = 210.0F;
                    living.yHeadRotO = 210.0F;
                    this.entityPreviewCache.put(entityId, living);
                    return living;
                }
            } catch (Exception ignored) {
            }
        }
        this.entityPreviewCache.put(entityId, null);
        return null;
    }

    private static ItemStack resolveSpawnEggOrFallback(String entityId) {
        ResourceLocation rl = ResourceLocation.tryParse(entityId + "_spawn_egg");
        if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) {
            return new ItemStack(BuiltInRegistries.ITEM.get(rl));
        }
        return new ItemStack(ModItems.BIO_SCANNER.get());
    }

    private void renderFormattedBodyText(
            GuiGraphics graphics,
            CodexBookData.Page page,
            String type,
            int contentX,
            int textY,
            int contentW,
            int pageBottom
    ) {
        boolean isCallout = "callout".equals(type);
        int accentArgb = parseAccentColor(page.accentColor(), isCallout ? 0xFF9B4DFF : 0xFF72EFDD);
        int textX = isCallout ? contentX + 6 : contentX;
        int wrapW = isCallout ? contentW - 12 : contentW;

        String rawText = Component.translatable(page.textKey()).getString();
        Component styledBody = parseMarkdownComponent(rawText);
        List<FormattedCharSequence> lines = new ArrayList<>(this.font.split(styledBody, wrapW));

        int availH = Math.max(10, pageBottom - textY - (isCallout ? 6 : 0));
        int lineStep = 9;
        int visibleLines = Math.max(1, availH / lineStep);
        this.maxTextScroll = Math.max(0, lines.size() - visibleLines);
        this.textScrollOffset = Mth.clamp(this.textScrollOffset, 0, this.maxTextScroll);

        if (isCallout) {
            int boxH = Math.min(pageBottom - textY, Math.min(lines.size(), visibleLines) * lineStep + 10);
            int bgTint = (0x55 << 24) | ((accentArgb & 0xFEFEFE) >> 2);
            graphics.fill(contentX, textY, contentX + contentW, textY + boxH, bgTint);
            graphics.renderOutline(contentX, textY, contentW, boxH, accentArgb);
            graphics.fill(contentX, textY, contentX + 3, textY + boxH, accentArgb);
            textY += 5;
        }

        for (int i = 0; i < visibleLines && (i + this.textScrollOffset) < lines.size(); i++) {
            graphics.drawString(this.font, lines.get(i + this.textScrollOffset), textX, textY + i * lineStep, 0xFFEDE7F6, false);
        }

        // Scroll indicator bar if text overflows
        if (this.maxTextScroll > 0) {
            int barX = contentX + contentW - 2;
            int barTop = textY;
            int barH = visibleLines * lineStep;
            graphics.fill(barX, barTop, barX + 2, barTop + barH, 0x553B2854);
            int thumbH = Math.max(8, (barH * visibleLines) / lines.size());
            int thumbY = barTop + ((barH - thumbH) * this.textScrollOffset) / this.maxTextScroll;
            graphics.fill(barX, thumbY, barX + 2, thumbY + thumbH, 0xFFB5E48C);
        }
    }

    /**
     * Parses inline {@code **bold**} and {@code `code`} markers into styled Minecraft Components.
     */
    public static Component parseMarkdownComponent(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        String normalized = raw.replace(" — ", " - ").replace("•", "*");
        MutableComponent root = Component.empty();
        Style normalStyle = Style.EMPTY.withColor(0xEDE7F6);
        Style boldStyle = Style.EMPTY.withColor(0xFFD166).withBold(true);
        Style codeStyle = Style.EMPTY.withColor(0x72EFDD);

        int idx = 0;
        int len = normalized.length();
        StringBuilder plain = new StringBuilder();

        while (idx < len) {
            if (idx + 1 < len && normalized.charAt(idx) == '*' && normalized.charAt(idx + 1) == '*') {
                int end = normalized.indexOf("**", idx + 2);
                if (end > idx + 2) {
                    if (!plain.isEmpty()) {
                        root.append(Component.literal(plain.toString()).withStyle(normalStyle));
                        plain.setLength(0);
                    }
                    String boldContent = normalized.substring(idx + 2, end);
                    root.append(Component.literal(boldContent).withStyle(boldStyle));
                    idx = end + 2;
                    continue;
                }
            }
            if (normalized.charAt(idx) == '`') {
                int end = normalized.indexOf('`', idx + 1);
                if (end > idx + 1) {
                    if (!plain.isEmpty()) {
                        root.append(Component.literal(plain.toString()).withStyle(normalStyle));
                        plain.setLength(0);
                    }
                    String codeContent = normalized.substring(idx + 1, end);
                    root.append(Component.literal(codeContent).withStyle(codeStyle));
                    idx = end + 1;
                    continue;
                }
            }
            plain.append(normalized.charAt(idx));
            idx++;
        }
        if (!plain.isEmpty()) {
            root.append(Component.literal(plain.toString()).withStyle(normalStyle));
        }
        return root;
    }

    public static int parseAccentColor(@Nullable String hex, int fallback) {
        if (hex == null || hex.isBlank()) {
            return fallback;
        }
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            int rgb = Integer.parseInt(clean, 16);
            return 0xFF000000 | (rgb & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void drawScaledText(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color, boolean shadow) {
        int w = this.font.width(text);
        if (w <= maxWidth || maxWidth <= 8) {
            graphics.drawString(this.font, text, x, y, color, shadow);
        } else {
            float scale = Math.max(0.62F, (float) maxWidth / (float) w);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y + (1.0F - scale) * 3.5F, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(this.font, text, 0, 0, color, shadow);
            graphics.pose().popPose();
        }
    }

    private void drawClippedOrScaledText(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color, boolean shadow) {
        int w = this.font.width(text);
        if (w <= maxWidth) {
            graphics.drawString(this.font, text, x, y, color, shadow);
        } else if (w <= maxWidth * 1.32F) {
            drawScaledText(graphics, text, x, y, maxWidth, color, shadow);
        } else {
            String raw = text.getString();
            String clipped = this.font.plainSubstrByWidth(raw, Math.max(8, maxWidth - 6)) + "..";
            graphics.drawString(this.font, clipped, x, y, color, shadow);
        }
    }

    private static ItemStack resolveItem(@Nullable String id) {
        if (id != null && !id.isBlank()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) {
                if (BuiltInRegistries.ITEM.containsKey(rl)) {
                    Item item = BuiltInRegistries.ITEM.get(rl);
                    if (item != Items.AIR) {
                        return new ItemStack(item);
                    }
                }
                ResourceLocation eggRl = ResourceLocation.tryParse(id + "_spawn_egg");
                if (eggRl != null && BuiltInRegistries.ITEM.containsKey(eggRl)) {
                    return new ItemStack(BuiltInRegistries.ITEM.get(eggRl));
                }
            }
        }
        return new ItemStack(ModItems.MYCELIAL_CODEX.get());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
