package com.alex.mycelialmind.compat.claims;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.config.MycelialConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Optional runtime bridge for land-claim protection mods (FTB Chunks and Open Parties & Claims).
 * Invoked purely via cached reflection so Mycelial Mind builds and runs with zero compile-time dependencies.
 */
public final class ClaimProtectionCompat {
    private static final String FTB_CHUNKS_MODID = "ftbchunks";
    private static final String OPAC_MODID = "openpartiesandclaims";

    private static volatile boolean ftbInitialized;
    private static volatile boolean ftbUnavailable;
    private static Method ftbApiMethod;
    private static Method ftbGetManagerMethod;
    private static Method ftbGetChunkMethod;
    private static Constructor<?> ftbChunkDimPosKeyCtor;
    private static Constructor<?> ftbChunkDimPosLevelCtor;

    private static volatile boolean opacInitialized;
    private static volatile boolean opacUnavailable;
    private static Method opacGetServerApiMethod;
    private static Method opacGetClaimsManagerMethod;
    private static Method opacGetByChunkPosMethod;
    private static Method opacGetByCoordsMethod;

    private ClaimProtectionCompat() {
    }

    /**
     * Returns {@code true} if {@code pos} lies inside a chunk claimed by FTB Chunks or
     * Open Parties &amp; Claims and {@link MycelialConfig#RESPECT_CLAIMED_CHUNKS} is enabled.
     * Safe to invoke even when neither mod is loaded or in headless test environments.
     */
    public static boolean isClaimProtected(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || !isRespectClaimedChunksEnabled()) {
            return false;
        }
        ModList modList = ModList.get();
        if (modList == null) {
            return false;
        }
        if (modList.isLoaded(FTB_CHUNKS_MODID) && isFtbChunkClaimed(level, pos)) {
            return true;
        }
        if (modList.isLoaded(OPAC_MODID) && isOpacChunkClaimed(level, pos)) {
            return true;
        }
        return false;
    }

    private static boolean isRespectClaimedChunksEnabled() {
        try {
            return MycelialConfig.RESPECT_CLAIMED_CHUNKS.get();
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private static boolean isFtbChunkClaimed(ServerLevel level, BlockPos pos) {
        if (ftbUnavailable) {
            return false;
        }
        try {
            if (!ftbInitialized) {
                initFtbReflection();
            }
            if (ftbUnavailable) {
                return false;
            }
            Object api = ftbApiMethod.invoke(null);
            if (api == null) {
                return false;
            }
            Object manager = ftbGetManagerMethod.invoke(api);
            if (manager == null) {
                return false;
            }
            Object chunkDimPos = ftbChunkDimPosKeyCtor != null
                    ? ftbChunkDimPosKeyCtor.newInstance(level.dimension(), new ChunkPos(pos))
                    : ftbChunkDimPosLevelCtor.newInstance(level, pos);
            return ftbGetChunkMethod.invoke(manager, chunkDimPos) != null;
        } catch (Throwable throwable) {
            ftbUnavailable = true;
            MycelialMind.LOGGER.warn("Disabling FTB Chunks reflection compat after invocation failure: {}", throwable.toString());
            return false;
        }
    }

    private static synchronized void initFtbReflection() {
        if (ftbInitialized || ftbUnavailable) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            Class<?> managerClass = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunkManager");
            Class<?> chunkDimPosClass = Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");

            ftbApiMethod = apiClass.getMethod("api");
            ftbGetManagerMethod = apiClass.getMethod("getManager");
            ftbGetChunkMethod = managerClass.getMethod("getChunk", chunkDimPosClass);

            try {
                ftbChunkDimPosKeyCtor = chunkDimPosClass.getConstructor(ResourceKey.class, ChunkPos.class);
            } catch (NoSuchMethodException ignored) {
                ftbChunkDimPosLevelCtor = chunkDimPosClass.getConstructor(Level.class, BlockPos.class);
            }
            ftbInitialized = true;
        } catch (Throwable throwable) {
            ftbUnavailable = true;
            MycelialMind.LOGGER.debug("FTB Chunks API reflection unavailable: {}", throwable.toString());
        }
    }

    private static boolean isOpacChunkClaimed(ServerLevel level, BlockPos pos) {
        if (opacUnavailable) {
            return false;
        }
        try {
            if (!opacInitialized) {
                initOpacReflection();
            }
            if (opacUnavailable) {
                return false;
            }
            Object serverApi = opacGetServerApiMethod.invoke(null, level.getServer());
            if (serverApi == null) {
                return false;
            }
            Object claimsManager = opacGetClaimsManagerMethod.invoke(serverApi);
            if (claimsManager == null) {
                return false;
            }
            ResourceLocation dimId = level.dimension().location();
            ChunkPos chunkPos = new ChunkPos(pos);
            if (opacGetByChunkPosMethod != null) {
                return opacGetByChunkPosMethod.invoke(claimsManager, dimId, chunkPos) != null;
            }
            return opacGetByCoordsMethod.invoke(claimsManager, dimId, chunkPos.x, chunkPos.z) != null;
        } catch (Throwable throwable) {
            opacUnavailable = true;
            MycelialMind.LOGGER.warn("Disabling Open Parties & Claims reflection compat after invocation failure: {}", throwable.toString());
            return false;
        }
    }

    private static synchronized void initOpacReflection() {
        if (opacInitialized || opacUnavailable) {
            return;
        }
        try {
            Class<?> serverApiClass = Class.forName("xaero.pac.openpartiesandclaims.api.OpenPACServerAPI");
            opacGetServerApiMethod = serverApiClass.getMethod("get", MinecraftServer.class);
            opacGetClaimsManagerMethod = serverApiClass.getMethod("getServerClaimsManager");
            Class<?> managerReturnType = opacGetClaimsManagerMethod.getReturnType();
            try {
                opacGetByChunkPosMethod = managerReturnType.getMethod("get", ResourceLocation.class, ChunkPos.class);
            } catch (NoSuchMethodException ignored) {
                opacGetByCoordsMethod = managerReturnType.getMethod("get", ResourceLocation.class, int.class, int.class);
            }
            opacInitialized = true;
        } catch (Throwable throwable) {
            opacUnavailable = true;
            MycelialMind.LOGGER.debug("Open Parties & Claims API reflection unavailable: {}", throwable.toString());
        }
    }
}
