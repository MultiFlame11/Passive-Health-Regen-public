package io.github.miche.passiveregen;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(PassiveRegenMod.MODID)
public class PassiveRegenMod {
    public static final String MODID = "passiveregen";
    public static final String NAME = "Passive Health Regen";
    public static final String VERSION = "1.2.0+1.16.5-forge";

    public PassiveRegenMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PassiveRegenConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new PassiveRegenHandler());
    }
}
