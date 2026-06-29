package com.ricardo.rpgloot;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Random;
import java.util.logging.Logger;

public final class RarityRoller {

    private final FileConfiguration config;
    private final Random random = new Random();
    private final Logger logger;

    public RarityRoller(FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
        validateWeights();
    }

    public Rarity roll() {
        int roll = random.nextInt(100);
        int cumulative = 0;
        for (Rarity rarity : Rarity.values()) {
            cumulative += getWeight(rarity);
            if (roll < cumulative) {
                return rarity;
            }
        }
        return Rarity.COMMON;
    }

    private int getWeight(Rarity rarity) {
        String key = "rarity-weights." + rarity.name().toLowerCase();
        return config.getInt(key, rarity.getDropWeight());
    }

    private void validateWeights() {
        int total = 0;
        for (Rarity rarity : Rarity.values()) {
            total += getWeight(rarity);
        }
        if (total != 100) {
            logger.warning("rarity-weights in config.yml add up to " + total + " instead of 100. Drop chances will be skewed.");
        }
    }
}
