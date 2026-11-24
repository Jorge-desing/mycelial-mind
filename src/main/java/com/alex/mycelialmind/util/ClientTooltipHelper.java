package com.alex.mycelialmind.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/**
 * Server-safe utility for progressive Shift-expandable item/block tooltips.
 */
public final class ClientTooltipHelper {
    private ClientTooltipHelper() {
    }

    public static boolean isShiftDown() {
        if (!FMLEnvironment.dist.isClient()) {
            return false;
        }
        return ClientShiftQuery.held();
    }

    public static void appendShiftDetails(List<Component> tooltip, String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        if (isShiftDown()) {
            for (String key : keys) {
                if (key != null && !key.isEmpty()) {
                    tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
                }
            }
        } else {
            tooltip.add(Component.translatable("tooltip.mycelialmind.hold_shift_details")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static final class ClientShiftQuery {
        private static boolean held() {
            return net.minecraft.client.gui.screens.Screen.hasShiftDown();
        }
    }
}
