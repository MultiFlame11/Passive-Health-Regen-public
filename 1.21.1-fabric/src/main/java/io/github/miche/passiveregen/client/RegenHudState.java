package io.github.miche.passiveregen.client;

public final class RegenHudState {
    private static final RegenHudState INSTANCE = new RegenHudState();

    private long outOfCombatTicks;
    private int damageCooldownTicks;
    private boolean regenActive;
    private boolean justHealed;
    private float currentHealth;
    private float maxHealth;
    private int maxRegenHealthPercent;
    private long packetReceivedAtMs;

    private float visualHealth;
    private float previousVisualHealth;
    private long visualHealthStartMs;
    private static final long VISUAL_HEALTH_LERP_MS = 200L;

    private RegenHudState() {
    }

    public static RegenHudState get() {
        return INSTANCE;
    }

    public void applyPacket(long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean justHealed, float currentHealth, float maxHealth, int maxRegenHealthPercent) {
        this.outOfCombatTicks = outOfCombatTicks;
        this.damageCooldownTicks = damageCooldownTicks;
        this.regenActive = regenActive;
        this.justHealed = justHealed;
        this.packetReceivedAtMs = System.currentTimeMillis();
        this.maxHealth = maxHealth;
        this.maxRegenHealthPercent = maxRegenHealthPercent;

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
        if (damageCooldownTicks <= 0) return regenActive ? 1.0F : 0.0F;
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - packetReceivedAtMs);
        double extrapolatedTicks = outOfCombatTicks + (elapsedMs / 50.0D);
        return (float) Math.max(0.0D, Math.min(1.0D, extrapolatedTicks / damageCooldownTicks));
    }

    public boolean isCooldownCounting() {
        return damageCooldownTicks > 0 && getCooldownProgress() > 0.0F && !isReady();
    }

    public float getSecondsRemaining() {
        float progress = getCooldownProgress();
        if (progress >= 1.0F) return 0.0F;
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
        if (maxHealth <= 0.0F) return 0.0F;
        float capHealth = maxHealth * (Math.max(0, Math.min(100, maxRegenHealthPercent)) / 100.0F);
        if (capHealth <= 0.0F) return 0.0F;
        return Math.max(0.0F, Math.min(1.0F, getDisplayedHealth() / capHealth));
    }

    public boolean isRegenActive() {
        return regenActive;
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
        justHealed = false;
        currentHealth = 0.0F;
        maxHealth = 0.0F;
        maxRegenHealthPercent = 100;
        packetReceivedAtMs = System.currentTimeMillis();
        visualHealth = 0.0F;
        previousVisualHealth = 0.0F;
        visualHealthStartMs = 0L;
    }
}
