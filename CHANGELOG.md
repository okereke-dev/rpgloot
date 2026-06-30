# Changelog

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
