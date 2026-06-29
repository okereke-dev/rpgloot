package com.ricardo.rpgloot;

public record RolledStat(BonusStat stat, double value) {

    public String serialize() {
        return stat.name() + ":" + value;
    }

    public static RolledStat deserialize(String raw) {
        String[] parts = raw.split(":");
        return new RolledStat(BonusStat.valueOf(parts[0]), Double.parseDouble(parts[1]));
    }
}
