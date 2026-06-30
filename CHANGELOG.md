# Changelog

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
