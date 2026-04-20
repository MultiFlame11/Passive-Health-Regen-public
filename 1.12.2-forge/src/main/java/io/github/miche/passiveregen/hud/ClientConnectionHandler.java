package io.github.miche.passiveregen.hud;

import io.github.miche.passiveregen.client.RegenHudState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public final class ClientConnectionHandler {
    @SubscribeEvent
    public void onClientPlayerJoinWorld(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote || !(event.getEntity() instanceof EntityPlayer)) {
            return;
        }

        Minecraft client = Minecraft.getMinecraft();
        if (event.getEntity() == client.player) {
            RegenHudState.get().reset();
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        RegenHudState.get().reset();
    }
}
