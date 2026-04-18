# Passive Health Regen

Small Forge `1.12.2` utility mod that adds configurable passive out-of-combat regeneration without using a potion effect.

## Default behavior

- Players only
- Server-side healing only
- Starts after `5` seconds (`100` ticks) without taking damage
- Base healing rate is half of vanilla Regeneration I on average
- Optional ramp-up toward full Regeneration I exists, but is disabled by default

## Default config

- `enabled=true`
- `rampUpEnabled=false`
- `damageCooldownTicks=100`
- `minimumHungerPercent=50`
- `updateIntervalTicks=20`
- `baseHealIntervalTicks=100`
- `fullStrengthHealIntervalTicks=50`
- `rampFullStrengthTicks=600`
- `scaleWithMaxHealth=false`
- `maxHealthScalingExponent=0.5`
- `maxHealthScalingCap=2.0`
