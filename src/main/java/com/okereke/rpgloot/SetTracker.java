package com.okereke.rpgloot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

    private static final UUID SET_SPEED_UUID = UUID.nameUUIDFromBytes("rpgloot:set_speed".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final UUID SET_LUCK_UUID  = UUID.nameUUIDFromBytes("rpgloot:set_luck".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    private final Map<UUID, ActiveSet> activeSets = new HashMap<>();
    // Transient edge-detection so re-equipping an already-completed set doesn't double count
    private final java.util.Set<UUID> currentlyFullSets = new java.util.HashSet<>();
    private final PlayerStats playerStats;

    public SetTracker(PlayerStats playerStats) {
        this.playerStats = playerStats;
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Recomputes and applies the active set for a player. Call after any equipment change. */
    public void recalculate(Player player) {
        removePassiveModifiers(player);
        UUID uuid = player.getUniqueId();

        if (!player.hasPermission("rpgloot.sets")) {
            activeSets.remove(uuid);
            currentlyFullSets.remove(uuid);
            player.getPersistentDataContainer().remove(Keys.ACTIVE_SET_RARITY);
            return;
        }

        ActiveSet best = findBestSet(player);

        if (best != null) {
            activeSets.put(uuid, best);
            applyPassiveModifiers(player, best);
        } else {
            activeSets.remove(uuid);
        }

        boolean isFull = best != null && best.isFull();
        if (isFull && !currentlyFullSets.contains(uuid)) {
            playerStats.incrementSetsCompleted(player);
        }
        if (isFull) currentlyFullSets.add(uuid); else currentlyFullSets.remove(uuid);

        // Exposed for other plugins (e.g. RPGMood achievements) to read via PDC — no event,
        // no dependency, mirrors how RPGMood exposes a scaled mob's level.
        if (isFull) {
            player.getPersistentDataContainer().set(Keys.ACTIVE_SET_RARITY, PersistentDataType.STRING, best.rarity().name());
        } else {
            player.getPersistentDataContainer().remove(Keys.ACTIVE_SET_RARITY);
        }

        refreshAllSetLore(player, best);
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
        currentlyFullSets.remove(uuid);
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
                var attr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                if (attr != null) {
                    double modifier = 0.1 * (set.value() / 100.0);
                    attr.addModifier(new AttributeModifier(
                            SET_SPEED_UUID, "rpgloot:set_speed", modifier, AttributeModifier.Operation.ADD_NUMBER));
                }
            }
            case LUCK_BOOST -> {
                var attr = player.getAttribute(Attribute.GENERIC_LUCK);
                if (attr != null) {
                    attr.addModifier(new AttributeModifier(
                            SET_LUCK_UUID, "rpgloot:set_luck", set.value(), AttributeModifier.Operation.ADD_NUMBER));
                }
            }
            default -> {}
        }
    }

    private void removePassiveModifiers(Player player) {
        removeByUuid(player, Attribute.GENERIC_MOVEMENT_SPEED, SET_SPEED_UUID);
        removeByUuid(player, Attribute.GENERIC_LUCK, SET_LUCK_UUID);
    }

    private void removeByUuid(Player player, Attribute attribute, UUID uuid) {
        var attr = player.getAttribute(attribute);
        if (attr == null) return;
        attr.getModifiers().stream()
                .filter(m -> m.getUniqueId().equals(uuid))
                .toList()
                .forEach(attr::removeModifier);
    }

    // ── Material tier ─────────────────────────────────────────────────────

    /**
     * Maps an item's Material to its base material tier (e.g. DIAMOND_SWORD → DIAMOND,
     * IRON_HELMET → IRON). Items sharing the same tier count toward the same set group,
     * so a full diamond armor set + diamond sword all contribute to one set.
     * Single-material items (TRIDENT, BOW, etc.) return their own name as the tier.
     */
    static String getMaterialTier(Material mat) {
        String name = mat.name();
        for (String suffix : ITEM_SUFFIXES) {
            if (name.endsWith(suffix)) return name.substring(0, name.length() - suffix.length());
        }
        return name;
    }

    private static final List<String> ITEM_SUFFIXES = List.of(
            "_SWORD", "_AXE", "_PICKAXE", "_SHOVEL", "_HOE",
            "_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS");

    // ── PDC helpers ───────────────────────────────────────────────────────

    private String setKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var pdc     = item.getItemMeta().getPersistentDataContainer();
        String name = pdc.get(Keys.SET_NAME, PersistentDataType.STRING);
        String rar  = pdc.get(Keys.RARITY,   PersistentDataType.STRING);
        if (name == null || rar == null) return null;
        // Group by tier so DIAMOND_HELMET + DIAMOND_SWORD count as the same set
        return name + ":" + rar + ":" + getMaterialTier(item.getType());
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

    // ── Set lore highlighting ─────────────────────────────────────────────

    /**
     * Updates the set section in the lore of every equipped item.
     * The active piece-count line is highlighted in rarity color + bold;
     * all others revert to DARK_GRAY.
     * active may be null (deactivation — all lines go to DARK_GRAY).
     */
    private void refreshAllSetLore(Player player, ActiveSet active) {
        for (ItemStack item : getEquippedItems(player)) {
            if (item == null || !item.hasItemMeta()) continue;
            String setName = getSetName(item);
            if (setName == null) continue;
            Rarity rarity  = getRarity(item);
            SetBonus bonus = SetBonus.fromName(setName);
            if (rarity == null || bonus == null) continue;

            // Determine which piece line (if any) to highlight for this item
            int highlight = 0;
            if (active != null
                    && active.bonus() == bonus
                    && active.rarity() == rarity
                    && getMaterialTier(active.material()).equals(getMaterialTier(item.getType()))) {
                highlight = active.pieces();
            }
            updateSetLoreLines(item, bonus, rarity, highlight);
        }
    }

    /** Rewrites the 4 piece-preview lines inside the item's existing lore. */
    private void updateSetLoreLines(ItemStack item, SetBonus bonus, Rarity rarity, int activePieces) {
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) return;

        List<Component> updated = new ArrayList<>(lore);
        var plainSerializer = PlainTextComponentSerializer.plainText();

        for (int i = 0; i < updated.size(); i++) {
            String text = plainSerializer.serialize(updated.get(i));
            if (text.contains("◈") && text.contains(bonus.getDisplayName())) {
                // Lines i+1 through i+4 are the piece previews (2 pcs … 5 pcs)
                for (int p = 2; p <= 5; p++) {
                    int idx = i + (p - 1);
                    if (idx >= updated.size()) break;
                    boolean isActive = (p == activePieces);
                    String lineText = "  " + bonus.previewLine(rarity, p);
                    updated.set(idx, isActive
                            ? Component.text(lineText, rarity.getColor())
                                    .decoration(TextDecoration.BOLD, true)
                                    .decoration(TextDecoration.ITALIC, false)
                            : Component.text(lineText, NamedTextColor.DARK_GRAY)
                                    .decoration(TextDecoration.ITALIC, false));
                }
                break;
            }
        }

        meta.lore(updated);
        item.setItemMeta(meta);
    }
}
