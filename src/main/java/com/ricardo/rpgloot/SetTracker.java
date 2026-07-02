package com.ricardo.rpgloot;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which set bonus (if any) is active per player and applies/removes
 * passive modifiers when the active set changes.
 *
 * A set requires 2–5 items sharing the same set name, rarity, AND material.
 * The set bonus scales with piece count and stacks additively with item stats.
 */
public final class SetTracker {

    /** Snapshot of a player's currently active set. */
    public record ActiveSet(SetBonus bonus, Rarity rarity, Material material, int pieces) {
        public double value() {
            return bonus.getValueForPieces(rarity, pieces);
        }
        public boolean isFull() {
            return pieces >= 5;
        }
    }

    private static final NamespacedKey SET_SPEED_KEY = new NamespacedKey("rpgloot", "set_speed");
    private static final NamespacedKey SET_LUCK_KEY  = new NamespacedKey("rpgloot", "set_luck");

    private final Map<UUID, ActiveSet> activeSets = new HashMap<>();

    // ── Public API ────────────────────────────────────────────────────────

    /** Recomputes and applies the active set for a player. Call after any equipment change. */
    public void recalculate(Player player) {
        removePassiveModifiers(player);

        if (!player.hasPermission("rpgloot.sets")) {
            activeSets.remove(player.getUniqueId());
            return;
        }

        ActiveSet best = findBestSet(player);

        if (best != null) {
            activeSets.put(player.getUniqueId(), best);
            applyPassiveModifiers(player, best);
        } else {
            activeSets.remove(player.getUniqueId());
        }
    }

    /** Returns the active set for a player, or null if none. */
    public ActiveSet getActiveSet(Player player) {
        return activeSets.get(player.getUniqueId());
    }

    /**
     * Returns the extra bonus value from the active set for a specific stat,
     * or 0 if the player has no active set or the set targets a different stat.
     */
    public double getSetBonus(Player player, BonusStat stat) {
        ActiveSet active = activeSets.get(player.getUniqueId());
        if (active == null || active.bonus().getBonusStat() != stat) return 0;
        return active.value();
    }

    /** Clears state when a player leaves. */
    public void remove(UUID uuid) {
        activeSets.remove(uuid);
    }

    // ── Set detection ─────────────────────────────────────────────────────

    private ActiveSet findBestSet(Player player) {
        List<ItemStack> equipped = getEquippedItems(player);

        // Group items by (setName:rarity:material)
        Map<String, List<ItemStack>> groups = new HashMap<>();
        for (ItemStack item : equipped) {
            String key = setKey(item);
            if (key != null) groups.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        ActiveSet best = null;
        for (Map.Entry<String, List<ItemStack>> entry : groups.entrySet()) {
            int count = Math.min(entry.getValue().size(), 5);
            if (count < 2) continue;

            ItemStack sample = entry.getValue().get(0);
            String setName = getSetName(sample);
            Rarity rarity  = getRarity(sample);
            SetBonus bonus  = SetBonus.fromName(setName);
            if (bonus == null || rarity == null) continue;

            if (best == null || count > best.pieces()) {
                best = new ActiveSet(bonus, rarity, sample.getType(), count);
            }
        }
        return best;
    }

    private List<ItemStack> getEquippedItems(Player player) {
        List<ItemStack> items = new ArrayList<>();
        PlayerInventory inv = player.getInventory();
        addIfNotEmpty(items, inv.getHelmet());
        addIfNotEmpty(items, inv.getChestplate());
        addIfNotEmpty(items, inv.getLeggings());
        addIfNotEmpty(items, inv.getBoots());
        addIfNotEmpty(items, inv.getItemInMainHand());
        return items;
    }

    private void addIfNotEmpty(List<ItemStack> list, ItemStack item) {
        if (item != null && item.getType() != Material.AIR) list.add(item);
    }

    // ── Passive modifier management ───────────────────────────────────────

    private void applyPassiveModifiers(Player player, ActiveSet set) {
        switch (set.bonus().getBonusStat()) {
            case SPEED_BOOST -> {
                var attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
                if (attr != null) {
                    double modifier = 0.1 * (set.value() / 100.0);
                    attr.addModifier(new AttributeModifier(
                            SET_SPEED_KEY, modifier, AttributeModifier.Operation.ADD_NUMBER));
                }
            }
            case LUCK_BOOST -> {
                var attr = player.getAttribute(Attribute.LUCK);
                if (attr != null) {
                    attr.addModifier(new AttributeModifier(
                            SET_LUCK_KEY, set.value(), AttributeModifier.Operation.ADD_NUMBER));
                }
            }
            default -> {}
        }
    }

    private void removePassiveModifiers(Player player) {
        removeKey(player, Attribute.MOVEMENT_SPEED, SET_SPEED_KEY);
        removeKey(player, Attribute.LUCK, SET_LUCK_KEY);
    }

    private void removeKey(Player player, Attribute attribute, NamespacedKey key) {
        var attr = player.getAttribute(attribute);
        if (attr == null) return;
        attr.getModifiers().stream()
                .filter(m -> m.getKey().equals(key))
                .toList()
                .forEach(attr::removeModifier);
    }

    // ── PDC helpers ───────────────────────────────────────────────────────

    private String setKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var pdc     = item.getItemMeta().getPersistentDataContainer();
        String name = pdc.get(Keys.SET_NAME, PersistentDataType.STRING);
        String rar  = pdc.get(Keys.RARITY,   PersistentDataType.STRING);
        if (name == null || rar == null) return null;
        return name + ":" + rar + ":" + item.getType().name();
    }

    private String getSetName(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(Keys.SET_NAME, PersistentDataType.STRING);
    }

    private Rarity getRarity(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.RARITY, PersistentDataType.STRING);
        try { return raw == null ? null : Rarity.valueOf(raw); }
        catch (IllegalArgumentException e) { return null; }
    }
}
