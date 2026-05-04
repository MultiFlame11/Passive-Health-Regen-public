# Passive Health Regen 1.3.3

Compatibility and persistence patch on top of 1.3.2.

## Changed

- Public package and Maven namespace now use `com.multiflame.passiveregen` across all version branches.

## Fixed

- Fabric cooldown persistence now fails soft when the reconnect cooldown save file contains malformed JSON.
- Fabric cooldown persistence now writes through a temp file before replacing the live save, which reduces the chance of leaving a broken JSON file behind after an interrupted write.
- Ramp-up timing now starts after the passive regen cooldown finishes instead of sharing the same out-of-combat timer.

## Added

- Updated release and workflow metadata for the new `1.3.3` version line.

## Upgrading

- If an older Fabric jar already left behind a broken cooldown save, delete `world/data/passive-health-regen-cooldowns.json` once and let the mod regenerate it.
- API users and integrations should update imports from `io.github.miche.passiveregen...` to `com.multiflame.passiveregen...`.

## Build coverage

Verified clean after this patch:

1.12.2 Forge, 1.16.5 Fabric, 1.16.5 Forge, 1.18.2 Fabric, 1.18.2 Forge, 1.20.1 Fabric, 1.20.1 Forge, 1.20.1 NeoForge, 1.20.4 NeoForge, 1.21.1 Fabric, 1.21.1 Forge, 1.21.1 NeoForge.

## Issues

If something breaks, open an issue with your version, loader, and config. Makes debugging way faster.
