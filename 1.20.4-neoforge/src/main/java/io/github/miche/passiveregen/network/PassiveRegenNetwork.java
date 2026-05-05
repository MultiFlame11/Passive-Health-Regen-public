package io.github.miche.passiveregen.network;

import io.github.miche.passiveregen.PassiveRegenMod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;

public final class PassiveRegenNetwork {
    private static final String PROTOCOL = "1";

    private PassiveRegenNetwork() {
    }

    public static void register(RegisterPayloadHandlerEvent event) {
        event.registrar(PassiveRegenMod.MODID)
            .versioned(PROTOCOL)
            .play(RegenHudPacket.ID, RegenHudPacket::new, handlers -> handlers.client(RegenHudPacket::handle));
    }
}
