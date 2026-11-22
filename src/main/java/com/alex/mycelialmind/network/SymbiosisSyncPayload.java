package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.symbiosis.PlayerSymbiosisData;
import com.alex.mycelialmind.symbiosis.SymbioticMutagen;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.EnumSet;
import java.util.Set;

/**
 * Synchronizes the local player's equipped SymbioticMutagens and Mycelial Infection level.
 */
public record SymbiosisSyncPayload(int mutagenMask, int infectionLevel) implements CustomPacketPayload {
    public static final Type<SymbiosisSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "symbiosis_sync"));

    public static final StreamCodec<ByteBuf, SymbiosisSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SymbiosisSyncPayload::mutagenMask,
            ByteBufCodecs.VAR_INT,
            SymbiosisSyncPayload::infectionLevel,
            SymbiosisSyncPayload::new
    );

    public static SymbiosisSyncPayload forPlayer(ServerPlayer player) {
        PlayerSymbiosisData data = player.getData(ModAttachments.SYMBIOSIS);
        MobEffectInstance infection = player.getEffect(ModEffects.MYCELIAL_INFECTION);
        int level = infection != null ? infection.getAmplifier() + 1 : 0;
        return new SymbiosisSyncPayload(data.encodeMask(), level);
    }

    public static void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, forPlayer(player));
    }

    public Set<SymbioticMutagen> decodeMutagens() {
        EnumSet<SymbioticMutagen> set = EnumSet.noneOf(SymbioticMutagen.class);
        for (SymbioticMutagen m : SymbioticMutagen.values()) {
            if ((mutagenMask & (1 << m.ordinal())) != 0) {
                set.add(m);
            }
        }
        return set;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnClient(SymbiosisSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientSymbiosisCache.update(payload.decodeMutagens(), payload.infectionLevel()));
    }
}
