package com.okereke.rpgloot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTable;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts vanilla weapons, armor, and tools already present in structure / vault / archaeology
 * loot into RPGLoot items (same material). Axes become {@link WeaponType#AXE_TOOL}.
 * Does not inject extra items — if Minecraft didn't generate gear, nothing is added.
 */
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
            Map.entry("chests/trial_chambers/reward_common", Rarity.RARE),
            Map.entry("chests/trial_chambers/reward_unique", Rarity.LEGENDARY),
            Map.entry("chests/trial_chambers/reward_ominous", Rarity.LEGENDARY),
            Map.entry("chests/trial_chambers/reward_ominous_common", Rarity.HERO),
            Map.entry("chests/trial_chambers/reward_ominous_rare", Rarity.LEGENDARY),
            Map.entry("chests/trial_chambers/reward_ominous_unique", Rarity.LEGENDARY),
            Map.entry("archaeology", Rarity.RARE)
    );

    private final RPGLootPlugin plugin;
    private final ItemRarityService rarityService;
    private final RarityRoller roller;

    public StructureLootListener(RPGLootPlugin plugin, ItemRarityService rarityService) {
        this.plugin = plugin;
        this.rarityService = rarityService;
        this.roller = new RarityRoller(plugin.getConfig(), plugin.getLogger());
        registerVaultHook();
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

        LootConvert.convertGear(event.getLoot(), maxRarity, rarityService, roller);
    }

    /**
     * Archaeology: brushing suspicious sand/gravel can drop iron axes / wooden hoes.
     * Convert those (and any other gear) with the {@code archaeology} max-rarity cap.
     */
    @EventHandler
    public void onBlockDropItem(BlockDropItemEvent event) {
        if (!plugin.getConfig().getBoolean("structure-loot.enabled", true)) return;

        BlockState state = event.getBlockState();
        if (state == null || !isSuspicious(state.getType())) return;
        if (!plugin.isDropsAllowed(event.getBlock().getLocation())) return;

        Rarity maxRarity = getMaxRarity("archaeology");
        if (maxRarity == null) maxRarity = Rarity.RARE;

        for (Item drop : event.getItems()) {
            ItemStack stack = drop.getItemStack();
            ItemStack converted = LootConvert.convertOne(stack, maxRarity, rarityService, roller);
            if (converted != null) {
                drop.setItemStack(converted);
            }
        }
    }

    /**
     * BlockDispenseLootEvent (vaults / trial rewards) exists on newer Paper APIs but this
     * project may compile against older paper-api — register reflectively.
     */
    @SuppressWarnings("unchecked")
    private void registerVaultHook() {
        try {
            Class<? extends Event> eventClass =
                    (Class<? extends Event>) Class.forName("org.bukkit.event.block.BlockDispenseLootEvent");
            Method getLootTable = eventClass.getMethod("getLootTable");
            Method getDispensedLoot = eventClass.getMethod("getDispensedLoot");
            Method setDispensedLoot = eventClass.getMethod("setDispensedLoot", List.class);
            Method getBlock = eventClass.getMethod("getBlock");

            EventExecutor executor = (listener, event) -> {
                try {
                    if (!plugin.getConfig().getBoolean("structure-loot.enabled", true)) return;

                    LootTable table = (LootTable) getLootTable.invoke(event);
                    if (table == null) return;

                    Rarity maxRarity = getMaxRarity(table.getKey().getKey());
                    if (maxRarity == null) return;

                    Block block = (Block) getBlock.invoke(event);
                    Location loc = block != null ? block.getLocation() : null;
                    if (loc != null && !plugin.isDropsAllowed(loc)) return;

                    List<ItemStack> dispensed = (List<ItemStack>) getDispensedLoot.invoke(event);
                    if (dispensed == null || dispensed.isEmpty()) return;

                    List<ItemStack> loot = new ArrayList<>(dispensed);
                    LootConvert.convertGear(loot, maxRarity, rarityService, roller);
                    setDispensedLoot.invoke(event, loot);
                } catch (ReflectiveOperationException ex) {
                    plugin.getLogger().warning("BlockDispenseLootEvent convert failed: " + ex.getMessage());
                }
            };

            plugin.getServer().getPluginManager().registerEvent(
                    eventClass,
                    this,
                    EventPriority.NORMAL,
                    executor,
                    plugin,
                    true
            );
            plugin.getLogger().info("BlockDispenseLootEvent hook registered — vault/trial loot converts to RPGLoot");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("BlockDispenseLootEvent not present — vault convert skipped (chests still convert)");
        } catch (Exception e) {
            plugin.getLogger().warning("Could not register BlockDispenseLootEvent hook: " + e.getMessage());
        }
    }

    private static boolean isSuspicious(Material type) {
        if (type == null) return false;
        String name = type.name();
        return name.equals("SUSPICIOUS_SAND") || name.equals("SUSPICIOUS_GRAVEL");
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
