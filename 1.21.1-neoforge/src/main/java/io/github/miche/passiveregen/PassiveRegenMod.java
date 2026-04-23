package io.github.miche.passiveregen;

import io.github.miche.passiveregen.client.PassiveRegenClient;
import io.github.miche.passiveregen.network.PassiveRegenNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(PassiveRegenMod.MODID)
public class PassiveRegenMod {
    public static final String MODID = "passiveregen";
    public static final String NAME = "Passive Health Regen";
public static final String VERSION = "1.3.0+1.21.1-neoforge";

    public PassiveRegenMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, PassiveRegenConfig.SPEC);
        modEventBus.addListener(PassiveRegenNetwork::register);
        NeoForge.EVENT_BUS.register(new PassiveRegenHandler());
        if (FMLEnvironment.dist.isClient()) {
            PassiveRegenClient.init(modEventBus);
        }
    }
}
