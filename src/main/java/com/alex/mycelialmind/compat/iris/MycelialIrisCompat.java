package com.alex.mycelialmind.compat.iris;

import net.irisshaders.iris.api.v0.IrisApi;
import net.neoforged.fml.ModList;

/**
 * Safe client-side Iris Shaders / LabPBR 1.3 compatibility bridge.
 * Only touches IrisApi when the "iris" mod is loaded.
 */
public final class MycelialIrisCompat {
    private MycelialIrisCompat() {
    }

    public static boolean isIrisLoaded() {
        return ModList.get().isLoaded("iris");
    }

    public static boolean isShaderPackInUse() {
        if (!isIrisLoaded()) {
            return false;
        }
        return IrisApiHelper.isShaderPackInUse();
    }

    private static final class IrisApiHelper {
        private static boolean isShaderPackInUse() {
            return IrisApi.getInstance().isShaderPackInUse();
        }
    }
}
