package io.github.miche.passiveregen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class PassiveRegenHandler {
    private final Map<UUID, Long> lastDamageTicks = new HashMap<>();

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (!PassiveRegenConfig.enabled || event.getAmount() <= 0.0F) {
            return;
        }

        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.getEntityLiving().world.isRemote) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        lastDamageTicks.put(player.getUniqueID(), player.world.getTotalWorldTime());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!PassiveRegenConfig.enabled || event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }

        EntityPlayer player = event.player;
        if (!shouldProcessPlayer(player)) {
            return;
        }

        long now = player.world.getTotalWorldTime();
        int updateTicks = Math.max(1, PassiveRegenConfig.updateIntervalTicks);
        if ((now + player.getEntityId()) % updateTicks != 0L) {
            return;
        }

        UUID playerId = player.getUniqueID();
        long lastDamageTick = lastDamageTicks.computeIfAbsent(playerId, unused -> now);
        long outOfCombatTicks = now - lastDamageTick;
        if (outOfCombatTicks < PassiveRegenConfig.damageCooldownTicks) {
            return;
        }

        float healAmount = PassiveRegenConfig.getHealAmountPerUpdate(outOfCombatTicks, player.getMaxHealth());
        if (healAmount > 0.0F) {
            player.heal(healAmount);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastDamageTicks.remove(event.player.getUniqueID());
    }

    private boolean shouldProcessPlayer(EntityPlayer player) {
        int minimumFoodLevel = (int) Math.ceil((Math.max(0, Math.min(100, PassiveRegenConfig.minimumHungerPercent)) / 100.0D) * 20.0D);
        return !player.isDead
            && !player.isSpectator()
            && !player.capabilities.disableDamage
            && player.getFoodStats().getFoodLevel() >= minimumFoodLevel
            && player.getHealth() < player.getMaxHealth();
    }
}
