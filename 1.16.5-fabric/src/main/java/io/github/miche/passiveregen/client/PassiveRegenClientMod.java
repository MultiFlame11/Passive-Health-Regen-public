package io.github.miche.passiveregen.client;

import io.github.miche.passiveregen.config.RegenHudConfig;
import io.github.miche.passiveregen.hud.RegenHudRenderer;
import io.github.miche.passiveregen.network.RegenHudPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

@Environment(EnvType.CLIENT)
public final class PassiveRegenClientMod implements ClientModInitializer {
    public static RegenHudConfig CONFIG;

    @Override
    public void onInitializeClient() {
        CONFIG = RegenHudConfig.load();

        ClientPlayNetworking.registerGlobalReceiver(RegenHudPacket.ID, (client, handler, buf, responseSender) -> {
            long outOfCombatTicks = buf.readLong();
            int damageCooldownTicks = buf.readInt();
            boolean regenActive = buf.readBoolean();
            boolean hungerBlocked = buf.readBoolean();
            boolean justHealed = buf.readBoolean();
            float currentHealth = buf.readFloat();
            float maxHealth = buf.readFloat();
            int maxRegenHealthPercent = buf.readInt();
            boolean nearCampfire = buf.readBoolean();
            boolean saturationBonus = buf.readBoolean();
            boolean poisoned = buf.readBoolean();
            boolean withered = buf.readBoolean();
            client.execute(() -> RegenHudState.get().applyPacket(outOfCombatTicks, damageCooldownTicks, regenActive, hungerBlocked, justHealed, currentHealth, maxHealth, maxRegenHealthPercent, nearCampfire, saturationBonus, poisoned, withered));
        });

        HudRenderCallback.EVENT.register(new RegenHudRenderer());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RegenHudState.get().reset());
    }
}
