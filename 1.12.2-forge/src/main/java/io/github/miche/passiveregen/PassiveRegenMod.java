package io.github.miche.passiveregen;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

@Mod(
    modid = PassiveRegenMod.MODID,
    name = PassiveRegenMod.NAME,
    version = PassiveRegenMod.VERSION,
    guiFactory = "io.github.miche.passiveregen.client.gui.PassiveRegenGuiFactory",
    acceptableRemoteVersions = "*"
)
public class PassiveRegenMod {
    public static final String MODID = "passiveregen";
    public static final String NAME = "Passive Health Regen";
    public static final String VERSION = "1.3.1+1.12.2-forge";
    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

    @SidedProxy(clientSide = "io.github.miche.passiveregen.ClientProxy", serverSide = "io.github.miche.passiveregen.CommonProxy")
    public static CommonProxy PROXY;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new PassiveRegenHandler());
        NetworkHandler.registerMessages();
        PROXY.preInit(event);
    }
}
