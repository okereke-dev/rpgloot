package com.ricardo.rpgloot;

public enum BonusStat {

    LIFESTEAL("Lifesteal", "%", new double[]{1, 3}, new double[]{3, 6}, new double[]{6, 10}, new double[]{8, 12}),
    CRIT_CHANCE("Crit Chance", "%", new double[]{2, 5}, new double[]{5, 10}, new double[]{10, 15}, new double[]{15, 20}),
    KNOCKBACK_BOOST("Knockback", " pts", new double[]{1, 1}, new double[]{1, 2}, new double[]{2, 3}, new double[]{3, 4}),
    SWEEP_BONUS("Sweep Damage", "%", new double[]{5, 10}, new double[]{10, 20}, new double[]{20, 30}, new double[]{30, 40});

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
