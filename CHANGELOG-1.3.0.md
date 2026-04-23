# Passive Health Regen 1.3.0

A big parity and polish release.

Full feature parity across every supported version. A HUD polish pass on the modern builds. A stack of small fixes that turned up during the port work. If you've been waiting on 1.16.5 or 1.18.2 to catch up, this is the one.

---

## Highlights

- **Every version now shares the same regen logic.** Saturation bonus, hunger penalties, poison/wither gates, combo kill bonuses, crouch/light/day-night/difficulty modifiers, large-damage cooldown penalty, and reconnect cooldown persistence. All present across the lineup.
- **Campfire gameplay everywhere it makes sense.** Sit near a lit campfire, heal faster, and optionally burn down your damage cooldown. Stacks cleanly with the saturation bonus so resting at camp with food actually pays off.
- **Modern HUD polish pass** on 1.20.1 and 1.21.1. Tighter fade in/out. Cleaner pulse and heal-flash timing. Full visual states for saturation, campfire, hunger-blocked, poison, wither, freezing, and critical health. Everything slots into a priority chain so only one state ever paints at a time.
- **New reserved assets** for modders and resource packs. See below.

---

## What's new on the gameplay side

**Saturation bonus, rewritten:**
- Activates over a configurable saturation threshold, with proper **hysteresis** so it doesn't flicker on the edge
- Costs saturation **per HP actually healed** (vanilla style) instead of a fixed tick drain
- Optional flat HP heal bonus on top of the multipliers, so each tick heals a visible chunk instead of a thin sliver
- Optional "scale by excess" mode where bonus strength ramps with how far above threshold you are
- Configurable saturation floor so the bonus won't self-starve you
- New idle drain knob for anyone who preferred the old per-tick drain behavior

**Poison and Wither healing gates:**
- Healing is disabled during these effects by default (toggleable per effect). If your pack provides immunity via another mod, flip the toggle off and healing resumes.

**Regen on kill:**
- Optional hostile-only restriction, mob blacklist, and a combo stack system that rewards kill streaks without going infinite.

**Bonus stacking mode:**
- Pick additive or multiplicative. Additive keeps multiple bonuses flat and steady. Multiplicative lets max stacks feel noticeably stronger.

**Campfire logic (modern versions):**
- Speed multiplier, heal multiplier, and optional per-cycle cooldown reduction. All routed through the shared `reduceCooldown` path so addon mods (bandages etc.) stack honestly.

**Reconnect cooldown persistence:**
- Your damage cooldown survives disconnects and relogs via persistent player data. No more logging out to skip the wait.

**Expanded addon API:**
- `isRegenReady`, `isHungerBlocked`, `getRemainingCooldownTicks`, `getCurrentHealRate`, `applyRegenPenalty`, `blockRegen`, `overrideHungerRestrictions`. Enough surface for bandage-style addons to integrate properly.

---

## HUD (1.20.1 and 1.21.1)

Modern builds now have:

- **Priority chain**: wither, then poison, then freezing, then hunger-blocked, then critical, then campfire, then saturation. Only the highest-priority state paints.
- **Wither**: near-black heart with purple dual-layer under-glow. Drifting crack pattern. Debris particles falling off the bottom with gravity.
- **Poison**: sickly green heart with green under-glow. Breathing splotches on the surface. Rising bubble particles (mix of 1px and fat 2×2). Ooze drips running down from the base.
- **Freezing**: cool blue dual-layer under-glow that intensifies when you're taking freeze damage. On-heart frost icing. Falling snow particles. A custom ice-shard heal burst.
- **Campfire**: 4-layer amber halo, rotating warmth rays, ember particles drifting off the heart. Takes visual priority over saturation when both are active so the two halos don't stack on top of each other.
- **Saturation bonus**: breathing gold halo, periodic short glint sweep across the heart, gold sparkle motes, gold tinted heal flash.
- **Critical health**: red shake with dual red pulse.
- **Hunger-blocked**: droop tilt with wobble and red pulse.

All HUD states honor `hudOpacity`, `hudScale`, fade settings, and the HUD position presets.

---

## New defaults

Fresh configs ship with:
- Timer display: **off** (it was too busy for most players)
- Saturation bonus: **on**
- Campfire bonuses: **on** (where supported)
- Freezing penalty: **on** (where supported)
- Poison/Wither heal gates: **on**

Existing configs are left untouched. Defaults only affect new installs.

---

## Modder and resource pack goodies

New in 1.3.0: a set of **reserved placeholder heart filenames** ship under `assets/passiveregen/textures/gui/reserved/` for future effect states:

- `regen_heart_radiation.png`
- `regen_heart_burning.png`
- `regen_heart_cursed.png`
- `regen_heart_blessed.png`
- `regen_heart_bleeding.png`
- `regen_heart_thirsty.png`

**Important context on what these are and aren't:** the files currently duplicate the default heart and are **not loaded by any render path in 1.3.0**. They are forward-compatibility slots, not a drop-in customization API. Packs can pre-ship art under these filenames now and it will kick in when a future version wires up the matching state logic.

Also worth flagging for pack authors: every effect overlay you see on the heart (poison bubbles, wither cracks, frost icing, campfire rays, gold motes, ice-shard heal burst, etc.) is **code-drawn in the renderer via pixel rectangles**, not loaded from a PNG. Resource packs can restyle the base heart sprite sheet but cannot recolor or reshape effect particles by swapping assets. Custom particle visuals require a mixin into `RegenHudRenderer`.

