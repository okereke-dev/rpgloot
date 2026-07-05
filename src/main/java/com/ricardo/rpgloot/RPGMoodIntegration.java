package com.ricardo.rpgloot;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

/**
 * Optional integration with RPGMood: if a mob was scaled by RPGMood, it carries a
 * "rpgmood:level" PersistentDataContainer tag. This reads that plain, cross-plugin PDC
 * key directly — no dependency on RPGMood's classes, no presence check needed, and no
 * effect at all if RPGMood isn't installed (the tag simply won't exist).
 */
public final class RPGMoodIntegration {

    private static final NamespacedKey LEVEL_KEY = new NamespacedKey("rpgmood", "level");

    private RPGMoodIntegration() {}

    /** The mob's RPGMood difficulty level, or null if it wasn't scaled by RPGMood. */
    public static Integer getMobLevel(LivingEntity entity) {
        return entity.getPersistentDataContainer().get(LEVEL_KEY, PersistentDataType.INTEGER);
    }

    /**
     * The minimum rarity floor for the given mob level, based on config's
     * rpgmood-integration.level-thresholds (rarity name -> minimum level), or null if
     * no threshold is met.
     */
    public static Rarity getRarityFloor(int mobLevel, ConfigurationSection thresholdsSection) {
        if (thresholdsSection == null) {
            return null;
        }

        Rarity floor = null;
        for (String key : thresholdsSection.getKeys(false)) {
            Rarity rarity;
            try {
                rarity = Rarity.valueOf(key.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                continue;
            }

            int threshold = thresholdsSection.getInt(key, Integer.MAX_VALUE);
            if (mobLevel >= threshold && (floor == null || rarity.ordinal() > floor.ordinal())) {
                floor = rarity;
            }
        }
        return floor;
    }
}
