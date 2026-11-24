package com.alex.mycelialmind.compat.curios;

import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

public final class MycelialCuriosCompat {
    private MycelialCuriosCompat() {}

    public static boolean hasEquippedScanner(Player player) {
        return player != null && ModList.get().isLoaded("curios") && CuriosAccess.hasEquippedScanner(player);
    }

    private static final class CuriosAccess {
        private static boolean hasEquippedScanner(Player player) {
            return CuriosApi.getCuriosInventory(player)
                    .flatMap(inventory -> inventory.findFirstCurio(ModItems.BIO_SCANNER.get()))
                    .isPresent();
        }
    }
}
