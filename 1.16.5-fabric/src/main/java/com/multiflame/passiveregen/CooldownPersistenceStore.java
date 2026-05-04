package com.multiflame.passiveregen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

final class CooldownPersistenceStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, SavedCooldown>>() {}.getType();

    private CooldownPersistenceStore() {
    }

    static SavedCooldown load(MinecraftServer server, UUID playerId) {
        Map<String, SavedCooldown> data = readAll(server);
        return data.get(playerId.toString());
    }

    static void save(MinecraftServer server, UUID playerId, long outOfCombatTicks, int cooldownDurationTicks) {
        Map<String, SavedCooldown> data = readAll(server);
        data.put(playerId.toString(), new SavedCooldown(Math.max(0L, outOfCombatTicks), Math.max(0, cooldownDurationTicks)));
        writeAll(server, data);
    }

    static void clear(MinecraftServer server, UUID playerId) {
        Map<String, SavedCooldown> data = readAll(server);
        if (data.remove(playerId.toString()) != null) {
            writeAll(server, data);
        }
    }

    private static Map<String, SavedCooldown> readAll(MinecraftServer server) {
        Path path = getPath(server);
        if (!Files.exists(path)) {
            return new HashMap<>();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            Map<String, SavedCooldown> loaded = GSON.fromJson(reader, MAP_TYPE);
            return loaded != null ? new HashMap<>(loaded) : new HashMap<>();
        } catch (IOException | RuntimeException ignored) {
            return new HashMap<>();
        }
    }

    private static void writeAll(MinecraftServer server, Map<String, SavedCooldown> data) {
        Path path = getPath(server);
        Path tempPath = path.resolveSibling(path.getFileName().toString() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(tempPath)) {
                GSON.toJson(data, MAP_TYPE, writer);
            }
            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignoredDelete) {
            }
        }
    }

    private static Path getPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
            .resolve("data")
            .resolve(PassiveRegenMod.MODID + "-cooldowns.json");
    }

    static final class SavedCooldown {
        private final long outOfCombatTicks;
        private final int cooldownDurationTicks;

        SavedCooldown(long outOfCombatTicks, int cooldownDurationTicks) {
            this.outOfCombatTicks = outOfCombatTicks;
            this.cooldownDurationTicks = cooldownDurationTicks;
        }

        long outOfCombatTicks() {
            return outOfCombatTicks;
        }

        int cooldownDurationTicks() {
            return cooldownDurationTicks;
        }
    }
}
