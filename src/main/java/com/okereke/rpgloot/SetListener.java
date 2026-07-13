package com.okereke.rpgloot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SetListener implements Listener {

    private final RPGLootPlugin plugin;
    private final SetTracker tracker;
    // Debounce: only one pending recalc task per player at a time
    private final Map<UUID, Integer> pendingTasks = new HashMap<>();

    public SetListener(RPGLootPlugin plugin, SetTracker tracker) {
        this.plugin  = plugin;
        this.tracker = tracker;
    }

    // Armor equipped / unequipped via inventory click — only fire for relevant slots
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        boolean isArmorSlot  = event.getSlotType() == InventoryType.SlotType.ARMOR;
        boolean isPlayerInv  = event.getView().getType() == InventoryType.CRAFTING;
        boolean isQuickBar   = event.getSlotType() == InventoryType.SlotType.QUICKBAR;
        if (!isArmorSlot && !isPlayerInv && !isQuickBar) return;
        scheduleRecalculate(player, 1L);
    }

    // Weapon swap (main hand change)
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        scheduleRecalculate(event.getPlayer(), 1L);
    }

    // Restore state on login
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleRecalculate(event.getPlayer(), 5L);
    }

    // Restore after respawn (equipment is cleared on death)
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        scheduleRecalculate(event.getPlayer(), 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelPending(event.getPlayer().getUniqueId());
        tracker.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        tracker.remove(event.getEntity().getUniqueId());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Cancels any pending recalc task and schedules a fresh one after delayTicks. */
    private void scheduleRecalculate(Player player, long delayTicks) {
        cancelPending(player.getUniqueId());
        int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingTasks.remove(player.getUniqueId());
            if (player.isOnline()) recalculateAndNotify(player);
        }, delayTicks).getTaskId();
        pendingTasks.put(player.getUniqueId(), taskId);
    }

    private void cancelPending(UUID uuid) {
        Integer taskId = pendingTasks.remove(uuid);
        if (taskId != null) plugin.getServer().getScheduler().cancelTask(taskId);
    }

    public void cancelAllPending() {
        pendingTasks.values().forEach(id -> plugin.getServer().getScheduler().cancelTask(id));
        pendingTasks.clear();
    }

    private void recalculateAndNotify(Player player) {
        SetTracker.ActiveSet before = tracker.getActiveSet(player);
        tracker.recalculate(player);
        SetTracker.ActiveSet after = tracker.getActiveSet(player);

        // Notify player when a set activates or changes piece count
        if (after != null && (before == null || !before.equals(after))) {
            String pieces = after.pieces() + "/5";
            String bonusVal = formatBonus(after);
            Component msg = Component.text()
                    .append(Component.text("[RPGLoot] ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text(after.bonus().getDisplayName() + " Set ", after.rarity().getColor()).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text("(" + pieces + ") — +" + bonusVal + " " + after.bonus().getBonusStat().getLabel(),
                            NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .build();
            player.sendActionBar(msg);
        } else if (before != null && after == null) {
            player.sendActionBar(Component.text("[RPGLoot] Set bonus deactivated", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
    }

    private String formatBonus(SetTracker.ActiveSet set) {
        double val = set.value();
        return val < 10
                ? String.format("%.1f", val) + set.bonus().getBonusStat().getUnit()
                : ((int) Math.round(val)) + set.bonus().getBonusStat().getUnit();
    }
}
