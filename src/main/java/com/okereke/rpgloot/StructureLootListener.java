package com.okereke.rpgloot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Structure / vault / archaeology loot: optional extra native {@link LootTable#populateLoot}
 * rolls, ensure-gear retries when no weapon/armor/tool appeared, then convert to RPGLoot.
 * Axes become {@link WeaponType#AXE_TOOL}.
 */
public final class StructureLootListener implements Listener {

    private static final int CHEST_CAPACITY = 27;

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
    private int extraRolls = 0;
    private float luckBonus = 0f;
    private boolean ensureGearEnabled = true;
    private int ensureGearAttempts = 8;
    private ChestLootDebug chestDebug;

    public StructureLootListener(RPGLootPlugin plugin, ItemRarityService rarityService) {
        this.plugin = plugin;
        this.rarityService = rarityService;
        this.roller = new RarityRoller(plugin.getConfig(), plugin.getLogger());
        reload();
        registerVaultHook();
    }

    public void setChestDebug(ChestLootDebug chestDebug) {
        this.chestDebug = chestDebug;
    }

    /** True if this loot-table key is in structure-loot.max-rarity (config or built-in defaults). */
    public boolean isRecognizedTable(String tableKey) {
        return getMaxRarity(tableKey) != null;
    }

    public void reload() {
        roller.reload(plugin.getConfig());
        extraRolls = Math.max(0, plugin.getConfig().getInt("structure-loot.extra-rolls", 0));
        luckBonus = (float) plugin.getConfig().getDouble("structure-loot.luck-bonus", 0.0);
        ensureGearEnabled = plugin.getConfig().getBoolean("structure-loot.ensure-gear.enabled", true);
        ensureGearAttempts = Math.max(0, plugin.getConfig().getInt("structure-loot.ensure-gear.attempts", 8));
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        if (!plugin.getConfig().getBoolean("structure-loot.enabled", true)) return;
        // populateLoot does not fire this event; fillInventory would — skip plugin-caused fills.
        if (event.isPlugin()) return;

        if (event.getLootTable() == null) return;
        String tableKey = event.getLootTable().getKey().getKey();

        Rarity maxRarity = getMaxRarity(tableKey);
        if (maxRarity == null) return; // not a recognized structure chest

        if (!plugin.isDropsAllowed(event.getLootContext().getLocation())) return;

        List<ItemStack> loot = event.getLoot();
        EnrichResult result = enrichAndConvert(event.getLootTable(), event.getLootContext(), loot, maxRarity);
        if (chestDebug != null) {
            chestDebug.notifyGenerate(
                    event.getLootContext().getLocation(),
                    tableKey,
                    result.gearStacksAfterEnrich(),
                    result.ensureGearUsed());
        }
    }

    private record EnrichResult(int gearStacksAfterEnrich, boolean ensureGearUsed) {}

    /**
     * Shared chest/vault pipeline: extra native rolls → ensure gear → trim → RPGLoot convert.
     * Rarity still comes from {@code rarity-weights} + {@code max-rarity} in convert.
     */
    private EnrichResult enrichAndConvert(LootTable table, LootContext context, List<ItemStack> loot, Rarity maxRarity) {
        if (loot == null) return new EnrichResult(0, false);
        appendNativeExtraRolls(table, context, loot);
        boolean hadGear = containsConvertibleGear(loot);
        boolean ensureUsed = ensureGearIfMissing(table, context, loot);
        trimToChestCapacity(loot);
        int gearCount = countConvertibleGear(loot);
        LootConvert.convertGear(loot, maxRarity, rarityService, roller);
        return new EnrichResult(gearCount, ensureUsed && !hadGear);
    }

    /**
     * Extra full rolls of the same vanilla loot table via {@link LootTable#populateLoot}.
     * Uses native pool weights/rolls — not a custom item inject. Luck only helps tables that
     * define {@code bonus_rolls} / entry {@code quality} (most structure chests ignore it).
     */
    private void appendNativeExtraRolls(LootTable table, LootContext base, List<ItemStack> loot) {
        if (extraRolls <= 0 || table == null || base == null) return;

        LootContext context = boostContext(base);
        for (int i = 0; i < extraRolls; i++) {
            appendNonAir(loot, table.populateLoot(ThreadLocalRandom.current(), context));
        }
    }

    /**
     * If the merged loot has no convertible weapon/armor/tool, re-roll the same vanilla table
     * up to {@code ensure-gear.attempts} times and add only gear stacks from the first successful roll.
     * @return true if gear was added by this method
     */
    private boolean ensureGearIfMissing(LootTable table, LootContext base, List<ItemStack> loot) {
        if (!ensureGearEnabled || ensureGearAttempts <= 0 || table == null || base == null) return false;
        if (containsConvertibleGear(loot)) return false;

        LootContext context = boostContext(base);
        for (int i = 0; i < ensureGearAttempts; i++) {
            Collection<ItemStack> rolled = table.populateLoot(ThreadLocalRandom.current(), context);
            if (rolled == null || rolled.isEmpty()) continue;

            boolean found = false;
            for (ItemStack stack : rolled) {
                if (LootConvert.isConvertibleGear(stack)) {
                    loot.add(stack.clone());
                    found = true;
                }
            }
            if (found) return true;
        }
        return false;
    }

    private static boolean containsConvertibleGear(List<ItemStack> loot) {
        return countConvertibleGear(loot) > 0;
    }

    private static int countConvertibleGear(List<ItemStack> loot) {
        int n = 0;
        for (ItemStack stack : loot) {
            if (LootConvert.isConvertibleGear(stack)) n++;
        }
        return n;
    }

    /**
     * When loot exceeds a single chest (27 slots), drop non-gear stacks first (random among them),
     * then random gear if still over capacity.
     */
    private void trimToChestCapacity(List<ItemStack> loot) {
        if (loot == null || loot.size() <= CHEST_CAPACITY) return;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        while (loot.size() > CHEST_CAPACITY) {
            List<Integer> nonGearIndexes = new ArrayList<>();
            for (int i = 0; i < loot.size(); i++) {
                if (!LootConvert.isConvertibleGear(loot.get(i))) {
                    nonGearIndexes.add(i);
                }
            }
            if (!nonGearIndexes.isEmpty()) {
                loot.remove((int) nonGearIndexes.get(rng.nextInt(nonGearIndexes.size())));
            } else {
                loot.remove(rng.nextInt(loot.size()));
            }
        }
    }

    private static void appendNonAir(List<ItemStack> loot, Collection<ItemStack> rolled) {
        if (rolled == null || rolled.isEmpty()) return;
        for (ItemStack stack : rolled) {
            if (stack != null && !stack.getType().isAir()) {
                loot.add(stack);
            }
        }
    }

    private LootContext boostContext(LootContext base) {
        LootContext.Builder builder = new LootContext.Builder(base.getLocation())
                .luck(base.getLuck() + luckBonus);
        HumanEntity killer = base.getKiller();
        if (killer != null) {
            builder.killer(killer);
        }
        Entity looted = base.getLootedEntity();
        if (looted != null) {
            builder.lootedEntity(looted);
        }
        return builder.build();
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
                    if (dispensed == null) return;

                    List<ItemStack> loot = new ArrayList<>(dispensed);
                    if (loc != null) {
                        LootContext ctx = new LootContext.Builder(loc).luck(luckBonus).build();
                        EnrichResult result = enrichAndConvert(table, ctx, loot, maxRarity);
                        if (chestDebug != null) {
                            chestDebug.notifyGenerate(loc, table.getKey().getKey(),
                                    result.gearStacksAfterEnrich(), result.ensureGearUsed());
                        }
                    } else {
                        LootConvert.convertGear(loot, maxRarity, rarityService, roller);
                    }
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
