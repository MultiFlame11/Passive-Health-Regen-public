package io.github.miche.passiveregen.client;

import io.github.miche.passiveregen.hud.RegenHudRenderer;
import io.github.miche.passiveregen.hud.RegenFeedbackHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public final class PassiveRegenClient {
    public static final RegenHudConfigHolder HOLDER = new RegenHudConfigHolder();

    private PassiveRegenClient() {
    }

    public static void init(IEventBus modEventBus) {
        HOLDER.config = io.github.miche.passiveregen.config.RegenHudConfig.load();
        RegenHudRenderer renderer = new RegenHudRenderer();
        NeoForge.EVENT_BUS.register(renderer);
        modEventBus.addListener(renderer::onAddLayers);
        NeoForge.EVENT_BUS.register(new RegenFeedbackHandler());
    }

    public static final class RegenHudConfigHolder {
        public io.github.miche.passiveregen.config.RegenHudConfig config;
    }
}
