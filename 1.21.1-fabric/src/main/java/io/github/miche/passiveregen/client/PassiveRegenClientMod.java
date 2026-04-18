package io.github.miche.passiveregen.client;

import io.github.miche.passiveregen.config.RegenHudConfig;
import io.github.miche.passiveregen.hud.RegenFeedbackHandler;
import io.github.miche.passiveregen.hud.RegenHudRenderer;
import io.github.miche.passiveregen.network.RegenHudPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PassiveRegenClientMod implements ClientModInitializer {
    public static RegenHudConfig CONFIG;

    @Override
    public void onInitializeClient() {
        CONFIG = RegenHudConfig.load();

        PayloadTypeRegistry.playS2C().register(RegenHudPacket.TYPE, RegenHudPacket.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(RegenHudPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> RegenHudState.get().applyPacket(
                payload.outOfCombatTicks(),
                payload.damageCooldownTicks(),
                payload.regenActive(),
                payload.justHealed(),
                payload.currentHealth(),
                payload.maxHealth(),
                payload.maxRegenHealthPercent()
            ));
        });

        HudRenderCallback.EVENT.register(new RegenHudRenderer());
        RegenFeedbackHandler.register();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RegenHudState.get().reset());
    }
}
