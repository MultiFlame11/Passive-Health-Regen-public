# Passive Health Regen 1.3.1

Small patch release. One bugfix, one new optional feature, nothing scary.

## Fixed

HUD config wasn't marked client-only, so dedicated servers generated a stray `passive-health-regen-hud.json` in their config folder. Harmless but it looked like the server was overriding client settings. My bad, forgot the annotation when I split the class out for 1.3.0.

Fixed across all 12 builds with the proper client-only tag per loader. Servers that already generated the stray file can delete it and it won't come back.

## New: optional hunger drain

Off by default. If you want eating to actually matter for passive regen, flip `hungerDrainEnabled` on and the mod will drain hunger as you heal, same way the existing saturation bonus already drains saturation.

Five knobs under a new `hungerDrain` section:

| Option | Default | What it does |
|---|---|---|
| `hungerDrainEnabled` | `false` | Master toggle. |
| `hungerDrainSpeedMultiplier` | `1.0` | Scalar on overall drain rate. |
| `hungerDrainCostPerHp` | `0.6` | Hunger drained per HP healed. |
| `hungerDrainIdleDrainPerTick` | `0.0` | Passive drain while at full HP. |
| `hungerDrainMinFloor` | `0.0` | Drain stops at this hunger value. |

Defaults are tuned so just flipping the master toggle gives a sensible baseline. Tune from there if you want it lighter or harsher. Section shape mirrors the existing `saturationBonus` section, so if you already use that it should feel familiar.

On 1.12.2, the drain directly subtracts from the food level. Modern versions do the same. Not routed through vanilla exhaustion, so it's predictable and independent of the saturation drain.

## Upgrading

No config migration needed. Old configs load fine and get the new `hungerDrain` block added with disabled defaults on the next write. If you want the new defaults fresh, delete the config and let it regenerate.

## Build coverage

All 12 builds touched, all 12 compile, all 12 ship:

1.12.2 Forge, 1.16.5 Forge, 1.16.5 Fabric, 1.18.2 Forge, 1.18.2 Fabric, 1.20.1 Forge, 1.20.1 Fabric, 1.20.1 NeoForge, 1.20.4 NeoForge, 1.21.1 Forge, 1.21.1 Fabric, 1.21.1 NeoForge.

## Issues

If something breaks, open an issue with your version, loader, and config. Makes debugging way faster.
