package com.alex.mycelialmind.client.screen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.data.CodexBookData;
import com.alex.mycelialmind.data.CodexReloadListener;
import com.alex.mycelialmind.hive.FieldEngramId;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.network.ClientDiscoveryCache;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialText;
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
import java.util.function.Supplier;

/**
 * Custom evolving bio-mycelial double-spread Codex Book GUI rendering {@code codex_book.json}
 * across 4 evolutionary Hive Mind atlases ({@code codex_book{,_awakened,_adaptive,_overmind}.png}),
 * 12 interactive chapter fungal ribbon tabs, live cross-book search, scrollable sprite-backed
 * entry index cards, textured buttons, clickable Act Medallion previewer, live synaptic spore
 * network, recipe crafting/station grids with hover tooltips, 3D rotating entity containment
 * previews, callout frames, and synaptic audio cues.
 */
public class MycelialCodexBookScreen extends Screen {
    private static final ResourceLocation ATLAS_DORMANT =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/gui/codex_book.png");
    private static final ResourceLocation ATLAS_AWAKENED =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/gui/codex_book_awakened.png");
    private static final ResourceLocation ATLAS_ADAPTIVE =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/gui/codex_book_adaptive.png");
    private static final ResourceLocation ATLAS_OVERMIND =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "textures/gui/codex_book_overmind.png");

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
    private int previewActOverride = -1;
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
     * Sprite-backed chitin & mycelial violet button matching the active evolutionary Codex atlas.
     */
    private static final class CodexButton extends Button {
        private final boolean accent;
        private final Supplier<ResourceLocation> atlasSupplier;

        CodexButton(int x, int y, int w, int h, Component message, OnPress onPress, boolean accent,
                    Supplier<ResourceLocation> atlasSupplier) {
            super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
            this.accent = accent;
            this.atlasSupplier = atlasSupplier;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isHoveredOrFocused() && this.active;
            int u = (hovered || (accent && this.active)) ? 220 : 184;
            graphics.blit(this.atlasSupplier.get(), getX(), getY(), width, height, u, 204, 36, 16, 256, 256);
            int textColor;
            if (!this.active) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x880B0812);
                graphics.renderOutline(getX(), getY(), width, height, 0xFF34244A);
                textColor = 0xFF5D4F73;
            } else if (hovered) {
                graphics.renderOutline(getX(), getY(), width, height, accent ? 0xFFB5E48C : 0xFF72EFDD);
                textColor = 0xFFFFFFFF;
            } else {
                graphics.renderOutline(getX(), getY(), width, height, accent ? 0xFF9B4DFF : 0xFF593D82);
                textColor = accent ? 0xFFB5E48C : 0xFFEDE7F6;
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

    public static ResourceLocation atlasForTier(@Nullable HiveProgressionTier tier) {
        if (tier == null) {
            return ATLAS_DORMANT;
        }
        return switch (tier) {
            case DORMANT -> ATLAS_DORMANT;
            case AWAKENED -> ATLAS_AWAKENED;
            case ADAPTIVE -> ATLAS_ADAPTIVE;
            case OVERMIND -> ATLAS_OVERMIND;
        };
    }

    public static int cyclePreviewActTier(int currentOverride, int effectiveActLevel, boolean allowAllActs) {
        int maxAllowed = Mth.clamp(allowAllActs ? 3 : effectiveActLevel, 0, 3);
        if (currentOverride < 0) {
            return maxAllowed > 0 ? Math.min(maxAllowed, effectiveActLevel == 0 ? 1 : 0) : 0;
        }
        int next = currentOverride + 1;
        return next > maxAllowed ? -1 : next;
    }

    private HiveProgressionTier resolveEffectiveHiveTier() {
        return HiveProgressionTier.resolveEffectiveTier(
                ClientHiveCache.getThreatLevel(),
                ClientDiscoveryCache.getEffectiveActTier().level()
        );
    }

    private HiveProgressionTier resolveDisplayedTier() {
        HiveProgressionTier effective = resolveEffectiveHiveTier();
        if (this.previewActOverride >= 0 && this.previewActOverride < HiveProgressionTier.values().length) {
            return HiveProgressionTier.values()[this.previewActOverride];
        }
        return effective;
    }

    private ResourceLocation getActiveAtlas() {
        return atlasForTier(resolveDisplayedTier());
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
        CodexBookData.Chapter activeChapter = book.chapters().get(this.chapterIdx);
        List<CodexBookData.Entry> entries = activeChapter.entries();
        if (entries.isEmpty()) {
            this.entryIdx = 0;
            this.pageIdx = 0;
        } else {
            this.entryIdx = Mth.clamp(this.entryIdx, 0, entries.size() - 1);
            CodexBookData.Entry currentEntry = entries.get(this.entryIdx);
            int pages = Math.max(1, currentEntry.pages().size());
            this.pageIdx = Mth.clamp(this.pageIdx, 0, pages - 1);
            if (isGateUnlocked(currentEntry.unlockGate())) {
                ClientUnreadCodexTracker.markRead(activeChapter.id(), currentEntry.id());
            }
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
                    CommonComponents.GUI_DONE, b -> onClose(), true, this::getActiveAtlas));
            return;
        }

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Top-bar Chapter < / > cycle buttons
        this.addRenderableWidget(new CodexButton(left + 12, top + 8, 52, 16,
                Component.translatable("gui.mycelialmind.codex.prev_chapter"),
                b -> selectChapter((chapterIdx - 1 + book.chapters().size()) % book.chapters().size()), false, this::getActiveAtlas));

        this.addRenderableWidget(new CodexButton(left + 72, top + 8, 52, 16,
                Component.translatable("gui.mycelialmind.codex.next_chapter"),
                b -> selectChapter((chapterIdx + 1) % book.chapters().size()), false, this::getActiveAtlas));

        // Spoiler toggle button
        this.addRenderableWidget(new CodexButton(left + PANEL_W - 114, top + 8, 82, 16,
                Component.translatable(showSpoilers
                        ? "gui.mycelialmind.codex.spoilers_on"
                        : "gui.mycelialmind.codex.spoilers_off"),
                b -> {
                    showSpoilers = !showSpoilers;
                    persistReadingPosition();
                    playUiSound(ModSounds.BIO_SCANNER_PING.get(), 1.15F);
                    rebuildNav();
                }, showSpoilers, this::getActiveAtlas));

        // Close button
        this.addRenderableWidget(new CodexButton(left + PANEL_W - 28, top + 8, 18, 16,
                Component.literal("X"), b -> onClose(), false, this::getActiveAtlas));

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
                    }, true, this::getActiveAtlas));
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
                Component.literal("<"), b -> turnPage(-1), false, this::getActiveAtlas));

        this.nextPageBtn = this.addRenderableWidget(new CodexButton(left + PANEL_W - 42, top + PANEL_H - 25, 28, 16,
                Component.literal(">"), b -> turnPage(1), false, this::getActiveAtlas));
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
        CodexBookData.Chapter ch = book.chapters().get(this.chapterIdx);
        if (!ch.entries().isEmpty()) {
            CodexBookData.Entry firstEntry = ch.entries().get(0);
            if (isGateUnlocked(firstEntry.unlockGate())) {
                ClientUnreadCodexTracker.markRead(ch.id(), firstEntry.id());
            }
        }
        persistReadingPosition();
        playUiSound(ModSounds.SYNAPTIC_PULSE.get(), 1.05F);
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
        CodexBookData.Chapter ch = book.chapters().get(this.chapterIdx);
        CodexBookData.Entry curEntry = entries.get(this.entryIdx);
        if (isGateUnlocked(curEntry.unlockGate())) {
            ClientUnreadCodexTracker.markRead(ch.id(), curEntry.id());
        }
        persistReadingPosition();
        updatePageButtonStates();
        playUiSound(SoundEvents.BOOK_PAGE_TURN, 1.04F);
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
            playUiSound(SoundEvents.BOOK_PAGE_TURN, delta > 0 ? 1.02F : 0.96F);
        }
    }

    private static void playUiSound(SoundEvent sound, float pitch) {
        if (!com.alex.mycelialmind.config.MycelialClientConfig.uiAudioFeedbackEnabled()) {
            return;
        }
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
                mc.player != null && mc.player.isCreative(),
                ClientDiscoveryCache.getDiscoveredEngramsMask(),
                ClientHiveCache.isMatriarchDefeated()
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int left = (this.width - PANEL_W) / 2;
            int top = (this.height - PANEL_H) / 2;
            int medX = left + INDEX_X_OFFSET + 3;
            int medY = top + 29;
            int telX = left + PAGE_X_OFFSET;
            int telY = top + 8;
            boolean clickedMedallion = mouseX >= medX && mouseX < medX + 18 && mouseY >= medY && mouseY < medY + 18;
            boolean clickedTelemetry = mouseX >= telX && mouseX < telX + 114 && mouseY >= telY && mouseY < telY + 19;
            if (clickedMedallion || clickedTelemetry) {
                Minecraft mc = Minecraft.getInstance();
                boolean allowAll = this.showSpoilers || (mc.player != null && mc.player.isCreative());
                int effectiveLevel = resolveEffectiveHiveTier().level();
                this.previewActOverride = cyclePreviewActTier(this.previewActOverride, effectiveLevel, allowAll);
                playUiSound(ModSounds.SYNAPTIC_PULSE.get(), 1.15F);
                return true;
            }
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
        HiveProgressionTier displayedTier = resolveDisplayedTier();
        ResourceLocation atlas = atlasForTier(displayedTier);

        renderFloatingSpores(graphics, left, top, nowMs, pulse, displayedTier.level());
        renderChapterRibbonTabs(graphics, atlas, mouseX, mouseY, pulse);
        renderBookTomeFrame(graphics, atlas, left, top, threatLevel, pulse);

        if (book == null || book.chapters().isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.mycelialmind.codex.not_loaded"),
                    this.width / 2, this.height / 2, 0xFFFF5555);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        CodexBookData.Chapter ch = book.chapters().get(chapterIdx);
        renderLeftIndexSpread(graphics, atlas, left, top, ch, displayedTier, mouseX, mouseY, pulse);
        renderRightReadingSpread(graphics, atlas, left, top, ch, displayedTier, threatLevel, mouseX, mouseY, pulse);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.searchBox != null && this.searchQuery.isEmpty() && !this.searchBox.isFocused()) {
            graphics.drawString(this.font, "...", this.searchBox.getX() + 2, this.searchBox.getY() + 1, 0xFF7B6796, false);
        }

        if (this.hoveredRecipeOrSpotlightStack != null && !this.hoveredRecipeOrSpotlightStack.isEmpty()) {
            graphics.renderTooltip(this.font, this.hoveredRecipeOrSpotlightStack, mouseX, mouseY);
        } else if (this.hoveredCustomTooltip != null) {
            graphics.renderTooltip(this.font, this.hoveredCustomTooltip, mouseX, mouseY);
        }
    }

    /**
     * Computes an organic Hive-Mind synaptic pulse intensity in [0.0, 1.0] whose cadence
     * and baseline amplitude scale with {@code threatLevel} (0..100).
     */
    public static float computeHeartbeatIntensity(float threatLevel, long timeMs) {
        float threatNorm = Mth.clamp(threatLevel / 100.0F, 0.0F, 1.0F);
        double speedHz = 0.0022 + threatNorm * 0.0048;
        float wave = 0.5F + 0.5F * (float) Math.sin(timeMs * speedHz);
        return Mth.clamp(0.15F * threatNorm + wave * (0.35F + 0.50F * threatNorm), 0.0F, 1.0F);
    }

    private void renderFloatingSpores(GuiGraphics graphics, int left, int top, long nowMs, float pulse, int stage) {
        double t = nowMs * 0.001;
        int w = Math.max(1, this.width);
        int h = Math.max(1, this.height);
        int[] xs = new int[24];
        int[] ys = new int[24];
        boolean[] visible = new boolean[24];
        for (int i = 0; i < 24; i++) {
            float seedX = ((i * 73 + 19) % 100) / 100.0F;
            float seedY = ((i * 41 + 37) % 100) / 100.0F;
            double driftY = (seedY - t * (0.024 + (i % 4) * 0.007)) % 1.0;
            if (driftY < 0.0) driftY += 1.0;
            double swayX = seedX + Math.sin(t * 0.85 + i * 1.6) * 0.028;
            int sx = Mth.clamp((int) (swayX * w), 2, w - 4);
            int sy = Mth.clamp((int) (driftY * h), 2, h - 4);
            xs[i] = sx;
            ys[i] = sy;
            boolean insideReadZone = sx > left + INDEX_X_OFFSET + 4
                    && sx < left + PANEL_W - 8
                    && sy > top + 28
                    && sy < top + PANEL_H - 26;
            if (insideReadZone && (i % 3 != 0)) {
                continue;
            }
            visible[i] = true;
            int size = (i % 5 == 0) ? 3 : 2;
            int alpha = Math.round(60 + 75 * pulse) << 24;
            int rgb = switch (i % 3) {
                case 0 -> 0xB5E48C;
                case 1 -> 0x72EFDD;
                default -> 0x9B4DFF;
            };
            graphics.fill(sx, sy, sx + size, sy + size, alpha | rgb);
        }
        // Connect nearby outer synaptic spores with dotted luminous axons from Awakened onward
        if (stage >= 1) {
            int maxDistSq = (42 + stage * 22) * (42 + stage * 22);
            int linkAlpha = Math.round(28 + stage * 18 + pulse * 30) << 24;
            int linksDrawn = 0;
            int maxLinks = 10 + stage * 8;
            for (int i = 0; i < 24 && linksDrawn < maxLinks; i++) {
                if (!visible[i]) continue;
                for (int j = i + 1; j < 24 && linksDrawn < maxLinks; j++) {
                    if (!visible[j]) continue;
                    int dx = xs[j] - xs[i];
                    int dy = ys[j] - ys[i];
                    if (dx * dx + dy * dy <= maxDistSq) {
                        int linkCol = linkAlpha | ((linksDrawn % 2 == 0) ? 0x72EFDD : 0xB5E48C);
                        for (int s = 1; s < 5; s++) {
                            int lx = xs[i] + (dx * s) / 5;
                            int ly = ys[i] + (dy * s) / 5;
                            graphics.fill(lx, ly, lx + 1, ly + 1, linkCol);
                        }
                        linksDrawn++;
                    }
                }
            }
        }
    }

    private void renderChapterRibbonTabs(GuiGraphics graphics, ResourceLocation atlas, int mouseX, int mouseY, float pulse) {
        for (ChapterTabHitbox tab : this.chapterTabs) {
            boolean active = tab.index() == this.chapterIdx;
            boolean hovered = tab.contains(mouseX, mouseY);
            int ty = active ? tab.y() - 2 : (hovered ? tab.y() - 1 : tab.y());
            int u = active ? 90 : 68;
            graphics.blit(atlas, tab.x(), ty, u, 204, 20, 18, 256, 256);
            graphics.fill(tab.x() + 20, ty, tab.x() + tab.w(), ty + 18, active ? 0xFF5A3286 : 0xFF281D3A);
            if (active || hovered) {
                int outline = active ? lerpBioColor(0xFFB5E48C, 0xFF72EFDD, pulse) : 0xFF9B4DFF;
                graphics.renderOutline(tab.x(), ty, tab.w(), 18, outline);
            }
            graphics.pose().pushPose();
            graphics.pose().translate(tab.x() + 4, ty + 1, 0);
            graphics.renderItem(tab.icon(), 0, 0);
            graphics.pose().popPose();

            if (ClientUnreadCodexTracker.hasUnreadInChapter(tab.chapter().id())) {
                int bx = tab.x() + tab.w() - 7;
                int by = ty - 2;
                graphics.fill(bx, by, bx + 7, by + 9, 0xEE140C1C);
                graphics.renderOutline(bx, by, 7, 9, 0xFF72EFDD);
                graphics.drawString(this.font, "!", bx + 2, by + 1, 0xFF72EFDD, false);
            }

            if (hovered) {
                this.hoveredCustomTooltip = Component.translatable("gui.mycelialmind.codex.chapter_header",
                        tab.chapter().number(), Component.translatable(tab.chapter().titleKey()));
            }
        }
    }

    private void renderBookTomeFrame(GuiGraphics graphics, ResourceLocation atlas, int left, int top, float threatLevel, float pulse) {
        graphics.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 0xCC09060E);
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF171124);
        int outerBorder = threatLevel >= 50.0F
                ? lerpBioColor(0xFF7B2CBF, 0xFF72EFDD, pulse)
                : lerpBioColor(0xFF5A3E85, 0xFF9B4DFF, pulse);
        int innerBorder = lerpBioColor(0xFF9D75CB, 0xFFB5E48C, pulse * (threatLevel >= 50.0F ? 0.55F : 0.30F));
        graphics.renderOutline(left, top, PANEL_W, PANEL_H, outerBorder);
        graphics.renderOutline(left + 2, top + 2, PANEL_W - 4, PANEL_H - 4, innerBorder);

        int axonGlow = lerpBioColor(0xFF6B3FA0, 0xFF72EFDD, pulse);
        graphics.fill(left + 12, top + 4, left + PANEL_W - 12, top + 5, axonGlow);
        graphics.fill(left + 12, top + PANEL_H - 5, left + PANEL_W - 12, top + PANEL_H - 4, axonGlow);

        int indexX = left + INDEX_X_OFFSET;
        int indexY = top + 28;
        int indexH = PANEL_H - 56;
        graphics.blit(atlas, indexX, indexY, INDEX_W, indexH, 9, 9, 76, 182, 256, 256);
        graphics.renderOutline(indexX, indexY, INDEX_W, indexH, 0xFF5A3E85);

        int spineX = left + SPINE_X_OFFSET;
        graphics.blit(atlas, spineX, top + 6, SPINE_W, PANEL_H - 12, 85, 7, 7, 186, 256, 256);

        if (threatLevel >= 50.0F) {
            int surgeAlpha = Math.round(36 + 64 * pulse) << 24;
            graphics.fill(spineX + 1, top + 14, spineX + SPINE_W - 1, top + PANEL_H - 14, surgeAlpha | 0x72EFDD);
        }

        int pageX = left + PAGE_X_OFFSET;
        int pageY = top + 28;
        int pageH = PANEL_H - 56;
        graphics.blit(atlas, pageX, pageY, PAGE_W, pageH, 92, 9, 155, 182, 256, 256);
        graphics.renderOutline(pageX, pageY, PAGE_W, pageH, 0xFF7A56B0);

        graphics.blit(atlas, left + 1, top + 1, 2, 2, 10, 10, 256, 256);
        graphics.blit(atlas, left + PANEL_W - 11, top + 1, 243, 2, 10, 10, 256, 256);
        graphics.blit(atlas, left + 1, top + PANEL_H - 11, 2, 187, 10, 10, 256, 256);
        graphics.blit(atlas, left + PANEL_W - 11, top + PANEL_H - 11, 243, 187, 10, 10, 256, 256);
    }

    private void renderLeftIndexSpread(GuiGraphics graphics, ResourceLocation atlas, int left, int top, CodexBookData.Chapter ch, HiveProgressionTier displayedTier, int mouseX, int mouseY, float pulse) {
        int indexX = left + INDEX_X_OFFSET;
        int indexY = top + 28;

        int unlockedCount = 0;
        for (CodexBookData.Entry e : ch.entries()) {
            if (isGateUnlocked(e.unlockGate())) {
                unlockedCount++;
            }
        }
        int medU = 112 + displayedTier.level() * 18;
        graphics.blit(atlas, indexX + 3, indexY + 2, 14, 14, medU, 204, 16, 16, 256, 256);
        String chBadge = "#" + ch.number() + "  [" + unlockedCount + "/" + ch.entries().size() + "]";
        drawScaledText(graphics, Component.literal(chBadge), indexX + 20, indexY + 5, INDEX_W - 24, 0xFFD8B4F8, true);

        if (mouseX >= indexX + 3 && mouseX < indexX + 19 && mouseY >= indexY + 2 && mouseY < indexY + 18) {
            this.hoveredCustomTooltip = buildActTooltip(displayedTier);
        }

        // Textured Search input well (0, 228, 106, 15)
        int sx = indexX + 3;
        int sy = top + 45;
        int sw = INDEX_W - 6;
        graphics.blit(atlas, sx, sy, sw, 15, 0, 228, 106, 15, 256, 256);
        boolean searchFocused = this.searchBox != null && this.searchBox.isFocused();
        int searchBorder = searchFocused ? lerpBioColor(0xFFB5E48C, 0xFF72EFDD, pulse) : 0xFF5A3E85;
        graphics.renderOutline(sx, sy, sw, 15, searchBorder);

        for (EntryCardHitbox card : this.entryCards) {
            boolean selected = card.chapterIndex() == this.chapterIdx && card.entryIndex() == this.entryIdx;
            boolean hovered = card.contains(mouseX, mouseY);

            // Textured entry index card plate (180, 228, 72, 17)
            graphics.blit(atlas, card.x(), card.y(), card.w(), card.h(), 180, 228, 72, 17, 256, 256);
            if (selected) {
                graphics.fill(card.x(), card.y(), card.x() + card.w(), card.y() + card.h(), 0x554A2B78);
            } else if (hovered) {
                graphics.fill(card.x(), card.y(), card.x() + card.w(), card.y() + card.h(), 0x33352056);
            }
            int border = selected ? 0xFFB5E48C : (hovered ? 0xFF72EFDD : 0xFF473168);
            graphics.renderOutline(card.x(), card.y(), card.w(), card.h(), border);

            if (selected) {
                graphics.fill(card.x(), card.y(), card.x() + 2, card.y() + card.h(), 0xFFB5E48C);
            }

            graphics.pose().pushPose();
            graphics.pose().translate(card.x() + 3, card.y() + 2.5F, 0);
            graphics.pose().scale(0.75F, 0.75F, 1.0F);
            graphics.renderItem(card.icon(), 0, 0);
            graphics.pose().popPose();

            boolean unread = card.unlocked() && ClientUnreadCodexTracker.isEntryUnread(
                    book.chapters().get(card.chapterIndex()).id(), card.entry().id());
            String prefix = card.fromSearch()
                    ? ("#" + (card.chapterIndex() + 1) + " ")
                    : (card.unlocked() ? "" : "[?] ");
            Component titleComp = Component.literal(prefix).append(Component.translatable(card.entry().titleKey()));
            int textColor = !card.unlocked() ? 0xFFFFB703 : (selected ? 0xFFB5E48C : 0xFFEDE7F6);
            int badgeReserve = 0;
            if (unread) {
                Component newBadge = Component.translatable("gui.mycelialmind.codex.new_badge");
                int bw = this.font.width(newBadge) + 6;
                int bx = card.x() + card.w() - bw - 3;
                int by = card.y() + 4;
                graphics.fill(bx, by, bx + bw, by + 10, 0xDD103B38);
                graphics.renderOutline(bx, by, bw, 10, 0xFF72EFDD);
                graphics.drawString(this.font, newBadge, bx + 3, by + 1, 0xFF72EFDD, false);
                badgeReserve = bw + 4;
            }
            drawClippedOrScaledText(graphics, titleComp, card.x() + 18, card.y() + 5, card.w() - 21 - badgeReserve, textColor, selected);

            if (hovered) {
                this.hoveredCustomTooltip = Component.translatable(card.entry().titleKey());
            }
        }
    }

    private Component buildActTooltip(HiveProgressionTier tier) {
        String mode = this.previewActOverride >= 0 ? "Preview" : "Live";
        return Component.literal(String.format(
                Locale.ROOT,
                "Act %d (%s) [%s] - HP x%.2f  DMG x%.2f  ARM +%.0f  SPD x%.2f",
                tier.actNumber(),
                MycelialText.progressionTier(tier).getString(),
                mode,
                tier.healthMultiplier(),
                tier.damageMultiplier(),
                tier.armorBonus(),
                tier.speedMultiplier()
        ));
    }

    private void renderRightReadingSpread(GuiGraphics graphics, ResourceLocation atlas, int left, int top, CodexBookData.Chapter ch, HiveProgressionTier displayedTier, float threatLevel, int mouseX, int mouseY, float pulse) {
        Component chHeader = Component.translatable("gui.mycelialmind.codex.chapter_header",
                ch.number(), Component.translatable(ch.titleKey()));
        drawScaledText(graphics, chHeader, left + PAGE_X_OFFSET, top + 8, 114, 0xFFE0C3FC, true);
        renderSynapticHeaderTelemetry(graphics, left + PAGE_X_OFFSET, top + 18, 114, displayedTier, threatLevel, pulse);

        if (mouseX >= left + PAGE_X_OFFSET && mouseX < left + PAGE_X_OFFSET + 114 && mouseY >= top + 8 && mouseY < top + 27) {
            this.hoveredCustomTooltip = buildActTooltip(displayedTier);
        }

        if (ch.entries().isEmpty()) {
            return;
        }

        CodexBookData.Entry entry = ch.entries().get(Math.min(entryIdx, ch.entries().size() - 1));
        int contentX = left + PAGE_X_OFFSET + 8;
        int contentW = PAGE_W - 16;
        int pageTop = top + 33;
        int pageBottom = top + PANEL_H - 32;

        if (!isGateUnlocked(entry.unlockGate())) {
            int medU = 112 + displayedTier.level() * 18;
            graphics.blit(atlas, contentX, pageTop, 16, 16, medU, 204, 16, 16, 256, 256);
            drawScaledText(graphics, Component.translatable(entry.titleKey()), contentX + 20, pageTop + 4, contentW - 22, 0xFFFFB703, true);
            graphics.fill(contentX, pageTop + 20, contentX + contentW, pageTop + 21, 0xFF7B2CBF);

            Component lockedMsg;
            String gate = entry.unlockGate();
            if (gate.startsWith("engram:")) {
                FieldEngramId engram = FieldEngramId.byIdOrNull(gate.substring("engram:".length()));
                if (engram != null) {
                    lockedMsg = Component.translatable("gui.mycelialmind.codex.locked.engram",
                            Component.translatable(engram.titleKey()),
                            Component.translatable(engram.hintKey()));
                } else {
                    lockedMsg = Component.translatable("gui.mycelialmind.codex.locked", gate);
                }
            } else if ("post_matriarch".equals(gate)) {
                lockedMsg = Component.translatable("gui.mycelialmind.codex.locked.post_matriarch");
            } else {
                lockedMsg = Component.translatable("gui.mycelialmind.codex.locked", gate);
            }

            int boxY = pageTop + 30;
            List<FormattedCharSequence> lines = this.font.split(lockedMsg, contentW - 12);
            int boxH = Math.max(46, lines.size() * 10 + 16);
            graphics.fill(contentX, boxY, contentX + contentW, boxY + boxH, 0xAA211334);
            graphics.renderOutline(contentX, boxY, contentW, boxH, 0xFFFFB703);
            for (int i = 0; i < lines.size(); i++) {
                graphics.drawString(this.font, lines.get(i), contentX + 6, boxY + 8 + i * 10, 0xFFFFD166, false);
            }
            return;
        }

        if (entry.pages().isEmpty()) {
            return;
        }

        CodexBookData.Page page = entry.pages().get(Math.min(pageIdx, entry.pages().size() - 1));

        ItemStack headerIcon = page.itemId() != null ? resolveItem(page.itemId()) : resolveItem(entry.iconItem());
        graphics.pose().pushPose();
        graphics.pose().translate(contentX, pageTop - 1, 0);
        graphics.pose().scale(0.85F, 0.85F, 1.0F);
        graphics.renderItem(headerIcon, 0, 0);
        graphics.pose().popPose();

        drawScaledText(graphics, Component.translatable(page.titleKey()), contentX + 17, pageTop + 3, contentW - 18, 0xFF72EFDD, true);
        int ruleColor = lerpBioColor(0xFF5A3E85, 0xFF9B4DFF, pulse);
        graphics.fill(contentX, pageTop + 15, contentX + contentW, pageTop + 16, ruleColor);
        graphics.fill(contentX + contentW / 2 - 8, pageTop + 15, contentX + contentW / 2 + 8, pageTop + 16, 0xFFB5E48C);

        int textY = pageTop + 20;
        String type = page.type() != null ? page.type() : "text";

        if ("spotlight".equals(type) && page.itemId() != null) {
            textY = renderSpotlightWidget(graphics, atlas, page, contentX, textY, contentW, mouseX, mouseY);
        } else if ("recipe".equals(type) && page.recipeId() != null) {
            textY = renderRecipeWidget(graphics, atlas, page, contentX, textY, contentW, mouseX, mouseY);
        } else if ("entity".equals(type) && page.entityId() != null) {
            textY = renderEntityWidget(graphics, atlas, page, contentX, textY, contentW, mouseX, mouseY, pulse);
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
            renderFormattedBodyText(graphics, atlas, page, type, contentX, textY, contentW, pageBottom);
        }

        String pageCounter = (pageIdx + 1) + " / " + entry.pages().size();
        graphics.drawCenteredString(this.font, pageCounter, left + PAGE_X_OFFSET + PAGE_W / 2, top + PANEL_H - 21, 0xFFD8B4F8);
    }

    private int renderSpotlightWidget(GuiGraphics graphics, ResourceLocation atlas, CodexBookData.Page page, int contentX, int y, int contentW, int mouseX, int mouseY) {
        ItemStack stack = resolveItem(page.itemId());
        int frameX = contentX + 4;
        int frameY = y;
        graphics.blit(atlas, frameX, frameY, 20, 204, 22, 22, 256, 256);
        graphics.renderItem(stack, frameX + 3, frameY + 3);

        drawScaledText(graphics, stack.getHoverName(), frameX + 28, frameY + 3, contentW - 34, 0xFFFFE6A7, true);
        drawScaledText(graphics, Component.literal(page.itemId()), frameX + 28, frameY + 13, contentW - 34, 0xFF9D84B7, false);

        if (mouseX >= frameX && mouseX < frameX + 22 && mouseY >= frameY && mouseY < frameY + 22) {
            this.hoveredRecipeOrSpotlightStack = stack;
        }
        return y + 26;
    }

    private int renderRecipeWidget(GuiGraphics graphics, ResourceLocation atlas, CodexBookData.Page page, int contentX, int y, int contentW, int mouseX, int mouseY) {
        boolean hasSecond = page.recipeId2() != null && !page.recipeId2().isBlank();
        int boxH = 56;
        graphics.fill(contentX, y, contentX + contentW, y + boxH, 0x88130C21);
        graphics.renderOutline(contentX, y, contentW, boxH, 0xFF6B3FA0);

        if (hasSecond) {
            int halfW = (contentW - 6) / 2;
            renderSingleRecipeCard(graphics, atlas, page.recipeId(), contentX + 2, y + 2, halfW, boxH - 4, mouseX, mouseY, true);
            graphics.fill(contentX + halfW + 3, y + 4, contentX + halfW + 4, y + boxH - 4, 0xFF4A306D);
            renderSingleRecipeCard(graphics, atlas, page.recipeId2(), contentX + halfW + 5, y + 2, halfW, boxH - 4, mouseX, mouseY, true);
        } else {
            renderSingleRecipeCard(graphics, atlas, page.recipeId(), contentX + 4, y + 2, contentW - 8, boxH - 4, mouseX, mouseY, false);
        }

        Component badge = Component.translatable("gui.mycelialmind.codex.recipe_badge", page.recipeId());
        drawScaledText(graphics, badge, contentX + 4, y + boxH - 9, contentW - 8, 0xFF9D84B7, false);
        return y + boxH + 4;
    }

    private void renderSingleRecipeCard(
            GuiGraphics graphics,
            ResourceLocation atlas,
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
            graphics.blit(atlas, fx, fy, 20, 204, 22, 22, 256, 256);
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
                    renderScaledSlot(graphics, atlas, ingStack, sx, sy, scale, false, mouseX, mouseY);
                }
            }
            int arrowX = gx0 + 3 * cell + (compact ? 3 : 8);
            int arrowY = gy0 + cell;
            graphics.blit(atlas, arrowX, arrowY, Math.round(18 * scale), Math.round(14 * scale), 44, 204, 22, 16, 256, 256);

            int outX = arrowX + Math.round(20 * scale);
            int outY = gy0 + cell - 2;
            renderScaledSlot(graphics, atlas, result, outX, outY, scale, true, mouseX, mouseY);
        } else {
            int inX = rx + (compact ? 8 : 26);
            int inY = ry + 11;
            ItemStack inStack = ingredients.isEmpty() ? ItemStack.EMPTY : pickCycledIngredient(ingredients.get(0));
            renderScaledSlot(graphics, atlas, inStack, inX, inY, 0.95F, false, mouseX, mouseY);

            int arrowX = inX + 22;
            graphics.blit(atlas, arrowX, inY + 1, 20, 15, 44, 204, 22, 16, 256, 256);

            int outX = arrowX + 24;
            renderScaledSlot(graphics, atlas, result, outX, inY - 2, 0.95F, true, mouseX, mouseY);
        }
    }

    private void renderScaledSlot(
            GuiGraphics graphics,
            ResourceLocation atlas,
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
            graphics.blit(atlas, x, y, boxSize, boxSize, 20, 204, 22, 22, 256, 256);
        } else {
            graphics.blit(atlas, x, y, boxSize, boxSize, 0, 204, 18, 18, 256, 256);
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

    private void renderSynapticHeaderTelemetry(GuiGraphics graphics, int x, int y, int width, HiveProgressionTier tier, float threatLevel, float pulse) {
        float fillFrac = Mth.clamp(threatLevel / 100.0F, 0.0F, 1.0F);
        int barH = 4;
        graphics.fill(x, y, x + width, y + barH, 0xCC110B1D);
        graphics.renderOutline(x, y, width, barH, 0xFF4A3270);
        int fillW = Math.max(fillFrac > 0.01F ? 2 : 0, Math.round((width - 2) * fillFrac));
        int fillColor = switch (tier) {
            case DORMANT -> lerpBioColor(0xFF4CC9F0, 0xFF72EFDD, pulse);
            case AWAKENED -> lerpBioColor(0xFF80ED99, 0xFFB5E48C, pulse);
            case ADAPTIVE -> lerpBioColor(0xFF9B4DFF, 0xFFC77DFF, pulse);
            case OVERMIND -> lerpBioColor(0xFFC77DFF, 0xFF72EFDD, pulse);
        };
        if (fillW > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + fillW, y + barH - 1, fillColor);
        }
        Component tierLabel = MycelialText.progressionTier(tier);
        String prefix = this.previewActOverride >= 0 ? "[Vista] " : "";
        String statText = prefix + tierLabel.getString() + " (" + Math.round(threatLevel) + "%)";
        drawScaledText(graphics, Component.literal(statText), x, y + 5, width, 0xFF9D84B7, false);
    }

    private int renderEntityWidget(GuiGraphics graphics, ResourceLocation atlas, CodexBookData.Page page, int contentX, int y, int contentW, int mouseX, int mouseY, float pulse) {
        int boxW = 54;
        int boxH = 50;
        // Textured Bio-Resonance Specimen Containment Chamber (108, 228, 54, 28)
        graphics.blit(atlas, contentX, y, boxW, boxH, 108, 228, 54, 28, 256, 256);
        graphics.renderOutline(contentX, y, boxW, boxH, lerpBioColor(0xFF72EFDD, 0xFFB5E48C, pulse));
        int scanY = y + 3 + (int) (((System.currentTimeMillis() / 35L) % Math.max(1, boxH - 8)));
        graphics.fill(contentX + 2, scanY, contentX + boxW - 2, scanY + 1, 0x5572EFDD);

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
        graphics.fill(infoX, y, infoX + infoW, y + boxH, 0x77160F26);
        graphics.renderOutline(infoX, y, infoW, boxH, 0xFF483270);

        Component badge = Component.translatable("gui.mycelialmind.codex.entity_badge", page.entityId());
        drawScaledText(graphics, badge, infoX + 4, y + 5, infoW - 8, 0xFF72EFDD, true);

        Component entityDisplayName = (page.entityNameKey() != null && !page.entityNameKey().isBlank())
                ? Component.translatable(page.entityNameKey())
                : (living != null ? living.getName() : Component.literal(page.entityId()));
        drawScaledText(graphics, entityDisplayName, infoX + 4, y + 17, infoW - 8, 0xFFEDE7F6, true);

        if (living != null) {
            String hpText = "HP: " + Math.round(living.getMaxHealth()) + "  |  "
                    + String.format(Locale.ROOT, "%.1fm x %.1fm", living.getBbWidth(), living.getBbHeight());
            drawScaledText(graphics, Component.literal(hpText), infoX + 4, y + 29, infoW - 8, 0xFFB5E48C, false);
        }
        drawScaledText(graphics, Component.literal(page.entityId()), infoX + 4, y + 40, infoW - 8, 0xFF9D84B7, false);

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
            ResourceLocation atlas,
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

        // Textured scrollbar track (164, 228, 6, 24) & spore thumb (172, 228, 6, 10)
        if (this.maxTextScroll > 0) {
            int barX = contentX + contentW - 5;
            int barTop = textY;
            int barH = visibleLines * lineStep;
            graphics.blit(atlas, barX, barTop, 5, barH, 164, 228, 6, 24, 256, 256);
            int thumbH = Math.max(10, (barH * visibleLines) / lines.size());
            int thumbY = barTop + ((barH - thumbH) * this.textScrollOffset) / this.maxTextScroll;
            graphics.blit(atlas, barX, thumbY, 5, thumbH, 172, 228, 6, 10, 256, 256);
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
