# Changelog

## [0.6.0] — 2026-07-05

### Added
- **RPGMood integration** — if RPGMood is installed, mobs it scales carry a `rpgmood:level` PersistentDataContainer tag; RPGLoot now reads that level and raises the minimum rarity floor for the drop based on configurable thresholds (`rpgmood-integration` in `config.yml`). No dependency between the two plugins, no effect if RPGMood isn't installed.

## [0.2.0] — 2026-06-30

### Added
- **Armor** — helmets, chestplates, leggings, boots now drop from hostile mobs (20% of all drops)
  - Rarity scales defense via flat ADD_NUMBER modifier — complementary to enchantments
  - Armor bonus stats: Thorns Chance, Dodge Chance, Damage Reduction, Health Boost, Speed Boost, Fall Reduction, Night Vision Chance
  - Health Boost and Speed Boost applied as passive attribute modifiers on the item
  - Night Vision Chance has a 10-second cooldown to prevent spam during bleed ticks
- **Tools** — pickaxe, shovel, hoe, fishing rod, woodcutter axe (AXE_TOOL) supported via `/rpgloot` command
  - Tool bonus stats: Fortune Boost, XP Boost, Auto-Smelt Chance, Replant Chance, Luck Boost, Double Catch Chance
  - Auto-Smelt converts ores and wood to smelted output on break
  - Replant auto-plants crops after breaking them (1 tick delayed)
  - Luck Boost applied as passive LUCK attribute modifier (improves vanilla fishing loot tables natively)
  - Double Catch fires on PlayerFishEvent to duplicate the catch
- **Mob-specific weapon pools** — mobs now drop weapons fitting their playstyle
  - Skeleton / Stray → Bow
  - Drowned → Trident
  - Pillager → Crossbow
  - Piglin / Brute / Zombified Piglin → Golden weapons / Crossbow
  - Wither Skeleton → Sword
  - Vindicator → Axe
  - All others → full weapon pool
- **Boss drops** — guaranteed RPGLoot items from bosses with configurable rarity range
  - Warden → Rare–Legendary (100%)
  - Wither → Rare–Legendary (100%)
  - Elder Guardian → Rare–Hero (80%)
  - Ender Dragon → Rare–Legendary (100%)
  - Elder Guardian caps at Diamond; all other bosses can yield Netherite
- **Structure loot** — vanilla structure chests have a 25% chance to contain an RPGLoot weapon (no Netherite)
  - Dungeon / Mineshaft → up to Uncommon
  - Desert Pyramid / Jungle Temple / Stronghold → up to Rare
  - Pillager Outpost / Ruined Portal / Woodland Mansion / Bastion Remnant → up to Hero
  - End City / Ancient City → up to Legendary
- **New config sections** — `structure-loot` (enabled + inject-chance) and `boss-drops` (per-boss chance + min-rarity + max-rarity)
- **AXE_TOOL** — woodcutter axe as a separate type from combat axe, distinguishable via PDC `item_category`
- `/rpgloot get/getall` — now supports all new types: helmet, chestplate, leggings, boots, pickaxe, shovel, hoe, fishing_rod, axe_tool
- **Mob material tier system (ceiling model)** — mobs drop weapons/armor made of materials up to their tier cap, never above
  - T1 Overworld basic mobs → up to Iron
  - T2 Overworld structure mobs (Pillager, Vindicator, Witch, Evoker, Ravager) → up to Golden
  - T3 Nether + End mobs → up to Diamond
  - Bosses only → up to Netherite
- **Weighted mob weapon pools** — 70% chance of mob's signature weapon type, 30% generic tier pool

### Changed
- Common rarity now grants a fixed +5% damage/defense bonus (was identical to vanilla stats); all rarities now show real stat lines in lore
- Armor drops reduced from 35% to 20% of total mob drops (weapons remain 80%)
- Structure loot inject chance reduced from 40% to 25%
- Netherite weapons are now boss-exclusive — no longer appear in regular mob drops or structure loot
- `rarity-weights` in config are now auto-normalized if they don't sum to 100 (warning logged)
- `RolledStat.deserialize()` now returns null on corrupted PDC data instead of crashing

## [0.1.1] — 2026-06-29

### Fixed
- Bleeding now has a proper proc chance (20–75% by rarity) instead of always triggering on every hit
- Bleeding damage per tick is now derived from weapon base damage × rarity factor, not from random hit damage — makes it predictable and weapon-tier consistent
- Bleed ticks no longer apply knockback, which was causing mobs to freeze/bounce in place
- Damage numbers no longer round small values to 0 — values under 10 show one decimal (e.g. `0.3`, `♥ +0.2`)
- Bleed re-hit on same target now correctly cancels the previous bleed before starting a new one
- Bleed ticks no longer re-trigger bonus stats (lifesteal, crit, etc.) on the attacker

### Added
- `damage-numbers` config option — set to `false` to disable floating damage numbers
- Damage numbers pop-in animation: numbers scale in quickly then rise, crits scale larger

## [0.1.0] — 2026-06-29

Initial release.

### Features
- 5 rarity tiers: Common, Uncommon, Rare, Hero, Legendary
- Supported weapons: swords, axes, tridents, maces, bows, crossbows
- Rarity scales attack damage and attack speed via flat ADD_NUMBER modifiers — enchantments unaffected
- Random RPG-style weapon names generated per weapon type
- Bonus stat system with unique pools per weapon type:
  - Sword / Axe: Lifesteal, Crit Chance, Knockback, Bleeding
  - Trident: Lifesteal, Crit Chance, Riptide Speed, Lightning Strike
  - Mace: Lifesteal, Knockback, Smash Radius, Fall Damage Bonus
  - Bow: Arrow Damage, Flame Chance, Arrow Punch, Multishot
  - Crossbow: Arrow Damage, Piercing, Multishot, Charge Speed
- Weapons obtainable only via hostile mob drops (no crafting)
- Floating damage numbers using TextDisplay with client-side interpolation
- Particle effects for crit, bleed ticks, lifesteal, and smash radius
- Admin command `/rpgloot get [rarity] [material]`, `getall [type]`, `stats`

### Configuration (`config.yml`)
- `drop-chance` — probability per kill (default 0.08)
- `rarity-weights` — percentage weights per tier, must sum to 100
- `stat-display` — `percentage` / `absolute` / `mixed`
