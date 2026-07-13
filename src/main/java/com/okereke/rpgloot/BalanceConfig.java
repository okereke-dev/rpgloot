package com.okereke.rpgloot;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads rarity damage/speed multiplier ranges and bonus-stat value ranges from config.yml,
 * falling back to the compiled-in defaults (Rarity / BonusStat enum values) for any tier or
 * stat not present in config. This lets server admins tune power level without recompiling,
 * while shipping with identical behavior to previous versions out of the box.
 */
public final class BalanceConfig {

    private final Logger logger;
    private final Map<Rarity, double[]> damageRanges = new EnumMap<>(Rarity.class);
    private final Map<Rarity, double[]> speedRanges  = new EnumMap<>(Rarity.class);
    private final Map<Rarity, double[]> armorRanges  = new EnumMap<>(Rarity.class);
    private final Map<BonusStat, double[][]> statRanges = new EnumMap<>(BonusStat.class);
    private boolean statsEnabled = true;

    public BalanceConfig(FileConfiguration config, Logger logger) {
        this.logger = logger;
        load(config);
    }

    public void reload(FileConfiguration config) {
        damageRanges.clear();
        speedRanges.clear();
        armorRanges.clear();
        statRanges.clear();
        load(config);
    }

    private void load(FileConfiguration config) {
        statsEnabled = config.getBoolean("stats-enabled", true);

        ConfigurationSection rarityMults = config.getConfigurationSection("rarity-multipliers");
        for (Rarity rarity : Rarity.values()) {
            double[] defaultDamage = {rarity.getMinDamageMultiplier(), rarity.getMaxDamageMultiplier()};
            double[] defaultSpeed  = {rarity.getMinSpeedMultiplier(), rarity.getMaxSpeedMultiplier()};
            double[] defaultArmor  = {rarity.getMinArmorMultiplier(), rarity.getMaxArmorMultiplier()};

            damageRanges.put(rarity, readPair(rarityMults, rarity.name(), "damage", defaultDamage));
            speedRanges.put(rarity, readPair(rarityMults, rarity.name(), "speed", defaultSpeed));
            armorRanges.put(rarity, readPair(rarityMults, rarity.name(), "armor", defaultArmor));
        }

        ConfigurationSection statSection = config.getConfigurationSection("bonus-stat-ranges");
        for (BonusStat stat : BonusStat.values()) {
            double[][] defaults = {
                    stat.getRangeFor(Rarity.UNCOMMON),
                    stat.getRangeFor(Rarity.RARE),
                    stat.getRangeFor(Rarity.HERO),
                    stat.getRangeFor(Rarity.LEGENDARY)
            };
            double[][] ranges = new double[4][];
            ranges[0] = readPair(statSection, stat.name(), "uncommon", defaults[0]);
            ranges[1] = readPair(statSection, stat.name(), "rare", defaults[1]);
            ranges[2] = readPair(statSection, stat.name(), "hero", defaults[2]);
            ranges[3] = readPair(statSection, stat.name(), "legendary", defaults[3]);
            statRanges.put(stat, ranges);
        }
    }

    private double[] readPair(ConfigurationSection root, String entryKey, String field, double[] fallback) {
        if (root == null) return fallback;
        ConfigurationSection entry = root.getConfigurationSection(entryKey);
        if (entry == null) return fallback;
        List<?> raw = entry.getList(field);
        if (raw == null || raw.size() != 2) return fallback;
        try {
            double min = ((Number) raw.get(0)).doubleValue();
            double max = ((Number) raw.get(1)).doubleValue();
            return new double[]{min, max};
        } catch (ClassCastException e) {
            logger.warning("Invalid " + field + " range for " + entryKey + " — using default.");
            return fallback;
        }
    }

    /** When false, RPGLoot items get rarity name/color/lore only — no combat stats or set bonuses. */
    public boolean isStatsEnabled() { return statsEnabled; }

    public double[] getDamageRange(Rarity rarity) { return damageRanges.get(rarity); }
    public double[] getSpeedRange(Rarity rarity)  { return speedRanges.get(rarity); }
    public double[] getArmorRange(Rarity rarity)  { return armorRanges.get(rarity); }

    public double[] getStatRange(BonusStat stat, Rarity rarity) {
        double[][] ranges = statRanges.get(stat);
        return switch (rarity) {
            case UNCOMMON  -> ranges[0];
            case RARE      -> ranges[1];
            case HERO      -> ranges[2];
            case LEGENDARY -> ranges[3];
            default        -> new double[]{0, 0};
        };
    }
}
