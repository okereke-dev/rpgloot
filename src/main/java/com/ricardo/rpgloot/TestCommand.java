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
import java.util.Random;

public final class TestCommand implements CommandExecutor, TabCompleter {

    private static final List<Material> WEAPON_POOL = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE);

    private final ItemRarityService rarityService;
    private final Random random = new Random();

    public TestCommand(ItemRarityService rarityService) {
        this.rarityService = rarityService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo jugadores.", NamedTextColor.RED));
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
                player.sendMessage(Component.text("Generado: " + rarity.getDisplayName() + " " + material.name(), rarity.getColor()));
            }
            case "getall" -> {
                for (Rarity rarity : Rarity.values()) {
                    Material material = WEAPON_POOL.get(random.nextInt(WEAPON_POOL.size()));
                    ItemStack weapon = new ItemStack(material);
                    rarityService.applyRarity(weapon, rarity);
                    player.getInventory().addItem(weapon);
                }
                player.sendMessage(Component.text("Generados 5 items, uno por rareza.", NamedTextColor.GREEN));
            }
            default -> sendHelp(player);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("get", "getall");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("get")) {
            return Arrays.stream(Rarity.values()).map(r -> r.name().toLowerCase()).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("get")) {
            return WEAPON_POOL.stream().map(m -> m.name().toLowerCase()).toList();
        }
        return List.of();
    }

    private Rarity parseRarity(String input) {
        if (input == null) {
            return Rarity.values()[random.nextInt(Rarity.values().length)];
        }
        try {
            return Rarity.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Rarity.COMMON;
        }
    }

    private Material parseMaterial(String input) {
        if (input == null) {
            return WEAPON_POOL.get(random.nextInt(WEAPON_POOL.size()));
        }
        try {
            Material mat = Material.valueOf(input.toUpperCase());
            return WEAPON_POOL.contains(mat) ? mat : WEAPON_POOL.get(random.nextInt(WEAPON_POOL.size()));
        } catch (IllegalArgumentException e) {
            return WEAPON_POOL.get(random.nextInt(WEAPON_POOL.size()));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("--- RPGLoot ---", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/rpgloot get [rareza] [material]", NamedTextColor.YELLOW)
                .append(Component.text(" — genera un arma", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/rpgloot getall", NamedTextColor.YELLOW)
                .append(Component.text(" — una arma de cada rareza", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("Rarezas: common, uncommon, rare, hero, legendary", NamedTextColor.GRAY));
    }
}
