# Passive Health Regen 1.3.2

Small cleanup patch on top of 1.3.1.

## Changed

- `disableNaturalRegen` now uses the vanilla `naturalRegeneration` gamerule when you turn it on.
- Fresh configs keep `disableNaturalRegen=false` by default.

## Fixed

- Saturation bonus no longer kicks in while the hunger or saturation penalty state is active.
- Saturation HUD sheen now stays off in that same penalty state.
- Removed the stray `1.20.1-forge` debug config field and logger from `1.3.1`.

## Added

- Better HUD compatibility for modern HUD replacement mods.
- `1.20.1-neoforge` and `1.20.4-neoforge` now expose `hudRenderOverlay`.
- `1.21.1-neoforge` HUD draw order now matches the Forge build.

## Upgrading

No migration needed. Existing configs keep their own value for `disableNaturalRegen`.

## Build coverage

All 12 builds touched, all 12 compile, all 12 ship:

1.12.2 Forge, 1.16.5 Forge, 1.16.5 Fabric, 1.18.2 Forge, 1.18.2 Fabric, 1.20.1 Forge, 1.20.1 Fabric, 1.20.1 NeoForge, 1.20.4 NeoForge, 1.21.1 Forge, 1.21.1 Fabric, 1.21.1 NeoForge.

## Issues

If something breaks, open an issue with your version, loader, and config. Makes debugging way faster.
