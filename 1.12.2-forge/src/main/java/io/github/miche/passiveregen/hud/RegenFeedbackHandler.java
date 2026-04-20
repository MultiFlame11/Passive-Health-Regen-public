package io.github.miche.passiveregen.hud;

import io.github.miche.passiveregen.client.RegenHudState;
import io.github.miche.passiveregen.config.RegenHudConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public final class RegenFeedbackHandler {
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft client = Minecraft.getMinecraft();
        if (client.player == null || client.world == null) {
            return;
        }

        if (RegenHudState.get().consumeJustHealed()) {
            trigger(client, RegenHudConfig.current());
        }
    }

    private void trigger(Minecraft client, RegenHudConfig config) {
        if (client.world == null || client.player == null || config == null) {
            return;
        }

        double x = client.player.posX;
        double y = client.player.posY + client.player.height * 0.6D;
        double z = client.player.posZ;

        if (config.particles != null) {
            for (RegenHudConfig.ParticleEntry entry : config.particles) {
                if (entry == null || entry.id == null || entry.id.trim().isEmpty() || entry.count <= 0) {
                    continue;
                }

                EnumParticleTypes particle = resolveParticle(entry.id);
                if (particle == null) {
                    continue;
                }

                for (int i = 0; i < entry.count; i++) {
                    double spread = entry.spread;
                    double px = x + (client.world.rand.nextDouble() * 2.0D - 1.0D) * spread;
                    double py = y + (client.world.rand.nextDouble() * 2.0D - 1.0D) * spread * 0.5D;
                    double pz = z + (client.world.rand.nextDouble() * 2.0D - 1.0D) * spread;
                    client.world.spawnParticle(particle, px, py, pz, 0.0D, 0.02D, 0.0D);
                }
            }
        }

        if (config.sounds != null) {
            for (RegenHudConfig.SoundEntry entry : config.sounds) {
                if (entry == null || entry.id == null || entry.id.trim().isEmpty()) {
                    continue;
                }

                SoundEvent sound = resolveSound(entry.id);
                if (sound == null) {
                    continue;
                }

                client.world.playSound(
                    client.player,
                    x,
                    y,
                    z,
                    sound,
                    SoundCategory.PLAYERS,
                    entry.volume,
                    entry.pitch
                );
            }
        }
    }

    private static EnumParticleTypes resolveParticle(String idString) {
        String clean = idString.trim();
        int colon = clean.indexOf(':');
        if (colon >= 0) {
            clean = clean.substring(colon + 1);
        }
        return EnumParticleTypes.getByName(clean);
    }

    private static SoundEvent resolveSound(String idString) {
        ResourceLocation id = new ResourceLocation(idString.contains(":") ? idString : "minecraft:" + idString);
        return ForgeRegistries.SOUND_EVENTS.getValue(id);
    }
}
