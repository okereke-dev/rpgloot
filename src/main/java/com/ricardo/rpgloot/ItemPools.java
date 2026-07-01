package com.ricardo.rpgloot;

import org.bukkit.Material;

import java.util.List;

/**
 * Shared item material pools.
 *
 * Tiered weapon/armor pools use a CEILING model: mobs can drop any material
 * from the worst up to the ceiling of their tier — never above it.
 *
 * T1 — Overworld basic   → up to Iron
 * T2 — Overworld structure (Pillagers, Vindicators…) → up to Golden
 * T3 — Nether            → up to Diamond
 * T4 — End / unlimited   → up to Netherite (full pool)
 */
public final class ItemPools {

    // ── Full pools (T4 / unrestricted) ────────────────────────────────────

    public static final List<Material> WEAPONS = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.WOODEN_AXE,   Material.STONE_AXE,   Material.IRON_AXE,
            Material.GOLDEN_AXE,   Material.DIAMOND_AXE,  Material.NETHERITE_AXE,
            Material.TRIDENT, Material.MACE, Material.BOW, Material.CROSSBOW);

    public static final List<Material> ARMOR = List.of(
            Material.LEATHER_HELMET,    Material.LEATHER_CHESTPLATE,    Material.LEATHER_LEGGINGS,    Material.LEATHER_BOOTS,
            Material.CHAINMAIL_HELMET,  Material.CHAINMAIL_CHESTPLATE,  Material.CHAINMAIL_LEGGINGS,  Material.CHAINMAIL_BOOTS,
            Material.IRON_HELMET,       Material.IRON_CHESTPLATE,       Material.IRON_LEGGINGS,       Material.IRON_BOOTS,
            Material.GOLDEN_HELMET,     Material.GOLDEN_CHESTPLATE,     Material.GOLDEN_LEGGINGS,     Material.GOLDEN_BOOTS,
            Material.DIAMOND_HELMET,    Material.DIAMOND_CHESTPLATE,    Material.DIAMOND_LEGGINGS,    Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET,  Material.NETHERITE_CHESTPLATE,  Material.NETHERITE_LEGGINGS,  Material.NETHERITE_BOOTS);

    public static final List<Material> TOOLS = List.of(
            Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
            Material.IRON_SHOVEL,  Material.DIAMOND_SHOVEL,  Material.NETHERITE_SHOVEL,
            Material.IRON_HOE,     Material.DIAMOND_HOE,     Material.NETHERITE_HOE,
            Material.FISHING_ROD);

    // ── Tiered general weapon pools (all weapon types, ceiling per tier) ──

    /** T1 ceiling: Iron. No gold/diamond/netherite. Mace excluded (end-game item). */
    public static final List<Material> WEAPONS_T1 = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.WOODEN_AXE,   Material.STONE_AXE,   Material.IRON_AXE,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT);

    /** T2 ceiling: Golden. Structure-hostile mobs. */
    public static final List<Material> WEAPONS_T2 = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD,
            Material.WOODEN_AXE,   Material.STONE_AXE,   Material.IRON_AXE,   Material.GOLDEN_AXE,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT);

    /** T3 ceiling: Diamond. Nether mobs. Mace added here. */
    public static final List<Material> WEAPONS_T3 = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD,
            Material.WOODEN_AXE,   Material.STONE_AXE,   Material.IRON_AXE,
            Material.GOLDEN_AXE,   Material.DIAMOND_AXE,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.MACE);

    // T4 = WEAPONS (full pool above)

    // ── Tiered armor pools ────────────────────────────────────────────────

    /** T1 ceiling: Iron armor. */
    public static final List<Material> ARMOR_T1 = List.of(
            Material.LEATHER_HELMET,   Material.LEATHER_CHESTPLATE,   Material.LEATHER_LEGGINGS,   Material.LEATHER_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.IRON_HELMET,      Material.IRON_CHESTPLATE,      Material.IRON_LEGGINGS,      Material.IRON_BOOTS);

    /** T2 ceiling: Golden armor. */
    public static final List<Material> ARMOR_T2 = List.of(
            Material.LEATHER_HELMET,   Material.LEATHER_CHESTPLATE,   Material.LEATHER_LEGGINGS,   Material.LEATHER_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.IRON_HELMET,      Material.IRON_CHESTPLATE,      Material.IRON_LEGGINGS,      Material.IRON_BOOTS,
            Material.GOLDEN_HELMET,    Material.GOLDEN_CHESTPLATE,    Material.GOLDEN_LEGGINGS,    Material.GOLDEN_BOOTS);

    /** T3 ceiling: Diamond armor. */
    public static final List<Material> ARMOR_T3 = List.of(
            Material.LEATHER_HELMET,   Material.LEATHER_CHESTPLATE,   Material.LEATHER_LEGGINGS,   Material.LEATHER_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.IRON_HELMET,      Material.IRON_CHESTPLATE,      Material.IRON_LEGGINGS,      Material.IRON_BOOTS,
            Material.GOLDEN_HELMET,    Material.GOLDEN_CHESTPLATE,    Material.GOLDEN_LEGGINGS,    Material.GOLDEN_BOOTS,
            Material.DIAMOND_HELMET,   Material.DIAMOND_CHESTPLATE,   Material.DIAMOND_LEGGINGS,   Material.DIAMOND_BOOTS);

    // T4 = ARMOR (full pool above)

    // ── Mob-specific weapon TYPE pools (single-type, no material filtering needed) ──

    public static final List<Material> SWORDS   = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD);
    public static final List<Material> SWORDS_T1 = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD);
    public static final List<Material> SWORDS_T2 = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD);
    public static final List<Material> SWORDS_T3 = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD);

    public static final List<Material> AXES     = List.of(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE);
    public static final List<Material> AXES_T1  = List.of(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE);
    public static final List<Material> AXES_T2  = List.of(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE);
    public static final List<Material> AXES_T3  = List.of(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE);

    public static final List<Material> RANGED    = List.of(Material.BOW);
    public static final List<Material> CROSSBOWS = List.of(Material.CROSSBOW);
    public static final List<Material> TRIDENTS  = List.of(Material.TRIDENT);

    /** Piglins are thematically gold-focused — pool stays fixed regardless of tier. */
    public static final List<Material> PIGLIN    = List.of(
            Material.GOLDEN_SWORD, Material.GOLDEN_AXE, Material.CROSSBOW);

    // ── Pooled constant used by StructureLootListener ─────────────────────
    public static final List<Material> WEAPONS_NO_NETHERITE = WEAPONS_T3;

    private ItemPools() {}
}
