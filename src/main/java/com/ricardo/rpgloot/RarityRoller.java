package com.ricardo.rpgloot;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

public final class RarityRoller {

    private final Map<Rarity, Integer> normalizedWeights = new EnumMap<>(Rarity.class);
    private final Random random = new Random();

    public RarityRoller(FileConfiguration config, Logger logger) {
        // Read raw weights from config
        int total = 0;
        for (Rarity r : Rarity.values()) {
            int w = config.getInt("rarity-weights." + r.name().toLowerCase(), r.getDropWeight());
            normalizedWeights.put(r, Math.max(0, w));
            total += Math.max(0, w);
        }

        if (total == 0) {
            logger.severe("All rarity-weights are 0 — resetting to defaults.");
            for (Rarity r : Rarity.values()) normalizedWeights.put(r, r.getDropWeight());
        } else if (total != 100) {
            logger.warning("rarity-weights sum to " + total + " instead of 100 — weights have been normalized automatically.");
            // Normalize so they sum to 100
            final int rawTotal = total;
            for (Rarity r : Rarity.values()) {
                normalizedWeights.put(r, (int) Math.round(normalizedWeights.get(r) * 100.0 / rawTotal));
            }
            // Fix any rounding drift on the most common rarity
            int normalized = normalizedWeights.values().stream().mapToInt(Integer::intValue).sum();
            if (normalized != 100) {
                normalizedWeights.merge(Rarity.COMMON, 100 - normalized, Integer::sum);
            }
        }
    }

    public Rarity roll() {
        return rollWithMax(Rarity.LEGENDARY);
    }

    public Rarity rollWithMin(Rarity minRarity) {
        int total = 0;
        for (Rarity r : Rarity.values()) {
            if (r.ordinal() >= minRarity.ordinal()) total += normalizedWeights.get(r);
        }
        if (total == 0) return minRarity;

        int roll = random.nextInt(total);
        int cumulative = 0;
        for (Rarity r : Rarity.values()) {
            if (r.ordinal() < minRarity.ordinal()) continue;
            cumulative += normalizedWeights.get(r);
            if (roll < cumulative) return r;
        }
        return minRarity;
    }

    /** Roll rarity within a specific range [min, max] inclusive. */
    public Rarity rollWithRange(Rarity minRarity, Rarity maxRarity) {
        int total = 0;
        for (Rarity r : Rarity.values()) {
            if (r.ordinal() >= minRarity.ordinal() && r.ordinal() <= maxRarity.ordinal())
                total += normalizedWeights.get(r);
        }
        if (total == 0) return minRarity;

        int roll = random.nextInt(total);
        int cumulative = 0;
        for (Rarity r : Rarity.values()) {
            if (r.ordinal() < minRarity.ordinal() || r.ordinal() > maxRarity.ordinal()) continue;
            cumulative += normalizedWeights.get(r);
            if (roll < cumulative) return r;
        }
        return minRarity;
    }

    public Rarity rollWithMax(Rarity maxRarity) {
        int total = 0;
        for (Rarity r : Rarity.values()) {
            if (r.ordinal() <= maxRarity.ordinal()) total += normalizedWeights.get(r);
        }
        if (total == 0) return Rarity.COMMON;

        int roll = random.nextInt(total);
        int cumulative = 0;
        for (Rarity r : Rarity.values()) {
            if (r.ordinal() > maxRarity.ordinal()) continue;
            cumulative += normalizedWeights.get(r);
            if (roll < cumulative) return r;
        }
        return Rarity.COMMON;
    }
}
