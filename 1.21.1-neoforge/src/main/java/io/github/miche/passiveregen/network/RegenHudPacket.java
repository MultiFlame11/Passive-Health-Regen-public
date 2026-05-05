package io.github.miche.passiveregen.network;

import io.github.miche.passiveregen.PassiveRegenMod;
import io.github.miche.passiveregen.client.RegenHudState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RegenHudPacket implements CustomPacketPayload {
    public static final Type<RegenHudPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PassiveRegenMod.MODID, "regen_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RegenHudPacket> STREAM_CODEC = CustomPacketPayload.codec(RegenHudPacket::write, RegenHudPacket::new);

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

    public RegenHudPacket(RegistryFriendlyByteBuf buf) {
        this(
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

    public static void send(ServerPlayer player, long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean hungerBlocked, boolean justHealed, float currentHealth, float maxHealth, int maxRegenHealthPercent, boolean nearCampfire, boolean saturationBonus, boolean poisoned, boolean withered) {
        PacketDistributor.sendToPlayer(player, new RegenHudPacket(outOfCombatTicks, damageCooldownTicks, regenActive, hungerBlocked, justHealed, currentHealth, maxHealth, maxRegenHealthPercent, nearCampfire, saturationBonus, poisoned, withered));
    }

    public static void handle(RegenHudPacket packet, IPayloadContext context) {
        RegenHudState.get().applyPacket(
            packet.outOfCombatTicks,
            packet.damageCooldownTicks,
            packet.regenActive,
            packet.hungerBlocked,
            packet.justHealed,
            packet.currentHealth,
            packet.maxHealth,
            packet.maxRegenHealthPercent,
            packet.nearCampfire,
            packet.saturationBonus,
            packet.poisoned,
            packet.withered
        );
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeLong(outOfCombatTicks);
        buf.writeInt(damageCooldownTicks);
        buf.writeBoolean(regenActive);
        buf.writeBoolean(hungerBlocked);
        buf.writeBoolean(justHealed);
        buf.writeFloat(currentHealth);
        buf.writeFloat(maxHealth);
        buf.writeInt(maxRegenHealthPercent);
        buf.writeBoolean(nearCampfire);
        buf.writeBoolean(saturationBonus);
        buf.writeBoolean(poisoned);
        buf.writeBoolean(withered);
    }

    @Override
    public Type<RegenHudPacket> type() {
        return TYPE;
    }
}
