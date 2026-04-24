package io.github.miche.passiveregen;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public final class PassiveRegenMod implements ModInitializer {
    public static final String MODID = "passiveregen";
    public static final String NAME = "Passive Health Regen";
    public static final String VERSION = "1.3.1+1.16.5-fabric";

    private final PassiveRegenHandler handler = new PassiveRegenHandler();
    private PassiveRegenConfig config;

    @Override
    public void onInitialize() {
        config = PassiveRegenConfig.load();
        handler.setConfig(config);

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killedEntity) -> {
            if (killer instanceof ServerPlayer) {
                handler.onEntityKilled((ServerPlayer) killer, killedEntity, config);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> handler.onServerTick(server, server.getPlayerList().getPlayers(), config));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> this.handler.onPlayerLogin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> this.handler.onPlayerDisconnect(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> handler.onPlayerRespawn(newPlayer));
    }
}
