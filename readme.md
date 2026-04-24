# Passive Health Regen

Adds passive health regeneration for players. Stay out of combat long enough with enough food and you heal up on your own. No potion needed.

Made mostly because eating food just to top off HP gets old fast, especially with mods that bump up max health.

Current release: **1.3.1**. See [CHANGELOG-1.3.1.md](CHANGELOG-1.3.1.md) for the latest release notes, [CHANGELOG-1.3.0.md](CHANGELOG-1.3.0.md) for the big parity release, and [MODDING.md](MODDING.md) for resource pack / addon author info.

---

## What it does

**Core regen:**
- Heals players after a set time without taking damage
- Requires a minimum hunger level before healing kicks in
- Optional minimum saturation gate on top of the hunger gate
- Optional ramp-up mode that starts slow and speeds up the longer you stay out of combat
- Optional scaling for players with larger health pools
- Stops regen above a configurable health percentage
- Blocks regen while configured potion effects are active
- Disable regen entirely in configured dimensions
- Separate longer cooldown for PvP damage
- Optional toggle to disable vanilla natural regeneration
- Toggle to allow or block regen while sprinting
- Reconnect cooldown persistence. Your damage cooldown survives disconnects

**Bonuses:**
- Hunger bonus. Eat well, heal faster and wait less, with an optional second tier at full hunger
- Hunger penalty mode. Below the food threshold, regen slows instead of stopping
- Saturation bonus with hysteresis, cost per HP healed, flat HP bonus on top of multipliers, optional scale-by-excess mode, saturation floor, and idle drain knob
- Crouch, light level, day/night, and difficulty modifiers
- Configurable stacking mode for all bonuses (multiplicative, additive, strongest-only)

**Combat feedback:**
- Regen-on-kill cooldown reduction with optional hostile-only restriction and entity blacklist
- Kill combo stacking for streak bonuses
- Large damage cooldown penalty. A single big hit extends your wait
- Poison and wither healing gates. On by default. Flip off if another mod provides immunity

**Environment:**
- Campfire proximity bonus with speed multiplier, heal multiplier, and optional per-cycle cooldown reduction
- Freezing penalty (1.18.2+). Slower regen, smaller heals, longer cooldown while frozen in powder snow. Optional hard block

**HUD:**
- Custom heart HUD on **every supported version**. Cooldown fill, heal flash, regen pulse, hunger-blocked droop, poison/wither tint, saturation glow
- **Modern versions (1.20.1+)** additionally get the fancy pass. Breathing multi-layer under-glows, poison bubbles and ooze drips, wither cracks and debris, campfire rays and embers, saturation glint sweep, freezing frost icing and ice-shard heal burst, strict priority chain
- HUD position presets, opacity, scale, and fade settings configurable in-game

**Server / client split:**
- All regen logic runs server-side
- The HUD is client-side. Install on both sides for the visual. Server-only installs still regen normally

---

## Addon

