package io.github.miche.passiveregen.network;

import io.github.miche.passiveregen.PassiveRegenMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RegenHudPacket implements IMessage {
    public long outOfCombatTicks;
    public int damageCooldownTicks;
    public boolean regenActive;
    public boolean hungerBlocked;
    public boolean justHealed;
    public boolean saturationBonus;
    public boolean poisoned;
    public boolean withered;
    public float currentHealth;
    public float maxHealth;
    public int maxRegenHealthPercent;

    public RegenHudPacket() {
    }

    public RegenHudPacket(long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean hungerBlocked, boolean justHealed, float currentHealth, float maxHealth, int maxRegenHealthPercent, boolean saturationBonus, boolean poisoned, boolean withered) {
        this.outOfCombatTicks = outOfCombatTicks;
        this.damageCooldownTicks = damageCooldownTicks;
        this.regenActive = regenActive;
        this.hungerBlocked = hungerBlocked;
        this.justHealed = justHealed;
        this.saturationBonus = saturationBonus;
        this.poisoned = poisoned;
        this.withered = withered;
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.maxRegenHealthPercent = maxRegenHealthPercent;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        outOfCombatTicks = buf.readLong();
        damageCooldownTicks = buf.readInt();
        regenActive = buf.readBoolean();
        hungerBlocked = buf.readBoolean();
        justHealed = buf.readBoolean();
        saturationBonus = buf.readBoolean();
        poisoned = buf.readBoolean();
        withered = buf.readBoolean();
        currentHealth = buf.readFloat();
        maxHealth = buf.readFloat();
        maxRegenHealthPercent = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(outOfCombatTicks);
        buf.writeInt(damageCooldownTicks);
        buf.writeBoolean(regenActive);
        buf.writeBoolean(hungerBlocked);
        buf.writeBoolean(justHealed);
        buf.writeBoolean(saturationBonus);
        buf.writeBoolean(poisoned);
        buf.writeBoolean(withered);
        buf.writeFloat(currentHealth);
        buf.writeFloat(maxHealth);
        buf.writeInt(maxRegenHealthPercent);
    }

    public static class Handler implements IMessageHandler<RegenHudPacket, IMessage> {
        @Override
        public IMessage onMessage(RegenHudPacket message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> PassiveRegenMod.PROXY.handleHudPacket(message));
            return null;
        }
    }
}
