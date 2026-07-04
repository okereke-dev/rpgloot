package com.ricardo.rpgloot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only inventory GUI showing all 8 sets and their piece-scaling values.
 * Clicking any set icon cycles the shared rarity view for all icons at once.
 */
public final class SetsMenu implements InventoryHolder {

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 17};

    // Indexed by SetBonus.ordinal()
    private static final Material[] ICONS = {
            Material.PHANTOM_MEMBRANE, // SHADOWVEIL
            Material.SHIELD,           // IRONBOUND
            Material.GOLDEN_SWORD,     // DAWNBREAKER
            Material.TRIDENT,          // TIDECALLER
            Material.NETHERITE_AXE,    // EMBERCLAW
            Material.FEATHER,          // STORMWARDEN
            Material.ENDER_PEARL,      // VOIDWALKER
            Material.GOLD_INGOT,       // GILDED
    };

    private final Inventory inventory;
    private Rarity viewRarity = Rarity.LEGENDARY;

    public SetsMenu() {
        inventory = Bukkit.createInventory(this, 27, Component.text("◈ RPGLoot Sets", NamedTextColor.GOLD));
        fillBorder();
        renderSets();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Advances the shared rarity view and re-renders all set icons. */
    public void cycleRarity() {
        Rarity[] values = Rarity.values();
        viewRarity = values[(viewRarity.ordinal() + 1) % values.length];
        renderSets();
    }

    public static boolean isSetSlot(int slot) {
        for (int s : SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    private void fillBorder() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);
        for (int i = 0; i < 9; i++) inventory.setItem(i, pane);
        for (int i = 18; i < 27; i++) inventory.setItem(i, pane);
    }

    private void renderSets() {
        SetBonus[] sets = SetBonus.values();
        for (int i = 0; i < sets.length; i++) {
            inventory.setItem(SLOTS[i], buildIcon(sets[i]));
        }
    }

    private ItemStack buildIcon(SetBonus set) {
        ItemStack item = new ItemStack(ICONS[set.ordinal()]);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("◈ " + set.getDisplayName(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(set.getBonusStat().getLabel(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(viewRarity.getDisplayName() + " values:", viewRarity.getColor())
                .decoration(TextDecoration.ITALIC, false));
        for (int pieces = 2; pieces <= 5; pieces++) {
            lore.add(Component.text("  " + set.previewLine(viewRarity, pieces), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Click to cycle rarity", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
