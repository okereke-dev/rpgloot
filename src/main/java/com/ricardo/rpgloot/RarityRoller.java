package com.ricardo.rpgloot;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Random;

public final class RarityRoller {

    private final FileConfiguration config;
    private final Random random = new Random();

    public RarityRoller(FileConfiguration config) {
        this.config = config;
    }

    public Rarity roll() {
        int totalWeight = 0;
        for (Rarity rarity : Rarity.values()) {
            totalWeight += getWeight(rarity);
        }

        int roll = random.nextInt(totalWeight);
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
}
