package io.github.miche.passiveregen;

import io.github.miche.passiveregen.client.PassiveRegenClient;
import io.github.miche.passiveregen.network.PassiveRegenNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(PassiveRegenMod.MODID)
public class PassiveRegenMod {
    public static final String MODID = "passiveregen";
    public static final String NAME = "Passive Health Regen";
    public static final String VERSION = "1.3.1+1.20.1-neoforge";

    public PassiveRegenMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PassiveRegenConfig.SPEC);
        PassiveRegenNetwork.init();
        MinecraftForge.EVENT_BUS.register(new PassiveRegenHandler());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> PassiveRegenClient::init);
    }
}
