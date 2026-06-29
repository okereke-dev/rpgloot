package com.ricardo.rpgloot;

import org.bukkit.Material;

import java.util.Map;

public final class VanillaStats {

    // Vanilla ADD_NUMBER bonus for ATTACK_DAMAGE (base entity value is 1.0)
    private static final Map<Material, Double> BASE_DAMAGE = Map.ofEntries(
            Map.entry(Material.WOODEN_SWORD, 4.0),
            Map.entry(Material.STONE_SWORD, 5.0),
            Map.entry(Material.IRON_SWORD, 6.0),
            Map.entry(Material.GOLDEN_SWORD, 4.0),
            Map.entry(Material.DIAMOND_SWORD, 7.0),
            Map.entry(Material.NETHERITE_SWORD, 8.0),
            Map.entry(Material.WOODEN_AXE, 6.0),
            Map.entry(Material.STONE_AXE, 8.0),
            Map.entry(Material.IRON_AXE, 8.0),
            Map.entry(Material.GOLDEN_AXE, 6.0),
            Map.entry(Material.DIAMOND_AXE, 8.0),
            Map.entry(Material.NETHERITE_AXE, 9.0),
            Map.entry(Material.TRIDENT, 8.0),
            Map.entry(Material.MACE, 5.0)
    );

    // Absolute speed bonus per attack speed multiplier tier (positive = faster)
    // Vanilla attack speed modifiers are negative (e.g. sword -2.4), so bonus is positive to reduce negativity
    private static final Map<Material, Double> BASE_SPEED = Map.ofEntries(
            Map.entry(Material.WOODEN_SWORD, 2.4),
            Map.entry(Material.STONE_SWORD, 2.4),
            Map.entry(Material.IRON_SWORD, 2.4),
            Map.entry(Material.GOLDEN_SWORD, 2.4),
            Map.entry(Material.DIAMOND_SWORD, 2.4),
            Map.entry(Material.NETHERITE_SWORD, 2.4),
            Map.entry(Material.WOODEN_AXE, 3.2),
            Map.entry(Material.STONE_AXE, 3.2),
            Map.entry(Material.IRON_AXE, 3.1),
            Map.entry(Material.GOLDEN_AXE, 3.2),
            Map.entry(Material.DIAMOND_AXE, 3.0),
            Map.entry(Material.NETHERITE_AXE, 2.9),
            Map.entry(Material.TRIDENT, 2.9),
            Map.entry(Material.MACE, 3.4)
    );

    private VanillaStats() {
    }

    public static double baseDamage(Material material) {
        return BASE_DAMAGE.getOrDefault(material, 1.0);
    }

    public static double baseSpeed(Material material) {
        return BASE_SPEED.getOrDefault(material, 2.4);
    }
}
