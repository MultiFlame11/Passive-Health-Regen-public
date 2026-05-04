package com.multiflame.passiveregen;

import com.multiflame.passiveregen.client.RegenHudState;
import com.multiflame.passiveregen.config.RegenHudConfig;
import com.multiflame.passiveregen.hud.ClientConnectionHandler;
import com.multiflame.passiveregen.hud.RegenFeedbackHandler;
import com.multiflame.passiveregen.hud.RegenHudRenderer;
import com.multiflame.passiveregen.network.RegenHudPacket;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        RegenHudConfig.load();
        MinecraftForge.EVENT_BUS.register(new RegenHudRenderer());
        MinecraftForge.EVENT_BUS.register(new RegenFeedbackHandler());
        MinecraftForge.EVENT_BUS.register(new ClientConnectionHandler());
    }

    @Override
    public void handleHudPacket(RegenHudPacket message) {
        RegenHudState.get().applyPacket(
            message.outOfCombatTicks,
            message.damageCooldownTicks,
            message.regenActive,
            message.hungerBlocked,
            message.justHealed,
            message.currentHealth,
            message.maxHealth,
            message.maxRegenHealthPercent,
            message.saturationBonus,
            message.poisoned,
            message.withered
        );
    }
}
