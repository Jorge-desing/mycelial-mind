package com.alex.mycelialmind.client;

import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
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
import com.alex.mycelialmind.registry.MycelialBuiltInPacks;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.Locale;

/**
 * Registers permission-free client commands under {@code /mycelialclient} via
 * {@link RegisterClientCommandsEvent} for HUD preferences, juice/particles, audio telemetry,
 * built-in pack preset detection, and Codex access.
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
                        .then(Commands.literal("offset")
                                .then(Commands.argument("x", IntegerArgumentType.integer(-200, 200))
                                        .then(Commands.argument("y", IntegerArgumentType.integer(-200, 200))
                                                .executes(ctx -> setHudOffset(
                                                        ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                        IntegerArgumentType.getInteger(ctx, "y"))))))
                        .then(Commands.literal("reset")
                                .executes(ctx -> resetHud(ctx.getSource())))
                        .then(anchorNode))
                .then(Commands.literal("juice")
                        .then(Commands.literal("shake")
                                .executes(ctx -> toggleJuiceOption(ctx.getSource(), "shake")))
                        .then(Commands.literal("vignette")
                                .executes(ctx -> toggleJuiceOption(ctx.getSource(), "vignette")))
                        .then(Commands.literal("bars")
                                .executes(ctx -> toggleJuiceOption(ctx.getSource(), "bars"))))
                .then(Commands.literal("particles")
                        .then(Commands.argument("scale", DoubleArgumentType.doubleArg(0.25D, 1.50D))
                                .executes(ctx -> setParticleScale(
                                        ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "scale")))))
                .then(Commands.literal("packs")
                        .executes(ctx -> showPackPreset(ctx.getSource())))
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
        Component presetName = Component.translatable(MycelialBuiltInPacks.detectActivePreset().translationKey());

        source.sendSuccess(() -> Component.translatable(
                "command.mycelialmind.client.status",
                threat, actName, traits, tier2, mutagens, colony, cycle, presetName
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

    private static int setHudOffset(CommandSourceStack source, int x, int y) {
        MycelialClientConfig.HUD_OFFSET_X.set(x);
        MycelialClientConfig.HUD_OFFSET_Y.set(y);
        MycelialClientConfig.SPEC.save();
        source.sendSuccess(() -> Component.translatable(
                "command.mycelialmind.client.hud_offset", x, y
        ).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int resetHud(CommandSourceStack source) {
        MycelialClientConfig.HUD_ANCHOR.set(MycelialClientConfig.HudAnchor.TOP_LEFT);
        MycelialClientConfig.HUD_OFFSET_X.set(0);
        MycelialClientConfig.HUD_OFFSET_Y.set(0);
        MycelialClientConfig.HUD_COMPACT_MODE.set(false);
        MycelialClientConfig.SPEC.save();
        source.sendSuccess(() -> Component.translatable("command.mycelialmind.client.hud_reset")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int toggleJuiceOption(CommandSourceStack source, String option) {
        String stateText;
        switch (option) {
            case "shake" -> {
                double next = MycelialClientConfig.SCREEN_SHAKE_SCALE.get() > 0.0D ? 0.0D : 1.0D;
                MycelialClientConfig.SCREEN_SHAKE_SCALE.set(next);
                stateText = String.format(Locale.ROOT, "%.2f", next);
            }
            case "vignette" -> {
                boolean next = !MycelialClientConfig.STATUS_VIGNETTE_ENABLED.get();
                MycelialClientConfig.STATUS_VIGNETTE_ENABLED.set(next);
                stateText = String.valueOf(next);
            }
            default -> {
                boolean next = !MycelialClientConfig.SMOOTH_HUD_BARS_ENABLED.get();
                MycelialClientConfig.SMOOTH_HUD_BARS_ENABLED.set(next);
                stateText = String.valueOf(next);
            }
        }
        MycelialClientConfig.SPEC.save();
        source.sendSuccess(() -> Component.translatable(
                "command.mycelialmind.client.juice_toggled", option, stateText
        ).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int setParticleScale(CommandSourceStack source, double scale) {
        double rounded = Math.round(scale * 100.0D) / 100.0D;
        MycelialClientConfig.PARTICLE_DENSITY_SCALE.set(rounded);
        MycelialClientConfig.SPEC.save();
        source.sendSuccess(() -> Component.translatable(
                "command.mycelialmind.client.particles_scale",
                String.format(Locale.ROOT, "%.2f", rounded)
        ).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int showPackPreset(CommandSourceStack source) {
        MycelialBuiltInPacks.ActivePreset preset = MycelialBuiltInPacks.detectActivePreset();
        int pylonFuel = MonumentFocusBlockEntity.resolvePylonRechargeAmount(new ItemStack(Items.AMETHYST_SHARD));
        int turretAmmo = PurificationTurretBlockEntity.resolveTurretAmmoGain(
                new ItemStack(ModItems.CONCENTRATED_ANTIDOTE.get()));
        int nutrient = MycelialBuiltInPacks.resolveCurrentBiomassNutrient();
        source.sendSuccess(() -> Component.translatable(
                "command.mycelialmind.client.packs",
                Component.translatable(preset.translationKey()),
                pylonFuel, turretAmmo, nutrient
        ).withStyle(ChatFormatting.AQUA), false);
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
