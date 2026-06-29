# RPGLoot

Paper plugin for Minecraft 1.21.4 that adds a rarity system to weapons obtained from hostile mob drops.

## Rarities

| Rarity | Color | Bonus Stats |
|---|---|---|
| Common | Gray | None |
| Uncommon | Yellow | 1 low stat |
| Rare | Purple | 1 decent stat |
| Hero | Green | 1 strong stat |
| Legendary | Orange | 2 strong stats |

Each weapon keeps vanilla Minecraft stats as a base. Higher rarities apply a random multiplier within their range to attack damage and attack speed.

## Bonus Stats (weapons)

- **Lifesteal** — heals a percentage of damage dealt
- **Crit Chance** — chance to deal a critical hit (x1.5 damage)
- **Knockback** — extra knockback on hit
- **Sweep Damage** — bonus damage on sweep attacks

## How to Obtain

Rarity weapons are only obtainable as drops from hostile mobs. Drop chance is configurable in `config.yml` (default 8%).

## Commands

| Command | Description |
|---|---|
| `/rpgloot get [rarity] [material]` | Generate a weapon with the given rarity and material |
| `/rpgloot getall` | Generate one weapon per rarity |

**Valid rarities:** `common`, `uncommon`, `rare`, `hero`, `legendary`

**Required permission:** `rpgloot.admin`

## Configuration

```yaml
# config.yml
drop-chance: 0.08  # 0.0 to 1.0
```

## Installation

1. Download `rpgloot.jar` from [Releases](../../releases)
2. Copy it to your Paper 1.21.4 server's `plugins/` folder
3. Restart the server

## Building from source

Requires Java 21 and Maven.

```bash
mvn clean package
```

Output jar: `target/rpgloot.jar`
