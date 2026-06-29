package com.ricardo.rpgloot;

import java.util.Random;

public final class RarityRoller {

    private final Random random = new Random();

    public Rarity roll() {
        int totalWeight = 0;
        for (Rarity rarity : Rarity.values()) {
            totalWeight += rarity.getDropWeight();
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (Rarity rarity : Rarity.values()) {
            cumulative += rarity.getDropWeight();
            if (roll < cumulative) {
                return rarity;
            }
        }
        return Rarity.COMMON;
    }
}
