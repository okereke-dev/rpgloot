# Changelog

## [0.8.3] — 2026-07-13

### Fixed
- **Only one piece of a full armor set was actually granting armor.** Every armor piece's `GENERIC_ARMOR` `AttributeModifier` used the exact same fixed UUID (`rpgloot_armor`) regardless of which piece it was. Minecraft's attribute system only keeps one active modifier per UUID — when a player wore a full 4-piece set, all four pieces collided on that identical UUID and only one of them actually contributed to the player's armor value, instead of all four summing together. A "Legendary" full iron set could show barely more armor than a single piece, let alone more than plain vanilla iron. Same collision affected weapon damage/speed (harmless in practice, since only one weapon is ever held) and the HEALTH_BOOST/SPEED_BOOST/LUCK_BOOST bonus stats (real impact: two pieces rolling the same bonus stat would silently drop one). Modifier UUIDs are now keyed per-material, so every equipped piece contributes independently. Same caveat as v0.8.2 — only affects newly generated items, not gear already in inventories.

## [0.8.2] — 2026-07-13

### Fixed
- **RPGLoot weapons and armor were far weaker than plain vanilla gear.** `ItemRarityService` computed each item's attack damage/speed/armor bonus as *only the increment over vanilla* (e.g. `+0.3 armor` for an Uncommon iron piece) and applied that as the item's sole custom `AttributeModifier`. Minecraft replaces — not stacks — an item's intrinsic material attribute with any custom modifier you add, so the item ended up granting just that tiny increment instead of vanilla-plus-bonus (an Uncommon Iron Chestplate was granting ~0.3 armor instead of ~6.3). This affected every weapon and armor piece ever generated, at every rarity, via both normal drops and Artifacts. Newly generated items (from a fresh kill, chest, or `/rpgloot get`) now correctly carry the full vanilla-plus-bonus value. **Items already in player inventories (or already worn by mobs) keep their old, too-weak values baked in** — only re-rolling/re-obtaining the item fixes it, there's no automatic migration for existing items.

## [0.8.1] — 2026-07-13

### Added
- **`config.yml` now auto-merges new keys on startup/`/rpgloot reload`** instead of only writing a default file when it's entirely missing. Any key added by a newer plugin version is filled in automatically without touching anything you've already customized. (New `ConfigMerge` utility, uses Bukkit's own `setDefaults`/`copyDefaults` mechanism.) Trade-off: the first merge strips the file's comments (values are all preserved exactly, comments are not — a `YamlConfiguration` limitation).

## [0.8.0] — 2026-07-13

### Changed
- **Loot visibility pass**, based on tester feedback that loot felt too rare: `drop-chance` raised `0.08` → `0.20` and `structure-loot.inject-chance` raised `0.25` → `0.45`. Rarity weights and the RPGMood level-floor system are untouched — this only makes *something* appear more often, not makes good items easier to get.

### Added
- **Mobs can now spawn already wielding/wearing a real RPGLoot item** (`mob-equip`, 15% chance per eligible hostile non-boss mob) — visible loot signal before a kill even happens, separate from and additive to the on-death drop-chance above. Reuses `LootListener`'s existing tier/pool/rarity-floor logic as-is, so this doesn't add a second rarity system to balance. If the mob dies, the item it's wearing is what drops (its equipment slot's drop chance is set to 100%).

## [0.7.1] — 2026-07-12

### Changed
- Java package renamed `com.ricardo.*` → `com.okereke.*`. No functional change — the cross-plugin `PersistentDataContainer` keys (`rpgmood:*`, `rpgloot:*`) are plugin-name-based, not package-based, so RPGMood's soft-integration is unaffected.

## [0.7.0] — 2026-07-12

### Added
- **RPGMood achievements integration** — exposes two new soft-integration hooks for other plugins (namely RPGMood) to read via `PersistentDataContainer`, no dependency either way:
  - `rpgloot:active_set_rarity` — written on the *player* while a full 5-piece set bonus is active (value is the set's rarity), cleared when it stops being full (`SetTracker.recalculate()`)
  - Distinct Artifacts found are now tracked per-player (`PlayerStats.artifactsFound`, persisted to `playerstats.yml` as `artifacts-found`), recorded in `LootListener` alongside the existing Legendary-found tracking

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
