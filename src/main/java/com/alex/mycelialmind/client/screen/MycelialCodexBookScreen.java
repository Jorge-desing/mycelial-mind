package com.alex.mycelialmind.client.screen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.data.CodexBookData;
import com.alex.mycelialmind.data.CodexReloadListener;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.network.ClientDiscoveryCache;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Custom bio-mycelial double-spread Codex Book GUI rendering {@code codex_book.json}
 * with procedural chitin/vellum textures ({@code textures/gui/codex_book.png}),
 * 12 interactive chapter icon ribbon tabs, live cross-book search, scrollable entry
 * index cards with item icons, custom themed buttons, persistent reading position,
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
    private static final int MAX_VISIBLE_ENTRIES = 7;

    // Persist last reading state across screen reopenings during the client session
    private static int savedChapterIdx = 0;
    private static int savedEntryIdx = 0;
    private static int savedPageIdx = 0;
    private static boolean savedShowSpoilers = false;

    @Nullable
    private CodexBookData book;
    private int chapterIdx = savedChapterIdx;
    private int entryIdx = savedEntryIdx;
    private int pageIdx = savedPageIdx;
    private int entryScrollOffset = 0;
    private int textScrollOffset = 0;
    private int maxTextScroll = 0;
    private boolean showSpoilers = savedShowSpoilers;
    private String searchQuery = "";

    @Nullable
    private EditBox searchBox;
    @Nullable
    private CodexButton prevPageBtn;
    @Nullable
    private CodexButton nextPageBtn;

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

    private record EntryCardHitbox(
            int chapterIndex,
            int entryIndex,
            int x,
            int y,
            int w,
            int h,
            CodexBookData.Entry entry,
            ItemStack icon,
            boolean unlocked,
            boolean fromSearch
    ) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    /**
     * Custom necrotic chitin & crimson-vein button matching the eldritch Codex aesthetic.
     */
    private static final class CodexButton extends Button {
        private final boolean accent;

        CodexButton(int x, int y, int w, int h, Component message, OnPress onPress, boolean accent) {
            super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
            this.accent = accent;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isHoveredOrFocused() && this.active;
            int bg;
            int border;
            int textColor;
            if (!this.active) {
                bg = 0x8811080D;
                border = 0xFF3A1A24;
                textColor = 0xFF6B4E58;
            } else if (hovered) {
                bg = accent ? 0xEE5E1724 : 0xEE3D1422;
                border = accent ? 0xFFB5E48C : 0xFFD90429;
                textColor = 0xFFF8F9FA;
            } else {
                bg = accent ? 0xDD380F19 : 0xCC1C0C14;
                border = accent ? 0xFF9E2A3B : 0xFF6E2534;
                textColor = accent ? 0xFFB5E48C : 0xFFE8DCE0;
            }
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
            graphics.renderOutline(getX(), getY(), width, height, border);
            if (this.active) {
                graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + 2, hovered ? 0x44B5E48C : 0x28D90429);
            }
            var font = Minecraft.getInstance().font;
            Component msg = getMessage();
            int tw = font.width(msg);
            int maxW = width - 6;
            if (tw <= maxW || maxW <= 4) {
                graphics.drawCenteredString(font, msg, getX() + width / 2, getY() + (height - 8) / 2, textColor);
            } else {
                float scale = Math.max(0.65F, (float) maxW / (float) tw);
                graphics.pose().pushPose();
                graphics.pose().translate(getX() + width / 2.0F, getY() + height / 2.0F, 0);
                graphics.pose().scale(scale, scale, 1.0F);
                graphics.drawCenteredString(font, msg, 0, -4, textColor);
                graphics.pose().popPose();
            }
        }
    }

    public MycelialCodexBookScreen() {
        super(Component.translatable("book.mycelialmind.mycelial_codex.name"));
    }

    @Override
    protected void init() {
        this.book = CodexReloadListener.getBook();
        clampReadingPosition();
        rebuildNav();
    }

    private void clampReadingPosition() {
        if (book == null || book.chapters().isEmpty()) {
            this.chapterIdx = 0;
            this.entryIdx = 0;
            this.pageIdx = 0;
            return;
        }
        this.chapterIdx = Mth.clamp(this.chapterIdx, 0, book.chapters().size() - 1);
        List<CodexBookData.Entry> entries = book.chapters().get(this.chapterIdx).entries();
        if (entries.isEmpty()) {
            this.entryIdx = 0;
            this.pageIdx = 0;
        } else {
            this.entryIdx = Mth.clamp(this.entryIdx, 0, entries.size() - 1);
            int pages = Math.max(1, entries.get(this.entryIdx).pages().size());
            this.pageIdx = Mth.clamp(this.pageIdx, 0, pages - 1);
        }
        persistReadingPosition();
    }

    private void persistReadingPosition() {
        savedChapterIdx = this.chapterIdx;
        savedEntryIdx = this.entryIdx;
        savedPageIdx = this.pageIdx;
        savedShowSpoilers = this.showSpoilers;
    }

    private void rebuildNav() {
        this.clearWidgets();
        this.chapterTabs.clear();
        this.textScrollOffset = 0;

        if (book == null || book.chapters().isEmpty()) {
            this.addRenderableWidget(new CodexButton(width / 2 - 40, height / 2 + 60, 80, 20,
                    CommonComponents.GUI_DONE, b -> onClose(), true));
            return;
        }

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Top-bar Chapter < / > cycle buttons
        this.addRenderableWidget(new CodexButton(left + 12, top + 8, 52, 16,
                Component.translatable("gui.mycelialmind.codex.prev_chapter"),
                b -> selectChapter((chapterIdx - 1 + book.chapters().size()) % book.chapters().size()), false));

        this.addRenderableWidget(new CodexButton(left + 72, top + 8, 52, 16,
                Component.translatable("gui.mycelialmind.codex.next_chapter"),
                b -> selectChapter((chapterIdx + 1) % book.chapters().size()), false));

        // Spoiler toggle button
        this.addRenderableWidget(new CodexButton(left + PANEL_W - 114, top + 8, 82, 16,
                Component.translatable(showSpoilers
                        ? "gui.mycelialmind.codex.spoilers_on"
                        : "gui.mycelialmind.codex.spoilers_off"),
                b -> {
                    showSpoilers = !showSpoilers;
                    persistReadingPosition();
                    playUiSound(ModSounds.HIVE_TRAIT_ADAPT.get(), 0.90F);
                    rebuildNav();
                }, showSpoilers));

        // Close button
        this.addRenderableWidget(new CodexButton(left + PANEL_W - 28, top + 8, 18, 16,
                Component.literal("X"), b -> onClose(), false));

        // Live Search EditBox inside top of left index vellum
        this.searchBox = new EditBox(this.font, left + INDEX_X_OFFSET + 4, top + 47, INDEX_W - 8, 13,
                Component.literal("Search"));
        this.searchBox.setMaxLength(40);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(0xFFEDE7F6);
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(val -> {
            this.searchQuery = val == null ? "" : val;
            this.entryScrollOffset = 0;
            rebuildEntryCards(left, top);
        });
        this.addRenderableWidget(this.searchBox);

        // Bidirectional Bio-Scanner telemetry shortcut
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.isCreative() || hasItemInInventory(mc.player, ModItems.BIO_SCANNER.get()))) {
            this.addRenderableWidget(new CodexButton(left + 14, top + PANEL_H - 25, 108, 16,
                    Component.translatable("gui.mycelialmind.codex.open_scanner"),
                    b -> {
                        playUiSound(ModSounds.BIO_SCANNER_PING.get(), 1.0F);
                        ClientCodexOpener.openBioCodex();
                    }, true));
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

        rebuildEntryCards(left, top);

        // Page < and > navigation buttons on the right reading spread
        this.prevPageBtn = this.addRenderableWidget(new CodexButton(left + PAGE_X_OFFSET + 6, top + PANEL_H - 25, 28, 16,
                Component.literal("<"), b -> turnPage(-1), false));

        this.nextPageBtn = this.addRenderableWidget(new CodexButton(left + PANEL_W - 42, top + PANEL_H - 25, 28, 16,
                Component.literal(">"), b -> turnPage(1), false));
        updatePageButtonStates();
    }

    private void rebuildEntryCards(int left, int top) {
        this.entryCards.clear();
        if (book == null || book.chapters().isEmpty()) {
            return;
        }
        String query = this.searchQuery.trim().toLowerCase(Locale.ROOT);
        int cardX = left + INDEX_X_OFFSET + 3;
        int cardY = top + 64;
        int cardW = INDEX_W - 6;
        int cardH = 17;

        if (!query.isEmpty()) {
            List<int[]> matches = new ArrayList<>();
            for (int ci = 0; ci < book.chapters().size(); ci++) {
                CodexBookData.Chapter ch = book.chapters().get(ci);
                for (int ei = 0; ei < ch.entries().size(); ei++) {
                    CodexBookData.Entry e = ch.entries().get(ei);
                    if (matchesQuery(ch, e, query)) {
                        matches.add(new int[]{ci, ei});
                    }
                }
            }
            int maxScroll = Math.max(0, matches.size() - MAX_VISIBLE_ENTRIES);
            this.entryScrollOffset = Mth.clamp(this.entryScrollOffset, 0, maxScroll);
            for (int slot = 0; slot < MAX_VISIBLE_ENTRIES && (slot + this.entryScrollOffset) < matches.size(); slot++) {
                int[] pair = matches.get(slot + this.entryScrollOffset);
                CodexBookData.Entry e = book.chapters().get(pair[0]).entries().get(pair[1]);
                boolean unlocked = isGateUnlocked(e.unlockGate());
                entryCards.add(new EntryCardHitbox(
                        pair[0], pair[1], cardX, cardY + slot * 19, cardW, cardH,
                        e, resolveItem(e.iconItem()), unlocked, true
                ));
            }
        } else {
            List<CodexBookData.Entry> entries = book.chapters().get(chapterIdx).entries();
            int maxScroll = Math.max(0, entries.size() - MAX_VISIBLE_ENTRIES);
            this.entryScrollOffset = Mth.clamp(this.entryScrollOffset, 0, maxScroll);
            for (int slot = 0; slot < MAX_VISIBLE_ENTRIES && (slot + this.entryScrollOffset) < entries.size(); slot++) {
                int idx = slot + this.entryScrollOffset;
                CodexBookData.Entry e = entries.get(idx);
                boolean unlocked = isGateUnlocked(e.unlockGate());
                entryCards.add(new EntryCardHitbox(
                        chapterIdx, idx, cardX, cardY + slot * 19, cardW, cardH,
                        e, resolveItem(e.iconItem()), unlocked, false
                ));
            }
        }
    }

    private static boolean matchesQuery(CodexBookData.Chapter ch, CodexBookData.Entry entry, String query) {
        if (entry.id().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        if (Component.translatable(entry.titleKey()).getString().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        if (Component.translatable(ch.titleKey()).getString().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        for (CodexBookData.Page p : entry.pages()) {
            if (Component.translatable(p.titleKey()).getString().toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
            if (p.itemId() != null && p.itemId().toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
            if (p.entityId() != null && p.entityId().toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private void updatePageButtonStates() {
        if (this.prevPageBtn == null || this.nextPageBtn == null || book == null || book.chapters().isEmpty()) {
            return;
        }
        List<CodexBookData.Entry> entries = book.chapters().get(chapterIdx).entries();
        if (entries.isEmpty()) {
            this.prevPageBtn.active = false;
            this.nextPageBtn.active = false;
            return;
        }
        CodexBookData.Entry curEntry = entries.get(Math.min(entryIdx, entries.size() - 1));
        int totalPages = Math.max(1, curEntry.pages().size());
        this.prevPageBtn.active = this.pageIdx > 0;
        this.nextPageBtn.active = this.pageIdx + 1 < totalPages;
    }

    private void selectChapter(int newChapterIdx) {
        if (book == null || book.chapters().isEmpty()) {
            return;
        }
        this.chapterIdx = Mth.clamp(newChapterIdx, 0, book.chapters().size() - 1);
        this.entryIdx = 0;
        this.pageIdx = 0;
        this.entryScrollOffset = 0;
        this.textScrollOffset = 0;
        this.searchQuery = "";
        persistReadingPosition();
        playUiSound(ModSounds.HOMEOSTASIS_HEARTBEAT.get(), 0.85F);
        rebuildNav();
    }

    private void selectEntry(int targetChapterIdx, int newEntryIdx) {
        if (book == null || book.chapters().isEmpty()) {
            return;
        }
        this.chapterIdx = Mth.clamp(targetChapterIdx, 0, book.chapters().size() - 1);
        List<CodexBookData.Entry> entries = book.chapters().get(chapterIdx).entries();
        if (entries.isEmpty()) {
            return;
        }
        this.entryIdx = Mth.clamp(newEntryIdx, 0, entries.size() - 1);
        this.pageIdx = 0;
        this.textScrollOffset = 0;
        persistReadingPosition();
        updatePageButtonStates();
        playUiSound(SoundEvents.BOOK_PAGE_TURN, 0.82F);
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
            persistReadingPosition();
            updatePageButtonStates();
            playUiSound(SoundEvents.BOOK_PAGE_TURN, 0.82F);
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
                    selectEntry(card.chapterIndex(), card.entryIndex());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        if (mouseX >= left + INDEX_X_OFFSET && mouseX < left + SPINE_X_OFFSET) {
            this.entryScrollOffset = Math.max(0, this.entryScrollOffset - (int) Math.signum(scrollY));
            rebuildEntryCards(left, top);
            return true;
        }
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
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.searchBox.setFocused(false);
                return true;
            }
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            turnPage(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            turnPage(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP && this.entryIdx > 0) {
            selectEntry(this.chapterIdx, this.entryIdx - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN && book != null && !book.chapters().isEmpty()) {
            int count = book.chapters().get(chapterIdx).entries().size();
            if (this.entryIdx + 1 < count) {
                selectEntry(this.chapterIdx, this.entryIdx + 1);
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
        long nowMs = System.currentTimeMillis();
        float threatLevel = (float) ClientHiveCache.getThreatLevel();
        float pulse = computeHeartbeatIntensity(threatLevel, nowMs);

        renderFloatingSpores(graphics, left, top, nowMs, pulse);
        renderChapterRibbonTabs(graphics, mouseX, mouseY, pulse);
        renderBookTomeFrame(graphics, left, top, threatLevel, pulse);

        if (book == null || book.chapters().isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.mycelialmind.codex.not_loaded"),
                    this.width / 2, this.height / 2, 0xFFFF5555);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        CodexBookData.Chapter ch = book.chapters().get(chapterIdx);
        renderLeftIndexSpread(graphics, left, top, ch, mouseX, mouseY, pulse);
        renderRightReadingSpread(graphics, left, top, ch, mouseX, mouseY, pulse);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.searchBox != null && this.searchQuery.isEmpty() && !this.searchBox.isFocused()) {
            graphics.drawString(this.font, "...", this.searchBox.getX() + 2, this.searchBox.getY() + 1, 0xFF8C5A66, false);
        }

        if (this.hoveredRecipeOrSpotlightStack != null && !this.hoveredRecipeOrSpotlightStack.isEmpty()) {
            graphics.renderTooltip(this.font, this.hoveredRecipeOrSpotlightStack, mouseX, mouseY);
        } else if (this.hoveredCustomTooltip != null) {
            graphics.renderTooltip(this.font, this.hoveredCustomTooltip, mouseX, mouseY);
        }
    }

    /**
     * Computes an organic Hive-Mind heartbeat intensity in [0.0, 1.0] whose cadence
     * and baseline amplitude accelerate with {@code threatLevel} (0..100).
     */
    public static float computeHeartbeatIntensity(float threatLevel, long timeMs) {
        float threatNorm = Mth.clamp(threatLevel / 100.0F, 0.0F, 1.0F);
        double speedHz = 0.0022 + threatNorm * 0.0048;
        float wave = 0.5F + 0.5F * (float) Math.sin(timeMs * speedHz);
        return Mth.clamp(0.15F * threatNorm + wave * (0.35F + 0.50F * threatNorm), 0.0F, 1.0F);
    }

    private void renderFloatingSpores(GuiGraphics graphics, int left, int top, long nowMs, float pulse) {
        double t = nowMs * 0.001;
        int w = Math.max(1, this.width);
        int h = Math.max(1, this.height);
        for (int i = 0; i < 22; i++) {
            float seedX = ((i * 73 + 19) % 100) / 100.0F;
            float seedY = ((i * 41 + 37) % 100) / 100.0F;
            double driftY = (seedY - t * (0.025 + (i % 4) * 0.008)) % 1.0;
            if (driftY < 0.0) driftY += 1.0;
            double swayX = seedX + Math.sin(t * 0.9 + i * 1.7) * 0.028;
            int sx = Mth.clamp((int) (swayX * w), 2, w - 4);
            int sy = Mth.clamp((int) (driftY * h), 2, h - 4);
            // Keep drifting spores outside the readable parchment interiors so text stays crystal-clear
            boolean insideReadZone = sx > left + INDEX_X_OFFSET + 4
                    && sx < left + PANEL_W - 8
                    && sy > top + 28
                    && sy < top + PANEL_H - 26;
            if (insideReadZone && (i % 3 != 0)) {
                continue;
            }
            int size = (i % 5 == 0) ? 3 : 2;
            int alpha = Math.round(55 + 75 * pulse) << 24;
            int rgb = switch (i % 3) {
                case 0 -> 0xB5E48C; // Toxic spore chartreuse
                case 1 -> 0xD90429; // Hive crimson vein
                default -> 0x9B4DFF; // Mycelial amethyst mist
            };
            graphics.fill(sx, sy, sx + size, sy + size, alpha | rgb);
        }
    }

    private void renderChapterRibbonTabs(GuiGraphics graphics, int mouseX, int mouseY, float pulse) {
        for (ChapterTabHitbox tab : this.chapterTabs) {
            boolean active = tab.index() == this.chapterIdx;
            boolean hovered = tab.contains(mouseX, mouseY);
            int ty = active ? tab.y() - 2 : (hovered ? tab.y() - 1 : tab.y());
            int u = active ? 90 : 68;
            graphics.blit(CODEX_GUI_TEXTURE, tab.x(), ty, u, 204, 20, 18, 256, 256);
            graphics.fill(tab.x() + 20, ty, tab.x() + tab.w(), ty + 18, active ? 0xFF6E1A2A : 0xFF28151D);
            if (active || hovered) {
                int outline = active ? lerpBioColor(0xFFB5E48C, 0xFFE9FF70, pulse) : 0xFFD90429;
                graphics.renderOutline(tab.x(), ty, tab.w(), 18, outline);
            }
            graphics.pose().pushPose();
            graphics.pose().translate(tab.x() + 4, ty + 1, 0);
            graphics.renderItem(tab.icon(), 0, 0);
            graphics.pose().popPose();

            if (hovered) {
                this.hoveredCustomTooltip = Component.translatable("gui.mycelialmind.codex.chapter_header",
                        tab.chapter().number(), Component.translatable(tab.chapter().titleKey()));
            }
        }
    }

    private void renderBookTomeFrame(GuiGraphics graphics, int left, int top, float threatLevel, float pulse) {
        // Outer necrotic chitin shell & pulsing Hive crimson-spore border
        graphics.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 0xCC070408);
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF160B12);
        int outerBorder = threatLevel >= 50.0F
                ? lerpBioColor(0xFF7A1C2A, 0xFFD90429, pulse)
                : lerpBioColor(0xFF5E2B38, 0xFF9E2A3B, pulse);
        int innerBorder = lerpBioColor(0xFF8C7E70, 0xFFB5E48C, pulse * (threatLevel >= 50.0F ? 0.55F : 0.25F));
        graphics.renderOutline(left, top, PANEL_W, PANEL_H, outerBorder);
        graphics.renderOutline(left + 2, top + 2, PANEL_W - 4, PANEL_H - 4, innerBorder);

        // Pulsing arterial hyphal veins around tome perimeter
        int veinGlow = lerpBioColor(0xFF6B1422, 0xFFD90429, pulse);
        graphics.fill(left + 12, top + 4, left + PANEL_W - 12, top + 5, veinGlow);
        graphics.fill(left + 12, top + PANEL_H - 5, left + PANEL_W - 12, top + PANEL_H - 4, veinGlow);

        // Left index membrane panel sampled from bio-horror codex_book.png
        int indexX = left + INDEX_X_OFFSET;
        int indexY = top + 28;
        int indexH = PANEL_H - 56;
        graphics.blit(CODEX_GUI_TEXTURE, indexX, indexY, INDEX_W, indexH, 9, 9, 76, 182, 256, 256);
        graphics.renderOutline(indexX, indexY, INDEX_W, indexH, 0xFF6E2534);

        // Central vertebral sinew & Watcher-Eyes spine sampled from codex_book.png
        int spineX = left + SPINE_X_OFFSET;
        graphics.blit(CODEX_GUI_TEXTURE, spineX, top + 6, SPINE_W, PANEL_H - 12, 85, 7, 7, 186, 256, 256);

        // Subtle rhythmic eye-glow pulse over the spine when Hive Threat is high
        if (threatLevel >= 50.0F) {
            int eyeAlpha = Math.round(40 + 70 * pulse) << 24;
            graphics.fill(spineX + 1, top + 14, spineX + SPINE_W - 1, top + PANEL_H - 14, eyeAlpha | 0xD90429);
        }

        // Right necrotic parchment spread sampled from codex_book.png
        int pageX = left + PAGE_X_OFFSET;
        int pageY = top + 28;
        int pageH = PANEL_H - 56;
        graphics.blit(CODEX_GUI_TEXTURE, pageX, pageY, PAGE_W, pageH, 92, 9, 155, 182, 256, 256);
        graphics.renderOutline(pageX, pageY, PAGE_W, pageH, 0xFF8C2A3B);

        // Four chitin bone & spore-pustule corner claws from codex_book.png
        graphics.blit(CODEX_GUI_TEXTURE, left + 1, top + 1, 2, 2, 10, 10, 256, 256);
        graphics.blit(CODEX_GUI_TEXTURE, left + PANEL_W - 11, top + 1, 243, 2, 10, 10, 256, 256);
        graphics.blit(CODEX_GUI_TEXTURE, left + 1, top + PANEL_H - 11, 2, 187, 10, 10, 256, 256);
        graphics.blit(CODEX_GUI_TEXTURE, left + PANEL_W - 11, top + PANEL_H - 11, 243, 187, 10, 10, 256, 256);
    }

    private void renderLeftIndexSpread(GuiGraphics graphics, int left, int top, CodexBookData.Chapter ch, int mouseX, int mouseY, float pulse) {
        int indexX = left + INDEX_X_OFFSET;
        int indexY = top + 28;

        // Hive Eye medallion + unlocked entry counter
        int unlockedCount = 0;
        for (CodexBookData.Entry e : ch.entries()) {
            if (isGateUnlocked(e.unlockGate())) {
                unlockedCount++;
            }
        }
        graphics.blit(CODEX_GUI_TEXTURE, indexX + 4, indexY + 3, 12, 12, 112, 204, 16, 16, 256, 256);
        String chBadge = "#" + ch.number() + "  [" + unlockedCount + "/" + ch.entries().size() + "]";
        drawScaledText(graphics, Component.literal(chBadge), indexX + 19, indexY + 5, INDEX_W - 24, 0xFFD8D0C2, true);

        // Pulsing search input well
        int sx = indexX + 3;
        int sy = top + 45;
        int sw = INDEX_W - 6;
        graphics.fill(sx, sy, sx + sw, sy + 15, 0xCC12080E);
        boolean searchFocused = this.searchBox != null && this.searchBox.isFocused();
        int searchBorder = searchFocused ? lerpBioColor(0xFFB5E48C, 0xFFE9FF70, pulse) : 0xFF6E2534;
        graphics.renderOutline(sx, sy, sw, 15, searchBorder);

        for (EntryCardHitbox card : this.entryCards) {
            boolean selected = card.chapterIndex() == this.chapterIdx && card.entryIndex() == this.entryIdx;
            boolean hovered = card.contains(mouseX, mouseY);

            int bg = selected ? 0xCC4A1624 : (hovered ? 0xB032121D : 0x88160B11);
            int border = selected ? 0xFFB5E48C : (hovered ? 0xFFD90429 : 0xFF54202D);
            graphics.fill(card.x(), card.y(), card.x() + card.w(), card.y() + card.h(), bg);
            graphics.renderOutline(card.x(), card.y(), card.w(), card.h(), border);

            if (selected) {
                graphics.fill(card.x(), card.y(), card.x() + 2, card.y() + card.h(), 0xFFB5E48C);
            }

            graphics.pose().pushPose();
            graphics.pose().translate(card.x() + 3, card.y() + 2.5F, 0);
            graphics.pose().scale(0.75F, 0.75F, 1.0F);
            graphics.renderItem(card.icon(), 0, 0);
            graphics.pose().popPose();

            String prefix = card.fromSearch()
                    ? ("#" + (card.chapterIndex() + 1) + " ")
                    : (card.unlocked() ? "" : "[?] ");
            Component titleComp = Component.literal(prefix).append(Component.translatable(card.entry().titleKey()));
            int textColor = !card.unlocked() ? 0xFFFFB703 : (selected ? 0xFFBAD868 : 0xFFEBE4E0);
            drawClippedOrScaledText(graphics, titleComp, card.x() + 18, card.y() + 5, card.w() - 21, textColor, selected);

            if (hovered) {
                this.hoveredCustomTooltip = Component.translatable(card.entry().titleKey());
            }
        }
    }

    private void renderRightReadingSpread(GuiGraphics graphics, int left, int top, CodexBookData.Chapter ch, int mouseX, int mouseY, float pulse) {
        // Chapter Header Banner on top bar
        Component chHeader = Component.translatable("gui.mycelialmind.codex.chapter_header",
                ch.number(), Component.translatable(ch.titleKey()));
        drawScaledText(graphics, chHeader, left + PAGE_X_OFFSET, top + 12, 114, 0xFFE8D8D0, true);

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
            graphics.fill(contentX, pageTop + 20, contentX + contentW, pageTop + 21, 0xFF8C2A3B);

            int boxY = pageTop + 30;
            graphics.fill(contentX, boxY, contentX + contentW, boxY + 46, 0xAA261015);
            graphics.renderOutline(contentX, boxY, contentW, 46, 0xFFD90429);
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

        drawScaledText(graphics, Component.translatable(page.titleKey()), contentX + 17, pageTop + 3, contentW - 18, 0xFFBAD868, true);
        int ruleColor = lerpBioColor(0xFF7A1C2A, 0xFFD90429, pulse);
        graphics.fill(contentX, pageTop + 15, contentX + contentW, pageTop + 16, ruleColor);
        graphics.fill(contentX + contentW / 2 - 8, pageTop + 15, contentX + contentW / 2 + 8, pageTop + 16, 0xFFB5E48C);

        int textY = pageTop + 20;
        String type = page.type() != null ? page.type() : "text";

        if ("spotlight".equals(type) && page.itemId() != null) {
            textY = renderSpotlightWidget(graphics, page, contentX, textY, contentW, mouseX, mouseY);
        } else if ("recipe".equals(type) && page.recipeId() != null) {
            textY = renderRecipeWidget(graphics, page, contentX, textY, contentW, mouseX, mouseY);
        } else if ("entity".equals(type) && page.entityId() != null) {
            textY = renderEntityWidget(graphics, page, contentX, textY, contentW, mouseX, mouseY, pulse);
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

    private int renderEntityWidget(GuiGraphics graphics, CodexBookData.Page page, int contentX, int y, int contentW, int mouseX, int mouseY, float pulse) {
        int boxW = 54;
        int boxH = 50;
        graphics.fill(contentX, y, contentX + boxW, y + boxH, 0xCC12070C);
        graphics.renderOutline(contentX, y, boxW, boxH, lerpBioColor(0xFF9E2A3B, 0xFFB5E48C, pulse));
        // Biological specimen floor & organic scan lines inside containment box
        graphics.fill(contentX + 2, y + boxH - 10, contentX + boxW - 2, y + boxH - 3, 0x6640151F);
        int scanY = y + 3 + (int) (((System.currentTimeMillis() / 35L) % Math.max(1, boxH - 8)));
        graphics.fill(contentX + 2, scanY, contentX + boxW - 2, scanY + 1, 0x44B5E48C);

        LivingEntity living = getOrCreatePreviewEntity(page.entityId());
        if (living != null) {
            try {
                float bbMax = Math.max(0.8F, Math.max(living.getBbWidth(), living.getBbHeight()));
                float pageScaleFactor = Mth.clamp(page.entityScale(), 0.55F, 1.35F);
                int entityScale = Mth.clamp(Math.round((24.0F * pageScaleFactor) / bbMax), 9, 24);
                boolean hoverBox = mouseX >= contentX && mouseX < contentX + boxW && mouseY >= y && mouseY < y + boxH;
                float angleX = hoverBox
                        ? (float) Math.atan((contentX + boxW / 2.0F - mouseX) / 40.0F)
                        : (float) Math.sin(System.currentTimeMillis() * 0.0012) * 0.55F;
                float angleY = hoverBox
                        ? (float) Math.atan((y + boxH / 2.0F - mouseY) / 40.0F)
                        : -0.08F;
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

        int infoX = contentX + boxW + 6;
        int infoW = contentW - boxW - 6;
        graphics.fill(infoX, y, infoX + infoW, y + boxH, 0x77160A10);
        graphics.renderOutline(infoX, y, infoW, boxH, 0xFF6E2534);

        Component badge = Component.translatable("gui.mycelialmind.codex.entity_badge", page.entityId());
        drawScaledText(graphics, badge, infoX + 4, y + 5, infoW - 8, 0xFFBAD868, true);

        Component entityDisplayName = (page.entityNameKey() != null && !page.entityNameKey().isBlank())
                ? Component.translatable(page.entityNameKey())
                : (living != null ? living.getName() : Component.literal(page.entityId()));
        drawScaledText(graphics, entityDisplayName, infoX + 4, y + 17, infoW - 8, 0xFFEDE7F6, true);

        if (living != null) {
            String hpText = "HP: " + Math.round(living.getMaxHealth()) + "  |  "
                    + String.format(Locale.ROOT, "%.1fm x %.1fm", living.getBbWidth(), living.getBbHeight());
            drawScaledText(graphics, Component.literal(hpText), infoX + 4, y + 29, infoW - 8, 0xFFFFD166, false);
        }
        drawScaledText(graphics, Component.literal(page.entityId()), infoX + 4, y + 40, infoW - 8, 0xFFB08A96, false);

        return y + boxH + 4;
    }

    private static int lerpBioColor(int argbA, int argbB, float t) {
        float clamped = Mth.clamp(t, 0.0F, 1.0F);
        int a = Math.round(((argbA >>> 24) & 0xFF) * (1.0F - clamped) + ((argbB >>> 24) & 0xFF) * clamped);
        int r = Math.round(((argbA >>> 16) & 0xFF) * (1.0F - clamped) + ((argbB >>> 16) & 0xFF) * clamped);
        int g = Math.round(((argbA >>> 8) & 0xFF) * (1.0F - clamped) + ((argbB >>> 8) & 0xFF) * clamped);
        int b = Math.round((argbA & 0xFF) * (1.0F - clamped) + (argbB & 0xFF) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
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
