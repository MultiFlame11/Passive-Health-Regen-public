package com.multiflame.passiveregen.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class PassiveRegenNetwork {
    private static final String PROTOCOL = "1";

    private PassiveRegenNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL)
            .playToClient(RegenHudPacket.TYPE, RegenHudPacket.STREAM_CODEC, RegenHudPacket::handle);
    }
}