Full spec (sprite sheet layout, what's actually moddable vs what isn't, reserved config field names, planned priority tiers, halo palette reference, sound event naming) is documented in `MODDING.md` at the repo root.

---

## Bug fixes from the port pass

Cross-version porting surfaced a handful of issues, all squashed:

- HUD priority leak where hunger-blocked droop could bleed into poison and wither states on 1.12.2
- Saturation glow correctly suppressed while hunger-blocked
- Full heal sparkle no longer accidentally suppressed by the saturation toggle
- Saturation bonus state now tracked in HUD sync equality. Packet no longer goes stale when the bonus flips mid-session
- Disconnect and reconnect properly clears stale HUD flags so effects don't briefly flash on reconnect before the first packet arrives
- Heart animation `t` now wraps via `% 3_600_000L` so time-based sinusoids don't lose precision as the Unix epoch grows
- Campfire amber glow no longer fully hides during the post-heal-flash window. It dims to 50% instead, avoiding the pink snap-back
- Campfire rays re-centered from (8,8) to (8,5) so they read as actually originating from the heart
- Freeze shake scoped to actual freeze damage (`frozenPct >= 1.0`), not merely "freezing," so the frost visuals stay readable
- Pink regen pulse no longer bleeds through saturation sheen during heal thumps

---

## Version support

All twelve builds compile and run. **Every version ships with the heart HUD.** The only difference is how fancy it gets.

### Basic HUD vs fancy HUD

Every version renders the custom heart HUD with the full set of core feedback states. Cooldown fill, heal flash, regen pulse, hunger-blocked droop, poison/wither tint, saturation glow. The same config tree, position presets, and HUD screen work everywhere.

What **1.20.1+ modern versions** additionally get is the **fancy pass**. The extra particle work and polish developed on the 1.20.1 Fabric branch and forward ported. That means:

- Breathing multi-layer under-glows (dual color halos behind the heart per state)
- Poison bubble particles, splotches, and ooze drips
- Wither crack overlay and falling debris particles
- Campfire ember particles and rotating warmth rays
- Saturation glint sweep, gold sparkle motes, breathing halo
- Freezing frost icing, falling snow, and ice-shard heal burst
- Strict priority chain so only one state paints at a time
- Tightened fade/pulse/thump timing

Older versions (1.12.2 through 1.18.2) show the same *state* feedback, just in a more basic form. You'll still see that you're poisoned or saturated or hunger-blocked. The heart just doesn't have the extra particle noise layered on top.

### What's the deal with freezing?

Vanilla Minecraft didn't have a freezing mechanic until **1.17**, when Caves & Cliffs added Powder Snow and freeze damage. Every version before that has no freeze system to hook into, so:

- **1.12.2 and 1.16.5**: no freezing penalty, no freezing HUD state. Not an omission. The mechanic doesn't exist on those versions to penalize or visualize.
- **1.18.2 and up**: freezing penalty is in the gameplay config.
- **1.20.1+ modern versions**: full freezing HUD treatment on top of the penalty (blue under-glow that pulses harder when damaging, frost icing on the heart, falling snow particles, ice-shard heal burst).

Nothing to configure on the older versions. The field just isn't there.

### Full matrix

| Version           | Gameplay | HUD         |
|-------------------|----------|-------------|
| 1.12.2 Forge      | ✅ Full* | ✅ Basic   |
| 1.16.5 Fabric     | ✅ Full* | ✅ Basic   |
| 1.16.5 Forge      | ✅ Full* | ✅ Basic   |
| 1.18.2 Fabric     | ✅ Full  | ✅ Basic   |
| 1.18.2 Forge      | ✅ Full  | ✅ Basic   |
| 1.20.1 Fabric     | ✅ Full  | 🌟 **Fancy** |
| 1.20.1 Forge      | ✅ Full  | 🌟 **Fancy** |
| 1.20.1 NeoForge   | ✅ Full  | 🌟 **Fancy** |
| 1.20.4 NeoForge   | ✅ Full  | 🌟 **Fancy** |
| 1.21.1 Fabric     | ✅ Full  | 🌟 **Fancy** |
| 1.21.1 Forge      | ✅ Full  | 🌟 **Fancy** |
| 1.21.1 NeoForge   | ✅ Full  | 🌟 **Fancy** |

\* Minus freezing. Not a feature of those Minecraft versions.

---

## Upgrading

- **Existing configs migrate cleanly.** No manual edits needed. Old saturation fields that were removed (`saturationBonusDrainPerTick`) are ignored silently by GSON.
- If you were running a pre-1.3.0 dev build with experimental values, a fresh config will get you the new defaults. You can delete the config and regenerate if you want the update defaults.

---

## Thanks

Thanks to everyone who helped playtest the saturation bonus tuning. Getting the feel right took a lot of iteration. Also, to the resource pack folks who've been asking about texture hooks: the `MODDING.md` spec and reserved filenames are for you.

If you hit a bug, please open an issue with the version and loader you're on and your config. Cross-version port releases always turn up edge cases I didn't catch, and a clean report makes it way easier to land a fix.

Enjoy the update.
