package com.okereke.rpgloot;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class StructureLootListener implements Listener {

    // Structure loot table path → max rarity allowed for that structure
    private static final Map<String, Rarity> STRUCTURE_MAX_RARITY = Map.ofEntries(
            Map.entry("chests/simple_dungeon",        Rarity.UNCOMMON),
            Map.entry("chests/abandoned_mineshaft",   Rarity.UNCOMMON),
            Map.entry("chests/desert_pyramid",        Rarity.RARE),
            Map.entry("chests/jungle_temple",         Rarity.RARE),
            Map.entry("chests/stronghold_corridor",   Rarity.RARE),
            Map.entry("chests/stronghold_library",    Rarity.RARE),
            Map.entry("chests/woodland_mansion",      Rarity.HERO),
            Map.entry("chests/pillager_outpost",      Rarity.RARE),
            Map.entry("chests/shipwreck_supply",      Rarity.UNCOMMON),
            Map.entry("chests/bastion_treasure",      Rarity.HERO),
            Map.entry("chests/ruined_portal",         Rarity.RARE),
            Map.entry("chests/end_city_treasure",     Rarity.LEGENDARY),
            Map.entry("chests/ancient_city",          Rarity.LEGENDARY)
    );

    // Structure loot uses the no-netherite pool — netherite is too powerful for random chest loot
    private static final List<Material> WEAPON_POOL = ItemPools.WEAPONS_NO_NETHERITE;

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

        Rarity maxRarity = STRUCTURE_MAX_RARITY.get(tableKey);
        if (maxRarity == null) return; // not a recognized structure chest

        if (!plugin.isDropsAllowed(event.getLootContext().getLocation())) return;

        double injectChance = plugin.getConfig().getDouble("structure-loot.inject-chance", 0.40);
        if (random.nextDouble() > injectChance) return;

        Rarity rarity = roller.rollWithMax(maxRarity);
        Material mat = WEAPON_POOL.get(random.nextInt(WEAPON_POOL.size()));
        ItemStack item = new ItemStack(mat);
        rarityService.applyRarity(item, rarity);

        event.getLoot().add(item);
    }
}
