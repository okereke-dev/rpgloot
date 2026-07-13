package com.okereke.rpgloot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class ItemRarityService {

    private static final SetBonus[] SET_POOL = SetBonus.values();

    // LRU cache — avoids re-parsing the bonus-stat PDC string on every damage event
    @SuppressWarnings("serial")
    private final Map<String, List<RolledStat>> statCache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, List<RolledStat>> eldest) {
            return size() > 512;
        }
    };

    private final Random random = new Random();
    private final WeaponNameGenerator nameGenerator = new WeaponNameGenerator();
    private final Plugin plugin;
    private final BalanceConfig balanceConfig;

    public ItemRarityService(Plugin plugin, BalanceConfig balanceConfig) {
        this.plugin = plugin;
        this.balanceConfig = balanceConfig;
    }

    /** Clears the stat-string parse cache after a config reload. */
    public void clearStatCache() {
        statCache.clear();
    }

    public boolean isSupportedWeapon(Material material) {
        return WeaponType.isSupported(material);
    }

    public ItemStack applyRarity(ItemStack item, Rarity rarity) {
        return applyRarity(item, rarity, WeaponType.of(item.getType()), null);
    }

    public ItemStack applyRarity(ItemStack item, Rarity rarity, WeaponType type) {
        return applyRarity(item, rarity, type, null);
    }

    public ItemStack applyRarity(ItemStack item, Rarity rarity, WeaponType type, SetBonus forcedSet) {
        if (type == null) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Cosmetic-only mode: skip all combat stats and set bonuses, keep name/rarity/lore only
        boolean statsEnabled = balanceConfig.isStatsEnabled();

        // Compute all multipliers once so attributes and lore stay in sync
        double[] damageRange = statsEnabled ? balanceConfig.getDamageRange(rarity) : new double[]{1.0, 1.0};
        double[] speedRange  = statsEnabled ? balanceConfig.getSpeedRange(rarity)  : new double[]{1.0, 1.0};
        double primaryMult = randomBetween(damageRange[0], damageRange[1]);
        double speedMult   = randomBetween(speedRange[0], speedRange[1]);

        Material mat = item.getType();

        if (statsEnabled && type.isWeapon() && !isRanged(type)) {
            double damageBonus = VanillaStats.baseDamage(mat) * (primaryMult - 1.0);
            double speedBonus  = VanillaStats.baseSpeed(mat)  * (speedMult  - 1.0);
            if (damageBonus > 0) meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                    UUID.nameUUIDFromBytes("rpgloot_damage".getBytes(StandardCharsets.UTF_8)),
                    "rpgloot_damage", damageBonus, AttributeModifier.Operation.ADD_NUMBER));
            if (speedBonus > 0) meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(
                    UUID.nameUUIDFromBytes("rpgloot_speed".getBytes(StandardCharsets.UTF_8)),
                    "rpgloot_speed", speedBonus, AttributeModifier.Operation.ADD_NUMBER));
        } else if (statsEnabled && type.isArmor()) {
            double defenseBonus = VanillaStats.baseArmor(mat) * (primaryMult - 1.0);
            if (defenseBonus > 0) meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                    UUID.nameUUIDFromBytes("rpgloot_armor".getBytes(StandardCharsets.UTF_8)),
                    "rpgloot_armor", defenseBonus, AttributeModifier.Operation.ADD_NUMBER));
        }

        List<RolledStat> rolledStats = statsEnabled ? rollBonusStats(rarity, type) : List.of();
        applyPassiveAttributes(meta, rolledStats);

        if (type == WeaponType.AXE_TOOL) {
            meta.getPersistentDataContainer().set(Keys.ITEM_CATEGORY, PersistentDataType.STRING, "AXE_TOOL");
        }

        // Assign set — forced if specified, otherwise random for weapons/armor (not tools)
        // Skipped entirely in cosmetic-only mode since sets grant mechanical bonuses.
        SetBonus setBonus = !statsEnabled ? null
                : forcedSet != null ? forcedSet
                : (type.isWeapon() || type.isArmor()) ? SET_POOL[random.nextInt(SET_POOL.length)] : null;
        if (setBonus != null) {
            meta.getPersistentDataContainer().set(Keys.SET_NAME, PersistentDataType.STRING, setBonus.getDisplayName());
        }

        String itemName = nameGenerator.generate(type);
        meta.getPersistentDataContainer().set(Keys.RARITY,      PersistentDataType.STRING, rarity.name());
        meta.getPersistentDataContainer().set(Keys.BONUS_STATS, PersistentDataType.STRING, serializeStats(rolledStats));
        meta.getPersistentDataContainer().set(Keys.WEAPON_NAME, PersistentDataType.STRING, itemName);

        meta.displayName(Component.text(itemName, rarity.getColor()).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(rarity.getDisplayName(), rarity.getColor()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (type.isWeapon()) {
            if (isRanged(type)) {
                lore.add(Component.text("Projectile weapon", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            } else if (statsEnabled) {
                double damageBonus = VanillaStats.baseDamage(mat) * (primaryMult - 1.0);
                double speedBonus  = VanillaStats.baseSpeed(mat)  * (speedMult  - 1.0);
                lore.add(statLine("Attack Damage", primaryMult, damageBonus));
                if (speedBonus > 0) lore.add(statLine("Attack Speed", speedMult, speedBonus));
            }
        } else if (type.isArmor()) {
            if (statsEnabled) {
                double defenseBonus = VanillaStats.baseArmor(mat) * (primaryMult - 1.0);
                lore.add(statLine("Defense", primaryMult, defenseBonus));
            }
        } else {
            lore.add(Component.text(toolLabel(type), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }

        if (!rolledStats.isEmpty()) {
            lore.add(Component.empty());
            for (RolledStat rolled : rolledStats) {
                lore.add(bonusStatLine(rolled));
            }
        }

        if (setBonus != null) {
            lore.add(Component.empty());
            lore.add(Component.text("◈ " + setBonus.getDisplayName() + " Set", rarity.getColor())
                    .decoration(TextDecoration.ITALIC, false));
            for (int p = 2; p <= 5; p++) {
                lore.add(Component.text("  " + setBonus.previewLine(rarity, p), NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Applies a hand-authored Artifact — fixed name, lore, and bonus stats, no RNG.
     * Always rolls at the top of Legendary's damage/speed range and is stored as
     * Rarity.LEGENDARY internally, so it plays through the exact same combat/armor/tool
     * listeners as a normal Legendary item. The ARTIFACT_ID PDC key marks it distinctly.
     */
    public ItemStack applyArtifact(ItemStack item, Artifact artifact) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        WeaponType type = artifact.getWeaponType();
        Material mat = item.getType();

        double[] damageRange = balanceConfig.getDamageRange(Rarity.LEGENDARY);
        double[] speedRange  = balanceConfig.getSpeedRange(Rarity.LEGENDARY);
        double primaryMult = damageRange[1];
        double speedMult   = speedRange[1];

        if (type.isWeapon() && !isRanged(type)) {
            double damageBonus = VanillaStats.baseDamage(mat) * (primaryMult - 1.0);
            double speedBonus  = VanillaStats.baseSpeed(mat)  * (speedMult  - 1.0);
            if (damageBonus > 0) meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                    UUID.nameUUIDFromBytes("rpgloot_damage".getBytes(StandardCharsets.UTF_8)),
                    "rpgloot_damage", damageBonus, AttributeModifier.Operation.ADD_NUMBER));
            if (speedBonus > 0) meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(
                    UUID.nameUUIDFromBytes("rpgloot_speed".getBytes(StandardCharsets.UTF_8)),
                    "rpgloot_speed", speedBonus, AttributeModifier.Operation.ADD_NUMBER));
        } else if (type.isArmor()) {
            double defenseBonus = VanillaStats.baseArmor(mat) * (primaryMult - 1.0);
            if (defenseBonus > 0) meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                    UUID.nameUUIDFromBytes("rpgloot_armor".getBytes(StandardCharsets.UTF_8)),
                    "rpgloot_armor", defenseBonus, AttributeModifier.Operation.ADD_NUMBER));
        }

        List<RolledStat> fixedStats = artifact.getFixedStats();
        applyPassiveAttributes(meta, fixedStats);

        meta.getPersistentDataContainer().set(Keys.RARITY,      PersistentDataType.STRING, Rarity.LEGENDARY.name());
        meta.getPersistentDataContainer().set(Keys.BONUS_STATS, PersistentDataType.STRING, serializeStats(fixedStats));
        meta.getPersistentDataContainer().set(Keys.WEAPON_NAME, PersistentDataType.STRING, artifact.getDisplayName());
        meta.getPersistentDataContainer().set(Keys.ARTIFACT_ID, PersistentDataType.STRING, artifact.name());

        TextColor artifactColor = TextColor.color(0xB026FF);
        meta.displayName(Component.text("✦ " + artifact.getDisplayName() + " ✦", artifactColor)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Artifact", artifactColor).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        for (String line : artifact.getFlavorText()) {
            lore.add(Component.text(line, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, true));
        }
        lore.add(Component.empty());

        if (isRanged(type)) {
            lore.add(Component.text("Projectile weapon", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        } else if (type.isWeapon()) {
            double damageBonus = VanillaStats.baseDamage(mat) * (primaryMult - 1.0);
            double speedBonus  = VanillaStats.baseSpeed(mat)  * (speedMult  - 1.0);
            lore.add(statLine("Attack Damage", primaryMult, damageBonus));
            if (speedBonus > 0) lore.add(statLine("Attack Speed", speedMult, speedBonus));
        } else if (type.isArmor()) {
            double defenseBonus = VanillaStats.baseArmor(mat) * (primaryMult - 1.0);
            lore.add(statLine("Defense", primaryMult, defenseBonus));
        }

        lore.add(Component.empty());
        for (RolledStat rolled : fixedStats) {
            lore.add(bonusStatLine(rolled));
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    // ── Passive attribute stats (applied directly to item, not via listeners) ─

    private void applyPassiveAttributes(ItemMeta meta, List<RolledStat> stats) {
        for (RolledStat rolled : stats) {
            switch (rolled.stat()) {
                case HEALTH_BOOST -> meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(
                        UUID.nameUUIDFromBytes("rpgloot_health".getBytes(StandardCharsets.UTF_8)),
                        "rpgloot_health", rolled.value(), AttributeModifier.Operation.ADD_NUMBER));
                case SPEED_BOOST -> meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(
                        UUID.nameUUIDFromBytes("rpgloot_speed_armor".getBytes(StandardCharsets.UTF_8)),
                        "rpgloot_speed_armor", 0.1 * (rolled.value() / 100.0), AttributeModifier.Operation.ADD_NUMBER));
                case LUCK_BOOST -> meta.addAttributeModifier(Attribute.GENERIC_LUCK, new AttributeModifier(
                        UUID.nameUUIDFromBytes("rpgloot_luck".getBytes(StandardCharsets.UTF_8)),
                        "rpgloot_luck", rolled.value(), AttributeModifier.Operation.ADD_NUMBER));
                default -> {}
            }
        }
    }

    // ── Public read helpers ───────────────────────────────────────────────

    public Rarity getRarity(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(Keys.RARITY, PersistentDataType.STRING);
        if (raw == null) return null;
        try { return Rarity.valueOf(raw); }
        catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown rarity value in PDC: '" + raw + "'");
            return null;
        }
    }

    public List<RolledStat> getBonusStats(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return List.of();
        String raw = item.getItemMeta().getPersistentDataContainer().get(Keys.BONUS_STATS, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return List.of();
        return statCache.computeIfAbsent(raw, this::parseStats);
    }

    private List<RolledStat> parseStats(String raw) {
        List<RolledStat> stats = new ArrayList<>();
        for (String part : raw.split(";")) {
            RolledStat rs = RolledStat.deserialize(part);
            if (rs != null) stats.add(rs);
        }
        return List.copyOf(stats); // immutable — safe to share across callers
    }

    public boolean isAxeTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String cat = item.getItemMeta().getPersistentDataContainer().get(Keys.ITEM_CATEGORY, PersistentDataType.STRING);
        return "AXE_TOOL".equals(cat);
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private List<RolledStat> rollBonusStats(Rarity rarity, WeaponType type) {
        List<RolledStat> result = new ArrayList<>();
        List<BonusStat> pool = new ArrayList<>(type.getBonusPool());
        int count = Math.min(rarity.getBonusStatCount(), pool.size());
        for (int i = 0; i < count; i++) {
            BonusStat chosen = pool.remove(random.nextInt(pool.size()));
            double[] range = balanceConfig.getStatRange(chosen, rarity);
            result.add(new RolledStat(chosen, randomBetween(range[0], range[1])));
        }
        return result;
    }

    private String serializeStats(List<RolledStat> stats) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stats.size(); i++) {
            if (i > 0) sb.append(";");
            sb.append(stats.get(i).serialize());
        }
        return sb.toString();
    }

    private Component statLine(String label, double multiplier, double absoluteBonus) {
        String display = plugin.getConfig().getString("stat-display", "percentage");
        int percent = (int) Math.round((multiplier - 1.0) * 100);
        String text = switch (display) {
            case "absolute" -> label + ": +" + String.format("%.2f", absoluteBonus);
            case "mixed"    -> label + ": +" + String.format("%.2f", absoluteBonus) + "  (+" + percent + "%)";
            default         -> label + ": +" + percent + "%";
        };
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private Component bonusStatLine(RolledStat rolled) {
        double val = rolled.value();
        String formatted = val < 10
                ? String.format("%.1f", val)
                : String.valueOf((int) Math.round(val));
        return Component.text(
                rolled.stat().getLabel() + ": +" + formatted + rolled.stat().getUnit(),
                NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false);
    }

    private boolean isRanged(WeaponType type) {
        return type == WeaponType.BOW || type == WeaponType.CROSSBOW;
    }

    private String toolLabel(WeaponType type) {
        return switch (type) {
            case FISHING_ROD -> "Fishing rod";
            case HOE         -> "Farming tool";
            case SHOVEL      -> "Digging tool";
            case AXE_TOOL    -> "Woodcutting axe";
            default          -> "Mining tool";
        };
    }

    private double randomBetween(double min, double max) {
        if (min >= max) return min;
        return min + random.nextDouble() * (max - min);
    }
}
