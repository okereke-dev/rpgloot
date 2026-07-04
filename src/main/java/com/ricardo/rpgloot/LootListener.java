package com.ricardo.rpgloot;

import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

/**
 * Handles all item drops from mobs and bosses.
 *
 * Material tier (ceiling model — mobs drop up to, never above, their tier):
 *   T1 — Overworld basic       → up to Iron
 *   T2 — Overworld structures  → up to Golden
 *   T3 — Nether + End          → up to Diamond
 *   Bosses only                → up to Netherite
 *
 * Weapon type selection (weighted):
 *   70% — mob's signature weapon type (thematic)
 *   30% — any weapon from the tier pool (variety)
 */
public final class LootListener implements Listener {

    private final RPGLootPlugin plugin;
    private final ItemRarityService rarityService;
    private final RarityRoller roller;
    private final Random random = new Random();

    public LootListener(RPGLootPlugin plugin, ItemRarityService rarityService) {
        this.plugin = plugin;
        this.rarityService = rarityService;
        this.roller = new RarityRoller(plugin.getConfig(), plugin.getLogger());
    }

    public void reload() {
        roller.reload(plugin.getConfig());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (isBoss(entity)) {
            handleBossDrop(event, entity);
            return;
        }

        if (!(entity instanceof Monster)) return;
        if (!(entity.getKiller() instanceof Player killer)) return;
        if (!killer.hasPermission("rpgloot.drops")) return;
        if (!plugin.isDropsAllowed(entity.getLocation())) return;

        double dropChance = plugin.getConfig().getDouble("drop-chance", 0.08);
        if (random.nextDouble() > dropChance) return;

        int tier = getMobTier(entity);

        ItemStack item;
        if (random.nextDouble() < 0.80) {
            // 80%: weapon — mob's signature type (70%) or generic tier pool (30%)
            List<org.bukkit.Material> pool = getWeaponPool(entity, tier);
            item = new ItemStack(pool.get(random.nextInt(pool.size())));
        } else {
            // 20%: armor piece from tier pool
            List<org.bukkit.Material> pool = getArmorPool(tier);
            item = new ItemStack(pool.get(random.nextInt(pool.size())));
        }

        rarityService.applyRarity(item, roller.roll());
        event.getDrops().add(item);
    }

    // ── Tier detection ────────────────────────────────────────────────────

    private int getMobTier(LivingEntity entity) {
        World.Environment env = entity.getWorld().getEnvironment();
        // Nether and End both cap at Diamond — Netherite is boss-exclusive
        if (env == World.Environment.NETHER || env == World.Environment.THE_END) return 3;
        if (isStructureMob(entity)) return 2;
        return 1;
    }

    private boolean isStructureMob(LivingEntity entity) {
        return entity instanceof Pillager
                || entity instanceof Vindicator
                || entity instanceof Evoker
                || entity instanceof Witch
                || entity instanceof Ravager
                || entity instanceof Illusioner;
    }

    // ── Pool selection ────────────────────────────────────────────────────

    private List<org.bukkit.Material> getWeaponPool(LivingEntity entity, int tier) {
        List<org.bukkit.Material> genericPool = getGenericWeaponPool(tier);
        List<org.bukkit.Material> specificPool = getMobSpecificPool(entity, tier);

        if (specificPool == null) return genericPool;

        // 70% signature weapon type, 30% generic tier pool for variety
        return random.nextDouble() < 0.70 ? specificPool : genericPool;
    }

    private List<org.bukkit.Material> getMobSpecificPool(LivingEntity entity, int tier) {
        if (entity instanceof Skeleton || entity instanceof Stray) return ItemPools.RANGED;
        if (entity instanceof Drowned)   return ItemPools.TRIDENTS;
        if (entity instanceof Pillager)  return ItemPools.CROSSBOWS;
        // Piglins are thematically gold-locked — no variety roll for them
        if (entity instanceof Piglin || entity instanceof PiglinBrute
                || entity instanceof PigZombie)                    return ItemPools.PIGLIN;
        if (entity instanceof WitherSkeleton) return ItemPools.SWORDS_T3; // Nether → diamond ceiling
        if (entity instanceof Vindicator)     return ItemPools.AXES_T2;   // Structure → golden ceiling
        return null; // generic mobs use tier pool only
    }

    private List<org.bukkit.Material> getGenericWeaponPool(int tier) {
        return switch (tier) {
            case 3  -> ItemPools.WEAPONS_T3;
            case 2  -> ItemPools.WEAPONS_T2;
            default -> ItemPools.WEAPONS_T1;
        };
    }

    private List<org.bukkit.Material> getArmorPool(int tier) {
        return switch (tier) {
            case 3  -> ItemPools.ARMOR_T3;
            case 2  -> ItemPools.ARMOR_T2;
            default -> ItemPools.ARMOR_T1;
        };
    }

    // ── Boss handling ─────────────────────────────────────────────────────

    private boolean isBoss(LivingEntity entity) {
        return entity instanceof ElderGuardian
                || entity instanceof Wither
                || entity instanceof Warden
                || entity instanceof EnderDragon;
    }

    private void handleBossDrop(EntityDeathEvent event, LivingEntity entity) {
        if (!plugin.getConfig().getBoolean("boss-drops.enabled", true)) return;
        if (!plugin.isDropsAllowed(entity.getLocation())) return;

        // Only drop for the killing player if they have permission
        if (entity.getKiller() instanceof Player killer && !killer.hasPermission("rpgloot.boss.drops")) return;

        String key = bossConfigKey(entity);
        if (key == null) return;

        double chance = plugin.getConfig().getDouble("boss-drops." + key + ".chance", 1.0);
        if (random.nextDouble() > chance) return;

        Rarity minRarity = parseRarity(key, "min-rarity", Rarity.RARE);
        Rarity maxRarity = parseRarity(key, "max-rarity", Rarity.LEGENDARY);
        Rarity rarity = roller.rollWithRange(minRarity, maxRarity);

        // Bosses are the only source of Netherite — use full pool including netherite
        List<org.bukkit.Material> pool = getBossWeaponPool(entity);
        org.bukkit.Material mat = pool.get(random.nextInt(pool.size()));
        ItemStack item = new ItemStack(mat);
        rarityService.applyRarity(item, rarity);
        event.getDrops().add(item);
    }

    private List<org.bukkit.Material> getBossWeaponPool(LivingEntity entity) {
        // Elder Guardian is mid-game — cap at diamond
        if (entity instanceof ElderGuardian) return ItemPools.WEAPONS_T3;
        // Warden, Wither, Ender Dragon → full pool including Netherite
        return ItemPools.WEAPONS;
    }

    private String bossConfigKey(LivingEntity entity) {
        if (entity instanceof Warden)        return "warden";
        if (entity instanceof Wither)        return "wither";
        if (entity instanceof ElderGuardian) return "elder-guardian";
        if (entity instanceof EnderDragon)   return "ender-dragon";
        return null;
    }

    private Rarity parseRarity(String bossKey, String field, Rarity fallback) {
        String raw = plugin.getConfig().getString("boss-drops." + bossKey + "." + field);
        if (raw == null) return fallback;
        try {
            return Rarity.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid rarity '" + raw + "' in boss-drops." + bossKey + "." + field);
            return fallback;
        }
    }
}
