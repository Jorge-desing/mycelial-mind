package com.alex.mycelialmind.compat.modonomicon;

import com.alex.mycelialmind.MycelialMind;
import com.klikli_dev.modonomicon.api.ModonomiconAPI;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * Optional Modonomicon integration for opening the Mycelial Mind codex book when installed.
 */
public final class MycelialModonomiconCompat {
    public static final ResourceLocation CODEX_BOOK_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "mycelial_codex");

    private MycelialModonomiconCompat() {
    }

    public static boolean isModonomiconLoaded() {
        return ModList.get().isLoaded("modonomicon");
    }

    public static boolean isModonomiconActive() {
        if (!isModonomiconLoaded()) {
            return false;
        }
        return Helper.isActive();
    }

    private static final class Helper {
        private static boolean isActive() {
            return !ModonomiconAPI.get().isStub() && ModonomiconAPI.get().getContextHelper(MycelialMind.MODID) != null;
        }
    }
}
