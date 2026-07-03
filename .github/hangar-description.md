[![Version](https://img.shields.io/badge/version-0.3.1-brightgreen?style=flat-square)](https://github.com/okereke-dev/rpgloot/releases) [![Paper](https://img.shields.io/badge/Paper-1.20.4--26.1.2-blue?style=flat-square)](https://papermc.io) [![License](https://img.shields.io/badge/license-MIT-lightgrey?style=flat-square)](https://github.com/okereke-dev/rpgloot/blob/master/LICENSE)

# ⚔️ RPGLoot

> Transform your vanilla server into an RPG loot experience — without replacing the game your players already love.

Hostile mobs, bosses, and ancient structures now drop weapons, armor, and tools with **randomized rarity tiers**, **unique generated names**, and **hand-crafted bonus stats**. Everything stacks naturally with vanilla mechanics — enchantments, attributes, and loot tables all work exactly as expected. Drop it in, restart, done.

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
# Drop chances
drop-chance: 0.15
boss-drop-chance: 1.0

# Rarity weights (higher = more common)
rarity-weights:
  COMMON: 50
  UNCOMMON: 25
  RARE: 15
  HERO: 7
  LEGENDARY: 3

# Structure loot
structure-loot-chance: 0.25
```

Per-mob overrides, multiplier ranges per rarity tier, and boss minimum rarity are all configurable.

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
- 📦 [Modrinth](https://modrinth.com/plugin/rpgloot)
- 🔧 [SpigotMC](https://www.spigotmc.org/resources/rpgloot.136622/)
