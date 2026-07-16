package com.okereke.rpgloot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Admin debug helpers for virgin structure loot containers (still have a {@link LootTable}
 * attached — not yet opened). Particles mark nearby virgin chests; look-at shows action-bar
 * status; generate events can ping players with debug mode on.
 */
public final class ChestLootDebug implements Listener, Runnable {

    private static final int DEFAULT_SCAN_RADIUS = 32;
    private static final int MAX_SCAN_RADIUS = 64;
    private static final int LOOK_RANGE = 6;

    private final RPGLootPlugin plugin;
    private final StructureLootListener structureLoot;
    private final Set<UUID> enabled = new HashSet<>();
    private BukkitTask task;

    public ChestLootDebug(RPGLootPlugin plugin, StructureLootListener structureLoot) {
        this.plugin = plugin;
        this.structureLoot = structureLoot;
    }

    public boolean isEnabled(Player player) {
        return enabled.contains(player.getUniqueId());
    }

    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        if (enabled.contains(id)) {
            enabled.remove(id);
            stopTaskIfIdle();
            return false;
        }
        enabled.add(id);
        startTask();
        return true;
    }

    public void disable(Player player) {
        enabled.remove(player.getUniqueId());
        stopTaskIfIdle();
    }

    /** Scans loaded blocks in a cube radius for virgin Lootable containers. */
    public List<VirginChest> scan(Location center, int radius) {
        int r = Math.min(MAX_SCAN_RADIUS, Math.max(1, radius));
        List<VirginChest> found = new ArrayList<>();
        if (center.getWorld() == null) return found;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        for (int x = cx - r; x <= cx + r; x++) {
            for (int y = Math.max(center.getWorld().getMinHeight(), cy - r);
                 y <= Math.min(center.getWorld().getMaxHeight() - 1, cy + r); y++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    if (!center.getWorld().isChunkLoaded(x >> 4, z >> 4)) continue;
                    Block block = center.getWorld().getBlockAt(x, y, z);
                    VirginChest virgin = inspect(block);
                    if (virgin != null) found.add(virgin);
                }
            }
        }
        return found;
    }

    public VirginChest inspect(Block block) {
        if (block == null) return null;
        BlockState state = block.getState();
        if (!(state instanceof Lootable lootable)) return null;
        LootTable table = lootable.getLootTable();
        if (table == null) return null;
        String key = table.getKey().getKey();
        boolean recognized = structureLoot.isRecognizedTable(key);
        return new VirginChest(block.getLocation(), key, recognized);
    }

    /** Chat ping when a recognized structure chest generates loot (ensure-gear path). */
    public void notifyGenerate(Location loc, String tableKey, int gearBeforeConvert, boolean ensureUsed) {
        if (enabled.isEmpty()) return;
        Component msg = Component.text()
                .append(Component.text("[RPGLoot debug] ", NamedTextColor.DARK_AQUA))
                .append(Component.text(tableKey, NamedTextColor.AQUA))
                .append(Component.text(" @ " + formatBlock(loc), NamedTextColor.GRAY))
                .append(Component.text(" gearStacks=" + gearBeforeConvert, NamedTextColor.YELLOW))
                .append(Component.text(ensureUsed ? " (ensure-gear)" : " (vanilla/extra)", NamedTextColor.DARK_GRAY))
                .build();
        for (UUID id : enabled) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()
                    && p.getWorld().equals(loc.getWorld())
                    && p.getLocation().distanceSquared(loc) <= 64 * 64) {
                p.sendMessage(msg);
            }
        }
    }

    @Override
    public void run() {
        if (enabled.isEmpty()) {
            stopTaskIfIdle();
            return;
        }
        for (UUID id : Set.copyOf(enabled)) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                enabled.remove(id);
                continue;
            }
            markNearby(player);
            showLookTarget(player);
        }
        stopTaskIfIdle();
    }

    private void markNearby(Player player) {
        int radius = plugin.getConfig().getInt("structure-loot.debug.particle-radius", 24);
        radius = Math.min(MAX_SCAN_RADIUS, Math.max(8, radius));
        List<VirginChest> chests = scan(player.getLocation(), radius);
        Particle.DustOptions dustRecognized = new Particle.DustOptions(Color.fromRGB(80, 220, 120), 1.2f);
        Particle.DustOptions dustOther = new Particle.DustOptions(Color.fromRGB(180, 180, 80), 1.0f);
        Particle dust = dustParticle();
        for (VirginChest chest : chests) {
            Location at = chest.location().clone().add(0.5, 0.6, 0.5);
            player.spawnParticle(
                    dust,
                    at,
                    4,
                    0.15, 0.15, 0.15,
                    0,
                    chest.recognized() ? dustRecognized : dustOther
            );
        }
    }

    /** Paper 1.20.5+ renamed REDSTONE → DUST; resolve at runtime for both compile and server. */
    private static Particle dustParticle() {
        try {
            return Particle.valueOf("DUST");
        } catch (IllegalArgumentException e) {
            return Particle.valueOf("REDSTONE");
        }
    }

    private void showLookTarget(Player player) {
        RayTraceResult hit = player.rayTraceBlocks(LOOK_RANGE);
        if (hit == null || hit.getHitBlock() == null) return;
        VirginChest virgin = inspect(hit.getHitBlock());
        BlockState state = hit.getHitBlock().getState();
        if (virgin != null) {
            player.sendActionBar(Component.text()
                    .append(Component.text("Virgin loot ", NamedTextColor.GREEN))
                    .append(Component.text(virgin.tableKey(), NamedTextColor.AQUA))
                    .append(Component.text(virgin.recognized() ? " [RPGLoot]" : " [other table]", NamedTextColor.DARK_GRAY))
                    .build());
        } else if (state instanceof Lootable) {
            player.sendActionBar(Component.text("Loot container — already generated / no table", NamedTextColor.RED));
        }
    }

    private void startTask() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this, 10L, 10L);
    }

    private void stopTaskIfIdle() {
        if (!enabled.isEmpty()) return;
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        disable(event.getPlayer());
    }

    public static int defaultScanRadius() {
        return DEFAULT_SCAN_RADIUS;
    }

    public static int clampRadius(int radius) {
        return Math.min(MAX_SCAN_RADIUS, Math.max(1, radius));
    }

    private static String formatBlock(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public record VirginChest(Location location, String tableKey, boolean recognized) {}
}
