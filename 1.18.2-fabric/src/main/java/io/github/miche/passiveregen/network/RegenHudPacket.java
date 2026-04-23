package io.github.miche.passiveregen.network;

import io.github.miche.passiveregen.PassiveRegenMod;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class RegenHudPacket {
    public static final ResourceLocation ID = new ResourceLocation(PassiveRegenMod.MODID, "regen_hud");

    private RegenHudPacket() {
    }

    public static void send(ServerPlayer player, long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean hungerBlocked, boolean justHealed, float currentHealth, float maxHealth, int maxRegenHealthPercent, boolean nearCampfire, boolean saturationBonus, boolean poisoned, boolean withered) {
        FriendlyByteBuf buf = PacketByteBufs.create();
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
        ServerPlayNetworking.send(player, ID, buf);
    }
}
