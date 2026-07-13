package com.okereke.rpgloot;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion — exposes the player's active set bonus and leaderboard
 * stats for use in scoreboards, tab list, and GUIs. Only registered if PlaceholderAPI
 * is installed.
 *
 * Placeholders:
 *   %rpgloot_active_set%              -> set display name, or "None"
 *   %rpgloot_active_set_pieces%       -> "3/5", or "0/5" if no active set
 *   %rpgloot_active_set_bonus%        -> formatted bonus value, e.g. "+6.1% Dodge Chance"
 *   %rpgloot_stat_legendaries_found%  -> total Legendary items found by this player
 *   %rpgloot_stat_sets_completed%     -> total times this player completed a full 5-piece set
 */
public final class RPGLootExpansion extends PlaceholderExpansion {

    private final RPGLootPlugin plugin;

    public RPGLootExpansion(RPGLootPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "rpgloot";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Okereke";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        SetTracker.ActiveSet active = plugin.getSetTracker().getActiveSet(player);

        return switch (params) {
            case "active_set" -> active != null ? active.bonus().getDisplayName() : "None";
            case "active_set_pieces" -> active != null ? active.pieces() + "/5" : "0/5";
            case "active_set_bonus" -> active != null
                    ? String.format("+%.1f%s %s", active.value(),
                            active.bonus().getBonusStat().getUnit(), active.bonus().getBonusStat().getLabel())
                    : "None";
            case "stat_legendaries_found" -> String.valueOf(plugin.getPlayerStats().getLegendariesFound(player.getUniqueId()));
            case "stat_sets_completed" -> String.valueOf(plugin.getPlayerStats().getSetsCompleted(player.getUniqueId()));
            default -> null;
        };
    }
}
