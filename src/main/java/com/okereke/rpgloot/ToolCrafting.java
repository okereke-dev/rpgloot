package com.okereke.rpgloot;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Resolves craftable tools to RPGLoot types and rolls craft rarity capped by material tier
 * so players cannot spam the crafting table for Legendary tools.
 */
public final class ToolCrafting {

    public enum MaterialTier {
        WOOD, STONE, GOLD, IRON, DIAMOND, NETHERITE
    }

    private final RPGLootPlugin plugin;
    private final ItemRarityService rarityService;
    private final Logger logger;
    private final Random random = new Random();
    private final Map<Rarity, Integer> craftWeights = new EnumMap<>(Rarity.class);
    private final Map<MaterialTier, Rarity> maxByTier = new EnumMap<>(MaterialTier.class);

    public ToolCrafting(RPGLootPlugin plugin, ItemRarityService rarityService) {
        this.plugin = plugin;
        this.rarityService = rarityService;
        this.logger = plugin.getLogger();
        reload(plugin.getConfig());
    }

    public void reload(FileConfiguration config) {
        craftWeights.clear();
        for (Rarity r : Rarity.values()) {
            craftWeights.put(r, Math.max(0, config.getInt("tool-crafting.weights." + r.name().toLowerCase(Locale.ROOT), defaultWeight(r))));
        }
        maxByTier.clear();
        ConfigurationSection section = config.getConfigurationSection("tool-crafting.max-rarity-by-tier");
        for (MaterialTier tier : MaterialTier.values()) {
            String key = tier.name().toLowerCase(Locale.ROOT);
            String raw = section != null ? section.getString(key) : null;
            maxByTier.put(tier, parseRarity(raw, defaultMax(tier)));
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("tool-crafting.enabled", true);
    }

    /** True if this material is a pickaxe, shovel, hoe, fishing rod, or axe (tool or combat material). */
    public static boolean isToolOrAxeMaterial(Material material) {
        if (material == null || material.isAir()) return false;
        String name = material.name();
        if (name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")) return true;
        if (name.endsWith("_AXE")) return true;
        return material == Material.FISHING_ROD;
    }

    /**
     * WeaponType for crafting/chest conversion of tools. Axes always become {@link WeaponType#AXE_TOOL}.
     */
    public static WeaponType resolveToolType(Material material) {
        if (material == null) return null;
        if (material.name().endsWith("_AXE")) return WeaponType.AXE_TOOL;
        WeaponType type = WeaponType.of(material);
        if (type != null && type.isTool()) return type;
        return null;
    }

    public static MaterialTier tierOf(Material material) {
        String name = material.name();
        if (name.startsWith("WOODEN_")) return MaterialTier.WOOD;
        if (name.startsWith("STONE_")) return MaterialTier.STONE;
        if (name.startsWith("GOLDEN_")) return MaterialTier.GOLD;
        if (name.startsWith("IRON_")) return MaterialTier.IRON;
        if (name.startsWith("DIAMOND_")) return MaterialTier.DIAMOND;
        if (name.startsWith("NETHERITE_")) return MaterialTier.NETHERITE;
        if (material == Material.FISHING_ROD) return MaterialTier.WOOD;
        return MaterialTier.WOOD;
    }

    public Rarity rollCraftRarity(Material material) {
        Rarity max = maxByTier.getOrDefault(tierOf(material), Rarity.COMMON);
        int total = 0;
        for (Rarity r : Rarity.values()) {
            if (r.ordinal() > max.ordinal()) continue;
            total += craftWeights.getOrDefault(r, 0);
        }
        if (total <= 0) return Rarity.COMMON;

        int roll = random.nextInt(total);
        int cumulative = 0;
        for (Rarity r : Rarity.values()) {
            if (r.ordinal() > max.ordinal()) continue;
            cumulative += craftWeights.getOrDefault(r, 0);
            if (roll < cumulative) return r;
        }
        return Rarity.COMMON;
    }

    /**
     * Applies RPGLoot tool rarity to a vanilla (or upgraded) tool stack.
     * @param preserveRarity if non-null, keep this rarity (e.g. smithing upgrade of an existing RPGLoot tool)
     */
    public ItemStack toRpgTool(ItemStack stack, Rarity preserveRarity) {
        if (stack == null || stack.getType().isAir()) return stack;
        WeaponType type = resolveToolType(stack.getType());
        if (type == null) return stack;

        Rarity rarity = preserveRarity != null ? preserveRarity : rollCraftRarity(stack.getType());
        // Never exceed craft max for freshly rolled crafts; preserved rarity from loot may be higher after smithing.
        if (preserveRarity == null) {
            Rarity max = maxByTier.getOrDefault(tierOf(stack.getType()), Rarity.COMMON);
            if (rarity.ordinal() > max.ordinal()) rarity = max;
        }

        ItemStack copy = stack.clone();
        return rarityService.applyRarity(copy, rarity, type);
    }

    private static int defaultWeight(Rarity r) {
        return switch (r) {
            case COMMON -> 55;
            case UNCOMMON -> 30;
            case RARE -> 12;
            case HERO -> 3;
            case LEGENDARY -> 0;
        };
    }

    private static Rarity defaultMax(MaterialTier tier) {
        return switch (tier) {
            case WOOD, STONE, GOLD, IRON -> Rarity.RARE;
            case DIAMOND, NETHERITE -> Rarity.HERO;
        };
    }

    private Rarity parseRarity(String raw, Rarity fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Rarity.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid tool-crafting max rarity '" + raw + "' — using " + fallback);
            return fallback;
        }
    }
}
