package com.alex.mycelialmind.client;

import com.alex.mycelialmind.client.audio.ClientAcousticController;
import com.alex.mycelialmind.client.audio.ClientAudioDiagnostics;
import com.alex.mycelialmind.client.screen.ClientCodexOpener;
import com.alex.mycelialmind.compat.curios.MycelialCuriosCompat;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.network.ClientColonyCache;
import com.alex.mycelialmind.network.ClientCycleCache;
import com.alex.mycelialmind.network.ClientDiscoveryCache;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.network.ClientSymbiosisCache;
import com.alex.mycelialmind.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.Locale;

/**
 * Registers permission-free client commands under {@code /mycelialclient} via
 * {@link RegisterClientCommandsEvent} for HUD preferences, audio telemetry, and Codex access.
 */
public final class MycelialClientCommands {
    private MycelialClientCommands() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        var anchorNode = Commands.literal("anchor");
        for (MycelialClientConfig.HudAnchor anchor : MycelialClientConfig.HudAnchor.values()) {
            String literal = anchor.name().toLowerCase(Locale.ROOT);
            anchorNode.then(Commands.literal(literal)
                    .executes(ctx -> setHudAnchor(ctx.getSource(), anchor)));
        }

        dispatcher.register(Commands.literal("mycelialclient")
                .then(Commands.literal("status")
                        .executes(ctx -> showStatus(ctx.getSource())))
                .then(Commands.literal("hud")
                        .then(Commands.literal("compact")
                                .executes(ctx -> toggleCompactHud(ctx.getSource())))
                        .then(anchorNode))
                .then(Commands.literal("audio")
                        .then(Commands.literal("status")
                                .executes(ctx -> showAudioStatus(ctx.getSource())))
                        .then(Commands.literal("reset")
                                .executes(ctx -> resetAudio(ctx.getSource()))))
                .then(Commands.literal("codex")
                        .executes(ctx -> openCodex(ctx.getSource()))));
    }

    private static int showStatus(CommandSourceStack source) {
        String threat = String.format(Locale.ROOT, "%.1f%%", ClientHiveCache.getThreatLevel());
        String actName = ClientDiscoveryCache.getEffectiveActTier().name();
        int traits = ClientHiveCache.getActiveTraits().size();
        int tier2 = ClientHiveCache.getTier2Traits().size();
        int mutagens = ClientSymbiosisCache.getEquippedMutagens().size();
        String colony = ClientColonyCache.hasColony()
                ? "T" + ClientColonyCache.getTier() + " (" + ClientColonyCache.getWorkers().size() + ")"
                : "none";
        String cycle = ClientCycleCache.isCycleActive()
                ? "#" + ClientCycleCache.getActiveCycleNumber()
                : String.valueOf(ClientCycleCache.getCompletedCycles());

        source.sendSuccess(() -> Component.translatable(
                "command.mycelialmind.client.status",
                threat, actName, traits, tier2, mutagens, colony, cycle
        ).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int toggleCompactHud(CommandSourceStack source) {
        boolean next = !MycelialClientConfig.HUD_COMPACT_MODE.get();
        MycelialClientConfig.HUD_COMPACT_MODE.set(next);
        MycelialClientConfig.SPEC.save();
        source.sendSuccess(() -> Component.translatable(
                next ? "command.mycelialmind.client.hud_compact_on" : "command.mycelialmind.client.hud_compact_off"
        ).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int setHudAnchor(CommandSourceStack source, MycelialClientConfig.HudAnchor anchor) {
        MycelialClientConfig.HUD_ANCHOR.set(anchor);
        MycelialClientConfig.SPEC.save();
        source.sendSuccess(() -> Component.translatable(
                "command.mycelialmind.client.hud_anchor",
                anchor.getTranslatedName()
        ).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int showAudioStatus(CommandSourceStack source) {
        String telemetry = ClientAudioDiagnostics.formatClientTelemetry();
        source.sendSuccess(() -> Component.translatable(
                "command.mycelialmind.client.audio_status",
                telemetry
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int resetAudio(CommandSourceStack source) {
        ClientSoundscapeDirector.resetSession();
        ClientAcousticController.resetSession();
        source.sendSuccess(() -> Component.translatable("command.mycelialmind.client.audio_reset")
                .withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int openCodex(CommandSourceStack source) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return 0;
        }
        if (!hasScanner(player)) {
            source.sendFailure(Component.translatable("command.mycelialmind.client.codex_requires_scanner"));
            return 0;
        }
        mc.execute(ClientCodexOpener::openBioCodex);
        return 1;
    }

    private static boolean hasScanner(Player player) {
        if (player.getMainHandItem().is(ModItems.BIO_SCANNER.get())
                || player.getOffhandItem().is(ModItems.BIO_SCANNER.get())
                || MycelialCuriosCompat.hasEquippedScanner(player)) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.BIO_SCANNER.get())) {
                return true;
            }
        }
        return false;
    }
}
