package io.github.miche.passiveregen.client;

public final class RegenHudState {
    private static final RegenHudState INSTANCE = new RegenHudState();
    private static final long VISUAL_HEALTH_LERP_MS = 200L;
    private static final long HEAL_FLASH_MS = 140L;
    private static final long HEAL_THUMP_MS = 120L;
    private static final long POST_FLASH_GLOW_DELAY_MS = 350L;
    private static final long SPARKLE_MS = 520L;
    private static final float CRITICAL_THRESHOLD = 0.20F;

    private long outOfCombatTicks;
    private int damageCooldownTicks;
    private boolean regenActive;
    private boolean hungerBlocked;
    private boolean justHealed;
    private float currentHealth;
    private float maxHealth;
    private int maxRegenHealthPercent;
    private long packetReceivedAtMs;

    private float visualHealth;
    private float previousVisualHealth;
    private long visualHealthStartMs;
    private long lastHealFlashAtMs;
    private float hudFadeAlpha = 0.0F;
    private long lastFadeUpdateMs = 0L;
    private long lastSparkleAtMs;
    private boolean prevHealthWasAtMax = true;

    private RegenHudState() {
    }

    public static RegenHudState get() {
        return INSTANCE;
    }

    public void applyPacket(long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean hungerBlocked, boolean justHealed, float currentHealth, float maxHealth, int maxRegenHealthPercent) {
        this.outOfCombatTicks = outOfCombatTicks;
        this.damageCooldownTicks = damageCooldownTicks;
        this.regenActive = regenActive;
        this.hungerBlocked = hungerBlocked;
        this.justHealed = justHealed;
        this.packetReceivedAtMs = System.currentTimeMillis();
        if (justHealed) {
            this.lastHealFlashAtMs = this.packetReceivedAtMs;
        }
        this.maxHealth = maxHealth;
        this.maxRegenHealthPercent = maxRegenHealthPercent;

        // Sparkle: fires once when a regen heal tops HP to max
        boolean wasAtMax = prevHealthWasAtMax;
        boolean nowAtMax = maxHealth > 0.0F && currentHealth >= maxHealth;
        if (justHealed && !wasAtMax && nowAtMax) {
            this.lastSparkleAtMs = this.packetReceivedAtMs;
        }
        prevHealthWasAtMax = nowAtMax;

        if (this.visualHealth == 0.0F && maxHealth > 0.0F) {
            this.visualHealth = currentHealth;
            this.previousVisualHealth = currentHealth;
        } else if (currentHealth != this.currentHealth) {
            this.previousVisualHealth = getDisplayedHealth();
            this.visualHealth = currentHealth;
            this.visualHealthStartMs = this.packetReceivedAtMs;
        }

        this.currentHealth = currentHealth;
    }

    public boolean consumeJustHealed() {
        boolean value = justHealed;
        justHealed = false;
        return value;
    }

