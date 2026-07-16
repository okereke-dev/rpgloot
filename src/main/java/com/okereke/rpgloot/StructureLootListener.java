package com.okereke.rpgloot;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class StructureLootListener implements Listener {

    // Structure loot table path → max rarity allowed for that structure. Used only as the
    // fallback default for any key missing from config.yml's structure-loot.max-rarity, so
    // admins can tune per-structure caps (or add new ones) without recompiling.
    private static final Map<String, Rarity> DEFAULT_STRUCTURE_MAX_RARITY = Map.ofEntries(
            Map.entry("chests/simple_dungeon", Rarity.RARE),
            Map.entry("chests/abandoned_mineshaft", Rarity.RARE),
            Map.entry("chests/desert_pyramid", Rarity.RARE),
            Map.entry("chests/jungle_temple", Rarity.RARE),
            Map.entry("chests/stronghold_corridor", Rarity.RARE),
            Map.entry("chests/stronghold_library", Rarity.HERO),
            Map.entry("chests/stronghold_crossing", Rarity.RARE),
            Map.entry("chests/woodland_mansion", Rarity.HERO),
            Map.entry("chests/pillager_outpost", Rarity.RARE),
            Map.entry("chests/shipwreck_supply", Rarity.RARE),
            Map.entry("chests/shipwreck_treasure", Rarity.RARE),
            Map.entry("chests/buried_treasure", Rarity.RARE),
            Map.entry("chests/bastion_treasure", Rarity.HERO),
            Map.entry("chests/bastion_bridge", Rarity.RARE),
            Map.entry("chests/bastion_other", Rarity.RARE),
            Map.entry("chests/bastion_hoglin_stable", Rarity.RARE),
            Map.entry("chests/nether_bridge", Rarity.RARE),
            Map.entry("chests/ruined_portal", Rarity.RARE),
            Map.entry("chests/end_city_treasure", Rarity.LEGENDARY),
            Map.entry("chests/ancient_city", Rarity.LEGENDARY),
            Map.entry("chests/underwater_ruin_small", Rarity.UNCOMMON),
            Map.entry("chests/underwater_ruin_big", Rarity.RARE),
            Map.entry("chests/igloo_chest", Rarity.UNCOMMON),
            Map.entry("chests/village/village_weaponsmith", Rarity.RARE),
            Map.entry("chests/village/village_toolsmith", Rarity.RARE),
            Map.entry("chests/village/village_armorer", Rarity.RARE),
            Map.entry("chests/trial_chambers/reward", Rarity.HERO),
            Map.entry("chests/trial_chambers/reward_rare", Rarity.LEGENDARY),
            Map.entry("chests/trial_chambers/reward_common", Rarity.RARE)
    );

    private final RPGLootPlugin plugin;
    private final ItemRarityService rarityService;
    private final RarityRoller roller;
    private final Random random = new Random();

    public StructureLootListener(RPGLootPlugin plugin, ItemRarityService rarityService) {
        this.plugin = plugin;
        this.rarityService = rarityService;
        this.roller = new RarityRoller(plugin.getConfig(), plugin.getLogger());
    }

    public void reload() {
        roller.reload(plugin.getConfig());
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        if (!plugin.getConfig().getBoolean("structure-loot.enabled", true)) return;

        if (event.getLootTable() == null) return;
        String tableKey = event.getLootTable().getKey().getKey();

        Rarity maxRarity = getMaxRarity(tableKey);
        if (maxRarity == null) return; // not a recognized structure chest

        if (!plugin.isDropsAllowed(event.getLootContext().getLocation())) return;

        double injectChance = plugin.getConfig().getDouble("structure-loot.inject-chance", 0.75);
        if (random.nextDouble() > injectChance) return;

        int maxItems = Math.max(1, plugin.getConfig().getInt("structure-loot.max-items", 2));
        double extraItemChance = plugin.getConfig().getDouble("structure-loot.extra-item-chance", 0.30);
        double armorChance = plugin.getConfig().getDouble("structure-loot.armor-chance", 0.35);

        event.getLoot().add(rollItem(maxRarity, armorChance));
        for (int added = 1; added < maxItems; added++) {
            if (random.nextDouble() > extraItemChance) break;
            event.getLoot().add(rollItem(maxRarity, armorChance));
        }
    }

    private ItemStack rollItem(Rarity maxRarity, double armorChance) {
        boolean armor = random.nextDouble() < armorChance;
        List<Material> pool = armor ? armorPoolFor(maxRarity) : weaponPoolFor(maxRarity);
        Material mat = pool.get(random.nextInt(pool.size()));
        ItemStack item = new ItemStack(mat);
        rarityService.applyRarity(item, roller.rollWithMax(maxRarity));
        return item;
    }

    /** Mid tier (≤ RARE): iron/gold. High tier (HERO+): iron→diamond. */
    private static List<Material> weaponPoolFor(Rarity maxRarity) {
        if (maxRarity.ordinal() >= Rarity.HERO.ordinal()) {
            return ItemPools.STRUCTURE_WEAPONS_HIGH;
        }
        return ItemPools.STRUCTURE_WEAPONS_MID;
    }

    private static List<Material> armorPoolFor(Rarity maxRarity) {
        if (maxRarity.ordinal() >= Rarity.HERO.ordinal()) {
            return ItemPools.ARMOR_T3;
        }
        return ItemPools.ARMOR_T2;
    }

    /** Reads the max rarity for a structure loot table from config.yml, falling back to the compiled-in default for that key (or null if the key isn't recognized at all). */
    private Rarity getMaxRarity(String tableKey) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("structure-loot.max-rarity");
        if (section != null && section.isString(tableKey)) {
            String raw = section.getString(tableKey);
            try {
                return Rarity.valueOf(raw.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid rarity '" + raw + "' for structure-loot.max-rarity." + tableKey + " — using default.");
            }
        }
        return DEFAULT_STRUCTURE_MAX_RARITY.get(tableKey);
    }
}
