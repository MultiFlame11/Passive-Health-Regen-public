# Passive Health Regen

Adds passive health regeneration for players. Stay out of combat long enough with enough food and you'll slowly heal up on your own, no potion needed.

Made mostly because eating food just to top off HP gets old fast, especially with mods that bump up max health.

---

## What it does

- Heals players after a set time without taking damage
- Requires a minimum hunger level before healing kicks in
- Optional ramp-up mode that starts slow and builds up the longer you stay out of combat
- Optional scaling for players with larger health pools
- Server-side only

---

## Config

Quick reference: 20 ticks = 1 second. 1 HP = half a heart. Default health bar is 20 HP (10 hearts).

### Core

| Option | Default | Description |
|---|---|---|
| `damageCooldownTicks` | `100` | Ticks without damage before healing starts. 100 = 5 seconds. |
| `minimumHungerPercent` | `50` | Minimum hunger bar fill required to heal. 0 = any hunger, 50 = at least half, 100 = completely full. |
| `healAmountPerTrigger` | `0.5` | HP healed per trigger. 0.5 = quarter heart, 1.0 = half heart, 2.0 = full heart. |
| `baseHealIntervalTicks` | `100` | Ticks between each heal at base rate. 100 = every 5 seconds. Vanilla Regen I is about every 50 ticks for reference. |
| `updateIntervalTicks` | `20` | How often the mod checks players. 20 = once per second. Lower is more responsive but adds a bit more overhead. |

### Ramp-up

Healing starts slow and speeds up the longer you stay out of combat. Off by default.

| Option | Default | Description |
|---|---|---|
| `rampUpEnabled` | `false` | Enables ramp-up mode. |
| `fullStrengthHealIntervalTicks` | `50` | Ticks between heals at full ramp-up speed. 50 = every 2.5 seconds. |
| `rampFullStrengthTicks` | `600` | Ticks out of combat before hitting full ramp-up speed. 600 = 30 seconds. |

### Health scaling

Heals faster for players with larger health pools. Off by default.

| Option | Default | Description |
|---|---|---|
| `scaleWithMaxHealth` | `false` | Enables heal rate scaling based on max HP above 20. |
| `maxHealthScalingExponent` | `0.5` | Curve for the scaling. 0.5 = square root, so a player with 80 HP heals about 2x as fast, not 4x. |
| `maxHealthScalingCap` | `2.0` | Maximum multiplier from health scaling. |

---

## Versions

| Version | Loader | Jar |
|---|---|---|
| 1.12.2 | Forge | `passive-health-regen-1.1.2+1.12.2-forge.jar` |
| 1.16.5 | Forge | `passive-health-regen-1.1.2+1.16.5-forge.jar` |
| 1.16.5 | Fabric | `passive-health-regen-1.1.2+1.16.5-fabric.jar` |
| 1.18.2 | Forge | `passive-health-regen-1.1.2+1.18.2-forge.jar` |
| 1.18.2 | Fabric | `passive-health-regen-1.1.2+1.18.2-fabric.jar` |
| 1.20.1 | Forge | `passive-health-regen-1.1.2+1.20.1-forge.jar` |
| 1.20.1 | NeoForge | `passive-health-regen-1.1.2+1.20.1-neoforge.jar` |
| 1.20.1 | Fabric | `passive-health-regen-1.1.2+1.20.1-fabric.jar` |
| 1.21.1 | Forge | `passive-health-regen-1.1.2+1.21.1-forge.jar` |
| 1.21.1 | NeoForge | `passive-health-regen-1.1.2+1.21.1-neoforge.jar` |
| 1.21.1 | Fabric | `passive-health-regen-1.1.2+1.21.1-fabric.jar` |
