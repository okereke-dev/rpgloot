package com.ricardo.rpgloot;

public enum BonusStat {

    // Sword & Axe
    LIFESTEAL("Lifesteal", "%", new double[]{1, 3}, new double[]{3, 6}, new double[]{6, 10}, new double[]{8, 12}),
    CRIT_CHANCE("Crit Chance", "%", new double[]{2, 5}, new double[]{5, 10}, new double[]{10, 15}, new double[]{15, 20}),
    KNOCKBACK_BOOST("Knockback", " pts", new double[]{1, 1}, new double[]{1, 2}, new double[]{2, 3}, new double[]{3, 4}),
    // Value = proc chance %. Damage per tick is derived from weapon base damage × rarity factor.
    BLEEDING("Bleeding", "%", new double[]{20, 30}, new double[]{30, 45}, new double[]{45, 60}, new double[]{60, 75}),

    // Trident
    RIPTIDE_SPEED("Riptide Speed", "%", new double[]{5, 10}, new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 40}),
    LIGHTNING_CHANCE("Lightning Strike", "%", new double[]{3, 6}, new double[]{6, 12}, new double[]{12, 18}, new double[]{18, 25}),

    // Mace
    SMASH_RADIUS("Smash Radius", " blk", new double[]{1, 2}, new double[]{2, 3}, new double[]{3, 4}, new double[]{4, 5}),
    FALL_DAMAGE_BONUS("Fall Damage Bonus", "%", new double[]{10, 20}, new double[]{20, 35}, new double[]{35, 50}, new double[]{50, 70}),

    // Bow
    ARROW_DAMAGE("Arrow Damage", "%", new double[]{5, 10}, new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 40}),
    FLAME_CHANCE("Flame Chance", "%", new double[]{5, 10}, new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 40}),
    ARROW_PUNCH("Arrow Punch", " pts", new double[]{1, 1}, new double[]{1, 2}, new double[]{2, 3}, new double[]{3, 4}),
    MULTISHOT_CHANCE("Multishot", "%", new double[]{5, 10}, new double[]{10, 15}, new double[]{15, 25}, new double[]{25, 35}),

    // Crossbow
    PIERCING_CHANCE("Piercing", "%", new double[]{5, 10}, new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 40}),
    CHARGE_SPEED("Charge Speed", "%", new double[]{5, 10}, new double[]{10, 15}, new double[]{15, 25}, new double[]{25, 35});

    private final String label;
    private final String unit;
    private final double[] uncommonRange;
    private final double[] rareRange;
    private final double[] heroRange;
    private final double[] legendaryRange;

    BonusStat(String label, String unit, double[] uncommonRange, double[] rareRange, double[] heroRange, double[] legendaryRange) {
        this.label = label;
        this.unit = unit;
        this.uncommonRange = uncommonRange;
        this.rareRange = rareRange;
        this.heroRange = heroRange;
        this.legendaryRange = legendaryRange;
    }

    public String getLabel() {
        return label;
    }

    public String getUnit() {
        return unit;
    }

    public double[] getRangeFor(Rarity rarity) {
        return switch (rarity) {
            case UNCOMMON -> uncommonRange;
            case RARE -> rareRange;
            case HERO -> heroRange;
            case LEGENDARY -> legendaryRange;
            default -> new double[]{0, 0};
        };
    }
}
