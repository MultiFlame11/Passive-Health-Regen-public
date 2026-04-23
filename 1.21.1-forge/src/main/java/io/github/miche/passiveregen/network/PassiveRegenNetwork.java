package io.github.miche.passiveregen.network;

import io.github.miche.passiveregen.PassiveRegenMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public final class PassiveRegenNetwork {
    private static final int PROTOCOL = 1;
    public static final SimpleChannel CHANNEL = ChannelBuilder
        .named(ResourceLocation.fromNamespaceAndPath(PassiveRegenMod.MODID, "main"))
        .networkProtocolVersion(PROTOCOL)
        .simpleChannel();

    private static int nextId = 0;

    private PassiveRegenNetwork() {
    }

    public static void init() {
        CHANNEL.messageBuilder(RegenHudPacket.class, nextId++)
            .encoder(RegenHudPacket::encode)
            .decoder(RegenHudPacket::decode)
            .consumerMainThread(RegenHudPacket::handle)
            .add();
    }
}
