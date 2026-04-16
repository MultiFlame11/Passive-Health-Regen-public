package io.github.miche.passiveregen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class PassiveRegenHandler {
    private final Map<UUID, Long> lastDamageTicks = new HashMap<>();

    public void onPlayerDamaged(ServerPlayer player) {
        lastDamageTicks.put(player.getUUID(), player.level().getGameTime());
    }

    public void onServerTick(Iterable<ServerPlayer> players, PassiveRegenConfig config) {
        if (!config.enabled) {
            return;
        }

        for (ServerPlayer player : players) {
            if (!shouldProcessPlayer(player, config)) {
                continue;
            }

            long now = player.level().getGameTime();
            int updateTicks = Math.max(1, config.updateIntervalTicks);
            if ((now + player.getId()) % updateTicks != 0L) {
                continue;
            }

            UUID playerId = player.getUUID();
            long lastDamageTick = lastDamageTicks.computeIfAbsent(playerId, unused -> now);
            long outOfCombatTicks = now - lastDamageTick;
            if (outOfCombatTicks < config.damageCooldownTicks) {
                continue;
            }

            float healAmount = config.getHealAmountPerUpdate(outOfCombatTicks, player.getMaxHealth());
            if (healAmount > 0.0F) {
                player.heal(healAmount);
            }
        }
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        lastDamageTicks.remove(player.getUUID());
    }

    private static boolean shouldProcessPlayer(ServerPlayer player, PassiveRegenConfig config) {
        return player.isAlive()
            && !player.isSpectator()
            && !player.getAbilities().invulnerable
            && player.getFoodData().getFoodLevel() >= config.getMinimumFoodLevel()
            && player.getHealth() < player.getMaxHealth();
    }
}
