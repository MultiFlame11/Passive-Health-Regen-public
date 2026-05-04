package com.multiflame.passiveregen;

import com.multiflame.passiveregen.network.RegenHudPacket;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;

public final class NetworkHandler {
    private static int nextId = 0;

    private NetworkHandler() {
    }

    public static void registerMessages() {
        PassiveRegenMod.NETWORK.registerMessage(RegenHudPacket.Handler.class, RegenHudPacket.class, nextId++, Side.CLIENT);
    }
}
