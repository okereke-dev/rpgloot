package com.ricardo.rpgloot;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ToolListener implements Listener {

    // Ores → smelted output for AUTO_SMELT
    private static final Map<Material, Material> SMELT_MAP = Map.ofEntries(
            Map.entry(Material.IRON_ORE,         Material.IRON_INGOT),
            Map.entry(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT),
            Map.entry(Material.GOLD_ORE,         Material.GOLD_INGOT),
            Map.entry(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT),
            Map.entry(Material.COPPER_ORE,       Material.COPPER_INGOT),
            Map.entry(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT),
            Map.entry(Material.NETHER_GOLD_ORE,  Material.GOLD_INGOT),
            Map.entry(Material.ANCIENT_DEBRIS,   Material.NETHERITE_SCRAP),
            Map.entry(Material.OAK_LOG,          Material.CHARCOAL),
            Map.entry(Material.BIRCH_LOG,        Material.CHARCOAL),
            Map.entry(Material.SPRUCE_LOG,       Material.CHARCOAL),
            Map.entry(Material.JUNGLE_LOG,       Material.CHARCOAL),
            Map.entry(Material.ACACIA_LOG,       Material.CHARCOAL),
            Map.entry(Material.DARK_OAK_LOG,     Material.CHARCOAL),
            Map.entry(Material.MANGROVE_LOG,     Material.CHARCOAL),
            Map.entry(Material.CHERRY_LOG,       Material.CHARCOAL),
            Map.entry(Material.SAND,             Material.GLASS),
            Map.entry(Material.RED_SAND,         Material.GLASS)
    );

    // Crops → replant seed material
    private static final Map<Material, Material> CROP_SEED_MAP = Map.of(
            Material.WHEAT,       Material.WHEAT_SEEDS,
            Material.CARROTS,     Material.CARROT,
            Material.POTATOES,    Material.POTATO,
            Material.BEETROOTS,   Material.BEETROOT_SEEDS,
            Material.NETHER_WART, Material.NETHER_WART
    );

    private final RPGLootPlugin plugin;
    private final ItemRarityService rarityService;
    private final SetTracker setTracker;
    private final Random random = new Random();

    public ToolListener(RPGLootPlugin plugin, ItemRarityService rarityService, SetTracker setTracker) {
        this.plugin = plugin;
        this.rarityService = rarityService;
        this.setTracker = setTracker;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) return;

        Rarity rarity = rarityService.getRarity(tool);
        if (rarity == null) return;

        List<RolledStat> stats = rarityService.getBonusStats(tool);
        if (stats.isEmpty()) return;

        Block block = event.getBlock();

        // FORTUNE_BOOST and AUTO_SMELT_CHANCE are handled in onBlockDropItem — modifying drop
        // quantities/types there (rather than cancelling this event) keeps BlockDropItemEvent
        // firing normally, so mcMMO's Double Drops/Harvest Lumber/Mother Lode and any
        // Telekinesis-style enchant plugin still get a chance to act on the same block break.
        for (RolledStat rolled : stats) {
            switch (rolled.stat()) {
                case XP_BOOST       -> applyXpBoost(event, player, rolled.value());
                case REPLANT_CHANCE -> applyReplant(block, rolled.value());
                default -> {}
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) return;

        Rarity rarity = rarityService.getRarity(tool);
        if (rarity == null) return;

        List<RolledStat> stats = rarityService.getBonusStats(tool);
        if (stats.isEmpty()) return;

        double fortuneBoostPct = 0;
        double smeltChancePct  = 0;
        for (RolledStat rolled : stats) {
            switch (rolled.stat()) {
                case FORTUNE_BOOST     -> fortuneBoostPct = rolled.value();
                case AUTO_SMELT_CHANCE -> smeltChancePct  = rolled.value();
                default -> {}
            }
        }
        if (fortuneBoostPct <= 0 && smeltChancePct <= 0) return;
        if (event.getItems().isEmpty()) return;

        if (smeltChancePct > 0 && random.nextDouble() * 100.0 < smeltChancePct) {
            applyAutoSmelt(event, fortuneBoostPct); // fortune boosts the smelted output quantity
        } else if (fortuneBoostPct > 0) {
            applyFortuneBoost(event, fortuneBoostPct);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();
        if (rarityService.getRarity(rod) == null) {
            rod = player.getInventory().getItemInOffHand();
            if (rarityService.getRarity(rod) == null) return;
        }

        for (RolledStat rolled : rarityService.getBonusStats(rod)) {
            if (rolled.stat() == BonusStat.DOUBLE_CATCH_CHANCE) {
                if (random.nextDouble() * 100.0 < rolled.value() && event.getCaught() != null) {
                    // Drop a copy of the caught item near the player
                    player.getWorld().dropItemNaturally(player.getLocation(),
                            ((Item) event.getCaught()).getItemStack().clone());
                }
            }
        }
    }

    // ── Stat implementations ──────────────────────────────────────────────

    /** Scales every drop entity's stack amount in place — composes with whatever is already in the list. */
    private void applyFortuneBoost(BlockDropItemEvent event, double fortuneBoostPct) {
        double multiplier = 1.0 + fortuneBoostPct / 100.0;
        for (Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            int boosted = Math.min((int) Math.round(stack.getAmount() * multiplier), stack.getMaxStackSize());
            stack.setAmount(boosted);
            item.setItemStack(stack);
        }
    }

    /** Collapses all drop entities into a single smelted stack, applying fortune to the total amount. */
    private void applyAutoSmelt(BlockDropItemEvent event, double fortuneBoostPct) {
        Material smelted = SMELT_MAP.get(event.getBlockState().getType());
        if (smelted == null) return;

        int baseAmount = event.getItems().stream().mapToInt(item -> item.getItemStack().getAmount()).sum();
        double multiplier = 1.0 + fortuneBoostPct / 100.0;
        int finalAmount = Math.min((int) Math.round(baseAmount * multiplier), new ItemStack(smelted).getMaxStackSize());

        // Per BlockDropItemEvent's contract, removing an entry from this list prevents it from
        // dropping — no need to despawn the entity directly.
        Iterator<Item> it = event.getItems().iterator();
        Item first = it.next();
        first.setItemStack(new ItemStack(smelted, finalAmount));
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    private void applyXpBoost(BlockBreakEvent event, Player player, double xpBoostPct) {
        int baseXp = event.getExpToDrop();
        if (baseXp <= 0) return;
        double total = xpBoostPct + setTracker.getSetBonus(player, BonusStat.XP_BOOST);
        event.setExpToDrop(baseXp + (int) Math.round(baseXp * (total / 100.0)));
    }

    private void applyReplant(Block block, double replantChancePct) {
        Material seed = CROP_SEED_MAP.get(block.getType());
        if (seed == null) return;
        if (random.nextDouble() * 100.0 > replantChancePct) return;

        // Replant after the block finishes breaking on the next tick
        Material cropType = block.getType();
        org.bukkit.Location loc = block.getLocation();
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> {
                    Block target = loc.getBlock();
                    if (target.getType().isAir()) {
                        target.setType(cropType);
                        // Set crop to age 0 (just planted)
                        if (target.getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
                            ageable.setAge(0);
                            target.setBlockData(ageable);
                        }
                    }
                }, 1L);
    }
}
