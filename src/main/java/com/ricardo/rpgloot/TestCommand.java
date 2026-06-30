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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class TestCommand implements CommandExecutor, TabCompleter {

    private static final List<Material> WEAPON_POOL = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.TRIDENT, Material.MACE,
            Material.BOW, Material.CROSSBOW);

    private static final Map<String, List<Material>> POOL_BY_TYPE = Map.of(
            "sword", List.of(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                    Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD),
            "axe", List.of(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                    Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE),
            "trident", List.of(Material.TRIDENT),
            "mace", List.of(Material.MACE),
            "bow", List.of(Material.BOW),
            "crossbow", List.of(Material.CROSSBOW));

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

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "get" -> {
                Rarity rarity = parseRarity(args.length > 1 ? args[1] : null);
                Material material = parseMaterial(args.length > 2 ? args[2] : null);
                ItemStack weapon = new ItemStack(material);
                rarityService.applyRarity(weapon, rarity);
                player.getInventory().addItem(weapon);
                player.sendMessage(Component.text("Generated: " + rarity.getDisplayName() + " " + material.name(), rarity.getColor()));
            }
            case "getall" -> {
                List<Material> pool = args.length > 1
                        ? POOL_BY_TYPE.getOrDefault(args[1].toLowerCase(), WEAPON_POOL)
                        : WEAPON_POOL;

                for (Rarity rarity : Rarity.values()) {
                    Material material = pool.get(random.nextInt(pool.size()));
                    ItemStack weapon = new ItemStack(material);
                    rarityService.applyRarity(weapon, rarity);
                    player.getInventory().addItem(weapon);
                }
                player.sendMessage(Component.text("Generated one weapon per rarity.", NamedTextColor.GREEN));
            }
            case "stats" -> {
                ItemStack held = player.getInventory().getItemInMainHand();
                Rarity rarity = rarityService.getRarity(held);
                if (rarity == null) {
                    player.sendMessage(Component.text("This item has no rarity.", NamedTextColor.RED));
                    return true;
                }
                List<RolledStat> stats = rarityService.getBonusStats(held);
                player.sendMessage(Component.text("--- Item Stats ---", NamedTextColor.GOLD));
                player.sendMessage(Component.text("Rarity: ", NamedTextColor.GRAY)
                        .append(Component.text(rarity.getDisplayName(), rarity.getColor())));
                if (stats.isEmpty()) {
                    player.sendMessage(Component.text("No bonus stats.", NamedTextColor.GRAY));
                } else {
                    for (RolledStat rolled : stats) {
                        player.sendMessage(Component.text(
                                rolled.stat().getLabel() + ": +" + (int) Math.round(rolled.value()) + rolled.stat().getUnit(),
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
        if (args.length == 1) {
            return List.of("get", "getall", "stats");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("get")) {
                return Arrays.stream(Rarity.values()).map(r -> r.name().toLowerCase()).toList();
            }
            if (args[0].equalsIgnoreCase("getall")) {
                return List.of("sword", "axe", "trident", "mace", "bow", "crossbow");
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("get")) {
            return WEAPON_POOL.stream().map(m -> m.name().toLowerCase()).toList();
        }
        return List.of();
    }

    private Rarity parseRarity(String input) {
        if (input == null) return Rarity.values()[random.nextInt(Rarity.values().length)];
        try {
            return Rarity.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Rarity.COMMON;
        }
    }

    private Material parseMaterial(String input) {
        if (input == null) return WEAPON_POOL.get(random.nextInt(WEAPON_POOL.size()));
        try {
            Material mat = Material.valueOf(input.toUpperCase());
            return WEAPON_POOL.contains(mat) ? mat : WEAPON_POOL.get(random.nextInt(WEAPON_POOL.size()));
        } catch (IllegalArgumentException e) {
            return WEAPON_POOL.get(random.nextInt(WEAPON_POOL.size()));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("--- RPGLoot ---", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/rpgloot get [rarity] [material]", NamedTextColor.YELLOW)
                .append(Component.text(" — generates a weapon", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/rpgloot getall [type]", NamedTextColor.YELLOW)
                .append(Component.text(" — one weapon per rarity (type: sword/axe/trident/mace/bow/crossbow)", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/rpgloot stats", NamedTextColor.YELLOW)
                .append(Component.text(" — shows stats of the item in hand", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("Rarities: common, uncommon, rare, hero, legendary", NamedTextColor.GRAY));
    }
}
