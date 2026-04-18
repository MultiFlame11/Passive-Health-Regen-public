package io.github.miche.passiveregen;

import io.github.miche.passiveregen.api.IPassiveRegenInternals;
import io.github.miche.passiveregen.api.PassiveRegenAPI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

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
        // 0L = "last damaged at world tick zero" — cooldown is always expired by the time
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

    // ── Event handlers ────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (!PassiveRegenConfig.enabled || event.getAmount() <= 0.0F) {
            return;
        }

        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.getEntityLiving().world.isRemote) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        long now = player.world.getTotalWorldTime();
        int pvpCooldown = PassiveRegenConfig.pvpDamageCooldownTicks;
        if (pvpCooldown >= 0 && event.getSource().getTrueSource() instanceof EntityPlayer) {
            long adjustedTick = now - PassiveRegenConfig.damageCooldownTicks + pvpCooldown;
            lastDamageTicks.put(player.getUniqueID(), adjustedTick);
        } else {
            lastDamageTicks.put(player.getUniqueID(), now);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!PassiveRegenConfig.enabled || !PassiveRegenConfig.regenOnKillEnabled) {
            return;
        }
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer killer = (EntityPlayer) event.getSource().getTrueSource();
        if (killer.world.isRemote) return;
        UUID killerId = killer.getUniqueID();
        Long lastDamageTick = lastDamageTicks.get(killerId);
        if (lastDamageTick == null) return;
        long now = killer.world.getTotalWorldTime();
        long remaining = lastDamageTick + PassiveRegenConfig.damageCooldownTicks - now;
        if (remaining <= 0) return;
        int reduction = Math.max(0, Math.min(100, PassiveRegenConfig.regenOnKillCooldownReduction));
        long reduced = (long) (remaining * (reduction / 100.0D));
        lastDamageTicks.put(killerId, lastDamageTick - reduced);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!PassiveRegenConfig.enabled || event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }

        EntityPlayer player = event.player;
        long now = player.world.getTotalWorldTime();
        serverTick = now;

        // Disable vanilla natural regen by keeping food exhaustion high
        if (PassiveRegenConfig.disableNaturalRegen) {
            net.minecraft.util.FoodStats foodStats = player.getFoodStats();
            if (foodStats.getFoodLevel() >= 18 && foodStats.getSaturationLevel() > 0) {
                // exhaust enough each tick to prevent vanilla regen (vanilla needs exhaustion >= 4.0)
                player.addExhaustion(0.1F);
            }
        }

        if (!shouldProcessPlayer(player)) {
            return;
        }

        int updateTicks = Math.max(1, PassiveRegenConfig.updateIntervalTicks);
        if ((now + player.getEntityId()) % updateTicks != 0L) {
            return;
        }

        int foodLevel = player.getFoodStats().getFoodLevel();

        UUID playerId = player.getUniqueID();
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
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.player.getUniqueID();
        lastDamageTicks.remove(playerId);
        activeBoosts.remove(playerId);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        UUID playerId = event.player.getUniqueID();
        lastDamageTicks.remove(playerId);
        activeBoosts.remove(playerId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean shouldProcessPlayer(EntityPlayer player) {
        int minimumFoodLevel = (int) Math.ceil((Math.max(0, Math.min(100, PassiveRegenConfig.minimumHungerPercent)) / 100.0D) * 20.0D);
        return !player.isDead
            && !player.isSpectator()
            && !player.capabilities.disableDamage
            && player.getFoodStats().getFoodLevel() >= minimumFoodLevel
            && player.getHealth() < player.getMaxHealth() * (PassiveRegenConfig.maxRegenHealthPercent / 100.0f)
            && !hasBlockedEffect(player, PassiveRegenConfig.blockedEffects)
            && !isDimensionBlacklisted(player, PassiveRegenConfig.dimensionBlacklist)
            && (PassiveRegenConfig.regenWhileSprinting || !player.isSprinting());
    }

    private static boolean hasBlockedEffect(net.minecraft.entity.player.EntityPlayer player, String[] blockedEffects) {
        if (blockedEffects == null || blockedEffects.length == 0) return false;
        java.util.Set<String> blocked = new java.util.HashSet<>(java.util.Arrays.asList(blockedEffects));
        for (net.minecraft.potion.PotionEffect effect : player.getActivePotionEffects()) {
            net.minecraft.util.ResourceLocation id = net.minecraftforge.fml.common.registry.ForgeRegistries.POTIONS.getKey(effect.getPotion());
            if (id != null && blocked.contains(id.toString())) return true;
        }
        return false;
    }

    private static boolean isDimensionBlacklisted(net.minecraft.entity.player.EntityPlayer player, String[] dimensionBlacklist) {
        if (dimensionBlacklist == null || dimensionBlacklist.length == 0) return false;
        String dimId = String.valueOf(player.dimension);
        return java.util.Arrays.asList(dimensionBlacklist).contains(dimId);
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
