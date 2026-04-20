package io.github.miche.passiveregen;

import io.github.miche.passiveregen.api.IPassiveRegenInternals;
import io.github.miche.passiveregen.api.PassiveRegenAPI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PassiveRegenHandler implements IPassiveRegenInternals {
    private final Map<UUID, Long> lastDamageTicks = new HashMap<>();
    private final Map<UUID, RegenBoost> activeBoosts = new HashMap<>();
    static volatile long serverTick = 0;

    public PassiveRegenHandler() {
        PassiveRegenAPI.register(this);
    }

    // ── PassiveRegenAPI implementation ────────────────────────────────────────

    @Override
    public void clearDamageCooldown(UUID playerUUID) {
        // 0L = "last damaged at world tick zero"  -- cooldown is always expired by the time
        // a player can use an item, so regen starts on the very next handler tick.
        lastDamageTicks.put(playerUUID, 0L);
    }

    @Override
    public void applyRegenBoost(UUID playerUUID, double multiplier, int durationTicks) {
        double m = Math.max(1.0, multiplier);
        long expiresAt = serverTick + durationTicks;
        RegenBoost existing = activeBoosts.get(playerUUID);
        // Highest multiplier wins. Equal multiplier refreshes duration.
        if (existing == null || m >= existing.multiplier) {
            activeBoosts.put(playerUUID, new RegenBoost(m, expiresAt));
        }
    }

    @Override
    public void reduceCooldown(UUID playerUUID, int percentReduction) {
        Long last = lastDamageTicks.get(playerUUID);
        if (last == null) return;
        long remaining = last + PassiveRegenConfig.DAMAGE_COOLDOWN_TICKS.get() - serverTick;
        if (remaining <= 0) return;
        int pct = Math.max(0, Math.min(100, percentReduction));
        long cut = (long)(remaining * (pct / 100.0));
        lastDamageTicks.put(playerUUID, last - cut);
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (!PassiveRegenConfig.ENABLED.get() || event.getAmount() <= 0.0F) {
            return;
        }

        LivingEntity living = event.getEntityLiving();
        if (!(living instanceof Player) || living.level.isClientSide) {
            return;
        }

        Player player = (Player) living;
        long now = player.level.getGameTime();
        int pvpCooldown = PassiveRegenConfig.PVP_DAMAGE_COOLDOWN_TICKS.get();
        if (pvpCooldown >= 0 && event.getSource().getEntity() instanceof Player) {
            long adjustedTick = now - PassiveRegenConfig.DAMAGE_COOLDOWN_TICKS.get() + pvpCooldown;
            lastDamageTicks.put(player.getUUID(), adjustedTick);
        } else {
            lastDamageTicks.put(player.getUUID(), now);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (!PassiveRegenConfig.ENABLED.get() || event.phase != TickEvent.Phase.END || player.level.isClientSide) {
            return;
        }

        long now = player.level.getGameTime();
        serverTick = now;

        // Disable vanilla natural regen by draining food exhaustion
        if (PassiveRegenConfig.DISABLE_NATURAL_REGEN.get()) {
            if (player.getFoodData().getFoodLevel() >= 18 && player.getFoodData().getSaturationLevel() > 0) {
                player.causeFoodExhaustion(0.1F);
            }
        }

        if (!shouldProcessPlayer(player)) {
            return;
        }

        int updateTicks = Math.max(1, PassiveRegenConfig.UPDATE_INTERVAL_TICKS.get());
        if ((now + player.getId()) % updateTicks != 0L) {
            return;
        }

        int foodLevel = player.getFoodData().getFoodLevel();

        UUID playerId = player.getUUID();
        Long lastDamageTick = lastDamageTicks.get(playerId);
        if (lastDamageTick == null) {
            return;
        }
        long outOfCombatTicks = now - lastDamageTick;
        int effectiveCooldown = PassiveRegenConfig.getEffectiveDamageCooldown(foodLevel);
        if (outOfCombatTicks < effectiveCooldown) {
            return;
        }

        float healAmount = PassiveRegenConfig.getHealAmountPerUpdate(outOfCombatTicks, player.getMaxHealth(), foodLevel);

        RegenBoost boost = activeBoosts.get(playerId);
        if (boost != null) {
            if (now >= boost.expiresAt) {
                activeBoosts.remove(playerId);
            } else {
                healAmount = (float) (healAmount * boost.multiplier);
            }
        }

        if (healAmount > 0.0F) {
            player.heal(healAmount);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!PassiveRegenConfig.ENABLED.get() || !PassiveRegenConfig.REGEN_ON_KILL_ENABLED.get()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        Player killer = (Player) event.getSource().getEntity();
        if (killer.level.isClientSide) return;
        UUID killerId = killer.getUUID();
        Long lastDamageTick = lastDamageTicks.get(killerId);
        if (lastDamageTick == null) return;
        long now = killer.level.getGameTime();
        int effectiveCooldown = PassiveRegenConfig.getEffectiveDamageCooldown(killer.getFoodData().getFoodLevel());
        long remaining = lastDamageTick + effectiveCooldown - now;
        if (remaining <= 0) return;
        int reduction = Math.max(0, Math.min(100, PassiveRegenConfig.REGEN_ON_KILL_COOLDOWN_REDUCTION.get()));
        long reduced = (long) (remaining * (reduction / 100.0D));
        lastDamageTicks.put(killerId, lastDamageTick - reduced);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        lastDamageTicks.remove(playerId);
        activeBoosts.remove(playerId);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        UUID playerId = event.getEntity().getUUID();
        lastDamageTicks.remove(playerId);
        activeBoosts.remove(playerId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean shouldProcessPlayer(Player player) {
        return player.isAlive()
            && !player.isSpectator()
            && !player.getAbilities().invulnerable
            && player.getFoodData().getFoodLevel() >= PassiveRegenConfig.getMinimumFoodLevel()
            && player.getHealth() < player.getMaxHealth() * (PassiveRegenConfig.MAX_REGEN_HEALTH_PERCENT.get() / 100.0f)
            && !hasBlockedEffect(player, PassiveRegenConfig.BLOCKED_EFFECTS.get())
            && !isDimensionBlacklisted(player, PassiveRegenConfig.DIMENSION_BLACKLIST.get())
            && (PassiveRegenConfig.REGEN_WHILE_SPRINTING.get() || !player.isSprinting());
    }

    private static boolean hasBlockedEffect(net.minecraft.world.entity.player.Player player, java.util.List<? extends String> blockedEffects) {
        if (blockedEffects == null || blockedEffects.isEmpty()) return false;
        for (net.minecraft.world.effect.MobEffectInstance inst : player.getActiveEffects()) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.core.Registry.MOB_EFFECT.getKey(inst.getEffect());
            if (id != null && blockedEffects.contains(id.toString())) return true;
        }
        return false;
    }

    private static boolean isDimensionBlacklisted(net.minecraft.world.entity.player.Player player, java.util.List<? extends String> dimensionBlacklist) {
        if (dimensionBlacklist == null || dimensionBlacklist.isEmpty()) return false;
        String dimId = player.level.dimension().location().toString();
        return dimensionBlacklist.contains(dimId);
    }

    private static final class RegenBoost {
        final double multiplier;
        final long expiresAt;

        RegenBoost(double multiplier, long expiresAt) {
            this.multiplier = multiplier;
            this.expiresAt = expiresAt;
        }
    }
}
