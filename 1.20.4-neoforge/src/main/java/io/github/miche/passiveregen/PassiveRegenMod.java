package io.github.miche.passiveregen;

import io.github.miche.passiveregen.client.PassiveRegenClient;
import io.github.miche.passiveregen.network.PassiveRegenNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(PassiveRegenMod.MODID)
public class PassiveRegenMod {
    public static final String MODID = "passiveregen";
    public static final String NAME = "Passive Health Regen";
    public static final String VERSION = "1.3.1+1.20.4-neoforge";

    public PassiveRegenMod(IEventBus modEventBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PassiveRegenConfig.SPEC);
        modEventBus.addListener(PassiveRegenNetwork::register);
        NeoForge.EVENT_BUS.register(new PassiveRegenHandler());
        if (FMLEnvironment.dist.isClient()) {
            PassiveRegenClient.init();
        }
    }
}
