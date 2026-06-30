package com.ricardo.rpgloot;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

public enum WeaponType {

    SWORD(
            EnumSet.of(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                    Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD),
            EnumSet.of(BonusStat.LIFESTEAL, BonusStat.CRIT_CHANCE, BonusStat.KNOCKBACK_BOOST, BonusStat.SWEEP_BONUS)
    ),
    AXE(
            EnumSet.of(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                    Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE),
            EnumSet.of(BonusStat.LIFESTEAL, BonusStat.CRIT_CHANCE, BonusStat.KNOCKBACK_BOOST, BonusStat.SWEEP_BONUS)
    ),
    TRIDENT(
            EnumSet.of(Material.TRIDENT),
            EnumSet.of(BonusStat.LIFESTEAL, BonusStat.CRIT_CHANCE, BonusStat.RIPTIDE_SPEED, BonusStat.LIGHTNING_CHANCE)
    ),
    MACE(
            EnumSet.of(Material.MACE),
            EnumSet.of(BonusStat.LIFESTEAL, BonusStat.KNOCKBACK_BOOST, BonusStat.SMASH_RADIUS, BonusStat.FALL_DAMAGE_BONUS)
    ),
    BOW(
            EnumSet.of(Material.BOW),
            EnumSet.of(BonusStat.ARROW_DAMAGE, BonusStat.FLAME_CHANCE, BonusStat.ARROW_PUNCH, BonusStat.MULTISHOT_CHANCE)
    ),
    CROSSBOW(
            EnumSet.of(Material.CROSSBOW),
            EnumSet.of(BonusStat.ARROW_DAMAGE, BonusStat.PIERCING_CHANCE, BonusStat.MULTISHOT_CHANCE, BonusStat.CHARGE_SPEED)
    );

    private final Set<Material> materials;
    private final Set<BonusStat> bonusPool;

    WeaponType(Set<Material> materials, Set<BonusStat> bonusPool) {
        this.materials = materials;
        this.bonusPool = bonusPool;
    }

    public Set<Material> getMaterials() {
        return materials;
    }

    public Set<BonusStat> getBonusPool() {
        return bonusPool;
    }

    public static WeaponType of(Material material) {
        for (WeaponType type : values()) {
            if (type.materials.contains(material)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isSupported(Material material) {
        return of(material) != null;
    }
}
