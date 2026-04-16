package io.github.miche.passiveregen;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public final class PassiveRegenMod implements ModInitializer {
    public static final String MODID = "passiveregen";
    public static final String NAME = "Passive Health Regen";
    public static final String VERSION = "1.1.2+1.21.1-fabric";

    private final PassiveRegenHandler handler = new PassiveRegenHandler();
    private PassiveRegenConfig config;

    @Override
    public void onInitialize() {
        config = PassiveRegenConfig.load();

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (config.enabled && entity instanceof ServerPlayer player && amount > 0.0F) {
                handler.onPlayerDamaged(player);
            }
            return true;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> handler.onServerTick(server.getPlayerList().getPlayers(), config));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> this.handler.onPlayerDisconnect(handler.player));
    }
}
