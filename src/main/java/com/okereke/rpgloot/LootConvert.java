package com.okereke.rpgloot;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Converts vanilla weapons, armor, and tools already present in a loot list into RPGLoot items
 * (same material). Axes become {@link WeaponType#AXE_TOOL}. Shared by structure chests, vaults,
 * and archaeology so type rules stay in one place.
 */
public final class LootConvert {

    private LootConvert() {}

    /**
     * In-place conversion of convertible stacks in {@code loot}. Skips air, already-RPGLoot stacks,
     * and non-gear materials. Returns the number of stacks converted.
     */
    public static int convertGear(List<ItemStack> loot, Rarity maxRarity,
                                  ItemRarityService rarityService, RarityRoller roller) {
        if (loot == null || maxRarity == null) return 0;
        int converted = 0;
        for (int i = 0; i < loot.size(); i++) {
            ItemStack result = convertOne(loot.get(i), maxRarity, rarityService, roller);
            if (result != null) {
                loot.set(i, result);
                converted++;
            }
        }
        return converted;
    }

    /**
     * True if this stack is a vanilla weapon, armor, or tool material RPGLoot can convert
     * (axes count as woodcutting tools). Does not check whether the stack is already RPGLoot.
     */
    public static boolean isConvertibleGear(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        if (stack.getType().name().endsWith("_AXE")) return true;
        WeaponType type = WeaponType.of(stack.getType());
        return type != null && (type.isWeapon() || type.isArmor() || type.isTool());
    }

    /**
     * Converts a single stack if it is convertible vanilla gear; otherwise returns {@code null}
     * (caller should leave the original alone).
     */
    public static ItemStack convertOne(ItemStack stack, Rarity maxRarity,
                                       ItemRarityService rarityService, RarityRoller roller) {
        if (!isConvertibleGear(stack)) return null;
        if (rarityService.getRarity(stack) != null) return null;

        WeaponType type;
        if (stack.getType().name().endsWith("_AXE")) {
            type = WeaponType.AXE_TOOL;
        } else {
            type = WeaponType.of(stack.getType());
            if (type == null) return null;
        }

        Rarity rarity = roller.rollWithMax(maxRarity);
        return rarityService.applyRarity(stack, rarity, type);
    }
}
