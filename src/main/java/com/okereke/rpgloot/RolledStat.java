package com.okereke.rpgloot;

public record RolledStat(BonusStat stat, double value) {

    public String serialize() {
        return stat.name() + ":" + value;
    }

    public static RolledStat deserialize(String raw) {
        try {
            String[] parts = raw.split(":", 2);
            if (parts.length < 2) return null;
            BonusStat stat = BonusStat.valueOf(parts[0]);
            double value = Double.parseDouble(parts[1]);
            return new RolledStat(stat, value);
        } catch (IllegalArgumentException e) {
            // Unknown stat name (e.g. from a downgrade) or malformed number — skip silently
            return null;
        }
    }
}
