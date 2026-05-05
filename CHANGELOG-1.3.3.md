# Passive Health Regen 1.3.3

Small bugfix patch on top of 1.3.2.

No new gameplay features here. This one just fixes a couple of annoying problems.

## Fixed

- Fixed a Fabric reconnect crash if the cooldown save file had malformed JSON. It now falls back to an empty cooldown map and keeps going. This affects the Fabric builds on 1.16.5, 1.18.2, 1.20.1, and 1.21.1.
- Fabric cooldown saves now write through a temp file first, so a bad shutdown is a lot less likely to leave the file corrupted.
- Fixed ramp-up timing so it starts after the damage cooldown ends. Before this, the damage cooldown and ramp-up were eating the same timer, which meant regen could start partway through its ramp instead of getting the full ramp window.

## Upgrading

- Existing configs are not touched.
- If an older Fabric build already left behind a broken cooldown save, delete `world/data/passive-health-regen-cooldowns.json` once and let the mod regenerate it.
- Addon and API users do not need to change imports for `1.3.3`. The public API namespace is still `io.github.miche.passiveregen...`.

## Build coverage

All 12 builds were checked and all 12 compile:

1.12.2 Forge, 1.16.5 Forge, 1.16.5 Fabric, 1.18.2 Forge, 1.18.2 Fabric, 1.20.1 Forge, 1.20.1 Fabric, 1.20.1 NeoForge, 1.20.4 NeoForge, 1.21.1 Forge, 1.21.1 Fabric, 1.21.1 NeoForge.

## Issues

If something breaks, open an issue with your version, loader, and config. That makes it way easier to track down.
