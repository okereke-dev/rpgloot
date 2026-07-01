package com.ricardo.rpgloot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class TestCommand implements CommandExecutor, TabCompleter {

    private static final Map<String, WeaponType> TYPE_MAP = Map.ofEntries(
            Map.entry("sword",       WeaponType.SWORD),
            Map.entry("axe",         WeaponType.AXE),
            Map.entry("axe_tool",    WeaponType.AXE_TOOL),
            Map.entry("trident",     WeaponType.TRIDENT),
            Map.entry("mace",        WeaponType.MACE),
            Map.entry("bow",         WeaponType.BOW),
            Map.entry("crossbow",    WeaponType.CROSSBOW),
            Map.entry("helmet",      WeaponType.HELMET),
            Map.entry("chestplate",  WeaponType.CHESTPLATE),
            Map.entry("leggings",    WeaponType.LEGGINGS),
            Map.entry("boots",       WeaponType.BOOTS),
            Map.entry("pickaxe",     WeaponType.PICKAXE),
            Map.entry("shovel",      WeaponType.SHOVEL),
            Map.entry("hoe",         WeaponType.HOE),
            Map.entry("fishing_rod", WeaponType.FISHING_ROD));

    private static final List<Material> ALL_POOL;
    static {
        ALL_POOL = new ArrayList<>();
        ALL_POOL.addAll(ItemPools.WEAPONS);
        ALL_POOL.addAll(ItemPools.ARMOR);
        ALL_POOL.addAll(ItemPools.TOOLS);
    }

    private final ItemRarityService rarityService;
    private final Random random = new Random();

    public TestCommand(ItemRarityService rarityService) {
        this.rarityService = rarityService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "get" -> {
                Rarity rarity     = parseRarity(args.length > 1 ? args[1] : null);
                String typeArg    = args.length > 2 ? args[2].toLowerCase() : null;
                String matArg     = args.length > 3 ? args[3].toUpperCase() : null;

                WeaponType type = typeArg != null ? TYPE_MAP.get(typeArg) : null;
                Material material = matArg != null ? parseMat(matArg) : null;
                if (material == null) material = randomMaterialFor(type);
                if (material == null) material = ALL_POOL.get(random.nextInt(ALL_POOL.size()));
                if (type == null) type = WeaponType.of(material);
                if (type == null) { player.sendMessage(Component.text("Unsupported material.", NamedTextColor.RED)); return true; }

                ItemStack item = new ItemStack(material);
                rarityService.applyRarity(item, rarity, type);
                player.getInventory().addItem(item);
                player.sendMessage(Component.text("Generated: " + rarity.getDisplayName() + " " + material.name(), rarity.getColor()));
            }
            case "getall" -> {
                String typeArg = args.length > 1 ? args[1].toLowerCase() : null;
                // Resolve type once outside loop only if explicitly specified
                WeaponType fixedType = typeArg != null ? TYPE_MAP.get(typeArg) : null;

                for (Rarity rarity : Rarity.values()) {
                    // Fresh material + type per rarity so random mode gives real diversity
                    WeaponType type = fixedType;
                    Material material = randomMaterialFor(type);
                    if (material == null) material = ALL_POOL.get(random.nextInt(ALL_POOL.size()));
                    if (type == null) type = WeaponType.of(material);
                    if (type == null) continue;

                    ItemStack item = new ItemStack(material);
                    rarityService.applyRarity(item, rarity, type);
                    player.getInventory().addItem(item);
                }
                player.sendMessage(Component.text("Generated one item per rarity.", NamedTextColor.GREEN));
            }
            case "stats" -> {
                ItemStack held = player.getInventory().getItemInMainHand();
                Rarity rarity = rarityService.getRarity(held);
                if (rarity == null) { player.sendMessage(Component.text("This item has no rarity.", NamedTextColor.RED)); return true; }
                List<RolledStat> stats = rarityService.getBonusStats(held);
                player.sendMessage(Component.text("--- Item Stats ---", NamedTextColor.GOLD));
                player.sendMessage(Component.text("Rarity: ", NamedTextColor.GRAY).append(Component.text(rarity.getDisplayName(), rarity.getColor())));
                if (stats.isEmpty()) {
                    player.sendMessage(Component.text("No bonus stats.", NamedTextColor.GRAY));
                } else {
                    for (RolledStat rolled : stats) {
                        double val = rolled.value();
                        String formatted = val < 10 ? String.format("%.1f", val) : String.valueOf((int) Math.round(val));
                        player.sendMessage(Component.text(
                                rolled.stat().getLabel() + ": +" + formatted + rolled.stat().getUnit(),
                                NamedTextColor.LIGHT_PURPLE));
                    }
                }
            }
            default -> sendHelp(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("get", "getall", "stats");
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("get"))    return Arrays.stream(Rarity.values()).map(r -> r.name().toLowerCase()).toList();
            if (args[0].equalsIgnoreCase("getall")) return List.copyOf(TYPE_MAP.keySet());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("get")) return List.copyOf(TYPE_MAP.keySet());
        return List.of();
    }

    private Rarity parseRarity(String input) {
        if (input == null) return Rarity.values()[random.nextInt(Rarity.values().length)];
        try { return Rarity.valueOf(input.toUpperCase()); } catch (IllegalArgumentException e) { return Rarity.COMMON; }
    }

    private Material parseMat(String input) {
        try { return Material.valueOf(input); } catch (IllegalArgumentException e) { return null; }
    }

    private Material randomMaterialFor(WeaponType type) {
        if (type == null) return null;
        List<Material> mats = List.copyOf(type.getMaterials());
        return mats.get(random.nextInt(mats.size()));
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("--- RPGLoot ---", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/rpgloot get [rarity] [type] [material]", NamedTextColor.YELLOW)
                .append(Component.text(" — generates an item", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/rpgloot getall [type]", NamedTextColor.YELLOW)
                .append(Component.text(" — one per rarity tier", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/rpgloot stats", NamedTextColor.YELLOW)
                .append(Component.text(" — shows stats of the item in hand", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("Types: sword axe axe_tool trident mace bow crossbow helmet chestplate leggings boots pickaxe shovel hoe fishing_rod", NamedTextColor.GRAY));
    }
}