**[Passive Regen Bandages](https://modrinth.com/mod/passive-regen-bandages)** adds craftable bandage items that instantly clear your regen cooldown and boost healing speed. Works best alongside this mod but can be used standalone.

---

## Config

Quick reference: 20 ticks = 1 second. 1 HP = half a heart. Default health bar is 20 HP (10 hearts).

Server config lives at `config/passive-health-regen.json`. HUD config lives at `config/passive-health-regen-hud.json` on the client.

### Core

| Option | Default | Description |
|---|---|---|
| `enabled` | `true` | Master toggle. Set to false to disable the mod without uninstalling. |
| `damageCooldownTicks` | `100` | Ticks without damage before healing starts. 100 = 5 seconds. |
| `minimumHungerPercent` | `50` | Minimum hunger bar fill required to heal. 0 = any hunger, 50 = half, 100 = full. |
| `minimumSaturationLevel` | `0.0` | Minimum saturation required to regen normally. 0.0 = disabled. |
| `healAmountPerTrigger` | `0.5` | HP healed per trigger. 0.5 = quarter heart, 1.0 = half heart, 2.0 = full heart. |
| `baseHealIntervalTicks` | `100` | Ticks between each heal at base rate. |
| `updateIntervalTicks` | `20` | How often the mod checks players. |

### Ramp-up

| Option | Default | Description |
|---|---|---|
| `rampUpEnabled` | `false` | Enables ramp-up mode. |
| `fullStrengthHealIntervalTicks` | `50` | Ticks between heals at full ramp-up speed. |
| `rampFullStrengthTicks` | `600` | Ticks out of combat before full ramp-up speed. |

### Health scaling

| Option | Default | Description |
|---|---|---|
| `scaleWithMaxHealth` | `false` | Enables heal rate scaling based on max HP above 20. |
| `maxHealthScalingExponent` | `0.5` | 0.5 = square root, gentle curve. 1.0 = linear. |
| `maxHealthScalingCap` | `2.0` | Maximum multiplier from health scaling. |

### Conditions

| Option | Default | Description |
|---|---|---|
| `maxRegenHealthPercent` | `100` | Regen stops at this percent of max HP. |
| `blockedEffects` | `[]` | Effect IDs that pause regen. |
| `dimensionBlacklist` | `[]` | Dimensions where regen is disabled. |
| `pvpDamageCooldownTicks` | `-1` | Separate cooldown after PvP damage. -1 = same as regular. |
| `disableNaturalRegen` | `false` | Drains food exhaustion to suppress vanilla natural regen. |
| `regenWhileSprinting` | `true` | If false, regen is paused while sprinting. |

### Hunger bonus

| Option | Default | Description |
|---|---|---|
| `hungerBonusEnabled` | `false` | Enables the hunger bonus. |
| `hungerBonusThresholdPercent` | `75` | Hunger required for the bonus. |
| `hungerBonusHealMultiplier` | `1.5` | Heal amount multiplier. |
| `hungerBonusSpeedMultiplier` | `1.5` | Heal speed multiplier. |
| `hungerBonusCooldownReduction` | `25` | Percent cooldown reduction. |
| `hungerFullBonusEnabled` | `false` | Extra tier at completely full hunger (20/20). |
| `hungerFullBonusHealMultiplier` | `2.0` | Full-hunger heal multiplier. |
| `hungerFullBonusSpeedMultiplier` | `2.0` | Full-hunger speed multiplier. |

### Hunger penalty

| Option | Default | Description |
|---|---|---|
| `hungerPenaltyEnabled` | `false` | Regen continues below the food threshold at reduced rate instead of stopping. |
| `hungerPenaltySpeedMultiplier` | `0.25` | Speed multiplier when penalized. |
| `hungerPenaltyHealMultiplier` | `1.0` | Heal amount multiplier when penalized. |

### Saturation bonus

| Option | Default | Description |
|---|---|---|
| `saturationBonusEnabled` | `true` | Enables the saturation bonus. |
| `saturationBonusThreshold` | `10.0` | Saturation required to activate. |
| `saturationBonusDeactivateThreshold` | `10.0` | Drop-out saturation (hysteresis). Equal to threshold = off. |
| `saturationBonusSpeedMultiplier` | `2.0` | Speed multiplier when active. |
| `saturationBonusHealMultiplier` | `2.0` | Heal amount multiplier when active. |
| `saturationBonusCostPerHp` | `1.0` | Saturation consumed per HP healed. Vanilla uses 1.5. |
| `saturationBonusIdleDrainPerTick` | `0.0` | Optional per-tick wick drain while active. 0 = vanilla model. |
| `saturationBonusMinSaturationFloor` | `0.0` | Drain will not push saturation below this. |
| `saturationBonusFlatHealBonus` | `0.25` | Flat HP added per heal tick on top of multipliers. |
| `saturationBonusScaleByExcess` | `false` | When true, bonus scales linearly with saturation above threshold. |

### Hunger drain (1.3.1+)

Optional. Off by default. Drains hunger as you heal the same way the saturation bonus drains saturation. Flip on if you want eating to actually matter for passive regen.

| Option | Default | Description |
|---|---|---|
| `hungerDrainEnabled` | `false` | Master toggle. |
| `hungerDrainSpeedMultiplier` | `1.0` | Scalar on overall drain rate. |
| `hungerDrainCostPerHp` | `0.6` | Hunger drained per HP healed. |
| `hungerDrainIdleDrainPerTick` | `0.0` | Passive drain while at full HP. |
| `hungerDrainMinFloor` | `0.0` | Drain stops at this hunger value. |

### Healing gates

| Option | Default | Description |
|---|---|---|
| `disableHealingDuringPoison` | `true` | Blocks regen while poisoned. Visuals always play regardless. |
| `disableHealingDuringWither` | `true` | Blocks regen under wither. Visuals always play regardless. |

### Bonus stacking

| Option | Default | Description |
|---|---|---|
| `bonusStackingMode` | `MULTIPLICATIVE` | `MULTIPLICATIVE`, `ADDITIVE`, or `STRONGEST_ONLY`. |

### Regen on kill

| Option | Default | Description |
|---|---|---|
| `regenOnKillEnabled` | `false` | Enables cooldown reduction on kill. |
| `regenOnKillCooldownReduction` | `50` | Percent of remaining cooldown removed on kill. |
| `regenOnKillHostileOnly` | `false` | Only hostile mobs grant the reduction. |
| `regenOnKillBlacklist` | `[]` | Entity IDs that never grant the bonus. |
| `regenOnKillComboEnabled` | `false` | Enables kill combo stacking. |
| `regenOnKillComboWindowTicks` | `200` | Window for consecutive kills to count as combo. |
| `regenOnKillComboMaxStacks` | `5` | Max combo stacks. |
| `regenOnKillComboReductionPerStack` | `10` | Extra percent per stack on top of base. |

### Bonus modifiers

Crouch, light level, day/night, difficulty. All off by default, all stack per `bonusStackingMode`.

| Option | Default | Description |
|---|---|---|
| `crouchBonusEnabled` | `false` | Bonus while sneaking. |
| `crouchSpeedMultiplier` | `1.5` | Speed multiplier while sneaking. |
| `crouchHealMultiplier` | `1.0` | Heal multiplier while sneaking. |
| `lightLevelBonusEnabled` | `false` | Scale regen with light level. |
| `lightLevelMinMultiplier` | `0.75` | Multiplier at light 0. |
| `lightLevelMaxMultiplier` | `1.25` | Multiplier at light 15. |
| `dayNightMultiplierEnabled` | `false` | Scale regen with time of day. |
| `dayMultiplier` | `1.25` | Daytime multiplier. |
| `nightMultiplier` | `0.75` | Nighttime multiplier. |
| `difficultyScalingEnabled` | `false` | Scale regen with world difficulty. |
| `peacefulMultiplier` | `2.0` | Peaceful. |
| `easyMultiplier` | `1.25` | Easy. |
| `normalMultiplier` | `1.0` | Normal. |
| `hardMultiplier` | `0.75` | Hard. |

### Large damage penalty

| Option | Default | Description |
|---|---|---|
| `largeDamagePenaltyEnabled` | `false` | Enables the large damage penalty. |
| `largeDamageThresholdPercent` | `50` | Percent of max HP a single hit must deal to count. |
| `largeDamageCooldownMultiplier` | `1.5` | Cooldown multiplier after a large hit. |

### Campfire

Sit near a lit campfire (soul campfires count) for faster, stronger heals. Routed through `reduceCooldown` so addon cooldown sources stack honestly.

| Option | Default | Description |
|---|---|---|
| `campfireRegenEnabled` | `true` | Enables campfire bonuses. |
| `campfireRadius` | `8` | Block radius to check. |
| `campfireSpeedMultiplier` | `2.0` | Speed multiplier near a campfire. |
| `campfireHealMultiplier` | `1.0` | Heal amount multiplier near a campfire. |
| `campfireCooldownReductionEnabled` | `false` | One-shot cooldown reduction when first near a campfire after damage. |
| `campfireCooldownReductionPercent` | `20` | Percent of remaining cooldown removed. |

### Freezing penalty (1.18.2+)

Vanilla freezing didn't exist before 1.17, so this section isn't present on 1.12.2 or 1.16.5.

| Option | Default | Description |
|---|---|---|
| `freezingPenaltyEnabled` | `true` | Enables the freezing penalty. |
| `freezingPenaltyThresholdPercent` | `0.0` | Fraction of freeze ticks before penalty kicks in. |
| `freezingSpeedMultiplier` | `0.5` | Speed multiplier while frozen past threshold. |
| `freezingHealMultiplier` | `0.75` | Heal amount multiplier while frozen past threshold. |
| `freezingCooldownMultiplier` | `1.75` | Cooldown multiplier while frozen. |
| `freezingBlocksRegen` | `false` | Hard block regen while frozen. Overrides the multipliers. |

---

## Developer API

Addon mods can interact with the regen system via the public API.

```java
// Clears the damage cooldown so regen starts on the next tick
PassiveRegenAPI.clearDamageCooldown(player.getUUID());

// Reduces the remaining cooldown by a percentage. Use this path so your
// addon stacks honestly with campfire and other reduction sources.
PassiveRegenAPI.reduceCooldown(player.getUUID(), 50);

// Temporary speed multiplier. 1.5 = 50% faster. durationTicks = length.
PassiveRegenAPI.applyRegenBoost(player.getUUID(), multiplier, durationTicks);

// Queries
PassiveRegenAPI.isRegenReady(player.getUUID());              // cooldown elapsed?
PassiveRegenAPI.isHungerBlocked(player.getUUID());           // below hunger threshold?
PassiveRegenAPI.getRemainingCooldownTicks(player.getUUID()); // ticks remaining
PassiveRegenAPI.getCurrentHealRate(player.getUUID());        // effective HP per update

// Penalties / blocks
PassiveRegenAPI.applyRegenPenalty(player.getUUID(), multiplier, durationTicks);
PassiveRegenAPI.blockRegen(player.getUUID(), durationTicks);

// Bypass hunger/saturation gates for a duration
PassiveRegenAPI.overrideHungerRestrictions(player.getUUID(), durationTicks);

// Availability check for optional dependencies
if (PassiveRegenAPI.isAvailable()) { ... }
```

Declare as an optional dependency in your `fabric.mod.json` or `mods.toml`.

For resource pack authors and renderer-level mods, see [MODDING.md](MODDING.md). Key points: the base heart is a PNG sprite sheet (packs can restyle it), but every effect overlay (poison bubbles, wither cracks, frost icing, campfire rays, gold motes) is code-drawn via pixel rectangles and cannot be swapped via PNG. The reserved filenames under `assets/passiveregen/textures/gui/reserved/` are forward-compatibility slots, not a drop-in customization API.

---

## Versions

All twelve builds ship with full gameplay and the heart HUD. Modern versions (1.20.1+) get the fancy particle HUD pass.

| Version | Gameplay | HUD |
|---|---|---|
| 1.12.2 Forge | Full* | Basic |
| 1.16.5 Fabric | Full* | Basic |
| 1.16.5 Forge | Full* | Basic |
| 1.18.2 Fabric | Full | Basic |
| 1.18.2 Forge | Full | Basic |
| 1.20.1 Fabric | Full | Fancy |
| 1.20.1 Forge | Full | Fancy |
| 1.20.1 NeoForge | Full | Fancy |
| 1.20.4 NeoForge | Full | Fancy |
| 1.21.1 Fabric | Full | Fancy |
| 1.21.1 Forge | Full | Fancy |
| 1.21.1 NeoForge | Full | Fancy |

\* No freezing penalty. The mechanic does not exist on those Minecraft versions.

---

## Issues

Please open an issue with your version, loader, and config. Cross-version releases always surface edge cases, and a clean report makes fixes land fast.
