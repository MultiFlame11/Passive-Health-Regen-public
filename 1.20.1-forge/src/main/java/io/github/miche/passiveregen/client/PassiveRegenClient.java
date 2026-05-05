package io.github.miche.passiveregen.client;

import io.github.miche.passiveregen.hud.RegenHudRenderer;
import io.github.miche.passiveregen.hud.RegenFeedbackHandler;
import net.minecraftforge.common.MinecraftForge;

public final class PassiveRegenClient {
    public static final RegenHudConfigHolder HOLDER = new RegenHudConfigHolder();

    private PassiveRegenClient() {
    }

    public static void init() {
        HOLDER.config = io.github.miche.passiveregen.config.RegenHudConfig.load();
        MinecraftForge.EVENT_BUS.register(new RegenHudRenderer());
        MinecraftForge.EVENT_BUS.register(new RegenFeedbackHandler());
    }

    public static final class RegenHudConfigHolder {
        public io.github.miche.passiveregen.config.RegenHudConfig config;
    }
}
