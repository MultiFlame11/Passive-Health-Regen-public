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
- Optional hunger bonus system — eat well, heal faster and wait less
- Optional regen-on-kill — killing an enemy shaves time off your regen cooldown
- Optional toggle to disable vanilla natural regeneration
- Toggle to allow or block regen while sprinting
- HUD overlay showing your regen cooldown and active healing state (1.20.1 Fabric and 1.21.1 Fabric only)
- Server-side only, except on 1.20.1 and 1.21.1 Fabric where the HUD requires client install too

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
| `disableNaturalRegen` | `false` | Drains food exhaustion to suppress vanilla natural regeneration. Useful if you want this mod to be the only source of passive HP recovery. |
| `regenWhileSprinting` | `true` | If false, regen is paused while the player is sprinting. |

### Hunger bonus

Gives bonus healing speed, heal amount, and shorter cooldown when hunger is above a threshold. Both tiers off by default.

| Option | Default | Description |
|---|---|---|
| `hungerBonusEnabled` | `false` | Enables the threshold hunger bonus. |
| `hungerBonusThresholdPercent` | `75` | Hunger level required to get the bonus. 75 = 15/20 bars. |
| `hungerBonusHealMultiplier` | `1.5` | Multiplier applied to heal amount when above threshold. |
| `hungerBonusSpeedMultiplier` | `1.5` | Multiplier applied to heal speed when above threshold. |
| `hungerBonusCooldownReduction` | `25` | Percent reduction to the damage cooldown when above threshold. 25 = 5s wait becomes 3.75s. |
| `hungerFullBonusEnabled` | `false` | Enables a second bonus tier that applies only at completely full hunger (20/20). Stacks on top of the threshold bonus. |
| `hungerFullBonusHealMultiplier` | `2.0` | Heal amount multiplier at full hunger. |
| `hungerFullBonusSpeedMultiplier` | `2.0` | Heal speed multiplier at full hunger. |

### Regen on kill

Killing an enemy reduces your remaining regen cooldown. Off by default.

| Option | Default | Description |
|---|---|---|
| `regenOnKillEnabled` | `false` | Enables cooldown reduction on kill. |
| `regenOnKillCooldownReduction` | `50` | Percent of remaining cooldown removed on kill. 50 = cuts remaining wait in half. |

---

## Developer API

Addon mods can interact with the regen system via the public API.

```java
// Clears the damage cooldown so regen starts on the next tick
PassiveRegenAPI.clearDamageCooldown(player.getUUID());

// Reduces the remaining cooldown by a percentage of what is left
// 50 = cuts remaining wait in half, 100 = same as clear
PassiveRegenAPI.reduceCooldown(player.getUUID(), 50);

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
