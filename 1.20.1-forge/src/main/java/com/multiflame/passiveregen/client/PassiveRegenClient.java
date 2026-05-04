package com.multiflame.passiveregen.client;

import com.multiflame.passiveregen.hud.RegenHudRenderer;
import com.multiflame.passiveregen.hud.RegenFeedbackHandler;
import net.minecraftforge.common.MinecraftForge;

public final class PassiveRegenClient {
    public static final RegenHudConfigHolder HOLDER = new RegenHudConfigHolder();

    private PassiveRegenClient() {
    }

    public static void init() {
        HOLDER.config = com.multiflame.passiveregen.config.RegenHudConfig.load();
        MinecraftForge.EVENT_BUS.register(new RegenHudRenderer());
        MinecraftForge.EVENT_BUS.register(new RegenFeedbackHandler());
    }

    public static final class RegenHudConfigHolder {
        public com.multiflame.passiveregen.config.RegenHudConfig config;
    }
}
