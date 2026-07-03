package com.ricardo.rpgloot;

import org.bukkit.Material;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class VanillaStats {

    // Vanilla ADD_NUMBER bonus for ATTACK_DAMAGE (base entity value is 1.0)
    private static final Map<Material, Double> BASE_DAMAGE;
    static {
        Map<Material, Double> m = new HashMap<>();
        m.put(Material.WOODEN_SWORD, 4.0);
        m.put(Material.STONE_SWORD,  5.0);
        m.put(Material.IRON_SWORD,   6.0);
        m.put(Material.GOLDEN_SWORD, 4.0);
        m.put(Material.DIAMOND_SWORD,    7.0);
        m.put(Material.NETHERITE_SWORD,  8.0);
        m.put(Material.WOODEN_AXE,  6.0);
        m.put(Material.STONE_AXE,   8.0);
        m.put(Material.IRON_AXE,    8.0);
        m.put(Material.GOLDEN_AXE,  6.0);
        m.put(Material.DIAMOND_AXE,   8.0);
        m.put(Material.NETHERITE_AXE, 9.0);
        m.put(Material.TRIDENT, 8.0);
        Material mace = Material.getMaterial("MACE");
        if (mace != null) m.put(mace, 5.0);
        BASE_DAMAGE = Collections.unmodifiableMap(m);
    }

    // Absolute speed bonus per attack speed multiplier tier (positive = faster)
    private static final Map<Material, Double> BASE_SPEED;
    static {
        Map<Material, Double> m = new HashMap<>();
        m.put(Material.WOODEN_SWORD,   2.4);
        m.put(Material.STONE_SWORD,    2.4);
        m.put(Material.IRON_SWORD,     2.4);
        m.put(Material.GOLDEN_SWORD,   2.4);
        m.put(Material.DIAMOND_SWORD,  2.4);
        m.put(Material.NETHERITE_SWORD, 2.4);
        m.put(Material.WOODEN_AXE,  3.2);
        m.put(Material.STONE_AXE,   3.2);
        m.put(Material.IRON_AXE,    3.1);
        m.put(Material.GOLDEN_AXE,  3.2);
        m.put(Material.DIAMOND_AXE,   3.0);
        m.put(Material.NETHERITE_AXE, 2.9);
        m.put(Material.TRIDENT, 2.9);
        Material mace = Material.getMaterial("MACE");
        if (mace != null) m.put(mace, 3.4);
        BASE_SPEED = Collections.unmodifiableMap(m);
    }

    // Vanilla ARMOR attribute value per piece (ADD_NUMBER over base 0)
    private static final Map<Material, Double> BASE_ARMOR = Map.ofEntries(
            Map.entry(Material.LEATHER_HELMET, 1.0),
            Map.entry(Material.LEATHER_CHESTPLATE, 3.0),
            Map.entry(Material.LEATHER_LEGGINGS, 2.0),
            Map.entry(Material.LEATHER_BOOTS, 1.0),
            Map.entry(Material.CHAINMAIL_HELMET, 2.0),
            Map.entry(Material.CHAINMAIL_CHESTPLATE, 5.0),
            Map.entry(Material.CHAINMAIL_LEGGINGS, 4.0),
            Map.entry(Material.CHAINMAIL_BOOTS, 1.0),
            Map.entry(Material.IRON_HELMET, 2.0),
            Map.entry(Material.IRON_CHESTPLATE, 6.0),
            Map.entry(Material.IRON_LEGGINGS, 5.0),
            Map.entry(Material.IRON_BOOTS, 2.0),
            Map.entry(Material.GOLDEN_HELMET, 2.0),
            Map.entry(Material.GOLDEN_CHESTPLATE, 5.0),
            Map.entry(Material.GOLDEN_LEGGINGS, 3.0),
            Map.entry(Material.GOLDEN_BOOTS, 1.0),
            Map.entry(Material.DIAMOND_HELMET, 3.0),
            Map.entry(Material.DIAMOND_CHESTPLATE, 8.0),
            Map.entry(Material.DIAMOND_LEGGINGS, 6.0),
            Map.entry(Material.DIAMOND_BOOTS, 3.0),
            Map.entry(Material.NETHERITE_HELMET, 3.0),
            Map.entry(Material.NETHERITE_CHESTPLATE, 8.0),
            Map.entry(Material.NETHERITE_LEGGINGS, 6.0),
            Map.entry(Material.NETHERITE_BOOTS, 3.0)
    );

    private VanillaStats() {}

    public static double baseDamage(Material material) {
        return BASE_DAMAGE.getOrDefault(material, 1.0);
    }

    public static double baseSpeed(Material material) {
        return BASE_SPEED.getOrDefault(material, 2.4);
    }

    public static double baseArmor(Material material) {
        return BASE_ARMOR.getOrDefault(material, 1.0);
    }
}
