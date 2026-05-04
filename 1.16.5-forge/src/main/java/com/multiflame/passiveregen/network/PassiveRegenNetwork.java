package com.multiflame.passiveregen.network;

import com.multiflame.passiveregen.PassiveRegenMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

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
