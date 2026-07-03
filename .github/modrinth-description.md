[![Version](https://img.shields.io/badge/version-0.3.3-brightgreen?style=flat-square)](https://github.com/okereke-dev/rpgloot/releases) [![Paper](https://img.shields.io/badge/Paper-1.20.4--26.1.2-blue?style=flat-square)](https://papermc.io) [![License](https://img.shields.io/badge/license-MIT-lightgrey?style=flat-square)](https://github.com/okereke-dev/rpgloot/blob/master/LICENSE)

# ⚔️ RPGLoot

> Transform your vanilla server into an RPG loot experience — without replacing the game your players already love.

Hostile mobs, bosses, and ancient structures now drop weapons, armor, and tools with **randomized rarity tiers**, **unique generated names**, and **hand-crafted bonus stats**. Everything stacks naturally with vanilla mechanics — enchantments, attributes, and loot tables all work exactly as expected. Drop it in, restart, done.

---

## 🌱 Vanilla-First Design

RPGLoot is built to be a **complement**, not an overhaul. It was reviewed against other RPG-loot plugins specifically to avoid the common pitfalls — uncapped stat stacking, PvP-breaking power creep, and conflicts with other plugins' attributes.

- **Hard caps on every stackable stat** — Dodge (50%), Damage Reduction (60%), Fall Reduction (80%), Crit Chance (35%), Bleeding (60%). No combination of item + set bonuses can exceed these, ever.
- **`stats-enabled: false`** — a one-line config flip that turns RPGLoot into a **pure cosmetic rarity layer**. Items keep their generated name, color, and rarity tag but grant zero attribute bonuses, zero bonus stats, zero set bonuses. Vanilla combat stays completely untouched.
- **Fully tunable power level** — every rarity multiplier and bonus-stat range lives in `config.yml`, not hardcoded. Want a subtler experience? Turn the numbers down. Want more power? Turn them up. No recompiling required.
- **Real vanilla attributes, not fake NBT** — stat bonuses use Bukkit's native `AttributeModifier` system, the same one vanilla enchantments use, so RPGLoot plays nicely with other plugins instead of fighting them.

---

## 🌟 Rarity System

Every item rolls one of **5 rarity tiers**, each with its own color, name style, and stat multiplier range:

| Tier | Color | Stat Bonus | Drop Chance |
|---|---|---|---|
| ⬜ **Common** | Gray | Low | High |
| 🟢 **Uncommon** | Green | Moderate | Medium |
| 🔵 **Rare** | Blue | High | Low |
| 🟡 **Hero** | Gold | Very High | Very Low |
| 🔴 **Legendary** | Red | Maximum | Rare |

Each rarity scales damage, defense, and speed independently — and unlocks stronger bonus stat rolls.

---

## ✨ Bonus Stats

Items roll **1–3 bonus stats** from a curated pool per item type. No filler — every stat has a real in-game effect.

**⚔️ Weapons**

| Stat | Effect |
|---|---|
| Lifesteal | Heal a % of damage dealt |
| Crit Chance | Bonus critical strike probability |
| Bleeding | DoT damage over several seconds |
| Knockback Boost | Extra knockback on hit |
| Smash Radius | Mace: splash damage to nearby enemies |
| Lightning Strike | Random lightning bolt on target |
| Riptide Speed | Boost while in water or rain (Trident) |
| Fall Damage Bonus | Damage scales with fall distance |

**🛡️ Armor**

| Stat | Effect |
|---|---|
| Health Boost | Permanent max HP increase |
| Dodge Chance | % chance to completely avoid a hit |
| Damage Reduction | Flat % less damage taken |
| Speed Boost | Passive movement speed increase |
| Thorns Chance | Reflect damage on hit |
| Night Vision | Persistent night vision effect |
| Fall Reduction | Reduce fall damage taken |

**⛏️ Tools**

| Stat | Effect |
|---|---|
| Fortune Boost | Extra fortune levels |
| XP Boost | Bonus XP from blocks and fishing |
| Auto-Smelt | Chance to auto-smelt mined blocks |
| Replant Chance | Auto-replant harvested crops (Hoe) |
| Double Catch | Chance for double fish loot |
| Luck Boost | Increased fishing luck |

> Stacking-sensitive stats are hard-capped regardless of item + set combinations: **Crit Chance ≤ 35%** · **Bleeding ≤ 60%** · **Dodge ≤ 50%** · **Damage Reduction ≤ 60%** · **Fall Reduction ≤ 80%**.

---

## 💎 Set Bonus System

Every weapon and armor piece belongs to one of **8 named sets**. Collect matching pieces — same **set name**, **rarity**, and **material** — to unlock scaling bonuses.

**Piece scaling:**

| Pieces | Multiplier |
|---|---|
| 2 | 30% |
| 3 | 55% |
| 4 | 75% |
| 5 | 100% + Special Effect |

**Available sets:**

| Set | Bonus Stat | Legendary (5 pcs) |
|---|---|---|
| 🌑 **Shadowveil** | Dodge Chance | +11.0% |
| ⛓️ **Ironbound** | Damage Reduction | +11.0% |
| 🌅 **Dawnbreaker** | Crit Chance | +11.0% |
| 🌊 **Tidecaller** | Lifesteal | +5.0% |
| 🔥 **Emberclaw** | Bleeding Chance | +20% |
| ⚡ **Stormwarden** | Speed Boost | +10.0% |
| 🌀 **Voidwalker** | XP Boost | +25% |
| ✨ **Gilded** | Luck | +1.8 pts |

The active piece count is **highlighted live in the item lore** in the set's rarity color. Bonuses stack additively with individual item stats.

---

## 💀 Drop Sources

**Hostile Mobs**
Every hostile mob has a configurable drop chance. Material tier scales with the mob's difficulty zone — skeletons don't drop Netherite, Wither Skeletons might.

| Zone | Material Ceiling |
|---|---|
| Overworld (basic) | Iron |
| Overworld (structures) | Gold |
| Nether | Diamond |
| End / unlimited | Netherite |

**Boss Mobs**
Elder Guardian, Wither, Ender Dragon, and Warden guarantee a drop with a configurable **minimum rarity floor**.

**Vanilla Structures**
Strongholds, Bastions, End Cities, Jungle Temples, and more have a configurable chance to contain an RPGLoot weapon in their loot chests.

---

## 🎯 Combat Feedback

Every hit tells a story:

- **Floating damage numbers** — color-coded per type, rise above the target
- 🩸 Bleed ticks show separate red numbers
- 💚 Lifesteal pulses green healing numbers on the attacker
- 💥 Crits show enlarged gold numbers
- ⚡ Smash hits show splash damage on nearby targets
- Particle effects for crits, lifesteal, bleeds, and mace smash rings

---

## ⚙️ Configuration

Everything is tunable in `config.yml` — no recompile needed:

```yaml
# Pure cosmetic mode — no stat/attribute changes at all
stats-enabled: true

# Drop chance per hostile mob kill
drop-chance: 0.08

# Rarity weights (higher = more common) — auto-normalized to 100
rarity-weights:
  common: 50
  uncommon: 30
  rare: 13
  hero: 5
  legendary: 2

# Structure loot
structure-loot:
  enabled: true
  inject-chance: 0.25

# Rarity damage/speed multipliers — fully overridable
rarity-multipliers:
  LEGENDARY: { damage: [1.30, 1.40], speed: [1.10, 1.20] }

# Bonus stat value ranges — fully overridable, one entry per stat
bonus-stat-ranges:
  CRIT_CHANCE: { uncommon: [2,5], rare: [5,10], hero: [10,15], legendary: [15,20] }
```

Per-mob material tiers, boss minimum rarity, and every rarity/stat range shown above ship pre-populated with the tested defaults and can be freely retuned.

---

## 📦 Installation

```
1. Drop rpgloot.jar into your /plugins folder
2. Restart the server
3. Done — no setup required
```

**Requirements:** Paper 1.20.4 – 26.1.2 · Java 21 · No dependencies

---

## 🔗 Links

- 📖 [Full Wiki](https://github.com/okereke-dev/rpgloot/wiki)
- 💻 [Source Code](https://github.com/okereke-dev/rpgloot)
- 🐛 [Report a Bug](https://github.com/okereke-dev/rpgloot/issues)
- 🐹 [Hangar](https://hangar.papermc.io/okereke-dev/RPGLoot)
- 🔧 [SpigotMC](https://www.spigotmc.org/resources/rpgloot.136622/)
