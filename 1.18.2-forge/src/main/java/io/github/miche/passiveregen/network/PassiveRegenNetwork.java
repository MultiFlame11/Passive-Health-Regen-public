package io.github.miche.passiveregen.network;

import io.github.miche.passiveregen.PassiveRegenMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PassiveRegenNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(PassiveRegenMod.MODID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );

    private static int nextId = 0;

    private PassiveRegenNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(nextId++, RegenHudPacket.class, RegenHudPacket::encode, RegenHudPacket::decode, RegenHudPacket::handle);
    }
}
