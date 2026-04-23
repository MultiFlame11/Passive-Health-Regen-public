# Passive Health Regen — Modding and Asset Reference

This document describes what's actually moddable, what isn't (yet), and which reserved names are safe to build against for forward compatibility.

A quick mental model of how the HUD renders before you read on:

1. The **base heart** is blitted from a PNG sprite sheet (outline, silhouette mask, fill).
2. Every **effect overlay** (poison bubbles, wither cracks, campfire rays, frost icing, gold motes, etc.) is drawn in code via pixel rectangles (`guiGraphics.fill(...)`). There is no sprite sheet for these.

That means resource packs can restyle the base heart freely, but effect-particle art is baked into the renderer. Theming those requires a mixin or a PR, not a PNG swap.

---

## Base heart sprite sheet (`textures/gui/regen_heart.png`)

The active heart is **16×64**. Three 16×16 rows are in use, one is reserved:

| Y  | Purpose                | Used by                                             |
|----|------------------------|-----------------------------------------------------|
|  0 | Outline sprite         | `drawHeart` outline pass, `drawFreezingHighlight`   |
| 16 | Silhouette / glow mask | `drawGlow` (all colored under-glows, regen pulse)   |
| 32 | Fill sprite            | Health fill and `drawHealFlash`                     |
| 48 | **Reserved**           | Unused in 1.3.0. No current render path samples it. |

Resource packs restyling the heart should preserve this layout. The y=48 row is intentionally unpainted so a future render pass can introduce an overlay without forcing packs to resize. Treat that row as reserved space, with the caveat that the semantic meaning may be pinned in a later release and packs that pre-ship content for it might need a follow-up upload.

---

## Effect overlays are code-drawn

Heads up for pack authors: the following effect visuals are not assets. They are drawn by `RegenHudRenderer` using `guiGraphics.fill(...)` pixel rectangles:

- Poison: breathing splotches on the heart, rising bubble particles (1px and 2×2), ooze drips
- Wither: crack spines on the heart, chip pixels, falling debris
- Freezing: frost icing band on the heart, falling snow particles, ice-shard heal burst
- Campfire: rotating warmth rays, rising ember particles
- Saturation: glint sweep across the heart, gold sparkle motes
- Critical / hunger: shake, wobble, droop tilt (transform, not pixels)
- Full-heal sparkle: radial pixel lines and dots

None of this is loaded from a texture file. A resource pack cannot recolor or reshape these by dropping assets into the jar. Changing them cleanly requires a mixin into the relevant `drawX` / `updateAndDrawX` method. A public registration API for custom HUD states is on the table but not in 1.3.0.

What packs **can** theme via PNG right now:
- The base heart sprite sheet (`regen_heart.png`)
- Any reserved heart variant (see next section), if a future version wires it up

---

## Reserved effect heart filenames (`textures/gui/reserved/`)

These placeholder files ship in the jar under canonical filenames. They are **not loaded by any render path in 1.3.0**. They exist so that when a future version adds matching state logic, packs that already customized the filename work without a follow-up upload.