    public float getCooldownProgress() {
        if (damageCooldownTicks <= 0) {
            return regenActive ? 1.0F : 0.0F;
        }
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - packetReceivedAtMs);
        double extrapolatedTicks = outOfCombatTicks + (elapsedMs / 50.0D);
        return (float) Math.max(0.0D, Math.min(1.0D, extrapolatedTicks / damageCooldownTicks));
    }

    public boolean isCooldownCounting() {
        return damageCooldownTicks > 0 && getCooldownProgress() > 0.0F && !isReady();
    }

    public float getSecondsRemaining() {
        float progress = getCooldownProgress();
        if (progress >= 1.0F) {
            return 0.0F;
        }
        float remainingTicks = Math.max(0.0F, damageCooldownTicks - (progress * damageCooldownTicks));
        return remainingTicks / 20.0F;
    }

    public float getDisplayedHealth() {
        if (visualHealthStartMs <= 0L) {
            return visualHealth == 0.0F ? currentHealth : visualHealth;
        }

        long elapsed = Math.max(0L, System.currentTimeMillis() - visualHealthStartMs);
        float progress = Math.min(1.0F, elapsed / (float) VISUAL_HEALTH_LERP_MS);
        float displayed = previousVisualHealth + (visualHealth - previousVisualHealth) * progress;
        if (progress >= 1.0F) {
            previousVisualHealth = visualHealth;
            visualHealthStartMs = 0L;
        }
        return displayed;
    }

    public float getHealthFillProgress() {
        if (maxHealth <= 0.0F) {
            return 0.0F;
        }

        float capHealth = maxHealth * (Math.max(0, Math.min(100, maxRegenHealthPercent)) / 100.0F);
        if (capHealth <= 0.0F) {
            return 0.0F;
        }

        return Math.max(0.0F, Math.min(1.0F, getDisplayedHealth() / capHealth));
    }

    public boolean isRegenActive() {
        return regenActive;
    }

    public boolean isHungerBlocked() {
        return hungerBlocked;
    }

    /** True when HP fill is below the critical threshold and health is greater than zero. */
    public boolean isCriticalHealth() {
        float fill = getHealthFillProgress();
        return maxHealth > 0.0F && fill > 0.0F && fill < CRITICAL_THRESHOLD;
    }

    /** 0→1 alpha for the sparkle animation; 0 when not active. */
    public float getSparkleAlpha() {
        if (lastSparkleAtMs <= 0L) return 0.0F;
        long elapsed = System.currentTimeMillis() - lastSparkleAtMs;
        if (elapsed >= SPARKLE_MS) return 0.0F;
        return 1.0F - elapsed / (float) SPARKLE_MS;
    }

    /** 0→1 progress through the sparkle animation; 0 when not active. */
    public float getSparkleProgress() {
        if (lastSparkleAtMs <= 0L) return 0.0F;
        long elapsed = System.currentTimeMillis() - lastSparkleAtMs;
        return Math.min(1.0F, elapsed / (float) SPARKLE_MS);
    }

    public float getHealThumpScale() {
        if (lastHealFlashAtMs <= 0L) return 1.0F;
        long elapsed = System.currentTimeMillis() - lastHealFlashAtMs;
        if (elapsed >= HEAL_THUMP_MS) return 1.0F;
        float progress = elapsed / (float) HEAL_THUMP_MS;
        // Peaks at 1.08x immediately, eases out with quadratic decay
        return 1.0F + 0.08F * (1.0F - progress * progress);
    }

    public float getHealFlashAlpha() {
        if (lastHealFlashAtMs <= 0L) {
            return 0.0F;
        }

        long elapsed = Math.max(0L, System.currentTimeMillis() - lastHealFlashAtMs);
        if (elapsed >= HEAL_FLASH_MS) {
            return 0.0F;
        }

        float progress = elapsed / (float) HEAL_FLASH_MS;
        return 0.95F * (1.0F - progress);
    }

    public float updateAndGetFadeAlpha(boolean visible, int fadeInMs, int fadeOutMs) {
        long now = System.currentTimeMillis();
        float deltaSec = lastFadeUpdateMs > 0L ? (now - lastFadeUpdateMs) / 1000.0F : 0.0F;
        lastFadeUpdateMs = now;

        float target = visible ? 1.0F : 0.0F;
        if (hudFadeAlpha == target) return hudFadeAlpha;

        if (visible) {
            float rate = fadeInMs > 0 ? deltaSec / (fadeInMs / 1000.0F) : 1.0F;
            hudFadeAlpha = Math.min(1.0F, hudFadeAlpha + rate);
        } else {
            float rate = fadeOutMs > 0 ? deltaSec / (fadeOutMs / 1000.0F) : 1.0F;
            hudFadeAlpha = Math.max(0.0F, hudFadeAlpha - rate);
        }
        return hudFadeAlpha;
    }

    public boolean isGlowSuppressed() {
        if (lastHealFlashAtMs <= 0L) return false;
        long elapsed = System.currentTimeMillis() - lastHealFlashAtMs;
        return elapsed < HEAL_FLASH_MS + POST_FLASH_GLOW_DELAY_MS;
    }

    public float getGlowPhaseSeconds() {
        if (lastHealFlashAtMs <= 0L) {
            // No heal yet  -- free-running phase from session start
            return System.currentTimeMillis() / 1000.0F;
        }
        long glowStartMs = lastHealFlashAtMs + HEAL_FLASH_MS + POST_FLASH_GLOW_DELAY_MS;
        long elapsed = System.currentTimeMillis() - glowStartMs;
        return Math.max(0L, elapsed) / 1000.0F;
    }

    public boolean isReady() {
        return damageCooldownTicks <= 0 || getCooldownProgress() >= 1.0F;
    }

    public float getCurrentHealth() {
        return currentHealth;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void reset() {
        outOfCombatTicks = 0L;
        damageCooldownTicks = 0;
        regenActive = false;
        hungerBlocked = false;
        justHealed = false;
        currentHealth = 0.0F;
        maxHealth = 0.0F;
        maxRegenHealthPercent = 100;
        packetReceivedAtMs = System.currentTimeMillis();
        visualHealth = 0.0F;
        previousVisualHealth = 0.0F;
        visualHealthStartMs = 0L;
        lastHealFlashAtMs = 0L;
        hudFadeAlpha = 0.0F;
        lastFadeUpdateMs = 0L;
        lastSparkleAtMs = 0L;
        prevHealthWasAtMax = true;
    }
}
