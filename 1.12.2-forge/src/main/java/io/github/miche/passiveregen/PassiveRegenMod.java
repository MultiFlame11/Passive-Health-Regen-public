package io.github.miche.passiveregen;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = PassiveRegenMod.MODID,
    name = PassiveRegenMod.NAME,
    version = PassiveRegenMod.VERSION,
    acceptableRemoteVersions = "*"
)
public class PassiveRegenMod {
    public static final String MODID = "passiveregen";
    public static final String NAME = "Passive Health Regen";
    public static final String VERSION = "1.2.0+1.12.2-forge";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new PassiveRegenHandler());
    }
}
