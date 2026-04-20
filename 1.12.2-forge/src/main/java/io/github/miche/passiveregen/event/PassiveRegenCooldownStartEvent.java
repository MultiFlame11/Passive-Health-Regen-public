package io.github.miche.passiveregen.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Fired when damage starts the passive regen cooldown.
 */
public class PassiveRegenCooldownStartEvent extends Event {
    protected final EntityPlayer player;
    protected final DamageSource damageSource;
    protected int cooldownTicks;

    public PassiveRegenCooldownStartEvent(EntityPlayer player, DamageSource damageSource, int cooldownTicks) {
        this.player = player;
        this.damageSource = damageSource;
        this.cooldownTicks = Math.max(0, cooldownTicks);
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = Math.max(0, cooldownTicks);
    }
}
