# RPGLoot

A Paper plugin for Minecraft 1.21.4 that adds an RPG-style rarity system to weapons. Hostile mobs have a configurable chance to drop weapons with randomized names, scaled stats, and unique bonus effects per weapon type.

---

## Table of contents

- [Features](#features)
- [Installation](#installation)
- [Rarities](#rarities)
- [Weapons](#weapons)
  - [Swords](#swords)
  - [Axes](#axes)
  - [Trident](#trident)
  - [Mace](#mace)
  - [Bow](#bow)
  - [Crossbow](#crossbow)
- [Bonus stats](#bonus-stats)
- [Drop system](#drop-system)
- [Commands](#commands)
- [Configuration](#configuration)
- [Building from source](#building-from-source)

---

## Features

- 5 rarity tiers with distinct colors and scaled stats
- Random RPG-style weapon names per weapon type (e.g. *Shadow Blade*, *Tidal Piercer*)
- Vanilla stats preserved — rarity bonuses stack independently from enchantments
- Unique bonus stat pools per weapon type (melee, trident, mace, bow, crossbow)
- Configurable drop chance and rarity weights
- Configurable stat display format (percentage, absolute, or mixed)
- Admin commands with tab-complete for testing

---

## Installation

1. Download `rpgloot.jar` from [Releases](../../releases)
2. Place it in your server's `plugins/` folder
3. Restart the server
4. Edit `plugins/RPGLoot/config.yml` to adjust drop rates and display options

**Requirements:** Paper 1.21.4 — does not support Spigot or CraftBukkit.

---

## Rarities

| Rarity | Color | Bonus stats | Drop weight |
|---|---|---|---|
| Common | Gray | None | 50% |
| Uncommon | Yellow | 1 (low) | 30% |
| Rare | Purple | 1 (decent) | 13% |
| Hero | Green | 1 (strong) | 5% |
| Legendary | Orange | 2 (strong) | 2% |

Each rarity applies a random multiplier within its range over the weapon's vanilla base stats. Enchantments (Sharpness, Power, etc.) are unaffected — they stack additively on top via `ADD_NUMBER`, with no amplification.

---

## Weapons

### Swords

Base damage varies by material tier. Attack speed is uniform across tiers (1.6 attacks/sec).

| Material | Vanilla dmg | Vanilla total |
|---|---|---|
| Wooden | 4 | 5 |
| Stone | 5 | 6 |
| Iron | 6 | 7 |
| Golden | 4 | 5 |
| Diamond | 7 | 8 |
| Netherite | 8 | 9 |

**Rarity scaling — Diamond Sword example:**

| Rarity | Dmg bonus | Dmg total | With Sharpness V |
|---|---|---|---|
| Common | +0% | 8 | 11 |
| Uncommon | +5–10% | 8.35–8.70 | 11.35–11.70 |
| Rare | +10–20% | 8.70–9.40 | 11.70–12.40 |
| Hero | +20–30% | 9.40–10.10 | 12.40–13.10 |
| Legendary | +30–40% | 10.10–10.80 | 13.10–13.80 |

**Bonus stat pool:** Lifesteal, Crit Chance, Knockback, Sweep Damage

---

### Axes

Axes deal more damage than swords but attack slower. Used as both a weapon and a tool — the rarity system only affects combat stats; chopping wood works normally.

| Material | Vanilla dmg | Atk speed |
|---|---|---|
| Wooden | 6 | 0.8/sec |
| Stone | 8 | 0.8/sec |
| Iron | 8 | 0.9/sec |
| Golden | 6 | 1.0/sec |
| Diamond | 8 | 1.0/sec |
| Netherite | 9 | 1.1/sec |

**Rarity scaling — Diamond Axe example:**

| Rarity | Dmg bonus | Dmg total | With Sharpness V |
|---|---|---|---|
| Common | +0% | 9 | 12 |
| Uncommon | +5–10% | 9.40–9.80 | 12.40–12.80 |
| Rare | +10–20% | 9.80–10.60 | 12.80–13.60 |
| Hero | +20–30% | 10.60–11.40 | 13.60–14.40 |
| Legendary | +30–40% | 11.40–12.20 | 14.40–15.20 |

**Bonus stat pool:** Lifesteal, Crit Chance, Knockback, Sweep Damage

---

### Trident

Melee weapon with the same attribute system as swords. Base: **8 dmg / 1.1 atk/sec**.

| Rarity | Dmg bonus | Dmg total |
|---|---|---|
| Common | +0% | 9 |
| Uncommon | +5–10% | 9.40–9.80 |
| Rare | +10–20% | 9.80–10.60 |
| Hero | +20–30% | 10.60–11.40 |
| Legendary | +30–40% | 11.40–12.20 |

**Bonus stat pool:**

| Stat | Range | Effect |
|---|---|---|
| Lifesteal | 1–12% | Heals a percentage of damage dealt |
| Crit Chance | 2–20% | Chance to deal 1.5× damage |
| Riptide Speed | 5–40% | Boosts player velocity when attacking in water or rain |
| Lightning Strike | 3–25% | Chance to strike lightning on the target (+4 dmg) |

---

### Mace

Heavy weapon with a slow attack speed. Excels at fall-damage combos. Base: **5 dmg / 0.6 atk/sec**.

| Rarity | Dmg bonus | Dmg total |
|---|---|---|
| Common | +0% | 6 |
| Uncommon | +5–10% | 6.25–6.50 |
| Rare | +10–20% | 6.50–7.00 |
| Hero | +20–30% | 7.00–7.50 |
| Legendary | +30–40% | 7.50–8.00 |

**Bonus stat pool:**

| Stat | Range | Effect |
|---|---|---|
| Lifesteal | 1–12% | Heals a percentage of damage dealt |
| Knockback | 1–4 pts | Extra knockback on hit |
| Smash Radius | 1–5 blk | Damages and knocks back nearby entities on hit |
| Fall Damage Bonus | 10–70% | Scales extra damage with fall distance before the hit |

---

### Bow

Projectile weapon — no `ATTACK_DAMAGE` attribute. Damage is applied to the arrow via `EntityShootBowEvent`. Rarity multipliers do not apply to base bow stats; all scaling comes from bonus stats.

**Bonus stat pool:**

| Stat | Range | Effect |
|---|---|---|
| Arrow Damage | 5–40% | Multiplies arrow damage on impact |
| Flame Chance | 5–40% | Chance to fire a flaming arrow |
| Arrow Punch | 1–4 pts | Adds knockback to the arrow |
| Multishot | 5–35% | Chance to fire 2 extra arrows with slight spread |

---

### Crossbow

Same projectile system as the bow. No `ATTACK_DAMAGE` attribute.

**Bonus stat pool:**

| Stat | Range | Effect |
|---|---|---|
| Arrow Damage | 5–40% | Multiplies arrow damage on impact |
| Piercing | 5–40% | Chance for the arrow to pierce through an extra entity |
| Multishot | 5–35% | Chance to fire 2 extra arrows |
| Charge Speed | 5–35% | Boosts arrow velocity to simulate a faster draw |

---

## Bonus stats

Bonus stat ranges by rarity tier (min–max):

| Stat | Uncommon | Rare | Hero | Legendary |
|---|---|---|---|---|
| Lifesteal | 1–3% | 3–6% | 6–10% | 8–12% |
| Crit Chance | 2–5% | 5–10% | 10–15% | 15–20% |
| Knockback | 1 pt | 1–2 pts | 2–3 pts | 3–4 pts |
| Sweep Damage | 5–10% | 10–20% | 20–30% | 30–40% |
| Riptide Speed | 5–10% | 10–20% | 20–30% | 30–40% |
| Lightning Strike | 3–6% | 6–12% | 12–18% | 18–25% |
| Smash Radius | 1–2 blk | 2–3 blk | 3–4 blk | 4–5 blk |
| Fall Dmg Bonus | 10–20% | 20–35% | 35–50% | 50–70% |
| Arrow Damage | 5–10% | 10–20% | 20–30% | 30–40% |
| Flame Chance | 5–10% | 10–20% | 20–30% | 30–40% |
| Arrow Punch | 1 pt | 1–2 pts | 2–3 pts | 3–4 pts |
| Multishot | 5–10% | 10–15% | 15–25% | 25–35% |
| Piercing | 5–10% | 10–20% | 20–30% | 30–40% |
| Charge Speed | 5–10% | 10–15% | 15–25% | 25–35% |

Values are rolled randomly within the range when the item is generated and stored permanently in the item's `PersistentDataContainer`.

---

## Drop system

Weapons with rarity only drop from **hostile mobs** killed by a **player**. Two rolls happen on each kill:

1. **Drop roll** — checked against `drop-chance` (default 8%)
2. **Rarity roll** — weighted random pick from the rarity table

The weapon type and material are chosen randomly from the full pool.

**Expected drop rates at default settings (8% drop-chance):**

| Rarity | Weight | Chance per kill |
|---|---|---|
| Common | 50% | 4.00% |
| Uncommon | 30% | 2.40% |
| Rare | 13% | 1.04% |
| Hero | 5% | 0.40% |
| Legendary | 2% | 0.16% |

On average, one Legendary drops every **~625 mob kills**.

---

## Commands

All commands require the `rpgloot.admin` permission.

| Command | Description |
|---|---|
| `/rpgloot get [rarity] [material]` | Gives a weapon with the specified rarity and material. Both arguments are optional — omitting them picks a random value. |
| `/rpgloot getall [type]` | Gives one weapon per rarity tier. Optionally filter by type: `sword`, `axe`, `trident`, `mace`, `bow`, `crossbow`. |
| `/rpgloot stats` | Shows the rarity and bonus stats of the item currently held in the main hand. |

**Valid rarities:** `common` `uncommon` `rare` `hero` `legendary`

**Valid materials (examples):** `diamond_sword` `netherite_axe` `trident` `mace` `bow` `crossbow`

All arguments support tab-complete.

---

## Configuration

Located at `plugins/RPGLoot/config.yml`. Generated automatically on first run.

```yaml
# Chance that a hostile mob drops a rarity weapon on death (0.0 - 1.0)
drop-chance: 0.08

# How stats are displayed in item lore
# Options:
#   percentage — "Attack Damage: +20%"           (default)
#   absolute   — "Attack Damage: +1.40"
#   mixed      — "Attack Damage: +1.40  (+20%)"
stat-display: percentage

# Drop chance per rarity (%) — must add up to 100
rarity-weights:
  common: 50
  uncommon: 30
  rare: 13
  hero: 5
  legendary: 2
```

If `rarity-weights` does not add up to 100, a warning is printed to the server console on startup.

---

## Building from source

**Requirements:** Java 21, Maven

```bash
git clone https://github.com/okereke-dev/rpgloot.git
cd rpgloot
mvn clean package
```

Output: `target/rpgloot.jar`
