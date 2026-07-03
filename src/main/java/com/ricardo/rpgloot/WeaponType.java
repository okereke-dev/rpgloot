package com.ricardo.rpgloot;

import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum WeaponType {

    // ── Weapons ───────────────────────────────────────────────────────────
    SWORD(
            EnumSet.of(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                    Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD),
            EnumSet.of(BonusStat.LIFESTEAL, BonusStat.CRIT_CHANCE, BonusStat.KNOCKBACK_BOOST, BonusStat.BLEEDING),
            Category.WEAPON
    ),
    AXE(
            EnumSet.of(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                    Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE),
            EnumSet.of(BonusStat.LIFESTEAL, BonusStat.CRIT_CHANCE, BonusStat.KNOCKBACK_BOOST, BonusStat.BLEEDING),
            Category.WEAPON
    ),
    TRIDENT(
            EnumSet.of(Material.TRIDENT),
            EnumSet.of(BonusStat.LIFESTEAL, BonusStat.CRIT_CHANCE, BonusStat.RIPTIDE_SPEED, BonusStat.LIGHTNING_CHANCE),
            Category.WEAPON
    ),
    MACE(
            buildMaceMaterials(),
            EnumSet.of(BonusStat.LIFESTEAL, BonusStat.KNOCKBACK_BOOST, BonusStat.SMASH_RADIUS, BonusStat.FALL_DAMAGE_BONUS),
            Category.WEAPON
    ),
    BOW(
            EnumSet.of(Material.BOW),
            EnumSet.of(BonusStat.ARROW_DAMAGE, BonusStat.FLAME_CHANCE, BonusStat.ARROW_PUNCH, BonusStat.MULTISHOT_CHANCE),
            Category.WEAPON
    ),
    CROSSBOW(
            EnumSet.of(Material.CROSSBOW),
            EnumSet.of(BonusStat.ARROW_DAMAGE, BonusStat.PIERCING_CHANCE, BonusStat.MULTISHOT_CHANCE, BonusStat.CHARGE_SPEED),
            Category.WEAPON
    ),

    // ── Armor ─────────────────────────────────────────────────────────────
    HELMET(
            EnumSet.of(Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
                    Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET),
            EnumSet.of(BonusStat.THORNS_CHANCE, BonusStat.NIGHT_VISION_CHANCE, BonusStat.HEALTH_BOOST, BonusStat.DODGE_CHANCE),
            Category.ARMOR
    ),
    CHESTPLATE(
            EnumSet.of(Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
                    Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE),
            EnumSet.of(BonusStat.DAMAGE_REDUCTION, BonusStat.THORNS_CHANCE, BonusStat.HEALTH_BOOST, BonusStat.DODGE_CHANCE),
            Category.ARMOR
    ),
    LEGGINGS(
            EnumSet.of(Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
                    Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS),
            EnumSet.of(BonusStat.SPEED_BOOST, BonusStat.DODGE_CHANCE, BonusStat.HEALTH_BOOST, BonusStat.DAMAGE_REDUCTION),
            Category.ARMOR
    ),
    BOOTS(
            EnumSet.of(Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
                    Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS),
            EnumSet.of(BonusStat.FALL_REDUCTION, BonusStat.SPEED_BOOST, BonusStat.HEALTH_BOOST, BonusStat.DODGE_CHANCE),
            Category.ARMOR
    ),

    // ── Tools ─────────────────────────────────────────────────────────────
    AXE_TOOL(
            EnumSet.of(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                    Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE),
            EnumSet.of(BonusStat.FORTUNE_BOOST, BonusStat.XP_BOOST, BonusStat.AUTO_SMELT_CHANCE),
            Category.TOOL
    ),
    PICKAXE(
            EnumSet.of(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                    Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE),
            EnumSet.of(BonusStat.FORTUNE_BOOST, BonusStat.XP_BOOST, BonusStat.AUTO_SMELT_CHANCE),
            Category.TOOL
    ),
    SHOVEL(
            EnumSet.of(Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                    Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL),
            EnumSet.of(BonusStat.FORTUNE_BOOST, BonusStat.XP_BOOST, BonusStat.AUTO_SMELT_CHANCE),
            Category.TOOL
    ),
    HOE(
            EnumSet.of(Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
                    Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE),
            EnumSet.of(BonusStat.REPLANT_CHANCE, BonusStat.FORTUNE_BOOST, BonusStat.XP_BOOST),
            Category.TOOL
    ),
    FISHING_ROD(
            EnumSet.of(Material.FISHING_ROD),
            EnumSet.of(BonusStat.LUCK_BOOST, BonusStat.DOUBLE_CATCH_CHANCE, BonusStat.FORTUNE_BOOST),
            Category.TOOL
    );

    public enum Category { WEAPON, ARMOR, TOOL }

    private final Set<Material> materials;
    private final Set<BonusStat> bonusPool;
    private final Category category;

    WeaponType(Set<Material> materials, Set<BonusStat> bonusPool, Category category) {
        this.materials = materials;
        this.bonusPool = bonusPool;
        this.category = category;
    }

    public Set<Material> getMaterials() { return materials; }
    public Set<BonusStat> getBonusPool() { return bonusPool; }
    public Category getCategory() { return category; }

    @SuppressWarnings("unchecked")
    private static Set<Material> buildMaceMaterials() {
        Material mace = Material.getMaterial("MACE");
        return mace != null ? EnumSet.of(mace) : Collections.emptySet();
    }

    public boolean isWeapon() { return category == Category.WEAPON; }
    public boolean isArmor()  { return category == Category.ARMOR; }
    public boolean isTool()   { return category == Category.TOOL; }

    /** Resolves material to type. For AXE materials, always returns AXE (combat). Use explicit type for AXE_TOOL. */
    public static WeaponType of(Material material) {
        for (WeaponType type : values()) {
            // Skip AXE_TOOL so axe materials resolve to AXE by default
            if (type == AXE_TOOL) continue;
            if (type.materials.contains(material)) return type;
        }
        return null;
    }

    public static boolean isSupported(Material material) {
        return of(material) != null;
    }
}
