# Passive Health Regen 1.3.2

Cleanup pass on top of 1.3.1. Mostly fixes and one structural change to how `disableNaturalRegen` actually works.

## Changed

`disableNaturalRegen` now toggles the vanilla `naturalRegeneration` gamerule directly. The old behavior leaned on exhaustion based soft suppression, which had edge cases where vanilla regen could still tick through. Going through the gamerule is the right hook and makes it a hard off when the toggle is on.

Default for `disableNaturalRegen` flipped to `true` on fresh configs. The mod is opinionated about owning the regen loop so the new default reflects that. Old configs are not touched (see Upgrading below).

## Fixed

Saturation bonus could still apply when the player was in the hunger or saturation penalty state. The bonus now correctly stays off while the penalty state is active.

Saturation bonus HUD sheen had the same bleed through in the penalty state. The visual now stays off too, matching the gameplay state.

Removed a temporary `1.20.1-forge` debug config field and logger that snuck into 1.3.1. Pure dead code, no behavior impact, but it was generating extra log lines and an unused config knob on that one build only. Gone now.

## Added

Modern HUD compatibility with mods that replace the vanilla health bar:

- `1.20.1-neoforge` and `1.20.4-neoforge` now use `hudRenderOverlay` for the heart paint, same hook that `1.20.1-forge` already uses. Lets HUD replacement mods cleanly pass the heart render through instead of stomping it.
- `1.21.1-neoforge` HUD render layer moved to draw above the hotbar, matching where `1.21.1-forge` draws. Was rendering one layer off from the forge sibling, which read as a small visual mismatch when running the same setup on the two loaders.

## Upgrading

Existing configs are not auto migrated. If you upgrade and want vanilla natural regen actually disabled, check your config and set `disableNaturalRegen=true` by hand. Or delete the config to regenerate it with the new defaults. Fresh installs get the right default automatically.

No other migration steps. The saturation and HUD fixes apply on next tick.

## Build coverage

All 12 builds touched, all 12 compile, all 12 ship:

1.12.2 Forge, 1.16.5 Forge, 1.16.5 Fabric, 1.18.2 Forge, 1.18.2 Fabric, 1.20.1 Forge, 1.20.1 Fabric, 1.20.1 NeoForge, 1.20.4 NeoForge, 1.21.1 Forge, 1.21.1 Fabric, 1.21.1 NeoForge.

## Issues

If something breaks, open an issue with your version, loader, and config. Makes debugging way faster.
