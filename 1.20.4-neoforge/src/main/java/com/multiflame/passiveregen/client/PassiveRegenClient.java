package com.multiflame.passiveregen.client;

import com.multiflame.passiveregen.hud.RegenHudRenderer;
import com.multiflame.passiveregen.hud.RegenFeedbackHandler;
import net.neoforged.neoforge.common.NeoForge;

public final class PassiveRegenClient {
    public static final RegenHudConfigHolder HOLDER = new RegenHudConfigHolder();

    private PassiveRegenClient() {
    }

    public static void init() {
        HOLDER.config = com.multiflame.passiveregen.config.RegenHudConfig.load();
        NeoForge.EVENT_BUS.register(new RegenHudRenderer());
        NeoForge.EVENT_BUS.register(new RegenFeedbackHandler());
    }

    public static final class RegenHudConfigHolder {
        public com.multiflame.passiveregen.config.RegenHudConfig config;
    }
}