| Filename                       | Planned state trigger                                          |
|--------------------------------|----------------------------------------------------------------|
| `regen_heart_radiation.png`    | Mekanism / Scorched-Earth / radiation effects                  |
| `regen_heart_burning.png`      | On-fire / burning status (vanilla fire, lava, hot floor)       |
| `regen_heart_cursed.png`       | Cursed / hex effects from magic mods (Iron's, Bewitchment)     |
| `regen_heart_blessed.png`      | Buffed / blessed / holy effects                                |
| `regen_heart_bleeding.png`     | Bleeding / wound DoT effects (First Aid, Epic Fight)           |
| `regen_heart_thirsty.png`      | Thirst / dehydration effects (Dehydration, Thirst Was Taken)   |

Each reserved sprite follows the same 16×64 layout as the main heart.

### Realistic expectation
When these get wired up, the most likely implementation is **base heart swap plus a tint**, not a fresh code-drawn effect overlay. So a pack customizing a reserved PNG will control the base look of that effect state, but the particle-layer visuals (if any are added) will still live in code.

### Adding your own reserved slot
If you want to pre-stage art for an effect not on this list, put it alongside the existing files using the same naming convention (`regen_heart_<effect>.png`) and open an issue or PR so the name can be reserved in a future version. Name collisions in `reserved/` are treated as stable once documented here.

---

## Effect priority chain (1.3.0)

The HUD paints exactly one highest-priority state at a time. Current order, highest first:

```
wither > poison > freezing > hunger-blocked > critical > campfire > saturation
```

Future reserved effects will slot into this chain. Rough design intent:

| State          | Proposed tier                                            |
|----------------|----------------------------------------------------------|
| Radiation      | Above wither. Strongest DoT, overrides everything.       |
| Burning        | Between poison and freezing. Symmetric with poison.      |
| Bleeding       | Between poison and freezing.                             |
| Cursed         | Between hunger-blocked and critical.                     |
| Thirsty        | Between hunger-blocked and critical.                     |
| Blessed        | Below saturation. Weakest, purely additive glow.         |

Nothing is final until wired. This table is design intent, not a contract.

---

## Under-glow palette reference

Each state currently paints a halo behind the heart at roughly 1.15× to 1.40× scale via `drawGlow(...)`. When proposing a new state, pick a halo tint that doesn't collide with the existing palette:

| State           | Halo tint(s)                          |
|-----------------|---------------------------------------|
| Wither          | `#501050`, `#200820` (dual purple)    |
| Poison          | `#5FD43B` (green)                     |
| Freezing        | `#6FB8E8`, `#2A7AB4` (dual blue)      |
| Hunger blocked  | `#DC2828`, `#B41414` (dual red)       |
| Critical        | `#FF3232`, `#DC1414` (dual red)       |
| Campfire        | `#A02800` to `#FFF078` (4 amber)      |
| Saturation      | `#FFCC22` (gold)                      |
| Regen (default) | config tint                           |

Suggested (reserved, non-binding):
- Radiation: toxic yellow-green `#C8FF00` with dark green outer
- Burning: orange-red `#FF5A1A` with dim red outer
- Bleeding: crimson `#AA0818`, single layer
- Cursed: violet `#6A1A9A` with slow desaturated pulse
- Blessed: warm white-gold `#FFE8B4` with very slow breath
- Thirsty: dry ochre `#C89840` at low alpha

---

## Config field reservations

These fields are **not present** in 1.3.0's config but are reserved so a future version can introduce them without a migration step. Don't pre-write them into your configs. They'll be ignored today and defaults may shift before wiring:

Server-side (`passive-health-regen.json`):
- `disableHealingDuringRadiation` (bool)
- `disableHealingDuringBurning` (bool)
- `disableHealingDuringBleeding` (bool)
- `disableHealingDuringCursed` (bool)
- `disableHealingDuringThirst` (bool)
- `blessedHealMultiplier` (double)

Client-side (`passive-health-regen-hud.json`):
- `hudRadiationEffectEnabled` (bool)
- `hudBurningEffectEnabled` (bool)
- `hudBleedingEffectEnabled` (bool)
- `hudCursedEffectEnabled` (bool)
- `hudBlessedEffectEnabled` (bool)
- `hudThirstEffectEnabled` (bool)

---

## Sound events

No custom sound events ship in 1.3.0. When effect-specific sounds land, they'll be namespaced `passiveregen:heart.<effect>.<event>` (e.g. `passiveregen:heart.radiation.warn`). Packs shouldn't pre-ship ogg files for these today.

---

## Compatibility notes for addon mods

- **Bandages and other cooldown-reduction addons**: route cooldown reduction through the server's `reduceCooldown(...)` path (see `PassiveRegenHandler`). Keeps stacking with campfire and future reduction sources honest.
- **Status-effect immunity packs**: flip `disableHealingDuringPoison` and `disableHealingDuringWither` (and future equivalents) to `false` so the HUD still plays the effect aesthetic while healing continues normally.
- **Custom HUD states**: there is no registry API in 1.3.0. To push a fully custom state onto the heart, mixin into `RegenHudRenderer` after the existing priority chain. A public registration API is under consideration for a later release.
- **Custom particle visuals for existing states**: same answer. No PNG swap works for effect overlays. Mixin into the specific `drawX` / `updateAndDrawX` method for the state you're retheming.
