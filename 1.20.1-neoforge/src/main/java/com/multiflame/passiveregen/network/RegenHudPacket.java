package com.multiflame.passiveregen.network;

import com.multiflame.passiveregen.client.RegenHudState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class RegenHudPacket {
    private final long outOfCombatTicks;
    private final int damageCooldownTicks;
    private final boolean regenActive;
    private final boolean hungerBlocked;
    private final boolean justHealed;
    private final float currentHealth;
    private final float maxHealth;
    private final int maxRegenHealthPercent;
    private final boolean nearCampfire;
    private final boolean saturationBonus;
    private final boolean poisoned;
    private final boolean withered;

    public RegenHudPacket(long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean hungerBlocked, boolean justHealed, float currentHealth, float maxHealth, int maxRegenHealthPercent, boolean nearCampfire, boolean saturationBonus, boolean poisoned, boolean withered) {
        this.outOfCombatTicks = outOfCombatTicks;
        this.damageCooldownTicks = damageCooldownTicks;
        this.regenActive = regenActive;
        this.hungerBlocked = hungerBlocked;
        this.justHealed = justHealed;
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.maxRegenHealthPercent = maxRegenHealthPercent;
        this.nearCampfire = nearCampfire;
        this.saturationBonus = saturationBonus;
        this.poisoned = poisoned;
        this.withered = withered;
    }

    public static void send(ServerPlayer player, long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean hungerBlocked, boolean justHealed, float currentHealth, float maxHealth, int maxRegenHealthPercent, boolean nearCampfire, boolean saturationBonus, boolean poisoned, boolean withered) {
        PassiveRegenNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RegenHudPacket(outOfCombatTicks, damageCooldownTicks, regenActive, hungerBlocked, justHealed, currentHealth, maxHealth, maxRegenHealthPercent, nearCampfire, saturationBonus, poisoned, withered));
    }

    public static void encode(RegenHudPacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.outOfCombatTicks);
        buf.writeInt(packet.damageCooldownTicks);
        buf.writeBoolean(packet.regenActive);
        buf.writeBoolean(packet.hungerBlocked);
        buf.writeBoolean(packet.justHealed);
        buf.writeFloat(packet.currentHealth);
        buf.writeFloat(packet.maxHealth);
        buf.writeInt(packet.maxRegenHealthPercent);
        buf.writeBoolean(packet.nearCampfire);
        buf.writeBoolean(packet.saturationBonus);
        buf.writeBoolean(packet.poisoned);
        buf.writeBoolean(packet.withered);
    }

    public static RegenHudPacket decode(FriendlyByteBuf buf) {
        return new RegenHudPacket(
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
        );
    }

    public static void handle(RegenHudPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.enqueueWork(() -> RegenHudState.get().applyPacket(packet.outOfCombatTicks, packet.damageCooldownTicks, packet.regenActive, packet.hungerBlocked, packet.justHealed, packet.currentHealth, packet.maxHealth, packet.maxRegenHealthPercent, packet.nearCampfire, packet.saturationBonus, packet.poisoned, packet.withered));
        }
        context.setPacketHandled(true);
    }
}
