package com.ricardo.rpgloot;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {

    public static NamespacedKey RARITY;
    public static NamespacedKey BONUS_STATS;
    public static NamespacedKey WEAPON_NAME;
    public static NamespacedKey ITEM_CATEGORY;
    public static NamespacedKey SET_NAME;
    public static NamespacedKey ARTIFACT_ID;
    public static NamespacedKey ACTIVE_SET_RARITY;

    private Keys() {}

    public static void init(Plugin plugin) {
        RARITY            = new NamespacedKey(plugin, "rarity");
        BONUS_STATS       = new NamespacedKey(plugin, "bonus_stats");
        WEAPON_NAME       = new NamespacedKey(plugin, "weapon_name");
        ITEM_CATEGORY     = new NamespacedKey(plugin, "item_category");
        SET_NAME          = new NamespacedKey(plugin, "set_name");
        ARTIFACT_ID       = new NamespacedKey(plugin, "artifact_id");
        // Written on the PLAYER (not an item) by SetTracker while a full 5-piece set is
        // active; value is the active set's Rarity name. Absent when no full set is active.
        // Public convention other plugins (e.g. RPGMood achievements) can read via PDC.
        ACTIVE_SET_RARITY = new NamespacedKey(plugin, "active_set_rarity");
    }
}
