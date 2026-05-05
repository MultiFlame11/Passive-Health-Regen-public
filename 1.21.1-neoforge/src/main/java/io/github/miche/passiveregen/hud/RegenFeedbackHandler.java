package io.github.miche.passiveregen.hud;

import io.github.miche.passiveregen.client.PassiveRegenClient;
import io.github.miche.passiveregen.client.RegenHudState;
import io.github.miche.passiveregen.config.RegenHudConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@OnlyIn(Dist.CLIENT)
public final class RegenFeedbackHandler {
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        if (RegenHudState.get().consumeJustHealed()) {
            trigger(client, PassiveRegenClient.HOLDER.config);
        }
    }

    private static void trigger(Minecraft client, RegenHudConfig config) {
        ClientLevel level = client.level;
        if (level == null || client.player == null || config == null) {
            return;
        }

        double x = client.player.getX();
        double y = client.player.getY() + client.player.getBbHeight() * 0.6D;
        double z = client.player.getZ();

        if (config.particles != null) {
            for (RegenHudConfig.ParticleEntry entry : config.particles) {
                if (entry == null || entry.id == null || entry.id.isBlank() || entry.count <= 0) continue;
                ParticleOptions options = resolveParticle(entry.id);
                if (options == null) continue;
                for (int i = 0; i < entry.count; i++) {
                    double spread = entry.spread;
                    double px = x + (level.random.nextDouble() * 2.0D - 1.0D) * spread;
                    double py = y + (level.random.nextDouble() * 2.0D - 1.0D) * spread * 0.5D;
                    double pz = z + (level.random.nextDouble() * 2.0D - 1.0D) * spread;
                    level.addParticle(options, px, py, pz, 0.0D, 0.02D, 0.0D);
                }
            }
        }

        if (config.sounds != null) {
            for (RegenHudConfig.SoundEntry entry : config.sounds) {
                if (entry == null || entry.id == null || entry.id.isBlank()) continue;
                SoundEvent sound = resolveSound(entry.id);
                if (sound == null) continue;
                level.playLocalSound(x, y, z, sound, SoundSource.PLAYERS, entry.volume, entry.pitch, false);
            }
        }
    }

    private static ParticleOptions resolveParticle(String idString) {
        ResourceLocation id = ResourceLocation.tryParse(idString);
        if (id == null || !BuiltInRegistries.PARTICLE_TYPE.containsKey(id)) {
            return null;
        }

        if (BuiltInRegistries.PARTICLE_TYPE.get(id) instanceof SimpleParticleType simple) {
            return simple;
        }

        return null;
    }

    private static SoundEvent resolveSound(String idString) {
        ResourceLocation id = ResourceLocation.tryParse(idString);
        if (id == null || !BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
            return null;
        }
        return BuiltInRegistries.SOUND_EVENT.get(id);
    }
}
