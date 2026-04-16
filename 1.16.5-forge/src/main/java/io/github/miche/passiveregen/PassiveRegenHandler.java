package io.github.miche.passiveregen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PassiveRegenHandler {
    private final Map<UUID, Long> lastDamageTicks = new HashMap<>();

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (!PassiveRegenConfig.ENABLED.get() || event.getAmount() <= 0.0F) {
            return;
        }

        LivingEntity living = event.getEntityLiving();
        if (!(living instanceof PlayerEntity) || living.level.isClientSide) {
            return;
        }

        PlayerEntity player = (PlayerEntity) living;
        lastDamageTicks.put(player.getUUID(), player.level.getGameTime());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        PlayerEntity player = event.player;
        if (!PassiveRegenConfig.ENABLED.get() || event.phase != TickEvent.Phase.END || player.level.isClientSide) {
            return;
        }

        if (!shouldProcessPlayer(player)) {
            return;
        }

        long now = player.level.getGameTime();
        int updateTicks = Math.max(1, PassiveRegenConfig.UPDATE_INTERVAL_TICKS.get());
        if ((now + player.getId()) % updateTicks != 0L) {
            return;
        }

        UUID playerId = player.getUUID();
        long lastDamageTick = lastDamageTicks.computeIfAbsent(playerId, unused -> now);
        long outOfCombatTicks = now - lastDamageTick;
        if (outOfCombatTicks < PassiveRegenConfig.DAMAGE_COOLDOWN_TICKS.get()) {
            return;
        }

        float healAmount = PassiveRegenConfig.getHealAmountPerUpdate(outOfCombatTicks, player.getMaxHealth());
        if (healAmount > 0.0F) {
            player.heal(healAmount);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastDamageTicks.remove(event.getPlayer().getUUID());
    }

    private static boolean shouldProcessPlayer(PlayerEntity player) {
        return player.isAlive()
            && !player.isSpectator()
            && !player.abilities.invulnerable
            && player.getFoodData().getFoodLevel() >= PassiveRegenConfig.getMinimumFoodLevel()
            && player.getHealth() < player.getMaxHealth();
    }
}
