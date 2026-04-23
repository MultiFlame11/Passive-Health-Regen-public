package io.github.miche.passiveregen.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public abstract class PassiveRegenTickEvent extends Event {
    protected final EntityPlayer player;
    protected float healAmount;

    protected PassiveRegenTickEvent(EntityPlayer player, float healAmount) {
        this.player = player;
        this.healAmount = healAmount;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public float getHealAmount() {
        return healAmount;
    }

    public void setHealAmount(float healAmount) {
        this.healAmount = Math.max(0.0F, healAmount);
    }

    @Cancelable
    public static final class Pre extends PassiveRegenTickEvent {
        public Pre(EntityPlayer player, float healAmount) {
            super(player, healAmount);
        }
    }

    public static final class Post extends PassiveRegenTickEvent {
        public Post(EntityPlayer player, float healAmount) {
            super(player, healAmount);
        }
    }
}
