package com.ricardo.rpgloot;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {

    public static NamespacedKey RARITY;
    public static NamespacedKey BONUS_STATS;

    private Keys() {
    }

    public static void init(Plugin plugin) {
        RARITY = new NamespacedKey(plugin, "rarity");
        BONUS_STATS = new NamespacedKey(plugin, "bonus_stats");
    }
}
