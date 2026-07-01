package com.ricardo.rpgloot;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
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
    private final Random random = new Random();

    public ToolListener(RPGLootPlugin plugin, ItemRarityService rarityService) {
        this.plugin = plugin;
        this.rarityService = rarityService;
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

        for (RolledStat rolled : stats) {
            switch (rolled.stat()) {
                case FORTUNE_BOOST -> applyFortuneBoost(event, block, rolled.value());
                case XP_BOOST      -> applyXpBoost(event, rolled.value());
                case AUTO_SMELT_CHANCE -> applyAutoSmelt(event, player, block, rolled.value());
                case REPLANT_CHANCE    -> applyReplant(block, rolled.value());
                default -> {}
            }
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
                            ((org.bukkit.entity.Item) event.getCaught()).getItemStack().clone());
                }
            }
        }
    }

    // ── Stat implementations ──────────────────────────────────────────────

    private void applyFortuneBoost(BlockBreakEvent event, Block block, double fortuneBoostPct) {
        if (random.nextDouble() * 100.0 > fortuneBoostPct) return;
        List<ItemStack> drops = new ArrayList<>(block.getDrops(event.getPlayer().getInventory().getItemInMainHand()));
        if (drops.isEmpty()) return;
        event.setDropItems(false);
        // Scale drop amount by the stat value: e.g. 35% fortune boost → 1.35× drops
        double multiplier = 1.0 + fortuneBoostPct / 100.0;
        for (ItemStack drop : drops) {
            ItemStack bonus = drop.clone();
            int boosted = (int) Math.round(drop.getAmount() * multiplier);
            bonus.setAmount(Math.min(boosted, drop.getMaxStackSize()));
            block.getWorld().dropItemNaturally(block.getLocation(), bonus);
        }
    }

    private void applyXpBoost(BlockBreakEvent event, double xpBoostPct) {
        int baseXp = event.getExpToDrop();
        if (baseXp <= 0) return;
        double bonus = baseXp * (xpBoostPct / 100.0);
        event.setExpToDrop(baseXp + (int) Math.round(bonus));
    }

    private void applyAutoSmelt(BlockBreakEvent event, Player player, Block block, double smeltChancePct) {
        if (random.nextDouble() * 100.0 > smeltChancePct) return;
        Material smelted = SMELT_MAP.get(block.getType());
        if (smelted == null) return;

        // Replace normal drops with smelted output
        List<ItemStack> drops = new ArrayList<>(block.getDrops(player.getInventory().getItemInMainHand()));
        if (drops.isEmpty()) return;
        event.setDropItems(false);
        // Drop smelted result (quantity matches first drop)
        int amount = drops.get(0).getAmount();
        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(smelted, amount));
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
