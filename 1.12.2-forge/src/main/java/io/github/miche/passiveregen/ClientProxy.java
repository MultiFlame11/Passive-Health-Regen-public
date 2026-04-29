package io.github.miche.passiveregen;

import io.github.miche.passiveregen.client.RegenHudState;
import io.github.miche.passiveregen.config.RegenHudConfig;
import io.github.miche.passiveregen.hud.ClientConnectionHandler;
import io.github.miche.passiveregen.hud.RegenFeedbackHandler;
import io.github.miche.passiveregen.hud.RegenHudRenderer;
import io.github.miche.passiveregen.network.RegenHudPacket;
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
