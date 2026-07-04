package com.ricardo.rpgloot;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Lightweight, per-player persistent counters for the leaderboard — backed by a single
 * playerstats.yml file (not player PDC, to stay portable across Bukkit API versions).
 * Saved synchronously on every increment; these events are rare (Legendary finds, full
 * set completions) so the I/O cost is negligible.
 */
public final class PlayerStats {

    private final Logger logger;
    private final File file;
    private final YamlConfiguration data;

    private final Map<UUID, Integer> legendariesFound = new HashMap<>();
    private final Map<UUID, Integer> setsCompleted = new HashMap<>();
    private final Map<UUID, String> lastKnownName = new HashMap<>();

    public PlayerStats(File dataFolder, Logger logger) {
        this.logger = logger;
        this.file = new File(dataFolder, "playerstats.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        for (String key : data.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                legendariesFound.put(uuid, data.getInt(key + ".legendaries-found", 0));
                setsCompleted.put(uuid, data.getInt(key + ".sets-completed", 0));
                lastKnownName.put(uuid, data.getString(key + ".name", "Unknown"));
            } catch (IllegalArgumentException e) {
                logger.warning("Skipping invalid UUID key in playerstats.yml: '" + key + "'");
            }
        }
    }

    public void incrementLegendariesFound(Player player) {
        UUID uuid = player.getUniqueId();
        legendariesFound.merge(uuid, 1, Integer::sum);
        lastKnownName.put(uuid, player.getName());
        save(uuid);
    }

    public void incrementSetsCompleted(Player player) {
        UUID uuid = player.getUniqueId();
        setsCompleted.merge(uuid, 1, Integer::sum);
        lastKnownName.put(uuid, player.getName());
        save(uuid);
    }

    public int getLegendariesFound(UUID uuid) { return legendariesFound.getOrDefault(uuid, 0); }
    public int getSetsCompleted(UUID uuid)    { return setsCompleted.getOrDefault(uuid, 0); }
    public String nameFor(UUID uuid)          { return lastKnownName.getOrDefault(uuid, "Unknown"); }

    public List<Map.Entry<UUID, Integer>> topLegendariesFound(int limit) {
        return legendariesFound.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .toList();
    }

    public List<Map.Entry<UUID, Integer>> topSetsCompleted(int limit) {
        return setsCompleted.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .toList();
    }

    private void save(UUID uuid) {
        String key = uuid.toString();
        data.set(key + ".legendaries-found", legendariesFound.getOrDefault(uuid, 0));
        data.set(key + ".sets-completed", setsCompleted.getOrDefault(uuid, 0));
        data.set(key + ".name", lastKnownName.get(uuid));
        try {
            data.save(file);
        } catch (IOException e) {
            logger.warning("Failed to save playerstats.yml: " + e.getMessage());
        }
    }
}
