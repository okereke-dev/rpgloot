package com.okereke.rpgloot;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;

/**
 * Hand-authored, one-of-a-kind items with a fixed name, fixed lore, and fixed bonus stats
 * (no randomized ranges). Each is tied to a specific boss with a small independent chance
 * to replace that boss's normal Legendary drop.
 */
public enum Artifact {

    WARDENS_MAUL(
            "The Warden's Maul", Material.NETHERITE_SWORD, WeaponType.MACE,
            List.of(new RolledStat(BonusStat.SMASH_RADIUS, 5.0), new RolledStat(BonusStat.LIFESTEAL, 12.0)),
            List.of("Forged in the deep dark,", "it hums with sonic resonance."),
            EntityType.WARDEN, 0.02),

    WITHERBANE(
            "Witherbane", Material.NETHERITE_SWORD, WeaponType.SWORD,
            List.of(new RolledStat(BonusStat.BLEEDING, 60.0), new RolledStat(BonusStat.CRIT_CHANCE, 20.0)),
            List.of("Tempered in withering flame,", "it never truly stops burning."),
            EntityType.WITHER, 0.02),

    TIDELORDS_TRIDENT(
            "Tidelord's Trident", Material.TRIDENT, WeaponType.TRIDENT,
            List.of(new RolledStat(BonusStat.RIPTIDE_SPEED, 40.0), new RolledStat(BonusStat.LIGHTNING_CHANCE, 25.0)),
            List.of("The deep ocean answers", "to its call."),
            EntityType.ELDER_GUARDIAN, 0.02),

    DRAGONFANG_BOW(
            "Dragonfang Bow", Material.BOW, WeaponType.BOW,
            List.of(new RolledStat(BonusStat.ARROW_DAMAGE, 40.0), new RolledStat(BonusStat.MULTISHOT_CHANCE, 35.0)),
            List.of("Carved from a fang", "older than the End itself."),
            EntityType.ENDER_DRAGON, 0.02);

    private final String displayName;
    private final Material material;
    private final WeaponType weaponType;
    private final List<RolledStat> fixedStats;
    private final List<String> flavorText;
    private final EntityType bossType;
    private final double dropChance;

    Artifact(String displayName, Material material, WeaponType weaponType,
             List<RolledStat> fixedStats, List<String> flavorText,
             EntityType bossType, double dropChance) {
        this.displayName = displayName;
        this.material     = material;
        this.weaponType   = weaponType;
        this.fixedStats   = fixedStats;
        this.flavorText   = flavorText;
        this.bossType     = bossType;
        this.dropChance   = dropChance;
    }

    public String getDisplayName()        { return displayName; }
    public Material getMaterial()         { return material; }
    public WeaponType getWeaponType()     { return weaponType; }
    public List<RolledStat> getFixedStats() { return fixedStats; }
    public List<String> getFlavorText()   { return flavorText; }
    public EntityType getBossType()       { return bossType; }
    public double getDropChance()         { return dropChance; }

    public static Artifact fromName(String name) {
        if (name == null) return null;
        for (Artifact artifact : values()) {
            if (artifact.name().equalsIgnoreCase(name) || artifact.displayName.equalsIgnoreCase(name)) {
                return artifact;
            }
        }
        return null;
    }
}
