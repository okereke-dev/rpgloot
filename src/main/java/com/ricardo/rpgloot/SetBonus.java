package com.ricardo.rpgloot;

/**
 * Defines each named set, its thematic bonus stat, and base values per rarity.
 *
 * Piece scaling (applied to base value):
 *   2 pieces → 30%
 *   3 pieces → 55%
 *   4 pieces → 75%
 *   5 pieces → 100% + visual special effect
 */
public enum SetBonus {

    //                   Display name    Stat                    Com   Unc   Rar   Her   Leg
    SHADOWVEIL  ("Shadowveil",  BonusStat.DODGE_CHANCE,    new double[]{2.0, 3.5, 5.5,  8.0, 11.0}),
    IRONBOUND   ("Ironbound",   BonusStat.DAMAGE_REDUCTION,new double[]{2.0, 3.5, 5.5,  8.0, 11.0}),
    DAWNBREAKER ("Dawnbreaker", BonusStat.CRIT_CHANCE,     new double[]{2.0, 3.5, 5.5,  8.0, 11.0}),
    TIDECALLER  ("Tidecaller",  BonusStat.LIFESTEAL,       new double[]{0.5, 1.0, 2.0,  3.5,  5.0}),
    EMBERCLAW   ("Emberclaw",   BonusStat.BLEEDING,        new double[]{5.0, 8.0,12.0, 16.0, 20.0}),
    STORMWARDEN ("Stormwarden", BonusStat.SPEED_BOOST,     new double[]{2.0, 3.5, 5.0,  7.0, 10.0}),
    VOIDWALKER  ("Voidwalker",  BonusStat.XP_BOOST,        new double[]{5.0, 8.0,12.0, 18.0, 25.0}),
    GILDED      ("Gilded",      BonusStat.LUCK_BOOST,      new double[]{0.3, 0.5, 0.8,  1.2,  1.8});

    // Multipliers for 2, 3, 4, 5 pieces
    private static final double[] SCALING = {0.30, 0.55, 0.75, 1.00};

    private final String displayName;
    private final BonusStat bonusStat;
    private final double[]  baseValues; // indexed by Rarity.ordinal()

    SetBonus(String displayName, BonusStat bonusStat, double[] baseValues) {
        this.displayName = displayName;
        this.bonusStat   = bonusStat;
        this.baseValues  = baseValues;
    }

    public String    getDisplayName() { return displayName; }
    public BonusStat getBonusStat()   { return bonusStat; }

    /** Base value at 5 pieces for the given rarity. */
    public double getBaseValue(Rarity rarity) {
        return baseValues[rarity.ordinal()];
    }

    /** Actual bonus value for a given rarity and piece count (2–5). */
    public double getValueForPieces(Rarity rarity, int pieces) {
        if (pieces < 2 || pieces > 5) return 0;
        return getBaseValue(rarity) * SCALING[pieces - 2];
    }

    /** Preview value at each piece count for lore display. */
    public String previewLine(Rarity rarity, int pieces) {
        double val = getValueForPieces(rarity, pieces);
        String formatted = val < 10
                ? String.format("%.1f", val)
                : String.valueOf((int) Math.round(val));
        String suffix = pieces == 5 ? " + Special" : "";
        return pieces + " pcs: +" + formatted + bonusStat.getUnit() + suffix;
    }

    public static SetBonus fromName(String name) {
        if (name == null) return null;
        for (SetBonus sb : values()) {
            if (sb.displayName.equalsIgnoreCase(name)) return sb;
        }
        return null;
    }
}
