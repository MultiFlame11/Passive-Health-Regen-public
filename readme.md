# Passive Health Regen

Adds passive health regeneration for players. Stay out of combat long enough with enough food and you'll slowly heal up on your own, no potion needed.

Made mostly because eating food just to top off HP gets old fast, especially with mods that bump up max health.

---

## What it does

- Heals players after a set time without taking damage
- Requires a minimum hunger level before healing kicks in
- Optional ramp-up mode that starts slow and builds up the longer you stay out of combat
- Optional scaling for players with larger health pools
- Stops regen above a configurable health percentage
- Blocks regen while specific potion effects are active
- Disable regen entirely in certain dimensions
- Separate longer cooldown for PvP damage
- HUD overlay showing your regen cooldown and active healing state (1.20.1 Fabric only for now)
- Server-side only, except on 1.20.1 Fabric where the HUD requires client install too

---

## Addon

**[Passive Regen Bandages](https://modrinth.com/mod/passive-regen-bandages)** adds craftable bandage items that instantly clear your regen cooldown and boost healing speed. Works best alongside this mod but can be used standalone.

---

## Config

Quick reference: 20 ticks = 1 second. 1 HP = half a heart. Default health bar is 20 HP (10 hearts).

### Core

| Option | Default | Description |
|---|---|---|
| `enabled` | `true` | Master toggle. Set to false to disable the mod without uninstalling. |
| `damageCooldownTicks` | `100` | Ticks without damage before healing starts. 100 = 5 seconds. |
| `minimumHungerPercent` | `50` | Minimum hunger bar fill required to heal. 0 = any hunger, 50 = half, 100 = full. |
| `healAmountPerTrigger` | `0.5` | HP healed per trigger. 0.5 = quarter heart, 1.0 = half heart, 2.0 = full heart. |
| `baseHealIntervalTicks` | `100` | Ticks between each heal at base rate. 100 = every 5 seconds. |
| `updateIntervalTicks` | `20` | How often the mod checks players. Lower is more responsive but adds a bit more overhead. |

### Ramp-up

Healing starts slow and speeds up the longer you stay out of combat. Off by default.

| Option | Default | Description |
|---|---|---|
| `rampUpEnabled` | `false` | Enables ramp-up mode. |
| `fullStrengthHealIntervalTicks` | `50` | Ticks between heals at full ramp-up speed. |
| `rampFullStrengthTicks` | `600` | Ticks out of combat before hitting full ramp-up speed. 600 = 30 seconds. |

### Health scaling

Heals faster for players with larger health pools. Off by default.

| Option | Default | Description |
|---|---|---|
| `scaleWithMaxHealth` | `false` | Enables heal rate scaling based on max HP above 20. |
| `maxHealthScalingExponent` | `0.5` | Curve for the scaling. 0.5 = square root, gentle curve. 1.0 = linear. |
| `maxHealthScalingCap` | `2.0` | Maximum multiplier from health scaling. |

### Conditions

| Option | Default | Description |
|---|---|---|
| `maxRegenHealthPercent` | `100` | Regen stops when health reaches this percentage of max health. 80 = stops at 80%. |
| `blockedEffects` | `[]` | List of effect IDs that pause regen while active. Example: `["minecraft:poison"]` |
| `dimensionBlacklist` | `[]` | List of dimension IDs where regen is disabled. Example: `["minecraft:the_nether"]` |
| `pvpDamageCooldownTicks` | `-1` | Separate cooldown after taking damage from another player. -1 = same as regular cooldown. |

---

## Developer API

Addon mods can interact with the regen system via the public API.

```java
// Clears the damage cooldown so regen starts on the next tick
PassiveRegenAPI.clearDamageCooldown(player.getUUID());

// Applies a temporary regen speed multiplier
// 1.5 = 50% faster, 2.0 = double speed
// durationTicks = how long the boost lasts
PassiveRegenAPI.applyRegenBoost(player.getUUID(), multiplier, durationTicks);

// Check if the mod is loaded before calling (useful for optional dependencies)
if (PassiveRegenAPI.isAvailable()) { ... }
```

Declare as an optional dependency in your `fabric.mod.json` or `mods.toml`.

---

## Versions

| Version | Loader |
|---|---|
| 1.12.2 | Forge |
| 1.16.5 | Forge, Fabric |
| 1.18.2 | Forge, Fabric |
| 1.20.1 | Forge, NeoForge, Fabric |
| 1.20.4 | NeoForge |
| 1.21.1 | Forge, NeoForge, Fabric |
