package io.github.miche.passiveregen.network;

import io.github.miche.passiveregen.PassiveRegenMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record RegenHudPacket(
    long outOfCombatTicks,
    int damageCooldownTicks,
    boolean regenActive,
    boolean justHealed,
    float currentHealth,
    float maxHealth,
    int maxRegenHealthPercent
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RegenHudPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PassiveRegenMod.MODID, "regen_hud"));

    public static final StreamCodec<FriendlyByteBuf, RegenHudPacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeLong(packet.outOfCombatTicks());
            buf.writeInt(packet.damageCooldownTicks());
            buf.writeBoolean(packet.regenActive());
            buf.writeBoolean(packet.justHealed());
            buf.writeFloat(packet.currentHealth());
            buf.writeFloat(packet.maxHealth());
            buf.writeInt(packet.maxRegenHealthPercent());
        },
        buf -> new RegenHudPacket(
            buf.readLong(),
            buf.readInt(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readInt()
        )
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean justHealed, float currentHealth, float maxHealth, int maxRegenHealthPercent) {
        ServerPlayNetworking.send(player, new RegenHudPacket(outOfCombatTicks, damageCooldownTicks, regenActive, justHealed, currentHealth, maxHealth, maxRegenHealthPercent));
    }
}
