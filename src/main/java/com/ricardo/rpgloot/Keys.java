package com.ricardo.rpgloot;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {

    public static NamespacedKey RARITY;
    public static NamespacedKey BONUS_STATS;
    public static NamespacedKey WEAPON_NAME;
    public static NamespacedKey ITEM_CATEGORY;
    public static NamespacedKey SET_NAME;

    private Keys() {}

    public static void init(Plugin plugin) {
        RARITY        = new NamespacedKey(plugin, "rarity");
        BONUS_STATS   = new NamespacedKey(plugin, "bonus_stats");
        WEAPON_NAME   = new NamespacedKey(plugin, "weapon_name");
        ITEM_CATEGORY = new NamespacedKey(plugin, "item_category");
        SET_NAME      = new NamespacedKey(plugin, "set_name");
    }
}
