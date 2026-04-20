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
- Optional hunger bonus system -- eat well, heal faster and wait less
- Optional hunger penalty mode -- regen still works below the food threshold, just slower
- Optional regen-on-kill -- killing an enemy shaves time off your regen cooldown
- Optional kill combo system -- consecutive kills stack up for bigger cooldown cuts
- Optional bonus modifiers: crouch, light level, day/night cycle, difficulty
- Optional campfire proximity bonus (1.20.1-fabric, next release)
- Optional toggle to disable vanilla natural regeneration
- Toggle to allow or block regen while sprinting
- HUD overlay showing your regen cooldown and active healing state (1.12.2-forge, 1.20.1-fabric, 1.21.1-fabric)
- Server-side only, except on 1.12.2-forge, 1.20.1-fabric, and 1.21.1-fabric where the HUD requires client install too

---

## Addon

**[Passive Regen Bandages](https://modrinth.com/mod/passive-regen-bandages)** adds craftable bandage items that instantly clear your regen cooldown and boost healing speed. Works best alongside this mod but can be used standalone.

---

## Config

Quick reference: 20 ticks = 1 second. 1 HP = half a heart. Default health bar is 20 HP (10 hearts).

Options marked **[upcoming]** are implemented in 1.12.2-forge and 1.20.1-fabric but are not yet in the current public release (1.2.1). They will be available in the next version.

### Core

| Option | Default | Description |
|---|---|---|
| `enabled` | `true` | Master toggle. Set to false to disable the mod without uninstalling. |
| `damageCooldownTicks` | `100` | Ticks without damage before healing starts. 100 = 5 seconds. |
| `minimumHungerPercent` | `50` | Minimum hunger bar fill required to heal. 0 = any hunger, 50 = half, 100 = full. |
| `minimumSaturationLevel` | `0.0` | Minimum saturation required to regen normally. 0.0 = disabled. Applies alongside the hunger check. **[upcoming]** |
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
| `disableNaturalRegen` | `false` | Drains food exhaustion to suppress vanilla natural regeneration. Useful if you want this mod to be the only source of passive HP recovery. **[upcoming]** |
| `regenWhileSprinting` | `true` | If false, regen is paused while the player is sprinting. **[upcoming]** |

### Hunger bonus **[upcoming]**

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

### Hunger penalty **[upcoming]**

Instead of hard-blocking regen below the food threshold, the penalty mode lets regen continue at a reduced rate. The HUD still shows orange so players can see something is different.

| Option | Default | Description |
|---|---|---|
| `hungerPenaltyEnabled` | `false` | If true, regen continues below the food threshold but at reduced speed and heal amount instead of stopping entirely. |
| `hungerPenaltySpeedMultiplier` | `0.25` | Speed multiplier when penalized. 0.25 = 4x slower than normal. |
| `hungerPenaltyHealMultiplier` | `1.0` | Heal amount multiplier when penalized. 1.0 = same heal size, just slower. |

### Saturation bonus **[upcoming]**

Rewards players with high saturation with faster and stronger heals. Off by default.

| Option | Default | Description |
|---|---|---|
| `saturationBonusEnabled` | `false` | Enables the saturation bonus. |
| `saturationBonusThreshold` | `10.0` | Saturation level required to get the bonus. |
| `saturationBonusSpeedMultiplier` | `1.25` | Speed multiplier when saturation is above the threshold. |
| `saturationBonusHealMultiplier` | `1.25` | Heal amount multiplier when saturation is above the threshold. |

### Bonus stacking **[upcoming]**

Controls how multiple active bonuses combine when more than one applies at once.

| Option | Default | Description |
|---|---|---|
| `bonusStackingMode` | `MULTIPLICATIVE` | How bonuses combine. `MULTIPLICATIVE` = all multiply together. `ADDITIVE` = extras add up. `STRONGEST_ONLY` = only the highest bonus applies. |

### Regen on kill **[upcoming]**

Killing an enemy reduces your remaining regen cooldown. Off by default.

| Option | Default | Description |
|---|---|---|
| `regenOnKillEnabled` | `false` | Enables cooldown reduction on kill. |
| `regenOnKillCooldownReduction` | `50` | Percent of remaining cooldown removed on kill. 50 = cuts remaining wait in half. |
| `regenOnKillHostileOnly` | `false` | If true, only hostile mobs grant the cooldown reduction. Stops passive animals from being farmed for free regen. |
| `regenOnKillBlacklist` | `[]` | Entity IDs that never grant the kill bonus. Example: `["minecraft:villager"]` |

### Kill combo **[upcoming]**

Consecutive kills within a time window stack up for bigger cooldown cuts on top of the base reduction.

| Option | Default | Description |
|---|---|---|
| `regenOnKillComboEnabled` | `false` | Enables kill combo stacking. |
| `regenOnKillComboWindowTicks` | `200` | How long after a kill the next kill counts as a combo. 200 = 10 seconds. |
| `regenOnKillComboMaxStacks` | `5` | Maximum combo stacks before the reduction caps out. |
| `regenOnKillComboReductionPerStack` | `10` | Extra percent of cooldown removed per combo stack on top of the base reduction. |

### Bonus modifiers **[upcoming]**

These are all off by default and stack with each other and the hunger/saturation bonuses according to `bonusStackingMode`.

**Crouch bonus** -- heal faster and stronger while sneaking.

| Option | Default | Description |
|---|---|---|
| `crouchBonusEnabled` | `false` | Enables the crouch bonus. |
| `crouchSpeedMultiplier` | `1.5` | Speed multiplier while sneaking. |
| `crouchHealMultiplier` | `1.0` | Heal amount multiplier while sneaking. 1.0 = no change. |

**Light level** -- regen scales with how bright your surroundings are.

| Option | Default | Description |
|---|---|---|
| `lightLevelBonusEnabled` | `false` | Enables light level scaling. |
| `lightLevelMinMultiplier` | `0.75` | Multiplier at light level 0. Below 1.0 = penalty. |
| `lightLevelMaxMultiplier` | `1.25` | Multiplier at light level 15. Interpolated linearly between min and max. |

**Day/night** -- regen changes based on the time of day.

| Option | Default | Description |
|---|---|---|
| `dayNightMultiplierEnabled` | `false` | Enables day/night scaling. |
| `dayMultiplier` | `1.25` | Multiplier during daytime. |
| `nightMultiplier` | `0.75` | Multiplier during nighttime. |

**Difficulty scaling** -- regen scales with world difficulty.

| Option | Default | Description |
|---|---|---|
| `difficultyScalingEnabled` | `false` | Enables difficulty scaling. |
| `peacefulMultiplier` | `2.0` | Multiplier on Peaceful. |
| `easyMultiplier` | `1.25` | Multiplier on Easy. |
| `normalMultiplier` | `1.0` | Multiplier on Normal. |
| `hardMultiplier` | `0.75` | Multiplier on Hard. |

### Large damage penalty **[upcoming]**

Taking a big hit in one shot extends your regen cooldown beyond the normal duration.

| Option | Default | Description |
|---|---|---|
| `largeDamagePenaltyEnabled` | `false` | Enables the large damage penalty. |
| `largeDamageThresholdPercent` | `50` | Single hit must deal at least this percent of max HP to count as a large hit. |
| `largeDamageCooldownMultiplier` | `1.5` | Multiplier applied to the cooldown duration after a large hit. |

### Campfire **[upcoming, 1.20.1-fabric only]**

Regen activates when standing near a lit campfire regardless of other conditions. Soul campfires count too.

| Option | Default | Description |
|---|---|---|
| `campfireRegenEnabled` | `false` | Enables campfire proximity regen. |
| `campfireRadius` | `8` | Block radius to check for a nearby lit campfire. |

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

// Bypasses hunger and saturation gates for a duration (upcoming, 1.12.2-forge)
// Player regens at full normal rate regardless of food state
PassiveRegenAPI.overrideHungerRestrictions(player.getUUID(), durationTicks);

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
