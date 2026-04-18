package io.github.miche.passiveregen;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class PassiveRegenMod implements ModInitializer {
    public static final String MODID = "passiveregen";
    public static final String NAME = "Passive Health Regen";
    public static final String VERSION = "1.2.0+1.16.5-fabric";

    private final PassiveRegenHandler handler = new PassiveRegenHandler();
    private PassiveRegenConfig config;

    @Override
    public void onInitialize() {
        config = PassiveRegenConfig.load();

        ServerTickEvents.END_SERVER_TICK.register(server -> handler.onServerTick(server.getPlayerList().getPlayers(), config));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> this.handler.onPlayerDisconnect(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> handler.onPlayerRespawn(newPlayer));
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killedEntity) -> {
            if (killer instanceof ServerPlayer) {
                handler.onEntityKilled((ServerPlayer) killer, config, world.getGameTime());
            }
        });
    }
}
