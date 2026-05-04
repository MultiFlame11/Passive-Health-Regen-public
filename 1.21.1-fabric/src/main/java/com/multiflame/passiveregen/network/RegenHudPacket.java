package com.multiflame.passiveregen.network;

import com.multiflame.passiveregen.PassiveRegenMod;
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
    boolean hungerBlocked,
    boolean justHealed,
    float currentHealth,
    float maxHealth,
    int maxRegenHealthPercent,
    boolean nearCampfire,
    boolean saturationBonus,
    boolean poisoned,
    boolean withered
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RegenHudPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PassiveRegenMod.MODID, "regen_hud"));

    public static final StreamCodec<FriendlyByteBuf, RegenHudPacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeLong(packet.outOfCombatTicks());
            buf.writeInt(packet.damageCooldownTicks());
            buf.writeBoolean(packet.regenActive());
            buf.writeBoolean(packet.hungerBlocked());
            buf.writeBoolean(packet.justHealed());
            buf.writeFloat(packet.currentHealth());
            buf.writeFloat(packet.maxHealth());
            buf.writeInt(packet.maxRegenHealthPercent());
            buf.writeBoolean(packet.nearCampfire());
            buf.writeBoolean(packet.saturationBonus());
            buf.writeBoolean(packet.poisoned());
            buf.writeBoolean(packet.withered());
        },
        buf -> new RegenHudPacket(
            buf.readLong(),
            buf.readInt(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readInt(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean()
        )
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean hungerBlocked, boolean justHealed, float currentHealth, float maxHealth, int maxRegenHealthPercent, boolean nearCampfire, boolean saturationBonus, boolean poisoned, boolean withered) {
        ServerPlayNetworking.send(player, new RegenHudPacket(outOfCombatTicks, damageCooldownTicks, regenActive, hungerBlocked, justHealed, currentHealth, maxHealth, maxRegenHealthPercent, nearCampfire, saturationBonus, poisoned, withered));
    }
}
