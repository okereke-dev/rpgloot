package com.ricardo.rpgloot;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum Rarity {

    COMMON(TextColor.color(0x9D9D9D), "Common", 1.05, 1.05, 1.0, 1.0, 0, 50),
    UNCOMMON(NamedTextColor.YELLOW, "Uncommon", 1.05, 1.10, 1.00, 1.05, 1, 30),
    RARE(TextColor.color(0xAA00AA), "Rare", 1.10, 1.20, 1.00, 1.10, 1, 13),
    HERO(NamedTextColor.GREEN, "Hero", 1.20, 1.30, 1.05, 1.15, 1, 5),
    LEGENDARY(NamedTextColor.GOLD, "Legendary", 1.30, 1.40, 1.10, 1.20, 2, 2);

    private final TextColor color;
    private final String displayName;
    private final double minDamageMultiplier;
    private final double maxDamageMultiplier;
    private final double minSpeedMultiplier;
    private final double maxSpeedMultiplier;
    private final int bonusStatCount;
    private final int dropWeight;

    Rarity(TextColor color, String displayName,
           double minDamageMultiplier, double maxDamageMultiplier,
           double minSpeedMultiplier, double maxSpeedMultiplier,
           int bonusStatCount, int dropWeight) {
        this.color = color;
        this.displayName = displayName;
        this.minDamageMultiplier = minDamageMultiplier;
        this.maxDamageMultiplier = maxDamageMultiplier;
        this.minSpeedMultiplier = minSpeedMultiplier;
        this.maxSpeedMultiplier = maxSpeedMultiplier;
        this.bonusStatCount = bonusStatCount;
        this.dropWeight = dropWeight;
    }

    public TextColor getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMinDamageMultiplier() {
        return minDamageMultiplier;
    }

    public double getMaxDamageMultiplier() {
        return maxDamageMultiplier;
    }

    public double getMinSpeedMultiplier() {
        return minSpeedMultiplier;
    }

    public double getMaxSpeedMultiplier() {
        return maxSpeedMultiplier;
    }

    public int getBonusStatCount() {
        return bonusStatCount;
    }

    public int getDropWeight() {
        return dropWeight;
    }
}
