package com.multiflame.passiveregen.client;

import com.multiflame.passiveregen.config.RegenHudConfig;
import com.multiflame.passiveregen.hud.RegenFeedbackHandler;
import com.multiflame.passiveregen.hud.RegenHudRenderer;
import com.multiflame.passiveregen.network.RegenHudPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

@Environment(EnvType.CLIENT)
public final class PassiveRegenClientMod implements ClientModInitializer {
    public static RegenHudConfig CONFIG;

    @Override
    public void onInitializeClient() {
        CONFIG = RegenHudConfig.load();
        PayloadTypeRegistry.playS2C().register(RegenHudPacket.TYPE, RegenHudPacket.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(RegenHudPacket.TYPE, (payload, context) ->
            context.client().execute(() -> RegenHudState.get().applyPacket(
                payload.outOfCombatTicks(),
                payload.damageCooldownTicks(),
                payload.regenActive(),
                payload.hungerBlocked(),
                payload.justHealed(),
                payload.currentHealth(),
                payload.maxHealth(),
                payload.maxRegenHealthPercent(),
                payload.nearCampfire(),
                payload.saturationBonus(),
                payload.poisoned(),
                payload.withered()
            ))
        );

        HudRenderCallback.EVENT.register(new RegenHudRenderer());
        RegenFeedbackHandler.register();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RegenHudState.get().reset());
    }
}
