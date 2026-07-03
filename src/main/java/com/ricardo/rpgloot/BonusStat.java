package com.ricardo.rpgloot;

public enum BonusStat {

    // ── Sword & Axe (combat) ──────────────────────────────────────────────
    LIFESTEAL("Lifesteal", "%", new double[]{1, 3}, new double[]{3, 6}, new double[]{6, 10}, new double[]{8, 12}),
    CRIT_CHANCE("Crit Chance", "%", new double[]{2, 5}, new double[]{5, 10}, new double[]{10, 15}, new double[]{15, 20}),
    KNOCKBACK_BOOST("Knockback", " pts", new double[]{1, 1}, new double[]{1, 2}, new double[]{2, 3}, new double[]{3, 4}),
    // Value = proc chance %. Damage per tick derived from weapon base damage × rarity factor.
    BLEEDING("Bleeding", "%", new double[]{20, 30}, new double[]{30, 45}, new double[]{45, 60}, new double[]{60, 75}),

    // ── Trident ───────────────────────────────────────────────────────────
    RIPTIDE_SPEED("Riptide Speed", "%", new double[]{5, 10}, new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 40}),
    LIGHTNING_CHANCE("Lightning Strike", "%", new double[]{3, 6}, new double[]{6, 12}, new double[]{12, 18}, new double[]{18, 25}),

    // ── Mace ──────────────────────────────────────────────────────────────
    SMASH_RADIUS("Smash Radius", " blk", new double[]{1, 2}, new double[]{2, 3}, new double[]{3, 4}, new double[]{4, 5}),
    FALL_DAMAGE_BONUS("Fall Damage Bonus", "%", new double[]{10, 20}, new double[]{20, 35}, new double[]{35, 50}, new double[]{50, 70}),

    // ── Bow ───────────────────────────────────────────────────────────────
    ARROW_DAMAGE("Arrow Damage", "%", new double[]{5, 10}, new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 40}),
    FLAME_CHANCE("Flame Chance", "%", new double[]{5, 10}, new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 40}),
    ARROW_PUNCH("Arrow Punch", " pts", new double[]{1, 1}, new double[]{1, 2}, new double[]{2, 3}, new double[]{3, 4}),
    MULTISHOT_CHANCE("Multishot", "%", new double[]{5, 10}, new double[]{10, 15}, new double[]{15, 25}, new double[]{25, 35}),

    // ── Crossbow ──────────────────────────────────────────────────────────
    PIERCING_CHANCE("Piercing", "%", new double[]{5, 10}, new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 40}),
    CHARGE_SPEED("Charge Speed", "%", new double[]{5, 10}, new double[]{10, 15}, new double[]{15, 25}, new double[]{25, 35}),

    // ── Armor ─────────────────────────────────────────────────────────────
    THORNS_CHANCE("Thorns", "%", new double[]{5, 10}, new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 45}),
    DODGE_CHANCE("Dodge", "%", new double[]{3, 8}, new double[]{8, 15}, new double[]{15, 22}, new double[]{22, 30}),
    DAMAGE_REDUCTION("Damage Reduction", "%", new double[]{2, 5}, new double[]{5, 10}, new double[]{10, 15}, new double[]{15, 20}),
    HEALTH_BOOST("Health Boost", " hp", new double[]{1, 2}, new double[]{2, 3}, new double[]{3, 5}, new double[]{5, 8}),
    // Kept low — this is a passive item AttributeModifier that stacks across all worn pieces
    SPEED_BOOST("Speed Boost", "%", new double[]{1, 2}, new double[]{2, 4}, new double[]{4, 6}, new double[]{6, 8}),
    FALL_REDUCTION("Fall Reduction", "%", new double[]{10, 20}, new double[]{20, 35}, new double[]{35, 50}, new double[]{50, 70}),
    NIGHT_VISION_CHANCE("Night Vision", "%", new double[]{5, 10}, new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 45}),

    // ── Tools ─────────────────────────────────────────────────────────────
    FORTUNE_BOOST("Fortune Boost", "%", new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 40}, new double[]{40, 50}),
    XP_BOOST("XP Boost", "%", new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 40}, new double[]{40, 50}),
    AUTO_SMELT_CHANCE("Auto-Smelt", "%", new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 40}, new double[]{40, 50}),
    REPLANT_CHANCE("Replant", "%", new double[]{15, 25}, new double[]{25, 35}, new double[]{35, 48}, new double[]{48, 60}),
    LUCK_BOOST("Luck", " pts", new double[]{1, 1}, new double[]{1, 2}, new double[]{2, 3}, new double[]{3, 4}),
    DOUBLE_CATCH_CHANCE("Double Catch", "%", new double[]{10, 20}, new double[]{20, 35}, new double[]{35, 50}, new double[]{50, 65});

    private final String label;
    private final String unit;
    private final double[] uncommonRange;
    private final double[] rareRange;
    private final double[] heroRange;
    private final double[] legendaryRange;

    BonusStat(String label, String unit,
              double[] uncommonRange, double[] rareRange,
              double[] heroRange, double[] legendaryRange) {
        this.label = label;
        this.unit = unit;
        this.uncommonRange = uncommonRange;
        this.rareRange = rareRange;
        this.heroRange = heroRange;
        this.legendaryRange = legendaryRange;
    }

    public String getLabel() { return label; }
    public String getUnit()  { return unit; }

    public double[] getRangeFor(Rarity rarity) {
        return switch (rarity) {
            case UNCOMMON  -> uncommonRange;
            case RARE      -> rareRange;
            case HERO      -> heroRange;
            case LEGENDARY -> legendaryRange;
            default        -> new double[]{0, 0};
        };
    }
}
